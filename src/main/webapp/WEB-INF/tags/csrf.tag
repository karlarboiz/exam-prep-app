<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="com.examprep.util.CsrfUtil" %>
<c:set var="csrfToken" value="<%= CsrfUtil.getToken(request) %>" />
<input type="hidden" name="_csrf" value="${csrfToken}" />
