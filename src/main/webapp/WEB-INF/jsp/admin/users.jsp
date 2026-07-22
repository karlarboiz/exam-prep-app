<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Users" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Users</h1>

<div class="card">
    <table class="data-table">
        <thead>
        <tr><th>Username</th><th>Email</th><th>Role</th><th>Exam level</th><th>Registered</th></tr>
        </thead>
        <tbody>
        <c:forEach var="u" items="${users}">
            <tr>
                <td>${u.username}</td>
                <td>${u.email}</td>
                <td><span class="badge ${u.role == 'ADMIN' ? 'badge-admin' : 'badge-user'}">${u.role}</span></td>
                <td>
                    <c:choose>
                        <c:when test="${u.examLevel != null}">${u.examLevel.displayName()}</c:when>
                        <c:otherwise>—</c:otherwise>
                    </c:choose>
                </td>
                <td>${u.createdAt}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
