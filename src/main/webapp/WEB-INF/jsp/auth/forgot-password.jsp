<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.forgot.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="auth-card">
    <h1><fmt:message key="forgot.heading"/></h1>
    <p class="subtitle"><fmt:message key="forgot.subtitle"/></p>
    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
    <c:if test="${not empty success}">
        <div class="alert alert-success"><c:out value="${success}"/></div>
    </c:if>
    <form method="post" action="${ctx}/forgot-password" class="form">
        <ep:csrf/>
        <div class="form-group">
            <label for="email"><fmt:message key="forgot.email"/></label>
            <input type="email" id="email" name="email" required autofocus>
        </div>
        <button type="submit" class="btn btn-primary"><fmt:message key="forgot.submit"/></button>
    </form>
    <p class="auth-link"><a href="${ctx}/login"><fmt:message key="forgot.backLogin"/></a></p>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
