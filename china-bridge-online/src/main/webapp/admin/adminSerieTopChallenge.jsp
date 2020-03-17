<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.tournament.serie.SerieTopChallengeMgr" %>
<%@ page import="com.funbridge.server.ws.FBWSException" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    SerieTopChallengeMgr serieTopChallengeMgr = ContextManager.getSerieTopChallengeMgr();

    if (operation != null) {
        if (operation.equals("initPeriod")) {
            serieTopChallengeMgr.initPeriod();
            resultOperation = "Success to init period";
        }
        else if (operation.equals("enableSerieTopChallenge")) {
            if (!serieTopChallengeMgr.isEnable()) {
                serieTopChallengeMgr.setEnable(true);
                resultOperation = "Success to enable SerieTopChallenge ...";
            } else {
                resultOperation = "SerieTopChallenge already enable !";
            }
        }
        else if (operation.equals("disableSerieTopChallenge")) {
            if (serieTopChallengeMgr.isEnable()) {
                serieTopChallengeMgr.setEnable(false);
                resultOperation = "Success to disable SerieTopChallenge ...";
            } else {
                resultOperation = "SerieTopChallenge already disable !";
            }
        }
        else if (operation.equals("enablePeriodProcessing")) {
            if (!serieTopChallengeMgr.isPeriodChangeProcessing()) {
                serieTopChallengeMgr.setPeriodChangeProcessing(true);
                resultOperation = "Success to enable period processing ...";
            } else {
                resultOperation = "Period processing already enable !";
            }
        }
        else if (operation.equals("disablePeriodProcessing")) {
            if (serieTopChallengeMgr.isPeriodChangeProcessing()) {
                serieTopChallengeMgr.setPeriodChangeProcessing(false);
                resultOperation = "Success to disable period processing ...";
            } else {
                resultOperation = "Period processing already disable !";
            }
        }
    }

    String strCheckSerieTopChallengeEnable = "";
    try {serieTopChallengeMgr.checkSerieTopChallengeEnable();strCheckSerieTopChallengeEnable="true";}
    catch (FBWSException e) {
        strCheckSerieTopChallengeEnable = "false - "+e.getType()+" - "+e.getMessage();
    }
%>
<html>
    <head>
        <title>Funbridge Server - Administration SERIE TOP CHALLENGE</title>
        <script type="text/javascript">
            function clickMgrSetEnableValue(value) {
                if (value) {
                    if (confirm("Enable serie top challenge ?")) {
                        document.forms["formTourSerieTopChallenge"].operation.value="enableSerieTopChallenge";
                        document.forms["formTourSerieTopChallenge"].submit();
                    }
                } else {
                    if (confirm("Disable serie top challenge ?")) {
                        document.forms["formTourSerieTopChallenge"].operation.value="disableSerieTopChallenge";
                        document.forms["formTourSerieTopChallenge"].submit();
                    }
                }
            }
            function clickMgrSetPeriodProcessingValue(value) {
                if (value) {
                    if (confirm("Enable period processing ?")) {
                        document.forms["formTourSerieTopChallenge"].operation.value="enablePeriodProcessing";
                        document.forms["formTourSerieTopChallenge"].submit();
                    }
                } else {
                    if (confirm("Disable period processing ?")) {
                        document.forms["formTourSerieTopChallenge"].operation.value="disablePeriodProcessing";
                        document.forms["formTourSerieTopChallenge"].submit();
                    }
                }
            }
            function clickMgrInitPeriod() {
                if (confirm("Init period ?")) {
                    document.forms["formTourSerieTopChallenge"].operation.value="initPeriod";
                    document.forms["formTourSerieTopChallenge"].submit();
                }
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION SERIE TOP CHALLENGE</h1>
        [<a href="admin.jsp">Administration</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formTourSerieTopChallenge" method="post" action="adminSerieTopChallenge.jsp">
            <input type="hidden" name="operation" value=""/>
            <b>Serie enable</b> = <%=serieTopChallengeMgr.isEnable()%> -
            <input type="button" value="<%=serieTopChallengeMgr.isEnable()?"Disable SerieTopChallenge":"Enable SerieTopChallenge"%>" onclick="clickMgrSetEnableValue(<%=!serieTopChallengeMgr.isEnable()%>)"/> - Period processing=<%=serieTopChallengeMgr.isPeriodChangeProcessing()%> -
            <input type="button" value="<%=serieTopChallengeMgr.isPeriodChangeProcessing()?"Disable Period Processing":"Enable Period Processing"%>" onclick="clickMgrSetPeriodProcessingValue(<%=!serieTopChallengeMgr.isPeriodChangeProcessing()%>)"/>
            <br/>
            <b>CheckTopChallenge</b> = <%=strCheckSerieTopChallengeEnable%><br>
            <b>Current period</b> = {<%=serieTopChallengeMgr.getPeriodTopChallenge()%>} - Serie Period = <%=serieTopChallengeMgr.getPeriodSerie()%>
            <% if (!serieTopChallengeMgr.isEnable()){%>&nbsp;&nbsp;&nbsp;<input type="button" value="InitPeriod" onclick="clickMgrInitPeriod()"/><%}%>
            <br/>
            <b>Nb tournament available</b> = <%=serieTopChallengeMgr.countTournamentAvailable()%> - <b>Nb TournamentPlayer</b> in progress = <%=serieTopChallengeMgr.countTournamentPlayerForCurrentPeriod(false)%> - only finished = <%=serieTopChallengeMgr.countTournamentPlayerForCurrentPeriod(true)%>
        </form>
    </body>
</html>
