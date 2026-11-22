package com.examprep.service;

import com.examprep.config.AppConfig;
import com.examprep.model.GoogleDriveFile;
import com.examprep.model.GoogleDriveListing;
import com.examprep.util.SimpleJson;
import io.jsonwebtoken.Jwts;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lists files in one configured Drive folder using a service account.
 * Scope is metadata-readonly — this app does not download file bytes.
 */
public class GoogleDriveService {

    public static final int MAX_SELECTED = 20;
    public static final int MAX_LISTED = 200;
    public static final String ROOT_FOLDER_ID = "root";
    static final String DEFAULT_API_BASE = "https://www.googleapis.com/drive/v3";
    private static final Pattern PARENT_ID = Pattern.compile("\"parents\"\\s*:\\s*\\[\\s*\"([^\"]+)\"");
    private static final String SCOPE = "https://www.googleapis.com/auth/drive.metadata.readonly";
    private static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int PAGE_SIZE = 100;

    private final String folderId;
    private final String credentialsJson;
    private final HttpClient httpClient;
    private final String apiBase;
    private final Object tokenLock = new Object();
    private String cachedToken;
    private Instant tokenExpiresAt;

    public GoogleDriveService() {
        this(AppConfig.get("google.drive.folderId", ""),
                AppConfig.get("google.drive.serviceAccountJson", ""),
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build(),
                DEFAULT_API_BASE);
    }

    GoogleDriveService(String folderId, String credentialsJsonOrPath, HttpClient httpClient, String apiBase) {
        this.folderId = blankToEmpty(folderId);
        this.credentialsJson = loadCredentials(credentialsJsonOrPath);
        this.httpClient = httpClient;
        this.apiBase = blankToEmpty(apiBase).isEmpty() ? DEFAULT_API_BASE : apiBase.trim();
    }

    public boolean isConfigured() {
        return !folderId.isBlank() && !credentialsJson.isBlank();
    }

    public String folderId() {
        return folderId;
    }

