<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.dashboard.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1><fmt:message key="dashboard.welcome"><fmt:param value="${fn:escapeXml(currentUser.username)}"/></fmt:message></h1>
<p class="subtitle">
    <fmt:message key="dashboard.subtitle"/>
    <c:if test="${not empty examLevel}">
        <span class="badge badge-user"><fmt:message key="dashboard.track"><fmt:param>
            <fmt:message key="examLevel.${examLevel}"/>
        </fmt:param></fmt:message></span>
    </c:if>
</p>

<c:if test="${not empty weeklyError}">
    <div class="alert alert-warning">${weeklyError}</div>
</c:if>

<c:if test="${weekly.missedWeekNotice}">
    <div class="alert alert-warning">
        <fmt:message key="dashboard.missedWeek"/>
    </div>
</c:if>

<c:if test="${not empty weekly.current}">
    <div class="card">
        <h2><fmt:message key="dashboard.thisWeek"/>
            <span class="badge badge-${weekly.current.status}"><fmt:message key="status.${weekly.current.status}"/></span>
            <c:if test="${weekly.current.finalWeek}">
                <span class="badge badge-WEEKLY"><fmt:message key="dashboard.readinessWeek"/></span>
            </c:if>
        </h2>
        <p class="exam-meta">
            <fmt:message key="dashboard.weekOf"><fmt:param value="${weekly.current.weekNumber}"/><fmt:param value="${weekly.totalWeeks}"/></fmt:message>
            &middot; ${ep:fmt(weekly.current.weekStart)} – ${ep:fmt(weekly.current.weekEnd)}
        </p>
        <c:if test="${weekly.current.finalWeek}">
            <p class="hint"><fmt:message key="dashboard.finalHint"/></p>
        </c:if>
        <c:if test="${not empty weekly.bankWarning}">
            <p class="hint">${weekly.bankWarning}</p>
        </c:if>
        <div class="actions">
            <c:if test="${weekly.canStartWeekly}">
                <a href="${ctx}/user/weekly" class="btn btn-primary"><fmt:message key="dashboard.startWeekly"/></a>
            </c:if>
            <c:if test="${weekly.canContinueWeekly}">
                <a href="${ctx}/user/weekly?attemptId=${ep:enc(weekly.inProgressWeeklyAttemptId)}" class="btn btn-primary"><fmt:message key="dashboard.continueExam"/></a>
            </c:if>
            <c:if test="${weekly.current.officialAttemptId != null}">
                <a href="${ctx}/user/study-plan?regimenId=${ep:enc(weekly.current.id)}" class="btn btn-outline"><fmt:message key="dashboard.studyPlan"/></a>
            </c:if>
            <c:if test="${weekly.canReview}">
                <a href="${ctx}/user/review?regimenId=${ep:enc(weekly.studyPlan.regimen.id)}" class="btn btn-outline"><fmt:message key="dashboard.reviewMisses"/></a>
            </c:if>
            <c:if test="${weekly.canContinueCheckpoint}">
                <a href="${ctx}/user/checkpoint?attemptId=${ep:enc(weekly.inProgressCheckpointAttemptId)}" class="btn btn-primary"><fmt:message key="dashboard.continueCheckpoint"/></a>
            </c:if>
            <c:if test="${weekly.checkpointAvailable}">
                <a href="${ctx}/user/checkpoint" class="btn btn-outline"><fmt:message key="dashboard.checkpoint"/></a>
            </c:if>
        </div>
    </div>
</c:if>

