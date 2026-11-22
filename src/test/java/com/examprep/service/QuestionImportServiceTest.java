package com.examprep.service;

import com.examprep.dao.QuestionDao;
import com.examprep.dao.SubjectDao;
import com.examprep.importing.ExcelQuestionParser;
import com.examprep.importing.QuestionImportResult;
import com.examprep.importing.QuestionImportRow;
import com.examprep.model.Question;
import com.examprep.model.Subject;
import com.examprep.support.DatabaseTestSupport;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionImportServiceTest extends DatabaseTestSupport {

    private final QuestionImportService importService = new QuestionImportService();
    private final SubjectDao subjectDao = new SubjectDao();
    private final QuestionDao questionDao = new QuestionDao();

    @Test
    void suggestedBatchLabelUsesCseImportDate() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-23T02:00:00Z"), ZoneOffset.ofHours(8));
        assertEquals("cse-import-2026-08-23", QuestionImportService.suggestedBatchLabel(clock));
    }

    @Test
    void importStampsSuggestedDailyBatchLabel() throws Exception {
        String today = QuestionImportService.suggestedBatchLabel();
        QuestionImportResult result = importService.importFromExcel(
                new ByteArrayInputStream(buildWorkbook(null, null, "What is H2O?", "Water", null)),
                today);

        assertEquals(1, result.getImportedCount());
        Long subjectId = subjectDao.findByNameIgnoreCase("Imported Science").orElseThrow().getId();
        Question imported = questionDao.findBySubjectId(subjectId).get(0);
        assertEquals(today, imported.getBatchLabel());
        assertTrue(today.startsWith("cse-import-"));
    }

    @Test
    void importCreatesNewSubjectVisibleOnBothTracksByDefault() throws Exception {
        byte[] xlsx = buildWorkbook(null, null, "What is H2O?", "Water");
        QuestionImportResult result = importService.importFromExcel(new ByteArrayInputStream(xlsx));

        assertEquals(1, result.getImportedCount());
        Optional<Subject> subject = subjectDao.findByNameIgnoreCase("Imported Science");
        assertTrue(subject.isPresent());
        assertTrue(subject.get().isProfessional());
        assertTrue(subject.get().isSubProfessional());
    }

    @Test
    void importRespectsExplicitLevelFlags() throws Exception {
        byte[] xlsx = buildWorkbook("true", "false", "What is H2O?", "Water");
        QuestionImportResult result = importService.importFromExcel(new ByteArrayInputStream(xlsx));

        assertEquals(1, result.getImportedCount());
        Subject subject = subjectDao.findByNameIgnoreCase("Imported Science").orElseThrow();
        assertTrue(subject.isProfessional());
        assertFalse(subject.isSubProfessional());
    }

    @Test
    void importRejectsBothLevelFlagsFalse() throws Exception {
        byte[] xlsx = buildWorkbook("false", "false", "What is H2O?", "Water");
        QuestionImportResult result = importService.importFromExcel(new ByteArrayInputStream(xlsx));

        assertEquals(0, result.getImportedCount());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().get(0).contains("subject level flags"));
        assertTrue(subjectDao.findByNameIgnoreCase("Imported Science").isEmpty());
    }

    @Test
    void reimportUpdatesExistingQuestionInsteadOfDuplicating() throws Exception {
        int before = questionDao.findAll().size();
        QuestionImportResult first = importService.importFromExcel(
                new ByteArrayInputStream(buildWorkbook(null, null, "What is H2O?", "Water", "science-v1")));
        assertEquals(1, first.getImportedCount());
        assertEquals(0, first.getUpdatedCount());
        assertEquals(before + 1, questionDao.findAll().size());

        QuestionImportResult second = importService.importFromExcel(
                new ByteArrayInputStream(buildWorkbook(null, null, "What is H2O?", "Dihydrogen monoxide", null)),
                "science-v1");
        assertEquals(0, second.getImportedCount());
        assertEquals(1, second.getUpdatedCount());
        assertEquals(before + 1, questionDao.findAll().size());

        Long subjectId = subjectDao.findByNameIgnoreCase("Imported Science").orElseThrow().getId();
        Question updated = questionDao.findBySubjectIdPromptAndBatchIgnoreCase(
                subjectId, "What is H2O?", "science-v1").orElseThrow();
        assertEquals("Dihydrogen monoxide", updated.getOptionB());
        assertEquals("science-v1", updated.getBatchLabel());
    }

    @Test
    void differentBatchLabelsDoNotOverwriteEachOther() throws Exception {
        importService.importFromExcel(
                new ByteArrayInputStream(buildWorkbook(null, null, "What is H2O?", "Water", null)),
                "science-2025");
        QuestionImportResult second = importService.importFromExcel(
                new ByteArrayInputStream(buildWorkbook(null, null, "What is H2O?", "Dihydrogen monoxide", null)),
                "science-2026");

        assertEquals(1, second.getImportedCount());
        assertEquals(0, second.getUpdatedCount());

        Long subjectId = subjectDao.findByNameIgnoreCase("Imported Science").orElseThrow().getId();
        List<Question> questions = questionDao.findBySubjectId(subjectId);
        assertEquals(2, questions.size());
        Question older = questionDao.findBySubjectIdPromptAndBatchIgnoreCase(
                subjectId, "What is H2O?", "science-2025").orElseThrow();
        Question newer = questionDao.findBySubjectIdPromptAndBatchIgnoreCase(
                subjectId, "What is H2O?", "science-2026").orElseThrow();
        assertEquals("Water", older.getOptionB());
        assertEquals("Dihydrogen monoxide", newer.getOptionB());
    }

    @Test
    void labeledImportDoesNotUpdateUnlabeledQuestion() throws Exception {
        importService.importFromExcel(
                new ByteArrayInputStream(buildWorkbook(null, null, "What is H2O?", "Water")));
        QuestionImportResult labeled = importService.importFromExcel(
                new ByteArrayInputStream(buildWorkbook(null, null, "What is H2O?", "Steam", null)),
                "science-v2");

        assertEquals(1, labeled.getImportedCount());
        assertEquals(0, labeled.getUpdatedCount());

        Long subjectId = subjectDao.findByNameIgnoreCase("Imported Science").orElseThrow().getId();
        Question unlabeled = questionDao.findBySubjectIdAndPromptIgnoreCase(subjectId, "What is H2O?").orElseThrow();
        assertEquals("Water", unlabeled.getOptionB());
        assertTrue(unlabeled.getBatchLabel() == null || unlabeled.getBatchLabel().isBlank());
    }

    @Test
    void excelBatchLabelMismatchIsRejected() throws Exception {
        QuestionImportResult result = importService.importFromExcel(
                new ByteArrayInputStream(buildWorkbook(null, null, "What is H2O?", "Water", "other-batch")),
                "science-v1");

        assertEquals(0, result.getImportedCount());
        assertEquals(0, result.getUpdatedCount());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().get(0).contains("does not match this import's batch"));
        assertTrue(subjectDao.findByNameIgnoreCase("Imported Science").isEmpty());
    }

    @Test
    void templateCanBeImported() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        importService.writeTemplate(out);
        List<QuestionImportRow> rows = new ExcelQuestionParser().parse(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, rows.size());
        assertEquals("Sample Subject", rows.get(0).getSubject());

        int before = questionDao.findAll().size();
        QuestionImportResult result = importService.importFromExcel(
                new ByteArrayInputStream(out.toByteArray()), "sample-batch");
        assertEquals(1, result.getImportedCount());
        assertEquals(before + 1, questionDao.findAll().size());
        Long subjectId = subjectDao.findByNameIgnoreCase("Sample Subject").orElseThrow().getId();
        Question imported = questionDao.findBySubjectId(subjectId).get(0);
        assertEquals("sample-batch", imported.getBatchLabel());
    }

    @Test
    void exportRoundTripUpsertsRatherThanDuplicates() throws Exception {
        importService.importFromExcel(
                new ByteArrayInputStream(buildWorkbook(null, null, "What is H2O?", "Water", null)),
                "science-v1");
        Long subjectId = subjectDao.findByNameIgnoreCase("Imported Science").orElseThrow().getId();
        List<Question> exportedQuestions = questionDao.findBySubjectId(subjectId);
        assertEquals(1, exportedQuestions.size());
        assertEquals("science-v1", exportedQuestions.get(0).getBatchLabel());

        ByteArrayOutputStream exported = new ByteArrayOutputStream();
        importService.exportQuestions(exportedQuestions, exported);

        QuestionImportResult result = importService.importFromExcel(
                new ByteArrayInputStream(exported.toByteArray()), "science-v1");
        assertEquals(0, result.getImportedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(1, questionDao.findBySubjectId(subjectId).size());
    }

    @Test
    void rejectsBadLabelsEmptyFilesAndInvalidRows() throws Exception {
        assertEquals(null, QuestionImportService.normalizeBatchLabel("  "));
        java.lang.reflect.Method matchKey = QuestionImportService.class.getDeclaredMethod(
                "matchKey", String.class, String.class);
        matchKey.setAccessible(true);
        assertTrue(matchKey.invoke(null, null, "Prompt").toString().startsWith("\0"));
        assertTrue(matchKey.invoke(null, " ", "Prompt").toString().startsWith("\0"));
        assertTrue(matchKey.invoke(null, "Batch", "Prompt").toString().contains("batch"));
        assertFalse(QuestionImportService.isValidBatchLabel(null));
        assertFalse(QuestionImportService.isValidBatchLabel("x".repeat(101)));
        assertTrue(QuestionImportService.batchLabelError("__UNLABELED__").contains("reserved"));
        assertTrue(QuestionImportService.batchLabelError("x".repeat(101)).contains("at most"));
        assertTrue(QuestionImportService.suggestedBatchLabel(null).startsWith("cse-import-"));

        QuestionImportResult reserved = importService.importFromExcel(
                new ByteArrayInputStream(buildWorkbook(null, null, "What is H2O?", "Water")),
                "__unlabeled__");
        assertTrue(reserved.getErrors().get(0).contains("reserved"));

        QuestionImportResult empty = importService.importFromExcel(new ByteArrayInputStream(headerOnly()));
        assertTrue(empty.getErrors().get(0).contains("No data rows"));

        QuestionImportResult invalid = importService.importFromExcel(
                new ByteArrayInputStream(invalidRows()), "science-v1");
        String errors = String.join("\n", invalid.getErrors());
        assertTrue(errors.contains("subject is required"));
        assertTrue(errors.contains("prompt is required"));
        assertTrue(errors.contains("1000"));
        assertTrue(errors.contains("option_a"));
        assertTrue(errors.contains("500"));
        assertTrue(errors.contains("correct_option is required"));
        assertTrue(errors.contains("A, B, C, or D"));
        assertTrue(errors.contains("difficulty"));
        assertTrue(errors.contains("explanation is required"));
        assertTrue(errors.contains("2000"));
        assertTrue(errors.contains("reserved") || errors.contains("at most"));
        assertTrue(errors.contains("does not match"));

        QuestionImportResult flags = importService.importFromExcel(
                new ByteArrayInputStream(buildWorkbook(null, "yes", "Only sub?", "Yes")));
        assertEquals(1, flags.getImportedCount());
        assertEquals(1, importService.importFromExcel(
                new ByteArrayInputStream(levelWorkbook("Flag True", "true", null))).getImportedCount());
        assertEquals(1, importService.importFromExcel(
                new ByteArrayInputStream(levelWorkbook("Flag One", "1", null))).getImportedCount());
        assertEquals(1, importService.importFromExcel(
                new ByteArrayInputStream(levelWorkbook("Flag Y", "y", "0"))).getImportedCount());
        assertEquals(1, importService.importFromExcel(
                new ByteArrayInputStream(levelWorkbook("Flag Blank", " ", " "))).getImportedCount());
        assertTrue(importService.importFromExcel(
                new ByteArrayInputStream(levelWorkbook("Flag None", "no", "false")))
                .getErrors().get(0).contains("subject level"));
        Subject subject = subjectDao.findByNameIgnoreCase("Imported Science").orElseThrow();
        assertFalse(subject.isProfessional());
        assertTrue(subject.isSubProfessional());

        byte[] twice = twoSamePrompts();
        QuestionImportResult firstPass = importService.importFromExcel(new ByteArrayInputStream(twice), "dup-batch");
        assertEquals(1, firstPass.getImportedCount());
        QuestionImportResult secondPass = importService.importFromExcel(new ByteArrayInputStream(twice), "dup-batch");
        assertEquals(1, secondPass.getUpdatedCount());
    }

    private byte[] headerOnly() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("subject");
            header.createCell(1).setCellValue("prompt");
            header.createCell(2).setCellValue("option_a");
            header.createCell(3).setCellValue("option_b");
            header.createCell(4).setCellValue("option_c");
            header.createCell(5).setCellValue("option_d");
            header.createCell(6).setCellValue("correct_option");
            header.createCell(7).setCellValue("explanation");
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] invalidRows() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            String[] cols = {
                    "subject", "prompt", "option_a", "option_b", "option_c", "option_d",
                    "correct_option", "explanation", "difficulty", "batch_label"
            };
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            addInvalid(sheet, 1, "", "prompt", "A", "B", "C", "D", "A", "because", "EASY", null);
            addInvalid(sheet, 2, "Sub", "", "A", "B", "C", "D", "A", "because", "EASY", null);
            addInvalid(sheet, 3, "Sub", "p".repeat(1001), "A", "B", "C", "D", "A", "because", "EASY", null);
            addInvalid(sheet, 4, "Sub", "prompt", "", "B", "C", "D", "A", "because", "EASY", null);
            addInvalid(sheet, 13, "Sub", "prompt b", "A", "", "C", "D", "A", "because", "EASY", null);
            addInvalid(sheet, 14, "Sub", "prompt c", "A", "B", "", "D", "A", "because", "EASY", null);
            addInvalid(sheet, 15, "Sub", "prompt d", "A", "B", "C", "", "A", "because", "EASY", null);
            addInvalid(sheet, 5, "Sub", "prompt", "a".repeat(501), "B", "C", "D", "A", "because", "EASY", null);
            addInvalid(sheet, 16, "Sub", "prompt long b", "A", "b".repeat(501), "C", "D", "A", "because", "EASY", null);
            addInvalid(sheet, 17, "Sub", "prompt long c", "A", "B", "c".repeat(501), "D", "A", "because", "EASY", null);
            addInvalid(sheet, 18, "Sub", "prompt long d", "A", "B", "C", "d".repeat(501), "A", "because", "EASY", null);
            addInvalid(sheet, 6, "Sub", "prompt", "A", "B", "C", "D", "", "because", "EASY", null);
            addInvalid(sheet, 7, "Sub", "prompt", "A", "B", "C", "D", "E", "because", "EASY", null);
            addInvalid(sheet, 8, "Sub", "prompt", "A", "B", "C", "D", "A", "because", "WILD", null);
            addInvalid(sheet, 9, "Sub", "prompt", "A", "B", "C", "D", "A", "", "EASY", null);
            addInvalid(sheet, 10, "Sub", "prompt", "A", "B", "C", "D", "A", "e".repeat(2001), "", null);
            addInvalid(sheet, 11, "Sub", "prompt", "A", "B", "C", "D", "A", "because", "", "__unlabeled__");
            addInvalid(sheet, 12, "Sub", "prompt", "A", "B", "C", "D", "A", "because", "", "other-batch");
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void addInvalid(Sheet sheet, int rowIndex, String subject, String prompt, String a, String b, String c,
                            String d, String correct, String explanation, String difficulty, String batch) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(subject);
        row.createCell(1).setCellValue(prompt);
        row.createCell(2).setCellValue(a);
        row.createCell(3).setCellValue(b);
        row.createCell(4).setCellValue(c);
        row.createCell(5).setCellValue(d);
        row.createCell(6).setCellValue(correct);
        row.createCell(7).setCellValue(explanation);
        row.createCell(8).setCellValue(difficulty);
        if (batch != null) {
            row.createCell(9).setCellValue(batch);
        }
    }

    private byte[] twoSamePrompts() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            String[] cols = {
                    "subject", "prompt", "option_a", "option_b", "option_c", "option_d",
                    "correct_option", "explanation"
            };
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            for (int i = 1; i <= 2; i++) {
                Row data = sheet.createRow(i);
                data.createCell(0).setCellValue("Dup Subject");
                data.createCell(1).setCellValue("Same prompt");
                data.createCell(2).setCellValue("A" + i);
                data.createCell(3).setCellValue("B");
                data.createCell(4).setCellValue("C");
                data.createCell(5).setCellValue("D");
                data.createCell(6).setCellValue("A");
                data.createCell(7).setCellValue("Because");
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] levelWorkbook(String subject, String professional, String subProfessional) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            String[] cols = {
                    "subject", "prompt", "option_a", "option_b", "option_c", "option_d",
                    "correct_option", "explanation", "is_professional", "is_sub_professional"
            };
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue(subject);
            data.createCell(1).setCellValue("Prompt for " + subject);
            data.createCell(2).setCellValue("A");
            data.createCell(3).setCellValue("B");
            data.createCell(4).setCellValue("C");
            data.createCell(5).setCellValue("D");
            data.createCell(6).setCellValue("A");
            data.createCell(7).setCellValue("Because");
            if (professional != null) {
                data.createCell(8).setCellValue(professional);
            }
            if (subProfessional != null) {
                data.createCell(9).setCellValue(subProfessional);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] buildWorkbook(String professional, String subProfessional, String prompt, String optionB)
            throws Exception {
        return buildWorkbook(professional, subProfessional, prompt, optionB, null);
    }

    private byte[] buildWorkbook(String professional, String subProfessional, String prompt, String optionB,
                                 String batchLabel) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            String[] cols = {
                    "subject", "prompt", "option_a", "option_b", "option_c", "option_d",
                    "correct_option", "explanation", "is_professional", "is_sub_professional", "batch_label"
            };
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("Imported Science");
            data.createCell(1).setCellValue(prompt);
            data.createCell(2).setCellValue("Oxygen");
            data.createCell(3).setCellValue(optionB);
            data.createCell(4).setCellValue("Hydrogen");
            data.createCell(5).setCellValue("Helium");
            data.createCell(6).setCellValue("B");
            data.createCell(7).setCellValue("Water is H2O.");
            if (professional != null) {
                data.createCell(8).setCellValue(professional);
            }
            if (subProfessional != null) {
                data.createCell(9).setCellValue(subProfessional);
            }
            if (batchLabel != null) {
                data.createCell(10).setCellValue(batchLabel);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
