package com.examprep.job;

import com.examprep.config.DatabaseManager;
import com.examprep.importing.QuestionImportResult;
import com.examprep.service.QuestionImportService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

public final class QuestionImportJob {

    private QuestionImportJob() {
    }

    public static void main(String[] args) {
        if (args.length != 1 || args[0].isBlank()) {
            System.err.println("Usage: QuestionImportJob <path-to-questions.xlsx>");
            System.exit(2);
            return;
        }

        Path path = Path.of(args[0]);
        if (!Files.isRegularFile(path)) {
            System.err.println("File not found: " + path.toAbsolutePath());
            System.exit(1);
            return;
        }
        if (!path.getFileName().toString().toLowerCase().endsWith(".xlsx")) {
            System.err.println("Only .xlsx files are supported");
            System.exit(1);
            return;
        }

        DatabaseManager.init();
        try {
            QuestionImportResult result;
            try (InputStream in = Files.newInputStream(path)) {
                result = new QuestionImportService().importFromExcel(in);
            }

            System.out.println("Imported: " + result.getImportedCount());
            if (result.hasErrors()) {
                System.out.println("Errors:");
                for (String error : result.getErrors()) {
                    System.out.println("  - " + error);
                }
            }

            if (result.getImportedCount() == 0) {
                System.exit(1);
            }
            System.exit(0);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Failed to read Excel: " + e.getMessage());
            System.exit(1);
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            System.exit(1);
        } finally {
            DatabaseManager.shutdown();
        }
    }
}
