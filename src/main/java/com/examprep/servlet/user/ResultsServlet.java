package com.examprep.servlet.user;

import com.examprep.model.ExamAttempt;
import com.examprep.model.User;
import com.examprep.service.ExamService;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@WebServlet("/user/result")
public class ResultsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ResultsServlet.class);

    private final ExamService examService = new ExamService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.getCurrentUser(req);
        String attemptIdParam = req.getParameter("attemptId");
        if (attemptIdParam == null) {
            resp.sendRedirect(req.getContextPath() + "/user/dashboard");
            return;
        }

        try {
            Long attemptId = Long.parseLong(attemptIdParam);
            ExamAttempt attempt = examService.getAttempt(attemptId);
            if (!attempt.getUserId().equals(user.getId()) && !user.isAdmin()) {
                log.warn("User {} forbidden from viewing attemptId={}", user.getUsername(), attemptId);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            req.setAttribute("attempt", attempt);
            req.setAttribute("answers", examService.getAttemptAnswers(attemptId));
            req.getRequestDispatcher("/WEB-INF/jsp/user/result.jsp").forward(req, resp);
        } catch (Exception e) {
            log.error("Failed to render result attemptId={} for user={}", attemptIdParam, user.getUsername(), e);
            throw new ServletException(e);
        }
    }
}
