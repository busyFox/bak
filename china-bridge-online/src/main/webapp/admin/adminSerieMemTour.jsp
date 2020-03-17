<%@ page import="com.funbridge.server.tournament.serie.TourSerieMgr" %>
<%@ page import="com.funbridge.server.tournament.serie.data.TourSerieTournament" %>
<%@ page import="com.funbridge.server.tournament.serie.memory.TourSerieMemDeal" %>
<%@ page import="com.funbridge.server.tournament.serie.memory.TourSerieMemPeriodRanking" %>
<%@ page import="com.gotogames.common.tools.StringTools" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="adminSerieOperation.jsp"%>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    String serie = request.getParameter("serie");
    String tourID = request.getParameter("tourID");
    TourSerieMgr tourMgr = ContextManager.getTourSerieMgr();
    TourSerieMemTour memTour = tourMgr.getMemoryMgr().getMemTour(serie, tourID);
    if (operation != null) {
        if (operation.equals("finish")) {
            long ts = System.currentTimeMillis();
            resultOperation = "Finish operation="+tourMgr.getMemoryMgr().finishTournament(serie, tourID);
            resultOperation +=" - serie="+serie+" - tourID="+tourID+" - TS="+(System.currentTimeMillis() - ts);
        }
        else if (operation.equals("backup")) {
            long ts = System.currentTimeMillis();
            if (tourMgr.getMemoryMgr().backupMemTourToFile(tourMgr.getMemoryMgr().getMemTour(serie, tourID))) {
                resultOperation = "Success to backup memTour - ts="+(System.currentTimeMillis() - ts);
            } else {
                resultOperation = "Failed to backup memTour";
            }
        }
        else if (operation.equals("updateResultPeriodRanking")) {
            long playerID = Long.parseLong(request.getParameter("playerID"));
            TourSerieMemTourPlayer rankPla = memTour.getRankingPlayer(playerID);
            if (rankPla != null) {
                if (rankPla.getNbDealsPlayed() == 4) {
                    TourSerieMemPeriodRanking periodRanking = tourMgr.getSerieRanking(serie);
                    if (periodRanking != null) {
                        resultOperation = "Result set : periodRankingPlayer="+periodRanking.addPlayerTournamentResult(memTour, rankPla);
                    } else {
                        resultOperation = "No period ranking found for serie="+serie;
                    }
                } else {
                    resultOperation = "Player has not played all deals !! rankPla="+rankPla;
                }
            } else {
                resultOperation = "No ranking player found on memTour for player="+playerID;
            }
        }
        else if (operation.equals("showResultDealForPlayer")) {
            long playerID = Long.parseLong(request.getParameter("playerID"));
            List<Double> listResultDeal = memTour.getListResultForPlayer(playerID);
            resultOperation = "Result deal for player="+playerID+" - list="+ StringTools.listToString(listResultDeal);
        }
        else if (operation.equals("computeResultRanking")) {
            memTour.computeResult();
            memTour.computeRanking(true);
            resultOperation = "Compute result & ranking is done ...";
        }
        else if (operation.equals("loadMissingResult")) {
            synchronized (tourMgr.getMemoryMgr().getLockTour(tourID)) {
                int nbResultLoad = 0;
                int nbRankingPlayerMissing = 0;
                int nbAddRankingPlayer = 0;
                int nbProblem = 0;
                TourSerieMemPeriodRanking serieRanking = tourMgr.getSerieRanking(serie);
                for (int i = 1; i <= 4; i++) {
                    List<TourSerieGame> listGame = tourMgr.listGameFinishedOnTournamentAndDeal(tourID, i, 0, 0);
//                List<TourSerieGame> listGame = tourMgr.listGameOnTournamentForPlayer(tourID, playerIDpb);
                    for (TourSerieGame tg : listGame) {
                        TourSerieMemTourPlayer e = memTour.getRankingPlayer(tg.getPlayerID());
                        if (e == null) {
                            nbRankingPlayerMissing++;
                            memTour.addResult(tg, true);
                            nbResultLoad++;
                        } else if (!e.dealsPlayed.contains(tg.getDealID()) && e.getNbDealsPlayed() < 4) {
                            nbResultLoad++;
                            memTour.addResult(tg, true);
                            if (e.getNbDealsPlayed() == 4) {
                                memTour.computeResult();
                                memTour.computeRanking(true);
                                serieRanking.addPlayerTournamentResult(memTour, e);
                                nbAddRankingPlayer++;
                            }
                        } else if (!e.dealsPlayed.contains(tg.getDealID())) {
                            nbProblem++;
                        }
                    }
                }
                if (nbResultLoad > 0) {
//                memTour.computeResult();
//                memTour.computeRanking(true);
                }
                resultOperation += "nbResultLoad=" + nbResultLoad + " - nbRankingPlayerMissing=" + nbRankingPlayerMissing + " - nbAddRankingPlayer=" + nbAddRankingPlayer + " - nbProblem=" + nbProblem;
            }
        } else if (operation.equals("reloadTournamentResult")) {
            synchronized (tourMgr.getMemoryMgr().getLockTour(tourID)) {
                memTour.ranking.clear();
                memTour.listPlayerFinishAll.clear();
                memTour.listPlayerForRanking.clear();

                TourSerieMemPeriodRanking serieRanking = tourMgr.getSerieRanking(serie);
                List<TourSerieGame> listGame = tourMgr.listGameOnTournament(tourID);
                int nbTourFinished = 0, nbGameFinished = 0;
                for (TourSerieGame tg : listGame) {
                    if (tg.isFinished()) {
                        nbGameFinished++;
                        TourSerieMemTourPlayer e = memTour.addResult(tg, true);
                        if (e.getNbDealsPlayed() == 4) {
                            serieRanking.addPlayerTournamentResult(memTour, e);
                            nbTourFinished++;
                        }
                    } else {
                        memTour.getOrCreateRankingPlayer(tg.getPlayerID());
                    }
                }
                memTour.computeResult();
                memTour.computeRanking(true);

                resultOperation = "Reset or load all games - size="+listGame.size()+" - nbGameFinished="+nbGameFinished+" - nbTourFinished="+nbTourFinished+"<br/>";
            }
        }
    }
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
    List<TourSerieMemTourPlayer> listRankingPlayer = null;
    if (memTour != null) {
        listRankingPlayer = memTour.getRanking(rankingOffset, rankingNbMax, null, useRankingFinished);
    }
