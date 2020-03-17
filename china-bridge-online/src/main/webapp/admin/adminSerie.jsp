<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.tournament.game.GameMgr" %>
<%@ page import="com.funbridge.server.tournament.serie.TourSerieMgr" %>
<%@ page import="com.funbridge.server.tournament.serie.data.TourSeriePeriod" %>
<%@ page import="com.funbridge.server.tournament.serie.memory.TourSerieMemPeriodRanking" %>
<%@ page import="com.funbridge.server.tournament.serie.memory.TourSerieMemPeriodRankingPlayer" %>
<%@ page import="com.funbridge.server.ws.FBWSException" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    TourSerieMgr tourMgr = ContextManager.getTourSerieMgr();
    String strCheckPeriodValid = "";
    try {tourMgr.checkPeriodValid();strCheckPeriodValid="true";}
    catch (FBWSException e) {
        strCheckPeriodValid = "false - "+e.getType()+" - "+e.getMessage();
    }
    String strCheckSerie = "";
    try {tourMgr.checkSerieEnable();strCheckSerie="true";}
    catch (FBWSException e) {
        strCheckSerie = "false - "+e.getType()+" - "+e.getMessage();
    }
    if (operation != null) {
        if (operation.equals("testUpdateResult") && tourMgr.getConfigBooleanValue("devMode")) {
            resultOperation = tourMgr.testUpdateResult();
        } else if (operation.equals("testChangePeriod") && tourMgr.getConfigBooleanValue("devMode")) {
            long ts = System.currentTimeMillis();
            resultOperation = "change period result="+tourMgr.changePeriod()+" - ts="+(System.currentTimeMillis()-ts);
        } else if (operation.equals("testCreatePeriod") && tourMgr.getConfigBooleanValue("devMode")) {
            resultOperation = "period created : "+tourMgr.createNewPeriod(request.getParameter("textCreatePeriodID"));
        } else if (operation.equals("testProcessReminder") && tourMgr.getConfigBooleanValue("devMode")) {
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
                int nbNotif = tourMgr.processReminder(calProcessReminder.getTimeInMillis());
                resultOperation = "process reminder done. Nb notif="+nbNotif;
            } else {
                resultOperation = "process reminder failed";
            }
        } else if (operation.equals("getPeriod")) {
            TourSeriePeriod p = tourMgr.getPeriodForID(request.getParameter("textGetPeriod"));
            resultOperation = "Get period="+p;
        } else if (operation.equals("backupData")) {
            long ts = System.currentTimeMillis();
            int nbBackupMemTour = tourMgr.getMemoryMgr().backupAllMemTour();
            resultOperation = "Backup - nbBackupMemTour="+nbBackupMemTour+" - ts="+(System.currentTimeMillis() - ts);
            ts = System.currentTimeMillis();
            int nbBackupSerieRanking = tourMgr.backupAllSerieRanking();
            resultOperation += " - nbBackupSerieRanking="+nbBackupSerieRanking+" - ts="+(System.currentTimeMillis() - ts);
        } else if (operation.equals("reloadThreshold")) {
            for (String s : TourSerieMgr.SERIE_TAB) {
                TourSerieMemPeriodRanking rs = tourMgr.getSerieRanking(s);
                if (rs != null) {
                    rs.loadThresholdFromConfiguration();
                    rs.computeRanking();
                }
            }
            resultOperation = "reload threshold from configuration success for all series";
        } else if (operation.equals("resetData") && tourMgr.getConfigBooleanValue("devMode")) {
            long ts = System.currentTimeMillis();
            tourMgr.startUp();
            resultOperation = "Reset data done ... - ts="+(System.currentTimeMillis() - ts);
        } else if (operation.equals("enableSerie")) {
            if (!tourMgr.isEnable()) {
                tourMgr.setEnable(true);
                resultOperation = "Success to enable serie ...";
            } else {
                resultOperation = "Serie already enable !";
            }
        } else if (operation.equals("disableSerie")) {
            if (tourMgr.isEnable()) {
                tourMgr.setEnable(false);
                resultOperation = "Success to disable serie ...";
            } else {
                resultOperation = "Serie already disable !";
            }
        } else if (operation.equals("enablePeriodProcessing")) {
            if (!tourMgr.isPeriodChangeProcessing()) {
                tourMgr.setPeriodChangeProcessing(true);
                resultOperation = "Success to enable period processing ...";
            } else {
                resultOperation = "Period processing already enable !";
            }
        } else if (operation.equals("disablePeriodProcessing")) {
            if (tourMgr.isPeriodChangeProcessing()) {
                tourMgr.setPeriodChangeProcessing(false);
                resultOperation = "Success to disable period processing ...";
            } else {
                resultOperation = "Period processing already disable !";
            }
        } else if (operation.equals("loadData")) {
            tourMgr.loadAllSerieRankingFromFile();
            tourMgr.getMemoryMgr().loadTournamentForPeriod(tourMgr.getCurrentPeriod().getPeriodID());
        }
    }
