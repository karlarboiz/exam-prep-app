<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.login.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="auth-card">
    <h1><fmt:message key="login.heading"/></h1>
    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
    <c:if test="${not empty success}">
        <div class="alert alert-success"><c:out value="${success}"/></div>
    </c:if>
    <form method="post" action="${ctx}/login" class="form">
        <ep:csrf/>
        <div class="form-group">
            <label for="username"><fmt:message key="login.username"/></label>
            <input type="text" id="username" name="username" required autofocus>
        </div>
        <div class="form-group">
            <label for="password"><fmt:message key="login.password"/></label>
            <input type="password" id="password" name="password" required>
        </div>
        <button type="submit" class="btn btn-primary"><fmt:message key="login.submit"/></button>
    </form>
    <p class="auth-link"><a href="${ctx}/forgot-password"><fmt:message key="login.forgot"/></a></p>
    <p class="auth-link"><fmt:message key="login.registerHint"/></p>
    <c:if test="${showAdminHint}">
        <p class="hint"><fmt:message key="login.adminHint"><fmt:param value="admin"/><fmt:param value="admin123"/></fmt:message></p>
    </c:if>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
