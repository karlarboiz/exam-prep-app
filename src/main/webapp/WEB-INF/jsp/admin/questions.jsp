<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
        <label>Filter by batch:</label>
        <select name="batchLabel" onchange="this.form.submit()">
            <option value="">All batches</option>
            <option value="__unlabeled__" ${filterBatchLabel == '__unlabeled__' ? 'selected' : ''}>Unlabeled</option>
            <c:forEach var="label" items="${batchLabels}">
                <option value="${label}" ${filterBatchLabel == label ? 'selected' : ''}>${label}</option>
            </c:forEach>
        </select>
    </form>
</div>

<div class="card">
    <h2>Import from Excel</h2>
    <p>Upload an <code>.xlsx</code> file with columns:
        subject, prompt, option_a–d, correct_option, difficulty (optional), explanation,
        optional <code>is_professional</code> / <code>is_sub_professional</code>
        (used only when the subject is created; omit both to enable both exam tracks),
        and optional <code>batch_label</code>.
        New imports default to <code>${suggestedBatchLabel}</code> (<code>cse-import-</code> plus
        today's date). Re-import updates only items that already have this same label and
        matching subject + prompt — other batches are left alone.</p>
    <div class="actions import-actions">
        <a href="${ctx}/admin/questions?action=template" class="btn btn-outline">Download template</a>
        <c:url var="exportUrl" value="/admin/questions">
            <c:param name="action" value="export"/>
            <c:if test="${not empty filterSubjectId}">
                <c:param name="subjectId" value="${filterSubjectId}"/>
            </c:if>
            <c:if test="${not empty filterBatchLabel}">
                <c:param name="batchLabel" value="${filterBatchLabel}"/>
            </c:if>
        </c:url>
        <a href="${exportUrl}" class="btn btn-outline">Export questions</a>
    </div>
    <form method="post" action="${ctx}/admin/questions" enctype="multipart/form-data" class="form">
        <ep:csrf/>
        <input type="hidden" name="action" value="import">
        <div class="form-group">
            <label for="batchLabel">Batch label</label>
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
            <p class="field-hint">Today's batch is <code>${suggestedBatchLabel}</code>.
                Reuse an existing label to update that batch. A different label inserts a separate set.</p>
        </div>
        <div class="form-group">
            <label for="file">Excel file</label>
            <input type="file" id="file" name="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" required>
        </div>
        <button type="submit" class="btn btn-primary">Import</button>
    </form>
</div>

<%-- Add Question (create) is hidden; form is shown only when editing an existing question. --%>
<c:if test="${not empty editQuestion}">
    <div class="card">
        <h2>Edit Question</h2>
        <form method="post" action="${ctx}/admin/questions" class="form">
            <ep:csrf/>
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="id" value="${editQuestion.id}">
            <div class="form-group">
                <label for="subjectId">Subject</label>
                <select id="subjectId" name="subjectId" required>
                    <c:forEach var="s" items="${subjects}">
                        <option value="${s.id}" ${editQuestion.subjectId == s.id ? 'selected' : ''}>${s.name}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-group">
                <label>Batch</label>
                <p><c:choose>
                    <c:when test="${not empty editQuestion.batchLabel}"><span class="badge badge-admin">${editQuestion.batchLabel}</span></c:when>
                    <c:otherwise><span class="badge badge-muted">Unlabeled</span></c:otherwise>
                </c:choose></p>
                <p class="field-hint">Set by Excel import. Re-import the same batch to update this item.</p>
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
                    <option value="MEDIUM" ${empty editQuestion.difficulty || editQuestion.difficulty == 'MEDIUM' ? 'selected' : ''}>Medium</option>
                    <option value="HARD" ${editQuestion.difficulty == 'HARD' ? 'selected' : ''}>Hard</option>
                </select>
            </div>
            <div class="form-group">
                <label for="explanation">Explanation</label>
                <textarea id="explanation" name="explanation" rows="3">${editQuestion.explanation}</textarea>
            </div>
            <div class="form-group">
                <label for="imageUrl">Diagram image URL</label>
                <input type="text" id="imageUrl" name="imageUrl" value="${editQuestion.imageUrl}"
                       maxlength="500" placeholder="https://… or /media/diagram.png" autocomplete="off">
                <p class="field-hint">Optional. Shown with the question stem for word problems. Leave blank for text-only items.</p>
                <figure id="imagePreview" class="question-figure admin-image-preview${empty editQuestion.imageUrl ? ' is-hidden' : ''}" data-question-image>
                    <button type="button" class="question-image-trigger" aria-label="Enlarge diagram preview">
                        <img class="question-image" id="imagePreviewImg"
                             <c:if test="${not empty editQuestion.imageUrl}">
                             src="${fn:startsWith(editQuestion.imageUrl, '/') ? ctx.concat(editQuestion.imageUrl) : editQuestion.imageUrl}"
                             </c:if>
                             alt="Question diagram preview"
                             loading="lazy">
                    </button>
                    <figcaption class="question-image-hint">Preview — tap to enlarge</figcaption>
                    <p class="question-image-fallback" hidden>Diagram could not be loaded. Check the URL.</p>
                </figure>
            </div>
            <button type="submit" class="btn btn-primary">Update</button>
            <a href="${ctx}/admin/questions" class="btn btn-outline">Cancel</a>
        </form>
    </div>
</c:if>

<div class="card">
    <h2>Question Bank</h2>
    <table class="data-table">
        <thead>
        <tr><th>Subject</th><th>Batch</th><th>Question</th><th>Correct</th><th>Actions</th></tr>
        </thead>
        <tbody>
        <c:forEach var="q" items="${questions}">
            <tr>
                <td>${q.subjectName}</td>
                <td>
                    <c:choose>
                        <c:when test="${not empty q.batchLabel}"><span class="badge badge-admin">${q.batchLabel}</span></c:when>
                        <c:otherwise><span class="badge badge-muted">Unlabeled</span></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    ${q.prompt}
                    <c:if test="${not empty q.imageUrl}">
                        <span class="badge badge-muted">Image</span>
                    </c:if>
                </td>
                <td>${q.correctOption}</td>
                <td class="actions">
                    <a href="${ctx}/admin/questions?edit=${ep:enc(q.id)}" class="btn btn-sm">Edit</a>
                    <form method="post" action="${ctx}/admin/questions" class="inline-form" onsubmit="return confirm('Delete this question?');">
                        <ep:csrf/>
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
