<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Access grants" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Access grants</h1>
<p class="subtitle">One-time purchase tokens from the funnel. Revoke unused or redeemed grants when needed.</p>

<c:if test="${not empty error}">
    <div class="alert alert-error">${error}</div>
</c:if>

<div class="card">
    <c:choose>
        <c:when test="${empty grants}">
            <p class="empty-state">No access grants yet.</p>
        </c:when>
        <c:otherwise>
            <table class="data-table">
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Status</th>
                    <th>Exam level</th>
                    <th>Expires</th>
                    <th>User</th>
                    <th>Plan</th>
                    <th>Source</th>
                    <th>Created</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="g" items="${grants}">
                    <tr>
                        <td>${g.id}</td>
                        <td><span class="badge badge-${g.status}">${g.status}</span></td>
                        <td>
                            <c:choose>
                                <c:when test="${g.examLevel != null}">${g.examLevel.displayName()}</c:when>
                                <c:otherwise>—</c:otherwise>
                            </c:choose>
                        </td>
                        <td>${ep:fmt(g.expiresAt)}</td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty g.username}">${g.username}</c:when>
                                <c:otherwise>—</c:otherwise>
                            </c:choose>
                        </td>
                        <td>${empty g.planCode ? '—' : g.planCode}</td>
                        <td>${empty g.sourceRef ? '—' : g.sourceRef}</td>
                        <td>${ep:fmt(g.createdAt)}</td>
                        <td class="actions">
                            <c:if test="${g.status == 'UNUSED' || g.status == 'REDEEMED'}">
                                <form method="post" action="${ctx}/admin/access-grants" class="inline-form"
                                      onsubmit="return confirm('Revoke this access grant?');">
                                    <input type="hidden" name="action" value="revoke">
                                    <input type="hidden" name="id" value="${g.id}">
                                    <button type="submit" class="btn btn-sm btn-danger">Revoke</button>
                                </form>
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
