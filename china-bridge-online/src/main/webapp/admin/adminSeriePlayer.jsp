<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.player.cache.PlayerCache" %>
<%@ page import="com.funbridge.server.player.data.Player" %>
<%@ page import="com.funbridge.server.tournament.serie.TourSerieMgr" %>
<%@ page import="com.funbridge.server.tournament.serie.data.TourSerieGame" %>
<%@ page import="com.funbridge.server.tournament.serie.data.TourSeriePlayer" %>
<%@ page import="com.funbridge.server.tournament.serie.data.TourSerieTournament" %>
<%@ page import="com.funbridge.server.tournament.serie.data.TourSerieTournamentPlayer" %>
<%@ page import="com.funbridge.server.tournament.serie.memory.*" %>
<%@ page import="com.funbridge.server.ws.player.WSSerieStatus" %>
<%@ page import="com.funbridge.server.ws.result.ResultListTournamentArchive" %>
<%@ page import="com.funbridge.server.ws.result.WSRankingSeriePlayer" %>
<%@ page import="com.funbridge.server.ws.result.WSTournamentArchive" %>
<%@ page import="com.funbridge.server.ws.support.SupportService" %>
<%@ page import="com.funbridge.server.ws.support.WSSupResponse" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    final int SHOW_RANKING = 0;
    final int SHOW_LIST_GAME = 1;
    final int SHOW_LIST_TOUR_PERIOD = 2;
    final int SHOW_LIST_TOUR_FINISHED = 3;
    final int SHOW_LIST_GAME_TOUR = 4;
    final int SHOW_LIST_ARCHIVE = 5;

    String operation = request.getParameter("operation");
    String resultOperation = "";
    String paramPlayerID = request.getParameter("playerID");
    long playerID = -1;
    try {
        playerID = Long.parseLong(paramPlayerID);
    } catch (Exception e) {}
    Player player = ContextManager.getPlayerMgr().getPlayer(playerID);
    TourSerieMgr serieMgr = ContextManager.getTourSerieMgr();
    // SHOW OPTION : 0=ranking - 1=list game - 2=list tour of current period - 3=list tour finished - 4=List game on tour

    int offset = 0;
    String paramOffset = request.getParameter("offset");
    if (paramOffset != null) {
        offset = Integer.parseInt(paramOffset);
        if (offset < 0) {
            offset = 0;
        }
    }

    int showOption = 0;
    String paramShowOption = request.getParameter("showoption");
    if (paramShowOption != null) {
        showOption = Integer.parseInt(paramShowOption);
    }

    String paramTourID = request.getParameter("tourID");
    if (operation != null) {
        if (operation.equals("deleteGame")) {
            String paramGameID = request.getParameter("gameID");
            TourSerieGame game = serieMgr.getGame(paramGameID);
            if (game != null) {
                if (game.getPlayerID() == playerID) {
                    if (serieMgr.removeResultForTournamentInProgress(game.getTournament().getIDStr(), game.getDealIndex(), playerID)) {
                        resultOperation = "SUCCESS to remove game - game="+game;
                    } else {
                        resultOperation = "FAILED to remove game - game="+game;
                    }
                } else {
                    resultOperation = "Failed : player is not owner of this game - game="+game;
                }
            } else {
                resultOperation = "Failed : no game found with ID="+paramGameID;
            }
        }
        else if (operation.equals("removeResultsInProgress")) {
            TourSeriePlayer seriePlayer = serieMgr.getTourSeriePlayer(playerID);
            if (seriePlayer != null) {
                resultOperation = "Close session ="+ContextManager.getPresenceMgr().closeSessionForPlayer(playerID, null);
                int nbGameRemoved = serieMgr.removeAllResultForTournamentInProgress(seriePlayer.getSerie(), playerID);
                resultOperation += " - Remove all results for tournament in progress for player="+playerID+" - nbGameRemoved="+nbGameRemoved;
            } else {
                resultOperation = "FAILED to get serie for player="+playerID;
            }
        }
        else if (operation.equals("changePlayerSerie")) {
            SupportService.WSSupChangePlayerSerieParam param = new SupportService.WSSupChangePlayerSerieParam();
            param.playerID = playerID;
            param.newSerie = request.getParameter("newserie");
            WSSupResponse resp = ContextManager.getSupportService().changePlayerSerie(param);
            resultOperation = "Change player serie - param="+param+" - result="+resp.toString();
        }
    }

    PlayerCache pc = ContextManager.getPlayerCacheMgr().getOrLoadPlayerCache(playerID);
    String serie = pc.serie;
    TourSerieMemPeriodRanking serieRanking = serieMgr.getSerieRanking(serie);
    WSSerieStatus serieStatus = serieMgr.buildSerieStatusForPlayer(pc);
    TourSeriePlayer tsp = serieMgr.getTourSeriePlayer(playerID);

