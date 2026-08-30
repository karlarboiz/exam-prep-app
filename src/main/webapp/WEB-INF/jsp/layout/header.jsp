<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="navPath" value="${requestScope['jakarta.servlet.forward.servlet_path']}"/>
<c:if test="${empty navPath}">
    <c:set var="navPath" value="${pageContext.request.servletPath}"/>
</c:if>
<!DOCTYPE html>
<html lang="${appLocale.htmlLang}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title><c:if test="${not empty pageTitleKey}"><fmt:message key="${pageTitleKey}"/> - </c:if><fmt:message key="app.name"/></title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@500;600;700&family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/css/app.css">
</head>
<body>
<header class="site-header">
    <div class="container header-inner">
        <a href="${ctx}/" class="logo">
            <span class="logo-mark" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                    <path d="M4 4.5A2.5 2.5 0 0 1 6.5 7H20"/>
                    <path d="M20 22V7"/>
                    <path d="M6.5 7v10"/>
                </svg>
            </span>
            Exam Prep
        </a>
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
            <div class="header-nav" id="main-nav">
                <nav class="nav-links" aria-label="Main">
                    <c:choose>
                        <c:when test="${currentUser.role == 'ADMIN'}">
                            <a href="${ctx}/admin/dashboard" class="${navPath == '/admin/dashboard' ? 'is-active' : ''}">Dashboard</a>
                            <a href="${ctx}/admin/subjects" class="${navPath == '/admin/subjects' ? 'is-active' : ''}">Subjects</a>
                            <a href="${ctx}/admin/questions" class="${navPath == '/admin/questions' ? 'is-active' : ''}">Questions</a>
                            <a href="${ctx}/admin/exams" class="${navPath == '/admin/exams' ? 'is-active' : ''}">Exams</a>
                            <a href="${ctx}/admin/users" class="${navPath == '/admin/users' ? 'is-active' : ''}">Users</a>
                            <a href="${ctx}/admin/access-grants" class="${navPath == '/admin/access-grants' ? 'is-active' : ''}" title="Access grants">Access</a>
                            <a href="${ctx}/admin/integrity" class="${navPath == '/admin/integrity' ? 'is-active' : ''}">Integrity</a>
                            <a href="${ctx}/admin/n8n" class="${navPath == '/admin/n8n' ? 'is-active' : ''}">n8n</a>
                        </c:when>
                        <c:otherwise>
                            <a href="${ctx}/user/dashboard" class="${navPath == '/user/dashboard' ? 'is-active' : ''}">
                                <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <rect x="3" y="3" width="7" height="7" rx="1"/>
                                    <rect x="14" y="3" width="7" height="7" rx="1"/>
                                    <rect x="3" y="14" width="7" height="7" rx="1"/>
                                    <rect x="14" y="14" width="7" height="7" rx="1"/>
                                </svg>
                                Dashboard
                            </a>
                            <a href="${ctx}/user/study-plan" class="${navPath == '/user/study-plan' ? 'is-active' : ''}">
                                <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <rect x="3" y="4" width="18" height="18" rx="2"/>
                                    <path d="M16 2v4M8 2v4M3 10h18"/>
                                </svg>
                                Study plan
                            </a>
                            <a href="${ctx}/user/history" class="${navPath == '/user/history' ? 'is-active' : ''}">
                                <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <circle cx="12" cy="12" r="9"/>
                                    <path d="M12 7v5l3 2"/>
                                </svg>
                                History
                            </a>
                        </c:otherwise>
                    </c:choose>
                </nav>
                <div class="account-menu">
                    <a href="${ctx}/account" class="account-chip${currentUser.role == 'ADMIN' ? ' is-admin' : ''}${navPath == '/account' ? ' is-active' : ''}">
                        <span class="account-avatar" aria-hidden="true">
                            <c:choose>
                                <c:when test="${not empty currentUser.username}">
                                    ${fn:toUpperCase(fn:substring(currentUser.username, 0, 1))}
                                </c:when>
                                <c:otherwise>
                                    <svg class="account-avatar-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M20 21a8 8 0 0 0-16 0"/>
                                        <circle cx="12" cy="8" r="4"/>
                                    </svg>
                                </c:otherwise>
                            </c:choose>
                        </span>
                        <span class="account-text">
                            <span class="account-name"><c:out value="${currentUser.username}"/></span>
                            <span class="account-role">${currentUser.role == 'ADMIN' ? 'Admin' : 'User'}</span>
                        </span>
                    </a>
                    <a href="${ctx}/logout" class="account-logout" aria-label="Log out">
                        <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                            <polyline points="16 17 21 12 16 7"/>
                            <line x1="21" y1="12" x2="9" y2="12"/>
                        </svg>
                        <span class="account-logout-label">Log out</span>
                    </a>
                </div>
            </div>
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
})();
</script>
<main class="container main-content">
