<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.tournament.duel.DuelMgr" %>
<%@ page import="com.funbridge.server.tournament.duel.data.DuelTournamentPlayer" %>
<%@ page import="com.funbridge.server.tournament.duel.memory.DuelMemTournament" %>
<%@ page import="java.util.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    DuelMgr duelMgr = ContextManager.getDuelMgr();
    long playerID = -1;
    List<DuelMemTournament> listAllDuelResult =  duelMgr.getMemoryMgr().listDuelMemTournament();
    List<DuelMemTournament> listSelectionPlayer = new ArrayList<DuelMemTournament>();
    boolean showSelectionPlayer = false, showAll = false;
    if (operation != null) {
        if (operation.equals("backupData")) {
            int nbBackupOK = duelMgr.getMemoryMgr().backupAllDuel(false);
            resultOperation = "Backup operation - nbDuel="+ listAllDuelResult.size()+" - nbBackup="+nbBackupOK;
        }
        else if (operation.equals("listForPlayer")) {
            playerID = Long.parseLong(request.getParameter("playerID"));
            showSelectionPlayer = true;
            resultOperation = "List duels for playerID="+playerID;
        }
        else if (operation.equals("listAll")) {
            showAll = true;
            resultOperation = "List all duels";
        }
        else if (operation.equals("testProcessReminder")) {
            Calendar calProcessReminder = Calendar.getInstance();
            String dateProcessReminder = request.getParameter("dateProcessReminder");

            boolean bParamOK = true;
            if (dateProcessReminder != null && dateProcessReminder.length() > 0) {
                try {
                    calProcessReminder.setTimeInMillis(Constantes.stringDateHour2Timestamp(dateProcessReminder));
                } catch (Exception e) {
                    resultOperation = "Bad format for date creation - "+dateProcessReminder;
                    bParamOK = false;
                }
            }

            if (bParamOK) {
                int nbNotif = duelMgr.processReminder(calProcessReminder.getTimeInMillis());
                resultOperation = "process reminder done. Nb notif="+nbNotif;
            } else {
                resultOperation = "process reminder failed";
            }
        }
        else if (operation.equals("finishExpiredDuels")) {
            duelMgr.finishExpiredDuels();
            resultOperation = "finish expired duels";
        }
    }
    Set<String> setTournamentID = new HashSet<String>();
    long reminderLimitHigh = ((long) duelMgr.getConfigIntValue("reminderLimitHigh", 300)) * 60 * 1000;
    int nbDuelsExpired = 0;
    int nbDuelsToReminder = 0, nbDuelsAlreadyReminder = 0;
    int nbDuelsArgineNotFinish = 0;
    for (DuelMemTournament e : listAllDuelResult) {
        setTournamentID.add(e.tourID);
        if (e.isExpired()) {
            nbDuelsExpired++;
        }
        if (e.isToReminder(reminderLimitHigh)) {
            nbDuelsToReminder++;
        }
        if (e.dateReminder > 0) {
            nbDuelsAlreadyReminder++;
        }
        if (e.isDuelWithArgine()) {
            if (!e.isAllPlayedForPlayer(Constantes.PLAYER_ARGINE_ID)) {
                if ((System.currentTimeMillis() - e.getMemTournamentPlayer(Constantes.PLAYER_ARGINE_ID).dateLastPlay) > Constantes.TIMESTAMP_MINUTE) {
                    nbDuelsArgineNotFinish++;
                }
            }
        }
        if (showSelectionPlayer && (e.player1ID == playerID || e.player2ID == playerID)) {
            listSelectionPlayer.add(e);
        }
    }

    List<DuelMemTournament> listToShow = null;
    if (showSelectionPlayer) {
        listToShow = listSelectionPlayer;
    } else if (showAll){
        listToShow = listAllDuelResult;
    }

    List<DuelTournamentPlayer> listDuelExpired = duelMgr.listDuelTournamentPlayerExpired(5);
