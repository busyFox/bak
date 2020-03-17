<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.tournament.serie.TourSerieMgr" %>
<%@ page import="com.funbridge.server.tournament.serie.data.TourSerieDeal" %>
<%@ page import="com.funbridge.server.tournament.serie.data.TourSerieTournament" %>
<%@ page import="com.funbridge.server.tournament.serie.memory.TourSerieMemDeal" %>
<%@ page import="com.funbridge.server.tournament.serie.memory.TourSerieMemDealPlayer" %>
<%@ page import="com.funbridge.server.tournament.serie.memory.TourSerieMemTour" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    String serie = request.getParameter("serie");
    String tourID = request.getParameter("tourID");
    String dealID = request.getParameter("dealID");
    TourSerieMgr tourMgr = ContextManager.getTourSerieMgr();
    TourSerieMemTour memTour = tourMgr.getMemoryMgr().getMemTour(serie, tourID);
    TourSerieMemDeal memDeal = null;
    if (memTour != null && dealID != null) {
        memDeal = memTour.getResultDeal(dealID);
    }

    if (operation != null) {
        if (operation.equals("deleteGame")) {
            if (memDeal != null) {
                String paramPlayerID = request.getParameter("playerID");
                long playerID = Long.parseLong(paramPlayerID);
                if (tourMgr.removeResultForTournamentInProgress(tourID, memDeal.dealIndex, playerID)) {
                    resultOperation = "Success to remove result memory and game for playerID="+playerID+" on dealIndex="+memDeal.dealIndex+" for tourID="+tourID;
                } else {
                    resultOperation = "Failed to remove result for playerID="+playerID+" on dealIndex="+memDeal.dealIndex+" for tourID="+tourID;
                }
            }
        }
    }
%>
<html>
    <head>
        <title>Funbridge Server - Administration Tournament SERIE</title>
        <script>
            function clickViewGame(gameID) {
                window.open("adminSerieViewGame.jsp?gameID="+gameID);
                return false;
            }
            function clickDeleteGame(playerID) {
                if (confirm("Delete game for playerID="+playerID)) {
                    document.forms["formTourSerieMemDeal"].operation.value = "deleteGame";
                    document.forms["formTourSerieMemDeal"].playerID.value = playerID;
                    document.forms["formTourSerieMemDeal"].submit();
                }
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION TOURNAMENT SERIE <%=serie.toUpperCase()%> - TOUR<%=tourID%></h1>
        [<a href="adminSerieMemTour.jsp?serie=<%=serie%>&tourID=<%=tourID%>">Back to Tournament Serie <%=tourID%></a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formTourSerieMemDeal" method="post" action="adminSerieMemDeal.jsp?serie=<%=serie%>&tourID=<%=tourID%>&dealID=<%=dealID%>">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="playerID" value=""/>
        <% if (memTour != null && memDeal != null) {
            TourSerieTournament tour = tourMgr.getTournament(tourID);
            TourSerieDeal deal = (TourSerieDeal)tour.getDealAtIndex(memDeal.dealIndex);
        %>
            Tournament : <%=tour.toString()%><br/>
            Deal : <%=deal.toString()%><br/>
            Nb player = <%=memDeal.getNbPlayer()%><br/>
            <br/>
            <b>Ranking</b><br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Result</th><th>PlayerID</th><th>Score</th><th>NbPlayerBestScore</th><th>NbPlayerSameScore</th><th>Contract</th><th>Declarer</th><th>NbTricks</th><th>Operation</th></tr>
                <%
                    List<Long> listPla = memDeal.getListPlayerOrderScore();
                    for (long pla : listPla) {
                        TourSerieMemDealPlayer rankPla = memDeal.getResultPlayer(pla);%>
                <tr>
                    <td><%=rankPla.result%></td>
                    <td><%=rankPla.playerID%></td>
                    <td><%=rankPla.score%></td>
                    <td><%=rankPla.nbPlayerBestScore%></td>
                    <td><%=rankPla.nbPlayerSameScore%></td>
                    <td><%=rankPla.getContractWS()%></td>
                    <td><%=rankPla.declarer%></td>
                    <td><%=rankPla.nbTricks%></td>
                    <td>
                        <input type="button" value="View Game" onclick="clickViewGame('<%=rankPla.gameID%>')">&nbsp;&nbsp;&nbsp;
                        <input type="button" value="Delete Game" onclick="clickDeleteGame(<%=rankPla.playerID%>)">
                    </td>
                </tr>
                <%}%>
            </table>
        <%}else{%>
            No memTour found for serie=<%=serie%> and tourID=<%=tourID%>
        <%}%>
        </form>
    </body>
</html>