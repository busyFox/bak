<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.tournament.duel.DuelMgr" %>
<%@ page import="com.funbridge.server.tournament.duel.data.DuelArgineInProgress" %>
<%@ page import="com.funbridge.server.tournament.duel.data.DuelGame" %>
<%@ page import="com.funbridge.server.tournament.duel.data.DuelTournament" %>
<%@ page import="com.funbridge.server.tournament.game.Game" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    DuelMgr duelMgr = ContextManager.getDuelMgr();
    String showListSelection = "showDuels";
    String paramShowList = request.getParameter("showList");
    long tsLimitLastDate = System.currentTimeMillis() - 10*Constantes.TIMESTAMP_MINUTE;
    if (paramShowList != null && paramShowList.length() > 0) {
        showListSelection = paramShowList;
    }
    if (operation != null) {
        if (operation.equals("createTournamentDuelArgine")) {
            resultOperation = "Tournament created="+duelMgr.createArgineDuelAndStartPlay();
        }
        else if (operation.equals("restartPlayArgine")) {
            String paramOperation = request.getParameter("paramOperation");
            if (paramOperation != null && paramOperation.length() > 0) {
                DuelTournament tour = duelMgr.getTournament(paramOperation);
                if (tour != null) {
                    duelMgr.startPlayArgineAlone(tour);
                    resultOperation = "Call startPlayArgineAlone on tournament="+tour;
                }
                else {
                    resultOperation = "No tournament found for id="+paramOperation;
                }
            } else {
                resultOperation = "Parameter not valid ...";
            }
        }
        else if (operation.equals("removeDuel")) {
            String paramOperation = request.getParameter("paramOperation");
            duelMgr.removeDuelArgineInProgress(paramOperation);
            resultOperation = "Remove duel Argine in progress for tourID="+paramOperation;
        }
        else if (operation.equals("removeGame")) {
            String paramOperation = request.getParameter("paramOperation");
            resultOperation = "Remove game with ID="+paramOperation+" - result="+duelMgr.getGameMgr().removeArgineGameRunning(paramOperation);
        }
        else if (operation.equals("removeGameNoUpdate")) {
            List<Game> allGames = new ArrayList<Game>(duelMgr.getGameMgr().getMapArgineGameRunning().values());
            int nbMaxToProcess = 50;
            int nbGameRemove = 0;
            for (Game g : allGames) {
                if (g.getLastDate() < tsLimitLastDate) {
                    if (duelMgr.getGameMgr().removeArgineGameRunning(g.getIDStr())) {
                        nbGameRemove++;
                    }
                }
                if (nbGameRemove >= nbMaxToProcess) {
                    break;
                }
            }
            resultOperation = "Nb game remove="+nbGameRemove;
        }
    }
    Map<String, Game> mapGameArgine = duelMgr.getGameMgr().getMapArgineGameRunning();
    int nbGameNoUpdate = 0;
    for (Game e:mapGameArgine.values()) {
        if (e.getLastDate() <= tsLimitLastDate) {
            nbGameNoUpdate++;
        }
    }
