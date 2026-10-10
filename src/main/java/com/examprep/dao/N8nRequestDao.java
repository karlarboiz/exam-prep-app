package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.N8nRequest;
import com.examprep.model.N8nRequestKind;
import com.examprep.model.N8nRequestStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class N8nRequestDao {

    private static final String SELECT_COLUMNS =
            "SELECT id, admin_user_id, kind, summary, status, error_message, created_at FROM n8n_requests";

    public N8nRequest insert(Long adminUserId, N8nRequestKind kind, String summary,
                             N8nRequestStatus status, String errorMessage) throws SQLException {
        String sql = """
                INSERT INTO n8n_requests (admin_user_id, kind, summary, status, error_message)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, adminUserId);
            ps.setString(2, kind.name());
            ps.setString(3, summary);
            ps.setString(4, status.name());
            if (errorMessage != null && !errorMessage.isBlank()) {
                ps.setString(5, errorMessage);
            } else {
                ps.setNull(5, Types.VARCHAR);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        }
        throw new SQLException("Failed to insert n8n request row");
    }

    public Optional<N8nRequest> findById(Long id) throws SQLException {
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
        return java.util.Optional.empty();
    }

    public List<N8nRequest> findRecent(int limit) throws SQLException {
        String sql = SELECT_COLUMNS + " ORDER BY created_at DESC, id DESC LIMIT ?";
        List<N8nRequest> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
            }
        }
        return rows;
    }

    private static N8nRequest mapRow(ResultSet rs) throws SQLException {
        N8nRequest row = new N8nRequest();
        row.setId(rs.getLong("id"));
        row.setAdminUserId(rs.getLong("admin_user_id"));
        row.setKind(N8nRequestKind.fromString(rs.getString("kind")));
        row.setSummary(rs.getString("summary"));
        row.setStatus(N8nRequestStatus.fromString(rs.getString("status")));
        row.setErrorMessage(rs.getString("error_message"));
        if (rs.getTimestamp("created_at") != null) {
            row.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return row;
    }
}
