<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.tournament.duel.DuelMemoryMgr"%>
<%@ page import="com.funbridge.server.tournament.duel.DuelMgr" %>
<%@ page import="com.funbridge.server.tournament.duel.data.DuelTournamentPlayer" %>
<%@ page import="com.funbridge.server.tournament.duel.memory.DuelMemDeal" %>
<%@ page import="com.funbridge.server.tournament.duel.memory.DuelMemGame" %>
<%@ page import="com.funbridge.server.tournament.duel.memory.DuelMemTournament" %>
<%@ page import="com.funbridge.server.ws.result.WSResultTournamentPlayer" %>
<%
    DuelMgr duelMgr = ContextManager.getDuelMgr();
    DuelMemoryMgr duelMemoryMgr = ContextManager.getDuelMgr().getMemoryMgr();
    String tourPlayerID = request.getParameter("tourPlayerID");
    String operation = request.getParameter("operation");
    String resultOperation = "";

    if (operation != null) {
        if (operation.equals("finish")) {
            if (ContextManager.getDuelMgr().finishDuel(tourPlayerID)) {
                resultOperation = "SUCCESS to finish duelTournamentPlayer with ID="+tourPlayerID;
            } else {
                resultOperation = "FAILED to finish duelTournamentPlayer with ID="+tourPlayerID;
            }
        } else if (operation.equals("backupData")) {
            String fileBackup = duelMemoryMgr.backupMemTournamentToFile(tourPlayerID);
            if (fileBackup != null) {
                resultOperation += "Success to backup DuelMemTournament :" + fileBackup;
            } else {
                resultOperation += "Failed to backup DuelMemTournament with ID=" + tourPlayerID;
            }

        }
        else if (operation.equals("removeTournamentDuel")){
            DuelTournamentPlayer duelTournamentPlayer = duelMgr.getTournamentPlayer(tourPlayerID);
            if (duelTournamentPlayer != null) {
                ContextManager.getPresenceMgr().closeSessionForPlayer(duelTournamentPlayer.getPlayer1ID(), null);
                ContextManager.getPresenceMgr().closeSessionForPlayer(duelTournamentPlayer.getPlayer2ID(), null);

                ContextManager.getDuelMgr().removeDuel(duelTournamentPlayer.getIDStr(),
                        true,
                        true,
                        true);
                resultOperation = "Sucess to remove duelTournamentPlayer = "+duelTournamentPlayer;
            } else {
                if (request.getParameter("forceOperation") != null && request.getParameter("forceOperation").equals("1")) {
                    ContextManager.getDuelMgr().getMemoryMgr().removeMemTournament(tourPlayerID);
                    resultOperation = "No duelTournamentPlayer found with tourPlayerID = " + tourPlayerID + " => force to remove directly on memory mgr";
                } else {
                    resultOperation = "No duelTournamentPlayer found with tourPlayerID = " + tourPlayerID;
                }

            }
        }
        else if (operation.equals("reloadData")) {
            if (duelMgr.reloadDuelInMemory(tourPlayerID)) {
                resultOperation = "Success to reload in memory duelTournamentPlayer with ID="+tourPlayerID;
            } else {
                resultOperation = "No duelTournamentPlayer found with tourPlayerID = "+tourPlayerID+" or duelMemTournament not found";
            }
        }
        else if (operation.equals("runPlayArgine")) {
            DuelMemTournament duelMemTournament = duelMemoryMgr.getDuelMemTournament(tourPlayerID);
            if (duelMemTournament != null) {
                if (duelMemTournament.isDuelWithArgine()) {
                    if (!duelMemTournament.isAllPlayedForPlayer(Constantes.PLAYER_ARGINE_ID)) {
                        resultOperation = "Start play argine on tournament=" + duelMemTournament + " - start="+duelMgr.startPlayArgine(duelMemTournament.tourID);
                    } else {
                        resultOperation = "Argine has already played all deals !!";
                    }
                } else {
                    resultOperation = "This tournament is not a duel with Argine !! memTournament="+duelMemTournament;
                }
            } else {
                resultOperation = "No memTournament found for tourPlayerID="+tourPlayerID;
            }
        }
        else {
            resultOperation = "Operation not supported : "+operation;
        }
    }
    DuelMemTournament duelMemTournament = duelMemoryMgr.getDuelMemTournament(tourPlayerID);
    String paramDealIndex = request.getParameter("dealIndex");
    DuelMemDeal memDeal = null;
    if (paramDealIndex != null && paramDealIndex.length() > 0 && duelMemTournament != null) {
        memDeal = duelMemTournament.getMemDeal(Integer.parseInt(paramDealIndex));
    }

    if (operation != null) {
        if (operation.equals("computeResultDuel")){
            if (duelMemTournament != null) {
                duelMemTournament.computeResultRanking();
                resultOperation = "Success to compute result on duel";
            } else {
                resultOperation = "Failed to compute result on duel";
            }
        }
        else if (operation.equals("computeResultDeal")){
            if (memDeal != null) {
                memDeal.computeResult();
                resultOperation = "Success to compute result on deal="+memDeal;
            } else {
                resultOperation = "Failed to compute result on deal";
            }
        }
    }
