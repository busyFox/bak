<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.team.data.Team" %>
<%@ page import="com.funbridge.server.tournament.team.TourTeamMgr" %>
<%@ page import="com.funbridge.server.tournament.team.data.TeamPeriod" %>
<%@ page import="com.funbridge.server.tournament.team.data.TeamTour" %>
<%@ page import="com.funbridge.server.tournament.team.memory.TeamMemDivisionResult" %>
<%@ page import="com.funbridge.server.tournament.team.memory.TeamMemDivisionTourResult" %>
<%@ page import="com.funbridge.server.tournament.team.memory.TeamMemTournament" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    TourTeamMgr tourTeamMgr = ContextManager.getTourTeamMgr();
    if (operation != null) {
        if (operation.equals("runProcess")) {
            long ts = System.currentTimeMillis();
            tourTeamMgr.runProcessForPeriod();
            resultOperation = "Operation runProcessForPeriod executed with success - ts="+(System.currentTimeMillis() - ts);
        } else if (operation.equals("changeTour")) {
            long ts = System.currentTimeMillis();
            tourTeamMgr.processChangeTour();
            resultOperation = "Operation processChangeTour executed with success - ts="+(System.currentTimeMillis() - ts);
        } else if (operation.equals("changePeriod")) {
            long ts = System.currentTimeMillis();
            tourTeamMgr.processChangePeriod();
            resultOperation = "Operation processChangePeriod executed with success - ts="+(System.currentTimeMillis() - ts);
        } else if (operation.equals("enableTourTeam")) {
            if (!tourTeamMgr.isEnable()) {
                tourTeamMgr.setEnable(true);
                resultOperation = "Success to enable tourTeam ...";
            } else {
                resultOperation = "TourTeam already enable !";
            }
        } else if (operation.equals("disableTourTeam")) {
            if (tourTeamMgr.isEnable()) {
                tourTeamMgr.setEnable(false);
                resultOperation = "Success to disable TourTeam ...";
            } else {
                resultOperation = "TourTeam already disable !";
            }
        } else if (operation.equals("enablePeriodProcessing")) {
            if (!tourTeamMgr.isPeriodChangeProcessing()) {
                tourTeamMgr.setPeriodChangeProcessing(true);
                resultOperation = "Success to enable period change processing ...";
            } else {
                resultOperation = "Period change processing already enable !";
            }
        } else if (operation.equals("disablePeriodProcessing")) {
            if (tourTeamMgr.isPeriodChangeProcessing()) {
                tourTeamMgr.setPeriodChangeProcessing(false);
                resultOperation = "Success to disable period change processing ...";
            } else {
                resultOperation = "Period change processing already disable !";
            }
        }  else if (operation.equals("enableTourProcessing")) {
            if (!tourTeamMgr.isTourChangeProcessing()) {
                tourTeamMgr.setTourChangeProcessing(true);
                resultOperation = "Success to enable tour change processing ...";
            } else {
                resultOperation = "Tour change processing already enable !";
            }
        } else if (operation.equals("disableTourProcessing")) {
            if (tourTeamMgr.isTourChangeProcessing()) {
                tourTeamMgr.setTourChangeProcessing(false);
                resultOperation = "Success to disable tour change processing ...";
            } else {
                resultOperation = "Tour change processing already disable !";
            }
        } else if (operation.equals("testPlayDeal")) {
            boolean allTournament = false;
            if (request.getParameter("testPlayDealAll") != null && request.getParameter("testPlayDealAll").equals("1")) {
                allTournament = true;
            }
            resultOperation = "Test play deal result="+tourTeamMgr.testUpdateResult(request.getParameter("divisionTestPlayDeal"), allTournament);
        } else if (operation.equals("computeAveragePerformanceMinimum")) {
            for (String div : TourTeamMgr.DIVISION_TAB) {
                Team teamLowestPerformance = tourTeamMgr.getTeamWithLowestAveragePerformanceForDivision(div);
                resultOperation += "Compute average performance for division=" + div + " - team=" + teamLowestPerformance + " - value=" + (teamLowestPerformance != null ? teamLowestPerformance.getAverageHandicap() : 0) + "<br/>";
            }
        } else if (operation.equals("reloadAveragePerformanceMinimum")) {
            resultOperation = "Nb values changed : " + tourTeamMgr.getMemoryMgr().loadAveragePerformanceMinimumForAllDivisions();
        } else if (operation.equals("initChampionship")) {
            tourTeamMgr.initChampionship();
            resultOperation = "Init championship is done !";
        } else if (operation.equals("loadThresholdDivisions")) {
            for (String div : TourTeamMgr.DIVISION_TAB) {
                TeamMemDivisionResult divisionResult = tourTeamMgr.getMemoryMgr().getMemDivisionResult(div);
                if (divisionResult != null) {
                    divisionResult.loadThresholdFromConfiguration();
                    divisionResult.computeRanking();
                }
            }
            resultOperation = "Load threshold for divisions with success !";
        } else if (operation.equals("computeRankingPeriod")) {
            for (String div : TourTeamMgr.DIVISION_TAB) {
                TeamMemDivisionResult divisionResult = tourTeamMgr.getMemoryMgr().getMemDivisionResult(div);
                if (divisionResult != null) {
                    divisionResult.computeRanking();
                }
            }
            resultOperation = "Compute ranking for all divisions with success !";
        } else if (operation.equals("resetDivisions")) {
            for (String div : TourTeamMgr.DIVISION_TAB) {
                TeamMemDivisionResult divisionResult = tourTeamMgr.getMemoryMgr().getMemDivisionResult(div);
                if (divisionResult != null) {
                    divisionResult.clearData();
                    resultOperation += div + " - clear divisionResult<br/>";
                }
                TeamMemDivisionTourResult divisionTourResult = tourTeamMgr.getMemoryMgr().getMemDivisionTourResult(div);
                if (divisionTourResult != null) {
                    divisionTourResult.clearData();
                    resultOperation += div + " - clear divisionTourResult<br/>";
                }
                Map<String, TeamMemTournament> mapTour = tourTeamMgr.getMemoryMgr().getMapTourForDivision(div);
                if (mapTour != null) {
                    mapTour.clear();
                    resultOperation += div + " - clear map Tournament<br/>";
                }
            }
        } else if (operation.equals("reloadCurrentPeriod")) {
            tourTeamMgr.reloadCurrentPeriod();
            resultOperation = "Success to reload current period";
        }
    }
    int nbMaxPeriod = 3;
    List<TeamPeriod> periods = tourTeamMgr.listPeriod(0, nbMaxPeriod);
