package com.examprep.service;

import com.examprep.config.AppConfig;
import com.examprep.dao.GoogleDriveAccountDao;
import com.examprep.model.GoogleDriveAccount;
import com.examprep.model.User;
import com.examprep.util.SimpleJson;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Admin Google OAuth for Drive listing. Tokens stay per admin in H2.
 */
public class GoogleOAuthService {

    public static final String STATE_ATTR = "google_oauth_state";
    public static final String CALLBACK_PATH = "/admin/n8n/drive/callback";
    static final String DEFAULT_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    static final String DEFAULT_TOKEN_URL = "https://oauth2.googleapis.com/token";
    static final String DEFAULT_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String SCOPE = "https://www.googleapis.com/auth/drive.metadata.readonly"
            + " https://www.googleapis.com/auth/userinfo.email";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String clientId;
    private final String clientSecret;
    private final String configuredRedirectUri;
    private final String authUrl;
    private final String tokenUrl;
    private final String userInfoUrl;
    private final HttpClient httpClient;
    private final GoogleDriveAccountDao accountDao;

    public GoogleOAuthService() {
        this(AppConfig.get("google.oauth.clientId", ""),
                AppConfig.get("google.oauth.clientSecret", ""),
                AppConfig.get("google.oauth.redirectUri", ""),
                DEFAULT_AUTH_URL,
                DEFAULT_TOKEN_URL,
                DEFAULT_USERINFO_URL,
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build(),
                new GoogleDriveAccountDao());
    }

    GoogleOAuthService(String clientId, String clientSecret, String configuredRedirectUri,
                       String authUrl, String tokenUrl, String userInfoUrl,
                       HttpClient httpClient, GoogleDriveAccountDao accountDao) {
        this.clientId = blankToEmpty(clientId);
        this.clientSecret = blankToEmpty(clientSecret);
        this.configuredRedirectUri = blankToEmpty(configuredRedirectUri);
        this.authUrl = blankToEmpty(authUrl).isEmpty() ? DEFAULT_AUTH_URL : authUrl.trim();
        this.tokenUrl = blankToEmpty(tokenUrl).isEmpty() ? DEFAULT_TOKEN_URL : tokenUrl.trim();
        this.userInfoUrl = blankToEmpty(userInfoUrl).isEmpty() ? DEFAULT_USERINFO_URL : userInfoUrl.trim();
        this.httpClient = httpClient;
        this.accountDao = accountDao;
    }

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    public Optional<GoogleDriveAccount> findAccount(User admin) throws SQLException {
        if (admin == null || admin.getId() == null) {
            return Optional.empty();
        }
        return accountDao.findByAdminUserId(admin.getId());
    }

    public boolean isConnected(User admin) throws SQLException {
        return findAccount(admin).isPresent();
    }

    public String newState() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String authorizationUrl(String redirectUri, String state) {
        if (!isConfigured()) {
            throw new IllegalArgumentException("Google Drive sign-in is not configured");
        }
        return authUrl
                + "?client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&response_type=code"
                + "&scope=" + enc(SCOPE)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + enc(state);
    }

    public String redirectUri(HttpServletRequest req) {
        if (!configuredRedirectUri.isBlank()) {
            return configuredRedirectUri;
        }
        String publicUrl = AppConfig.get("app.public.url", "").trim();
        if (!publicUrl.isBlank()) {
            return publicUrl.replaceAll("/$", "") + CALLBACK_PATH;
        }
        StringBuffer url = req.getRequestURL();
        String uri = req.getRequestURI();
        String base = url.substring(0, url.length() - uri.length());
        return base + req.getContextPath() + CALLBACK_PATH;
    }

    public void completeLogin(User admin, String code, String redirectUri) throws SQLException {
        requireAdmin(admin);
        if (!isConfigured()) {
            throw new IllegalArgumentException("Google Drive sign-in is not configured");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Google did not return an authorization code");
        }
        try {
            TokenResponse tokens = exchangeCode(code.trim(), redirectUri);
            String email = fetchEmail(tokens.accessToken);
            Optional<GoogleDriveAccount> existing = accountDao.findByAdminUserId(admin.getId());
            String refresh = tokens.refreshToken;
            if ((refresh == null || refresh.isBlank()) && existing.isPresent()) {
                refresh = existing.get().getRefreshToken();
            }
            if (refresh == null || refresh.isBlank()) {
                throw new IllegalStateException("Google Drive sign-in did not return a refresh token");
            }
            GoogleDriveAccount account = new GoogleDriveAccount();
            account.setAdminUserId(admin.getId());
            account.setGoogleEmail(email == null || email.isBlank() ? admin.getUsername() : email);
            account.setAccessToken(tokens.accessToken);
            account.setRefreshToken(refresh);
            account.setAccessExpiresAt(LocalDateTime.now().plusSeconds(Math.max(60, tokens.expiresIn - 60)));
            accountDao.upsert(account);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Google Drive sign-in failed");
        } catch (IOException e) {
            throw new IllegalStateException("Google Drive sign-in failed");
        }
    }

    public void disconnect(User admin) throws SQLException {
        requireAdmin(admin);
        accountDao.deleteByAdminUserId(admin.getId());
    }

    public String accessToken(User admin) throws SQLException {
        requireAdmin(admin);
        GoogleDriveAccount account = accountDao.findByAdminUserId(admin.getId())
                .orElseThrow(() -> new IllegalArgumentException("Connect Google Drive first"));
        if (account.getAccessExpiresAt() != null
                && account.getAccessExpiresAt().isAfter(LocalDateTime.now().plusSeconds(30))) {
            return account.getAccessToken();
        }
        try {
            TokenResponse tokens = refresh(account.getRefreshToken());
            account.setAccessToken(tokens.accessToken);
            if (tokens.refreshToken != null && !tokens.refreshToken.isBlank()) {
                account.setRefreshToken(tokens.refreshToken);
            }
            account.setAccessExpiresAt(LocalDateTime.now().plusSeconds(Math.max(60, tokens.expiresIn - 60)));
            accountDao.upsert(account);
            return account.getAccessToken();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Could not list Google Drive files");
        } catch (IOException e) {
            throw new IllegalStateException("Could not list Google Drive files");
        }
    }

    private TokenResponse exchangeCode(String code, String redirectUri) throws IOException, InterruptedException {
        String body = "grant_type=authorization_code"
                + "&code=" + enc(code)
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret)
                + "&redirect_uri=" + enc(redirectUri);
        return postToken(body);
    }

    private TokenResponse refresh(String refreshToken) throws IOException, InterruptedException {
        String body = "grant_type=refresh_token"
                + "&refresh_token=" + enc(refreshToken)
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret);
        return postToken(body);
    }

    private TokenResponse postToken(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Google token HTTP " + response.statusCode());
        }
        String json = response.body() == null ? "" : response.body();
        String access = SimpleJson.stringField(json, "access_token")
                .orElseThrow(() -> new IOException("Google token response is missing access_token"));
        String refresh = SimpleJson.stringField(json, "refresh_token").orElse("");
        int expiresIn = SimpleJson.intField(json, "expires_in").orElse(3600);
        return new TokenResponse(access, refresh, expiresIn);
    }

    private String fetchEmail(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(userInfoUrl))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return "";
        }
        return SimpleJson.stringField(response.body(), "email").orElse("");
    }

    private static void requireAdmin(User admin) {
        if (admin == null || admin.getId() == null) {
            throw new IllegalArgumentException("Admin user is required");
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private record TokenResponse(String accessToken, String refreshToken, int expiresIn) {
    }
}
