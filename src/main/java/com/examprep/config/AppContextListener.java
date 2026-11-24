package com.examprep.config;

import com.examprep.service.FollowUpMailJob;
import com.examprep.service.N8nQuestionSchedulerJob;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.SQLException;

@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        DatabaseManager.init();
        try {
            SeedData.seedAdminIfMissing();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed admin user", e);
        }
        sce.getServletContext().setAttribute("appName", "Exam Prep App");
        FollowUpMailJob.start();
        N8nQuestionSchedulerJob.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        N8nQuestionSchedulerJob.stop();
        FollowUpMailJob.stop();
        DatabaseManager.shutdown();
    }
}
