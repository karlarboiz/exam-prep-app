package com.examprep.servlet.admin;

import com.examprep.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@WebServlet("/admin/users")
public class UserServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(UserServlet.class);

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("users", authService.findAllUsers());
            req.getRequestDispatcher("/WEB-INF/jsp/admin/users.jsp").forward(req, resp);
        } catch (Exception e) {
            log.error("Failed to render admin users page", e);
            throw new ServletException(e);
        }
    }
}
