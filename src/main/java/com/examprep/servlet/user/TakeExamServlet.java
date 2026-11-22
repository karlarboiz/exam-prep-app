package com.examprep.servlet.user;

import com.examprep.model.AnswerRevealMode;
import com.examprep.model.AttemptAnswer;
import com.examprep.model.AttemptStatus;
import com.examprep.model.Exam;
import com.examprep.model.ExamAttempt;
import com.examprep.model.Question;
import com.examprep.model.User;
import com.examprep.service.BehaviorTrackingService;
import com.examprep.service.ExamService;
import com.examprep.util.IdCipher;
import com.examprep.util.OptionShuffle;
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
import java.util.Optional;

@WebServlet("/user/exam")
public class TakeExamServlet extends HttpServlet {

    private final ExamService examService = new ExamService();
    private final BehaviorTrackingService behaviorTrackingService = new BehaviorTrackingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.getCurrentUser(req);
        String examIdParam = req.getParameter("examId");
        String attemptIdParam = req.getParameter("attemptId");

        try {
            if (attemptIdParam != null) {
                showExamPage(IdCipher.dec(attemptIdParam), user, req, resp);
                return;
            }

            if (examIdParam == null) {
                resp.sendRedirect(req.getContextPath() + "/user/dashboard");
                return;
            }

            Long examId = IdCipher.dec(examIdParam);
            Exam exam = examService.getExam(examId).orElse(null);
            if (exam == null || !examService.isExamAvailableForLevel(exam, user.getExamLevel())) {
                resp.sendRedirect(req.getContextPath() + "/user/dashboard");
                return;
            }

            Optional<ExamAttempt> inProgress = examService.findInProgressAttempt(user.getId(), examId);
            if (inProgress.isPresent()) {
                resp.sendRedirect(req.getContextPath() + "/user/exam?attemptId="
                        + IdCipher.enc(inProgress.get().getId()));
                return;
            }

            showStartOptions(exam, req, resp);
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

        try {
            if ("start".equals(action)) {
                handleStart(req, resp, user);
                return;
            }

            Long attemptId = IdCipher.dec(req.getParameter("attemptId"));
            ExamAttempt attempt = examService.getAttempt(attemptId);
            if (!attempt.getUserId().equals(user.getId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if ("behavior".equals(action)) {
                BehaviorIngest.handle(req, resp, user.getId(), attemptId);
                return;
            }

            if ("submit".equals(action)) {
                Map<Long, String> answers = collectAnswers(req, attemptId);
                ExamAttempt completed = examService.submitExam(attemptId, answers);
                resp.sendRedirect(req.getContextPath() + "/user/result?attemptId=" + IdCipher.enc(completed.getId()));
                return;
            }

            if ("answer".equals(action)) {
                Long questionId = Long.parseLong(req.getParameter("questionId"));
                String selected = req.getParameter("selectedOption");
                AttemptAnswer saved = null;
                if (selected != null && !selected.isBlank()) {
                    saved = examService.saveAnswer(attemptId, questionId, selected);
                }
                if ("1".equals(req.getParameter("ajax"))) {
                    if (saved != null && attempt.isRevealAfterEach()) {
                        writeAnswerFeedbackJson(resp, saved);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    }
                    return;
                }
                showExamPage(attemptId, user, req, resp);
            }
        } catch (IllegalStateException e) {
            String attemptIdParam = req.getParameter("attemptId");
            if (attemptIdParam != null && !attemptIdParam.isBlank()) {
                resp.sendRedirect(req.getContextPath() + "/user/result?attemptId="
                        + IdCipher.enc(IdCipher.dec(attemptIdParam)));
            } else {
                resp.sendRedirect(req.getContextPath() + "/user/dashboard");
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void handleStart(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        Long examId = IdCipher.dec(req.getParameter("examId"));
        Exam exam = examService.getExam(examId).orElse(null);
        if (exam == null || !examService.isExamAvailableForLevel(exam, user.getExamLevel())) {
            resp.sendRedirect(req.getContextPath() + "/user/dashboard");
            return;
        }

        AnswerRevealMode mode;
        try {
            mode = AnswerRevealMode.fromString(req.getParameter("answerRevealMode"));
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", "Choose when to show correct answers.");
            showStartOptions(exam, req, resp);
            return;
        }

        ExamAttempt attempt = examService.startExam(user.getId(), examId, mode);
        resp.sendRedirect(req.getContextPath() + "/user/exam?attemptId=" + IdCipher.enc(attempt.getId()));
    }

    private void showStartOptions(Exam exam, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        List<Question> questions = examService.getExamQuestions(exam.getId());
        req.setAttribute("exam", exam);
        req.setAttribute("questionCount", questions.size());
        req.setAttribute("revealModes", AnswerRevealMode.values());
        req.getRequestDispatcher("/WEB-INF/jsp/user/exam-start.jsp").forward(req, resp);
    }

    private void showExamPage(Long attemptId, User user, HttpServletRequest req, HttpServletResponse resp)
            throws Exception {
        ExamAttempt attempt = examService.getAttempt(attemptId);
        if (!attempt.getUserId().equals(user.getId())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            resp.sendRedirect(req.getContextPath() + "/user/result?attemptId=" + IdCipher.enc(attemptId));
            return;
        }

        if (examService.isExpired(attempt)) {
            examService.submitExam(attemptId, examService.getAnswerMap(attemptId));
            resp.sendRedirect(req.getContextPath() + "/user/result?attemptId=" + IdCipher.enc(attemptId));
            return;
        }

        List<Question> questions = examService.getExamQuestions(attempt.getExamId());
        OptionShuffle.applyForAttempt(questions, attemptId);
        Map<Long, String> answers = examService.getAnswerMap(attemptId);
        int secondsPerQuestion = questions.isEmpty()
                ? 1
                : Math.max(1, (attempt.getDurationMinutes() * 60) / questions.size());

        boolean returnedFromLeave = behaviorTrackingService.acknowledgeReturnIfAway(user.getId(), attemptId);
        attempt = examService.getAttempt(attemptId);

        Map<Long, AttemptAnswer> feedbackByQuestion = Map.of();
        if (attempt.isRevealAfterEach()) {
            feedbackByQuestion = new HashMap<>();
            for (AttemptAnswer answer : examService.getAttemptAnswers(attemptId)) {
                if (answer.getSelectedOption() != null && !answer.getSelectedOption().isBlank()) {
                    feedbackByQuestion.put(answer.getQuestionId(), answer);
                }
            }
        }

        req.setAttribute("attempt", attempt);
        req.setAttribute("questions", questions);
        req.setAttribute("answers", answers);
        req.setAttribute("feedbackByQuestion", feedbackByQuestion);
        req.setAttribute("secondsPerQuestion", secondsPerQuestion);
        req.setAttribute("showReturnWarning", returnedFromLeave);
        req.setAttribute("deadline", examService.getDeadline(attempt).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        req.getRequestDispatcher("/WEB-INF/jsp/user/take-exam.jsp").forward(req, resp);
    }

    private void writeAnswerFeedbackJson(HttpServletResponse resp, AttemptAnswer answer) throws IOException {
        Question q = answer.getQuestion();
        String correctOption = q.getCorrectOption();
        String correctText = q.getOptionText(correctOption);
        String explanation = q.getExplanation() != null ? q.getExplanation() : "";

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("""
                {"correct":%s,"selectedOption":"%s","correctOption":"%s","correctText":%s,"explanation":%s}
                """.formatted(
                Boolean.TRUE.equals(answer.getCorrect()),
                escapeJson(answer.getSelectedOption()),
                escapeJson(correctOption),
                jsonString(correctText),
                jsonString(explanation)
        ).trim());
    }

    private static String jsonString(String value) {
        return "\"" + escapeJson(value) + "\"";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
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
