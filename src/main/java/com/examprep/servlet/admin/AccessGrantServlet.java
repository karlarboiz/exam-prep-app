package com.examprep.servlet.admin;

import com.examprep.model.ExamLevel;
import com.examprep.service.AccessGrantService;
import com.examprep.util.IdCipher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/access-grants")
public class AccessGrantServlet extends HttpServlet {

    private final AccessGrantService accessGrantService = new AccessGrantService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("grants", accessGrantService.listAll());
            req.getRequestDispatcher("/WEB-INF/jsp/admin/access-grants.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if (action == null) {
                throw new IllegalArgumentException("Unknown action");
            }
            switch (action) {
                case "create" -> {
                    rememberForm(req);
                    ExamLevel examLevel = parseExamLevel(req.getParameter("examLevel"));
                    Integer durationDays = parseDurationDays(req.getParameter("durationDays"));
                    String planCode = blankToNull(req.getParameter("planCode"));
                    String sourceRef = blankToNull(req.getParameter("sourceRef"));
                    AccessGrantService.CreatedAccessToken created =
                            accessGrantService.createToken(null, durationDays, planCode, sourceRef, examLevel);
                    req.setAttribute("createdRawToken", created.rawToken());
                    req.setAttribute("createdGrant", created.grant());
                    req.setAttribute("registerPath", "/register?token=" + created.rawToken());
                    doGet(req, resp);
                }
                case "revoke" -> {
                    Long id = IdCipher.dec(req.getParameter("id"));
                    accessGrantService.revoke(id);
                    resp.sendRedirect(req.getContextPath() + "/admin/access-grants");
                }
                default -> throw new IllegalArgumentException("Unknown action");
            }
        } catch (Exception e) {
            req.setAttribute("error", e.getMessage());
            doGet(req, resp);
        }
    }

    private static void rememberForm(HttpServletRequest req) {
        req.setAttribute("formExamLevel", req.getParameter("examLevel"));
        req.setAttribute("formDurationDays", req.getParameter("durationDays"));
        req.setAttribute("formPlanCode", req.getParameter("planCode"));
        req.setAttribute("formSourceRef", req.getParameter("sourceRef"));
    }

    private static ExamLevel parseExamLevel(String value) {
        try {
            ExamLevel examLevel = ExamLevel.fromString(value);
            if (examLevel == null) {
                throw new IllegalArgumentException("Exam level is required");
            }
            return examLevel;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("required")) {
                throw e;
            }
            throw new IllegalArgumentException("Exam level must be Professional or Sub-Professional");
        }
    }

    private static Integer parseDurationDays(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Duration days is required");
        }
        try {
            int days = Integer.parseInt(value.trim());
            if (days <= 0) {
                throw new IllegalArgumentException("Duration days must be a positive whole number");
            }
            return days;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Duration days must be a positive whole number");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
