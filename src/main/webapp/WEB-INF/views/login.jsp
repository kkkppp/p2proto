<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head><title>Login</title></head>
<body>
    <form action="${loginUrl}" method="post">
        <label>Username:</label><input type="text" name="username"/><br/>
        <label>Password:</label><input type="password" name="password"/><br/>
        <input type="submit" value="Login"/>
    </form>
</body>
</html>
