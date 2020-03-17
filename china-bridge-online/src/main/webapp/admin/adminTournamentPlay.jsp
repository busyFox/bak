<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.player.data.Player"%>
<%@page import="com.funbridge.server.tournament.duel.data.DuelTournament"%>
<%@page import="com.funbridge.server.tournament.game.Tournament"%>
<%@page import="com.funbridge.server.tournament.serie.data.TourSerieTournament"%>
<%@page import="com.funbridge.server.tournament.team.data.TeamTournament"%>
<%@page import="com.funbridge.server.tournament.timezone.data.TimezoneTournament"%>
<%@page import="com.funbridge.server.ws.support.SupportService"%>
<%@page import="com.funbridge.server.ws.support.WSSupGamePlay"%>
<%@page import="com.funbridge.server.ws.support.WSSupResponse"%>
<%@ page import="com.funbridge.server.ws.support.WSSupTournamentPlay" %>
<%@ page import="java.util.List" %>
<html>
<%
    String paramOperation = request.getParameter("operation");
    if (paramOperation == null) {
        paramOperation = "";
    }
    String resultOperation = "";
    String strOffset = request.getParameter("offset");
    String strPlayerID = request.getParameter("playerID");
    String strCategory = request.getParameter("category");
    Player player = null;
    long playerID = -1;
    if (strPlayerID != null && strPlayerID.length() != 0) {
        playerID = Long.parseLong(strPlayerID);
        player = ContextManager.getPlayerMgr().getPlayer(playerID);
    }
    int category = -1;
    if (strCategory != null) {
        category = Integer.parseInt(strCategory);
    }
    int offset = 0;
    if (strOffset != null && strOffset.length() > 0) {
        offset = Integer.parseInt(strOffset);
    }
    String strTourID = request.getParameter("tourID");
    int nbMax = 20;
    List<WSSupTournamentPlay> listTourPlay = null;
    List<WSSupGamePlay> listGamePlay = null;
    if (paramOperation != null) {
        if (paramOperation.equals("listTourPlay")) {
            if (player == null) {
                resultOperation = "listTourPlay Failed - no player found with ID=" + strPlayerID;
            } else if (category == -1) {
                resultOperation = "listTourPlay Failed - category not valid ! ";
            } else {
                SupportService.WSSupListTournamentPlayForPlayerParam param = new SupportService.WSSupListTournamentPlayForPlayerParam();
                param.nbMax = nbMax;
                param.offset = offset;
                param.playerID = playerID;
                param.category = category;
                WSSupResponse supResponse = ContextManager.getSupportService().listTournamentPlayForPlayer(param);
                if (supResponse.isResponseException()) {
                    resultOperation = "listTournament Failed - Result Exception=" + supResponse.getException();
                } else {
                    listTourPlay = (List<WSSupTournamentPlay>) supResponse.getData();
                }
            }
        }
        else if (paramOperation.equals("listGamePlay")) {
            if (player == null) {
                resultOperation = "listGamePlay Failed - no player found with ID=" + strPlayerID;
            } else if (category == -1) {
                resultOperation = "listGamePlay Failed - category not valid ! ";
            } else if (strTourID == null|| strTourID.length() == 0) {
                resultOperation = "listGamePlay Failed - tourID is null ! ";
            } else {
                SupportService.WSSupListGamePlayForTournamentAndPlayerParam param = new SupportService.WSSupListGamePlayForTournamentAndPlayerParam();
                param.category = category;
                param.playerID = playerID;
                param.tourIDstr = strTourID;
                WSSupResponse supResponse = ContextManager.getSupportService().listGamePlayForTournamentAndPlayer(param);
                if (supResponse.isResponseException()) {
                    resultOperation = "listTournament Failed - Result Exception=" + supResponse.getException();
                } else {
                    listGamePlay = (List<WSSupGamePlay>) supResponse.getData();
                }
            }
        }
    }
