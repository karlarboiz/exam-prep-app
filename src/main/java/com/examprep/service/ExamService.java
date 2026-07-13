package com.examprep.service;

import com.examprep.dao.AttemptDao;
import com.examprep.dao.ExamDao;
import com.examprep.dao.QuestionDao;
import com.examprep.dao.SubjectDao;
import com.examprep.model.AttemptAnswer;
import com.examprep.model.AttemptStatus;
import com.examprep.model.Exam;
import com.examprep.model.ExamAttempt;
import com.examprep.model.Question;
import com.examprep.model.Subject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExamService {

    private final SubjectDao subjectDao = new SubjectDao();
    private final ExamDao examDao = new ExamDao();
    private final QuestionDao questionDao = new QuestionDao();
    private final AttemptDao attemptDao = new AttemptDao();

    public List<Subject> getSubjects() throws SQLException {
        return subjectDao.findAll();
    }

    public List<Exam> getActiveExams() throws SQLException {
        return examDao.findActive();
    }

    public Optional<Exam> getExam(Long examId) throws SQLException {
        return examDao.findById(examId);
    }

    public List<Question> getExamQuestions(Long examId) throws SQLException {
        return questionDao.findByExamId(examId);
    }

    public ExamAttempt startExam(Long userId, Long examId) throws SQLException {
        Optional<ExamAttempt> existing = attemptDao.findInProgress(userId, examId);
        if (existing.isPresent()) {
            ExamAttempt attempt = existing.get();
            if (isExpired(attempt)) {
                attemptDao.updateStatus(attempt.getId(), AttemptStatus.EXPIRED);
            } else {
                return attempt;
            }
        }
        return attemptDao.create(userId, examId);
    }

    public ExamAttempt getAttempt(Long attemptId) throws SQLException {
        return attemptDao.findById(attemptId).orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
    }

    public boolean isExpired(ExamAttempt attempt) {
        LocalDateTime deadline = attempt.getStartedAt().plusMinutes(attempt.getDurationMinutes());
        return LocalDateTime.now().isAfter(deadline);
    }

    public LocalDateTime getDeadline(ExamAttempt attempt) {
        return attempt.getStartedAt().plusMinutes(attempt.getDurationMinutes());
    }

    public void saveAnswer(Long attemptId, Long questionId, String selectedOption) throws SQLException {
        ExamAttempt attempt = getAttempt(attemptId);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("Attempt is not in progress");
        }
        if (isExpired(attempt)) {
            attemptDao.updateStatus(attemptId, AttemptStatus.EXPIRED);
            throw new IllegalStateException("Exam time has expired");
        }
        Question question = questionDao.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));
        boolean correct = question.getCorrectOption().equalsIgnoreCase(selectedOption);
        attemptDao.saveAnswer(attemptId, questionId, selectedOption.toUpperCase(), correct);
    }

    public ExamAttempt submitExam(Long attemptId, Map<Long, String> answers) throws SQLException {
        ExamAttempt attempt = getAttempt(attemptId);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return attempt;
        }
        AttemptStatus finalStatus = isExpired(attempt) ? AttemptStatus.EXPIRED : AttemptStatus.COMPLETED;

        List<Question> questions = questionDao.findByExamId(attempt.getExamId());
        for (Question question : questions) {
            String selected = answers.get(question.getId());
            if (selected != null && !selected.isBlank()) {
                boolean correct = question.getCorrectOption().equalsIgnoreCase(selected);
                attemptDao.saveAnswer(attemptId, question.getId(), selected.toUpperCase(), correct);
            }
        }

        BigDecimal score = calculateScore(attemptId, questions.size());
        attemptDao.completeAttempt(attemptId, score, finalStatus);
        return attemptDao.findById(attemptId).orElseThrow();
    }

    private BigDecimal calculateScore(Long attemptId, int totalQuestions) throws SQLException {
        if (totalQuestions == 0) {
            return BigDecimal.ZERO;
        }
        List<AttemptAnswer> answers = attemptDao.findAnswersByAttemptId(attemptId);
        long correct = answers.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
        return BigDecimal.valueOf(correct * 100.0 / totalQuestions).setScale(2, RoundingMode.HALF_UP);
    }

    public List<ExamAttempt> getUserHistory(Long userId) throws SQLException {
        return attemptDao.findByUserId(userId);
    }

    public List<AttemptAnswer> getAttemptAnswers(Long attemptId) throws SQLException {
        return attemptDao.findAnswersByAttemptId(attemptId);
    }

    public Map<Long, String> getAnswerMap(Long attemptId) throws SQLException {
        Map<Long, String> map = new HashMap<>();
        for (AttemptAnswer answer : attemptDao.findAnswersByAttemptId(attemptId)) {
            map.put(answer.getQuestionId(), answer.getSelectedOption());
        }
        return map;
    }
}
