<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.tournament.generic.memory.GenericMemDeal"%>
<%@page import="com.funbridge.server.tournament.generic.memory.GenericMemTournament"%>
<%@page import="com.funbridge.server.tournament.generic.memory.TournamentGenericMemoryMgr"%>
<%@ page import="com.funbridge.server.tournament.training.TrainingMgr" %>
<%@ page import="com.funbridge.server.tournament.training.data.TrainingTournament" %>
<%@ page import="com.funbridge.server.ws.result.WSResultTournamentPlayer" %>
<%@ page import="java.util.List" %>
<html>
<%
    String tourID = request.getParameter("tourid");
    TrainingMgr tourMgr = ContextManager.getTrainingMgr();
    TournamentGenericMemoryMgr memMgr = tourMgr.getMemoryMgr();
    TrainingTournament tour = (TrainingTournament)tourMgr.getTournament(tourID);
    String operation = request.getParameter("operation");
    String resultOperation = "";
    if (operation != null) {
        if (operation.equals("delete")) {
            resultOperation = "Delete tournament from memory : ";
            if (memMgr.removeTournament(tourID)) {
                resultOperation += "Sucess";
            } else {
                resultOperation += "Failed";
            }
        } else if (operation.equals("finish")) {
            tourMgr.finishTournament(tourID);
            resultOperation += "Sucess finish for training";
        } else if (operation.equals("backup")) {
            resultOperation = "Backup tournament to file : "+tourID;
            GenericMemTournament mtr = memMgr.getTournament(tourID);
            if (mtr != null) {
                if (memMgr.backupMemTourResultToFile(mtr)) {
                    resultOperation += "<br/>Sucess to transform memTourResult to string mtr="+mtr;
                } else {
                    resultOperation += "<br/>Failed to transform memTourResult to string mtr="+mtr;
                }
            }
        } else if (operation.equals("computeResult")) {
            resultOperation = "Time to compute result : ";
            GenericMemTournament mtr = memMgr.getTournament(tourID);
            long ts = System.currentTimeMillis();
            mtr.computeResult();
            resultOperation += (System.currentTimeMillis() - ts)+" ms";
        }
    }
    GenericMemTournament mtr = memMgr.getTournament(tourID);
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
    List<WSResultTournamentPlayer> listResultPlayer = tourMgr.getListWSResultTournamentPlayer(tour, rankingOffset, rankingNbMax, null, -1, useRankingFinished, null);
%>
<head>
    <title>Funbridge Server - Administration Tournament Memory Detail</title>
    <script type="text/javascript">
        function clickComputeResult() {
            if (confirm("Compute result on this tournament ?")) {
                document.forms["formTraining"].operation.value="computeResult";
                document.forms["formTraining"].submit();
            }
        }
        function clickDelete() {
            if (confirm("Delete this tournament from memory ?")) {
                document.forms["formTraining"].operation.value="delete";
                document.forms["formTraining"].submit();
            }
        }
        function clickBackup() {
            if (confirm("Backup this tournament to file ?")) {
                document.forms["formTraining"].operation.value="backup";
                document.forms["formTraining"].submit();
            }
        }
        function clickFinish() {
            if (confirm("Finish this tournament ?")) {
                document.forms["formTraining"].operation.value="finish";
                document.forms["formTraining"].submit();
            }
        }
        function clickRankingOffset(offset) {
            document.forms["formTraining"].rankingoffset.value=offset;
            document.forms["formTraining"].submit();
        }
        function changeCheckFinish(value) {
            document.forms["formTraining"].rankingfinished.value=value;
            document.forms["formTraining"].submit();
        }
    </script>
</head>
<body>
<h1>TOURNAMENT TRAINING MEMORY DETAIL</h1>
[<a href="adminTraining.jsp">Back to Training Tournament list</a>]
<br/><br/>
<%if (resultOperation.length() > 0) {%>
<b>RESULTAT OPERATION : <%= operation%></b>
<br/>Result = <%=resultOperation %>
<hr width="90%"/>
<%} %>
<%if (tour == null) {%>
Tournament ID <%=tourID %> not valid. No tournament found !
<%} else {%>
Tournament : <%= tour.toString()%>
<br/><%} %>
<%if (mtr == null) { %>
No more data for this tournament in memory
<%} else { %>
Memory result : nb Player=<%=mtr.getNbPlayer() %> - nb Player finish=<%=mtr.getNbPlayerFinishAll() %><br/>
<form name="formTraining" method="post" action="adminTrainingMemoryTournament.jsp">
    <input type="hidden" name="tourid" value="<%=tourID%>"/>
    <input type="hidden" name="operation" value=""/>
    <input type="hidden" name="rankingoffset" value="<%=rankingOffset%>"/>
    <input type="hidden" name="rankingfinished" value="<%=useRankingFinished?"1":"0"%>"/>
    <input type="button" value="Compute result" onclick="clickComputeResult()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
    <input type="button" value="Delete from memory" onclick="clickDelete()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
    <input type="button" value="Backup file" onclick="clickBackup()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
    <input type="button" value="Finish tournament" onclick="clickFinish()"/>
</form>
<b>List of Deals:</b>
<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
    <tr><th>ID</th><th>Nb Player</th><th>Score Average</th><th>Operation</th></tr>
    <%
        GenericMemDeal[] resultDeal = mtr.deals;
        for (int i = 0; i < resultDeal.length; i++) {
            GenericMemDeal mrd = resultDeal[i];
    %>
    <tr><td><%=mrd.dealID %></td><td><%=mrd.getNbPlayers() %></td><td><%=mrd.getScoreAverage() %></td><td><a href="adminTrainingMemoryDeal.jsp?dealid=<%=mrd.dealID %>">Detail</a></td></tr>

    <%}
    %>
</table>
<br/>
<b>Ranking :</b><br/>
Only player finished tournament : <input type="checkbox" name="checkFinish" <%=useRankingFinished?"checked":""%> onchange="changeCheckFinish(<%=useRankingFinished?'0':'1'%>)"><br/>
<%if(rankingOffset > 0) {%>
<input type="button" value="Previous" onclick="clickRankingOffset(<%=rankingOffset - rankingNbMax%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<%}%>
<% if (listResultPlayer.size() >= rankingNbMax) {%>
<input type="button" value="Next" onclick="clickRankingOffset(<%=rankingOffset + rankingNbMax%>)">
<%}%>
<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
    <tr><th>Rank</th><th>Finisher</th><th>PlayerID</th><th>Player Pseudo</th><th>Result</th><th>Nb Deal Played</th></tr>
    <%
        for (WSResultTournamentPlayer resPla : listResultPlayer) {
    %>
    <tr><td><%=resPla.getRank() %></td><td><%=mtr.hasPlayerFinish(resPla.getPlayerID())%></td><td><%=resPla.getPlayerID() %></td><td><%=resPla.getPlayerPseudo() %></td><td><%=resPla.getResult() %></td><td><%=resPla.getNbDealPlayed() %></td></tr>
    <%}%>
</table>

<%} %>
</body>
</html>