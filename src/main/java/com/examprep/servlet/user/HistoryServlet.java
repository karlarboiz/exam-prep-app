package com.examprep.servlet.user;

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

@WebServlet("/user/history")
public class HistoryServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(HistoryServlet.class);

    private final ExamService examService = new ExamService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.getCurrentUser(req);
        try {
            req.setAttribute("history", examService.getUserHistory(user.getId()));
            req.getRequestDispatcher("/WEB-INF/jsp/user/history.jsp").forward(req, resp);
        } catch (Exception e) {
            log.error("Failed to render history for user={}", user.getUsername(), e);
            throw new ServletException(e);
        }
    }
}
