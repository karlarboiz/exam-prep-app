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

    private static final String SELECT_COLUMNS = """
            SELECT q.id, q.subject_id, q.prompt, q.option_a, q.option_b, q.option_c, q.option_d,
                   q.correct_option, q.difficulty, q.explanation, q.image_url, q.batch_label,
                   s.name AS subject_name
            """;

    public List<Question> findAll() throws SQLException {
        String sql = SELECT_COLUMNS + """
                FROM questions q
                JOIN subjects s ON s.id = q.subject_id
                ORDER BY q.id
                """;
        return queryList(sql);
    }

    public List<Question> findBySubjectId(Long subjectId) throws SQLException {
        return findFiltered(subjectId, null, false);
    }

    /**
     * Filter the bank by optional subject and batch label.
     * {@code unlabeledOnly} selects rows with a null or blank batch label.
     */
    public List<Question> findFiltered(Long subjectId, String batchLabel, boolean unlabeledOnly) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
        sql.append("""
                FROM questions q
                JOIN subjects s ON s.id = q.subject_id
                WHERE 1=1
                """);
        if (subjectId != null) {
            sql.append(" AND q.subject_id = ?");
        }
        if (unlabeledOnly) {
            sql.append(" AND (q.batch_label IS NULL OR TRIM(q.batch_label) = '')");
        } else if (batchLabel != null && !batchLabel.isBlank()) {
            sql.append(" AND LOWER(TRIM(q.batch_label)) = LOWER(TRIM(?))");
        }
        sql.append(" ORDER BY q.id");

        List<Question> questions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = 1;
            if (subjectId != null) {
                ps.setLong(i++, subjectId);
            }
            if (!unlabeledOnly && batchLabel != null && !batchLabel.isBlank()) {
                ps.setString(i, batchLabel);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapRow(rs));
                }
            }
        }
        return questions;
    }

    public List<String> listBatchLabels() throws SQLException {
        String sql = """
                SELECT DISTINCT batch_label
                FROM questions
                WHERE batch_label IS NOT NULL AND TRIM(batch_label) <> ''
                ORDER BY LOWER(batch_label)
                """;
        List<String> labels = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                labels.add(rs.getString("batch_label"));
            }
        }
        return labels;
    }

    public List<Question> findByExamId(Long examId) throws SQLException {
        String sql = SELECT_COLUMNS + """
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

    public List<Question> findByIds(List<Long> questionIds) throws SQLException {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        String placeholders = questionIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
        String sql = SELECT_COLUMNS
                + " FROM questions q JOIN subjects s ON s.id = q.subject_id WHERE q.id IN ("
                + placeholders + ")";
        List<Question> found = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (Long id : questionIds) {
                ps.setLong(i++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    found.add(mapRow(rs));
                }
            }
        }
        java.util.Map<Long, Question> byId = new java.util.HashMap<>();
        for (Question q : found) {
            byId.put(q.getId(), q);
        }
        List<Question> ordered = new ArrayList<>();
        for (Long id : questionIds) {
            Question q = byId.get(id);
            if (q != null) {
                ordered.add(q);
            }
        }
        return ordered;
    }

    public List<Question> findByAttemptId(Long attemptId) throws SQLException {
        String sql = SELECT_COLUMNS + """
                FROM attempt_questions aq
                JOIN questions q ON q.id = aq.question_id
                JOIN subjects s ON s.id = q.subject_id
                WHERE aq.attempt_id = ?
                ORDER BY aq.sort_order
                """;
        List<Question> questions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapRow(rs));
                }
            }
        }
        return questions;
    }

    public void setAttemptQuestions(Long attemptId, List<Long> questionIds) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement deletePs = conn.prepareStatement(
                        "DELETE FROM attempt_questions WHERE attempt_id = ?")) {
                    deletePs.setLong(1, attemptId);
                    deletePs.executeUpdate();
                }
                String insertSql = "INSERT INTO attempt_questions (attempt_id, question_id, sort_order) VALUES (?, ?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    int order = 1;
                    for (Long questionId : questionIds) {
                        insertPs.setLong(1, attemptId);
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

    public boolean hasAttemptQuestions(Long attemptId) throws SQLException {
        String sql = "SELECT 1 FROM attempt_questions WHERE attempt_id = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Optional<Question> findById(Long id) throws SQLException {
        String sql = SELECT_COLUMNS + """
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

    public Optional<Question> findBySubjectIdAndPromptIgnoreCase(Long subjectId, String prompt) throws SQLException {
        return findBySubjectIdPromptAndBatchIgnoreCase(subjectId, prompt, null);
    }

    public Optional<Question> findBySubjectIdPromptAndBatchIgnoreCase(Long subjectId, String prompt, String batchLabel)
            throws SQLException {
        String sql = SELECT_COLUMNS + """
                FROM questions q
                JOIN subjects s ON s.id = q.subject_id
                WHERE q.subject_id = ? AND LOWER(TRIM(q.prompt)) = LOWER(TRIM(?))
                """;
        if (batchLabel == null || batchLabel.isBlank()) {
            sql += " AND (q.batch_label IS NULL OR TRIM(q.batch_label) = '')";
        } else {
            sql += " AND LOWER(TRIM(q.batch_label)) = LOWER(TRIM(?))";
        }
        sql += " ORDER BY q.id LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, subjectId);
            ps.setString(2, prompt);
            if (batchLabel != null && !batchLabel.isBlank()) {
                ps.setString(3, batchLabel);
            }
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
                INSERT INTO questions (subject_id, prompt, option_a, option_b, option_c, option_d,
                                       correct_option, difficulty, explanation, image_url, batch_label)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindQuestion(ps, question);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        }
        throw new SQLException("Failed to create question");
    }

    public int createBatch(List<Question> questions) throws SQLException {
        if (questions == null || questions.isEmpty()) {
            return 0;
        }
        String sql = """
                INSERT INTO questions (subject_id, prompt, option_a, option_b, option_c, option_d,
                                       correct_option, difficulty, explanation, image_url, batch_label)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Question question : questions) {
                bindQuestion(ps, question);
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            int count = 0;
            for (int result : results) {
                if (result >= 0 || result == Statement.SUCCESS_NO_INFO) {
                    count++;
                }
            }
            return count;
        }
    }

    public void update(Question question) throws SQLException {
        String sql = """
                UPDATE questions SET subject_id = ?, prompt = ?, option_a = ?, option_b = ?, option_c = ?,
                       option_d = ?, correct_option = ?, difficulty = ?, explanation = ?, image_url = ?,
                       batch_label = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindQuestion(ps, question);
            ps.setLong(12, question.getId());
            ps.executeUpdate();
        }
    }

    public int updateBatch(List<Question> questions) throws SQLException {
        if (questions == null || questions.isEmpty()) {
            return 0;
        }
        String sql = """
                UPDATE questions SET subject_id = ?, prompt = ?, option_a = ?, option_b = ?, option_c = ?,
                       option_d = ?, correct_option = ?, difficulty = ?, explanation = ?, batch_label = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Question question : questions) {
                bindQuestionCore(ps, question);
                ps.setString(10, question.getBatchLabel());
                ps.setLong(11, question.getId());
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            int count = 0;
            for (int result : results) {
                if (result >= 0 || result == Statement.SUCCESS_NO_INFO) {
                    count++;
                }
            }
            return count;
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

    private void bindQuestion(PreparedStatement ps, Question question) throws SQLException {
        bindQuestionCore(ps, question);
        ps.setString(10, question.getImageUrl());
        ps.setString(11, question.getBatchLabel());
    }

    private void bindQuestionCore(PreparedStatement ps, Question question) throws SQLException {
        ps.setLong(1, question.getSubjectId());
        ps.setString(2, question.getPrompt());
        ps.setString(3, question.getOptionA());
        ps.setString(4, question.getOptionB());
        ps.setString(5, question.getOptionC());
        ps.setString(6, question.getOptionD());
        ps.setString(7, question.getCorrectOption());
        ps.setString(8, question.getDifficulty());
        ps.setString(9, question.getExplanation());
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
        question.setExplanation(rs.getString("explanation"));
        question.setImageUrl(rs.getString("image_url"));
        question.setBatchLabel(rs.getString("batch_label"));
        question.setSubjectName(rs.getString("subject_name"));
        return question;
    }
}
