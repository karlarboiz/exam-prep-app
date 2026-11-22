package com.examprep.service;

import com.examprep.dao.AttemptDao;
import com.examprep.dao.ExamDao;
import com.examprep.dao.QuestionDao;
import com.examprep.dao.UserDao;
import com.examprep.model.AttemptStatus;
import com.examprep.model.Exam;
import com.examprep.model.ExamAttempt;
import com.examprep.model.ExamLevel;
import com.examprep.model.Question;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.support.DatabaseTestSupport;
import com.examprep.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamServiceTest extends DatabaseTestSupport {

    private final ExamService examService = new ExamService();
    private final UserDao userDao = new UserDao();
    private final AttemptDao attemptDao = new AttemptDao();
    private final ExamDao examDao = new ExamDao();
    private final QuestionDao questionDao = new QuestionDao();

    @Test
    void listsSubjectsAndExamsByLevel() throws Exception {
        assertFalse(examService.getSubjects().isEmpty());
        assertFalse(examService.getSubjects(ExamLevel.PROFESSIONAL).isEmpty());
        assertFalse(examService.getSubjects(ExamLevel.SUB_PROFESSIONAL).isEmpty());
        assertFalse(examService.getActiveExams().isEmpty());
        assertFalse(examService.getActiveExams(ExamLevel.PROFESSIONAL).isEmpty());
        assertTrue(examService.getExam(1L).isPresent());
        assertEquals(5, examService.getExamQuestions(1L).size());
    }

    @Test
    void availabilityDependsOnExamKindAndSubjectTrack() throws Exception {
        Exam practice = examService.getExam(1L).orElseThrow();
        assertTrue(examService.isExamAvailableForLevel(practice, null));
        assertTrue(examService.isExamAvailableForLevel(practice, ExamLevel.PROFESSIONAL));
        assertTrue(examService.isExamAvailableForLevel(practice, ExamLevel.SUB_PROFESSIONAL));
        assertFalse(examService.isExamAvailableForLevel(null, ExamLevel.PROFESSIONAL));

        Exam diagnostic = examService.getExam(2L).orElseThrow();
        assertFalse(examService.isExamAvailableForLevel(diagnostic, null));
        Exam weekly = examService.getExam(3L).orElseThrow();
        assertFalse(examService.isExamAvailableForLevel(weekly, ExamLevel.PROFESSIONAL));

        practice.setActive(false);
        assertFalse(examService.isExamAvailableForLevel(practice, null));
        practice.setActive(true);
        practice.setSubjectId(999_999L);
        assertFalse(examService.isExamAvailableForLevel(practice, ExamLevel.PROFESSIONAL));

        com.examprep.model.Subject subject = new com.examprep.dao.SubjectDao()
                .create("Sub only", null, false, true);
        Exam subOnly = new Exam();
        subOnly.setSubjectId(subject.getId());
        subOnly.setTitle("Sub practice");
        subOnly.setDurationMinutes(10);
        subOnly.setActive(true);
        Exam saved = examDao.create(subOnly);
        assertFalse(examService.isExamAvailableForLevel(saved, ExamLevel.PROFESSIONAL));
        assertTrue(examService.isExamAvailableForLevel(saved, ExamLevel.SUB_PROFESSIONAL));
    }

    @Test
    void startResumeSaveAndSubmitScoresTheAttempt() throws Exception {
        User user = student("pat");
        ExamAttempt started = examService.startExam(user.getId(), 1L);
        ExamAttempt resumed = examService.startExam(user.getId(), 1L);
        assertEquals(started.getId(), resumed.getId());
        assertFalse(examService.isExpired(started));
        assertTrue(examService.getDeadline(started).isAfter(started.getStartedAt()));

        examService.saveAnswer(started.getId(), 1L, "c");
        assertEquals("C", examService.getAnswerMap(started.getId()).get(1L));
        assertEquals(1, examService.getAttemptAnswers(started.getId()).size());

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "C");
        answers.put(2L, "");
        ExamAttempt completed = examService.submitExam(started.getId(), answers);
        assertEquals(AttemptStatus.COMPLETED, completed.getStatus());
        assertEquals(completed.getId(), examService.submitExam(started.getId(), Map.of()).getId());
        assertEquals(1, examService.getUserHistory(user.getId()).size());

        assertThrows(IllegalStateException.class, () -> examService.saveAnswer(started.getId(), 1L, "A"));
        assertThrows(IllegalArgumentException.class, () -> examService.getAttempt(999_999L));
    }

    @Test
    void expiredAttemptIsClosedAndMissingQuestionIsRejected() throws Exception {
        User user = student("late");
        ExamAttempt attempt = examService.startExam(user.getId(), 1L);
        attemptDao.updateStartedAt(attempt.getId(), LocalDateTime.now().minusHours(2));
        ExamAttempt reloaded = examService.getAttempt(attempt.getId());
        assertTrue(examService.isExpired(reloaded));

        ExamAttempt restarted = examService.startExam(user.getId(), 1L);
        assertEquals(AttemptStatus.EXPIRED, examService.getAttempt(attempt.getId()).getStatus());
        assertEquals(AttemptStatus.IN_PROGRESS, restarted.getStatus());

        attemptDao.updateStartedAt(restarted.getId(), LocalDateTime.now().minusHours(2));
        assertThrows(IllegalStateException.class, () -> examService.saveAnswer(restarted.getId(), 1L, "A"));
        assertEquals(AttemptStatus.EXPIRED, examService.getAttempt(restarted.getId()).getStatus());

        ExamAttempt fresh = examService.startExam(user.getId(), 1L);
        assertThrows(IllegalArgumentException.class, () -> examService.saveAnswer(fresh.getId(), 999_999L, "A"));

        Exam empty = new Exam();
        empty.setSubjectId(1L);
        empty.setTitle("Empty");
        empty.setDurationMinutes(5);
        empty.setActive(true);
        Exam saved = examDao.create(empty);
        ExamAttempt blank = examService.startExam(user.getId(), saved.getId());
        ExamAttempt scored = examService.submitExam(blank.getId(), Map.of());
        assertEquals(0, scored.getScorePercent().signum());
    }

    @Test
    void submitMarksExpiredWhenTheClockHasRunOut() throws Exception {
        User user = student("clock");
        ExamAttempt attempt = examService.startExam(user.getId(), 1L);
        attemptDao.updateStartedAt(attempt.getId(), LocalDateTime.now().minusHours(2));
        ExamAttempt submitted = examService.submitExam(attempt.getId(), Map.of(1L, "C", 2L, "B"));
        assertEquals(AttemptStatus.EXPIRED, submitted.getStatus());
        assertTrue(submitted.getScorePercent().signum() > 0);
    }

    private User student(String username) throws Exception {
        return userDao.create(username, username + "@example.com", PasswordUtil.hash("password123"),
                Role.USER, ExamLevel.PROFESSIONAL);
    }
}