%>
<html>
<head>
    <title>Funbridge Server - Administration SERIE</title>
    <script type="text/javascript">
        function clickLoadData() {
            if (confirm("Load all data as on startup ?")) {
                document.forms["formTourSerie"].operation.value="loadData";
                document.forms["formTourSerie"].submit();
            }
        }
        function clickBackupData() {
            if (confirm("Backup all data to file ?")) {
                document.forms["formTourSerie"].operation.value="backupData";
                document.forms["formTourSerie"].submit();
            }
        }
        function clickResetData() {
            if (confirm("Reset all data ?")) {
                document.forms["formTourSerie"].operation.value="resetData";
                document.forms["formTourSerie"].submit();
            }
        }
        function clickTestUpdateResult() {
            if (confirm("TEST - addResult ?")) {
                document.forms["formTourSerie"].operation.value = "testUpdateResult";
                document.forms["formTourSerie"].submit();
            }
        }
        function clickTestChangePeriod() {
            if (confirm("TEST - changePeriod ?")) {
                document.forms["formTourSerie"].operation.value = "testChangePeriod";
                document.forms["formTourSerie"].submit();
            }
        }
        function clickTestCreatePeriod() {
            if (confirm("TEST - createPeriod ?")) {
                document.forms["formTourSerie"].operation.value = "testCreatePeriod";
                document.forms["formTourSerie"].submit();
            }
        }
        function clickTestProcessReminder() {
            document.forms["formTourSerie"].operation.value = "testProcessReminder";
            document.forms["formTourSerie"].submit();
        }
        function clickGetPeriod() {
            document.forms["formTourSerie"].operation.value="getPeriod";
            document.forms["formTourSerie"].submit();
        }
        function clickReloadThreshold() {
            if (confirm("Reload threshold for all series ?")) {
                document.forms["formTourSerie"].operation.value = "reloadThreshold";
                document.forms["formTourSerie"].submit();
            }
        }
        function clickGetInfoPlayer() {
            window.open("adminSeriePlayer.jsp?playerID="+document.forms["formTourSerie"].textInfoPlayerID.value);
            return false;
        }
        function clickSerieSetEnableValue(value) {
            if (value) {
                if (confirm("Enable serie ?")) {
                    document.forms["formTourSerie"].operation.value="enableSerie";
                    document.forms["formTourSerie"].submit();
                }
            } else {
                if (confirm("Disable serie ?")) {
                    document.forms["formTourSerie"].operation.value="disableSerie";
                    document.forms["formTourSerie"].submit();
                }
            }
        }
        function clickSerieSetPeriodProcessingValue(value) {
            if (value) {
                if (confirm("Enable period processing ?")) {
                    document.forms["formTourSerie"].operation.value="enablePeriodProcessing";
                    document.forms["formTourSerie"].submit();
                }
            } else {
                if (confirm("Disable period processing ?")) {
                    document.forms["formTourSerie"].operation.value="disablePeriodProcessing";
                    document.forms["formTourSerie"].submit();
                }
            }
        }
    </script>
