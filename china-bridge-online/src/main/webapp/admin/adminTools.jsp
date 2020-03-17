<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.gotogames.common.crypt.AESCrypto"%>
<%@page import="com.gotogames.common.crypt.SHACrypt"%>
<%@ page import="java.nio.charset.Charset" %>
<html>
<%
String paramOperation = request.getParameter("operation");
String resultOperation = "";
if (paramOperation != null) {
	if (paramOperation.equals("datetime")) {
		String value = request.getParameter("value");
		String convertType = request.getParameter("convertType");
		if (value != null) {
			if (convertType.equals("ts2date")) {
				resultOperation = "Timestamp "+value+" => date : ";
				try {
					long lVal = Long.parseLong(value);
					resultOperation += Constantes.timestamp2StringDateHour(lVal);
				} catch (Exception e) {
					resultOperation += "Parsing long value failed !";
				}
			}
			else {
				resultOperation = "Date "+value+" => timestamp : ";
				try {
					resultOperation += Constantes.stringDateHour2Timestamp(value);
				} catch (Exception e) {
					resultOperation += "Parsing string value failed !";
				}
			}
		} else {
			resultOperation = "Error param value not found !";
		}
	}
	else if (paramOperation.equals("shacrypt")) {
		String value = request.getParameter("value");
		if (value != null && value.length() > 0) {
			resultOperation = "SHA crypt for "+value+" = "+SHACrypt.sha256Hex(value);
		} else {
			resultOperation = "Error param value not found or empty !";
		}
	}
    else if (paramOperation.equals("aescrypt")) {
        String value = request.getParameter("value");
        if (value != null && value.length() > 0) {
            resultOperation = "AES crypt for "+value+" = "+AESCrypto.crypt(value, Constantes.CRYPT_KEY);
        } else {
            resultOperation = "Error param value not found or empty !";
        }
    }
    else if (paramOperation.equals("aesdecrypt")) {
        String value = request.getParameter("value");
        if (value != null && value.length() > 0) {
            resultOperation = "AES decrypt for "+value+" = "+AESCrypto.decrypt(value, Constantes.CRYPT_KEY);
        } else {
            resultOperation = "Error param value not found or empty !";
        }
    }
	else if (paramOperation.equals("reloadEngineSession")) {
        String engineSel = request.getParameter("engineSel");
        if (engineSel != null) {
        	if (engineSel.equals("training")) {
            	ContextManager.getTrainingMgr().getGameMgr().getEngine().openSession();
            	resultOperation = "openSession done for Training";
            }
        	else if (engineSel.equals("timezone")) {
            	ContextManager.getTimezoneMgr().getGameMgr().getEngine().openSession();
            	resultOperation = "openSession done for Timezone";
            }
            else if (engineSel.equals("duel")) {
                ContextManager.getDuelMgr().getGameMgr().getEngine().openSession();
                resultOperation = "openSession done for Duel";
            }
            else if (engineSel.equals("serie")) {
                ContextManager.getTourSerieMgr().getGameMgr().getEngine().openSession();
                resultOperation = "openSession done for Serie";
            }
            else if (engineSel.equals("tournamentGame2")) {
            	ContextManager.getTournamentGame2Mgr().getEngine().openSession();
            	resultOperation = "openSession done for TournamentGame2";
            }
        	else {
        		resultOperation = "Parameter engineSel not valid : "+engineSel;
        	}
        } else {
        	resultOperation = "Parameter engineSel not valid : "+engineSel;
        }
	}
}
%>
	<head>
		<title>Funbridge Server - Administration TOOLS</title>
		<script type="text/javascript">
            function reloadEngineConnection(engineName) {
                if (confirm("Reload engine connection for "+engineName)) {
                    document.forms["formToolsEngine"].engineSel.value = engineName;
                    document.forms["formToolsEngine"].submit();
                }
            }
		</script>
	</head>
	<body>
		<h1>ADMINISTRATION TOOLS</h1>
		<a href="admin.jsp">Administration</a><br/><br/>
		<hr width="90%"/>
		<%if (resultOperation.length() > 0) {%>
		<b>RESULTAT OPERATION : <%= paramOperation%></b>
		<br/>Result = <%=resultOperation %>
		<hr width="90%"/>
		<%} %>
		<br/>
		System encoding file = <%=System.getProperty("file.encoding")%> - Charset default = <%=Charset.defaultCharset().displayName()%>
        <hr width="90%"/>
		<b>Date Time convertion</b><br/>
		<form name="formToolsDate" method="post" action="adminTools.jsp">
		<input type="hidden" name="operation" value="datetime"/>
		Convertion type : TS to date <input type="radio" name="convertType" value="ts2date" checked="checked"/>&nbsp;&nbsp;&nbsp;Date to TS <input type="radio" name="convertType" value="date2ts"/><br/>
		Value (date or TS) : <input type="text" name="value">&nbsp;&nbsp;&nbsp;(Format for date : dd/MM/yyyy - HH:mm:ss)
		<br/>
		<input type="submit" value="Convert"/>
		</form>
		<hr width="90%"/>
		<b>SHA Crypt</b><br/>
		<form name="formToolsCrypt" method="post" action="adminTools.jsp">
		<input type="hidden" name="operation" value="shacrypt"/>
		Value : <input type="text" name="value" size="80">
		<br/>
		<input type="button" value="Crypt SHA" onclick="document.forms['formToolsCrypt'].operation.value='shacrypt';document.forms['formToolsCrypt'].submit();"/>&nbsp;&nbsp;&nbsp;
        <input type="button" value="Crypt AES" onclick="document.forms['formToolsCrypt'].operation.value='aescrypt';document.forms['formToolsCrypt'].submit();"/>&nbsp;&nbsp;&nbsp;
        <input type="button" value="Decrypt SHA" onclick="document.forms['formToolsCrypt'].operation.value='aesdecrypt';document.forms['formToolsCrypt'].submit();"/>&nbsp;&nbsp;&nbsp;
		</form>
		<hr width="90%"/>
		<form name="formToolsEngine" method="post" action="adminTools.jsp">
		<input type="hidden" name="operation" value="reloadEngineSession">
		<input type="hidden" name="engineSel" value="">
		<b>Engine WebService connection</b><br/>
		<b>Training Engine</b> : <%=ContextManager.getTrainingMgr().getGameMgr().getEngine() %><br/>
		<input type="button" value="reload connection" onclick="reloadEngineConnection('training');"/><br/>
		<br/>
		<b>Timezone Engine</b> : <%=ContextManager.getTimezoneMgr().getGameMgr().getEngine() %><br/>
		<input type="button" value="reload connection" onclick="reloadEngineConnection('timezone');"/><br/>
		<br/>
        <b>Duel Engine</b> : <%=ContextManager.getDuelMgr().getGameMgr().getEngine() %><br/>
        <input type="button" value="reload connection" onclick="reloadEngineConnection('duel');"/><br/>
        <br/>
        <b>Serie Engine</b> : <%=ContextManager.getTourSerieMgr().getGameMgr().getEngine() %><br/>
        <input type="button" value="reload connection" onclick="reloadEngineConnection('serie');"/><br/>
        <br/>
        <b>TournamentGame2 Engine</b> : <%=ContextManager.getTournamentGame2Mgr().getEngine() %><br/>
		<input type="button" value="reload connection" onclick="reloadEngineConnection('tournamentGame2');"/><br/>
		<br/>
		</form>
	</body>
</html>