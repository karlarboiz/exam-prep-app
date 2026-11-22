package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.FollowUpKind;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class RegimenFollowUpDao {

    public boolean exists(Long regimenId, FollowUpKind kind) throws SQLException {
        String sql = "SELECT 1 FROM regimen_follow_ups WHERE regimen_id = ? AND kind = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, regimenId);
            ps.setString(2, kind.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insert(Long regimenId, FollowUpKind kind, LocalDateTime sentAt) throws SQLException {
        String sql = "INSERT INTO regimen_follow_ups (regimen_id, kind, sent_at) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, regimenId);
            ps.setString(2, kind.name());
            ps.setTimestamp(3, Timestamp.valueOf(sentAt));
            ps.executeUpdate();
        }
    }
}
