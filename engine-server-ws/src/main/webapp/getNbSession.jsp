<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="com.gotogames.bridge.engineserver.user.UserVirtualMgr"%>
<%
UserVirtualMgr userMgr = ContextManager.getUserMgr();
int nbUser = -1;
if (userMgr != null) {
    nbUser = userMgr.getNbUser();
}
%><%=nbUser%>
