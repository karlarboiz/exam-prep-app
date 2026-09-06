<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.adminDashboard.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1><fmt:message key="admin.dashboard.heading"/></h1>
<p class="subtitle"><fmt:message key="admin.dashboard.subtitle"/></p>

<div class="stats-grid">
    <div class="stat-card">
        <span class="stat-value">${subjects.size()}</span>
        <span class="stat-label"><fmt:message key="admin.dashboard.subjects"/></span>
    </div>
    <div class="stat-card">
        <span class="stat-value">${questions.size()}</span>
        <span class="stat-label"><fmt:message key="admin.dashboard.questions"/></span>
    </div>
    <div class="stat-card">
        <span class="stat-value">${exams.size()}</span>
        <span class="stat-label"><fmt:message key="admin.dashboard.exams"/></span>
    </div>
    <div class="stat-card">
        <span class="stat-value">${users.size()}</span>
        <span class="stat-label"><fmt:message key="admin.dashboard.users"/></span>
    </div>
</div>

<div class="card">
    <h2><fmt:message key="admin.dashboard.quick"/></h2>
    <div class="action-links">
        <a href="${ctx}/admin/subjects" class="btn btn-primary"><fmt:message key="admin.dashboard.manageSubjects"/></a>
        <a href="${ctx}/admin/questions" class="btn btn-primary"><fmt:message key="admin.dashboard.manageQuestions"/></a>
        <a href="${ctx}/admin/exams" class="btn btn-primary"><fmt:message key="admin.dashboard.manageExams"/></a>
        <a href="${ctx}/admin/users" class="btn btn-outline"><fmt:message key="admin.dashboard.viewUsers"/></a>
        <a href="${ctx}/admin/integrity" class="btn btn-outline"><fmt:message key="admin.dashboard.integrity"/></a>
        <a href="${ctx}/admin/n8n" class="btn btn-outline"><fmt:message key="admin.dashboard.n8n"/></a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
