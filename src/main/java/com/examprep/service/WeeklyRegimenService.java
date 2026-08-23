package com.examprep.service;

import com.examprep.i18n.LocaleSupport;
import com.examprep.i18n.Messages;
import com.examprep.config.AppConfig;
import com.examprep.dao.AccessGrantDao;
import com.examprep.dao.AttemptDao;
import com.examprep.dao.DiagnosticSubjectScoreDao;
import com.examprep.dao.ExamDao;
import com.examprep.dao.QuestionDao;
import com.examprep.dao.SubjectDao;
import com.examprep.dao.UserDao;
import com.examprep.dao.WeeklyRegimenDao;
import com.examprep.dao.WeeklySubjectScoreDao;
import com.examprep.model.AccessGrant;
import com.examprep.model.AttemptAnswer;
import com.examprep.model.AttemptKind;
import com.examprep.model.AttemptStatus;
import com.examprep.model.DiagnosticSubjectScore;
import com.examprep.model.Exam;
import com.examprep.model.ExamAttempt;
import com.examprep.model.ExamLevel;
import com.examprep.model.Question;
import com.examprep.model.StudyPlan;
import com.examprep.model.Subject;
import com.examprep.model.SubjectBand;
import com.examprep.model.User;
import com.examprep.model.WeeklyDashboard;
import com.examprep.model.WeeklyRegimen;
import com.examprep.model.WeeklyRegimenStatus;
import com.examprep.model.WeeklySubjectScore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class WeeklyRegimenService {

    public static final int DEFAULT_BASE_PER_SUBJECT = 4;
    public static final int DEFAULT_CHECKPOINT_QUESTIONS = 8;
    public static final int DEFAULT_CHECKPOINT_DURATION = 15;
    public static final int DEFAULT_CHECKPOINT_MIN_FRESH = 5;

    private final UserDao userDao = new UserDao();
    private final AccessGrantDao accessGrantDao = new AccessGrantDao();
    private final ExamDao examDao = new ExamDao();
    private final QuestionDao questionDao = new QuestionDao();
    private final SubjectDao subjectDao = new SubjectDao();
    private final AttemptDao attemptDao = new AttemptDao();
    private final WeeklyRegimenDao regimenDao = new WeeklyRegimenDao();
    private final WeeklySubjectScoreDao weeklyScoreDao = new WeeklySubjectScoreDao();
    private final DiagnosticSubjectScoreDao diagnosticScoreDao = new DiagnosticSubjectScoreDao();
    private final MailService mailService = new MailService();
    private final BehaviorTrackingService behaviorTrackingService = new BehaviorTrackingService();

    private Clock clock = Clock.systemDefaultZone();

    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public WeeklyDashboard resolveDashboard(Long userId) throws SQLException {
        WeeklyRegimen current = ensureCurrentWeek(userId);
        User user = userDao.findById(userId).orElseThrow();
        AccessGrant grant = accessGrantDao.findLatestRedeemedByUserId(userId).orElse(null);
        int totalWeeks = grant == null || user.getDiagnosticCompletedAt() == null
                ? 1
                : WeekClock.totalWeeks(user.getDiagnosticCompletedAt(), grant.getExpiresAt());

        WeeklyDashboard dashboard = new WeeklyDashboard();
        dashboard.setCurrent(current);
        dashboard.setTotalWeeks(totalWeeks);
        dashboard.setMissedWeekNotice(regimenDao.findByUserId(userId).stream()
                .anyMatch(r -> r.getStatus() == WeeklyRegimenStatus.MISSED));

        Optional<ExamAttempt> weeklyInProgress = attemptDao.findInProgressByRegimen(
                userId, current.getId(), AttemptKind.WEEKLY);
        if (weeklyInProgress.isPresent() && isExpired(weeklyInProgress.get())) {
            submitWeeklyExam(weeklyInProgress.get().getId(), getAnswerMap(weeklyInProgress.get().getId()));
            current = regimenDao.findById(current.getId()).orElse(current);
            dashboard.setCurrent(current);
            weeklyInProgress = Optional.empty();
        }

        boolean locked = current.hasOfficialScore();
        dashboard.setCanContinueWeekly(weeklyInProgress.isPresent() && !locked);
        dashboard.setInProgressWeeklyAttemptId(weeklyInProgress.map(ExamAttempt::getId).orElse(null));
        dashboard.setCanStartWeekly(!locked && weeklyInProgress.isEmpty()
                && current.getStatus() != WeeklyRegimenStatus.MISSED);

        StudyPlan plan = buildStudyPlan(userId, current);
        dashboard.setStudyPlan(plan);
        dashboard.setCanReview(plan.getMisses() != null && !plan.getMisses().isEmpty());

        Optional<ExamAttempt> checkpointInProgress = attemptDao.findInProgressByRegimen(
                userId, current.getId(), AttemptKind.CHECKPOINT);
        dashboard.setCanContinueCheckpoint(checkpointInProgress.isPresent());
        dashboard.setInProgressCheckpointAttemptId(checkpointInProgress.map(ExamAttempt::getId).orElse(null));
        boolean checkpointOk = locked
                && !now().isAfter(current.getWeekEnd())
                && checkpointInProgress.isEmpty()
                && countFreshWeakItems(user, current) >= checkpointMinFresh();
        dashboard.setCheckpointAvailable(checkpointOk);
        if (locked && !checkpointOk && checkpointInProgress.isEmpty() && !now().isAfter(current.getWeekEnd())) {
            dashboard.setBankWarning(Messages.get(LocaleSupport.current(), "dashboard.bankWarning"));
        }
        return dashboard;
    }

    public WeeklyRegimen ensureCurrentWeek(Long userId) throws SQLException {
        User user = userDao.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getDiagnosticCompletedAt() == null) {
            throw new IllegalStateException("Diagnostic must be completed before the weekly regimen starts");
        }
        AccessGrant grant = accessGrantDao.findLatestRedeemedByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No access grant for weekly regimen"));

        LocalDateTime diagAt = user.getDiagnosticCompletedAt();
        LocalDateTime expiresAt = grant.getExpiresAt();
        int currentWeek = WeekClock.weekNumber(diagAt, expiresAt, now());

        for (WeeklyRegimen existing : regimenDao.findByUserId(userId)) {
            if (existing.getWeekNumber() < currentWeek
                    && existing.getStatus() == WeeklyRegimenStatus.OPEN
                    && !existing.hasOfficialScore()) {
                regimenDao.markMissed(existing.getId());
            }
        }

        Optional<WeeklyRegimen> row = regimenDao.findByUserAndWeek(userId, currentWeek);
        if (row.isPresent()) {
            return row.get();
        }
        return createWeek(user, grant, currentWeek);
    }

    public ExamAttempt startWeeklyExam(Long userId) throws SQLException {
        WeeklyRegimen regimen = ensureCurrentWeek(userId);
        if (regimen.hasOfficialScore()) {
            throw new IllegalStateException("This week's official exam is already recorded");
        }
        if (regimen.getStatus() == WeeklyRegimenStatus.MISSED) {
            throw new IllegalStateException("This week was missed; wait for the next week's form");
        }

        Optional<ExamAttempt> existing = attemptDao.findInProgressByRegimen(userId, regimen.getId(), AttemptKind.WEEKLY);
        if (existing.isPresent()) {
            ExamAttempt attempt = existing.get();
            if (isExpired(attempt)) {
                return submitWeeklyExam(attempt.getId(), getAnswerMap(attempt.getId()));
            }
            return attempt;
        }

        Exam exam = examDao.findActiveWeekly()
                .orElseThrow(() -> new IllegalStateException("No active weekly exam is configured"));
        List<Long> formIds = regimenDao.findFormQuestionIds(regimen.getId());
        if (formIds.isEmpty()) {
            throw new IllegalStateException("No questions available for this week's exam");
        }
        ExamAttempt attempt = attemptDao.create(userId, exam.getId(), AttemptKind.WEEKLY, regimen.getId(), null);
        questionDao.setAttemptQuestions(attempt.getId(), formIds);
        return attemptDao.findById(attempt.getId()).orElseThrow();
    }

    public ExamAttempt submitWeeklyExam(Long attemptId, Map<Long, String> answers) throws SQLException {
        ExamAttempt attempt = getAttempt(attemptId);
        if (attempt.getAttemptKind() != AttemptKind.WEEKLY) {
            throw new IllegalStateException("Not a weekly exam attempt");
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return attempt;
        }
        WeeklyRegimen regimen = regimenDao.findById(attempt.getRegimenId())
                .orElseThrow(() -> new IllegalStateException("Weekly regimen not found"));
        if (regimen.hasOfficialScore()) {
            attemptDao.updateStatus(attemptId, AttemptStatus.EXPIRED);
            return attemptDao.findById(attemptId).orElseThrow();
        }

        persistAnswers(attemptId, answers, questionDao.findByAttemptId(attemptId));
        AttemptStatus finalStatus = isExpired(attempt) ? AttemptStatus.EXPIRED : AttemptStatus.COMPLETED;
        List<Question> questions = questionDao.findByAttemptId(attemptId);
        BigDecimal score = calculateScore(attemptId, questions.size());
        attemptDao.completeAttempt(attemptId, score, finalStatus);
        behaviorTrackingService.refreshSummary(attemptId);

        try {
            regimenDao.setOfficialAttempt(regimen.getId(), attemptId);
        } catch (SQLException e) {
            return attemptDao.findById(attemptId).orElseThrow();
        }

        List<WeeklySubjectScore> subjectScores = buildWeeklySubjectScores(regimen.getId(), attemptId, questions);
        weeklyScoreDao.replaceForRegimen(regimen.getId(), subjectScores);

        WeeklyRegimen locked = regimenDao.findById(regimen.getId()).orElse(regimen);
        User user = userDao.findById(attempt.getUserId()).orElseThrow();
        boolean grantActive = accessGrantDao.findActiveByUserId(user.getId()).isPresent()
                && !now().isAfter(accessGrantDao.findLatestRedeemedByUserId(user.getId())
                .map(AccessGrant::getExpiresAt).orElse(now().minusSeconds(1)));
        StudyPlan plan = buildStudyPlan(user.getId(), locked);
        mailService.sendStudyPlanDigest(user, locked, plan, grantActive, now());

        return attemptDao.findById(attemptId).orElseThrow();
    }

    public ExamAttempt startCheckpoint(Long userId) throws SQLException {
        WeeklyRegimen regimen = ensureCurrentWeek(userId);
        if (!regimen.hasOfficialScore()) {
            throw new IllegalStateException("Finish this week's exam before a checkpoint");
        }
        if (now().isAfter(regimen.getWeekEnd())) {
            throw new IllegalStateException("Checkpoint is only available during this week");
        }

        Optional<ExamAttempt> existing = attemptDao.findInProgressByRegimen(
                userId, regimen.getId(), AttemptKind.CHECKPOINT);
        if (existing.isPresent()) {
            ExamAttempt attempt = existing.get();
            if (isExpired(attempt)) {
                return submitCheckpoint(attempt.getId(), getAnswerMap(attempt.getId()));
            }
            return attempt;
        }

        User user = userDao.findById(userId).orElseThrow();
        List<Long> sampled = sampleCheckpointQuestions(user, regimen);
        if (sampled.size() < checkpointMinFresh()) {
            throw new IllegalStateException("Not enough unused items for a checkpoint");
        }
        Exam exam = examDao.findActiveWeekly()
                .orElseThrow(() -> new IllegalStateException("No active weekly exam is configured"));
        ExamAttempt attempt = attemptDao.create(
                userId, exam.getId(), AttemptKind.CHECKPOINT, regimen.getId(), checkpointDurationMinutes());
        questionDao.setAttemptQuestions(attempt.getId(), sampled);
        return attemptDao.findById(attempt.getId()).orElseThrow();
    }

    public ExamAttempt submitCheckpoint(Long attemptId, Map<Long, String> answers) throws SQLException {
        ExamAttempt attempt = getAttempt(attemptId);
        if (attempt.getAttemptKind() != AttemptKind.CHECKPOINT) {
            throw new IllegalStateException("Not a checkpoint attempt");
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return attempt;
        }
        persistAnswers(attemptId, answers, questionDao.findByAttemptId(attemptId));
        AttemptStatus finalStatus = isExpired(attempt) ? AttemptStatus.EXPIRED : AttemptStatus.COMPLETED;
        List<Question> questions = questionDao.findByAttemptId(attemptId);
        attemptDao.completeAttempt(attemptId, calculateScore(attemptId, questions.size()), finalStatus);
        behaviorTrackingService.refreshSummary(attemptId);
        return attemptDao.findById(attemptId).orElseThrow();
    }

    public StudyPlan getStudyPlan(Long userId, Long regimenId) throws SQLException {
        WeeklyRegimen regimen;
        if (regimenId != null) {
            regimen = regimenDao.findById(regimenId)
                    .orElseThrow(() -> new IllegalArgumentException("Study plan not found"));
            if (!regimen.getUserId().equals(userId)) {
                throw new IllegalArgumentException("Study plan not found");
            }
        } else {
            regimen = regimenDao.findLatestCompleted(userId).orElseGet(() -> {
                try {
                    return ensureCurrentWeek(userId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return buildStudyPlan(userId, regimen);
    }

    public List<AttemptAnswer> getReviewMisses(Long userId, Long regimenId) throws SQLException {
        return getStudyPlan(userId, regimenId).getMisses();
    }

    public ExamAttempt getAttempt(Long attemptId) throws SQLException {
        return attemptDao.findById(attemptId).orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
    }

    public List<Question> getAttemptQuestions(Long attemptId) throws SQLException {
        return questionDao.findByAttemptId(attemptId);
    }

    public boolean isExpired(ExamAttempt attempt) {
        return now().isAfter(getDeadline(attempt));
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

    public Map<Long, String> getAnswerMap(Long attemptId) throws SQLException {
        Map<Long, String> map = new HashMap<>();
        for (AttemptAnswer answer : attemptDao.findAnswersByAttemptId(attemptId)) {
            map.put(answer.getQuestionId(), answer.getSelectedOption());
        }
        return map;
    }

    public List<AttemptAnswer> getAttemptAnswers(Long attemptId) throws SQLException {
        return attemptDao.findAnswersByAttemptId(attemptId);
    }

    int basePerSubject() {
        return AppConfig.getInt("weekly.questions.per.subject", DEFAULT_BASE_PER_SUBJECT);
    }

    int checkpointQuestionCount() {
        return AppConfig.getInt("weekly.checkpoint.questions", DEFAULT_CHECKPOINT_QUESTIONS);
    }

    int checkpointDurationMinutes() {
        return AppConfig.getInt("weekly.checkpoint.duration.minutes", DEFAULT_CHECKPOINT_DURATION);
    }

    int checkpointMinFresh() {
        return AppConfig.getInt("weekly.checkpoint.min.fresh", DEFAULT_CHECKPOINT_MIN_FRESH);
    }

    private WeeklyRegimen createWeek(User user, AccessGrant grant, int weekNumber) throws SQLException {
        WeeklyRegimen regimen = new WeeklyRegimen();
        regimen.setUserId(user.getId());
        regimen.setWeekNumber(weekNumber);
        regimen.setWeekStart(WeekClock.weekStart(user.getDiagnosticCompletedAt(), weekNumber));
        regimen.setWeekEnd(WeekClock.weekEnd(user.getDiagnosticCompletedAt(), grant.getExpiresAt(), weekNumber));
        regimen.setStatus(WeeklyRegimenStatus.OPEN);
        regimen.setFinalWeek(WeekClock.isFinalWeek(user.getDiagnosticCompletedAt(), grant.getExpiresAt(), weekNumber));
        WeeklyRegimen created = regimenDao.create(regimen);

        Exam exam = examDao.findActiveWeekly()
                .orElseThrow(() -> new IllegalStateException("No active weekly exam is configured"));
        int base = exam.getQuestionsPerSubject() != null ? exam.getQuestionsPerSubject() : basePerSubject();
        List<Long> sampled = sampleWeeklyForm(user, weekNumber, created.isFinalWeek(), base);
        regimenDao.setFormQuestions(created.getId(), sampled);
        return regimenDao.findById(created.getId()).orElse(created);
    }

    List<Long> sampleWeeklyForm(User user, int weekNumber, boolean finalWeek, int basePerSubject)
            throws SQLException {
        List<Subject> subjects = subjectDao.findByExamLevel(user.getExamLevel());
        Map<Long, SubjectBand> bands = loadBandsForSampling(user.getId(), weekNumber);
        List<Long> subjectIds = subjects.stream()
                .filter(s -> {
                    try {
                        return !questionDao.findBySubjectId(s.getId()).isEmpty();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(Subject::getId)
                .collect(Collectors.toList());
        Map<Long, Integer> quotas = QuestionSampler.quotas(subjectIds, bands, basePerSubject, finalWeek);
        Set<Long> seen = regimenDao.findSeenQuestionIds(user.getId());
        Set<Long> lastWeek = weekNumber > 1
                ? regimenDao.findFormQuestionIdsForWeek(user.getId(), weekNumber - 1)
                : Set.of();

        List<Long> sampled = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quotas.entrySet()) {
            List<Question> pool = questionDao.findBySubjectId(entry.getKey());
            sampled.addAll(QuestionSampler.pick(pool, entry.getValue(), lastWeek, seen));
        }
        return sampled;
    }

    private Map<Long, SubjectBand> loadBandsForSampling(Long userId, int weekNumber) throws SQLException {
        Map<Long, SubjectBand> bands = new HashMap<>();
        if (weekNumber <= 1) {
            for (DiagnosticSubjectScore score : diagnosticScoreDao.findLatestByUserId(userId)) {
                bands.put(score.getSubjectId(), score.getBand());
            }
            return bands;
        }
        Optional<WeeklyRegimen> previous = regimenDao.findByUserAndWeek(userId, weekNumber - 1);
        if (previous.isPresent() && previous.get().hasOfficialScore()) {
            for (WeeklySubjectScore score : weeklyScoreDao.findByRegimenId(previous.get().getId())) {
                bands.put(score.getSubjectId(), score.getBand());
            }
            return bands;
        }
        Optional<WeeklyRegimen> lastCompleted = regimenDao.findLatestCompleted(userId);
        if (lastCompleted.isPresent()) {
            for (WeeklySubjectScore score : weeklyScoreDao.findByRegimenId(lastCompleted.get().getId())) {
                bands.put(score.getSubjectId(), score.getBand());
            }
            return bands;
        }
        for (DiagnosticSubjectScore score : diagnosticScoreDao.findLatestByUserId(userId)) {
            bands.put(score.getSubjectId(), score.getBand());
        }
        return bands;
    }

    private StudyPlan buildStudyPlan(Long userId, WeeklyRegimen regimen) throws SQLException {
        StudyPlan plan = new StudyPlan();
        User user = userDao.findById(userId).orElseThrow();
        plan.setEmailTo(user.getEmail());

        WeeklyRegimen source = regimen;
        if (!regimen.hasOfficialScore()) {
            source = regimenDao.findLatestCompleted(userId).orElse(regimen);
        }
        plan.setRegimen(source);
        plan.setEmailSent(source.getEmailSentAt() != null);

        if (source.hasOfficialScore()) {
            plan.setSubjectScores(weeklyScoreDao.findByRegimenId(source.getId()));
            plan.setMisses(loadMisses(source.getOfficialAttemptId()));
            plan.setFromDiagnostic(false);
        } else {
            plan.setFromDiagnostic(true);
            List<WeeklySubjectScore> fromDiag = new ArrayList<>();
            for (DiagnosticSubjectScore score : diagnosticScoreDao.findLatestByUserId(userId)) {
                WeeklySubjectScore row = new WeeklySubjectScore();
                row.setSubjectId(score.getSubjectId());
                row.setSubjectName(score.getSubjectName());
                row.setScorePercent(score.getScorePercent());
                row.setBand(score.getBand());
                fromDiag.add(row);
            }
            plan.setSubjectScores(fromDiag);
            plan.setMisses(List.of());
        }
        plan.setTargets(buildTargets(plan.getSubjectScores(), plan.getMisses()));
        return plan;
    }

    private List<AttemptAnswer> loadMisses(Long attemptId) throws SQLException {
        List<AttemptAnswer> misses = new ArrayList<>();
        for (AttemptAnswer answer : attemptDao.findAnswersByAttemptId(attemptId)) {
            if (!Boolean.TRUE.equals(answer.getCorrect())) {
                misses.add(answer);
            }
        }
        return misses;
    }

    static List<String> buildTargets(List<WeeklySubjectScore> scores, List<AttemptAnswer> misses) {
        List<String> targets = new ArrayList<>();
        Map<Long, Long> missCount = new LinkedHashMap<>();
        Map<Long, String> names = new HashMap<>();
        if (misses != null) {
            for (AttemptAnswer miss : misses) {
                if (miss.getQuestion() == null) {
                    continue;
                }
                Long subjectId = miss.getQuestion().getSubjectId();
                missCount.merge(subjectId, 1L, Long::sum);
                names.put(subjectId, miss.getQuestion().getSubjectName());
            }
        }
        List<WeeklySubjectScore> ordered = new ArrayList<>();
        if (scores != null) {
            for (WeeklySubjectScore score : scores) {
                if (score.getBand() == SubjectBand.WEAK) {
                    ordered.add(score);
                }
            }
            for (WeeklySubjectScore score : scores) {
                if (score.getBand() == SubjectBand.DEVELOPING) {
                    ordered.add(score);
                }
            }
        }
        for (WeeklySubjectScore score : ordered) {
            if (targets.size() >= 5) {
                break;
            }
            long missesForSubject = missCount.getOrDefault(score.getSubjectId(), 0L);
            String bandLabel = Messages.get(LocaleSupport.current(), "band." + score.getBand().name());
            if (missesForSubject > 0) {
                String key = missesForSubject == 1 ? "dashboard.target.review" : "dashboard.target.reviewPlural";
                targets.add(Messages.format(LocaleSupport.current(), key,
                        score.getSubjectName(), missesForSubject, bandLabel));
            } else {
                targets.add(Messages.format(LocaleSupport.current(), "dashboard.target.keep",
                        score.getSubjectName(), bandLabel, score.getScorePercent()));
            }
        }
        if (targets.size() < 3 && misses != null) {
            for (AttemptAnswer miss : misses) {
                if (targets.size() >= 5) {
                    break;
                }
                if (miss.getQuestion() == null) {
                    continue;
                }
                String prompt = miss.getQuestion().getPrompt();
                if (prompt != null && prompt.length() > 80) {
                    prompt = prompt.substring(0, 77) + "...";
                }
                String target = Messages.format(LocaleSupport.current(), "dashboard.target.reread", prompt);
                if (!targets.contains(target)) {
                    targets.add(target);
                }
            }
        }
        return targets.size() > 5 ? targets.subList(0, 5) : targets;
    }

    private List<WeeklySubjectScore> buildWeeklySubjectScores(Long regimenId, Long attemptId, List<Question> questions)
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
        List<WeeklySubjectScore> scores = new ArrayList<>();
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
            WeeklySubjectScore score = new WeeklySubjectScore();
            score.setRegimenId(regimenId);
            score.setSubjectId(entry.getKey());
            score.setSubjectName(subjectNames.get(entry.getKey()));
            score.setScorePercent(percent);
            score.setBand(DiagnosticService.bandForPercent(percent));
            scores.add(score);
        }
        return scores;
    }

    private void persistAnswers(Long attemptId, Map<Long, String> answers, List<Question> questions)
            throws SQLException {
        for (Question question : questions) {
            String selected = answers != null ? answers.get(question.getId()) : null;
            if (selected != null && !selected.isBlank()) {
                boolean correct = question.getCorrectOption().equalsIgnoreCase(selected);
                attemptDao.saveAnswer(attemptId, question.getId(), selected.toUpperCase(), correct);
            } else {
                attemptDao.saveAnswer(attemptId, question.getId(), null, false);
            }
        }
    }

    private BigDecimal calculateScore(Long attemptId, int totalQuestions) throws SQLException {
        if (totalQuestions == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        List<AttemptAnswer> answers = attemptDao.findAnswersByAttemptId(attemptId);
        long correct = answers.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
        return BigDecimal.valueOf(correct * 100.0 / totalQuestions).setScale(2, RoundingMode.HALF_UP);
    }

    private int countFreshWeakItems(User user, WeeklyRegimen regimen) throws SQLException {
        return sampleCheckpointQuestions(user, regimen).size();
    }

    private List<Long> sampleCheckpointQuestions(User user, WeeklyRegimen regimen) throws SQLException {
        Set<Long> weakSubjects = new HashSet<>();
        for (WeeklySubjectScore score : weeklyScoreDao.findByRegimenId(regimen.getId())) {
            if (score.getBand() == SubjectBand.WEAK || score.getBand() == SubjectBand.DEVELOPING) {
                weakSubjects.add(score.getSubjectId());
            }
        }
        if (weakSubjects.isEmpty()) {
            for (Subject subject : subjectDao.findByExamLevel(user.getExamLevel())) {
                weakSubjects.add(subject.getId());
            }
        }
        Set<Long> seen = regimenDao.findSeenQuestionIds(user.getId());
        Set<Long> thisForm = new HashSet<>(regimenDao.findFormQuestionIds(regimen.getId()));
        List<Question> pool = new ArrayList<>();
        for (Long subjectId : weakSubjects) {
            for (Question q : questionDao.findBySubjectId(subjectId)) {
                if (!thisForm.contains(q.getId())) {
                    pool.add(q);
                }
            }
        }
        return QuestionSampler.pick(pool, checkpointQuestionCount(), thisForm, seen);
    }
}
