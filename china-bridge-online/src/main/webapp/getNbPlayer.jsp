<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.player.PlayerMgr"%>
<%
PlayerMgr playerMgr = ContextManager.getPlayerMgr();
int nbPlayer = 0;
if (playerMgr != null) {
	nbPlayer = playerMgr.getNbPlayer();
}
%><%=nbPlayer%>