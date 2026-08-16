<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title><c:if test="${not empty pageTitle}">${pageTitle} - </c:if>Exam Prep App</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@500;600;700&family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/css/app.css">
</head>
<body>
<header class="site-header">
    <div class="container header-inner">
        <a href="${ctx}/" class="logo">Exam Prep App</a>
        <c:if test="${not empty currentUser}">
            <button type="button"
                    class="nav-toggle"
                    id="nav-toggle"
                    aria-expanded="false"
                    aria-controls="main-nav"
                    aria-label="Toggle navigation">
                <span class="nav-toggle-bar" aria-hidden="true"></span>
                <span class="nav-toggle-bar" aria-hidden="true"></span>
                <span class="nav-toggle-bar" aria-hidden="true"></span>
            </button>
            <nav class="main-nav" id="main-nav">
                <c:choose>
                    <c:when test="${currentUser.role == 'ADMIN'}">
                        <a href="${ctx}/admin/dashboard">Dashboard</a>
                        <a href="${ctx}/admin/subjects">Subjects</a>
                        <a href="${ctx}/admin/questions">Questions</a>
                        <a href="${ctx}/admin/exams">Exams</a>
                        <a href="${ctx}/admin/users">Users</a>
                        <a href="${ctx}/admin/access-grants">Access grants</a>
                        <a href="${ctx}/admin/integrity">Integrity</a>
                    </c:when>
                    <c:otherwise>
                        <a href="${ctx}/user/dashboard">Dashboard</a>
                        <a href="${ctx}/user/history">History</a>
                    </c:otherwise>
                </c:choose>
                <a href="${ctx}/account">Account</a>
                <span class="user-badge">${currentUser.username} (${currentUser.role})</span>
                <a href="${ctx}/logout" class="btn btn-outline">Logout</a>
            </nav>
        </c:if>
    </div>
</header>
<script>
(function () {
    var toggle = document.getElementById('nav-toggle');
    var nav = document.getElementById('main-nav');
    if (!toggle || !nav) return;

    toggle.addEventListener('click', function () {
        var open = nav.classList.toggle('is-open');
        toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        toggle.classList.toggle('is-active', open);
    });

    nav.querySelectorAll('a').forEach(function (link) {
        link.addEventListener('click', function () {
            nav.classList.remove('is-open');
            toggle.classList.remove('is-active');
            toggle.setAttribute('aria-expanded', 'false');
        });
    });
})();
</script>
<main class="container main-content">
