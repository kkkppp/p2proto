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
</div>
