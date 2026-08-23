<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.register.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="auth-card">
    <h1><fmt:message key="register.heading"/></h1>
    <p class="subtitle"><fmt:message key="register.subtitle"/></p>
    <c:if test="${not empty error}">
        <div class="alert alert-error">${error}</div>
    </c:if>
    <c:choose>
        <c:when test="${empty accessToken}">
            <p class="empty-state"><fmt:message key="register.empty"/>
                <a href="${ctx}/login"><fmt:message key="register.returnLogin"/></a></p>
        </c:when>
        <c:otherwise>
            <form method="post" action="${ctx}/register" class="form">
                <ep:csrf/>
                <input type="hidden" name="token" value="${accessToken}">
                <div class="form-group">
                    <label for="tokenDisplay"><fmt:message key="register.token"/></label>
                    <input type="text" id="tokenDisplay" value="${accessToken}" readonly class="token-readonly">
                </div>
                <div class="form-group">
                    <label for="username"><fmt:message key="register.username"/></label>
                    <input type="text" id="username" name="username" value="${username}" required autofocus>
                </div>
                <div class="form-group">
                    <label for="email"><fmt:message key="register.email"/></label>
                    <input type="email" id="email" name="email" value="${email}" required>
                </div>
                <div class="form-group">
                    <label for="examLevelDisplay"><fmt:message key="register.examLevel"/></label>
                    <input type="text" id="examLevelDisplay" readonly class="token-readonly"
                           value="<c:choose><c:when test="${not empty examLevel}"><fmt:message key="examLevel.${examLevel}"/></c:when><c:otherwise><fmt:message key="examLevel.none"/></c:otherwise></c:choose>">
                    <p class="exam-meta"><fmt:message key="register.examLevelHint"/></p>
                </div>
                <div class="form-group">
                    <label for="password"><fmt:message key="register.password"/></label>
                    <input type="password" id="password" name="password" required minlength="6">
                </div>
                <div class="form-group">
                    <label for="confirmPassword"><fmt:message key="register.confirmPassword"/></label>
                    <input type="password" id="confirmPassword" name="confirmPassword" required minlength="6">
                </div>
                <button type="submit" class="btn btn-primary"><fmt:message key="register.submit"/></button>
            </form>
        </c:otherwise>
    </c:choose>
    <p class="auth-link"><fmt:message key="register.haveAccount"/> <a href="${ctx}/login"><fmt:message key="register.login"/></a></p>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
