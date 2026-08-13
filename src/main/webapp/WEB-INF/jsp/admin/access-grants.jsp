<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Access grants" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Access grants</h1>
<p class="subtitle">Mint a one-time purchase token, or revoke unused or redeemed grants.</p>

<c:if test="${not empty error}">
    <div class="alert alert-error">${error}</div>
</c:if>
<c:if test="${not empty createdRawToken}">
    <div class="alert alert-success">Token created. Copy it now — the raw value is not stored and will not be shown again.</div>
    <div class="card">
        <h2>New access token</h2>
        <div class="form-group">
            <label for="createdToken">Raw token</label>
            <input type="text" id="createdToken" value="${createdRawToken}" readonly class="token-readonly">
        </div>
        <div class="form-group">
            <label for="registerLink">Register link</label>
            <input type="text" id="registerLink" value="${ctx}${registerPath}" readonly class="token-readonly">
        </div>
        <p class="exam-meta">
            Exam level: ${createdGrant.examLevel.displayName()}
            · Expires: ${ep:fmt(createdGrant.expiresAt)}
        </p>
        <a href="${ctx}${registerPath}" class="btn btn-primary">Open register link</a>
    </div>
</c:if>

<div class="card">
    <h2>Mint token</h2>
    <form method="post" action="${ctx}/admin/access-grants" class="form">
        <input type="hidden" name="action" value="create">
        <div class="grid-2">
            <div class="form-group">
                <label for="examLevel">Exam level</label>
                <select id="examLevel" name="examLevel" required>
                    <option value="PROFESSIONAL"
                            ${empty formExamLevel || formExamLevel == 'PROFESSIONAL' ? 'selected' : ''}>
                        Professional
                    </option>
                    <option value="SUB_PROFESSIONAL"
                            ${formExamLevel == 'SUB_PROFESSIONAL' ? 'selected' : ''}>
                        Sub-Professional
                    </option>
                </select>
            </div>
            <div class="form-group">
                <label for="durationDays">Duration (days)</label>
                <input type="number" id="durationDays" name="durationDays" min="1" required
                       value="${empty formDurationDays ? 3 : formDurationDays}">
            </div>
            <div class="form-group">
                <label for="planCode">Plan code</label>
                <input type="text" id="planCode" name="planCode" value="${formPlanCode}" placeholder="optional">
            </div>
            <div class="form-group">
                <label for="sourceRef">Source</label>
                <input type="text" id="sourceRef" name="sourceRef" value="${formSourceRef}"
                       placeholder="e.g. support ticket">
            </div>
        </div>
        <p class="exam-meta">Locks the buyer’s exam track. Raw token is shown once after create.</p>
        <button type="submit" class="btn btn-primary">Create token</button>
    </form>
</div>

<div class="card">
    <h2>All grants</h2>
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
