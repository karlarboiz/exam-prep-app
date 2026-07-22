package com.examprep.service;

import com.examprep.dao.QuestionDao;
import com.examprep.dao.SubjectDao;
import com.examprep.importing.ExcelQuestionParser;
import com.examprep.importing.QuestionImportResult;
import com.examprep.importing.QuestionImportRow;
import com.examprep.model.Question;
import com.examprep.model.Subject;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class QuestionImportService {

    private static final Set<String> VALID_OPTIONS = Set.of("A", "B", "C", "D");
    private static final Set<String> VALID_DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private final ExcelQuestionParser parser = new ExcelQuestionParser();
    private final QuestionDao questionDao = new QuestionDao();
    private final SubjectDao subjectDao = new SubjectDao();

    public QuestionImportResult importFromExcel(InputStream inputStream) throws IOException, SQLException {
        List<QuestionImportRow> rows = parser.parse(inputStream);
        QuestionImportResult result = new QuestionImportResult();
        if (rows.isEmpty()) {
            result.addError("No data rows found in Excel file");
            return result;
        }

        Map<String, Long> subjectCache = new HashMap<>();
        List<Question> toInsert = new ArrayList<>();

        for (QuestionImportRow row : rows) {
            Optional<String> validationError = validate(row);
            if (validationError.isPresent()) {
                result.addError("Row " + row.getExcelRowNumber() + ": " + validationError.get());
                continue;
            }

            String subjectKey = row.getSubject().trim().toLowerCase(Locale.ROOT);
            Long subjectId = subjectCache.get(subjectKey);
            if (subjectId == null) {
                subjectId = resolveOrCreateSubject(row.getSubject().trim());
                subjectCache.put(subjectKey, subjectId);
            }

            Question question = new Question();
            question.setSubjectId(subjectId);
            question.setPrompt(row.getPrompt().trim());
            question.setOptionA(row.getOptionA().trim());
            question.setOptionB(row.getOptionB().trim());
            question.setOptionC(row.getOptionC().trim());
            question.setOptionD(row.getOptionD().trim());
            question.setCorrectOption(row.getCorrectOption().trim().toUpperCase(Locale.ROOT));
            question.setDifficulty(normalizeDifficulty(row.getDifficulty()));
            question.setExplanation(row.getExplanation().trim());
            toInsert.add(question);
        }

        if (!toInsert.isEmpty()) {
            result.setImportedCount(questionDao.createBatch(toInsert));
        }
        return result;
    }

    private Long resolveOrCreateSubject(String name) throws SQLException {
        Optional<Subject> existing = subjectDao.findByNameIgnoreCase(name);
        if (existing.isPresent()) {
            return existing.get().getId();
        }
        return subjectDao.create(name, null, false, false).getId();
    }

    private Optional<String> validate(QuestionImportRow row) {
        if (isBlank(row.getSubject())) {
            return Optional.of("subject is required");
        }
        if (isBlank(row.getPrompt())) {
            return Optional.of("prompt is required");
        }
        if (row.getPrompt().trim().length() > 1000) {
            return Optional.of("prompt must be at most 1000 characters");
        }
        if (isBlank(row.getOptionA()) || isBlank(row.getOptionB())
                || isBlank(row.getOptionC()) || isBlank(row.getOptionD())) {
            return Optional.of("option_a, option_b, option_c, and option_d are required");
        }
        if (row.getOptionA().trim().length() > 500 || row.getOptionB().trim().length() > 500
                || row.getOptionC().trim().length() > 500 || row.getOptionD().trim().length() > 500) {
            return Optional.of("each option must be at most 500 characters");
        }
        if (isBlank(row.getCorrectOption())) {
            return Optional.of("correct_option is required");
        }
        String correct = row.getCorrectOption().trim().toUpperCase(Locale.ROOT);
        if (!VALID_OPTIONS.contains(correct)) {
            return Optional.of("correct_option must be A, B, C, or D");
        }
        if (!isBlank(row.getDifficulty())) {
            String difficulty = row.getDifficulty().trim().toUpperCase(Locale.ROOT);
            if (!VALID_DIFFICULTIES.contains(difficulty)) {
                return Optional.of("difficulty must be EASY, MEDIUM, or HARD");
            }
        }
        if (isBlank(row.getExplanation())) {
            return Optional.of("explanation is required");
        }
        if (row.getExplanation().trim().length() > 2000) {
            return Optional.of("explanation must be at most 2000 characters");
        }
        return Optional.empty();
    }

    private String normalizeDifficulty(String difficulty) {
        if (isBlank(difficulty)) {
            return "MEDIUM";
        }
        return difficulty.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
