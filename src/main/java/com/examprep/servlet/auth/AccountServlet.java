package com.examprep.servlet.auth;

import com.examprep.i18n.Messages;
import com.examprep.model.User;
import com.examprep.service.AuthService;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/account")
public class AccountServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (WebUtil.getCurrentUser(req) == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if ("1".equals(req.getParameter("changed"))) {
            req.setAttribute("success", Messages.get(req, "error.account.updated"));
        }
        req.getRequestDispatcher("/WEB-INF/jsp/auth/account.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User currentUser = WebUtil.getCurrentUser(req);
        if (currentUser == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String currentPassword = req.getParameter("currentPassword");
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");

        try {
            authService.changePassword(currentUser.getId(), currentPassword, newPassword, confirmPassword);
            resp.sendRedirect(req.getContextPath() + "/account?changed=1");
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", Messages.fromException(req, e.getMessage()));
            req.getRequestDispatcher("/WEB-INF/jsp/auth/account.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", Messages.get(req, "error.account.failed"));
            req.getRequestDispatcher("/WEB-INF/jsp/auth/account.jsp").forward(req, resp);
        }
    }
}
