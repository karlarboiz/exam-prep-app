<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Placement Diagnostic" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div id="examShell" class="exam-shell${showIntro ? ' is-intro-pending' : ''}">
    <div class="exam-header">
        <h1>${attempt.examTitle}</h1>
        <p class="exam-meta">Placement diagnostic &middot; ${questions.size()} questions</p>
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

    <form id="examForm" method="post" action="${ctx}/user/diagnostic" class="exam-form">
        <input type="hidden" name="attemptId" value="${attempt.id}">
        <input type="hidden" name="action" value="submit">

        <c:forEach var="q" items="${questions}" varStatus="status">
            <div class="question-card${status.index == 0 ? '' : ' is-hidden'}" data-index="${status.index}">
                <h3>Question ${status.index + 1} <c:if test="${not empty q.subjectName}"><span class="exam-meta">(${q.subjectName})</span></c:if></h3>
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

        <div class="exam-nav">
            <button type="button" id="prevBtn" class="btn btn-outline" disabled>Previous</button>
            <button type="button" id="nextBtn" class="btn btn-primary">Next</button>
            <button type="submit" id="submitBtn" class="btn btn-primary btn-lg is-hidden"
                    onclick="return confirm('Submit diagnostic? You cannot change answers after submitting.');">
                Submit Diagnostic
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
                Your previous attempt expired or was not finished. You must retake the diagnostic.
            </p>
        </c:if>
        <h2 id="introTitle">Before you begin</h2>
        <p class="intro-lead">
            This placement diagnostic measures your readiness across subjects so we can guide your practice.
        </p>
        <ul class="intro-list">
            <li>You will answer <strong>${questions.size()}</strong> sampled questions across subjects.</li>
            <li>There is an overall time limit and a per-question timer.</li>
            <li>You must finish in one sitting. Leaving unfinished or running out of time means you will have to retake it.</li>
            <li>Submit only when you are done — answers cannot be changed afterward.</li>
        </ul>
        <p class="intro-countdown">Starting in <span id="introCountdown">10</span>s</p>
        <button type="button" id="introStartBtn" class="btn btn-primary btn-lg">Start now</button>
    </div>
</div>
</c:if>

<script>
    const ctx = '${ctx}';
    const attemptId = '${attempt.id}';
    let deadline = new Date('${deadline}');
    const secondsPerQuestion = ${secondsPerQuestion};
    const showIntro = ${showIntro ? 'true' : 'false'};
    const examForm = document.getElementById('examForm');
    const examShell = document.getElementById('examShell');
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
    let examStarted = false;
    let timerInterval = null;

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
        if (!examStarted) return;
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
        if (!examStarted) return;
        const body = new URLSearchParams({
            action: 'answer',
            ajax: '1',
            attemptId: attemptId,
            questionId: String(questionId),
            selectedOption: option
        });
        fetch(ctx + '/user/diagnostic', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: body.toString()
        }).then(function (res) {
            if (res.redirected || res.status === 403) {
                window.location.href = res.url || (ctx + '/user/diagnostic');
            }
        }).catch(function () { /* keep selection local if save fails briefly */ });
    }

    function startExam(newDeadlineIso) {
        if (examStarted) return;
        examStarted = true;
        if (newDeadlineIso) {
            deadline = new Date(newDeadlineIso);
        }
        const intro = document.getElementById('diagnosticIntro');
        if (intro) {
            intro.classList.add('is-hidden');
        }
        examShell.classList.remove('is-intro-pending');
        showQuestion(0);
        timerInterval = setInterval(updateTimers, 1000);
        updateTimers();
    }

    function beginAndStart() {
        const body = new URLSearchParams({
            action: 'begin',
            ajax: '1',
            attemptId: attemptId
        });
        fetch(ctx + '/user/diagnostic', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: body.toString()
        }).then(function (res) {
            if (res.redirected) {
                window.location.href = res.url || (ctx + '/user/diagnostic?retake=1');
                return null;
            }
            if (!res.ok) {
                window.location.href = ctx + '/user/diagnostic?retake=1';
                return null;
            }
            return res.json();
        }).then(function (data) {
            if (data && data.deadline) {
                startExam(data.deadline);
            }
        }).catch(function () {
            window.location.href = ctx + '/user/diagnostic?retake=1';
        });
    }

    prevBtn.addEventListener('click', goPrev);
    nextBtn.addEventListener('click', goNext);

    if (showIntro) {
        const countdownEl = document.getElementById('introCountdown');
        const startBtn = document.getElementById('introStartBtn');
        let remaining = 10;
        let introTimer = null;
        let beginning = false;

        function tickIntro() {
            remaining -= 1;
            if (countdownEl) {
                countdownEl.textContent = String(Math.max(0, remaining));
            }
            if (remaining <= 0) {
                clearInterval(introTimer);
                requestBegin();
            }
        }

        function requestBegin() {
            if (beginning) return;
            beginning = true;
            if (introTimer) clearInterval(introTimer);
            if (startBtn) startBtn.disabled = true;
            beginAndStart();
        }

        if (startBtn) {
            startBtn.addEventListener('click', requestBegin);
        }
        introTimer = setInterval(tickIntro, 1000);
    } else {
        startExam();
    }
</script>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
