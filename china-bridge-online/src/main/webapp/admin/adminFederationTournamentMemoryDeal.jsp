<%@ page import="com.funbridge.server.tournament.federation.FederationMgr" %>
<%@ page import="com.funbridge.server.tournament.federation.TourFederationMgr" %>
<%@ page import="com.funbridge.server.tournament.federation.data.TourFederationTournament" %>
<%@ page import="com.funbridge.server.tournament.federation.memory.TourFederationMemDeal" %>
<%@ page import="com.funbridge.server.tournament.federation.memory.TourFederationMemDealPlayer" %>
<%@ page import="com.funbridge.server.tournament.federation.memory.TourFederationMemTournament" %>
<%@ page import="com.funbridge.server.tournament.generic.memory.TournamentGenericMemoryMgr" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String dealID = request.getParameter("dealid");
    String federationName = request.getParameter("federationName");
    TourFederationMgr tourMgr = FederationMgr.getTourFederationMgr(federationName);
    TournamentGenericMemoryMgr memoryMgr = tourMgr.getMemoryMgr();
    TourFederationTournament tour = (TourFederationTournament) tourMgr.getTournamentWithDeal(dealID);
    TourFederationMemTournament memTour = null;
    TourFederationMemDeal memDeal = null;
    String tourID = "";

    if (tour != null) {
        tourID = tour.getIDStr();
        memTour = (TourFederationMemTournament)memoryMgr.getTournament(tourID);
        if (memTour != null) {
            memDeal = (TourFederationMemDeal)memTour.getMemDeal(dealID);
        }
    }

    String operation = request.getParameter("operation");
    String resultOperation = "";
    if (operation != null) {
        if (operation.equals("removePlayerResult")) {
            if (memDeal != null) {
                String strPlaID = request.getParameter("playerid");
                try {
                    long playerID = Long.parseLong(strPlaID);
                    boolean result = memDeal.removeResultPlayer(playerID);
                    if (result) {
                        memDeal.computeScoreData();
                    }
                    resultOperation = "Remove result on tourID=" + tourID + " - dealID=" + dealID + " for playerID=" + playerID + " result=" + result;
                } catch (NumberFormatException e) {
                    resultOperation = "Parameter playerid not valid : " + strPlaID;
                }
            } else {
                resultOperation = "No memDeal found for dealID=" + dealID;
            }
        }
        else if (operation.equals("computeRankingResult")) {
            if (memDeal != null) {
                memDeal.computeRanking();
                resultOperation = "Compute ranking & result done";
            } else {
                resultOperation = "ResultDeal not found for tourID=" + tourID + " - dealID=" + dealID;
            }
        }
        else {
            resultOperation = "Operation not supported : "+operation;
        }
    }
%>
<html>
    <head>
        <title>Funbridge Server - Administration <%=federationName%></title>
        <script type="text/javascript">
            function clickRemovePlayerResult(playerID) {
                if (confirm("Remove result for player="+playerID+" ? Very important : Be sure to have remove game before !!")) {
                    document.forms["formMemoryDetailDeal"].operation.value="removePlayerResult";
                    document.forms["formMemoryDetailDeal"].playerid.value=""+playerID;
                    document.forms["formMemoryDetailDeal"].submit();
                }
            }
            function clickComputeRankingResult() {
                if (confirm("Compute ranking & result on this deal ?")) {
                    document.forms["formMemoryDetailDeal"].operation.value = "computeRankingResult";
                    document.forms["formMemoryDetailDeal"].submit();
                }
            }
        </script>
    </head>
    <body>
        <h1>TOURNAMENT <%=federationName%> MEMORY DETAIL DEAL</h1>
        [<a href="adminFederationTournamentMemoryTournament.jsp?federationName=<%=federationName%>&tourid=<%=tourID %>">Back to Tournament Memory Detail</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <%if (memDeal == null) {%>
        <b>No memory data found for deal ID=<%=dealID %></b>
        <%} else {%>
        <form name="formMemoryDetailDeal" method="post" action="adminFederationTournamentMemoryDeal.jsp?federationName=<%=federationName%>">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="dealid" value="<%=dealID%>"/>
            <input type="hidden" name="playerid" value=""/>
            Tournament : <%= (tour!=null?tour.toString():"Tour is null")%><br/>
            Memory Deal =<%=memDeal %><br/>
            <input type="button" value="Compute result & ranking" onclick="clickComputeRankingResult()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <br/>
            <b>Ranking :</b>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Result</th><th>Rank</th><th>Rank Tour</th><th>Player</th><th>score</th><th>nbPlayerBestScore</th><th>nbPlayerSameScore</th><th>Contract</th><th>Declarer</th><th>Nb Tricks</th><th>Lead</th><th>Operation</th></tr>
                <%
                    for (TourFederationMemDealPlayer resPla : (List<TourFederationMemDealPlayer>)memDeal.getResultListOrderByScore()) {
                %>
                <tr>
                    <td><%=resPla.result %></td>
                    <td><%=resPla.nbPlayerBetterScore+1 %></td>
                    <td><%=memDeal.getMemTour().getTournamentPlayer(resPla.playerID).ranking %></td>
                    <td><%=resPla.playerID%></td>
                    <td><%=resPla.score %></td>
                    <td><%=resPla.nbPlayerBetterScore %></td>
                    <td><%=resPla.nbPlayerSameScore %></td>
                    <td><%=resPla.getContractWS() %></td>
                    <td><%=resPla.declarer %></td>
                    <td><%=resPla.nbTricks %></td>
                    <td><%=resPla.begins%></td>
                    <td><input type="button" value="Remove player result" onclick="clickRemovePlayerResult(<%=resPla.playerID%>)"></td>
                </tr>
                <%}
                %>
            </table>
        </form>
        <%} %>
    </body>
</html>
