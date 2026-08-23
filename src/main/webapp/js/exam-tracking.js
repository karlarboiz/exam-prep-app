(function (global) {
    'use strict';

    var state = {
        enabled: false,
        away: false,
        submitted: false,
        leaveCount: 0,
        cfg: null
    };

    function questionId() {
        return state.cfg.getQuestionId ? state.cfg.getQuestionId() : null;
    }

    function remainingMs() {
        return state.cfg.getRemainingMs ? state.cfg.getRemainingMs() : 0;
    }

    function post(eventType, beacon) {
        var qid = questionId();
        if (!qid) {
            return Promise.resolve(null);
        }
        var body = new URLSearchParams({
            action: 'behavior',
            ajax: '1',
            attemptId: String(state.cfg.attemptId),
            questionId: String(qid),
            eventType: eventType,
            remainingQuestionMs: String(Math.max(0, Math.floor(remainingMs()))),
            _csrf: state.cfg.csrfToken || ''
        });
        var url = state.cfg.ctx + state.cfg.endpoint;
        if (beacon && navigator.sendBeacon) {
            navigator.sendBeacon(url, new Blob([body.toString()], {
                type: 'application/x-www-form-urlencoded'
            }));
            return Promise.resolve(null);
        }
        var headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
        if (state.cfg.csrfToken) {
            headers['X-CSRF-Token'] = state.cfg.csrfToken;
        }
        return fetch(url, {
            method: 'POST',
            headers: headers,
            body: body.toString(),
            keepalive: !!beacon
        }).then(function (res) {
            if (!res.ok) {
                return null;
            }
            return res.json();
        }).catch(function () {
            return null;
        });
    }

    function leave(beacon) {
        if (!state.enabled || state.submitted || state.away) {
            return;
        }
        if (!questionId()) {
            return;
        }
        state.away = true;
        post('LEAVE', beacon);
    }

    function returned() {
        if (!state.enabled || state.submitted || !state.away) {
            return;
        }
        state.away = false;
        post('RETURN', false).then(function (data) {
            if (data && typeof data.leaveCount === 'number') {
                state.leaveCount = data.leaveCount;
            }
            if (state.cfg.onReturn) {
                state.cfg.onReturn(state.leaveCount);
            }
        });
    }

    function bindNavLeaves() {
        function onNav() {
            leave(true);
        }
        var nav = document.getElementById('main-nav');
        if (nav) {
            nav.querySelectorAll('a').forEach(function (link) {
                link.addEventListener('click', onNav);
            });
        }
        var logo = document.querySelector('.logo');
        if (logo) {
            logo.addEventListener('click', onNav);
        }
    }

    function init(cfg) {
        if (state.enabled) {
            return;
        }
        state.cfg = cfg;
        state.enabled = true;
        state.away = false;
        state.submitted = false;
        state.leaveCount = cfg.leaveCount || 0;

        document.addEventListener('visibilitychange', function () {
            if (document.hidden) {
                leave(false);
            } else {
                returned();
            }
        });
        window.addEventListener('blur', function () {
            leave(false);
        });
        window.addEventListener('focus', function () {
            returned();
        });
        window.addEventListener('pagehide', function () {
            leave(true);
        });
        window.addEventListener('beforeunload', function () {
            leave(true);
        });
        bindNavLeaves();
    }

    function disable() {
        state.submitted = true;
        state.enabled = false;
    }

    global.ExamTracking = {
        init: init,
        disable: disable
    };
})(window);
