<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Exams" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Exams</h1>
<c:if test="${not empty error}">
    <div class="alert alert-error">${error}</div>
</c:if>

<div class="grid-2">
    <div class="card">
        <h2><c:choose><c:when test="${not empty editExam}">Edit Exam</c:when><c:otherwise>Create Exam</c:otherwise></c:choose></h2>
        <form method="post" action="${ctx}/admin/exams" class="form">
            <input type="hidden" name="action" value="${not empty editExam ? 'update' : 'create'}">
            <c:if test="${not empty editExam}">
                <input type="hidden" name="id" value="${editExam.id}">
            </c:if>
            <div class="form-group">
                <label for="subjectId">Subject</label>
                <select id="subjectId" name="subjectId" required>
                    <c:forEach var="s" items="${subjects}">
                        <option value="${s.id}" ${(not empty editExam && editExam.subjectId == s.id) ? 'selected' : ''}>${s.name}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-group">
                <label for="title">Title</label>
                <input type="text" id="title" name="title" value="${editExam.title}" required>
            </div>
            <div class="form-group">
                <label for="durationMinutes">Duration (minutes)</label>
                <input type="number" id="durationMinutes" name="durationMinutes" value="${empty editExam ? 30 : editExam.durationMinutes}" min="1" required>
            </div>
            <div class="form-group checkbox-group">
                <label>
                    <input type="checkbox" name="active" ${empty editExam || editExam.active ? 'checked' : ''}>
                    Active
                </label>
            </div>
            <div class="form-group">
                <label>Select Questions</label>
                <div class="checkbox-list">
                    <c:forEach var="q" items="${questions}">
                        <label class="checkbox-item">
                            <input type="checkbox" name="questionIds" value="${q.id}"
                                <c:forEach var="selId" items="${selectedQuestionIds}">
                                    <c:if test="${selId == q.id}">checked</c:if>
                                </c:forEach>>
                            [${q.subjectName}] ${q.prompt}
                        </label>
                    </c:forEach>
                </div>
            </div>
            <button type="submit" class="btn btn-primary">
                <c:choose><c:when test="${not empty editExam}">Update</c:when><c:otherwise>Create</c:otherwise></c:choose>
            </button>
            <c:if test="${not empty editExam}">
                <a href="${ctx}/admin/exams" class="btn btn-outline">Cancel</a>
            </c:if>
        </form>
    </div>

    <div class="card">
        <h2>All Exams</h2>
        <table class="data-table">
            <thead>
            <tr><th>Title</th><th>Subject</th><th>Duration</th><th>Questions</th><th>Status</th><th>Actions</th></tr>
            </thead>
            <tbody>
            <c:forEach var="exam" items="${exams}">
                <tr>
                    <td>${exam.title}</td>
                    <td>${exam.subjectName}</td>
                    <td>${exam.durationMinutes} min</td>
                    <td>${exam.questionCount}</td>
                    <td><span class="badge ${exam.active ? 'badge-success' : 'badge-muted'}">${exam.active ? 'Active' : 'Inactive'}</span></td>
                    <td class="actions">
                        <a href="${ctx}/admin/exams?edit=${exam.id}" class="btn btn-sm">Edit</a>
                        <form method="post" action="${ctx}/admin/exams" class="inline-form" onsubmit="return confirm('Delete this exam?');">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="${exam.id}">
                            <button type="submit" class="btn btn-sm btn-danger">Delete</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
