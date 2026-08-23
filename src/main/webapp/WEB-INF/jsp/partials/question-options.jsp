<div class="options">
    <c:forEach var="opt" items="${q.displayOptions}">
        <label class="option-label">
            <input type="radio" name="answer_${q.id}" value="${opt.letter}"
                ${answers[q.id] == opt.letter ? 'checked' : ''}
                onchange="saveAnswer(${q.id}, '${opt.letter}')">
            <span class="option-letter">${opt.letter}.</span>
            <span class="option-text"><c:out value="${opt.text}"/></span>
        </label>
    </c:forEach>
</div>
