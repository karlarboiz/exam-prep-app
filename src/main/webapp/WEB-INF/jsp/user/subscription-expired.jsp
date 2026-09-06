<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.expired.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="auth-card expired-panel">
    <h1><fmt:message key="expired.heading"/></h1>
    <p class="subtitle"><fmt:message key="expired.subtitle"/></p>
    <c:if test="${not empty expiresAtLabel}">
        <div class="alert alert-warning"><fmt:message key="expired.endedOn"><fmt:param value="${expiresAtLabel}"/></fmt:message></div>
    </c:if>
    <c:if test="${empty expiresAtLabel}">
        <div class="alert alert-warning"><fmt:message key="expired.none"/></div>
    </c:if>
    <p><fmt:message key="expired.body"/></p>
    <div class="action-links" style="margin-top: 1.5rem; justify-content: center;">
        <a class="btn btn-outline" href="${ctx}/logout"><fmt:message key="expired.logout"/></a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
