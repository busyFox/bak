<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.ws.engine.EngineService" %>
<%@ page import="com.funbridge.server.ws.engine.PlayGameStepData" %>
<%@ page import="java.util.Collection" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String paramOperation = request.getParameter("operation");
    String resultOperation = "";
    EngineService engineService = ContextManager.getEngineService();
    if (paramOperation != null) {
        if (paramOperation.equals("clearOldRequest")) {

            int nbRemove = engineService.clearOldStepData(System.currentTimeMillis() - 10*60*1000);
            resultOperation = "clearOldRequest operation - nbRemove="+nbRemove;
        }
    }
    Collection<PlayGameStepData> colReq = engineService.getMapStepData().values();
%>
<html>
    <head>
        <title>Funbridge Server - Administration Player</title>
        <script type="text/javascript">
            function clickClearOldRequest() {
                if (confirm("Clear old requests ?")) {
                    document.forms["formEngine"].operation.value="clearOldRequest";
                    document.forms["formEngine"].submit();
                }
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION ENGINE</h1>
        <a href="admin.jsp">Administration</a><br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= paramOperation%></b>
        <br/>Result = <%=resultOperation %>
        <%} %>
        <form name="formEngine" method="post" action="adminEngine.jsp">
            <input type="hidden" name="operation" value=""/>
            Nb request waiting : <%=colReq.size()%>
            <br/>
            <input type="button" value="Clear old requests" onclick="clickClearOldRequest()">
            <br/><br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>asyncID</th><th>Timestamp</th><th>requestType</th><th>param</th></tr>
                <% for(PlayGameStepData e : colReq) {%>
                <tr>
                    <td><%=e.asyncID%></td>
                    <td><%=Constantes.timestamp2StringDateHour(e.timestamp)%></td>
                    <td><%=e.requestType%></td>
                    <td><%=e.param%></td>
                </tr>
                <%}%>
            </table>
        </form>
    </body>
</html>
