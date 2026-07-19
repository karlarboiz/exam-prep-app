package com.examprep.servlet.admin;

import com.examprep.model.Exam;
import com.examprep.service.AdminService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet("/admin/exams")
public class ExamServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ExamServlet.class);

    private final AdminService adminService = new AdminService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String editId = req.getParameter("edit");
            if (editId != null) {
                Long examId = Long.parseLong(editId);
                adminService.getExam(examId).ifPresent(exam -> {
                    req.setAttribute("editExam", exam);
                    try {
                        req.setAttribute("selectedQuestionIds", adminService.getExamQuestionIds(examId));
                    } catch (Exception e) {
                        log.error("Failed to load question ids for examId={}", examId, e);
                        throw new RuntimeException(e);
                    }
                });
            }
            req.setAttribute("exams", adminService.getAllExams());
            req.setAttribute("subjects", adminService.getAllSubjects());
            req.setAttribute("questions", adminService.getAllQuestions());
            req.getRequestDispatcher("/WEB-INF/jsp/admin/exams.jsp").forward(req, resp);
        } catch (Exception e) {
            log.error("Failed to render exams page", e);
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            switch (action) {
                case "create", "update" -> {
                    Exam exam = new Exam();
                    if ("update".equals(action)) {
                        exam.setId(Long.parseLong(req.getParameter("id")));
                    }
                    exam.setSubjectId(Long.parseLong(req.getParameter("subjectId")));
                    exam.setTitle(req.getParameter("title").trim());
                    exam.setDurationMinutes(Integer.parseInt(req.getParameter("durationMinutes")));
                    exam.setActive("on".equals(req.getParameter("active")) || "true".equals(req.getParameter("active")));

                    String[] questionIdParams = req.getParameterValues("questionIds");
                    List<Long> questionIds = questionIdParams == null ? List.of() :
                            Arrays.stream(questionIdParams).map(Long::parseLong).collect(Collectors.toList());

                    if ("create".equals(action)) {
                        adminService.createExam(exam, questionIds);
                    } else {
                        adminService.updateExam(exam, questionIds);
                    }
                }
                case "delete" -> adminService.deleteExam(Long.parseLong(req.getParameter("id")));
                default -> throw new IllegalArgumentException("Unknown action");
            }
            log.info("Exam action={} completed", action);
            resp.sendRedirect(req.getContextPath() + "/admin/exams");
        } catch (Exception e) {
            log.error("Exam action={} failed", action, e);
            req.setAttribute("error", e.getMessage());
            doGet(req, resp);
        }
    }
}
