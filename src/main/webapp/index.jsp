<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:choose>
    <c:when test="${not empty currentUser}">
        <c:choose>
            <c:when test="${currentUser.role == 'ADMIN'}">
                <c:redirect url="${ctx}/admin/dashboard"/>
            </c:when>
            <c:otherwise>
                <c:redirect url="${ctx}/user/dashboard"/>
            </c:otherwise>
        </c:choose>
    </c:when>
    <c:otherwise>
        <c:redirect url="${ctx}/login"/>
    </c:otherwise>
</c:choose>
