package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.ExamLevel;
import com.examprep.model.Role;
import com.examprep.model.User;

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

public class UserDao {

    private static final String SELECT_COLUMNS =
            "SELECT id, username, email, password_hash, role, exam_level, created_at, diagnostic_completed_at FROM users";

    public Optional<User> findById(Long id) throws SQLException {
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
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE email = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<User> findAll() throws SQLException {
        String sql = SELECT_COLUMNS + " ORDER BY created_at DESC";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    public User create(String username, String email, String passwordHash, Role role, ExamLevel examLevel)
            throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return create(conn, username, email, passwordHash, role, examLevel);
        }
    }

    public User create(Connection conn, String username, String email, String passwordHash, Role role,
                       ExamLevel examLevel) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, role, exam_level) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, role.name());
            if (examLevel != null) {
                ps.setString(5, examLevel.name());
            } else {
                ps.setNull(5, Types.VARCHAR);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(conn, keys.getLong(1)).orElseThrow();
                }
            }
        }
        throw new SQLException("Failed to create user");
    }

    public Optional<User> findById(Connection conn, Long id) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(Connection conn, String username) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(Connection conn, String email) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean isDiagnosticCompleted(Long userId) throws SQLException {
        String sql = "SELECT diagnostic_completed_at FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("diagnostic_completed_at") != null;
                }
            }
        }
        return false;
    }

    public void markDiagnosticCompleted(Long userId) throws SQLException {
        String sql = "UPDATE users SET diagnostic_completed_at = ? WHERE id = ? AND diagnostic_completed_at IS NULL";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(Role.fromString(rs.getString("role")));
        user.setExamLevel(ExamLevel.fromString(rs.getString("exam_level")));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp diagnosticCompletedAt = rs.getTimestamp("diagnostic_completed_at");
        if (diagnosticCompletedAt != null) {
            user.setDiagnosticCompletedAt(diagnosticCompletedAt.toLocalDateTime());
        }
        return user;
    }
}
