package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.EmailOutbox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class EmailOutboxDao {

    public EmailOutbox insert(Long userId, Long regimenId, String toAddress, String subject, String body)
            throws SQLException {
        String sql = """
                INSERT INTO email_outbox (user_id, regimen_id, to_address, subject, body)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            if (regimenId != null) {
                ps.setLong(2, regimenId);
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setString(3, toAddress);
            ps.setString(4, subject);
            ps.setString(5, body);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    EmailOutbox row = new EmailOutbox();
                    row.setId(keys.getLong(1));
                    row.setUserId(userId);
                    row.setRegimenId(regimenId);
                    row.setToAddress(toAddress);
                    row.setSubject(subject);
                    row.setBody(body);
                    return row;
                }
            }
        }
        throw new SQLException("Failed to insert email outbox row");
    }

    public List<EmailOutbox> findByUserId(Long userId) throws SQLException {
        String sql = """
                SELECT id, user_id, regimen_id, to_address, subject, body, created_at
                FROM email_outbox
                WHERE user_id = ?
                ORDER BY created_at DESC
                """;
        List<EmailOutbox> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EmailOutbox row = new EmailOutbox();
                    row.setId(rs.getLong("id"));
                    row.setUserId(rs.getLong("user_id"));
                    long regimenId = rs.getLong("regimen_id");
                    if (!rs.wasNull()) {
                        row.setRegimenId(regimenId);
                    }
                    row.setToAddress(rs.getString("to_address"));
                    row.setSubject(rs.getString("subject"));
                    row.setBody(rs.getString("body"));
                    if (rs.getTimestamp("created_at") != null) {
                        row.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    public List<EmailOutbox> findByRegimenId(Long regimenId) throws SQLException {
        String sql = """
                SELECT id, user_id, regimen_id, to_address, subject, body, created_at
                FROM email_outbox
                WHERE regimen_id = ?
                ORDER BY created_at DESC
                """;
        List<EmailOutbox> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, regimenId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EmailOutbox row = new EmailOutbox();
                    row.setId(rs.getLong("id"));
                    row.setUserId(rs.getLong("user_id"));
                    row.setRegimenId(rs.getLong("regimen_id"));
                    row.setToAddress(rs.getString("to_address"));
                    row.setSubject(rs.getString("subject"));
                    row.setBody(rs.getString("body"));
                    if (rs.getTimestamp("created_at") != null) {
                        row.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }
}
