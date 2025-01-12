<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!-- Users List Partial View -->
<h2>Users</h2>
<a href="${pageContext.request.contextPath}/table/${tableName}/create"
   data-url="${pageContext.request.contextPath}/table/${tableName}/create"
   class="btn btn-primary"
   onclick="loadContent(event, this);">Create New User</a>
<br/><br/>

<!-- Determine which fields to render -->
<c:set var="renderFields">
  <c:choose>
    <c:when test="${not empty fieldsToRender}">${fieldsToRender}</c:when>
    <c:otherwise>${allFields}</c:otherwise>
  </c:choose>
</c:set>

<!-- Users Table -->
<table class="user-table">
  <thead>
  <tr>
    <!-- Dynamically render table headers -->
    <c:forEach var="field" items="${renderFields}">
      <th>${columnLabels[field]}</th>
    </c:forEach>
    <th>Actions</th>
  </tr>
  </thead>
  <tbody>
  <!-- Iterate over each user and render table rows -->
  <c:forEach var="user" items="${users}">
    <tr>
      <c:forEach var="field" items="${renderFields}">
        <td>${user[field]}</td>
      </c:forEach>
      <td>
        <!-- Edit Button -->
        <a href="${pageContext.request.contextPath}/users/edit/${user['id']}"
           data-url="${pageContext.request.contextPath}/users/edit/${user['id']}"
           class="btn btn-secondary">Edit</a>

        <!-- Delete Button -->
        <a href="${pageContext.request.contextPath}/users/delete/${user['id']}"
           class="btn btn-danger"
           onclick="return confirm('Are you sure you want to delete this user?'); loadContent(event, this);">
          Delete
        </a>
      </td>
    </tr>
  </c:forEach>
  </tbody>
</table>
