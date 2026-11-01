package com.examprep.service;

import com.examprep.model.GoogleDriveFile;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleDriveServiceTest {

    private HttpServer server;
    private String baseUrl;
    private String credentialsJson;
    private final AtomicReference<String> lastAuth = new AtomicReference<>();
    private final AtomicInteger listStatus = new AtomicInteger(200);
    private final AtomicInteger tokenStatus = new AtomicInteger(200);
    private final AtomicReference<String> tokenBody = new AtomicReference<>(
            "{\"access_token\":\"test-token\",\"expires_in\":3600}");
    private final AtomicInteger tokenHits = new AtomicInteger();
    private final AtomicReference<String> listBody = new AtomicReference<>(
            "{\"files\":[{\"id\":\"file-1\",\"name\":\"constitution.pdf\",\"mimeType\":\"application/pdf\"},"
                    + "{\"id\":\"folder-1\",\"name\":\"Nested\",\"mimeType\":\"application/vnd.google-apps.folder\"},"
                    + "{\"id\":\"file-2\",\"name\":\"notes.docx\",\"mimeType\":\"application/vnd.openxmlformats-officedocument.wordprocessingml.document\"}]}");

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", exchange -> {
            tokenHits.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            byte[] ok = tokenBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(tokenStatus.get(), ok.length);
            exchange.getResponseBody().write(ok);
            exchange.close();
        });
        server.createContext("/files", exchange -> {
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getRawQuery() == null ? "" : exchange.getRequestURI().getRawQuery();
            String payload;
            if (path.contains("/files/folder-1")) {
                payload = "{\"id\":\"folder-1\",\"name\":\"Nested\",\"parents\":[\"parent-9\"]}";
            } else if (path.contains("/files/orphan")) {
                payload = "{\"id\":\"orphan\",\"name\":\"Orphan\"}";
            } else if ("page".equals(listBody.get())) {
                payload = query.contains("pageToken") ? filesJson(100, 150, "more") : filesJson(150, 0, "next");
            } else if ("blank-page".equals(listBody.get())) {
                payload = "{\"files\":[{\"id\":\"only\",\"name\":\"One\",\"mimeType\":\"text/plain\"}],\"nextPageToken\":\"\"}";
            } else {
                payload = listBody.get();
            }
            byte[] body = payload.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(listStatus.get(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        credentialsJson = serviceAccountJson(testKeyPair(), baseUrl + "/token");
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void notConfiguredWhenFolderOrJsonBlank() {
        HttpClient client = httpClient();
        assertFalse(new GoogleDriveService("", credentialsJson, client, baseUrl).isConfigured());
        assertFalse(new GoogleDriveService("folder-id", "", client, baseUrl).isConfigured());
    }

    @Test
    void parseFilesSkipsFolders() {
        List<GoogleDriveFile> files = GoogleDriveService.parseFiles(listBody.get());
        assertEquals(2, files.size());
        assertEquals("file-1", files.get(0).getId());
        assertEquals("PDF", files.get(0).getKindLabel());
        assertEquals("file-2", files.get(1).getId());
        assertEquals("Word", files.get(1).getKindLabel());
    }

    @Test
    void parseItemsKeepsFoldersWhenAsked() {
        List<GoogleDriveFile> items = GoogleDriveService.parseItems(listBody.get(), true);
        assertEquals(3, items.size());
        assertTrue(items.stream().anyMatch(GoogleDriveFile::isFolder));
    }

    @Test
    void listFolderFilesUsesBearerToken() {
        GoogleDriveService service = newService();
        List<GoogleDriveFile> files = service.listFolderFiles();
        assertEquals(2, files.size());
        assertEquals("Bearer test-token", lastAuth.get());
        assertEquals("constitution.pdf", files.get(0).getName());
    }

    @Test
    void resolveSelectedReturnsMatchingFiles() {
        GoogleDriveService service = newService();
        List<GoogleDriveFile> selected = service.resolveSelected(new String[] {"file-2", "file-1"});
        assertEquals(2, selected.size());
        assertEquals("file-2", selected.get(0).getId());
        assertEquals("file-1", selected.get(1).getId());
    }

    @Test
    void resolveSelectedRejectsUnknownId() {
        GoogleDriveService service = newService();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.resolveSelected(new String[] {"missing"}));
        assertTrue(ex.getMessage().contains("not in the configured folder"));
    }

    @Test
    void resolveSelectedRejectsTooMany() {
        GoogleDriveService service = newService();
        String[] ids = new String[GoogleDriveService.MAX_SELECTED + 1];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = "file-" + i;
        }
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.resolveSelected(ids));
        assertTrue(ex.getMessage().contains("at most 20"));
    }

    @Test
    void listFailureIsStateError() {
        listStatus.set(403);
        GoogleDriveService service = newService();
        IllegalStateException ex = assertThrows(IllegalStateException.class, service::listFolderFiles);
        assertTrue(ex.getMessage().contains("Could not list"));
    }

    @Test
    void parsesEdgesAndNormalizesFolderIds() {
        assertTrue(GoogleDriveService.parseItems(null, true).isEmpty());
        assertTrue(GoogleDriveService.parseItems("  ", false).isEmpty());
        assertTrue(GoogleDriveService.parseItems("{\"kind\":\"drive\"}", true).isEmpty());
        assertTrue(GoogleDriveService.parseItems("{\"files\":1}", true).isEmpty());
        assertTrue(GoogleDriveService.parseItems("{\"files\":[{\"id\":\"x\"}", true).isEmpty());
        assertTrue(GoogleDriveService.parseItems("{\"files\":[1]}", true).isEmpty());
        assertTrue(GoogleDriveService.parseItems("{\"files\":[{\"id\":\"x\"]}", true).isEmpty());
        assertEquals(1, GoogleDriveService.parseItems(
                "{\"files\":[{\"id\":\"nest\",\"meta\":{\"a\":1}}]}", false).size());
        assertTrue(GoogleDriveService.parseItems("{\"files\":[{\"id\":\"ok\"} trailing", false).isEmpty()
                || GoogleDriveService.parseItems("{\"files\":[{\"id\":\"ok\"} trailing", false).size() <= 1);
        List<GoogleDriveFile> parsed = GoogleDriveService.parseItems(
                "{\"files\":[{\"id\":\"a\\\"b\",\"name\":\"n}ame\",\"mimeType\":\"text/plain\",\"size\":\"nope\"},"
                        + "{\"mimeType\":\"text/plain\"},{\"id\":\"plain\",\"size\":\"12\"}]}",
                true);
        assertEquals(2, parsed.size());
        assertEquals("plain", parsed.get(1).getName());
        assertEquals(12L, parsed.get(1).getSizeBytes());
        assertEquals("root", GoogleDriveService.normalizeFolderId(null));
        assertEquals("root", GoogleDriveService.normalizeFolderId(" bad id "));
        assertEquals("folder_1", GoogleDriveService.normalizeFolderId(" folder_1 "));
        assertTrue(new GoogleDriveService().listFolderFiles().isEmpty());
        assertFalse(new GoogleDriveService(null, null, httpClient(), "").isConfigured());
    }

    @Test
    void listsRootAndNestedFoldersAndCapsPages() throws Exception {
        GoogleDriveService service = newService();
        var root = service.listFolder("user-token", " ");
        assertEquals("My Drive", root.getFolderName());
        assertEquals("root", root.getFolderId());
        assertFalse(root.getFolders().isEmpty());

        var nested = service.listFolder("user-token", "folder-1");
        assertEquals("Nested", nested.getFolderName());
        assertEquals("parent-9", nested.getParentId());
        var orphan = service.listFolder("user-token", "orphan");
        assertEquals("root", orphan.getParentId());
        assertTrue(service.resolveSelected("user-token", "root", null).isEmpty());
        assertTrue(service.resolveSelected("user-token", "root", new String[0]).isEmpty());
        assertEquals(1, service.resolveSelected("user-token", "root", new String[] {" ", "file-1"}).size());
        assertThrows(IllegalArgumentException.class, () ->
                service.resolveSelected("user-token", "root", new String[] {"missing"}));
        String[] tooMany = new String[GoogleDriveService.MAX_SELECTED + 1];
        java.util.Arrays.fill(tooMany, "file-1");
        assertThrows(IllegalArgumentException.class, () -> service.resolveSelected("user-token", "root", tooMany));
        assertTrue(service.resolveSelected((String[]) null).isEmpty());
        assertTrue(service.resolveSelected(new String[0]).isEmpty());
        assertEquals(1, service.resolveSelected(new String[] {null, "file-1"}).size());
        assertThrows(IllegalArgumentException.class, () ->
                new GoogleDriveService("", "", httpClient(), baseUrl).resolveSelected(new String[] {"file-1"}));
        assertTrue(service.resolveSelected(new String[] {" ", "file-1"}).stream()
                .anyMatch(file -> "file-1".equals(file.getId())));

        listBody.set("page");
        GoogleDriveService paging = new GoogleDriveService("o'brien", credentialsJson, httpClient(), baseUrl);
        assertEquals(GoogleDriveService.MAX_LISTED, paging.listFolderFiles().size());
        int tokensAfterFirstList = tokenHits.get();
        assertTrue(tokensAfterFirstList >= 1);
        assertEquals(GoogleDriveService.MAX_LISTED, paging.listFolderFiles().size());
        assertEquals(tokensAfterFirstList, tokenHits.get());

        java.lang.reflect.Field expires = GoogleDriveService.class.getDeclaredField("tokenExpiresAt");
        expires.setAccessible(true);
        expires.set(paging, java.time.Instant.EPOCH);
        assertEquals(GoogleDriveService.MAX_LISTED, paging.listFolderFiles().size());
        expires.set(paging, null);
        assertEquals(GoogleDriveService.MAX_LISTED, paging.listFolderFiles().size());

        listBody.set("blank-page");
        GoogleDriveService listed = new GoogleDriveService("folder", credentialsJson, httpClient(), baseUrl);
        assertEquals(1, listed.listFolderFiles().size());
        java.lang.reflect.Method listUrl = GoogleDriveService.class.getDeclaredMethod(
                "listUrl", String.class, boolean.class, String.class);
        listUrl.setAccessible(true);
        assertTrue(listUrl.invoke(listed, "root", false, null).toString().contains("pageSize"));
        assertTrue(listUrl.invoke(listed, "root", false, "").toString().contains("pageSize"));
        assertTrue(listUrl.invoke(listed, "root", false, "next").toString().contains("pageToken"));

        GoogleDriveService nullBody = new GoogleDriveService("folder", credentialsJson, nullBodyClient(), baseUrl);
        assertTrue(nullBody.listFolder("token", "root").getFiles().isEmpty());
        assertThrows(IllegalArgumentException.class, () ->
                nullBody.resolveSelected("token", "root", new String[] {null, " ", "missing"}));

        HttpClient lowStatus = scripted(uri -> 150, uri -> "");
        assertThrows(IllegalStateException.class, () ->
                new GoogleDriveService("folder", credentialsJson, lowStatus, baseUrl).listFolder("tok", "root"));
        assertThrows(IllegalStateException.class, () ->
                new GoogleDriveService("folder", credentialsJson, lowStatus, baseUrl).listFolderFiles());
    }

    @Test
    void credentialFailuresAndInterrupts() throws Exception {
        assertThrows(IllegalStateException.class, () ->
                new GoogleDriveService("folder", "{\"private_key\":\"x\"}", httpClient(), baseUrl).listFolderFiles());
        assertThrows(IllegalStateException.class, () ->
                new GoogleDriveService("folder", "{\"client_email\":\"a@b.c\"}", httpClient(), baseUrl).listFolderFiles());
        assertThrows(IllegalStateException.class, () ->
                new GoogleDriveService("folder", serviceAccountJson(testKeyPair(), baseUrl + "/token")
                        .replace("BEGIN PRIVATE KEY", "BEGIN RSA"), httpClient(), baseUrl).listFolderFiles());
        tokenStatus.set(401);
        assertThrows(IllegalStateException.class, newService()::listFolderFiles);
        tokenStatus.set(200);
        tokenBody.set("{}");
        assertThrows(IllegalStateException.class, newService()::listFolderFiles);
        tokenBody.set("{\"access_token\":\"test-token\"}");
        assertFalse(newService().listFolderFiles().isEmpty());

        java.nio.file.Path file = java.nio.file.Files.createTempFile("drive", ".json");
        java.nio.file.Files.writeString(file, credentialsJson);
        assertTrue(new GoogleDriveService("folder", file.toString(), httpClient(), "  ").isConfigured());
        assertTrue(new GoogleDriveService("folder", "/missing/drive.json", httpClient(), baseUrl).isConfigured());

        listStatus.set(500);
        assertThrows(IllegalStateException.class, () -> newService().listFolder("tok", "folder-1"));
        Thread.currentThread().interrupt();
        try {
            assertThrows(IllegalStateException.class, () -> newService().listFolderFiles());
            assertThrows(IllegalStateException.class, () -> newService().listFolder("tok", "root"));
        } finally {
            Thread.interrupted();
        }
    }

    private GoogleDriveService newService() {
        return new GoogleDriveService("folder-abc", credentialsJson, httpClient(), baseUrl);
    }

    private static HttpClient httpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    static HttpClient nullBodyClient() {
        return scripted(uri -> 200, uri -> null);
    }

    static HttpClient scripted(java.util.function.Function<java.net.URI, Integer> statusFor,
                               java.util.function.Function<java.net.URI, String> bodyFor) {
        return new HttpClient() {
            @Override
            public Optional<CookieHandler> cookieHandler() {
                return Optional.empty();
            }

            @Override
            public Optional<Duration> connectTimeout() {
                return Optional.of(Duration.ofSeconds(1));
            }

            @Override
            public Redirect followRedirects() {
                return Redirect.NEVER;
            }

            @Override
            public Optional<ProxySelector> proxy() {
                return Optional.empty();
            }

            @Override
            public SSLContext sslContext() {
                try {
                    return SSLContext.getDefault();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public SSLParameters sslParameters() {
                return new SSLParameters();
            }

            @Override
            public Optional<Authenticator> authenticator() {
                return Optional.empty();
            }

            @Override
            public Version version() {
                return Version.HTTP_1_1;
            }

            @Override
            public Optional<java.util.concurrent.Executor> executor() {
                return Optional.empty();
            }

            @Override
            public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
                return empty(request);
            }

            @Override
            public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
                return CompletableFuture.completedFuture(empty(request));
            }

            @Override
            public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler,
                    HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
                return sendAsync(request, responseBodyHandler);
            }

            private <T> HttpResponse<T> empty(HttpRequest request) {
                return new HttpResponse<>() {
                    @Override
                    public int statusCode() {
                        return statusFor.apply(request.uri());
                    }

                    @Override
                    public HttpRequest request() {
                        return request;
                    }

                    @Override
                    public Optional<HttpResponse<T>> previousResponse() {
                        return Optional.empty();
                    }

                    @Override
                    public HttpHeaders headers() {
                        return HttpHeaders.of(Map.of(), (a, b) -> true);
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public T body() {
                        return (T) bodyFor.apply(request.uri());
                    }

                    @Override
                    public Optional<javax.net.ssl.SSLSession> sslSession() {
                        return Optional.empty();
                    }

                    @Override
                    public java.net.URI uri() {
                        return request.uri();
                    }

                    @Override
                    public Version version() {
                        return Version.HTTP_1_1;
                    }
                };
            }
        };
    }

    private static KeyPair testKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String filesJson(int count, int idOffset, String nextPage) {
        StringBuilder files = new StringBuilder("{\"files\":[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                files.append(',');
            }
            files.append("{\"id\":\"p").append(idOffset + i).append("\",\"name\":\"n\",\"mimeType\":\"text/plain\"}");
        }
        files.append(']');
        if (nextPage != null) {
            files.append(",\"nextPageToken\":\"").append(nextPage).append('"');
        }
        files.append('}');
        return files.toString();
    }

    private static String serviceAccountJson(KeyPair pair, String tokenUri) {
        String pem = "-----BEGIN PRIVATE KEY-----\\n"
                + Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())
                + "\\n-----END PRIVATE KEY-----\\n";
        return "{\"client_email\":\"drive@test.iam.gserviceaccount.com\","
                + "\"private_key\":\"" + pem + "\","
                + "\"token_uri\":\"" + tokenUri + "\"}";
    }
}
