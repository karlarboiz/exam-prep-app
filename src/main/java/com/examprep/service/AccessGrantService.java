package com.examprep.service;

import com.examprep.config.DatabaseManager;
import com.examprep.dao.AccessGrantDao;
import com.examprep.dao.UserDao;
import com.examprep.model.AccessGrant;
import com.examprep.model.AccessGrantStatus;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.util.PasswordUtil;
import com.examprep.util.TokenHashUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class AccessGrantService {

    private final AccessGrantDao accessGrantDao = new AccessGrantDao();
    private final UserDao userDao = new UserDao();

    public record CreatedAccessToken(String rawToken, AccessGrant grant) {
    }

    public CreatedAccessToken createToken(LocalDateTime expiresAt, Integer durationDays,
                                          String planCode, String sourceRef) throws SQLException {
        LocalDateTime expiry = resolveExpiry(expiresAt, durationDays);
        if (!expiry.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expiration must be in the future");
        }
        String rawToken = TokenHashUtil.generateRawToken();
        String hash = TokenHashUtil.sha256(rawToken);
        AccessGrant grant = accessGrantDao.create(hash, expiry, planCode, sourceRef);
        return new CreatedAccessToken(rawToken, grant);
    }

    public AccessGrant requireUnusedToken(String rawToken) throws SQLException {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Access token is required");
        }
        String hash = TokenHashUtil.sha256(rawToken.trim());
        AccessGrant grant = accessGrantDao.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid access token"));
        if (grant.getStatus() == AccessGrantStatus.REVOKED) {
            throw new IllegalArgumentException("This access token has been revoked");
        }
        if (grant.getStatus() == AccessGrantStatus.REDEEMED) {
            throw new IllegalArgumentException("This access token has already been used");
        }
        if (!grant.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("This access token has expired");
        }
        return grant;
    }

    public User registerWithToken(String rawToken, String username, String email, String password)
            throws SQLException {
        AccessGrant grant = requireUnusedToken(rawToken);

        try (Connection conn = DatabaseManager.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                if (userDao.findByUsername(conn, username).isPresent()) {
                    throw new IllegalArgumentException("Username already exists");
                }
                if (userDao.findByEmail(conn, email).isPresent()) {
                    throw new IllegalArgumentException("Email already exists");
                }
                String hash = PasswordUtil.hash(password);
                User user = userDao.create(conn, username, email, hash, Role.USER);
                accessGrantDao.redeem(conn, grant.getId(), user.getId());
                conn.commit();
                return user;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public boolean hasActiveAccess(Long userId) throws SQLException {
        return accessGrantDao.findActiveByUserId(userId).isPresent();
    }

    public Optional<AccessGrant> findLatestRedeemed(Long userId) throws SQLException {
        return accessGrantDao.findLatestRedeemedByUserId(userId);
    }

    private LocalDateTime resolveExpiry(LocalDateTime expiresAt, Integer durationDays) {
        if (expiresAt != null) {
            return expiresAt.truncatedTo(ChronoUnit.SECONDS);
        }
        if (durationDays != null && durationDays > 0) {
            return LocalDateTime.now().plusDays(durationDays).truncatedTo(ChronoUnit.SECONDS);
        }
        throw new IllegalArgumentException("Provide expiresAt or durationDays");
    }
}
