<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
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
        <a href="${ctx}/" class="logo"><fmt:message key="app.name"/></a>
        <div class="header-end">
            <div class="lang-switch" role="group" aria-label="<fmt:message key="lang.switch.label"/>">
                <form method="post" action="${ctx}/locale" class="inline-form">
                    <ep:csrf/>
                    <input type="hidden" name="lang" value="tl">
                    <input type="hidden" name="returnTo" value="${currentPath}">
                    <button type="submit"
                            class="lang-btn${appLocale == 'TL' ? ' is-active' : ''}"
                            <c:if test="${appLocale == 'TL'}">aria-current="true"</c:if>>
                        <fmt:message key="lang.tagalog"/>
                    </button>
                </form>
                <form method="post" action="${ctx}/locale" class="inline-form">
                    <ep:csrf/>
                    <input type="hidden" name="lang" value="en">
                    <input type="hidden" name="returnTo" value="${currentPath}">
                    <button type="submit"
                            class="lang-btn${appLocale == 'EN' ? ' is-active' : ''}"
                            <c:if test="${appLocale == 'EN'}">aria-current="true"</c:if>>
                        <fmt:message key="lang.english"/>
                    </button>
                </form>
            </div>
            <c:if test="${not empty currentUser}">
                <button type="button"
                        class="nav-toggle"
                        id="nav-toggle"
                        aria-expanded="false"
                        aria-controls="main-nav"
                        aria-label="<fmt:message key="nav.toggle"/>">
                    <span class="nav-toggle-bar" aria-hidden="true"></span>
                    <span class="nav-toggle-bar" aria-hidden="true"></span>
                    <span class="nav-toggle-bar" aria-hidden="true"></span>
                </button>
                <nav class="main-nav" id="main-nav">
                    <c:choose>
                        <c:when test="${currentUser.role == 'ADMIN'}">
                            <a href="${ctx}/admin/dashboard"><fmt:message key="nav.dashboard"/></a>
                            <a href="${ctx}/admin/subjects"><fmt:message key="nav.subjects"/></a>
                            <a href="${ctx}/admin/questions"><fmt:message key="nav.questions"/></a>
                            <a href="${ctx}/admin/exams"><fmt:message key="nav.exams"/></a>
                            <a href="${ctx}/admin/users"><fmt:message key="nav.users"/></a>
                            <a href="${ctx}/admin/access-grants"><fmt:message key="nav.accessGrants"/></a>
                            <a href="${ctx}/admin/integrity"><fmt:message key="nav.integrity"/></a>
                        </c:when>
                        <c:otherwise>
                            <a href="${ctx}/user/dashboard"><fmt:message key="nav.dashboard"/></a>
                            <a href="${ctx}/user/study-plan"><fmt:message key="nav.studyPlan"/></a>
                            <a href="${ctx}/user/history"><fmt:message key="nav.history"/></a>
                        </c:otherwise>
                    </c:choose>
                    <a href="${ctx}/account"><fmt:message key="nav.account"/></a>
                    <span class="user-badge">${currentUser.username} (<fmt:message key="role.${currentUser.role}"/>)</span>
                    <a href="${ctx}/logout" class="btn btn-outline"><fmt:message key="nav.logout"/></a>
                </nav>
            </c:if>
        </div>
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
