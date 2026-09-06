package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.PasswordResetToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

public class PasswordResetTokenDao {

    public void insert(Long userId, String tokenHash, LocalDateTime expiresAt) throws SQLException {
        String sql = "INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    public void invalidateUnusedForUser(Long userId) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ? AND used_at IS NULL";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    public Optional<PasswordResetToken> findByHash(String tokenHash) throws SQLException {
        String sql = """
                SELECT id, user_id, token_hash, expires_at, used_at, created_at
                FROM password_reset_tokens
                WHERE token_hash = ?
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PasswordResetToken token = new PasswordResetToken();
                    token.setId(rs.getLong("id"));
                    token.setUserId(rs.getLong("user_id"));
                    token.setTokenHash(rs.getString("token_hash"));
                    Timestamp expiresAt = rs.getTimestamp("expires_at");
                    if (expiresAt != null) {
                        token.setExpiresAt(expiresAt.toLocalDateTime());
                    }
                    Timestamp usedAt = rs.getTimestamp("used_at");
                    if (usedAt != null) {
                        token.setUsedAt(usedAt.toLocalDateTime());
                    }
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        token.setCreatedAt(createdAt.toLocalDateTime());
                    }
                    return Optional.of(token);
                }
            }
        }
        return Optional.empty();
    }

    public void markUsed(Long id) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
