package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.WeeklyRegimen;
import com.examprep.model.WeeklyRegimenStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class WeeklyRegimenDao {

    private static final String SELECT_COLUMNS = """
            SELECT id, user_id, week_number, week_start, week_end, status, is_final_week,
                   official_attempt_id, email_sent_at, created_at
            FROM weekly_regimens
            """;

    public WeeklyRegimen create(WeeklyRegimen regimen) throws SQLException {
        String sql = """
                INSERT INTO weekly_regimens (user_id, week_number, week_start, week_end, status,
                                             is_final_week, official_attempt_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, regimen.getUserId());
            ps.setInt(2, regimen.getWeekNumber());
            ps.setTimestamp(3, Timestamp.valueOf(regimen.getWeekStart()));
            ps.setTimestamp(4, Timestamp.valueOf(regimen.getWeekEnd()));
            ps.setString(5, regimen.getStatus().name());
            ps.setBoolean(6, regimen.isFinalWeek());
            if (regimen.getOfficialAttemptId() != null) {
                ps.setLong(7, regimen.getOfficialAttemptId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        }
        throw new SQLException("Failed to create weekly regimen");
    }

    public Optional<WeeklyRegimen> findById(Long id) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<WeeklyRegimen> findByUserAndWeek(Long userId, int weekNumber) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE user_id = ? AND week_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, weekNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<WeeklyRegimen> findByUserId(Long userId) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE user_id = ? ORDER BY week_number";
        List<WeeklyRegimen> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
            }
        }
        return rows;
    }

    public Optional<WeeklyRegimen> findLatestCompleted(Long userId) throws SQLException {
        String sql = SELECT_COLUMNS + """
                WHERE user_id = ? AND status = 'COMPLETED' AND official_attempt_id IS NOT NULL
                ORDER BY week_number DESC
                LIMIT 1
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void markMissed(Long id) throws SQLException {
        String sql = "UPDATE weekly_regimens SET status = 'MISSED' WHERE id = ? AND official_attempt_id IS NULL";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void setOfficialAttempt(Long id, Long attemptId) throws SQLException {
        String sql = """
                UPDATE weekly_regimens
                SET official_attempt_id = ?, status = 'COMPLETED'
                WHERE id = ? AND official_attempt_id IS NULL
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            ps.setLong(2, id);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new SQLException("Official attempt already set for regimen " + id);
            }
        }
    }

    public void markEmailSent(Long id, LocalDateTime sentAt) throws SQLException {
        String sql = "UPDATE weekly_regimens SET email_sent_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(sentAt));
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void setFormQuestions(Long regimenId, List<Long> questionIds) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement deletePs = conn.prepareStatement(
                        "DELETE FROM weekly_form_questions WHERE regimen_id = ?")) {
                    deletePs.setLong(1, regimenId);
                    deletePs.executeUpdate();
                }
                String insertSql = "INSERT INTO weekly_form_questions (regimen_id, question_id, sort_order) VALUES (?, ?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    int order = 1;
                    for (Long questionId : questionIds) {
                        insertPs.setLong(1, regimenId);
                        insertPs.setLong(2, questionId);
                        insertPs.setInt(3, order++);
                        insertPs.addBatch();
                    }
                    insertPs.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<Long> findFormQuestionIds(Long regimenId) throws SQLException {
        String sql = "SELECT question_id FROM weekly_form_questions WHERE regimen_id = ? ORDER BY sort_order";
        List<Long> ids = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, regimenId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("question_id"));
                }
            }
        }
        return ids;
    }

    public Set<Long> findSeenQuestionIds(Long userId) throws SQLException {
        String sql = """
                SELECT question_id FROM weekly_form_questions wf
                JOIN weekly_regimens wr ON wr.id = wf.regimen_id
                WHERE wr.user_id = ?
                UNION
                SELECT aq.question_id FROM attempt_questions aq
                JOIN exam_attempts a ON a.id = aq.attempt_id
                WHERE a.user_id = ?
                UNION
                SELECT aa.question_id FROM attempt_answers aa
                JOIN exam_attempts a ON a.id = aa.attempt_id
                WHERE a.user_id = ?
                """;
        Set<Long> ids = new HashSet<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
        }
        return ids;
    }

    public Set<Long> findFormQuestionIdsForWeek(Long userId, int weekNumber) throws SQLException {
        String sql = """
                SELECT wf.question_id
                FROM weekly_form_questions wf
                JOIN weekly_regimens wr ON wr.id = wf.regimen_id
                WHERE wr.user_id = ? AND wr.week_number = ?
                """;
        Set<Long> ids = new HashSet<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, weekNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
        }
        return ids;
    }

    private WeeklyRegimen mapRow(ResultSet rs) throws SQLException {
        WeeklyRegimen regimen = new WeeklyRegimen();
        regimen.setId(rs.getLong("id"));
        regimen.setUserId(rs.getLong("user_id"));
        regimen.setWeekNumber(rs.getInt("week_number"));
        Timestamp start = rs.getTimestamp("week_start");
        if (start != null) {
            regimen.setWeekStart(start.toLocalDateTime());
        }
        Timestamp end = rs.getTimestamp("week_end");
        if (end != null) {
            regimen.setWeekEnd(end.toLocalDateTime());
        }
        regimen.setStatus(WeeklyRegimenStatus.fromString(rs.getString("status")));
        regimen.setFinalWeek(rs.getBoolean("is_final_week"));
        long officialId = rs.getLong("official_attempt_id");
        if (!rs.wasNull()) {
            regimen.setOfficialAttemptId(officialId);
        }
        Timestamp emailed = rs.getTimestamp("email_sent_at");
        if (emailed != null) {
            regimen.setEmailSentAt(emailed.toLocalDateTime());
        }
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            regimen.setCreatedAt(created.toLocalDateTime());
        }
        return regimen;
    }
}
