<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.studyPlan.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1><fmt:message key="studyPlan.heading"/></h1>
<p class="subtitle">
    <fmt:message key="studyPlan.subtitle"/>
    <c:if test="${not empty regimen}">
        <span class="badge badge-${regimen.status}"><fmt:message key="studyPlan.week"><fmt:param value="${regimen.weekNumber}"/></fmt:message></span>
    </c:if>
</p>

<c:if test="${studyPlan.fromDiagnostic}">
    <div class="alert alert-warning"><fmt:message key="studyPlan.fromDiagnostic"/></div>
</c:if>

<c:if test="${studyPlan.emailSent}">
    <p class="hint"><fmt:message key="studyPlan.digestSent"><fmt:param value="${fn:escapeXml(studyPlan.emailTo)}"/></fmt:message></p>
</c:if>

<div class="card">
    <h2><fmt:message key="studyPlan.bands"/></h2>
    <c:choose>
        <c:when test="${empty studyPlan.subjectScores}">
            <p class="empty-state"><fmt:message key="studyPlan.noScores"/></p>
        </c:when>
        <c:otherwise>
            <table class="data-table">
                <thead>
                <tr>
                    <th><fmt:message key="studyPlan.subject"/></th>
                    <th><fmt:message key="studyPlan.score"/></th>
                    <th><fmt:message key="studyPlan.band"/></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="s" items="${studyPlan.subjectScores}">
                    <tr>
                        <td><c:out value="${s.subjectName}"/></td>
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
    <h2><fmt:message key="studyPlan.focus"/></h2>
    <c:choose>
        <c:when test="${empty studyPlan.targets}">
            <p class="empty-state"><fmt:message key="studyPlan.noTargets"/></p>
        </c:when>
        <c:otherwise>
            <ul class="subject-list">
                <c:forEach var="t" items="${studyPlan.targets}">
                    <li><c:out value="${t}"/></li>
                </c:forEach>
            </ul>
        </c:otherwise>
    </c:choose>
</div>

<div class="actions">
    <c:if test="${not empty studyPlan.misses}">
        <a href="${ctx}/user/review?regimenId=${ep:enc(regimen.id)}" class="btn btn-primary"><fmt:message key="studyPlan.reviewMisses"/></a>
    </c:if>
    <a href="${ctx}/user/dashboard" class="btn btn-outline"><fmt:message key="studyPlan.back"/></a>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
