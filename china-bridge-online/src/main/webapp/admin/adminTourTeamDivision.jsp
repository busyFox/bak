<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.team.cache.TeamCacheMgr" %>
<%@ page import="com.funbridge.server.team.data.Team" %>
<%@ page import="com.funbridge.server.tournament.team.TourTeamMgr" %>
<%@ page import="com.funbridge.server.tournament.team.memory.*" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    String division = request.getParameter("division");
    String rankingType = request.getParameter("rankingType");
    if (rankingType == null || rankingType.length() == 0) {
        rankingType = "TOUR";
    }
    TourTeamMgr tourTeamMgr = ContextManager.getTourTeamMgr();
    TeamCacheMgr teamCacheMgr = ContextManager.getTeamCacheMgr();
    TeamMemDivisionTourResult divisionTourResult = tourTeamMgr.getMemoryMgr().getMemDivisionTourResult(division);
    TeamMemDivisionResult divisionResult = tourTeamMgr.getMemoryMgr().getMemDivisionResult(division);
    if (operation != null) {
        if (operation.equals("computeAveragePerformanceMinimum")) {
            Team teamLowestPerformance = tourTeamMgr.getTeamWithLowestAveragePerformanceForDivision(division);
            resultOperation = "Team with lowest performance = "+teamLowestPerformance+" - average performance="+(teamLowestPerformance!=null?teamLowestPerformance.getAveragePerformanceIMP():0);
        }
        else if (operation.equals("computeTourResultRanking")) {
            divisionTourResult.computeResult();
            divisionTourResult.computeRanking();
            resultOperation = "Compute result & ranking for divisionTourResult="+divisionTourResult;
        }
        else if (operation.equals("computeRanking")) {
            divisionResult.computeRanking();
            resultOperation = "Compute ranking for divisionResult="+divisionResult;
        }
        else if (operation.equals("testPlayDeal")) {
            resultOperation = "TEST playDeal result="+tourTeamMgr.testUpdateResult(division, false);
        }
        else if (operation.equals("testPlayDealAll")) {
            resultOperation = "TEST playDealAll result="+tourTeamMgr.testUpdateResult(division, true);
        }
        else if (operation.equals("loadPointsPeriod")) {
            resultOperation = "Load points period - nbTeams updated="+tourTeamMgr.getMemoryMgr().setTeamPointsAndResultPeriodForDivision(division);
        }
        else if (operation.equals("reloadAverageHandicap")) {
            resultOperation = "Load points period - nbTeams updated="+tourTeamMgr.getMemoryMgr().reloadTeamAverageHandicapForDivision(division);
        }
    }
    List<TeamMemTournament> listTournament = tourTeamMgr.getMemoryMgr().listMemTournamentForDivision(division);
    List<TeamMemDivisionResultTeam> rankingPeriod = null;
    List<TeamMemDivisionTourResultTeam> rankingTour = null;

    if (rankingType.equals("TOUR")) {
        rankingTour = divisionTourResult.getRanking(0,500);
    }
    else if (rankingType.equals("PERIOD")) {
        rankingPeriod = divisionResult.getRanking(0,500);
    }
