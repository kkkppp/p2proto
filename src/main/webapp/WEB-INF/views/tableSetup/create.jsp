<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>


<form:form
        method="POST"
        action="${pageContext.request.contextPath}/tableSetup/save"
        modelAttribute="table"
        class="create-user-form"
        aria-labelledby="createUserForm">

  <div class="form-group">
    <label for="tableName"><fmt:message key="tables.name" /></label>
    <form:input
            path="tableName"
            id="tableName"
            class="form-control"
            aria-required="true" />
    <label for="tableLabel"><fmt:message key="tables.label" /></label>
    <form:input
            path="tableLabel"
            id="tableLabel"
            class="form-control"
            aria-required="true" />
    <label for="tablePluralLabel"><fmt:message key="tables.plurallabel" /></label>
    <form:input
            path="tablePluralLabel"
            id="tablePluralLabel"
            class="form-control"
            aria-required="true" />
  </div>

  <div class="form-actions">
    <button type="submit" class="btn btn-primary" onclick="showLoading()"><fmt:message key="button.finish" /></button>
    <a href="${pageContext.request.contextPath}/tableSetup"
       class="btn btn-secondary"
       data-url="${pageContext.request.contextPath}/tableSetup"
       onclick="loadContent(event, this);">
      <fmt:message key="button.cancel" />
    </a>
  </div>

</form:form>
<div id="loadingIndicator" style="display: none;">
  <p class="loading">Submitting...</p>
</div>
