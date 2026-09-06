package com.examprep.service;

import com.examprep.config.AppConfig;
import com.examprep.dao.PasswordResetTokenDao;
import com.examprep.dao.UserDao;
import com.examprep.model.AppLocale;
import com.examprep.model.ExamLevel;
import com.examprep.model.PasswordResetToken;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.util.JwtUtil;
import com.examprep.util.LoginLockout;
import com.examprep.util.PasswordUtil;
import com.examprep.util.TokenHashUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AuthService {

    public static final String REGISTER_TOKEN_ATTR = "registerAccessToken";
    public static final String RESET_TOKEN_ATTR = "passwordResetToken";

    private final UserDao userDao = new UserDao();
    private final PasswordResetTokenDao resetTokenDao = new PasswordResetTokenDao();
    private final MailService mailService = new MailService();
    private final LoginLockout loginLockout = new LoginLockout();

    public Optional<User> authenticate(String username, String password) throws SQLException {
        if (loginLockout.isLocked(username)) {
            throw new IllegalArgumentException("Too many failed login attempts. Try again later.");
        }
        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isEmpty()) {
            loginLockout.recordFailure(username);
            return Optional.empty();
        }
        User user = userOpt.get();
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            loginLockout.recordFailure(username);
            return Optional.empty();
        }
        loginLockout.recordSuccess(username);
        return Optional.of(user);
    }

    public String issueToken(User user) {
        return JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(),
                user.getTokenVersion());
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

    public void updateLocale(Long userId, AppLocale locale) throws SQLException {
        if (locale == null) {
            locale = AppLocale.DEFAULT;
        }
        userDao.updateLocale(userId, locale);
    }

    public void updateProfile(Long userId, String username, String email, String currentPassword)
            throws SQLException {
        if (isBlank(currentPassword)) {
            throw new IllegalArgumentException("Current password is required");
        }
        if (isBlank(username)) {
            throw new IllegalArgumentException("Username is required");
        }
        if (isBlank(email)) {
            throw new IllegalArgumentException("Email is required");
        }
        String trimmedUsername = username.trim();
        String trimmedEmail = email.trim();
        if (trimmedUsername.length() > 50) {
            throw new IllegalArgumentException("Username is too long");
        }
        if (trimmedEmail.length() > 100) {
            throw new IllegalArgumentException("Email is too long");
        }
        if (!isValidEmail(trimmedEmail)) {
            throw new IllegalArgumentException("Email is invalid");
        }
        User user = requireUser(userId);
        if (!PasswordUtil.verify(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        Optional<User> usernameOwner = userDao.findByUsername(trimmedUsername);
        if (usernameOwner.isPresent() && !usernameOwner.get().getId().equals(userId)) {
            throw new IllegalArgumentException("Username already exists");
        }
        Optional<User> emailOwner = userDao.findByEmail(trimmedEmail);
        if (emailOwner.isPresent() && !emailOwner.get().getId().equals(userId)) {
            throw new IllegalArgumentException("Email already exists");
        }
        userDao.updateUsernameAndEmail(userId, trimmedUsername, trimmedEmail);
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

    /**
     * Always succeeds from the caller's view. Sends a reset mail only when the email exists.
     */
    public void requestPasswordReset(String email, String publicBaseUrl) throws SQLException {
        if (isBlank(email)) {
            return;
        }
        Optional<User> userOpt = userDao.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();
        resetTokenDao.invalidateUnusedForUser(user.getId());
        String rawToken = TokenHashUtil.generateRawToken();
        int ttlMinutes = AppConfig.getInt("password.reset.ttl.minutes", 60);
        resetTokenDao.insert(user.getId(), TokenHashUtil.sha256(rawToken),
                LocalDateTime.now().plusMinutes(ttlMinutes));

        String base = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/$", "");
        String link = base + "/reset-password?token=" + rawToken;
        String subject = "Reset your Exam Prep password";
        String body = "Hi " + user.getUsername() + ",\n\n"
                + "Use this link within " + ttlMinutes + " minutes to choose a new password:\n"
                + link + "\n\n"
                + "If you did not request this, you can ignore the email.\n";
        mailService.send(user.getId(), null, user.getEmail(), subject, body);
    }

    public Optional<User> peekResetToken(String rawToken) throws SQLException {
        if (isBlank(rawToken)) {
            return Optional.empty();
        }
        Optional<PasswordResetToken> tokenOpt = resetTokenDao.findByHash(TokenHashUtil.sha256(rawToken.trim()));
        if (tokenOpt.isEmpty() || !tokenOpt.get().isUsable(LocalDateTime.now())) {
            return Optional.empty();
        }
        return userDao.findById(tokenOpt.get().getUserId());
    }

    public void resetPassword(String rawToken, String newPassword, String confirmPassword) throws SQLException {
        if (isBlank(rawToken)) {
            throw new IllegalArgumentException("This reset link is invalid or expired");
        }
        if (isBlank(newPassword) || isBlank(confirmPassword)) {
            throw new IllegalArgumentException("All password fields are required");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New passwords do not match");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        Optional<PasswordResetToken> tokenOpt = resetTokenDao.findByHash(TokenHashUtil.sha256(rawToken.trim()));
        if (tokenOpt.isEmpty() || !tokenOpt.get().isUsable(LocalDateTime.now())) {
            throw new IllegalArgumentException("This reset link is invalid or expired");
        }
        PasswordResetToken token = tokenOpt.get();
        userDao.updatePasswordHash(token.getUserId(), PasswordUtil.hash(newPassword));
        resetTokenDao.markUsed(token.getId());
        resetTokenDao.invalidateUnusedForUser(token.getUserId());
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

    private static boolean isValidEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 && email.indexOf('.', at) > at + 1 && email.indexOf(' ') < 0;
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
