<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Questions" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Questions</h1>
<c:if test="${not empty error}">
    <div class="alert alert-error">${error}</div>
</c:if>
<c:if test="${not empty importSuccess}">
    <div class="alert alert-success">${importSuccess}</div>
</c:if>
<c:if test="${not empty importErrors}">
    <div class="alert alert-error">
        <p>Import row errors:</p>
        <ul>
            <c:forEach var="err" items="${importErrors}">
                <li>${err}</li>
            </c:forEach>
        </ul>
    </div>
</c:if>

<div class="filter-bar">
    <form method="get" action="${ctx}/admin/questions">
        <label>Filter by subject:</label>
        <select name="subjectId" onchange="this.form.submit()">
            <option value="">All subjects</option>
            <c:forEach var="s" items="${subjects}">
                <option value="${s.id}" ${filterSubjectId == s.id ? 'selected' : ''}>${s.name}</option>
            </c:forEach>
        </select>
    </form>
</div>

<div class="card" style="margin-bottom: 1.5rem;">
    <h2>Import from Excel</h2>
    <p>Upload an <code>.xlsx</code> file with columns:
        subject, prompt, option_a–d, correct_option, difficulty (optional), explanation.</p>
    <form method="post" action="${ctx}/admin/questions" enctype="multipart/form-data" class="form">
        <input type="hidden" name="action" value="import">
        <div class="form-group">
            <label for="file">Excel file</label>
            <input type="file" id="file" name="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" required>
        </div>
        <button type="submit" class="btn btn-primary">Import</button>
    </form>
</div>

<div class="grid-2">
    <div class="card">
        <h2><c:choose><c:when test="${not empty editQuestion}">Edit Question</c:when><c:otherwise>Add Question</c:otherwise></c:choose></h2>
        <form method="post" action="${ctx}/admin/questions" class="form">
            <input type="hidden" name="action" value="${not empty editQuestion ? 'update' : 'create'}">
            <c:if test="${not empty editQuestion}">
                <input type="hidden" name="id" value="${editQuestion.id}">
            </c:if>
            <div class="form-group">
                <label for="subjectId">Subject</label>
                <select id="subjectId" name="subjectId" required>
                    <c:forEach var="s" items="${subjects}">
                        <option value="${s.id}" ${(not empty editQuestion && editQuestion.subjectId == s.id) ? 'selected' : ''}>${s.name}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-group">
                <label for="prompt">Question</label>
                <textarea id="prompt" name="prompt" rows="3" required>${editQuestion.prompt}</textarea>
            </div>
            <div class="form-group">
                <label for="optionA">Option A</label>
                <input type="text" id="optionA" name="optionA" value="${editQuestion.optionA}" required>
            </div>
            <div class="form-group">
                <label for="optionB">Option B</label>
                <input type="text" id="optionB" name="optionB" value="${editQuestion.optionB}" required>
            </div>
            <div class="form-group">
                <label for="optionC">Option C</label>
                <input type="text" id="optionC" name="optionC" value="${editQuestion.optionC}" required>
            </div>
            <div class="form-group">
                <label for="optionD">Option D</label>
                <input type="text" id="optionD" name="optionD" value="${editQuestion.optionD}" required>
            </div>
            <div class="form-group">
                <label for="correctOption">Correct Option</label>
                <select id="correctOption" name="correctOption" required>
                    <option value="A" ${editQuestion.correctOption == 'A' ? 'selected' : ''}>A</option>
                    <option value="B" ${editQuestion.correctOption == 'B' ? 'selected' : ''}>B</option>
                    <option value="C" ${editQuestion.correctOption == 'C' ? 'selected' : ''}>C</option>
                    <option value="D" ${editQuestion.correctOption == 'D' ? 'selected' : ''}>D</option>
                </select>
            </div>
            <div class="form-group">
                <label for="difficulty">Difficulty</label>
                <select id="difficulty" name="difficulty">
                    <option value="EASY" ${editQuestion.difficulty == 'EASY' ? 'selected' : ''}>Easy</option>
                    <option value="MEDIUM" ${empty editQuestion || editQuestion.difficulty == 'MEDIUM' ? 'selected' : ''}>Medium</option>
                    <option value="HARD" ${editQuestion.difficulty == 'HARD' ? 'selected' : ''}>Hard</option>
                </select>
            </div>
            <div class="form-group">
                <label for="explanation">Explanation</label>
                <textarea id="explanation" name="explanation" rows="3">${editQuestion.explanation}</textarea>
            </div>
            <button type="submit" class="btn btn-primary">
                <c:choose><c:when test="${not empty editQuestion}">Update</c:when><c:otherwise>Create</c:otherwise></c:choose>
            </button>
            <c:if test="${not empty editQuestion}">
                <a href="${ctx}/admin/questions" class="btn btn-outline">Cancel</a>
            </c:if>
        </form>
    </div>

    <div class="card">
        <h2>Question Bank</h2>
        <table class="data-table">
            <thead>
            <tr><th>Subject</th><th>Question</th><th>Correct</th><th>Actions</th></tr>
            </thead>
            <tbody>
            <c:forEach var="q" items="${questions}">
                <tr>
                    <td>${q.subjectName}</td>
                    <td>${q.prompt}</td>
                    <td>${q.correctOption}</td>
                    <td class="actions">
                        <a href="${ctx}/admin/questions?edit=${ep:enc(q.id)}" class="btn btn-sm">Edit</a>
                        <form method="post" action="${ctx}/admin/questions" class="inline-form" onsubmit="return confirm('Delete this question?');">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="${q.id}">
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
