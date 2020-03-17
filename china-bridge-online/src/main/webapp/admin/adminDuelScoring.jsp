<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.ws.result.ResultServiceRest" %>
<%@ page import="com.funbridge.server.ws.result.ResultServiceRestImpl" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String error = "";
    ResultServiceRestImpl resultServ = ContextManager.getResultService();
    List<ResultServiceRest.PlayerDuelScoring> podium = new ArrayList();

    if (operation != null) {
        long rivalId = request.getParameter("rivalId") == null || request.getParameter("rivalId").isEmpty() || request.getParameter("rivalId") == "0" ? -2 : Long.parseLong(request.getParameter("rivalId"));
        if (operation.equals("bestScoreEver")) {
            ResultServiceRest.GetDuelBestScoreParam param = new ResultServiceRest.GetDuelBestScoreParam();
            param.rivalId = rivalId;
            podium = resultServ.processGetDuelBestScoreEver(param).podium;
        }
        else  if (operation.equals("bestScoreMonthly")) {
            if(request.getParameter("periodId").isEmpty()){
                error = "Missing period";
            }else{
                ResultServiceRest.GetDuelBestScoreMonthlyParam param = new ResultServiceRest.GetDuelBestScoreMonthlyParam();
                param.rivalId = rivalId;
                param.periodId = request.getParameter("periodId");
                podium = resultServ.processGetDuelBestScoreMonthly(param).podium;

            }
        }  if (operation.equals("bestScorWeekly")) {
            if(request.getParameter("startDate").isEmpty() || request.getParameter("endDate").isEmpty()){
                error = "Missing Date";
            }else{
                ResultServiceRest.GetDuelBestScoreWeeklyParam param = new ResultServiceRest.GetDuelBestScoreWeeklyParam();
                param.rivalId = rivalId;
                param.startDate = request.getParameter("startDate").replaceAll("-", "");
                param.endDate = request.getParameter("endDate").replaceAll("-", "");
                podium = resultServ.processGetDuelBestScoreWeekly(param).podium;
            }
        }
    }
%>
<html>
    <head>
        <title>Funbridge Server - Duel Scoring</title>
        <script type="text/javascript">
            function clickRunOperation(operation) {
                if (confirm("Run operation "+operation+" ?")) {
                    document.forms["formDuel"].operation.value = operation;
                    document.forms["formDuel"].submit();
                }
            }
        </script>
    </head>
    <body>
    <h1>ADMINISTRATION DUEL SCORING</h1>
    [<a href="adminDuel.jsp">Administration Duel</a>]
    <br/><br/>
    <%=!error.isEmpty() ? "ERREUR: " + error : ""%>
    <hr/>
    <form name="formDuel" method="post" action="adminDuelScoring.jsp">
        <input type="hidden" name="operation" value=""/>
        RivalId : <input type="number" name="rivalId"  value='<%=request.getParameter("rivalId")%>'/> (default: Argine)<br />
        Period month <input type="input" name="periodId" value='<%=request.getParameter("periodId") == null ? "" : request.getParameter("periodId")%>'/> (yyyyMM)<br />
        Date Week <input type="date" name="startDate" value='<%=request.getParameter("startDate")%>'/> - <input type="date" name="endDate" value='<%=request.getParameter("endDate")%>'/> (yyyyMMdd)
        <br />
        <input type="button" value="Best Score Ever" onclick="clickRunOperation('bestScoreEver')">
        <input type="button" value="Best Score Monthly" onclick="clickRunOperation('bestScoreMonthly')">
        <input type="button" value="Best Score Weekly" onclick="clickRunOperation('bestScorWeekly')">
        <br/>
        <br/>
        <h2>Podium</h2>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><th>ID</th><th>Pseudo</th><th>Score</th></tr>
            <% for (ResultServiceRest.PlayerDuelScoring e : podium) {%>
            <tr>
                <td><%=e.playerId%></td>
                <td><%=e.pseudo%></td>
                <td><%=e.score%></td>
            </tr>
            <%}%>
        </table>
    </form>
    </body>
</html>
