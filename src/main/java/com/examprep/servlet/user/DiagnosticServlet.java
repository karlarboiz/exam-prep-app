package com.examprep.servlet.user;

import com.examprep.model.AttemptStatus;
import com.examprep.model.ExamAttempt;
import com.examprep.model.Question;
import com.examprep.model.User;
import com.examprep.service.DiagnosticService;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/user/diagnostic")
public class DiagnosticServlet extends HttpServlet {

    private final DiagnosticService diagnosticService = new DiagnosticService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.getCurrentUser(req);
        String attemptIdParam = req.getParameter("attemptId");

        try {
            if (diagnosticService.isDiagnosticCompleted(user.getId()) && attemptIdParam == null) {
                resp.sendRedirect(req.getContextPath() + "/user/dashboard");
                return;
            }

            if (attemptIdParam != null) {
                showDiagnosticPage(Long.parseLong(attemptIdParam), user, req, resp);
                return;
            }

            ExamAttempt attempt = diagnosticService.startDiagnostic(user.getId());
            resp.sendRedirect(req.getContextPath() + "/user/diagnostic?attemptId=" + attempt.getId());
        } catch (IllegalStateException e) {
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/jsp/user/diagnostic-unavailable.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.getCurrentUser(req);
        String action = req.getParameter("action");
        Long attemptId = Long.parseLong(req.getParameter("attemptId"));

        try {
            ExamAttempt attempt = diagnosticService.getAttempt(attemptId);
            if (!attempt.getUserId().equals(user.getId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if ("submit".equals(action)) {
                Map<Long, String> answers = collectAnswers(req, attemptId);
                ExamAttempt completed = diagnosticService.submitDiagnostic(attemptId, answers);
                resp.sendRedirect(req.getContextPath() + "/user/diagnostic/result?attemptId=" + completed.getId());
                return;
            }

            if ("answer".equals(action)) {
                Long questionId = Long.parseLong(req.getParameter("questionId"));
                String selected = req.getParameter("selectedOption");
                if (selected != null && !selected.isBlank()) {
                    diagnosticService.saveAnswer(attemptId, questionId, selected);
                }
                if ("1".equals(req.getParameter("ajax"))) {
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    return;
                }
                showDiagnosticPage(attemptId, user, req, resp);
            }
        } catch (IllegalStateException e) {
            resp.sendRedirect(req.getContextPath() + "/user/diagnostic/result?attemptId=" + attemptId);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void showDiagnosticPage(Long attemptId, User user, HttpServletRequest req, HttpServletResponse resp)
            throws Exception {
        ExamAttempt attempt = diagnosticService.getAttempt(attemptId);
        if (!attempt.getUserId().equals(user.getId())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            resp.sendRedirect(req.getContextPath() + "/user/diagnostic/result?attemptId=" + attemptId);
            return;
        }

        if (diagnosticService.isExpired(attempt)) {
            diagnosticService.submitDiagnostic(attemptId, diagnosticService.getAnswerMap(attemptId));
            resp.sendRedirect(req.getContextPath() + "/user/diagnostic/result?attemptId=" + attemptId);
            return;
        }

        List<Question> questions = diagnosticService.getAttemptQuestions(attemptId);
        Map<Long, String> answers = diagnosticService.getAnswerMap(attemptId);
        int secondsPerQuestion = questions.isEmpty()
                ? 1
                : Math.max(1, (attempt.getDurationMinutes() * 60) / questions.size());

        req.setAttribute("attempt", attempt);
        req.setAttribute("questions", questions);
        req.setAttribute("answers", answers);
        req.setAttribute("secondsPerQuestion", secondsPerQuestion);
        req.setAttribute("deadline", diagnosticService.getDeadline(attempt)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        req.getRequestDispatcher("/WEB-INF/jsp/user/diagnostic.jsp").forward(req, resp);
    }

    private Map<Long, String> collectAnswers(HttpServletRequest req, Long attemptId) throws Exception {
        Map<Long, String> answers = new HashMap<>(diagnosticService.getAnswerMap(attemptId));
        List<Question> questions = diagnosticService.getAttemptQuestions(attemptId);
        for (Question question : questions) {
            String param = req.getParameter("answer_" + question.getId());
            if (param != null && !param.isBlank()) {
                answers.put(question.getId(), param);
            }
        }
        return answers;
    }
}
