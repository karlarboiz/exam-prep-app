package com.examprep.service;

import com.examprep.dao.UserDao;
import com.examprep.model.ExamLevel;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.util.JwtUtil;
import com.examprep.util.PasswordUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public Optional<User> authenticate(String username, String password) throws SQLException {
        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public String issueToken(User user) {
        return JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
    }

    public User register(String username, String email, String password, ExamLevel examLevel) throws SQLException {
        if (examLevel == null) {
            throw new IllegalArgumentException("Exam level is required");
        }
        if (userDao.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userDao.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        String hash = PasswordUtil.hash(password);
        return userDao.create(username, email, hash, Role.USER, examLevel);
    }

    public Optional<User> findById(Long id) throws SQLException {
        return userDao.findById(id);
    }

    public List<User> findAllUsers() throws SQLException {
        return userDao.findAll();
    }

    public void updateUser(Long actorId, Long targetId, Role role, ExamLevel examLevel) throws SQLException {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        User target = requireUser(targetId);
        if (role == Role.USER && examLevel == null) {
            throw new IllegalArgumentException("Exam level is required for student users");
        }
        preventRemovingLastAdmin(actorId, target, role != Role.ADMIN);
        boolean examLevelChanged = !Objects.equals(target.getExamLevel(), examLevel);
        boolean resetDiagnostic = role == Role.USER && examLevelChanged;
        userDao.updateRoleAndExamLevel(targetId, role, examLevel, resetDiagnostic);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword)
            throws SQLException {
        if (isBlank(currentPassword) || isBlank(newPassword) || isBlank(confirmPassword)) {
            throw new IllegalArgumentException("All password fields are required");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New passwords do not match");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }
        User user = requireUser(userId);
        if (!PasswordUtil.verify(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        userDao.updatePasswordHash(userId, PasswordUtil.hash(newPassword));
    }

    public void deleteUser(Long actorId, Long targetId) throws SQLException {
        User target = requireUser(targetId);
        if (actorId != null && actorId.equals(target.getId())) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        if (target.getRole() == Role.ADMIN && userDao.countByRole(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("Cannot remove the last admin");
        }
        userDao.delete(targetId);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private User requireUser(Long targetId) throws SQLException {
        return userDao.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void preventRemovingLastAdmin(Long actorId, User target, boolean removingAdminRole)
            throws SQLException {
        if (actorId != null && actorId.equals(target.getId()) && removingAdminRole) {
            throw new IllegalArgumentException("You cannot remove your own admin access");
        }
        if (target.getRole() == Role.ADMIN && removingAdminRole && userDao.countByRole(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("Cannot remove the last admin");
        }
    }
}
