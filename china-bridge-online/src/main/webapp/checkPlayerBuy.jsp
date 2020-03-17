<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.player.data.Player"%>
<%
// check if player exist and if player has already buy deals
// 0 : player not found
// 1 : player found, not buy
// 2 : player found and buy deals
long plaID = 0;
String strPlaID = request.getParameter("plaID");
String strEmail = request.getParameter("email");
Player p = null;
int result = 0;
if (strPlaID != null) {
	try {
		plaID = Long.parseLong(strPlaID);
		p = ContextManager.getPlayerMgr().getPlayer(plaID);
		if (p!=null) {
			if (ContextManager.getStoreMgr().getNbTransactionsForPlayer(p.getID(), false) > 0) {
				result = 2;
			} else {
				result = 1;
			}
		}
	} catch (Exception e) {}
} else if (strEmail != null) {
	p = ContextManager.getPlayerMgr().getPlayerByMail(strEmail);
	if (p!=null) {
		if (ContextManager.getStoreMgr().getNbTransactionsForPlayer(p.getID(), false) > 0) {
			result = 2;
		} else {
			result = 1;
		}
	}
}
%><%=result%>