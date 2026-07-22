<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Diagnostic Result" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="result-header">
    <h1>Diagnostic Result</h1>
    <h2>${attempt.examTitle}</h2>
    <p class="exam-meta">Placement assessment</p>
</div>

<div class="result-summary card">
    <div class="score-circle">
        <span class="score-value">${attempt.scorePercent != null ? attempt.scorePercent : 0}%</span>
        <span class="score-label">Overall</span>
    </div>
    <div class="result-details">
        <p><strong>Readiness:</strong> ${readiness.displayName}</p>
        <p><strong>Mean subject score:</strong> ${meanSubjectPercent}%</p>
        <p><strong>Status:</strong> <span class="badge badge-${attempt.status}">${attempt.status}</span></p>
        <p><strong>Started:</strong> ${ep:fmt(attempt.startedAt)}</p>
        <c:if test="${attempt.completedAt != null}">
            <p><strong>Completed:</strong> ${ep:fmt(attempt.completedAt)}</p>
        </c:if>
    </div>
</div>

<div class="card">
    <h2>Subject Strength</h2>
    <c:choose>
        <c:when test="${empty subjectScores}">
            <p>No subjects were assessed (question bank may be empty).</p>
        </c:when>
        <c:otherwise>
            <table class="data-table">
                <thead>
                <tr><th>Subject</th><th>Score</th><th>Band</th></tr>
                </thead>
                <tbody>
                <c:forEach var="s" items="${subjectScores}">
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
    <h2>Answer Review</h2>
    <c:forEach var="a" items="${answers}" varStatus="status">
        <div class="review-card ${a.correct ? 'correct' : 'incorrect'}">
            <h3>Question ${status.index + 1}
                <c:if test="${not empty a.question.subjectName}">
                    <span class="exam-meta">(${a.question.subjectName})</span>
                </c:if>
            </h3>
            <p>${a.question.prompt}</p>
            <p><strong>Your answer:</strong> ${a.selectedOption != null ? a.selectedOption : 'Not answered'}
                — ${a.question.getOptionText(a.selectedOption)}</p>
            <c:if test="${!a.correct}">
                <p><strong>Correct answer:</strong> ${a.question.correctOption}
                    — ${a.question.getOptionText(a.question.correctOption)}</p>
            </c:if>
            <c:if test="${not empty a.question.explanation}">
                <p><strong>Explanation:</strong> ${a.question.explanation}</p>
            </c:if>
        </div>
    </c:forEach>
</div>

<div class="action-links">
    <a href="${ctx}/user/dashboard" class="btn btn-primary">Continue to Dashboard</a>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
