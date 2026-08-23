<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.review.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1><fmt:message key="review.heading"/></h1>
<p class="subtitle"><fmt:message key="review.subtitle"/></p>

<c:choose>
    <c:when test="${empty misses}">
        <div class="card">
            <p class="empty-state"><fmt:message key="review.empty"/></p>
        </div>
    </c:when>
    <c:otherwise>
        <c:forEach var="a" items="${misses}" varStatus="status">
            <div class="review-card incorrect">
                <h3><fmt:message key="review.miss"><fmt:param value="${status.index + 1}"/></fmt:message>
                    <c:if test="${not empty a.question.subjectName}">
                        <span class="exam-meta">(${a.question.subjectName})</span>
                    </c:if>
                </h3>
                <p>${a.question.prompt}</p>
                <c:set var="imageUrl" value="${a.question.imageUrl}"/>
                <fmt:message key="exam.imageAlt" var="imageAlt"><fmt:param value="${status.index + 1}"/></fmt:message>
                <c:set var="imageLoading" value="lazy"/>
                <%@ include file="/WEB-INF/jsp/partials/question-image.jsp" %>
                <p><strong><fmt:message key="review.yourAnswer"/></strong>
                    ${a.selectedOption != null ? a.selectedOption : ''}
                    <c:if test="${a.selectedOption == null}"><fmt:message key="review.notAnswered"/></c:if>
                    — ${a.question.getOptionText(a.selectedOption)}</p>
                <p><strong><fmt:message key="review.correctAnswer"/></strong> ${a.question.correctOption}
                    — ${a.question.getOptionText(a.question.correctOption)}</p>
                <c:if test="${not empty a.question.explanation}">
                    <p><strong><fmt:message key="review.explanation"/></strong> ${a.question.explanation}</p>
                </c:if>
            </div>
        </c:forEach>
    </c:otherwise>
</c:choose>

<div class="actions">
    <c:if test="${not empty regimen}">
        <a href="${ctx}/user/study-plan?regimenId=${ep:enc(regimen.id)}" class="btn btn-outline"><fmt:message key="review.backStudyPlan"/></a>
    </c:if>
    <a href="${ctx}/user/dashboard" class="btn btn-primary"><fmt:message key="review.dashboard"/></a>
</div>

<script src="${ctx}/js/question-image.js"></script>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
