<%@page import="com.gotogames.bridge.engineserver.common.ContextManager"%>
<%@page import="com.gotogames.bridge.engineserver.user.UserVirtual"%>
<%@page import="com.gotogames.bridge.engineserver.user.UserVirtualFBServer"%>
<%@page import="com.gotogames.bridge.engineserver.user.UserVirtualMgr"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.*" %>
<%@ page import="com.gotogames.bridge.engineserver.session.EngineSessionMgr" %>
<%@ page import="com.gotogames.common.session.Session" %>
<%
    UserVirtualMgr userMgr = ContextManager.getUserMgr();
    EngineSessionMgr sessionMgr = ContextManager.getSessionMgr();
    String operation = request.getParameter("operation");
    String paramOperation = "";
    String resultOperation = "";
    if (operation != null) {
        if (operation.equals("removeAllUser")) {
            resultOperation = "Nb user removed = "+userMgr.removeAllUserServer();
        }
        else if (operation.equals("removeUser")) {
            paramOperation = request.getParameter("selection");
            UserVirtual u = userMgr.getUserVirtual(paramOperation);
            if (u != null) {
                userMgr.removeUser(u, true);
                resultOperation = "Success to remove user=" + u;
            } else {
                resultOperation = "No user found for login=" + paramOperation;
            }
        }
        else if (operation.equals("removeOld")) {
            List<UserVirtual> listUser = new ArrayList<UserVirtual>(userMgr.getListUser());
            Collections.sort(listUser, new Comparator<UserVirtual>() {
                @Override
                public int compare(UserVirtual o1, UserVirtual o2) {
                    return Long.compare(o1.getDateLastActivity(), o2.getDateLastActivity());
                }
            });
            long tsLimit = System.currentTimeMillis() - 24*60*60*1000;
            int nbMaxToRemove = 100;
            int nbRemove = 0;
            for (UserVirtual u : listUser) {
                if (u.getDateLastActivity() < tsLimit) {
                    userMgr.removeUser(u, true);
                    nbRemove++;
                    resultOperation += "Success to remove user="+u+"<br/>";
                }
                if (nbRemove >= nbMaxToRemove) {
                    break;
                }
            }
            resultOperation = "<b>Nb Remove="+nbRemove+"<br/>"+resultOperation;
        }
        else if (operation.equals("loadSimpleRequestUser")) {
            userMgr.loadSimpleRequestUserList();
            resultOperation = "Operation loadSimpleRequestUser done !";
        }
        else {
            resultOperation = "operation unknown ! ("+operation+")";
        }
    }
    List<UserVirtual> listUser = new ArrayList<UserVirtual>(userMgr.getListUser());
    Collections.sort(listUser, new Comparator<UserVirtual>() {
        @Override
        public int compare(UserVirtual o1, UserVirtual o2) {
            return o1.getLogin().compareTo(o2.getLogin());
        }
    });
    int nbServer = 0;
    for (UserVirtual u : listUser) {
        if (!u.isEngine()) {
            nbServer++;
        }
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss:SSS");

    Map<Long, Session> mapSessions = new HashMap<Long, Session>();
    for (Session s : sessionMgr.getAllCurrentSession()) {
        mapSessions.put(s.getLoginID(), s);
    }
%>

<html>
<head>
    <title>EngineServer - Administration User</title>
    <script type="text/javascript">
        function clickRemoveAllUser() {
            if (confirm("Close all session ?")) {
                document.forms["formUser"].operation.value = "removeAllUser";
                document.forms["formUser"].submit();
            }
        }
        function clickRemoveOldUser() {
            if (confirm("Remove old session ?")) {
                document.forms["formUser"].operation.value = "removeOld";
                document.forms["formUser"].submit();
            }
        }
        function clickLoadSimpleRequestUser() {
            if (confirm("Load simpleRequest user from configuration file ?")) {
                document.forms["formUser"].operation.value = "loadSimpleRequestUser";
                document.forms["formUser"].submit();
            }
        }
        function clickRemoveUser(login) {
            if (confirm("Close session for user=" + login + " ?")) {
                document.forms["formUser"].operation.value = "removeUser";
                document.forms["formUser"].selection.value = login;
                document.forms["formUser"].submit();
            }
        }
    </script>
</head>
<body>
<h1>ADMINISTRATION USER</h1>
<a href="admin.jsp">Administration</a><br/><br/>
<hr width="90%"/>
<a href="adminUserServer.jsp">Refresh</a><br/>
<%if (operation != null){ %>
<b>Operation executed : <%=operation %> - parametres=<%=paramOperation %></b><br/>
Operation result : <%=resultOperation %>
<hr width="90%"/>
<%} %>
<br/>
<form name="formUser" action="adminUserServer.jsp" method="post">
    <input type="hidden" name="operation" value=""/>
    <input type="hidden" name="selection" value=""/>
    <hr width="90%"/>
    <b>List of Users Connected : </b>
    <input type="button" value="Remove All user" onclick="clickRemoveAllUser()"/>&nbsp;&nbsp;&nbsp;
    <input type="button" value="Remove Old user" onclick="clickRemoveOldUser()"/>
    <input type="button" value="Load User simpleRequest (from configuration)" onclick="clickLoadSimpleRequestUser()"/>
    <br/><br/>
    <%if (listUser != null){%>
    Current date=<%=sdf.format(new Date()) %><br/>
    Nb Server = <%=nbServer %><br/>

    <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
        <tr><th>ID</th><th>User info</th><th>Stat</th><th>Date last activity</th><th>Session</th><th>Operation</th></tr>
        <% for (UserVirtual u : listUser) {
            if (u.isEngine()) {
                continue;
            }
            UserVirtualFBServer userServer = (UserVirtualFBServer)u;
        %>
        <tr>
            <td><%=u.getID() %></td>
            <td><%=u.toString()%></td>
            <td>NbRqt=<%=userServer.getNbRequest()%> - NbRqtWS=<%=userServer.getNbRequestWebSocket()%></td>
            <td><%=sdf.format(new Date(u.getDateLastActivity()))%></td>
            <td><%=(mapSessions.get(u.getID()) == null) ? "null" : mapSessions.get(u.getID()).getID()+" - Date last activity : "+sdf.format(mapSessions.get(u.getID()).getDateLastActivity())%></td>
            <td><input type="button" value="Remove user" onclick="clickRemoveUser('<%=u.getLoginID() %>')"/>&nbsp;&nbsp;&nbsp;</td>
        </tr>
        <%}%>
    </table>
    <%} else {%>
    LIST USER NULL !!
    <%} %>
</form>
</body>
</html>