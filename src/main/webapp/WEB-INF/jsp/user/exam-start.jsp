<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Start Exam" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="exam-start">
    <h1>${exam.title}</h1>
    <p class="subtitle">${exam.subjectName} &middot; ${questionCount} questions &middot; ${exam.durationMinutes} minutes</p>

    <c:if test="${not empty error}">
        <p class="alert alert-error">${error}</p>
    </c:if>

    <form method="post" action="${ctx}/user/exam" class="card exam-start-form">
        <ep:csrf/>
        <input type="hidden" name="action" value="start">
        <input type="hidden" name="examId" value="${ep:enc(exam.id)}">

        <h2>Answer feedback</h2>
        <p class="hint">Choose when correct answers are shown for this attempt. You cannot change this after you begin.</p>

        <div class="reveal-choices" role="radiogroup" aria-label="When to show correct answers">
            <label class="reveal-choice">
                <input type="radio" name="answerRevealMode" value="AFTER_EACH" required>
                <span class="reveal-choice-body">
                    <span class="reveal-choice-title">After each question</span>
                    <span class="reveal-choice-desc">See whether you were right (and the correct answer) as soon as you pick an option.</span>
                </span>
            </label>
            <label class="reveal-choice">
                <input type="radio" name="answerRevealMode" value="AFTER_EXAM" checked>
                <span class="reveal-choice-body">
                    <span class="reveal-choice-title">After the exam</span>
                    <span class="reveal-choice-desc">Keep answers hidden until you submit; review everything on the result page.</span>
                </span>
            </label>
        </div>

        <p class="hint exam-integrity-note">Leaving the exam page (changing tabs or using the menu) is recorded for exam integrity.</p>

        <div class="exam-actions">
            <button type="submit" class="btn btn-primary btn-lg">Begin Exam</button>
            <a href="${ctx}/user/dashboard" class="btn btn-outline">Cancel</a>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
