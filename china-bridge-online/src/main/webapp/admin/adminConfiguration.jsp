<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.common.FBConfiguration"%>
<%@page import="com.funbridge.server.engine.ArgineEngineMgr"%>
<%@page import="com.funbridge.server.engine.ArgineProfile"%>
<%@page import="com.funbridge.server.engine.ArgineTextElement"%>
<%@page import="com.funbridge.server.engine.EngineRest"%>
<%@page import="com.gotogames.common.tools.StringTools"%>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<%
    String paramOperation = request.getParameter("operation");
    String resultOperation = "";
    List<ArgineTextElement> listArgineText = null;
    List<ArgineProfile> listArgineProfiles = null;
    List<ArgineProfile> listArgineProfilesCard = null;
    ArgineEngineMgr argineEngineMgr = ContextManager.getArgineEngineMgr();
    if (paramOperation != null) {
        if (paramOperation.equals("findValue")) {
            String key = request.getParameter("key");
            if (key != null) {
                resultOperation = "key="+key+"<br/>value="+FBConfiguration.getInstance().getStringValue(key, null);
            } else {
                resultOperation = "Error param key not found !";
            }
        }
        else if (paramOperation.equals("engineParamReload")) {
            if (ContextManager.getTourSerieMgr().getGameMgr().getEngine() != null) {
                ContextManager.getTourSerieMgr().getGameMgr().getEngine().loadConfig();
            }
            if (ContextManager.getTournamentGame2Mgr().getEngine() != null) {
                ContextManager.getTournamentGame2Mgr().getEngine().loadConfig();
            }
            if (ContextManager.getTimezoneMgr().getGameMgr().getEngine() != null) {
                ContextManager.getTimezoneMgr().getGameMgr().getEngine().loadConfig();
            }
            if (ContextManager.getTrainingMgr().getGameMgr().getEngine() != null) {
                ContextManager.getTrainingMgr().getGameMgr().getEngine().loadConfig();
            }
            if (ContextManager.getDuelMgr().getGameMgr().getEngine() != null) {
                ContextManager.getDuelMgr().getGameMgr().getEngine().loadConfig();
            }
            resultOperation = "Success to reload engine parameters";
        }
        else if (paramOperation.equals("argineTextList")) {
            listArgineText = argineEngineMgr.getAllArgineText();
        }
        else if (paramOperation.equals("argineTextReload")) {
            argineEngineMgr.loadArgineText();
            listArgineText = argineEngineMgr.getAllArgineText();
            resultOperation = "Success to reload argine texts";
        }
        else if (paramOperation.equals("argineDecode")) {
            String strToDecode = request.getParameter("arginetext");
            resultOperation = "Decode "+strToDecode+" -> "+argineEngineMgr.decodeText(strToDecode, "fr");
        }
        else if (paramOperation.equals("changeEngineVersion")) {
            List<String> listKeyValue = new ArrayList<String>();
            listKeyValue.add("tournament.engine.options.defaultVersion="+request.getParameter("engineVersionDefault"));
            listKeyValue.add("tournament.TRAINING.engineVersion="+request.getParameter("engineVersionTraining"));
            listKeyValue.add("tournament.TRAINING_PARTNER.engineVersion="+request.getParameter("engineVersionTrainingPartner"));
            listKeyValue.add("tournament.NEWSERIE.engineVersion="+request.getParameter("engineVersionSerie"));
            listKeyValue.add("tournament.DUEL.engineVersion="+request.getParameter("engineVersionDuel"));
            listKeyValue.add("tournament.NEWTIMEZONE.engineVersion=" + request.getParameter("engineVersionTimezone"));
            listKeyValue.add("tournament.TRIAL.engineVersion="+request.getParameter("engineVersionTrial"));

            if (request.getParameter("engineVersionALL") != null && request.getParameter("engineVersionALL").length() > 0) {
                listKeyValue.add("engine.argine.version.ALL="+request.getParameter("engineVersionALL"));
            }
            if (request.getParameter("engineVersionTRAINING") != null && request.getParameter("engineVersionTRAINING").length() > 0) {
                listKeyValue.add("engine.argine.version."+Constantes.TOURNAMENT_CATEGORY_NAME_TRAINING+"="+request.getParameter("engineVersionTRAINING"));
            }
            if (request.getParameter("engineVersionSERIE") != null && request.getParameter("engineVersionSERIE").length() > 0) {
                listKeyValue.add("engine.argine.version."+Constantes.TOURNAMENT_CATEGORY_NAME_NEWSERIE+"="+request.getParameter("engineVersionSERIE"));
            }
            if (request.getParameter("engineVersionDUEL") != null && request.getParameter("engineVersionDUEL").length() > 0) {
                listKeyValue.add("engine.argine.version."+Constantes.TOURNAMENT_CATEGORY_NAME_DUEL+"="+request.getParameter("engineVersionDUEL"));
            }
            if (request.getParameter("engineVersionTIMEZONE") != null && request.getParameter("engineVersionTIMEZONE").length() > 0) {
                listKeyValue.add("engine.argine.version."+Constantes.TOURNAMENT_CATEGORY_NAME_TIMEZONE+"="+request.getParameter("engineVersionTIMEZONE"));
            }
            if (request.getParameter("engineVersionTRIAL") != null && request.getParameter("engineVersionTRIAL").length() > 0) {
                listKeyValue.add("engine.argine.version."+Constantes.TOURNAMENT_CATEGORY_NAME_TRIAL+"="+request.getParameter("engineVersionTRIAL"));
            }

            resultOperation = "changeEngineVersion listKeyValue="+ StringTools.listToString(listKeyValue)+" -> save config="+FBConfiguration.getInstance().saveValuesKeys(listKeyValue);
        }
        else if (paramOperation.equals("argineProfilesList")) {
            if (argineEngineMgr.getAllProfiles() != null) {
                listArgineProfiles = argineEngineMgr.getAllProfiles().profiles;
            }
            if (argineEngineMgr.getAllProfilesCards() != null) {
                listArgineProfilesCard = argineEngineMgr.getAllProfilesCards().profiles;
            }
        }
        else if (paramOperation.equals("argineProfilesReload")) {
            argineEngineMgr.loadProfiles();
            argineEngineMgr.loadProfilesCards();
            if (argineEngineMgr.getAllProfiles() != null) {
                listArgineProfiles = argineEngineMgr.getAllProfiles().profiles;
            }
            if (argineEngineMgr.getAllProfilesCards() != null) {
                listArgineProfilesCard = argineEngineMgr.getAllProfilesCards().profiles;
            }
            resultOperation = "Success to reload argine profiles";
        }
    }
    EngineRest engine = ContextManager.getTrainingMgr().getGameMgr().getEngine();
