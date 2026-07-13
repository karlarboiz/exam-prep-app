<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:if test="${not empty pageTitle}">${pageTitle} - </c:if>Exam Prep App</title>
    <link rel="stylesheet" href="${ctx}/css/app.css">
</head>
<body>
<header class="site-header">
    <div class="container header-inner">
        <a href="${ctx}/" class="logo">Exam Prep App</a>
        <c:if test="${not empty currentUser}">
            <nav class="main-nav">
                <c:choose>
                    <c:when test="${currentUser.role == 'ADMIN'}">
                        <a href="${ctx}/admin/dashboard">Dashboard</a>
                        <a href="${ctx}/admin/subjects">Subjects</a>
                        <a href="${ctx}/admin/questions">Questions</a>
                        <a href="${ctx}/admin/exams">Exams</a>
                        <a href="${ctx}/admin/users">Users</a>
                    </c:when>
                    <c:otherwise>
                        <a href="${ctx}/user/dashboard">Dashboard</a>
                        <a href="${ctx}/user/history">History</a>
                    </c:otherwise>
                </c:choose>
                <span class="user-badge">${currentUser.username} (${currentUser.role})</span>
                <a href="${ctx}/logout" class="btn btn-outline">Logout</a>
            </nav>
        </c:if>
    </div>
</header>
<main class="container main-content">
