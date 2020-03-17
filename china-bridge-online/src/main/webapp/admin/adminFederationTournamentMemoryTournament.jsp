<%@ page import="com.funbridge.server.tournament.federation.FederationMgr" %>
<%@ page import="com.funbridge.server.tournament.federation.TourFederationMgr" %>
<%@ page import="com.funbridge.server.tournament.federation.data.TourFederationTournament" %>
<%@ page import="com.funbridge.server.tournament.federation.memory.TourFederationMemDeal" %>
<%@ page import="com.funbridge.server.tournament.federation.memory.TourFederationMemTournament" %>
<%@ page import="com.funbridge.server.tournament.generic.memory.TournamentGenericMemoryMgr" %>
<%@ page import="com.funbridge.server.ws.result.WSResultTournamentPlayer" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String federationName = request.getParameter("federationName");
    TourFederationMgr tourMgr = FederationMgr.getTourFederationMgr(federationName);
    TournamentGenericMemoryMgr memoryMgr = tourMgr.getMemoryMgr();
    String tourID = request.getParameter("tourid");
    String operation = request.getParameter("operation");
    String resultOperation = "";
    TourFederationTournament tour = (TourFederationTournament) tourMgr.getTournament(tourID);

    if (operation != null) {
        if (operation.equals("delete")) {
            resultOperation = "Delete tournament from memory : ";
            if (memoryMgr.removeTournament(tourID)) {
                resultOperation += "Sucess";
            } else {
                resultOperation += "Failed";
            }
        } else if (operation.equals("finish")) {
            if (tour != null) {
                if (tourMgr.finishTournament(tour)) {
                    resultOperation += "Sucess finish timezone tour="+tour;
                } else {
                    resultOperation += "Failed to finished timezone tour="+tour;
                }
            } else {
                resultOperation += "No tournament found with ID="+tourID;
            }
        } else if (operation.equals("backup")) {
            resultOperation = "Backup tournament to file : "+tourID;
            TourFederationMemTournament mtr = (TourFederationMemTournament) memoryMgr.getTournament(tourID);
            if (mtr != null) {
                if (memoryMgr.backupMemTourResultToFile(mtr)) {
                    resultOperation += "<br/>Sucess to transform memTourResult to string mtr="+mtr;
                } else {
                    resultOperation += "<br/>Failed to transform memTourResult to string mtr="+mtr;
                }
            }
        } else if (operation.equals("computeResult")) {
            resultOperation = "Time to compute result : ";
            TourFederationMemTournament mtr = (TourFederationMemTournament) memoryMgr.getTournament(tourID);
            long ts = System.currentTimeMillis();
            mtr.computeResult();
            resultOperation += (System.currentTimeMillis() - ts)+" ms";
        } else if (operation.equals("addPlayer")) {
            String strPlayerID = request.getParameter("playerIdToAdd");
            try {
                long playerID = Long.parseLong(strPlayerID);


                // add player on mem tournament
                TourFederationMemTournament mtr = (TourFederationMemTournament) memoryMgr.getTournament(tourID);
                mtr.addPlayer(playerID, System.currentTimeMillis());

                resultOperation = "Success to add playerID="+strPlayerID+" to tournament";
            } catch (Exception e) {
                resultOperation = "Exception to add playerID="+strPlayerID+" - e="+e.getMessage();
            }
        } else if (operation.equals("resetCloseInProgress")) {
            TourFederationMemTournament mtr = (TourFederationMemTournament) memoryMgr.getTournament(tourID);
            mtr.closeInProgress = false;
            resultOperation = "Success to reset flag close in progress";
        }
    }

    TourFederationMemTournament mtr = (TourFederationMemTournament) memoryMgr.getTournament(tourID);

    int rankingOffset = 0;
    int rankingNbMax = 50;
    String paramRankingOffset = request.getParameter("rankingoffset");
    if (paramRankingOffset != null) {
        rankingOffset = Integer.parseInt(paramRankingOffset);
        if (rankingOffset < 0) {
            rankingOffset = 0;
        }
    }
    boolean useRankingFinished = true;
    String paramRankingFinished = request.getParameter("rankingfinished");
    if (paramRankingFinished != null) {
        if (paramRankingFinished.equals("0")) {
            useRankingFinished = false;
        }
    }
    List<WSResultTournamentPlayer> listResultPlayer = tourMgr.getListWSResultTournamentPlayer(tour, rankingOffset, rankingNbMax, null, -1, useRankingFinished, null);
