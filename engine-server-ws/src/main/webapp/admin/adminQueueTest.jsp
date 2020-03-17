<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="com.gotogames.bridge.engineserver.request.QueueMgr"%>
<%@page import="com.gotogames.bridge.engineserver.request.data.QueueData"%>
<%@page import="com.gotogames.common.bridge.BridgeGame"%>
<%@page import="com.gotogames.common.bridge.PBNConvertion"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.List"%>
<%@ page import="java.util.Map" %>
<%@ page import="com.gotogames.bridge.engineserver.common.Constantes" %>
<%
    QueueMgr queueMgr = ContextManager.getQueueMgr();
    String operation = request.getParameter("operation");
    String paramOperation = "";
    String resultOperation = "";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss:SSS");
    if (queueMgr != null){
        if (operation != null) {
            if (operation.equals("cleanAll")) {
                queueMgr.getQueueTestMap().clear();
                resultOperation = "Clean all TEST success";
            } else if (operation.equals("removeRequest")) {
                paramOperation = request.getParameter("selection");
                String[] reqID = paramOperation.split("-");
                for (int i = 0; i < reqID.length; i++) {
                    queueMgr.removeTestData(Long.parseLong(reqID[i]));
                }
                resultOperation = "Remove TEST success for nbRequest="+reqID.length+" - selection="+paramOperation;
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
            } else if (operation.equals("removeOldest")) {
                List<QueueData> listData = queueMgr.getTestQueueDataList();
                long currentTS = System.currentTimeMillis();
                int nbRemove = 0;
                for (QueueData e : listData) {
                    if (currentTS - e.timestamp > (20*60*1000)) {
                        queueMgr.removeTestData(e.ID);
                        nbRemove++;
                    }
                }
                resultOperation = "Remove oldest request TEST (>1h) nbRemove="+nbRemove;
            }
        }
    }
    List<QueueData> listData = queueMgr.getTestQueueDataList();
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
        <a href="adminQueueTest.jsp">Refresh</a><br/>
		<form name="formQueue" action="adminQueueTest.jsp" method="post">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="selection" value=""/>
            Current date=<%=sdf.format(new Date()) %><br/>
            Date of next job to remove oldest TEST request = <%=Constantes.timestamp2StringDateHour(queueMgr.getDateNextJobRemoveOldestTest())%><br/>
            <b>Queue of request to compute : </b>
            <%if (listData != null){%>
                Nb request = <%=listData.size() %> - Current index TEST value=<%=queueMgr.getCurrentIndexTest() %><br/>
                <input type="hidden" name="nbData" value="<%=listData.size() %>"/>
                <input type="button" value="Clean All" onclick="clickClean()"/>&nbsp;&nbsp;&nbsp;
                <input type="button" value="Remove Selection" onclick="clickRemoveSelection()"/>&nbsp;&nbsp;&nbsp;
                <input type="button" value="Remove Oldest" onclick="clickRemoveOldest()"/>&nbsp;&nbsp;&nbsp;
                <input type="button" value="Select All" onclick="clickSelectAll()"/>
                <br/>
                <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Sel.</th><th>ID</th><th>Operation</th><th>Date</th><th>Engine computing</th><th>Engine version</th><th>Save Cache</th><th>Type</th><th>URL setResult</th><th>Request</th></tr>
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
                            <input type="button" value="Convert PBN" onclick="clickConvertPBN('<%=temp.getRequest() %>')"/>
                        </td>
                        <td><%=sdf.format(new Date(temp.timestamp))%></td>
                        <td><%=temp.engineComputingToString() %></td>
                        <td><%=temp.getEngineVersion() %></td>
                        <td><%=temp.saveInCache%></td>
                        <td><%=temp.getRequestType() %></td>
                        <td><%=temp.getUrlSetResult()%></td>
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