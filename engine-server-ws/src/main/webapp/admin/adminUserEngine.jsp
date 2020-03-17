<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="com.gotogames.bridge.engineserver.common.EngineConfiguration"%>
<%@page import="com.gotogames.bridge.engineserver.request.QueueMgr"%>
<%@page import="com.gotogames.bridge.engineserver.user.UserVirtual"%>
<%@page import="com.gotogames.bridge.engineserver.user.UserVirtualMgr"%>
<%@page import="com.gotogames.bridge.engineserver.ws.compute.ComputeServiceImpl"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@include file="requestFunction.jsp" %>
<%
    ComputeServiceImpl computeService = ContextManager.getComputeService();
    QueueMgr queueMgr = ContextManager.getQueueMgr();
    UserVirtualMgr userMgr = ContextManager.getUserMgr();
    String operation = request.getParameter("operation");
    String paramOperation = "";
    String resultOperation = "";
    String paramVersion = "";
    if (operation != null) {
        if (operation.equals("removeAllUser")) {
            resultOperation = "Nb user removed = "+userMgr.removeAllUserEngine();
        }
        else if (operation.equals("removeUser")) {
            paramOperation = request.getParameter("selection");
            UserVirtual u = userMgr.getUserVirtual(paramOperation);
            if (u != null) {
                userMgr.removeUser(u, true);
                resultOperation = "Success to remove user=" + u;
            } else {
                resultOperation = "No user found for login=" + paramOperation;
            }
        }
        else if (operation.equals("findEngineWS")) {
            paramVersion = request.getParameter("findEngineVersion");
            if (paramVersion == null || paramVersion.length() == 0) {
                resultOperation = "No engine version defined !";
            } else {
                resultOperation = "Method used for find engineWS = " + EngineConfiguration.getInstance().getStringValue("user.methodFindUserEngineWS", "method1") + "<br/>";
                resultOperation += "Engine selected = " + userMgr.findUserEngineWS(Integer.parseInt(paramVersion));
            }
        }
        else if (operation.equals("commandStop")) {
            paramOperation = request.getParameter("selection");
            UserVirtual u = userMgr.getUserVirtual(paramOperation);
            if (u != null && u instanceof UserVirtualEngine) {
                UserVirtualEngine ue = (UserVirtualEngine)u;
                if (ue.getWebSocket() != null) {
                    resultOperation = "Command STOP result="+ue.getWebSocket().sendCommandStop();
                } else {
                    resultOperation = "Failed - no websocket for engine="+ue;
                }
            } else {
                resultOperation = "No user found for login=" + paramOperation;
            }
        }
        else if (operation.equals("commandReboot")) {
            paramOperation = request.getParameter("selection");
            UserVirtual u = userMgr.getUserVirtual(paramOperation);
            if (u != null && u instanceof UserVirtualEngine) {
                UserVirtualEngine ue = (UserVirtualEngine)u;
                if (ue.getWebSocket() != null) {
                    resultOperation = "Command REBOOT result="+ue.getWebSocket().sendCommandReboot();
                } else {
                    resultOperation = "Failed - no websocket for engine="+ue;
                }
            } else {
                resultOperation = "No user found for login=" + paramOperation;
            }
        }
        else if (operation.equals("commandRestart")) {
            paramOperation = request.getParameter("selection");
            UserVirtual u = userMgr.getUserVirtual(paramOperation);
            if (u != null && u instanceof UserVirtualEngine) {
                UserVirtualEngine ue = (UserVirtualEngine)u;
                if (ue.getWebSocket() != null) {
                    resultOperation = "Command RESTART result="+ue.getWebSocket().sendCommandRestart();
                } else {
                    resultOperation = "Failed - no websocket for engine="+ue;
                }
            } else {
                resultOperation = "No user found for login=" + paramOperation;
            }
        }
        else if (operation.equals("commandUpdateFbMoteur")) {
            paramOperation = request.getParameter("selection");
            UserVirtual u = userMgr.getUserVirtual(paramOperation);
            if (u != null && u instanceof UserVirtualEngine) {
                UserVirtualEngine ue = (UserVirtualEngine)u;
                if (ue.getWebSocket() != null) {
                    resultOperation = "Command UPDATE result="+ue.getWebSocket().sendCommandUpdate();
                } else {
                    resultOperation = "Failed - no websocket for engine="+ue;
                }
            } else {
                resultOperation = "No user found for login=" + paramOperation;
            }
        }
        else if (operation.equals("commandUpdateDLL")) {
            paramOperation = request.getParameter("selection");
            UserVirtual u = userMgr.getUserVirtual(paramOperation);
            int configEngineDLLVersion = userMgr.getConfigEngineDLLVersion();
            if (u != null && u instanceof UserVirtualEngine) {
                UserVirtualEngine ue = (UserVirtualEngine)u;
                if (ue.getWebSocket() != null) {
//                    QueueData dataUpdate = new QueueData();
//                    String rqt = Constantes.REQUEST_DEAL_BIDINFO+Constantes.REQUEST_FIELD_SEPARATOR+
//                            Constantes.buildOptionsForEngine(0, 251, 0, 0, 0)+Constantes.REQUEST_FIELD_SEPARATOR+
//                            ""+Constantes.REQUEST_FIELD_SEPARATOR+
//                            ""+Constantes.REQUEST_FIELD_SEPARATOR+
//                            100+Constantes.REQUEST_FIELD_SEPARATOR+
//                            0+Constantes.REQUEST_FIELD_SEPARATOR+"?";
//                    dataUpdate.setRequest(rqt);
                    if (!ue.containsEngineVersion(configEngineDLLVersion)) {
                        resultOperation = "Command UPDATE DLL => version=" + configEngineDLLVersion + " - result=" + ue.getWebSocket().sendCommandUpdateDLL(configEngineDLLVersion);
                    } else {
                        resultOperation = "Command UPDATE DLL => version=" + configEngineDLLVersion + " - no need to update DLL => engine already include this DLL !";
                    }
                } else {
                    resultOperation = "Failed - no websocket for engine="+ue;
                }
            } else {
                resultOperation = "No user found for login=" + paramOperation;
            }
        }
        else if (operation.equals("commandEnable")) {
            paramOperation = request.getParameter("selection");
            UserVirtual u = userMgr.getUserVirtual(paramOperation);
            if (u != null && u instanceof UserVirtualEngine) {
                UserVirtualEngine ue = (UserVirtualEngine)u;
                ue.setEnable(true);
                resultOperation = "Enable user = "+ue;
            } else {
                resultOperation = "No user found for login=" + paramOperation;
            }
        }
        else if (operation.equals("commandDisable")) {
            paramOperation = request.getParameter("selection");
            UserVirtual u = userMgr.getUserVirtual(paramOperation);
            if (u != null && u instanceof UserVirtualEngine) {
                UserVirtualEngine ue = (UserVirtualEngine)u;
                ue.setEnable(false);
                resultOperation = "Enable user = "+ue;
            } else {
                resultOperation = "No user found for login=" + paramOperation;
            }
        }
        else if (operation.equals("listEngineToRestart")) {
            List<UserVirtualEngine> listEngineToRestart = userMgr.listUserEngineWSToRestart(0);
            resultOperation = "List engine to restart : <br/>";
            for (UserVirtualEngine e : listEngineToRestart) {
                resultOperation += e+"<br/>";
            }
        }
        else if (operation.equals("listEngineNoResult")) {
            List<UserVirtualEngine> listEngineNoResult = userMgr.listUserEngineWSWithNoResult();
            resultOperation = "List engine with no result : <br/>";
            for (UserVirtualEngine e : listEngineNoResult) {
                resultOperation += e+"<br/>";
            }
        }
        else if (operation.equals("listEngineToUpdateDLL")) {
            List<UserVirtualEngine> listEngineToUpdateDLL = userMgr.listUserEngineWSToUpdateDLL(0);
            resultOperation = "List engine to update DLL : <br/>";
            for (UserVirtualEngine e : listEngineToUpdateDLL) {
                resultOperation += e+"<br/>";
            }
        }
        else if (operation.equals("listEngineToUpdateFbMoteur")) {
            List<UserVirtualEngine> listEngineToUpdateFbMoteur = userMgr.listUserEngineWSToUpdateFbMoteur(0);
            resultOperation = "List engine to update FbMoteur : <br/>";
            for (UserVirtualEngine e : listEngineToUpdateFbMoteur) {
                resultOperation += e+"<br/>";
            }
        }
        else if (operation.equals("restartToListEngineNoResult")) {
            List<UserVirtualEngine> listEngineNoResult = userMgr.listUserEngineWSWithNoResult();
            resultOperation = "List engine noResult : <br/>";
            int nbOK = 0;
            int nbMax = 10;
            for (UserVirtualEngine e : listEngineNoResult) {
                resultOperation += e;
                if (nbOK < nbMax) {
                    if (e.getWebSocket() != null && e.getWebSocket().sendCommandRestart()) {
                        resultOperation += " - <b>Restart : OK</b><br/>";
                        nbOK++;
                    } else {
                        resultOperation += " - <b>Restart : Failed</b><br/>";
                    }
                } else {
                    resultOperation += " - restart not send ... to many engine !!";
                }
            }
            resultOperation += "Nb engine noResult ="+listEngineNoResult.size()+" - Nb restart OK="+nbOK;
        }
        else if (operation.equals("resetRequestInProgressToListEngineNoResult")) {
            List<UserVirtualEngine> listEngineNoResult = userMgr.listUserEngineWSWithNoResult();
            resultOperation = "List engine noResult : <br/>";
            for (UserVirtualEngine e : listEngineNoResult) {
                e.setNbRequestsInProgress(0);
                resultOperation += e;
            }
        }
        else if (operation.equals("resetNbRequestInProgress")) {
            paramOperation = request.getParameter("selection");
            UserVirtual u = userMgr.getUserVirtual(paramOperation);
            if (u != null && u instanceof UserVirtualEngine) {
                UserVirtualEngine ue = (UserVirtualEngine)u;
                ue.setNbRequestsInProgress(0);
                resultOperation = "Reset nb request in progress for user = "+ue;
            } else {
                resultOperation = "No user found for login=" + paramOperation;
            }
        }
        else {
            resultOperation = "operation unknown ! ("+operation+")";
        }
    }
    List<UserVirtual> listUser = new ArrayList<UserVirtual>(userMgr.getListUser());
