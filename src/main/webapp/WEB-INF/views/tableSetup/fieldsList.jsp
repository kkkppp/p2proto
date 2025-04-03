<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<h2>
<fmt:message key="tables.fields.title">
  <fmt:param value="${table.tablePluralLabel}"/>
</fmt:message>
</h2>
<a href="${pageContext.request.contextPath}/tableSetup/createField?domain=text"
   data-url="${pageContext.request.contextPath}/tableSetup/createField?domain=text"
   class="btn btn-primary"
   onclick="loadContent(event, this);">
  <fmt:message key="fields.button.createText" />
</a>
<a href="${pageContext.request.contextPath}/tableSetup/createField?domain=choice"
   data-url="${pageContext.request.contextPath}/tableSetup/createField?domain=choice"
   class="btn btn-primary"
   onclick="loadContent(event, this);">
  <fmt:message key="fields.button.createChoice" />
</a>
<a href="${pageContext.request.contextPath}/tableSetup/createLF"
   data-url="${pageContext.request.contextPath}/tableSetup/createLF"
   class="btn btn-primary"
   onclick="loadContent(event, this);">
  <fmt:message key="fields.button.createLF" />
</a>
<br/><br/>

<div class="mb-3">
  <button class="btn btn-outline-secondary mb-2" type="button" data-toggle="collapse" data-target="#additionalButtons" aria-expanded="false" aria-controls="additionalButtons">
    <fmt:message key="fields.button.createOther" />
  </button>

  <div class="collapse" id="additionalButtons">
    <div class="d-flex flex-row gap-2 mb-2">
      <a href="#" class="btn btn-info mb-1">
        <fmt:message key="fields.button.createDate" />
      </a>
      <a href="#" class="btn btn-info mb-1">
        <fmt:message key="fields.button.createEmail" />
      </a>
      <a href="#" class="btn btn-info">
        <fmt:message key="fields.button.createNumber" />
      </a>
      <a href="#" class="btn btn-info">
        <fmt:message key="fields.button.createMChoice" />
      </a>
    </div>
  </div>
</div>

<table class="user-table">
  <thead>
  <tr>
    <th><fmt:message key="column.name"/></th>
    <th><fmt:message key="column.label"/></th>
    <th><fmt:message key="column.domain"/></th>
    <th><fmt:message key="label.actions" /></th>
  </tr>
  </thead>
  <tbody>
  <c:forEach var="column" items="${table.columns}">
    <tr>
      <td>${column.name}</td>
      <td>${column.label}</td>
      <td>${column.domain}</td>
      <td>
        <!-- Edit Button -->
        <a href="${pageContext.request.contextPath}/tableSetup/editField/${column.id}"
           data-url="${pageContext.request.contextPath}/tableSetup/editField/${column.id}"
           class="btn btn-secondary"
           onclick="loadContent(event, this);"><fmt:message key="button.edit" /></a>

        <!-- Delete Button -->
        <a href="${pageContext.request.contextPath}/tableSetup/deleteField/${column.id}"
           class="btn btn-danger"
           onclick="return confirmDelete('${column.label}'); loadContent(event, this);">
          <fmt:message key="button.delete" />
        </a>
      </td>
    </tr>
  </c:forEach>
  </tbody>
</table>