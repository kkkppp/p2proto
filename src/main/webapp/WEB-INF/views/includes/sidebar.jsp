<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!-- Left Sidebar -->
<div class="sidebar">
    <h1>Platform 2</h1>
    <ul>
        <c:forEach var="menuItem" items="${menu}">
            <li class="${menuItem.title == activeMenu ? 'open' : ''}">
                <!-- Parent Menu Item -->
                <a href="#"
                   class="${not empty menuItem.children ? 'has-submenu' : ''}">
                        ${menuItem.title}
                </a>
                <c:if test="${not empty menuItem.children}">
                    <ul>
                        <c:forEach var="subMenu" items="${menuItem.children}">
                            <li>
                                <!-- Submenu Item -->
                                <a href="${pageContext.request.contextPath}${subMenu.url}"
                                   data-url="${pageContext.request.contextPath}${subMenu.url}">
                                        ${subMenu.title}
                                </a>
                            </li>
                        </c:forEach>
                    </ul>
                </c:if>
            </li>
        </c:forEach>
    </ul>

    <!-- Logout Link at the Bottom -->
    <div class="logout-container">
        <a href="${pageContext.request.contextPath}/logout" class="logout-link">Logout</a>
    </div>
</div>

<!-- CSS to position the logout link at the bottom -->
<style>
    .sidebar {
        display: flex;
        flex-direction: column;
        height: 100vh; /* Full height sidebar */
    }

    .logout-container {
        margin-top: auto; /* Pushes logout to the bottom */
        padding: 20px;
    }

    .logout-link {
        display: block;
        text-align: center;
        color: red;
        font-weight: bold;
        text-decoration: none;
    }

    .logout-link:hover {
        text-decoration: underline;
    }
</style>
