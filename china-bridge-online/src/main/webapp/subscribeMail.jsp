<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.player.PlayerMgr"%>
<%
PlayerMgr playerMgr = ContextManager.getPlayerMgr();
String message = "";
if (playerMgr != null) {
	String email = request.getParameter("email");
	String plaid = request.getParameter("plaid");
	String mailCR = request.getParameter("CR");
	String mailNL = request.getParameter("NL");
	String mailBD = request.getParameter("BD");
	if ((email != null && email.length() > 0) &&
		(plaid != null && plaid.length() > 0) &&
		(mailCR != null && mailCR.length() == 1) &&
		(mailNL != null && mailNL.length() == 1) &&
		(mailBD != null && mailBD.length() == 1)) {
		try {
			long playerID = Long.parseLong(plaid);
			boolean mailCREnable = mailCR.equals("1");
			boolean mailNLEnable = mailNL.equals("1");
			boolean mailBDEnable = mailBD.equals("1");
			if (playerMgr.setPlayerMailEnable(playerID, email, mailNLEnable, mailCREnable, mailBDEnable)) {
				message = "OK";
			} else {
				message = "OPERATION_FAILED";
			}
		} catch (Exception e) {
			message = "EXCEPTION";
		}
	} else {
		message = "PARAMETERS_NOT_VALID : email="+email+" - plaid="+plaid+" - mailCR="+mailCR+" - mailNL="+mailNL+" - mailBD="+mailBD;
	}
} else {
	message = "ERROR_SERVER";
}
%><%=message%>