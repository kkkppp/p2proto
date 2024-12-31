<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html>
<head>
    <title>Create User</title>
</head>
<body>
<h2>Create New User</h2>
<form:form method="POST" action="${pageContext.request.contextPath}/users/save" modelAttribute="user">
    <table>
        <tr>
            <td>Username:</td>
            <td>
                <form:input path="username" />
                <form:errors path="username" cssClass="error" />
            </td>
        </tr>
        <tr>
            <td>Email:</td>
            <td>
                <form:input path="email" />
                <form:errors path="email" cssClass="error" />
            </td>
        </tr>
        <tr>
            <td>First Name:</td>
            <td>
                <form:input path="firstName" />
                <form:errors path="firstName" cssClass="error" />
            </td>
        </tr>
        <tr>
            <td>Last Name:</td>
            <td>
                <form:input path="lastName" />
                <form:errors path="lastName" cssClass="error" />
            </td>
        </tr>
        <tr>
            <td>Password:</td>
            <td>
                <form:input path="password" />
                <form:errors path="password" cssClass="error" />
            </td>
        </tr>
        <!-- Add more fields with validation as necessary -->
        <tr>
            <td colspan="2">
                <input type="submit" value="Create" />
                <a href="${pageContext.request.contextPath}/users">Cancel</a>
            </td>
        </tr>
    </table>
</form:form>
<style>
    .error {
        color: red;
    }
</style>
</body>
</html>
