<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.player.cache.PlayerCache" %>
<%@ page import="com.funbridge.server.player.cache.PlayerCacheMgr" %>
<%@ page import="com.funbridge.server.player.data.Player" %>
<%@ page import="com.funbridge.server.tournament.privatetournament.PrivateTournamentMgr" %>
<%@ page import="com.funbridge.server.tournament.privatetournament.data.PrivateTournament" %>
<%@ page import="com.funbridge.server.tournament.privatetournament.data.PrivateTournamentPlayer" %>
<%@ page import="com.funbridge.server.tournament.privatetournament.data.PrivateTournamentProperties" %>
<%@ page import="com.funbridge.server.ws.FBWSException" %>
<%@ page import="com.funbridge.server.ws.tournament.WSPrivateTournamentProperties" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String operation = request.getParameter("operation");
    String resultOperation = "";
    PrivateTournamentMgr tournamentMgr = ContextManager.getPrivateTournamentMgr();
    PlayerCacheMgr playerCacheMgr = ContextManager.getPlayerCacheMgr();
    int paramNbDaysToRemove = 0;

    if (operation != null) {
        if (operation.equals("processStartup")) {
            resultOperation = ""+tournamentMgr.startupTournaments();
        }
        else if (operation.equals("processFinish")) {
            resultOperation = "Nb tournament finish="+tournamentMgr.finishExpiredTournament();
        }
        else if (operation.equals("processRemovePropertiesExpired")) {
            paramNbDaysToRemove = Integer.parseInt(request.getParameter("paramNbDaysToRemove"));
            List<PrivateTournamentProperties> listRemove = tournamentMgr.removeExpiredProperties(paramNbDaysToRemove);
            resultOperation = "Nb properties expired remove ="+listRemove.size();
            for (PrivateTournamentProperties e : listRemove) {
                resultOperation += "<br>"+e.toString();
            }
        }
        else if (operation.equals("listPropertiesExpired")) {
            paramNbDaysToRemove = Integer.parseInt(request.getParameter("paramNbDaysToRemove"));
            List<PrivateTournamentProperties> listRemove = tournamentMgr.listExpiredProperties(paramNbDaysToRemove);
            resultOperation = "Nb properties expired="+listRemove.size();
            for (PrivateTournamentProperties e : listRemove) {
                resultOperation += "<br>"+e.toString();
            }
        }
        else if (operation.equals("create")) {
            WSPrivateTournamentProperties wsProp = new WSPrivateTournamentProperties();
            wsProp.name = request.getParameter("paramCreateName");
            wsProp.nbDeals = Integer.parseInt(request.getParameter("paramCreateNbDeals"));
            wsProp.startDate = Constantes.stringDateHour2Timestamp(request.getParameter("paramCreateStartDate"));
            wsProp.duration = Integer.parseInt(request.getParameter("paramCreateDuration"));
            wsProp.description = request.getParameter("paramCreateDescription");
            wsProp.recurrence = request.getParameter("paramCreateRecurrence");
            wsProp.accessRule = request.getParameter("paramCreateAccessrule");
            wsProp.password = request.getParameter("paramCreatePassword");
            wsProp.rankingType = Integer.parseInt(request.getParameter("paramCreateRankingType"));
            if (wsProp.isValid()) {
                try {
                    PrivateTournamentProperties properties = tournamentMgr.createPropertiesForFunbridge(wsProp);
                    resultOperation = "Properties created="+properties;
                } catch (FBWSException e) {
                    resultOperation = "Failed to create properties for Funbridge - wsProp="+wsProp+" - exception="+e.getMessage();
                }
            } else {
                resultOperation = "Create parameters not valid properties="+wsProp;
            }
        }
        else if (operation.equals("remove")) {
            String tourID = request.getParameter("tournamentSelected");
            PrivateTournament tour = (PrivateTournament)tournamentMgr.getTournament(tourID);
            if (tour != null) {
                try {
                    if (tournamentMgr.removeProperties(tour.getPropertiesID(), Constantes.PLAYER_FUNBRIDGE_ID)) {
                        resultOperation = "Success to remove properties for tournament="+tour;
                    } else {
                        resultOperation = "Failed to remove properties for tournament="+tour;
                    }
                } catch (Exception e) {
                    resultOperation = "Exception to remove properties for tournament="+tour+" - exception="+e.getMessage();
                }
            } else {
                resultOperation = "No tournament found with ID="+tourID;
            }
        }
        else if (operation.equals("exportMailing")) {
            String selTourID = request.getParameter("selTourID");
            if (selTourID != null) {
                PrivateTournament tour = (PrivateTournament) tournamentMgr.getTournament(selTourID);
                if (tour != null) {
                    List<PrivateTournamentPlayer> results = tournamentMgr.listTournamentPlayerForTournament(tour.getIDStr(), 0, -1);
                    if (results != null && results.size() > 0) {
                        String export = "";
                        for (PrivateTournamentPlayer result : results) {
                            Player p = ContextManager.getPlayerMgr().getPlayer(result.getPlayerID());
                            if (p != null) {
                                export += result.getRank() + "," + p.getMail() + "," + p.getNickname() + "," + result.getResult() + "<br/>";
                            }
                        }
                        resultOperation = export;
                    } else {
                        resultOperation = "No results found for tourID=" + selTourID;
                    }
                } else {
                    resultOperation = "No tournament found with id=" + selTourID;
                }
            } else {
                resultOperation = "No parameter selTourID found";
            }
        }
    }
    String paramSearch = request.getParameter("paramSearch");
    if (paramSearch == null) paramSearch = "";
    String paramAccessRule = request.getParameter("paramAccessRule");
    if (paramAccessRule == null) paramAccessRule = "";
    String paramCountryCode = request.getParameter("paramCountryCode");
    if (paramCountryCode == null) paramCountryCode = "";
    int nbDealMin = 0;
    String paramDealMin = request.getParameter("paramDealMin");
    if (StringUtils.isNumeric(paramDealMin)) {
        nbDealMin = Integer.parseInt(paramDealMin);
    }
    int nbDealMax = 0;
    String paramDealMax = request.getParameter("paramDealMax");
    if (StringUtils.isNumeric(paramDealMax)) {
        nbDealMax = Integer.parseInt(paramDealMax);
    }
    int rankingType = 0;
    String paramRankingType = request.getParameter("paramRankingType");
    if (StringUtils.isNumeric(paramRankingType)) {
        rankingType = Integer.parseInt(paramRankingType);
    }
    int offset = 0;
    int nbMax = 50;
    String paramOffset = request.getParameter("offset");
    if (paramOffset != null) {
        offset = Integer.parseInt(paramOffset);
        if (offset < 0) {
            offset = 0;
        }
    }
    PrivateTournamentMgr.ListTournamentResult ltr = tournamentMgr.listTournament(paramSearch, 0, nbDealMin, nbDealMax, rankingType, paramAccessRule, paramCountryCode, false, -1, offset, nbMax);
    List<PrivateTournament> listTour = ltr.tournamentList;
    int countTournament = ltr.count;
    int nbTournamentAvailable = tournamentMgr.countTournamentAvailable();
