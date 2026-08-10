<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="Login" scope="request"/>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="auth-card">
    <h1>Login</h1>
    <div class="alert alert-error${empty error ? ' is-hidden' : ''}" id="login-error" role="alert">
        <c:out value="${error}"/>
    </div>
    <form method="post" action="${ctx}/login" class="form" id="login-form" novalidate>
        <div class="form-group">
            <label for="username">Username</label>
            <input type="text" id="username" name="username" required autofocus autocomplete="username">
        </div>
        <div class="form-group">
            <label for="password">Password</label>
            <input type="password" id="password" name="password" required autocomplete="current-password">
        </div>
        <button type="submit" class="btn btn-primary" id="login-submit">Login</button>
    </form>
    <p class="auth-link">New account? Use the registration link from your purchase (includes an access token).</p>
    <p class="hint">Default admin: <code>admin</code> / <code>admin123</code></p>
</div>
<script>
(function () {
    var form = document.getElementById('login-form');
    var errorEl = document.getElementById('login-error');
    var submitBtn = document.getElementById('login-submit');
    if (!form || !errorEl || !submitBtn) return;

    function showError(message) {
        errorEl.textContent = message;
        errorEl.classList.remove('is-hidden');
    }

    form.addEventListener('submit', function (event) {
        var username = (form.username.value || '').trim();
        var password = form.password.value || '';

        if (!username || !password) {
            event.preventDefault();
            showError('Username and password are required');
            if (!username) {
                form.username.focus();
            } else {
                form.password.focus();
            }
            return;
        }

        submitBtn.disabled = true;
        submitBtn.textContent = 'Signing in…';
    });
})();
</script>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
