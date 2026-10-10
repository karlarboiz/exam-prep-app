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
