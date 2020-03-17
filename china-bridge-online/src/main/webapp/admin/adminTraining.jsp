<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.tournament.game.GameMgr"%>
<%@page import="com.funbridge.server.tournament.generic.memory.GenericMemTournament"%>
<%@page import="com.funbridge.server.tournament.training.TrainingMgr"%>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%
    TrainingMgr tourMgr = ContextManager.getTrainingMgr();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    String operation = request.getParameter("operation");
    String resultOperation = "";
    if (operation != null) {
        if (operation.equals("loadData")) {
            String strPathFile = request.getParameter("pathFile");
            long ts = System.currentTimeMillis();
            if (tourMgr.getMemoryMgr().loadMemTourFromFile(strPathFile)!= null) {
                resultOperation = "Success to load data from file="+strPathFile+" - ts="+(System.currentTimeMillis() - ts);
            } else {
                resultOperation = "Failed to load data from file="+strPathFile+" - ts="+(System.currentTimeMillis() - ts);
            }
        }
        else if (operation.equals("backupData")) {
            int nbBackupOK = tourMgr.getMemoryMgr().backupAllTour();
            resultOperation = "Backup success - nbTour="+tourMgr.getMemoryMgr().getSize()+" - nbBackup="+nbBackupOK;
        }
        else if (operation.equals("closeFinishTournament")) {
            int nbTourFinishProcess = 0;
            int nbMaxProcess = 10;
            List<GenericMemTournament> listTourResult = tourMgr.getMemoryMgr().listTournament();
            for (GenericMemTournament e : listTourResult) {
                if (e.getNbPlayerFinishAll()>=e.nbMaxPlayers) {
                    tourMgr.finishTournament(e.tourID);
                    nbTourFinishProcess++;
                }
                if (nbTourFinishProcess >= nbMaxProcess) {
                    break;
                }
            }
            resultOperation = "CloseFinishTournament - nbTourFinishProcess="+nbTourFinishProcess;
        }
    }
    List<GenericMemTournament> listTourResult = tourMgr.getMemoryMgr().listTournament();
    int nbTourFinish = 0;
    for (GenericMemTournament e : listTourResult) {
        if (e.getNbPlayerFinishAll()>=e.nbMaxPlayers) {
            nbTourFinish++;
        }
    }
%>

<html>
	<head>
		<title>Funbridge Server - Administration Tournament Memory</title>
		<script type="text/javascript">
		function clickLoadData() {
			if (confirm("Load data from file ?")) {
				document.forms["formTraining"].operation.value="loadData";
				document.forms["formTraining"].submit();
			}
		}
		function clickBackupData() {
			if (confirm("Backup all data to file ?")) {
				document.forms["formTraining"].operation.value="backupData";
				document.forms["formTraining"].submit();
			}
		}
        function clickCloseFinishTournament() {
            if (confirm("Close tournaments finished ? (nbMax=10)")) {
                document.forms["formTraining"].operation.value="closeFinishTournament";
                document.forms["formTraining"].submit();
            }
        }
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION TOURNAMENT TRAINING</h1>
		[<a href="admin.jsp">Administration</a>]
		<br/><br/>
		<%if (resultOperation.length() > 0) {%>
		<b>RESULTAT OPERATION : <%= operation%></b>
		<br/>Result = <%=resultOperation %>
		<hr width="90%"/>
		<%} %>
		<form name="formTraining" method="post" action="adminTraining.jsp">
		<input type="hidden" name="operation" value=""/>
		<b>Load tournament memory from file</b><br/>
		JSON Path file : <input type="text" name="pathFile" size="80"/><br/>
		<input type="button" value="Load Data" onclick="clickLoadData()"/>
		<hr width="90%"/>
		<b>Backup all memory tournament to file</b><br/>
		<input type="button" value="Backup All Data" onclick="clickBackupData()"/>
        <hr width="90%"/>
        <b>Close tournament finished</b><br/>
            Close tournament with all players have finished the tournament.<br/>
        <input type="button" value="Close tournament finished" onclick="clickCloseFinishTournament()"/>
		<hr width="90%"/>
		Nb player in this mode = <%=tourMgr.getGameMgr().getPlayerRunningSize() %><br/>
		<br/>
		<b>SCHEDULER</b><br/>
		Date next scheduler generator=<%=tourMgr.getStringDateNextGeneratorScheduler() %><br/>
		Date next scheduler close=<%=tourMgr.getStringDateNextCloseScheduler() %><br/>
		<br/>
		<b>GAME STATUS</b><br/>
		<%
		    GameMgr.ThreadPoolData threadDataPlay = tourMgr.getGameMgr().getThreadPoolDataPlay();
            GameMgr.ThreadPoolData threadDataReplay = tourMgr.getGameMgr().getThreadPoolDataReplay();
		%>
		Play Game status : ActiveCount=<%=threadDataPlay.activeCount%> - QueueSize=<%=threadDataPlay.queueSize%> - PoolSize=<%=threadDataPlay.poolSize%><br/>
		Replay Game status : ActiveCount=<%=threadDataReplay.activeCount%> - QueueSize=<%=threadDataReplay.queueSize%> - PoolSize=<%=threadDataReplay.poolSize%><br/>
		<br/>
		<b>TOURNAMENT IN MEMORY</b><br/>
		Nb tournament = <%=listTourResult.size() %><br/>
        Nb tournament Finish = <%=nbTourFinish %><br/>
		Nb tournament not full IMP = <%=tourMgr.countNbNotFullTournamentsInMemory(Constantes.TOURNAMENT_RESULT_IMP) %> - Nb Tournament not full PAIRE = <%=tourMgr.countNbNotFullTournamentsInMemory(Constantes.TOURNAMENT_RESULT_PAIRE) %>
		<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
			<tr><th>ID</th><th>resultType</th><th>End date</th><th>Nb Player</th><th>Nb Player Finish</th><th>Operation</th></tr>
			<% for (GenericMemTournament mtr : listTourResult) {%>
			<tr>
				<td><%=mtr.tourID %></td>
				<td><%=mtr.resultType %></td>
				<td><%=sdf.format(new Date(mtr.endDate)) %></td>
				<td><%=mtr.getNbPlayer() %></td>
				<td><%=mtr.getNbPlayerFinishAll() %></td>
				<td><a href="adminGenericMemoryTournament.jsp?tourid=<%=mtr.tourID %>&category=<%=Constantes.TOURNAMENT_CATEGORY_TRAINING%>">Detail</a></td>
			</tr>
			<%}%>	
		</table>
		</form>
	</body>
</html>