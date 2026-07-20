<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Take Exam" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="exam-header">
    <h1>${attempt.examTitle}</h1>
    <p class="exam-meta">${attempt.subjectName} &middot; ${questions.size()} questions</p>
    <div class="timer-bar">
        <span id="timer-label">Time remaining:</span>
        <span id="timer" class="timer-value">--:--</span>
    </div>
</div>

<form id="examForm" method="post" action="${ctx}/user/exam" class="exam-form">
    <input type="hidden" name="attemptId" value="${attempt.id}">
    <input type="hidden" name="action" value="submit">

    <c:forEach var="q" items="${questions}" varStatus="status">
        <div class="question-card">
            <h3>Question ${status.index + 1}</h3>
            <p class="question-prompt">${q.prompt}</p>
            <div class="options">
                <label class="option-label">
                    <input type="radio" name="answer_${q.id}" value="A"
                        ${answers[q.id] == 'A' ? 'checked' : ''}
                        onchange="saveAnswer(${q.id}, 'A')">
                    <span class="option-letter">A.</span>
                    <span class="option-text">${q.optionA}</span>
                </label>
                <label class="option-label">
                    <input type="radio" name="answer_${q.id}" value="B"
                        ${answers[q.id] == 'B' ? 'checked' : ''}
                        onchange="saveAnswer(${q.id}, 'B')">
                    <span class="option-letter">B.</span>
                    <span class="option-text">${q.optionB}</span>
                </label>
                <label class="option-label">
                    <input type="radio" name="answer_${q.id}" value="C"
                        ${answers[q.id] == 'C' ? 'checked' : ''}
                        onchange="saveAnswer(${q.id}, 'C')">
                    <span class="option-letter">C.</span>
                    <span class="option-text">${q.optionC}</span>
                </label>
                <label class="option-label">
                    <input type="radio" name="answer_${q.id}" value="D"
                        ${answers[q.id] == 'D' ? 'checked' : ''}
                        onchange="saveAnswer(${q.id}, 'D')">
                    <span class="option-letter">D.</span>
                    <span class="option-text">${q.optionD}</span>
                </label>
            </div>
        </div>
    </c:forEach>

    <div class="exam-actions">
        <button type="submit" class="btn btn-primary btn-lg" onclick="return confirm('Submit exam? You cannot change answers after submitting.');">
            Submit Exam
        </button>
    </div>
</form>

<form id="saveForm" method="post" action="${ctx}/user/exam" style="display:none;">
    <input type="hidden" name="attemptId" value="${attempt.id}">
    <input type="hidden" name="action" value="answer">
    <input type="hidden" name="questionId" id="saveQuestionId">
    <input type="hidden" name="selectedOption" id="saveSelectedOption">
</form>

<script>
    const deadline = new Date('${deadline}');
    const timerEl = document.getElementById('timer');
    const examForm = document.getElementById('examForm');
    const timerEl = document.getElementById('timer');
    const questionTimerEl = document.getElementById('question-timer');
    const progressEl = document.getElementById('exam-progress');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const submitBtn = document.getElementById('submitBtn');
    const cards = Array.from(document.querySelectorAll('.question-card'));
    const questionCount = cards.length;

    let currentIndex = 0;
    let questionEndsAt = Date.now() + secondsPerQuestion * 1000;
    let submitted = false;

    function formatTime(ms) {
        const totalSecs = Math.max(0, Math.floor(ms / 1000));
        const mins = Math.floor(totalSecs / 60);
        const secs = totalSecs % 60;
        return String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0');
    }

    function showQuestion(index) {
        currentIndex = Math.max(0, Math.min(index, questionCount - 1));
        cards.forEach(function (card, i) {
            card.classList.toggle('is-hidden', i !== currentIndex);
        });
        progressEl.textContent = 'Question ' + (currentIndex + 1) + ' of ' + questionCount;
        prevBtn.disabled = currentIndex === 0;
        const isLast = currentIndex === questionCount - 1;
        nextBtn.classList.toggle('is-hidden', isLast);
        submitBtn.classList.toggle('is-hidden', !isLast);
        questionEndsAt = Date.now() + secondsPerQuestion * 1000;
        questionTimerEl.classList.remove('timer-warning', 'timer-expired');
        updateTimers();
    }

    function goNext() {
        if (currentIndex < questionCount - 1) {
            showQuestion(currentIndex + 1);
        } else {
            submitExam();
        }
    }

    function goPrev() {
        if (currentIndex > 0) {
            showQuestion(currentIndex - 1);
        }
    }

    function submitExam() {
        if (submitted) return;
        submitted = true;
        examForm.submit();
    }

    function updateTimers() {
        const now = Date.now();
        const overallDiff = deadline.getTime() - now;
        if (overallDiff <= 0) {
            timerEl.textContent = '00:00';
            timerEl.classList.add('timer-expired');
            examForm.submit();
            return;
        }
        const mins = Math.floor(diff / 60000);
        const secs = Math.floor((diff % 60000) / 1000);
        timerEl.textContent = String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0');
        if (diff < 60000) timerEl.classList.add('timer-warning');
    }

    function saveAnswer(questionId, option) {
        document.getElementById('saveQuestionId').value = questionId;
        document.getElementById('saveSelectedOption').value = option;
        document.getElementById('saveForm').submit();
    }

    updateTimer();
    setInterval(updateTimer, 1000);
</script>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
