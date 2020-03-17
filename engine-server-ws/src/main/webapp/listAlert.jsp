<%@ page import="com.gotogames.bridge.engineserver.request.data.QueueData" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="com.gotogames.bridge.engineserver.common.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    AlertMgr alertMgr = ContextManager.getAlertMgr();
    String operation = request.getParameter("operation");
    String paramOperation = "";
    String resultOperation = "";
    int engineSelection = 0;
    String paramEngine = request.getParameter("engine");
    if (paramEngine != null && paramEngine.length() > 0) {
        try {
            engineSelection = Integer.parseInt(paramEngine);
        } catch (Exception e) {}
    }
    if (operation != null && operation.length() > 0) {
        if (operation.equals("sendMailReport")) {
            String reportDate = request.getParameter("reportDate");
            if (alertMgr.sendMailReport(reportDate)) {
                resultOperation = "Success to sendMailReport for date="+reportDate;
            } else {
                resultOperation = "Failed to sendMailReport for date="+reportDate;
            }
        } else if (operation.equals("addManuelAlert")) {
            QueueData data = new QueueData();
            data.timestamp = System.currentTimeMillis();
            String testAlertMessage = request.getParameter("testAlertMessage");
            String testAlertRequest = request.getParameter("testAlertRequest");
            String testAlertResult = request.getParameter("testAlertResult");
            if (testAlertResult == null || testAlertResult.length() == 0) {
                data.resultValue = "NO_RESULT PA";
            } else {
                data.resultValue = testAlertResult;
            }
            if (testAlertRequest == null || testAlertRequest.length() == 0) {
                String strRequest = "WASSNENWESWWENWENWSNWWSSSNSNNEEWNSWEWSWWNEENSSWEENENES;000001DD00;01000000001000001001011111101101110000102100101123310;PA1NPA3CPA3DPA3NPA;0";
                data.setRequest(strRequest);
            } else {
                data.setRequest(testAlertRequest);
            }
            AlertData alertData = alertMgr.saveAlert("TEST - "+testAlertMessage, "TEST", data);
            if (alertData != null) {
                resultOperation = "Success to save alert="+alertData+" - queueData="+data;
            } else {
                resultOperation = "Failed to save alert ! queueData="+data;
            }
        } else if (operation.equals("removeAlerts")) {
            paramOperation = request.getParameter("selection");
            if (paramOperation != null && paramOperation.length() > 0) {
                String tabAlert[] = paramOperation.split(";");
                alertMgr.removeAlerts(Arrays.asList(tabAlert));
                resultOperation = "Selection to remove="+tabAlert.length;
            } else {
                resultOperation = "Selection is empty";
            }
        } else if (operation.equals("removeAlertsForEngine")) {
            if (engineSelection > 0) {
                alertMgr.removeAlertsForEngine(Arrays.asList(engineSelection));
                resultOperation = "Alerts with engine="+engineSelection;
                engineSelection = 0;
            } else {
                resultOperation = "No engine selection !";
            }
        } else if (operation.equals("processCheckAlert")) {
            resultOperation = "Process checkAlert - nb alert="+alertMgr.processCheckAlert();
        }
    }
    int idxStart = 0;
    int nbMax = 50;
    String paramOffset = request.getParameter("listoffset");
    if (paramOffset != null) {
        idxStart = Integer.parseInt(paramOffset);
        if (idxStart < 0) {
            idxStart = 0;
        }
    }

    List<AlertData> listAlerts = alertMgr.listAlert(engineSelection, idxStart, nbMax);

    String strAlertesVersion = "";
    List<AlertDataGroupMapping> alertDataGroupMappingList = alertMgr.countAlertGroupByEngine();
    for (AlertDataGroupMapping e : alertDataGroupMappingList) {
        if (strAlertesVersion.length() > 0) {
            strAlertesVersion += " / ";
        }
        strAlertesVersion += e._id+":"+e.nb;
    }
