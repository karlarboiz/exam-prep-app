<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Dashboard" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<h1>Welcome, ${currentUser.username}</h1>
<p class="subtitle">
    Choose a practice exam to begin preparing.
    <c:if test="${not empty examLevel}">
        <span class="badge badge-user">${examLevel.displayName()} track</span>
    </c:if>
</p>

<div class="grid-2">
    <div class="card">
        <h2>Available Exams</h2>
        <c:choose>
            <c:when test="${empty exams}">
                <p class="empty-state">No exams available yet. Check back later.</p>
            </c:when>
            <c:otherwise>
                <div class="exam-grid">
                    <c:forEach var="exam" items="${exams}">
                        <div class="exam-card">
                            <h3>${exam.title}</h3>
                            <p class="exam-meta">${exam.subjectName}</p>
                            <p class="exam-meta">${exam.questionCount} questions &middot; ${exam.durationMinutes} minutes</p>
                            <a href="${ctx}/user/exam?examId=${ep:enc(exam.id)}" class="btn btn-primary">Start Exam</a>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="card">
        <h2>Subjects</h2>
        <ul class="subject-list">
            <c:forEach var="subject" items="${subjects}">
                <li><strong>${subject.name}</strong> — ${subject.description}</li>
            </c:forEach>
        </ul>
    </div>
</div>

<c:if test="${not empty history}">
    <div class="card">
        <h2>Recent Attempts</h2>
        <table class="data-table">
            <thead>
            <tr><th>Exam</th><th>Subject</th><th>Score</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
            <c:forEach var="h" items="${history}" begin="0" end="4">
                <tr>
                    <td>${h.examTitle}</td>
                    <td>${h.subjectName}</td>
                    <td>${h.scorePercent != null ? h.scorePercent : '-'}%</td>
                    <td><span class="badge badge-${h.status}">${h.status}</span></td>
                    <td>
                        <c:if test="${h.status != 'IN_PROGRESS'}">
                            <a href="${ctx}/user/result?attemptId=${ep:enc(h.id)}" class="btn btn-sm">View</a>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        <a href="${ctx}/user/history" class="btn btn-outline">View All History</a>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
