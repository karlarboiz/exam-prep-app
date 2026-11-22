package com.examprep.servlet.user;

import com.examprep.model.AttemptKind;
import com.examprep.model.AttemptStatus;
import com.examprep.model.ExamAttempt;
import com.examprep.model.Question;
import com.examprep.model.User;
import com.examprep.service.BehaviorTrackingService;
import com.examprep.service.WeeklyRegimenService;
import com.examprep.util.IdCipher;
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

@WebServlet("/user/checkpoint")
public class CheckpointServlet extends HttpServlet {

    private final WeeklyRegimenService weeklyRegimenService = new WeeklyRegimenService();
    private final BehaviorTrackingService behaviorTrackingService = new BehaviorTrackingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.getCurrentUser(req);
        String attemptIdParam = req.getParameter("attemptId");
        try {
            if (attemptIdParam != null) {
                showExamPage(IdCipher.dec(attemptIdParam), user, req, resp);
                return;
            }
            ExamAttempt attempt = weeklyRegimenService.startCheckpoint(user.getId());
            if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
                resp.sendRedirect(req.getContextPath() + "/user/result?attemptId=" + IdCipher.enc(attempt.getId()));
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/user/checkpoint?attemptId=" + IdCipher.enc(attempt.getId()));
        } catch (IllegalStateException e) {
            resp.sendRedirect(req.getContextPath() + "/user/dashboard");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.getCurrentUser(req);
        String action = req.getParameter("action");
        Long attemptId = IdCipher.dec(req.getParameter("attemptId"));
        try {
            ExamAttempt attempt = weeklyRegimenService.getAttempt(attemptId);
            if (!attempt.getUserId().equals(user.getId()) || attempt.getAttemptKind() != AttemptKind.CHECKPOINT) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if ("behavior".equals(action)) {
                BehaviorIngest.handle(req, resp, user.getId(), attemptId);
                return;
            }
            if ("submit".equals(action)) {
                ExamAttempt completed = weeklyRegimenService.submitCheckpoint(attemptId, collectAnswers(req, attemptId));
                resp.sendRedirect(req.getContextPath() + "/user/result?attemptId=" + IdCipher.enc(completed.getId()));
                return;
            }
            if ("answer".equals(action)) {
                Long questionId = Long.parseLong(req.getParameter("questionId"));
                String selected = req.getParameter("selectedOption");
                if (selected != null && !selected.isBlank()) {
                    weeklyRegimenService.saveAnswer(attemptId, questionId, selected);
                }
                if ("1".equals(req.getParameter("ajax"))) {
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    return;
                }
                showExamPage(attemptId, user, req, resp);
            }
        } catch (IllegalStateException e) {
            resp.sendRedirect(req.getContextPath() + "/user/dashboard");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void showExamPage(Long attemptId, User user, HttpServletRequest req, HttpServletResponse resp)
            throws Exception {
        ExamAttempt attempt = weeklyRegimenService.getAttempt(attemptId);
        if (!attempt.getUserId().equals(user.getId()) || attempt.getAttemptKind() != AttemptKind.CHECKPOINT) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            resp.sendRedirect(req.getContextPath() + "/user/result?attemptId=" + IdCipher.enc(attemptId));
            return;
        }
        if (weeklyRegimenService.isExpired(attempt)) {
            ExamAttempt completed = weeklyRegimenService.submitCheckpoint(
                    attemptId, weeklyRegimenService.getAnswerMap(attemptId));
            resp.sendRedirect(req.getContextPath() + "/user/result?attemptId=" + IdCipher.enc(completed.getId()));
            return;
        }
        List<Question> questions = weeklyRegimenService.getAttemptQuestions(attemptId);
        boolean returnedFromLeave = behaviorTrackingService.acknowledgeReturnIfAway(user.getId(), attemptId);
        attempt = weeklyRegimenService.getAttempt(attemptId);
        int secondsPerQuestion = questions.isEmpty()
                ? 1
                : Math.max(1, (attempt.getDurationMinutes() * 60) / questions.size());
        req.setAttribute("attempt", attempt);
        req.setAttribute("questions", questions);
        req.setAttribute("answers", weeklyRegimenService.getAnswerMap(attemptId));
        req.setAttribute("secondsPerQuestion", secondsPerQuestion);
        req.setAttribute("showReturnWarning", returnedFromLeave);
        req.setAttribute("deadline", weeklyRegimenService.getDeadline(attempt).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        req.setAttribute("examPostPath", "/user/checkpoint");
        req.getRequestDispatcher("/WEB-INF/jsp/user/take-exam.jsp").forward(req, resp);
    }

    private Map<Long, String> collectAnswers(HttpServletRequest req, Long attemptId) throws Exception {
        Map<Long, String> answers = new HashMap<>(weeklyRegimenService.getAnswerMap(attemptId));
        for (Question question : weeklyRegimenService.getAttemptQuestions(attemptId)) {
            String param = req.getParameter("answer_" + question.getId());
            if (param != null && !param.isBlank()) {
                answers.put(question.getId(), param);
            }
        }
        return answers;
    }
}