%>
<html>
    <head>
        <title>ENGINE - Alerts</title>
        <script type="text/javascript">
            function clickSendMailReport(){
                if (confirm("Send mail report ?")){
                    document.forms["formAlert"].operation.value="sendMailReport";
                    document.forms["formAlert"].submit();
                }
            }
            function clickAddManuelAlert(){
                if (confirm("Add manuel alert")){
                    document.forms["formAlert"].operation.value="addManuelAlert";
                    document.forms["formAlert"].submit();
                }
            }
            function clickShowPBN(e) {
                var subRow = e.parentNode.parentNode.nextElementSibling;
                if (subRow.style.display === 'none') {
                    subRow.style.display = 'table-row';
                    e.value = 'Hide PBN';
                } else {
                    subRow.style.display = 'none';
                    e.value = 'Show PBN';
                }

            }
            function clickListOffset(offset) {
                document.forms["formAlert"].listoffset.value = offset;
                document.forms["formAlert"].submit();
            }
            function clickRemoveSelection() {
                selID = "";
                for (i=1; i <= document.forms["formAlert"].nbData.value; i++) {
                    if (document.getElementById("sel_"+i).checked) {
                        if (selID != "") {
                            selID += ";";
                        }
                        selID += document.getElementById("sel_"+i).value;
                    }
                }
                if (confirm("Remove all selected request ?")) {
                    document.forms["formAlert"].operation.value="removeAlerts";
                    document.forms["formAlert"].selection.value=selID;
                    document.forms["formAlert"].submit();
                }
            }
            function clickRemoveForEngine() {
                if (document.forms["formAlert"].engine.value == "") {
                    alert("No engine selection !");
                } else if (confirm("Remove all alerts with engine version="+document.forms["formAlert"].engine.value)){
                    document.forms["formAlert"].operation.value="removeAlertsForEngine";
                    document.forms["formAlert"].submit();
                }
            }
            function clickSelectAll() {
                for (i=1; i <= document.forms["formAlert"].nbData.value; i++) {
                    if(!document.getElementById("sel_"+i).checked) {
                        document.getElementById("sel_"+i).checked = true;
                    }
                }
            }
            function clickSelectEngine() {
                document.forms["formAlert"].listoffset.value = 0;
                document.forms["formAlert"].submit();
            }
            function clickProcessCheckAlert() {
                document.forms["formAlert"].operation.value="processCheckAlert";
                document.forms["formAlert"].submit();
            }
        </script>
    </head>
    <body>
        <h1>LIST ALERTS</h1>
        <a href="admin/admin.jsp">Administration</a><br/><br/>
        <%if (operation != null){ %>
        <b>Operation executed : <%=operation %> - parametres=<%=paramOperation %></b><br/>
        Operation result : <%=resultOperation %>
        <%} %>
        <br/>
        <hr width="95%"/>
        <form name="formAlert" action="listAlert.jsp" method="post">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="selection" value=""/>
            <input type="hidden" name="nbData" value="<%=listAlerts.size() %>"/>
            <input type="hidden" name="listoffset" value="<%=idxStart%>"/>
            Send Mail Report for date : <input type="text" name="reportDate" value="<%=Constantes.timestamp2StringDate(System.currentTimeMillis() - (24*60*60*1000))%>">&nbsp;&nbsp;&nbsp;
            <input type="button" value="Test sendMailReport" onclick="clickSendMailReport()"><br/>
            Add manuel alert : request=<input type="text" name="testAlertRequest" size="70"> - resultValue=<input type="text" name="testAlertResult" size="10"> - message=<input type="text" name="testAlertMessage" size="20">
            &nbsp;&nbsp;&nbsp;<input type="button" value="Add test alert" onclick="clickAddManuelAlert()"><br/>
            <hr width="95%"/>
            Nb total alerts : <%=alertMgr.countAllAlert()%> - Nb alerts for the last 24h : <%=alertMgr.countAlertBetweenDate(System.currentTimeMillis() - 24*60*60*1000, System.currentTimeMillis())%><br/>
            Nb alerts / engine version : <%=strAlertesVersion%><br/>
            Engine Selection : <input type="text" name="engine" value="<%=engineSelection>0?engineSelection:""%>">&nbsp;&nbsp;&nbsp;<input type="button" value="Search with engine" onclick="clickSelectEngine()">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Remove for engine" onclick="clickRemoveForEngine()"><br/>
            <input type="button" value="Remove selection" onclick="clickRemoveSelection()">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Select All" onclick="clickSelectAll()"><br/>
            Request type : (BID:0, CARD:1, BIDINFO:2, PAR:3)<br/>
            Date of next jo check alert : <%=Constantes.timestamp2StringDateHour(alertMgr.getDateNextJobCheckAlert())%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Run process Check Alert" onclick="clickProcessCheckAlert()"><br/>
            <%if(idxStart > 0) {%>
            <input type="button" value="Previous" onclick="clickListOffset(<%=idxStart - nbMax%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (listAlerts.size() >= nbMax) {%>
            <input type="button" value="Next" onclick="clickListOffset(<%=idxStart + nbMax%>)">
            <%}%>
            <br/>
            Offset = <%=idxStart%> - NbMax = <%=nbMax%><br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Sel</th><th>Date</th><th>Engine</th><th>EngineLogin</th><th>Deal</th><th>RequestType</th><th>Game</th><th>Message</th><th>Operation</th></tr>
                <% int idx = 0;for (AlertData e : listAlerts) { idx++;%>
                <tr>
                    <td><input type="checkbox" id="sel_<%=idx %>" name="sel_<%=idx %>" value="<%=e.getIDStr() %>"/></td>
                    <td><%=Constantes.timestamp2StringDateHour(e.date)%></td>
                    <td><%=e.engineVersion%></td>
                    <td><%=e.engineLogin%></td>
                    <td><%=e.deal%></td>
                    <td><%=e.requestType%></td>
                    <td><%=e.game%></td>
                    <td><%=e.message%></td>
                    <td><input type="button" value="Show PBN" onclick="clickShowPBN(this)"></td>
                </tr>
                <tr style="display: none;"><td colspan="9"><%=e.pbn%></td></tr>
                <%}%>
            </table>
        </form>
    </body>
</html>
