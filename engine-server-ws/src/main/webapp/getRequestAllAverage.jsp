<%@page import="com.gotogames.common.tools.NumericalTools"%>
<%@page import="com.gotogames.bridge.engineserver.ws.request.RequestServiceImpl"%>
<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%
RequestServiceImpl mgr = ContextManager.getRequestService();
double rate1 = NumericalTools.round(mgr.getMeterRequestAll().getOneMinuteRate(),2);
double rate5 = NumericalTools.round(mgr.getMeterRequestAll().getFiveMinuteRate(),2);
double rate15 = NumericalTools.round(mgr.getMeterRequestAll().getFifteenMinuteRate(),2);
%><%=rate1%>;<%=rate5%>;<%=rate15%>