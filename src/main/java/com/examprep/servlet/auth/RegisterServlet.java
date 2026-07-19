package com.examprep.servlet.auth;

import com.examprep.model.User;
import com.examprep.service.AccessGrantService;
import com.examprep.service.AuthService;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(RegisterServlet.class);

    private final AuthService authService = new AuthService();
    private final AccessGrantService accessGrantService = new AccessGrantService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User currentUser = WebUtil.getCurrentUser(req);
        if (currentUser != null) {
            resp.sendRedirect(req.getContextPath() + "/user/dashboard");
            return;
        }

        String token = req.getParameter("token");
        if (token == null || token.isBlank()) {
            req.setAttribute("error", "An access token is required to register. Purchase a subscription to receive one.");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
            return;
        }

        try {
            accessGrantService.requireUnusedToken(token.trim());
            req.setAttribute("accessToken", token.trim());
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid registration token on GET: {}", e.getMessage());
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
        } catch (Exception e) {
            log.error("Failed to validate registration token", e);
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
            forwardWithForm(req, resp, token, username, email);
            return;
        }

        if (username == null || username.isBlank() || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            req.setAttribute("error", "All fields are required");
            forwardWithForm(req, resp, token, username, email);
            return;
        }

        if (!password.equals(confirmPassword)) {
            req.setAttribute("error", "Passwords do not match");
            forwardWithForm(req, resp, token, username, email);
            return;
        }

        if (password.length() < 6) {
            req.setAttribute("error", "Password must be at least 6 characters");
            forwardWithForm(req, resp, token, username, email);
            return;
        }

        try {
            log.debug("Registering username={} email={}", username.trim(), email.trim());
            User user = accessGrantService.registerWithToken(
                    token.trim(), username.trim(), email.trim(), password);
            String sessionToken = authService.issueToken(user);
            WebUtil.setAuthCookie(resp, sessionToken);
            log.info("Registered and logged in user={}", user.getUsername());
            resp.sendRedirect(req.getContextPath() + "/user/dashboard");
        } catch (IllegalArgumentException e) {
            log.warn("Registration rejected for username={}: {}", username, e.getMessage());
            req.setAttribute("error", e.getMessage());
            forwardWithForm(req, resp, token, username, email);
        } catch (Exception e) {
            log.error("Registration failed for username={}", username, e);
            req.setAttribute("error", "Registration failed. Please try again.");
            forwardWithForm(req, resp, token, username, email);
        }
    }

    private void forwardWithForm(HttpServletRequest req, HttpServletResponse resp,
                                 String token, String username, String email)
            throws ServletException, IOException {
        req.setAttribute("accessToken", token);
        req.setAttribute("username", username);
        req.setAttribute("email", email);
        req.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(req, resp);
    }
}
