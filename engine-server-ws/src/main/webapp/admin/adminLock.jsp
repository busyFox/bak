<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="com.gotogames.bridge.engineserver.common.LockMgr"%>
<%@page import="com.gotogames.bridge.engineserver.common.LockData"%>
<%
LockMgr lockMgr = ContextManager.getLockMgr();
String operation = request.getParameter("operation");
String paramOperation = "";
SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss:SSS");
if (lockMgr != null){
	if (operation != null) {
		if (operation.equals("cleanAll")) {
			lockMgr.destroy();
		} else if (operation.equals("removeKeyID")) {
			paramOperation = request.getParameter("selection");
			String[] reqID = paramOperation.split("-");
			for (int i = 0; i < reqID.length; i++) {
				lockMgr.removeLockDataKeyID(Long.parseLong(reqID[i]));
			}
		} else if (operation.equals("removeKeyRequest")) {
			paramOperation = request.getParameter("selection");
			String[] req = paramOperation.split("-");
			for (int i = 0; i < req.length; i++) {
				lockMgr.removeLockDataKeyRequest(req[i]);
			}
		}
	}
}
List<LockData> listLockDataKeyID = lockMgr.getLockDataKeyIDList();
List<LockData> listLockDataKeyRequest = lockMgr.getLockDataKeyRequestList();
%>


<html>
	<head>
		<title>EngineServer - Administration Lock</title>
		<script type="text/javascript">
		function clickClean(){
			if (confirm("Clean all data from queue ?")){
				document.forms["formClean"].operation.value="cleanAll";
				document.forms["formClean"].submit();
			}
		}
		function clickRemoveKeyID(id){
			if (confirm("Remove key with ID="+id+" from lock ?")){
				document.forms["formClean"].operation.value="removeKeyID";
				document.forms["formClean"].selection.value=id;
				document.forms["formClean"].submit();
			}
		}
		function clickRemoveKeyRequest(req){
			if (confirm("Remove key with request="+req+" from lock ?")){
				document.forms["formClean"].operation.value="removeKeyRequest";
				document.forms["formClean"].selection.value=req;
				document.forms["formClean"].submit();
			}
		}
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION LOCK</h1>
		<a href="adminLock.jsp">Refresh</a><br/><br/>
		<%if (operation != null){ %>
		<b>Operation executed : <%=operation %> - parametres=<%=paramOperation %></b><br/>
		<%} %>
		<form name="formClean" action="adminLock.jsp" method="post">
		<input type="hidden" name="operation" value=""/>
		<input type="hidden" name="selection" value=""/>
		<input type="button" value="Clean All" onclick="clickClean()"/>
		
		<br/><hr width="95%" align="center"/>
		<b>List of lock on key ID : </b>
		<%if (listLockDataKeyID != null){
			%>
			Nb lock = <%=listLockDataKeyID.size() %><br/>
			<br/>
			<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
			<tr><th>Operation</th><th>Date</th><th>Count user</th><th>Data value</th></tr>
			<%
			
			for (int i = 0; i < listLockDataKeyID.size(); i++) {
				LockData temp = listLockDataKeyID.get(i);
				%>
				<tr>
				<td><input type="button" value="Remove" onclick="clickRemoveKeyID(<%=temp.getDataValue() %>)"/></td>
				<td><%=sdf.format(new Date(temp.getTimestamp()))%></td>
				<td><%=temp.getCount() %></td>
				<td><%=temp.getDataValue() %></td>
				</tr>
				<%
			}%>
			</table>
		<%} else {%>
		LIST DATA KEY ID NULL !!
		<%} %>
		<br/><hr width="95%" align="center"/>
		<b>List of lock on key REQUEST : </b>
		<%if (listLockDataKeyRequest != null){
			%>
			Nb lock = <%=listLockDataKeyRequest.size() %><br/>
			<br/>
			<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
			<tr><th>Operation</th><th>Date</th><th>Count user</th><th>Data value</th></tr>
			<%
			
			for (int i = 0; i < listLockDataKeyRequest.size(); i++) {
				LockData temp = listLockDataKeyRequest.get(i);
				%>
				<tr>
				<td><input type="button" value="Remove" onclick="clickRemoveKeyRequest(<%=temp.getDataValue() %>)"/></td>
				<td><%=sdf.format(new Date(temp.getTimestamp()))%></td>
				<td><%=temp.getCount() %></td>
				<td><%=temp.getDataValue() %></td>
				</tr>
				<%
			}%>
			</table>
				
		<%} else {%>
		LIST DATA KEY REQUEST NULL !!
		<%} %>
		</form>
	</body>
</html>