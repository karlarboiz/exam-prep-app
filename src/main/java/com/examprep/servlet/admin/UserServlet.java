package com.examprep.servlet.admin;

import com.examprep.i18n.Messages;
import com.examprep.model.ExamLevel;
import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.service.AuthService;
import com.examprep.util.IdCipher;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/users")
public class UserServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("users", authService.findAllUsers());
            req.getRequestDispatcher("/WEB-INF/jsp/admin/users.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        User actor = WebUtil.getCurrentUser(req);
        try {
            if (actor == null) {
                throw new IllegalArgumentException("Not signed in");
            }
            if (action == null) {
                throw new IllegalArgumentException("Unknown action");
            }
            switch (action) {
                case "update" -> {
                    Long targetId = IdCipher.dec(req.getParameter("id"));
                    Role role = parseRole(req.getParameter("role"));
                    ExamLevel examLevel = parseExamLevel(req.getParameter("examLevel"));
                    authService.updateUser(actor.getId(), targetId, role, examLevel);
                    resp.sendRedirect(req.getContextPath() + "/admin/users");
                }
                case "delete" -> {
                    Long targetId = IdCipher.dec(req.getParameter("id"));
                    authService.deleteUser(actor.getId(), targetId);
                    resp.sendRedirect(req.getContextPath() + "/admin/users");
                }
                default -> throw new IllegalArgumentException("Unknown action");
            }
        } catch (Exception e) {
            req.setAttribute("error", Messages.fromException(req, e.getMessage()));
            doGet(req, resp);
        }
    }

    private static Role parseRole(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        try {
            return Role.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Role must be ADMIN or USER");
        }
    }

    private static ExamLevel parseExamLevel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            ExamLevel examLevel = ExamLevel.fromString(value);
            if (examLevel == null) {
                throw new IllegalArgumentException("Exam level must be Professional or Sub-Professional");
            }
            return examLevel;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("Exam level")) {
                throw e;
            }
            throw new IllegalArgumentException("Exam level must be Professional or Sub-Professional");
        }
    }
}
