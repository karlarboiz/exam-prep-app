package com.examprep.servlet.user;

import com.examprep.model.BehaviorEventType;
import com.examprep.model.ExamAttempt;
import com.examprep.service.BehaviorTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

final class BehaviorIngest {

    private BehaviorIngest() {
    }

    static void handle(HttpServletRequest req, HttpServletResponse resp, Long userId, Long attemptId)
            throws IOException {
        BehaviorTrackingService tracking = new BehaviorTrackingService();
        try {
            Long questionId = Long.parseLong(req.getParameter("questionId"));
            BehaviorEventType type = BehaviorEventType.fromString(req.getParameter("eventType"));
            Integer remainingMs = parseRemainingMs(req.getParameter("remainingQuestionMs"));
            tracking.record(userId, attemptId, questionId, type, remainingMs);
            ExamAttempt attempt = tracking.getAttempt(attemptId);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"recorded\":true,\"leaveCount\":"
                    + attempt.getLeaveCount() + ",\"suspectLeaveCount\":"
                    + attempt.getSuspectLeaveCount() + "}");
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static Integer parseRemainingMs(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