<c:if test="${not empty weekly.studyPlan}">
    <div class="card">
        <h2><fmt:message key="dashboard.studyPlanHeading"/></h2>
        <c:if test="${weekly.studyPlan.fromDiagnostic}">
            <p class="hint"><fmt:message key="dashboard.studyPlanFromDiagnostic"/></p>
        </c:if>
        <c:if test="${weekly.studyPlan.emailSent}">
            <p class="hint"><fmt:message key="dashboard.digestSent"><fmt:param value="${weekly.studyPlan.emailTo}"/></fmt:message></p>
        </c:if>
        <c:if test="${not empty weekly.studyPlan.subjectScores}">
            <table class="data-table">
                <thead>
                <tr>
                    <th><fmt:message key="dashboard.subject"/></th>
                    <th><fmt:message key="dashboard.score"/></th>
                    <th><fmt:message key="dashboard.band"/></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="s" items="${weekly.studyPlan.subjectScores}">
                    <tr>
                        <td>${s.subjectName}</td>
                        <td>${s.scorePercent}%</td>
                        <td><span class="badge badge-${s.band}"><fmt:message key="band.${s.band}"/></span></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:if>
        <c:if test="${not empty weekly.studyPlan.targets}">
            <h3><fmt:message key="dashboard.focus"/></h3>
            <ul class="subject-list">
                <c:forEach var="t" items="${weekly.studyPlan.targets}">
                    <li>${t}</li>
                </c:forEach>
            </ul>
        </c:if>
        <c:if test="${weekly.studyPlan.regimen != null && weekly.studyPlan.regimen.officialAttemptId != null}">
            <div class="actions">
                <a href="${ctx}/user/study-plan?regimenId=${ep:enc(weekly.studyPlan.regimen.id)}" class="btn btn-primary"><fmt:message key="dashboard.openStudyPlan"/></a>
            </div>
        </c:if>
    </div>
</c:if>

<div class="grid-2">
    <div class="card">
        <h2><fmt:message key="dashboard.subjects"/></h2>
        <ul class="subject-list">
            <c:forEach var="subject" items="${subjects}">
                <li><strong><c:out value="${subject.name}"/></strong> — <c:out value="${subject.description}"/></li>
            </c:forEach>
        </ul>
    </div>
    <div class="card">
        <h2><fmt:message key="dashboard.practice"/></h2>
        <p class="hint"><fmt:message key="dashboard.practiceHint"/></p>
        <c:choose>
            <c:when test="${empty exams}">
                <p class="empty-state"><fmt:message key="dashboard.noPractice"/></p>
            </c:when>
            <c:otherwise>
                <div class="exam-grid">
                    <c:forEach var="exam" items="${exams}">
                        <div class="exam-card">
                            <h3><c:out value="${exam.title}"/></h3>
                            <p class="exam-meta"><c:out value="${exam.subjectName}"/></p>
                            <p class="exam-meta"><fmt:message key="common.questions"><fmt:param value="${exam.questionCount}"/></fmt:message>
                                &middot; <fmt:message key="common.minutes"><fmt:param value="${exam.durationMinutes}"/></fmt:message></p>
                            <a href="${ctx}/user/exam?examId=${ep:enc(exam.id)}" class="btn btn-outline"><fmt:message key="dashboard.startPractice"/></a>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<c:if test="${not empty history}">
    <div class="card">
        <h2><fmt:message key="dashboard.recent"/></h2>
        <table class="data-table">
            <thead>
            <tr>
                <th><fmt:message key="dashboard.exam"/></th>
                <th><fmt:message key="dashboard.type"/></th>
                <th><fmt:message key="dashboard.score"/></th>
                <th><fmt:message key="dashboard.status"/></th>
                <th></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="h" items="${history}" begin="0" end="4">
                <tr>
                    <td>${h.examTitle}</td>
                    <td><span class="badge badge-${h.attemptKind}"><fmt:message key="attemptKind.${h.attemptKind}"/></span></td>
                    <td>${h.scorePercent != null ? h.scorePercent : '-'}%</td>
                    <td><span class="badge badge-${h.status}"><fmt:message key="status.${h.status}"/></span></td>
                    <td>
                        <c:if test="${h.status != 'IN_PROGRESS'}">
                            <c:choose>
                                <c:when test="${h.attemptKind == 'WEEKLY' && h.regimenId != null}">
                                    <a href="${ctx}/user/study-plan?regimenId=${ep:enc(h.regimenId)}" class="btn btn-sm"><fmt:message key="action.view"/></a>
                                </c:when>
                                <c:when test="${h.diagnostic}">
                                    <a href="${ctx}/user/diagnostic/result?attemptId=${ep:enc(h.id)}" class="btn btn-sm"><fmt:message key="action.view"/></a>
                                </c:when>
                                <c:otherwise>
                                    <a href="${ctx}/user/result?attemptId=${ep:enc(h.id)}" class="btn btn-sm"><fmt:message key="action.view"/></a>
                                </c:otherwise>
                            </c:choose>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        <a href="${ctx}/user/history" class="btn btn-outline"><fmt:message key="dashboard.viewAll"/></a>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
