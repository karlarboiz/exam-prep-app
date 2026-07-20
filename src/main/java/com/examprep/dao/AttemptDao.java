package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.AttemptAnswer;
import com.examprep.model.AttemptStatus;
import com.examprep.model.ExamAttempt;
import com.examprep.model.Question;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttemptDao {

    public Optional<ExamAttempt> findById(Long id) throws SQLException {
        String sql = """
                SELECT a.id, a.user_id, a.exam_id, a.started_at, a.completed_at, a.score_percent, a.status,
                       e.title AS exam_title, e.duration_minutes, s.name AS subject_name
                FROM exam_attempts a
                JOIN exams e ON e.id = a.exam_id
                JOIN subjects s ON s.id = e.subject_id
                WHERE a.id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapAttempt(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<ExamAttempt> findInProgress(Long userId, Long examId) throws SQLException {
        String sql = """
                SELECT a.id, a.user_id, a.exam_id, a.started_at, a.completed_at, a.score_percent, a.status,
                       e.title AS exam_title, e.duration_minutes, s.name AS subject_name
                FROM exam_attempts a
                JOIN exams e ON e.id = a.exam_id
                JOIN subjects s ON s.id = e.subject_id
                WHERE a.user_id = ? AND a.exam_id = ? AND a.status = 'IN_PROGRESS'
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, examId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapAttempt(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<ExamAttempt> findByUserId(Long userId) throws SQLException {
        String sql = """
                SELECT a.id, a.user_id, a.exam_id, a.started_at, a.completed_at, a.score_percent, a.status,
                       e.title AS exam_title, e.duration_minutes, s.name AS subject_name
                FROM exam_attempts a
                JOIN exams e ON e.id = a.exam_id
                JOIN subjects s ON s.id = e.subject_id
                WHERE a.user_id = ?
                ORDER BY a.started_at DESC
                """;
        List<ExamAttempt> attempts = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    attempts.add(mapAttempt(rs));
                }
            }
        }
        return attempts;
    }

    public ExamAttempt create(Long userId, Long examId) throws SQLException {
        String sql = "INSERT INTO exam_attempts (user_id, exam_id, started_at, status) VALUES (?, ?, ?, 'IN_PROGRESS')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setLong(2, examId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        }
        throw new SQLException("Failed to create attempt");
    }

    public void saveAnswer(Long attemptId, Long questionId, String selectedOption, boolean isCorrect) throws SQLException {
        String sql = """
                MERGE INTO attempt_answers (attempt_id, question_id, selected_option, is_correct)
                KEY (attempt_id, question_id)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            ps.setLong(2, questionId);
            ps.setString(3, selectedOption);
            ps.setBoolean(4, isCorrect);
            ps.executeUpdate();
        }
    }

    public List<AttemptAnswer> findAnswersByAttemptId(Long attemptId) throws SQLException {
        String sql = """
                SELECT aa.attempt_id, aa.question_id, aa.selected_option, aa.is_correct,
                       q.subject_id, q.prompt, q.option_a, q.option_b, q.option_c, q.option_d,
                       q.correct_option, q.difficulty, q.explanation
                FROM attempt_answers aa
                JOIN questions q ON q.id = aa.question_id
                WHERE aa.attempt_id = ?
                ORDER BY aa.question_id
                """;
        List<AttemptAnswer> answers = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapAnswer(rs));
                }
            }
        }
        return answers;
    }

    public void completeAttempt(Long attemptId, BigDecimal scorePercent, AttemptStatus status) throws SQLException {
        String sql = "UPDATE exam_attempts SET completed_at = ?, score_percent = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setBigDecimal(2, scorePercent);
            ps.setString(3, status.name());
            ps.setLong(4, attemptId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(Long attemptId, AttemptStatus status) throws SQLException {
        String sql = "UPDATE exam_attempts SET status = ?, completed_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, attemptId);
            ps.executeUpdate();
        }
    }

    private ExamAttempt mapAttempt(ResultSet rs) throws SQLException {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId(rs.getLong("id"));
        attempt.setUserId(rs.getLong("user_id"));
        attempt.setExamId(rs.getLong("exam_id"));
        Timestamp startedAt = rs.getTimestamp("started_at");
        if (startedAt != null) {
            attempt.setStartedAt(startedAt.toLocalDateTime());
        }
        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) {
            attempt.setCompletedAt(completedAt.toLocalDateTime());
        }
        BigDecimal score = rs.getBigDecimal("score_percent");
        attempt.setScorePercent(score);
        attempt.setStatus(AttemptStatus.fromString(rs.getString("status")));
        attempt.setExamTitle(rs.getString("exam_title"));
        attempt.setSubjectName(rs.getString("subject_name"));
        attempt.setDurationMinutes(rs.getInt("duration_minutes"));
        return attempt;
    }

    private AttemptAnswer mapAnswer(ResultSet rs) throws SQLException {
        AttemptAnswer answer = new AttemptAnswer();
        answer.setAttemptId(rs.getLong("attempt_id"));
        answer.setQuestionId(rs.getLong("question_id"));
        answer.setSelectedOption(rs.getString("selected_option"));
        answer.setCorrect(rs.getBoolean("is_correct"));

        Question question = new Question();
        question.setId(rs.getLong("question_id"));
        question.setSubjectId(rs.getLong("subject_id"));
        question.setPrompt(rs.getString("prompt"));
        question.setOptionA(rs.getString("option_a"));
        question.setOptionB(rs.getString("option_b"));
        question.setOptionC(rs.getString("option_c"));
        question.setOptionD(rs.getString("option_d"));
        question.setCorrectOption(rs.getString("correct_option"));
        question.setDifficulty(rs.getString("difficulty"));
        question.setExplanation(rs.getString("explanation"));
        answer.setQuestion(question);
        return answer;
    }
}
