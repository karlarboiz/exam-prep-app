package com.examprep.servlet.auth;

import com.examprep.model.AccessGrant;
import com.examprep.model.ExamLevel;
import com.examprep.model.User;
import com.examprep.service.AccessGrantService;
import com.examprep.service.AuthService;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final AuthService authService = new AuthService();
    private final AccessGrantService accessGrantService = new AccessGrantService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User currentUser = WebUtil.getCurrentUser(req);
        if (currentUser != null) {
            if (currentUser.isDiagnosticCompleted()) {
                resp.sendRedirect(req.getContextPath() + "/user/dashboard");
            } else {
                resp.sendRedirect(req.getContextPath() + "/user/diagnostic");
            }
            return;
        }

        String token = req.getParameter("token");
        if (token == null || token.isBlank()) {
            req.setAttribute("error", "An access token is required to register. Purchase a subscription to receive one.");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }

        try {
            AccessGrant grant = accessGrantService.requireUnusedToken(token.trim());
            req.setAttribute("accessToken", token.trim());
            req.setAttribute("examLevel", grant.getExamLevel());
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        if (token == null || token.isBlank()) {
            req.setAttribute("error", "An access token is required to register");
            forwardWithForm(req, resp, token, username, email, null);
            return;
        }

        ExamLevel examLevelFromGrant = null;
        try {
            examLevelFromGrant = accessGrantService.requireUnusedToken(token.trim()).getExamLevel();
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            forwardWithForm(req, resp, token, username, email, null);
            return;
        } catch (Exception e) {
            throw new ServletException(e);
        }

        if (username == null || username.isBlank() || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            req.setAttribute("error", "All fields are required");
            forwardWithForm(req, resp, token, username, email, examLevelFromGrant);
            return;
        }

        if (!password.equals(confirmPassword)) {
            req.setAttribute("error", "Passwords do not match");
            forwardWithForm(req, resp, token, username, email, examLevelFromGrant);
            return;
        }

        if (password.length() < 6) {
            req.setAttribute("error", "Password must be at least 6 characters");
            forwardWithForm(req, resp, token, username, email, examLevelFromGrant);
            return;
        }

        try {
            User user = accessGrantService.registerWithToken(
                    token.trim(), username.trim(), email.trim(), password);
            String sessionToken = authService.issueToken(user);
            WebUtil.setAuthCookie(resp, sessionToken);
            resp.sendRedirect(req.getContextPath() + "/user/diagnostic");
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            forwardWithForm(req, resp, token, username, email, examLevelFromGrant);
        } catch (Exception e) {
            req.setAttribute("error", "Registration failed. Please try again.");
            forwardWithForm(req, resp, token, username, email, examLevelFromGrant);
        }
    }

    private void forwardWithForm(HttpServletRequest req, HttpServletResponse resp,
                                 String token, String username, String email, ExamLevel examLevel)
            throws ServletException, IOException {
        req.setAttribute("accessToken", token);
        req.setAttribute("username", username);
        req.setAttribute("email", email);
        req.setAttribute("examLevel", examLevel);
        req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
    }
}
