package com.examprep.servlet.admin;

import com.examprep.importing.QuestionImportResult;
import com.examprep.model.Question;
import com.examprep.service.AdminService;
import com.examprep.service.QuestionImportService;
import com.examprep.util.IdCipher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@WebServlet("/admin/questions")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 12 * 1024 * 1024)
public class QuestionServlet extends HttpServlet {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final AdminService adminService = new AdminService();
    private final QuestionImportService importService = new QuestionImportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String downloadAction = req.getParameter("action");
            if ("template".equals(downloadAction)) {
                writeExcel(resp, "question-import-template.xlsx", importService::writeTemplate);
                return;
            }
            if ("export".equals(downloadAction)) {
                List<Question> questions = loadFilteredQuestions(req.getParameter("subjectId"));
                writeExcel(resp, "questions.xlsx", out -> importService.exportQuestions(questions, out));
                return;
            }

            String subjectId = req.getParameter("subjectId");
            String editId = req.getParameter("edit");

            if (editId != null) {
                try {
                    adminService.getQuestion(IdCipher.dec(editId))
                            .ifPresent(q -> req.setAttribute("editQuestion", q));
                } catch (IllegalArgumentException ignored) {
                    // Bad/garbage token — show create form instead of 500
                }
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
                        question.setId(IdCipher.dec(req.getParameter("id")));
                        adminService.updateQuestion(question);
                    }
                    resp.sendRedirect(req.getContextPath() + "/admin/questions");
                }
                case "delete" -> {
                    adminService.deleteQuestion(IdCipher.dec(req.getParameter("id")));
                    resp.sendRedirect(req.getContextPath() + "/admin/questions");
                }
                case "import" -> {
                    handleImport(req);
                    doGet(req, resp);
                }
                default -> throw new IllegalArgumentException("Unknown action");
            }
        } catch (Exception e) {
            req.setAttribute("error", e.getMessage());
            doGet(req, resp);
        }
    }

    private void handleImport(HttpServletRequest req) throws Exception {
        Part filePart = req.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            throw new IllegalArgumentException("Please choose an .xlsx file to import");
        }
        String fileName = filePart.getSubmittedFileName();
        if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx files are supported");
        }

        try (InputStream in = filePart.getInputStream()) {
            QuestionImportResult result = importService.importFromExcel(in);
            req.setAttribute("importCount", result.getImportedCount());
            req.setAttribute("updateCount", result.getUpdatedCount());
            if (result.hasErrors()) {
                req.setAttribute("importErrors", result.getErrors());
            }
            boolean wroteRows = result.getImportedCount() > 0 || result.getUpdatedCount() > 0;
            if (!wroteRows && result.hasErrors()) {
                req.setAttribute("error", "Import completed with no rows inserted or updated. See errors below.");
            } else if (wroteRows) {
                req.setAttribute("importSuccess", importSuccessMessage(result));
            }
        }
    }

    private static String importSuccessMessage(QuestionImportResult result) {
        StringBuilder message = new StringBuilder();
        if (result.getImportedCount() > 0) {
            message.append("Imported ").append(result.getImportedCount()).append(" question(s).");
        }
        if (result.getUpdatedCount() > 0) {
            if (!message.isEmpty()) {
                message.append(' ');
            }
            message.append("Updated ").append(result.getUpdatedCount()).append(" existing question(s).");
        }
        return message.toString();
    }

    private List<Question> loadFilteredQuestions(String subjectId) throws Exception {
        if (subjectId != null && !subjectId.isBlank()) {
            return adminService.getQuestionsBySubject(Long.parseLong(subjectId));
        }
        return adminService.getAllQuestions();
    }

    private void writeExcel(HttpServletResponse resp, String filename, ExcelBody writer) throws Exception {
        resp.setContentType(XLSX_CONTENT_TYPE);
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        writer.write(resp.getOutputStream());
    }

    @FunctionalInterface
    private interface ExcelBody {
        void write(OutputStream out) throws Exception;
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
        String explanation = req.getParameter("explanation");
        if (explanation != null && !explanation.isBlank()) {
            question.setExplanation(explanation.trim());
        }
        return question;
    }
}
