<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.diagnosticResult.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="result-header">
    <h1><fmt:message key="diagnosticResult.heading"/></h1>
    <h2>${attempt.examTitle}</h2>
    <p class="exam-meta"><fmt:message key="diagnosticResult.meta"/></p>
</div>

<div class="result-summary card">
    <div class="score-circle">
        <span class="score-value">${attempt.scorePercent != null ? attempt.scorePercent : 0}%</span>
        <span class="score-label"><fmt:message key="diagnosticResult.overall"/></span>
    </div>
    <div class="result-details">
        <p><strong><fmt:message key="diagnosticResult.readiness"/></strong> <fmt:message key="readiness.${readiness}"/></p>
        <p><strong><fmt:message key="diagnosticResult.mean"/></strong> ${meanSubjectPercent}%</p>
        <p><strong><fmt:message key="diagnosticResult.status"/></strong> <span class="badge badge-${attempt.status}"><fmt:message key="status.${attempt.status}"/></span></p>
        <p><strong><fmt:message key="diagnosticResult.started"/></strong> ${ep:fmt(attempt.startedAt)}</p>
        <c:if test="${attempt.completedAt != null}">
            <p><strong><fmt:message key="diagnosticResult.completed"/></strong> ${ep:fmt(attempt.completedAt)}</p>
        </c:if>
    </div>
</div>

<div class="card">
    <h2><fmt:message key="diagnosticResult.strength"/></h2>
    <c:choose>
        <c:when test="${empty subjectScores}">
            <p><fmt:message key="diagnosticResult.empty"/></p>
        </c:when>
        <c:otherwise>
            <table class="data-table">
                <thead>
                <tr>
                    <th><fmt:message key="diagnosticResult.subject"/></th>
                    <th><fmt:message key="diagnosticResult.score"/></th>
                    <th><fmt:message key="diagnosticResult.band"/></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="s" items="${subjectScores}">
                    <tr>
                        <td>${s.subjectName}</td>
                        <td>${s.scorePercent}%</td>
                        <td><span class="badge badge-${s.band}"><fmt:message key="band.${s.band}"/></span></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <h2><fmt:message key="diagnosticResult.review"/></h2>
    <c:forEach var="a" items="${answers}" varStatus="status">
        <div class="review-card ${a.correct ? 'correct' : 'incorrect'}">
            <h3><fmt:message key="result.question"><fmt:param value="${status.index + 1}"/></fmt:message>
                <c:if test="${not empty a.question.subjectName}">
                    <span class="exam-meta">(${a.question.subjectName})</span>
                </c:if>
            </h3>
            <p>${a.question.prompt}</p>
            <c:set var="imageUrl" value="${a.question.imageUrl}"/>
            <fmt:message key="exam.imageAlt" var="imageAlt"><fmt:param value="${status.index + 1}"/></fmt:message>
            <c:set var="imageLoading" value="lazy"/>
            <%@ include file="/WEB-INF/jsp/partials/question-image.jsp" %>
            <p><strong><fmt:message key="result.yourAnswer"/></strong>
                <c:choose>
                    <c:when test="${a.selectedOption != null}">${a.selectedOption}</c:when>
                    <c:otherwise><fmt:message key="result.notAnswered"/></c:otherwise>
                </c:choose>
                — ${a.question.getOptionText(a.selectedOption)}</p>
            <c:if test="${!a.correct}">
                <p><strong><fmt:message key="result.correctAnswer"/></strong> ${a.question.correctOption}
                    — ${a.question.getOptionText(a.question.correctOption)}</p>
            </c:if>
            <c:if test="${not empty a.question.explanation}">
                <p><strong><fmt:message key="result.explanation"/></strong> ${a.question.explanation}</p>
            </c:if>
        </div>
    </c:forEach>
</div>

<div class="action-links">
    <a href="${ctx}/user/dashboard" class="btn btn-primary"><fmt:message key="diagnosticResult.continue"/></a>
</div>

<script src="${ctx}/js/question-image.js"></script>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
