
<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="com.gotogames.bridge.engineserver.cache.TreeMgr"%>
<%@page import="com.gotogames.bridge.engineserver.cache.TreeBidCard"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="com.gotogames.common.tools.StringTools"%>
<%@page import="com.gotogames.common.bridge.BridgeConventions"%>
<%@page import="com.gotogames.bridge.engineserver.cache.TreeBidInfo"%><html>
<%
TreeMgr treeMgr = ContextManager.getTreeMgr();
String treeKey = request.getParameter("treekey");
String treeType = request.getParameter("type");
SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
String operation = request.getParameter("operation");
String paramOperation = "";
String resultOperation = "";
String treeDeal = "", treeOptions = "";
long treeTSLastConsult = 0, treeTSLastAdd = 0;
int treeNbNode = 0;
boolean bTreeFound = false;
if (treeType.equals("bidinfo")) {
	TreeBidInfo tree = treeMgr.getTreeBidInfo(treeKey);
	if (tree != null) {
		bTreeFound = true;
		treeDeal = tree.getDeal();
		treeOptions = tree.getOptions();
		treeTSLastAdd = tree.getTsLastAdd();
		treeTSLastConsult = tree.getTsLastConsult();
		treeNbNode = tree.getNbNode();
	}
} else if (treeType.equals("bidcard")) {
	TreeBidCard tree = treeMgr.getTreeBidCard(treeKey);
	if (tree != null) {
		bTreeFound = true;
		treeDeal = tree.getDeal();
		treeOptions = tree.getOptions();
		treeTSLastAdd = tree.getTsLastAdd();
		treeTSLastConsult = tree.getTsLastConsult();
		treeNbNode = tree.getNbNode();
	}
}


if (operation != null) {
	if (operation.equals("query")) {
		String queryConv = request.getParameter("queryConv");
		String queryGame = request.getParameter("queryGame");
		String queryType = request.getParameter("queryType");
		if (queryConv == null || queryGame == null || queryType == null) {
			resultOperation = "query parameter is null !";
			paramOperation = queryConv + " - " + queryGame + " - " + queryType;
		} else {
			int queryConvSel = Integer.parseInt(queryConv);
			if (queryConvSel >= 0 && queryConvSel <= 5) {
				queryConv = StringTools.strToHexa(new String(BridgeConventions.getConventionByteForProfil(queryConvSel)));
			} else {
				queryConv = request.getParameter("queryConvOther");
			}
			paramOperation = queryConv + " - " + queryGame + " - " + queryType;
			long ts1 = System.currentTimeMillis();
			resultOperation = treeMgr.getCacheData(
				treeDeal+";"+
				treeOptions+";"+
				queryConv+";"+
				queryGame+";"+
				queryType);
			long ts2 = System.currentTimeMillis();
			resultOperation += " TS="+(ts2-ts1);
		}
	}
}
%>
	<head>
		<title>EngineServer - Administration Cache</title>
		<script type="text/javascript">
		function clickRunQuery() {
			document.forms["formCacheTreeQuery"].operation.value="query";
			document.forms["formCacheTreeQuery"].submit();
		}
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION TREE BID CARD</h1>
		[<a href="adminCache.jsp">Back to cache administration</a>]
		<br/>
		<%
		if (!bTreeFound){ %>
		<b>No tree found for key=<%=treeKey %></b><br/>
		<%}else{ %>
		Tree Key = <%=treeKey %><br/>
		Tree Deal = <%=treeDeal %><br/>
		Tree Options = <%=treeOptions %><br/>
		Nb Node = <%=treeNbNode %>
		<br/>
		Date last consultation = <%=sdf.format(new Date(treeTSLastConsult)) %> - 
		Date last add = <%=sdf.format(new Date(treeTSLastAdd)) %>
		<br/>
		[<a href="adminCacheTreeDisplay.jsp?type=<%=treeType %>&treekey=<%=treeKey %>">Display</a>]
		<hr width="95%"/>
		<%if (operation != null){ %>
		<b>Operation executed : <%=operation %> - parameters=<%=paramOperation %></b><br/>
		Operation result = <%=resultOperation %><br/><br/>
		<%} %>
		<form name="formCacheTreeQuery" action="adminCacheTree.jsp" method="post">
		<input type="hidden" name="operation" value=""/>
		<input type="hidden" name="treekey" value="<%=treeKey %>"/>
		<input type="hidden" name="type" value="<%=treeType %>"/>
		<input type="hidden" name="selection" value=""/>
		<b>Query parameters :</b><br/>
		Convention selection = <input type="radio" name="queryConv" value="0" checked="checked"> Novice - <input type="radio" name="queryConv" value="1"> Club - <input type="radio" name="queryConv" value="2"> Competition - <input type="radio" name="queryConv" value="3"> USA - <input type="radio" name="queryConv" value="4"> Acol - <input type="radio" name="queryConv" value="5"> Polon - <input type="radio" name="queryConv" value="6"> Other : <input type="text" name="queryConvOther" /> 
		<br/>Game  = <input type="text" name="queryGame"size="60"/>
		<%if (treeType.equals("bidcard")){ %>
		<br/>Type = <input type="radio" name="queryType" value="0" checked="checked"> Bid - <input type="radio" name="queryType" value="1"> Card<br/>
		<%} else {%>
		<input type="hidden" name="queryType" value="2"/>
		<%} %>
		<br/><input type="button" value="Run Query" onclick="clickRunQuery()"/>
		<br/><br/>
		</form>
		<%} %>
	</body>
</html>