package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.ExamLevel;
import com.examprep.model.Subject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SubjectDao {

    private static final String SELECT_COLS =
            "id, name, description, is_professional, is_sub_professional";

    public List<Subject> findAll() throws SQLException {
        String sql = "SELECT " + SELECT_COLS + " FROM subjects ORDER BY name";
        List<Subject> subjects = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                subjects.add(mapRow(rs));
            }
        }
        return subjects;
    }

    public List<Subject> findByExamLevel(ExamLevel examLevel) throws SQLException {
        if (examLevel == null) {
            return findAll();
        }
        String levelColumn = examLevel == ExamLevel.PROFESSIONAL
                ? "is_professional"
                : "is_sub_professional";
        String sql = "SELECT " + SELECT_COLS + " FROM subjects WHERE " + levelColumn + " = TRUE ORDER BY name";
        List<Subject> subjects = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                subjects.add(mapRow(rs));
            }
        }
        return subjects;
    }

    public Optional<Subject> findById(Long id) throws SQLException {
        String sql = "SELECT " + SELECT_COLS + " FROM subjects WHERE id = ?";
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

    public Optional<Subject> findByNameIgnoreCase(String name) throws SQLException {
        String sql = "SELECT " + SELECT_COLS + " FROM subjects WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Subject create(String name, String description, boolean professional, boolean subProfessional)
            throws SQLException {
        String sql = "INSERT INTO subjects (name, description, is_professional, is_sub_professional) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setBoolean(3, professional);
            ps.setBoolean(4, subProfessional);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        }
        throw new SQLException("Failed to create subject");
    }

    public void update(Long id, String name, String description, boolean professional, boolean subProfessional)
            throws SQLException {
        String sql = "UPDATE subjects SET name = ?, description = ?, is_professional = ?, is_sub_professional = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setBoolean(3, professional);
            ps.setBoolean(4, subProfessional);
            ps.setLong(5, id);
            ps.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM subjects WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Subject mapRow(ResultSet rs) throws SQLException {
        Subject subject = new Subject();
        subject.setId(rs.getLong("id"));
        subject.setName(rs.getString("name"));
        subject.setDescription(rs.getString("description"));
        subject.setProfessional(rs.getBoolean("is_professional"));
        subject.setSubProfessional(rs.getBoolean("is_sub_professional"));
        return subject;
    }
}
