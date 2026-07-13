<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Admin Dashboard" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Admin Dashboard</h1>
<p class="subtitle">Manage exam content and monitor users.</p>

<div class="stats-grid">
    <div class="stat-card">
        <span class="stat-value">${subjects.size()}</span>
        <span class="stat-label">Subjects</span>
    </div>
    <div class="stat-card">
        <span class="stat-value">${questions.size()}</span>
        <span class="stat-label">Questions</span>
    </div>
    <div class="stat-card">
        <span class="stat-value">${exams.size()}</span>
        <span class="stat-label">Exams</span>
    </div>
    <div class="stat-card">
        <span class="stat-value">${users.size()}</span>
        <span class="stat-label">Users</span>
    </div>
</div>

<div class="card">
    <h2>Quick Actions</h2>
    <div class="action-links">
        <a href="${ctx}/admin/subjects" class="btn btn-primary">Manage Subjects</a>
        <a href="${ctx}/admin/questions" class="btn btn-primary">Manage Questions</a>
        <a href="${ctx}/admin/exams" class="btn btn-primary">Manage Exams</a>
        <a href="${ctx}/admin/users" class="btn btn-outline">View Users</a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
