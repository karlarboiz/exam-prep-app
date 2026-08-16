<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Integrity" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Flagged attempts</h1>
<p class="subtitle">Attempts with at least one suspect leave — unanswered HARD item while away from the page.</p>

<div class="card">
    <c:choose>
        <c:when test="${empty flagged}">
            <p class="empty-state">No flagged attempts.</p>
        </c:when>
        <c:otherwise>
            <table class="data-table">
                <thead>
                <tr>
                    <th>User</th>
                    <th>Exam</th>
                    <th>Type</th>
                    <th>Started</th>
                    <th>Status</th>
                    <th>Score</th>
                    <th>Leaves</th>
                    <th>Suspect</th>
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
                                    <span class="badge badge-diagnostic">Diagnostic</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge badge-practice">Practice</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>${ep:fmt(a.startedAt)}</td>
                        <td><span class="badge badge-${a.status}">${a.status}</span></td>
                        <td>${a.scorePercent != null ? a.scorePercent : '—'}%</td>
                        <td>${a.leaveCount}</td>
                        <td><span class="badge badge-suspect">${a.suspectLeaveCount}</span></td>
                        <td>
                            <a href="${ctx}/admin/integrity?attemptId=${ep:enc(a.id)}" class="btn btn-sm">Timeline</a>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
