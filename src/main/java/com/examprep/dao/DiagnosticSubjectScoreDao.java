package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.DiagnosticSubjectScore;
import com.examprep.model.SubjectBand;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DiagnosticSubjectScoreDao {

    public void replaceForAttempt(Long attemptId, List<DiagnosticSubjectScore> scores) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement deletePs = conn.prepareStatement(
                        "DELETE FROM diagnostic_subject_scores WHERE attempt_id = ?")) {
                    deletePs.setLong(1, attemptId);
                    deletePs.executeUpdate();
                }
                String insertSql = """
                        INSERT INTO diagnostic_subject_scores (attempt_id, subject_id, score_percent, band)
                        VALUES (?, ?, ?, ?)
                        """;
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    for (DiagnosticSubjectScore score : scores) {
                        insertPs.setLong(1, attemptId);
                        insertPs.setLong(2, score.getSubjectId());
                        insertPs.setBigDecimal(3, score.getScorePercent());
                        insertPs.setString(4, score.getBand().name());
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

    public List<DiagnosticSubjectScore> findByAttemptId(Long attemptId) throws SQLException {
        String sql = """
                SELECT d.attempt_id, d.subject_id, d.score_percent, d.band, s.name AS subject_name
                FROM diagnostic_subject_scores d
                JOIN subjects s ON s.id = d.subject_id
                WHERE d.attempt_id = ?
                ORDER BY s.name
                """;
        List<DiagnosticSubjectScore> scores = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DiagnosticSubjectScore score = new DiagnosticSubjectScore();
                    score.setAttemptId(rs.getLong("attempt_id"));
                    score.setSubjectId(rs.getLong("subject_id"));
                    score.setSubjectName(rs.getString("subject_name"));
                    score.setScorePercent(rs.getBigDecimal("score_percent"));
                    score.setBand(SubjectBand.fromString(rs.getString("band")));
                    scores.add(score);
                }
            }
        }
        return scores;
    }
}
