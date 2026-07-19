package com.examprep.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("Starting Exam Prep App");
        DatabaseManager.init();
        try {
            SeedData.seedAdminIfMissing();
        } catch (SQLException e) {
            log.error("Failed to seed admin user", e);
            throw new RuntimeException("Failed to seed admin user", e);
        }
        sce.getServletContext().setAttribute("appName", "Exam Prep App");
        log.info("Exam Prep App ready");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Shutting down Exam Prep App");
        DatabaseManager.shutdown();
    }
}
