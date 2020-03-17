<%@page import="com.gotogames.bridge.engineserver.common.Constantes"%>
<%@page import="com.gotogames.common.session.Session"%>
<%@page import="com.gotogames.bridge.engineserver.user.UserVirtual"%>
<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="com.gotogames.bridge.engineserver.user.UserVirtualMgr"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.gotogames.common.bridge.BridgeTransformData"%>
<%@page import="com.gotogames.common.bridge.GameBridgeRule"%>
<%@page import="java.util.List"%>
<%@ page import="com.gotogames.common.bridge.BridgeGame" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="com.gotogames.common.bridge.PBNConvertion" %>
<html>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    String cryptData = "";
    String deadlockThreadsData = "";
    List<String> decryptData = null;
    String strRequest = "";
    if (operation != null) {
        if (operation.equals("decryptTreeData")) {
            cryptData = request.getParameter("cryptData");
            if (cryptData != null && cryptData.length() > 0) {
                decryptData = new ArrayList<String>();
                String[] temp = cryptData.split("\r\n");
                for (int i = 0; i < temp.length; i++) {
                    if (temp[i].length() > 100) {
                        String t = temp[i].substring(100);
                        String g = "";
                        int idx = 0;
                        while (idx < t.length()) {
                            if (GameBridgeRule.isEndBids(g)) {
                                g += BridgeTransformData.convertBridgeCard2String(t.charAt(idx));
                            } else {
                                g += BridgeTransformData.convertBridgeBid2String(t.charAt(idx));
                            }
                            idx ++;
                            idx ++;
                        }
                        decryptData.add(g);
                    }
                }
            }
        }
        else if (operation.equals("deadlockThread")) {
            List<String> listInfoDeadlock = ContextManager.getLogStatMgr().getInfoDeadlockedThreads(false);
            for (String s : listInfoDeadlock) {
                deadlockThreadsData+=s+"\r\n";
            }
        }
        else if (operation.equals("convertRequestToPBN")) {
            strRequest = request.getParameter("strRequest");
            String[] data = strRequest.split(";");
            if (data.length >= 5) {
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
                resultOperation = "PBN=<br>"+pbn+"<br>selection="+strRequest;
            }
        }
    }
%>
	<head>
		<title>EngineServer - Administration Request</title>
		<script type="text/javascript">
            function decryptTreeData() {
                document.forms["formTools"].operation.value="decryptTreeData";
                document.forms["formTools"].submit();
            }
            function getDeadlockThread() {
                document.forms["formTools"].operation.value="deadlockThread";
                document.forms["formTools"].submit();
            }
            function convertRequestToPBN() {
                document.forms["formTools"].operation.value="convertRequestToPBN";
                document.forms["formTools"].submit();
            }
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION Tools</h1>
		<a href="admin.jsp">Administration</a><br/><br/>
		<%if (operation != null){%>
		<b>Operation executed : <%=operation%></b><br/>
		Operation result = <%=resultOperation%><br/>
		<hr with="90%"/>
		<%}%>
		<br/>
		<form name="formTools" action="adminTools.jsp" method="post">
            <input type="hidden" name="operation" value=""/>
            <hr width="90%"/>
            <b>Convert Request to PBN</b><br/>
            Request string format (5 fields separated by ';') : <br/>
            <textarea name="strRequest" cols="80" rows="4" wrap="off"><%=strRequest%></textarea><br/>
            <input type="button" value="Convert Request to PBN" onclick="convertRequestToPBN()"><br/>
            <hr width="90%"/>
            <b>Dealocked Threads</b><br/>
            <input type="button" value="Get deadlock Thread" onclick="getDeadlockThread()"/><br/>
            <textarea name="deadlockThreadsData" cols="120" rows="15"><%=deadlockThreadsData%></textarea><br/>
            <hr width="90%"/>
            <b>Decrypt tree data</b><br/>
            Data :<br/>
            <textarea name="cryptData" cols="100" rows="10" wrap="off"><%=cryptData%></textarea><br/>
            <input type="button" value="Decrypt" onclick="decryptTreeData()"/><br/>
            <%if (decryptData != null) {
                String decrypDataTextArea = "";
                for (String s : decryptData) {
                    decrypDataTextArea+=s+"\r\n";
                }
            %>
            Result :<br/>
            <textarea readonly="readonly" cols="100" rows="10" wrap="off"><%=decrypDataTextArea%></textarea>
            <%} %>
		</form>
	</body>
</html>