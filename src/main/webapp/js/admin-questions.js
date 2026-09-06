(function () {
    var script = document.currentScript;
    var ctx = (script && script.getAttribute('data-context-path')) || '';

    var input = document.getElementById('imageUrl');
    var preview = document.getElementById('imagePreview');
    var img = document.getElementById('imagePreviewImg');
    if (input && preview && img) {
        function resolveSrc(value) {
            var url = (value || '').trim();
            if (!url) return '';
            if (url.charAt(0) === '/') return ctx + url;
            return url;
        }

        function refreshPreview() {
            var src = resolveSrc(input.value);
            preview.classList.remove('is-broken', 'is-loaded');
            if (!src) {
                preview.classList.add('is-hidden');
                img.removeAttribute('src');
                return;
            }
            preview.classList.remove('is-hidden');
            img.src = src;
            if (window.ExamQuestionImages) {
                delete preview.dataset.imageBound;
                ExamQuestionImages.init(preview.parentNode);
            }
        }

        input.addEventListener('input', refreshPreview);
        refreshPreview();
    }

    var form = document.getElementById('batchDeleteForm');
    var selectAll = document.getElementById('selectAllQuestions');
    if (!form || !selectAll) return;

    function questionChecks() {
        return Array.prototype.slice.call(document.querySelectorAll('input.question-check'));
    }

    function syncSelectAll() {
        var boxes = questionChecks();
        var checked = boxes.filter(function (box) { return box.checked; }).length;
        selectAll.checked = boxes.length > 0 && checked === boxes.length;
        selectAll.indeterminate = checked > 0 && checked < boxes.length;
    }

    selectAll.addEventListener('change', function () {
        questionChecks().forEach(function (box) {
            box.checked = selectAll.checked;
        });
        selectAll.indeterminate = false;
    });

    questionChecks().forEach(function (box) {
        box.addEventListener('change', syncSelectAll);
    });

    form.addEventListener('submit', function (event) {
        var count = questionChecks().filter(function (box) { return box.checked; }).length;
        if (count === 0) {
            event.preventDefault();
            alert(form.getAttribute('data-select-none'));
            return;
        }
        var confirmTpl = form.getAttribute('data-confirm-tpl') || '';
        if (!confirm(confirmTpl.replace('{0}', String(count)))) {
            event.preventDefault();
        }
    });
})();