%>
<html>
<head>
    <title>Funbridge Server - Administration</title>
    <script type="text/javascript">
        function clickRunOperation(operation) {
            if (confirm("Run operation "+operation+" ?")) {
                document.forms["formTourTeam"].operation.value = operation;
                document.forms["formTourTeam"].submit();
            }
        }
        function clickSetEnableValue(value) {
            if (value) {
                if (confirm("Enable TourTeam ?")) {
                    document.forms["formTourTeam"].operation.value="enableTourTeam";
                    document.forms["formTourTeam"].submit();
                }
            } else {
                if (confirm("Disable TourTeam ?")) {
                    document.forms["formTourTeam"].operation.value="disableTourTeam";
                    document.forms["formTourTeam"].submit();
                }
            }
        }
        function clickSetPeriodProcessingValue(value) {
            if (value) {
                if (confirm("Enable period processing ?")) {
                    document.forms["formTourTeam"].operation.value="enablePeriodProcessing";
                    document.forms["formTourTeam"].submit();
                }
            } else {
                if (confirm("Disable period processing ?")) {
                    document.forms["formTourTeam"].operation.value="disablePeriodProcessing";
                    document.forms["formTourTeam"].submit();
                }
            }
        }
        function clickSetTourProcessingValue(value) {
            if (value) {
                if (confirm("Enable tour processing ?")) {
                    document.forms["formTourTeam"].operation.value="enableTourProcessing";
                    document.forms["formTourTeam"].submit();
                }
            } else {
                if (confirm("Disable tour processing ?")) {
                    document.forms["formTourTeam"].operation.value="disableTourProcessing";
                    document.forms["formTourTeam"].submit();
                }
            }
        }
        function clickTestPlayDeal(division) {
            if (confirm("Run operation testPlayDeal for division="+division+" ?")) {
                if (confirm("Play deal for all tournament ?")) {
                    document.forms["formTourTeam"].testPlayDealAll.value = "1";
                }
                document.forms["formTourTeam"].divisionTestPlayDeal.value = division;
                document.forms["formTourTeam"].operation.value = "testPlayDeal";
                document.forms["formTourTeam"].submit();
            }
        }
    </script>