%>
<html>
    <head>
        <title>ADMINISTRATION TOUR TEAM DIVISION</title>
        <script type="text/javascript">
            function clickRanking(type) {
                document.forms["formTourTeamDivision"].rankingType.value=type;
                document.forms["formTourTeamDivision"].submit();
            }
            function clickRunOperation(operation) {
                if (confirm("Run operation "+operation+" ?")) {
                    document.forms["formTourTeamDivision"].operation.value = operation;
                    document.forms["formTourTeamDivision"].submit();
                }
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION TOUR TEAM <%=division%></h1>
        [<a href="adminTourTeam.jsp">Administration TOUR TEAM</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formTourTeamDivision" method="post" action="adminTourTeamDivision.jsp?division=<%=division%>">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="division" value="<%=division%>"/>
            <input type="hidden" name="rankingType" value="<%=rankingType%>"/>
            Division Result=<%=divisionResult%><br/>
            Threshold ranking UP = <%=divisionResult.getPositionThresholdUp()%> - Threshold ranking DOWN = <%=divisionResult.getPositionThresholdDown()%><br/>
            Division TourResult=<%=divisionTourResult%><br/>
            Average performance minimum value=<%=divisionResult.averagePerformanceMinimum%>&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Compute Minimum value" onclick="clickRunOperation('computeAveragePerformanceMinimum')"><br/>
            TEST - <input type="button" value="Test PlayDeal" onclick="clickRunOperation('testPlayDeal')">&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Test PlayDealAll" onclick="clickRunOperation('testPlayDealAll')"><br/>

            <br/>
            <b>Tournaments for this division</b><br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Tour</th><th>Group</th><th>Nb players</th><th>Nb players finish</th></tr>
                <% for (TeamMemTournament memTournament : listTournament) {%>
                <tr>
                    <td><a href="adminTourTeamTournament.jsp?tournamentID=<%=memTournament.tourID%>"><%=memTournament.tourID%></a></td>
                    <td><%=memTournament.group%></td>
                    <td><%=memTournament.getNbPlayers()%></td>
                    <td><%=memTournament.getNbPlayersWhoFinished()%></td>
                </tr>
                <%}%>
            </table>
            <hr width="90%"/>
            <b>Results for division <%=division%></b><br/>
            <% if (rankingTour != null) {%>
            <b>Ranking for current tour</b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Ranking for PERIOD" onclick="clickRanking('PERIOD')"><br/>
            <br/>
            <input type="button" value="Compute Tour Result & Ranking" onclick="clickRunOperation('computeTourResultRanking')"><br/>
            <br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Team ID</th><th>Team Name</th><th>Rank</th><th>Result</th><th>Points</th><th>Nb tournament played</th><th>Details</th></tr>
                <% for (TeamMemDivisionTourResultTeam resultTeam : rankingTour) {%>
                <tr>
                    <td><%=resultTeam.teamID%></td>
                    <td><%=teamCacheMgr.getOrLoadTeamCache(resultTeam.teamID).name%></td>
                    <td><%=resultTeam.rank%></td>
                    <td><%=resultTeam.result%></td>
                    <td><%=resultTeam.points%></td>
                    <td><%=resultTeam.getNbTournamentPlayed()%></td>
                    <td>
                        <%
                            String details = "";
                            for (TeamMemTournamentPlayer data : resultTeam.listData) {
                                if (details.length() > 0) {
                                    details += "<br>";
                                }
                                details += "group="+data.memTour.group+" - "+data.toString();
                            }
                        %>
                        <%=details%>
                    </td>
                </tr>
                <%}%>
            </table>
            <%}%>
            <% if (rankingPeriod != null) {%>
            <b>Ranking for current period</b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Ranking for TOUR" onclick="clickRanking('TOUR')"><br/>
            <br/>
            <input type="button" value="Compute Ranking" onclick="clickRunOperation('computeRanking')">&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Load Points Period" onclick="clickRunOperation('loadPointsPeriod')">&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Reload Average Handicap" onclick="clickRunOperation('reloadAverageHandicap')"><br/>
            <br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Team ID</th><th>Team Name</th><th>Rank</th><th>Points (Period)</th><th>Result (Period)</th><th>Handicap</th><th>Trend</th></tr>
                <% for (TeamMemDivisionResultTeam resultTeam : rankingPeriod) {%>
                <tr>
                    <td><%=resultTeam.teamID%></td>
                    <td><%=teamCacheMgr.getOrLoadTeamCache(resultTeam.teamID).name%></td>
                    <td><%=resultTeam.rank%></td>
                    <td><%=resultTeam.points%> (<%=resultTeam.pointsPeriod%>)</td>
                    <td><%=resultTeam.results%> (<%=resultTeam.resultsPeriod%>)</td>
                    <td><%=resultTeam.averageHandicap%></td>
                    <td><%=resultTeam.trend%></td>
                </tr>
                <%}%>
            </table>
            <%}%>
        </form>
    </body>
</html>
