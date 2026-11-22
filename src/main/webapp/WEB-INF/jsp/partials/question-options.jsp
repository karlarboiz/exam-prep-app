<div class="options">
    <c:forEach var="opt" items="${q.displayOptions}">
        <label class="option-label${not empty fb ? ' is-locked' : ''}${not empty fb and fb.question.correctOption == opt.letter ? ' is-correct-option' : ''}${not empty fb and answers[q.id] == opt.letter and !fb.correct ? ' is-wrong-option' : ''}">
            <input type="radio" name="answer_${q.id}" value="${opt.letter}" data-question-id="${q.id}"
                ${answers[q.id] == opt.letter ? 'checked' : ''}
                ${not empty fb ? 'disabled' : ''}>
            <span class="option-letter">${opt.letter}.</span>
            <span class="option-text"><c:out value="${opt.text}"/></span>
        </label>
    </c:forEach>
</div>
