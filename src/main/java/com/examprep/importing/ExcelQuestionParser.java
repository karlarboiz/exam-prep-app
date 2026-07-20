package com.examprep.importing;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ExcelQuestionParser {

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "subject", "prompt", "option_a", "option_b", "option_c", "option_d",
            "correct_option", "explanation"
    );

    private final DataFormatter formatter = new DataFormatter();

    public List<QuestionImportRow> parse(InputStream inputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Excel file has no sheets");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel file is missing a header row");
            }

            Map<String, Integer> headerIndex = mapHeaders(headerRow);
            for (String required : REQUIRED_HEADERS) {
                if (!headerIndex.containsKey(required)) {
                    throw new IllegalArgumentException("Missing required column: " + required);
                }
            }

            List<QuestionImportRow> rows = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, headerIndex)) {
                    continue;
                }
                QuestionImportRow importRow = new QuestionImportRow(i + 1);
                importRow.setSubject(cellValue(row, headerIndex.get("subject")));
                importRow.setPrompt(cellValue(row, headerIndex.get("prompt")));
                importRow.setOptionA(cellValue(row, headerIndex.get("option_a")));
                importRow.setOptionB(cellValue(row, headerIndex.get("option_b")));
                importRow.setOptionC(cellValue(row, headerIndex.get("option_c")));
                importRow.setOptionD(cellValue(row, headerIndex.get("option_d")));
                importRow.setCorrectOption(cellValue(row, headerIndex.get("correct_option")));
                Integer difficultyCol = headerIndex.get("difficulty");
                if (difficultyCol != null) {
                    importRow.setDifficulty(cellValue(row, difficultyCol));
                }
                importRow.setExplanation(cellValue(row, headerIndex.get("explanation")));
                rows.add(importRow);
            }
            return rows;
        }
    }

    private Map<String, Integer> mapHeaders(Row headerRow) {
        Map<String, Integer> headers = new HashMap<>();
        short lastCell = headerRow.getLastCellNum();
        for (int i = 0; i < lastCell; i++) {
            String raw = cellValue(headerRow, i);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String key = raw.trim().toLowerCase(Locale.ROOT);
            headers.put(key, i);
        }
        return headers;
    }

    private boolean isBlankRow(Row row, Map<String, Integer> headerIndex) {
        for (Integer index : headerIndex.values()) {
            String value = cellValue(row, index);
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String cellValue(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell);
        return value != null ? value.trim() : null;
    }
}
