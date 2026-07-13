package com.examprep.servlet.admin;

import com.examprep.service.AdminService;
import com.examprep.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    private final AdminService adminService = new AdminService();
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("subjects", adminService.getAllSubjects());
            req.setAttribute("exams", adminService.getAllExams());
            req.setAttribute("users", authService.findAllUsers());
            req.setAttribute("questions", adminService.getAllQuestions());
            req.getRequestDispatcher("/webapp/WEB-INF/jsp/admin/dashboard.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
