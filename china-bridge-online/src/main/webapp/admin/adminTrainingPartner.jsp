<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.tournament.TournamentGame2Mgr"%>
<%@page import="com.funbridge.server.tournament.category.TournamentTrainingPartnerMgr"%>
<%@page import="com.funbridge.server.tournament.data.Tournament"%>
<%@page import="com.funbridge.server.tournament.data.TournamentChallenge"%>
<%@page import="com.funbridge.server.tournament.data.TournamentGame2"%>
<%@page import="com.funbridge.server.tournament.data.TournamentTable2"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@ page import="java.util.Map" %>
<html>
<%
    TournamentTrainingPartnerMgr mgr = ContextManager.getTournamentTrainingPartnerMgr();
    TournamentGame2Mgr game2Mgr = ContextManager.getTournamentGame2Mgr();
    String operation = request.getParameter("operation");
    String resultOperation = "";
    if (operation != null && operation.length() > 0) {
        if (operation.equals("removeThread")) {
            try {
                long gameID = Long.parseLong(request.getParameter("paramOperation"));
                game2Mgr.removeListThreadGameRunning(gameID);
                resultOperation = "Success to remove gameThread for gameID="+gameID;
            } catch (Exception e) {
                resultOperation = "Exception : "+e.getMessage();
            }
        }
        else if (operation.equals("finishTournament")) {
            try {
                long tourID = Long.parseLong(request.getParameter("paramOperation"));
                if (mgr.finishTournament(tourID)) {
                    resultOperation = "Success to finish tournament with tourID=" + tourID;
                } else {
                    resultOperation = "Failed to finish tournament with tourID="+tourID;
                }
            } catch (Exception e) {
                resultOperation = "Exception : "+e.getMessage();
            }
        }
        else if (operation.equals("removeChallengeFromMap")) {
            try {
                long challengeID = Long.parseLong(request.getParameter("paramOperation"));
                TournamentChallenge tc = ContextManager.getTournamentChallengeMgr().getMapChallenge().remove(challengeID);
                if (tc != null) {
                    resultOperation = "Success to remove challenge with challengeID=" + challengeID+" - tc="+tc;
                } else {
                    resultOperation = "Failed to remove challenge with challengeID="+challengeID;
                }
            } catch (Exception e) {
                resultOperation = "Exception : "+e.getMessage();
            }
        }
    }
    Map<Long, TournamentTable2> mapTable = null;
    Map<Long, TournamentChallenge> mapChallenge = null;
    String selection = request.getParameter("selection");
    if (selection == null || selection.length() == 0) {
        selection = "game";
    }
    if (selection.equals("game")) {
        mapTable = mgr.getMapTable();
    }
    else if (selection.equals("challenge")) {
        mapChallenge = ContextManager.getTournamentChallengeMgr().getMapChallenge();
    }