%>
	<head>
		<title>Funbridge Server - Administration Player</title>
		<script type="text/javascript">
		function clickGetValue() {
			if (document.forms["formConfig"].key.value.replace(/^\s+/g,'').replace(/\s+$/g,'') == '') {
				alert("Key value is not valid !");
			} else {
				document.forms["formConfig"].operation.value="findValue";
				document.forms["formConfig"].submit();
			}
		}
		function clickRestrictionReload() {
			document.forms["formConfig"].operation.value="restrictionReload";
			document.forms["formConfig"].submit();
		}
		function clickRestrictionList() {
			document.forms["formConfig"].operation.value="restrictionList";
			document.forms["formConfig"].submit();
		}
		function clickEngineParamReload() {
			document.forms["formConfig"].operation.value="engineParamReload";
			document.forms["formConfig"].submit();
		}
		function clickArgineTextList() {
			document.forms["formConfig"].operation.value="argineTextList";
			document.forms["formConfig"].submit();
		}
		function clickArgineTextReload() {
			document.forms["formConfig"].operation.value="argineTextReload";
			document.forms["formConfig"].submit();
		}
        function clickArgineProfilesList() {
            document.forms["formConfig"].operation.value="argineProfilesList";
            document.forms["formConfig"].submit();
        }
        function clickArgineProfilesReload() {
            document.forms["formConfig"].operation.value="argineProfilesReload";
            document.forms["formConfig"].submit();
        }
		function clickArgineDecode() {
			document.forms["formConfig"].operation.value="argineDecode";
			document.forms["formConfig"].submit();
		}
        function clickChangeEngineVersion() {
            if (confirm("Change engine version ?")) {
                document.forms["formConfig"].operation.value="changeEngineVersion";
                document.forms["formConfig"].submit();
            }
        }
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION CONFIGURATION</h1>
		<a href="admin.jsp">Administration</a><br/><br/>
		<%if (resultOperation.length() > 0) {%>
		<b>RESULTAT OPERATION : <%= paramOperation%></b>
		<br/>Result = <%=resultOperation %>
		<%} %>
		<form name="formConfig" method="post" action="adminConfiguration.jsp">
		<input type="hidden" name="operation" value=""/>
		<br/>
		<hr width="90%"/>
		<b>Restrictions</b><br/>

		<input type="button" value="List restriction" onclick="clickRestrictionList()"/><br/>
		<input type="button" value="Reload restriction" onclick="clickRestrictionReload()"/><br/>
		<hr width="90%"/>
		<b>Engine configuration</b><br/>
		<%if (engine != null) { %>
		Engine URL Session = <%=engine.getURLServiceSession() %><br/>
		Login = <%=engine.getLogin() %> - password = <%=engine.getPassword() %> - URL setResult = <%=engine.getUrlSetResult()%><br/>
		Engine URL Request = <%=engine.getURLServiceRequest() %> - Timeout Request = <%=engine.getTimeoutGetResult() %><br/>
		<%} else { %>
		Engine is null !
		<%} %>
        <input type="button" value="Reload engine parameters" onclick="clickEngineParamReload()"/><br/><br/>

        <br/>
        Engine Version Tournament (New method) : Enable=<%=FBConfiguration.getInstance().getIntValue("engine.argine.version.useGlobal", 1)%><br/>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><th>Category</th><th>Value in config file</th><th>value process</th></tr>
            <tr>
                <td>ALL</td>
                <td><input type="text" name="engineVersionALL" value="<%=FBConfiguration.getInstance().getStringValue("engine.argine.version.ALL", "")%>"></td>
                <td><%=argineEngineMgr.getEngineVersion(0)%></td>
            </tr>
            <tr>
                <td>Training</td>
                <td><input type="text" name="engineVersionTRAINING" value="<%=FBConfiguration.getInstance().getStringValue("engine.argine.version."+Constantes.TOURNAMENT_CATEGORY_NAME_TRAINING, "")%>"></td>
                <td><%=argineEngineMgr.getEngineVersion(Constantes.TOURNAMENT_CATEGORY_TRAINING)%></td>
            </tr>
            <tr>
                <td>Serie</td>
                <td><input type="text" name="engineVersionSERIE" value="<%=FBConfiguration.getInstance().getStringValue("engine.argine.version."+Constantes.TOURNAMENT_CATEGORY_NAME_NEWSERIE, "")%>"></td>
                <td><%=argineEngineMgr.getEngineVersion(Constantes.TOURNAMENT_CATEGORY_NEWSERIE)%></td>
            </tr>
            <tr>
                <td>Duel</td>
                <td><input type="text" name="engineVersionDUEL" value="<%=FBConfiguration.getInstance().getStringValue("engine.argine.version."+Constantes.TOURNAMENT_CATEGORY_NAME_DUEL, "")%>"></td>
                <td><%=argineEngineMgr.getEngineVersion(Constantes.TOURNAMENT_CATEGORY_DUEL)%></td>
            </tr>
            <tr>
                <td>Timezone</td>
                <td><input type="text" name="engineVersionTIMEZONE" value="<%=FBConfiguration.getInstance().getStringValue("engine.argine.version."+Constantes.TOURNAMENT_CATEGORY_NAME_TIMEZONE, "")%>"></td>
                <td><%=argineEngineMgr.getEngineVersion(Constantes.TOURNAMENT_CATEGORY_TIMEZONE)%></td>
            </tr>
        </table>
        <input type="button" value="Change Engine Version" onclick="clickChangeEngineVersion()"/><br/>
		<hr width="90%"/>
		<b>Argine Text</b><br/>
		<%if (listArgineText != null) {%>
		Nb Argine text=<%=listArgineText.size() %><br/>
			<%
				String[] tabLang = argineEngineMgr.getSupportedLang();
			%>
		<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
			<tr><th>Name</th><%for (String lang:tabLang){%><th>Text <%=lang%></th><%}%></tr>
		<%for (ArgineTextElement e : listArgineText) {	%>
			<tr>
				<td><%=e.name %></td>
				<%for (String lang:tabLang){%>
				<td><%=e.getTextForLang(lang)%></td>
				<%}%>
			</tr>
		<%} %>
		</table>
		<%} %>
		<input type="button" value="List Argine text" onclick="clickArgineTextList()"/><br/>
		<input type="button" value="Reload Argine text" onclick="clickArgineTextReload()"/><br/>
		<br/>
		Decode argine text : <input type="text" name="arginetext" size="80"><br/>
		<input type="button" value="Decode Argine" onclick="clickArgineDecode()"/>
		<hr width="90%"/>
        <b>Argine Profiles BID</b><br/>
        <%if (listArgineProfiles != null) {%>
        Nb Argine profiles=<%=listArgineProfiles.size()%><br/>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><th>ID</th><th>Name</th><th>Value</th></tr>
            <% for (ArgineProfile ap : listArgineProfiles) {%>
            <tr>
                <td><%=ap.id%></td>
                <td><%=ap.name%></td>
                <td><%=ap.value%></td>
            </tr>
            <%}%>
        </table>
        <%}%>
        <b>Argine Profiles CARD</b><br/>
        <%if (listArgineProfilesCard != null) {%>
        Nb Argine profiles=<%=listArgineProfilesCard.size()%><br/>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><th>ID</th><th>Name</th><th>Value</th></tr>
            <% for (ArgineProfile ap : listArgineProfilesCard) {%>
            <tr>
                <td><%=ap.id%></td>
                <td><%=ap.name%></td>
                <td><%=ap.value%></td>
            </tr>
            <%}%>
        </table>
        <%}%>
        <input type="button" value="List Argine profiles" onclick="clickArgineProfilesList()"/><br/>
        <input type="button" value="Reload Argine profiles" onclick="clickArgineProfilesReload()"/><br/>
        <hr width="90%"/>
        <b>Find value for key</b><br/>
		Key : <input type="text" name="key" size="80">
		<br/>
		<input type="button" value="Get Value" onclick="clickGetValue()"/>
		</form>
	</body>
</html>