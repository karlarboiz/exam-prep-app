package com.examprep.servlet.auth;

import com.examprep.config.AppConfig;
import com.examprep.i18n.LocaleSupport;
import com.examprep.i18n.Messages;
import com.examprep.model.AppLocale;
import com.examprep.model.User;
import com.examprep.service.AuthService;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User currentUser = WebUtil.getCurrentUser(req);
        if (currentUser != null) {
            redirectToDashboard(currentUser, req, resp);
            return;
        }
        if ("1".equals(req.getParameter("reset"))) {
            req.setAttribute("success", Messages.get(req, "login.resetSuccess"));
        }
        req.setAttribute("showAdminHint", !AppConfig.isProduction());
        req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        req.setAttribute("showAdminHint", !AppConfig.isProduction());
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            req.setAttribute("error", Messages.get(req, "error.login.required"));
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
            return;
        }

        try {
            Optional<User> userOpt = authService.authenticate(username.trim(), password);
            if (userOpt.isEmpty()) {
                req.setAttribute("error", Messages.get(req, "error.login.invalid"));
                req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
                return;
            }
            User user = userOpt.get();
            String token = authService.issueToken(user);
            WebUtil.setAuthCookie(req, resp, token);
            try {
                AppLocale cookieLocale = LocaleSupport.fromCookie(req);
                if (cookieLocale != null && cookieLocale != user.getLocale()) {
                    authService.updateLocale(user.getId(), cookieLocale);
                }
            } catch (Exception ignored) {
                // Session cookie is already set; locale stays on this browser.
            }
            redirectToDashboard(user, req, resp);
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", Messages.fromException(req, e.getMessage()));
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", Messages.get(req, "error.login.failed"));
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
        }
    }

    private void redirectToDashboard(User user, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (user.isAdmin()) {
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
            return;
        }
        if (!user.isDiagnosticCompleted()) {
            resp.sendRedirect(req.getContextPath() + "/user/diagnostic");
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/user/dashboard");
    }
}
