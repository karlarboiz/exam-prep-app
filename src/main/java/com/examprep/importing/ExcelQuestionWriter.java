package com.examprep.importing;

import com.examprep.model.Question;
import com.examprep.model.Subject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public final class ExcelQuestionWriter {

    public static final String[] COLUMNS = {
            "subject", "prompt", "option_a", "option_b", "option_c", "option_d",
            "correct_option", "difficulty", "explanation",
            "is_professional", "is_sub_professional"
    };

    private ExcelQuestionWriter() {
    }

    public static void writeTemplate(OutputStream out) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("questions");
            writeHeader(sheet);
            Row example = sheet.createRow(1);
            String[] values = {
                    "Sample Subject",
                    "What is 2 + 2?",
                    "3", "4", "5", "6",
                    "B",
                    "EASY",
                    "2 + 2 equals 4.",
                    "true",
                    "true"
            };
            for (int i = 0; i < values.length; i++) {
                example.createCell(i).setCellValue(values[i]);
            }
            autosize(sheet);
            workbook.write(out);
        }
    }

    public static void writeQuestions(List<Question> questions, Map<Long, Subject> subjectsById, OutputStream out)
            throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("questions");
            writeHeader(sheet);
            int rowIndex = 1;
            for (Question question : questions) {
                Row row = sheet.createRow(rowIndex++);
                Subject subject = subjectsById.get(question.getSubjectId());
                String subjectName = question.getSubjectName() != null
                        ? question.getSubjectName()
                        : (subject != null ? subject.getName() : "");
                boolean professional = subject != null && subject.isProfessional();
                boolean subProfessional = subject != null && subject.isSubProfessional();
                String[] values = {
                        subjectName,
                        nullToEmpty(question.getPrompt()),
                        nullToEmpty(question.getOptionA()),
                        nullToEmpty(question.getOptionB()),
                        nullToEmpty(question.getOptionC()),
                        nullToEmpty(question.getOptionD()),
                        nullToEmpty(question.getCorrectOption()),
                        nullToEmpty(question.getDifficulty()),
                        nullToEmpty(question.getExplanation()),
                        professional ? "true" : "false",
                        subProfessional ? "true" : "false"
                };
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }
            autosize(sheet);
            workbook.write(out);
        }
    }

    private static void writeHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < COLUMNS.length; i++) {
            header.createCell(i).setCellValue(COLUMNS[i]);
        }
    }

    private static void autosize(Sheet sheet) {
        for (int i = 0; i < COLUMNS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
