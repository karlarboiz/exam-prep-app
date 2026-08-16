<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Account" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Account</h1>
<p class="subtitle">View your profile and change your password.</p>

<c:if test="${not empty error}">
    <div class="alert alert-error">${error}</div>
</c:if>
<c:if test="${not empty success}">
    <div class="alert alert-success">${success}</div>
</c:if>

<div class="card">
    <h2>Profile</h2>
    <div class="grid-2">
        <div class="form-group">
            <label for="username">Username</label>
            <input type="text" id="username" value="${currentUser.username}" readonly class="token-readonly">
        </div>
        <div class="form-group">
            <label for="email">Email</label>
            <input type="text" id="email" value="${currentUser.email}" readonly class="token-readonly">
        </div>
        <div class="form-group">
            <label for="role">Role</label>
            <input type="text" id="role" value="${currentUser.role}" readonly class="token-readonly">
        </div>
        <div class="form-group">
            <label for="examLevel">Exam level</label>
            <input type="text" id="examLevel" readonly class="token-readonly"
                   value="${not empty currentUser.examLevel ? currentUser.examLevel.displayName : '—'}">
        </div>
    </div>
</div>

<div class="card">
    <h2>Change password</h2>
    <form method="post" action="${ctx}/account" class="form">
        <div class="form-group">
            <label for="currentPassword">Current password</label>
            <input type="password" id="currentPassword" name="currentPassword" required autocomplete="current-password">
        </div>
        <div class="form-group">
            <label for="newPassword">New password</label>
            <input type="password" id="newPassword" name="newPassword" required minlength="6" autocomplete="new-password">
        </div>
        <div class="form-group">
            <label for="confirmPassword">Confirm new password</label>
            <input type="password" id="confirmPassword" name="confirmPassword" required minlength="6" autocomplete="new-password">
        </div>
        <button type="submit" class="btn btn-primary">Update password</button>
    </form>
    <p class="hint">Must be at least 6 characters and different from your current password.</p>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