%>
<html>
    <head>
        <title>Funbridge Server - Administration <%= federationName %>%></title>
        <script type="text/javascript">
            function clickComputeResult() {
                if (confirm("Compute result on this tournament ?")) {
                    document.forms["formFederationTournament"].operation.value="computeResult";
                    document.forms["formFederationTournament"].submit();
                }
            }
            function clickDelete() {
                if (confirm("Delete this tournament from memory ?")) {
                    document.forms["formFederationTournament"].operation.value="delete";
                    document.forms["formFederationTournament"].submit();
                }
            }
            function clickBackup() {
                if (confirm("Backup this tournament to file ?")) {
                    document.forms["formFederationTournament"].operation.value="backup";
                    document.forms["formFederationTournament"].submit();
                }
            }
            function clickFinish() {
                if (confirm("Finish this tournament ?")) {
                    document.forms["formFederationTournament"].operation.value="finish";
                    document.forms["formFederationTournament"].submit();
                }
            }
            function clickRankingOffset(offset) {
                document.forms["formFederationTournament"].rankingoffset.value=offset;
                document.forms["formFederationTournament"].submit();
            }
            function changeCheckFinish(value) {
                document.forms["formFederationTournament"].rankingfinished.value=value;
                document.forms["formFederationTournament"].submit();
            }
            function clickAddPlayer() {
                if (confirm("Add playerID="+document.forms["formFederationTournament"].playerIdToAdd.value+" to tournament ?")) {
                    document.forms["formFederationTournament"].operation.value="addPlayer";
                    document.forms["formFederationTournament"].submit();
                }
            }
            function clickResetCloseInProgress() {
                if (confirm("Reset flag close in progress for tournament ?")) {
                    document.forms["formFederationTournament"].operation.value = "resetCloseInProgress";
                    document.forms["formFederationTournament"].submit();
                }
            }
        </script>
    </head>
    <body>
        <h1>TOURNAMENT <%= federationName %> MEMORY DETAIL</h1>
        [<a href="adminCBOFederation.jsp?federationName=<%=federationName%>">Back to Tournament <%= federationName %> list</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formFederationTournament" method="post" action="adminFederationTournamentMemoryTournament.jsp?federationName=<%=federationName%>">
            <input type="hidden" name="tourid" value="<%=tourID%>"/>
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="rankingoffset" value="<%=rankingOffset%>"/>
            <input type="hidden" name="rankingfinished" value="<%=useRankingFinished?"1":"0"%>"/>
            <input type="button" value="Compute result" onclick="clickComputeResult()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Delete from memory" onclick="clickDelete()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Backup file" onclick="clickBackup()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Finish tournament" onclick="clickFinish()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Remove flag close in progress" onclick="clickResetCloseInProgress()"/>
            <br/>
            Add player to tournament : <input type="text" name="playerIdToAdd">&nbsp;&nbsp;&nbsp;<input type="button" value="Add player" onclick="clickAddPlayer()"><br/>
            <%if (tour == null) {%>
            Tournament ID <%=tourID %> not valid. No tournament found !
            <%} else {%>
            <b>Tournament</b> : <%= tour.toString()%><br/>
            <%} %>
            <%if (mtr == null) { %>
            No more data for this tournament in memory
            <%} else { %>
            <b>Tournament Memory</b> : closeInProgress=<%=mtr.closeInProgress%> - nbDealsToPlay=<%=mtr.nbDealsToPlay%> - nb Player=<%=mtr.getNbPlayer() %> - nb Player finish=<%=mtr.getNbPlayerFinishAll() %><br/>
            <%}%>

            <b>List of Deals:</b>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>ID</th><th>Nb Player</th><th>Operation</th></tr>
                <%
                    TourFederationMemDeal[] resultDeal = (TourFederationMemDeal[])mtr.deals;
                    for (int i = 0; i < resultDeal.length; i++) {
                        TourFederationMemDeal mrd = resultDeal[i];
                %>
                <tr><td><%=mrd.dealID %></td><td><%=mrd.getNbPlayers() %></td><td><a href="adminFederationTournamentMemoryDeal.jsp?federationName=<%=federationName%>&dealid=<%=mrd.dealID %>">Detail</a></td></tr>

                <%}
                %>
            </table>
            <br/>
            <b>Ranking :</b><br/>
            Only player finished tournament : <input type="checkbox" name="checkFinish" <%=useRankingFinished?"checked":""%> onchange="changeCheckFinish(<%=useRankingFinished?'0':'1'%>)"><br/>
            <%if(rankingOffset > 0) {%>
            <input type="button" value="Previous" onclick="clickRankingOffset(<%=rankingOffset - rankingNbMax%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (listResultPlayer.size() >= rankingNbMax) {%>
            <input type="button" value="Next" onclick="clickRankingOffset(<%=rankingOffset + rankingNbMax%>)">
            <%}%>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Rank</th><th>Finisher</th><th>PlayerID</th><th>Player Pseudo</th><th>Result</th><th>Nb Deal Played</th></tr>
                <%
                    for (WSResultTournamentPlayer resPla : listResultPlayer) {
                %>
                <tr><td><%=resPla.getRank() %></td><td><%=mtr.hasPlayerFinish(resPla.getPlayerID())%></td><td><%=resPla.getPlayerID() %></td><td><%=resPla.getPlayerPseudo() %></td><td><%=resPla.getResult() %></td><td><%=resPla.getNbDealPlayed() %></td></tr>
                <%}%>
            </table>
        </form>
    </body>
</html>
