<div id="integrityWarning" class="intro-modal${showReturnWarning ? '' : ' is-hidden'}" role="alertdialog"
     aria-modal="true" aria-labelledby="integrityWarningTitle">
    <div class="intro-modal-backdrop"></div>
    <div class="intro-modal-panel">
        <h2 id="integrityWarningTitle">Stay on this page</h2>
        <p>Leaving the exam page is recorded. This leave was logged.</p>
        <p class="hint">Leaves this attempt: <span id="integrityLeaveCount">${attempt.leaveCount}</span></p>
        <button type="button" id="integrityWarningBtn" class="btn btn-primary">Continue exam</button>
    </div>
</div>
