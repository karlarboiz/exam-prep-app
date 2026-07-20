package com.examprep.service;

import com.examprep.dao.AttemptDao;
import com.examprep.dao.DiagnosticSubjectScoreDao;
import com.examprep.dao.ExamDao;
import com.examprep.dao.QuestionDao;
import com.examprep.dao.SubjectDao;
import com.examprep.dao.UserDao;
import com.examprep.model.AttemptAnswer;
import com.examprep.model.AttemptStatus;
import com.examprep.model.DiagnosticResult;
import com.examprep.model.DiagnosticSubjectScore;
import com.examprep.model.Exam;
import com.examprep.model.ExamAttempt;
import com.examprep.model.Question;
import com.examprep.model.ReadinessLabel;
import com.examprep.model.Subject;
import com.examprep.model.SubjectBand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class DiagnosticService {

    public static final int DEFAULT_QUESTIONS_PER_SUBJECT = 5;

    private final ExamDao examDao = new ExamDao();
    private final QuestionDao questionDao = new QuestionDao();
    private final AttemptDao attemptDao = new AttemptDao();
    private final SubjectDao subjectDao = new SubjectDao();
    private final UserDao userDao = new UserDao();
    private final DiagnosticSubjectScoreDao subjectScoreDao = new DiagnosticSubjectScoreDao();

    public boolean isDiagnosticCompleted(Long userId) throws SQLException {
        return userDao.isDiagnosticCompleted(userId);
    }

    public ExamAttempt startDiagnostic(Long userId) throws SQLException {
        Exam exam = examDao.findActiveDiagnostic()
                .orElseThrow(() -> new IllegalStateException("No active diagnostic exam is configured"));

        Optional<ExamAttempt> existing = attemptDao.findInProgress(userId, exam.getId());
        if (existing.isPresent()) {
            ExamAttempt attempt = existing.get();
            if (isExpired(attempt)) {
                // Abandoned / AFK past deadline — score as EXPIRED but do not complete the gate
                submitDiagnostic(attempt.getId(), getAnswerMap(attempt.getId()));
            } else {
                return attempt;
            }
        }

        ExamAttempt attempt = attemptDao.create(userId, exam.getId());
        List<Long> sampledIds = sampleQuestionIds(exam);
        if (sampledIds.isEmpty()) {
            throw new IllegalStateException("No questions available to build a diagnostic");
        }
        questionDao.setAttemptQuestions(attempt.getId(), sampledIds);
        return attemptDao.findById(attempt.getId()).orElseThrow();
    }

    /**
     * Starts the exam clock after the intro modal. Resets {@code started_at} only when
     * the attempt still has no saved answers (fresh start).
     */
    public LocalDateTime beginDiagnostic(Long attemptId) throws SQLException {
        ExamAttempt attempt = getAttempt(attemptId);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("Attempt is not in progress");
        }
        if (isExpired(attempt)) {
            submitDiagnostic(attemptId, getAnswerMap(attemptId));
            throw new IllegalStateException("Exam time has expired");
        }
        if (attemptDao.findAnswersByAttemptId(attemptId).isEmpty()) {
            attemptDao.updateStartedAt(attemptId, LocalDateTime.now());
        }
        return getDeadline(getAttempt(attemptId));
    }

    public ExamAttempt getAttempt(Long attemptId) throws SQLException {
        return attemptDao.findById(attemptId).orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
    }

    public List<Question> getAttemptQuestions(Long attemptId) throws SQLException {
        return questionDao.findByAttemptId(attemptId);
    }

    public boolean isExpired(ExamAttempt attempt) {
        LocalDateTime deadline = attempt.getStartedAt().plusMinutes(attempt.getDurationMinutes());
        return LocalDateTime.now().isAfter(deadline);
    }

    public LocalDateTime getDeadline(ExamAttempt attempt) {
        return attempt.getStartedAt().plusMinutes(attempt.getDurationMinutes());
    }

    public void saveAnswer(Long attemptId, Long questionId, String selectedOption) throws SQLException {
        ExamAttempt attempt = getAttempt(attemptId);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("Attempt is not in progress");
        }
        if (isExpired(attempt)) {
            attemptDao.updateStatus(attemptId, AttemptStatus.EXPIRED);
            throw new IllegalStateException("Exam time has expired");
        }
        Question question = questionDao.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));
        boolean correct = question.getCorrectOption().equalsIgnoreCase(selectedOption);
        attemptDao.saveAnswer(attemptId, questionId, selectedOption.toUpperCase(), correct);
    }

    public ExamAttempt submitDiagnostic(Long attemptId, Map<Long, String> answers) throws SQLException {
        ExamAttempt attempt = getAttempt(attemptId);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return attempt;
        }

        AttemptStatus finalStatus = isExpired(attempt) ? AttemptStatus.EXPIRED : AttemptStatus.COMPLETED;
        List<Question> questions = questionDao.findByAttemptId(attemptId);
        for (Question question : questions) {
            String selected = answers.get(question.getId());
            if (selected != null && !selected.isBlank()) {
                boolean correct = question.getCorrectOption().equalsIgnoreCase(selected);
                attemptDao.saveAnswer(attemptId, question.getId(), selected.toUpperCase(), correct);
            }
        }

        BigDecimal overallScore = calculateOverallScore(attemptId, questions.size());
        attemptDao.completeAttempt(attemptId, overallScore, finalStatus);

        List<DiagnosticSubjectScore> subjectScores = buildSubjectScores(attemptId, questions);
        subjectScoreDao.replaceForAttempt(attemptId, subjectScores);

        // Only a finished (non-expired) attempt clears the hard gate; AFK/timeout requires retake
        if (finalStatus == AttemptStatus.COMPLETED) {
            userDao.markDiagnosticCompleted(attempt.getUserId());
        }

        return attemptDao.findById(attemptId).orElseThrow();
    }

    public DiagnosticResult getResult(Long attemptId) throws SQLException {
        ExamAttempt attempt = getAttempt(attemptId);
        List<DiagnosticSubjectScore> subjectScores = subjectScoreDao.findByAttemptId(attemptId);
        List<AttemptAnswer> answers = attemptDao.findAnswersByAttemptId(attemptId);

        DiagnosticResult result = new DiagnosticResult();
        result.setAttempt(attempt);
        result.setSubjectScores(subjectScores);
        result.setAnswers(answers);
        result.setMeanSubjectPercent(meanSubjectPercent(subjectScores));
        result.setReadiness(computeReadiness(subjectScores));
        return result;
    }

    public Map<Long, String> getAnswerMap(Long attemptId) throws SQLException {
        Map<Long, String> map = new HashMap<>();
        for (AttemptAnswer answer : attemptDao.findAnswersByAttemptId(attemptId)) {
            map.put(answer.getQuestionId(), answer.getSelectedOption());
        }
        return map;
    }

    static SubjectBand bandForPercent(BigDecimal percent) {
        if (percent.compareTo(new BigDecimal("75")) >= 0) {
            return SubjectBand.STRONG;
        }
        if (percent.compareTo(new BigDecimal("50")) >= 0) {
            return SubjectBand.DEVELOPING;
        }
        return SubjectBand.WEAK;
    }

    static ReadinessLabel computeReadiness(List<DiagnosticSubjectScore> subjectScores) {
        if (subjectScores == null || subjectScores.isEmpty()) {
            return ReadinessLabel.NEEDS_FOUNDATION;
        }
        BigDecimal mean = meanSubjectPercent(subjectScores);
        long weakCount = subjectScores.stream().filter(s -> s.getBand() == SubjectBand.WEAK).count();

        if (mean.compareTo(new BigDecimal("50")) < 0 || weakCount >= 2) {
            return ReadinessLabel.NEEDS_FOUNDATION;
        }
        if (mean.compareTo(new BigDecimal("75")) >= 0 && weakCount == 0) {
            return ReadinessLabel.EXAM_READY;
        }
        if (mean.compareTo(new BigDecimal("75")) >= 0) {
            return ReadinessLabel.NEAR_READY;
        }
        return ReadinessLabel.BUILDING;
    }

    static BigDecimal meanSubjectPercent(List<DiagnosticSubjectScore> subjectScores) {
        if (subjectScores == null || subjectScores.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (DiagnosticSubjectScore score : subjectScores) {
            sum = sum.add(score.getScorePercent());
        }
        return sum.divide(BigDecimal.valueOf(subjectScores.size()), 2, RoundingMode.HALF_UP);
    }

    private List<Long> sampleQuestionIds(Exam exam) throws SQLException {
        int perSubject = exam.getQuestionsPerSubject() != null
                ? exam.getQuestionsPerSubject()
                : DEFAULT_QUESTIONS_PER_SUBJECT;
        if (perSubject < 1) {
            perSubject = DEFAULT_QUESTIONS_PER_SUBJECT;
        }

        List<Subject> subjects = subjectDao.findAll();
        List<Long> sampled = new ArrayList<>();
        for (Subject subject : subjects) {
            List<Question> pool = questionDao.findBySubjectId(subject.getId());
            if (pool.isEmpty()) {
                continue;
            }
            sampled.addAll(sampleWithDifficultyMix(pool, perSubject));
        }
        return sampled;
    }

    private List<Long> sampleWithDifficultyMix(List<Question> pool, int count) {
        int take = Math.min(count, pool.size());
        Map<String, List<Question>> byDifficulty = new LinkedHashMap<>();
        byDifficulty.put("EASY", new ArrayList<>());
        byDifficulty.put("MEDIUM", new ArrayList<>());
        byDifficulty.put("HARD", new ArrayList<>());
        List<Question> other = new ArrayList<>();
        for (Question q : pool) {
            String d = q.getDifficulty() == null ? "MEDIUM" : q.getDifficulty().toUpperCase();
            if (byDifficulty.containsKey(d)) {
                byDifficulty.get(d).add(q);
            } else {
                other.add(q);
            }
        }
        for (List<Question> list : byDifficulty.values()) {
            Collections.shuffle(list, ThreadLocalRandom.current());
        }
        Collections.shuffle(other, ThreadLocalRandom.current());

        List<Long> picked = new ArrayList<>();
        String[] order = {"EASY", "MEDIUM", "HARD"};
        int difficultyIndex = 0;
        while (picked.size() < take) {
            boolean added = false;
            for (int i = 0; i < order.length && picked.size() < take; i++) {
                String key = order[(difficultyIndex + i) % order.length];
                List<Question> bucket = byDifficulty.get(key);
                if (!bucket.isEmpty()) {
                    picked.add(bucket.remove(0).getId());
                    added = true;
                    difficultyIndex = (difficultyIndex + i + 1) % order.length;
                    break;
                }
            }
            if (!added) {
                break;
            }
        }
        for (Question q : other) {
            if (picked.size() >= take) {
                break;
            }
            picked.add(q.getId());
        }
        if (picked.size() < take) {
            List<Question> remaining = new ArrayList<>(pool);
            remaining.removeIf(q -> picked.contains(q.getId()));
            Collections.shuffle(remaining, ThreadLocalRandom.current());
            for (Question q : remaining) {
                if (picked.size() >= take) {
                    break;
                }
                picked.add(q.getId());
            }
        }
        return picked;
    }

    private List<DiagnosticSubjectScore> buildSubjectScores(Long attemptId, List<Question> questions)
            throws SQLException {
        Map<Long, List<Question>> bySubject = new LinkedHashMap<>();
        for (Question q : questions) {
            bySubject.computeIfAbsent(q.getSubjectId(), k -> new ArrayList<>()).add(q);
        }
        Map<Long, AttemptAnswer> answerByQuestion = new HashMap<>();
        for (AttemptAnswer answer : attemptDao.findAnswersByAttemptId(attemptId)) {
            answerByQuestion.put(answer.getQuestionId(), answer);
        }

        Map<Long, String> subjectNames = new HashMap<>();
        for (Subject subject : subjectDao.findAll()) {
            subjectNames.put(subject.getId(), subject.getName());
        }

        List<DiagnosticSubjectScore> scores = new ArrayList<>();
        for (Map.Entry<Long, List<Question>> entry : bySubject.entrySet()) {
            List<Question> subjectQuestions = entry.getValue();
            long correct = 0;
            for (Question q : subjectQuestions) {
                AttemptAnswer answer = answerByQuestion.get(q.getId());
                if (answer != null && Boolean.TRUE.equals(answer.getCorrect())) {
                    correct++;
                }
            }
            BigDecimal percent = BigDecimal.valueOf(correct * 100.0 / subjectQuestions.size())
                    .setScale(2, RoundingMode.HALF_UP);
            DiagnosticSubjectScore score = new DiagnosticSubjectScore();
            score.setAttemptId(attemptId);
            score.setSubjectId(entry.getKey());
            score.setSubjectName(subjectNames.get(entry.getKey()));
            score.setScorePercent(percent);
            score.setBand(bandForPercent(percent));
            scores.add(score);
        }
        return scores;
    }

    private BigDecimal calculateOverallScore(Long attemptId, int totalQuestions) throws SQLException {
        if (totalQuestions == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        List<AttemptAnswer> answers = attemptDao.findAnswersByAttemptId(attemptId);
        long correct = answers.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
        return BigDecimal.valueOf(correct * 100.0 / totalQuestions).setScale(2, RoundingMode.HALF_UP);
    }
}
