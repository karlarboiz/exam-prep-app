<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.reset.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="auth-card">
    <h1><fmt:message key="reset.heading"/></h1>
    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
    <c:choose>
        <c:when test="${resetReady}">
            <form method="post" action="${ctx}/reset-password" class="form">
                <ep:csrf/>
                <div class="form-group">
                    <label for="newPassword"><fmt:message key="reset.newPassword"/></label>
                    <input type="password" id="newPassword" name="newPassword" required minlength="6"
                           autocomplete="new-password">
                </div>
                <div class="form-group">
                    <label for="confirmPassword"><fmt:message key="reset.confirmPassword"/></label>
                    <input type="password" id="confirmPassword" name="confirmPassword" required minlength="6"
                           autocomplete="new-password">
                </div>
                <button type="submit" class="btn btn-primary"><fmt:message key="reset.submit"/></button>
            </form>
        </c:when>
        <c:otherwise>
            <p class="auth-link"><a href="${ctx}/forgot-password"><fmt:message key="reset.requestNew"/></a></p>
        </c:otherwise>
    </c:choose>
    <p class="auth-link"><a href="${ctx}/login"><fmt:message key="forgot.backLogin"/></a></p>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
