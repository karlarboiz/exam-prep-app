package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.SubjectBand;
import com.examprep.model.WeeklySubjectScore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WeeklySubjectScoreDao {

    public void replaceForRegimen(Long regimenId, List<WeeklySubjectScore> scores) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement deletePs = conn.prepareStatement(
                        "DELETE FROM weekly_subject_scores WHERE regimen_id = ?")) {
                    deletePs.setLong(1, regimenId);
                    deletePs.executeUpdate();
                }
                String insertSql = """
                        INSERT INTO weekly_subject_scores (regimen_id, subject_id, score_percent, band)
                        VALUES (?, ?, ?, ?)
                        """;
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    for (WeeklySubjectScore score : scores) {
                        insertPs.setLong(1, regimenId);
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

    public List<WeeklySubjectScore> findByRegimenId(Long regimenId) throws SQLException {
        String sql = """
                SELECT w.regimen_id, w.subject_id, w.score_percent, w.band, s.name AS subject_name
                FROM weekly_subject_scores w
                JOIN subjects s ON s.id = w.subject_id
                WHERE w.regimen_id = ?
                ORDER BY s.name
                """;
        List<WeeklySubjectScore> scores = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, regimenId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WeeklySubjectScore score = new WeeklySubjectScore();
                    score.setRegimenId(rs.getLong("regimen_id"));
                    score.setSubjectId(rs.getLong("subject_id"));
                    score.setScorePercent(rs.getBigDecimal("score_percent"));
                    score.setBand(SubjectBand.fromString(rs.getString("band")));
                    score.setSubjectName(rs.getString("subject_name"));
                    scores.add(score);
                }
            }
        }
        return scores;
    }
}
