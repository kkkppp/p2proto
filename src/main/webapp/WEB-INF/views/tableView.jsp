<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
  <title>User List</title>
</head>
<body>
<h2>Users</h2>
<a href="${pageContext.request.contextPath}/users/create">Create New User</a>
<br/><br/>

<!--
    We'll combine allFields (the complete list of columns) and fieldsToRender
    (the optional parameter from the request) in a single "renderFields" variable.
-->
<c:set var="renderFields">
  <c:choose>
    <!-- If fieldsToRender is not empty, use that -->
    <c:when test="${not empty fieldsToRender}">${fieldsToRender}</c:when>
    <!-- Otherwise, fall back to allFields -->
    <c:otherwise>${allFields}</c:otherwise>
  </c:choose>
</c:set>

<table border="1">
  <tr>
    <!-- Dynamically render a <th> for each field we are showing -->
    <c:forEach var="field" items="${renderFields}">
      <th>${field}</th>
    </c:forEach>
    <th>Actions</th>
  </tr>
  <!-- For each user (a Map<String, Object>), loop over the fields and output the value -->
  <c:forEach var="user" items="${users}">
    <tr>
      <c:forEach var="field" items="${renderFields}">
        <!-- user[field] will retrieve the Map value for the given column name -->
        <td>${user[field]}</td>
      </c:forEach>
      <!-- Example: "id" is the primary key in this table -->
      <td>
        <a href="${pageContext.request.contextPath}/users/edit/${user['id']}">Edit</a> |
        <a href="${pageContext.request.contextPath}/users/delete/${user['id']}"
           onclick="return confirm('Are you sure?');">Delete</a>
      </td>
    </tr>
  </c:forEach>
</table>
</body>
</html>
