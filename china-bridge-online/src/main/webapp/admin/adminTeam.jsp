<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.team.TeamMgr" %>
<%@ page import="com.funbridge.server.team.cache.TeamCacheMgr" %>
<%@ page import="com.funbridge.server.team.data.Team" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    TeamMgr teamMgr = ContextManager.getTeamMgr();
    String paramSearch = request.getParameter("searchTeam");
    String paramCountryCode = request.getParameter("countryCode");
    String paramDivision = request.getParameter("division");
    List<Team> listTeam = null;
    int nbEmptyTeams = -1;
    if (operation != null) {
        if (operation.equals("updateLastPeriodWithCompleteTeamForAllDivision")) {
            ContextManager.getTourTeamMgr().updateLastPeriodWithCompleteTeamForAllDivision();
            resultOperation = "Success to update filed lastPeriodWithCompleteTeam for all teams ...";
        }
        else if (operation.equals("upgradeHandicapAndGroupForAllTeams")) {
            ContextManager.getTourTeamMgr().upgradeHandicapAndGroupForAllTeams();
            resultOperation = "Success to upgrade handicap and group for all teams ...";
        }
        else if (operation.equals("upgradeHandicapAndGroup")) {
            String teamID = request.getParameter("teamSelected");
            if (teamID != null) {
                Team team = teamMgr.findTeamByID(teamID);
                if (team != null) {
                    ContextManager.getTourTeamMgr().upgradeHandicapAndGroupForTeam(team, true);
                    resultOperation = "Success to upgrade handicap and group for team="+team;
                } else {
                    resultOperation = "Failed - no team found with ID="+teamID;
                }
            } else {
                resultOperation = "Failed - no teamSelected parameter found";
            }
        }
        else if (operation.equals("displayHistory")) {
            String teamID = request.getParameter("teamSelected");
            if (teamID != null) {
                Team team = teamMgr.findTeamByID(teamID);
                if (team != null) {
                    resultOperation = "Display history for team="+team+"<br>History="+ContextManager.getTourTeamMgr().getDivisionHistoryForTeam(team);
                } else {
                    resultOperation = "Failed - no team found with ID="+teamID;
                }
            } else {
                resultOperation = "Failed - no teamSelected parameter found";
            }
        }
        else if (operation.equals("createTeamsTEST")) {
            List<Team> listTeamsTEST = teamMgr.createTeamsTEST();
            resultOperation = "Nb team TEST created : "+listTeamsTEST.size();
        }
        else if (operation.equals("integrateTeamsInDivision")) {
            resultOperation = "Nb teams integrated="+ContextManager.getTourTeamMgr().processIntegrateTeamsInDivisions();
        }
        else if (operation.equals("simulationTeamsIntegration")) {
            Map<String, List<Team>> simulateIntegrationTeams = ContextManager.getTourTeamMgr().simulationIntegrationTeamsInDivision();
            List<String> listDivisionIntegration = new ArrayList<String>(simulateIntegrationTeams.keySet());
            Collections.sort(listDivisionIntegration);
            TeamCacheMgr teamCacheMgr = ContextManager.getTeamCacheMgr();
            for (String div : listDivisionIntegration) {
                List<Team> teamsDivIntegration = simulateIntegrationTeams.get(div);
                resultOperation += "Division "+div+" - nb teams="+teamsDivIntegration.size()+" - teams :";
                for (Team t : teamsDivIntegration) {
                    resultOperation += " ("+teamCacheMgr.getOrLoadTeamCache(t.getIDStr()).name+", "+t.getIDStr()+")";
                }
                resultOperation += "<br/>";
            }
        }
        else if (operation.equals("listEmptyTeams")) {
            listTeam = teamMgr.listEmptyTeams();
            nbEmptyTeams = listTeam.size();
            resultOperation = "LIST EMPTY TEAMS - nb teams="+nbEmptyTeams;
        }
        else if (operation.equals("clearEmptyTeams")) {
            List<Team> emptyTeamsRemove = teamMgr.clearEmptyTeams();
            resultOperation = "Nb teams removed : "+emptyTeamsRemove.size()+"<br/>";
            for (Team e : emptyTeamsRemove) {
                resultOperation += e.toString()+"<br/>";
            }
        }
    }
    int offset = 0;
    int nbMax = 50;
    String paramOffset = request.getParameter("offset");
    if (paramOffset != null) {
        offset = Integer.parseInt(paramOffset);
        if (offset < 0) {
            offset = 0;
        }
    }
    if (listTeam == null) {
        listTeam = teamMgr.listTeams(paramSearch, paramCountryCode, paramDivision, offset, nbMax);
    }
    if (nbEmptyTeams == -1) {
        List<Team> emptyTeams = teamMgr.listEmptyTeams();
        nbEmptyTeams = emptyTeams.size();
    }