%>
<html>
    <head>
        <title>Funbridge Server - Administration</title>
        <script type="text/javascript">
            function clickOffset(offset) {
                document.forms["formTourPrivate"].offset.value=offset;
                document.forms["formTourPrivate"].submit();
            }
            function clickRunOperation(op) {
                if (confirm("Run operation : "+op+" ?")) {
                    document.forms["formTourPrivate"].operation.value=op;
                    document.forms["formTourPrivate"].submit();
                }
            }
            function clickRunOperationOnTournament(op, tourID, tourName) {
                if (confirm("Run operation : "+op+" on tournament "+tourName+" ?")) {
                    document.forms["formTourPrivate"].operation.value=op;
                    document.forms["formTourPrivate"].tournamentSelected.value=tourID;
                    document.forms["formTourPrivate"].submit();
                }
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION TOUR PRIVATE</h1>
        [<a href="admin.jsp">Administration</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formTourPrivate" method="post" action="adminTourPrivate.jsp">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="tournamentSelected" value=""/>
            <input type="hidden" name="offset" value="<%=offset%>"/>
            Date next process startup = <%=Constantes.timestamp2StringDateHour(tournamentMgr.getDateNextJobStartup())%>&nbsp;&nbsp;&nbsp;<input type="button" value="Run process Startup" onclick="clickRunOperation('processStartup')"><br/>
            Date next process finish = <%=Constantes.timestamp2StringDateHour(tournamentMgr.getDateNextJobFinish())%>&nbsp;&nbsp;&nbsp;Nb tournament expired and not finished = <%=tournamentMgr.countTournamentNotFinishedAndExpired()%>&nbsp;&nbsp;&nbsp;<input type="button" value="Run process Finish" onclick="clickRunOperation('processFinish')"><br/>
            Date next process remove = <%=Constantes.timestamp2StringDateHour(tournamentMgr.getDateNextJobRemove())%>&nbsp;&nbsp;&nbsp;Nb days limit : <input type="number" name="paramNbDaysToRemove" size="5" value="<%=paramNbDaysToRemove%>" >&nbsp;&nbsp;&nbsp;<input type="button" value="List properties expired" onclick="clickRunOperation('listPropertiesExpired')">&nbsp;&nbsp;&nbsp;<input type="button" value="Run process remove" onclick="clickRunOperation('processRemovePropertiesExpired')"><br/>
            Nb tournament available = <%=nbTournamentAvailable%>&nbsp;&nbsp;&nbsp;-&nbsp;&nbsp;&nbsp;Nb tournament in memory = <%=tournamentMgr.getMemoryMgr().getSize()%><br/>
            <b>Create tournament FunBridge</b><br/>
            Name = <input type="text" name="paramCreateName">&nbsp;&nbsp;&nbsp;
            Nb Deals = <input type="number" name="paramCreateNbDeals" size="4" value="5">(3 => 40)&nbsp;&nbsp;&nbsp;
            Start Date = <input type="text" name="paramCreateStartDate">(format dd/MM/yyyy - HH:mm:ss)&nbsp;&nbsp;&nbsp;
            Duration = <input type="number" name="paramCreateDuration" size="8" value="30">(Nb minutes)<br/>
            Description = <input type="text" name="paramCreateDescription">&nbsp;&nbsp;&nbsp;
            Recurrence = <input type="text" name="paramCreateRecurrence" size="10" value="once">(once, daily or weekly)&nbsp;&nbsp;&nbsp;
            Access Rule = <input type="text" name="paramCreateAccessrule" size="10" value="public">(public or password)&nbsp;&nbsp;&nbsp;
            Password = <input type="text" name="paramCreatePassword" size="10" value="">&nbsp;&nbsp;&nbsp;
            Ranking type = <input type="number" name="paramCreateRankingType" size="2" value="1">(Paire : 1, IMP : 2)<br>
            <input type="button" value="Create" onclick="clickRunOperation('create')"><br/>
            <br/><br/>
            <b>Export results for mailing</b><br/>
            tournamentID = <input type="text" name="selTourID" value="" size="50"/><input type="button" value="Export results" onclick="clickRunOperation('exportMailing')">
            <br/><br/>
            <b>Parameters for list tournament</b><br/>
            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Search = <input type="text" name="paramSearch" value="<%=paramSearch%>" size="50"/>&nbsp;&nbsp;&nbsp;-&nbsp;&nbsp;&nbsp;CountryCode = <input type="text" name="paramCountryCode" value="<%=paramCountryCode%>" size="4"/><br/>
            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Nb Deal min = <input type="number" name="paramDealMin" value="<%=nbDealMin%>" size="4"/>&nbsp;&nbsp;&nbsp;-&nbsp;&nbsp;&nbsp;Nb Deal max = <input type="number" name="paramDealMax" value="<%=nbDealMax%>" size="4"/><br/>
            <input type="button" value="Search" onclick="document.forms['formTourPrivate'].submit()"><br/><br/>
            <%if(offset > 0) {%>
            <input type="button" value="Previous" onclick="clickOffset(<%=offset - nbMax%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (listTour != null && listTour.size() >= nbMax) {%>
            <input type="button" value="Next" onclick="clickOffset(<%=offset + nbMax%>)">
            <%}%>
            Nb Tournaments for selection = <%=countTournament%><br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>ID</th><th>Name</th><th>Owner</th><th>Date</th><th>Status (StartupDone, InMemory)</th><th>Operations</th><th>Properties</th></tr>
                <% for (PrivateTournament t : listTour) {
                    PlayerCache pc = playerCacheMgr.getPlayerCache(t.getOwnerID());
                    boolean tourInMemory = tournamentMgr.getMemoryMgr().getTournament(t.getIDStr()) != null;
                %>
                <tr>
                    <td>
                        <% if (tourInMemory) {%>
                        <a href="adminGenericMemoryTournament.jsp?tourid=<%=t.getIDStr()%>&category=<%=Constantes.TOURNAMENT_CATEGORY_PRIVATE%>"><%=t.getIDStr()%></a>
                        <%} else {%>
                        <%=t.getIDStr()%>
                        <%}%>
                    </td>
                    <td><%=t.getName()%></td>
                    <td><%=t.getOwnerID()%> - <%=pc!=null?pc.pseudo:null%></td>
                    <td><%=Constantes.timestamp2StringDateHour(t.getStartDate())%> - <%=Constantes.timestamp2StringDateHour(t.getEndDate())%></td>
                    <td><%=t.isStartupDone()%> - <%=tournamentMgr.getMemoryMgr().getTournament(t.getIDStr())!=null%></td>
                    <td><input type="button" value="Remove" onclick="clickRunOperationOnTournament('remove', '<%=t.getIDStr()%>', '<%=t.getName()%>')"></td>
                    <td><%=tournamentMgr.getPrivateTournamentProperties(t.getPropertiesID())%></td>
                </tr>
                <%}%>
            </table>
        </form>
    </body>
</html>
