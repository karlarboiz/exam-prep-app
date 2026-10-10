package com.examprep.config;

import com.examprep.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class SeedData {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private SeedData() {
    }

    public static void seedAdminIfMissing() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?")) {
                ps.setString(1, DEFAULT_ADMIN_USERNAME);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return;
                    }
                }
            }

            String adminUsername = AppConfig.get("admin.username", DEFAULT_ADMIN_USERNAME);
            String adminPassword = AppConfig.get("admin.password", null);

            if (adminPassword == null || adminPassword.isBlank()) {
                if (AppConfig.isProduction()) {
                    throw new IllegalStateException(
                            "Admin password must be set via ADMIN_PASSWORD environment variable in production");
                }
                adminPassword = DEFAULT_ADMIN_PASSWORD;
                System.err.println("WARNING: Using default admin password. " +
                        "Set ADMIN_PASSWORD environment variable in production.");
            }

            String hash = PasswordUtil.hash(adminPassword);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, 'ADMIN')")) {
                ps.setString(1, adminUsername);
                ps.setString(2, adminUsername + "@examprep.local");
                ps.setString(3, hash);
                ps.executeUpdate();
            }
        }
    }
}
