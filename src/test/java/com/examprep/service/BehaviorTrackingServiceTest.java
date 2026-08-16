package com.examprep.service;

import com.examprep.dao.AttemptDao;
import com.examprep.dao.QuestionDao;
import com.examprep.dao.UserDao;
import com.examprep.model.AttemptBehaviorEvent;
import com.examprep.model.AttemptStatus;
import com.examprep.model.BehaviorEventType;
import com.examprep.model.ExamAttempt;
import com.examprep.model.ExamLevel;
import com.examprep.model.Question;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.support.DatabaseTestSupport;
import com.examprep.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorTrackingServiceTest extends DatabaseTestSupport {

    private final BehaviorTrackingService tracking = new BehaviorTrackingService();
    private final AttemptDao attemptDao = new AttemptDao();
    private final UserDao userDao = new UserDao();
    private final QuestionDao questionDao = new QuestionDao();
    private final ExamService examService = new ExamService();
    private final DiagnosticService diagnosticService = new DiagnosticService();

    @Test
    void unansweredHardLeaveIsSuspect() throws Exception {
        User user = student("hardleave");
        markHard(1L);
        ExamAttempt attempt = attemptDao.create(user.getId(), 1L);

        AttemptBehaviorEvent event = tracking.record(user.getId(), attempt.getId(), 1L,
                BehaviorEventType.LEAVE, 12_000).orElseThrow();

        assertTrue(event.isSuspect());
        assertFalse(event.isQuestionAnswered());
        assertEquals("HARD", event.getQuestionDifficulty());
        ExamAttempt flagged = tracking.getAttempt(attempt.getId());
        assertEquals(1, flagged.getLeaveCount());
        assertEquals(1, flagged.getSuspectLeaveCount());
        assertEquals(1, tracking.getFlaggedAttempts().size());
    }

    @Test
    void answeredHardLeaveIsNotSuspect() throws Exception {
        User user = student("answeredhard");
        markHard(1L);
        ExamAttempt attempt = attemptDao.create(user.getId(), 1L);
        examService.saveAnswer(attempt.getId(), 1L, "C");

        AttemptBehaviorEvent event = tracking.record(user.getId(), attempt.getId(), 1L,
                BehaviorEventType.LEAVE, 8_000).orElseThrow();

        assertFalse(event.isSuspect());
        assertTrue(event.isQuestionAnswered());
        ExamAttempt loaded = tracking.getAttempt(attempt.getId());
        assertEquals(1, loaded.getLeaveCount());
        assertEquals(0, loaded.getSuspectLeaveCount());
        assertTrue(tracking.getFlaggedAttempts().isEmpty());
    }

    @Test
    void unansweredEasyLeaveIsNotSuspect() throws Exception {
        User user = student("easyleave");
        ExamAttempt attempt = attemptDao.create(user.getId(), 1L);

        AttemptBehaviorEvent event = tracking.record(user.getId(), attempt.getId(), 2L,
                BehaviorEventType.LEAVE, 5_000).orElseThrow();

        assertFalse(event.isSuspect());
        assertEquals("EASY", event.getQuestionDifficulty());
        assertEquals(0, tracking.getAttempt(attempt.getId()).getSuspectLeaveCount());
    }

    @Test
    void diagnosticIntroLeaveIsIgnored() throws Exception {
        User user = student("introleave");
        ExamAttempt attempt = diagnosticService.startDiagnostic(user.getId());
        assertFalse(attemptDao.findById(attempt.getId()).orElseThrow().isIntegrityTracking());

        List<Question> questions = diagnosticService.getAttemptQuestions(attempt.getId());
        assertFalse(questions.isEmpty());

        Optional<AttemptBehaviorEvent> recorded = tracking.record(
                user.getId(), attempt.getId(), questions.get(0).getId(),
                BehaviorEventType.LEAVE, 9_000);

        assertTrue(recorded.isEmpty());
        assertTrue(tracking.getTimeline(attempt.getId()).isEmpty());
        assertEquals(0, tracking.getAttempt(attempt.getId()).getLeaveCount());
    }

    @Test
    void returnStoresAwayDurationAndDuplicateLeaveIsIgnored() throws Exception {
        User user = student("awaytime");
        ExamAttempt attempt = attemptDao.create(user.getId(), 1L);

        tracking.record(user.getId(), attempt.getId(), 2L, BehaviorEventType.LEAVE, 4_000);
        tracking.record(user.getId(), attempt.getId(), 2L, BehaviorEventType.LEAVE, 3_000);
        Thread.sleep(30);
        AttemptBehaviorEvent returned = tracking.record(user.getId(), attempt.getId(), 2L,
                BehaviorEventType.RETURN, 3_500).orElseThrow();

        assertEquals(BehaviorEventType.RETURN, returned.getEventType());
        assertTrue(returned.getAwayDurationMs() != null && returned.getAwayDurationMs() >= 0);
        List<AttemptBehaviorEvent> timeline = tracking.getTimeline(attempt.getId());
        assertEquals(2, timeline.size());
        assertEquals(1, tracking.getAttempt(attempt.getId()).getLeaveCount());
    }

    @Test
    void submitCopiesIntegritySummary() throws Exception {
        User user = student("summary");
        markHard(1L);
        ExamAttempt attempt = attemptDao.create(user.getId(), 1L);
        tracking.record(user.getId(), attempt.getId(), 1L, BehaviorEventType.LEAVE, 2_000);

        ExamAttempt completed = examService.submitExam(attempt.getId(), Map.of());
        assertEquals(AttemptStatus.COMPLETED, completed.getStatus());
        assertEquals(1, completed.getLeaveCount());
        assertEquals(1, completed.getSuspectLeaveCount());
    }

    @Test
    void rejectsOtherUsersAttempt() throws Exception {
        User owner = student("owner");
        User other = student("other");
        ExamAttempt attempt = attemptDao.create(owner.getId(), 1L);

        assertThrows(IllegalArgumentException.class, () ->
                tracking.record(other.getId(), attempt.getId(), 1L, BehaviorEventType.LEAVE, 1_000));
    }

    @Test
    void ignoresEventsAfterSubmit() throws Exception {
        User user = student("done");
        ExamAttempt attempt = attemptDao.create(user.getId(), 1L);
        examService.submitExam(attempt.getId(), Map.of());

        assertTrue(tracking.record(user.getId(), attempt.getId(), 1L,
                BehaviorEventType.LEAVE, 1_000).isEmpty());
    }

    private User student(String username) throws Exception {
        return userDao.create(username, username + "@example.com", PasswordUtil.hash("password123"),
                Role.USER, ExamLevel.PROFESSIONAL);
    }

    private void markHard(Long questionId) throws Exception {
        Question question = questionDao.findById(questionId).orElseThrow();
        question.setDifficulty("HARD");
        questionDao.update(question);
    }
}
