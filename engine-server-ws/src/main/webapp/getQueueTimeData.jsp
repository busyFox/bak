<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="com.gotogames.bridge.engineserver.ws.request.RequestServiceImpl"%>
<%
RequestServiceImpl requestService = ContextManager.getRequestService();
%><%=requestService.getMeterAverageTimeCache()%>;<%=requestService.getMeterAverageTimeNoCache()%>