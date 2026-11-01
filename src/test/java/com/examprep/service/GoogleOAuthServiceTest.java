package com.examprep.service;

import com.examprep.dao.GoogleDriveAccountDao;
import com.examprep.dao.UserDao;
import com.examprep.model.GoogleDriveAccount;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.support.ConfigOverrides;
import com.examprep.support.DatabaseTestSupport;
import com.examprep.util.PasswordUtil;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleOAuthServiceTest extends DatabaseTestSupport {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> lastTokenBody = new AtomicReference<>();
    private final AtomicInteger tokenStatus = new AtomicInteger(200);
    private final AtomicReference<String> tokenJson = new AtomicReference<>(
            "{\"access_token\":\"ya29.access\",\"refresh_token\":\"1//refresh\",\"expires_in\":3600}");
    private final AtomicInteger userInfoStatus = new AtomicInteger(200);

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", exchange -> {
            lastTokenBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] ok = tokenJson.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(tokenStatus.get(), ok.length);
            exchange.getResponseBody().write(ok);
            exchange.close();
        });
        server.createContext("/userinfo", exchange -> {
            byte[] ok = "{\"email\":\"owner@example.com\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(userInfoStatus.get(), ok.length);
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

    @Test
    void defaultsBlankConfigAndRedirects() throws Exception {
        assertFalse(new GoogleOAuthService().isConfigured());
        GoogleOAuthService blankUrls = new GoogleOAuthService(" ", null, " https://app.example/callback ",
                "", "", " ", httpClient(), new GoogleDriveAccountDao());
        assertFalse(blankUrls.isConfigured());
        GoogleOAuthService configured = new GoogleOAuthService("id", "secret", " https://app.example/cb ",
                " ", " ", " ", httpClient(), new GoogleDriveAccountDao());
        assertTrue(configured.authorizationUrl("http://localhost/cb", "state").contains("accounts.google.com"));
        assertEquals("https://app.example/cb", configured.redirectUri(request("http://localhost:8080/admin/n8n",
                "/admin/n8n", "")));
        assertFalse(configured.newState().isBlank());
        assertTrue(configured.findAccount(null).isEmpty());
        User nameless = new User();
        assertTrue(configured.findAccount(nameless).isEmpty());
        assertFalse(configured.isConnected(nameless));
        GoogleOAuthService off = new GoogleOAuthService("", "secret", "", baseUrl, baseUrl, baseUrl,
                httpClient(), new GoogleDriveAccountDao());
        assertThrows(IllegalArgumentException.class, () -> off.authorizationUrl("http://cb", "s"));

        String previous = ConfigOverrides.set("app.public.url", "http://public.example/");
        try {
            GoogleOAuthService fromPublic = newService();
            assertEquals("http://public.example/admin/n8n/drive/callback",
                    fromPublic.redirectUri(request("http://localhost/ignored", "/ignored", "/app")));
        } finally {
            ConfigOverrides.restore("app.public.url", previous);
        }
        GoogleOAuthService fromRequest = newService();
        assertEquals("http://localhost:8080/app/admin/n8n/drive/callback",
                fromRequest.redirectUri(request("http://localhost:8080/app/admin/n8n", "/app/admin/n8n", "/app")));
    }

    @Test
    void loginRejectsBadInputAndKeepsExistingRefreshToken() throws Exception {
        User admin = new UserDao().create("boss", "boss@example.com", PasswordUtil.hash("password123"),
                Role.ADMIN, null);
        GoogleOAuthService service = newService();
        assertThrows(IllegalArgumentException.class, () -> service.completeLogin(null, "code", baseUrl));
        assertThrows(IllegalArgumentException.class, () -> service.completeLogin(new User(), "code", baseUrl));
        assertThrows(IllegalArgumentException.class, () -> service.completeLogin(admin, " ", baseUrl));
        assertThrows(IllegalArgumentException.class, () -> service.completeLogin(admin, null, baseUrl));
        GoogleOAuthService off = new GoogleOAuthService("", "", "", baseUrl + "/auth", baseUrl + "/token",
                baseUrl + "/userinfo", httpClient(), new GoogleDriveAccountDao());
        assertThrows(IllegalArgumentException.class, () -> off.completeLogin(admin, "code", baseUrl));
        off.disconnect(admin);

        tokenJson.set("{\"access_token\":\"ya29.access\",\"expires_in\":10}");
        assertThrows(IllegalStateException.class, () -> service.completeLogin(admin, "code", baseUrl));

        tokenJson.set("{\"access_token\":\"ya29.access\",\"refresh_token\":\"1//refresh\",\"expires_in\":10}");
        service.completeLogin(admin, "code", baseUrl);
        tokenJson.set("{\"access_token\":\"ya29.next\",\"expires_in\":3600}");
        userInfoStatus.set(404);
        service.completeLogin(admin, "code-2", baseUrl);
        GoogleDriveAccount account = service.findAccount(admin).orElseThrow();
        assertEquals("boss", account.getGoogleEmail());
        assertEquals("1//refresh", account.getRefreshToken());
        assertEquals("ya29.next", service.accessToken(admin));

        account.setAccessExpiresAt(LocalDateTime.now().minusMinutes(5));
        new GoogleDriveAccountDao().upsert(account);
        tokenJson.set("{\"access_token\":\"ya29.refreshed\",\"refresh_token\":\"1//new\",\"expires_in\":120}");
        assertEquals("ya29.refreshed", service.accessToken(admin));

        account = service.findAccount(admin).orElseThrow();
        account.setAccessExpiresAt(LocalDateTime.now().minusMinutes(5));
        account.setRefreshToken("1//new");
        new GoogleDriveAccountDao().upsert(account);
        tokenJson.set("{\"access_token\":\"ya29.again\"}");
        assertEquals("ya29.again", service.accessToken(admin));
        assertEquals("1//new", service.findAccount(admin).orElseThrow().getRefreshToken());

        tokenStatus.set(199);
        account = service.findAccount(admin).orElseThrow();
        account.setAccessExpiresAt(LocalDateTime.now().minusMinutes(5));
        new GoogleDriveAccountDao().upsert(account);
        assertThrows(IllegalStateException.class, () -> service.accessToken(admin));
        tokenStatus.set(200);
        HttpClient lowUserInfo = GoogleDriveServiceTest.scripted(
                uri -> uri.getPath().contains("userinfo") ? 150 : 200,
                uri -> uri.getPath().contains("userinfo")
                        ? ""
                        : "{\"access_token\":\"ya29.low-user\",\"refresh_token\":\"1//low\",\"expires_in\":3600}");
        GoogleOAuthService lowStatus = new GoogleOAuthService("client-id", "client-secret", "",
                baseUrl + "/auth", baseUrl + "/token", baseUrl + "/userinfo",
                lowUserInfo, new GoogleDriveAccountDao());
        assertDoesNotThrow(() -> lowStatus.completeLogin(admin, "low-status", baseUrl));
    }

    @Test
    void tokenAndNetworkFailuresBecomeStateErrors() throws Exception {
        User admin = new UserDao().create("boss", "boss@example.com", PasswordUtil.hash("password123"),
                Role.ADMIN, null);
        GoogleOAuthService service = newService();
        tokenStatus.set(500);
        assertThrows(IllegalStateException.class, () -> service.completeLogin(admin, "code", baseUrl));
        tokenStatus.set(200);
        tokenJson.set("{}");
        assertThrows(IllegalStateException.class, () -> service.completeLogin(admin, "code", baseUrl));

        tokenJson.set("{\"access_token\":\"ya29.access\",\"refresh_token\":\"1//refresh\",\"expires_in\":3600}");
        service.completeLogin(admin, "code", baseUrl);
        GoogleDriveAccount account = service.findAccount(admin).orElseThrow();
        account.setAccessExpiresAt(LocalDateTime.now().minusMinutes(5));
        new GoogleDriveAccountDao().upsert(account);

        Thread.currentThread().interrupt();
        try {
            assertThrows(IllegalStateException.class, () -> service.completeLogin(admin, "again", baseUrl));
        } finally {
            Thread.interrupted();
        }
        GoogleOAuthService refreshDown = new GoogleOAuthService("client-id", "client-secret", "",
                baseUrl + "/auth", "http://127.0.0.1:1/token", baseUrl + "/userinfo",
                httpClient(), new GoogleDriveAccountDao());
        GoogleDriveAccount stored = service.findAccount(admin).orElseThrow();
        stored.setAccessExpiresAt(LocalDateTime.now().minusMinutes(5));
        new GoogleDriveAccountDao().upsert(stored);
        GoogleDriveAccount copy = service.findAccount(admin).orElseThrow();
        new GoogleDriveAccountDao().upsert(copy);
        assertThrows(IllegalStateException.class, () -> refreshDown.accessToken(admin));
        GoogleOAuthService closed = new GoogleOAuthService("client-id", "client-secret", "",
                baseUrl + "/auth", "http://127.0.0.1:1/token", baseUrl + "/userinfo",
                httpClient(), new GoogleDriveAccountDao());
        assertThrows(IllegalStateException.class, () -> closed.completeLogin(admin, "code", baseUrl));
        assertThrows(IllegalArgumentException.class, () -> service.accessToken(new User()));
        User other = new User();
        other.setId(999_999L);
        assertThrows(IllegalArgumentException.class, () -> service.accessToken(other));

        GoogleDriveAccount expired = service.findAccount(admin).orElseThrow();
        expired.setAccessExpiresAt(LocalDateTime.now().minusMinutes(5));
        new GoogleDriveAccountDao().upsert(expired);
        GoogleOAuthService nullBody = new GoogleOAuthService("client-id", "client-secret", "",
                baseUrl + "/auth", baseUrl + "/token", baseUrl + "/userinfo",
                GoogleDriveServiceTest.nullBodyClient(), new GoogleDriveAccountDao());
        assertThrows(IllegalStateException.class, () -> nullBody.accessToken(admin));
        GoogleOAuthService lowStatus = new GoogleOAuthService("client-id", "client-secret", "",
                baseUrl + "/auth", baseUrl + "/token", baseUrl + "/userinfo",
                GoogleDriveServiceTest.scripted(uri -> 150, uri -> ""),
                new GoogleDriveAccountDao());
        assertThrows(IllegalStateException.class, () -> lowStatus.accessToken(admin));

        GoogleDriveAccount openEnded = new GoogleDriveAccount();
        openEnded.setAdminUserId(admin.getId());
        openEnded.setAccessToken("old");
        openEnded.setRefreshToken("1//open");
        GoogleOAuthService missingExpiry = new GoogleOAuthService("client-id", "client-secret", "",
                baseUrl + "/auth", baseUrl + "/token", baseUrl + "/userinfo",
                httpClient(), new GoogleDriveAccountDao() {
                    @Override
                    public java.util.Optional<GoogleDriveAccount> findByAdminUserId(Long adminUserId) {
                        return java.util.Optional.of(openEnded);
                    }

                    @Override
                    public void upsert(GoogleDriveAccount account) {
                    }
                });
        tokenJson.set("{\"access_token\":\"ya29.null-expiry\",\"expires_in\":3600}");
        assertEquals("ya29.null-expiry", missingExpiry.accessToken(admin));
    }

    private GoogleOAuthService newService() {
        return new GoogleOAuthService("client-id", "client-secret", "",
                baseUrl + "/auth", baseUrl + "/token", baseUrl + "/userinfo",
                httpClient(), new GoogleDriveAccountDao());
    }

    private static HttpClient httpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    private static HttpServletRequest request(String url, String uri, String contextPath) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRequestURL" -> new StringBuffer(url);
                    case "getRequestURI" -> uri;
                    case "getContextPath" -> contextPath;
                    case "toString" -> "request";
                    case "hashCode" -> 1;
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
