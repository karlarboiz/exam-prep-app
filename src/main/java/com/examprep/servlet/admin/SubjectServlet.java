package com.examprep.servlet.admin;

import com.examprep.service.AdminService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@WebServlet("/admin/subjects")
public class SubjectServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SubjectServlet.class);

    private final AdminService adminService = new AdminService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String editId = req.getParameter("edit");
            if (editId != null) {
                adminService.getSubject(Long.parseLong(editId)).ifPresent(s -> req.setAttribute("editSubject", s));
            }
            req.setAttribute("subjects", adminService.getAllSubjects());
            req.getRequestDispatcher("/WEB-INF/jsp/admin/subjects.jsp").forward(req, resp);
        } catch (Exception e) {
            log.error("Failed to render subjects page", e);
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
                    if (name == null || name.isBlank()) {
                        req.setAttribute("error", "Name is required");
                        doGet(req, resp);
                        return;
                    }
                    adminService.createSubject(name.trim(), description != null ? description.trim() : "");
                }
                case "update" -> {
                    Long id = Long.parseLong(req.getParameter("id"));
                    String name = req.getParameter("name");
                    String description = req.getParameter("description");
                    adminService.updateSubject(id, name.trim(), description != null ? description.trim() : "");
                }
                case "delete" -> {
                    Long id = Long.parseLong(req.getParameter("id"));
                    adminService.deleteSubject(id);
                }
                default -> throw new IllegalArgumentException("Unknown action");
            }
            log.info("Subject action={} completed", action);
            resp.sendRedirect(req.getContextPath() + "/admin/subjects");
        } catch (Exception e) {
            log.error("Subject action={} failed", action, e);
            req.setAttribute("error", e.getMessage());
            doGet(req, resp);
        }
    }
}
