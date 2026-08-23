<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.subjects.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1><fmt:message key="subjects.heading"/></h1>
<c:if test="${not empty error}">
    <div class="alert alert-error"><c:out value="${error}"/></div>
</c:if>

<div class="grid-2">
    <div class="card">
        <h2><c:choose><c:when test="${not empty editSubject}"><fmt:message key="subjects.edit"/></c:when><c:otherwise><fmt:message key="subjects.add"/></c:otherwise></c:choose></h2>
        <form method="post" action="${ctx}/admin/subjects" class="form">
            <ep:csrf/>
            <input type="hidden" name="action" value="${not empty editSubject ? 'update' : 'create'}">
            <c:if test="${not empty editSubject}">
                <input type="hidden" name="id" value="${editSubject.id}">
            </c:if>
            <div class="form-group">
                <label for="name"><fmt:message key="subjects.name"/></label>
                <input type="text" id="name" name="name" value="<c:out value='${editSubject.name}'/>" required>
            </div>
            <div class="form-group">
                <label for="description"><fmt:message key="subjects.description"/></label>
                <textarea id="description" name="description" rows="3"><c:out value="${editSubject.description}"/></textarea>
            </div>
            <div class="form-group checkbox-group">
                <label>
                    <input type="checkbox" name="professional" value="true"
                           <c:if test="${editSubject.professional}">checked</c:if>>
                    <p><fmt:message key="subjects.professional"/></p>
                </label>
            </div>
            <div class="form-group checkbox-group">
                <label>
                    <input type="checkbox" name="subProfessional" value="true"
                           <c:if test="${editSubject.subProfessional}">checked</c:if>>
                    <p><fmt:message key="subjects.subProfessional"/></p>
                </label>
            </div>
            <button type="submit" class="btn btn-primary">
                <c:choose><c:when test="${not empty editSubject}"><fmt:message key="action.update"/></c:when><c:otherwise><fmt:message key="action.create"/></c:otherwise></c:choose>
            </button>
            <c:if test="${not empty editSubject}">
                <a href="${ctx}/admin/subjects" class="btn btn-outline"><fmt:message key="action.cancel"/></a>
            </c:if>
        </form>
    </div>

    <div class="card">
        <h2><fmt:message key="subjects.all"/></h2>
        <fmt:message key="confirm.deleteSubject" var="confirmDeleteSubject"/>
        <table class="data-table">
            <thead>
            <tr>
                <th><fmt:message key="subjects.name"/></th>
                <th><fmt:message key="subjects.description"/></th>
                <th><fmt:message key="subjects.level"/></th>
                <th><fmt:message key="subjects.actions"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="subject" items="${subjects}">
                <tr>
                    <td><c:out value="${subject.name}"/></td>
                    <td>${subject.description}</td>
                    <td>
                        <c:choose>
                            <c:when test="${subject.professional and subject.subProfessional}"><fmt:message key="subjects.both"/></c:when>
                            <c:when test="${subject.professional}"><fmt:message key="subjects.professional"/></c:when>
                            <c:when test="${subject.subProfessional}"><fmt:message key="subjects.subProfessional"/></c:when>
                            <c:otherwise><fmt:message key="common.dash"/></c:otherwise>
                        </c:choose>
                    </td>
                    <td class="actions">
                        <a href="${ctx}/admin/subjects?edit=${ep:enc(subject.id)}" class="btn btn-sm"><fmt:message key="action.edit"/></a>
                        <form method="post" action="${ctx}/admin/subjects" class="inline-form"
                              data-confirm="${confirmDeleteSubject}"
                              onsubmit="return confirm(this.dataset.confirm);">
                            <ep:csrf/>
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="${subject.id}">
                            <button type="submit" class="btn btn-sm btn-danger"><fmt:message key="action.delete"/></button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
