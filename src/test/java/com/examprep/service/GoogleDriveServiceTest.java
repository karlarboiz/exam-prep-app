package com.examprep.service;

import com.examprep.model.GoogleDriveFile;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
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
    private final AtomicReference<String> listBody = new AtomicReference<>(
            "{\"files\":[{\"id\":\"file-1\",\"name\":\"constitution.pdf\",\"mimeType\":\"application/pdf\"},"
                    + "{\"id\":\"folder-1\",\"name\":\"Nested\",\"mimeType\":\"application/vnd.google-apps.folder\"},"
                    + "{\"id\":\"file-2\",\"name\":\"notes.docx\",\"mimeType\":\"application/vnd.openxmlformats-officedocument.wordprocessingml.document\"}]}");

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", exchange -> {
            byte[] ok = "{\"access_token\":\"test-token\",\"expires_in\":3600}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, ok.length);
            exchange.getResponseBody().write(ok);
            exchange.close();
        });
        server.createContext("/files", exchange -> {
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = listBody.get().getBytes(StandardCharsets.UTF_8);
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

    private GoogleDriveService newService() {
        return new GoogleDriveService("folder-abc", credentialsJson, httpClient(), baseUrl);
    }

    private static HttpClient httpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    private static KeyPair testKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
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
