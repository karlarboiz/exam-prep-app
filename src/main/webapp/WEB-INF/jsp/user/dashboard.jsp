<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Dashboard" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Welcome, ${currentUser.username}</h1>
<p class="subtitle">
    This week's regimen
    <c:if test="${not empty examLevel}">
        <span class="badge badge-user">${examLevel.displayName()} track</span>
    </c:if>
</p>

<c:if test="${not empty weeklyError}">
    <div class="alert alert-warning">${weeklyError}</div>
</c:if>

<c:if test="${weekly.missedWeekNotice}">
    <div class="alert alert-warning">
        A previous week was marked missed because the exam was not submitted in time.
        Your last study plan is still available. This week's form is ready when the clock rolled.
    </div>
</c:if>

<c:if test="${not empty weekly.current}">
    <div class="card">
        <h2>This week
            <span class="badge badge-${weekly.current.status}">${weekly.current.status}</span>
            <c:if test="${weekly.current.finalWeek}">
                <span class="badge badge-WEEKLY">Readiness week</span>
            </c:if>
        </h2>
        <p class="exam-meta">
            Week ${weekly.current.weekNumber} of ${weekly.totalWeeks}
            &middot; ${ep:fmt(weekly.current.weekStart)} – ${ep:fmt(weekly.current.weekEnd)}
        </p>
        <c:if test="${weekly.current.finalWeek}">
            <p class="hint">Last week of access — mixed readiness exam, not a new topic dump.</p>
        </c:if>
        <c:if test="${not empty weekly.bankWarning}">
            <p class="hint">${weekly.bankWarning}</p>
        </c:if>
        <div class="actions">
            <c:if test="${weekly.canStartWeekly}">
                <a href="${ctx}/user/weekly" class="btn btn-primary">Start this week's exam</a>
            </c:if>
            <c:if test="${weekly.canContinueWeekly}">
                <a href="${ctx}/user/weekly?attemptId=${ep:enc(weekly.inProgressWeeklyAttemptId)}" class="btn btn-primary">Continue exam</a>
            </c:if>
            <c:if test="${weekly.current.officialAttemptId != null}">
                <a href="${ctx}/user/study-plan?regimenId=${ep:enc(weekly.current.id)}" class="btn btn-outline">Study plan</a>
            </c:if>
            <c:if test="${weekly.canReview}">
                <a href="${ctx}/user/review?regimenId=${ep:enc(weekly.studyPlan.regimen.id)}" class="btn btn-outline">Review misses</a>
            </c:if>
            <c:if test="${weekly.canContinueCheckpoint}">
                <a href="${ctx}/user/checkpoint?attemptId=${ep:enc(weekly.inProgressCheckpointAttemptId)}" class="btn btn-primary">Continue checkpoint</a>
            </c:if>
            <c:if test="${weekly.checkpointAvailable}">
                <a href="${ctx}/user/checkpoint" class="btn btn-outline">Mid-week checkpoint</a>
            </c:if>
        </div>
    </div>
</c:if>

<c:if test="${not empty weekly.studyPlan}">
    <div class="card">
        <h2>Study plan</h2>
        <c:if test="${weekly.studyPlan.fromDiagnostic}">
            <p class="hint">Seeded from your placement diagnostic until you finish this week's exam.</p>
        </c:if>
        <c:if test="${weekly.studyPlan.emailSent}">
            <p class="hint">A digest was sent to ${weekly.studyPlan.emailTo}.</p>
        </c:if>
        <c:if test="${not empty weekly.studyPlan.subjectScores}">
            <table class="data-table">
                <thead>
                <tr><th>Subject</th><th>Score</th><th>Band</th></tr>
                </thead>
                <tbody>
                <c:forEach var="s" items="${weekly.studyPlan.subjectScores}">
                    <tr>
                        <td>${s.subjectName}</td>
                        <td>${s.scorePercent}%</td>
                        <td><span class="badge badge-${s.band}">${s.band}</span></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:if>
        <c:if test="${not empty weekly.studyPlan.targets}">
            <h3>Focus</h3>
            <ul class="subject-list">
                <c:forEach var="t" items="${weekly.studyPlan.targets}">
                    <li>${t}</li>
                </c:forEach>
            </ul>
        </c:if>
        <c:if test="${weekly.studyPlan.regimen != null && weekly.studyPlan.regimen.officialAttemptId != null}">
            <div class="actions">
                <a href="${ctx}/user/study-plan?regimenId=${ep:enc(weekly.studyPlan.regimen.id)}" class="btn btn-primary">Open study plan</a>
            </div>
        </c:if>
    </div>
</c:if>

<div class="grid-2">
    <div class="card">
        <h2>Subjects</h2>
        <ul class="subject-list">
            <c:forEach var="subject" items="${subjects}">
                <li><strong>${subject.name}</strong> — ${subject.description}</li>
            </c:forEach>
        </ul>
    </div>
    <div class="card">
        <h2>Optional practice</h2>
        <p class="hint">Extra papers if you want more volume. They do not replace this week's official score.</p>
        <c:choose>
            <c:when test="${empty exams}">
                <p class="empty-state">No extra practice exams.</p>
            </c:when>
            <c:otherwise>
                <div class="exam-grid">
                    <c:forEach var="exam" items="${exams}">
                        <div class="exam-card">
                            <h3>${exam.title}</h3>
                            <p class="exam-meta">${exam.subjectName}</p>
                            <p class="exam-meta">${exam.questionCount} questions &middot; ${exam.durationMinutes} minutes</p>
                            <a href="${ctx}/user/exam?examId=${ep:enc(exam.id)}" class="btn btn-outline">Start practice</a>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<c:if test="${not empty history}">
    <div class="card">
        <h2>Recent Attempts</h2>
        <table class="data-table">
            <thead>
            <tr><th>Exam</th><th>Type</th><th>Score</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
            <c:forEach var="h" items="${history}" begin="0" end="4">
                <tr>
                    <td>${h.examTitle}</td>
                    <td><span class="badge badge-${h.attemptKind}">${h.attemptKind.displayName()}</span></td>
                    <td>${h.scorePercent != null ? h.scorePercent : '-'}%</td>
                    <td><span class="badge badge-${h.status}">${h.status}</span></td>
                    <td>
                        <c:if test="${h.status != 'IN_PROGRESS'}">
                            <c:choose>
                                <c:when test="${h.attemptKind == 'WEEKLY' && h.regimenId != null}">
                                    <a href="${ctx}/user/study-plan?regimenId=${ep:enc(h.regimenId)}" class="btn btn-sm">View</a>
                                </c:when>
                                <c:when test="${h.diagnostic}">
                                    <a href="${ctx}/user/diagnostic/result?attemptId=${ep:enc(h.id)}" class="btn btn-sm">View</a>
                                </c:when>
                                <c:otherwise>
                                    <a href="${ctx}/user/result?attemptId=${ep:enc(h.id)}" class="btn btn-sm">View</a>
                                </c:otherwise>
                            </c:choose>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        <a href="${ctx}/user/history" class="btn btn-outline">View All History</a>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
