<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.player.PlayerMgr"%>
<%@page import="com.funbridge.server.presence.FBSession"%>
<%@page import="com.funbridge.server.presence.PresenceMgr"%>
<%@page import="com.gotogames.common.session.Session"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.*"%>
<%@page import="java.util.Map.Entry"%>
<html>
<%
    PresenceMgr presenceMgr = ContextManager.getPresenceMgr();
    PlayerMgr playerMgr = ContextManager.getPlayerMgr();
    String paramOperation = request.getParameter("operation");
    String resultOperation = "";
    if (paramOperation != null) {
        if (paramOperation.equals("close")) {
            String sessionID = request.getParameter("sessionid");
            if (sessionID != null) {
                if (presenceMgr.closeSession(sessionID, null)) {
                    resultOperation = "OK - close session";
                } else {
                    resultOperation = "Error to close session with id="+sessionID;
                }
            } else {
                resultOperation = "Error param sessionid not found !";
            }
        }
        else if (paramOperation.equals("closeall")) {
            presenceMgr.closeAllSession(Constantes.EVENT_VALUE_DISCONNECT_MAINTENANCE);
            resultOperation = "OK - close all session";
        }
        else if (paramOperation.equals("changeMaintenance")) {
            presenceMgr.setServiceMaintenance(!presenceMgr.isServiceMaintenance());
            resultOperation = "OK - status maintenance change";
        }
    }
    List<Session> listSession = presenceMgr.getAllCurrentSession();
    int nbGamePlay = 0, nbGameReplay = 0;
    int nbSessionWebSocket = 0;
    Map<String, Integer> mapDeviceTypeNbSession = new HashMap<String, Integer>();
    Map<String, Integer> mapVersionNbSession = new HashMap<String, Integer>();
    for (Session s : listSession) {
        if (s instanceof FBSession) {
            FBSession fbs = (FBSession) s;
            if (fbs.getCurrentGameTable() != null) {
                if (fbs.getCurrentGameTable().isReplay()) {
                    nbGameReplay++;
                } else {
                    nbGamePlay++;
                }
            }
            int curDeviceNbSession = 0;
            if (mapDeviceTypeNbSession.get(fbs.getDeviceType()) != null) {
                    curDeviceNbSession = mapDeviceTypeNbSession.get(fbs.getDeviceType());
            }
            curDeviceNbSession++;
            mapDeviceTypeNbSession.put(fbs.getDeviceType(), curDeviceNbSession);
            int curVersionNbSession = 0;
            if (mapVersionNbSession.get(fbs.getClientVersion()) != null) {
                    curVersionNbSession = mapVersionNbSession.get(fbs.getClientVersion());
            }
            curVersionNbSession++;
            mapVersionNbSession.put(fbs.getClientVersion(), curVersionNbSession);

            if (fbs.getWebSocket() != null) {
                nbSessionWebSocket++;
            }
        }
    }
%>
	<head>
		<title>Funbridge Server - Administration Session</title>
		<script type="text/javascript">
		function clickCloseSession(sessionID, login) {
			if (confirm("Close session for player="+login)) {
				document.forms["formSession"].operation.value = "close";
				document.forms["formSession"].sessionid.value = sessionID;
				document.forms["formSession"].submit();
			}
		}
		function clickDetailSession(sessionID) {
			window.open("adminSessionDetail.jsp?sessionID="+sessionID);
			return false;
		}
		function clickCloseAllSession() {
			if (confirm("Close all session")) {
				document.forms["formSession"].operation.value = "closeall";
				document.forms["formSession"].submit();
			}
		}
		function clickMaintenance() {
			if (confirm("Change maintenance status ?")) {
				document.forms["formSession"].operation.value = "changeMaintenance";
				document.forms["formSession"].submit();
			}
		}
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION SESSION</h1>
		<a href="admin.jsp">Administration</a><br/><br/>
		<%if (resultOperation.length() > 0) {%>
		<b>RESULTAT OPERATION : <%= paramOperation%></b>
		<br/>Result = <%=resultOperation %>
		<%} %>
		<hr width="90%"/>
		<form name="formSession" method="post" action="adminSession.jsp">
		<b>Current date=</b><%=Constantes.timestamp2StringDateHour(System.currentTimeMillis()) %><br/>
		<b>Service maintenance</b> = <%=presenceMgr.isServiceMaintenance() %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="<%=presenceMgr.isServiceMaintenance()?"Disable maintenance":"Enable maintenance"%>" onclick="clickMaintenance()"><br/>
		Lock device size=<%=ContextManager.getPlayerMgr().getLockDeviceSize() %><br/>
		<b>List of Users Connected : </b><br/>
		
		<%if (listSession != null){
			%>
			Nb Users = <%=listSession.size() %> - Nb WebSocket = <%=nbSessionWebSocket %><br/>
			Nb Websocket Event = <%=ContextManager.getClientWebSocketMgr().getMapWebSocket().size() %> - <a href="adminWebSocket.jsp">Detail</a><br/>
			<%
            Iterator<Entry<String, Integer>> itDev = mapDeviceTypeNbSession.entrySet().iterator();
            while (itDev.hasNext()) {
                    Entry<String, Integer> e = itDev.next();%>
                    Type : <%=e.getKey() %> - Nb Session : <%=e.getValue().intValue() %><br/>
            <% }
            %>
            <%
            Iterator<Entry<String, Integer>> itVer = mapVersionNbSession.entrySet().iterator();
            while (itVer.hasNext()) {
                    Entry<String, Integer> e = itVer.next();%>
                    Version : <%=e.getKey() %> - Nb Session : <%=e.getValue().intValue() %><br/>
            <% }
            %>

			
			<input type="hidden" name="operation" value=""/>
			<input type="hidden" name="sessionid" value=""/>
			[<a href="adminSession.jsp">Refresh</a>] - <input type="button" value="Close All" onclick="clickCloseAllSession()"/><br/><br/>
			<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
			<tr><th>Player ID</th><th>Login</th><th>Pseudo</th><th>Version</th><th>Type</th><th>Date last activity</th><th>Nb Events</th><th>Freemium</th><th>NbPlay</th><th>NbReplay</th><th>Replay Enable</th><th>WebSocket</th><th>countServicesCall</th><th>Operation</th></tr>
			<%
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss:SSS");
			for (int i = 0; i < listSession.size(); i++) {
				FBSession temp = (FBSession)listSession.get(i);
				%><tr>
					<td><%=temp.getPlayer().getID() %></td>
					<td><%=temp.getLogin() %></td>
					<td><%=temp.getPlayer().getNickname() %></td>
					<td><%=temp.getClientVersion() %></td>
					<td><%=temp.getDeviceType() %></td>
					<td><%=sdf.format(new Date(temp.getDateLastActivity())) %></td>
					<td><%=temp.getListEvent().size() %></td>
                    <td><%=temp.isFreemium()%></td>
                    <td><%=temp.getNbDealPlayed()%></td>
                    <td><%=temp.getNbDealReplay()%></td>
                    <td><%=playerMgr.isReplayEnabledForPlayer(temp)%></td>
					<td><%=temp.getWebSocket() != null %></td>
					<td>
						<input type="button" value="Close" onclick="clickCloseSession('<%=temp.getID() %>','<%=temp.getLogin()%>')"/>&nbsp;&nbsp;&nbsp;
						<input type="button" value="Detail" onclick="clickDetailSession('<%=temp.getID() %>')"/>
					</td>
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