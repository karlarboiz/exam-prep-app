package com.examprep.service;

import com.examprep.config.AppConfig;
import com.examprep.dao.N8nRequestDao;
import com.examprep.model.N8nRequest;
import com.examprep.model.N8nRequestKind;
import com.examprep.model.N8nRequestStatus;
import com.examprep.model.User;
import com.examprep.util.MultipartForm;
import com.examprep.util.SimpleJson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class N8nService {

    public static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    public static final int RECENT_LIMIT = 20;
    public static final String OUTPUT_CONTRACT =
            "Excel .xlsx with header row: subject, prompt, option_a, option_b, option_c, option_d, "
                    + "correct_option, difficulty, explanation, optional batch_label. One MCQ per row. "
                    + "correct_option is A/B/C/D. difficulty is EASY/MEDIUM/HARD.";

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int SUMMARY_MAX = 300;
    private static final int ERROR_MAX = 500;
    private static final int MESSAGE_MAX = 4000;
    private static final int COUNT_MAX = 100;
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "docx", "txt", "xlsx", "png", "jpg", "jpeg");
    private static final Set<String> VALID_DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private final String questionsUrl;
    private final String analyzeUrl;
    private final String secret;
    private final HttpClient httpClient;
    private final N8nRequestDao requestDao;

    public N8nService() {
        this(AppConfig.get("n8n.webhook.questions", ""),
                AppConfig.get("n8n.webhook.analyze", ""),
                AppConfig.get("n8n.webhook.secret", ""),
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build(),
                new N8nRequestDao());
    }

    N8nService(String questionsUrl, String analyzeUrl, String secret,
               HttpClient httpClient, N8nRequestDao requestDao) {
        this.questionsUrl = blankToEmpty(questionsUrl);
        this.analyzeUrl = blankToEmpty(analyzeUrl);
        this.secret = blankToEmpty(secret);
        this.httpClient = httpClient;
        this.requestDao = requestDao;
    }

    public boolean isQuestionsConfigured() {
        return !questionsUrl.isBlank();
    }

    public boolean isAnalyzeConfigured() {
        return !analyzeUrl.isBlank();
    }

    public List<N8nRequest> recentRequests() throws SQLException {
        return requestDao.findRecent(RECENT_LIMIT);
    }

    public N8nRequest requestQuestions(User admin, String message, String subject,
                                       String countRaw, String difficulty, String batchLabel)
            throws SQLException {
        requireAdmin(admin);
        if (!isQuestionsConfigured()) {
            throw new IllegalArgumentException("n8n question webhook is not configured");
        }
        String trimmedMessage = requiredText(message, "Describe the question batch you want", MESSAGE_MAX);
        String trimmedSubject = optionalText(subject, 100);
        String count = normalizeCount(countRaw);
        String normalizedDifficulty = normalizeDifficulty(difficulty);
        String normalizedBatch = optionalText(batchLabel, QuestionImportService.BATCH_LABEL_MAX_LENGTH);
        if (normalizedBatch != null && !QuestionImportService.isValidBatchLabel(normalizedBatch)) {
            throw new IllegalArgumentException(
                    "Batch label must be at most " + QuestionImportService.BATCH_LABEL_MAX_LENGTH
                            + " characters and cannot be the reserved unlabeled filter");
        }

        String json = SimpleJson.object(
                "requestedBy", admin.getUsername(),
                "message", trimmedMessage,
                "subject", nullToEmpty(trimmedSubject),
                "count", nullToEmpty(count),
                "difficulty", nullToEmpty(normalizedDifficulty),
                "batchLabel", nullToEmpty(normalizedBatch),
                "outputContract", OUTPUT_CONTRACT
        );
        return send(admin.getId(), N8nRequestKind.QUESTIONS, trimmedMessage, questionsUrl,
                "application/json; charset=UTF-8", json.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "n8n did not accept the question request");
    }

    public N8nRequest analyzeFile(User admin, String fileName, String contentType,
                                  byte[] bytes, String message) throws SQLException {
        requireAdmin(admin);
        if (!isAnalyzeConfigured()) {
            throw new IllegalArgumentException("n8n analyze webhook is not configured");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Choose a file to analyze");
        }
        if (bytes.length > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("File is too large");
        }
        String safeName = fileName == null ? "" : fileName.trim();
        if (!isAllowedFilename(safeName)) {
            throw new IllegalArgumentException("File type is not allowed");
        }
        String trimmedMessage = optionalText(message, MESSAGE_MAX);

        try {
            MultipartForm form = new MultipartForm();
            form.addText("requestedBy", admin.getUsername());
            form.addText("originalFilename", safeName);
            if (trimmedMessage != null) {
                form.addText("message", trimmedMessage);
            }
            form.addFile("file", safeName, inferContentType(safeName, contentType), bytes);
            byte[] body = form.finish();
            String summary = trimmedMessage == null ? safeName : safeName + " — " + trimmedMessage;
            return send(admin.getId(), N8nRequestKind.ANALYZE, summary, analyzeUrl,
                    form.contentType(), body, "n8n did not accept the file");
        } catch (IOException e) {
            recordFailure(admin.getId(), N8nRequestKind.ANALYZE, safeName, e.getMessage());
            throw new IllegalStateException("n8n did not accept the file");
        }
    }

    public static boolean isAllowedFilename(String fileName) {
        String ext = extension(fileName);
        return ext != null && ALLOWED_EXTENSIONS.contains(ext);
    }

    private N8nRequest send(Long adminUserId, N8nRequestKind kind, String summary, String url,
                            String contentType, byte[] body, String failureMessage) throws SQLException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if (!secret.isBlank()) {
            builder.header("X-N8n-Secret", secret);
        }
        int status;
        try {
            HttpResponse<Void> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
            status = response.statusCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recordFailure(adminUserId, kind, summary, "Interrupted");
            throw new IllegalStateException(failureMessage);
        } catch (IOException e) {
            recordFailure(adminUserId, kind, summary, e.getMessage());
            throw new IllegalStateException(failureMessage);
        }
        if (status >= 200 && status < 300) {
            return requestDao.insert(adminUserId, kind, clip(summary, SUMMARY_MAX),
                    N8nRequestStatus.ACCEPTED, null);
        }
        recordFailure(adminUserId, kind, summary, "HTTP " + status);
        throw new IllegalStateException(failureMessage);
    }

    private void recordFailure(Long adminUserId, N8nRequestKind kind, String summary, String error)
            throws SQLException {
        requestDao.insert(adminUserId, kind, clip(summary, SUMMARY_MAX),
                N8nRequestStatus.FAILED, clip(error, ERROR_MAX));
    }

    private static void requireAdmin(User admin) {
        if (admin == null || admin.getId() == null) {
            throw new IllegalArgumentException("Admin user is required");
        }
    }

    private static String requiredText(String value, String missingMessage, int max) {
        String trimmed = optionalText(value, max);
        if (trimmed == null) {
            throw new IllegalArgumentException(missingMessage);
        }
        return trimmed;
    }

    private static String optionalText(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > max) {
            return trimmed.substring(0, max);
        }
        return trimmed;
    }

    private static String normalizeCount(String raw) {
        String trimmed = optionalText(raw, 10);
        if (trimmed == null) {
            return null;
        }
        try {
            int count = Integer.parseInt(trimmed);
            if (count < 1 || count > COUNT_MAX) {
                throw new IllegalArgumentException("Question count must be between 1 and 100");
            }
            return Integer.toString(count);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Question count must be between 1 and 100");
        }
    }

    private static String normalizeDifficulty(String raw) {
        String trimmed = optionalText(raw, 20);
        if (trimmed == null) {
            return null;
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (!VALID_DIFFICULTIES.contains(upper)) {
            throw new IllegalArgumentException("Difficulty must be EASY, MEDIUM, or HARD");
        }
        return upper;
    }

    private static String extension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        String base = slash >= 0 ? fileName.substring(slash + 1) : fileName;
        int dot = base.lastIndexOf('.');
        if (dot < 0 || dot == base.length() - 1) {
            return null;
        }
        return base.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String inferContentType(String fileName, String submitted) {
        if (submitted != null && !submitted.isBlank() && !"application/octet-stream".equals(submitted)) {
            return submitted;
        }
        String ext = extension(fileName);
        if (ext == null) {
            return "application/octet-stream";
        }
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt" -> "text/plain";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

    private static String clip(String value, int max) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
