package com.examprep.service;

import com.examprep.dao.UserDao;
import com.examprep.model.ExamLevel;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.support.DatabaseTestSupport;
import com.examprep.util.JwtUtil;
import com.examprep.util.PasswordUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest extends DatabaseTestSupport {

    private static final String PASSWORD = "password123";

    private final AuthService authService = new AuthService();
    private final UserDao userDao = new UserDao();

    @Test
    void authenticateReturnsUserForValidCredentials() throws Exception {
        createStudent("pat", ExamLevel.PROFESSIONAL);

        Optional<User> authenticated = authService.authenticate("pat", PASSWORD);

        assertTrue(authenticated.isPresent());
        assertEquals("pat", authenticated.get().getUsername());
    }

    @Test
    void authenticateReturnsEmptyWhenUsernameUnknown() throws Exception {
        assertTrue(authService.authenticate("missing", PASSWORD).isEmpty());
    }

    @Test
    void authenticateReturnsEmptyWhenPasswordWrong() throws Exception {
        createStudent("pat", ExamLevel.PROFESSIONAL);

        assertTrue(authService.authenticate("pat", "wrong-password").isEmpty());
    }

    @Test
    void issueTokenIncludesUserClaims() throws Exception {
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);

        String token = authService.issueToken(student);
        Claims claims = JwtUtil.parseToken(token);

        assertEquals(student.getId(), JwtUtil.getUserId(claims));
        assertEquals("pat", JwtUtil.getUsername(claims));
        assertEquals(Role.USER, JwtUtil.getRole(claims));
    }

    @Test
    void registerCreatesStudentWithHashedPassword() throws Exception {
        User created = authService.register("pat", "pat@example.com", PASSWORD, ExamLevel.SUB_PROFESSIONAL);

        assertEquals("pat", created.getUsername());
        assertEquals("pat@example.com", created.getEmail());
        assertEquals(Role.USER, created.getRole());
        assertEquals(ExamLevel.SUB_PROFESSIONAL, created.getExamLevel());
        assertTrue(PasswordUtil.verify(PASSWORD, created.getPasswordHash()));
        assertTrue(authService.authenticate("pat", PASSWORD).isPresent());
    }

    @Test
    void registerRequiresExamLevel() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.register("pat", "pat@example.com", PASSWORD, null));
        assertTrue(ex.getMessage().toLowerCase().contains("exam level"));
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        createStudent("pat", ExamLevel.PROFESSIONAL);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.register("pat", "other@example.com", PASSWORD, ExamLevel.PROFESSIONAL));
        assertTrue(ex.getMessage().toLowerCase().contains("username"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        createStudent("pat", ExamLevel.PROFESSIONAL);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.register("other", "pat@example.com", PASSWORD, ExamLevel.PROFESSIONAL));
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
    }

    @Test
    void findByIdReturnsUserWhenPresent() throws Exception {
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);

        Optional<User> found = authService.findById(student.getId());

        assertTrue(found.isPresent());
        assertEquals("pat", found.get().getUsername());
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() throws Exception {
        assertTrue(authService.findById(999_999L).isEmpty());
    }

    @Test
    void findAllUsersReturnsCreatedAccounts() throws Exception {
        createAdmin("boss");
        createStudent("pat", ExamLevel.PROFESSIONAL);

        List<User> users = authService.findAllUsers();

        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> "boss".equals(u.getUsername())));
        assertTrue(users.stream().anyMatch(u -> "pat".equals(u.getUsername())));
    }

    @Test
    void adminCanChangeStudentExamLevel() throws Exception {
        User admin = createAdmin("boss");
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);
        userDao.markDiagnosticCompleted(student.getId());
        assertTrue(userDao.isDiagnosticCompleted(student.getId()));

        authService.updateUser(admin.getId(), student.getId(), Role.USER, ExamLevel.SUB_PROFESSIONAL);

        User updated = userDao.findById(student.getId()).orElseThrow();
        assertEquals(Role.USER, updated.getRole());
        assertEquals(ExamLevel.SUB_PROFESSIONAL, updated.getExamLevel());
        assertFalse(userDao.isDiagnosticCompleted(student.getId()));
    }

    @Test
    void sameExamLevelDoesNotResetDiagnostic() throws Exception {
        User admin = createAdmin("boss");
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);
        userDao.markDiagnosticCompleted(student.getId());

        authService.updateUser(admin.getId(), student.getId(), Role.USER, ExamLevel.PROFESSIONAL);

        User updated = userDao.findById(student.getId()).orElseThrow();
        assertEquals(ExamLevel.PROFESSIONAL, updated.getExamLevel());
        assertTrue(userDao.isDiagnosticCompleted(student.getId()));
    }

    @Test
    void updateUserRequiresRole() throws Exception {
        User admin = createAdmin("boss");
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.updateUser(admin.getId(), student.getId(), null, ExamLevel.PROFESSIONAL));
        assertTrue(ex.getMessage().toLowerCase().contains("role"));
    }

    @Test
    void updateUserRejectsUnknownTarget() throws Exception {
        User admin = createAdmin("boss");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.updateUser(admin.getId(), 999_999L, Role.USER, ExamLevel.PROFESSIONAL));
        assertTrue(ex.getMessage().toLowerCase().contains("not found"));
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
    void cannotDemoteLastAdminWhenActorIsNull() throws Exception {
        User admin = createAdmin("boss");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.updateUser(null, admin.getId(), Role.USER, ExamLevel.PROFESSIONAL));
        assertTrue(ex.getMessage().toLowerCase().contains("last admin"));
    }

    @Test
    void cannotRemoveOwnAdminAccess() throws Exception {
        User admin = createAdmin("boss");
        createAdmin("other");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.updateUser(admin.getId(), admin.getId(), Role.USER, ExamLevel.PROFESSIONAL));
        assertTrue(ex.getMessage().toLowerCase().contains("own admin"));
    }

    @Test
    void adminCanKeepOwnAdminRole() throws Exception {
        User admin = createAdmin("boss");

        authService.updateUser(admin.getId(), admin.getId(), Role.ADMIN, null);

        User updated = userDao.findById(admin.getId()).orElseThrow();
        assertEquals(Role.ADMIN, updated.getRole());
        assertNull(updated.getExamLevel());
    }

    @Test
    void canDemoteAnotherAdminWhenMoreThanOneExists() throws Exception {
        User admin = createAdmin("boss");
        User other = createAdmin("other");

        authService.updateUser(admin.getId(), other.getId(), Role.USER, ExamLevel.PROFESSIONAL);

        User demoted = userDao.findById(other.getId()).orElseThrow();
        assertEquals(Role.USER, demoted.getRole());
        assertEquals(ExamLevel.PROFESSIONAL, demoted.getExamLevel());
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
    void cannotDeleteLastAdmin() throws Exception {
        User admin = createAdmin("boss");
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.deleteUser(student.getId(), admin.getId()));
        assertTrue(ex.getMessage().toLowerCase().contains("last admin"));
    }

    @Test
    void deleteUserRejectsUnknownTarget() throws Exception {
        User admin = createAdmin("boss");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.deleteUser(admin.getId(), 999_999L));
        assertTrue(ex.getMessage().toLowerCase().contains("not found"));
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

    @Test
    void canDeleteAnotherUserWhenActorIsNull() throws Exception {
        createAdmin("boss");
        User student = createStudent("pat", ExamLevel.PROFESSIONAL);

        authService.deleteUser(null, student.getId());

        assertTrue(userDao.findById(student.getId()).isEmpty());
    }

    private User createAdmin(String username) throws Exception {
        return userDao.create(username, username + "@example.com", PasswordUtil.hash(PASSWORD),
                Role.ADMIN, null);
    }

    private User createStudent(String username, ExamLevel examLevel) throws Exception {
        return userDao.create(username, username + "@example.com", PasswordUtil.hash(PASSWORD),
                Role.USER, examLevel);
    }
}
