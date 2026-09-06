package com.examprep.service;

import com.examprep.dao.GoogleDriveAccountDao;
import com.examprep.dao.UserDao;
import com.examprep.model.GoogleDriveAccount;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleOAuthServiceTest extends DatabaseTestSupport {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> lastTokenBody = new AtomicReference<>();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", exchange -> {
            lastTokenBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] ok = "{\"access_token\":\"ya29.access\",\"refresh_token\":\"1//refresh\",\"expires_in\":3600}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, ok.length);
            exchange.getResponseBody().write(ok);
            exchange.close();
        });
        server.createContext("/userinfo", exchange -> {
            byte[] ok = "{\"email\":\"owner@example.com\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, ok.length);
            exchange.getResponseBody().write(ok);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void notConfiguredWithoutClientSecret() {
        GoogleOAuthService service = new GoogleOAuthService("client", "", "",
                baseUrl + "/auth", baseUrl + "/token", baseUrl + "/userinfo",
                httpClient(), new GoogleDriveAccountDao());
        assertFalse(service.isConfigured());
    }

    @Test
    void authorizationUrlAsksForOfflineDriveAccess() {
        GoogleOAuthService service = newService();
        String url = service.authorizationUrl("http://localhost:8080/admin/n8n/drive/callback", "abc");
        assertTrue(url.contains("access_type=offline"));
        assertTrue(url.contains("drive.metadata.readonly"));
        assertTrue(url.contains("state=abc"));
    }

    @Test
    void completeLoginStoresAccount() throws Exception {
        User admin = new UserDao().create("boss", "boss@example.com", PasswordUtil.hash("password123"),
                Role.ADMIN, null);
        GoogleOAuthService service = newService();

        service.completeLogin(admin, "auth-code", "http://localhost/callback");

        assertTrue(lastTokenBody.get().contains("code=auth-code"));
        GoogleDriveAccount account = service.findAccount(admin).orElseThrow();
        assertEquals("owner@example.com", account.getGoogleEmail());
        assertEquals("ya29.access", service.accessToken(admin));
        assertTrue(service.isConnected(admin));

        service.disconnect(admin);
        assertFalse(service.isConnected(admin));
    }

    private GoogleOAuthService newService() {
        return new GoogleOAuthService("client-id", "client-secret", "",
                baseUrl + "/auth", baseUrl + "/token", baseUrl + "/userinfo",
                httpClient(), new GoogleDriveAccountDao());
    }

    private static HttpClient httpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }
}
