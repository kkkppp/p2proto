<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!-- Custom Table Tabular View -->
<h2><fmt:message key="tables.title" /></h2>
<a href="${pageContext.request.contextPath}/tableSetup/create"
   data-url="${pageContext.request.contextPath}/tableSetup/create"
   class="btn btn-primary"
   onclick="loadContent(event, this);">
  <fmt:message key="tables.button.create">
    <fmt:param value=""/>
  </fmt:message>
</a>
<br/><br/>

<table class="user-table">
  <thead>
  <tr>
    <th><fmt:message key="tables.name"/></th>
    <th><fmt:message key="tables.label"/></th>
    <th><fmt:message key="tables.plurallabel"/></th>
    <th><fmt:message key="label.actions" /></th>
  </tr>
  </thead>
  <tbody>
  <!-- Iterate over each user and render table rows -->
  <c:forEach var="table" items="${metadataList}">
    <tr>
      <td>${table.tableName}</td>
      <td>${table.tableLabel}</td>
      <td>${table.tablePluralLabel}</td>
      <td>
        <!-- Edit Button -->
        <a href="${pageContext.request.contextPath}/tableSetup/edit/${table.id}"
           data-url="${pageContext.request.contextPath}/tableSetup/edit/${table.id}"
           class="btn btn-secondary"
           onclick="loadContent(event, this);"><fmt:message key="button.edit" /></a>

        <!-- Delete Button -->
        <a href="${pageContext.request.contextPath}/tableSetup/delete/${table.id}"
           class="btn btn-danger"
           onclick="return confirmDelete('${tableLabel}'); loadContent(event, this);">
          <fmt:message key="button.delete" />
        </a>
      </td>
    </tr>
  </c:forEach>
  </tbody>
</table>