</head>
    <body>
        <h1>ADMINISTRATION TOUR TEAM</h1>
        [<a href="admin.jsp">Administration</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formTourTeam" method="post" action="adminTourTeam.jsp">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="divisionTestPlayDeal" value=""/>
            <input type="hidden" name="testPlayDealAll" value="0"/>
            <input type="button" value="Run Process" onclick="clickRunOperation('runProcess')">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Change Tour" onclick="clickRunOperation('changeTour')">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Change Period" onclick="clickRunOperation('changePeriod')">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <% if (!tourTeamMgr.isChampionshipStarted()) {%>
            <input type="button" value="Init Championship" onclick="clickRunOperation('initChampionship')">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Reset Divisions" onclick="clickRunOperation('resetDivisions')">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Reload Current Period" onclick="clickRunOperation('reloadCurrentPeriod')">
            <%}%>
            <br/>
            <br/>
            <b>Championship Started</b> = <%=tourTeamMgr.isChampionshipStarted()%> - <b>TourTeam enable</b> = <%=tourTeamMgr.isEnable()%> - <input type="button" value="<%=tourTeamMgr.isEnable()?"Disable TourTeam":"Enable TourTeam"%>" onclick="clickSetEnableValue(<%=!tourTeamMgr.isEnable()%>)"/><br/>
            <b>Current period</b> = <%=tourTeamMgr.getCurrentPeriod()%><br/>
            <b>Current Tour</b> = <%=tourTeamMgr.getCurrentPeriod() != null ?tourTeamMgr.getCurrentPeriod().getCurrentTour():null%><br/>
            Period change processing = <%=tourTeamMgr.isPeriodChangeProcessing()%>&nbsp;&nbsp;<input type="button" value="<%=tourTeamMgr.isPeriodChangeProcessing()?"Disable Period Processing":"Enable Period Processing"%>" onclick="clickSetPeriodProcessingValue(<%=!tourTeamMgr.isPeriodChangeProcessing()%>)"/>&nbsp;&nbsp;-&nbsp;&nbsp;
            Tour change processing = <%=tourTeamMgr.isTourChangeProcessing()%>&nbsp;&nbsp;<input type="button" value="<%=tourTeamMgr.isTourChangeProcessing()?"Disable Tour Processing":"Enable Tour Processing"%>" onclick="clickSetTourProcessingValue(<%=!tourTeamMgr.isTourChangeProcessing()%>)"/><br/>
            <b>Date next period</b> = <%=Constantes.timestamp2StringDateHour(tourTeamMgr.getDateNextPeriod())%><br/>
            <b>Date next run for TeamProcess</b> = <%=Constantes.timestamp2StringDateHour(tourTeamMgr.getDateNextJobProcess())%><br/>
            Compute minimum performance for all divisions - <input type="button" value="Compute minimum performance" onclick="clickRunOperation('computeAveragePerformanceMinimum')"><br/>
            Compute ranking for all divisions - <input type="button" value="Compute ranking" onclick="clickRunOperation('computeRankingPeriod')"><br/>
            Reload value for average performance minimum from configuration file - <input type="button" value="Reload minimum performance from configuration" onclick="clickRunOperation('reloadAveragePerformanceMinimum')"><br/>
            Load threshold UP & DOWN for division from configuration file - <input type="button" value="Load threshold divisions" onclick="clickRunOperation('loadThresholdDivisions')"><br/>
            <br/>
            <b>TEST</b><br/>Play next deal for all team test <input type="button" value="TEST for all division" onclick="clickTestPlayDeal('')">
            <% for (String division : TourTeamMgr.DIVISION_TAB) {%>
            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="TEST for division <%=division%>" onclick="clickTestPlayDeal('<%=division%>')">
            <%}%>
            <br/>
            <hr width="95%"/>
            <br/>
            <b>Division</b><br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Division</th><th>Nb teams</th><th>Threshold</th><th>Average performance minimum</th></tr>
                <% for (String division : TourTeamMgr.DIVISION_TAB) {
                    TeamMemDivisionResult memDivision = tourTeamMgr.getMemoryMgr().getMemDivisionResult(division);
                %>
                <tr>
                    <td><a href="adminTourTeamDivision.jsp?division=<%=division%>"><%=division%></a></td>
                    <td><%=memDivision!=null?memDivision.mapResultTeam.size():null%></td>
                    <td><%=memDivision!=null?memDivision.thresholdUp+" - "+memDivision.thresholdDown:null%></td>
                    <td><%=memDivision!=null?memDivision.averagePerformanceMinimum:null%></td>
                </tr>
                <%}%>
            </table>
            <br/>
            <b>The last <%=nbMaxPeriod%> periods:</b><br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>ID</th><th>Date</th><th>Finished</th><th>Tours</th></tr>
                <% for (TeamPeriod tp : periods) {%>
                <tr>
                    <td><%=tp.getID()%></td>
                    <td><%=Constantes.timestamp2StringDateHour(tp.getDateStart())%> - <%=Constantes.timestamp2StringDateHour(tp.getDateEnd())%></td>
                    <td><%=tp.isFinished()%> - tours finished=<%=tp.isAllTourFinished()%></td>
                    <td>
                        <% for (TeamTour tour : tp.getTours()) {%>
                        <%=tour.toString()%><br/>
                        <%}%>
                    </td>
                </tr>
                <%}%>
            </table>
        </form>
    </body>
</html>
