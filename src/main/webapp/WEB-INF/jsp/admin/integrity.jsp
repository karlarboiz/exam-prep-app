<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.integrity.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1><fmt:message key="integrity.heading"/></h1>
<p class="subtitle"><fmt:message key="integrity.subtitle"/></p>

<div class="card">
    <c:choose>
        <c:when test="${empty flagged}">
            <p class="empty-state"><fmt:message key="integrity.empty"/></p>
        </c:when>
        <c:otherwise>
            <table class="data-table">
                <thead>
                <tr>
                    <th><fmt:message key="integrity.user"/></th>
                    <th><fmt:message key="integrity.exam"/></th>
                    <th><fmt:message key="integrity.type"/></th>
                    <th><fmt:message key="integrity.started"/></th>
                    <th><fmt:message key="integrity.status"/></th>
                    <th><fmt:message key="integrity.score"/></th>
                    <th><fmt:message key="integrity.leaves"/></th>
                    <th><fmt:message key="integrity.suspect"/></th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="a" items="${flagged}">
                    <tr>
                        <td>${a.username}</td>
                        <td>${a.examTitle}</td>
                        <td>
                            <c:choose>
                                <c:when test="${a.diagnostic}">
                                    <span class="badge badge-diagnostic"><fmt:message key="attemptKind.DIAGNOSTIC"/></span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge badge-practice"><fmt:message key="attemptKind.PRACTICE"/></span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>${ep:fmt(a.startedAt)}</td>
                        <td><span class="badge badge-${a.status}"><fmt:message key="status.${a.status}"/></span></td>
                        <td>${a.scorePercent != null ? a.scorePercent : '—'}%</td>
                        <td>${a.leaveCount}</td>
                        <td><span class="badge badge-suspect">${a.suspectLeaveCount}</span></td>
                        <td>
                            <a href="${ctx}/admin/integrity?attemptId=${ep:enc(a.id)}" class="btn btn-sm"><fmt:message key="integrity.view"/></a>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
