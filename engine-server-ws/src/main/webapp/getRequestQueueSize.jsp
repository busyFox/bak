<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="com.gotogames.bridge.engineserver.request.QueueMgr"%>
<%
QueueMgr mgr = ContextManager.getQueueMgr();
int queueSize = mgr.getQueueSize();
int queueEngine = mgr.getQueueSizeEngine();
int queueNoEngine = mgr.getQueueSizeNoEngine();
%><%=queueNoEngine%>;<%=queueEngine%>;<%=queueSize%>