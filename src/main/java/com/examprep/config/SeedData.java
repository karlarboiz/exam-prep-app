package com.examprep.config;

import com.examprep.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class SeedData {

    private SeedData() {
    }

    public static void seedAdminIfMissing() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?")) {
                ps.setString(1, "admin");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return;
                    }
                }
            }
            String hash = PasswordUtil.hash("admin123");
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, 'ADMIN')")) {
                ps.setString(1, "admin");
                ps.setString(2, "admin@examprep.local");
                ps.setString(3, hash);
                ps.executeUpdate();
            }
        }
    }
}
