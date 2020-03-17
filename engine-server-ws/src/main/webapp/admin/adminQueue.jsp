<%@page import="com.gotogames.common.tools.NumericalTools"%>
<%@page import="com.codahale.metrics.Meter"%>
<%@page import="com.gotogames.bridge.engineserver.ws.request.RequestServiceImpl"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="com.gotogames.bridge.engineserver.request.QueueMgr"%>
<%@page import="com.gotogames.bridge.engineserver.request.data.QueueData"%>
<%@ page import="com.gotogames.common.bridge.BridgeGame" %>
<%@ page import="com.gotogames.common.bridge.PBNConvertion" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.concurrent.ThreadPoolExecutor" %>
<%@ page import="com.gotogames.bridge.engineserver.cache.RedisCache" %>
<%
    QueueMgr queueMgr = ContextManager.getQueueMgr();
    RequestServiceImpl requestService = ContextManager.getRequestService();
    RedisCache redisCache = ContextManager.getRedisCache();
    String operation = request.getParameter("operation");
    String paramOperation = "";
    String resultOperation = "";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss:SSS");
    if (queueMgr != null){
        if (operation != null) {
            if (operation.equals("cleanAll")) {
                queueMgr.getQueueMap().clear();
                resultOperation = "Clean all success";
            } else if (operation.equals("removeRequest")) {
                paramOperation = request.getParameter("selection");
                String[] reqID = paramOperation.split("-");
                for (int i = 0; i < reqID.length; i++) {
                    queueMgr.removeData(Long.parseLong(reqID[i]));
                }
                resultOperation = "Remove success for nbRequest="+reqID.length+" - selection="+paramOperation;
            }  else if (operation.equals("resetEngine")) {
                paramOperation = request.getParameter("selection");
                String[] reqID = paramOperation.split("-");
                for (int i = 0; i < reqID.length; i++) {
                    queueMgr.resetEngineOnData(Long.parseLong(reqID[i]));
                }
                resultOperation = "Reset engine success for nbRequest="+reqID.length+" - selection="+paramOperation;
            } else if (operation.equals("loadPolling")) {
                queueMgr.loadPollingData();
            } else if (operation.equals("convertPBN")) {
                String[] data = request.getParameter("selection").split(";");
                if (data.length == 5) {
                    char dealer = data[0].charAt(0);
                    char vulnerability = data[0].charAt(1);
                    String deal = data[0].substring(2);
                    String bidsAndCards = data[3];
                    String bids = "";
                    String cards = "";
                    if (bidsAndCards.indexOf("PAPAPA") > 0) {
                        bids = bidsAndCards.substring(0, bidsAndCards.indexOf("PAPAPA")+6);
                        cards = bidsAndCards.substring(bidsAndCards.indexOf("PAPAPA")+6);
                    } else {
                        bids = bidsAndCards;
                    }
                    int engineVersion = -1;
                    try {
                        engineVersion = Integer.decode("0x" + data[1].substring(8, 10) + data[1].substring(6, 8));
                    } catch (Exception e){};
                    BridgeGame bg = BridgeGame.create(deal, dealer, vulnerability, bids, cards);
                    Map<String, String> metaData = new HashMap<String, String>();
                    metaData.put("conventions", data[2]);
                    metaData.put("engineVersion", ""+engineVersion);
                    String pbn = PBNConvertion.gameToPBN(bg, metaData, "<br>");
                    resultOperation = "PBN=<br>"+pbn+"<br>selection="+paramOperation;
                }
            } else if (operation.equals("initPoolThreadSetResult")) {
                queueMgr.initThreadPoolSetResult();
                resultOperation = "Init thread pool setResult with success";
            } else if (operation.equals("removeOldest")) {
                List<QueueData> listData = queueMgr.getQueueDataList();
                long currentTS = System.currentTimeMillis();
                int nbRemove = 0;
                for (QueueData e : listData) {
                    if (currentTS - e.timestamp > (20*60*1000)) {
                        queueMgr.removeData(e.ID);
                        nbRemove++;
                    }
                }
                resultOperation = "Remove oldest request (>1h) nbRemove="+nbRemove;
            }
        }
    }
    List<QueueData> listData = queueMgr.getQueueDataList();
    Meter meterRequestAll = ContextManager.getRequestService().getMeterRequestAll();
    Meter meterRequestBid = ContextManager.getRequestService().getMeterRequestBid();
    Meter meterRequestBidInfo = ContextManager.getRequestService().getMeterRequestBidInfo();
    Meter meterRequestCard = ContextManager.getRequestService().getMeterRequestCard();
    Meter meterRequestPar = ContextManager.getRequestService().getMeterRequestPar();
    Meter meterRequestClaim = ContextManager.getRequestService().getMeterRequestClaim();
    Meter meterRequestNoCache = ContextManager.getRequestService().getMeterRequestNoCache();

    Meter meterRequestEngine = ContextManager.getRequestService().getMeterRequestEngine();
    Meter meterRequestEngineBid = ContextManager.getRequestService().getMeterRequestEngineBid();
    Meter meterRequestEngineBidInfo = ContextManager.getRequestService().getMeterRequestEngineBidInfo();
    Meter meterRequestEngineCard = ContextManager.getRequestService().getMeterRequestEngineCard();
    Meter meterRequestEnginePar = ContextManager.getRequestService().getMeterRequestEnginePar();
    Meter meterRequestEngineClaim = ContextManager.getRequestService().getMeterRequestEngineClaim();
    Meter meterRequestEngineNoCache = ContextManager.getRequestService().getMeterRequestEngineNoCache();

    long reqAll = meterRequestAll.getCount(), reqBid = meterRequestBid.getCount(), reqBidInfo = meterRequestBidInfo.getCount(), reqCard = meterRequestCard.getCount(), reqPar = meterRequestPar.getCount(), reqNoCache = meterRequestNoCache.getCount(), reqClaim = meterRequestClaim.getCount();
    long reqAll1 = (long)meterRequestAll.getOneMinuteRate(), reqBid1 = (long)meterRequestBid.getOneMinuteRate(), reqBidInfo1 = (long)meterRequestBidInfo.getOneMinuteRate(), reqCard1 = (long)meterRequestCard.getOneMinuteRate(), reqPar1 = (long)meterRequestPar.getOneMinuteRate(), reqNoCache1 = (long)meterRequestNoCache.getOneMinuteRate(), reqClaim1 = (long)meterRequestClaim.getOneMinuteRate();
    long reqAll5 = (long)meterRequestAll.getFiveMinuteRate(), reqBid5 = (long)meterRequestBid.getFiveMinuteRate(), reqBidInfo5 = (long)meterRequestBidInfo.getFiveMinuteRate(), reqCard5 = (long)meterRequestCard.getFiveMinuteRate(), reqPar5 = (long)meterRequestPar.getFiveMinuteRate(), reqNoCache5 = (long)meterRequestNoCache.getFiveMinuteRate(), reqClaim5 = (long)meterRequestClaim.getFiveMinuteRate();
    long reqAll15 = (long)meterRequestAll.getFifteenMinuteRate(), reqBid15 = (long)meterRequestBid.getFifteenMinuteRate(), reqBidInfo15 = (long)meterRequestBidInfo.getFifteenMinuteRate(), reqCard15 = (long)meterRequestCard.getFifteenMinuteRate(), reqPar15 = (long)meterRequestPar.getFifteenMinuteRate(), reqNoCache15 = (long)meterRequestNoCache.getFifteenMinuteRate(), reqClaim15 = (long)meterRequestClaim.getFifteenMinuteRate();

    long reqEngineAll = meterRequestEngine.getCount(), reqEngineBid = meterRequestEngineBid.getCount(), reqEngineBidInfo = meterRequestEngineBidInfo.getCount(), reqEngineCard = meterRequestEngineCard.getCount(), reqEnginePar = meterRequestEnginePar.getCount(), reqEngineNoCache = meterRequestEngineNoCache.getCount(), reqEngineClaim = meterRequestEngineClaim.getCount();
    long reqEngineAll1 = (long)meterRequestEngine.getOneMinuteRate(), reqEngineBid1 = (long)meterRequestEngineBid.getOneMinuteRate(), reqEngineBidInfo1 = (long)meterRequestEngineBidInfo.getOneMinuteRate(), reqEngineCard1 = (long)meterRequestEngineCard.getOneMinuteRate(), reqEnginePar1 = (long)meterRequestEnginePar.getOneMinuteRate(), reqEngineNoCache1 = (long)meterRequestEngineNoCache.getOneMinuteRate(), reqEngineClaim1 = (long)meterRequestEngineClaim.getOneMinuteRate();
    long reqEngineAll5 = (long)meterRequestEngine.getFiveMinuteRate(), reqEngineBid5 = (long)meterRequestEngineBid.getFiveMinuteRate(), reqEngineBidInfo5 = (long)meterRequestEngineBidInfo.getFiveMinuteRate(), reqEngineCard5 = (long)meterRequestEngineCard.getFiveMinuteRate(), reqEnginePar5 = (long)meterRequestEnginePar.getFiveMinuteRate(), reqEngineNoCache5 = (long)meterRequestEngineNoCache.getFiveMinuteRate(), reqEngineClaim5 = (long)meterRequestEngineClaim.getFiveMinuteRate();
    long reqEngineAll15 = (long)meterRequestEngine.getFifteenMinuteRate(), reqEngineBid15 = (long)meterRequestEngineBid.getFifteenMinuteRate(), reqEngineBidInfo15 = (long)meterRequestEngineBidInfo.getFifteenMinuteRate(), reqEngineCard15 = (long)meterRequestEngineCard.getFifteenMinuteRate(), reqEnginePar15 = (long)meterRequestEnginePar.getFifteenMinuteRate(), reqEngineNoCache15 = (long)meterRequestEngineNoCache.getFifteenMinuteRate(), reqEngineClaim15 = (long)meterRequestEngineClaim.getFifteenMinuteRate();

    ThreadPoolExecutor threadPoolSetResult = (ThreadPoolExecutor)queueMgr.getThreadPoolSetResult();
