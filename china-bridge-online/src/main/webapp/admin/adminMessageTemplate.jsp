<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.message.MessageMgr" %>
<%@ page import="com.funbridge.server.message.MessageNotifMgr" %>
<%@ page import="com.funbridge.server.texts.TextUIData" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String resultOperation = "";
    String operation = request.getParameter("operation");
    MessageNotifMgr notifMgr = ContextManager.getMessageNotifMgr();
    MessageMgr messageMgr = ContextManager.getMessageMgr();
    List<TextUIData> listNotif = notifMgr.listAllTemplate();
    List<TextUIData> listMessage = messageMgr.listAllTemplate();
%>
<html>
    <head>
        <title>Funbridge Server - Administration</title>
        <script type="text/javascript">
        </script>
    </head>
    <body>
    <h1>ADMINISTRATION MESSAGE</h1>
    <a href="adminMessage.jsp">Administration Message</a><br/><br/>
    <%if (resultOperation.length() > 0) {%>
    <b>Result operation = <%=resultOperation %></b>
    <br/>
    <%} %>
    <form name="formMsg" method="post" action="adminMessageTemplate.jsp">
        <input type="hidden" name="operation" value=""/>
        <hr width="95%"/>
        <b>Template Message</b>
        Nb Template Message = <%=listMessage.size() %><br/>
        <%
            String[] tabLangMessage = messageMgr.getSupportedLang();
        %>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><th>Name</th><%for (String lang:tabLangMessage){%><th>Text <%=lang%></th><%}%></tr>
            <%for (TextUIData e : listMessage) {%>
            <tr>
                <td><%=e.name %></td>
                <%for (String lang:tabLangMessage){%>
                <td><%=e.getText(lang) %></td>
                <%}%>
            </tr>
            <%}%>
        </table>
        <br/>
        <hr width="95%"/>
        <b>Template Notif</b><br/>
        Nb Template Notif = <%=listNotif.size() %><br/>
        <%
            String[] tabLangNotif = notifMgr.getSupportedLang();
        %>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><th>Name</th><%for (String lang:tabLangNotif){%><th>Text <%=lang%></th><%}%></tr>
            <%
                for (TextUIData tpl : listNotif) {
            %>
            <tr>
                <td><%=tpl.name %></td>
                <%for (String lang:tabLangNotif){%>
                <td><%=tpl.getText(lang) %></td>
                <%}%>
            </tr>
            <%}
            %>
        </table>
    </form>

    </body>
</html>
