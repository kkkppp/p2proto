<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<h2>Create New User</h2>

<form:form method="POST" action="${pageContext.request.contextPath}/table/${tableName}/save" modelAttribute="record" class="create-user-form" aria-labelledby="createUserForm">

  <c:forEach var="field" items="${record.fields}" varStatus="status">
    <div class="form-group">
      <label for="${field.name}">${field.label}:</label>

      <c:choose>
        <c:when test="${field.type == 'PASSWORD'}">
          <form:password path="fields[${status.index}].value" id="${field.name}" class="form-control" aria-required="${field.required}" />
        </c:when>

        <c:when test="${field.type == 'EMAIL'}">
          <form:input path="fields[${status.index}].value" id="${field.name}" class="form-control" aria-required="${field.required}" type="email" />
        </c:when>

        <c:when test="${field.type == 'SELECT'}">
          <form:select path="fields[${status.index}].value" id="${field.name}" class="form-control" aria-required="${field.required}">
            <form:option value="" label="-- Select --" />
            <c:forEach var="option" items="${field.options}">
              <form:option value="${option}" label="${option}" />
            </c:forEach>
          </form:select>
        </c:when>

        <c:when test="${field.type == 'RADIO'}">
          <div>
            <c:forEach var="option" items="${field.options}">
              <label>
                <input type="radio" name="fields[${status.index}].value" value="${option}" ${field.required ? 'required' : ''} />
                  ${option}
              </label>
              &nbsp;&nbsp;
            </c:forEach>
          </div>
        </c:when>

        <c:when test="${field.type == 'CHECKBOX'}">
          <div>
            <c:forEach var="option" items="${field.options}">
              <label>
                <input type="checkbox" name="fields[${status.index}].value" value="${option}" ${field.required ? 'required' : ''} />
                  ${option}
              </label>
              &nbsp;&nbsp;
            </c:forEach>
          </div>
        </c:when>

        <c:otherwise>
          <form:input path="fields[${status.index}].value" id="${field.name}" class="form-control" aria-required="${field.required}" />
        </c:otherwise>
      </c:choose>

      <c:if test="${not empty field.errors}">
        <div class="error" aria-live="polite">
          <c:forEach var="error" items="${field.errors}">
            <p>${error}</p>
          </c:forEach>
        </div>
      </c:if>
    </div>
  </c:forEach>

  <div class="form-actions">
    <button type="submit" class="btn btn-primary" onclick="showLoading()">Create</button>
    <a href="${pageContext.request.contextPath}/table/${tableName}" class="btn btn-secondary" data-url="${pageContext.request.contextPath}/users" onclick="loadContent(event, this);">Cancel</a>
  </div>

</form:form>

<div id="loadingIndicator" style="display: none;">
  <p class="loading">Submitting...</p>
</div>

<script>
  function showLoading() {
    document.getElementById('loadingIndicator').style.display = 'block';
  }
</script>
