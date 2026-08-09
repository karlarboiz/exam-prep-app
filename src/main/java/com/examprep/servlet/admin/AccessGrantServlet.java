package com.examprep.servlet.admin;

import com.examprep.service.AccessGrantService;
import com.examprep.util.IdCipher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/access-grants")
public class AccessGrantServlet extends HttpServlet {

    private final AccessGrantService accessGrantService = new AccessGrantService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("grants", accessGrantService.listAll());
            req.getRequestDispatcher("/WEB-INF/jsp/admin/access-grants.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("revoke".equals(action)) {
                Long id = IdCipher.dec(req.getParameter("id"));
                accessGrantService.revoke(id);
                resp.sendRedirect(req.getContextPath() + "/admin/access-grants");
                return;
            }
            req.setAttribute("error", "Unknown action");
            doGet(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", e.getMessage());
            doGet(req, resp);
        }
    }
}
