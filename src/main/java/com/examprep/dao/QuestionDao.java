package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuestionDao {

    public List<Question> findAll() throws SQLException {
        String sql = """
                SELECT q.id, q.subject_id, q.prompt, q.option_a, q.option_b, q.option_c, q.option_d,
                       q.correct_option, q.difficulty, s.name AS subject_name
                FROM questions q
                JOIN subjects s ON s.id = q.subject_id
                ORDER BY q.id
                """;
        return queryList(sql);
    }

    public List<Question> findBySubjectId(Long subjectId) throws SQLException {
        String sql = """
                SELECT q.id, q.subject_id, q.prompt, q.option_a, q.option_b, q.option_c, q.option_d,
                       q.correct_option, q.difficulty, s.name AS subject_name
                FROM questions q
                JOIN subjects s ON s.id = q.subject_id
                WHERE q.subject_id = ?
                ORDER BY q.id
                """;
        List<Question> questions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapRow(rs));
                }
            }
        }
        return questions;
    }

    public List<Question> findByExamId(Long examId) throws SQLException {
        String sql = """
                SELECT q.id, q.subject_id, q.prompt, q.option_a, q.option_b, q.option_c, q.option_d,
                       q.correct_option, q.difficulty, s.name AS subject_name
                FROM exam_questions eq
                JOIN questions q ON q.id = eq.question_id
                JOIN subjects s ON s.id = q.subject_id
                WHERE eq.exam_id = ?
                ORDER BY eq.sort_order
                """;
        List<Question> questions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapRow(rs));
                }
            }
        }
        return questions;
    }

    public Optional<Question> findById(Long id) throws SQLException {
        String sql = """
                SELECT q.id, q.subject_id, q.prompt, q.option_a, q.option_b, q.option_c, q.option_d,
                       q.correct_option, q.difficulty, s.name AS subject_name
                FROM questions q
                JOIN subjects s ON s.id = q.subject_id
                WHERE q.id = ?
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

    public Question create(Question question) throws SQLException {
        String sql = """
                INSERT INTO questions (subject_id, prompt, option_a, option_b, option_c, option_d, correct_option, difficulty)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, question.getSubjectId());
            ps.setString(2, question.getPrompt());
            ps.setString(3, question.getOptionA());
            ps.setString(4, question.getOptionB());
            ps.setString(5, question.getOptionC());
            ps.setString(6, question.getOptionD());
            ps.setString(7, question.getCorrectOption());
            ps.setString(8, question.getDifficulty());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        }
        throw new SQLException("Failed to create question");
    }

    public void update(Question question) throws SQLException {
        String sql = """
                UPDATE questions SET subject_id = ?, prompt = ?, option_a = ?, option_b = ?, option_c = ?,
                       option_d = ?, correct_option = ?, difficulty = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, question.getSubjectId());
            ps.setString(2, question.getPrompt());
            ps.setString(3, question.getOptionA());
            ps.setString(4, question.getOptionB());
            ps.setString(5, question.getOptionC());
            ps.setString(6, question.getOptionD());
            ps.setString(7, question.getCorrectOption());
            ps.setString(8, question.getDifficulty());
            ps.setLong(9, question.getId());
            ps.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM questions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private List<Question> queryList(String sql) throws SQLException {
        List<Question> questions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                questions.add(mapRow(rs));
            }
        }
        return questions;
    }

    private Question mapRow(ResultSet rs) throws SQLException {
        Question question = new Question();
        question.setId(rs.getLong("id"));
        question.setSubjectId(rs.getLong("subject_id"));
        question.setPrompt(rs.getString("prompt"));
        question.setOptionA(rs.getString("option_a"));
        question.setOptionB(rs.getString("option_b"));
        question.setOptionC(rs.getString("option_c"));
        question.setOptionD(rs.getString("option_d"));
        question.setCorrectOption(rs.getString("correct_option"));
        question.setDifficulty(rs.getString("difficulty"));
        question.setSubjectName(rs.getString("subject_name"));
        return question;
    }
}
