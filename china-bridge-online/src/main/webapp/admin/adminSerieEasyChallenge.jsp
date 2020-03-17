<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.tournament.serie.SerieEasyChallengeMgr" %>
<%@ page import="com.funbridge.server.ws.FBWSException" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    SerieEasyChallengeMgr SerieEasyChallengeMgr = ContextManager.getSerieEasyChallengeMgr();

    if (operation != null) {
        if (operation.equals("initPeriod")) {
            SerieEasyChallengeMgr.initPeriod();
            resultOperation = "Success to init period";
        }
        else if (operation.equals("enableSerieEasyChallenge")) {
            if (!SerieEasyChallengeMgr.isEnable()) {
                SerieEasyChallengeMgr.setEnable(true);
                resultOperation = "Success to enable SerieEasyChallenge ...";
            } else {
                resultOperation = "SerieEasyChallenge already enable !";
            }
        }
        else if (operation.equals("disableSerieEasyChallenge")) {
            if (SerieEasyChallengeMgr.isEnable()) {
                SerieEasyChallengeMgr.setEnable(false);
                resultOperation = "Success to disable SerieEasyChallenge ...";
            } else {
                resultOperation = "SerieEasyChallenge already disable !";
            }
        }
        else if (operation.equals("enablePeriodProcessing")) {
            if (!SerieEasyChallengeMgr.isPeriodChangeProcessing()) {
                SerieEasyChallengeMgr.setPeriodChangeProcessing(true);
                resultOperation = "Success to enable period processing ...";
            } else {
                resultOperation = "Period processing already enable !";
            }
        }
        else if (operation.equals("disablePeriodProcessing")) {
            if (SerieEasyChallengeMgr.isPeriodChangeProcessing()) {
                SerieEasyChallengeMgr.setPeriodChangeProcessing(false);
                resultOperation = "Success to disable period processing ...";
            } else {
                resultOperation = "Period processing already disable !";
            }
        }
    }

    String strCheckSerieEasyChallengeEnable = "";
    try {SerieEasyChallengeMgr.checkSerieEasyChallengeEnable();strCheckSerieEasyChallengeEnable="true";}
    catch (FBWSException e) {
        strCheckSerieEasyChallengeEnable = "false - "+e.getType()+" - "+e.getMessage();
    }
%>
<html>
    <head>
        <title>Funbridge Server - Administration SERIE EASY CHALLENGE</title>
        <script type="text/javascript">
            function clickMgrSetEnableValue(value) {
                if (value) {
                    if (confirm("Enable serie east challenge ?")) {
                        document.forms["formTourSerieEasyChallenge"].operation.value="enableSerieEasyChallenge";
                        document.forms["formTourSerieEasyChallenge"].submit();
                    }
                } else {
                    if (confirm("Disable serie easy challenge ?")) {
                        document.forms["formTourSerieEasyChallenge"].operation.value="disableSerieEasyChallenge";
                        document.forms["formTourSerieEasyChallenge"].submit();
                    }
                }
            }
            function clickMgrSetPeriodProcessingValue(value) {
                if (value) {
                    if (confirm("Enable period processing ?")) {
                        document.forms["formTourSerieEasyChallenge"].operation.value="enablePeriodProcessing";
                        document.forms["formTourSerieEasyChallenge"].submit();
                    }
                } else {
                    if (confirm("Disable period processing ?")) {
                        document.forms["formTourSerieEasyChallenge"].operation.value="disablePeriodProcessing";
                        document.forms["formTourSerieEasyChallenge"].submit();
                    }
                }
            }
            function clickMgrInitPeriod() {
                if (confirm("Init period ?")) {
                    document.forms["formTourSerieEasyChallenge"].operation.value="initPeriod";
                    document.forms["formTourSerieEasyChallenge"].submit();
                }
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION SERIE EASY CHALLENGE</h1>
        [<a href="admin.jsp">Administration</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formTourSerieEasyChallenge" method="post" action="adminSerieEasyChallenge.jsp">
            <input type="hidden" name="operation" value=""/>
            <b>Serie enable</b> = <%=SerieEasyChallengeMgr.isEnable()%> -
            <input type="button" value="<%=SerieEasyChallengeMgr.isEnable()?"Disable SerieEasyChallenge":"Enable SerieEasyChallenge"%>" onclick="clickMgrSetEnableValue(<%=!SerieEasyChallengeMgr.isEnable()%>)"/> - Period processing=<%=SerieEasyChallengeMgr.isPeriodChangeProcessing()%> -
            <input type="button" value="<%=SerieEasyChallengeMgr.isPeriodChangeProcessing()?"Disable Period Processing":"Enable Period Processing"%>" onclick="clickMgrSetPeriodProcessingValue(<%=!SerieEasyChallengeMgr.isPeriodChangeProcessing()%>)"/>
            <br/>
            <b>CheckEasyChallenge</b> = <%=strCheckSerieEasyChallengeEnable%><br>
            <b>Current period</b> = {<%=SerieEasyChallengeMgr.getPeriodEasyChallenge()%>} - Serie Period = <%=SerieEasyChallengeMgr.getPeriodSerie()%>
            <% if (!SerieEasyChallengeMgr.isEnable()){%>&nbsp;&nbsp;&nbsp;<input type="button" value="InitPeriod" onclick="clickMgrInitPeriod()"/><%}%>
            <br/>
            <b>Nb tournament available</b> = <%=SerieEasyChallengeMgr.countTournamentAvailable(0)%> - <b>Nb TournamentPlayer</b> in progress = <%=SerieEasyChallengeMgr.countTournamentPlayerForCurrentPeriod(false)%> - only finished = <%=SerieEasyChallengeMgr.countTournamentPlayerForCurrentPeriod(true)%>
        </form>
    </body>
</html>
