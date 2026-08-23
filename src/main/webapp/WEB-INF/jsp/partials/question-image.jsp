<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:if test="${not empty imageUrl}">
    <c:set var="resolvedSrc" value="${imageUrl}"/>
    <c:if test="${fn:startsWith(imageUrl, '/')}">
        <c:set var="resolvedSrc" value="${ctx}${imageUrl}"/>
    </c:if>
    <figure class="question-figure" data-question-image>
        <button type="button" class="question-image-trigger" aria-label="<fmt:message key="image.enlarge"/>">
            <img class="question-image"
                 src="${resolvedSrc}"
                 alt="${empty imageAlt ? 'Question diagram' : imageAlt}"
                 loading="${empty imageLoading ? 'lazy' : imageLoading}"
                 decoding="async">
        </button>
        <figcaption class="question-image-hint"><fmt:message key="image.hint"/></figcaption>
        <p class="question-image-fallback" hidden><fmt:message key="image.fallback"/></p>
    </figure>
</c:if>
