<%@ page import="com.funbridge.server.common.FBConfiguration" %>
<html>
	<head>
		<title>Administration</title>
	</head>
	<body>
		<h1 align="center">ADMINISTRATION FUNBRIDGE</h1>
		<div align="center">
			<b>
				[<a href="adminSession.jsp">SESSION</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminEngine.jsp">ENGINE</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminGameMgr.jsp">GAME MGR</a>]
				<br/><br/>
				[<a href="adminPlayer.jsp">PLAYER</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminPlayerCache.jsp">PLAYER CACHE</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminMessage.jsp">MESSAGES, PUSH & NOTIF</a>]
				<br/><br/>
            	[<a href="adminTraining.jsp">TRAINING</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminSerieTopChallenge.jsp">SERIE TOP CHALLENGE</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminSerieEasyChallenge.jsp">SERIE EASY CHALLENGE</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminLearning.jsp">LEARNING</a>]
				<br><br>
				[<a href="adminCBOFederation.jsp">CBO Federation</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminTimezone.jsp">TIMEZONE</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminSerie.jsp">SERIE</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminTourPrivate.jsp">PRIVATE</a>]
				<br><br>
				[<a href="adminDuel.jsp">DUEL</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminTrainingPartner.jsp">TRAINING PARTNER</a>]
				<br/><br/>
				[<a href="adminTeam.jsp">TEAM</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminTourTeam.jsp">TOUR TEAM</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminTeamCache.jsp">TEAM CACHE</a>]
				<br/><br/>
				[<a href="adminTournamentPlay.jsp">TOURNAMENT PLAY</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminRanking.jsp">RANKING</a>]
				<br/><br/>
				[<a href="adminTextsUI.jsp">TEXTS UI</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminOperation.jsp">OPERATIONS</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminConfiguration.jsp">CONFIGURATION</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminTools.jsp">TOOLS</a>]&nbsp;&nbsp;--&nbsp;&nbsp;[<a href="adminNursing.jsp">NURSING</a>]
			</b>
            <br/><br/>
            Version : <%=FBConfiguration.getInstance().getVersionTxt()%>
		</div>
	</body>
</html>