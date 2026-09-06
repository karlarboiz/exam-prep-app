<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div id="integrityWarning" class="intro-modal${showReturnWarning ? '' : ' is-hidden'}" role="alertdialog"
     aria-modal="true" aria-labelledby="integrityWarningTitle">
    <div class="intro-modal-backdrop"></div>
    <div class="intro-modal-panel">
        <h2 id="integrityWarningTitle"><fmt:message key="integrity.stay"/></h2>
        <p><fmt:message key="integrity.body"/></p>
        <p class="hint"><fmt:message key="integrity.leaves"/> <span id="integrityLeaveCount">${attempt.leaveCount}</span></p>
        <button type="button" id="integrityWarningBtn" class="btn btn-primary"><fmt:message key="integrity.continue"/></button>
    </div>
</div>
