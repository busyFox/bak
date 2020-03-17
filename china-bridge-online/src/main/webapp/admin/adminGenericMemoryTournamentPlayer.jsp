<%@page import="com.funbridge.server.common.ContextManager"%>
<%@ page import="com.funbridge.server.tournament.generic.TournamentGenericMgr" %>
<%@ page import="com.funbridge.server.tournament.generic.memory.GenericMemTournamentPlayer" %>
<%@ page import="com.funbridge.server.tournament.generic.memory.TournamentGenericMemoryMgr" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.List" %>
<%@include file="common.jsp"%>
<html>
<%
    long playerID = 0;
    try {
        playerID = Long.parseLong(request.getParameter("playerid"));
    } catch (Exception e) {}
    int category = Integer.parseInt(request.getParameter("category"));
    TournamentGenericMgr tourMgr = ContextManager.getTournamentMgrForCategory(category);
    TournamentGenericMemoryMgr memMgr = tourMgr.getMemoryMgr();
    Calendar dateBefore = Calendar.getInstance();
    dateBefore.add(Calendar.DAY_OF_YEAR, - 30);
    long dateRef = dateBefore.getTimeInMillis();
    List<GenericMemTournamentPlayer> listTournamentPlayers = memMgr.listMemTournamentForPlayer(playerID, false, 0, dateRef);
    String operation = request.getParameter("operation");
    String resultOperation = "";
    if (operation != null) {
    }
%>
<head>
    <title>Funbridge Server - Administration Tournament Player Memory Detail</title>
    <script type="text/javascript">
    </script>
</head>
<body>
<h1>TOURNAMENT <%=Constantes.tourCategory2Name(category)%> PLAYER CACHE DETAIL</h1>
[<a href="<%=getAdminTourPage(category)%>">Back to Tournament list</a>]
<br/><br/>
<%if (resultOperation.length() > 0) {%>
<b>RESULTAT OPERATION : <%= operation%></b>
<br/>Result = <%=resultOperation %>
<hr width="90%"/>
<%} %>
<%if (playerID == 0) {%>
PlayerID <%=playerID %> not valid. No tournament player found !
<%} else {%>
PlayerID : <%= playerID%>
<br/><%} %>
<%if (listTournamentPlayers == null) { %>
No more data for this player in memory
<%} else { %>
Memory result : nb TournamentPlayer=<%=listTournamentPlayers.size() %><br/>
<form name="formGenericTourPlayer" method="post" action="adminGenericMemoryTournamentlayer.jsp">
    <input type="hidden" name="category" value="<%=category%>"/>
    <input type="hidden" name="playerid" value="<%=playerID%>"/>
    <input type="hidden" name="operation" value=""/>
<br/>
<b>List of Tournaments:</b>
<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
    <tr><th>Ranking</th><th>Ranking Finished</th><th>Result</th><th>Date Start</th><th>Date Last Play</th><th>Current Deal Index</th><th>Nb Players Finish<br>With Best Result</th><th>Played Deals</th><th>Operation</th></tr>
    <%
        for (GenericMemTournamentPlayer memTournamentPlayer : listTournamentPlayers) {
    %>
    <tr>
        <td><%=memTournamentPlayer.ranking %></td>
        <td><%=memTournamentPlayer.rankingFinished %></td>
        <td><%=memTournamentPlayer.result %></td>
        <td><%=Constantes.timestamp2StringDateHour(memTournamentPlayer.dateStart) %></td>
        <td><%=Constantes.timestamp2StringDateHour(memTournamentPlayer.dateLastPlay) %></td>
        <td><%=memTournamentPlayer.currentDealIndex %></td>
        <td><%=memTournamentPlayer.nbPlayerFinishWithBestResult %></td>
        <td>
            <%
                for (String deal : memTournamentPlayer.playedDeals) {
            %>
            <%=deal %><br>
            <%}%>
        </td>
        <td><a href="adminGenericMemoryTournament.jsp?tourid=<%=memTournamentPlayer.memTour.tourID %>&category=<%=Constantes.TOURNAMENT_CATEGORY_TRAINING%>">Detail</a></td>
    </tr>

    <%}
    %>
</table>
<%} %>
</body>
</html>