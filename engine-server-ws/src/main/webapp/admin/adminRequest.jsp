<%@page import="com.gotogames.bridge.engineserver.common.*"%>
<%@ page import="com.gotogames.bridge.engineserver.request.QueueMgr" %>
<%@ page import="com.gotogames.bridge.engineserver.request.data.QueueData" %>
<%@ page import="com.gotogames.bridge.engineserver.user.UserVirtual" %>
<%@ page import="com.gotogames.bridge.engineserver.user.UserVirtualEngine" %>
<%@ page import="com.gotogames.bridge.engineserver.user.UserVirtualMgr" %>
<%@ page import="com.gotogames.bridge.engineserver.ws.request.RequestService" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="java.util.List" %>
<html>
<%
    QueueMgr queueMgr = ContextManager.getQueueMgr();
    UserVirtualMgr userMgr = ContextManager.getUserMgr();
    String operation = request.getParameter("operation");
    String resultOperation = "";
    List<UserVirtual> listUser = new ArrayList<UserVirtual>(userMgr.getListUser());
    Collections.sort(listUser, new Comparator<UserVirtual>() {
        @Override
        public int compare(UserVirtual o1, UserVirtual o2) {
            return o1.getLogin().compareTo(o2.getLogin());
        }
    });
    ArgineConventions argineConventionsBids = EngineConfiguration.getInstance().getArgineConventionsBids();
    ArgineConventions argineConventionsCards = EngineConfiguration.getInstance().getArgineConventionsCards();
    if (operation != null) {
        if (operation.equals("computeRequest") || operation.equals("cacheSearch")) {
            String deal = request.getParameter("deal");
            String optionsValue = request.getParameter("optionsValue");
            String optionsEngine = request.getParameter("optionsEngine");
            String optionsType = request.getParameter("optionsType");
            String optionsSpread = request.getParameter("optionsSpread");
            String conventionsBidsFree = request.getParameter("conventionsBidsFree");
            String conventionsBidsProfil = request.getParameter("conventionsBidsProfil");
            String conventionsCardsFree = request.getParameter("conventionsCardsFree");
            String conventionsCardsProfil = request.getParameter("conventionsCardsProfil");
            String game = request.getParameter("game");
            String type = request.getParameter("type");

            RequestService.GetResultParam param = new RequestService.GetResultParam();
            if (conventionsBidsFree != null && conventionsBidsFree.length() > 0) {
                param.conventions = conventionsBidsFree;
            } else {
                param.conventions = argineConventionsBids.getConvention(Integer.parseInt(conventionsBidsProfil)).value;
            }
            if (conventionsCardsFree != null && conventionsCardsFree.length() > 0) {
                param.conventions += conventionsCardsFree;
            } else {
                param.conventions += argineConventionsCards.getConvention(Integer.parseInt(conventionsCardsProfil)).value;
            }
            if (optionsValue != null && optionsValue.length() > 0) {
                param.options = optionsValue;
            } else {
                param.options = Constantes.buildOptionsForEngine(Integer.parseInt(optionsType), Integer.parseInt(optionsEngine), 0, 0, Integer.parseInt(optionsSpread));
            }
            param.game = game;
            param.requestType = Integer.parseInt(type);
            param.deal = deal;

            if (operation.equals("computeRequest")) {
                String engineSelection = request.getParameter("engineSelection");
                // create data but don't put yet in the queue
                QueueData queueData = ContextManager.getQueueMgr().createData(param.getKey(), false, null, null, null, false);
                synchronized (queueData) {
                    try {
                        boolean needToWait = true;
                        if (engineSelection.equals("AUTO")) {
                            queueMgr.addDataToQueue(queueData);
                        } else {
                            UserVirtualEngine userEngine = (UserVirtualEngine) userMgr.getUserVirtualByLogin(engineSelection);
                            if (userEngine != null && userEngine.isEngineWebSocketReady()) {
                                queueMgr.getQueueMap().put(queueData.ID, queueData);
                                queueData.addEngineComputing(userEngine.getID());
                                userEngine.getWebSocket().sendCommandCompute(queueData);
                            } else {
                                resultOperation += "No engine found with login=" + engineSelection;
                                needToWait = false;
                            }
                        }
                        if (needToWait) {
                            // now wait for notify event
                            queueData.wait(30 * 1000);
                            // get result from queue data
                            resultOperation = "<b>Request</b>=" + param + "<br/><b>Engine Result original</b>=" + queueData.getResultOriginal()+" result value="+queueData.resultValue;
                        }
                    } catch (InterruptedException e) {
                    }
                }
            }
            else if (operation.equals("cacheSearch")) {
                String cacheResult = ContextManager.getTreeMgr().getCacheData(param.getKey());
                resultOperation = "<b>Request</b>="+param+"<br/><b>Cache Result</b>="+cacheResult;
            }
        }
    }

