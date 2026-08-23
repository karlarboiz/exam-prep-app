<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.exams.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1><fmt:message key="exams.heading"/></h1>
<c:if test="${not empty error}">
    <div class="alert alert-error"><c:out value="${error}"/></div>
</c:if>

<div class="grid-2">
    <div class="card">
        <h2><c:choose><c:when test="${not empty editExam}"><fmt:message key="exams.edit"/></c:when><c:otherwise><fmt:message key="exams.create"/></c:otherwise></c:choose></h2>
        <form method="post" action="${ctx}/admin/exams" class="form" id="examForm">
            <ep:csrf/>
            <input type="hidden" name="action" value="${not empty editExam ? 'update' : 'create'}">
            <c:if test="${not empty editExam}">
                <input type="hidden" name="id" value="${editExam.id}">
            </c:if>
            <div class="form-group">
                <label for="subjectId"><fmt:message key="exams.subject"/> <span class="exam-meta"><fmt:message key="exams.subjectHint"/></span></label>
                <select id="subjectId" name="subjectId" required>
                    <c:forEach var="s" items="${subjects}">
                        <option value="${s.id}" ${(not empty editExam && editExam.subjectId == s.id) ? 'selected' : ''}><c:out value="${s.name}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-group">
                <label for="title"><fmt:message key="exams.titleLabel"/></label>
                <input type="text" id="title" name="title" value="<c:out value='${editExam.title}'/>" required>
            </div>
            <div class="form-group">
                <label for="durationMinutes"><fmt:message key="exams.duration"/></label>
                <input type="number" id="durationMinutes" name="durationMinutes" value="${empty editExam ? 30 : editExam.durationMinutes}" min="1" required>
            </div>
            <div class="form-group checkbox-group">
                <label>
                    <input type="checkbox" name="active" ${empty editExam || editExam.active ? 'checked' : ''}>
                    <fmt:message key="exams.active"/>
                </label>
            </div>
            <div class="form-group checkbox-group">
                <label>
                    <input type="checkbox" name="diagnostic" id="diagnostic"
                           ${not empty editExam && editExam.diagnostic ? 'checked' : ''}
                           onchange="toggleDiagnosticFields()">
                    <fmt:message key="exams.diagnostic"/>
                </label>
            </div>
            <div class="form-group" id="questionsPerSubjectGroup">
                <label for="questionsPerSubject"><fmt:message key="exams.perSubject"/></label>
                <input type="number" id="questionsPerSubject" name="questionsPerSubject"
                       value="${not empty editExam && editExam.questionsPerSubject != null ? editExam.questionsPerSubject : 5}"
                       min="1">
            </div>
            <div class="form-group" id="questionPickerGroup">
                <label><fmt:message key="exams.selectOrder"/></label>
                <p class="exam-meta"><fmt:message key="exams.selectHint"/></p>
                <div class="checkbox-list exam-question-list" id="examQuestionList">
                    <c:forEach var="q" items="${questions}">
                        <div class="exam-question-row">
                            <label class="checkbox-item">
                                <input type="checkbox" class="exam-q-check" name="questionIds" value="${q.id}"
                                    <c:forEach var="selId" items="${selectedQuestionIds}">
                                        <c:if test="${selId == q.id}">checked</c:if>
                                    </c:forEach>
                                    onchange="onExamQuestionCheck(this)">
                                [<c:out value="${q.subjectName}"/>] <c:out value="${q.prompt}"/>
                            </label>
                            <div class="exam-q-order-actions">
                                <button type="button" class="btn btn-sm btn-outline" onclick="moveExamQuestion(this, -1)"><fmt:message key="exams.up"/></button>
                                <button type="button" class="btn btn-sm btn-outline" onclick="moveExamQuestion(this, 1)"><fmt:message key="exams.down"/></button>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </div>
            <button type="submit" class="btn btn-primary">
                <c:choose><c:when test="${not empty editExam}"><fmt:message key="action.update"/></c:when><c:otherwise><fmt:message key="action.create"/></c:otherwise></c:choose>
            </button>
            <c:if test="${not empty editExam}">
                <a href="${ctx}/admin/exams" class="btn btn-outline"><fmt:message key="action.cancel"/></a>
            </c:if>
        </form>
    </div>

    <div class="card">
        <h2><fmt:message key="exams.all"/></h2>
        <fmt:message key="confirm.deleteExam" var="confirmDeleteExam"/>
        <table class="data-table">
            <thead>
            <tr>
                <th><fmt:message key="exams.colTitle"/></th>
                <th><fmt:message key="exams.colSubject"/></th>
                <th><fmt:message key="exams.colType"/></th>
                <th><fmt:message key="exams.colDuration"/></th>
                <th><fmt:message key="exams.colQuestions"/></th>
                <th><fmt:message key="exams.colStatus"/></th>
                <th><fmt:message key="exams.colActions"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="exam" items="${exams}">
                <tr>
                    <td><c:out value="${exam.title}"/></td>
                    <td>${exam.subjectName}</td>
                    <td>
                        <c:choose>
                            <c:when test="${exam.diagnostic}"><fmt:message key="exams.typeDiagnostic"><fmt:param value="${exam.questionsPerSubject}"/></fmt:message></c:when>
                            <c:otherwise><fmt:message key="exams.typePractice"/></c:otherwise>
                        </c:choose>
                    </td>
                    <td><fmt:message key="common.min"><fmt:param value="${exam.durationMinutes}"/></fmt:message></td>
                    <td>${exam.diagnostic ? '—' : exam.questionCount}</td>
                    <td><span class="badge ${exam.active ? 'badge-success' : 'badge-muted'}"><c:choose><c:when test="${exam.active}"><fmt:message key="exams.active"/></c:when><c:otherwise><fmt:message key="exams.inactive"/></c:otherwise></c:choose></span></td>
                    <td class="actions">
                        <a href="${ctx}/admin/exams?edit=${ep:enc(exam.id)}" class="btn btn-sm"><fmt:message key="action.edit"/></a>
                        <form method="post" action="${ctx}/admin/exams" class="inline-form"
                              data-confirm="${confirmDeleteExam}"
                              onsubmit="return confirm(this.dataset.confirm);">
                            <ep:csrf/>
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="${exam.id}">
                            <button type="submit" class="btn btn-sm btn-danger"><fmt:message key="action.delete"/></button>
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
