<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Subjects" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Subjects</h1>
<c:if test="${not empty error}">
    <div class="alert alert-error">${error}</div>
</c:if>

<div class="grid-2">
    <div class="card">
        <h2><c:choose><c:when test="${not empty editSubject}">Edit Subject</c:when><c:otherwise>Add Subject</c:otherwise></c:choose></h2>
        <form method="post" action="${ctx}/admin/subjects" class="form">
            <input type="hidden" name="action" value="${not empty editSubject ? 'update' : 'create'}">
            <c:if test="${not empty editSubject}">
                <input type="hidden" name="id" value="${editSubject.id}">
            </c:if>
            <div class="form-group">
                <label for="name">Name</label>
                <input type="text" id="name" name="name" value="${editSubject.name}" required>
            </div>
            <div class="form-group">
                <label for="description">Description</label>
                <textarea id="description" name="description" rows="3">${editSubject.description}</textarea>
            </div>
            <button type="submit" class="btn btn-primary">
                <c:choose><c:when test="${not empty editSubject}">Update</c:when><c:otherwise>Create</c:otherwise></c:choose>
            </button>
            <c:if test="${not empty editSubject}">
                <a href="${ctx}/admin/subjects" class="btn btn-outline">Cancel</a>
            </c:if>
        </form>
    </div>

    <div class="card">
        <h2>All Subjects</h2>
        <table class="data-table">
            <thead>
            <tr><th>Name</th><th>Description</th><th>Actions</th></tr>
            </thead>
            <tbody>
            <c:forEach var="subject" items="${subjects}">
                <tr>
                    <td>${subject.name}</td>
                    <td>${subject.description}</td>
                    <td class="actions">
                        <a href="${ctx}/admin/subjects?edit=${subject.id}" class="btn btn-sm">Edit</a>
                        <form method="post" action="${ctx}/admin/subjects" class="inline-form" onsubmit="return confirm('Delete this subject?');">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="${subject.id}">
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
