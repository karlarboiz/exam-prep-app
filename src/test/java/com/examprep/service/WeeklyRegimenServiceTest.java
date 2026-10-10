package com.examprep.service;

import com.examprep.dao.AttemptDao;
import com.examprep.dao.DiagnosticSubjectScoreDao;
import com.examprep.dao.EmailOutboxDao;
import com.examprep.dao.UserDao;
import com.examprep.dao.WeeklyRegimenDao;
import com.examprep.dao.WeeklySubjectScoreDao;
import com.examprep.model.AttemptKind;
import com.examprep.model.AttemptStatus;
import com.examprep.model.DiagnosticSubjectScore;
import com.examprep.model.EmailOutbox;
import com.examprep.model.ExamAttempt;
import com.examprep.model.ExamLevel;
import com.examprep.model.Question;
import com.examprep.model.SubjectBand;
import com.examprep.model.User;
import com.examprep.model.WeeklyDashboard;
import com.examprep.model.WeeklyRegimen;
import com.examprep.model.WeeklyRegimenStatus;
import com.examprep.model.WeeklySubjectScore;
import com.examprep.support.DatabaseTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyRegimenServiceTest extends DatabaseTestSupport {

    private final WeeklyRegimenService service = new WeeklyRegimenService();
    private final AccessGrantService accessGrantService = new AccessGrantService();
    private final DiagnosticService diagnosticService = new DiagnosticService();
    private final UserDao userDao = new UserDao();
    private final AttemptDao attemptDao = new AttemptDao();
    private final WeeklyRegimenDao regimenDao = new WeeklyRegimenDao();
    private final WeeklySubjectScoreDao weeklyScoreDao = new WeeklySubjectScoreDao();
    private final DiagnosticSubjectScoreDao diagnosticScoreDao = new DiagnosticSubjectScoreDao();
    private final EmailOutboxDao outboxDao = new EmailOutboxDao();

    @Test
    void officialAttemptLocksAndDoesNotOverwriteScore() throws Exception {
        User user = readyUser(LocalDateTime.now().minusHours(1), 30);

        ExamAttempt first = service.startWeeklyExam(user.getId());
        Map<Long, String> answers = allCorrect(first.getId());
        ExamAttempt submitted = service.submitWeeklyExam(first.getId(), answers);
        assertEquals(AttemptStatus.COMPLETED, submitted.getStatus());
        assertNotNull(submitted.getScorePercent());

        WeeklyRegimen week = regimenDao.findByUserAndWeek(user.getId(), 1).orElseThrow();
        assertEquals(submitted.getId(), week.getOfficialAttemptId());
        assertEquals(WeeklyRegimenStatus.COMPLETED, week.getStatus());
        BigDecimal officialScore = submitted.getScorePercent();

        assertThrows(IllegalStateException.class, () -> service.startWeeklyExam(user.getId()));

        ExamAttempt again = service.submitWeeklyExam(first.getId(), Map.of());
        assertEquals(officialScore, again.getScorePercent());
        assertEquals(submitted.getId(), regimenDao.findById(week.getId()).orElseThrow().getOfficialAttemptId());
    }

    @Test
    void reviewDoesNotWriteANewOfficialScore() throws Exception {
        User user = readyUser(LocalDateTime.now().minusHours(1), 30);
        ExamAttempt attempt = service.startWeeklyExam(user.getId());
        service.submitWeeklyExam(attempt.getId(), allWrong(attempt.getId()));
        WeeklyRegimen week = regimenDao.findByUserAndWeek(user.getId(), 1).orElseThrow();
        Long officialId = week.getOfficialAttemptId();
        int attemptCount = attemptDao.findByUserId(user.getId()).size();

        assertFalse(service.getReviewMisses(user.getId(), week.getId()).isEmpty());
        assertEquals(officialId, regimenDao.findById(week.getId()).orElseThrow().getOfficialAttemptId());
        assertEquals(attemptCount, attemptDao.findByUserId(user.getId()).size());
        assertTrue(attemptDao.findByUserId(user.getId()).stream()
                .noneMatch(a -> a.getAttemptKind() == AttemptKind.REVIEW));
    }

    @Test
    void nextWeekOverSamplesWeakSubjects() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 3, 1, 8, 0);
        User user = readyUser(start, 28);
        service.setClock(Clock.fixed(start.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));

        WeeklyRegimen week1 = service.ensureCurrentWeek(user.getId());
        WeeklySubjectScore weak = score(week1.getId(), 1L, "20.00", SubjectBand.WEAK);
        WeeklySubjectScore strong = score(week1.getId(), 2L, "90.00", SubjectBand.STRONG);
        weeklyScoreDao.replaceForRegimen(week1.getId(), List.of(weak, strong));
        ExamAttempt placeholder = attemptDao.create(user.getId(), 3L, AttemptKind.WEEKLY, week1.getId(), null);
        attemptDao.completeAttempt(placeholder.getId(), new BigDecimal("40.00"), AttemptStatus.COMPLETED);
        regimenDao.setOfficialAttempt(week1.getId(), placeholder.getId());

        Instant week2Instant = start.plusDays(8).atZone(ZoneId.systemDefault()).toInstant();
        service.setClock(Clock.fixed(week2Instant, ZoneId.systemDefault()));
        WeeklyRegimen week2 = service.ensureCurrentWeek(user.getId());
        assertEquals(2, week2.getWeekNumber());

        List<Question> form = new com.examprep.dao.QuestionDao().findByIds(regimenDao.findFormQuestionIds(week2.getId()));
        long weakCount = form.stream().filter(q -> q.getSubjectId() == 1L).count();
        long strongCount = form.stream().filter(q -> q.getSubjectId() == 2L).count();
        assertTrue(weakCount > strongCount, "weak=" + weakCount + " strong=" + strongCount);

        Set<Long> week1Ids = regimenDao.findFormQuestionIdsForWeek(user.getId(), 1);
        Set<Long> week2Ids = Set.copyOf(regimenDao.findFormQuestionIds(week2.getId()));
        if (!week1Ids.isEmpty() && week2Ids.size() >= 4) {
            assertTrue(week2Ids.stream().anyMatch(id -> !week1Ids.contains(id))
                    || week1Ids.isEmpty());
        }
    }

    @Test
    void missedWeekKeepsLastPlanAndUnlocksNextForm() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 4, 1, 8, 0);
        User user = readyUser(start, 28);
        service.setClock(Clock.fixed(start.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
        WeeklyRegimen week1 = service.ensureCurrentWeek(user.getId());
        assertEquals(WeeklyRegimenStatus.OPEN, week1.getStatus());
        assertFalse(week1.hasOfficialScore());

        service.setClock(Clock.fixed(start.plusDays(8).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
        WeeklyDashboard dashboard = service.resolveDashboard(user.getId());
        assertEquals(2, dashboard.getCurrent().getWeekNumber());
        assertEquals(WeeklyRegimenStatus.MISSED, regimenDao.findById(week1.getId()).orElseThrow().getStatus());
        assertTrue(dashboard.isMissedWeekNotice());
        assertTrue(dashboard.getStudyPlan().isFromDiagnostic());
        assertTrue(dashboard.isCanStartWeekly());
        assertNotEquals(week1.getId(), dashboard.getCurrent().getId());
    }

    @Test
    void diagnosticBandsSeedWeekOneAndEmailGoesToOutbox() throws Exception {
        User user = readyUser(LocalDateTime.now().minusHours(1), 21);
        WeeklyRegimen week = service.ensureCurrentWeek(user.getId());
        assertFalse(regimenDao.findFormQuestionIds(week.getId()).isEmpty());

        ExamAttempt attempt = service.startWeeklyExam(user.getId());
        service.submitWeeklyExam(attempt.getId(), allWrong(attempt.getId()));

        List<EmailOutbox> mail = outboxDao.findByRegimenId(week.getId());
        assertEquals(1, mail.size());
        assertTrue(mail.get(0).getBody().toLowerCase().contains("study plan"));
        assertNotNull(regimenDao.findById(week.getId()).orElseThrow().getEmailSentAt());

        WeeklyDashboard dashboard = service.resolveDashboard(user.getId());
        assertFalse(dashboard.isCanStartWeekly());
        assertFalse(service.getReviewMisses(user.getId(), week.getId()).isEmpty());
        assertTrue(dashboard.isCanReview(), "misses=" + dashboard.getStudyPlan().getMisses().size());
        assertFalse(dashboard.getStudyPlan().getTargets().isEmpty());
        assertTrue(dashboard.getStudyPlan().getTargets().size() <= 5);
    }

    @Test
    void expiredGrantSkipsEmail() throws Exception {
        AccessGrantService.CreatedAccessToken token = accessGrantService.createToken(
                LocalDateTime.now().plusDays(1), null, "prep", "wk-exp", ExamLevel.PROFESSIONAL);
        User user = accessGrantService.registerWithToken(
                token.rawToken(), "expuser", "expuser@example.com", "password123");
        completeDiagnostic(user.getId());
        userDao.setDiagnosticCompletedAt(user.getId(), LocalDateTime.now().minusHours(2));

        ExamAttempt attempt = service.startWeeklyExam(user.getId());
        accessGrantService.revoke(token.grant().getId());
        service.submitWeeklyExam(attempt.getId(), allCorrect(attempt.getId()));

        WeeklyRegimen week = regimenDao.findByUserAndWeek(user.getId(), 1).orElseThrow();
        assertTrue(outboxDao.findByRegimenId(week.getId()).isEmpty());
        assertNotNull(week.getOfficialAttemptId());
    }

    @Test
    void checkpointDoesNotReplaceOfficialScore() throws Exception {
        User user = readyUser(LocalDateTime.now().minusHours(1), 30);
        ExamAttempt weekly = service.startWeeklyExam(user.getId());
        service.submitWeeklyExam(weekly.getId(), allWrong(weekly.getId()));
        WeeklyRegimen week = regimenDao.findByUserAndWeek(user.getId(), 1).orElseThrow();
        Long officialId = week.getOfficialAttemptId();

        ExamAttempt checkpoint = service.startCheckpoint(user.getId());
        assertEquals(AttemptKind.CHECKPOINT, checkpoint.getAttemptKind());
        service.submitCheckpoint(checkpoint.getId(), allCorrect(checkpoint.getId()));

        assertEquals(officialId, regimenDao.findById(week.getId()).orElseThrow().getOfficialAttemptId());
        assertNotEquals(officialId, checkpoint.getId());
    }

    private User readyUser(LocalDateTime diagnosticAt, int grantDays) throws Exception {
        String stamp = String.valueOf(System.nanoTime());
        AccessGrantService.CreatedAccessToken token = accessGrantService.createToken(
                LocalDateTime.now().plusDays(Math.max(grantDays, 14)), null, "prep", stamp, ExamLevel.PROFESSIONAL);
        User user = accessGrantService.registerWithToken(
                token.rawToken(), "u" + stamp.substring(stamp.length() - 8),
                "u" + stamp.substring(stamp.length() - 8) + "@example.com", "password123");
        completeDiagnostic(user.getId());
        userDao.setDiagnosticCompletedAt(user.getId(), diagnosticAt);
        new com.examprep.dao.AccessGrantDao().updateExpiresAt(
                token.grant().getId(), diagnosticAt.plusDays(grantDays));
        return userDao.findById(user.getId()).orElseThrow();
    }

    private void completeDiagnostic(Long userId) throws Exception {
        ExamAttempt attempt = diagnosticService.startDiagnostic(userId);
        diagnosticService.beginDiagnostic(attempt.getId());
        Map<Long, String> answers = new HashMap<>();
        for (Question q : diagnosticService.getAttemptQuestions(attempt.getId())) {
            answers.put(q.getId(), q.getCorrectOption());
        }
        diagnosticService.submitDiagnostic(attempt.getId(), answers);
        List<DiagnosticSubjectScore> scores = diagnosticScoreDao.findLatestByUserId(userId);
        assertFalse(scores.isEmpty());
    }

    private Map<Long, String> allCorrect(Long attemptId) throws Exception {
        Map<Long, String> answers = new HashMap<>();
        for (Question q : service.getAttemptQuestions(attemptId)) {
            answers.put(q.getId(), q.getCorrectOption());
        }
        return answers;
    }

    private Map<Long, String> allWrong(Long attemptId) throws Exception {
        Map<Long, String> answers = new HashMap<>();
        for (Question q : service.getAttemptQuestions(attemptId)) {
            answers.put(q.getId(), "A".equalsIgnoreCase(q.getCorrectOption()) ? "B" : "A");
        }
        return answers;
    }

    private static WeeklySubjectScore score(Long regimenId, Long subjectId, String percent, SubjectBand band) {
        WeeklySubjectScore row = new WeeklySubjectScore();
        row.setRegimenId(regimenId);
        row.setSubjectId(subjectId);
        row.setScorePercent(new BigDecimal(percent));
        row.setBand(band);
        return row;
    }
}
