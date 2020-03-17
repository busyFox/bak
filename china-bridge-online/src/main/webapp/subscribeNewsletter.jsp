<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.player.PlayerMgr"%>
<%
PlayerMgr playerMgr = ContextManager.getPlayerMgr();
String message = "";
if (playerMgr != null) {
	String email = request.getParameter("email");
	String plaid = request.getParameter("plaid");
	String enable = request.getParameter("enable");
	if ((email != null && email.length() > 0) && (plaid != null && plaid.length() > 0) && (enable != null && enable.length() > 0)) {
		try {
			long playerID = Long.parseLong(plaid);
			boolean newsletterEnable = enable.equals("1");
			if (playerMgr.setPlayerNewsletter(playerID, email, newsletterEnable)) {
				message = "OK";
			} else {
				message = "OPERATION_FAILED";
			}
		} catch (Exception e) {
			message = "EXCEPTION";
		}
	} else {
		message = "PARAMETERS_NOT_VALID";
	}
} else {
	message = "ERROR_SERVER";
}
%><%=message%>