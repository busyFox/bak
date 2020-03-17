<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="com.gotogames.bridge.engineserver.user.UserVirtual"%>
<%@page import="com.gotogames.bridge.engineserver.user.UserVirtualMgr"%>
<%@page import="java.util.Collection"%>
<%
UserVirtualMgr userVirtualMgr = (UserVirtualMgr)(ContextManager.getContext().getBean("userVirtualMgr"));
int nbEngine = 0;
if (userVirtualMgr != null) {
	Collection<UserVirtual> users = userVirtualMgr.getListUser();
	for (UserVirtual u : users) {
        if (u.isEngine()) {
			nbEngine++;
		}
	}
}
%><%=nbEngine%>
