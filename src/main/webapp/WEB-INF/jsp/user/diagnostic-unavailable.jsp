<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.diagnosticUnavailable.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="card">
    <h1><fmt:message key="diagnosticUnavailable.heading"/></h1>
    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
    <p><fmt:message key="diagnosticUnavailable.body"/></p>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
