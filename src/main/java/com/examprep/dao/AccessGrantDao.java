package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.AccessGrant;
import com.examprep.model.AccessGrantStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

public class AccessGrantDao {

    public AccessGrant create(String tokenHash, LocalDateTime expiresAt, String planCode, String sourceRef)
            throws SQLException {
        String sql = """
                INSERT INTO access_grants (token_hash, status, expires_at, plan_code, source_ref)
                VALUES (?, 'UNUSED', ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tokenHash);
            ps.setTimestamp(2, Timestamp.valueOf(expiresAt));
            setNullableString(ps, 3, planCode);
            setNullableString(ps, 4, sourceRef);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        }
        throw new SQLException("Failed to create access grant");
    }

    public Optional<AccessGrant> findById(Long id) throws SQLException {
        String sql = """
                SELECT id, token_hash, status, expires_at, redeemed_at, user_id, plan_code, source_ref, created_at
                FROM access_grants WHERE id = ?
                """;
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

    public Optional<AccessGrant> findByTokenHash(String tokenHash) throws SQLException {
        String sql = """
                SELECT id, token_hash, status, expires_at, redeemed_at, user_id, plan_code, source_ref, created_at
                FROM access_grants WHERE token_hash = ?
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<AccessGrant> findActiveByUserId(Long userId) throws SQLException {
        String sql = """
                SELECT id, token_hash, status, expires_at, redeemed_at, user_id, plan_code, source_ref, created_at
                FROM access_grants
                WHERE user_id = ? AND status = 'REDEEMED' AND expires_at > CURRENT_TIMESTAMP
                ORDER BY expires_at DESC
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

    public Optional<AccessGrant> findLatestRedeemedByUserId(Long userId) throws SQLException {
        String sql = """
                SELECT id, token_hash, status, expires_at, redeemed_at, user_id, plan_code, source_ref, created_at
                FROM access_grants
                WHERE user_id = ? AND status = 'REDEEMED'
                ORDER BY expires_at DESC
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

    public void redeem(Connection conn, Long grantId, Long userId) throws SQLException {
        String sql = """
                UPDATE access_grants
                SET status = 'REDEEMED', user_id = ?, redeemed_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status = 'UNUSED'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, grantId);
            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new SQLException("Access grant could not be redeemed (already used or missing)");
            }
        }
    }

    private static void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private AccessGrant mapRow(ResultSet rs) throws SQLException {
        AccessGrant grant = new AccessGrant();
        grant.setId(rs.getLong("id"));
        grant.setTokenHash(rs.getString("token_hash"));
        grant.setStatus(AccessGrantStatus.fromString(rs.getString("status")));
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            grant.setExpiresAt(expiresAt.toLocalDateTime());
        }
        Timestamp redeemedAt = rs.getTimestamp("redeemed_at");
        if (redeemedAt != null) {
            grant.setRedeemedAt(redeemedAt.toLocalDateTime());
        }
        long userId = rs.getLong("user_id");
        if (!rs.wasNull()) {
            grant.setUserId(userId);
        }
        grant.setPlanCode(rs.getString("plan_code"));
        grant.setSourceRef(rs.getString("source_ref"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            grant.setCreatedAt(createdAt.toLocalDateTime());
        }
        return grant;
    }
}
