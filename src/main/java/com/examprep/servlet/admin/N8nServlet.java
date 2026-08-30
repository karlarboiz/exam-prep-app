package com.examprep.servlet.admin;

import com.examprep.i18n.Messages;
import com.examprep.model.User;
import com.examprep.service.N8nService;
import com.examprep.service.QuestionImportService;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;

@WebServlet("/admin/n8n")
@MultipartConfig(maxFileSize = N8nService.MAX_FILE_BYTES, maxRequestSize = N8nService.MAX_FILE_BYTES + (2 * 1024 * 1024))
public class N8nServlet extends HttpServlet {

    private final N8nService n8nService = new N8nService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("questionsConfigured", n8nService.isQuestionsConfigured());
            req.setAttribute("analyzeConfigured", n8nService.isAnalyzeConfigured());
            req.setAttribute("recentRequests", n8nService.recentRequests());
            req.setAttribute("suggestedBatchLabel", QuestionImportService.suggestedBatchLabel());
            req.getRequestDispatcher("/WEB-INF/jsp/admin/n8n.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User admin = WebUtil.getCurrentUser(req);
        String action = req.getParameter("action");
        rememberForm(req);
        try {
            if (action == null) {
                throw new IllegalArgumentException("Unknown action");
            }
            switch (action) {
                case "questions" -> {
                    n8nService.requestQuestions(
                            admin,
                            req.getParameter("message"),
                            req.getParameter("subject"),
                            req.getParameter("count"),
                            req.getParameter("difficulty"),
                            req.getParameter("batchLabel"));
                    req.setAttribute("success", Messages.get(req, "n8n.questions.sent"));
                }
                case "analyze" -> {
                    Part filePart = req.getPart("file");
                    if (filePart == null || filePart.getSize() == 0) {
                        throw new IllegalArgumentException("Choose a file to analyze");
                    }
                    byte[] bytes;
                    try (InputStream in = filePart.getInputStream()) {
                        bytes = in.readAllBytes();
                    }
                    n8nService.analyzeFile(
                            admin,
                            filePart.getSubmittedFileName(),
                            filePart.getContentType(),
                            bytes,
                            req.getParameter("analyzeMessage"));
                    req.setAttribute("success", Messages.get(req, "n8n.analyze.sent"));
                }
                default -> throw new IllegalArgumentException("Unknown action");
            }
            doGet(req, resp);
        } catch (IllegalArgumentException | IllegalStateException e) {
            req.setAttribute("error", Messages.fromException(req, e.getMessage()));
            doGet(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", Messages.get(req, "error.unexpected"));
            doGet(req, resp);
        }
    }

    private static void rememberForm(HttpServletRequest req) {
        req.setAttribute("formMessage", req.getParameter("message"));
        req.setAttribute("formSubject", req.getParameter("subject"));
        req.setAttribute("formCount", req.getParameter("count"));
        req.setAttribute("formDifficulty", req.getParameter("difficulty"));
        req.setAttribute("formBatchLabel", req.getParameter("batchLabel"));
        req.setAttribute("formAnalyzeMessage", req.getParameter("analyzeMessage"));
    }
}
