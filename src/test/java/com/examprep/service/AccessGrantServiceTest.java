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

        service.revoke(created.grant().getId());
        assertFalse(service.hasActiveAccess(user.getId()));
    }
}
