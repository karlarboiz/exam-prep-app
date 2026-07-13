<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Forbidden" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="error-page">
    <h1>403 — Forbidden</h1>
    <p>You do not have permission to access this page.</p>
    <a href="${ctx}/" class="btn btn-primary">Go Home</a>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
