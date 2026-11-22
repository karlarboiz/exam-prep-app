package com.examprep.service;

import com.examprep.dao.AttemptDao;
import com.examprep.dao.ExamDao;
import com.examprep.dao.QuestionDao;
import com.examprep.dao.SubjectDao;
import com.examprep.dao.UserDao;
import com.examprep.model.AttemptStatus;
import com.examprep.model.DiagnosticResult;
import com.examprep.model.DiagnosticSubjectScore;
import com.examprep.model.Exam;
import com.examprep.model.ExamAttempt;
import com.examprep.model.ExamLevel;
import com.examprep.model.Question;
import com.examprep.model.ReadinessLabel;
import com.examprep.model.Role;
import com.examprep.model.SubjectBand;
import com.examprep.model.User;
import com.examprep.support.DatabaseTestSupport;
import com.examprep.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticServiceTest extends DatabaseTestSupport {

    private final DiagnosticService diagnosticService = new DiagnosticService();
    private final UserDao userDao = new UserDao();
    private final AttemptDao attemptDao = new AttemptDao();
    private final ExamDao examDao = new ExamDao();
    private final QuestionDao questionDao = new QuestionDao();
    private final SubjectDao subjectDao = new SubjectDao();

    @Test
    void completedDiagnosticStoresBandsAndReadiness() throws Exception {
        User user = student("pat", ExamLevel.PROFESSIONAL);
        assertFalse(diagnosticService.isDiagnosticCompleted(user.getId()));

        ExamAttempt started = diagnosticService.startDiagnostic(user.getId());
        assertEquals(started.getId(), diagnosticService.startDiagnostic(user.getId()).getId());
        assertFalse(diagnosticService.getAttemptQuestions(started.getId()).isEmpty());
        LocalDateTime deadline = diagnosticService.beginDiagnostic(started.getId());
        assertTrue(deadline.isAfter(LocalDateTime.now().minusMinutes(1)));
        diagnosticService.beginDiagnostic(started.getId());

        List<Question> questions = diagnosticService.getAttemptQuestions(started.getId());
        diagnosticService.saveAnswer(started.getId(), questions.get(0).getId(),
                questions.get(0).getCorrectOption().toLowerCase());
        assertEquals(diagnosticService.getDeadline(diagnosticService.getAttempt(started.getId())),
                diagnosticService.beginDiagnostic(started.getId()));
        Map<Long, String> answers = new HashMap<>();
        for (Question question : questions) {
            answers.put(question.getId(), question.getCorrectOption());
        }
        answers.put(questions.get(0).getId(), " ");
        ExamAttempt completed = diagnosticService.submitDiagnostic(started.getId(), answers);
        assertEquals(AttemptStatus.COMPLETED, completed.getStatus());
        assertEquals(completed.getId(), diagnosticService.submitDiagnostic(started.getId(), Map.of()).getId());
        assertTrue(diagnosticService.isDiagnosticCompleted(user.getId()));

        DiagnosticResult result = diagnosticService.getResult(started.getId());
        assertFalse(result.getSubjectScores().isEmpty());
        assertFalse(result.getAnswers().isEmpty());
        assertTrue(diagnosticService.getAnswerMap(started.getId()).size() >= 1);
        assertThrows(IllegalStateException.class, () ->
                diagnosticService.saveAnswer(started.getId(), questions.get(0).getId(), "A"));
        assertThrows(IllegalStateException.class, () -> diagnosticService.beginDiagnostic(started.getId()));
    }

    @Test
    void readinessLabelsFollowMeanAndWeakCount() {
        assertEquals(SubjectBand.STRONG, DiagnosticService.bandForPercent(new BigDecimal("75")));
        assertEquals(SubjectBand.DEVELOPING, DiagnosticService.bandForPercent(new BigDecimal("50")));
        assertEquals(SubjectBand.WEAK, DiagnosticService.bandForPercent(new BigDecimal("49.99")));
        assertEquals(ReadinessLabel.NEEDS_FOUNDATION, DiagnosticService.computeReadiness(null));
        assertEquals(ReadinessLabel.NEEDS_FOUNDATION, DiagnosticService.computeReadiness(List.of()));
        assertEquals(new BigDecimal("0.00"), DiagnosticService.meanSubjectPercent(null));

        assertEquals(ReadinessLabel.NEEDS_FOUNDATION, DiagnosticService.computeReadiness(List.of(
                score("40.00", SubjectBand.WEAK))));
        assertEquals(ReadinessLabel.NEEDS_FOUNDATION, DiagnosticService.computeReadiness(List.of(
                score("80.00", SubjectBand.WEAK), score("80.00", SubjectBand.WEAK), score("90.00", SubjectBand.STRONG))));
        assertEquals(ReadinessLabel.EXAM_READY, DiagnosticService.computeReadiness(List.of(
                score("80.00", SubjectBand.STRONG), score("90.00", SubjectBand.STRONG))));
        assertEquals(ReadinessLabel.NEAR_READY, DiagnosticService.computeReadiness(List.of(
                score("90.00", SubjectBand.STRONG), score("90.00", SubjectBand.WEAK))));
        assertEquals(ReadinessLabel.BUILDING, DiagnosticService.computeReadiness(List.of(
                score("60.00", SubjectBand.DEVELOPING))));
    }

    @Test
    void expiredIntroClosesTheAttemptAndAFreshOneCanStart() throws Exception {
        User user = student("late", ExamLevel.SUB_PROFESSIONAL);
        ExamAttempt first = diagnosticService.startDiagnostic(user.getId());
        attemptDao.updateStartedAt(first.getId(), LocalDateTime.now().minusHours(2));
        ExamAttempt second = diagnosticService.startDiagnostic(user.getId());
        assertEquals(AttemptStatus.EXPIRED, diagnosticService.getAttempt(first.getId()).getStatus());
        assertEquals(AttemptStatus.IN_PROGRESS, second.getStatus());
        assertFalse(diagnosticService.isDiagnosticCompleted(user.getId()));

        attemptDao.updateStartedAt(second.getId(), LocalDateTime.now().minusHours(2));
        assertThrows(IllegalStateException.class, () ->
                diagnosticService.saveAnswer(second.getId(),
                        diagnosticService.getAttemptQuestions(second.getId()).get(0).getId(), "A"));
        assertThrows(IllegalStateException.class, () -> diagnosticService.beginDiagnostic(second.getId()));
        assertThrows(IllegalStateException.class, () ->
                diagnosticService.saveAnswer(second.getId(), 1L, "A"));
    }

    @Test
    void missingExamEmptyBankAndOddDifficultiesAreHandled() throws Exception {
        User user = student("empty", null);
        for (Question question : questionDao.findAll()) {
            if ("EASY".equals(question.getDifficulty())) {
                question.setDifficulty("ODD");
            } else if ("MEDIUM".equals(question.getDifficulty())) {
                question.setDifficulty("hard");
            }
            questionDao.update(question);
        }
        Exam diagnostic = examDao.findById(2L).orElseThrow();
        diagnostic.setQuestionsPerSubject(null);
        examDao.update(diagnostic);
        subjectDao.create("Empty track", null, true, true);
        ExamAttempt sampled = diagnosticService.startDiagnostic(user.getId());
        assertFalse(diagnosticService.getAttemptQuestions(sampled.getId()).isEmpty());

        subjectDao.update(1L, "General Knowledge", "x", false, false);
        subjectDao.update(2L, "Numerical Ability", "x", false, false);
        User professional = student("pro", ExamLevel.PROFESSIONAL);
        for (Exam exam : examDao.findAll()) {
            if (exam.isDiagnostic() && exam.isActive()) {
                exam.setActive(false);
                examDao.update(exam);
            }
        }
        assertThrows(IllegalStateException.class, () -> diagnosticService.startDiagnostic(professional.getId()));

        Exam replacement = new Exam();
        replacement.setSubjectId(1L);
        replacement.setTitle("Empty diagnostic");
        replacement.setDurationMinutes(10);
        replacement.setActive(true);
        replacement.setDiagnostic(true);
        replacement.setQuestionsPerSubject(5);
        examDao.create(replacement);
        assertThrows(IllegalStateException.class, () -> diagnosticService.startDiagnostic(professional.getId()));
        assertThrows(IllegalArgumentException.class, () -> diagnosticService.getAttempt(999_999L));
        assertThrows(IllegalArgumentException.class, () ->
                diagnosticService.saveAnswer(sampled.getId(), 999_999L, "A"));
    }

    @Test
    void submitWithNoAnswersScoresZeroWhenTheFormIsEmpty() throws Exception {
        User user = student("zero", ExamLevel.PROFESSIONAL);
        ExamAttempt attempt = diagnosticService.startDiagnostic(user.getId());
        new com.examprep.dao.QuestionDao();
        try (var conn = com.examprep.config.DatabaseManager.getConnection();
             var ps = conn.prepareStatement("DELETE FROM attempt_questions WHERE attempt_id = ?")) {
            ps.setLong(1, attempt.getId());
            ps.executeUpdate();
        }
        ExamAttempt submitted = diagnosticService.submitDiagnostic(attempt.getId(), Map.of());
        assertEquals(0, submitted.getScorePercent().signum());
        DiagnosticResult result = diagnosticService.getResult(attempt.getId());
        assertEquals(ReadinessLabel.NEEDS_FOUNDATION, result.getReadiness());
    }

    @Test
    void scoresCountAMissingAnswerAndAWrongAnswer() throws Exception {
        User user = student("mix", ExamLevel.PROFESSIONAL);
        ExamAttempt attempt = diagnosticService.startDiagnostic(user.getId());
        diagnosticService.beginDiagnostic(attempt.getId());
        List<Question> questions = diagnosticService.getAttemptQuestions(attempt.getId());
        Map<Long, String> answers = new HashMap<>();
        for (int i = 1; i < questions.size(); i++) {
            Question question = questions.get(i);
            String selected = i == 1
                    ? ("A".equalsIgnoreCase(question.getCorrectOption()) ? "B" : "A")
                    : question.getCorrectOption();
            answers.put(question.getId(), selected);
        }
        diagnosticService.submitDiagnostic(attempt.getId(), answers);
        assertFalse(diagnosticService.getResult(attempt.getId()).getSubjectScores().isEmpty());

        Method mix = DiagnosticService.class.getDeclaredMethod("sampleWithDifficultyMix", List.class, int.class);
        mix.setAccessible(true);
        Question easy = new Question();
        easy.setId(1L);
        easy.setDifficulty("EASY");
        Question alsoEasy = new Question();
        alsoEasy.setId(2L);
        alsoEasy.setDifficulty("EASY");
        Question unlabeled = new Question();
        unlabeled.setId(3L);
        unlabeled.setDifficulty(null);
        @SuppressWarnings("unchecked")
        List<Long> picked = (List<Long>) mix.invoke(diagnosticService, List.of(easy, alsoEasy, unlabeled), 1);
        assertEquals(1, picked.size());

        Question odd = new Question();
        odd.setId(4L);
        odd.setDifficulty("ODD");
        Question oddToo = new Question();
        oddToo.setId(5L);
        oddToo.setDifficulty("ODD");
        @SuppressWarnings("unchecked")
        List<Long> others = (List<Long>) mix.invoke(diagnosticService, List.of(odd, oddToo), 2);
        assertEquals(2, others.size());

        User late = student("clock", ExamLevel.PROFESSIONAL);
        ExamAttempt running = diagnosticService.startDiagnostic(late.getId());
        attemptDao.updateStartedAt(running.getId(), LocalDateTime.now().minusHours(2));
        assertThrows(IllegalStateException.class, () -> diagnosticService.beginDiagnostic(running.getId()));

        Exam diagnostic = examDao.findActiveDiagnostic().orElseThrow();
        diagnostic.setQuestionsPerSubject(0);
        examDao.update(diagnostic);
        assertFalse(diagnosticService.getAttemptQuestions(
                diagnosticService.startDiagnostic(student("recount", ExamLevel.PROFESSIONAL).getId()).getId()).isEmpty());
    }

    private User student(String username, ExamLevel level) throws Exception {
        return userDao.create(username, username + "@example.com", PasswordUtil.hash("password123"),
                Role.USER, level);
    }

    private static DiagnosticSubjectScore score(String percent, SubjectBand band) {
        DiagnosticSubjectScore score = new DiagnosticSubjectScore();
        score.setScorePercent(new BigDecimal(percent));
        score.setBand(band);
        return score;
    }
}
