package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.AttemptAnswer;
import com.examprep.model.AttemptKind;
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
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttemptDao {

    private static final String ATTEMPT_SELECT = """
            SELECT a.id, a.user_id, a.exam_id, a.started_at, a.completed_at, a.score_percent, a.status,
                   a.leave_count, a.suspect_leave_count, a.integrity_tracking,
                   a.attempt_kind, a.regimen_id,
                   e.title AS exam_title,
                   COALESCE(a.duration_minutes_override, e.duration_minutes) AS duration_minutes,
                   e.is_diagnostic, e.is_weekly, s.name AS subject_name,
                   u.username
            FROM exam_attempts a
            JOIN exams e ON e.id = a.exam_id
            JOIN subjects s ON s.id = e.subject_id
            JOIN users u ON u.id = a.user_id
            """;

    public Optional<ExamAttempt> findById(Long id) throws SQLException {
        String sql = ATTEMPT_SELECT + " WHERE a.id = ?";
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
        String sql = ATTEMPT_SELECT + """
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
        String sql = ATTEMPT_SELECT + """
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
        return create(userId, examId, AttemptKind.PRACTICE, null, null);
    }

    public ExamAttempt create(Long userId, Long examId, AttemptKind kind, Long regimenId,
                              Integer durationOverrideMinutes) throws SQLException {
        String sql = """
                INSERT INTO exam_attempts (user_id, exam_id, started_at, status, attempt_kind, regimen_id,
                                           duration_minutes_override)
                VALUES (?, ?, ?, 'IN_PROGRESS', ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setLong(2, examId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, kind != null ? kind.name() : AttemptKind.PRACTICE.name());
            if (regimenId != null) {
                ps.setLong(5, regimenId);
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            if (durationOverrideMinutes != null) {
                ps.setInt(6, durationOverrideMinutes);
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        }
        throw new SQLException("Failed to create attempt");
    }

    public Optional<ExamAttempt> findInProgressByRegimen(Long userId, Long regimenId, AttemptKind kind)
            throws SQLException {
        String sql = ATTEMPT_SELECT + """
                WHERE a.user_id = ? AND a.regimen_id = ? AND a.attempt_kind = ? AND a.status = 'IN_PROGRESS'
                ORDER BY a.started_at DESC
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, regimenId);
            ps.setString(3, kind.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapAttempt(rs));
                }
            }
        }
        return Optional.empty();
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
            if (selectedOption == null) {
                ps.setNull(3, Types.CHAR);
            } else {
                ps.setString(3, selectedOption);
            }
            ps.setBoolean(4, isCorrect);
            ps.executeUpdate();
        }
    }

    public List<AttemptAnswer> findAnswersByAttemptId(Long attemptId) throws SQLException {
        String sql = """
                SELECT aa.attempt_id, aa.question_id, aa.selected_option, aa.is_correct,
                       q.subject_id, q.prompt, q.option_a, q.option_b, q.option_c, q.option_d,
                       q.correct_option, q.difficulty, q.explanation, q.image_url, s.name AS subject_name
                FROM attempt_answers aa
                JOIN questions q ON q.id = aa.question_id
                JOIN subjects s ON s.id = q.subject_id
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

    public boolean hasSelectedAnswer(Long attemptId, Long questionId) throws SQLException {
        String sql = """
                SELECT selected_option FROM attempt_answers
                WHERE attempt_id = ? AND question_id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            ps.setLong(2, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String selected = rs.getString("selected_option");
                    return selected != null && !selected.isBlank();
                }
            }
        }
        return false;
    }

    public void setIntegrityTracking(Long attemptId, boolean enabled) throws SQLException {
        String sql = "UPDATE exam_attempts SET integrity_tracking = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setLong(2, attemptId);
            ps.executeUpdate();
        }
    }

    public void updateIntegrityCounts(Long attemptId, int leaveCount, int suspectLeaveCount) throws SQLException {
        String sql = "UPDATE exam_attempts SET leave_count = ?, suspect_leave_count = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaveCount);
            ps.setInt(2, suspectLeaveCount);
            ps.setLong(3, attemptId);
            ps.executeUpdate();
        }
    }

    public List<ExamAttempt> findFlagged() throws SQLException {
        String sql = ATTEMPT_SELECT + """
                WHERE a.suspect_leave_count > 0
                ORDER BY a.completed_at DESC NULLS LAST, a.started_at DESC
                """;
        List<ExamAttempt> attempts = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                attempts.add(mapAttempt(rs));
            }
        }
        return attempts;
    }

    public void updateStartedAt(Long attemptId, LocalDateTime startedAt) throws SQLException {
        String sql = "UPDATE exam_attempts SET started_at = ? WHERE id = ? AND status = 'IN_PROGRESS'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(startedAt));
            ps.setLong(2, attemptId);
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
        attempt.setDiagnostic(rs.getBoolean("is_diagnostic"));
        attempt.setWeekly(rs.getBoolean("is_weekly"));
        attempt.setAttemptKind(AttemptKind.fromString(rs.getString("attempt_kind")));
        long regimenId = rs.getLong("regimen_id");
        if (!rs.wasNull()) {
            attempt.setRegimenId(regimenId);
        }
        attempt.setLeaveCount(rs.getInt("leave_count"));
        attempt.setSuspectLeaveCount(rs.getInt("suspect_leave_count"));
        attempt.setIntegrityTracking(rs.getBoolean("integrity_tracking"));
        attempt.setUsername(rs.getString("username"));
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
        question.setImageUrl(rs.getString("image_url"));
        question.setSubjectName(rs.getString("subject_name"));
        answer.setQuestion(question);
        return answer;
    }
}
