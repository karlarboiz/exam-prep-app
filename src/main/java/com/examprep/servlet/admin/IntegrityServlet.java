package com.examprep.servlet.admin;

import com.examprep.service.BehaviorTrackingService;
import com.examprep.util.IdCipher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/integrity")
public class IntegrityServlet extends HttpServlet {

    private final BehaviorTrackingService behaviorTrackingService = new BehaviorTrackingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String attemptIdParam = req.getParameter("attemptId");
        try {
            if (attemptIdParam != null && !attemptIdParam.isBlank()) {
                Long attemptId = IdCipher.dec(attemptIdParam);
                req.setAttribute("attempt", behaviorTrackingService.getAttempt(attemptId));
                req.setAttribute("events", behaviorTrackingService.getTimeline(attemptId));
                req.getRequestDispatcher("/WEB-INF/jsp/admin/integrity-detail.jsp").forward(req, resp);
                return;
            }
            req.setAttribute("flagged", behaviorTrackingService.getFlaggedAttempts());
            req.getRequestDispatcher("/WEB-INF/jsp/admin/integrity.jsp").forward(req, resp);
        } catch (IllegalArgumentException e) {
            resp.sendRedirect(req.getContextPath() + "/admin/integrity");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