%>
<html>
    <head>
        <title>Funbridge Server - Administration</title>
        <script type="text/javascript" src="renderjson.js"></script>
        <script type="text/javascript">
            function clickOffset(offset) {
                document.forms["formTeam"].offset.value=offset;
                document.forms["formTeam"].submit();
            }
            function clickRunOperation(op) {
                if (confirm("Run operation : "+op+" ?")) {
                    document.forms["formTeam"].operation.value=op;
                    document.forms["formTeam"].submit();
                }
            }
            function clickRunOperationForTeam(teamName, teamID, op) {
                if (confirm("Run operation : "+op+" for the team "+teamName+" ?")) {
                    document.forms["formTeam"].operation.value=op;
                    document.forms["formTeam"].teamSelected.value=teamID;
                    document.forms["formTeam"].submit();
                }
            }
        </script>

    </head>
    <body>
        <h1>ADMINISTRATION TEAM</h1>
        [<a href="admin.jsp">Administration</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formTeam" method="post" action="adminTeam.jsp">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="teamSelected" value=""/>
            <input type="hidden" name="offset" value="<%=offset%>"/>
            Update Field lastPeriodWithCompleteTeam : <input type="button" value="Update Field for All Teams" onclick="clickRunOperation('updateLastPeriodWithCompleteTeamForAllDivision')"><br/>
            Upgrade Handicap And Group For All Teams : <input type="button" value="Upgrade for All Teams" onclick="clickRunOperation('upgradeHandicapAndGroupForAllTeams')"><br/>
            Nb teams to integrate : <%=teamMgr.countTeamsReadyToIntegrateDivisions()%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Simulation teams integration" onclick="clickRunOperation('simulationTeamsIntegration')">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Integrate news teams in divisions" onclick="clickRunOperation('integrateTeamsInDivision')"><br/>
            Nb empty teams : <%=nbEmptyTeams%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="List empty teams" onclick="clickRunOperation('listEmptyTeams')">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Clear empty teams" onclick="clickRunOperation('clearEmptyTeams')"><br/>
            <br/>
            TEST - Create teams for test ... <input type="button" value="Create Teams TEST" onclick="clickRunOperation('createTeamsTEST')"><br/>
            Nb Teams = <%=teamMgr.countTeams(paramSearch, paramCountryCode, paramDivision)%><br/>
            <br/>
            Search : <input type="text" name="searchTeam" value="<%=paramSearch%>">&nbsp;&nbsp;&nbsp;<input type="button" value="Search" onclick="document.forms['formTeam'].submit()"><br/>
            <%if(offset > 0) {%>
            <input type="button" value="Previous" onclick="clickOffset(<%=offset - nbMax%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (listTeam != null && listTeam.size() >= nbMax) {%>
            <input type="button" value="Next" onclick="clickOffset(<%=offset + nbMax%>)">
            <%}%>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>ID</th><th>Name</th><th>CountryCode</th><th>Creation</th><th>Division</th><th>AverageHandicap</th><th>Nb Players</th><th>periodComplete</th><th>ChatroomID</th><th>Operations</th><th>Data</th></tr>
                <% for (Team t : listTeam) {%>
                <tr>
                    <td><%=t.getIDStr()%></td>
                    <td><%=t.getName()%></td>
                    <td><%=t.getCountryCode()%></td>
                    <td><%=Constantes.timestamp2StringDateHour(t.getDateCreation())%></td>
                    <td><%=t.getDivision()%></td>
                    <td><%=t.getAverageHandicap()%></td>
                    <td><%=t.getNbPlayers()%></td>
                    <td><%=t.getLastPeriodWithCompleteTeam()%></td>
                    <td><%=t.getChatroomID()%></td>
                    <td>
                        <input type="button" value="Upgrade Handicap And Group" onclick="clickRunOperationForTeam('<%=t.getName()%>','<%=t.getIDStr()%>','upgradeHandicapAndGroup')"/>&nbsp;
                        <input type="button" value="Display division history" onclick="clickRunOperationForTeam('<%=t.getName()%>','<%=t.getIDStr()%>','displayHistory')"/>
                    </td>
                    <td>
                        <div id="team_<%=t.getIDStr()%>"></div>
                        <script>
                            document.getElementById("team_<%=t.getIDStr()%>").appendChild(
                                renderjson.set_icons('+','-')(<%=ContextManager.getJSONTools().transform2String(t, false) %>)
                            );
                        </script>
                    </td>
                </tr>
                <% }%>
            </table>
        </form>
    </body>
</html>
