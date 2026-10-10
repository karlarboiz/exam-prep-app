(function () {
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

    document.addEventListener('DOMContentLoaded', toggleDiagnosticFields);
    var diagnostic = document.getElementById('diagnostic');
    if (diagnostic) {
        diagnostic.addEventListener('change', toggleDiagnosticFields);
    }
})();