%>
<html>
    <head>
        <title>Funbridge Server - Administration Tournament SERIE</title>
        <script type="text/javascript">
            function finishTournament() {
                if (confirm("Finish tournament ?")) {
                    document.forms["formTourSerieMemTour"].operation.value = "finish";
                    document.forms["formTourSerieMemTour"].submit();
                }
            }
            function backupTournament() {
                if (confirm("Backup tournament ?")) {
                    document.forms["formTourSerieMemTour"].operation.value = "backup";
                    document.forms["formTourSerieMemTour"].submit();
                }
            }
            function clickRankingOffset(offset) {
                document.forms["formTourSerieMemTour"].rankingoffset.value=offset;
                document.forms["formTourSerieMemTour"].submit();
            }
            function changeCheckFinish(value) {
                document.forms["formTourSerieMemTour"].rankingfinished.value=value;
                document.forms["formTourSerieMemTour"].submit();
            }
            function clickUpdateResultPeriodRanking(playerID) {
                if (confirm("Update player result on period ranking for playerID="+playerID)) {
                    document.forms["formTourSerieMemTour"].operation.value = "updateResultPeriodRanking";
                    document.forms["formTourSerieMemTour"].playerID.value = playerID;
                    document.forms["formTourSerieMemTour"].submit();
                }
            }
            function showResultDealForPlayer(playerID) {
                document.forms["formTourSerieMemTour"].operation.value = "showResultDealForPlayer";
                document.forms["formTourSerieMemTour"].playerID.value = playerID;
                document.forms["formTourSerieMemTour"].submit();
            }
            function clickComputeResultRanking() {
                if (confirm("Compute result & ranking on tournament ?")) {
                    document.forms["formTourSerieMemTour"].operation.value = "computeResultRanking";
                    document.forms["formTourSerieMemTour"].submit();
                }
            }
            function clickLoadMissingResult() {
                if (confirm("Load missing result on tournament ?")) {
                    document.forms["formTourSerieMemTour"].operation.value = "loadMissingResult";
                    document.forms["formTourSerieMemTour"].submit();
                }
            }
            function clickReloadTournamentResult() {
                if (confirm("Reset data from MemTour and reload all data result (best in maintenance status !)?")) {
                    document.forms["formTourSerieMemTour"].operation.value = "reloadTournamentResult";
                    document.forms["formTourSerieMemTour"].submit();
                }
            }
        </script>
    <body>
        <h1>ADMINISTRATION TOURNAMENT SERIE <%=serie.toUpperCase()%></h1>
        [<a href="adminSerieDetail.jsp?serie=<%=serie%>">Back to Tournament Serie <%=serie%></a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <% if (memTour != null) {
            TourSerieTournament tour = tourMgr.getTournament(tourID);
        %>
        <form name="formTourSerieMemTour" method="post" action="adminSerieMemTour.jsp?serie=<%=serie%>&tourID=<%=tourID%>">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="playerID" value=""/>
            <input type="hidden" name="rankingoffset" value="<%=rankingOffset%>"/>
            <input type="hidden" name="rankingfinished" value="<%=useRankingFinished?"1":"0"%>"/>

            Tournament : <%=tour.toString()%><br/>
            Nb player = <%=memTour.getNbPlayers()%> - Nb player for ranking = <%=memTour.getNbPlayersForRanking()%> - Nb player finish = <%=memTour.getNbPlayersFinish()%><br/>
            <%
                String playerProblem = "";
                List<TourSerieMemTourPlayer> listRanking = memTour.getRanking(0, 0, null, true);
                int nbPlayerProblem = 0;
                for (TourSerieMemTourPlayer e : listRanking) {
                    if (e.getNbDealsPlayed() == 4 && e.result == 0) {
                        playerProblem += e.playerID + " - ";
                        nbPlayerProblem++;
                    }
                }
            %>
            Player problem : nb=<%=nbPlayerProblem%> - players=<%=playerProblem%><br/>
            Nb game BDD=<%=countNbGameBDD(tourID)%> - Nb game Mem=<%=countNbGameMem(memTour)%><br/>
            <br/>
            <input type="button" value="Finish Tournament" onclick="finishTournament()"><br/>
            <input type="button" value="Backup Tournament" onclick="backupTournament()"><br/>
            <br/>
            <input type="button" value="Compute Result & Ranking" onclick="clickComputeResultRanking()"><br/>
            <input type="button" value="Load Missing Result" onclick="clickLoadMissingResult()"><br/>
            <input type="button" value="Reload Tournament Result" onclick="clickReloadTournamentResult()"><br/>
            <br/>
            <b>Deals</b><br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Deal ID</th><th>Index</th><th>Nb Player</th></tr>
                <% for (TourSerieMemDeal d : memTour.deals) {%>
                <tr>
                    <td><a href="adminSerieMemDeal.jsp?serie=<%=serie%>&tourID=<%=tourID%>&dealID=<%=d.dealID%>"><%=d.dealID%></a></td>
                    <td><%=d.dealIndex%></td>
                    <td><%=d.getNbPlayer()%></td>
                </tr>
                <%}%>
            </table>
            <br/>
            <b>Ranking</b><br/>
            Only player finished tournament : <input type="checkbox" name="checkFinish" <%=useRankingFinished?"checked":""%> onchange="changeCheckFinish(<%=useRankingFinished?'0':'1'%>)"><br/>
            <%if(rankingOffset > 0) {%>
            <input type="button" value="Previous" onclick="clickRankingOffset(<%=rankingOffset - rankingNbMax%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (listRankingPlayer != null && listRankingPlayer.size() >= rankingNbMax) {%>
            <input type="button" value="Next" onclick="clickRankingOffset(<%=rankingOffset + rankingNbMax%>)">
            <%}%>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Rank</th><th>RankFinisher</th><th>PlayerID</th><th>Result</th><th>Nb Deal played</th><th>Operation</th></tr>
                <%
                    for (TourSerieMemTourPlayer rankPla : listRankingPlayer) {
                %>
                <tr>
                    <td><%=rankPla.ranking%></td>
                    <td><%=rankPla.rankingFinished%></td>
                    <td><%=rankPla.playerID%></td>
                    <td><%=rankPla.result%></td>
                    <td><%=rankPla.getNbDealsPlayed()%> - <%=StringTools.listToString(memTour.getListResultForPlayer(rankPla.playerID))%></td>
                    <td>
                        <input type="button" value="Update result on period ranking" onclick="clickUpdateResultPeriodRanking(<%=rankPla.playerID%>)">&nbsp;&nbsp;&nbsp;
                        <input type="button" value="Show result deal" onclick="showResultDealForPlayer(<%=rankPla.playerID%>)">
                    </td>
                </tr>
                <%}%>
            </table>
        </form>
        <%}else{%>
            No memTour found for serie=<%=serie%> and tourID=<%=tourID%>
        <%}%>
    </body>
</html>
