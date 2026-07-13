package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.Exam;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExamDao {

    public List<Exam> findAll() throws SQLException {
        String sql = """
                SELECT e.id, e.subject_id, e.title, e.duration_minutes, e.is_active,
                       s.name AS subject_name,
                       (SELECT COUNT(*) FROM exam_questions eq WHERE eq.exam_id = e.id) AS question_count
                FROM exams e
                JOIN subjects s ON s.id = e.subject_id
                ORDER BY e.id DESC
                """;
        return queryList(sql);
    }

    public List<Exam> findActive() throws SQLException {
        String sql = """
                SELECT e.id, e.subject_id, e.title, e.duration_minutes, e.is_active,
                       s.name AS subject_name,
                       (SELECT COUNT(*) FROM exam_questions eq WHERE eq.exam_id = e.id) AS question_count
                FROM exams e
                JOIN subjects s ON s.id = e.subject_id
                WHERE e.is_active = TRUE
                ORDER BY e.title
                """;
        return queryList(sql);
    }

    public Optional<Exam> findById(Long id) throws SQLException {
        String sql = """
                SELECT e.id, e.subject_id, e.title, e.duration_minutes, e.is_active,
                       s.name AS subject_name,
                       (SELECT COUNT(*) FROM exam_questions eq WHERE eq.exam_id = e.id) AS question_count
                FROM exams e
                JOIN subjects s ON s.id = e.subject_id
                WHERE e.id = ?
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

    public Exam create(Exam exam) throws SQLException {
        String sql = "INSERT INTO exams (subject_id, title, duration_minutes, is_active) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, exam.getSubjectId());
            ps.setString(2, exam.getTitle());
            ps.setInt(3, exam.getDurationMinutes());
            ps.setBoolean(4, exam.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        }
        throw new SQLException("Failed to create exam");
    }

    public void update(Exam exam) throws SQLException {
        String sql = "UPDATE exams SET subject_id = ?, title = ?, duration_minutes = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, exam.getSubjectId());
            ps.setString(2, exam.getTitle());
            ps.setInt(3, exam.getDurationMinutes());
            ps.setBoolean(4, exam.isActive());
            ps.setLong(5, exam.getId());
            ps.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM exams WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void setExamQuestions(Long examId, List<Long> questionIds) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement deletePs = conn.prepareStatement("DELETE FROM exam_questions WHERE exam_id = ?")) {
                    deletePs.setLong(1, examId);
                    deletePs.executeUpdate();
                }
                String insertSql = "INSERT INTO exam_questions (exam_id, question_id, sort_order) VALUES (?, ?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    int order = 1;
                    for (Long questionId : questionIds) {
                        insertPs.setLong(1, examId);
                        insertPs.setLong(2, questionId);
                        insertPs.setInt(3, order++);
                        insertPs.addBatch();
                    }
                    insertPs.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<Long> getQuestionIds(Long examId) throws SQLException {
        String sql = "SELECT question_id FROM exam_questions WHERE exam_id = ? ORDER BY sort_order";
        List<Long> ids = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("question_id"));
                }
            }
        }
        return ids;
    }

    private List<Exam> queryList(String sql) throws SQLException {
        List<Exam> exams = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                exams.add(mapRow(rs));
            }
        }
        return exams;
    }

    private Exam mapRow(ResultSet rs) throws SQLException {
        Exam exam = new Exam();
        exam.setId(rs.getLong("id"));
        exam.setSubjectId(rs.getLong("subject_id"));
        exam.setTitle(rs.getString("title"));
        exam.setDurationMinutes(rs.getInt("duration_minutes"));
        exam.setActive(rs.getBoolean("is_active"));
        exam.setSubjectName(rs.getString("subject_name"));
        exam.setQuestionCount(rs.getInt("question_count"));
        return exam;
    }
}
