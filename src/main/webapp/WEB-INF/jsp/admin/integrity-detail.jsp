<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Attempt integrity" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<p><a href="${ctx}/admin/integrity">&larr; Flagged attempts</a></p>
<h1>Integrity timeline</h1>
<p class="subtitle">${attempt.username} &middot; ${attempt.examTitle}</p>

<div class="stats-grid">
    <div class="stat-card">
        <span class="stat-value">${attempt.leaveCount}</span>
        <span class="stat-label">Leaves</span>
    </div>
    <div class="stat-card">
        <span class="stat-value">${attempt.suspectLeaveCount}</span>
        <span class="stat-label">Suspect leaves</span>
    </div>
    <div class="stat-card">
        <span class="stat-value">${attempt.scorePercent != null ? attempt.scorePercent : '—'}</span>
        <span class="stat-label">Score %</span>
    </div>
    <div class="stat-card">
        <span class="stat-value"><span class="badge badge-${attempt.status}">${attempt.status}</span></span>
        <span class="stat-label">
            <c:choose>
                <c:when test="${attempt.diagnostic}">Diagnostic</c:when>
                <c:otherwise>Practice</c:otherwise>
            </c:choose>
        </span>
    </div>
</div>

<div class="card">
    <c:choose>
        <c:when test="${empty events}">
            <p class="empty-state">No leave or return events on this attempt.</p>
        </c:when>
        <c:otherwise>
            <table class="data-table">
                <thead>
                <tr>
                    <th>When</th>
                    <th>Event</th>
                    <th>Question</th>
                    <th>Difficulty</th>
                    <th>Answered?</th>
                    <th>Away</th>
                    <th>Flag</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="e" items="${events}">
                    <tr>
                        <td>${ep:fmt(e.occurredAt)}</td>
                        <td>${e.eventType}</td>
                        <td>
                            <c:if test="${e.questionNumber != null}">Q${e.questionNumber}: </c:if>
                            ${e.questionPrompt}
                        </td>
                        <td>${e.questionDifficulty}</td>
                        <td>${e.questionAnswered ? 'Yes' : 'No'}</td>
                        <td>${e.awayDurationLabel}</td>
                        <td>
                            <c:if test="${e.suspect}">
                                <span class="badge badge-suspect">Suspect</span>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