%>
	<head>
		<title>Funbridge Server - Tournament Play</title>
		<script type="text/javascript">
		function clickListTournament(valOffset) {
			document.forms["formTourPlay"].operation.value = "listTourPlay";
			document.forms["formTourPlay"].offset.value = valOffset;
			document.forms["formTourPlay"].submit();
		}
		function clickTournamentDetail(pla, tour) {
			document.forms["formTourPlay"].operation.value = "listGamePlay";
			document.forms["formTourPlay"].playerID.value=pla;
			document.forms["formTourPlay"].tourID.value=tour;
			document.forms["formTourPlay"].submit();
		}
		function clickGameDetail(pla, category, deal) {
			window.open("viewGame.jsp?operation=viewGame&playerID="+pla+"&category="+category+"&dealID="+deal);
			return false;
		}
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION TOURNAMENT PLAYED</h1>
		<a href="admin.jsp">Administration</a><br/><br/>
		<%if (resultOperation.length() > 0) {%>
		<b>RESULTAT OPERATION : <%= paramOperation%></b>
		<br/>Result = <%=resultOperation %>
		<%} %>
		<hr width="90%"/>
		<form name="formTourPlay" action="adminTournamentPlay.jsp" method="post">
			List Tournament for player<br/>
			<input type="hidden" name="operation" value=""/>
			<input type="hidden" name="tourID" value=""/>
			<input type="hidden" name="offset" value=""/>
			Player ID = <input type="text" name="playerID" value="<%=(strPlayerID!=null?strPlayerID:"")%>"/><br/>
			Category = 
			<input type="radio" name="category" value="<%=Constantes.TOURNAMENT_CATEGORY_TRAINING %>" <%=(category==Constantes.TOURNAMENT_CATEGORY_TRAINING?"checked=\"checked\"":"") %>/> TRAINING&nbsp;-&nbsp;
			<input type="radio" name="category" value="<%=Constantes.TOURNAMENT_CATEGORY_NEWSERIE %>" <%=(category==Constantes.TOURNAMENT_CATEGORY_NEWSERIE?"checked=\"checked\"":"") %>/> SERIE&nbsp;-&nbsp;
			<input type="radio" name="category" value="<%=Constantes.TOURNAMENT_CATEGORY_DUEL %>" <%=(category==Constantes.TOURNAMENT_CATEGORY_DUEL?"checked=\"checked\"":"") %>/> DUEL&nbsp;-&nbsp;
			<input type="radio" name="category" value="<%=Constantes.TOURNAMENT_CATEGORY_TIMEZONE %>" <%=(category==Constantes.TOURNAMENT_CATEGORY_TIMEZONE?"checked=\"checked\"":"") %>/> TIMEZONE&nbsp;-&nbsp;
            <input type="radio" name="category" value="<%=Constantes.TOURNAMENT_CATEGORY_TEAM %>" <%=(category==Constantes.TOURNAMENT_CATEGORY_TEAM?"checked=\"checked\"":"") %>/> TEAM&nbsp;-&nbsp;
            <input type="radio" name="category" value="<%=Constantes.TOURNAMENT_CATEGORY_PRIVATE %>" <%=(category==Constantes.TOURNAMENT_CATEGORY_PRIVATE?"checked=\"checked\"":"") %>/> PRIVATE&nbsp;-&nbsp;
            <input type="radio" name="category" value="<%=Constantes.TOURNAMENT_CATEGORY_TOUR_CBO %>" <%=(category==Constantes.TOURNAMENT_CATEGORY_TOUR_CBO ?"checked=\"checked\"":"") %>/> CBO&nbsp;-&nbsp;
			<br/>
			<input type="button" value="List Tournament" onclick="clickListTournament(0)"/>
			<br/><br/>
			<%if (player != null) {%>
			<b>Player</b> :<br/><%=player %><br/>
			<%} %>
			<%if (strTourID != null && strTourID.length() > 0) {
                String strTournament = "";
                if (category == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
                    TimezoneTournament tour = ContextManager.getTimezoneMgr().getTournament(strTourID);
                    if (tour != null) {
                        strTournament = tour.toString();
                    }
                }
                else if (category == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
                    Tournament tour = ContextManager.getTrainingMgr().getTournament(strTourID);
                    if (tour != null) {
                        strTournament = tour.toString();
                    }
                }
                else if (category == Constantes.TOURNAMENT_CATEGORY_DUEL) {
                    DuelTournament tour = ContextManager.getDuelMgr().getTournament(strTourID);
                    if (tour != null) {
                        strTournament = tour.toString();
                    }
                }
                else if (category == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
                    TourSerieTournament tour = ContextManager.getTourSerieMgr().getTournament(strTourID);
                    if (tour != null) {
                        strTournament = tour.toString();
                    }
                }
                else if (category == Constantes.TOURNAMENT_CATEGORY_TEAM) {
                    TeamTournament tour = ContextManager.getTourTeamMgr().getTournament(strTourID);
                    if (tour != null) {
                        strTournament = tour.toString();
                    }
                }
                else if (category == Constantes.TOURNAMENT_CATEGORY_TOUR_CBO) {
                    Tournament tour = ContextManager.getTourCBOMgr().getTournament(strTourID);
                    if (tour != null) {
                        strTournament = tour.toString();
                    }
                }
                else if (category == Constantes.TOURNAMENT_CATEGORY_PRIVATE) {
                    Tournament tour = ContextManager.getPrivateTournamentMgr().getTournament(strTourID);
                    if (tour != null) {
                        strTournament = tour.toString();
                    }
                }
            %>
			<b>Tournament</b> :<br/><%=strTournament %><br/>
			<%} %>
			<% if (listTourPlay != null) {%>
            <%if (offset > 0) {%>
            <input type="button" value="previous" onclick="clickListTournament(<%=offset-nbMax%>)"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%} %>
            <%if (listTourPlay.size() == nbMax) {%>
            <input type="button" value="next" onclick="clickListTournament(<%=offset+nbMax%>)"/>
            <%} %>
			<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
				<tr><th>Tour ID</th><th>Tour Name</th><th>ResultType</th><th>Finished</th><th>Begin Date</th><th>End Date</th><th>Nb Player</th><th>Player Start</th><th>Player End</th><th>Rank</th><th>Result</th><th>Player Finished</th><th>Operation</th></tr>
				<% for (WSSupTournamentPlay e : listTourPlay){%>
                <tr>
                    <td><%=e.tourIDstr%></td>
                    <td><%=e.name%></td>
                    <td><%=Constantes.tournamentResultType2String(e.resultType)%></td>
                    <td><%=e.finished%></td>
                    <td><%=e.dateStart%></td>
                    <td><%=e.dateEnd%></td>
                    <td><%=e.nbPlayer%></td>
                    <td><%=e.playerDateStart%></td>
                    <td><%=e.playerDateLast%></td>
                    <td><%=e.playerRank%></td>
                    <td><%=e.playerResult%></td>
                    <td><%=e.playerFinished%></td>
                    <td><input type="button" value="Detail" onclick="clickTournamentDetail(<%=player.getID()%>,'<%=e.tourIDstr%>')"/></td>
                </tr>
                <%}%>
			</table>
			<br/>
			<%if (offset > 0) {%>
				<input type="button" value="previous" onclick="clickListTournament(<%=offset-nbMax%>)"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
			<%} %>
			<%if (listTourPlay.size() == nbMax) {%>
				<input type="button" value="next" onclick="clickListTournament(<%=offset+nbMax%>)"/>
			<%} %>
			<%} %>
			</br/>
			<%if (listGamePlay != null) {%>
			<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
				<tr><th>Game ID</th><th>Deal ID</th><th>Finished</th><th>Start Date</th><th>Last Date</th><th>Rank</th><th>Result</th><th>Score</th><th>Contract</th><th>Operation</th></tr>
                <% for (WSSupGamePlay e : listGamePlay) {%>
                <tr>
                    <td><%=e.game.gameIDstr%></td>
                    <td><%=e.deal.dealIDstr%></td>
                    <td><%=e.game.finished%></td>
                    <td><%=e.game.dateStart%></td>
                    <td><%=e.game.dateLast%></td>
                    <td><%=e.game.rank%></td>
                    <td><%=e.game.result%></td>
                    <td><%=e.game.score%></td>
                    <td><%=e.game.contract%> - <%=e.game.declarer%></td>
                    <td><input type="button" value="Detail" onclick="clickGameDetail(<%=playerID%>,<%=category%>,'<%=e.deal.dealIDstr%>')"/></td>
                </tr>
                <%}%>
			</table>
			<%} %>
		</form>
		
	</body>
</html>