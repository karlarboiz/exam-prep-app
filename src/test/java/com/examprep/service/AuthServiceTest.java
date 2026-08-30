package com.examprep.service;

import com.examprep.dao.UserDao;
import com.examprep.model.ExamLevel;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.support.DatabaseTestSupport;
import com.examprep.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest extends DatabaseTestSupport {

    private final AuthService authService = new AuthService();
    private final UserDao userDao = new UserDao();

    @Test
    void adminCanChangeStudentExamLevel() throws Exception {
        User admin = createAdmin("boss");
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);

        authService.updateUser(admin.getId(), student.getId(), Role.USER, ExamLevel.SUB_PROFESSIONAL);

        User updated = userDao.findById(student.getId()).orElseThrow();
        assertEquals(Role.USER, updated.getRole());
        assertEquals(ExamLevel.SUB_PROFESSIONAL, updated.getExamLevel());
    }

    @Test
    void studentRequiresExamLevel() throws Exception {
        User admin = createAdmin("boss");
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.updateUser(admin.getId(), student.getId(), Role.USER, null));
        assertTrue(ex.getMessage().toLowerCase().contains("exam level"));
    }

    @Test
    void cannotDemoteLastAdmin() throws Exception {
        User admin = createAdmin("boss");
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.updateUser(student.getId(), admin.getId(), Role.USER, ExamLevel.PROFESSIONAL));
        assertTrue(ex.getMessage().toLowerCase().contains("last admin"));
    }

    @Test
    void cannotDeleteOwnAccount() throws Exception {
        User admin = createAdmin("boss");
        createAdmin("other");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.deleteUser(admin.getId(), admin.getId()));
        assertTrue(ex.getMessage().toLowerCase().contains("own"));
    }

    @Test
    void updateProfileChangesUsernameAndEmail() throws Exception {
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);
        int version = userDao.findById(student.getId()).orElseThrow().getTokenVersion();

        authService.updateProfile(student.getId(), "patricia", "patricia@example.com", "password123");

        User updated = userDao.findById(student.getId()).orElseThrow();
        assertEquals("patricia", updated.getUsername());
        assertEquals("patricia@example.com", updated.getEmail());
        assertEquals(version, updated.getTokenVersion());
        assertTrue(authService.authenticate("patricia", "password123").isPresent());
        assertTrue(authService.authenticate("pat", "password123").isEmpty());
    }

    @Test
    void updateProfileRejectsTakenUsernameAndEmail() throws Exception {
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);
        createStudent("other", ExamLevel.SUB_PROFESSIONAL);

        IllegalArgumentException usernameTaken = assertThrows(IllegalArgumentException.class, () ->
                authService.updateProfile(student.getId(), "other", "pat@example.com", "password123"));
        assertTrue(usernameTaken.getMessage().toLowerCase().contains("username already"));

        IllegalArgumentException emailTaken = assertThrows(IllegalArgumentException.class, () ->
                authService.updateProfile(student.getId(), "pat", "other@example.com", "password123"));
        assertTrue(emailTaken.getMessage().toLowerCase().contains("email already"));

        User unchanged = userDao.findById(student.getId()).orElseThrow();
        assertEquals("pat", unchanged.getUsername());
        assertEquals("pat@example.com", unchanged.getEmail());
    }

    @Test
    void updateProfileRejectsWrongPasswordAndInvalidEmail() throws Exception {
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);

        IllegalArgumentException wrong = assertThrows(IllegalArgumentException.class, () ->
                authService.updateProfile(student.getId(), "patricia", "patricia@example.com", "wrong"));
        assertTrue(wrong.getMessage().toLowerCase().contains("current password"));

        IllegalArgumentException invalid = assertThrows(IllegalArgumentException.class, () ->
                authService.updateProfile(student.getId(), "patricia", "not-an-email", "password123"));
        assertTrue(invalid.getMessage().toLowerCase().contains("email is invalid"));

        User unchanged = userDao.findById(student.getId()).orElseThrow();
        assertEquals("pat", unchanged.getUsername());
        assertEquals("pat@example.com", unchanged.getEmail());
    }

    @Test
    void changePasswordUpdatesHash() throws Exception {
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);

        authService.changePassword(student.getId(), "password123", "newpass1", "newpass1");

        assertTrue(authService.authenticate("pat", "newpass1").isPresent());
        assertTrue(authService.authenticate("pat", "password123").isEmpty());
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() throws Exception {
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.changePassword(student.getId(), "wrong", "newpass1", "newpass1"));
        assertTrue(ex.getMessage().toLowerCase().contains("current password"));
        assertTrue(authService.authenticate("pat", "password123").isPresent());
    }

    @Test
    void changePasswordBumpsTokenVersion() throws Exception {
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);
        int version = userDao.findById(student.getId()).orElseThrow().getTokenVersion();

        authService.changePassword(student.getId(), "password123", "newpass1", "newpass1");

        assertEquals(version + 1, userDao.findById(student.getId()).orElseThrow().getTokenVersion());
    }

    @Test
    void lockoutAfterRepeatedFailures() throws Exception {
        createStudent("lockedout", ExamLevel.PROFESSIONAL);
        for (int i = 0; i < 5; i++) {
            assertTrue(authService.authenticate("lockedout", "wrong").isEmpty());
        }
        IllegalArgumentException locked = assertThrows(IllegalArgumentException.class, () ->
                authService.authenticate("lockedout", "wrong"));
        assertTrue(locked.getMessage().toLowerCase().contains("too many"));
    }

    @Test
    void passwordResetWorksAndUnknownEmailIsSilent() throws Exception {
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);
        authService.requestPasswordReset("nobody@example.com", "http://localhost");
        assertTrue(new com.examprep.dao.EmailOutboxDao().findByUserId(student.getId()).isEmpty());

        authService.requestPasswordReset("pat@example.com", "http://localhost");
        var rows = new com.examprep.dao.EmailOutboxDao().findByUserId(student.getId());
        assertEquals(1, rows.size());
        String body = rows.get(0).getBody();
        int idx = body.indexOf("token=");
        assertTrue(idx >= 0);
        String raw = body.substring(idx + 6).split("\\s")[0];

        authService.resetPassword(raw, "resetpass1", "resetpass1");
        assertTrue(authService.authenticate("pat", "resetpass1").isPresent());
        assertTrue(authService.authenticate("pat", "password123").isEmpty());

        IllegalArgumentException reused = assertThrows(IllegalArgumentException.class, () ->
                authService.resetPassword(raw, "another1", "another1"));
        assertTrue(reused.getMessage().toLowerCase().contains("invalid"));
    }

    @Test
    void changePasswordRejectsMismatchAndReuse() throws Exception {
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);

        IllegalArgumentException mismatch = assertThrows(IllegalArgumentException.class, () ->
                authService.changePassword(student.getId(), "password123", "newpass1", "newpass2"));
        assertTrue(mismatch.getMessage().toLowerCase().contains("match"));

        IllegalArgumentException reuse = assertThrows(IllegalArgumentException.class, () ->
                authService.changePassword(student.getId(), "password123", "password123", "password123"));
        assertTrue(reuse.getMessage().toLowerCase().contains("different"));
    }

    @Test
    void canPromoteAndDeleteAnotherUser() throws Exception {
        User admin = createAdmin("boss");
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);

        authService.updateUser(admin.getId(), student.getId(), Role.ADMIN, null);
        User promoted = userDao.findById(student.getId()).orElseThrow();
        assertEquals(Role.ADMIN, promoted.getRole());
        assertNull(promoted.getExamLevel());

        authService.deleteUser(admin.getId(), student.getId());
        assertTrue(userDao.findById(student.getId()).isEmpty());
    }

    private User createAdmin(String username) throws Exception {
        return userDao.create(username, username + "@example.com", PasswordUtil.hash("password123"),
                Role.ADMIN, null);
    }

    private User createStudent(String username, ExamLevel examLevel) throws Exception {
        return userDao.create(username, username + "@example.com", PasswordUtil.hash("password123"),
                Role.USER, examLevel);
    }
}
