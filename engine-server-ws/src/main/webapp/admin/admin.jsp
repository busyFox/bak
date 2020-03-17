<%@ page import="com.gotogames.bridge.engineserver.common.EngineConfiguration" %>
<html>
	<head>
		<title>EngineServer - Administration</title>
	</head>
	<body>
		<h1>ADMINISTRATION ENGINE SERVER</h1>
        <p align="center">
            <b>
                [<a href="adminSession.jsp">SESSION</a>]&nbsp;&nbsp;--&nbsp;&nbsp;
                [<a href="adminUserEngine.jsp">USER ENGINE</a>]&nbsp;&nbsp;--&nbsp;&nbsp;
                [<a href="adminUserServer.jsp">USER SERVER</a>]<br/>
                [<a href="adminCache.jsp">CACHE</a>]&nbsp;&nbsp;--&nbsp;&nbsp;
                [<a href="adminQueue.jsp">QUEUE</a>]&nbsp;&nbsp;--&nbsp;&nbsp;
                [<a href="adminQueueTest.jsp">QUEUE TEST</a>]&nbsp;&nbsp;--&nbsp;&nbsp;
                [<a href="adminRequest.jsp">REQUEST</a>]<br/>
                [<a href="adminTools.jsp">TOOLS</a>]&nbsp;&nbsp;--&nbsp;&nbsp;
                [<a href="../listAlert.jsp">ALERTS</a>]
            </b>
            <br/><br/>
            Version : <%=EngineConfiguration.getInstance().getVersionTxt()%>
        </p>
	</body>
</html>