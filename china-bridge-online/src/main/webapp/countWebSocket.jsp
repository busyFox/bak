<%@page import="com.funbridge.server.presence.FBSession"%>
<%@page import="com.gotogames.common.session.Session"%>
<%@page import="java.util.List"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.presence.PresenceMgr"%>
<%
int nbWebSocket = 0;
PresenceMgr presenceMgr = ContextManager.getPresenceMgr();
List<Session> listSession = presenceMgr.getAllCurrentSession();
for (Session s : listSession) {
	if (s instanceof FBSession) {
		FBSession fbs = (FBSession) s;
		if (fbs.getWebSocket() != null) {
        	nbWebSocket++;
        }
	}
}
%><%=nbWebSocket%>