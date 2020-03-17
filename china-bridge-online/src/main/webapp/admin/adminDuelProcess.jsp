<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.message.MessageNotifMgr" %>
<%@ page import="com.funbridge.server.message.data.MessageNotifGroup" %>
<%@ page import="com.funbridge.server.presence.PresenceMgr" %>
<%@ page import="com.funbridge.server.tournament.duel.DuelMemoryMgr" %>
<%@ page import="com.funbridge.server.tournament.duel.DuelMgr" %>
<%@ page import="com.funbridge.server.tournament.duel.data.DuelGame" %>
<%@ page import="com.funbridge.server.tournament.duel.data.DuelTournamentPlayer" %>
<%@ page import="com.funbridge.server.tournament.duel.memory.DuelMemTournament" %>
<%@ page import="org.springframework.data.mongodb.core.MongoTemplate" %>
<%@ page import="java.util.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    DuelMgr duelMgr = ContextManager.getDuelMgr();
    MessageNotifMgr notifMgr = ContextManager.getMessageNotifMgr();
    PresenceMgr presenceMgr = ContextManager.getPresenceMgr();
    String notifID = request.getParameter("notifID");
    List<DuelMemTournament> listDuelResult = null;
    Calendar calDateStart = Calendar.getInstance();
    calDateStart.set(Calendar.YEAR, 2015);
    calDateStart.set(Calendar.MONTH, Calendar.SEPTEMBER);
    calDateStart.set(Calendar.DAY_OF_MONTH, 16);
    calDateStart.set(Calendar.HOUR_OF_DAY, 8);
    calDateStart.set(Calendar.MINUTE, 0);
    calDateStart.set(Calendar.SECOND, 0);
    calDateStart.set(Calendar.MILLISECOND, 0);
    long tsDateStart = calDateStart.getTimeInMillis();
    MongoTemplate mongoTemplate = (MongoTemplate)ContextManager.getContext().getBean("mongoDuelTemplate");
    if (operation != null) {
        if (operation.equals("removeTourDuelOne")) {
            String tourPlayerIDSel = request.getParameter("tourPlayerIDSel");
            DuelTournamentPlayer duelTournamentPlayer = duelMgr.getTournamentPlayer(tourPlayerIDSel);
            if (duelTournamentPlayer != null) {
                try {
                    long ts = System.currentTimeMillis();
                    long player1 = duelTournamentPlayer.getPlayer1ID();
                    long player2 = duelTournamentPlayer.getPlayer2ID();
                    presenceMgr.closeSessionForPlayer(player1, null);
                    presenceMgr.closeSessionForPlayer(player2, null);

                    duelMgr.removeDuel(duelTournamentPlayer.getIDStr(),
                            true,
                            true,
                            true);
                    resultOperation = "Sucess to remove duelTournamentPlayer = " + duelTournamentPlayer+" - time="+(System.currentTimeMillis() - ts);
                } catch (Exception e) {
                    resultOperation = "Fail to remove duelTournamentPlayer = " + duelTournamentPlayer+" - exception="+e.getMessage();
                }
            } else {
                resultOperation = "No duelTournamentPlayer found with tourPlayerIDSel = "+tourPlayerIDSel;
            }
        } else if (operation.equals("removeTourDuelBatch")) {
            listDuelResult =  duelMgr.getMemoryMgr().listDuelMemTournament();
            int batchSize = 10;
            String strBatchSize = request.getParameter("batchSize");
            if (strBatchSize != null && strBatchSize.length() > 0) {
                batchSize = Integer.parseInt(strBatchSize);
            }
            resultOperation = "batchSize = "+batchSize;
            long ts = System.currentTimeMillis();
            int nbProcess = 0, nbGameRemove = 0;

            for (Iterator<DuelMemTournament> iterator=listDuelResult.iterator(); iterator.hasNext();) {
                DuelMemTournament e = iterator.next();
                boolean doTheJob = false;
                int nbGameDB1 = duelMgr.countGameFinishedOnTournamentAndPlayer(e.tourID, e.player1ID);
                int nbGameDB2 = duelMgr.countGameFinishedOnTournamentAndPlayer(e.tourID, e.player2ID);
                if (e.getNbDealPlayedByPlayer1() != nbGameDB1 && nbGameDB1 >= 5) {
                    doTheJob = true;
                } else if (e.getNbDealPlayedByPlayer2() != nbGameDB2 && nbGameDB2 >= 5) {
                    doTheJob = true;
                }
                if (doTheJob) {
                    try {
                        long player1 = e.player1ID;
                        long player2 = e.player2ID;
                        presenceMgr.closeSessionForPlayer(player1, null);
                        presenceMgr.closeSessionForPlayer(player2, null);

                        // remove game
                        int nbGameRemovePla1 = 0, nbGameRemovePla2 = 0;
                        // for player1
                        List<DuelGame> listGame1 = duelMgr.listGameOnTournamentForPlayer(e.tourID, e.player1ID);
                        for (DuelGame g : listGame1) {
                            if (g.getStartDate() > tsDateStart) {
                                mongoTemplate.remove(g);
                                nbGameRemovePla1++;
                            }
                        }
                        // for player2
                        List<DuelGame> listGame2 = duelMgr.listGameOnTournamentForPlayer(e.tourID, e.player2ID);
                        for (DuelGame g : listGame2) {
                            if (g.getStartDate() > tsDateStart) {
                                mongoTemplate.remove(g);
                                nbGameRemovePla2++;
                            }
                        }

                        if (nbGameRemovePla1 > 0 || nbGameRemovePla2 > 0) {
                            // remove it from memory
                            duelMgr.getMemoryMgr().removeMemTournament(e.tourPlayerID);
                            // remove duelTourPlayer
                            duelMgr.removeTournamentPlayer(e.tourPlayerID);
                            // reset player duel
                            duelMgr.resetPlayerDuel(e.playerDuelID);

                            nbProcess++;
                            nbGameRemove += nbGameRemovePla1 + nbGameRemovePla2;
                            iterator.remove();
                        }
                        //duelMgr.removeDuel(e.tourPlayerID,true,true,true);

                        resultOperation += "<br>Sucess to remove duelTournamentPlayer with id="+ e.tourPlayerID+" - player1="+player1+" - player2="+player2+" - nbGameRemovePla1="+nbGameRemovePla1+" - nbGameRemovePla2="+nbGameRemovePla2;
                    } catch (Exception ex) {
                        resultOperation += "<br>Fail to remove duelTournamentPlayer with id=" + e.tourPlayerID+" - exception="+ex.getMessage();
                    }
                }
                if (nbProcess >= batchSize) {
                    break;
                }
            }
            resultOperation += "<br><br>Nb tournamentDuel process="+nbProcess+" - nbGameRemove="+nbGameRemove+" - time="+(System.currentTimeMillis() - ts);
        }
        else if (operation.equals("processCreditMessage")) {
            String descriptionTransaction = "[Server] Geste commercial suite aux problèmes sur les duels après mise à jour 16/09";
            String strPlayerIDList = request.getParameter("playerIDList");
            String[] listPlayerID = strPlayerIDList.split("\r\n");
            MessageNotifGroup notif = notifMgr.getNotifGroup(notifID);
            if (notif != null) {
                resultOperation = "PlayerID list size=" + listPlayerID.length;
                int nbProcess = 0;
                long ts = System.currentTimeMillis();
                for (String e : listPlayerID) {
                    try {
                        long playerID = Long.parseLong(e.trim());
                        notifMgr.insertNotifGroupReadForPlayer(notif, playerID);
                        resultOperation += "<br>Success with playerID : " + playerID;
                        nbProcess++;
                    } catch (Exception ex) {
                        resultOperation += "<br>Failed with playerID : " + e+" - exception="+ex.getMessage();
                    }
                }
                resultOperation += "<br><br>List size="+listPlayerID.length+" - nbProcess="+nbProcess+" - ts="+(System.currentTimeMillis() - ts);
            } else {
                resultOperation = "No notifGroup found for notifID="+notifID;
            }
        }
        else if (operation.equals("processReloadData")) {
            DuelMemoryMgr duelMemoryMgr = duelMgr.getMemoryMgr();
            listDuelResult =  duelMemoryMgr.listDuelMemTournament();
            int nbReloadTourPlayer = 0;
            int batchSize = 100;
            long ts = System.currentTimeMillis();
            for (Iterator<DuelMemTournament> iterator=listDuelResult.iterator(); iterator.hasNext();) {
                DuelMemTournament e = iterator.next();
                boolean doTheJob = false;
                int nbGameDB1 = duelMgr.countGameFinishedOnTournamentAndPlayer(e.tourID, e.player1ID);
                int nbGameDB2 = duelMgr.countGameFinishedOnTournamentAndPlayer(e.tourID, e.player2ID);
                if (e.getNbDealPlayedByPlayer1() != nbGameDB1) {
                    doTheJob = true;
                }
                else if (e.getNbDealPlayedByPlayer2() != nbGameDB2) {
                    doTheJob = true;
                }
                if (doTheJob) {
                    DuelTournamentPlayer duelTournamentPlayer = duelMgr.getTournamentPlayer(e.tourPlayerID);
                    if (duelTournamentPlayer != null) {
                        nbReloadTourPlayer++;
                        // remove from memory
                        duelMemoryMgr.removeMemTournament(e.tourPlayerID);

                        // create mem tournament
                        duelMemoryMgr.addTournamentPlayer(duelTournamentPlayer);

                        // add game for player1
                        List<DuelGame> listGame1 = duelMgr.listGameOnTournamentForPlayer(duelTournamentPlayer.getTournament().getIDStr(), duelTournamentPlayer.getPlayer1ID());
                        for (DuelGame g : listGame1) {
                            if (g.isFinished()) {
                                duelMgr.updateMemoryDataForGame(g);
                            }
                        }
                        // add game for player2
                        List<DuelGame> listGame2 = duelMgr.listGameOnTournamentForPlayer(duelTournamentPlayer.getTournament().getIDStr(), duelTournamentPlayer.getPlayer2ID());
                        for (DuelGame g : listGame2) {
                            if (g.isFinished()) {
                                duelMgr.updateMemoryDataForGame(g);
                            }
                        }
                        if (nbReloadTourPlayer >= batchSize) {
                            break;
                        }
                    }
                }
                resultOperation = "processReloadData nbReloadTourPlayer="+nbReloadTourPlayer+" - ts="+(System.currentTimeMillis() - ts);
                listDuelResult =  duelMgr.getMemoryMgr().listDuelMemTournament();
            }

        }
    }
    if (listDuelResult == null) {
        listDuelResult =  duelMgr.getMemoryMgr().listDuelMemTournament();
    }
    Set<String> setTournamentID = new HashSet<String>();
    Set<String> setDuelProblem = new HashSet<String>();
    Set<Long> setPlayerProblem = new HashSet<Long>();
    for (DuelMemTournament e : listDuelResult) {
        setTournamentID.add(e.tourID);
        int nbGameDB1 = duelMgr.countGameFinishedOnTournamentAndPlayer(e.tourID, e.player1ID);
        int nbGameDB2 = duelMgr.countGameFinishedOnTournamentAndPlayer(e.tourID, e.player2ID);
        if (e.getNbDealPlayedByPlayer1() != nbGameDB1) {
            setDuelProblem.add(e.tourPlayerID);
            setPlayerProblem.add(e.player1ID);
            setPlayerProblem.add(e.player2ID);
        }
        else if (e.getNbDealPlayedByPlayer2() != nbGameDB2) {
            setDuelProblem.add(e.tourPlayerID);
            setPlayerProblem.add(e.player1ID);
            setPlayerProblem.add(e.player2ID);
        }
    }
