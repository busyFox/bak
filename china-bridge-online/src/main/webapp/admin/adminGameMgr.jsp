<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.common.FBConfiguration" %>
<%@ page import="com.funbridge.server.tournament.data.SpreadGameData" %>
<%@ page import="com.funbridge.server.tournament.game.GameMgr" %>
<%@ page import="com.funbridge.server.tournament.game.GameThread" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String paramOperation = request.getParameter("operation");
    String resultOperation = "";
    String categoryName = request.getParameter("categoryNameOperation");

    List<GameMgr> listGameMgr = ContextManager.getListGameMgr();


    Map<String, GameThread> mapThreadRunning = null;
    Map<String, SpreadGameData> mapSpreadWaiting = null;
    if (paramOperation != null && paramOperation.length() > 0) {
        if (paramOperation.equals("purgeSpreadPlayWaiting")) {
            for (GameMgr g : listGameMgr) {
                if (g.getTournamentMgr().getTournamentCategoryName().equals(categoryName)) {
                    resultOperation = "Nb purge="+g.purgeSpreadPlayExpired();
                    break;
                }
            }
        }
        else if (paramOperation.equals("listAllThreadRunning")) {
            for (GameMgr g : listGameMgr) {
                if (g.getTournamentMgr().getTournamentCategoryName().equals(categoryName)) {
                    mapThreadRunning = g.getMapThreadRunning();
                    break;
                }
            }
            if (mapThreadRunning == null) {
                resultOperation = "Failed to find map thread running for categoryName="+ categoryName;
            }
        }
        else if (paramOperation.equals("listAllSpreadWaiting")) {
            for (GameMgr g : listGameMgr) {
                if (g.getTournamentMgr().getTournamentCategoryName().equals(categoryName)) {
                    mapSpreadWaiting = g.getMapSpreadWaiting();
                    break;
                }
            }
            if (mapSpreadWaiting == null) {
                resultOperation = "Failed to find map spread waiting for categoryName="+ categoryName;
            }
        }
        else if (paramOperation.equals("removeSpreadPlayWaiting")) {
            String gameID = request.getParameter("spreadPlayWaitingGameID");
            if (gameID != null && gameID.length() > 0) {
                for (GameMgr g : listGameMgr) {
                    if (g.getTournamentMgr().getTournamentCategoryName().equals(categoryName)) {
                        g.removeSpreadPlay(gameID);
                        break;
                    }
                }
            } else {
                resultOperation = "Parameter spreadPlayWaitingGameID not found";
            }
        }
        else if (paramOperation.equals("removeThreadRunning")) {
            String gameID = request.getParameter("threadRunningGameID");
            if (gameID != null && gameID.length() > 0) {
                for (GameMgr g : listGameMgr) {
                    if (g.getTournamentMgr().getTournamentCategoryName().equals(categoryName)) {
                        g.removeThreadPlayRunning(gameID, null);
                        break;
                    }
                }
            } else {
                resultOperation = "Parameter spreadPlayWaitingGameID not found";
            }
        }
        else if (paramOperation.equals("initPoolConfig")) {
            for (GameMgr g : listGameMgr) {
                if (g.getTournamentMgr().getTournamentCategoryName().equals(categoryName)) {
                    g.getEngine().getWebsocketMgr().initPoolConfig();
                    resultOperation = "Success to init poolConfig websocket for category="+ categoryName;
                    break;
                }
            }
        }
        else if (paramOperation.equals("initAllPoolConfig")) {
            for (GameMgr g : listGameMgr) {
                g.getEngine().getWebsocketMgr().initPoolConfig();
                resultOperation += "<br>Success to init poolConfig websocket for category="+ g.getTournamentMgr().getTournamentCategoryName();
            }
        }
        else if (paramOperation.equals("startTestPool")) {
            for (GameMgr g : listGameMgr) {
                if (g.getTournamentMgr().getTournamentCategoryName().equals(categoryName)) {
                    g.getEngine().getWebsocketMgr().startTest();
                    resultOperation = "Success to start TEST on pool websocket for category="+ categoryName;
                    break;
                }
            }
        }
        else if (paramOperation.equals("stopTestPool")) {
            for (GameMgr g : listGameMgr) {
                if (g.getTournamentMgr().getTournamentCategoryName().equals(categoryName)) {
                    g.getEngine().getWebsocketMgr().stopTest();
                    resultOperation = "Success to stop TEST on pool websocket for category="+ categoryName;
                    break;
                }
            }
        }
        else if (paramOperation.equals("enableEngineServer") || paramOperation.equals("disableEngineServer")) {
            for (GameMgr g : listGameMgr) {
                if (categoryName.equals("ALL") || categoryName.equals(g.getTournamentMgr().getTournamentCategoryName())) {
                    g.getEngine().setEngineServerEnable(paramOperation.equals("enableEngineServer"));
                    resultOperation += "Success to "+paramOperation+" for category="+ g.getTournamentMgr().getTournamentCategoryName()+"<br>";
                }
            }
        }
        else if (paramOperation.equals("resetPool")) {
            for (GameMgr g : listGameMgr) {
                if (categoryName.equals("ALL") || categoryName.equals(g.getTournamentMgr().getTournamentCategoryName())) {
                    if (g.getEngine().getWebsocketMgr() != null) {
                        g.getEngine().getWebsocketMgr().initPool();
                        resultOperation += "Success to resetPool for category=" + g.getTournamentMgr().getTournamentCategoryName() + "<br/>";
                    } else {
                        resultOperation += "Failed to resetPool for category="+g.getTournamentMgr().getTournamentCategoryName()+" - websocketMgr is null<br/>";
                    }
                }
            }
        }
    }


    boolean serverEngineEnable = FBConfiguration.getInstance().getConfigBooleanValue("engine.server.enable", true);
