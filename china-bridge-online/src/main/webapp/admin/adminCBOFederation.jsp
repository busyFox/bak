<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.common.FBConfiguration" %>
<%@ page import="com.funbridge.server.player.cache.PlayerCache" %>
<%@ page import="com.funbridge.server.player.data.Player" %>
<%@ page import="com.funbridge.server.tournament.federation.TourFederationGenerateSettingsElement" %>
<%@ page import="com.funbridge.server.tournament.federation.TourFederationMgr" %>
<%@ page import="com.funbridge.server.tournament.federation.TourFederationStatPeriodMgr" %>
<%@ page import="com.funbridge.server.tournament.federation.data.TourFederationTournament" %>
<%@ page import="com.funbridge.server.tournament.federation.data.TourFederationTournamentPlayer" %>
<%@ page import="com.funbridge.server.ws.FBWSException" %>
<%@ page import="com.funbridge.server.ws.result.ResultServiceRest" %>
<%@ page import="com.funbridge.server.ws.result.WSMainRankingPlayer" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.apache.commons.lang.math.NumberUtils" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    TourFederationStatPeriodMgr tourFederationStatPeriodMgr = ContextManager.getTourFederationStatPeriodMgr();
    TourFederationMgr tourMgr = ContextManager.getTourCBOMgr();

    String authorFilter = request.getParameter("authorFilter");
    String categoryFilter = request.getParameter("categoryFilter");
    String startDateFilterField = request.getParameter("startDateFilter");
    String endDateFilterField = request.getParameter("endDateFilter");

    String operation = request.getParameter("operation");
    String resultOperation = "";
    List<TourFederationTournament> listTourAvailable = null;
    if(tourMgr != null) {
        if (operation != null && operation.length() > 0) {
            if (operation.equals("finishOldTour")) {
                String selTourID = request.getParameter("selTourID");
                if (selTourID != null) {
                    TourFederationTournament oldTour = (TourFederationTournament) tourMgr.getTournament(selTourID);
                    if (oldTour != null) {
                        if (tourMgr.finishTournament(oldTour)) {
                            resultOperation = "Success to finish tournament=" + oldTour;
                        } else {
                            resultOperation = "Failed to finish tournament=" + oldTour;
                        }
                    } else {
                        resultOperation = "No tournament found with id=" + selTourID;
                    }
                } else {
                    resultOperation = "No parameter selTourID found";
                }
            } else if (operation.equals("generateReportFile")) {
                String selTourID = request.getParameter("selTourID");
                String sendReportMail = request.getParameter("sendReportMail");
                boolean sendMail =sendReportMail != null && sendReportMail.equalsIgnoreCase("true");
                if (selTourID != null) {
                    TourFederationTournament tour = (TourFederationTournament) tourMgr.getTournament(selTourID);
                    if (tour != null) {
                        List<TourFederationTournamentPlayer> results = tourMgr.listTournamentPlayerForTournament(tour.getIDStr(), 0, -1);
                        if (results != null && results.size() > 0) {
                            try {
                                tourMgr.generateReportForTournament(tour, results, sendMail);
                                resultOperation = "Report file successfully generated";
                            } catch (FBWSException e) {
                                resultOperation = "Failed to generate report file for tourID=" + selTourID + " - Error : " + e.getMessage();
                            }
                        } else {
                            resultOperation = "No results found for tourID=" + selTourID;
                        }
                    } else {
                        resultOperation = "No tournament found with id=" + selTourID;
                    }
                } else {
                    resultOperation = "No parameter selTourID found";
                }
            } else if (operation.equals("exportMailing")) {
                String selTourID = request.getParameter("selTourID");
                if (selTourID != null) {
                    TourFederationTournament tour = (TourFederationTournament) tourMgr.getTournament(selTourID);
                    if (tour != null) {
                        List<TourFederationTournamentPlayer> results = tourMgr.listTournamentPlayerForTournament(tour.getIDStr(), 0, -1);
                        if (results != null && results.size() > 0) {
                            String export = "";
                            for (TourFederationTournamentPlayer result : results) {
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
            } else if (operation.equals("finishAll")) {
                List toursFinished = tourMgr.processFinishTournament();
                resultOperation = "Nb tours finished : " + toursFinished.size();
            } else if (operation.equals("checkTournamentToGenerate")) {
                List<TourFederationTournament> genTour = tourMgr.checkTournamentToGenerate();
                resultOperation += "Nb tournament generated : " + genTour.size();
                for (TourFederationTournament t : genTour) {
                    resultOperation += "<br/" + t.toString();
                }
            } else if (operation.equals("executeProcessPrepare")) {
                List<TourFederationTournament> preparedTour = (List<TourFederationTournament>) tourMgr.processPrepareTournament();
                resultOperation += "Nb tournament prepared : " + preparedTour.size();
                for (TourFederationTournament t : preparedTour) {
                    resultOperation += "<br/>" + t.toString();
                }
            } else if (operation.equals("executeProcessFinish")) {
                List<TourFederationTournament> finishTour = tourMgr.processFinishTournament();
                resultOperation += "Nb tournament finished : " + finishTour.size();
                for (TourFederationTournament t : finishTour) {
                    resultOperation += "<br/>" + t.toString();
                }
            } else if (operation.equals("removeTour")) {
                String selTourID = request.getParameter("selTourID");
                if (selTourID != null) {
                    resultOperation = "Tournament to remove=" + tourMgr.getTournament(selTourID);
                    resultOperation += "<br/>Remove tournament with ID=" + selTourID + " - result=" + tourMgr.removeTournament(selTourID);
                } else {
                    resultOperation = "No parameter selTourID found";
                }
            } else if (operation.equals("createTour")) {
                Calendar calStartDate = Calendar.getInstance();
                Calendar calEndDate = (Calendar) calStartDate.clone();
                TourFederationGenerateSettingsElement settingsElement = tourMgr.buildGenerateSettingsElement();
                String startDateParam = request.getParameter("creationTourStartDate");
                String endDateParam = request.getParameter("creationTourEndDate");
                int type = Integer.parseInt(request.getParameter("creationTourType"));
                boolean free = Boolean.valueOf(request.getParameter("creationTourFree"));
                boolean endowed = Boolean.valueOf(request.getParameter("creationTourEndowed"));
                boolean special = Boolean.valueOf(request.getParameter("creationTourSpecial"));

                boolean bParamOK = true;
                if (startDateParam != null && startDateParam.length() > 0) {
                    try {
                        calStartDate.setTimeInMillis(Constantes.stringDateHour2Timestamp(startDateParam));
                    } catch (Exception e) {
                        resultOperation = "Bad format for date creation - " + startDateParam;
                        bParamOK = false;
                    }
                }
                if (bParamOK) {
                    if (endDateParam != null && endDateParam.length() > 0) {
                        try {
                            calEndDate.setTimeInMillis(Constantes.stringDateHour2Timestamp(endDateParam));
                        } catch (Exception e) {
                            resultOperation = "Bad format for end date - " + endDateParam;
                            bParamOK = false;
                        }
                    }
                }
                if (bParamOK) {
                    settingsElement.name = request.getParameter("creationTourName");
                    settingsElement.subName = request.getParameter("creationTourSubName");
                    settingsElement.startHour = calStartDate.get(Calendar.HOUR_OF_DAY) + "h" + calStartDate.get(Calendar.MINUTE);
                    settingsElement.endHour = calEndDate.get(Calendar.HOUR_OF_DAY) + "h" + calEndDate.get(Calendar.MINUTE);
                    if (NumberUtils.isNumber(request.getParameter("creationTourRegistrationDurationHour"))) {
                        settingsElement.registrationDurationHour = Integer.valueOf(request.getParameter("creationTourRegistrationDurationHour"));
                    } else {
                        settingsElement.registrationDurationHour = 24 * 7;
                    }
                    settingsElement.resultType = type;
                    settingsElement.free = free;
                    settingsElement.endowed = endowed;
                    settingsElement.special = special;
                    settingsElement.coef = Integer.valueOf(request.getParameter("coef"));
                    settingsElement.coefPF = Integer.valueOf(request.getParameter("coefPF"));
                    tourMgr.createTournament(settingsElement, calStartDate, "ADMIN_PORTAL");
                }
            } else if (operation.equals("createOrUpdateTourStat")) {
                boolean status = tourMgr.createOrUpdateTourFederationStat();
                if (status) {
                    resultOperation = "create or update TourStat done";
                } else {
                    resultOperation = "An error occurred during operation.";
                }
            } else if (operation.equals("generateMonthlyReport")) {
                String month = request.getParameter("reportMonth");
                boolean sendMail = false;
                String sendReportMail = request.getParameter("sendReportMail");
                if (sendReportMail != null && sendReportMail.equalsIgnoreCase("true")) {
                    sendMail = true;
                }
                if (month != null) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy");
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(sdf.parse(month));
                        tourMgr.generateMonthlyReport(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR), sendMail);
                        resultOperation = "monthly report successfully generated" + (sendMail ? " and sent" : "");
                    } catch (Exception e) {
                        resultOperation = "Failed to generate monthly report for month=" + month + " - Error : " + e.getMessage();
                    }
                } else {
                    resultOperation = "No parameter month found";
                }
            } else if (operation.equals("initMgr")) {
                tourMgr.init();
            } else if(operation.equals("startUpMgr")){
                tourMgr.startUp();
            } else if (operation.equals("ranking")) {
                String periodID = request.getParameter("periodID");
                if (StringUtils.isNotBlank(periodID)) {
                    ResultServiceRest.GetMainRankingResponse rankingResponse = tourMgr.getRankingFederation(new PlayerCache(), periodID, null, null, 0, 10000);
                    resultOperation = "RANK;PSEUDO;POINTS";
                    for (WSMainRankingPlayer rankingPlayer : rankingResponse.ranking) {
                        resultOperation += "<br>" + rankingPlayer.rank + ";" + rankingPlayer.playerPseudo + ";" + rankingPlayer.getStringValue();
                    }
                } else {
                    resultOperation = "periodID should not be empty";
                }
            }  else if (operation.equals("importMemoryFromFile")) {
                String strPathFile = request.getParameter("pathFile");
                long ts = System.currentTimeMillis();
                if (tourMgr.getMemoryMgr().loadMemTourFromFile(strPathFile) != null) {
                    resultOperation = "Success to load data from file="+strPathFile+" - ts="+(System.currentTimeMillis() - ts);
                } else {
                    resultOperation = "Failed to load data from file="+strPathFile+" - ts="+(System.currentTimeMillis() - ts);
                }
            } else if (operation.equals("testFormula")) {
                int nbParticipants = Integer.parseInt(request.getParameter("nbParticipants"));
                if (nbParticipants > 0) {
                    try {
                        List<TourFederationTournamentPlayer> participants = new ArrayList();
                        for(int i=1; i<=nbParticipants; i++){
                            TourFederationTournamentPlayer participant = new TourFederationTournamentPlayer();
                            participant.setRank(i);
                            participants.add(participant);
                        }
                        tourMgr.computePoints(participants);
                        for(TourFederationTournamentPlayer participant : participants){
                            resultOperation += "Rang : " + participant.getRank() + " - Points : "+participant.getPoints()+"<br/>";
                        }
                    } catch (Exception e) {
                        resultOperation = "Failed to test formula - Error : " + e.getMessage();
                    }
                } else {
                    resultOperation = "nbParticipants=0 !";
                }
            }
        }
        SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd");
        long startDateFilter = startDateFilterField != null && !startDateFilterField.isEmpty() ? sdf.parse(startDateFilterField).getTime() : 0;
        long endDateFilter = endDateFilterField != null && !endDateFilterField.isEmpty() ? sdf.parse(endDateFilterField).getTime() : 0;

        listTourAvailable = tourMgr.findAvailableTournament(tourMgr.getConfigIntValue("nbMaxDayListTournament", 8), categoryFilter, authorFilter, startDateFilter, endDateFilter);
    }
%>
<html>
    <head>
        <title>Funbridge Server - Administration CBO Federation</title>
        <script type="text/javascript">
            function loadManager() {
                document.forms["federationTournamentForm"].operation.value="loadManager";
                document.forms["federationTournamentForm"].submit();
            }
            function clickFinishOldTour(selTourID) {
                if (confirm("Finish this old tournament ?")) {
                    document.forms["federationTournamentForm"].operation.value="finishOldTour";
                    document.forms["federationTournamentForm"].selTourID.value=selTourID;
                    document.forms["federationTournamentForm"].submit();
                }
            }
            function clickRemoveTour(selTourID) {
                if (confirm("Remove this tournament with ID="+selTourID+" ?")) {
                    document.forms["federationTournamentForm"].operation.value="removeTour";
                    document.forms["federationTournamentForm"].selTourID.value=selTourID;
                    document.forms["federationTournamentForm"].submit();
                }
            }
            function clickFinishAll() {
                if (confirm("Finish all tournaments ?")) {
                    document.forms["federationTournamentForm"].operation.value="finishAll";
                    document.forms["federationTournamentForm"].submit();
                }
            }
            function clickCommand(operation) {
                if (confirm("Execute command : "+operation)) {
                    document.forms["federationTournamentForm"].operation.value=operation;
                    document.forms["federationTournamentForm"].submit();
                }
            }
            function clickGenerateReportFile(selTourID) {
                if (confirm("Generate repoprt file for this tournament ?")) {
                    document.forms["federationTournamentForm"].operation.value="generateReportFile";
                    document.forms["federationTournamentForm"].selTourID.value=selTourID;
                    if(confirm("Send again the mail containing report file ?")){
                        document.forms["federationTournamentForm"].sendReportMail.value="true";
                    }
                    document.forms["federationTournamentForm"].submit();
                }
            }
            function clickExportMailing(selTourID) {
                if (confirm("Export data for a mailing ?")) {
                    document.forms["federationTournamentForm"].operation.value="exportMailing";
                    document.forms["federationTournamentForm"].selTourID.value=selTourID;
                    document.forms["federationTournamentForm"].submit();
                }
            }
            function clickCreateTournament() {
                if (confirm("Create tournament ?")) {
                    document.forms["federationTournamentForm"].operation.value="createTour";
                    document.forms["federationTournamentForm"].submit();
                }
            }
            function clickGenerateMonthlyReport() {
                if (confirm("Generate again the monthly report file for provided month ?")) {
                    document.forms["federationTournamentForm"].operation.value="generateMonthlyReport";
                    if(confirm("Send again the mail containing the monthly report file ?")){
                        document.forms["federationTournamentForm"].sendReportMail.value="true";
                    }
                    document.forms["federationTournamentForm"].submit();
                }
            }
            function clickImportMemoryFromFile() {
                if (confirm("Load data from file ?")) {
                    document.forms["federationTournamentForm"].operation.value="importMemoryFromFile";
                    document.forms["federationTournamentForm"].submit();
                }
            }
            function clickGenerateFinancialReport() {
                if (confirm("Generate again the financial report file for provided month ?")) {
                    document.forms["federationTournamentForm"].operation.value="generateFinancialReport";
                    if(confirm("Send again the mail containing the financial report files to the ACBL ?")){
                        document.forms["federationTournamentForm"].sendReportMail.value="true";
                    }
                    document.forms["formACBL"].submit();
                }
            }
            function searchTournament(){
                document.forms["federationTournamentForm"].operation.value = "searchTournament";
                document.forms["federationTournamentForm"].submit();
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION TOURNAMENT CBO</h1>
        [<a href="admin.jsp">Administration</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr />
        <%} %>
        <form name="federationTournamentForm" method="post" action="adminCBOFederation.jsp">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="selTourID" value="">
            <input type="hidden" name="sendReportMail" value="false">

            <% if(tourMgr != null){ %>
                <b>Date next job generator tournament</b> = <%=Constantes.timestamp2StringDateHour(tourMgr.getDateNextJobGenerator())%><br/>
                <b>Date next job enable tournament</b> = <%=Constantes.timestamp2StringDateHour(tourMgr.getDateNextJobEnable())%><br/>
                <b>Date next job finish tournament</b> = <%=Constantes.timestamp2StringDateHour(tourMgr.getDateNextJobFinish())%><br/>
                <hr />

                <table border="1" style="background-color:white; float:left; margin-bottom: 1rem;  margin-right:1rem;" cellpadding="2" cellspacing="0">
                    <thead><th colspan="2">Create tournament</th></thead>
                    <tbody>
                    <tr><td>Name</td><td><input style="width:100%" type="text" name="creationTourName" size="50"/></td></tr>
                    <tr><td>Sub name</td><td><input style="width:100%" type="text" name="creationTourSubName" size="50"/></td></tr>
                    <tr><td>Start date</td><td><input style="width:100%" type="text" name="creationTourStartDate" size="30" value="<%=Constantes.timestamp2StringDateHour(System.currentTimeMillis()+(5*Constantes.TIMESTAMP_MINUTE))%>" placeholder="dd/MM/yyyy - HH:mm:ss"/></td></tr>
                    <tr><td>End date</td><td><input style="width:100%" type="text" name="creationTourEndDate" size="30" value="<%=Constantes.timestamp2StringDateHour(System.currentTimeMillis()+(65*Constantes.TIMESTAMP_MINUTE))%>" placeholder="dd/MM/yyyy - HH:mm:ss"/></td></tr>
                    <tr><td>Type</td><td><select style="width:100%" name="creationTourType"><option value="<%=Constantes.TOURNAMENT_RESULT_PAIRE%>">PAIRE</option><option value="<%=Constantes.TOURNAMENT_RESULT_IMP%>">IMP</option></select></td></tr>
                    <tr><td>Registration Duration Hour</td><td><input style="width:100%" type="text" name="creationTourRegistrationDurationHour" value="240"/></td></tr>
                    <tr><td>Catégories</td>
                        <td>
                            <select name="creationTourFree"><option value="<%=false%>">NOT FREE</option><option value="<%=true%>">FREE</option></select>
                            <select name="creationTourEndowed"><option value="<%=false%>">NOT ENDOWED</option><option value="<%=true%>">ENDOWED</option></select>
                            <select name="creationTourSpecial"><option value="<%=false%>">NOT SPECIAL</option><option value="<%=true%>">SPECIAL</option></select>
                        </td>
                    </tr>
                    <tr><td colspan="2"><input style="width:100%" type="button" value="Create tournament" onclick="clickCreateTournament()"></td></tr>
                    </tbody>
                </table>

                <table border="1" style="background-color:white; width:500px; float:left; margin-bottom: 1rem;" cellpadding="2" cellspacing="0">
                    <thead><th>Options</th></thead>
                    <tbody>
                        <tr><td><input type="button" style="width:100%" value="Init Mgr again" onclick="clickCommand('initMgr')"></td></tr>
                        <tr><td><input type="button" style="width:100%" value="Run Startup Method Mgr" onclick="clickCommand('startUpMgr')"></td></tr>
                        <tr><td><input type="button" style="width:100%" value="Check tournament to Generate" onclick="clickCommand('checkTournamentToGenerate')"></td></tr>
                        <tr><td><input type="button" style="width:100%" value="Execute process prepare" onclick="clickCommand('executeProcessPrepare')"></td></tr>
                        <tr><td><input type="button" style="width:100%" value="Execute process finish" onclick="clickCommand('executeProcessFinish')"></td></tr>
                        <tr><td><input type="text" style="width:48%" name="periodID" placeholder="yyyyMM"> <input type="button" style="width:51%" value="Show ranking" onclick="clickCommand('ranking')"></td></tr>
                        <tr><td><input type="text" style="width:48%" placeholder="Path file" name="pathFile"/>  <input type="button" style="width:51%" value="Load tournament memory from file" onclick="clickImportMemoryFromFile()"/></td></tr>
                    </tbody>
                </table>



                <hr style="clear:both;margin-top:1rem;"/>
                <div>
                    <b>List tournament available</b>
                    <span style="float:right">Nb tournaments in memory : <%=tourMgr.getMemoryMgr().getSize()%></span>
                </div>
                <hr />
                    Filtre:
                    author <input type="text" name="authorFilter" value="<%=authorFilter != null && !authorFilter.isEmpty() ? authorFilter : ""%>"/>
                    category <select name="categoryFilter">
                                <option></option>
                                <option value="FREE" <%="FREE".equals(categoryFilter) ? "selected='selected'" : "" %>>free</option>
                                <option value="ENDOWED" <%="ENDOWED".equals(categoryFilter) ? "selected='selected'" : "" %>>endowed</option>
                                <option value="SPECIAL" <%="SPECIAL".equals(categoryFilter) ? "selected='selected'" : "" %>>special</option>
                             </select>
                    startDate <input type="date" name="startDateFilter" value="<%=startDateFilterField%>" />
                    endDate <input type="date" name="endDateFilter" value="<%=endDateFilterField%>"/>
                                   <input type="button" value="search" onclick="searchTournament()" />
                <hr />
                <table border="1" style="text-align:center;background-color:white" cellpadding="2" cellspacing="0">
                    <tr><th>ID</th><th>Name</th><th>Sub name</th><th>Register Start date</th><th>Register End date</th><th>Nb player register</th><th>Start date</th><th>End date</th><th>Type</th><th>Status</th><th>Enable to play</th><th>Process prepare date</th><th>Info</th><th>Free</th><th>Endowed</th><th>Special</th><th>Coef</th><th>Coef PF</th><th>Author</th><th>Operation</th></tr>
                    <%
                        for (TourFederationTournament t : listTourAvailable) {
                            int status = tourMgr.getTournamentStatus(t, null, null);
                    %>
                    <tr>
                        <td><%=t.getIDStr()%></td>
                        <td><%=t.getName()%></td>
                        <td><%=t.getSubName() == null ? "" : t.getSubName()%></td>
                        <td><%=Constantes.timestamp2StringDateHour(t.getRegistrationStartDate())%></td>
                        <td><%=Constantes.timestamp2StringDateHour(t.getRegistrationEndDate())%></td>
                        <td><%=t.countNbPlayersRegistered()%></td>
                        <td><%=Constantes.timestamp2StringDateHour(t.getStartDate())%></td>
                        <td><%=Constantes.timestamp2StringDateHour(t.getEndDate())%></td>
                        <td><%=((t.getResultType() == 1)?"PAIRE":"IMP")%></td>
                        <td><%=status==TourFederationMgr.TOUR_STATUS_CANCELED?"CANCELED":status==TourFederationMgr.TOUR_STATUS_PLAYED?"PLAYED":status==TourFederationMgr.TOUR_STATUS_IN_PROGRESS ?"IN PROGRESS":status==TourFederationMgr.TOUR_STATUS_NO_REGISTER?"NO REGISTER":status==TourFederationMgr.TOUR_STATUS_REGISTER_ENABLED?"REGISTER ENABLED":"UNKNOWN"%></td>
                        <td><%=t.isEnableToPlay()%></td>
                        <td><%=t.getProcessPrepareDate()>0?Constantes.timestamp2StringDateHour(t.getProcessPrepareDate()):0%></td>
                        <td><%=t.getInfo()%></td>
                        <td><%=t.isFree()%></td>
                        <td><%=t.isEndowed()%></td>
                        <td><%=t.isSpecial()%></td>
                        <td><%=(t.getCoef() != 0 ? t.getCoef() : 1)%></td>
                        <td><%=(t.getCoefPF() != 0 ? t.getCoefPF() : 1)%></td>
                        <td style="word-wrap: break-word;max-width: 75px;"><%= t.getAuthor() %></td>
                        <td>
                            <% if (tourMgr.getMemoryMgr().getTournament(t.getIDStr()) != null){%>
                            <a href="adminFederationTournamentMemoryTournament.jsp?federationName=CBO&tourid=<%=t.getIDStr()%>">Memory details</a>
                            <%} else {%>
                            <input type="button" value="Remove" onclick="clickRemoveTour('<%=t.getIDStr()%>')">
                            <%}%>
                        </td>
                    </tr>
                    <% } %>
                </table>
                <hr />
                <b>List old tournaments</b><br/>
                <input type="button" value="Finish all tournaments" onclick="clickFinishAll()"><br/>
                <table border="1" style="width:100%;text-align:center;background-color:white" cellpadding="2" cellspacing="0">
                    <tr><th>ID</th><th>Name</th><th>Nb players / registered</th><th>Nb Deals</th><th>Start date</th><th>End date</th><th>Finished</th><th>Process prepare date</th><th>Info</th><th>Free</th><th>Endowed</th><th>Special</th><th>Coef</th><th>Coef PF</th><th>Author</th><th>Operation</th></tr>
                    <%
                        List<TourFederationTournament> listOldTour = tourMgr.listOldTournament(0, 50);
                        for (TourFederationTournament t : listOldTour) {%>
                        <tr>
                            <td><%=t.getIDStr()%></td>
                            <td><%=t.getName()%></td>
                            <td><%=t.getNbPlayers()%> / <%=t.getMapRegistration().size()%></td>
                            <td><%=t.getNbDeals()%></td>
                            <td><%=Constantes.timestamp2StringDateHour(t.getStartDate())%></td>
                            <td><%=Constantes.timestamp2StringDateHour(t.getEndDate())%></td>
                            <td><%=t.isFinished()%></td>
                            <td><%=Constantes.timestamp2StringDateHour(t.getProcessPrepareDate())%></td>
                            <td><%=t.getInfo()%></td>
                            <td><%=t.isFree()%></td>
                            <td><%=t.isEndowed()%></td>
                            <td><%=t.isSpecial()%></td>
                            <td><%=(t.getCoef() != 0 ? t.getCoef() : 1)%></td>
                            <td><%=(t.getCoefPF() != 0 ? t.getCoefPF() : 1)%></td>
                            <td style="word-wrap: break-word;max-width: 75px;"><%= t.getAuthor() %></td>
                            <td>
                                <%if (!t.isFinished()){%><input type="button" value="Finish it" onclick="clickFinishOldTour('<%=t.getIDStr()%>')"/><%}%>
                            </td>
                        </tr>
                    <%}%>
                </table>
            <% } %>
        </form>
    </body>
</html>
