<%@ page import="com.gotogames.common.tools.NetTools" %><%
    int nbEngine = 0;
    try {
        String tempNbEngine = NetTools.getURLContent("http://ggfront02p.csgames.net:8080/engine-server-ws/getNbEngine.jsp", 5 * 1000, 10);
        nbEngine = Integer.parseInt(tempNbEngine);
    } catch (Exception e) {

    }
%><%=nbEngine%>