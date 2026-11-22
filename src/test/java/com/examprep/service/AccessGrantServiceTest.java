package com.examprep.service;

import com.examprep.model.AccessGrant;
import com.examprep.model.AccessGrantStatus;
import com.examprep.model.ExamLevel;
import com.examprep.model.User;
import com.examprep.support.DatabaseTestSupport;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessGrantServiceTest extends DatabaseTestSupport {

    private final AccessGrantService service = new AccessGrantService();

    @Test
    void createRedeemAndActiveAccess() throws Exception {
        AccessGrantService.CreatedAccessToken created = service.createToken(
                null, 3, "review", "order-1", ExamLevel.PROFESSIONAL);

        assertEquals(AccessGrantStatus.UNUSED, created.grant().getStatus());
        assertEquals(ExamLevel.PROFESSIONAL, created.grant().getExamLevel());

        User user = service.registerWithToken(
                created.rawToken(), "alice", "alice@example.com", "password123");

        assertTrue(service.hasActiveAccess(user.getId()));
        assertEquals(ExamLevel.PROFESSIONAL, user.getExamLevel());
        assertThrows(IllegalArgumentException.class, () ->
                service.registerWithToken(created.rawToken(), "bob", "bob@example.com", "password123"));
    }

    @Test
    void revokeUnusedTokenBlocksRedeem() throws Exception {
        AccessGrantService.CreatedAccessToken created = service.createToken(
                null, 3, null, null, ExamLevel.SUB_PROFESSIONAL);

        AccessGrant revoked = service.revoke(created.grant().getId());
        assertEquals(AccessGrantStatus.REVOKED, revoked.getStatus());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.registerWithToken(created.rawToken(), "carol", "carol@example.com", "password123"));
        assertTrue(ex.getMessage().toLowerCase().contains("revoked"));
    }

    @Test
    void revokeRedeemedGrantEndsAccessImmediately() throws Exception {
        AccessGrantService.CreatedAccessToken created = service.createToken(
                LocalDateTime.now().plusDays(10), null, null, null, ExamLevel.PROFESSIONAL);
        User user = service.registerWithToken(
                created.rawToken(), "dave", "dave@example.com", "password123");
        assertTrue(service.hasActiveAccess(user.getId()));

        assertTrue(service.findLatestRedeemed(user.getId()).isPresent());
        assertFalse(service.listAll().isEmpty());
        service.revoke(created.grant().getId());
        assertFalse(service.hasActiveAccess(user.getId()));

        assertThrows(IllegalArgumentException.class, () -> service.revoke(created.grant().getId()));
    }

    @Test
    void rejectsMissingExpiryBlankTokensAndConflicts() throws Exception {
        assertThrows(IllegalArgumentException.class, () ->
                service.createToken(null, null, null, null, ExamLevel.PROFESSIONAL));
        assertThrows(IllegalArgumentException.class, () ->
                service.createToken(null, 0, null, null, ExamLevel.PROFESSIONAL));
        assertThrows(IllegalArgumentException.class, () ->
                service.createToken(LocalDateTime.now().minusDays(1), null, null, null, ExamLevel.PROFESSIONAL));
        assertThrows(IllegalArgumentException.class, () ->
                service.createToken(LocalDateTime.now().plusDays(2), null, null, null, null));

        assertThrows(IllegalArgumentException.class, () -> service.requireUnusedToken(null));
        assertThrows(IllegalArgumentException.class, () -> service.requireUnusedToken("  "));
        assertThrows(IllegalArgumentException.class, () -> service.requireUnusedToken("not-a-real-token"));

        AccessGrantService.CreatedAccessToken created = service.createToken(
                null, 5, "plan", "ref", ExamLevel.SUB_PROFESSIONAL);
        new com.examprep.dao.AccessGrantDao().updateExpiresAt(
                created.grant().getId(), LocalDateTime.now().minusMinutes(1));
        IllegalArgumentException expired = assertThrows(IllegalArgumentException.class, () ->
                service.registerWithToken(created.rawToken(), "erin", "erin@example.com", "password123"));
        assertTrue(expired.getMessage().toLowerCase().contains("expired"));

        AccessGrantService.CreatedAccessToken first = service.createToken(
                null, 4, null, null, ExamLevel.PROFESSIONAL);
        service.registerWithToken(first.rawToken(), "frank", "frank@example.com", "password123");
        AccessGrantService.CreatedAccessToken second = service.createToken(
                null, 4, null, null, ExamLevel.PROFESSIONAL);
        assertThrows(IllegalArgumentException.class, () ->
                service.registerWithToken(second.rawToken(), "frank", "gina@example.com", "password123"));
        assertThrows(IllegalArgumentException.class, () ->
                service.registerWithToken(second.rawToken(), "gina", "frank@example.com", "password123"));

        assertThrows(IllegalArgumentException.class, () -> service.revoke(null));
        assertThrows(IllegalArgumentException.class, () -> service.revoke(999_999L));

        AccessGrantService stuck = new AccessGrantService();
        java.lang.reflect.Field dao = AccessGrantService.class.getDeclaredField("accessGrantDao");
        dao.setAccessible(true);
        dao.set(stuck, new com.examprep.dao.AccessGrantDao() {
            @Override
            public boolean revoke(Long grantId) {
                return false;
            }
        });
        AccessGrantService.CreatedAccessToken live = service.createToken(
                null, 3, null, null, ExamLevel.PROFESSIONAL);
        IllegalArgumentException couldNot = assertThrows(IllegalArgumentException.class,
                () -> stuck.revoke(live.grant().getId()));
        assertTrue(couldNot.getMessage().contains("could not be revoked"));
    }

    @Test
    void tokenWithoutExamLevelIsRejected() throws Exception {
        String raw = com.examprep.util.TokenHashUtil.generateRawToken();
        new com.examprep.dao.AccessGrantDao().create(
                com.examprep.util.TokenHashUtil.sha256(raw),
                LocalDateTime.now().plusDays(2), null, null, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.registerWithToken(raw, "helen", "helen@example.com", "password123"));
        assertTrue(ex.getMessage().toLowerCase().contains("exam level"));
    }
}
