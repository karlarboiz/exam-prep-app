package com.examprep.servlet.admin;

import com.examprep.model.Question;
import com.examprep.service.AdminService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@WebServlet("/admin/questions")
public class QuestionServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(QuestionServlet.class);

    private final AdminService adminService = new AdminService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String subjectId = req.getParameter("subjectId");
            String editId = req.getParameter("edit");

            if (editId != null) {
                adminService.getQuestion(Long.parseLong(editId)).ifPresent(q -> req.setAttribute("editQuestion", q));
            }

            if (subjectId != null && !subjectId.isBlank()) {
                req.setAttribute("questions", adminService.getQuestionsBySubject(Long.parseLong(subjectId)));
                req.setAttribute("filterSubjectId", Long.parseLong(subjectId));
            } else {
                req.setAttribute("questions", adminService.getAllQuestions());
            }

            req.setAttribute("subjects", adminService.getAllSubjects());
            req.getRequestDispatcher("/WEB-INF/jsp/admin/questions.jsp").forward(req, resp);
        } catch (Exception e) {
            log.error("Failed to render questions page", e);
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            switch (action) {
                case "create", "update" -> {
                    Question question = buildQuestion(req);
                    if ("create".equals(action)) {
                        adminService.createQuestion(question);
                    } else {
                        question.setId(Long.parseLong(req.getParameter("id")));
                        adminService.updateQuestion(question);
                    }
                }
                case "delete" -> adminService.deleteQuestion(Long.parseLong(req.getParameter("id")));
                default -> throw new IllegalArgumentException("Unknown action");
            }
            log.info("Question action={} completed", action);
            resp.sendRedirect(req.getContextPath() + "/admin/questions");
        } catch (Exception e) {
            log.error("Question action={} failed", action, e);
            req.setAttribute("error", e.getMessage());
            doGet(req, resp);
        }
    }

    private Question buildQuestion(HttpServletRequest req) {
        Question question = new Question();
        question.setSubjectId(Long.parseLong(req.getParameter("subjectId")));
        question.setPrompt(req.getParameter("prompt").trim());
        question.setOptionA(req.getParameter("optionA").trim());
        question.setOptionB(req.getParameter("optionB").trim());
        question.setOptionC(req.getParameter("optionC").trim());
        question.setOptionD(req.getParameter("optionD").trim());
        question.setCorrectOption(req.getParameter("correctOption").toUpperCase());
        String difficulty = req.getParameter("difficulty");
        question.setDifficulty(difficulty != null ? difficulty : "MEDIUM");
        return question;
    }
}