//    Collections.sort(listUser, Comparator.comparing(UserVirtual::getLoginPrefix));
    Collections.sort(listUser, new Comparator<UserVirtual>() {
        @Override
        public int compare(UserVirtual o1, UserVirtual o2) {
            return o1.getLogin().compareTo(o2.getLogin());
        }
    });
    int nbFbMoteur = 0;
    for (UserVirtual u : listUser) {
        if (u.isEngine()) {
            nbFbMoteur++;
        }
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    String COLOR_RED = "#FF0000", COLOR_ORANGE="#FFA500", COLOR_GREEN="#00FF00";
%>

<%@ page import="com.gotogames.bridge.engineserver.user.UserVirtualEngine" %>
<%@ page import="com.codahale.metrics.Meter" %>
<%@ page import="com.gotogames.common.tools.NumericalTools" %>
<%@ page import="com.gotogames.bridge.engineserver.common.Constantes" %>
<%@ page import="java.util.*" %>
<html>
<head>
    <title>EngineServer - Administration User</title>
    <script type="text/javascript">
        function clickRemoveAllUser() {
            if (confirm("Close all session ?")) {
                document.forms["formUser"].operation.value = "removeAllUser";
                document.forms["formUser"].submit();
            }
        }
        function clickRemoveUser(login) {
            if (confirm("Close session for user=" + login + " ?")) {
                document.forms["formUser"].operation.value = "removeUser";
                document.forms["formUser"].selection.value = login;
                document.forms["formUser"].submit();
            }
        }
        function clickCommandStop(login) {
            if (confirm("Send command STOP for user=" + login + " ?")) {
                document.forms["formUser"].operation.value = "commandStop";
                document.forms["formUser"].selection.value = login;
                document.forms["formUser"].submit();
            }
        }
        function clickCommandReboot(login) {
            if (confirm("Send command STOP for user=" + login + " ?")) {
                document.forms["formUser"].operation.value = "commandReboot";
                document.forms["formUser"].selection.value = login;
                document.forms["formUser"].submit();
            }
        }
        function clickCommandRestart(login) {
            if (confirm("Send command RESTART for user=" + login + " ?")) {
                document.forms["formUser"].operation.value = "commandRestart";
                document.forms["formUser"].selection.value = login;
                document.forms["formUser"].submit();
            }
        }
        function clickResetNbRequestInProgress(login) {
            if (confirm("Reset nb request in progress for user=" + login + " ?")) {
                document.forms["formUser"].operation.value = "resetNbRequestInProgress";
                document.forms["formUser"].selection.value = login;
                document.forms["formUser"].submit();
            }
        }
        function clickCommandUpdateFbMoteur(login) {
            if (confirm("Send command UPDATE for user=" + login + " ?")) {
                document.forms["formUser"].operation.value = "commandUpdateFbMoteur";
                document.forms["formUser"].selection.value = login;
                document.forms["formUser"].submit();
            }
        }
        function clickCommandUpdateDLL(login) {
            if (confirm("Send command UPDATE DLL for user=" + login + " ?")) {
                document.forms["formUser"].operation.value = "commandUpdateDLL";
                document.forms["formUser"].selection.value = login;
                document.forms["formUser"].submit();
            }
        }
        function clickEnableUser(login, enable) {
            if (enable) {
                if (confirm("Enable user=" + login + " ?")) {
                    document.forms["formUser"].operation.value = "commandEnable";
                    document.forms["formUser"].selection.value = login;
                    document.forms["formUser"].submit();
                }
            } else {
                if (confirm("Disable user=" + login + " ?")) {
                    document.forms["formUser"].operation.value = "commandDisable";
                    document.forms["formUser"].selection.value = login;
                    document.forms["formUser"].submit();
                }
            }
        }
        function clickListEngineToRestart() {
            document.forms["formUser"].operation.value = "listEngineToRestart";
            document.forms["formUser"].submit();
        }
        function clickListEngineNoResult() {
            document.forms["formUser"].operation.value = "listEngineNoResult";
            document.forms["formUser"].submit();
        }
        function clickRestartListEngineNoResult() {
            document.forms["formUser"].operation.value = "restartToListEngineNoResult";
            document.forms["formUser"].submit();
        }
        function clickResetRequestInProgressListEngineNoResult() {
            document.forms["formUser"].operation.value = "resetRequestInProgressToListEngineNoResult";
            document.forms["formUser"].submit();
        }
        function clickListEngineToUpdateDLL() {
            document.forms["formUser"].operation.value = "listEngineToUpdateDLL";
            document.forms["formUser"].submit();
        }
        function clickListEngineToUpdateFbMoteur() {
            document.forms["formUser"].operation.value = "listEngineToUpdateFbMoteur";
            document.forms["formUser"].submit();
        }

        function clickFindEngineWS() {
            document.forms["formUser"].operation.value = "findEngineWS";
            document.forms["formUser"].submit();
        }

    </script>
</head>
<body>
<h1>ADMINISTRATION USER</h1>
<a href="admin.jsp">Administration</a><br/><br/>
<hr width="90%"/>
<a href="adminUserEngine.jsp">Refresh</a><br/>
<%if (operation != null){ %>
<b>Operation executed : <%=operation %> - parametres=<%=paramOperation %></b><br/>
Operation result : <%=resultOperation %>
<hr width="90%"/>
<%} %>
<br/>
<form name="formUser" action="adminUserEngine.jsp" method="post">
    <input type="hidden" name="operation" value=""/>
    <input type="hidden" name="selection" value=""/>
    Engine version : <input type="text" name="findEngineVersion" size="6" value="<%=paramVersion%>">&nbsp;&nbsp;&nbsp;<input type="button" value="Find engine WS" onclick="clickFindEngineWS()"/><br/>
    Nb result from FbMoteurs = <%=computeService.getNbResultFbMoteur() %><br/>
    Nb result from Others = <%=computeService.getNbResultFbOthers() %><br/>
    Time to Get Query to compute = <%=queueMgr.getAverageTimeGet() %><br/>
    Time to Get Query empty = <%=queueMgr.getAverageTimeGetEmpty() %><br/>
    Option request only for FbMoteur = <%=EngineConfiguration.getInstance().getIntValue("request.onlyFbMoteur", 0) %><br/>
    <br/>
    Engine DLL current version = <%=userMgr.getConfigEngineDLLVersion()%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
    FbMoteur current version = <%=userMgr.getConfigEngineFbMoteurVersion()%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
    URL Download FbMoteur = <%=EngineConfiguration.getInstance().getStringValue("user.updateDownloadURL", null)%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
    URL Download Argine DLL = <%=EngineConfiguration.getInstance().getStringValue("user.updateArgineDownloadURL", null)%><br/>
    Current date=<%=sdf.format(new Date()) %><br/>
    Date of next JobRestartUpdateEngineWS : <%=Constantes.timestamp2StringDateHour(userMgr.getDateNextJobRestartUpdateEngineWS())%>&nbsp;&nbsp;&nbsp;Restart enable for current hour=<%=userMgr.isRestartEngineWSEnableForCurrentHour()%>&nbsp;&nbsp;&nbsp;<input type="button" value="List engine need to restart" onclick="clickListEngineToRestart()">&nbsp;&nbsp;&nbsp;<input type="button" value="List engine to updateDLL" onclick="clickListEngineToUpdateDLL()">&nbsp;&nbsp;&nbsp;<input type="button" value="List engine to update FbMoteur" onclick="clickListEngineToUpdateFbMoteur()"><br/>
    Date of next JobCheckEngineNoResult : <%=Constantes.timestamp2StringDateHour(userMgr.getDateNextJobCheckEngineNoResult())%>&nbsp;&nbsp;&nbsp;<input type="button" value="List engine with no result" onclick="clickListEngineNoResult()">&nbsp;&nbsp;&nbsp;<input type="button" value="Restart List engine with no result" onclick="clickRestartListEngineNoResult()">&nbsp;&nbsp;&nbsp;<input type="button" value="Reset request in progress for List engine with no result" onclick="clickResetRequestInProgressListEngineNoResult()"><br/>
    <hr width="90%"/>
    <b>List of Users Connected : </b><br/>
    Nb Engine = <%=nbFbMoteur %><br/>
    <input type="button" value="Remove All user" onclick="clickRemoveAllUser()"/>
    <br/><br/>
    <%if (listUser != null){%>
    <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
        <tr><th>ID</th><th>User info</th><th>Stat</th><th>Queue</th><th>Thread</th><th>IN</th><th>Info</th><th>Operation</th></tr>
        <% for (UserVirtual u : listUser) {
            if (!u.isEngine()) {
                continue;
            }
            UserVirtualEngine userEngine = ((UserVirtualEngine)u);
            String stat = "";
            boolean enable = true;
            enable = userEngine.isEnable();
            Meter meter = userEngine.getMetricsRequest();
            if (meter != null) {
                stat = "REQ="+ NumericalTools.round(meter.getOneMinuteRate(),2)+" - ";
            }
            stat += "Q="+userEngine.getQueueSize()+" - ";
            stat += "T="+userEngine.getNbThread()+" ("+userEngine.getAvailableThreadPercent()+"%) - ";
            stat += "ms="+userEngine.getComputeTime()+" - ";
//            if (userEngine.getEngineStat() != null) {
//                stat += "ms=" + userEngine.getEngineStat().averageTimeRequest + " - ";
//            }
            stat += "NB="+userEngine.getNbCompute()+" - ";
            stat += "IN="+userEngine.getNbRequestsInProgress()+" - ";
            stat += "INDEX="+NumericalTools.round(userEngine.computePerformanceIndex(), 6);
        %>
        <tr>
            <td><%=u.getID() %></td>
            <td><%=u.toString()%></td>
            <td><%=stat%></td>
            <td bgcolor="<%=userEngine.isEngineWebSocketReady()?(userEngine.getQueueSize()>0?COLOR_ORANGE:COLOR_GREEN):""%>"><%=userEngine.getQueueSize()%></td>
            <td bgcolor="<%=userEngine.isEngineWebSocketReady()?(userEngine.getAvailableThreadPercent()<50?COLOR_ORANGE:COLOR_GREEN):""%>"><%=userEngine.getAvailableThreadPercent()%>%</td>
            <td bgcolor="<%=userEngine.isEngineWebSocketReady()?(userEngine.getNbRequestsInProgress()>(userEngine.getNbThread()/2)?COLOR_ORANGE:COLOR_GREEN):""%>"><%=userEngine.getNbRequestsInProgress()%></td>
            <td>
                <%=sdf.format(new Date(u.getDateLastActivity()))+((u instanceof UserVirtualEngine)?" - "+sdf.format(new Date(((UserVirtualEngine) u).getDateLastResult()))+"<br/>NbMinutesSinceCreation="+((UserVirtualEngine)u).getNbMinutesSinceCreation()+" - NbMinutesBeforeRestart="+((UserVirtualEngine)u).computeNbMinutesBeforeRestart()+" - needToRestart="+((UserVirtualEngine)u).needToRestart():"") %>
            </td>
            <td>
                <input type="button" value="Remove user" onclick="clickRemoveUser('<%=u.getLoginID() %>')"/>&nbsp;&nbsp;&nbsp;
                <input type="button" value="<%=enable?"Disable user":"Enable user"%>" onclick="clickEnableUser('<%=u.getLoginID() %>', <%=!enable%>)"/>&nbsp;&nbsp;&nbsp;
                <input type="button" value="Command Stop" onclick="clickCommandStop('<%=u.getLoginID() %>')"/>&nbsp;&nbsp;&nbsp;
                <input type="button" value="Command Restart" onclick="clickCommandRestart('<%=u.getLoginID() %>')"/>&nbsp;&nbsp;&nbsp;
                <input type="button" value="Reset Nb Request In Progress" onclick="clickResetNbRequestInProgress('<%=u.getLoginID() %>')"/>&nbsp;&nbsp;&nbsp;
                <input type="button" value="Command Reboot" onclick="clickCommandReboot('<%=u.getLoginID() %>')"/>&nbsp;&nbsp;&nbsp;
                <input type="button" value="Command Update FbMoteur" onclick="clickCommandUpdateFbMoteur('<%=u.getLoginID() %>')"/>&nbsp;&nbsp;&nbsp;
                <input type="button" value="Command Update DLL" onclick="clickCommandUpdateDLL('<%=u.getLoginID() %>')"/>
            </td>
        </tr>
        <%}%>
    </table>
    <%} else {%>
    LIST USER NULL !!
    <%} %>
</form>
</body>
</html>