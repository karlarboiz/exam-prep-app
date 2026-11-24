package com.examprep.service;

import com.examprep.config.AppConfig;
import com.examprep.dao.UserDao;
import com.examprep.model.N8nRequest;
import com.examprep.model.Role;
import com.examprep.model.User;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically POSTs a question-batch request to the configured n8n webhook.
 * Off by default — enable with {@code n8n.scheduler.enabled=true} and a non-blank message.
 */
public final class N8nQuestionSchedulerJob {

    private static ScheduledExecutorService executor;

    private N8nQuestionSchedulerJob() {
    }

    public static boolean isEnabled() {
        return AppConfig.getBoolean("n8n.scheduler.enabled", false);
    }

    public static int intervalHours() {
        return Math.max(1, AppConfig.getInt("n8n.scheduler.interval.hours", 24));
    }

    public static String configuredMessage() {
        return blankToEmpty(AppConfig.get("n8n.scheduler.message", ""));
    }

    public static String configuredSubject() {
        return blankToEmpty(AppConfig.get("n8n.scheduler.subject", ""));
    }

    public static String configuredCount() {
        return blankToEmpty(AppConfig.get("n8n.scheduler.count", "20"));
    }

    public static String configuredDifficulty() {
        return blankToEmpty(AppConfig.get("n8n.scheduler.difficulty", "MEDIUM"));
    }

    public static String configuredBatchLabel() {
        return blankToEmpty(AppConfig.get("n8n.scheduler.batchLabel", ""));
    }

    /** True when enabled and both webhook URL and message are set. */
    public static boolean isReady(N8nService n8n) {
        return isEnabled()
                && n8n != null
                && n8n.isQuestionsConfigured()
                && !configuredMessage().isBlank();
    }

    public static synchronized void start() {
        if (executor != null && !executor.isShutdown()) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        int hours = intervalHours();
        int initialMinutes = Math.max(1, AppConfig.getInt("n8n.scheduler.initial.delay.minutes", 5));
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "n8n-question-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        Runnable tick = N8nQuestionSchedulerJob::runOnce;
        executor.schedule(tick, initialMinutes, TimeUnit.MINUTES);
        executor.scheduleAtFixedRate(tick, hours, hours, TimeUnit.HOURS);
        System.out.println("[n8n-scheduler] started; first run in " + initialMinutes
                + " min, then every " + hours + " h");
    }

    public static synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    static void runOnce() {
        runOnce(new N8nService(), new UserDao());
    }

    static void runOnce(N8nService n8n, UserDao userDao) {
        if (!isEnabled()) {
            return;
        }
        if (n8n == null || !n8n.isQuestionsConfigured()) {
            System.err.println("[n8n-scheduler] skipped: question webhook is not configured");
            return;
        }
        String message = configuredMessage();
        if (message.isBlank()) {
            System.err.println("[n8n-scheduler] skipped: n8n.scheduler.message is blank");
            return;
        }
        try {
            User admin = resolveAdmin(userDao);
            if (admin == null) {
                System.err.println("[n8n-scheduler] skipped: no ADMIN user to attribute the request");
                return;
            }
            String batchLabel = configuredBatchLabel();
            if (batchLabel.isBlank()) {
                batchLabel = QuestionImportService.suggestedBatchLabel();
            }
            String subject = configuredSubject();
            N8nRequest row = n8n.requestQuestions(
                    admin,
                    message,
                    subject.isBlank() ? null : subject,
                    configuredCount(),
                    configuredDifficulty().isBlank() ? null : configuredDifficulty(),
                    batchLabel,
                    null,
                    null);
            System.out.println("[n8n-scheduler] " + row.getStatus() + " id=" + row.getId()
                    + " as " + admin.getUsername());
        } catch (Exception e) {
            System.err.println("[n8n-scheduler] failed: " + e.getMessage());
        }
    }

    private static User resolveAdmin(UserDao userDao) throws Exception {
        String username = AppConfig.get("admin.username", "admin");
        var byName = userDao.findByUsername(username);
        if (byName.isPresent() && byName.get().getRole() == Role.ADMIN) {
            return byName.get();
        }
        for (User user : userDao.findAll()) {
            if (user.getRole() == Role.ADMIN) {
                return user;
            }
        }
        return null;
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
