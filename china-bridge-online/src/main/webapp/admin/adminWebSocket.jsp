<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.ws.servlet.ClientWebSocketEndpoint"%>
<%@page import="com.funbridge.server.ws.servlet.ClientWebSocketMgr"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Map.Entry"%>
<html>
<%
ClientWebSocketMgr eventWsMgr = ContextManager.getClientWebSocketMgr();
String paramOperation = request.getParameter("operation");
String resultOperation = "";
if (paramOperation != null) {
	if (paramOperation.equals("close")) {
	}
}
Map<String, ClientWebSocketEndpoint> mapWS = eventWsMgr.getMapWebSocket();
%>
	<head>
		<title>Funbridge Server - Administration</title>
		<script type="text/javascript">
		function clickCloseSession(sessionID, login) {
			if (confirm("Close session for player="+login)) {
				document.forms["formSessionDetail"].operation.value = "close";
				document.forms["formSessionDetail"].submit();
			}
		}
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION EVENT WEBSOCKET</h1>
		[<a href="adminSession.jsp">Back to session</a>]<br/><br/>
		<%if (resultOperation.length() > 0) {%>
		<b>RESULTAT OPERATION : <%= paramOperation%></b>
		<br/>Result = <%=resultOperation %>
		<%} %>
		<hr width="90%"/>
		<form name="formWebSocket" method="post" action="adminWebSocket.jsp">
		<input type="hidden" name="operation" value="">
		Nb WebSocket = <%=mapWS.size() %>
		<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
			<tr><th>Session ID</th><th>Description</th><th>Date last activity</th><th>Operation</th></tr>
		<%
		Iterator<Entry<String, ClientWebSocketEndpoint>> itDev = mapWS.entrySet().iterator();
		while (itDev.hasNext()) {
			ClientWebSocketEndpoint ws = itDev.next().getValue();%>
			<tr>
				<td><%=ws.getSessionID() %></td>
				<td><%=ws.getDescription() %></td>
				<td><%=Constantes.timestamp2StringDateHour(ws.getTsLastActivity()) %></td>
				<td></td>
			</tr>
		<%} %>
		</table>
	</body>
</html>