%>
	<head>
		<title>Funbridge Server - Administration Tournament</title>
		<script type="text/javascript">
		function clickDisplayChallenge() {
			document.forms["formTrainingPartner"].selection.value='challenge';
			document.forms["formTrainingPartner"].submit();
		}
		function clickDisplayGame() {
			document.forms["formTrainingPartner"].selection.value='game';
			document.forms["formTrainingPartner"].submit();
		}
        function clickRemoveThread(gameID) {
            if (confirm("Remove thread for gameID="+gameID)) {
                document.forms["formTrainingPartner"].operation.value = 'removeThread';
                document.forms["formTrainingPartner"].paramOperation.value = ""+gameID;
                document.forms["formTrainingPartner"].submit();
            }
        }
        function clickFinishTournament(tourID) {
            if (confirm("Finish tournament with tourID="+tourID)) {
                document.forms["formTrainingPartner"].operation.value = 'finishTournament';
                document.forms["formTrainingPartner"].paramOperation.value = ""+tourID;
                document.forms["formTrainingPartner"].submit();
            }
        }
            function clickRemoveChallengeFromMap(challengeID) {
                if (confirm("Remove challenge from map with challengeID="+challengeID)) {
                    document.forms["formTrainingPartner"].operation.value = 'removeChallengeFromMap';
                    document.forms["formTrainingPartner"].paramOperation.value = ""+challengeID;
                    document.forms["formTrainingPartner"].submit();
                }
            }
		</script>
	</head>
	<body>
		<h1>TOURNAMENT TRAINING PARTNER</h1>
		<a href="admin.jsp">Administration</a><br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <%} %>
		<hr width="90%"/>
		<form name="formTrainingPartner" method="post" action="adminTrainingPartner.jsp">
		<input type="hidden" name="selection" value="<%=selection%>">
            <input type="hidden" name="operation" value="">
            <input type="hidden" name="paramOperation" value="">
		<%if (selection.equals("game")) {%>
		<input type="button" value="DISPLAY CHALLENGE" onclick="clickDisplayChallenge()"/>
		<%} else { %>
		<input type="button" value="DISPLAY GAME" onclick="clickDisplayGame()"/>
		<%} %>
		<br/><br/>
		<%if (mapTable != null) {
			List<TournamentTable2> listTable = new ArrayList<TournamentTable2>(mapTable.values());
			int nbNotFinished = 0;
			for (TournamentTable2 t : listTable) {
				if (t.getCurrentGame() == null || (t.getCurrentGame() != null && !t.getCurrentGame().isFinished())) {
					nbNotFinished++;
				}
			}
			%>
			<b>List of game in progress</b><br/>
			Nb game in memory=<%=listTable.size() %> - Nb game not finished=<%=nbNotFinished %><br/>
			Date of last purge=<%=Constantes.timestamp2StringDateHour(mgr.getDateLastPurge()) %><br/>
			<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
				<tr><th>ID</th><th>Tournament</th><th>Creator</th><th>South</th><th>North</th><th>ChallengeID</th><th>Game</th><th>Thread running</th></tr>
				<%for (TournamentTable2 table : listTable) {
					TournamentGame2 game = table.getCurrentGame();
					Tournament tour = table.getTournament();
                    boolean threadRunning = game2Mgr.isThreadGameRunning(game.getID());
					%>
				<tr>
					<td><%=table.getID() %></td>
					<td><%=tour.getID() %> - <%=Constantes.timestamp2StringDateHour(tour.getBeginDate()) %> - <%=Constantes.timestamp2StringDateHour(tour.getEndDate()) %> <input type="button" value="Finish" onclick="clickFinishTournament(<%=tour.getID()%>)"></td>
					<td><%=table.getCreator().getID() %> - <%=table.getCreator().getNickname() %></td>
					<td><%=table.getPlayerSouth().getID() %> - <%=table.getPlayerSouth().getNickname() %></td>
					<td><%=table.getPlayerNorth().getID() %> - <%=table.getPlayerNorth().getNickname() %></td>
					<td><%=table.getChallengeID() %></td>
					<td><%if (game!=null) {%><%=game.getID() %> - <%=game.isFinished() %> - <%=game.getCurrentPlayer() %> <%} %></td>
					<td><%=threadRunning %> <%if (threadRunning) {%><input type="button" value="Remove" onclick="clickRemoveThread(<%=game.getID()%>)"><%}%></td>
				</tr>
				<%} %>
			</table>
		<%} else if (mapChallenge != null) {
			List<TournamentChallenge> listChallenge = new ArrayList<TournamentChallenge>(mapChallenge.values());
		
			%>
			<b>List of challenge in progress</b><br/>
			Date of last purge=<%=Constantes.timestamp2StringDateHour(ContextManager.getTournamentChallengeMgr().getDateLastPurge()) %><br/>
			Status : 0=init - 1=waiting - 2=play - 3=end
			<br/>
			<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
				<tr><th>ID</th><th>Creator</th><th>Partner</th><th>Date creation</th><th>Date expiration</th><th>Status</th><th>Date last status change</th><th>Table</th><th>Operation</th></tr>
				<%for (TournamentChallenge cha : listChallenge) {%>
				<tr>
					<td><%=cha.getID() %></td>
					<td><%=cha.getCreator().getID() %> - <%=cha.getCreator().getNickname() %></td>
					<td><%=cha.getPartner().getID() %> - <%=cha.getPartner().getNickname() %></td>
					<td><%=Constantes.timestamp2StringDateHour(cha.getDateCreation()) %></td>
					<td><%=Constantes.timestamp2StringDateHour(cha.getDateExpiration()) %></td>
					<td><%=cha.getStatus() %></td>
					<td><%=Constantes.timestamp2StringDateHour(cha.getDateLastStatusChange()) %></td>
					<td><%if(cha.getTable() != null) { %><%=cha.getTable().getID()%><%} %></td>
                    <td><input type="button" value="remove from map" onclick="clickRemoveChallengeFromMap(<%=cha.getID()%>)"></td>
				</tr>
				<%} %>
			</table>
		<%} %>
		</form>
	</body>
</html>