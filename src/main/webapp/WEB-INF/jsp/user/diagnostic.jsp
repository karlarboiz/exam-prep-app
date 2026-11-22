<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitleKey" value="page.diagnostic.title" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>
<fmt:message key="exam.questionOf" var="questionOfPattern"/>
<fmt:message key="diagnostic.confirmSubmit" var="confirmSubmit"/>

<div id="examShell" class="exam-shell${showIntro ? ' is-intro-pending' : ''}"
     data-ctx="<c:out value='${ctx}'/>"
     data-post-url="<c:out value='${ctx}/user/diagnostic'/>"
     data-attempt-id="<c:out value='${attempt.id}'/>"
     data-deadline="<c:out value='${deadline}'/>"
     data-seconds-per-question="<c:out value='${secondsPerQuestion}'/>"
     data-show-intro="${showIntro}"
     data-fail-redirect="<c:out value='${ctx}/user/diagnostic?retake=1'/>"
     data-answer-fail-redirect="<c:out value='${ctx}/user/diagnostic'/>"
     data-tracking-endpoint="/user/diagnostic"
     data-leave-count="${attempt.leaveCount}">
    <div class="exam-header">
        <h1><c:out value="${attempt.examTitle}"/></h1>
        <p class="exam-meta"><fmt:message key="diagnostic.meta"><fmt:param value="${questions.size()}"/></fmt:message></p>
        <p class="hint exam-integrity-note"><fmt:message key="exam.integrityNote"/></p>
        <p class="exam-progress" id="exam-progress"><fmt:message key="exam.questionOf"><fmt:param value="1"/><fmt:param value="${questions.size()}"/></fmt:message></p>
        <div class="timer-bar">
            <div class="timer-slot">
                <span class="timer-label"><fmt:message key="exam.questionTime"/></span>
                <span id="question-timer" class="timer-value">--:--</span>
            </div>
            <div class="timer-slot">
                <span class="timer-label"><fmt:message key="exam.examTime"/></span>
                <span id="timer" class="timer-value">--:--</span>
            </div>
        </div>
    </div>

    <form id="examForm" method="post" action="${ctx}/user/diagnostic" class="exam-form"
          data-question-of="${questionOfPattern}" data-confirm-submit="${confirmSubmit}">
        <ep:csrf/>
        <input type="hidden" name="attemptId" value="${attempt.id}">
        <input type="hidden" name="action" value="submit">

        <c:forEach var="q" items="${questions}" varStatus="status">
            <div class="question-card${status.index == 0 ? '' : ' is-hidden'}${empty q.imageUrl ? '' : ' has-image'}"
                 data-index="${status.index}"
                 data-question-id="${q.id}"
                 data-has-image="${not empty q.imageUrl}">
                <h3><fmt:message key="exam.question"><fmt:param value="${status.index + 1}"/></fmt:message> <c:if test="${not empty q.subjectName}"><span class="exam-meta">(<c:out value="${q.subjectName}"/>)</span></c:if></h3>
                <p class="question-prompt"><c:out value="${q.prompt}"/></p>
                <c:set var="imageUrl" value="${q.imageUrl}"/>
                <fmt:message key="exam.imageAlt" var="imageAlt"><fmt:param value="${status.index + 1}"/></fmt:message>
                <c:set var="imageLoading" value="${status.index == 0 ? 'eager' : 'lazy'}"/>
                <%@ include file="/WEB-INF/jsp/partials/question-image.jsp" %>
                <%@ include file="/WEB-INF/jsp/partials/question-options.jsp" %>
            </div>
        </c:forEach>

        <div class="exam-nav">
            <button type="button" id="prevBtn" class="btn btn-outline" disabled><fmt:message key="exam.previous"/></button>
            <button type="button" id="nextBtn" class="btn btn-primary"><fmt:message key="exam.next"/></button>
            <button type="submit" id="submitBtn" class="btn btn-primary btn-lg is-hidden">
                <fmt:message key="diagnostic.submit"/>
            </button>
        </div>
    </form>
</div>

<c:if test="${showIntro}">
<div id="diagnosticIntro" class="intro-modal" role="dialog" aria-modal="true" aria-labelledby="introTitle">
    <div class="intro-modal-backdrop"></div>
    <div class="intro-modal-panel">
        <c:if test="${retake}">
            <p class="alert alert-warning intro-retake-note">
                <fmt:message key="diagnostic.retake"/>
            </p>
        </c:if>
        <h2 id="introTitle"><fmt:message key="diagnostic.introTitle"/></h2>
        <p class="intro-lead">
            <fmt:message key="diagnostic.introLead"/>
        </p>
        <ul class="intro-list">
            <li><fmt:message key="diagnostic.introSample"><fmt:param value="${questions.size()}"/></fmt:message></li>
            <li><fmt:message key="diagnostic.introTimers"/></li>
            <li><fmt:message key="diagnostic.introSitting"/></li>
            <li><fmt:message key="diagnostic.introIntegrity"/></li>
            <li><fmt:message key="diagnostic.introSubmit"/></li>
        </ul>
        <p class="intro-countdown"><fmt:message key="diagnostic.introCountdown"/> <span id="introCountdown">10</span>s</p>
        <button type="button" id="introStartBtn" class="btn btn-primary btn-lg"><fmt:message key="diagnostic.startNow"/></button>
    </div>
</div>
</c:if>

<%@ include file="/WEB-INF/jsp/user/integrity-warning.jsp" %>

<script src="${ctx}/js/exam-tracking.js" defer></script>
<script src="${ctx}/js/question-image.js" defer></script>
<script src="${ctx}/js/exam-session.js" defer></script>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
