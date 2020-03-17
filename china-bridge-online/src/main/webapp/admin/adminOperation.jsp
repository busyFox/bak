<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.operation.OperationMgr" %>
<%@ page import="com.funbridge.server.operation.connection.OperationConnection" %>
<%@ page import="com.funbridge.server.operation.connection.OperationConnectionNotif" %>
<%@ page import="com.funbridge.server.operation.connection.OperationConnectionNotifPlayerList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<%
    OperationMgr operationMgr = ContextManager.getOperationMgr();
    String paramOperation = request.getParameter("operation");
    String resultOperation = "";
    boolean showOperationEditor = false;
    OperationConnection opEdit = null;
    if (paramOperation != null) {
        if (paramOperation.equals("reload")) {
            resultOperation = "Reload list operation connection in memory result="+operationMgr.loadListMemoryOperationConnection();
        }
        else if (paramOperation.equals("purge")) {
            resultOperation = "Purge operationConnection expied nbRemove="+operationMgr.purgeExpiredOperationConnection();
        }
        else if (paramOperation.equals("create")) {
            showOperationEditor = true;
        }
        else if (paramOperation.equals("edit")) {
            String operationID = request.getParameter("operationID");
            if (operationID != null) {
                opEdit = operationMgr.getOperationConnection(operationID);
                if (opEdit != null) {
                    showOperationEditor = true;
                    resultOperation = "Find with sucess operation="+opEdit;
                } else {
                    resultOperation = "Failed to find operation Connection with ID="+operationID;
                }
            } else {
                resultOperation = "No parameter operationID !";
            }
        }
        else if (paramOperation.equals("delete")) {
            String operationID = request.getParameter("operationID");
            if (operationID != null) {
                OperationConnection opDelete = operationMgr.getOperationConnection(operationID);
                if (opDelete != null) {
                    operationMgr.removeOperationConnection(opDelete);
                    resultOperation = "Remove with sucess operation="+opDelete;
                } else {
                    resultOperation = "Failed to retrieve operation Connection with ID="+operationID;
                }
            } else {
                resultOperation = "No parameter operationID !";
            }
        }
        else if (paramOperation.equals("update")) {
            String operationID = request.getParameter("operationID");
            OperationConnection opSave = null;
            if (operationID != null) {
                opSave = operationMgr.getOperationConnection(operationID);
                if (opSave == null) {
                    resultOperation = "Failed to find operation Connection with ID="+operationID;
                }
            } else {
                resultOperation = "No parameter operationID !";
            }
            if (opSave != null) {
                String operationName = request.getParameter("operationName");
                String operationEnable = request.getParameter("operationEnable");
                String strDateExpiration = request.getParameter("operationDateExpiration");
                String strDateStart = request.getParameter("operationDateStart");
                String strDateLastConnectionBefore = request.getParameter("operationDateLastConnectionBefore");
                String strParametersNames = request.getParameter("operationSpecificParametersNames");
                String strParametersValues = request.getParameter("operationSpecificParametersValues");
                Map<String, String> mapParameters = null;
                if (strParametersNames != null && strParametersValues != null) {
                    mapParameters = new HashMap<String, String>();
                    String[] tabParamName = strParametersNames.split(";");
                    String[] tabParamValue = strParametersValues.split(";");
                    if (tabParamName.length == tabParamValue.length) {
                        for (int i = 0; i < tabParamName.length; i++) {
                            mapParameters.put(tabParamName[i], tabParamValue[i]);
                        }
                    }
                }
                opSave.name = operationName;
                opSave.enable = operationEnable != null;
                opSave.dateExpiration = (strDateExpiration!=null&&strDateExpiration.length()>0)?Constantes.stringDateHour2Timestamp(strDateExpiration):0;
                opSave.dateStart = (strDateStart!=null&&strDateStart.length()>0)?Constantes.stringDateHour2Timestamp(strDateStart):0;
                opSave.dateLastConnectionBefore = (strDateLastConnectionBefore!=null&&strDateLastConnectionBefore.length()>0)?Constantes.stringDateHour2Timestamp(strDateLastConnectionBefore):0;
                if (opSave.setSpecificParametersValues(mapParameters)) {
                    operationMgr.saveOperationConnection(opSave);
                    resultOperation = "OperationConnection save with success - op="+opSave;
                } else {
                    resultOperation = "operationType method setSpecificParametersValues failed - strParametersNames=" + strParametersNames+" - strParametersValues="+strParametersValues;
                }
            }
        }
        else if (paramOperation.equals("saveNew")) {
            String operationType = request.getParameter("operationType");
            String operationName = request.getParameter("operationName");
            String operationEnable = request.getParameter("operationEnable");
            String strDateExpiration = request.getParameter("operationDateExpiration");
            String strDateStart = request.getParameter("operationDateStart");
            String strDateLastConnectionBefore = request.getParameter("operationDateLastConnectionBefore");
            String strParametersNames = request.getParameter("operationSpecificParametersNames");
            String strParametersValues = request.getParameter("operationSpecificParametersValues");
            Map<String, String> mapParameters = null;
            if (strParametersNames != null && strParametersValues != null) {
                mapParameters = new HashMap<String, String>();
                String[] tabParamName = strParametersNames.split(";");
                String[] tabParamValue = strParametersValues.split(";");
                if (tabParamName.length == tabParamValue.length) {
                    for (int i = 0; i < tabParamName.length; i++) {
                        mapParameters.put(tabParamName[i], tabParamValue[i]);
                    }
                }
            }
            if (operationType != null) {
                if (operationName != null && operationName.length() > 0) {
                    OperationConnection op = null;
                    if (operationType.equals(OperationConnectionNotif.getOperationType())) {
                        op = new OperationConnectionNotif(operationName);
                    } else if (operationType.equals(OperationConnectionNotifPlayerList.getOperationType())) {
                        op = new OperationConnectionNotifPlayerList(operationName);
                    } else {
                        resultOperation = "operationType not supported - operationType=" + operationType;
                    }
                    if (op != null) {
                        op.enable = operationEnable != null;
                        op.dateExpiration = (strDateExpiration!=null&&strDateExpiration.length()>0)?Constantes.stringDateHour2Timestamp(strDateExpiration):0;
                        op.dateStart = (strDateStart!=null&&strDateStart.length()>0)?Constantes.stringDateHour2Timestamp(strDateStart):0;
                        op.dateLastConnectionBefore = (strDateLastConnectionBefore!=null&&strDateLastConnectionBefore.length()>0)?Constantes.stringDateHour2Timestamp(strDateLastConnectionBefore):0;
                        if (op.setSpecificParametersValues(mapParameters)) {
                            operationMgr.saveOperationConnection(op);
                            resultOperation = "OperationConnection created with success - op="+op;
                        } else {
                            resultOperation = "operationType method setSpecificParametersValues failed - strParametersNames=" + strParametersNames+" - strParametersValues="+strParametersValues;
                        }
                    }
                } else {
                    resultOperation = "operationName not valid - operationName=" + operationName;
                }
            } else {
                resultOperation = "operationType not valid - operationType="+operationType;
            }

        }
        else {
            resultOperation = "Operation not supported : "+paramOperation;
        }
    }
    List<OperationConnection> listMemoryOperationConnection = operationMgr.getListMemoryOperationConnection();
    List<OperationConnection> listOperationConnection = operationMgr.listOperationConnection(false, 0, 0);
    int nbOperationExpired = 0;
    for (OperationConnection op : listOperationConnection) {
        if (!op.checkDateExpiration()) {
            nbOperationExpired++;
        }
    }

