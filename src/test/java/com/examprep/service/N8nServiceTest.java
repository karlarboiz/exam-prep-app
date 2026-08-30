package com.examprep.service;

import com.examprep.dao.N8nRequestDao;
import com.examprep.dao.UserDao;
import com.examprep.model.N8nRequest;
import com.examprep.model.N8nRequestKind;
import com.examprep.model.N8nRequestStatus;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.support.DatabaseTestSupport;
import com.examprep.util.PasswordUtil;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class N8nServiceTest extends DatabaseTestSupport {

    private HttpServer server;
    private String questionsUrl;
    private String analyzeUrl;
    private final AtomicInteger questionsStatus = new AtomicInteger(200);
    private final AtomicInteger analyzeStatus = new AtomicInteger(200);
    private final AtomicReference<String> lastSecret = new AtomicReference<>();
    private final AtomicReference<String> lastQuestionsBody = new AtomicReference<>();
    private final AtomicReference<String> lastAnalyzeBody = new AtomicReference<>();
    private final AtomicReference<String> lastAnalyzeContentType = new AtomicReference<>();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/questions", exchange -> {
            lastSecret.set(exchange.getRequestHeaders().getFirst("X-N8n-Secret"));
            lastQuestionsBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] ok = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(questionsStatus.get(), ok.length);
            exchange.getResponseBody().write(ok);
            exchange.close();
        });
        server.createContext("/analyze", exchange -> {
            lastAnalyzeContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            lastAnalyzeBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(analyzeStatus.get(), -1);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();
        questionsUrl = "http://127.0.0.1:" + port + "/questions";
        analyzeUrl = "http://127.0.0.1:" + port + "/analyze";
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void questionRequestAcceptedWritesAudit() throws Exception {
        User admin = createAdmin();
        N8nService service = newService();

        N8nRequest row = service.requestQuestions(admin, "20 constitution items", "General Knowledge",
                "20", "medium", "cse-import-2026-08-30");

        assertEquals(N8nRequestStatus.ACCEPTED, row.getStatus());
        assertEquals(N8nRequestKind.QUESTIONS, row.getKind());
        assertEquals("20 constitution items", row.getSummary());
        assertEquals("n8n-secret", lastSecret.get());
        assertTrue(lastQuestionsBody.get().contains("\"requestedBy\":\"boss\""));
        assertTrue(lastQuestionsBody.get().contains("\"count\":\"20\""));
        assertTrue(lastQuestionsBody.get().contains("\"difficulty\":\"MEDIUM\""));
        assertTrue(lastQuestionsBody.get().contains("outputContract"));
    }

    @Test
    void questionRequestRejectedWhenWebhookBlank() throws Exception {
        User admin = createAdmin();
        N8nService service = new N8nService("", analyzeUrl, "secret",
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                new N8nRequestDao());
        assertFalse(service.isQuestionsConfigured());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.requestQuestions(admin, "make questions", null, null, null, null));
        assertTrue(ex.getMessage().contains("not configured"));
        assertTrue(service.recentRequests().isEmpty());
    }

    @Test
    void questionRequestFailedWritesAudit() throws Exception {
        User admin = createAdmin();
        questionsStatus.set(500);
        N8nService service = newService();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.requestQuestions(admin, "make questions", null, null, null, null));
        assertTrue(ex.getMessage().contains("did not accept"));

        List<N8nRequest> rows = service.recentRequests();
        assertEquals(1, rows.size());
        assertEquals(N8nRequestStatus.FAILED, rows.get(0).getStatus());
        assertEquals("HTTP 500", rows.get(0).getErrorMessage());
    }

    @Test
    void analyzeAcceptedForAllowedFile() throws Exception {
        User admin = createAdmin();
        N8nService service = newService();

        N8nRequest row = service.analyzeFile(admin, "notes.pdf", "application/pdf",
                "hello".getBytes(StandardCharsets.UTF_8), "extract topics");

        assertEquals(N8nRequestStatus.ACCEPTED, row.getStatus());
        assertEquals(N8nRequestKind.ANALYZE, row.getKind());
        assertTrue(row.getSummary().contains("notes.pdf"));
        assertTrue(lastAnalyzeContentType.get().startsWith("multipart/form-data"));
        assertTrue(lastAnalyzeBody.get().contains("extract topics"));
        assertTrue(lastAnalyzeBody.get().contains("notes.pdf"));
    }

    @Test
    void analyzeRejectsDisallowedType() throws Exception {
        User admin = createAdmin();
        N8nService service = newService();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.analyzeFile(admin, "payload.exe", "application/octet-stream",
                        new byte[] {1, 2, 3}, null));
        assertTrue(ex.getMessage().toLowerCase().contains("not allowed"));
        assertTrue(service.recentRequests().isEmpty());
    }

    @Test
    void analyzeRejectsEmptyFile() throws Exception {
        User admin = createAdmin();
        N8nService service = newService();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.analyzeFile(admin, "notes.pdf", "application/pdf", new byte[0], null));
        assertTrue(ex.getMessage().toLowerCase().contains("choose a file"));
    }

    private N8nService newService() {
        return new N8nService(questionsUrl, analyzeUrl, "n8n-secret",
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                new N8nRequestDao());
    }

    private User createAdmin() throws Exception {
        return new UserDao().create("boss", "boss@example.com", PasswordUtil.hash("password123"),
                Role.ADMIN, null);
    }
}
