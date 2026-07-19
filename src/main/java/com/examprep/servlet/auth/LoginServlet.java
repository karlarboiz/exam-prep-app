package com.examprep.servlet.auth;

import com.examprep.model.User;
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
import java.util.Optional;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(LoginServlet.class);

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User currentUser = WebUtil.getCurrentUser(req);
        if (currentUser != null) {
            redirectToDashboard(currentUser, req, resp);
            return;
        }
        req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("Login attempt with missing username or password");
            req.setAttribute("error", "Username and password are required");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
            return;
        }

        try {
            log.debug("Authenticating username={}", username.trim());
            Optional<User> userOpt = authService.authenticate(username.trim(), password);
            if (userOpt.isEmpty()) {
                log.warn("Failed login for username={}", username.trim());
                req.setAttribute("error", "Invalid username or password");
                req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
                return;
            }
            User user = userOpt.get();
            String token = authService.issueToken(user);
            WebUtil.setAuthCookie(resp, token);
            log.info("User {} logged in as {}", user.getUsername(), user.getRole());
            redirectToDashboard(user, req, resp);
        } catch (Exception e) {
            log.error("Login failed for username={}", username.trim(), e);
            req.setAttribute("error", "Login failed. Please try again.");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
        }
    }

    private void redirectToDashboard(User user, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (user.isAdmin()) {
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        } else {
            resp.sendRedirect(req.getContextPath() + "/user/dashboard");
        }
    }
}
