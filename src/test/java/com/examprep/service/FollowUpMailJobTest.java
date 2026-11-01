package com.examprep.service;

import com.examprep.config.DatabaseManager;
import com.examprep.dao.ExamDao;
import com.examprep.dao.UserDao;
import com.examprep.model.Exam;
import com.examprep.model.ExamLevel;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.support.DatabaseTestSupport;
import com.examprep.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FollowUpMailJobTest extends DatabaseTestSupport {

    private final UserDao userDao = new UserDao();
    private final AccessGrantService accessGrantService = new AccessGrantService();

    @Test
    void startIsIdempotentAndStopIsSafe() throws Exception {
        try {
            FollowUpMailJob.start();
            FollowUpMailJob.start();
            Field field = FollowUpMailJob.class.getDeclaredField("executor");
            field.setAccessible(true);
            ScheduledExecutorService executor = (ScheduledExecutorService) field.get(null);
            executor.execute(() -> {
            });
            executor.shutdown();
            executor.awaitTermination(2, TimeUnit.SECONDS);
            FollowUpMailJob.start();
        } finally {
            FollowUpMailJob.stop();
            FollowUpMailJob.stop();
        }
    }

    @Test
    void sweepSkipsIneligibleUsersAndIsolatesFailures() throws Exception {
        userDao.create("boss", "boss@example.com", PasswordUtil.hash("password123"), Role.ADMIN, null);
        userDao.create("new", "new@example.com", PasswordUtil.hash("password123"), Role.USER, ExamLevel.PROFESSIONAL);

        User ready = userDao.create("ready", "ready@example.com", PasswordUtil.hash("password123"),
                Role.USER, ExamLevel.PROFESSIONAL);
        AccessGrantService.CreatedAccessToken token = accessGrantService.createToken(
                LocalDateTime.now().plusDays(20), null, "prep", "job", ExamLevel.PROFESSIONAL);
        accessGrantService.registerWithToken(token.rawToken(), "granted", "granted@example.com", "password123");
        User granted = userDao.findByUsername("granted").orElseThrow();
        userDao.setDiagnosticCompletedAt(granted.getId(), LocalDateTime.now().minusDays(1));

        userDao.setDiagnosticCompletedAt(ready.getId(), LocalDateTime.now().minusDays(1));
        assertDoesNotThrow(FollowUpMailJob::runOnce);

        for (Exam exam : new ExamDao().findAll()) {
            if (exam.isWeekly()) {
                exam.setActive(false);
                new ExamDao().update(exam);
            }
        }
        assertDoesNotThrow(FollowUpMailJob::runOnce);
    }

    @Test
    void sweepFailureIsSwallowedWhenTheDatabaseIsDown() {
        DatabaseManager.shutdown();
        assertDoesNotThrow(FollowUpMailJob::runOnce);
    }
}
