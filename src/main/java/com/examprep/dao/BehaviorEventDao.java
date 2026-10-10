package com.examprep.dao;

import com.examprep.config.DatabaseManager;
import com.examprep.model.AttemptBehaviorEvent;
import com.examprep.model.BehaviorEventType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BehaviorEventDao {

    public AttemptBehaviorEvent insert(AttemptBehaviorEvent event) throws SQLException {
        String sql = """
                INSERT INTO attempt_behavior_events
                    (attempt_id, question_id, event_type, occurred_at, question_answered,
                     remaining_question_ms, away_duration_ms, suspect, question_difficulty)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, event.getAttemptId());
            ps.setLong(2, event.getQuestionId());
            ps.setString(3, event.getEventType().name());
            ps.setTimestamp(4, Timestamp.valueOf(event.getOccurredAt()));
            ps.setBoolean(5, event.isQuestionAnswered());
            setNullableInt(ps, 6, event.getRemainingQuestionMs());
            setNullableInt(ps, 7, event.getAwayDurationMs());
            ps.setBoolean(8, event.isSuspect());
            ps.setString(9, event.getQuestionDifficulty());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    event.setId(keys.getLong(1));
                    return event;
                }
            }
        }
        throw new SQLException("Failed to insert behavior event");
    }

    public Optional<AttemptBehaviorEvent> findLastByAttemptId(Long attemptId) throws SQLException {
        String sql = """
                SELECT id, attempt_id, question_id, event_type, occurred_at, question_answered,
                       remaining_question_ms, away_duration_ms, suspect, question_difficulty
                FROM attempt_behavior_events
                WHERE attempt_id = ?
                ORDER BY occurred_at DESC, id DESC
                LIMIT 1
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<AttemptBehaviorEvent> findByAttemptId(Long attemptId) throws SQLException {
        String sql = """
                SELECT e.id, e.attempt_id, e.question_id, e.event_type, e.occurred_at, e.question_answered,
                       e.remaining_question_ms, e.away_duration_ms, e.suspect, e.question_difficulty,
                       q.prompt AS question_prompt,
                       COALESCE(aq.sort_order, eq.sort_order) AS question_number
                FROM attempt_behavior_events e
                JOIN questions q ON q.id = e.question_id
                JOIN exam_attempts a ON a.id = e.attempt_id
                LEFT JOIN attempt_questions aq
                    ON aq.attempt_id = e.attempt_id AND aq.question_id = e.question_id
                LEFT JOIN exam_questions eq
                    ON eq.exam_id = a.exam_id AND eq.question_id = e.question_id
                WHERE e.attempt_id = ?
                ORDER BY e.occurred_at ASC, e.id ASC
                """;
        List<AttemptBehaviorEvent> events = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttemptBehaviorEvent event = mapRow(rs);
                    event.setQuestionPrompt(rs.getString("question_prompt"));
                    int number = rs.getInt("question_number");
                    if (!rs.wasNull()) {
                        event.setQuestionNumber(number);
                    }
                    events.add(event);
                }
            }
        }
        return events;
    }

    public int[] countLeaves(Long attemptId) throws SQLException {
        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN event_type = 'LEAVE' THEN 1 ELSE 0 END), 0) AS leave_count,
                    COALESCE(SUM(CASE WHEN event_type = 'LEAVE' AND suspect THEN 1 ELSE 0 END), 0) AS suspect_leave_count
                FROM attempt_behavior_events
                WHERE attempt_id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt("leave_count"), rs.getInt("suspect_leave_count")};
                }
            }
        }
        return new int[]{0, 0};
    }

    private AttemptBehaviorEvent mapRow(ResultSet rs) throws SQLException {
        AttemptBehaviorEvent event = new AttemptBehaviorEvent();
        event.setId(rs.getLong("id"));
        event.setAttemptId(rs.getLong("attempt_id"));
        event.setQuestionId(rs.getLong("question_id"));
        event.setEventType(BehaviorEventType.fromString(rs.getString("event_type")));
        Timestamp occurredAt = rs.getTimestamp("occurred_at");
        if (occurredAt != null) {
            event.setOccurredAt(occurredAt.toLocalDateTime());
        }
        event.setQuestionAnswered(rs.getBoolean("question_answered"));
        int remaining = rs.getInt("remaining_question_ms");
        if (!rs.wasNull()) {
            event.setRemainingQuestionMs(remaining);
        }
        int away = rs.getInt("away_duration_ms");
        if (!rs.wasNull()) {
            event.setAwayDurationMs(away);
        }
        event.setSuspect(rs.getBoolean("suspect"));
        event.setQuestionDifficulty(rs.getString("question_difficulty"));
        return event;
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
