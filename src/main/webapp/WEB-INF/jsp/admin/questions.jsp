<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.questions.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1><fmt:message key="questions.heading"/></h1>
<c:if test="${not empty error}">
    <div class="alert alert-error">${error}</div>
</c:if>
<c:if test="${not empty importSuccess}">
    <div class="alert alert-success">${importSuccess}</div>
</c:if>
<c:if test="${not empty importErrors}">
    <div class="alert alert-error">
        <p><fmt:message key="questions.importErrors"/></p>
        <ul>
            <c:forEach var="err" items="${importErrors}">
                <li>${err}</li>
            </c:forEach>
        </ul>
    </div>
</c:if>

<div class="filter-bar">
    <form method="get" action="${ctx}/admin/questions">
        <label><fmt:message key="questions.filterSubject"/></label>
        <select name="subjectId" onchange="this.form.submit()">
            <option value=""><fmt:message key="questions.allSubjects"/></option>
            <c:forEach var="s" items="${subjects}">
                <option value="${s.id}" ${filterSubjectId == s.id ? 'selected' : ''}>${s.name}</option>
            </c:forEach>
        </select>
        <label><fmt:message key="questions.filterBatch"/></label>
        <select name="batchLabel" onchange="this.form.submit()">
            <option value=""><fmt:message key="questions.allBatches"/></option>
            <option value="__unlabeled__" ${filterBatchLabel == '__unlabeled__' ? 'selected' : ''}><fmt:message key="questions.unlabeled"/></option>
            <c:forEach var="label" items="${batchLabels}">
                <option value="${label}" ${filterBatchLabel == label ? 'selected' : ''}>${label}</option>
            </c:forEach>
        </select>
    </form>
</div>

<div class="card">
    <h2><fmt:message key="questions.importHeading"/></h2>
    <p><fmt:message key="questions.importHelp"><fmt:param value="${suggestedBatchLabel}"/></fmt:message></p>
    <div class="actions import-actions">
        <a href="${ctx}/admin/questions?action=template" class="btn btn-outline"><fmt:message key="questions.template"/></a>
        <c:url var="exportUrl" value="/admin/questions">
            <c:param name="action" value="export"/>
            <c:if test="${not empty filterSubjectId}">
                <c:param name="subjectId" value="${filterSubjectId}"/>
            </c:if>
            <c:if test="${not empty filterBatchLabel}">
                <c:param name="batchLabel" value="${filterBatchLabel}"/>
            </c:if>
        </c:url>
        <a href="${exportUrl}" class="btn btn-outline"><fmt:message key="questions.export"/></a>
    </div>
    <form method="post" action="${ctx}/admin/questions" enctype="multipart/form-data" class="form">
        <ep:csrf/>
        <input type="hidden" name="action" value="import">
        <div class="form-group">
            <label for="batchLabel"><fmt:message key="questions.batchLabel"/></label>
            <c:set var="importBatchLabel"
                   value="${not empty param.batchLabel && filterBatchLabel != '__unlabeled__' ? param.batchLabel : suggestedBatchLabel}"/>
            <input type="text" id="batchLabel" name="batchLabel" required maxlength="100"
                   list="existingBatchLabels" autocomplete="off"
                   placeholder="${suggestedBatchLabel}"
                   value="${importBatchLabel}">
            <datalist id="existingBatchLabels">
                <c:forEach var="label" items="${batchLabels}">
                    <option value="${label}"></option>
                </c:forEach>
            </datalist>
            <p class="field-hint"><fmt:message key="questions.batchHint"><fmt:param value="${suggestedBatchLabel}"/></fmt:message></p>
        </div>
        <div class="form-group">
            <label for="file"><fmt:message key="questions.file"/></label>
            <input type="file" id="file" name="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" required>
        </div>
        <button type="submit" class="btn btn-primary"><fmt:message key="action.import"/></button>
    </form>
</div>

