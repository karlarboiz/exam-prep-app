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

<div id="examShell" class="exam-shell${showIntro ? ' is-intro-pending' : ''}">
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
            <button type="submit" id="submitBtn" class="btn btn-primary btn-lg is-hidden"
                    onclick="return confirm(document.getElementById('examForm').dataset.confirmSubmit);">
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

<script src="${ctx}/js/exam-tracking.js"></script>
<script src="${ctx}/js/question-image.js"></script>
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
        progressEl.textContent = (examForm.dataset.questionOf || 'Question {0} of {1}')
            .replace('{0}', String(currentIndex + 1))
            .replace('{1}', String(questionCount));
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

    function csrfToken() {
        const el = document.querySelector('input[name="_csrf"]');
        return el ? el.value : '';
    }

    function currentQuestionId() {
        const card = cards[currentIndex];
        return card ? card.getAttribute('data-question-id') : null;
    }

    function showIntegrityWarning(leaveCount) {
        const modal = document.getElementById('integrityWarning');
        const countEl = document.getElementById('integrityLeaveCount');
        if (countEl && typeof leaveCount === 'number') {
            countEl.textContent = String(leaveCount);
        }
        if (modal) {
            modal.classList.remove('is-hidden');
        }
    }

    function startTracking() {
        if (!window.ExamTracking) return;
        ExamTracking.init({
            ctx: ctx,
            attemptId: attemptId,
            endpoint: '/user/diagnostic',
            csrfToken: csrfToken(),
            leaveCount: ${attempt.leaveCount},
            getQuestionId: currentQuestionId,
            getRemainingMs: function () {
                return Math.max(0, questionEndsAt - Date.now());
            },
            onReturn: showIntegrityWarning
        });
    }

    function submitExam() {
        if (submitted) return;
        submitted = true;
        if (window.ExamTracking) {
            ExamTracking.disable();
        }
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
        if (csrfToken()) {
            body.set('_csrf', csrfToken());
        }
        fetch(ctx + '/user/diagnostic', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-CSRF-Token': csrfToken()
            },
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
        startTracking();
    }

    function beginAndStart() {
        const body = new URLSearchParams({
            action: 'begin',
            ajax: '1',
            attemptId: attemptId
        });
        if (csrfToken()) {
            body.set('_csrf', csrfToken());
        }
        fetch(ctx + '/user/diagnostic', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-CSRF-Token': csrfToken()
            },
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

    const warningBtn = document.getElementById('integrityWarningBtn');
    if (warningBtn) {
        warningBtn.addEventListener('click', function () {
            document.getElementById('integrityWarning').classList.add('is-hidden');
        });
    }

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
