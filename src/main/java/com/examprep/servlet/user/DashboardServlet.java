package com.examprep.servlet.user;

import com.examprep.i18n.Messages;
import com.examprep.model.User;
import com.examprep.service.ExamService;
import com.examprep.service.WeeklyRegimenService;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/user/dashboard")
public class DashboardServlet extends HttpServlet {

    private final ExamService examService = new ExamService();
    private final WeeklyRegimenService weeklyRegimenService = new WeeklyRegimenService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.getCurrentUser(req);
        try {
            req.setAttribute("subjects", examService.getSubjects(user.getExamLevel()));
            req.setAttribute("exams", examService.getActiveExams(user.getExamLevel()));
            req.setAttribute("history", examService.getUserHistory(user.getId()));
            req.setAttribute("examLevel", user.getExamLevel());
            try {
                req.setAttribute("weekly", weeklyRegimenService.resolveDashboard(user.getId()));
            } catch (IllegalStateException e) {
                req.setAttribute("weeklyError", Messages.fromException(req, e.getMessage()));
            }
            req.getRequestDispatcher("/WEB-INF/jsp/user/dashboard.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
