(function () {
    'use strict';

    const root = document.getElementById('examShell');
    if (!root) {
        return;
    }

    const ctx = root.dataset.ctx || '';
    const postUrl = root.dataset.postUrl || '';
    const attemptId = root.dataset.attemptId || '';
    let deadline = root.dataset.deadline ? new Date(root.dataset.deadline) : null;
    const secondsPerQuestion = parseInt(root.dataset.secondsPerQuestion || '0', 10);
    const showIntro = root.dataset.showIntro === 'true';
    const failRedirect = root.dataset.failRedirect || '';
    const answerFailRedirect = root.dataset.answerFailRedirect || '';
    const trackingEndpoint = root.dataset.trackingEndpoint || postUrl.replace(ctx, '') || '';
    const leaveCount = parseInt(root.dataset.leaveCount || '0', 10);
    const revealAfterEach = root.dataset.revealAfterEach === 'true';
    const feedbackCorrect = root.dataset.feedbackCorrect || 'Correct';
    const feedbackIncorrect = root.dataset.feedbackIncorrect || 'Incorrect';
    const feedbackCorrectAnswer = root.dataset.feedbackCorrectAnswer || 'Correct answer:';
    const feedbackExplanation = root.dataset.feedbackExplanation || 'Explanation:';

    const examForm = document.getElementById('examForm');
    const submitConfirm = (examForm && examForm.dataset.confirmSubmit) || root.dataset.submitConfirm || '';
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
    let examStarted = !showIntro;
    let timerInterval = null;

    function csrfToken() {
        const el = document.querySelector('input[name="_csrf"]');
        return el ? el.value : '';
    }

    function currentQuestionId() {
        const card = cards[currentIndex];
        return card ? card.getAttribute('data-question-id') : null;
    }

    function formatTime(ms) {
        const totalSecs = Math.max(0, Math.floor(ms / 1000));
        const mins = Math.floor(totalSecs / 60);
        const secs = totalSecs % 60;
        return String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0');
    }

    function updateProgressLabel() {
        if (!progressEl || !examForm) {
            return;
        }
        const pattern = examForm.dataset.questionOf || 'Question {0} of {1}';
        progressEl.textContent = pattern
            .replace('{0}', String(currentIndex + 1))
            .replace('{1}', String(questionCount));
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
        updateProgressLabel();
        if (prevBtn) {
            prevBtn.disabled = currentIndex === 0;
        }
        const isLast = currentIndex === questionCount - 1;
        if (nextBtn) {
            nextBtn.classList.toggle('is-hidden', isLast);
        }
        if (submitBtn) {
            submitBtn.classList.toggle('is-hidden', !isLast);
        }
        questionEndsAt = Date.now() + remainingMs[currentIndex];
        if (questionTimerEl) {
            questionTimerEl.classList.remove('timer-warning', 'timer-expired');
        }
        if (window.ExamQuestionImages) {
            window.ExamQuestionImages.prepareCard(cards[currentIndex]);
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

    function showIntegrityWarning(count) {
        const modal = document.getElementById('integrityWarning');
        const countEl = document.getElementById('integrityLeaveCount');
        if (countEl && typeof count === 'number') {
            countEl.textContent = String(count);
        }
        if (modal) {
            modal.classList.remove('is-hidden');
        }
    }

    function startTracking() {
        if (!window.ExamTracking) {
            return;
        }
        window.ExamTracking.init({
            ctx: ctx,
            attemptId: attemptId,
            endpoint: trackingEndpoint,
            csrfToken: csrfToken(),
            leaveCount: leaveCount,
            getQuestionId: currentQuestionId,
            getRemainingMs: function () {
                return Math.max(0, questionEndsAt - Date.now());
            },
            onReturn: showIntegrityWarning
        });
    }

    function submitExam() {
        if (submitted) {
            return;
        }
        submitted = true;
        if (window.ExamTracking) {
            window.ExamTracking.disable();
        }
        if (examForm) {
            examForm.querySelectorAll('input[type="radio"][disabled]').forEach(function (input) {
                input.disabled = false;
            });
            examForm.submit();
        }
    }

    function updateTimers() {
        if (!examStarted || !deadline || !timerEl || !questionTimerEl) {
            return;
        }
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
        if (overallDiff < 60000) {
            timerEl.classList.add('timer-warning');
        }

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

    function postBody(params) {
        const body = new URLSearchParams(params);
        const token = csrfToken();
        if (token) {
            body.set('_csrf', token);
        }
        return body;
    }

    function postHeaders() {
        const token = csrfToken();
        const headers = {'Content-Type': 'application/x-www-form-urlencoded'};
        if (token) {
            headers['X-CSRF-Token'] = token;
        }
        return headers;
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    }

    function lockQuestionOptions(card, data) {
        const labels = card.querySelectorAll('.option-label');
        labels.forEach(function (label) {
            const input = label.querySelector('input[type="radio"]');
            if (!input) {
                return;
            }
            input.disabled = true;
            label.classList.add('is-locked');
            label.classList.toggle('is-correct-option', input.value === data.correctOption);
            label.classList.toggle('is-wrong-option',
                input.value === data.selectedOption && !data.correct);
        });
        card.setAttribute('data-answered', '1');
    }

    function renderFeedback(card, data) {
        const panel = card.querySelector('.answer-feedback');
        if (!panel) {
            return;
        }
        panel.classList.remove('is-hidden', 'correct', 'incorrect');
        panel.classList.add('review-card', data.correct ? 'correct' : 'incorrect');
        let html = '<p class="feedback-verdict">'
            + escapeHtml(data.correct ? feedbackCorrect : feedbackIncorrect) + '</p>';
        if (!data.correct) {
            html += '<p><strong>' + escapeHtml(feedbackCorrectAnswer) + '</strong> '
                + escapeHtml(data.correctOption)
                + ' — ' + escapeHtml(data.correctText || '') + '</p>';
        }
        if (data.explanation) {
            html += '<p><strong>' + escapeHtml(feedbackExplanation) + '</strong> '
                + escapeHtml(data.explanation) + '</p>';
        }
        panel.innerHTML = html;
        lockQuestionOptions(card, data);
    }

    function saveAnswer(questionId, option) {
        if (!examStarted) {
            return;
        }
        const card = document.querySelector('.question-card[data-question-id="' + questionId + '"]');
        if (card && card.getAttribute('data-answered') === '1' && revealAfterEach) {
            return;
        }
        fetch(postUrl, {
            method: 'POST',
            headers: postHeaders(),
            body: postBody({
                action: 'answer',
                ajax: '1',
                attemptId: attemptId,
                questionId: String(questionId),
                selectedOption: option
            }).toString()
        }).then(function (res) {
            if (res.redirected || res.status === 403) {
                window.location.href = res.url || answerFailRedirect || postUrl;
                return;
            }
            if (revealAfterEach && res.status === 200) {
                return res.json().then(function (data) {
                    if (card) {
                        renderFeedback(card, data);
                    }
                });
            }
        }).catch(function () { /* keep selection local if save fails briefly */ });
    }

    function startExam(newDeadlineIso) {
        if (examStarted && timerInterval) {
            return;
        }
        examStarted = true;
        if (newDeadlineIso) {
            deadline = new Date(newDeadlineIso);
        }
        const intro = document.getElementById('diagnosticIntro');
        if (intro) {
            intro.classList.add('is-hidden');
        }
        root.classList.remove('is-intro-pending');
        showQuestion(0);
        if (!timerInterval) {
            timerInterval = setInterval(updateTimers, 1000);
        }
        updateTimers();
        startTracking();
    }

    function beginAndStart() {
        fetch(postUrl, {
            method: 'POST',
            headers: postHeaders(),
            body: postBody({
                action: 'begin',
                ajax: '1',
                attemptId: attemptId
            }).toString()
        }).then(function (res) {
            if (res.redirected) {
                window.location.href = res.url || failRedirect;
                return null;
            }
            if (!res.ok) {
                window.location.href = failRedirect;
                return null;
            }
            return res.json();
        }).then(function (data) {
            if (data && data.deadline) {
                startExam(data.deadline);
            }
        }).catch(function () {
            window.location.href = failRedirect;
        });
    }

    if (examForm) {
        examForm.addEventListener('change', function (event) {
            const input = event.target;
            if (!input.matches('input[type="radio"][data-question-id]')) {
                return;
            }
            saveAnswer(input.dataset.questionId, input.value);
        });
        examForm.addEventListener('submit', function (event) {
            if (submitConfirm && !window.confirm(submitConfirm)) {
                event.preventDefault();
            }
        });
    }

    if (prevBtn) {
        prevBtn.addEventListener('click', goPrev);
    }
    if (nextBtn) {
        nextBtn.addEventListener('click', goNext);
    }

    const warningBtn = document.getElementById('integrityWarningBtn');
    if (warningBtn) {
        warningBtn.addEventListener('click', function () {
            const modal = document.getElementById('integrityWarning');
            if (modal) {
                modal.classList.add('is-hidden');
            }
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
            if (beginning) {
                return;
            }
            beginning = true;
            if (introTimer) {
                clearInterval(introTimer);
            }
            if (startBtn) {
                startBtn.disabled = true;
            }
            beginAndStart();
        }

        if (startBtn) {
            startBtn.addEventListener('click', requestBegin);
        }
        introTimer = setInterval(tickIntro, 1000);
    } else if (deadline) {
        startExam();
    }
})();
