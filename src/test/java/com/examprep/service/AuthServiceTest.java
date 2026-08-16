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