<%-- Add Question (create) is hidden; form is shown only when editing an existing question. --%>
<c:if test="${not empty editQuestion}">
    <div class="card">
        <h2><fmt:message key="questions.edit"/></h2>
        <form method="post" action="${ctx}/admin/questions" class="form">
            <ep:csrf/>
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="id" value="${editQuestion.id}">
            <div class="form-group">
                <label for="subjectId"><fmt:message key="questions.subject"/></label>
                <select id="subjectId" name="subjectId" required>
                    <c:forEach var="s" items="${subjects}">
                        <option value="${s.id}" ${editQuestion.subjectId == s.id ? 'selected' : ''}>${s.name}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-group">
                <label><fmt:message key="questions.batch"/></label>
                <p><c:choose>
                    <c:when test="${not empty editQuestion.batchLabel}"><span class="badge badge-admin">${editQuestion.batchLabel}</span></c:when>
                    <c:otherwise><span class="badge badge-muted"><fmt:message key="questions.unlabeled"/></span></c:otherwise>
                </c:choose></p>
                <p class="field-hint"><fmt:message key="questions.batchHintEdit"/></p>
            </div>
            <div class="form-group">
                <label for="prompt"><fmt:message key="questions.prompt"/></label>
                <textarea id="prompt" name="prompt" rows="3" required>${editQuestion.prompt}</textarea>
            </div>
            <div class="form-group">
                <label for="optionA"><fmt:message key="questions.optionA"/></label>
                <input type="text" id="optionA" name="optionA" value="${editQuestion.optionA}" required>
            </div>
            <div class="form-group">
                <label for="optionB"><fmt:message key="questions.optionB"/></label>
                <input type="text" id="optionB" name="optionB" value="${editQuestion.optionB}" required>
            </div>
            <div class="form-group">
                <label for="optionC"><fmt:message key="questions.optionC"/></label>
                <input type="text" id="optionC" name="optionC" value="${editQuestion.optionC}" required>
            </div>
            <div class="form-group">
                <label for="optionD"><fmt:message key="questions.optionD"/></label>
                <input type="text" id="optionD" name="optionD" value="${editQuestion.optionD}" required>
            </div>
            <div class="form-group">
                <label for="correctOption"><fmt:message key="questions.correct"/></label>
                <select id="correctOption" name="correctOption" required>
                    <option value="A" ${editQuestion.correctOption == 'A' ? 'selected' : ''}>A</option>
                    <option value="B" ${editQuestion.correctOption == 'B' ? 'selected' : ''}>B</option>
                    <option value="C" ${editQuestion.correctOption == 'C' ? 'selected' : ''}>C</option>
                    <option value="D" ${editQuestion.correctOption == 'D' ? 'selected' : ''}>D</option>
                </select>
            </div>
            <div class="form-group">
                <label for="difficulty"><fmt:message key="questions.difficulty"/></label>
                <select id="difficulty" name="difficulty">
                    <option value="EASY" ${editQuestion.difficulty == 'EASY' ? 'selected' : ''}><fmt:message key="questions.easy"/></option>
                    <option value="MEDIUM" ${empty editQuestion.difficulty || editQuestion.difficulty == 'MEDIUM' ? 'selected' : ''}><fmt:message key="questions.medium"/></option>
                    <option value="HARD" ${editQuestion.difficulty == 'HARD' ? 'selected' : ''}><fmt:message key="questions.hard"/></option>
                </select>
            </div>
            <div class="form-group">
                <label for="explanation"><fmt:message key="questions.explanation"/></label>
                <textarea id="explanation" name="explanation" rows="3">${editQuestion.explanation}</textarea>
            </div>
            <div class="form-group">
                <label for="imageUrl"><fmt:message key="questions.imageUrl"/></label>
                <input type="text" id="imageUrl" name="imageUrl" value="${editQuestion.imageUrl}"
                       maxlength="500" placeholder="https://… or /media/diagram.png" autocomplete="off">
                <p class="field-hint"><fmt:message key="questions.imageHint"/></p>
                <figure id="imagePreview" class="question-figure admin-image-preview${empty editQuestion.imageUrl ? ' is-hidden' : ''}" data-question-image>
                    <button type="button" class="question-image-trigger" aria-label="<fmt:message key="questions.enlargePreview"/>">
                        <img class="question-image" id="imagePreviewImg"
                             <c:if test="${not empty editQuestion.imageUrl}">
                             src="${fn:startsWith(editQuestion.imageUrl, '/') ? ctx.concat(editQuestion.imageUrl) : editQuestion.imageUrl}"
                             </c:if>
                             alt="<fmt:message key="questions.imagePreviewAlt"/>"
                             loading="lazy">
                    </button>
                    <figcaption class="question-image-hint"><fmt:message key="questions.imagePreviewHint"/></figcaption>
                    <p class="question-image-fallback" hidden><fmt:message key="questions.imagePreviewFallback"/></p>
                </figure>
            </div>
            <button type="submit" class="btn btn-primary"><fmt:message key="action.update"/></button>
            <a href="${ctx}/admin/questions" class="btn btn-outline"><fmt:message key="action.cancel"/></a>
        </form>
    </div>