%>
<html>
    <head>
        <title>Funbridge Server - Administration</title>
        <script type="text/javascript">
            function clickRunOperation(operation) {
                if (confirm("Run operation "+operation+" ?")) {
                    document.forms["formDuel"].operation.value = operation;
                    document.forms["formDuel"].submit();
                }
            }
            function clickRunOperationParameter(operation, parameter) {
                if (confirm("Run operation "+operation+" with parameter "+parameter+" ?")) {
                    document.forms["formDuel"].operation.value = operation;
                    document.forms["formDuel"].paramOperation.value = parameter;
                    document.forms["formDuel"].submit();
                }
            }
            function clickShowList(selection) {
                document.forms["formDuel"].showList.value = selection;
                document.forms["formDuel"].submit();
            }
        </script>
    </head>
    <body>
    <h1>ADMINISTRATION DUEL ARGINE</h1>
    [<a href="adminDuel.jsp">Administration Duel</a>]
    <br/><br/>
    <%if (resultOperation.length() > 0) {%>
    <b>RESULTAT OPERATION : <%= operation%></b>
    <br/>Result = <%=resultOperation %>
    <hr width="90%"/>
    <%} %>
    <form name="formDuel" method="post" action="adminDuelArgine.jsp">
        <input type="hidden" name="operation" value=""/>
        <input type="hidden" name="paramOperation" value=""/>
        <input type="hidden" name="showList" value="<%=showListSelection%>"/>
        Duel Argine Enable = <%=duelMgr.getConfigIntValue("duelArgineEnable", 1) == 1%> - Task Enable = <%=duelMgr.getConfigIntValue("duelArgineThread.taskEnable", 1) == 1%><br/>
        Nb Duel Argine In Progress = <%=duelMgr.countDuelArgineInProgress(false, false)%> - Nb Free Ready = <%=duelMgr.countDuelArgineInProgress(true, true)%> - Nb Free Not Ready = <%=duelMgr.countDuelArgineInProgress(true, false)%><br/>
        Nb Duel with no update = <%=duelMgr.countDuelArgineInProgressWithNoUpdate()%> - Nb expired = <%=duelMgr.countDuelArgineInProgressWithTournamentExpired()%><br/>
        Nb Argine Game in progress=<%=mapGameArgine.size() %> - Nb games with no update = <%=nbGameNoUpdate%>&nbsp;&nbsp;&nbsp;<input type="button" value="Remove game no update from Map" onclick="clickRunOperation('removeGameNoUpdate')"><br/>
        Date next run for DuelArgineProcess = <%=Constantes.timestamp2StringDateHour(duelMgr.getDateNextJobDuelArgineProcess())%> - process enable = <%=duelMgr.getConfigIntValue("duelArgine.taskEnable", 1)%><br/>
        <input type="button" value="Create tournament Duel Argine" onclick="clickRunOperation('createTournamentDuelArgine')">
        <br/>
        <br/>
        <% if (showListSelection.equals("showDuels") || showListSelection.equals("showDuelsNoUpdate")) {
            List<DuelArgineInProgress> duelArgineInProgressList = null;
            if (showListSelection.equals("showDuels")) {
                duelArgineInProgressList = duelMgr.listDuelArgineInProgress(false, 0, 0);
            } else {
                duelArgineInProgressList = duelMgr.listDuelArgineInProgressWithNoUpdate(0);
            }
        %>
        <b>List of duels in progress <% if (showListSelection.equals("showDuelsNoUpdate")) {%> with no update<%}%></b><br/>
        <input type="button" value="Show list GAMES" onclick="clickShowList('showGames')">&nbsp;&nbsp;&nbsp;
        <%if (showListSelection.equals("showDuels")) {%><input type="button" value="Show list DUELS WITH NO UPDATE" onclick="clickShowList('showDuelsNoUpdate')"><%}else {%>
        <input type="button" value="Show list DUELS" onclick="clickShowList('showDuels')">
        <%}%>
        <br/>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><th>ID</th><th>TourID</th><th>Free</th><th>PlayerID</th><th>DuelTourPlayer</th><th>Date start</th><th>Date last update</th><th>Date expiration</th><th>DealsPlayed</th><th>CurrentDealIndex</th><th>Operation</th><th>Game info</th></tr>
            <% for (DuelArgineInProgress e : duelArgineInProgressList) {%>
            <tr>
                <td><%=e.getIDStr()%></td>
                <td><%=e.tourID%></td>
                <td><%=e.isFree()%></td>
                <td><%=e.playerID%></td>
                <td><%=e.duelTourPlayerID%></td>
                <td><%=Constantes.timestamp2StringDateHour(e.dateCreation)%></td>
                <td><%=Constantes.timestamp2StringDateHour(e.dateLastUpdate)%></td>
                <td><%=Constantes.timestamp2StringDateHour(e.tournamentExpirationDate)%></td>
                <td><%=e.listDealsPlayedByArgine%></td>
                <td><%=e.currentDealIndexArgine%></td>
                <td>
                    <%if (!e.argineFinish) {%><input type="button" value="Restart Play Argine" onclick="clickRunOperationParameter('restartPlayArgine', '<%=e.tourID%>')">
                    <%} else {%><input type="button" value="Remove" onclick="clickRunOperationParameter('removeDuel', '<%=e.tourID%>')"><%}%>
                </td>
                <td>
                    <% String gameInfo = "";
                    if (e.currentGameID != null) {
                        Game g = mapGameArgine.get(e.currentGameID);
                        if (g != null) {
                            gameInfo = "DealIndex="+g.getDealIndex()+" - date start="+Constantes.timestamp2StringDateHour(g.getStartDate())+" - " +
                                    "date last="+Constantes.timestamp2StringDateHour(g.getLastDate())+" - Contract="+g.getContractWS()+" - " +
                                    "Step="+g.getStep()+" - nbTricks="+g.getNbTricks();
                        }
                    }
                    %>
                    <%=gameInfo%>
                </td>
            </tr>
            <%}%>
        </table>
        <%} else if (showListSelection.equals("showGames")) {%>
        <b>List of game Argine</b><br/>
        <input type="button" value="Show list DUELS" onclick="clickShowList('showDuels')">&nbsp;&nbsp;&nbsp;<input type="button" value="Show list DUELS WITH NO UPDATE" onclick="clickShowList('showDuelsNoUpdate')">
        <br/>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><th>ID</th><th>TourID</th><th>DuelTourPlayer</th><th>Start Date</th><th>Last Date</th><th>Index</th><th>Contract</th><th>Bids</th><th>Cards</th><th>Operation</th></tr>
            <% for (Game e : mapGameArgine.values()) {
                DuelGame duelGame = (DuelGame)e;
            %>
            <tr>
                <td><%=duelGame.getIDStr()%></td>
                <td><%=duelGame.getTournament().getIDStr()%></td>
                <td><%=duelGame.getTournamentPlayerID()%></td>
                <td><%=Constantes.timestamp2StringDateHour(duelGame.getStartDate())%></td>
                <td><%=Constantes.timestamp2StringDateHour(duelGame.getLastDate())%></td>
                <td><%=duelGame.getDealIndex()%></td>
                <td><%=duelGame.getContractWS()%></td>
                <td><%=duelGame.getBids()%></td>
                <td><%=duelGame.getCards()%></td>
                <td><input type="button" value="Remove" onclick="clickRunOperationParameter('removeGame', '<%=duelGame.getIDStr()%>')"></td>
            </tr>
            <%}%>
        </table>
        <%}%>
    </form>
    </body>
</html>
