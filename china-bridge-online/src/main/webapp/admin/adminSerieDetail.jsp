<%@ page import="com.funbridge.server.tournament.serie.TourSerieMgr" %>
<%@ page import="com.funbridge.server.tournament.serie.memory.TourSerieMemPeriodRanking" %>
<%@ page import="com.funbridge.server.ws.result.WSRankingSeriePlayer" %>
<%@ page import="org.apache.commons.io.FilenameUtils" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="adminSerieOperation.jsp"%>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    String serie = request.getParameter("serie");
    TourSerieMgr tourMgr = ContextManager.getTourSerieMgr();
    TourSerieMemPeriodRanking serieRanking = tourMgr.getSerieRanking(serie);
    if (operation != null) {
        if (operation.equals("reloadThreshold")) {
            serieRanking.loadThresholdFromConfiguration();
            serieRanking.computeRanking();
            resultOperation = "Operation "+operation+" done with success";
        }
        else if (operation.equals("backupRankingSerie")) {
            long ts = System.currentTimeMillis();
            boolean resultBackup = tourMgr.backupSerieRanking(serieRanking);
            if (resultBackup) {
                resultOperation = "Success to backup ranking serie - ts="+(System.currentTimeMillis() - ts);
            } else {
                resultOperation = "Failed to backup ranking serie";
            }
        }
        else if (operation.equals("loadRankingSerieFile")) {
            String path = tourMgr.getStringResolvEnvVariableValue("backupMemoryPath", null);
            path = FilenameUtils.concat(path, serie+".json");
            resultOperation = "Loading serie from file="+path+" result="+tourMgr.loadSerieRankingFromFile(path);
        }
        else if (operation.equals("reloadNbPlayer")) {
            int nbPlayer = 0;
            if (!serie.equals(TourSerieMgr.SERIE_NC)) {
                nbPlayer = tourMgr.countPlayerInSerieExcludeReserve(serie, true);
            }
            serieRanking.updateNbPlayerSerie(nbPlayer);
            resultOperation = "Reload nb player (exclude reserve) in serie="+serie+" - nbPlayer="+nbPlayer;
        }
        else if (operation.equals("loadMemTourFromFile")) {
            String path = tourMgr.getStringResolvEnvVariableValue("backupMemoryPath", null);
            int nbOK = 0, nbFailed = 0;
            if (path != null) {
                path = FilenameUtils.concat(path, serie);
                File fPath = new File(path);
                for (File f : fPath.listFiles()) {
                    if (tourMgr.getMemoryMgr().loadMemTourFromFile(f.getPath()) != null) {
                        nbOK++;
                    } else {
                        nbFailed++;
                    }
                }
            }
            resultOperation = "loadMemTourFromFile - path="+path+" - nbOK="+nbOK+" - nbFailed="+nbFailed;
        }
    }
    Map<String, TourSerieMemTour> mapTour = tourMgr.getMemoryMgr().getMapTourForSerie(serie);
    boolean showRanking = (request.getParameter("showranking")!=null && request.getParameter("showranking").equals("1"));
    int offset = 0;
    int rankingNbMax = 50;
    String paramOffset = request.getParameter("offset");
    if (paramOffset != null) {
        offset = Integer.parseInt(paramOffset);
        if (offset < 0) {
            offset = 0;
        }
    }
