package com.examprep.servlet.auth;

import com.examprep.i18n.Messages;
import com.examprep.service.AuthService;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (WebUtil.getCurrentUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/account");
            return;
        }

        String queryToken = req.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
            req.getSession(true).setAttribute(AuthService.RESET_TOKEN_ATTR, queryToken.trim());
            resp.sendRedirect(req.getContextPath() + "/reset-password");
            return;
        }

        String token = sessionToken(req);
        if (token == null) {
            req.setAttribute("error", Messages.get(req, "error.reset.invalid"));
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
            return;
        }

        try {
            if (authService.peekResetToken(token).isEmpty()) {
                clearSessionToken(req);
                req.setAttribute("error", Messages.get(req, "error.reset.invalid"));
            } else {
                req.setAttribute("resetReady", true);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
        req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = sessionToken(req);
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");
        try {
            authService.resetPassword(token, newPassword, confirmPassword);
            clearSessionToken(req);
            resp.sendRedirect(req.getContextPath() + "/login?reset=1");
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", Messages.fromException(req, e.getMessage()));
            req.setAttribute("resetReady", sessionToken(req) != null);
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", Messages.get(req, "error.reset.failed"));
            req.setAttribute("resetReady", sessionToken(req) != null);
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
        }
    }

    private static String sessionToken(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(AuthService.RESET_TOKEN_ATTR);
        return value instanceof String s && !s.isBlank() ? s : null;
    }

    private static void clearSessionToken(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.removeAttribute(AuthService.RESET_TOKEN_ATTR);
        }
    }
}