</c:if>

<div class="card">
    <h2><fmt:message key="questions.bank"/></h2>
    <fmt:message key="confirm.deleteQuestion" var="confirmDeleteQuestion"/>
    <table class="data-table">
        <thead>
        <tr>
            <th><fmt:message key="questions.colSubject"/></th>
            <th><fmt:message key="questions.colBatch"/></th>
            <th><fmt:message key="questions.colQuestion"/></th>
            <th><fmt:message key="questions.colCorrect"/></th>
            <th><fmt:message key="questions.colActions"/></th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="q" items="${questions}">
            <tr>
                <td>${q.subjectName}</td>
                <td>
                    <c:choose>
                        <c:when test="${not empty q.batchLabel}"><span class="badge badge-admin">${q.batchLabel}</span></c:when>
                        <c:otherwise><span class="badge badge-muted"><fmt:message key="questions.unlabeled"/></span></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    ${q.prompt}
                    <c:if test="${not empty q.imageUrl}">
                        <span class="badge badge-muted"><fmt:message key="questions.imageBadge"/></span>
                    </c:if>
                </td>
                <td>${q.correctOption}</td>
                <td class="actions">
                    <a href="${ctx}/admin/questions?edit=${ep:enc(q.id)}" class="btn btn-sm"><fmt:message key="action.edit"/></a>
                    <form method="post" action="${ctx}/admin/questions" class="inline-form"
                          data-confirm="${confirmDeleteQuestion}"
                          onsubmit="return confirm(this.dataset.confirm);">
                        <ep:csrf/>
                        <input type="hidden" name="action" value="delete">
                        <input type="hidden" name="id" value="${q.id}">
                        <button type="submit" class="btn btn-sm btn-danger"><fmt:message key="action.delete"/></button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>

<script src="${ctx}/js/question-image.js"></script>
<script>
(function () {
    var input = document.getElementById('imageUrl');
    var preview = document.getElementById('imagePreview');
    var img = document.getElementById('imagePreviewImg');
    if (!input || !preview || !img) return;

    function resolveSrc(value) {
        var url = (value || '').trim();
        if (!url) return '';
        if (url.charAt(0) === '/') return '${ctx}' + url;
        return url;
    }

    function refreshPreview() {
        var src = resolveSrc(input.value);
        preview.classList.remove('is-broken', 'is-loaded');
        if (!src) {
            preview.classList.add('is-hidden');
            img.removeAttribute('src');
            return;
        }
        preview.classList.remove('is-hidden');
        img.src = src;
        if (window.ExamQuestionImages) {
            delete preview.dataset.imageBound;
            ExamQuestionImages.init(preview.parentNode);
        }
    }

    input.addEventListener('input', refreshPreview);
    refreshPreview();
})();
</script>
<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