%>
<html>
    <head>
        <title>Funbridge Server - Administration SERIE</title>
        <script type="text/javascript">
            function showTournament() {
                document.forms["formTourSerieMem"].showranking.value="0";
                document.forms["formTourSerieMem"].submit();
            }
            function showRanking() {
                document.forms["formTourSerieMem"].showranking.value="1";
                document.forms["formTourSerieMem"].submit();
            }
            function showRankingPlaData(playerID) {
                document.forms["formTourSerieMem"].operation.value="showPlayerData";
                document.forms["formTourSerieMem"].playerID.value=playerID;
                document.forms["formTourSerieMem"].showranking.value="1";
                document.forms["formTourSerieMem"].submit();
            }
            function showPlayerData(playerID) {
                window.open("adminSeriePlayer.jsp?playerID="+playerID);
                return false;
            }
            function reloadThreshold() {
                if (confirm("Reload threshold from configuration ?")) {
                    document.forms["formTourSerieMem"].operation.value = "reloadThreshold";
                    document.forms["formTourSerieMem"].submit();
                }
            }
            function backupRankingSerie() {
                if (confirm("Backup the ranking serie ?")) {
                    document.forms["formTourSerieMem"].operation.value = "backupRankingSerie";
                    document.forms["formTourSerieMem"].submit();
                }
            }
            function clickOffset(offset) {
                document.forms["formTourSerieMem"].offset.value=offset;
                document.forms["formTourSerieMem"].submit();
            }
            function clickReloadNbPlayer() {
                if (confirm("Reload nb player in serie ?")) {
                    document.forms["formTourSerieMem"].operation.value = "reloadNbPlayer";
                    document.forms["formTourSerieMem"].submit();
                }
            }
            function clickLoadMemTourFromFile() {
                if (confirm("Load mem tour from file ?")) {
                    document.forms["formTourSerieMem"].operation.value = "loadMemTourFromFile";
                    document.forms["formTourSerieMem"].submit();
                }
            }
            function clickLoadRankingSerieFromFile() {
                if (confirm("Load mem tour from file ?")) {
                    document.forms["formTourSerieMem"].operation.value = "loadRankingSerieFile";
                    document.forms["formTourSerieMem"].submit();
                }
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION SERIE <%=serie.toUpperCase()%></h1>
        [<a href="adminSerie.jsp">Back to Serie</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %><br/>
        <hr width="90%"/>
        <%} %>
        <form name="formTourSerieMem" method="post" action="adminSerieDetail.jsp?serie=<%=serie%>">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="playerID" value=""/>
            <input type="hidden" name="showranking" value="<%=showRanking?1:0%>"/>
            <input type="hidden" name="offset" value="<%=offset%>"/>
            Serie : <%=serie%><br/>
            Nb total player (exclude reserve) : <%=serieRanking.getNbPlayerForRanking()%> - Nb player active : <%=serieRanking.getNbPlayerActive(null, null)%><br/>
            <input type="button" value="Reload nb player" onclick="clickReloadNbPlayer()">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Backup Ranking Serie" onclick="backupRankingSerie()"><br/>
            <input type="button" value="Load Ranking serie from file" onclick="clickLoadRankingSerieFromFile()">
            <input type="button" value="Load mem tournament from file" onclick="clickLoadMemTourFromFile()">
            <br/>
            <b>Threshold</b><br/>
            <input type="button" value="Reload Threshold" onclick="reloadThreshold()"><br/>
            Threshold Nb UP : <%=serieRanking.thresholdNbUp%> - Threshold Nb DOWN: <%=serieRanking.thresholdNbDown%><br/>
            Threshold Result UP : <%=serieRanking.thresholdResultUp%> - Threshold Result DOWN: <%=serieRanking.thresholdResultDown%><br/>
            <hr width="90%"/>
            <input type="button" value="<%=showRanking?"Show TOURNAMENT":"Show RANKING"%>" onclick="<%=showRanking?"showTournament()":"showRanking()"%>"><br/>
            <%if (showRanking) {
                List<WSRankingSeriePlayer> listRankingPlayer = tourMgr.getWSRankingForSerie(serie, null, false, offset, rankingNbMax);
            %>
            <b>Ranking</b><br/>
            <%if(offset > 0) {%>
            <input type="button" value="Previous" onclick="clickOffset(<%=offset - rankingNbMax%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (listRankingPlayer.size() >= rankingNbMax) {%>
            <input type="button" value="Next" onclick="clickOffset(<%=offset + rankingNbMax%>)">
            <%}%>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Rank</th><th>Result</th><th>PlayerID</th><th>Pseudo</th><th>Country</th><th>Nb Tour played</th><th>Trend</th><th>Operation</th></tr>
                <%
                    for (WSRankingSeriePlayer e : listRankingPlayer) {
                %>
                <tr>
                    <td><%=e.rank%></td>
                    <td><%=e.result%></td>
                    <td><%=e.playerID%></td>
                    <td><%=e.playerPseudo%></td>
                    <td><%=e.countryCode%> - <%=e.countryCode.equals("FR")%></td>
                    <td><%=e.nbTournamentPlayed%></td>
                    <td><%=e.trend%></td>
                    <td><input type="button" value="Show Data" onclick="showPlayerData('<%=e.playerID%>')"></td>
                </tr>
                <%}%>
            </table>
            <%} else {%>
            <b>Tournament</b><br/>
            Nb tournament in memory : <%=mapTour.size()%><br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Tour</th><th>Nb Player</th><th>Nb Player Ranking</th><th>Nb Player Finish</th><th>Problem ?</th></tr>
                <%
                    for (TourSerieMemTour memTour : mapTour.values()) {
                        int nbGameMem = countNbGameMem(memTour);
                        int nbGameBDD = countNbGameBDD(memTour.tourID);
                %>
                <tr>
                    <td><a href="adminSerieMemTour.jsp?serie=<%=serie%>&tourID=<%=memTour.tourID%>"><%=memTour.tourID %></a></td>
                    <td><%=memTour.getNbPlayers() %></td>
                    <td><%=memTour.getNbPlayersForRanking() %></td>
                    <td><%=memTour.getNbPlayersFinish() %></td>
                    <td <%=nbGameBDD!=nbGameMem?"bgcolor=\"#FF0000\"":""%>>NbGameMem=<%=nbGameMem%> - NbGameBDD<%=nbGameBDD%></td>
                </tr>
                <%} %>
            </table>
            <%}%>
        </form>
    </body>
</html>
