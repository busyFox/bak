<%@page import="com.gotogames.bridge.engineserver.cache.TreeBidCard"%>
<%@page import="com.gotogames.bridge.engineserver.cache.TreeBidInfo"%>
<%@page import="com.gotogames.bridge.engineserver.cache.TreeMgr"%>
<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.List"%>
<%@include file="requestFunction.jsp" %>
<html>
<%
    TreeMgr treeMgr = ContextManager.getTreeMgr();
    String operation = request.getParameter("operation");
    String paramOperation = "";
    String resultOperation = "";
    TreeBidCard treeBidCardSearch= null;
    if (operation != null) {
        if (operation.equals("clearAll")) {
            long ts1 = System.currentTimeMillis();
            treeMgr.clearAll();
            long ts2 = System.currentTimeMillis();
            resultOperation = "OK - TS="+(ts2-ts1);
        } else if (operation.equals("deleteTree")) {
            paramOperation = request.getParameter("selection");
            String bidInfo =  request.getParameter("bidinfo");
            String[] treeKey = paramOperation.split("-");
            boolean bTemp = true;
            long ts1 = System.currentTimeMillis();
            for (int i = 0; i < treeKey.length; i++) {
                if (bidInfo.equals("bidinfo")) {
                    bTemp = bTemp && treeMgr.deleteTreeBidInfoForKey(treeKey[i]);
                } else if (bidInfo.equals("bidcard")) {
                    bTemp = bTemp && treeMgr.deleteTreeBidCardForKey(treeKey[i]);
                }
            }
            resultOperation = ""+bTemp;
            long ts2 = System.currentTimeMillis();
            resultOperation += " TS="+(ts2-ts1);
        } else if (operation.equals("query")) {
            String queryDeal = request.getParameter("queryDeal");
            String queryEngine = request.getParameter("queryEngine");
            String queryResultType = request.getParameter("queryResultType");
            String querySpreadEnable = request.getParameter("querySpreadEnable");
            String queryConvBidsValue = request.getParameter("queryConvBidsValue");
            String queryConvCardsValue = request.getParameter("queryConvCardsValue");
            String queryGame = request.getParameter("queryGame");
            String queryType = request.getParameter("queryType");
            paramOperation = "queryDeal = "+queryDeal + " - queryEngine=" + queryEngine + " - queryResultType=" + queryResultType + " - queryConvBidsValue=" + queryConvBidsValue + " - queryConvCardsValue=" + queryConvCardsValue + " - queryGame=" + queryGame + " - queryType=" + queryType;
            if (queryDeal == null || queryEngine == null || queryResultType == null || querySpreadEnable == null || queryConvBidsValue == null || queryConvCardsValue == null || queryGame == null || queryType == null) {
                resultOperation = "query parameter not valid !";
            } else {
                try {
                    if (queryConvBidsValue.length() > 47) {
                        queryConvBidsValue = queryConvBidsValue.substring(0, 47);
                    }
                    if (queryConvCardsValue.length() > 6) {
                        queryConvCardsValue = queryConvCardsValue.substring(0, 6);
                    }
                    long ts1 = System.currentTimeMillis();
                    String queryKey = queryDeal + ";" +
                            buildRequestOptions(Integer.parseInt(queryResultType), Integer.parseInt(queryEngine), 0, 0, Integer.parseInt(querySpreadEnable)) + ";" +
                            queryConvBidsValue + queryConvCardsValue + ";" +
                            queryGame + ";" +
                            queryType;
                    resultOperation = "Query key="+queryKey+" <br/> result="+treeMgr.getCacheData(queryKey);
                    long ts2 = System.currentTimeMillis();
                    resultOperation += " TS=" + (ts2 - ts1);
                } catch (Exception e) {

                }
            }
        }
        else if (operation.equals("searchBidCardTree")) {
            String queryBidCardDeal = request.getParameter("queryBidCardDeal");
            String queryBidCardResultType = request.getParameter("queryBidCardResultType");
            String queryBidCardEngine = request.getParameter("queryBidCardEngine");
            String querySpreadEnable = request.getParameter("queryBidCardSpreadEnable");
            String options = buildRequestOptions(Integer.parseInt(queryBidCardResultType), Integer.parseInt(queryBidCardEngine), 0, 0, Integer.parseInt(querySpreadEnable));
            String treeKeyBidCard = queryBidCardDeal+options;
            treeBidCardSearch = treeMgr.getTreeBidCard(treeKeyBidCard);
            resultOperation = "Search tree with key="+treeKeyBidCard+" - found="+(treeBidCardSearch!=null);
        }
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
%>
	<head>
		<title>EngineServer - Administration Cache</title>
		<script type="text/javascript">
            function clickTreeDelete(treeKey, bidInfo) {
                if (confirm("Remove tree with key="+treeKey)){
                    document.forms["formCache"].bidinfo.value=bidInfo;
                    document.forms["formCache"].operation.value="deleteTree";
                    document.forms["formCache"].selection.value=treeKey;
                    document.forms["formCache"].submit();
                }
            }
            function clickTreeQuery(treeKey, bidInfo) {
                document.location = "adminCacheTree.jsp?type="+bidInfo+"&treekey="+treeKey;

            }
            function clickTreeDisplay(treeKey, bidInfo) {
                document.location = "adminCacheTreeDisplay.jsp?type="+bidInfo+"&treekey="+treeKey;

            }
            function clickRunQuery() {
                document.forms["formCache"].operation.value="query";
                document.forms["formCache"].submit();
            }
            function clickClearAll() {
                if (confirm("Clear all data cache ?")){
                    document.forms["formCache"].operation.value="clearAll";
                    document.forms["formCache"].submit();
                }
            }
            function clickSearchBidCardTree() {
                document.forms["formCache"].operation.value="searchBidCardTree";
                document.forms["formCache"].submit();
            }
            function clickSelectConventionBids() {
                var profil = document.forms["formCache"].queryConvBidsProfil.value;
                var value;
                var valueReadOnly = true;
                if (profil == 1) {
                    value = "00001100002001013001110000000000000000000000000";
                } else if (profil == 2) {
                    value = "01001100002001013001111111101101110000102100101";
                } else if (profil == 3) {
                    value = "02011100012001013101111111111111111001112111111";
                } else if (profil == 4) {
                    value = "00000000000000000001010000000000000000000000000";
                } else if (profil == 5) {
                    value = "01000000001000001001011111101101110000102100101";
                } else if (profil == 6) {
                    value = "02010000011000001111011111111111111001112111111";
                } else if (profil == 7) {
                    value = "20101222100000000002110000000001000000000000000";
                } else if (profil == 8) {
                    value = "21101100101000001002111111101101110000102100101";
                } else if (profil == 9) {
                    value = "22111100111000001112111111111111111001112111111";
                } else if (profil == 10) {
                    value = "30003333200000000001010000000000000000000000000";
                } else if (profil == 11) {
                    value = "31003333201000001001011111101001110000102100101";
                } else if (profil == 12) {
                    value = "32013333211000001111011111111111111001112111111";
                } else if (profil == 13) {
                    value = "00001222100000000001010000000000000000000000000";
                } else if (profil == 14) {
                    valueReadOnly = false;
                    value = "";
                }
                document.forms["formCache"].queryConvBidsValue.readOnly=valueReadOnly;
                document.forms["formCache"].queryConvBidsValue.value=value;
            }
            function clickSelectConventionCards() {
                var profil = document.forms["formCache"].queryConvCardsProfil.value;
                var value;
                var valueReadOnly = true;
                if (profil == 1) {
                    value="000000";
                } else if (profil == 2) {
                    value = "123310";
                } else if (profil == 3) {
                    valueReadOnly = false;
                    value = "";
                }
                document.forms["formCache"].queryConvCardsValue.readOnly=valueReadOnly;
                document.forms["formCache"].queryConvCardsValue.value=value;
            }
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION CACHE</h1>
		<a href="admin.jsp">Administration</a><br/><br/>
		<hr width="90%"/>
		<form name="formCache" action="adminCache.jsp" method="post">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="selection" value=""/>
            <input type="hidden" name="bidinfo" value=""/>
            <a href="adminCache.jsp">Refresh</a>
            <br/><br/>
            <input type="button" value="Clear all Cache" onclick="clickClearAll()"/>
            <br/>
            <%
                if (operation != null){
            %>
            <hr width="95%"/>
            <b>Operation executed : <%=operation%> - parameters=<%=paramOperation%></b><br/>
            Operation result = <%=resultOperation%><br/>
            <%
                }
            %>
            <hr width="95%"/>
            <b>Query Parameters</b><br/>
            Deal = <input type="text" name="queryDeal" size="100"/><br/>(Dealer + Vulnerability + distrib. For bid info : SASSSSWWWNNNEEESSSSWWWNNNEEESSSSWWWNNNEEESSSSWWWNNNEEE)<br/>
            Engine version = <input type="text" name="queryEngine" size="10"/> - Result Type = <input type="text" name="queryResultType" size="3" value="1"/>(1:PAIRE, 2:IMP) - Spread Enable = <input type="text" name="querySpreadEnable" size="3" value="0">(Only for card query type)<br/>
            Bids Profil =
            <p style="margin-left: 20px; margin-top: 0px; margin-bottom: 0px">
                <input type="radio" name="queryConvBidsProfil" value="1" onclick="clickSelectConventionBids()"> 1(SAYC_1) -
                <input type="radio" name="queryConvBidsProfil" value="2" onclick="clickSelectConventionBids()"> 2(SAYC_2) -
                <input type="radio" name="queryConvBidsProfil" value="3" onclick="clickSelectConventionBids()"> 3(SAYC_3) -
                <input type="radio" name="queryConvBidsProfil" value="4" onclick="clickSelectConventionBids()"> 4(MAJ5_1) -
                <input type="radio" name="queryConvBidsProfil" value="5" onclick="clickSelectConventionBids()"> 5(MAJ5_2) -
                <input type="radio" name="queryConvBidsProfil" value="6" onclick="clickSelectConventionBids()"> 6(MAJ5_3) -
                <input type="radio" name="queryConvBidsProfil" value="7" onclick="clickSelectConventionBids()"> 7(ACOL_1) -
                <input type="radio" name="queryConvBidsProfil" value="8" onclick="clickSelectConventionBids()"> 8(ACOL_2) -
                <input type="radio" name="queryConvBidsProfil" value="9" onclick="clickSelectConventionBids()"> 9(ACOL_3) -
                <input type="radio" name="queryConvBidsProfil" value="10" onclick="clickSelectConventionBids()"> 10(POLISH_1) -
                <input type="radio" name="queryConvBidsProfil" value="11" onclick="clickSelectConventionBids()"> 11(POLISH_2) -
                <input type="radio" name="queryConvBidsProfil" value="12" onclick="clickSelectConventionBids()"> 12(POLISH_3) -
                <input type="radio" name="queryConvBidsProfil" value="13" onclick="clickSelectConventionBids()"> 13(MAJ5_OLD) -
                <input type="radio" name="queryConvBidsProfil" value="14" onclick="clickSelectConventionBids()"> Free<br/>
                Value : <input type="text" name="queryConvBidsValue" size="80" readonly/><br/>
            </p>
            Cards Conventions =
            <p style="margin-left: 20px; margin-top: 0px; margin-bottom: 0px">
                <input type="radio" name="queryConvCardsProfil" value="1" onclick="clickSelectConventionCards()"> 1(NO_MEANING) -
                <input type="radio" name="queryConvCardsProfil" value="2" onclick="clickSelectConventionCards()"> 2(CLASSIC)<br/>
                <input type="radio" name="queryConvCardsProfil" value="3" onclick="clickSelectConventionCards()"> Free<br/>
                Value : <input type="text" name="queryConvCardsValue" size="60"/><br/>
            </p>
            Game = <input type="text" name="queryGame" size="100"/><br/>
            Type = <input type="radio" name="queryType" value="0" checked="checked"> Bid - <input type="radio" name="queryType" value="1"> Card - <input type="radio" name="queryType" value="2"> Bid Info<br/>
            <input type="button" value="Run Query" onclick="clickRunQuery()"/>
            <br/>
            <hr width="95%"/>
            <b>Tree BID INFO</b><br/>
            Size = <%=treeMgr.getNbTreeBidInfo()%><br/>
            Info = <%=treeMgr.getTreeBidInfoCacheInfo()%><br/>
            <br/>
            <b>Tree BID CARD</b><br/>
            Size = <%=treeMgr.getNbTreeBidCard()%></b><br/>
            Info = <%=treeMgr.getTreeBidCardCacheInfo()%><br>
            <hr width="95%"/>
            <b>LIST TREE CACHE BID INFO</b>
            <br/>
            Nb Tree = <%=treeMgr.getNbTreeBidInfo()%> - Nb Node = <%=treeMgr.getCacheNbNodeBidInfo() %>
            <br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Tree Key</th><th>Nb node</th><th>Date last consult</th><th>Date last add</th><th>Operation</th></tr>
            <%
                List<TreeBidInfo> listTreeBidInfo = treeMgr.getListTreeBidInfo();
                if (listTreeBidInfo != null) {
                    for (TreeBidInfo t : listTreeBidInfo) {
            %>
                <tr>
                    <td><%=t.getTreeDealKey() %></td>
                    <td><%=t.getNbNode() %></td>
                    <td><%=sdf.format(new Date(t.getTsLastConsult())) %></td>
                    <td><%=sdf.format(new Date(t.getTsLastAdd())) %></td>
                    <td>
                        <input type="button" value="Query" onclick="clickTreeQuery('<%=t.getTreeDealKey() %>', 'bidinfo')"/>&nbsp;&nbsp;&nbsp;
                        <input type="button" value="Display" onclick="clickTreeDisplay('<%=t.getTreeDealKey() %>', 'bidinfo')"/>&nbsp;&nbsp;&nbsp;
                        <input type="button" value="Delete" onclick="clickTreeDelete('<%=t.getTreeDealKey() %>', 'bidinfo')"/>
                    </td>
                </tr>
            <%}}%>
            </table>
            <br/>
            <hr width="95%"/>
            <b>LIST TREE CACHE BID CARD</b>
            <br/>
            Nb Tree = <%=treeMgr.getNbTreeBidCard()%> - Nb Node = <%=treeMgr.getCacheNbNodeBidCard() %>
            <br/>
            Deal = <input type="text" name="queryBidCardDeal" size="60">(Dealer + Vulnerability + distrib)<br/>
            Result type = <input type="text" name="queryBidCardResultType" size="3"> - Engine Version = <input type="text" name="queryBidCardEngine" size="10"> - Spread Enable = <input type="text" name="queryBidCardSpreadEnable" size="3" value="0"><br/>
            <input type="button" value="Search" onclick="clickSearchBidCardTree()"/>
            <% if (treeBidCardSearch != null) {%>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Tree Key</th><th>Nb node</th><th>Date last consult</th><th>Date last add</th><th>Operation</th></tr>
                <tr>
                    <td><%=treeBidCardSearch.getTreeDealKey() %></td>
                    <td><%=treeBidCardSearch.getNbNode() %></td>
                    <td><%=sdf.format(new Date(treeBidCardSearch.getTsLastConsult())) %></td>
                    <td><%=sdf.format(new Date(treeBidCardSearch.getTsLastAdd())) %></td>
                    <td>
                        <input type="button" value="Query" onclick="clickTreeQuery('<%=treeBidCardSearch.getTreeDealKey() %>', 'bidcard')"/>&nbsp;
                        <input type="button" value="Display" onclick="clickTreeDisplay('<%=treeBidCardSearch.getTreeDealKey() %>', 'bidcard')"/>&nbsp;
                        <input type="button" value="Delete" onclick="clickTreeDelete('<%=treeBidCardSearch.getTreeDealKey() %>', 'bidcard')"/>
                    </td>
                </tr>
            <%}%>
            </table>
		</form>
	</body>
</html>