%>
<head>
    <title>Funbridge Server - Administration Operation</title>
    <script type="text/javascript">
        function clickReload() {
            if (confirm("Reload operationConnection in memory ?")) {
                document.forms["formOperation"].operation.value = "reload";
                document.forms["formOperation"].submit();
            }
        }
        function clickPurge() {
            if (confirm("Purge all expired operationConnection ?")) {
                document.forms["formOperation"].operation.value = "purge";
                document.forms["formOperation"].submit();
            }
        }
        function clickCreate() {
            document.forms["formOperation"].operation.value="create";
            document.forms["formOperation"].submit();
        }
        function changeOperationType() {
            if (document.forms["formOperation"].operationType.value == "OperationConnectionNotif") {
                document.forms["formOperation"].operationSpecificParametersNames.value = "<%=OperationConnectionNotif.getStringSpecificParameters()%>";
            }
            else if (document.forms["formOperation"].operationType.value == "OperationConnectionNotifPlayerList") {
                document.forms["formOperation"].operationSpecificParametersNames.value = "<%=OperationConnectionNotifPlayerList.getStringSpecificParameters()%>";
            }
            else {
                document.forms["formOperation"].operationSpecificParametersNames.value = "";
            }
        }
        function clickSaveNewOperation() {
            document.forms["formOperation"].operation.value="saveNew";
            document.forms["formOperation"].submit();
        }
        function clickUpdateOperation() {
            document.forms["formOperation"].operation.value="update";
            document.forms["formOperation"].submit();
        }
        function clickEditOperation(operationID) {
            document.forms["formOperation"].operation.value="edit";
            document.forms["formOperation"].operationID.value=operationID;
            document.forms["formOperation"].submit();
        }
        function clickDeleteOperation(operationID) {
            document.forms["formOperation"].operation.value="delete";
            document.forms["formOperation"].operationID.value=operationID;
            document.forms["formOperation"].submit();
        }
    </script>
