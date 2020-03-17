<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.team.cache.TeamCacheMgr" %>
<%@ page import="com.funbridge.server.tournament.team.TourTeamMgr" %>
<%@ page import="com.funbridge.server.tournament.team.data.TeamTournament" %>
<%@ page import="com.funbridge.server.tournament.team.memory.TeamMemDeal" %>
<%@ page import="com.funbridge.server.tournament.team.memory.TeamMemTournament" %>
<%@ page import="com.funbridge.server.tournament.team.memory.TeamMemTournamentPlayer" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    String tournamentID = request.getParameter("tournamentID");
    TourTeamMgr tourTeamMgr = ContextManager.getTourTeamMgr();
    TeamTournament tournament = tourTeamMgr.getTournament(tournamentID);
    TeamCacheMgr teamCacheMgr = ContextManager.getTeamCacheMgr();
    TeamMemTournament memTournament = null;
    if (tournament != null) {
        memTournament = tourTeamMgr.getMemoryMgr().getMemTournamentDivisionAndTourID(tournament.getDivision(), tournamentID);

    }

    int rankingOffset = 0;
    int rankingNbMax = 50;
    String paramRankingOffset = request.getParameter("rankingoffset");
    if (paramRankingOffset != null) {
        rankingOffset = Integer.parseInt(paramRankingOffset);
        if (rankingOffset < 0) {
            rankingOffset = 0;
        }
    }
    boolean useRankingFinished = true;
    String paramRankingFinished = request.getParameter("rankingfinished");
    if (paramRankingFinished != null) {
        if (paramRankingFinished.equals("0")) {
            useRankingFinished = false;
        }
    }
    List<TeamMemTournamentPlayer> listRankingPlayer = null;
    if (memTournament != null) {
        listRankingPlayer = memTournament.getRanking(rankingOffset, rankingNbMax, null, useRankingFinished);
    }
%>
<html>
    <head>
        <title>ADMINISTRATION TOUR TEAM TOURNAMENT</title>
        <script type="text/javascript">
            function clickRankingOffset(offset) {
                document.forms["formTourTeamTournament"].rankingoffset.value=offset;
                document.forms["formTourTeamTournament"].submit();
            }
            function changeCheckFinish(value) {
                document.forms["formTourTeamTournament"].rankingfinished.value=value;
                document.forms["formTourTeamTournament"].submit();
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION TOUR TEAM - TOURNAMENT</h1>
        [<a href="adminTourTeamDivision.jsp?division=<%=tournament!=null?tournament.getDivision():"D01"%>">Administration TOUR TEAM DIVISION</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formTourTeamTournament" method="post" action="adminTourTeamTournament.jsp?tournamentID=<%=tournamentID%>">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="rankingoffset" value="<%=rankingOffset%>"/>
            <input type="hidden" name="rankingfinished" value="<%=useRankingFinished?"1":"0"%>"/>
            Tournament = <%=tournament%><br/>
            MemTournament = <%=memTournament%><br/>
            <% if (memTournament != null) {%>
            <b>Deals</b>
            <br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>DealID</th><th>Index</th><th>Nb Player</th></tr>
                <% for (TeamMemDeal memDeal : memTournament.deals) {%>
                <tr>
                    <td><a href="adminTourTeamDeal.jsp?dealID=<%=memDeal.dealID%>"><%=memDeal.dealID%></a></td>
                    <td><%=memDeal.dealIndex%></td>
                    <td><%=memDeal.getNbPlayers()%></td>
                </tr>
                <%}%>
            </table>
            <br/>
            <b>Ranking</b><br/>
            Only player finished tournament : <input type="checkbox" name="checkFinish" <%=useRankingFinished?"checked":""%> onchange="changeCheckFinish(<%=useRankingFinished?'0':'1'%>)"><br/>
            <%if(rankingOffset > 0) {%>
            <input type="button" value="Previous" onclick="clickRankingOffset(<%=rankingOffset - rankingNbMax%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (listRankingPlayer != null && listRankingPlayer.size() >= rankingNbMax) {%>
            <input type="button" value="Next" onclick="clickRankingOffset(<%=rankingOffset + rankingNbMax%>)">
            <%}%>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Rank</th><th>Rank finished</th><th>Player</th><th>Team ID</th><th>Team Name</th><th>Result</th><th>Points</th><th>Nb Deals played</th></tr>
                <% for (TeamMemTournamentPlayer rankPla : listRankingPlayer) {%>
                <tr>
                    <td><%=rankPla.ranking%></td>
                    <td><%=rankPla.rankingFinished%></td>
                    <td><%=rankPla.playerID%></td>
                    <td><%=rankPla.teamID%></td>
                    <td><%=teamCacheMgr.getOrLoadTeamCache(rankPla.teamID).name%></td>
                    <td><%=rankPla.result%></td>
                    <td><%=rankPla.points%></td>
                    <td><%=rankPla.getNbPlayedDeals()%></td>
                </tr>
                <%}%>
            </table>
            <%}%>
        </form>
    </body>
</html>