%>
<html>
    <head>
        <title>Funbridge Server - Administration</title>
        <script type="text/javascript">
            function clickBackupData() {
                if (confirm("Backup all data to file ?")) {
                    document.forms["formDuel"].operation.value="backupData";
                    document.forms["formDuel"].submit();
                }
            }
            function clickListForPlayer() {
                document.forms["formDuel"].operation.value="listForPlayer";
                document.forms["formDuel"].submit();
            }
            function clickListAll() {
                document.forms["formDuel"].operation.value="listAll";
                document.forms["formDuel"].submit();
            }
            function clickTestProcessReminder() {
                document.forms["formDuel"].operation.value = "testProcessReminder";
                document.forms["formDuel"].submit();
            }
            function clickOperation(operation) {
                document.forms["formDuel"].operation.value = operation;
                document.forms["formDuel"].submit();
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION TOURNAMENT DUEL</h1>
        [<a href="admin.jsp">Administration</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formDuel" method="post" action="adminDuel.jsp">
            <input type="hidden" name="operation" value=""/>
            Nb DuelTournamentPlayer in memory=<%=listAllDuelResult.size() %><br/>
            Nb Tournament used = <%=setTournamentID.size()%><br/>
            Date next scheduler finish=<%=duelMgr.getStringDateNextFinishScheduler() %><br/>
            Date next scheduler reminder=<%=duelMgr.getStringDateNextReminderScheduler() %><br/>
            Date next scheduler purge request=<%=duelMgr.getStringDateNextPurgeRequestScheduler() %><br/>
            Date next scheduler push tournament reminder=<%=duelMgr.getStringDateNextReminderScheduler()%><br/>
            <br/>
            <a href="adminDuelArgine.jsp">View duel with Argine</a><br/><a href="adminDuelScoring.jsp">View duel scoring</a><br/>
            <br/>
            <b>Backup all memory duel to files</b><br/>
            <input type="button" value="Backup All Data" onclick="clickBackupData()"/><br/>
            <br/>
            <b>TEST</b><br/>
            Process notif reminder : <input type="text" name="dateProcessReminder" size="30" value="<%=Constantes.timestamp2StringDateHour(System.currentTimeMillis())%>"/> (Format : dd/MM/yyyy - HH:mm:ss) &nbsp;&nbsp;&nbsp;<input type="button" value="Process reminder" onclick="clickTestProcessReminder()"/><br/>
            <hr width="95%"/>
            <b>Duel list in progress</b><br/>
            Nb total duels=<%=listAllDuelResult.size() %> - Nb duels expired=<%=nbDuelsExpired%> - Nb duels to reminder=<%=nbDuelsToReminder%> - Nb duels already reminder=<%=nbDuelsAlreadyReminder%> - Nb duels Argine not finished=<%=nbDuelsArgineNotFinish%><br/>
            <br/>
            List duels for player = <input type="text" name="playerID" value="<%=playerID!=-1?playerID:""%>">&nbsp;&nbsp;&nbsp;<input type="button" value="List duels for player" onclick="clickListForPlayer()">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="List all duels" onclick="clickListAll()"><br/>
            Nb duels to show = <%=listToShow!=null?listToShow.size():0%><br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>TournamentPlayer ID</th><th>PlayerDuel ID</th><th>Tour ID</th><th>Status</th><th>Date start</th><th>Date end</th><th>Players</th><th>Result</th><th>Player Finish</th><th>Nb Deals played</th></tr>
                <%
                    if (listToShow != null) {
                        for (DuelMemTournament e : listToShow) {
                        boolean argineNotFinish = false;
                        if (e.isDuelWithArgine()) {
                            if (!e.isAllPlayedForPlayer(Constantes.PLAYER_ARGINE_ID)) {
                                if ((System.currentTimeMillis() - e.getMemTournamentPlayer(Constantes.PLAYER_ARGINE_ID).dateLastPlay) > Constantes.TIMESTAMP_MINUTE) {
                                    argineNotFinish = true;
                                }
                            }
                        }
                %>
                <tr>
                    <td><a href="adminDuelDetail.jsp?tourPlayerID=<%=e.tourPlayerID%>" target="_blank"><%=e.tourPlayerID%></a></td>
                    <td><%=e.playerDuelID%></td>
                    <td><%=e.tourID%></td>
                    <td><%=e.isExpired()?"Expired":e.isAllPlayed()?"All played":e.isToReminder(reminderLimitHigh)?"To Reminder":e.dateReminder>0?"Already reminder":""%></td>
                    <td><%=Constantes.timestamp2StringDateHour(e.dateStart)%></td>
                    <td><%=Constantes.timestamp2StringDateHour(e.dateFinish)%></td>
                    <td><%=e.player1ID%> - <%=e.player2ID%></td>
                    <td><%=e.getResultPlayer(e.player1ID)%> - <%=e.getResultPlayer(e.player2ID)%></td>
                    <td><%=e.isAllPlayedForPlayer(e.player1ID)%> - <%=e.isAllPlayedForPlayer(e.player2ID)%></td>
                    <td><%=e.getNbDealPlayedByPlayer1()%> - <%=e.getNbDealPlayedByPlayer2()%><%=argineNotFinish?" - ArgineNotFinish":""%></td>
                </tr>
                <%
                        }
                    }
                %>
            </table>
            <hr width="95%"/>

            <b>Expired duels</b><br/>
            <input type="button" value="Finish expired duels" onclick="clickOperation('finishExpiredDuels')"/>
            <br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>TournamentPlayer ID</th><th>PlayerDuel ID</th><th>Tour ID</th><th>Creation date</th><th>Date end</th><th>Players</th><th>Result</th><th>Winner</th></tr>
                <%
                    if (listDuelExpired != null) {
                        for (DuelTournamentPlayer e : listDuelExpired) {
                %>
                <tr>
                    <td><a href="adminDuelDetail.jsp?tourPlayerID=<%=e.getIDStr()%>" target="_blank"><%=e.getIDStr()%></a></td>
                    <td><%=e.getPlayerDuelID()%></td>
                    <td><%=e.getTournament().getIDStr()%></td>
                    <td><%=Constantes.timestamp2StringDateHour(e.getCreationDate())%></td>
                    <td><%=Constantes.timestamp2StringDateHour(e.getFinishDate())%></td>
                    <td><%=e.getPlayer1ID()%> - <%=e.getPlayer2ID()%></td>
                    <td><%=e.getResultForPlayer(e.getPlayer1ID())%> - <%=e.getResultForPlayer(e.getPlayer2ID())%></td>
                    <td><%=e.getWinner()%></td>
                </tr>
                <%
                        }
                    }
                %>
            </table>
        </form>
    </body>
</html>
