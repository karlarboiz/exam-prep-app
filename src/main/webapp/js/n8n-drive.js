(function () {
    document.querySelectorAll("[data-drive-check]").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var on = btn.getAttribute("data-drive-check") === "all";
            document.querySelectorAll('input[name="driveFileIds"]').forEach(function (el) {
                el.checked = on;
            });
        });
    });
})();
