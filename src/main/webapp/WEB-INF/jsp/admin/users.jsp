<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.users.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1><fmt:message key="users.heading"/></h1>
<p class="subtitle"><fmt:message key="users.subtitle"/></p>
<c:if test="${not empty error}">
    <div class="alert alert-error"><c:out value="${error}"/></div>
</c:if>
<fmt:message key="confirm.deleteUser" var="confirmDeleteUser"/>

<div class="card">
    <table class="data-table">
        <thead>
        <tr>
            <th><fmt:message key="users.username"/></th>
            <th><fmt:message key="users.email"/></th>
            <th><fmt:message key="users.registered"/></th>
            <th><fmt:message key="users.roleLevel"/></th>
            <th><fmt:message key="users.actions"/></th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="u" items="${users}">
            <tr>
                <td><c:out value="${u.username}"/></td>
                <td><c:out value="${u.email}"/></td>
                <td>${ep:fmt(u.createdAt)}</td>
                <td>
                    <form method="post" action="${ctx}/admin/users" class="table-inline-form">
                        <ep:csrf/>
                        <input type="hidden" name="action" value="update">
                        <input type="hidden" name="id" value="${ep:enc(u.id)}">
                        <label class="sr-only" for="role-${u.id}"><fmt:message key="users.role"/></label>
                        <select id="role-${u.id}" name="role" <c:if test="${u.id == currentUser.id}">disabled</c:if>>
                            <option value="USER" ${u.role == 'USER' ? 'selected' : ''}><fmt:message key="users.roleUser"/></option>
                            <option value="ADMIN" ${u.role == 'ADMIN' ? 'selected' : ''}><fmt:message key="users.roleAdmin"/></option>
                        </select>
                        <c:if test="${u.id == currentUser.id}">
                            <input type="hidden" name="role" value="${u.role}">
                        </c:if>
                        <label class="sr-only" for="examLevel-${u.id}"><fmt:message key="users.examLevel"/></label>
                        <select id="examLevel-${u.id}" name="examLevel">
                            <option value="" ${empty u.examLevel ? 'selected' : ''}><fmt:message key="examLevel.none"/></option>
                            <option value="PROFESSIONAL" ${u.examLevel == 'PROFESSIONAL' ? 'selected' : ''}><fmt:message key="examLevel.PROFESSIONAL"/></option>
                            <option value="SUB_PROFESSIONAL" ${u.examLevel == 'SUB_PROFESSIONAL' ? 'selected' : ''}><fmt:message key="examLevel.SUB_PROFESSIONAL"/></option>
                        </select>
                        <button type="submit" class="btn btn-sm btn-primary"><fmt:message key="action.save"/></button>
                    </form>
                </td>
                <td class="actions">
                    <c:if test="${u.id != currentUser.id}">
                        <form method="post" action="${ctx}/admin/users" class="inline-form"
                              data-confirm="${confirmDeleteUser}"
                              onsubmit="return confirm(this.dataset.confirm);">
                            <ep:csrf/>
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="${ep:enc(u.id)}">
                            <button type="submit" class="btn btn-sm btn-danger"><fmt:message key="action.delete"/></button>
                        </form>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
