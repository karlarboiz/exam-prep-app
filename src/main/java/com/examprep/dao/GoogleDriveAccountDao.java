package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.GoogleDriveAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

public class GoogleDriveAccountDao {

    private static final String SELECT_COLUMNS = """
            SELECT id, admin_user_id, google_email, access_token, refresh_token,
                   access_expires_at, created_at, updated_at
            FROM google_drive_accounts
            """;

    public Optional<GoogleDriveAccount> findByAdminUserId(Long adminUserId) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE admin_user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, adminUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void upsert(GoogleDriveAccount account) throws SQLException {
        String sql = """
                MERGE INTO google_drive_accounts
                    (admin_user_id, google_email, access_token, refresh_token, access_expires_at, updated_at)
                KEY (admin_user_id)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, account.getAdminUserId());
            ps.setString(2, account.getGoogleEmail());
            ps.setString(3, account.getAccessToken());
            ps.setString(4, account.getRefreshToken());
            ps.setTimestamp(5, Timestamp.valueOf(account.getAccessExpiresAt()));
            ps.executeUpdate();
        }
    }

    public void deleteByAdminUserId(Long adminUserId) throws SQLException {
        String sql = "DELETE FROM google_drive_accounts WHERE admin_user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, adminUserId);
            ps.executeUpdate();
        }
    }

    private static GoogleDriveAccount mapRow(ResultSet rs) throws SQLException {
        GoogleDriveAccount row = new GoogleDriveAccount();
        row.setId(rs.getLong("id"));
        row.setAdminUserId(rs.getLong("admin_user_id"));
        row.setGoogleEmail(rs.getString("google_email"));
        row.setAccessToken(rs.getString("access_token"));
        row.setRefreshToken(rs.getString("refresh_token"));
        if (rs.getTimestamp("access_expires_at") != null) {
            row.setAccessExpiresAt(rs.getTimestamp("access_expires_at").toLocalDateTime());
        }
        if (rs.getTimestamp("created_at") != null) {
            row.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            row.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return row;
    }
}
