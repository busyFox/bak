<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.tournament.game.GameMgr"%>
<%@page import="com.funbridge.server.tournament.generic.memory.GenericMemTournament"%>
<%@ page import="com.funbridge.server.tournament.timezone.TimezoneMgr" %>
<%@ page import="com.funbridge.server.tournament.timezone.data.TimezoneTournament" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%
TimezoneMgr tourMgr = ContextManager.getTimezoneMgr();
SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
List<TimezoneTournament> listTourNotFinished = tourMgr.listTournamentNotFinished();
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
		String strFileBackup = tourMgr.getStringResolvEnvVariableValue("backupMemoryPath", null);
		if (strFileBackup != null) {
			int nbBackupOK = tourMgr.getMemoryMgr().backupAllTour();
			resultOperation = "Backup success path="+strFileBackup+" - nbTour="+tourMgr.getMemoryMgr().getSize()+" - nbBackup="+nbBackupOK;
		} else {
			resultOperation = "Backup failed ... to backupMemoryPath defined in configuration";
		}
	}
}
List<GenericMemTournament> listTourResult = tourMgr.getMemoryMgr().listTournament();
%>

<html>
	<head>
		<title>Funbridge Server - Administration</title>
		<script type="text/javascript">
		function clickLoadData() {
			if (confirm("Load data from file ?")) {
				document.forms["formTimezone"].operation.value="loadData";
				document.forms["formTimezone"].submit();
			}
		}
		function clickBackupData() {
			if (confirm("Backup all data to file ?")) {
				document.forms["formTimezone"].operation.value="backupData";
				document.forms["formTimezone"].submit();
			}
		}
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION TOURNAMENT TIMEZONE</h1>
		[<a href="admin.jsp">Administration</a>]
		<br/><br/>
		<%if (resultOperation.length() > 0) {%>
		<b>RESULTAT OPERATION : <%= operation%></b>
		<br/>Result = <%=resultOperation %>
		<hr width="90%"/>
		<%} %>
		<form name="formTimezone" method="post" action="adminTimezone.jsp">
		<input type="hidden" name="operation" value=""/>
		<b>Load tournament memory from file</b><br/>
		JSON Path file : <input type="text" name="pathFile" size="80"/><br/>
		<input type="button" value="Load Data" onclick="clickLoadData()"/>
		<hr width="90%"/>
		<b>Backup all memory tournament to file</b><br/>
		<input type="button" value="Backup All Data" onclick="clickBackupData()"/>
		<hr width="90%"/>
		Nb player in this mode = <%=tourMgr.getGameMgr().getPlayerRunningSize()%><br/>
		<br/>
		<b>SCHEDULER</b><br/>
		Date next scheduler generator=<%=tourMgr.getStringDateNextGeneratorScheduler()%><br/>
		Date next scheduler finish=<%=tourMgr.getStringDateNextFinishScheduler()%><br/>
		<br/>
		<b>GAME STATUS</b><br/>
		<%
			GameMgr.ThreadPoolData threadDataPlay = tourMgr.getGameMgr().getThreadPoolDataPlay();
			GameMgr.ThreadPoolData threadDataReplay = tourMgr.getGameMgr().getThreadPoolDataReplay();
		%>
		Play Game status : ActiveCount=<%=threadDataPlay.activeCount%> - QueueSize=<%=threadDataPlay.queueSize%> - PoolSize=<%=threadDataPlay.poolSize%><br/>
		Replay Game status : ActiveCount=<%=threadDataReplay.activeCount%> - QueueSize=<%=threadDataReplay.queueSize%> - PoolSize=<%=threadDataReplay.poolSize%><br/>
		<br/>
		<b>TOURNAMENT NOT FINISHED</b><br/>
		Nb tournament = <%=listTourNotFinished.size() %><br/>
		<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
			<tr><th>ID</th><th>Name</th><th>resultType</th><th>Status</th><th>Begin date</th><th>End date</th><th>Nb Player</th><th>Operation</th></tr>
			<% for (TimezoneTournament t : listTourNotFinished) {%>
			<tr>
				<td><%=t.getIDStr() %></td>
				<td><%=t.getName() %></td>
				<td><%=t.getResultType() %></td>
				<td><%=t.getEndDate() < System.currentTimeMillis()?"FINISHED":t.getStartDate() > System.currentTimeMillis()?"WAITING":"RUNNING" %></td>
				<td><%=Constantes.timestamp2StringDateHour(t.getStartDate()) %></td>
				<td><%=Constantes.timestamp2StringDateHour(t.getEndDate()) %></td>
				<td><%=tourMgr.getMemoryMgr().getNbPlayerOnTournament(t.getIDStr(), null, false) %></td>
				<td></td>
			</tr>
			<%} %>
		</table>
		<br/>
		<b>TOURNAMENT IN MEMORY</b><br/>
		Nb tournament = <%=listTourResult.size() %><br/>
		<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
			<tr><th>ID</th><th>Name</th><th>NbDeal</th><th>resultType</th><th>End date</th><th>Nb Player</th><th>Nb Player Finish</th><th>Operation</th></tr>
			<% for (GenericMemTournament mtr : listTourResult) {%>
			<tr>
				<td><%=mtr.tourID %></td>
				<td><%=mtr.name %></td>
				<td><%=mtr.getNbDeal() %></td>
				<td><%=mtr.resultType %></td>
				<td><%=sdf.format(new Date(mtr.endDate)) %></td>
				<td><%=mtr.getNbPlayer() %></td>
				<td><%=mtr.getNbPlayerFinishAll() %></td>
				<td><a href="adminGenericMemoryTournament.jsp?tourid=<%=mtr.tourID %>&category=<%=Constantes.TOURNAMENT_CATEGORY_TIMEZONE%>">Detail</a></td>
			</tr>
			<%}%>	
		</table>
		</form>
	</body>
</html>