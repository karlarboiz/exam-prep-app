package com.examprep.servlet.admin;

import com.examprep.service.AdminService;
import com.examprep.util.IdCipher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/subjects")
public class SubjectServlet extends HttpServlet {

    private final AdminService adminService = new AdminService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String editId = req.getParameter("edit");
            if (editId != null) {
                try {
                    long id = IdCipher.dec(editId);
                    adminService.getSubject(id).ifPresent(s -> req.setAttribute("editSubject", s));
                } catch (IllegalArgumentException ignored) {
                    // Bad/garbage token — show create form instead of 500
                }
            }
            req.setAttribute("subjects", adminService.getAllSubjects());
            req.getRequestDispatcher("/WEB-INF/jsp/admin/subjects.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            switch (action) {
                case "create" -> {
                    String name = req.getParameter("name");
                    String description = req.getParameter("description");
                    boolean professional = req.getParameter("professional") != null;
                    boolean subProfessional = req.getParameter("subProfessional") != null;
                    if (name == null || name.isBlank()) {
                        req.setAttribute("error", "Name is required");
                        doGet(req, resp);
                        return;
                    }
                    if (!professional && !subProfessional) {
                        req.setAttribute("error", "Select at least one level: Professional or Sub-Professional");
                        doGet(req, resp);
                        return;
                    }
                    adminService.createSubject(
                            name.trim(),
                            description != null ? description.trim() : "",
                            professional,
                            subProfessional);
                }
                case "update" -> {
                    Long id = IdCipher.dec(req.getParameter("id"));
                    String name = req.getParameter("name");
                    String description = req.getParameter("description");
                    boolean professional = req.getParameter("professional") != null;
                    boolean subProfessional = req.getParameter("subProfessional") != null;
                    if (name == null || name.isBlank()) {
                        req.setAttribute("error", "Name is required");
                        doGet(req, resp);
                        return;
                    }
                    if (!professional && !subProfessional) {
                        req.setAttribute("error", "Select at least one level: Professional or Sub-Professional");
                        doGet(req, resp);
                        return;
                    }
                    adminService.updateSubject(
                            id,
                            name.trim(),
                            description != null ? description.trim() : "",
                            professional,
                            subProfessional);
                }
                case "delete" -> {
                    Long id = IdCipher.dec(req.getParameter("id"));
                    adminService.deleteSubject(id);
                }
                default -> throw new IllegalArgumentException("Unknown action");
            }
            resp.sendRedirect(req.getContextPath() + "/admin/subjects");
        } catch (Exception e) {
            req.setAttribute("error", e.getMessage());
            doGet(req, resp);
        }
    }
}
