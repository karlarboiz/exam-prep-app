<c:if test="${not empty imageUrl}">
    <c:set var="resolvedSrc" value="${imageUrl}"/>
    <c:if test="${fn:startsWith(imageUrl, '/')}">
        <c:set var="resolvedSrc" value="${ctx}${imageUrl}"/>
    </c:if>
    <figure class="question-figure" data-question-image>
        <button type="button" class="question-image-trigger" aria-label="Enlarge diagram">
            <img class="question-image"
                 src="${resolvedSrc}"
                 alt="${empty imageAlt ? 'Question diagram' : imageAlt}"
                 loading="${empty imageLoading ? 'lazy' : imageLoading}"
                 decoding="async">
        </button>
        <figcaption class="question-image-hint">Tap image to enlarge</figcaption>
        <p class="question-image-fallback" hidden>Diagram could not be loaded. Continue with the question text.</p>
    </figure>
</c:if>