    public List<GoogleDriveFile> listFolderFiles() {
        if (!isConfigured()) {
            return List.of();
        }
        try {
            return listChildren(serviceAccountAccessToken(), folderId, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Could not list Google Drive files");
        } catch (IOException e) {
            throw new IllegalStateException("Could not list Google Drive files");
        }
    }

    public GoogleDriveListing listFolder(String accessToken, String folderId) {
        String resolved = normalizeFolderId(folderId);
        try {
            GoogleDriveListing listing = new GoogleDriveListing();
            listing.setFolderId(resolved);
            if (ROOT_FOLDER_ID.equals(resolved)) {
                listing.setFolderName("My Drive");
                listing.setParentId(null);
            } else {
                applyFolderMetadata(accessToken, resolved, listing);
            }
            List<GoogleDriveFile> items = listChildren(accessToken, resolved, true);
            for (GoogleDriveFile item : items) {
                if (item.isFolder()) {
                    listing.getFolders().add(item);
                } else {
                    listing.getFiles().add(item);
                }
            }
            return listing;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Could not list Google Drive files");
        } catch (IOException e) {
            throw new IllegalStateException("Could not list Google Drive files");
        }
    }

    public List<GoogleDriveFile> resolveSelected(String accessToken, String folderId, String[] requestedIds) {
        if (requestedIds == null || requestedIds.length == 0) {
            return List.of();
        }
        if (requestedIds.length > MAX_SELECTED) {
            throw new IllegalArgumentException("Select at most 20 Drive files");
        }
        Map<String, GoogleDriveFile> byId = new LinkedHashMap<>();
        for (GoogleDriveFile file : listFolder(accessToken, folderId).getFiles()) {
            byId.put(file.getId(), file);
        }
        List<GoogleDriveFile> selected = new ArrayList<>();
        for (String rawId : requestedIds) {
            if (rawId == null || rawId.isBlank()) {
                continue;
            }
            GoogleDriveFile file = byId.get(rawId.trim());
            if (file == null) {
                throw new IllegalArgumentException("A selected Drive file is not in the configured folder");
            }
            selected.add(file);
        }
        return List.copyOf(selected);
    }

    public List<GoogleDriveFile> resolveSelected(String[] requestedIds) {
        if (requestedIds == null || requestedIds.length == 0) {
            return List.of();
        }
        if (requestedIds.length > MAX_SELECTED) {
            throw new IllegalArgumentException("Select at most 20 Drive files");
        }
        if (!isConfigured()) {
            throw new IllegalArgumentException("Google Drive is not configured");
        }
        Map<String, GoogleDriveFile> byId = new LinkedHashMap<>();
        for (GoogleDriveFile file : listFolderFiles()) {
            byId.put(file.getId(), file);
        }
        List<GoogleDriveFile> selected = new ArrayList<>();
        for (String rawId : requestedIds) {
            if (rawId == null || rawId.isBlank()) {
                continue;
            }
            GoogleDriveFile file = byId.get(rawId.trim());
            if (file == null) {
                throw new IllegalArgumentException("A selected Drive file is not in the configured folder");
            }
            selected.add(file);
        }
        return List.copyOf(selected);
    }

    static List<GoogleDriveFile> parseFiles(String json) {
        return parseItems(json, false);
    }

    static List<GoogleDriveFile> parseItems(String json, boolean includeFolders) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        int filesKey = json.indexOf("\"files\"");
        if (filesKey < 0) {
            return List.of();
        }
        int bracket = json.indexOf('[', filesKey);
        if (bracket < 0) {
            return List.of();
        }
        int end = matching(json, bracket, '[', ']');
        if (end < 0) {
            return List.of();
        }
        String array = json.substring(bracket + 1, end);
        List<GoogleDriveFile> files = new ArrayList<>();
        int i = 0;
        while (i < array.length()) {
            int objStart = array.indexOf('{', i);
            if (objStart < 0) {
                break;
            }
            int objEnd = matching(array, objStart, '{', '}');
            if (objEnd < 0) {
                break;
            }
            String obj = array.substring(objStart, objEnd + 1);
            SimpleJson.stringField(obj, "id").ifPresent(id -> {
                String mime = SimpleJson.stringField(obj, "mimeType").orElse("");
                if (!includeFolders && GoogleDriveFile.FOLDER_MIME.equals(mime)) {
                    return;
                }
                GoogleDriveFile file = new GoogleDriveFile();
                file.setId(id);
                file.setName(SimpleJson.stringField(obj, "name").orElse(id));
                file.setMimeType(mime);
                file.setModifiedTime(SimpleJson.stringField(obj, "modifiedTime").orElse(null));
                SimpleJson.stringField(obj, "size").ifPresent(size -> {
                    try {
                        file.setSizeBytes(Long.parseLong(size));
                    } catch (NumberFormatException ignored) {
                        // Drive sometimes omits size for Google-native docs
                    }
                });
                files.add(file);
            });
            i = objEnd + 1;
        }
        return files;
    }

    public static String normalizeFolderId(String folderId) {
        if (folderId == null || folderId.isBlank()) {
            return ROOT_FOLDER_ID;
        }
        String trimmed = folderId.trim();
        if (!trimmed.matches("[A-Za-z0-9_-]+")) {
            return ROOT_FOLDER_ID;
        }
        return trimmed;
    }

    private List<GoogleDriveFile> listChildren(String accessToken, String folderId, boolean includeFolders)
            throws IOException, InterruptedException {
        List<GoogleDriveFile> files = new ArrayList<>();
        String pageToken = null;
        do {
            String json = get(accessToken, listUrl(folderId, includeFolders, pageToken));
            files.addAll(parseItems(json, includeFolders));
            pageToken = SimpleJson.stringField(json, "nextPageToken").orElse(null);
        } while (pageToken != null && !pageToken.isBlank() && files.size() < MAX_LISTED);
        if (files.size() > MAX_LISTED) {
            return List.copyOf(files.subList(0, MAX_LISTED));
        }
        return List.copyOf(files);
    }

