<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Diagnostic Unavailable" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="card">
    <h1>Diagnostic Unavailable</h1>
    <c:if test="${not empty error}">
        <div class="alert alert-error">${error}</div>
    </c:if>
    <p>Please contact an administrator to configure an active diagnostic exam and ensure the question bank has subjects with questions.</p>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
