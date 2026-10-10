package com.examprep.servlet.user;

import com.examprep.model.AttemptStatus;
import com.examprep.model.DiagnosticResult;
import com.examprep.model.ExamAttempt;
import com.examprep.model.User;
import com.examprep.service.DiagnosticService;
import com.examprep.util.IdCipher;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/user/diagnostic/result")
public class DiagnosticResultServlet extends HttpServlet {

    private final DiagnosticService diagnosticService = new DiagnosticService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.getCurrentUser(req);
        String attemptIdParam = req.getParameter("attemptId");
        if (attemptIdParam == null) {
            resp.sendRedirect(req.getContextPath() + "/user/diagnostic");
            return;
        }

        try {
            Long attemptId = IdCipher.dec(attemptIdParam);
            ExamAttempt attempt = diagnosticService.getAttempt(attemptId);
            if (!attempt.getUserId().equals(user.getId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // Expired / abandoned attempts do not clear the gate — send user to retake
            if (attempt.getStatus() != AttemptStatus.COMPLETED
                    && !diagnosticService.isDiagnosticCompleted(user.getId())) {
                resp.sendRedirect(req.getContextPath() + "/user/diagnostic?retake=1");
                return;
            }

            DiagnosticResult result = diagnosticService.getResult(attemptId);
            req.setAttribute("result", result);
            req.setAttribute("attempt", result.getAttempt());
            req.setAttribute("subjectScores", result.getSubjectScores());
            req.setAttribute("answers", result.getAnswers());
            req.setAttribute("readiness", result.getReadiness());
            req.setAttribute("meanSubjectPercent", result.getMeanSubjectPercent());
            req.getRequestDispatcher("/WEB-INF/jsp/user/diagnostic-result.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
