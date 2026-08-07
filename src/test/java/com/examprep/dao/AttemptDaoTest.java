package com.examprep.dao;

import com.examprep.model.AttemptStatus;
import com.examprep.model.ExamAttempt;
import com.examprep.model.ExamLevel;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.support.DatabaseTestSupport;
import com.examprep.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttemptDaoTest extends DatabaseTestSupport {

    private final AttemptDao attemptDao = new AttemptDao();
    private final UserDao userDao = new UserDao();

    @Test
    void findByIdIncludesDiagnosticFlag() throws Exception {
        User user = userDao.create(
                "histuser", "hist@example.com", PasswordUtil.hash("password123"),
                Role.USER, ExamLevel.PROFESSIONAL);

        // Seed exam id 1 = practice, id 2 = diagnostic (schema.sql)
        ExamAttempt practice = attemptDao.create(user.getId(), 1L);
        ExamAttempt diagnostic = attemptDao.create(user.getId(), 2L);

        ExamAttempt loadedPractice = attemptDao.findById(practice.getId()).orElseThrow();
        ExamAttempt loadedDiagnostic = attemptDao.findById(diagnostic.getId()).orElseThrow();

        assertFalse(loadedPractice.isDiagnostic());
        assertTrue(loadedDiagnostic.isDiagnostic());
        assertTrue(attemptDao.findByUserId(user.getId()).stream()
                .anyMatch(a -> a.isDiagnostic() && a.getStatus() == AttemptStatus.IN_PROGRESS));
    }
}
