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

    var optionList = document.getElementById('optionOrderList');
    var correctSelect = document.getElementById('correctOption');
    if (optionList) {
        var letters = ['A', 'B', 'C', 'D'];
        var labelTpl = optionList.getAttribute('data-option-label-tpl') || 'Option {0}';
        var activeRow = null;
        var pointerId = null;
        var dragging = false;
        var startY = 0;
        var usingPointer = false;

        function optionRows() {
            return Array.prototype.slice.call(optionList.querySelectorAll('.option-order-row'));
        }

        function applyOptionOrder() {
            var correctLetter = correctSelect ? correctSelect.value : '';
            var correctRow = null;
            optionRows().forEach(function (row) {
                if (row.getAttribute('data-letter') === correctLetter) {
                    correctRow = row;
                }
            });

            optionRows().forEach(function (row, index) {
                var letter = letters[index];
                var field = row.querySelector('input[type="text"]');
                var letterLabel = row.querySelector('.option-letter');
                row.setAttribute('data-letter', letter);
                if (letterLabel) {
                    letterLabel.textContent = letter;
                    letterLabel.setAttribute('for', 'option' + letter);
                }
                if (field) {
                    field.id = 'option' + letter;
                    field.name = 'option' + letter;
                    field.setAttribute('aria-label', labelTpl.replace('{0}', letter));
                }
            });

            if (correctSelect && correctRow) {
                correctSelect.value = correctRow.getAttribute('data-letter');
            }
        }

        function moveRowToPoint(clientX, clientY) {
            if (!activeRow) {
                return;
            }
            activeRow.classList.add('is-pointer-pass');
            var el = document.elementFromPoint(clientX, clientY);
            activeRow.classList.remove('is-pointer-pass');
            var target = el && el.closest ? el.closest('.option-order-row') : null;
            if (!target || target === activeRow || !optionList.contains(target)) {
                return;
            }
            var rect = target.getBoundingClientRect();
            if ((clientY - rect.top) > rect.height / 2) {
                target.after(activeRow);
            } else {
                target.before(activeRow);
            }
        }

        function beginDrag(row, clientY, id) {
            activeRow = row;
            pointerId = id;
            dragging = false;
            startY = clientY;
        }

        function continueDrag(clientX, clientY) {
            if (!activeRow) {
                return;
            }
            if (!dragging && Math.abs(clientY - startY) < 4) {
                return;
            }
            dragging = true;
            activeRow.classList.add('is-dragging');
            moveRowToPoint(clientX, clientY);
        }

        function finishDrag() {
            var handle = activeRow && activeRow.querySelector('.option-drag-handle');
            if (handle && pointerId != null && handle.releasePointerCapture) {
                try {
                    handle.releasePointerCapture(pointerId);
                } catch (ignored) {
                    // Already released
                }
            }
            if (activeRow) {
                activeRow.classList.remove('is-dragging');
                if (dragging) {
                    applyOptionOrder();
                }
            }
            activeRow = null;
            pointerId = null;
            dragging = false;
            usingPointer = false;
        }

        optionList.addEventListener('pointerdown', function (event) {
            var handle = event.target.closest('.option-drag-handle');
            var row = handle && handle.closest('.option-order-row');
            if (!row || event.button !== 0) {
                return;
            }
            usingPointer = true;
            beginDrag(row, event.clientY, event.pointerId);
            if (handle.setPointerCapture) {
                handle.setPointerCapture(event.pointerId);
            }
        });

        optionList.addEventListener('pointermove', function (event) {
            if (!usingPointer || pointerId !== event.pointerId) {
                return;
            }
            continueDrag(event.clientX, event.clientY);
        });

        optionList.addEventListener('pointerup', function (event) {
            if (!usingPointer || pointerId !== event.pointerId) {
                return;
            }
            finishDrag();
        });
        optionList.addEventListener('pointercancel', function (event) {
            if (!usingPointer || pointerId !== event.pointerId) {
                return;
            }
            finishDrag();
        });

        optionList.addEventListener('mousedown', function (event) {
            if (usingPointer) {
                return;
            }
            var handle = event.target.closest('.option-drag-handle');
            var row = handle && handle.closest('.option-order-row');
            if (!row || event.button !== 0) {
                return;
            }
            event.preventDefault();
            beginDrag(row, event.clientY, 'mouse');
        });

        document.addEventListener('mousemove', function (event) {
            if (usingPointer || pointerId !== 'mouse') {
                return;
            }
            continueDrag(event.clientX, event.clientY);
        });

        document.addEventListener('mouseup', function () {
            if (usingPointer || pointerId !== 'mouse') {
                return;
            }
            finishDrag();
        });

        optionList.addEventListener('dragstart', function (event) {
            var handle = event.target.closest('.option-drag-handle');
            var row = (handle && handle.closest('.option-order-row'))
                || event.target.closest('.option-order-row');
            if (!row) {
                return;
            }
            beginDrag(row, event.clientY, 'dnd');
            dragging = true;
            row.classList.add('is-dragging');
            if (event.dataTransfer) {
                event.dataTransfer.effectAllowed = 'move';
                event.dataTransfer.setData('text/plain', row.getAttribute('data-letter') || '');
            }
        });

        optionList.addEventListener('dragover', function (event) {
            if (pointerId !== 'dnd' || !activeRow) {
                return;
            }
            event.preventDefault();
            moveRowToPoint(event.clientX, event.clientY);
        });

        optionList.addEventListener('drop', function (event) {
            if (pointerId !== 'dnd') {
                return;
            }
            event.preventDefault();
            finishDrag();
        });

        optionList.addEventListener('dragend', function () {
            if (pointerId !== 'dnd') {
                return;
            }
            finishDrag();
        });

        optionList.addEventListener('keydown', function (event) {
            if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') {
                return;
            }
            var handle = event.target.closest('.option-drag-handle');
            var row = handle && handle.closest('.option-order-row');
            if (!row) {
                return;
            }
            event.preventDefault();
            if (event.key === 'ArrowUp' && row.previousElementSibling) {
                optionList.insertBefore(row, row.previousElementSibling);
            } else if (event.key === 'ArrowDown' && row.nextElementSibling) {
                optionList.insertBefore(row.nextElementSibling, row);
            }
            applyOptionOrder();
            handle.focus();
        });

        function optionRows() {
            return Array.prototype.slice.call(optionList.querySelectorAll('.option-order-row'));
        }

        function applyOptionOrder() {
            var correctLetter = correctSelect ? correctSelect.value : '';
            var correctRow = null;
            optionRows().forEach(function (row) {
                if (row.getAttribute('data-letter') === correctLetter) {
                    correctRow = row;
                }
            });

            optionRows().forEach(function (row, index) {
                var letter = letters[index];
                var field = row.querySelector('input[type="text"]');
                var letterLabel = row.querySelector('.option-letter');
                row.setAttribute('data-letter', letter);
                if (letterLabel) {
                    letterLabel.textContent = letter;
                    letterLabel.setAttribute('for', 'option' + letter);
                }
                if (field) {
                    field.id = 'option' + letter;
                    field.name = 'option' + letter;
                    field.setAttribute('aria-label', labelTpl.replace('{0}', letter));
                }
            });

            if (correctSelect && correctRow) {
                correctSelect.value = correctRow.getAttribute('data-letter');
            }
        }

        function moveRowToPoint(clientX, clientY) {
            if (!activeRow) {
                return;
            }
            activeRow.classList.add('is-pointer-pass');
            var el = document.elementFromPoint(clientX, clientY);
            activeRow.classList.remove('is-pointer-pass');
            var target = el && el.closest ? el.closest('.option-order-row') : null;
            if (!target || target === activeRow || !optionList.contains(target)) {
                return;
            }
            var rect = target.getBoundingClientRect();
            if ((clientY - rect.top) > rect.height / 2) {
                target.after(activeRow);
            } else {
                target.before(activeRow);
            }
        }

        function endPointerDrag(event) {
            if (pointerId !== event.pointerId) {
                return;
            }
            var handle = activeRow && activeRow.querySelector('.option-drag-handle');
            if (handle && handle.releasePointerCapture) {
                try {
                    handle.releasePointerCapture(event.pointerId);
                } catch (ignored) {
                    // Already released
                }
            }
            if (activeRow) {
                activeRow.classList.remove('is-dragging');
                if (dragging) {
                    applyOptionOrder();
                }
            }
            activeRow = null;
            pointerId = null;
            dragging = false;
        }

        optionList.addEventListener('pointerdown', function (event) {
            var handle = event.target.closest('.option-drag-handle');
            var row = handle && handle.closest('.option-order-row');
            if (!row || event.button !== 0) {
                return;
            }
            activeRow = row;
            pointerId = event.pointerId;
            dragging = false;
            startY = event.clientY;
            if (handle.setPointerCapture) {
                handle.setPointerCapture(event.pointerId);
            }
        });

        optionList.addEventListener('pointermove', function (event) {
            if (pointerId !== event.pointerId || !activeRow) {
                return;
            }
            if (!dragging && Math.abs(event.clientY - startY) < 4) {
                return;
            }
            dragging = true;
            activeRow.classList.add('is-dragging');
            moveRowToPoint(event.clientX, event.clientY);
        });

        optionList.addEventListener('pointerup', endPointerDrag);
        optionList.addEventListener('pointercancel', endPointerDrag);

        optionList.addEventListener('keydown', function (event) {
            if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') {
                return;
            }
            var handle = event.target.closest('.option-drag-handle');
            var row = handle && handle.closest('.option-order-row');
            if (!row) {
                return;
            }
            event.preventDefault();
            if (event.key === 'ArrowUp' && row.previousElementSibling) {
                optionList.insertBefore(row, row.previousElementSibling);
            } else if (event.key === 'ArrowDown' && row.nextElementSibling) {
                optionList.insertBefore(row.nextElementSibling, row);
            }
            applyOptionOrder();
            handle.focus();
        });
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
