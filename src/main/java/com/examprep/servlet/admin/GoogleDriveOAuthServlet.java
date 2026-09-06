package com.examprep.servlet.admin;

import com.examprep.model.User;
import com.examprep.service.GoogleOAuthService;
import com.examprep.util.WebUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet({"/admin/n8n/drive/connect", "/admin/n8n/drive/callback", "/admin/n8n/drive/disconnect"})
public class GoogleDriveOAuthServlet extends HttpServlet {

    private final GoogleOAuthService oauthService = new GoogleOAuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (!path.endsWith("/disconnect")) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        User admin = WebUtil.getCurrentUser(req);
        try {
            oauthService.disconnect(admin);
        } catch (SQLException e) {
            resp.sendRedirect(req.getContextPath() + "/admin/n8n?drive=error");
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/admin/n8n?drive=disconnected");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User admin = WebUtil.getCurrentUser(req);
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.endsWith("/disconnect")) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        if (path.endsWith("/connect")) {
            startConnect(admin, req, resp);
            return;
        }
        handleCallback(admin, req, resp);
    }

    private void startConnect(User admin, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (admin == null || !oauthService.isConfigured()) {
            resp.sendRedirect(req.getContextPath() + "/admin/n8n?drive=error");
            return;
        }
        String state = oauthService.newState();
        req.getSession(true).setAttribute(GoogleOAuthService.STATE_ATTR, state);
        resp.sendRedirect(oauthService.authorizationUrl(oauthService.redirectUri(req), state));
    }

    private void handleCallback(User admin, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String error = req.getParameter("error");
        if (error != null && !error.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/admin/n8n?drive=denied");
            return;
        }
        HttpSession session = req.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(GoogleOAuthService.STATE_ATTR);
        String state = req.getParameter("state");
        if (expected == null || state == null || !expected.equals(state)) {
            resp.sendRedirect(req.getContextPath() + "/admin/n8n?drive=error");
            return;
        }
        session.removeAttribute(GoogleOAuthService.STATE_ATTR);
        try {
            oauthService.completeLogin(admin, req.getParameter("code"), oauthService.redirectUri(req));
            resp.sendRedirect(req.getContextPath() + "/admin/n8n?drive=connected");
        } catch (IllegalArgumentException | IllegalStateException | SQLException e) {
            resp.sendRedirect(req.getContextPath() + "/admin/n8n?drive=error");
        }
    }
}
