<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Subscription expired" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="auth-card expired-panel">
    <h1>Subscription expired</h1>
    <p class="subtitle">Your access to quizzes and learning materials has ended.</p>
    <c:if test="${not empty expiresAtLabel}">
        <div class="alert alert-warning">Access ended on <strong>${expiresAtLabel}</strong>.</div>
    </c:if>
    <c:if test="${empty expiresAtLabel}">
        <div class="alert alert-warning">No active subscription is linked to this account.</div>
    </c:if>
    <p>Renew from the purchase site to receive a new access token, then contact support if you need it linked to this account.</p>
    <div class="action-links" style="margin-top: 1.5rem; justify-content: center;">
        <a class="btn btn-outline" href="${ctx}/logout">Log out</a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
