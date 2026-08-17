package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.AccessGrant;
import com.examprep.model.AccessGrantStatus;
import com.examprep.model.ExamLevel;

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

public class AccessGrantDao {

    private static final String SELECT_COLUMNS = """
            SELECT g.id, g.token_hash, g.status, g.expires_at, g.redeemed_at, g.user_id,
                   g.plan_code, g.source_ref, g.exam_level, g.created_at, u.username
            FROM access_grants g
            LEFT JOIN users u ON u.id = g.user_id
            """;

    public AccessGrant create(String tokenHash, LocalDateTime expiresAt, String planCode, String sourceRef,
                              ExamLevel examLevel) throws SQLException {
        String sql = """
                INSERT INTO access_grants (token_hash, status, expires_at, plan_code, source_ref, exam_level)
                VALUES (?, 'UNUSED', ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tokenHash);
            ps.setTimestamp(2, Timestamp.valueOf(expiresAt));
            setNullableString(ps, 3, planCode);
            setNullableString(ps, 4, sourceRef);
            if (examLevel != null) {
                ps.setString(5, examLevel.name());
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
        throw new SQLException("Failed to create access grant");
    }

    public Optional<AccessGrant> findById(Long id) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE g.id = ?";
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
        String sql = SELECT_COLUMNS + " WHERE g.token_hash = ?";
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
        String sql = SELECT_COLUMNS + """
                WHERE g.user_id = ? AND g.status = 'REDEEMED' AND g.expires_at > CURRENT_TIMESTAMP
                ORDER BY g.expires_at DESC
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
        String sql = SELECT_COLUMNS + """
                WHERE g.user_id = ? AND g.status = 'REDEEMED'
                ORDER BY g.expires_at DESC
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

    public List<AccessGrant> findAll() throws SQLException {
        String sql = SELECT_COLUMNS + " ORDER BY g.created_at DESC";
        List<AccessGrant> grants = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                grants.add(mapRow(rs));
            }
        }
        return grants;
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

    /**
     * Marks a grant REVOKED. Unused tokens can no longer be redeemed; redeemed grants
     * immediately lose active access even if {@code expires_at} is still in the future.
     */
    public void updateExpiresAt(Long grantId, LocalDateTime expiresAt) throws SQLException {
        String sql = "UPDATE access_grants SET expires_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(expiresAt));
            ps.setLong(2, grantId);
            ps.executeUpdate();
        }
    }

    public boolean revoke(Long grantId) throws SQLException {
        String sql = """
                UPDATE access_grants
                SET status = 'REVOKED'
                WHERE id = ? AND status IN ('UNUSED', 'REDEEMED')
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, grantId);
            return ps.executeUpdate() == 1;
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
        grant.setExamLevel(ExamLevel.fromString(rs.getString("exam_level")));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            grant.setCreatedAt(createdAt.toLocalDateTime());
        }
        grant.setUsername(rs.getString("username"));
        return grant;
    }
}
