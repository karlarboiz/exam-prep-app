package com.examprep.service;

import com.examprep.config.AppConfig;
import com.examprep.dao.AccessGrantDao;
import com.examprep.dao.UserDao;
import com.examprep.model.Role;
import com.examprep.model.User;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Emails students who miss a weekly exam or checkpoint even if they do not open the app.
 * A dashboard visit sends the same follow-up immediately.
 */
public final class FollowUpMailJob {

    private static ScheduledExecutorService executor;

    private FollowUpMailJob() {
    }

    public static synchronized void start() {
        if (executor != null && !executor.isShutdown()) {
            return;
        }
        int hours = Math.max(1, AppConfig.getInt("weekly.followup.interval.hours", 6));
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "follow-up-mail");
            thread.setDaemon(true);
            return thread;
        });
        executor.schedule(FollowUpMailJob::runOnce, 1, TimeUnit.MINUTES);
        executor.scheduleAtFixedRate(FollowUpMailJob::runOnce, hours, hours, TimeUnit.HOURS);
    }

    public static synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    static void runOnce() {
        WeeklyRegimenService regimens = new WeeklyRegimenService();
        UserDao userDao = new UserDao();
        AccessGrantDao accessGrantDao = new AccessGrantDao();
        try {
            for (User user : userDao.findAll()) {
                if (user.getRole() != Role.USER || user.getDiagnosticCompletedAt() == null) {
                    continue;
                }
                if (accessGrantDao.findActiveByUserId(user.getId()).isEmpty()) {
                    continue;
                }
                try {
                    regimens.sweepFollowUps(user.getId());
                } catch (Exception e) {
                    System.err.println("[follow-up] user " + user.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[follow-up] sweep failed: " + e.getMessage());
        }
    }
}
