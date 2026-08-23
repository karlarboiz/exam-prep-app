<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.history.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1><fmt:message key="history.heading"/></h1>
<p class="subtitle"><fmt:message key="history.subtitle"/></p>

<div class="card">
    <c:choose>
        <c:when test="${empty history}">
            <p class="empty-state"><fmt:message key="history.empty"/> <a href="${ctx}/user/dashboard"><fmt:message key="history.takeFirst"/></a>.</p>
        </c:when>
        <c:otherwise>
            <table class="data-table">
                <thead>
                <tr>
                    <th><fmt:message key="history.exam"/></th>
                    <th><fmt:message key="history.type"/></th>
                    <th><fmt:message key="history.subject"/></th>
                    <th><fmt:message key="history.started"/></th>
                    <th><fmt:message key="history.completed"/></th>
                    <th><fmt:message key="history.score"/></th>
                    <th><fmt:message key="history.status"/></th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="h" items="${history}">
                    <tr>
                        <td>${h.examTitle}</td>
                        <td>
                            <span class="badge badge-${h.attemptKind}"><fmt:message key="attemptKind.${h.attemptKind}"/></span>
                        </td>
                        <td>${h.subjectName}</td>
                        <td>${ep:fmt(h.startedAt)}</td>
                        <td>${h.completedAt != null ? ep:fmt(h.completedAt) : '-'}</td>
                        <td>${h.scorePercent != null ? h.scorePercent : '-'}%</td>
                        <td><span class="badge badge-${h.status}"><fmt:message key="status.${h.status}"/></span></td>
                        <td>
                            <c:if test="${h.status != 'IN_PROGRESS'}">
                                <c:choose>
                                    <c:when test="${h.attemptKind == 'WEEKLY' && h.regimenId != null}">
                                        <a href="${ctx}/user/study-plan?regimenId=${ep:enc(h.regimenId)}" class="btn btn-sm"><fmt:message key="history.viewResult"/></a>
                                    </c:when>
                                    <c:when test="${h.diagnostic || h.attemptKind == 'DIAGNOSTIC'}">
                                        <a href="${ctx}/user/diagnostic/result?attemptId=${ep:enc(h.id)}" class="btn btn-sm"><fmt:message key="history.viewResult"/></a>
                                    </c:when>
                                    <c:otherwise>
                                        <a href="${ctx}/user/result?attemptId=${ep:enc(h.id)}" class="btn btn-sm"><fmt:message key="history.viewResult"/></a>
                                    </c:otherwise>
                                </c:choose>
                            </c:if>
                            <c:if test="${h.status == 'IN_PROGRESS'}">
                                <c:choose>
                                    <c:when test="${h.attemptKind == 'WEEKLY'}">
                                        <a href="${ctx}/user/weekly?attemptId=${ep:enc(h.id)}" class="btn btn-sm btn-primary"><fmt:message key="history.continue"/></a>
                                    </c:when>
                                    <c:when test="${h.attemptKind == 'CHECKPOINT'}">
                                        <a href="${ctx}/user/checkpoint?attemptId=${ep:enc(h.id)}" class="btn btn-sm btn-primary"><fmt:message key="history.continue"/></a>
                                    </c:when>
                                    <c:when test="${h.diagnostic || h.attemptKind == 'DIAGNOSTIC'}">
                                        <a href="${ctx}/user/diagnostic?attemptId=${ep:enc(h.id)}" class="btn btn-sm btn-primary"><fmt:message key="history.continue"/></a>
                                    </c:when>
                                    <c:otherwise>
                                        <a href="${ctx}/user/exam?attemptId=${ep:enc(h.id)}" class="btn btn-sm btn-primary"><fmt:message key="history.continue"/></a>
                                    </c:otherwise>
                                </c:choose>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
