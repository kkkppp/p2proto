<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:choose>
  <c:when test="${mode == 'create'}">
    <h2>Create New User</h2>
  </c:when>
  <c:otherwise>
    <h2>Edit User</h2>
  </c:otherwise>
</c:choose>

<form:form
        method="POST"
        action="${pageContext.request.contextPath}/table/${tableName}/save"
        modelAttribute="record"
        class="create-user-form"
        aria-labelledby="createUserForm"
        onsubmit="return validatePasswords()">

  <c:forEach var="field" items="${record.fields}" varStatus="status">
    <c:choose>
      <c:when test="${field.autoGenerated}">
        <c:if test="${mode == 'edit'}">
          <div class="form-group">
            <label for="${field.name}">${field.label}:</label>
            <form:input
                    path="fields[${status.index}].value"
                    id="${field.name}"
                    class="form-control"
                    readonly="true" />
          </div>
        </c:if>

        <c:if test="${mode == 'create'}">
          <form:hidden path="fields[${status.index}].value" />
        </c:if>

        <!-- Always pass metadata to the server via hidden fields -->
        <form:hidden path="fields[${status.index}].name"/>
        <form:hidden path="fields[${status.index}].type"/>
        <form:hidden path="fields[${status.index}].required"/>
        <form:hidden path="fields[${status.index}].autoGenerated"/>
      </c:when>

      <c:otherwise>
        <div class="form-group">
          <label for="${field.name}">${field.label}:</label>

          <c:choose>
            <c:when test="${field.type == 'PASSWORD'}">
              <c:set var="passwordMask" scope="page" value="" />

              <c:if test="${mode == 'edit'}">
                <c:set var="passwordMask" scope="page" value="********"/>
              </c:if>

              <form:password
                      path="fields[${status.index}].value"
                      id="${field.name}"
                      class="form-control"
                      aria-required="${field.required}"
                      value="${passwordMask}"/>

              <div class="form-group" style="margin-top: 10px;">
                <label for="${field.name}Confirm">Confirm ${field.label}:</label>
                <input
                        type="password"
                        id="${field.name}Confirm"
                        class="form-control"
                        aria-required="${field.required}"
                        value="${passwordMask}" />
              </div>
            </c:when>

            <c:when test="${field.type == 'EMAIL'}">
              <form:input
                      path="fields[${status.index}].value"
                      id="${field.name}"
                      class="form-control"
                      aria-required="${field.required}"
                      type="email" />
            </c:when>

            <c:when test="${field.type == 'SELECT'}">
              <form:select
                      path="fields[${status.index}].value"
                      id="${field.name}"
                      class="form-control"
                      aria-required="${field.required}">
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
                    <input type="radio"
                           name="fields[${status.index}].value"
                           value="${option}"
                      ${field.required ? 'required' : ''} />
                      ${option}
                  </label>
                  &nbsp;&nbsp;
                </c:forEach>
              </div>
            </c:when>

            <c:when test="${field.type == 'CHECKBOX'}">
              <div>
                <input type="checkbox"
                       name="fields[${status.index}].value"
                       value="true"
                  ${field.required ? 'required' : ''} />
              </div>
            </c:when>

            <c:otherwise>
              <form:input
                      path="fields[${status.index}].value"
                      id="${field.name}"
                      class="form-control"
                      aria-required="${field.required}" />
            </c:otherwise>
          </c:choose>

          <!-- Display any validation errors -->
          <c:if test="${not empty field.errors}">
            <div class="error" aria-live="polite">
              <c:forEach var="error" items="${field.errors}">
                <p>${error}</p>
              </c:forEach>
            </div>
          </c:if>

          <!-- Hidden fields for name, type, required, etc. -->
          <form:hidden path="fields[${status.index}].name" />
          <form:hidden path="fields[${status.index}].type" />
          <form:hidden path="fields[${status.index}].required" />
        </div>
      </c:otherwise>
    </c:choose>
  </c:forEach>

  <div class="form-actions">
    <button type="submit" class="btn btn-primary" onclick="showLoading()">Finish</button>
    <a href="${pageContext.request.contextPath}/table/${tableName}"
       class="btn btn-secondary"
       data-url="${pageContext.request.contextPath}/table/${tableName}"
       onclick="loadContent(event, this);">
      Cancel
    </a>
  </div>

  <sec:csrfInput/>
</form:form>

<div id="loadingIndicator" style="display: none;">
  <p class="loading">Submitting...</p>
</div>

