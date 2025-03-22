<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!-- Custom Table Tabular View -->
<h2>${tableLabelPlural}</h2>
<a href="${pageContext.request.contextPath}/table/${tableName}/create"
   data-url="${pageContext.request.contextPath}/table/${tableName}/create"
   class="btn btn-primary"
   onclick="loadContent(event, this);">
  <fmt:message key="button.createNew">
    <fmt:param value="${tableLabel}"/>
  </fmt:message>
</a>
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
    <th><fmt:message key="label.actions" /></th>
  </tr>
  </thead>
  <tbody>
  <!-- Iterate over each user and render table rows -->
  <c:forEach var="record" items="${records}">
    <tr>
      <c:forEach var="field" items="${renderFields}">
        <td>${record[field]}</td>
      </c:forEach>
      <td>
        <!-- Edit Button -->
        <a href="${pageContext.request.contextPath}/table/${tableName}/edit/${record['id']}"
           data-url="${pageContext.request.contextPath}/table/${tableName}/edit/${record['id']}"
           class="btn btn-secondary"
           onclick="loadContent(event, this);"><fmt:message key="button.edit" /></a>

        <!-- Delete Button -->
        <a href="#"
           class="btn btn-danger"
           onclick="deleteRecord('${tableName}', '${record['id']}');">
          <fmt:message key="button.delete" />
        </a>
      </td>
    </tr>
  </c:forEach>
  </tbody>
</table>