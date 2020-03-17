<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.tournament.team.TourTeamMgr" %>
<%@ page import="com.funbridge.server.tournament.team.data.TeamTournament" %>
<%@ page import="com.funbridge.server.tournament.team.memory.TeamMemDeal" %>
<%@ page import="com.funbridge.server.tournament.team.memory.TeamMemDealPlayer" %>
<%@ page import="com.funbridge.server.tournament.team.memory.TeamMemTournament" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    String dealID = request.getParameter("dealID");
    String tournamentID = TourTeamMgr.extractTourIDFromDealID(dealID);
    TourTeamMgr tourMgr = ContextManager.getTourTeamMgr();
    TeamTournament tournament = tourMgr.getTournament(tournamentID);
    TeamMemDeal memDeal = null;
    TeamMemTournament memTournament = null;
    if (tournament != null) {
        memTournament = tourMgr.getMemoryMgr().getMemTournamentDivisionAndTourID(tournament.getDivision(), tournamentID);
        if (memTournament != null) {
            memDeal = memTournament.getDealWithID(dealID);
        }
    }
%>
<html>
    <head>
        <title>ADMINISTRATION TOUR TEAM - DEAL</title>
        <script type="text/javascript">
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION TOUR TEAM - DEAL ID = <%=dealID%></h1>
        [<a href="adminTourTeamTournament.jsp?tournamentID=<%=tournamentID%>">Back to Tournament</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formTourTeamDeal" method="post" action="adminTourTeamDeal.jsp?dealID=<%=dealID%>">
            <input type="hidden" name="operation" value=""/>
            Tournament = <%=tournament%><br/>
            MemTournament = <%=memTournament%> - nbPlayersForRanking=<%=memTournament.getNbPlayersForRanking()%><br/>
            MemDeal = <%=memDeal%><br/>
            <b>Ranking</b><br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Result</th><th>PlayerID</th><th>Score</th><th>NbPlayerBestScore</th><th>NbPlayerSameScore</th><th>Contract</th><th>Declarer</th><th>NbTricks</th><th>Operation</th></tr>
                <% for (TeamMemDealPlayer dealPlayer : memDeal.getResultListOrderByScore()) {%>
                <tr>
                    <td><%=dealPlayer.result%></td>
                    <td><%=dealPlayer.playerID%></td>
                    <td><%=dealPlayer.score%></td>
                    <td><%=dealPlayer.nbPlayerBetterScore%></td>
                    <td><%=dealPlayer.nbPlayerSameScore%></td>
                    <td><%=dealPlayer.getContractWS()%></td>
                    <td><%=dealPlayer.declarer%></td>
                    <td><%=dealPlayer.nbTricks%></td>
                    <td></td>
                </tr>
                <%}%>
            </table>
        </form>
    </body>
</html>
