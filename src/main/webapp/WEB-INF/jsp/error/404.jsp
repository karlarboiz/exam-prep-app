<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.notFound.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="error-page">
    <h1><fmt:message key="error.404.heading"/></h1>
    <p><fmt:message key="error.404.body"/></p>
    <a href="${ctx}/" class="btn btn-primary"><fmt:message key="error.home"/></a>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