</head>
<body>
<h1>ADMINISTRATION SERIE</h1>
[<a href="admin.jsp">Administration</a>]
<br/><br/>
<%if (resultOperation.length() > 0) {%>
<b>RESULTAT OPERATION : <%= operation%></b>
<br/>Result = <%=resultOperation %>
<hr width="90%"/>
<%} %>
<form name="formTourSerie" method="post" action="adminSerie.jsp">
    <input type="hidden" name="operation" value=""/>
    <!--<b>Load tournament memory from file</b><br/>
    JSON Path file : <input type="text" name="pathFile" size="80"/><br/>
    <input type="button" value="Load Data" onclick="clickLoadData()"/>-->
    <hr width="90%"/>
    <b>Backup all data to files (Period ranking + tournament)</b><br/>
    <input type="button" value="Backup All Data" onclick="clickBackupData()"/>
    <hr width="90%"/>
    <% if (tourMgr.getConfigIntValue("loadDataOnStartup", 1) == 0) {%>
    <b>Load data</b><br/>
    Load data for current period : <input type="button" value="Load data" onclick="clickLoadData()"><br/>
    <%}%>
    <% if (tourMgr.getConfigBooleanValue("devMode")){%>
    <b>Reset all current data</b><br/>
    Remove all data in memory (no backup) and startup serie manager.<br/>
    <input type="button" value="Reset All Data" onclick="clickResetData()"/>
    <hr width="90%"/>
    <b>TEST</b><br/>
    Generate result : <input type="button" value="Test updateResult" onclick="clickTestUpdateResult()"/><br/>
    Change period : <input type="button" value="Test changePeriod" onclick="clickTestChangePeriod()"/><br/>
    Create period for ID : <input type="text" name="textCreatePeriodID"/>&nbsp;&nbsp;&nbsp;<input type="button" value="Test create period" onclick="clickTestCreatePeriod()"/><br/>
    Process notif reminder : <input type="text" name="dateProcessReminder" size="30" value="<%=Constantes.timestamp2StringDateHour(System.currentTimeMillis())%>"/> (Format : dd/MM/yyyy - HH:mm:ss) &nbsp;&nbsp;&nbsp;<input type="button" value="Process reminder" onclick="clickTestProcessReminder()"/><br/>
    <hr width="90%"/>
    <%}%>
    <b>SERIE DATA</b><br/>
    Test getPeriod : periodID=<input type="text" name="textGetPeriod"/> <input type="button" value="Get Period" onclick="clickGetPeriod()"/><br/>
    Get info Serie for Player ID : <input type="text" name="textInfoPlayerID"/>&nbsp;&nbsp;&nbsp;<input type="button" value="Get info Serie for player" onclick="clickGetInfoPlayer()"/><br/>
    <hr width="90%"/>
    Nb player in this mode = <%=tourMgr.getGameMgr().getPlayerRunningSize()%><br/>
    <br/>
    Date next scheduler finish=<%=tourMgr.getSchedulerChangePeriodDateNextRun()%><br/>
    <%
        GameMgr.ThreadPoolData threadDataPlay = tourMgr.getGameMgr().getThreadPoolDataPlay();
        GameMgr.ThreadPoolData threadDataReplay = tourMgr.getGameMgr().getThreadPoolDataReplay();
    %>
    Play Game status : ActiveCount=<%=threadDataPlay.activeCount%> - QueueSize=<%=threadDataPlay.queueSize%> - PoolSize=<%=threadDataPlay.poolSize%><br/>
    Replay Game status : ActiveCount=<%=threadDataReplay.activeCount%> - QueueSize=<%=threadDataReplay.queueSize%> - PoolSize=<%=threadDataReplay.poolSize%><br/>
    <hr width="90%"/>
    <b>Serie enable</b> = <%=tourMgr.isEnable()%> - <input type="button" value="<%=tourMgr.isEnable()?"Disable Serie":"Enable Serie"%>" onclick="clickSerieSetEnableValue(<%=!tourMgr.isEnable()%>)"/> - Period processing=<%=tourMgr.isPeriodChangeProcessing()%> - <input type="button" value="<%=tourMgr.isPeriodChangeProcessing()?"Disable Period Processing":"Enable Period Processing"%>" onclick="clickSerieSetPeriodProcessingValue(<%=!tourMgr.isPeriodChangeProcessing()%>)"/><br/>
    <b>CheckSerie</b> = <%=strCheckSerie%> - <b>CheckPeriodValid</b> = <%=strCheckPeriodValid%><br/>
    <b>Current period</b> = <%=tourMgr.getCurrentPeriod()%><br/>
    <b>Next period</b> = <%=tourMgr.getPeriodIDNext1()%> - <b>Previous period</b> = <%=tourMgr.getPeriodIDPrevious()%> - <b>Period-2</b> = <%=tourMgr.getPeriodIDBefore2()%> - <b>Period-3</b> = <%=tourMgr.getPeriodIDBefore3()%><br/>
    <b>Date next scheduler push tournament reminder</b> = <%=tourMgr.getStringDateNextReminderScheduler()%><br/>
    Bonus - NbTour = <%=tourMgr.getBonusNbTour()%> - Remove = <%=tourMgr.getBonusRemove()%><br/>
    Nb Total Tournament Serie in memory = <%=tourMgr.getMemoryMgr().getTotalNbTour()%><br/>
    Nb players in series (except NC and TOP and Reserve) = <%=tourMgr.countPlayerInSeriesExcludeNC_TOP_Reserve(true)%> - Nb players in serie TOP = <%=tourMgr.countPlayerInSerieExcludeReserve(TourSerieMgr.SERIE_TOP, true)%><br/>
    <input type="button" value="Reload Threshold" onclick="clickReloadThreshold()"><br/>
    <br/>
    <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
        <tr><th>Serie</th><th>Nb Player total</th><th>Nb Player Active</th><th>Nb FR</th><th>NbDown</th><th>NbMaintain</th><th>NbUp</th><th>Distribution</th><th>Threshold Up</th><th>Threshold Down</th><th>Nb Tournament</th></tr>
        <%
            for (String serie : TourSerieMgr.SERIE_TAB) {
                TourSerieMemPeriodRanking serieRanking = tourMgr.getSerieRanking(serie);
                if (serieRanking != null) {
                    List<TourSerieMemPeriodRankingPlayer> listPlayerRanking = serieRanking.getAllResult();
                    int nbMaintain = 0, nbUp = 0, nbDown = 0;
                    for (TourSerieMemPeriodRankingPlayer e:listPlayerRanking) {
                        if (e.trend > 0) {
                            nbUp++;
                        } else if (e.trend < 0) {
                            nbDown++;
                        } else {
                            nbMaintain++;
                        }
                    }
        %>
        <tr>
            <td><a href="adminSerieDetail.jsp?serie=<%=serie%>"><%=serie %></a></td>
            <td><%=serieRanking.getNbPlayerForRanking() %></td>
            <td><%=serieRanking.getNbPlayerActive(null, null) %></td>
            <td><%=serieRanking.getNbPlayerActive("FR", null)%></td>
            <td><%=nbDown%></td>
            <td><%=nbMaintain%></td>
            <td><%=nbUp%></td>
            <td><%=tourMgr.getPercentDistributionPlayer(serie)%>%</td>
            <td><%=serieRanking.thresholdResultUp %> - <%=serieRanking.thresholdNbUp%></td>
            <td><%=serieRanking.thresholdResultDown %> - <%=serieRanking.thresholdNbDown%></td>
            <td><%=tourMgr.getMemoryMgr().getNbTourInSerie(serie)%></td>
        </tr>
        <%}} %>
    </table>
    <br/>
</form>
</body>
</html>
