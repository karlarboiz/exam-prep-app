<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Review" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Review misses</h1>
<p class="subtitle">Untimed replay. This does not change your official week score.</p>

<c:choose>
    <c:when test="${empty misses}">
        <div class="card">
            <p class="empty-state">No missed items to review.</p>
        </div>
    </c:when>
    <c:otherwise>
        <c:forEach var="a" items="${misses}" varStatus="status">
            <div class="review-card incorrect">
                <h3>Miss ${status.index + 1}
                    <c:if test="${not empty a.question.subjectName}">
                        <span class="exam-meta">(${a.question.subjectName})</span>
                    </c:if>
                </h3>
                <p>${a.question.prompt}</p>
                <c:set var="imageUrl" value="${a.question.imageUrl}"/>
                <c:set var="imageAlt" value="Diagram for miss ${status.index + 1}"/>
                <c:set var="imageLoading" value="lazy"/>
                <%@ include file="/WEB-INF/jsp/partials/question-image.jsp" %>
                <p><strong>Your answer:</strong> ${a.selectedOption != null ? a.selectedOption : 'Not answered'}
                    — ${a.question.getOptionText(a.selectedOption)}</p>
                <p><strong>Correct answer:</strong> ${a.question.correctOption}
                    — ${a.question.getOptionText(a.question.correctOption)}</p>
                <c:if test="${not empty a.question.explanation}">
                    <p><strong>Explanation:</strong> ${a.question.explanation}</p>
                </c:if>
            </div>
        </c:forEach>
    </c:otherwise>
</c:choose>

<div class="actions">
    <c:if test="${not empty regimen}">
        <a href="${ctx}/user/study-plan?regimenId=${ep:enc(regimen.id)}" class="btn btn-outline">Back to study plan</a>
    </c:if>
    <a href="${ctx}/user/dashboard" class="btn btn-primary">Dashboard</a>
</div>

<script src="${ctx}/js/question-image.js"></script>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
