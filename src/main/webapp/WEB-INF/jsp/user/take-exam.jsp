<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Take Exam" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

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
        <div class="question-card${status.index == 0 ? '' : ' is-hidden'}${empty q.imageUrl ? '' : ' has-image'}"
             data-index="${status.index}"
             data-has-image="${not empty q.imageUrl}">
            <h3>Question ${status.index + 1}</h3>
            <p class="question-prompt">${q.prompt}</p>
            <c:set var="imageUrl" value="${q.imageUrl}"/>
            <c:set var="imageAlt" value="Diagram for question ${status.index + 1}"/>
            <c:set var="imageLoading" value="${status.index == 0 ? 'eager' : 'lazy'}"/>
            <%@ include file="/WEB-INF/jsp/partials/question-image.jsp" %>
            <%@ include file="/WEB-INF/jsp/partials/question-options.jsp" %>
        </div>
    </c:forEach>

    <div class="exam-nav">
        <button type="button" id="prevBtn" class="btn btn-outline" disabled>Previous</button>
        <button type="button" id="nextBtn" class="btn btn-primary">Next</button>
        <button type="submit" id="submitBtn" class="btn btn-primary btn-lg is-hidden"
                onclick="return confirm('Submit exam? You cannot change answers after submitting.');">
            Submit Exam
        </button>
    </div>
</form>

<script src="${ctx}/js/question-image.js"></script>
<script>
    const ctx = '${ctx}';
    const attemptId = '${attempt.id}';
    const deadline = new Date('${deadline}');
    const secondsPerQuestion = ${secondsPerQuestion};
    const examForm = document.getElementById('examForm');
    const timerEl = document.getElementById('timer');
    const questionTimerEl = document.getElementById('question-timer');
    const progressEl = document.getElementById('exam-progress');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const submitBtn = document.getElementById('submitBtn');
    const cards = Array.from(document.querySelectorAll('.question-card'));
    const questionCount = cards.length;

    const remainingMs = Array.from({length: questionCount}, function () {
        return secondsPerQuestion * 1000;
    });
    let currentIndex = 0;
    let questionEndsAt = Date.now() + remainingMs[0];
    let questionStarted = false;
    let submitted = false;

    function formatTime(ms) {
        const totalSecs = Math.max(0, Math.floor(ms / 1000));
        const mins = Math.floor(totalSecs / 60);
        const secs = totalSecs % 60;
        return String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0');
    }

    function showQuestion(index) {
        const nextIndex = Math.max(0, Math.min(index, questionCount - 1));
        if (questionStarted) {
            remainingMs[currentIndex] = Math.max(0, questionEndsAt - Date.now());
        }
        questionStarted = true;
        currentIndex = nextIndex;
        cards.forEach(function (card, i) {
            card.classList.toggle('is-hidden', i !== currentIndex);
        });
        progressEl.textContent = 'Question ' + (currentIndex + 1) + ' of ' + questionCount;
        prevBtn.disabled = currentIndex === 0;
        const isLast = currentIndex === questionCount - 1;
        nextBtn.classList.toggle('is-hidden', isLast);
        submitBtn.classList.toggle('is-hidden', !isLast);
        questionEndsAt = Date.now() + remainingMs[currentIndex];
        questionTimerEl.classList.remove('timer-warning', 'timer-expired');
        if (window.ExamQuestionImages) {
            ExamQuestionImages.prepareCard(cards[currentIndex]);
        }
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
            questionTimerEl.textContent = '00:00';
            questionTimerEl.classList.add('timer-expired');
            submitExam();
            return;
        }
        timerEl.textContent = formatTime(overallDiff);
        if (overallDiff < 60000) timerEl.classList.add('timer-warning');

        const questionDiff = questionEndsAt - now;
        if (questionDiff <= 0) {
            questionTimerEl.textContent = '00:00';
            questionTimerEl.classList.add('timer-expired');
            goNext();
            return;
        }
        questionTimerEl.textContent = formatTime(questionDiff);
        if (questionDiff < 15000) {
            questionTimerEl.classList.add('timer-warning');
        } else {
            questionTimerEl.classList.remove('timer-warning');
        }
    }

    function saveAnswer(questionId, option) {
        const body = new URLSearchParams({
            action: 'answer',
            ajax: '1',
            attemptId: attemptId,
            questionId: String(questionId),
            selectedOption: option
        });
        fetch(ctx + '/user/exam', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: body.toString()
        }).then(function (res) {
            if (res.redirected || res.status === 403) {
                window.location.href = res.url || (ctx + '/user/dashboard');
            }
        }).catch(function () { /* keep selection local if save fails briefly */ });
    }

    prevBtn.addEventListener('click', goPrev);
    nextBtn.addEventListener('click', goNext);

    showQuestion(0);
    setInterval(updateTimers, 1000);
</script>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
