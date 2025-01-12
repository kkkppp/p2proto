<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!-- Create User Form -->
<h2>Create New User</h2>

<form:form method="POST" action="${pageContext.request.contextPath}/users/save" modelAttribute="user" class="create-user-form" aria-labelledby="createUserForm">

  <!-- Username Field -->
  <div class="form-group">
    <label for="username">Username:</label>
    <form:input path="username" id="username" class="form-control" aria-required="true" />
    <form:errors path="username" cssClass="error" aria-live="polite" />
  </div>

  <!-- Email Field -->
  <div class="form-group">
    <label for="email">Email:</label>
    <form:input path="email" id="email" class="form-control" aria-required="true" />
    <form:errors path="email" cssClass="error" aria-live="polite" />
  </div>

  <!-- First Name Field -->
  <div class="form-group">
    <label for="firstName">First Name:</label>
    <form:input path="firstName" id="firstName" class="form-control" />
    <form:errors path="firstName" cssClass="error" aria-live="polite" />
  </div>

  <!-- Last Name Field -->
  <div class="form-group">
    <label for="lastName">Last Name:</label>
    <form:input path="lastName" id="lastName" class="form-control" />
    <form:errors path="lastName" cssClass="error" aria-live="polite" />
  </div>

  <!-- Password Field -->
  <div class="form-group">
    <label for="password">Password:</label>
    <form:password path="password" id="password" class="form-control" aria-required="true" />
    <form:errors path="password" cssClass="error" aria-live="polite" />
  </div>

  <!-- Add more fields with validation as necessary -->

  <!-- Form Actions -->
  <div class="form-actions">
    <button type="submit" class="btn btn-primary" onclick="showLoading()">Create</button>
    <a href="${pageContext.request.contextPath}/users" class="btn btn-secondary" data-url="${pageContext.request.contextPath}/users">Cancel</a>
  </div>

</form:form>

<!-- Loading Indicator -->
<div id="loadingIndicator" style="display: none;">
  <p class="loading">Submitting...</p>
</div>

<script>
  function showLoading() {
    document.getElementById('loadingIndicator').style.display = 'block';
  }
</script>
