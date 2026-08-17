<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
                    <th>Type</th>
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
                        <td>
                            <span class="badge badge-${h.attemptKind}">${h.attemptKind.displayName()}</span>
                        </td>
                        <td>${h.subjectName}</td>
                        <td>${ep:fmt(h.startedAt)}</td>
                        <td>${h.completedAt != null ? ep:fmt(h.completedAt) : '-'}</td>
                        <td>${h.scorePercent != null ? h.scorePercent : '-'}%</td>
                        <td><span class="badge badge-${h.status}">${h.status}</span></td>
                        <td>
                            <c:if test="${h.status != 'IN_PROGRESS'}">
                                <c:choose>
                                    <c:when test="${h.attemptKind == 'WEEKLY' && h.regimenId != null}">
                                        <a href="${ctx}/user/study-plan?regimenId=${ep:enc(h.regimenId)}" class="btn btn-sm">View Result</a>
                                    </c:when>
                                    <c:when test="${h.diagnostic || h.attemptKind == 'DIAGNOSTIC'}">
                                        <a href="${ctx}/user/diagnostic/result?attemptId=${ep:enc(h.id)}" class="btn btn-sm">View Result</a>
                                    </c:when>
                                    <c:otherwise>
                                        <a href="${ctx}/user/result?attemptId=${ep:enc(h.id)}" class="btn btn-sm">View Result</a>
                                    </c:otherwise>
                                </c:choose>
                            </c:if>
                            <c:if test="${h.status == 'IN_PROGRESS'}">
                                <c:choose>
                                    <c:when test="${h.attemptKind == 'WEEKLY'}">
                                        <a href="${ctx}/user/weekly?attemptId=${ep:enc(h.id)}" class="btn btn-sm btn-primary">Continue</a>
                                    </c:when>
                                    <c:when test="${h.attemptKind == 'CHECKPOINT'}">
                                        <a href="${ctx}/user/checkpoint?attemptId=${ep:enc(h.id)}" class="btn btn-sm btn-primary">Continue</a>
                                    </c:when>
                                    <c:when test="${h.diagnostic || h.attemptKind == 'DIAGNOSTIC'}">
                                        <a href="${ctx}/user/diagnostic?attemptId=${ep:enc(h.id)}" class="btn btn-sm btn-primary">Continue</a>
                                    </c:when>
                                    <c:otherwise>
                                        <a href="${ctx}/user/exam?attemptId=${ep:enc(h.id)}" class="btn btn-sm btn-primary">Continue</a>
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
