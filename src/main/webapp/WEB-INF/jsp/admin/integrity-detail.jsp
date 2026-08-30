<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.integrityDetail.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<p><a href="${ctx}/admin/integrity">&larr; <fmt:message key="integrityDetail.back"/></a></p>
<h1><fmt:message key="integrityDetail.heading"/></h1>
<p class="subtitle"><c:out value="${attempt.username}"/> &middot; <c:out value="${attempt.examTitle}"/></p>

<div class="stats-grid">
    <div class="stat-card">
        <span class="stat-value">${attempt.leaveCount}</span>
        <span class="stat-label"><fmt:message key="integrityDetail.leaves"/></span>
    </div>
    <div class="stat-card">
        <span class="stat-value">${attempt.suspectLeaveCount}</span>
        <span class="stat-label"><fmt:message key="integrityDetail.suspectLeaves"/></span>
    </div>
    <div class="stat-card">
        <span class="stat-value">${attempt.scorePercent != null ? attempt.scorePercent : '—'}</span>
        <span class="stat-label"><fmt:message key="integrityDetail.score"/></span>
    </div>
    <div class="stat-card">
        <span class="stat-value"><span class="badge badge-${attempt.status}"><fmt:message key="status.${attempt.status}"/></span></span>
        <span class="stat-label">
            <c:choose>
                <c:when test="${attempt.diagnostic}"><fmt:message key="attemptKind.DIAGNOSTIC"/></c:when>
                <c:otherwise><fmt:message key="attemptKind.PRACTICE"/></c:otherwise>
            </c:choose>
        </span>
    </div>
</div>

<div class="card">
    <c:choose>
        <c:when test="${empty events}">
            <p class="empty-state"><fmt:message key="integrityDetail.empty"/></p>
        </c:when>
        <c:otherwise>
            <table class="data-table">
                <thead>
                <tr>
                    <th><fmt:message key="integrityDetail.when"/></th>
                    <th><fmt:message key="integrityDetail.event"/></th>
                    <th><fmt:message key="integrityDetail.question"/></th>
                    <th><fmt:message key="integrityDetail.difficulty"/></th>
                    <th><fmt:message key="integrityDetail.answered"/></th>
                    <th><fmt:message key="integrityDetail.away"/></th>
                    <th><fmt:message key="integrityDetail.flag"/></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="e" items="${events}">
                    <tr>
                        <td>${ep:fmt(e.occurredAt)}</td>
                        <td>${e.eventType}</td>
                        <td>
                            <c:if test="${e.questionNumber != null}"><fmt:message key="integrityDetail.questionPrefix"><fmt:param value="${e.questionNumber}"/></fmt:message> </c:if>
                            ${e.questionPrompt}
                        </td>
                        <td>${e.questionDifficulty}</td>
                        <td><c:choose><c:when test="${e.questionAnswered}"><fmt:message key="common.yes"/></c:when><c:otherwise><fmt:message key="common.no"/></c:otherwise></c:choose></td>
                        <td>${e.awayDurationLabel}</td>
                        <td>
                            <c:if test="${e.suspect}">
                                <span class="badge badge-suspect"><fmt:message key="integrityDetail.suspectBadge"/></span>
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
