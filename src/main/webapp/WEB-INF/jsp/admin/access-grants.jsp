<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.accessGrants.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1><fmt:message key="grants.heading"/></h1>
<p class="subtitle"><fmt:message key="grants.subtitle"/></p>

<c:if test="${not empty error}">
    <div class="alert alert-error"><c:out value="${error}"/></div>
</c:if>
<c:if test="${not empty createdRawToken}">
    <div class="alert alert-success"><fmt:message key="grants.created"/></div>
    <div class="card">
        <h2><fmt:message key="grants.newToken"/></h2>
        <div class="form-group">
            <label for="createdToken"><fmt:message key="grants.rawToken"/></label>
            <input type="text" id="createdToken" value="<c:out value='${createdRawToken}'/>" readonly class="token-readonly">
        </div>
        <div class="form-group">
            <label for="registerLink"><fmt:message key="grants.registerLink"/></label>
            <input type="text" id="registerLink" value="<c:out value='${ctx}${registerPath}'/>" readonly class="token-readonly">
        </div>
        <p class="hint"><fmt:message key="grants.registerHint"/></p>
        <p class="exam-meta">
            <fmt:message key="grants.examLevelMeta">
                <fmt:param><fmt:message key="examLevel.${createdGrant.examLevel}"/></fmt:param>
                <fmt:param value="${ep:fmt(createdGrant.expiresAt)}"/>
            </fmt:message>
        </p>
        <a href="${ctx}${registerPath}" class="btn btn-primary"><fmt:message key="grants.openRegister"/></a>
    </div>
</c:if>

<div class="card">
    <h2><fmt:message key="grants.mint"/></h2>
    <form method="post" action="${ctx}/admin/access-grants" class="form">
        <ep:csrf/>
        <input type="hidden" name="action" value="create">
        <div class="grid-2">
            <div class="form-group">
                <label for="examLevel"><fmt:message key="grants.examLevel"/></label>
                <select id="examLevel" name="examLevel" required>
                    <option value="PROFESSIONAL"
                            ${empty formExamLevel || formExamLevel == 'PROFESSIONAL' ? 'selected' : ''}>
                        <fmt:message key="examLevel.PROFESSIONAL"/>
                    </option>
                    <option value="SUB_PROFESSIONAL"
                            ${formExamLevel == 'SUB_PROFESSIONAL' ? 'selected' : ''}>
                        <fmt:message key="examLevel.SUB_PROFESSIONAL"/>
                    </option>
                </select>
            </div>
            <div class="form-group">
                <label for="durationDays"><fmt:message key="grants.duration"/></label>
                <input type="number" id="durationDays" name="durationDays" min="1" required
                       value="${empty formDurationDays ? 3 : formDurationDays}">
            </div>
            <div class="form-group">
                <label for="planCode"><fmt:message key="grants.planCode"/></label>
                <input type="text" id="planCode" name="planCode" value="${formPlanCode}" placeholder="<fmt:message key="grants.planPlaceholder"/>">
            </div>
            <div class="form-group">
                <label for="sourceRef"><fmt:message key="grants.source"/></label>
                <input type="text" id="sourceRef" name="sourceRef" value="${formSourceRef}"
                       placeholder="<fmt:message key="grants.sourcePlaceholder"/>">
            </div>
        </div>
        <p class="exam-meta"><fmt:message key="grants.mintHint"/></p>
        <button type="submit" class="btn btn-primary"><fmt:message key="grants.createToken"/></button>
    </form>
</div>

<div class="card">
    <h2><fmt:message key="grants.all"/></h2>
    <fmt:message key="confirm.revokeGrant" var="confirmRevokeGrant"/>
    <c:choose>
        <c:when test="${empty grants}">
            <p class="empty-state"><fmt:message key="grants.empty"/></p>
        </c:when>
        <c:otherwise>
            <table class="data-table">
                <thead>
                <tr>
                    <th><fmt:message key="grants.id"/></th>
                    <th><fmt:message key="grants.status"/></th>
                    <th><fmt:message key="grants.examLevel"/></th>
                    <th><fmt:message key="grants.expires"/></th>
                    <th><fmt:message key="grants.user"/></th>
                    <th><fmt:message key="grants.plan"/></th>
                    <th><fmt:message key="grants.source"/></th>
                    <th><fmt:message key="grants.createdAt"/></th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="g" items="${grants}">
                    <tr>
                        <td>${g.id}</td>
                        <td><span class="badge badge-${g.status}"><fmt:message key="grantStatus.${g.status}"/></span></td>
                        <td>
                            <c:choose>
                                <c:when test="${g.examLevel != null}"><fmt:message key="examLevel.${g.examLevel}"/></c:when>
                                <c:otherwise><fmt:message key="common.dash"/></c:otherwise>
                            </c:choose>
                        </td>
                        <td>${ep:fmt(g.expiresAt)}</td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty g.username}"><c:out value="${g.username}"/></c:when>
                                <c:otherwise><fmt:message key="common.dash"/></c:otherwise>
                            </c:choose>
                        </td>
                        <td>${empty g.planCode ? '—' : g.planCode}</td>
                        <td>${empty g.sourceRef ? '—' : g.sourceRef}</td>
                        <td>${ep:fmt(g.createdAt)}</td>
                        <td class="actions">
                            <c:if test="${g.status == 'UNUSED' || g.status == 'REDEEMED'}">
                                <form method="post" action="${ctx}/admin/access-grants" class="inline-form"
                                      data-confirm="${confirmRevokeGrant}"
                                      onsubmit="return confirm(this.dataset.confirm);">
                                    <ep:csrf/>
                                    <input type="hidden" name="action" value="revoke">
                                    <input type="hidden" name="id" value="${g.id}">
                                    <button type="submit" class="btn btn-sm btn-danger"><fmt:message key="action.revoke"/></button>
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
