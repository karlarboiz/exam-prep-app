package com.examprep.service;

import com.examprep.dao.SubjectDao;
import com.examprep.importing.QuestionImportResult;
import com.examprep.model.Subject;
import com.examprep.support.DatabaseTestSupport;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionImportServiceTest extends DatabaseTestSupport {

    private final QuestionImportService importService = new QuestionImportService();
    private final SubjectDao subjectDao = new SubjectDao();

    @Test
    void importCreatesNewSubjectVisibleOnBothTracksByDefault() throws Exception {
        byte[] xlsx = buildWorkbook(null, null);
        QuestionImportResult result = importService.importFromExcel(new ByteArrayInputStream(xlsx));

        assertEquals(1, result.getImportedCount());
        Optional<Subject> subject = subjectDao.findByNameIgnoreCase("Imported Science");
        assertTrue(subject.isPresent());
        assertTrue(subject.get().isProfessional());
        assertTrue(subject.get().isSubProfessional());
    }

    @Test
    void importRespectsExplicitLevelFlags() throws Exception {
        byte[] xlsx = buildWorkbook("true", "false");
        QuestionImportResult result = importService.importFromExcel(new ByteArrayInputStream(xlsx));

        assertEquals(1, result.getImportedCount());
        Subject subject = subjectDao.findByNameIgnoreCase("Imported Science").orElseThrow();
        assertTrue(subject.isProfessional());
        assertFalse(subject.isSubProfessional());
    }

    @Test
    void importRejectsBothLevelFlagsFalse() throws Exception {
        byte[] xlsx = buildWorkbook("false", "false");
        QuestionImportResult result = importService.importFromExcel(new ByteArrayInputStream(xlsx));

        assertEquals(0, result.getImportedCount());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().get(0).contains("subject level flags"));
        assertTrue(subjectDao.findByNameIgnoreCase("Imported Science").isEmpty());
    }

    private byte[] buildWorkbook(String professional, String subProfessional) throws Exception {
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
            data.createCell(0).setCellValue("Imported Science");
            data.createCell(1).setCellValue("What is H2O?");
            data.createCell(2).setCellValue("Oxygen");
            data.createCell(3).setCellValue("Water");
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
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
