<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Take Exam" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div id="examShell" class="exam-shell"
     data-ctx="<c:out value='${ctx}'/>"
     data-post-url="<c:out value='${ctx}/user/exam'/>"
     data-attempt-id="<c:out value='${attempt.id}'/>"
     data-deadline="<c:out value='${deadline}'/>"
     data-seconds-per-question="<c:out value='${secondsPerQuestion}'/>"
     data-show-intro="false"
     data-submit-confirm="Submit exam? You cannot change answers after submitting."
     data-answer-fail-redirect="<c:out value='${ctx}/user/dashboard'/>">
<div class="exam-header">
    <h1>${attempt.examTitle}</h1>
    <p class="exam-meta">${attempt.subjectName} &middot; ${questions.size()} questions</p>
    <p class="exam-progress" id="exam-progress">Question 1 of ${questions.size()}</p>
    <div class="timer-bar">
        <div class="timer-slot">
            <span class="timer-label">Question time:</span>
            <span id="question-timer" class="timer-value">--:--</span>
        </div>
        <div class="timer-slot">
            <span class="timer-label">Exam time:</span>
            <span id="timer" class="timer-value">--:--</span>
        </div>
    </div>
</div>

<form id="examForm" method="post" action="${ctx}/user/exam" class="exam-form">
    <input type="hidden" name="attemptId" value="${attempt.id}">
    <input type="hidden" name="action" value="submit">

    <c:forEach var="q" items="${questions}" varStatus="status">
        <div class="question-card${status.index == 0 ? '' : ' is-hidden'}" data-index="${status.index}">
            <h3>Question ${status.index + 1}</h3>
            <p class="question-prompt">${q.prompt}</p>
            <div class="options">
                <label class="option-label">
                    <input type="radio" name="answer_${q.id}" value="A" data-question-id="${q.id}"
                        ${answers[q.id] == 'A' ? 'checked' : ''}>
                    <span class="option-letter">A.</span>
                    <span class="option-text">${q.optionA}</span>
                </label>
                <label class="option-label">
                    <input type="radio" name="answer_${q.id}" value="B" data-question-id="${q.id}"
                        ${answers[q.id] == 'B' ? 'checked' : ''}>
                    <span class="option-letter">B.</span>
                    <span class="option-text">${q.optionB}</span>
                </label>
                <label class="option-label">
                    <input type="radio" name="answer_${q.id}" value="C" data-question-id="${q.id}"
                        ${answers[q.id] == 'C' ? 'checked' : ''}>
                    <span class="option-letter">C.</span>
                    <span class="option-text">${q.optionC}</span>
                </label>
                <label class="option-label">
                    <input type="radio" name="answer_${q.id}" value="D" data-question-id="${q.id}"
                        ${answers[q.id] == 'D' ? 'checked' : ''}>
                    <span class="option-letter">D.</span>
                    <span class="option-text">${q.optionD}</span>
                </label>
            </div>
        </div>
    </c:forEach>

    <div class="exam-nav">
        <button type="button" id="prevBtn" class="btn btn-outline" disabled>Previous</button>
        <button type="button" id="nextBtn" class="btn btn-primary">Next</button>
        <button type="submit" id="submitBtn" class="btn btn-primary btn-lg is-hidden">
            Submit Exam
        </button>
    </div>
</form>
</div>

<script src="${ctx}/js/exam-session.js" defer></script>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
