<%@page import="com.funbridge.server.common.Constantes"%>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.tournament.game.Tournament" %>
<%@ page import="com.funbridge.server.tournament.generic.TournamentGenericMgr" %>
<%@ page import="com.funbridge.server.tournament.generic.memory.GenericMemDeal" %>
<%@ page import="com.funbridge.server.tournament.generic.memory.GenericMemDealPlayer" %>
<%@ page import="com.funbridge.server.tournament.generic.memory.GenericMemTournament" %>
<%@ page import="java.util.List" %>
<%
    int category = Integer.parseInt(request.getParameter("category"));
    String dealID = request.getParameter("dealid");
    TournamentGenericMgr tourMgr = ContextManager.getTournamentMgrForCategory(category);
    Tournament tour = tourMgr.getTournamentWithDeal(dealID);
    GenericMemTournament memTour = null;
    GenericMemDeal memDeal = null;
    String tourID = "";

    if (tour != null) {
        tourID = tour.getIDStr();
        memTour = tourMgr.getMemoryMgr().getTournament(tourID);
        if (memTour != null) {
            memDeal = memTour.getMemDeal(dealID);
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
    <title>Funbridge Server - Administration Tournament Memory Detail Deal</title>
    <script type="text/javascript">
        function clickRemovePlayerResult(playerID) {
            if (confirm("Remove result for player="+playerID+" ? Very important : Be sure to have remove game before !!")) {
                document.forms["formGenericDeal"].operation.value="removePlayerResult";
                document.forms["formGenericDeal"].playerid.value=""+playerID;
                document.forms["formGenericDeal"].submit();
            }
        }
        function clickComputeRankingResult() {
            if (confirm("Compute ranking & result on this deal ?")) {
                document.forms["formGenericDeal"].operation.value = "computeRankingResult";
                document.forms["formGenericDeal"].submit();
            }
        }
    </script>
</head>
<body>
<h1>TOURNAMENT <%=Constantes.tourCategory2Name(category)%> MEMORY DETAIL DEAL</h1>
[<a href="adminGenericMemoryTournament.jsp?tourid=<%=tourID %>&category=<%=category%>">Back to Tournament Cache Detail</a>]
<br/><br/>
<%if (resultOperation.length() > 0) {%>
<b>RESULTAT OPERATION : <%= operation%></b>
<br/>Result = <%=resultOperation %>
<hr width="90%"/>
<%} %>
<%if (memDeal == null) {%>
<b>No memory data found for deal ID=<%=dealID %></b>
<%} else {%>
<form name="formGenericDeal" method="post" action="adminGenericMemoryDeal.jsp">
    <input type="hidden" name="operation" value=""/>
    <input type="hidden" name="category" value="<%=category%>"/>
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
            for (GenericMemDealPlayer resPla : (List<GenericMemDealPlayer>)memDeal.getResultListOrderByScore()) {
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