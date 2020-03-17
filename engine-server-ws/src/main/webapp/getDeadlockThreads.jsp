<%@ page import="com.gotogames.bridge.engineserver.common.ContextManager" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    int mode = 0; /* 0=only log, 1=log and display, 2=only display */
    String paramMode = request.getParameter("mode");
    if (paramMode != null) {
        if (StringUtils.isNumeric(paramMode)) {
            try {
                mode = Integer.parseInt(paramMode);
            }catch (Exception e) {}
        }
    }
    boolean logIt = false;
    if (mode == 0 || mode == 1) {
        logIt = true;
    }
    List<String> infoDL = ContextManager.getLogStatMgr().getInfoDeadlockedThreads(logIt);
%>
<% if (mode == 0) {%>
OK
<%} else if (mode == 1 || mode == 2){
    for (String e : infoDL) {%>
<%=e%><br/>
<%}}%>