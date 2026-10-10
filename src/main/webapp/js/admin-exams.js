(function () {
    'use strict';

    function toggleDiagnosticFields() {
        var diagnostic = document.getElementById('diagnostic');
        var questionsPerSubjectGroup = document.getElementById('questionsPerSubjectGroup');
        var questionPickerGroup = document.getElementById('questionPickerGroup');
        if (!diagnostic || !questionsPerSubjectGroup || !questionPickerGroup) {
            return;
        }
        var isDiagnostic = diagnostic.checked;
        questionsPerSubjectGroup.style.display = isDiagnostic ? '' : 'none';
        questionPickerGroup.style.display = isDiagnostic ? 'none' : '';
    }

    function moveExamQuestion(btn, dir) {
        var row = btn.closest('.exam-question-row');
        if (!row) {
            return;
        }
        var sibling = dir < 0 ? row.previousElementSibling : row.nextElementSibling;
        if (!sibling) {
            return;
        }
        if (dir < 0) {
            row.parentNode.insertBefore(row, sibling);
        } else {
            row.parentNode.insertBefore(sibling, row);
        }
    }

    function onExamQuestionCheck(cb) {
        var row = cb.closest('.exam-question-row');
        var list = row && row.parentNode;
        if (!row || !list) {
            return;
        }
        if (cb.checked) {
            var lastChecked = null;
            list.querySelectorAll('.exam-question-row').forEach(function (r) {
                var check = r.querySelector('.exam-q-check');
                if (check && check.checked && r !== row) {
                    lastChecked = r;
                }
            });
            if (lastChecked) {
                lastChecked.after(row);
            } else {
                list.insertBefore(row, list.firstChild);
            }
        } else {
            list.appendChild(row);
        }
    }

    function initExamFormActions() {
        var examForm = document.getElementById('examForm');
        if (!examForm) {
            return;
        }
        examForm.addEventListener('change', function (event) {
            var target = event.target;
            if (target.classList && target.classList.contains('exam-q-check')) {
                onExamQuestionCheck(target);
            }
        });
        examForm.addEventListener('click', function (event) {
            var btn = event.target.closest('button[data-exam-move]');
            if (!btn) {
                return;
            }
            event.preventDefault();
            var dir = parseInt(btn.getAttribute('data-exam-move'), 10);
            moveExamQuestion(btn, dir);
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        toggleDiagnosticFields();
        initExamFormActions();
    });

    var diagnostic = document.getElementById('diagnostic');
    if (diagnostic) {
        diagnostic.addEventListener('change', toggleDiagnosticFields);
    }
})();