%>
<html>
	<head>
		<title>EngineServer - Administration Queue</title>
		<script type="text/javascript">
		function clickClean(){
			if (confirm("Clean all data from queue ?")){
				document.forms["formQueue"].operation.value="cleanAll";
				document.forms["formQueue"].submit();
			}
		}
		function clickRemoveRequest(id){
			if (confirm("Remove request with ID="+id+" from queue ?")){
				document.forms["formQueue"].operation.value="removeRequest";
				document.forms["formQueue"].selection.value=id;
				document.forms["formQueue"].submit();
			}
		}
        function clickRemoveOldest(){
            if (confirm("Remove oldest request from queue ?")){
                document.forms["formQueue"].operation.value="removeOldest";
                document.forms["formQueue"].submit();
            }
        }
		function clickResetEngine(id) {
			if (confirm("Reset engine no request ID="+id+" ?")){
				document.forms["formQueue"].operation.value="resetEngine";
				document.forms["formQueue"].selection.value=id;
				document.forms["formQueue"].submit();
			}
		}
		function clickLoadPollingConfig(){
			if (confirm("Load polling settings from configuration file ?")){
				document.forms["formQueue"].operation.value="loadPolling";
				document.forms["formQueue"].submit();
			}
		}
		function clickRemoveSelection() {
            selID = "";
            for (i=0; i < document.forms["formQueue"].nbData.value; i++) {
            	if (document.getElementById("sel"+i).checked) {
                	if (selID != "") {
                    	selID += "-";
                    }
                    selID += document.getElementById("sel"+i).value;
                }
            }
            if (confirm("Remove all selected request ?")) {
            	document.forms["formQueue"].operation.value="removeRequest";
                document.forms["formQueue"].selection.value=selID;
                document.forms["formQueue"].submit();
            }
    	}
		function clickSelectAll() {
            for (i=0; i < document.forms["formQueue"].nbData.value; i++) {
            	if(!document.getElementById("sel"+i).checked) {
            		document.getElementById("sel"+i).checked = true;
            	}
            }
	    }
        function clickConvertPBN(data) {
            if (confirm("Convert request to PBN  ?")){
                document.forms["formQueue"].operation.value="convertPBN";
                document.forms["formQueue"].selection.value=data;
                document.forms["formQueue"].submit();
            }
        }

        function clickInitPool() {
            if (confirm("Init Pool thread SetResult ?")){
                document.forms["formQueue"].operation.value="initPoolThreadSetResult";
                document.forms["formQueue"].submit();
            }
        }
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION QUEUE</h1>
		<a href="admin.jsp">Administration</a><br/><br/>
        <hr width="90%"/>
		<%if (operation != null){ %>
		<b>Operation executed : <%=operation %> - parametres=<%=paramOperation %></b><br/>
        <%=resultOperation%><br/><hr width="90%"/>
		<%} %>
        <a href="adminQueue.jsp">Refresh</a><br/>
		<form name="formQueue" action="adminQueue.jsp" method="post">
		<input type="hidden" name="operation" value=""/>
		<input type="hidden" name="selection" value=""/>
		Current date=<%=sdf.format(new Date()) %><br/>
        Pool thread setResult : ThreadActiveCount=<%=threadPoolSetResult.getActiveCount()%> - Queue size=<%=threadPoolSetResult.getQueue().size()%> - MaxPoolSize=<%=threadPoolSetResult.getMaximumPoolSize()%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Init Pool !" onclick="clickInitPool()"><br/>
		Time Data Cache = <%=requestService.getMeterAverageTimeCache() %> (<%=requestService.getMeterTimeCache().size()%>) - Time Data No Cache = <%=requestService.getMeterAverageTimeNoCache() %>(<%=requestService.getMeterTimeNoCache().size()%>) - Time Data Global = <%=requestService.getMeterAverageTimeGlobal() %>(<%=requestService.getMeterTimeGlobal().size()%>)<br/>
        Time Redis Cache GET = <%=redisCache.getMeterAverageTimeCacheGet()%> (<%=redisCache.getMeterTimeCacheGet().size()%>) - Time Redis Cache SET = <%=redisCache.getMeterAverageTimeCacheSet()%> (<%=redisCache.getMeterTimeCacheSet().size()%>)<br/>
		Nb Result found in Cache = <%=requestService.getNbResultCache() %> - Nb Result not found in Cache = <%=requestService.getNbResultNoCache() %><br/>
		Request Metrics : (1/5/15 : rate 1mn / 5mn / 15mn)
		<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
			<tr><td>Type</td><td>Request (1/5/15)</td><td>Engine (1/5/15)</td><td>Use cache</td></tr>
			<tr>
                <td>All</td>
                <td><%=reqAll %>&nbsp;&nbsp;&nbsp;(<%=reqAll1%>/<%=reqAll5%>/<%=reqAll15%>)</td>
                <td><%=reqEngineAll %>&nbsp;&nbsp;&nbsp;(<%=reqEngineAll1%>/<%=reqEngineAll5%>/<%=reqEngineAll15%>)</td>
                <td><%=NumericalTools.round((double)(reqAll-reqEngineAll)*100/reqAll, 2)%>%</td>
            </tr>
            <tr>
                <td>Bid</td>
                <td><%=reqBid %>&nbsp;&nbsp;&nbsp;(<%=reqBid1%>/<%=reqBid5%>/<%=reqBid15%>)</td>
                <td><%=reqEngineBid %>&nbsp;&nbsp;&nbsp;(<%=reqEngineBid1%>/<%=reqEngineBid5%>/<%=reqEngineBid15%>)</td>
                <td><%=NumericalTools.round((double)(reqBid-reqEngineBid)*100/reqBid, 2)%>%</td>
            </tr>
            <tr>
                <td>BidInfo</td>
                <td><%=reqBidInfo %>&nbsp;&nbsp;&nbsp;(<%=reqBidInfo1%>/<%=reqBidInfo5%>/<%=reqBidInfo15%>)</td>
                <td><%=reqEngineBidInfo %>&nbsp;&nbsp;&nbsp;(<%=reqEngineBidInfo1%>/<%=reqEngineBidInfo5%>/<%=reqEngineBidInfo15%>)</td>
                <td><%=NumericalTools.round((double)(reqBidInfo-reqEngineBidInfo)*100/reqBidInfo, 2)%>%</td>
            </tr>
            <tr>
                <td>Card</td>
                <td><%=reqCard %>&nbsp;&nbsp;&nbsp;(<%=reqCard1%>/<%=reqCard5%>/<%=reqCard15%>)</td>
                <td><%=reqEngineCard %>&nbsp;&nbsp;&nbsp;(<%=reqEngineCard1%>/<%=reqEngineCard5%>/<%=reqEngineCard15%>)</td>
                <td><%=NumericalTools.round((double)(reqCard-reqEngineCard)*100/reqCard, 2)%>%</td>
            </tr>
            <tr>
                <td>Par</td>
                <td><%=reqPar %>&nbsp;&nbsp;&nbsp;(<%=reqPar1%>/<%=reqPar5%>/<%=reqPar15%>)</td>
                <td><%=reqEnginePar %>&nbsp;&nbsp;&nbsp;(<%=reqEnginePar1%>/<%=reqEnginePar5%>/<%=reqEnginePar15%>)</td>
                <td><%=NumericalTools.round((double)(reqPar-reqEnginePar)*100/reqPar, 2)%>%</td>
            </tr>
            <tr>
                <td>Claim</td>
                <td><%=reqClaim %>&nbsp;&nbsp;&nbsp;(<%=reqClaim1%>/<%=reqClaim5%>/<%=reqClaim15%>)</td>
                <td><%=reqEngineClaim %>&nbsp;&nbsp;&nbsp;(<%=reqEngineClaim1%>/<%=reqEngineClaim5%>/<%=reqEngineClaim5%>)</td>
                <td><%=NumericalTools.round((double)(reqClaim-reqEngineClaim)*100/reqClaim, 2)%>%</td>
            </tr>
            <tr>
                <td>NoCache</td>
                <td><%=reqNoCache %>&nbsp;&nbsp;&nbsp;(<%=reqNoCache1%>/<%=reqNoCache5%>/<%=reqNoCache15%>)</td>
                <td><%=reqEngineNoCache %>&nbsp;&nbsp;&nbsp;(<%=reqEngineNoCache1%>/<%=reqEngineNoCache5%>/<%=reqEngineNoCache15%>)</td>
                <td><%=NumericalTools.round((double)(reqNoCache-reqEngineNoCache)*100/reqNoCache, 2)%>%</td>
            </tr>
		</table>
		<hr width="90%"/>
		<b>Polling queue settings:</b><br/>
		<input type="button" value="Load config" onclick="clickLoadPollingConfig()"/>
		<br/>
		compute=<%=queueMgr.isPollingCompute() %> -	default value=<%=queueMgr.getPollingDefaultValue() %><br/>
		<b>Polling Users</b><br/>
		<%
		String[] pollingUsers = queueMgr.getPollingUsers();
		if (pollingUsers != null) {
			for (int i = 0; i < pollingUsers.length; i++) {%>
				User : <%=pollingUsers[i] %> - settings(<%=queueMgr.getPollingSettingForUser(pollingUsers[i]) %>) - Current value=<%=queueMgr.getPollingValue(pollingUsers[i]) %><br/>
			<%}
		} else {
		%>
		No polling user defined in configuration file !
		<%} %>
		Threshold smaller : <%=queueMgr.getPollingThresholdSmaller().toString() %><br/>
		Function smaller : <%=queueMgr.getPollingFunctionSmaller().toString() %><br/>
		Threshold middle : <%=queueMgr.getPollingThresholdMiddle().toString() %><br/>
		Funtion greater : <%=queueMgr.getPollingFunctionGreater().toString() %><br/>
		Threshold greater : <%=queueMgr.getPollingThresholdGreater().toString() %> 
		<hr width="90%"/>
		<b>Queue of request to compute : </b>
		<%if (listData != null){%>
			Nb request = <%=listData.size() %> - Current index value=<%=queueMgr.getCurrentIndex() %><br/>
			<input type="hidden" name="nbData" value="<%=listData.size() %>"/>
			<input type="button" value="Clean All" onclick="clickClean()"/>&nbsp;&nbsp;&nbsp;
			<input type="button" value="Remove Selection" onclick="clickRemoveSelection()"/>&nbsp;&nbsp;&nbsp;
            <input type="button" value="Remove Oldest" onclick="clickRemoveOldest()"/>&nbsp;&nbsp;&nbsp;
			<input type="button" value="Select All" onclick="clickSelectAll()"/>
			<br/>
			<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
			<tr><th>Sel.</th><th>ID</th><th>Operation</th><th>Date</th><th>Engine computing</th><th>Engine version</th><th>Save Cache</th><th>Type</th><th>URL setResult</th><th>User</th><th>Request</th></tr>
			<%
			for (int i = 0; i < listData.size(); i++) {
				QueueData temp = listData.get(i);
				%>
				<tr>
                    <td><input type="checkbox" id="sel<%=i %>" name="sel<%=i %>" value="<%=temp.ID %>"/></td>
                    <td><%=temp.ID %></td>
                    <td>
                        <input type="button" value="Remove" onclick="clickRemoveRequest(<%=temp.ID %>)"/>
                        &nbsp;&nbsp;&nbsp;
                        <input type="button" value="Reset Engine" onclick="clickResetEngine(<%=temp.ID %>)"/>
                        &nbsp;&nbsp;&nbsp;
                        <input type="button" value="Convert PBN" onclick="clickConvertPBN('<%=temp.getRequest() %>')"/>
                    </td>
                    <td><%=sdf.format(new Date(temp.timestamp))%></td>
                    <td><%=temp.engineComputingToString() %></td>
                    <td><%=temp.getEngineVersion() %></td>
                    <td><%=temp.saveInCache%></td>
                    <td><%=temp.getRequestType() %></td>
                    <td><%=temp.getUrlSetResult()%></td>
                    <td><%=temp.getUser()!=null?temp.getUser().getLoginID():null%></td>
                    <td><%=temp.getRequest() %></td>
				</tr>
				<%
			}
		%>
			</table>
			
		<%} else {%>
		LIST SESSION NULL !!
		<%} %>
		</form>
	</body>
</html>