<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>User List</title>
</head>
<body>
<h2>Users</h2>
<a href="${pageContext.request.contextPath}/users/create">Create New User</a>
<table border="1">
    <tr>
        <th>Username</th>
        <th>Email</th>
        <th>First Name</th>
        <th>Last Name</th>
        <th>Actions</th>
    </tr>
    <c:forEach var="user" items="${users}">
        <tr>
            <td>${user.username}</td>
            <td>${user.email}</td>
            <td>${user.first_name}</td>
            <td>${user.last_name}</td>
            <td>
                <a href="${pageContext.request.contextPath}/users/edit/${user.id}">Edit</a> |
                <a href="${pageContext.request.contextPath}/users/delete/${user.id}" onclick="return confirm('Are you sure?');">Delete</a>
            </td>
        </tr>
    </c:forEach>
</table>
</body>
</html>
