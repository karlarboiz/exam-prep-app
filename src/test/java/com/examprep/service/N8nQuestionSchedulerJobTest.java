package com.examprep.service;

import com.examprep.dao.N8nRequestDao;
import com.examprep.dao.UserDao;
import com.examprep.model.N8nRequest;
import com.examprep.model.N8nRequestKind;
import com.examprep.model.N8nRequestStatus;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.support.ConfigOverrides;
import com.examprep.support.DatabaseTestSupport;
import com.examprep.util.PasswordUtil;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class N8nQuestionSchedulerJobTest extends DatabaseTestSupport {

    private final UserDao userDao = new UserDao();
    private final N8nRequestDao requestDao = new N8nRequestDao();
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private HttpServer server;
    private String questionsUrl;
    private String prevEnabled;
    private String prevMessage;
    private String prevSubject;
    private String prevCount;
    private String prevDifficulty;
    private String prevBatch;
    private String prevInterval;
    private String prevDelay;

    @BeforeEach
    void startStub() throws IOException {
        hits.set(0);
        lastBody.set(null);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/questions", exchange -> {
            hits.incrementAndGet();
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        questionsUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/questions";

        prevEnabled = ConfigOverrides.set("n8n.scheduler.enabled", "false");
        prevMessage = ConfigOverrides.set("n8n.scheduler.message", "");
        prevSubject = ConfigOverrides.set("n8n.scheduler.subject", "");
        prevCount = ConfigOverrides.set("n8n.scheduler.count", "20");
        prevDifficulty = ConfigOverrides.set("n8n.scheduler.difficulty", "MEDIUM");
        prevBatch = ConfigOverrides.set("n8n.scheduler.batchLabel", "");
        prevInterval = ConfigOverrides.set("n8n.scheduler.interval.hours", "24");
        prevDelay = ConfigOverrides.set("n8n.scheduler.initial.delay.minutes", "5");
    }

    @AfterEach
    void cleanup() {
        N8nQuestionSchedulerJob.stop();
        if (server != null) {
            server.stop(0);
        }
        ConfigOverrides.restore("n8n.scheduler.enabled", prevEnabled);
        ConfigOverrides.restore("n8n.scheduler.message", prevMessage);
        ConfigOverrides.restore("n8n.scheduler.subject", prevSubject);
        ConfigOverrides.restore("n8n.scheduler.count", prevCount);
        ConfigOverrides.restore("n8n.scheduler.difficulty", prevDifficulty);
        ConfigOverrides.restore("n8n.scheduler.batchLabel", prevBatch);
        ConfigOverrides.restore("n8n.scheduler.interval.hours", prevInterval);
        ConfigOverrides.restore("n8n.scheduler.initial.delay.minutes", prevDelay);
    }

    @Test
    void startIsNoOpWhenDisabled() {
        assertFalse(N8nQuestionSchedulerJob.isEnabled());
        N8nQuestionSchedulerJob.start();
        N8nQuestionSchedulerJob.start();
        N8nQuestionSchedulerJob.stop();
        N8nQuestionSchedulerJob.stop();
    }

    @Test
    void startIsIdempotentWhenEnabled() throws Exception {
        ConfigOverrides.set("n8n.scheduler.enabled", "true");
        ConfigOverrides.set("n8n.scheduler.message", "batch please");
        try {
            N8nQuestionSchedulerJob.start();
            N8nQuestionSchedulerJob.start();
            Field field = N8nQuestionSchedulerJob.class.getDeclaredField("executor");
            field.setAccessible(true);
            ScheduledExecutorService executor = (ScheduledExecutorService) field.get(null);
            assertTrue(executor != null && !executor.isShutdown());
            executor.shutdown();
            executor.awaitTermination(2, TimeUnit.SECONDS);
            N8nQuestionSchedulerJob.start();
        } finally {
            N8nQuestionSchedulerJob.stop();
        }
    }

    @Test
    void runOnceSkipsWhenDisabledOrIncomplete() throws Exception {
        User admin = userDao.create("admin", "admin@example.com", PasswordUtil.hash("password123"),
                Role.ADMIN, null);
        N8nService service = newService();

        assertDoesNotThrow(() -> N8nQuestionSchedulerJob.runOnce(service, userDao));
        assertEquals(0, hits.get());

        ConfigOverrides.set("n8n.scheduler.enabled", "true");
        assertDoesNotThrow(() -> N8nQuestionSchedulerJob.runOnce(service, userDao));
        assertEquals(0, hits.get());
        assertFalse(N8nQuestionSchedulerJob.isReady(service));

        ConfigOverrides.set("n8n.scheduler.message", "20 CSE items");
        assertTrue(N8nQuestionSchedulerJob.isReady(service));
        assertDoesNotThrow(() -> N8nQuestionSchedulerJob.runOnce(service, userDao));
        assertEquals(1, hits.get());
        assertTrue(lastBody.get().contains("\"requestedBy\":\"admin\""));
        assertTrue(lastBody.get().contains("20 CSE items"));

        List<N8nRequest> rows = requestDao.findRecent(5);
        assertEquals(1, rows.size());
        assertEquals(N8nRequestStatus.ACCEPTED, rows.get(0).getStatus());
        assertEquals(N8nRequestKind.QUESTIONS, rows.get(0).getKind());
        assertEquals(admin.getId(), rows.get(0).getAdminUserId());
    }

    @Test
    void runOnceUsesConfiguredPayloadAndSuggestedBatchLabel() throws Exception {
        userDao.create("boss", "boss@example.com", PasswordUtil.hash("password123"), Role.ADMIN, null);
        ConfigOverrides.set("n8n.scheduler.enabled", "true");
        ConfigOverrides.set("n8n.scheduler.message", "Generate constitution MCQs");
        ConfigOverrides.set("n8n.scheduler.subject", "General Knowledge");
        ConfigOverrides.set("n8n.scheduler.count", "15");
        ConfigOverrides.set("n8n.scheduler.difficulty", "HARD");
        ConfigOverrides.set("n8n.scheduler.batchLabel", "");

        N8nQuestionSchedulerJob.runOnce(newService(), userDao);

        assertEquals(1, hits.get());
        String body = lastBody.get();
        assertTrue(body.contains("Generate constitution MCQs"));
        assertTrue(body.contains("General Knowledge"));
        assertTrue(body.contains("\"count\":\"15\""));
        assertTrue(body.contains("HARD"));
        assertTrue(body.contains("cse-import-"));
    }

    @Test
    void runOnceSwallowsMissingAdmin() {
        ConfigOverrides.set("n8n.scheduler.enabled", "true");
        ConfigOverrides.set("n8n.scheduler.message", "batch");
        assertDoesNotThrow(() -> N8nQuestionSchedulerJob.runOnce(newService(), userDao));
        assertEquals(0, hits.get());
    }

    private N8nService newService() {
        return new N8nService(questionsUrl, "", "n8n-secret",
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                requestDao);
    }
}
