<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Study Plan" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Study plan</h1>
<p class="subtitle">
    What to review after your official week score.
    <c:if test="${not empty regimen}">
        <span class="badge badge-${regimen.status}">Week ${regimen.weekNumber}</span>
    </c:if>
</p>

<c:if test="${studyPlan.fromDiagnostic}">
    <div class="alert alert-warning">This plan is from your placement diagnostic. Take this week's exam for an updated plan.</div>
</c:if>

<c:if test="${studyPlan.emailSent}">
    <p class="hint">A digest was sent to ${studyPlan.emailTo}. This page is the full plan.</p>
</c:if>

<div class="card">
    <h2>Subject bands</h2>
    <c:choose>
        <c:when test="${empty studyPlan.subjectScores}">
            <p class="empty-state">No subject scores yet.</p>
        </c:when>
        <c:otherwise>
            <table class="data-table">
                <thead>
                <tr><th>Subject</th><th>Score</th><th>Band</th></tr>
                </thead>
                <tbody>
                <c:forEach var="s" items="${studyPlan.subjectScores}">
                    <tr>
                        <td>${s.subjectName}</td>
                        <td>${s.scorePercent}%</td>
                        <td><span class="badge badge-${s.band}">${s.band}</span></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <h2>Focus (3–5 targets)</h2>
    <c:choose>
        <c:when test="${empty studyPlan.targets}">
            <p class="empty-state">No review targets yet.</p>
        </c:when>
        <c:otherwise>
            <ul class="subject-list">
                <c:forEach var="t" items="${studyPlan.targets}">
                    <li>${t}</li>
                </c:forEach>
            </ul>
        </c:otherwise>
    </c:choose>
</div>

<div class="actions">
    <c:if test="${not empty studyPlan.misses}">
        <a href="${ctx}/user/review?regimenId=${ep:enc(regimen.id)}" class="btn btn-primary">Review misses</a>
    </c:if>
    <a href="${ctx}/user/dashboard" class="btn btn-outline">Back to dashboard</a>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
