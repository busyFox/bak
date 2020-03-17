<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.texts.TextUIData" %>
<%@ page import="com.funbridge.server.texts.TextUIMgr" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String resultOperation = "";
    String operation = request.getParameter("operation");
    TextUIMgr textMgr = ContextManager.getTextUIMgr();
    if (operation != null) {
        if (operation.equals("reloadTextConfiguration")) {
            if (textMgr.loadAllTextUIData()) {
                resultOperation = "Success to reload all text UI Data";
            } else {
                resultOperation = "Failed to reload all text UI Data";
            }
        }
        else if (operation.equals("buildPARText")) {
            String parText = request.getParameter("parText");
            String parLang = request.getParameter("parLang");
            resultOperation = "Build text for parText="+parText+" - lang="+parLang+" - result="+textMgr.getTextAnalyzePar(parText, parLang);
        }
        else if (operation.equals("buildBidText")) {
            String bidText = request.getParameter("bidText");
            String bidLang = request.getParameter("bidLang");
            resultOperation = "Build text for bidText="+bidText+" - lang="+bidLang+" - result="+textMgr.getTextAnalyzeBid(bidText, bidLang);
        }
        else if (operation.equals("buildPlayText")) {
            String playText = request.getParameter("playText");
            String playLang = request.getParameter("playLang");
            resultOperation = "Build text for parText="+playText+" - lang="+playLang+" - result="+textMgr.getTextAnalyzePlay(playText, playLang);
        }
    }
    List<TextUIData> listAllTexts = textMgr.listAllTexts();
%>
<html>
<head>
    <title>Funbridge Server - Administration</title>
    <script type="text/javascript">
        function clickReloadTextConfiguration() {
            if (confirm("Reload text ?")) {
                document.forms["formTextsUI"].operation.value = "reloadTextConfiguration";
                document.forms["formTextsUI"].submit();
            }
        }
        function clickBuildPARText() {
            document.forms["formTextsUI"].operation.value = "buildPARText";
            document.forms["formTextsUI"].submit();
        }
        function clickBuildBidText() {
            document.forms["formTextsUI"].operation.value = "buildBidText";
            document.forms["formTextsUI"].submit();
        }
        function clickBuildPlayText() {
            document.forms["formTextsUI"].operation.value = "buildPlayText";
            document.forms["formTextsUI"].submit();
        }
    </script>
</head>
<body>
<h1>ADMINISTRATION TEXTS UI</h1>
<a href="admin.jsp">Administration</a><br/><br/>
<%if (resultOperation.length() > 0) {%>
<b>Result operation = <%=resultOperation %></b>
<br/>
<%} %>
<form name="formTextsUI" method="post" action="adminTextsUI.jsp">
    <input type="hidden" name="operation" value=""/>
    PAR text to analyze : <input type="text" name="parText"> - Lang : <input type="text" name="parLang" value="fr" size="5">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Build PAR text" onclick="clickBuildPARText()"/>
    Bid text to analyze : <input type="text" name="bidText"> - Lang : <input type="text" name="bidLang" value="fr" size="5">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Build Bid text" onclick="clickBuildBidText()"/>
    Play text to analyze : <input type="text" name="playText"> - Lang : <input type="text" name="playLang" value="fr" size="5">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Build Play text" onclick="clickBuildPlayText()"/>
    <hr width="95%"/>
    <b>List textUI</b><br/>
    Reload text file configuration <input type="button" value="Reload Text Configuration" onclick="clickReloadTextConfiguration()"/><br/>
    Nb TextUIData = <%=listAllTexts.size() %><br/>
    <%
        String[] tabLang = textMgr.getSupportedLang();
    %>
    <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
        <tr><th>Type</th><th>Name</th><%for (String lang:tabLang){%><th>Text <%=lang%></th><%}%></tr>
        <%for (TextUIData e : textMgr.listTextUI()) {%>
        <tr>
            <td>textui </td>
            <td><%=e.name %></td>
            <%for (String lang:tabLang){%>
            <td><%=e.getText(lang) %>
            </td>
            <%}%>
        </tr>
        <%}%>

        <%for (TextUIData e : textMgr.listTextNotif()) {%>
        <tr>
            <td>notif</td>
            <td><%=e.name %></td>
            <%for (String lang:tabLang){%>
            <td><%=e.getText(lang) %>
            </td>
            <%}%>
        </tr>
        <%}%>

        <%for (TextUIData e : textMgr.listTextMsg()) {%>
        <tr>
            <td>msg</td>
            <td><%=e.name %></td>
            <%for (String lang:tabLang){%>
            <td><%=e.getText(lang) %>
            </td>
            <%}%>
        </tr>
        <%}%>
    </table>
</form>

</body>
</html>
