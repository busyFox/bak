<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.presence.PresenceMgr"%>
<%
PresenceMgr presenceMgr = ContextManager.getPresenceMgr();
int nbSession = -1;
if (presenceMgr != null) {
	nbSession = presenceMgr.getNbSession();
}
%><%=nbSession%>