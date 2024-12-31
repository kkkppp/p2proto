<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<head>
    <title>Edit User</title>
</head>
<body>
<h2>Edit User</h2>
<form:form method="POST" action="${pageContext.request.contextPath}/users/save" modelAttribute="user">
    <form:hidden path="id" />
    <table>
        <tr>
            <td>Username:</td>
            <td><form:input path="username" /></td>
        </tr>
        <tr>
            <td>Email:</td>
            <td><form:input path="email" /></td>
        </tr>
        <tr>
            <td>First Name:</td>
            <td><form:input path="firstName" /></td>
        </tr>
        <tr>
            <td>Last Name:</td>
            <td><form:input path="lastName" /></td>
        </tr>
        <!-- Add more fields as necessary -->
        <tr>
            <td colspan="2">
                <input type="submit" value="Save" />
                <a href="${pageContext.request.contextPath}/users">Cancel</a>
            </td>
        </tr>
    </table>
</form:form>
</body>
</html>
