<html>
<head><title>Home</title></head>
<body>
    <h1>Welcome, ${user.name}!</h1>
    <ul>
        <li><a href="${pageContext.request.contextPath}/users">Manage Users</a></li>
        <li><a href="${pageContext.request.contextPath}/groups">Manage Groups</a></li>
        <li><a href="${pageContext.request.contextPath}/roles">Manage Roles</a></li>
    </ul>
</body>
</html>