%>
<html>
<head>
    <title>Funbridge Server - Administration</title>
    <script type="text/javascript">
        function removeTournamenDuel(tourPlayerID) {
            if (confirm("Remove tournamentDuel with ID="+tourPlayerID)) {
                document.forms["formDuel"].operation.value="removeTourDuelOne";
                document.forms["formDuel"].tourPlayerIDSel.value=tourPlayerID;
                document.forms["formDuel"].submit();
            }
        }
        function clickBatchRemoveTournament() {
            if (confirm("Launch remove tournament process ?")) {
                document.forms["formDuel"].operation.value="removeTourDuelBatch";
                document.forms["formDuel"].submit();
            }
        }
        function clickBatchProcessCreditMessage() {
            if (confirm("Process Credit & Message for player list ?")) {
                document.forms["formDuel"].operation.value="processCreditMessage";
                document.forms["formDuel"].submit();
            }
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
<form name="formDuel" method="post" action="adminDuelProcess.jsp">
    <input type="hidden" name="operation" value=""/>
    <input type="hidden" name="tourPlayerIDSel" value=""/>
    Nb Tournament used = <%=setTournamentID.size()%><br/>
    Nb DuelTournamentPlayer in memory=<%=listDuelResult.size() %><br/>
    Nb Player with problem = <%=setPlayerProblem.size()%><br/>
    Date tsDateStart=<%=tsDateStart%> - <%=Constantes.timestamp2StringDateHour(tsDateStart)%><br/>
    <%
        String strPlayerID = "";
        for (Long e : setPlayerProblem) {
            strPlayerID +=e+"\r\n";
        }
    %>
    <textarea readonly="readonly" rows="10" cols="20"><%=strPlayerID%></textarea>
    <br/>
    <b>Backup all memory duel to files</b><br/>
    <input type="button" value="Backup All Data" onclick="clickBackupData()"/><br/>
    <br/>
    <b>Duel list in progress</b><br/>
    Nb duel=<%=listDuelResult.size() %><br/>
    Nb duel problem=<%=setDuelProblem.size() %><br/>
    Batch size = <input type="text" name="batchSize" value="10"><br/>
    <input type="button" value="Batch process remove" onclick="clickBatchRemoveTournament()">
    <br/>
    <br/>
    NotifID = <input type="text" name="notifID" value="<%=notifID!=null?notifID:""%>" size="30"><br/>
    List Players with problem:<br/>
    <textarea rows="10" cols="20" name="playerIDList"></textarea>
    <br/>
    <input type="button" value="Process Credit & Message" onclick="clickBatchProcessCreditMessage()">
    <br/>
    <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
        <tr><th>TournamentPlayer ID</th><th>PlayerDuel ID</th><th>Tour ID</th><th>Date start</th><th>Date end</th><th>Players</th><th>Result</th><th>Player Finish</th><th>Nb Deals played</th></tr>
        <% for (DuelMemTournament e : listDuelResult) {
            if (!setDuelProblem.contains(e.tourPlayerID)) {
                continue;
            }
        %>
        <tr>
            <td><a href="adminDuelDetail.jsp?tourPlayerID=<%=e.tourPlayerID%>" target="_blank"><%=e.tourPlayerID%></a></td>
            <td><%=e.playerDuelID%></td>
            <td><%=e.tourID%></td>
            <td><%=Constantes.timestamp2StringDateHour(e.dateStart)%></td>
            <td><%=Constantes.timestamp2StringDateHour(e.dateFinish)%></td>
            <td><%=e.player1ID%> - <%=e.player2ID%></td>
            <td><%=e.getResultPlayer(e.player1ID)%> - <%=e.getResultPlayer(e.player2ID)%></td>
            <td><%=e.isAllPlayedForPlayer(e.player1ID)%> - <%=e.isAllPlayedForPlayer(e.player2ID)%></td>
            <td <%=setDuelProblem.contains(e.tourPlayerID)?"bgcolor=\"#FF0000\"":""%>><%=e.getNbDealPlayedByPlayer1()%> - <%=e.getNbDealPlayedByPlayer2()%></td>
            <!--<td><input type="button" value="Remove tournament duel" onclick="removeTournamenDuel('<%=e.tourPlayerID%>')"></td>-->
        </tr>
        <%}%>
    </table>
</form>
</body>
</html>
