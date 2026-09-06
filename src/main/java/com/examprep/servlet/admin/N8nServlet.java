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
            User admin = WebUtil.getCurrentUser(req);
            req.setAttribute("questionsConfigured", n8nService.isQuestionsConfigured());
            req.setAttribute("analyzeConfigured", n8nService.isAnalyzeConfigured());
            req.setAttribute("driveConfigured", n8nService.isDriveConfigured());
            req.setAttribute("driveOAuthConfigured", n8nService.isOAuthConfigured());
            req.setAttribute("driveConnected", n8nService.isDriveConnected(admin));
            req.setAttribute("driveEmail", n8nService.connectedGoogleEmail(admin));
            req.setAttribute("recentRequests", n8nService.recentRequests());
            req.setAttribute("suggestedBatchLabel", QuestionImportService.suggestedBatchLabel());
            applyDriveFlash(req);
            loadDriveFiles(req, admin);
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
                            req.getParameter("batchLabel"),
                            req.getParameterValues("driveFileIds"),
                            req.getParameter("driveFolderId"));
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
        req.setAttribute("selectedDriveFileIds", req.getParameterValues("driveFileIds"));
        req.setAttribute("formDriveFolderId", req.getParameter("driveFolderId"));
    }

    private void loadDriveFiles(HttpServletRequest req, User admin) {
        boolean oauthReady;
        try {
            oauthReady = n8nService.isDriveConnected(admin);
        } catch (Exception e) {
            req.setAttribute("driveError", Messages.get(req, "error.n8n.drive.listFailed"));
            return;
        }
        if (!oauthReady && !n8nService.isDriveConfigured()) {
            return;
        }
        try {
            String folderId = req.getParameter("folder");
            if (folderId == null || folderId.isBlank()) {
                Object remembered = req.getAttribute("formDriveFolderId");
                if (remembered instanceof String rememberedFolder && !rememberedFolder.isBlank()) {
                    folderId = rememberedFolder;
                }
            }
            var listing = n8nService.listDrive(admin, folderId);
            req.setAttribute("driveListing", listing);
            req.setAttribute("driveFiles", listing.getFiles());
            req.setAttribute("driveFolders", listing.getFolders());
            req.setAttribute("driveFolderId", listing.getFolderId());
            req.setAttribute("driveFolderName", listing.getFolderName());
            req.setAttribute("driveParentId", listing.getParentId());
        } catch (RuntimeException e) {
            req.setAttribute("driveError", Messages.get(req, "error.n8n.drive.listFailed"));
        } catch (Exception e) {
            req.setAttribute("driveError", Messages.get(req, "error.n8n.drive.listFailed"));
        }
    }

    private static void applyDriveFlash(HttpServletRequest req) {
        String drive = req.getParameter("drive");
        if (drive == null) {
            return;
        }
        switch (drive) {
            case "connected" -> req.setAttribute("success", Messages.get(req, "n8n.drive.connected"));
            case "disconnected" -> req.setAttribute("success", Messages.get(req, "n8n.drive.disconnected"));
            case "denied" -> req.setAttribute("error", Messages.get(req, "error.n8n.drive.denied"));
            case "error" -> req.setAttribute("error", Messages.get(req, "error.n8n.drive.signInFailed"));
            default -> {
            }
        }
    }
}
