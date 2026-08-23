package com.examprep.servlet.auth;

import com.examprep.i18n.LocaleSupport;
import com.examprep.i18n.Messages;
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
import jakarta.servlet.http.HttpSession;

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

        String queryToken = req.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
            req.getSession(true).setAttribute(AuthService.REGISTER_TOKEN_ATTR, queryToken.trim());
            resp.sendRedirect(req.getContextPath() + "/register");
            return;
        }

        showRegisterForm(req, resp, sessionToken(req), null, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("claim".equals(action)) {
            String pasted = req.getParameter("token");
            if (pasted == null || pasted.isBlank()) {
                req.setAttribute("error", Messages.get(req, "error.register.tokenRequiredShort"));
                showRegisterForm(req, resp, null, null, null, null);
                return;
            }
            req.getSession(true).setAttribute(AuthService.REGISTER_TOKEN_ATTR, pasted.trim());
            resp.sendRedirect(req.getContextPath() + "/register");
            return;
        }

        String token = firstNonBlank(req.getParameter("token"), sessionToken(req));
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        if (token == null || token.isBlank()) {
            req.setAttribute("error", Messages.get(req, "error.register.tokenRequiredShort"));
            showRegisterForm(req, resp, token, username, email, null);
            return;
        }

        ExamLevel examLevelFromGrant = null;
        try {
            examLevelFromGrant = accessGrantService.requireUnusedToken(token.trim()).getExamLevel();
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", Messages.fromException(req, e.getMessage()));
            showRegisterForm(req, resp, token, username, email, null);
            return;
        } catch (Exception e) {
            throw new ServletException(e);
        }

        if (username == null || username.isBlank() || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            req.setAttribute("error", Messages.get(req, "error.register.fields"));
            showRegisterForm(req, resp, token, username, email, examLevelFromGrant);
            return;
        }

        if (!password.equals(confirmPassword)) {
            req.setAttribute("error", Messages.get(req, "error.register.mismatch"));
            showRegisterForm(req, resp, token, username, email, examLevelFromGrant);
            return;
        }

        if (password.length() < 6) {
            req.setAttribute("error", Messages.get(req, "error.password.tooShort"));
            showRegisterForm(req, resp, token, username, email, examLevelFromGrant);
            return;
        }

        try {
            User user = accessGrantService.registerWithToken(
                    token.trim(), username.trim(), email.trim(), password);
            clearSessionToken(req);
            authService.updateLocale(user.getId(), LocaleSupport.current(req));
            String sessionToken = authService.issueToken(user);
            WebUtil.setAuthCookie(req, resp, sessionToken);
            resp.sendRedirect(req.getContextPath() + "/user/diagnostic");
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", Messages.fromException(req, e.getMessage()));
            showRegisterForm(req, resp, token, username, email, examLevelFromGrant);
        } catch (Exception e) {
            req.setAttribute("error", Messages.get(req, "error.register.failed"));
            showRegisterForm(req, resp, token, username, email, examLevelFromGrant);
        }
    }

    private void showRegisterForm(HttpServletRequest req, HttpServletResponse resp,
                                  String token, String username, String email, ExamLevel examLevel)
            throws ServletException, IOException {
        if (token != null && !token.isBlank()) {
            try {
                AccessGrant grant = accessGrantService.requireUnusedToken(token.trim());
                req.setAttribute("accessToken", token.trim());
                req.setAttribute("examLevel", examLevel != null ? examLevel : grant.getExamLevel());
            } catch (IllegalArgumentException e) {
                if (req.getAttribute("error") == null) {
                    req.setAttribute("error", Messages.fromException(req, e.getMessage()));
                }
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
        req.setAttribute("username", username);
        req.setAttribute("email", email);
        req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
    }

    private static String sessionToken(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(AuthService.REGISTER_TOKEN_ATTR);
        return value instanceof String s && !s.isBlank() ? s : null;
    }

    private static void clearSessionToken(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.removeAttribute(AuthService.REGISTER_TOKEN_ATTR);
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }
}
