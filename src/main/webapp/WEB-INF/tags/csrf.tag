<%@ tag body-content="empty" trimDirectiveWhitespaces="true" import="com.examprep.util.CsrfUtil" %>
<input type="hidden" name="_csrf" value="<%= CsrfUtil.getToken(request) %>" />
