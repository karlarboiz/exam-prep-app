(function () {
    var lightbox = null;
    var lastFocus = null;

    function ensureLightbox() {
        if (lightbox) {
            return lightbox;
        }
        lightbox = document.createElement('div');
        lightbox.className = 'image-lightbox is-hidden';
        lightbox.setAttribute('role', 'dialog');
        lightbox.setAttribute('aria-modal', 'true');
        lightbox.setAttribute('aria-label', 'Enlarged diagram');
        lightbox.innerHTML =
            '<div class="image-lightbox-backdrop" data-lightbox-close></div>' +
            '<div class="image-lightbox-panel">' +
            '<button type="button" class="image-lightbox-close" data-lightbox-close aria-label="Close enlarged diagram">Close</button>' +
            '<img class="image-lightbox-img" alt="">' +
            '</div>';
        document.body.appendChild(lightbox);

        lightbox.addEventListener('click', function (event) {
            if (event.target.closest('[data-lightbox-close]')) {
                closeLightbox();
            }
        });
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && !lightbox.classList.contains('is-hidden')) {
                closeLightbox();
            }
        });
        return lightbox;
    }

    function openLightbox(src, alt) {
        var box = ensureLightbox();
        var img = box.querySelector('.image-lightbox-img');
        lastFocus = document.activeElement;
        img.src = src;
        img.alt = alt || 'Question diagram';
        box.classList.remove('is-hidden');
        document.body.classList.add('has-lightbox');
        var closeBtn = box.querySelector('.image-lightbox-close');
        if (closeBtn) {
            closeBtn.focus();
        }
    }

    function closeLightbox() {
        if (!lightbox) {
            return;
        }
        lightbox.classList.add('is-hidden');
        document.body.classList.remove('has-lightbox');
        var img = lightbox.querySelector('.image-lightbox-img');
        if (img) {
            img.removeAttribute('src');
        }
        if (lastFocus && typeof lastFocus.focus === 'function') {
            lastFocus.focus();
        }
    }

    function bindFigure(figure) {
        if (!figure || figure.dataset.imageBound === '1') {
            return;
        }
        figure.dataset.imageBound = '1';
        var img = figure.querySelector('.question-image');
        var trigger = figure.querySelector('.question-image-trigger');
        var fallback = figure.querySelector('.question-image-fallback');
        if (!img) {
            return;
        }

        img.addEventListener('error', function () {
            figure.classList.add('is-broken');
            if (trigger) {
                trigger.disabled = true;
            }
            if (fallback) {
                fallback.hidden = false;
            }
        });

        img.addEventListener('load', function () {
            figure.classList.add('is-loaded');
        });

        if (trigger) {
            trigger.addEventListener('click', function () {
                if (figure.classList.contains('is-broken') || !img.currentSrc && !img.src) {
                    return;
                }
                openLightbox(img.currentSrc || img.src, img.alt);
            });
        }
    }

    function init(root) {
        (root || document).querySelectorAll('[data-question-image]').forEach(bindFigure);
    }

    function prepareCard(card) {
        if (!card) {
            return;
        }
        var img = card.querySelector('.question-image');
        if (img) {
            img.loading = 'eager';
        }
        init(card);
    }

    window.ExamQuestionImages = {
        init: init,
        prepareCard: prepareCard,
        open: openLightbox,
        close: closeLightbox
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () {
            init(document);
        });
    } else {
        init(document);
    }
})();
