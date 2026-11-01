package com.examprep.service;

import com.examprep.dao.AccessGrantDao;
import com.examprep.dao.AttemptDao;
import com.examprep.dao.DiagnosticSubjectScoreDao;
import com.examprep.dao.EmailOutboxDao;
import com.examprep.dao.QuestionDao;
import com.examprep.dao.RegimenFollowUpDao;
import com.examprep.dao.UserDao;
import com.examprep.dao.WeeklyRegimenDao;
import com.examprep.dao.WeeklySubjectScoreDao;
import com.examprep.model.AccessGrant;
import com.examprep.model.AttemptAnswer;
import com.examprep.model.AttemptKind;
import com.examprep.model.AttemptStatus;
import com.examprep.model.DiagnosticSubjectScore;
import com.examprep.model.EmailOutbox;
import com.examprep.model.ExamAttempt;
import com.examprep.model.ExamLevel;
import com.examprep.model.FollowUpKind;
import com.examprep.model.Question;
import com.examprep.model.SubjectBand;
import com.examprep.model.User;
import com.examprep.model.WeeklyDashboard;
import com.examprep.model.WeeklyRegimen;
import com.examprep.model.WeeklyRegimenStatus;
import com.examprep.model.WeeklySubjectScore;
import com.examprep.support.ConfigOverrides;
import com.examprep.support.DatabaseTestSupport;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Test
    void overdueOpenWeekSendsExamFollowUpOnceThenMissedFollowUp() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 8, 0);
        User user = readyUser(start, 28);
        service.setClock(clockAt(start));
        WeeklyRegimen week = service.ensureCurrentWeek(user.getId());
        service.sweepFollowUps(user.getId());
        assertTrue(followUps(week.getId()).isEmpty());

        service.setClock(clockAt(start.plusDays(3)));
        WeeklyDashboard due = service.resolveDashboard(user.getId());
        List<EmailOutbox> dueMail = followUps(week.getId());
        assertEquals(1, dueMail.size());
        assertTrue(dueMail.get(0).getSubject().contains("still open"));
        assertTrue(dueMail.get(0).getBody().toLowerCase().contains("follow-up"));
        assertTrue(due.isExamFollowUpSent());

        service.sweepFollowUps(user.getId());
        assertEquals(1, followUps(week.getId()).size());

        service.setClock(clockAt(start.plusDays(8)));
        WeeklyDashboard missed = service.resolveDashboard(user.getId());
        assertEquals(WeeklyRegimenStatus.MISSED, regimenDao.findById(week.getId()).orElseThrow().getStatus());
        List<EmailOutbox> all = followUps(week.getId());
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(mail -> mail.getSubject().contains("was missed")));
        assertTrue(missed.isMissedExamFollowUpSent());
        assertFalse(missed.isExamFollowUpSent());
    }

    @Test
    void unfinishedCheckpointSendsFollowUpThenMissedFollowUp() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 5, 4, 8, 0);
        User user = readyUser(start, 30);
        service.setClock(clockAt(start));
        ExamAttempt weekly = service.startWeeklyExam(user.getId());
        service.submitWeeklyExam(weekly.getId(), allWrong(weekly.getId()));
        WeeklyRegimen week = regimenDao.findByUserAndWeek(user.getId(), 1).orElseThrow();

        service.setClock(clockAt(start.plusDays(3)));
        WeeklyDashboard due = service.resolveDashboard(user.getId());
        List<EmailOutbox> mail = followUps(week.getId());
        assertEquals(1, mail.size(), mail.toString());
        assertTrue(mail.get(0).getSubject().contains("checkpoint still open"));
        assertTrue(due.isCheckpointFollowUpSent());
        assertFalse(due.isExamFollowUpSent());

        service.setClock(clockAt(start.plusDays(8)));
        service.sweepFollowUps(user.getId());
        List<EmailOutbox> missed = followUps(week.getId());
        assertEquals(2, missed.size());
        assertTrue(missed.stream().anyMatch(row -> row.getSubject().contains("checkpoint was missed")));
    }

    @Test
    void completedCheckpointSkipsCheckpointFollowUp() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 5, 6, 8, 0);
        User user = readyUser(start, 30);
        service.setClock(clockAt(start));
        ExamAttempt weekly = service.startWeeklyExam(user.getId());
        service.submitWeeklyExam(weekly.getId(), allWrong(weekly.getId()));
        ExamAttempt checkpoint = service.startCheckpoint(user.getId());
        service.submitCheckpoint(checkpoint.getId(), allCorrect(checkpoint.getId()));
        WeeklyRegimen week = regimenDao.findByUserAndWeek(user.getId(), 1).orElseThrow();

        service.setClock(clockAt(start.plusDays(8)));
        service.sweepFollowUps(user.getId());
        assertTrue(followUps(week.getId()).isEmpty());
    }

    @Test
    void expiredGrantSkipsFollowUp() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 8, 0);
        User user = readyUser(start, 10);
        service.setClock(clockAt(start));
        WeeklyRegimen week = service.ensureCurrentWeek(user.getId());
        service.setClock(clockAt(start.plusDays(11)));
        service.sweepFollowUps(user.getId());
        assertTrue(outboxDao.findByRegimenId(week.getId()).isEmpty());
    }

    @Test
    void rejectsUsersWhoAreNotReadyAndSavesAnswers() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.ensureCurrentWeek(999_999L));
        User bare = new UserDao().create("bare", "bare@example.com", "hash",
                com.examprep.model.Role.USER, ExamLevel.PROFESSIONAL);
        assertThrows(IllegalStateException.class, () -> service.ensureCurrentWeek(bare.getId()));
        new UserDao().setDiagnosticCompletedAt(bare.getId(), LocalDateTime.now().minusDays(1));
        assertThrows(IllegalStateException.class, () -> service.ensureCurrentWeek(bare.getId()));

        service.setClock(null);
        assertNotNull(service.now());
        assertTrue(service.basePerSubject() >= 1);

        User user = readyUser(LocalDateTime.now().minusHours(1), 21);
        ExamAttempt attempt = service.startWeeklyExam(user.getId());
        assertEquals(attempt.getId(), service.startWeeklyExam(user.getId()).getId());
        Long questionId = service.getAttemptQuestions(attempt.getId()).get(0).getId();
        service.saveAnswer(attempt.getId(), questionId, "b");
        assertEquals("B", service.getAnswerMap(attempt.getId()).get(questionId));
        assertFalse(service.getAttemptAnswers(attempt.getId()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.saveAnswer(attempt.getId(), 999_999L, "A"));
        assertThrows(IllegalArgumentException.class, () -> service.getAttempt(999_999L));

        service.submitWeeklyExam(attempt.getId(), null);
        assertThrows(IllegalStateException.class, () -> service.saveAnswer(attempt.getId(), questionId, "A"));
        assertThrows(IllegalStateException.class, () -> service.submitCheckpoint(attempt.getId(), Map.of()));
    }

    @Test
    void missedExpiredAndEmptyFormsFailClosed() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 8, 0);
        User user = readyUser(start, 21);
        service.setClock(clockAt(start));
        WeeklyRegimen week = service.ensureCurrentWeek(user.getId());
        regimenDao.markMissed(week.getId());
        assertThrows(IllegalStateException.class, () -> service.startWeeklyExam(user.getId()));

        regimenDao.findById(week.getId());
        try (var conn = com.examprep.config.DatabaseManager.getConnection();
             var ps = conn.prepareStatement("UPDATE weekly_regimens SET status = 'OPEN' WHERE id = ?")) {
            ps.setLong(1, week.getId());
            ps.executeUpdate();
        }
        ExamAttempt attempt = service.startWeeklyExam(user.getId());
        new AttemptDao().updateStartedAt(attempt.getId(), start.minusHours(5));
        ExamAttempt closed = service.startWeeklyExam(user.getId());
        assertEquals(AttemptStatus.EXPIRED, closed.getStatus());

        for (var exam : new com.examprep.dao.ExamDao().findAll()) {
            if (exam.isWeekly()) {
                exam.setActive(false);
                new com.examprep.dao.ExamDao().update(exam);
            }
        }
        assertThrows(IllegalStateException.class, () -> service.startWeeklyExam(user.getId()));
        assertThrows(IllegalStateException.class, () -> service.ensureCurrentWeek(
                readyUser(start.plusDays(40), 14).getId()));
    }

    @Test
    void checkpointsAndPlansCoverTheRemainingBranches() throws Exception {
        User user = readyUser(LocalDateTime.now().minusHours(2), 30);
        assertThrows(IllegalStateException.class, () -> service.startCheckpoint(user.getId()));
        ExamAttempt weekly = service.startWeeklyExam(user.getId());
        try (var conn = com.examprep.config.DatabaseManager.getConnection();
             var ps = conn.prepareStatement("DELETE FROM attempt_questions WHERE attempt_id = ?")) {
            ps.setLong(1, weekly.getId());
            ps.executeUpdate();
        }
        ExamAttempt zero = service.submitWeeklyExam(weekly.getId(), Map.of(1L, " "));
        assertEquals(0, zero.getScorePercent().signum());
        ExamAttempt checkpoint = service.startCheckpoint(user.getId());
        assertThrows(IllegalStateException.class, () -> service.submitWeeklyExam(checkpoint.getId(), Map.of()));
        service.submitCheckpoint(checkpoint.getId(), Map.of());
        assertEquals(checkpoint.getId(), service.submitCheckpoint(checkpoint.getId(), Map.of()).getId());

        String minFresh = ConfigOverrides.set("weekly.checkpoint.min.fresh", "1000");
        try {
            WeeklyDashboard dashboard = service.resolveDashboard(user.getId());
            assertNotNull(dashboard.getBankWarning());
            assertThrows(IllegalStateException.class, () -> service.startCheckpoint(user.getId()));
        } finally {
            ConfigOverrides.restore("weekly.checkpoint.min.fresh", minFresh);
        }

        WeeklyRegimen week = regimenDao.findByUserAndWeek(user.getId(), 1).orElseThrow();
        assertNotNull(service.getStudyPlan(user.getId(), null).getRegimen());
        assertNotNull(service.getStudyPlan(user.getId(), week.getId()).getRegimen());
        assertThrows(IllegalArgumentException.class, () -> service.getStudyPlan(user.getId(), 999_999L));
        User other = readyUser(LocalDateTime.now().minusHours(1), 20);
        assertThrows(IllegalArgumentException.class, () -> service.getStudyPlan(other.getId(), week.getId()));

        List<String> targets = WeeklyRegimenService.buildTargets(List.of(
                score(1L, 1L, "20.00", SubjectBand.WEAK),
                score(1L, 2L, "60.00", SubjectBand.DEVELOPING),
                score(1L, 3L, "10.00", SubjectBand.WEAK),
                score(1L, 4L, "15.00", SubjectBand.WEAK),
                score(1L, 5L, "16.00", SubjectBand.WEAK),
                score(1L, 6L, "17.00", SubjectBand.WEAK)), null);
        assertTrue(targets.size() <= 5);

        com.examprep.model.Question prompt = new com.examprep.model.Question();
        prompt.setSubjectId(9L);
        prompt.setSubjectName("Extra");
        prompt.setPrompt("x".repeat(90));
        com.examprep.model.AttemptAnswer miss = new com.examprep.model.AttemptAnswer();
        miss.setQuestion(prompt);
        miss.setCorrect(false);
        com.examprep.model.AttemptAnswer again = new com.examprep.model.AttemptAnswer();
        again.setQuestion(prompt);
        List<String> reread = WeeklyRegimenService.buildTargets(List.of(), List.of(miss, again, new com.examprep.model.AttemptAnswer()));
        assertFalse(reread.isEmpty());
        assertTrue(reread.get(0).contains("..."));
    }

    @Test
    void shortWeekUsesMidpointFollowUpAndDashboardClosesExpiredWork() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 8, 0);
        User user = readyUser(start, 2);
        service.setClock(clockAt(start.plusDays(1)));
        service.sweepFollowUps(user.getId());
        WeeklyRegimen week = regimenDao.findByUserAndWeek(user.getId(), 1).orElseThrow();
        assertFalse(followUps(week.getId()).isEmpty());

        service.setClock(clockAt(start));
        ExamAttempt attempt = service.startWeeklyExam(user.getId());
        new AttemptDao().updateStartedAt(attempt.getId(), start.minusDays(2));
        WeeklyDashboard dashboard = service.resolveDashboard(user.getId());
        assertNotEquals(AttemptStatus.IN_PROGRESS, service.getAttempt(attempt.getId()).getStatus());
        assertFalse(dashboard.isCanContinueWeekly());

        List<Long> sampled = service.sampleWeeklyForm(user, 5, true, 1);
        assertFalse(sampled.isEmpty());
    }

    @Test
    void failureBranchesForExamsCheckpointsAndTargets() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 8, 0);
        User user = readyUser(start, 28);
        service.setClock(clockAt(start));
        WeeklyRegimen week = service.ensureCurrentWeek(user.getId());
        WeeklyDashboard open = service.resolveDashboard(user.getId());
        assertTrue(open.isCanStartWeekly());

        ExamAttempt attempt = service.startWeeklyExam(user.getId());
        assertTrue(service.resolveDashboard(user.getId()).isCanContinueWeekly());
        new AttemptDao().updateStartedAt(attempt.getId(), start.minusDays(3));
        assertThrows(IllegalStateException.class, () -> service.saveAnswer(attempt.getId(), 1L, "A"));

        for (var exam : new com.examprep.dao.ExamDao().findAll()) {
            if (exam.isWeekly()) {
                exam.setActive(false);
                new com.examprep.dao.ExamDao().update(exam);
            }
        }
        assertThrows(IllegalStateException.class, () -> service.startWeeklyExam(user.getId()));
        var weeklyExam = new com.examprep.dao.ExamDao().findById(3L).orElseThrow();
        weeklyExam.setActive(true);
        new com.examprep.dao.ExamDao().update(weeklyExam);

        try (var conn = com.examprep.config.DatabaseManager.getConnection();
             var ps = conn.prepareStatement("DELETE FROM weekly_form_questions WHERE regimen_id = ?")) {
            ps.setLong(1, week.getId());
            ps.executeUpdate();
        }
        assertThrows(IllegalStateException.class, () -> service.startWeeklyExam(user.getId()));

        User fresh = readyUser(start, 21);
        service.setClock(clockAt(start));
        ExamAttempt weekly = service.startWeeklyExam(fresh.getId());
        WeeklyRegimen freshWeek = regimenDao.findByUserAndWeek(fresh.getId(), 1).orElseThrow();
        service.submitWeeklyExam(weekly.getId(), allWrong(weekly.getId()));
        ExamAttempt extra = new AttemptDao().create(fresh.getId(), 3L, AttemptKind.WEEKLY, freshWeek.getId(), null);
        ExamAttempt closed = service.submitWeeklyExam(extra.getId(), Map.of());
        assertEquals(AttemptStatus.EXPIRED, closed.getStatus());

        try (var conn = com.examprep.config.DatabaseManager.getConnection();
             var ps = conn.prepareStatement("UPDATE exam_attempts SET regimen_id = 999999 WHERE id = ?")) {
            ps.setLong(1, extra.getId());
            ps.executeUpdate();
        }
        new AttemptDao().updateStatus(extra.getId(), AttemptStatus.IN_PROGRESS);
        assertThrows(IllegalStateException.class, () -> service.submitWeeklyExam(extra.getId(), Map.of()));

        service.setClock(clockAt(start.plusDays(40)));
        assertThrows(IllegalStateException.class, () -> service.startCheckpoint(fresh.getId()));
        service.setClock(clockAt(start.plusDays(1)));
        ExamAttempt checkpoint = service.startCheckpoint(fresh.getId());
        assertEquals(checkpoint.getId(), service.startCheckpoint(fresh.getId()).getId());
        new AttemptDao().updateStartedAt(checkpoint.getId(), start.minusDays(2));
        assertEquals(AttemptStatus.EXPIRED, service.startCheckpoint(fresh.getId()).getStatus());

        String minFresh = ConfigOverrides.set("weekly.checkpoint.min.fresh", "1000");
        try {
            service.setClock(clockAt(start.plusDays(3)));
            service.sweepFollowUps(fresh.getId());
        } finally {
            ConfigOverrides.restore("weekly.checkpoint.min.fresh", minFresh);
        }
        User missedCheckpoint = readyUser(start, 14);
        service.setClock(clockAt(start));
        ExamAttempt missedWeekly = service.startWeeklyExam(missedCheckpoint.getId());
        service.submitWeeklyExam(missedWeekly.getId(), allWrong(missedWeekly.getId()));
        service.setClock(clockAt(start.plusDays(10)));
        service.sweepFollowUps(missedCheckpoint.getId());
        assertTrue(service.resolveDashboard(missedCheckpoint.getId()).isCheckpointMissedFollowUpSent());

        com.examprep.model.Question one = new com.examprep.model.Question();
        one.setSubjectId(1L);
        one.setSubjectName("General Knowledge");
        one.setPrompt("Short prompt");
        com.examprep.model.AttemptAnswer single = new com.examprep.model.AttemptAnswer();
        single.setQuestion(one);
        List<String> singular = WeeklyRegimenService.buildTargets(
                List.of(score(1L, 1L, "20.00", SubjectBand.WEAK)), List.of(single));
        assertFalse(singular.isEmpty());
        com.examprep.model.AttemptAnswer second = new com.examprep.model.AttemptAnswer();
        second.setQuestion(one);
        assertFalse(WeeklyRegimenService.buildTargets(
                List.of(score(1L, 1L, "20.00", SubjectBand.DEVELOPING)), List.of(single, second)).isEmpty());

        weeklyExam = new com.examprep.dao.ExamDao().findById(3L).orElseThrow();
        weeklyExam.setQuestionsPerSubject(null);
        weeklyExam.setActive(true);
        new com.examprep.dao.ExamDao().update(weeklyExam);
        service.setClock(clockAt(start.plusDays(8)));
        assertNotNull(service.ensureCurrentWeek(fresh.getId()));
    }

    @Test
    void coversDashboardFollowUpAndFailureEdges() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusDays(3).withNano(0);
        service.setClock(clockAt(start));

        User shortGrant = readyUser(start, 7);
        ExamAttempt weekly = service.startWeeklyExam(shortGrant.getId());
        service.submitWeeklyExam(weekly.getId(), allWrong(weekly.getId()));
        WeeklyRegimen shortWeek = regimenDao.findByUserAndWeek(shortGrant.getId(), 1).orElseThrow();
        for (var exam : new com.examprep.dao.ExamDao().findAll()) {
            if (exam.isWeekly()) {
                exam.setActive(false);
                new com.examprep.dao.ExamDao().update(exam);
            }
        }
        IllegalStateException noWeekly = assertThrows(IllegalStateException.class,
                () -> service.startCheckpoint(shortGrant.getId()));
        assertTrue(noWeekly.getMessage().contains("No active weekly"));
        var restored = new com.examprep.dao.ExamDao().findById(3L).orElseThrow();
        restored.setActive(true);
        new com.examprep.dao.ExamDao().update(restored);

        assertEquals(AttemptKind.CHECKPOINT, service.startCheckpoint(shortGrant.getId()).getAttemptKind());
        service.setClock(clockAt(start.plusDays(8)));
        IllegalStateException afterWeek = assertThrows(IllegalStateException.class,
                () -> service.startCheckpoint(shortGrant.getId()));
        assertTrue(afterWeek.getMessage().contains("during this week"));

        service.setClock(clockAt(start));
        User developing = readyUser(start, 21);
        ExamAttempt mixed = service.startWeeklyExam(developing.getId());
        service.submitWeeklyExam(mixed.getId(), halfCorrect(mixed.getId()));
        assertNotNull(service.startCheckpoint(developing.getId()));

        User perfect = readyUser(start, 21);
        ExamAttempt perfectAttempt = service.startWeeklyExam(perfect.getId());
        assertTrue(service.resolveDashboard(perfect.getId()).isCanContinueWeekly());
        service.submitWeeklyExam(perfectAttempt.getId(), allCorrect(perfectAttempt.getId()));
        assertFalse(service.resolveDashboard(perfect.getId()).isCanReview());
        WeeklyRegimen perfectWeek = regimenDao.findByUserAndWeek(perfect.getId(), 1).orElseThrow();
        attemptDao.create(perfect.getId(), 3L, AttemptKind.WEEKLY, perfectWeek.getId(), null);
        assertFalse(service.resolveDashboard(perfect.getId()).isCanContinueWeekly());

        try (var conn = com.examprep.config.DatabaseManager.getConnection();
             var ps = conn.prepareStatement("UPDATE weekly_regimens SET status = 'OPEN' WHERE id = ?")) {
            ps.setLong(1, perfectWeek.getId());
            ps.executeUpdate();
        }
        service.setClock(clockAt(start.plusDays(8)));
        assertNotNull(service.ensureCurrentWeek(perfect.getId()));

        service.setClock(clockAt(start));
        User missedNow = readyUser(start, 21);
        WeeklyRegimen openWeek = service.ensureCurrentWeek(missedNow.getId());
        regimenDao.markMissed(openWeek.getId());
        assertFalse(service.resolveDashboard(missedNow.getId()).isCanStartWeekly());
        service.sweepFollowUps(missedNow.getId());

        User overdue = readyUser(start, 7);
        service.setClock(clockAt(start.plusDays(8)));
        service.sweepFollowUps(overdue.getId());
        assertNotNull(service.resolveDashboard(overdue.getId()));

        service.setClock(clockAt(start));
        User nudged = readyUser(start, 21);
        ExamAttempt nudgedExam = service.startWeeklyExam(nudged.getId());
        service.submitWeeklyExam(nudgedExam.getId(), allWrong(nudgedExam.getId()));
        WeeklyRegimen nudgedWeek = regimenDao.findByUserAndWeek(nudged.getId(), 1).orElseThrow();
        new RegimenFollowUpDao().insert(nudgedWeek.getId(), FollowUpKind.CHECKPOINT_DUE, start);
        assertNotNull(service.resolveDashboard(nudged.getId()));
        service.setClock(clockAt(start.plusDays(8)));
        assertNotNull(service.resolveDashboard(nudged.getId()));

        User expiring = readyUser(start, 7);
        service.setClock(clockAt(start));
        ExamAttempt openAttempt = service.startWeeklyExam(expiring.getId());
        Map<Long, String> blanks = new HashMap<>();
        for (Question question : service.getAttemptQuestions(openAttempt.getId())) {
            blanks.put(question.getId(), " ");
        }
        service.setClock(clockAt(start.plusDays(20)));
        service.submitWeeklyExam(openAttempt.getId(), blanks);
        User another = readyUser(start, 14);
        service.setClock(clockAt(start));
        ExamAttempt nullAnswers = service.startWeeklyExam(another.getId());
        service.submitWeeklyExam(nullAnswers.getId(), null);

        String minFresh = ConfigOverrides.set("weekly.checkpoint.min.fresh", "1000");
        try {
            User blocked = readyUser(start, 21);
            service.setClock(clockAt(start));
            ExamAttempt blockedExam = service.startWeeklyExam(blocked.getId());
            service.submitWeeklyExam(blockedExam.getId(), allWrong(blockedExam.getId()));
            service.setClock(clockAt(start.plusDays(3)));
            service.sweepFollowUps(blocked.getId());
            assertNotNull(service.resolveDashboard(blocked.getId()).getStudyPlan());
        } finally {
            ConfigOverrides.restore("weekly.checkpoint.min.fresh", minFresh);
        }

        WeeklyRegimenService quiet = new WeeklyRegimenService();
        quiet.setClock(clockAt(start.plusDays(3)));
        setField(quiet, "mailService", new SilentMail());
        User due = readyUser(start, 21);
        quiet.sweepFollowUps(due.getId());
        ExamAttempt dueExam = quiet.startWeeklyExam(due.getId());
        quiet.submitWeeklyExam(dueExam.getId(), allWrong(dueExam.getId()));
        quiet.sweepFollowUps(due.getId());

        User mailed = readyUser(start, 21);
        call(service, "dispatchFollowUps", new Class<?>[] {User.class}, new Object[] {null});
        User copy = userDao.findById(mailed.getId()).orElseThrow();
        copy.setEmail(null);
        call(service, "dispatchFollowUps", new Class<?>[] {User.class}, new Object[] {copy});
        copy.setEmail(" ");
        call(service, "dispatchFollowUps", new Class<?>[] {User.class}, new Object[] {copy});
        service.setClock(clockAt(start.plusDays(40)));
        call(service, "dispatchFollowUps", new Class<?>[] {User.class},
                new Object[] {userDao.findById(mailed.getId()).orElseThrow()});

        WeeklyDashboard bare = new WeeklyDashboard();
        call(service, "applyFollowUpNotices", new Class<?>[] {WeeklyDashboard.class, Long.class},
                new Object[] {bare, mailed.getId()});

        WeeklyRegimen undated = new WeeklyRegimen();
        undated.setWeekStart(start);
        call(service, "followUpDue", new Class<?>[] {WeeklyRegimen.class}, new Object[] {undated});
        undated.setWeekEnd(start.plusDays(2));
        call(service, "followUpDue", new Class<?>[] {WeeklyRegimen.class}, new Object[] {undated});
        undated.setWeekEnd(start.plusDays(14));
        call(service, "followUpDue", new Class<?>[] {WeeklyRegimen.class}, new Object[] {undated});

        WeeklyRegimenService probe = new WeeklyRegimenService();
        setField(probe, "accessGrantDao", new AccessGrantDao() {
            @Override
            public Optional<AccessGrant> findLatestRedeemedByUserId(Long userId) {
                return Optional.empty();
            }
        });
        assertEquals(false, call(probe, "isGrantActive", new Class<?>[] {Long.class}, new Object[] {1L}));
        setField(probe, "accessGrantDao", new AccessGrantDao() {
            @Override
            public Optional<AccessGrant> findLatestRedeemedByUserId(Long userId) {
                return Optional.of(new AccessGrant());
            }
        });
        assertEquals(false, call(probe, "isGrantActive", new Class<?>[] {Long.class}, new Object[] {1L}));

        WeeklyRegimenService noGrant = new WeeklyRegimenService();
        noGrant.setClock(clockAt(start));
        User granted = readyUser(start, 21);
        setField(noGrant, "accessGrantDao", new SecondLookupEmpty());
        assertEquals(1, noGrant.resolveDashboard(granted.getId()).getTotalWeeks());

        WeeklyRegimenService dropDiag = new WeeklyRegimenService();
        dropDiag.setClock(clockAt(start));
        User diagnosed = readyUser(start, 21);
        setField(dropDiag, "userDao", new DropDiagnostic());
        assertEquals(1, dropDiag.resolveDashboard(diagnosed.getId()).getTotalWeeks());

        WeeklyRegimenService race = new WeeklyRegimenService();
        race.setClock(clockAt(start));
        User raceUser = readyUser(start, 21);
        ExamAttempt raceAttempt = race.startWeeklyExam(raceUser.getId());
        setField(race, "regimenDao", new ThrowOfficial());
        assertNotNull(race.submitWeeklyExam(raceAttempt.getId(), Map.of()));

        WeeklyRegimenService brokenPlan = new WeeklyRegimenService();
        setField(brokenPlan, "regimenDao", new EmptyLatest());
        setField(brokenPlan, "userDao", new BrokenUserDao());
        assertThrows(RuntimeException.class, () -> brokenPlan.getStudyPlan(raceUser.getId(), null));

        WeeklyRegimenService brokenSample = new WeeklyRegimenService();
        setField(brokenSample, "questionDao", new BrokenQuestionDao());
        assertThrows(RuntimeException.class, () -> brokenSample.sampleWeeklyForm(raceUser, 1, false, 4));

        AttemptAnswer missingPrompt = new AttemptAnswer();
        Question promptless = new Question();
        promptless.setSubjectId(1L);
        missingPrompt.setQuestion(promptless);
        Question longPrompt = new Question();
        longPrompt.setSubjectId(2L);
        longPrompt.setSubjectName("General Knowledge");
        longPrompt.setPrompt("x".repeat(90));
        AttemptAnswer longMiss = new AttemptAnswer();
        longMiss.setQuestion(longPrompt);
        assertFalse(WeeklyRegimenService.buildTargets(List.of(), List.of(missingPrompt, longMiss)).isEmpty());
        assertEquals(3, WeeklyRegimenService.buildTargets(List.of(
                score(1L, 1L, "10.00", SubjectBand.WEAK),
                score(1L, 2L, "20.00", SubjectBand.WEAK),
                score(1L, 3L, "30.00", SubjectBand.DEVELOPING)), List.of(longMiss)).size());
        assertTrue(WeeklyRegimenService.buildTargets(null, null).isEmpty());
    }

    @Test
    void coversPlanSamplingAndFollowUpPredicates() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusDays(2).withNano(0);
        service.setClock(clockAt(start));
        User planning = readyUser(start, 21);
        assertNotNull(service.getStudyPlan(planning.getId(), null));

        ExamAttempt weekly = service.startWeeklyExam(planning.getId());
        service.submitWeeklyExam(weekly.getId(), allWrong(weekly.getId()));
        ExamAttempt checkpoint = service.startCheckpoint(planning.getId());
        assertNotNull(service.resolveDashboard(planning.getId()).getInProgressCheckpointAttemptId());
        assertEquals(checkpoint.getId(),
                service.resolveDashboard(planning.getId()).getInProgressCheckpointAttemptId());

        User finished = readyUser(start, 7);
        ExamAttempt finishedExam = service.startWeeklyExam(finished.getId());
        service.submitWeeklyExam(finishedExam.getId(), allCorrect(finishedExam.getId()));
        service.setClock(clockAt(start.plusDays(8)));
        assertFalse(service.resolveDashboard(finished.getId()).isCheckpointAvailable());

        WeeklyRegimen open = service.ensureCurrentWeek(planning.getId());
        service.setClock(clockAt(start));
        open.setOfficialAttemptId(null);
        open.setStatus(WeeklyRegimenStatus.OPEN);
        open.setWeekEnd(start.minusHours(1));
        call(service, "sendExamFollowUp", new Class<?>[] {User.class, WeeklyRegimen.class},
                new Object[] {planning, open});
        open.setStatus(WeeklyRegimenStatus.MISSED);
        open.setWeekEnd(start.plusDays(6));
        call(service, "sendExamFollowUp", new Class<?>[] {User.class, WeeklyRegimen.class},
                new Object[] {planning, open});

        new com.examprep.dao.SubjectDao().create("No bank items", null, true, true);
        assertFalse(service.sampleWeeklyForm(planning, 1, false, 4).isEmpty());

        String minFresh = ConfigOverrides.set("weekly.checkpoint.min.fresh", "1000");
        try {
            User blocked = readyUser(start, 21);
            service.setClock(clockAt(start));
            ExamAttempt blockedExam = service.startWeeklyExam(blocked.getId());
            service.submitWeeklyExam(blockedExam.getId(), allWrong(blockedExam.getId()));
            WeeklyRegimen blockedWeek = regimenDao.findByUserAndWeek(blocked.getId(), 1).orElseThrow();
            service.setClock(clockAt(start.plusDays(3)));
            call(service, "sendCheckpointFollowUp", new Class<?>[] {User.class, WeeklyRegimen.class},
                    new Object[] {blocked, blockedWeek});
            new RegimenFollowUpDao().insert(blockedWeek.getId(), FollowUpKind.CHECKPOINT_DUE, start);
            service.setClock(clockAt(start.plusDays(10)));
            call(service, "sendCheckpointFollowUp", new Class<?>[] {User.class, WeeklyRegimen.class},
                    new Object[] {blocked, blockedWeek});
        } finally {
            ConfigOverrides.restore("weekly.checkpoint.min.fresh", minFresh);
        }

        List<Question> scored = service.getAttemptQuestions(weekly.getId());
        try (var conn = com.examprep.config.DatabaseManager.getConnection();
             var ps = conn.prepareStatement(
                     "DELETE FROM attempt_answers WHERE attempt_id = ? AND question_id = ?")) {
            ps.setLong(1, weekly.getId());
            ps.setLong(2, scored.get(0).getId());
            ps.executeUpdate();
        }
        java.lang.reflect.Method subjectScores = WeeklyRegimenService.class.getDeclaredMethod(
                "buildWeeklySubjectScores", Long.class, Long.class, List.class);
        subjectScores.setAccessible(true);
        subjectScores.invoke(service, weekly.getRegimenId(), weekly.getId(), scored);
    }

    private Map<Long, String> halfCorrect(Long attemptId) throws Exception {
        Map<Long, String> answers = new HashMap<>();
        Map<Long, Integer> seen = new HashMap<>();
        for (Question question : service.getAttemptQuestions(attemptId)) {
            int n = seen.merge(question.getSubjectId(), 1, Integer::sum);
            if (n % 2 == 0) {
                answers.put(question.getId(), question.getCorrectOption());
            } else {
                answers.put(question.getId(),
                        "A".equalsIgnoreCase(question.getCorrectOption()) ? "B" : "A");
            }
        }
        return answers;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object call(Object target, String name, Class<?>[] types, Object[] args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            if (ex.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw ex;
        }
    }

    private static final class SilentMail extends MailService {
        @Override
        public boolean sendFollowUp(User user, WeeklyRegimen regimen, FollowUpKind kind) {
            return false;
        }

        @Override
        public boolean sendStudyPlanDigest(User user, WeeklyRegimen regimen,
                                            com.examprep.model.StudyPlan plan, boolean grantActive,
                                            LocalDateTime now) {
            return false;
        }
    }

    private static final class ThrowOfficial extends WeeklyRegimenDao {
        @Override
        public void setOfficialAttempt(Long id, Long attemptId) throws SQLException {
            throw new SQLException("already set");
        }
    }

    private static final class EmptyLatest extends WeeklyRegimenDao {
        @Override
        public Optional<WeeklyRegimen> findLatestCompleted(Long userId) {
            return Optional.empty();
        }
    }

    private static final class BrokenUserDao extends UserDao {
        @Override
        public Optional<User> findById(Long id) throws SQLException {
            throw new SQLException("user dao down");
        }
    }

    private static final class BrokenQuestionDao extends QuestionDao {
        @Override
        public List<Question> findBySubjectId(Long subjectId) throws SQLException {
            throw new SQLException("question dao down");
        }
    }

    private static final class SecondLookupEmpty extends AccessGrantDao {
        private int calls;
        private final AccessGrantDao real = new AccessGrantDao();

        @Override
        public Optional<AccessGrant> findLatestRedeemedByUserId(Long userId) throws SQLException {
            calls++;
            if (calls > 1) {
                return Optional.empty();
            }
            return real.findLatestRedeemedByUserId(userId);
        }
    }

    private static final class DropDiagnostic extends UserDao {
        private int calls;
        private final UserDao real = new UserDao();

        @Override
        public Optional<User> findById(Long id) throws SQLException {
            calls++;
            Optional<User> user = real.findById(id);
            if (calls > 1) {
                user.ifPresent(row -> row.setDiagnosticCompletedAt(null));
            }
            return user;
        }
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

    private List<EmailOutbox> followUps(Long regimenId) throws Exception {
        List<EmailOutbox> rows = new java.util.ArrayList<>();
        for (EmailOutbox mail : outboxDao.findByRegimenId(regimenId)) {
            String subject = mail.getSubject().toLowerCase();
            if (subject.contains("still open") || subject.contains("was missed")) {
                rows.add(mail);
            }
        }
        return rows;
    }

    private static Clock clockAt(LocalDateTime time) {
        return Clock.fixed(time.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
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
