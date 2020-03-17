
<%@page import="com.gotogames.bridge.engineserver.cache.TreeMgr"%>
<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.gotogames.bridge.engineserver.cache.TreeBidInfo"%>
<%@page import="com.gotogames.bridge.engineserver.cache.TreeBidCard"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@ page import="com.gotogames.common.bridge.GameBridgeRule" %>
<%@ page import="com.gotogames.common.bridge.BridgeTransformData" %>
<%@ page import="java.util.ArrayList" %>
<html>
<%
TreeMgr treeMgr = ContextManager.getTreeMgr();
String treeKey = request.getParameter("treekey");
String treeType = request.getParameter("type");
SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
String treeDeal = "", treeOptions = "";
long treeTSLastConsult = 0, treeTSLastAdd = 0;
int treeNbNode = 0;
boolean bTreeFound = false;
List<String> listData=new ArrayList<String>();
int CONV_LENGTH = 106;
if (treeType.equals("bidinfo")) {
	TreeBidInfo tree = treeMgr.getTreeBidInfo(treeKey);
	if (tree != null) {
		bTreeFound = true;
		treeDeal = tree.getDeal();
		treeOptions = tree.getOptions();
		treeTSLastAdd = tree.getTsLastAdd();
		treeTSLastConsult = tree.getTsLastConsult();
		treeNbNode = tree.getNbNode();
		listData = tree.listAll();
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
		List<String>listCryptData = tree.listAll();
		if (listCryptData != null) {
            for (String s : listCryptData) {
                String g = "";
                if (s.length() > CONV_LENGTH) {
                    int idx = 0;
                    while (idx < CONV_LENGTH) {
                        g += Character.toString(s.charAt(idx));
                        idx++;
                        idx++;
                    }
                    g += ";";
                    while (idx < s.length()) {
                        if (GameBridgeRule.isEndBids(g)) {
                            g += BridgeTransformData.convertBridgeCard2String(s.charAt(idx));
                        } else {
                            g += BridgeTransformData.convertBridgeBid2String(s.charAt(idx));
                        }
                        idx++;
                        idx++;
                    }
                } else {
                    g = "Data not valid !";
                }
                listData.add(g);
            }
        }
	}
}
%>
	<head>
		<title>EngineServer - Administration Cache</title>
	</head>
	<body>
		<h1>ADMINISTRATION TREE BID CARD</h1>
		[<a href="adminCache.jsp">Back to cache administration</a>]
		<br/>
		<%if (!bTreeFound){ %>
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
		[<a href="adminCacheTree.jsp?type=<%=treeType %>&treekey=<%=treeKey %>">Query tree</a>]
		<hr width="95%"/>
		<b>Tree Data</b><br/>
		<%if (listData == null || listData.isEmpty()){ %>
		Tree is empty !
		<%} else {
		String sTextArea="";
		for (String s : listData) {
        	sTextArea+=s+"\r\n";
		}%>
		<textarea readonly="readonly" cols="120" rows="35" wrap="off"><%=sTextArea%></textarea>
		<%}
		}
		%>
	</body>
</html>