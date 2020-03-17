<%@page import="com.gotogames.bridge.engineserver.common.EngineConfiguration"%>
<%@page import="com.gotogames.bridge.engineserver.request.QueueMgr"%>
<%@page import="com.gotogames.bridge.engineserver.ws.compute.ComputeServiceImpl"%>
<%@page import="com.gotogames.bridge.engineserver.user.UserVirtualMgr"%>
<%@page import="com.gotogames.bridge.engineserver.user.UserVirtual"%>
<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="com.gotogames.bridge.engineserver.session.EngineSessionMgr"%>
<%@page import="java.util.List"%>
<%@page import="com.gotogames.common.session.Session"%>
<%
EngineSessionMgr sessionMgr = ContextManager.getSessionMgr();
ComputeServiceImpl computeService = ContextManager.getComputeService();
QueueMgr queueMgr = ContextManager.getQueueMgr();
UserVirtualMgr userMgr = ContextManager.getUserMgr();
String operation = request.getParameter("operation");
String paramOperation = "";
String resultOperation = "";
if (operation != null) {
	if (operation.equals("cleanAll")) {
		sessionMgr.closeAllSession();
		resultOperation = "OK";
	} else if (operation.equals("delete")) {
		paramOperation = request.getParameter("selection");
		String[] sessionID = paramOperation.split("-");
		boolean bClose = true;
		for (int i = 0; i < sessionID.length; i++) {
			bClose = bClose && sessionMgr.closeSession(sessionID[i]);
		}
		if (bClose) resultOperation="OK"; else resultOperation="KO";
	} else {
		resultOperation = "operation unknown ! ("+operation+")";
	}
}
List<Session> listSession = null;
int nbFbMoteur = 0, nbFb2compute = 0, nbFb2Request = 0;
if (sessionMgr != null) {
	listSession = sessionMgr.getAllCurrentSession();
	for (Session s : listSession) {
		if (s.getLogin().startsWith("engine")){
			nbFbMoteur++;
		}
		else if (s.getLogin().startsWith("fb2compute")) {
			nbFb2compute++;
		}
		else if (s.getLogin().startsWith("fb2request")) {
			nbFb2Request++;
		}
	}
}
SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss:SSS");
%>

<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@ page import="com.gotogames.bridge.engineserver.user.UserVirtualEngine" %>
<%@ page import="com.gotogames.bridge.engineserver.user.UserVirtualFBServer" %>
<html>
	<head>
		<title>EngineServer - Administration Session</title>
		<script type="text/javascript">
		function clickClean(){
			if (confirm("Clean all data from queue ?")){
				document.forms["formSession"].operation.value="cleanAll";
				document.forms["formSession"].submit();
			}
		}
		function clickDelete(id){
			if (confirm("Remove session with ID="+id+" ?")){
				document.forms["formSession"].operation.value="delete";
				document.forms["formSession"].selection.value=id;
				document.forms["formSession"].submit();
			}
		}
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION SESSION</h1>
		<a href="admin.jsp">Administration</a><br/><br/>
		<hr width="90%"/>
		<a href="adminSession.jsp">Refresh</a><br/>
		<%if (operation != null){ %>
		<b>Operation executed : <%=operation %> - parametres=<%=paramOperation %></b><br/>
		Operation result : <%=resultOperation %>
		<%} %>
		<br/>
		Nb result from FbMoteurs = <%=computeService.getNbResultFbMoteur() %><br/>
		Nb result from Others = <%=computeService.getNbResultFbOthers() %><br/>
		Time to Get Query to compute = <%=queueMgr.getAverageTimeGet() %><br/>
		Time to Get Query empty = <%=queueMgr.getAverageTimeGetEmpty() %><br/>
		Option request only for FbMoteur = <%=EngineConfiguration.getInstance().getIntValue("request.onlyFbMoteur", 0) %>
		<hr width="90%"/>
		<b>List of Users Connected : </b>
		<form name="formSession" action="adminSession.jsp" method="post">
		<input type="hidden" name="operation" value=""/>
		<input type="hidden" name="selection" value=""/>
		<input type="button" value="Clean All" onclick="clickClean()"/>
		<br/><br/>
		<%if (listSession != null){
			%>
			Current date=<%=sdf.format(new Date()) %><br/>
			Nb Users = <%=listSession.size() %> - Nb Engine=<%=sessionMgr.getNbEngineConnected() %><br/>
			Nb FbMoteur=<%=nbFbMoteur %> - Nb fb2compute=<%=nbFb2compute %> - Nb fb2request=<%=nbFb2Request %><br/>
			
			<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
			<tr><th>ID</th><th>Login - LoginID</th><th>Date creation</th><th>Date last activity</th><th>Timeout</th><th>User</th><th>Operation</th></tr>
			<%
			for (int i = 0; i < listSession.size(); i++) {
				Session temp = listSession.get(i);
				UserVirtual u = userMgr.getUserVirtual(temp.getLogin());
				%><tr>
					<td><%=temp.getID() %></td>
					<td><%=temp.getLogin() %> - <%=temp.getLoginID()%></td>
					<td><%=sdf.format(new Date(temp.getDateCreation()))%></td>
					<td><%=sdf.format(new Date(temp.getDateLastActivity())) %></td>
                    <td><%=temp.getTimeout()%></td>
					<td><%=(u == null) ? "null" : u.getID()%></td>
					<td><input type="button" value="Delete" onclick="clickDelete('<%=temp.getID() %>')"/></td>
				</tr>
				<%
			}
		%>
			</table>
		<%} else {%>
		LIST SESSION NULL !!
		<%} %>
		</form>
	</body>
</html>