</head>
<body>
    <h1>ADMINISTRATION OPERATION</h1>
    <a href="admin.jsp">Administration</a><br/><br/>
    <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= paramOperation%></b>
        <br/>Result = <%=resultOperation %>
    <%} %>
    <form name="formOperation" method="post" action="adminOperation.jsp">
        <input type="hidden" name="operation" value=""/>
        <input type="hidden" name="operationID" value="<%=opEdit!=null?opEdit.ID.toString():""%>"/>
        <input type="button" value="Reload operations connection" onclick="clickReload()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input type="button" value="Purge expired operations connection" onclick="clickPurge()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <input type="button" value="Create operations connection" onclick="clickCreate()"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <br/>
        Date next run operation purge connection = <%=operationMgr.getDateNextTaskSchedulerPurgeConnection()%><br/>
        <br/>
        <% if (showOperationEditor) {%>
        <hr width="95%"/>
        <b>Operation editor</b><br/>
        Operation type :
        <select name="operationType" onchange="changeOperationType()" <%=opEdit!=null?"disabled":""%>>
            <option value=""></option>
            <option value="<%=OperationConnectionNotif.getOperationType()%>" <%=opEdit!=null&&opEdit.type.equals(OperationConnectionNotif.getOperationType())?"selected":""%>><%=OperationConnectionNotif.getOperationType()%></option>
            <option value="<%=OperationConnectionNotifPlayerList.getOperationType()%>" <%=opEdit!=null&&opEdit.type.equals(OperationConnectionNotifPlayerList.getOperationType())?"selected":""%>><%=OperationConnectionNotifPlayerList.getOperationType()%></option>
        </select><br/>
        Name : <input type="text" size="30" name="operationName" value="<%=opEdit!=null?opEdit.name:""%>">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        Enable : <input type="checkbox" name="operationEnable" <%=opEdit!=null&&opEdit.enable?"checked":""%>><br/>
        Date Expiration : <input type="text" size="20" name="operationDateExpiration" value="<%=opEdit!=null&&opEdit.dateExpiration!=0?Constantes.timestamp2StringDateHour(opEdit.dateExpiration):""%>">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        Date Start : <input type="text" size="20" name="operationDateStart" value="<%=opEdit!=null&&opEdit.dateStart!=0?Constantes.timestamp2StringDateHour(opEdit.dateStart):""%>">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        Date Last Connection Before : <input type="text" size="20" name="operationDateLastConnectionBefore" value="<%=opEdit!=null&&opEdit.dateLastConnectionBefore!=0?Constantes.timestamp2StringDateHour(opEdit.dateLastConnectionBefore):""%>">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(Format : dd/MM/yyyy - HH:mm:ss)<br/>
        Specific Parameters name : <input type="text" name="operationSpecificParametersNames" size="100" value="<%=opEdit!=null?opEdit.getSpecificParametersNames():""%>"><br/>
        Specific Parameters value : <input type="text" name="operationSpecificParametersValues" size="100" value="<%=opEdit!=null?opEdit.getSpecificParametersValues():""%>"><br/>
        <br/>
        <%if (opEdit == null) {%>
        <input type="button" value="Save New Operation" onclick="clickSaveNewOperation()">
        <%} else {%>
        <input type="button" value="Update Operation" onclick="clickUpdateOperation()">
        <%}%>
        <%}%>
        <hr width="95%"/>
        <b>List Memory Operation Connection</b><br>
        Nb Operation Connection in memory : <%=listMemoryOperationConnection.size()%><br/>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><th>Name</th><th>Type</th><th>Date expiration</th><th>Parameters valid</th><th>Description</th></tr>
            <% for (OperationConnection op : listMemoryOperationConnection) {%>
            <tr>
                <td><%=op.name%></td>
                <td><%=op.type%></td>
                <td><%=Constantes.timestamp2StringDateHour(op.dateExpiration)%></td>
                <td><%=op.areSpecificParameterValid()%></td>
                <td><%=op.toString()%></td>
            </tr>
            <%}%>
        </table>
        <br/>
        <hr width="95%"/>
        <b>List all Operation Connection</b><br/>
        Nb Operation Connection in DB : <%=listOperationConnection.size()%> - Nb Operation expired=<%=nbOperationExpired%><br/>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><th>Name</th><th>Type</th><th>Date expiration</th><th>Enable</th><th>Parameters valid</th><th>Description</th><th>Operation</th></tr>
            <% for (OperationConnection op : listOperationConnection) {%>
            <tr>
                <td><%=op.name%></td>
                <td><%=op.type%></td>
                <td><%=Constantes.timestamp2StringDateHour(op.dateExpiration)%></td>
                <td><%=op.enable%></td>
                <td><%=op.areSpecificParameterValid()%></td>
                <td><%=op.toString()%></td>
                <td>
                    <input type="button" value="Edit" onclick="clickEditOperation('<%=op.ID.toString()%>')">&nbsp;&nbsp;&nbsp;
                    <input type="button" value="Delete" onclick="clickDeleteOperation('<%=op.ID.toString()%>')">
                </td>
            </tr>
            <%}%>
        </table>
    </form>
</body>
</html>