    private void applyFolderMetadata(String accessToken, String folderId, GoogleDriveListing listing)
            throws IOException, InterruptedException {
        String url = apiBase + "/files/" + enc(folderId)
                + "?fields=" + enc("id,name,parents")
                + "&supportsAllDrives=true";
        String json = get(accessToken, url);
        listing.setFolderName(SimpleJson.stringField(json, "name").orElse(folderId));
        Matcher parents = PARENT_ID.matcher(json);
        listing.setParentId(parents.find() ? parents.group(1) : ROOT_FOLDER_ID);
    }

    private String listUrl(String folderId, boolean includeFolders, String pageToken) {
        String safeFolder = folderId.replace("'", "\\'");
        String query = "'" + safeFolder + "' in parents and trashed = false";
        if (!includeFolders) {
            query += " and mimeType != '" + GoogleDriveFile.FOLDER_MIME + "'";
        }
        StringBuilder url = new StringBuilder(apiBase).append("/files?")
                .append("q=").append(enc(query))
                .append("&fields=").append(enc("nextPageToken,files(id,name,mimeType,modifiedTime,size)"))
                .append("&pageSize=").append(PAGE_SIZE)
                .append("&orderBy=").append(enc("folder,name"))
                .append("&supportsAllDrives=true")
                .append("&includeItemsFromAllDrives=true");
        if (pageToken != null && !pageToken.isBlank()) {
            url.append("&pageToken=").append(enc(pageToken));
        }
        return url.toString();
    }

    private String get(String accessToken, String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Drive list HTTP " + response.statusCode());
        }
        return response.body() == null ? "" : response.body();
    }

    private String serviceAccountAccessToken() throws IOException, InterruptedException {
        synchronized (tokenLock) {
            if (cachedToken != null && tokenExpiresAt != null && Instant.now().isBefore(tokenExpiresAt)) {
                return cachedToken;
            }
            String clientEmail = SimpleJson.stringField(credentialsJson, "client_email")
                    .orElseThrow(() -> new IOException("Service account JSON is missing client_email"));
            String privateKeyPem = SimpleJson.stringField(credentialsJson, "private_key")
                    .map(SimpleJson::unescapeJsonString)
                    .orElseThrow(() -> new IOException("Service account JSON is missing private_key"));
            String tokenUri = SimpleJson.stringField(credentialsJson, "token_uri").orElse(DEFAULT_TOKEN_URI);
            Instant now = Instant.now();
            String jwt = Jwts.builder()
                    .issuer(clientEmail)
                    .audience().add(tokenUri).and()
                    .claim("scope", SCOPE)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(3600)))
                    .signWith(parsePkcs8(privateKeyPem), Jwts.SIG.RS256)
                    .compact();
            String body = "grant_type=" + enc("urn:ietf:params:oauth:grant-type:jwt-bearer")
                    + "&assertion=" + enc(jwt);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUri))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Drive token HTTP " + response.statusCode());
            }
            String token = SimpleJson.stringField(response.body(), "access_token")
                    .orElseThrow(() -> new IOException("Drive token response is missing access_token"));
            int expiresIn = SimpleJson.intField(response.body(), "expires_in").orElse(3600);
            cachedToken = token;
            tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 60));
            return cachedToken;
        }
    }

    private static PrivateKey parsePkcs8(String pem) throws IOException {
        String cleaned = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        try {
            byte[] der = Base64.getDecoder().decode(cleaned);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IOException("Service account private key is invalid");
        }
    }

    private static String loadCredentials(String raw) {
        String value = blankToEmpty(raw);
        if (value.isEmpty()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("{")) {
            return trimmed;
        }
        try {
            return Files.readString(Path.of(trimmed));
        } catch (IOException e) {
            return trimmed;
        }
    }

    private static int matching(String text, int open, char openCh, char closeCh) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                    continue;
                }
                if (c == '\\') {
                    escape = true;
                    continue;
                }
                if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == openCh) {
                depth++;
            } else if (c == closeCh) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
