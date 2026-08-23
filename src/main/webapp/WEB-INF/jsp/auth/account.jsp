<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.account.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1><fmt:message key="account.heading"/></h1>
<p class="subtitle"><fmt:message key="account.subtitle"/></p>

<c:if test="${not empty error}">
    <div class="alert alert-error"><c:out value="${error}"/></div>
</c:if>
<c:if test="${not empty success}">
    <div class="alert alert-success"><c:out value="${success}"/></div>
</c:if>

<div class="card">
    <h2><fmt:message key="account.profile"/></h2>
    <div class="grid-2">
        <div class="form-group">
            <label for="username"><fmt:message key="account.username"/></label>
            <input type="text" id="username" value="<c:out value='${currentUser.username}'/>" readonly class="token-readonly">
        </div>
        <div class="form-group">
            <label for="email"><fmt:message key="account.email"/></label>
            <input type="text" id="email" value="<c:out value='${currentUser.email}'/>" readonly class="token-readonly">
        </div>
        <div class="form-group">
            <label for="role"><fmt:message key="account.role"/></label>
            <input type="text" id="role" readonly class="token-readonly"
                   value="<fmt:message key="role.${currentUser.role}"/>">
        </div>
        <div class="form-group">
            <label for="examLevel"><fmt:message key="account.examLevel"/></label>
            <input type="text" id="examLevel" readonly class="token-readonly"
                   value="<c:choose><c:when test="${not empty currentUser.examLevel}"><fmt:message key="examLevel.${currentUser.examLevel}"/></c:when><c:otherwise><fmt:message key="examLevel.none"/></c:otherwise></c:choose>">
        </div>
    </div>
</div>

<div class="card">
    <h2><fmt:message key="account.changePassword"/></h2>
    <form method="post" action="${ctx}/account" class="form">
        <ep:csrf/>
        <div class="form-group">
            <label for="currentPassword"><fmt:message key="account.currentPassword"/></label>
            <input type="password" id="currentPassword" name="currentPassword" required autocomplete="current-password">
        </div>
        <div class="form-group">
            <label for="newPassword"><fmt:message key="account.newPassword"/></label>
            <input type="password" id="newPassword" name="newPassword" required minlength="6" autocomplete="new-password">
        </div>
        <div class="form-group">
            <label for="confirmPassword"><fmt:message key="account.confirmPassword"/></label>
            <input type="password" id="confirmPassword" name="confirmPassword" required minlength="6" autocomplete="new-password">
        </div>
        <button type="submit" class="btn btn-primary"><fmt:message key="account.updatePassword"/></button>
    </form>
    <p class="hint"><fmt:message key="account.passwordHint"/></p>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
