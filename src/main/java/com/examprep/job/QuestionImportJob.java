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
        if (args.length < 1 || args.length > 2 || args[0].isBlank()
                || (args.length == 2 && args[1].isBlank())) {
            System.err.println("Usage: QuestionImportJob <path-to-questions.xlsx> [batch-label]");
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

        String batchLabel = args.length == 2
                ? QuestionImportService.normalizeBatchLabel(args[1])
                : QuestionImportService.suggestedBatchLabel();
        if (batchLabel == null || !QuestionImportService.isValidBatchLabel(batchLabel)) {
            System.err.println("Batch label is required and must be at most "
                    + QuestionImportService.BATCH_LABEL_MAX_LENGTH + " characters");
            System.exit(2);
            return;
        }

        DatabaseManager.init();
        try {
            QuestionImportResult result;
            try (InputStream in = Files.newInputStream(path)) {
                result = new QuestionImportService().importFromExcel(in, batchLabel);
            }

            System.out.println("Imported: " + result.getImportedCount());
            System.out.println("Updated: " + result.getUpdatedCount());
            if (result.hasErrors()) {
                System.out.println("Errors:");
                for (String error : result.getErrors()) {
                    System.out.println("  - " + error);
                }
            }

            if (result.getImportedCount() + result.getUpdatedCount() == 0) {
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