%>
<html>
    <head>
        <title>Funbridge Server - Administration Tournament SERIE</title>
        <script>
            function showListGame() {
                document.forms["formSeriePlayer"].showoption.value="<%=SHOW_LIST_GAME%>";
                document.forms["formSeriePlayer"].offset.value=0;
                document.forms["formSeriePlayer"].submit();
            }
            function showListGameOnTour(tourID) {
                document.forms["formSeriePlayer"].showoption.value="<%=SHOW_LIST_GAME_TOUR%>";
                document.forms["formSeriePlayer"].offset.value=0;
                document.forms["formSeriePlayer"].tourID.value=tourID;
                document.forms["formSeriePlayer"].submit();
            }
            function showRanking() {
                document.forms["formSeriePlayer"].showoption.value="<%=SHOW_RANKING%>";
                document.forms["formSeriePlayer"].offset.value=0;
                document.forms["formSeriePlayer"].submit();
            }
            function showTourPeriod() {
                document.forms["formSeriePlayer"].showoption.value="<%=SHOW_LIST_TOUR_PERIOD%>";
                document.forms["formSeriePlayer"].offset.value=0;
                document.forms["formSeriePlayer"].submit();
            }
            function showTourFinished() {
                document.forms["formSeriePlayer"].showoption.value="<%=SHOW_LIST_TOUR_FINISHED%>";
                document.forms["formSeriePlayer"].offset.value=0;
                document.forms["formSeriePlayer"].submit();
            }
            function showListArchive() {
                document.forms["formSeriePlayer"].showoption.value="<%=SHOW_LIST_ARCHIVE%>";
                document.forms["formSeriePlayer"].offset.value=0;
                document.forms["formSeriePlayer"].submit();
            }
            function clickOffset(offset) {
                document.forms["formSeriePlayer"].offset.value=offset;
                document.forms["formSeriePlayer"].submit();
            }
            function clickViewGame(gameID) {
                window.open("adminSerieViewGame.jsp?gameID="+gameID);
                return false;
            }
            function clickViewMemTour(serie, tourID) {
                window.open("adminSerieMemTour.jsp?serie="+serie+"&tourID="+tourID);
                return false;
            }
            function clickViewMemDeal(serie, tourID, dealID) {
                window.open("adminSerieMemDeal.jsp?serie="+serie+"&tourID="+tourID+"&dealID="+dealID);
                return false;
            }
            function clickDeleteGame(gameID) {
                if (confirm("Delete game for playerID=<%=playerID%> with ID="+gameID)) {
                    document.forms["formSeriePlayer"].operation.value = "deleteGame";
                    document.forms["formSeriePlayer"].gameID.value = gameID;
                    document.forms["formSeriePlayer"].submit();
                }
            }
            function removeResultsInProgress() {
                if (confirm ("Remove all results for tournament in progress for playerID=<%=playerID%> ?")) {
                    if (confirm("Are you sure to remove all ?")) {
                        document.forms["formSeriePlayer"].operation.value = "removeResultsInProgress";
                        document.forms["formSeriePlayer"].submit();
                    }
                }
            }
            function clickChangePlayerSerie() {
                if (confirm ("For playerID=<%=playerID%> change serie from <%=serie%> to "+document.forms["formSeriePlayer"].newserie.value)) {
                    document.forms["formSeriePlayer"].operation.value = "changePlayerSerie";
                    document.forms["formSeriePlayer"].submit();
                }
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION SERIE FOR PLAYER <%=player!=null?player.getNickname():"!! NOT FOUND !!"%></h1>
        [<a href="adminSerie.jsp">Back to Serie</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %><br/>
        <hr width="90%"/>
        <%}%>
        <form name="formSeriePlayer" method="post" action="adminSeriePlayer.jsp?playerID=<%=playerID%>">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="showoption" value="<%=showOption%>"/>
            <input type="hidden" name="tourID" value="<%=paramTourID%>"/>
            <input type="hidden" name="offset" value="<%=offset%>"/>
            <input type="hidden" name="gameID" value="0"/>
            <% if (player == null) {%>
            NO PLAYER FOUND WITH ID=<%=paramPlayerID%>
            <%} else {
            %>
            Player = <%=player%><br/>
            <b>Player Serie</b>= [<%=tsp%>]<br/>
            <b>Player Cache</b>= [<%=pc%>]<br/>
            <b>Serie Status</b>= [<%=serieStatus.toString()%>] - historic=<%=serieStatus.historic%><br/>
            <input type="button" value="Remove all results for tournament in progress" onclick="removeResultsInProgress()"><br/>
            Change player serie from <%=serie%> to <select name="newserie"><% for (String s : TourSerieMgr.SERIE_TAB){%><option value="<%=s%>"><%=s%></option><%}%></select>&nbsp;&nbsp;&nbsp;
            <input type="button" value="Change player serie" onclick="clickChangePlayerSerie()">
            <br/>
            <hr width="90%"/>
            <%
                String trendSerie = TourSerieMgr.computeSerieEvolution(serie, serieStatus.trend, true);
                if (serieMgr.isPlayerReserve(serie, pc.serieLastPeriodPlayed, true)) {
                    trendSerie = TourSerieMgr.buildSerieReserve(trendSerie);
                }
            %>
            Trend Serie = <%=trendSerie%><br/>
            <%
                String trendText = ContextManager.getTextUIMgr().getTextSerieTrend(player.getLang(),
                        serie, serieStatus.trend, serieStatus.nbTournamentPlayed, pc.serieLastPeriodPlayed,
                        serieRanking.thresholdNbUp, serieRanking.thresholdNbDown, serieRanking.thresholdResultUp, serieRanking.thresholdResultDown);
            %>
            Trend Text = <%=trendText%><br/>
            <%
                String bonusText = "";
                if (serieStatus.nbTournamentPlayed < serieMgr.getBonusNbTour()) {
                    bonusText = ContextManager.getTextUIMgr().getTextSerieBonusFirst(player.getLang(), serieMgr.getBonusNbTour() - serieStatus.nbTournamentPlayed);
                } else {
                    if (serieMgr.getBonusNbTour() > 0) {
                        bonusText = ContextManager.getTextUIMgr().getTextSerieBonusNext(player.getLang(),
                                (serieStatus.nbTournamentPlayed / serieMgr.getBonusNbTour()) * serieMgr.getBonusRemove(),
                                serieMgr.getBonusRemove(),
                                serieMgr.getBonusNbTour());
                    }
                }
            %>
            Bonus Text = <%=bonusText%><br/>
            <%
                String bonusDescriptionText = ContextManager.getTextUIMgr().getTextSerieBonusDescription(player.getLang(), serieMgr.getBonusRemove(), serieMgr.getBonusNbTour());
            %>
            Bonus Description Text = <%=bonusDescriptionText%><br/>
            <hr width="90%"/>
            <%if (showOption != SHOW_RANKING) {%>
            <input type="button" value="Show Ranking" onclick="showRanking()">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <%if (showOption != SHOW_LIST_GAME) {%>
            <input type="button" value="Show List Game" onclick="showListGame()">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <%if (showOption != SHOW_LIST_TOUR_PERIOD) {%>
            <input type="button" value="Show Tour Period" onclick="showTourPeriod()">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <%if (showOption != SHOW_LIST_TOUR_FINISHED) {%>
            <input type="button" value="Show Tour Finished" onclick="showTourFinished()">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <%if (showOption != SHOW_LIST_ARCHIVE) {%>
            <input type="button" value="Show List Archive" onclick="showListArchive()">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <br/>
            <%
            //***********************************
            // RANKING
            //***********************************
            if (showOption == SHOW_RANKING) {%>
            <b>Ranking Serie</b><br/>
            <%
                TourSerieMemPeriodRankingPlayer rankSeriePlayer = serieRanking.getPlayerRank(playerID);
                WSRankingSeriePlayer wsRankSeriePlayer = serieMgr.buildWSRankingForPlayer(playerID, rankSeriePlayer, pc, playerID);
                List<WSRankingSeriePlayer> rankingExtract = serieMgr.getWSRankingExtractForSerie(serie, wsRankSeriePlayer, serieMgr.isPlayerReserve(serie, pc.serieLastPeriodPlayed, false), 5);
                int offsetRanking = 0;
                int nbMaxRank = 50;
                if (rankSeriePlayer != null) {
                    offsetRanking = rankSeriePlayer.rank - (nbMaxRank/2);
                }
                if (offsetRanking < 0) {
                    offsetRanking = 0;
                }
                List<WSRankingSeriePlayer> ranking = serieMgr.getWSRankingForSerie(serie, wsRankSeriePlayer, serieMgr.isPlayerReserve(serie, pc.serieLastPeriodPlayed, false), offsetRanking, nbMaxRank);
            %>
            RankSerie = [<%=serieRanking%>]<br/>
            rankSeriePlayer = [<%=rankSeriePlayer%>]<br/>
            <b>Ranking extract</b>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Rank</th><th>Result</th><th>PlayerID</th><th>Pseudo</th><th>NbTourPlayed</th><th>Trend</th></tr>
                <% for (WSRankingSeriePlayer e : rankingExtract) {%>
                <tr>
                    <td><%=e.getRank()%></td>
                    <td><%=e.getResult()%></td>
                    <td><%=e.getPlayerID()%></td>
                    <td><%=e.getPlayerPseudo()%></td>
                    <td><%=e.getNbTournamentPlayed()%></td>
                    <td><%=e.getTrend()%></td>
                </tr>
                <%}%>
            </table>
            <br/>
            <b>Ranking complete</b>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Rank</th><th>Result</th><th>PlayerID</th><th>Pseudo</th><th>NbTourPlayed</th><th>Trend</th></tr>
                <% for (WSRankingSeriePlayer e : ranking) {%>
                <tr>
                    <td><%=e.getRank()%></td>
                    <td><%=e.getResult()%></td>
                    <td><%=e.getPlayerID()%></td>
                    <td><%=e.getPlayerPseudo()%></td>
                    <td><%=e.getNbTournamentPlayed()%></td>
                    <td><%=e.getTrend()%></td>
                </tr>
                <%}%>
            </table>
            <%}
            //***********************************
            // LIST ALL GAME
            //***********************************
            else if (showOption== SHOW_LIST_GAME) {
                int nbMaxGame = 50;
                List<TourSerieGame> listGame = serieMgr.listGameForPlayer(playerID, offset, nbMaxGame);
            %>
            <b>List game</b><br/>
            Offset = <%=offset%> - nbMax = <%=nbMaxGame%><br/>
            <%if(offset > 0) {%>
            <input type="button" value="Previous" onclick="clickOffset(<%=offset - nbMaxGame%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (listGame.size() >= nbMaxGame) {%>
            <input type="button" value="Next" onclick="clickOffset(<%=offset + nbMaxGame%>)">
            <%}%>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>ID</th><th>Tournament</th><th>DealIndex</th><th>Finished</th><th>StartDate</th><th>LastDate</th><th>Rank</th><th>Result</th><th>Score</th><th>Contract</th><th>Operation</th></tr>
                <% for (TourSerieGame g : listGame) {%>
                <tr>
                    <td><%=g.getIDStr()%></td>
                    <td><%=g.getTournament()%></td>
                    <td><%=g.getDealIndex()%></td>
                    <td><%=g.isFinished()%></td>
                    <td><%=Constantes.timestamp2StringDateHour(g.getStartDate())%></td>
                    <td><%=Constantes.timestamp2StringDateHour(g.getLastDate())%></td>
                    <td><%=g.getRank()%></td>
                    <td><%=g.getResult()%></td>
                    <td><%=g.getScore()%></td>
                    <td><%=g.getContractWS()%></td>
                    <td>
                        <input type="button" value="View Game" onclick="clickViewGame('<%=g.getIDStr()%>')">
                        <%if (!g.getTournament().isFinished()){%>
                        &nbsp;&nbsp;&nbsp;<input type="button" value="View Mem Deal" onclick="clickViewMemDeal('<%=g.getTournament().getSerie()%>','<%=g.getTournament().getIDStr()%>', '<%=g.getDealID()%>')">
                        &nbsp;&nbsp;&nbsp;<input type="button" value="Delete" onclick="clickDeleteGame('<%=g.getIDStr()%>')">
                        <%}%>
                    </td>
                </tr>
                <%}%>
            </table>
            <%}
            //***********************************
            // LIST GAME ON TOUR
            //***********************************
            else if (showOption== SHOW_LIST_GAME_TOUR) {%>
            <b>List game for tournament <%=paramTourID%> and player=<%=playerID%></b><br/>
            <%
                TourSerieTournament tour = serieMgr.getTournament(paramTourID);
                if (tour != null) {
                    List<TourSerieGame> listGame = serieMgr.listGameOnTournamentForPlayer(tour.getIDStr(), playerID);
                    TourSerieMemTour memTour = serieMgr.getMemoryMgr().getMemTour(tour.getSerie(), tour.getIDStr());
                    %>
            Tournament = <%=tour%><br/>
            <%
                    if (memTour != null) {%>
            Mem Tour = <%=memTour%><br/>
            <input type="button" value="View Mem Tour Result" onclick="clickViewMemTour('<%=tour.getSerie()%>','<%=tour.getIDStr()%>')"><br/>
            <%      }%>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>ID</th><th>Tournament</th><th>DealIndex</th><th>Finished</th><th>StartDate</th><th>LastDate</th><th>Rank</th><th>Result</th><th>Score</th><th>Contract</th><th>Operation</th></tr>
                <% for (TourSerieGame g : listGame) {
                    String dealID = g.getDeal().getDealID(tour.getIDStr());
                    TourSerieMemDealPlayer memDealPlayer = null;
                    if (memTour != null) {
                        memDealPlayer = memTour.getResultDeal(dealID).getResultPlayer(playerID);
                    }
                %>
                <tr>
                    <td><%=g.getIDStr()%></td>
                    <td><%=g.getTournament()%></td>
                    <td><%=g.getDealIndex()%></td>
                    <td><%=g.isFinished()%></td>
                    <td><%=Constantes.timestamp2StringDateHour(g.getStartDate())%></td>
                    <td><%=Constantes.timestamp2StringDateHour(g.getLastDate())%></td>
                    <td><%=memDealPlayer!=null?memDealPlayer.nbPlayerBestScore+1:g.getRank()%></td>
                    <td><%=memDealPlayer!=null?memDealPlayer.result:g.getResult()%></td>
                    <td><%=g.getScore()%></td>
                    <td><%=g.getContractWS()%></td>
                    <td>
                        <input type="button" value="View Game" onclick="clickViewGame('<%=g.getIDStr()%>')">
                        <% if (memTour != null) {%>
                        &nbsp;&nbsp;&nbsp;<input type="button" value="View Mem Deal" onclick="clickViewMemDeal('<%=tour.getSerie()%>','<%=tour.getIDStr()%>', '<%=dealID%>')">
                        <%}%>
                        <%if (!g.getTournament().isFinished()){%>
                        &nbsp;&nbsp;&nbsp;<input type="button" value="Delete" onclick="clickDeleteGame('<%=g.getIDStr()%>')">
                        <%}%>
                    </td>
                </tr>
                <%}%>
            </table>
                <%} else {%>
            <b>No tournament found with ID=<%=paramTourID%></b>
                <%}
            }
            //***********************************
            // LIST TOURNAMENT IN CURRENT PERIOD
            //***********************************
            else if (showOption== SHOW_LIST_TOUR_PERIOD) {
                TourSerieMemPeriodRankingPlayer rankSeriePlayer = serieRanking.getPlayerRank(playerID);
                int nbMaxTourPeriod = 20;
                List<TourSerieMemPeriodRankingPlayerData> listData = new ArrayList<TourSerieMemPeriodRankingPlayerData>();
                if (rankSeriePlayer != null) {
                    listData = rankSeriePlayer.listTourPlayOrderDate(offset, nbMaxTourPeriod);
                }
            %>
            <b>List tournament in current period</b><br/>
            Offset = <%=offset%> - nbMax = <%=nbMaxTourPeriod%><br/>
            <%if(offset > 0) {%>
            <input type="button" value="Previous" onclick="clickOffset(<%=offset - nbMaxTourPeriod%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (listData != null && listData.size() >= nbMaxTourPeriod) {%>
            <input type="button" value="Next" onclick="clickOffset(<%=offset + nbMaxTourPeriod%>)">
            <%}%>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Tour ID</th><th>Date</th><th>Rank</th><th>Result</th><th>Operation</th></tr>
                <% if (listData!=null) {
                    for (TourSerieMemPeriodRankingPlayerData e : listData) {%>
                <tr>
                    <td><%=e.tourID%></td>
                    <td><%=Constantes.timestamp2StringDateHour(e.dateResult)%></td>
                    <td><%=e.rank%></td>
                    <td><%=e.result%></td>
                    <td><input type="button" value="Detail" onclick="showListGameOnTour('<%=e.tourID%>')"></td>
                </tr>
                <%}
                }%>
            </table>
            <%}
            //***********************************
            // LIST TOURNAMENT FINISHED
            //***********************************
            else if (showOption == SHOW_LIST_TOUR_FINISHED) {
                int nbMaxTourFinished = 20;
                List<TourSerieTournamentPlayer> listData = serieMgr.listTournamentPlayerOrderDateDes(playerID, offset, nbMaxTourFinished);
            %>
            <b>List tournament finished</b><br/>
            Offset = <%=offset%> - nbMax = <%=nbMaxTourFinished%><br/>
            <%if(offset > 0) {%>
            <input type="button" value="Previous" onclick="clickOffset(<%=offset - nbMaxTourFinished%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (listData.size() >= nbMaxTourFinished) {%>
            <input type="button" value="Next" onclick="clickOffset(<%=offset + nbMaxTourFinished%>)">
            <%}%>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Tournament</th><th>Date</th><th>Rank</th><th>Result</th><th>Operation</th></tr>
                <% for (TourSerieTournamentPlayer e : listData) {%>
                <tr>
                    <td><%=e.getTournament()%></td>
                    <td><%=Constantes.timestamp2StringDateHour(e.getLastDate())%></td>
                    <td><%=e.getRank()%></td>
                    <td><%=e.getResult()%></td>
                    <td><input type="button" value="Detail" onclick="showListGameOnTour('<%=e.getTournament().getIDStr()%>')"></td>
                </tr>
                <%}%>
            </table>
            <%}
            //***********************************
            // LIST ARCHIVE
            //***********************************
            else if (showOption == SHOW_LIST_ARCHIVE) {
                int nbMaxArchive = 30;
                ResultListTournamentArchive resultListTournamentArchive = serieMgr.listTournamentArchive(playerID, serie, offset, nbMaxArchive);
            %>
            <b>List Archives</b><br/>
            Offset = <%=offset%> - nbMax = <%=nbMaxArchive%> - nbTotal=<%=resultListTournamentArchive.nbTotal%><br/>
            <%if(offset > 0) {%>
            <input type="button" value="Previous" onclick="clickOffset(<%=offset - nbMaxArchive%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (resultListTournamentArchive.archives.size() >= nbMaxArchive) {%>
            <input type="button" value="Next" onclick="clickOffset(<%=offset + nbMaxArchive%>)">
            <%}%>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>PeriodID</th><th>Date</th><th>TourID</th><th>Rank</th><th>NbPlayers</th><th>Result</th></tr>
                <% for (WSTournamentArchive e : resultListTournamentArchive.archives) {%>
                <tr>
                    <td><%=e.periodID%></td>
                    <td><%=Constantes.timestamp2StringDateHour(e.date)%></td>
                    <td><%=e.tournamentID%></td>
                    <td><%=e.rank%></td>
                    <td><%=e.nbPlayers%></td>
                    <td><%=e.result%></td>
                </tr>
                <%}%>
            </table>
            <%}%>
        <%}%>
        </form>
    </body>
</html>
