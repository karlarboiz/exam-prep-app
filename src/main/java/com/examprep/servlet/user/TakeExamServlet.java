package com.examprep.servlet.user;

import com.examprep.model.AttemptStatus;
import com.examprep.model.Exam;
import com.examprep.model.ExamAttempt;
import com.examprep.model.Question;
import com.examprep.model.User;
import com.examprep.service.ExamService;
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

@WebServlet("/user/exam")
public class TakeExamServlet extends HttpServlet {

    private final ExamService examService = new ExamService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.getCurrentUser(req);
        String examIdParam = req.getParameter("examId");
        String attemptIdParam = req.getParameter("attemptId");

        try {
            if (attemptIdParam != null) {
                showExamPage(Long.parseLong(attemptIdParam), user, req, resp);
                return;
            }

            if (examIdParam == null) {
                resp.sendRedirect(req.getContextPath() + "/user/dashboard");
                return;
            }

            Long examId = Long.parseLong(examIdParam);
            Exam exam = examService.getExam(examId).orElse(null);
            if (exam == null || !exam.isActive()) {
                resp.sendRedirect(req.getContextPath() + "/user/dashboard");
                return;
            }

            ExamAttempt attempt = examService.startExam(user.getId(), examId);
            resp.sendRedirect(req.getContextPath() + "/user/exam?attemptId=" + attempt.getId());
        } catch (IllegalStateException e) {
            req.setAttribute("error", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/user/dashboard");
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
            ExamAttempt attempt = examService.getAttempt(attemptId);
            if (!attempt.getUserId().equals(user.getId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if ("submit".equals(action)) {
                Map<Long, String> answers = collectAnswers(req, attemptId);
                ExamAttempt completed = examService.submitExam(attemptId, answers);
                resp.sendRedirect(req.getContextPath() + "/user/result?attemptId=" + completed.getId());
                return;
            }

            if ("answer".equals(action)) {
                Long questionId = Long.parseLong(req.getParameter("questionId"));
                String selected = req.getParameter("selectedOption");
                if (selected != null && !selected.isBlank()) {
                    examService.saveAnswer(attemptId, questionId, selected);
                }
                showExamPage(attemptId, user, req, resp);
            }
        } catch (IllegalStateException e) {
            resp.sendRedirect(req.getContextPath() + "/user/result?attemptId=" + attemptId);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void showExamPage(Long attemptId, User user, HttpServletRequest req, HttpServletResponse resp)
            throws Exception {
        ExamAttempt attempt = examService.getAttempt(attemptId);
        if (!attempt.getUserId().equals(user.getId())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            resp.sendRedirect(req.getContextPath() + "/user/result?attemptId=" + attemptId);
            return;
        }

        if (examService.isExpired(attempt)) {
            examService.submitExam(attemptId, examService.getAnswerMap(attemptId));
            resp.sendRedirect(req.getContextPath() + "/user/result?attemptId=" + attemptId);
            return;
        }

        List<Question> questions = examService.getExamQuestions(attempt.getExamId());
        Map<Long, String> answers = examService.getAnswerMap(attemptId);

        req.setAttribute("attempt", attempt);
        req.setAttribute("questions", questions);
        req.setAttribute("answers", answers);
        req.setAttribute("deadline", examService.getDeadline(attempt).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        req.getRequestDispatcher("/WEB-INF/jsp/user/take-exam.jsp").forward(req, resp);
    }

    private Map<Long, String> collectAnswers(HttpServletRequest req, Long attemptId) throws Exception {
        Map<Long, String> answers = new HashMap<>(examService.getAnswerMap(attemptId));
        ExamAttempt attempt = examService.getAttempt(attemptId);
        List<Question> questions = examService.getExamQuestions(attempt.getExamId());
        for (Question question : questions) {
            String param = req.getParameter("answer_" + question.getId());
            if (param != null && !param.isBlank()) {
                answers.put(question.getId(), param);
            }
        }
        return answers;
    }
}
