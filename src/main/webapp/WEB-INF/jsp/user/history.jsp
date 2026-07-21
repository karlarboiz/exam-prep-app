<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="History" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Exam History</h1>
<p class="subtitle">Your past exam attempts and scores.</p>

<div class="card">
    <c:choose>
        <c:when test="${empty history}">
            <p class="empty-state">No exam attempts yet. <a href="${ctx}/user/dashboard">Take your first exam</a>.</p>
        </c:when>
        <c:otherwise>
            <table class="data-table">
                <thead>
                <tr>
                    <th>Exam</th>
                    <th>Subject</th>
                    <th>Started</th>
                    <th>Completed</th>
                    <th>Score</th>
                    <th>Status</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="h" items="${history}">
                    <tr>
                        <td>${h.examTitle}</td>
                        <td>${h.subjectName}</td>
                        <td>${h.startedAt}</td>
                        <td>${h.completedAt != null ? h.completedAt : '-'}</td>
                        <td>${h.scorePercent != null ? h.scorePercent : '-'}%</td>
                        <td><span class="badge badge-${h.status}">${h.status}</span></td>
                        <td>
                            <c:if test="${h.status != 'IN_PROGRESS'}">
                                <a href="${ctx}/user/result?attemptId=${ep:enc(h.id)}" class="btn btn-sm">View Result</a>
                            </c:if>
                            <c:if test="${h.status == 'IN_PROGRESS'}">
                                <a href="${ctx}/user/exam?attemptId=${ep:enc(h.id)}" class="btn btn-sm btn-primary">Continue</a>
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