%>
<html>
    <head>
        <title>Funbridge Server - Administration Player</title>
        <script type="text/javascript">
            function clickOperation(needConfirm, operationName, categoryName) {
                var submitOperation = false;
                if (needConfirm) {
                    if (confirm("Run operation "+operationName+" for category "+categoryName+" ?")) {
                        submitOperation = true;
                    }
                } else {
                    submitOperation = true;
                }
                if (submitOperation) {
                    document.forms["formGameMgr"].operation.value = operationName;
                    document.forms["formGameMgr"].categoryNameOperation.value = categoryName;
                    document.forms["formGameMgr"].submit();
                }
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION GAME MGR</h1>
        <a href="admin.jsp">Administration</a><br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= paramOperation%></b>
        <br/>Result = <%=resultOperation %>
        <%} %>
        <form name="formGameMgr" method="post" action="adminGameMgr.jsp">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="categoryNameOperation" value="<%=categoryName%>"/>
            <br/>
            <%if (mapThreadRunning != null) {
                String textArea = "";
                for (Map.Entry<String, GameThread> e : mapThreadRunning.entrySet()) {
                    textArea += e.getKey()+" - "+ Constantes.timestamp2StringDateHour(e.getValue().getDateLastThreadPlayRunning());
                    textArea += "\r\n";
                }
            %>
            Map ThreadRunning size=<%=mapThreadRunning.size()%><br>
            <textarea readonly="readonly" cols="50" rows="20" wrap="off"><%=textArea%></textarea><br/>
            Remove threadRunning for gameID : <input type="text" name="threadRunningGameID" size="30">&nbsp;&nbsp;&nbsp;<input type="button" value="Remove ThreadRunning" onclick="clickOperation(true, 'removeThreadRunning', '<%=categoryName%>')"><br/>
            <%}%>
            <%if (mapSpreadWaiting != null) {
                String textArea = "";
                for (Map.Entry<String, SpreadGameData> e : mapSpreadWaiting.entrySet()) {
                    textArea += e.getKey()+" - "+ Constantes.timestamp2StringDateHour(e.getValue().getDateLastAddSpreadPlay());
                    textArea += "\r\n";
                }
            %>
            Map SpreadPlayWaiting size=<%=mapSpreadWaiting.size()%><br>
            <textarea readonly="readonly" cols="50" rows="20" wrap="off"><%=textArea%></textarea><br/>
            Remove spreadPlayWaiting for gameID : <input type="text" name="spreadPlayWaitingGameID" size="30">&nbsp;&nbsp;&nbsp;<input type="button" value="Remove SpreadPlayWaiting" onclick="clickOperation(true, 'removeSpreadPlayWaiting', '<%=categoryName%>')"><br/>
            <%}%>
            <br/>
            Engine server enable (configuration) : <%=serverEngineEnable%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" value="Disable Engine Server for ALL" onclick="clickOperation(true, 'disableEngineServer', 'ALL')">&nbsp;
            <input type="button" value="Enable Engine Server for ALL" onclick="clickOperation(true, 'enableEngineServer', 'ALL')"><br/>
            Init poolConfig for all mode <input type="button" value="Init all poolConfig" onclick="clickOperation(true, 'initAllPoolConfig', 'ALL')">&nbsp;&nbsp;&nbsp;
            Reset pool for all mode <input type="button" value="Reset all pool" onclick="clickOperation(true, 'resetPool', 'ALL')">&nbsp;&nbsp;&nbsp;<br/>
            <br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Mode</th><th>Nb players Running</th><th>SynchroMode</th><th>Websocket</th><th>ThreadPool</th><th>MapThreadRunning</th><th>MapSpreadWaiting</th></tr>
                <% for (GameMgr g : listGameMgr) {%>
                <tr>
                    <td <%=!g.getEngine().isEngineServerEnable()?"bgcolor=\"#FF0000\"":""%>>
                        <%=g.getTournamentMgr().getTournamentCategoryName()%>&nbsp;<input type="button" value="<%=g.getEngine().isEngineServerEnable()?"Disable Engine":"Enable Engine"%>" onclick="clickOperation(true, '<%=g.getEngine().isEngineServerEnable()?"disableEngineServer":"enableEngineServer"%>', '<%=g.getTournamentMgr().getTournamentCategoryName()%>')">
                    </td>
                    <td><%=g.getPlayerRunningSize()%></td>
                    <td><%=g.isPlaySynchroMethod()?"SYNC":"ASYNC"%></td>
                    <td>
                        <%=g.isEngineWebSocketEnable()%> - Mgr : [<%=g.getEngine().getWebsocketMgr().toString()%>]&nbsp;&nbsp;&nbsp;
                        <input type="button" value="Init PoolConfig" onclick="clickOperation(true, 'initPoolConfig', '<%=g.getTournamentMgr().getTournamentCategoryName()%>')">&nbsp;
                        <input type="button" value="Reset Pool" onclick="clickOperation(true, 'resetPool', '<%=g.getTournamentMgr().getTournamentCategoryName()%>')">
                        <% if (g.getEngine().getWebsocketMgr().isDevMode()) {%>
                        <br/>
                        Test running=<%=g.getEngine().getWebsocketMgr().testRun%>&nbsp;&nbsp;&nbsp;Test Nb Thread=<%=g.getEngine().getWebsocketMgr().nbThreadTest%>&nbsp;&nbsp;&nbsp;<input type="button" value="Start Thread Test Pool" onclick="clickOperation(true, 'startTestPool', '<%=g.getTournamentMgr().getTournamentCategoryName()%>')">&nbsp;&nbsp;&nbsp;
                        <input type="button" value="Stop Test Pool" onclick="clickOperation(true, 'stopTestPool', '<%=g.getTournamentMgr().getTournamentCategoryName()%>')">
                        <%}%>
                    </td>
                    <td><%=g.getThreadPoolDataPlay()%></td>
                    <td>
                        size : <%=g.getMapThreadRunning().size()%>&nbsp;&nbsp;&nbsp;
                        <input type="button" value="List all values" onclick="clickOperation(false,'listAllThreadRunning','<%=g.getTournamentMgr().getTournamentCategoryName()%>')">
                    </td>
                    <td>
                        size : <%=g.getMapSpreadWaiting().size()%>&nbsp;&nbsp;&nbsp;
                        Next purge : <%=g.getStringDateNextSpreadPlayWaitingPurgeTask()%>&nbsp;&nbsp;&nbsp;
                        <input type="button" value="Purge SpreadPlayWaiting" onclick="clickOperation(true,'purgeSpreadPlayWaiting','<%=g.getTournamentMgr().getTournamentCategoryName()%>')">
                        <input type="button" value="List all values" onclick="clickOperation(false,'listAllSpreadWaiting','<%=g.getTournamentMgr().getTournamentCategoryName()%>')">
                    </td>
                </tr>
                <%}%>
            </table>
        </form>
    </body>
</html>
