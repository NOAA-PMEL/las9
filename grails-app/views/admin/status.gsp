<%--
  Created by IntelliJ IDEA.
  User: rhs
  Date: 5/26/20
  Time: 7:47 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Status of Data Sets</title>
</head>

<body>
<g:each in="${failedDatasets}">
    <p>Title: ${it.title}</p>
    <p>URL: ${it.url}</p>
    <p>status: ${it.status}</p>
    <p>Parent: ${it.parent.title}</p>
    <p>Parent URL: ${it.parent.url}</p>
    <br/>
</g:each>
</body>
</html>
