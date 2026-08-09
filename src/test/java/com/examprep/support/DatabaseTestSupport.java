package com.examprep.support;

import com.examprep.config.DatabaseManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Shared DB lifecycle for service/DAO tests: fresh in-memory H2 + schema per test class method.
 */
public abstract class DatabaseTestSupport {

    @BeforeEach
    void resetDatabase() {
        DatabaseManager.reinitForTesting();
    }

    @AfterAll
    static void shutdownDatabase() {
        DatabaseManager.shutdown();
    }
}