%>
<html>
	<head>
		<title>Funbridge Server - Administration</title>
		<script type="text/javascript">
            function clickFinish() {
                if (confirm("Finish duel with tourPlayerID=<%=tourPlayerID%> ?")) {
                    document.forms["formDuel"].operation.value = "finish";
                    document.forms["formDuel"].submit();
                }
            }
            function clickBackupData() {
                if (confirm("Backup all data to file ?")) {
                    document.forms["formDuel"].operation.value="backupData";
                    document.forms["formDuel"].submit();
                }
            }
            function clickDetailDeal(dealIndex) {
                document.forms["formDuel"].dealIndex.value=dealIndex;
                document.forms["formDuel"].submit();
            }
            function clickComputeResultDeal() {
                document.forms["formDuel"].operation.value="computeResultDeal";
                document.forms["formDuel"].submit();
            }
            function clickComputeResultDuel() {
                document.forms["formDuel"].operation.value="computeResultDuel";
                document.forms["formDuel"].submit();
            }
            function clickRemoveTournamentDuel() {
                if (confirm("Remove tournament duel ?")) {
                    if (confirm("Force operation if necessary ?")) {
                        document.forms["formDuel"].forceOperation.value = "1";
                    } else {
                        document.forms["formDuel"].forceOperation.value = "0";
                    }
                    document.forms["formDuel"].operation.value = "removeTournamentDuel";
                    document.forms["formDuel"].submit();
                }
            }
            function clickReloadData() {
                if (confirm("Reload data from DB ?")) {
                    document.forms["formDuel"].operation.value="reloadData";
                    document.forms["formDuel"].submit();
                }
            }
            function clickRunPlayArgine() {
                if (confirm("Run Play Argine thread ?")) {
                    document.forms["formDuel"].operation.value="runPlayArgine";
                    document.forms["formDuel"].submit();
                }
            }
		</script>
	</head>
	<body>
		<h1>TOURNAMENT MEMORY DETAIL</h1>
		[<a href="adminDuel.jsp">Back to Duel Administration</a>]
		<br/><br/>
		<%if (operation != null) {%>
		Operation = <%=operation %> - Result = <%=resultOperation %> <br/>
		<%} %>
		<%if (duelMemTournament == null) {%>
		No duelResult found in memory for tourPlayerID <%=tourPlayerID %>
		<%} else {
		DuelTournamentPlayer duelTournamentPlayer = ContextManager.getDuelMgr().getTournamentPlayer(tourPlayerID);
		%>
		DuelTournamentPlayer : <%= duelTournamentPlayer!=null?duelTournamentPlayer.toString():"no duelTournamentPlayer found for ID="+tourPlayerID%>
		<br/>
        <br/>
        <form name="formDuel" method="post" action="adminDuelDetail.jsp">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="forceOperation" value="0"/>
            <input type="hidden" name="tourPlayerID" value="<%=tourPlayerID%>"/>
            <input type="hidden" name="dealIndex" value="<%=(paramDealIndex!=null?paramDealIndex:"")%>"/>
            <input type="button" value="Backup !" onclick="clickBackupData()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Finish it !" onclick="clickFinish()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Compute Result Duel !" onclick="clickComputeResultDuel()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Reload data from DB !" onclick="clickReloadData()"/>
            <br/><br/>
            <input type="button" value="Remove Tournament Duel !" onclick="clickRemoveTournamentDuel()"/>
            <br/><br/>
            <% if (duelTournamentPlayer != null && duelTournamentPlayer.isDuelWithArgine()) {%>
            <input type="button" value="Run play argine" onclick="clickRunPlayArgine()"/>
            <%}%>
            <br/>
            <% if (memDeal != null) {%>
            <hr width="95%">
            Deal ID=<%=memDeal.dealID %> - Nb Score=<%=memDeal.getNbScore() %> - Score average=<%=memDeal.getScoreAverage() %><br/>
            <input type="button" value="Compute Result" onclick="clickComputeResultDeal()">
            <br/>
            <b>Ranking :</b>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Result</th><th>Player</th><th>score</th><th>Contract</th><th>Declarer</th><th>Nb Tricks</th><th>GameID</th></tr>
                <%
                    for (DuelMemGame e : memDeal.listMemGame()) {%>
                <tr>
                    <td><%=e.result %></td>
                    <td><%=e.playerID %></td>
                    <td><%=e.score %></td>
                    <td><%=e.getContractWS() %></td>
                    <td><%=e.declarer %></td>
                    <td><%=e.nbTricks %></td>
                    <td><%=e.gameID %></td>
                </tr>
                <%}%>
            </table>
            <hr width="95%">
            <%}%>

            <b>List of Deals:</b>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Deal ID</th><th>Nb Player</th><th>Score Average</th><th>Operation</th></tr>
            <%for (DuelMemDeal e : duelMemTournament.tabDeal) {%>
                <tr><td><%=e.dealID %></td><td><%=e.getNbScore() %></td><td><%=e.getScoreAverage() %></td><td><input type="button" value="Detail" onclick="clickDetailDeal(<%=e.dealIndex%>)"></td></tr>
            <%}%>
            </table>
            <br/>
            <b>Ranking :</b>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Rank</th><th>PlayerID</th><th>Player Pseudo</th><th>Result</th><th>Nb Deal Played</th></tr>
                <% WSResultTournamentPlayer e1 = duelMemTournament.getWSResultTournamentPlayer(ContextManager.getPlayerCacheMgr().getPlayerCache(duelMemTournament.player1ID));%>
                <tr><td><%=e1.getRank() %></td><td><%=e1.getPlayerID() %></td><td><%=e1.getPlayerPseudo() %></td><td><%=e1.getResult() %></td><td><%=e1.getNbDealPlayed() %></td></tr>
                <% WSResultTournamentPlayer e2 = duelMemTournament.getWSResultTournamentPlayer(ContextManager.getPlayerCacheMgr().getPlayerCache(duelMemTournament.player2ID));%>
                <tr><td><%=e2.getRank() %></td><td><%=e2.getPlayerID() %></td><td><%=e2.getPlayerPseudo() %></td><td><%=e2.getResult() %></td><td><%=e2.getNbDealPlayed() %></td></tr>
            </table>
            <%} %>
        </form>
	</body>
</html>