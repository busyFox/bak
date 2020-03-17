<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.presence.FBSession"%>
<%@page import="com.funbridge.server.presence.PresenceMgr"%>
<%@page import="com.funbridge.server.tournament.game.GameMgr" %>
<%@page import="com.funbridge.server.tournament.game.Table" %>
<%@ page import="com.funbridge.server.tournament.game.Tournament" %>
<%@ page import="com.funbridge.server.ws.event.Event" %>
<%@ page import="com.funbridge.server.ws.game.GameServiceRestImpl" %>
<%@ page import="com.funbridge.server.ws.player.WSPlayerInfo" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<html>
<%
	PresenceMgr presenceMgr = ContextManager.getPresenceMgr();
	String sessionID = request.getParameter("sessionID");
	String paramOperation = request.getParameter("operation");
	String resultOperation = "";
	FBSession fbSession = (FBSession)presenceMgr.getSession(sessionID);
	if (paramOperation != null) {
		if (paramOperation.equals("close")) {
			if (sessionID != null) {
				if (presenceMgr.closeSession(sessionID, null)) {
					resultOperation = "OK - close session";
				} else {
					resultOperation = "Error to close session with id="+sessionID;
				}
			} else {
				resultOperation = "Error param sessionid not found !";
			}
		} else if (paramOperation.equals("getArgineAdvice")) {
			if (fbSession != null) {
				Table table = fbSession.getCurrentGameTable();
				if (table != null) {
					Tournament tour = table.getTournament();
					if (tour != null) {
						try {
							GameServiceRestImpl.GameType gameType = ContextManager.getGameService().checkGameSessionAndReturnGameType(fbSession, 0, table.getGame().getIDStr());
							GameMgr gameMgr = ContextManager.getGameService().getGameMgr(gameType);
							if (gameMgr != null) {
								resultOperation = gameMgr.getArgineAdvice(fbSession);
							} else {
								resultOperation = "Error : GameMgr is null";
							}
						} catch (Exception e) {
							resultOperation = "Error : " + e.getMessage();
						}
					} else {
						resultOperation = "Error : tournament is null";
					}
				} else {
					resultOperation = "Error : table is null";
				}
			} else {
				resultOperation = "Error : session is null !";
			}
		}
	}
%>
<head>
	<title>Funbridge Server - Administration Session Detail</title>
	<script type="text/javascript">
        function clickCloseSession(sessionID, login) {
            if (confirm("Close session for player="+login)) {
                document.forms["formSessionDetail"].operation.value = "close";
                document.forms["formSessionDetail"].submit();
            }
        }
        function clickGetArgineAdvice(){
            document.forms["formSessionDetail"].operation.value = "getArgineAdvice";
            document.forms["formSessionDetail"].submit();
        }
	</script>
</head>
<body>
<h1>ADMINISTRATION SESSION DETAIL</h1>
<%if (resultOperation.length() > 0) {%>
<b>RESULTAT OPERATION : <%= paramOperation%></b>
<br/>Result = <%=resultOperation %>
<%} %>
<hr width="90%"/>
<%if (fbSession == null) { %>
No session found with ID=<%=sessionID %>
<%} else {
WSPlayerInfo wsPlayerInfo = ContextManager.getPlayerMgr().playerToWSPlayerInfo(fbSession);
%>
<form name="formSessionDetail" method="post" action="adminSessionDetail.jsp">
	<input type="hidden" name="sessionID" value="<%=sessionID%>">
	<input type="hidden" name="operation" value="">
	<b>Player</b><br/>
	<%=fbSession.getPlayer().toString() %><br/>
	PlayerInfo : <%=wsPlayerInfo%> <br/>
    Last Date connection = <%=Constantes.timestamp2StringDateHour(fbSession.getDateLastConnection())%><br/>
    Client Version =<%=fbSession.getClientVersion() %><br/>
	Device = <%=fbSession.getDevice().toString() %><br/>
	NbDuelInProgress = <%=fbSession.countDuelInProgress()%> - NbDuelRequest = <%=fbSession.countDuelRequest()%><br/>
	<br/>
	<b>Session</b>
    Freemium = <%=fbSession.isFreemium()%><br/>
	Date creation = <%=Constantes.timestamp2StringDateHour(fbSession.getDateCreation()) %><br/>
	Date last activity = <%=Constantes.timestamp2StringDateHour(fbSession.getDateLastActivity()) %><br/>
	Nb Deals Played = <%=fbSession.getNbDealPlayed() %> - Details : <%=fbSession.getMapCategoryPlay().toString()%><br/>
    Nb Deals Replayed = <%=fbSession.getNbDealReplay()%> - Details : <%=fbSession.getMapCategoryReplay().toString()%><br/>

	WebSocket getEvent : <%=fbSession.getWebSocket()!=null %><br/>
	<%
		List<Event> events = fbSession.getListEvent();
	%>
	List events : NbEvents=<%=events.size() %><br/>
	<%for (Event evt : events){ %>
	Evt:<%=evt.toString() %> - timestamp=<%=evt.timestamp%> (<%=Constantes.timestamp2StringDateHour(evt.timestamp)%>)<br/>
	<%} %>
    <br/>
    Services call : count=<%=fbSession.countServicesCall()%>
	<br/>
    <% for (Map.Entry<String, Integer> eCall : fbSession.mapCountServicesCall.entrySet()) {%>
    sevice call : <%=eCall.getKey()%> - nb : <%=eCall.getValue()%><br/>
    <%}%>
	<b>Current Game Table : </b><br/><%=fbSession.getCurrentGameTable() %><br/>
	<input type="button" value="getArgineAdvice" onclick="clickGetArgineAdvice()">
	<br/>
</form>
<%} %>
</body>
</html>