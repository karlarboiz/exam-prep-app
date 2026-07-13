<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Register" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="auth-card">
    <h1>Register</h1>
    <p class="subtitle">Create your account using your subscription access token.</p>
    <c:if test="${not empty error}">
        <div class="alert alert-error">${error}</div>
    </c:if>
    <c:choose>
        <c:when test="${empty accessToken}">
            <p class="empty-state">You need a valid access token from your purchase to register.
                <a href="${ctx}/login">Return to login</a></p>
        </c:when>
        <c:otherwise>
            <form method="post" action="${ctx}/register" class="form">
                <input type="hidden" name="token" value="${accessToken}">
                <div class="form-group">
                    <label for="tokenDisplay">Access token</label>
                    <input type="text" id="tokenDisplay" value="${accessToken}" readonly class="token-readonly">
                </div>
                <div class="form-group">
                    <label for="username">Username</label>
                    <input type="text" id="username" name="username" value="${username}" required autofocus>
                </div>
                <div class="form-group">
                    <label for="email">Email</label>
                    <input type="email" id="email" name="email" value="${email}" required>
                </div>
                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" required minlength="6">
                </div>
                <div class="form-group">
                    <label for="confirmPassword">Confirm Password</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" required minlength="6">
                </div>
                <button type="submit" class="btn btn-primary">Create account</button>
            </form>
        </c:otherwise>
    </c:choose>
    <p class="auth-link">Already have an account? <a href="${ctx}/login">Login</a></p>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
