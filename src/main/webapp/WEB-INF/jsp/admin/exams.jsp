<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
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
        <form method="post" action="${ctx}/admin/exams" class="form" id="examForm">
            <input type="hidden" name="action" value="${not empty editExam ? 'update' : 'create'}">
            <c:if test="${not empty editExam}">
                <input type="hidden" name="id" value="${editExam.id}">
            </c:if>
            <div class="form-group">
                <label for="subjectId">Subject <span class="exam-meta">(anchor for diagnostic; sampling uses all subjects)</span></label>
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
            <div class="form-group checkbox-group">
                <label>
                    <input type="checkbox" name="diagnostic" id="diagnostic"
                           ${not empty editExam && editExam.diagnostic ? 'checked' : ''}
                           onchange="toggleDiagnosticFields()">
                    Diagnostic (placement test — samples N questions per subject)
                </label>
            </div>
            <div class="form-group" id="questionsPerSubjectGroup">
                <label for="questionsPerSubject">Questions per subject</label>
                <input type="number" id="questionsPerSubject" name="questionsPerSubject"
                       value="${not empty editExam && editExam.questionsPerSubject != null ? editExam.questionsPerSubject : 5}"
                       min="1">
            </div>
            <div class="form-group" id="questionPickerGroup">
                <label>Select and order questions</label>
                <p class="exam-meta">Checked questions are included. Use Up/Down to set the order students see.</p>
                <div class="checkbox-list exam-question-list" id="examQuestionList">
                    <c:forEach var="q" items="${questions}">
                        <div class="exam-question-row">
                            <label class="checkbox-item">
                                <input type="checkbox" class="exam-q-check" name="questionIds" value="${q.id}"
                                    <c:forEach var="selId" items="${selectedQuestionIds}">
                                        <c:if test="${selId == q.id}">checked</c:if>
                                    </c:forEach>
                                    onchange="onExamQuestionCheck(this)">
                                [${q.subjectName}] ${q.prompt}
                            </label>
                            <div class="exam-q-order-actions">
                                <button type="button" class="btn btn-sm btn-outline" onclick="moveExamQuestion(this, -1)">Up</button>
                                <button type="button" class="btn btn-sm btn-outline" onclick="moveExamQuestion(this, 1)">Down</button>
                            </div>
                        </div>
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
            <tr><th>Title</th><th>Subject</th><th>Type</th><th>Duration</th><th>Questions</th><th>Status</th><th>Actions</th></tr>
            </thead>
            <tbody>
            <c:forEach var="exam" items="${exams}">
                <tr>
                    <td>${exam.title}</td>
                    <td>${exam.subjectName}</td>
                    <td>
                        <c:choose>
                            <c:when test="${exam.diagnostic}">Diagnostic (${exam.questionsPerSubject}/subject)</c:when>
                            <c:otherwise>Practice</c:otherwise>
                        </c:choose>
                    </td>
                    <td>${exam.durationMinutes} min</td>
                    <td>${exam.diagnostic ? '—' : exam.questionCount}</td>
                    <td><span class="badge ${exam.active ? 'badge-success' : 'badge-muted'}">${exam.active ? 'Active' : 'Inactive'}</span></td>
                    <td class="actions">
                        <a href="${ctx}/admin/exams?edit=${ep:enc(exam.id)}" class="btn btn-sm">Edit</a>
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

<script>
    function toggleDiagnosticFields() {
        var isDiagnostic = document.getElementById('diagnostic').checked;
        document.getElementById('questionsPerSubjectGroup').style.display = isDiagnostic ? '' : 'none';
        document.getElementById('questionPickerGroup').style.display = isDiagnostic ? 'none' : '';
    }
    function moveExamQuestion(btn, dir) {
        var row = btn.closest('.exam-question-row');
        if (!row) return;
        var sibling = dir < 0 ? row.previousElementSibling : row.nextElementSibling;
        if (!sibling) return;
        if (dir < 0) {
            row.parentNode.insertBefore(row, sibling);
        } else {
            row.parentNode.insertBefore(sibling, row);
        }
    }
    function onExamQuestionCheck(cb) {
        var row = cb.closest('.exam-question-row');
        var list = row && row.parentNode;
        if (!row || !list) return;
        if (cb.checked) {
            var lastChecked = null;
            list.querySelectorAll('.exam-question-row').forEach(function (r) {
                var check = r.querySelector('.exam-q-check');
                if (check && check.checked && r !== row) {
                    lastChecked = r;
                }
            });
            if (lastChecked) {
                lastChecked.after(row);
            } else {
                list.insertBefore(row, list.firstChild);
            }
        } else {
            list.appendChild(row);
        }
    }
    toggleDiagnosticFields();
</script>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
