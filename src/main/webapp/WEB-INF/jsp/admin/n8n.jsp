<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.n8n.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1><fmt:message key="n8n.heading"/></h1>
<p class="subtitle"><fmt:message key="n8n.subtitle"/></p>

<c:if test="${not empty error}">
    <div class="alert alert-error"><c:out value="${error}"/></div>
</c:if>
<c:if test="${not empty success}">
    <div class="alert alert-success"><c:out value="${success}"/></div>
</c:if>

<div class="grid-2">
    <div class="card">
        <h2><fmt:message key="n8n.questions.heading"/></h2>
        <c:choose>
            <c:when test="${questionsConfigured}">
                <p class="hint"><fmt:message key="n8n.questions.help"/></p>
                <form method="post" action="${ctx}/admin/n8n" class="form">
                    <ep:csrf/>
                    <input type="hidden" name="action" value="questions">
                    <div class="form-group">
                        <label for="message"><fmt:message key="n8n.questions.message"/></label>
                        <textarea id="message" name="message" rows="5" required maxlength="4000"><c:out value="${formMessage}"/></textarea>
                    </div>
                    <div class="form-group">
                        <label for="subject"><fmt:message key="n8n.questions.subject"/></label>
                        <input type="text" id="subject" name="subject" maxlength="100" value="<c:out value='${formSubject}'/>">
                    </div>
                    <div class="grid-2">
                        <div class="form-group">
                            <label for="count"><fmt:message key="n8n.questions.count"/></label>
                            <input type="number" id="count" name="count" min="1" max="100"
                                   value="${empty formCount ? 10 : formCount}">
                        </div>
                        <div class="form-group">
                            <label for="difficulty"><fmt:message key="n8n.questions.difficulty"/></label>
                            <select id="difficulty" name="difficulty">
                                <option value="" ${empty formDifficulty ? 'selected' : ''}><fmt:message key="n8n.questions.difficultyAny"/></option>
                                <option value="EASY" ${formDifficulty == 'EASY' ? 'selected' : ''}><fmt:message key="questions.easy"/></option>
                                <option value="MEDIUM" ${formDifficulty == 'MEDIUM' ? 'selected' : ''}><fmt:message key="questions.medium"/></option>
                                <option value="HARD" ${formDifficulty == 'HARD' ? 'selected' : ''}><fmt:message key="questions.hard"/></option>
                            </select>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="batchLabel"><fmt:message key="n8n.questions.batchLabel"/></label>
                        <input type="text" id="batchLabel" name="batchLabel" maxlength="100"
                               placeholder="${suggestedBatchLabel}"
                               value="${empty formBatchLabel ? suggestedBatchLabel : formBatchLabel}">
                    </div>
                    <button type="submit" class="btn btn-primary"><fmt:message key="n8n.questions.submit"/></button>
                </form>
            </c:when>
            <c:otherwise>
                <p class="empty-state"><fmt:message key="n8n.questions.unconfigured"/></p>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="card">
        <h2><fmt:message key="n8n.analyze.heading"/></h2>
        <c:choose>
            <c:when test="${analyzeConfigured}">
                <p class="hint"><fmt:message key="n8n.analyze.help"/></p>
                <form method="post" action="${ctx}/admin/n8n" enctype="multipart/form-data" class="form">
                    <ep:csrf/>
                    <input type="hidden" name="action" value="analyze">
                    <div class="form-group">
                        <label for="file"><fmt:message key="n8n.analyze.file"/></label>
                        <input type="file" id="file" name="file" required
                               accept=".pdf,.docx,.txt,.xlsx,.png,.jpg,.jpeg">
                    </div>
                    <div class="form-group">
                        <label for="analyzeMessage"><fmt:message key="n8n.analyze.message"/></label>
                        <textarea id="analyzeMessage" name="analyzeMessage" rows="4" maxlength="4000"><c:out value="${formAnalyzeMessage}"/></textarea>
                    </div>
                    <button type="submit" class="btn btn-primary"><fmt:message key="n8n.analyze.submit"/></button>
                </form>
            </c:when>
            <c:otherwise>
                <p class="empty-state"><fmt:message key="n8n.analyze.unconfigured"/></p>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<div class="card">
    <h2><fmt:message key="n8n.recent.heading"/></h2>
    <c:choose>
        <c:when test="${empty recentRequests}">
            <p class="empty-state"><fmt:message key="n8n.recent.empty"/></p>
        </c:when>
        <c:otherwise>
            <table class="data-table">
                <thead>
                <tr>
                    <th><fmt:message key="n8n.recent.kind"/></th>
                    <th><fmt:message key="n8n.recent.summary"/></th>
                    <th><fmt:message key="n8n.recent.status"/></th>
                    <th><fmt:message key="n8n.recent.sentAt"/></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="row" items="${recentRequests}">
                    <tr>
                        <td><span class="badge badge-${row.kind}"><fmt:message key="n8n.kind.${row.kind}"/></span></td>
                        <td><c:out value="${row.summary}"/></td>
                        <td><span class="badge badge-${row.status}"><fmt:message key="n8n.status.${row.status}"/></span></td>
                        <td>${ep:fmt(row.createdAt)}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<p class="exam-meta">
    <a href="${ctx}/admin/questions"><fmt:message key="n8n.importLink"/></a>
</p>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