%>
	<head>
		<title>EngineServer - Administration Request</title>
		<script type="text/javascript">
		function clickComputeRequest() {
			document.forms["formRequest"].operation.value="computeRequest";
			document.forms["formRequest"].submit();
		}
        function clickCacheSearch() {
            document.forms["formRequest"].operation.value="cacheSearch";
            document.forms["formRequest"].submit();
        }
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION Request</h1>
		<a href="admin.jsp">Administration</a><br/><br/>
		<hr width="95%"/>
		<%if (operation != null){%>
		<b>Operation executed : <%=operation%></b><br/>
		Operation result = <%=resultOperation%><br/>
        <hr width="95%"/>
		<%}%>
		<form name="formRequest" action="adminRequest.jsp" method="post">
			<input type="hidden" name="operation" value="computeRequest"/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><td>Parameters</td>
                    <td>
			Deal  = <input type="text" name="deal" size="80"/> (Dealer + Vulnerability + distrib -- For bid info : S/E - A<%=Constantes.REQUEST_DEAL_BIDINFO%>)<br/>
			Options : engine=<input type="text" name="optionsEngine" size="5" value="261"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            resultType=<input type="text" name="optionsType" size="2" value="1"/>(1 : Paire - 2 : IMP - 0 for bid info)&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            Spread=<input type="text" name="optionsSpread" size="2" value="0"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            Or set options value = <input type="text" name="optionsValue" size="25" value=""/>
            <br/>
			Conventions Bids Profil =
            <select name="conventionsBidsProfil">
                <% if (argineConventionsBids!=null) { for (ArgineConvention p : argineConventionsBids.profiles) {%>
                <option value="<%=p.id%>"><%=p.name%></option>
                <%}}%>
            </select>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            Free : <input type="text" name="conventionsBidsFree" size="80"/><br/>
            Conventions Cards Profil =
            <select name="conventionsCardsProfil">
                <% if (argineConventionsCards!=null) { for (ArgineConvention p : argineConventionsCards.profiles) {%>
                <option value="<%=p.id%>"><%=p.name%></option>
                <%}}%>
            </select>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            Free : <input type="text" name="conventionsCardsFree" size="80"/><br/>
			Game = <input type="text" name="game" size="80"/><br/>
			Type = <input type="radio" name="type" value="0" checked="checked"> Bid - <input type="radio" name="type" value="1"> Card - <input type="radio" name="type" value="2"> Bid Info<br/>
                    </td>
                </tr>
                <tr>
                    <td>Engine compute</td>
                    <td>
            Engine selection =
            <select name="engineSelection">
                <option value="AUTO">AUTO</option>
                <%
                    for (UserVirtual u : listUser) {
                        if (u.isEngine() && ((UserVirtualEngine)u).isEngineWebSocketReady()) {
                %>
                <option value="<%=u.getLogin()%>"><%=u.getLogin()%></option>
                <%
                        }
                    }
                %>
            </select><br/>
			<input type="button" value="Compute Request" onclick="clickComputeRequest()"/>
                    </td>
                </tr>
                <tr>
                    <td>Cache search</td>
                    <td><input type="button" value="Cache search" onclick="clickCacheSearch()"></td>
                </tr>
            </table>
		</form>
	</body>
</html>