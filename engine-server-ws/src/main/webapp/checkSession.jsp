<%@page import="com.gotogames.bridge.engineserver.common.EngineConfiguration"%>
<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="com.gotogames.bridge.engineserver.session.EngineSessionMgr"%>
<%@page import="java.util.List"%>
<%@page import="com.gotogames.common.session.Session"%>
<%
EngineSessionMgr sessionMgr = ContextManager.getSessionMgr();
List<Session> listSession = sessionMgr.getAllCurrentSession();
String check = "KO";
int nbUserMin = EngineConfiguration.getInstance().getIntValue("check.nbUserMin", 0);
if ((nbUserMin > 0) && (listSession.size() >= nbUserMin)) {
	check = "OK";
}
%><%=check %>