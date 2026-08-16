<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Users" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Users</h1>
<p class="subtitle">Change exam track or role. Deleting a user also removes their attempts.</p>
<c:if test="${not empty error}">
    <div class="alert alert-error">${error}</div>
</c:if>

<div class="card">
    <table class="data-table">
        <thead>
        <tr><th>Username</th><th>Email</th><th>Registered</th><th>Role &amp; level</th><th>Actions</th></tr>
        </thead>
        <tbody>
        <c:forEach var="u" items="${users}">
            <tr>
                <td>${u.username}</td>
                <td>${u.email}</td>
                <td>${ep:fmt(u.createdAt)}</td>
                <td>
                    <form method="post" action="${ctx}/admin/users" class="table-inline-form">
                        <ep:csrf/>
                        <input type="hidden" name="action" value="update">
                        <input type="hidden" name="id" value="${ep:enc(u.id)}">
                        <label class="sr-only" for="role-${u.id}">Role</label>
                        <select id="role-${u.id}" name="role" <c:if test="${u.id == currentUser.id}">disabled</c:if>>
                            <option value="USER" ${u.role == 'USER' ? 'selected' : ''}>User</option>
                            <option value="ADMIN" ${u.role == 'ADMIN' ? 'selected' : ''}>Admin</option>
                        </select>
                        <c:if test="${u.id == currentUser.id}">
                            <input type="hidden" name="role" value="${u.role}">
                        </c:if>
                        <label class="sr-only" for="examLevel-${u.id}">Exam level</label>
                        <select id="examLevel-${u.id}" name="examLevel">
                            <option value="" ${empty u.examLevel ? 'selected' : ''}>—</option>
                            <option value="PROFESSIONAL" ${u.examLevel == 'PROFESSIONAL' ? 'selected' : ''}>Professional</option>
                            <option value="SUB_PROFESSIONAL" ${u.examLevel == 'SUB_PROFESSIONAL' ? 'selected' : ''}>Sub-Professional</option>
                        </select>
                        <button type="submit" class="btn btn-sm btn-primary">Save</button>
                    </form>
                </td>
                <td class="actions">
                    <c:if test="${u.id != currentUser.id}">
                        <form method="post" action="${ctx}/admin/users" class="inline-form"
                              onsubmit="return confirm('Delete this user and their exam attempts?');">
                            <ep:csrf/>
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="${ep:enc(u.id)}">
                            <button type="submit" class="btn btn-sm btn-danger">Delete</button>
                        </form>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
