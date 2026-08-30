package com.examprep.servlet.auth;

import com.examprep.config.AppConfig;
import com.examprep.i18n.Messages;
import com.examprep.service.AuthService;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (WebUtil.getCurrentUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/account");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/jsp/auth/forgot-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (WebUtil.getCurrentUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/account");
            return;
        }
        String email = req.getParameter("email");
        try {
            authService.requestPasswordReset(email, publicBaseUrl(req));
            req.setAttribute("success", Messages.get(req, "forgot.sent"));
        } catch (Exception e) {
            req.setAttribute("success", Messages.get(req, "forgot.sent"));
        }
        req.getRequestDispatcher("/WEB-INF/jsp/auth/forgot-password.jsp").forward(req, resp);
    }

    private static String publicBaseUrl(HttpServletRequest req) {
        String configured = AppConfig.get("app.public.url", "").trim();
        if (!configured.isBlank()) {
            return configured.replaceAll("/$", "");
        }
        StringBuffer url = req.getRequestURL();
        String uri = req.getRequestURI();
        String base = url.substring(0, url.length() - uri.length());
        return base + req.getContextPath();
    }
}
