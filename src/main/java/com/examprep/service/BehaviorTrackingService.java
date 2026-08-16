package com.examprep.service;

import com.examprep.dao.AttemptDao;
import com.examprep.dao.BehaviorEventDao;
import com.examprep.dao.QuestionDao;
import com.examprep.model.AttemptBehaviorEvent;
import com.examprep.model.AttemptStatus;
import com.examprep.model.BehaviorEventType;
import com.examprep.model.ExamAttempt;
import com.examprep.model.Question;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class BehaviorTrackingService {

    private final AttemptDao attemptDao = new AttemptDao();
    private final BehaviorEventDao eventDao = new BehaviorEventDao();
    private final QuestionDao questionDao = new QuestionDao();

    public void enableTracking(Long attemptId) throws SQLException {
        attemptDao.setIntegrityTracking(attemptId, true);
    }

    public void disableTracking(Long attemptId) throws SQLException {
        attemptDao.setIntegrityTracking(attemptId, false);
    }

    public Optional<AttemptBehaviorEvent> record(Long userId, Long attemptId, Long questionId,
                                                 BehaviorEventType type, Integer remainingQuestionMs)
            throws SQLException {
        ExamAttempt attempt = attemptDao.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
        if (!attempt.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not the attempt owner");
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return Optional.empty();
        }
        if (!attempt.isIntegrityTracking()) {
            return Optional.empty();
        }
        if (!isQuestionOnAttempt(attempt, questionId)) {
            throw new IllegalArgumentException("Question is not on this attempt");
        }

        Optional<AttemptBehaviorEvent> last = eventDao.findLastByAttemptId(attemptId);
        if (type == BehaviorEventType.LEAVE && last.isPresent() && last.get().getEventType() == BehaviorEventType.LEAVE) {
            return last;
        }
        if (type == BehaviorEventType.RETURN
                && (last.isEmpty() || last.get().getEventType() == BehaviorEventType.RETURN)) {
            return Optional.empty();
        }

        Question question = questionDao.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));
        boolean answered = attemptDao.hasSelectedAnswer(attemptId, questionId);
        boolean suspect = type == BehaviorEventType.LEAVE
                && BehaviorIntegrity.isSuspectLeave(question.getDifficulty(), answered);

        Integer awayMs = null;
        if (type == BehaviorEventType.RETURN && last.isPresent()) {
            long duration = Duration.between(last.get().getOccurredAt(), LocalDateTime.now()).toMillis();
            awayMs = (int) Math.min(Math.max(duration, 0), Integer.MAX_VALUE);
        }

        AttemptBehaviorEvent event = new AttemptBehaviorEvent();
        event.setAttemptId(attemptId);
        event.setQuestionId(questionId);
        event.setEventType(type);
        event.setOccurredAt(LocalDateTime.now());
        event.setQuestionAnswered(answered);
        event.setRemainingQuestionMs(sanitizeMs(remainingQuestionMs));
        event.setAwayDurationMs(awayMs);
        event.setSuspect(suspect);
        event.setQuestionDifficulty(question.getDifficulty());
        eventDao.insert(event);
        refreshSummary(attemptId);
        return Optional.of(event);
    }

    public boolean acknowledgeReturnIfAway(Long userId, Long attemptId) throws SQLException {
        Optional<AttemptBehaviorEvent> last = eventDao.findLastByAttemptId(attemptId);
        if (last.isEmpty() || last.get().getEventType() != BehaviorEventType.LEAVE) {
            return false;
        }
        return record(userId, attemptId, last.get().getQuestionId(),
                BehaviorEventType.RETURN, last.get().getRemainingQuestionMs()).isPresent();
    }

    public void refreshSummary(Long attemptId) throws SQLException {
        int[] counts = eventDao.countLeaves(attemptId);
        attemptDao.updateIntegrityCounts(attemptId, counts[0], counts[1]);
    }

    public List<ExamAttempt> getFlaggedAttempts() throws SQLException {
        return attemptDao.findFlagged();
    }

    public List<AttemptBehaviorEvent> getTimeline(Long attemptId) throws SQLException {
        return eventDao.findByAttemptId(attemptId);
    }

    public ExamAttempt getAttempt(Long attemptId) throws SQLException {
        return attemptDao.findById(attemptId).orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
    }

    private boolean isQuestionOnAttempt(ExamAttempt attempt, Long questionId) throws SQLException {
        List<Question> questions = attempt.isDiagnostic()
                ? questionDao.findByAttemptId(attempt.getId())
                : questionDao.findByExamId(attempt.getExamId());
        return questions.stream().anyMatch(q -> q.getId().equals(questionId));
    }

    private static Integer sanitizeMs(Integer remainingQuestionMs) {
        if (remainingQuestionMs == null) {
            return null;
        }
        return Math.max(0, remainingQuestionMs);
    }
}
