package com.examprep.i18n;

import com.examprep.model.AppLocale;
import jakarta.servlet.http.HttpServletRequest;

import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;

public final class Messages {

    public static final String BUNDLE_NAME = "messages";

    private static final Map<String, String> EXCEPTION_KEYS = Map.ofEntries(
            Map.entry("Exam level is required", "error.examLevel.required"),
            Map.entry("Username already exists", "error.username.exists"),
            Map.entry("Email already exists", "error.email.exists"),
            Map.entry("Username is required", "error.username.required"),
            Map.entry("Email is required", "error.email.required"),
            Map.entry("Username is too long", "error.username.tooLong"),
            Map.entry("Email is too long", "error.email.tooLong"),
            Map.entry("Email is invalid", "error.email.invalid"),
            Map.entry("Current password is required", "error.password.currentRequired"),
            Map.entry("Role is required", "error.role.required"),
            Map.entry("Exam level is required for student users", "error.examLevel.studentRequired"),
            Map.entry("All password fields are required", "error.password.fieldsRequired"),
            Map.entry("New passwords do not match", "error.password.mismatch"),
            Map.entry("Password must be at least 6 characters", "error.password.tooShort"),
            Map.entry("New password must be different from the current password", "error.password.sameAsCurrent"),
            Map.entry("Current password is incorrect", "error.password.currentWrong"),
            Map.entry("Too many failed login attempts. Try again later.", "error.login.locked"),
            Map.entry("This reset link is invalid or expired", "error.reset.invalid"),
            Map.entry("You cannot delete your own account", "error.user.deleteSelf"),
            Map.entry("Cannot remove the last admin", "error.user.lastAdmin"),
            Map.entry("You cannot remove your own admin access", "error.user.removeOwnAdmin"),
            Map.entry("User not found", "error.user.notFound"),
            Map.entry("Access token is required", "error.token.required"),
            Map.entry("This access token has been revoked", "error.token.revoked"),
            Map.entry("This access token has already been used", "error.token.used"),
            Map.entry("This access token has expired", "error.token.expired"),
            Map.entry("This access token has no exam level assigned", "error.token.noLevel"),
            Map.entry("Diagnostic must be completed before the weekly regimen starts", "error.weekly.needDiagnostic"),
            Map.entry("This week's official exam is already recorded", "error.weekly.alreadyRecorded"),
            Map.entry("This week was missed; wait for the next week's form", "error.weekly.missed"),
            Map.entry("No questions available for this week's exam", "error.weekly.noQuestions"),
            Map.entry("Not a weekly exam attempt", "error.weekly.notWeekly"),
            Map.entry("Finish this week's exam before a checkpoint", "error.checkpoint.needWeekly"),
            Map.entry("Checkpoint is only available during this week", "error.checkpoint.notThisWeek"),
            Map.entry("Not enough unused items for a checkpoint", "error.checkpoint.noItems"),
            Map.entry("Not a checkpoint attempt", "error.checkpoint.notCheckpoint"),
            Map.entry("Study plan not found", "error.studyPlan.notFound"),
            Map.entry("Attempt is not in progress", "error.attempt.notInProgress"),
            Map.entry("Exam time has expired", "error.attempt.expired"),
            Map.entry("No questions available to build a diagnostic", "error.diagnostic.noQuestions"),
            Map.entry("Name is required", "error.name.required"),
            Map.entry("Select at least one level: Professional or Sub-Professional", "error.subject.levelRequired"),
            Map.entry("n8n question webhook is not configured", "error.n8n.questions.unconfigured"),
            Map.entry("n8n analyze webhook is not configured", "error.n8n.analyze.unconfigured"),
            Map.entry("Describe the question batch you want", "error.n8n.questions.messageRequired"),
            Map.entry("Question count must be between 1 and 100", "error.n8n.count"),
            Map.entry("Difficulty must be EASY, MEDIUM, or HARD", "error.n8n.difficulty"),
            Map.entry("Choose a file to analyze", "error.n8n.fileRequired"),
            Map.entry("File type is not allowed", "error.n8n.fileType"),
            Map.entry("File is too large", "error.n8n.fileLarge"),
            Map.entry("n8n did not accept the question request", "error.n8n.questions.rejected"),
            Map.entry("n8n did not accept the file", "error.n8n.analyze.rejected")
    );

    private Messages() {
    }

    public static ResourceBundle bundle(AppLocale locale) {
        AppLocale resolved = locale != null ? locale : AppLocale.DEFAULT;
        return ResourceBundle.getBundle(BUNDLE_NAME, resolved.toBundleLocale(), Utf8Control.INSTANCE);
    }

    public static String get(AppLocale locale, String key) {
        ResourceBundle bundle = bundle(locale);
        if (!bundle.containsKey(key)) {
            return key;
        }
        return bundle.getString(key);
    }

    public static String get(HttpServletRequest request, String key) {
        return get(LocaleSupport.current(request), key);
    }

    public static String format(AppLocale locale, String key, Object... args) {
        return MessageFormat.format(get(locale, key), args);
    }

    public static String format(HttpServletRequest request, String key, Object... args) {
        return format(LocaleSupport.current(request), key, args);
    }

    public static String fromException(HttpServletRequest request, String englishMessage) {
        if (englishMessage == null || englishMessage.isBlank()) {
            return get(request, "error.unexpected");
        }
        String key = EXCEPTION_KEYS.get(englishMessage);
        return key != null ? get(request, key) : englishMessage;
    }
}
