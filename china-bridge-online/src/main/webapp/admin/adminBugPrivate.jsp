<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.tournament.privatetournament.PrivateTournamentMgr" %>
<%@ page import="com.funbridge.server.tournament.privatetournament.data.PrivateTournament" %>
<%@ page import="com.funbridge.server.tournament.privatetournament.data.PrivateTournamentProperties" %>
<%@ page import="org.springframework.data.mongodb.core.query.Criteria" %>
<%@ page import="org.springframework.data.mongodb.core.query.Query" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    PrivateTournamentMgr tournamentMgr = ContextManager.getPrivateTournamentMgr();
    String operation = request.getParameter("operation");
    String resultOperation = "";
    Query q = Query.query(Criteria.where("recurrence").is("weekly").andOperator(Criteria.where("tsSetRemove").is(0)));
    List<PrivateTournamentProperties> propertiesList = tournamentMgr.getMongoTemplate().find(q, PrivateTournamentProperties.class);
    if (operation != null) {
        if (operation.equals("generate")) {
            String propertiesID = request.getParameter("propertiesID");
            PrivateTournamentProperties properties = tournamentMgr.getPrivateTournamentProperties(propertiesID);
            PrivateTournament privateTournament = tournamentMgr.createPrivateTournament(properties, false);
            resultOperation = "Create tournament="+privateTournament;
        }

        else if (operation.equals("generateAll")) {
            int nbTourGenerate = 0, nbFailed = 0;
            for (PrivateTournamentProperties p : propertiesList) {
                if (tournamentMgr.getNextTournamentForProperties(p.getIDStr()) == null) {
                    if (tournamentMgr.createPrivateTournament(p, false) != null) {
                        nbTourGenerate++;
                    } else {
                        nbFailed++;
                    }
                }
            }
            resultOperation = "Properties list size="+propertiesList.size()+" - nbTourGenerate="+nbTourGenerate+" - nbFailed="+nbFailed;
        }
    }



%>
<html>
    <head>
        <title>Bug Private</title>
        <script type="text/javascript">
            function clickRunOperationOnTournament(op, propertiesID) {
                if (confirm("Run operation : "+op+" on propertiesID "+propertiesID+" ?")) {
                    document.forms["formTourPrivate"].operation.value=op;
                    document.forms["formTourPrivate"].propertiesID.value=propertiesID;
                    document.forms["formTourPrivate"].submit();
                }
            }
        </script>
    </head>
    <body>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formTourPrivate" method="post" action="adminBugPrivate.jsp">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="propertiesID" value=""/>
            <input type="button" value="Generate All" onclick="clickRunOperationOnTournament('generateAll', 'all')">
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>ID</th><th>Name</th><th>Next startDate</th><th>Tournament</th><th>Operation</th></tr>
                <%
                    for (PrivateTournamentProperties p : propertiesList) { %>
                <tr>
                    <td><%=p.getIDStr()%></td><td><%=p.getName()%></td>
                    <td><%=Constantes.timestamp2StringDateHour(tournamentMgr.getNextStartDateForProperties(p, false))%></td>
                    <td><%=tournamentMgr.getNextTournamentForProperties(p.getIDStr())%></td>
                    <td><input type="button" value="Generate" onclick="clickRunOperationOnTournament('generate', '<%=p.getIDStr()%>')"></td>
                </tr>
                    <%}
                %>
            </table>
        </form>
    </body>
</html>
