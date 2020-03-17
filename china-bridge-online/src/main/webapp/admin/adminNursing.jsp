<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.common.FBConfiguration"%>
<%@page import="com.funbridge.server.nursing.NursingMgr"%>
<%@page import="com.funbridge.server.nursing.data.PlayerNursing"%>
<%@ page import="com.funbridge.server.player.PlayerMgr" %>
<%@ page import="com.funbridge.server.player.data.Player" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.List" %>
<html>
<%
	NursingMgr nursingMgr = ContextManager.getNursingMgr();
	PlayerMgr playerMgr = ContextManager.getPlayerMgr();

	Player player = null;
    List<Player> listPlayer = null;
	List<PlayerNursing> listPlayerNursing = null;
    List<String> nursingList = Arrays.asList(FBConfiguration.getInstance().getStringValue("nursing.list", "").split(";"));

	String paramOperation = request.getParameter("operation");
	String resultOperation = "";

	Boolean nursingAvailability = null;

    String playerID = request.getParameter("playerid");
    if (playerID != null && !playerID.isEmpty()) {
        player = playerMgr.getPlayer(Long.parseLong(playerID));
        if (player != null) {
            listPlayer = new ArrayList<Player>();
            listPlayer.add(player);
        }
    }
    String nursingName = request.getParameter("nursingName");

	if (paramOperation != null) {
	    if (paramOperation.equals("searchPlayer")) {
			String searchLogin = request.getParameter("searchlogin");
			if (searchLogin != null) {
				resultOperation = "ok - searchLogin="+searchLogin;
				listPlayer = playerMgr.searchPlayerWithMailOrPseudo(searchLogin);
			} else {
				resultOperation = "Error param searchlogin not found !";
			}
		}
		else if (paramOperation.equals("selectionPlayer")) {
			String selplaid = request.getParameter("selplaid");
			if (selplaid != null) {
				player = playerMgr.getPlayer(Long.parseLong(selplaid));
				if (player != null) {
					resultOperation = "Player found with id="+selplaid;
					listPlayer = new ArrayList<Player>();
					listPlayer.add(player);
				}
				else resultOperation = "No player found for id="+selplaid;
			} else {
				resultOperation = "Error param playerid not found !";
			}
		}
        else if (paramOperation.equals("showNursing")) {
            if (playerID != null) {
                listPlayerNursing = nursingMgr.listPlayerNursing(Long.parseLong(playerID));
            } else {
                resultOperation = "Error param playerid not found !";
            }
        }
        else if (paramOperation.equals("resetNursing")) {
            if (playerID != null) {
                boolean result = nursingMgr.resetNursing(Long.parseLong(playerID));
                resultOperation = "Delete status : " + result;
            } else {
                resultOperation = "Error param playerid not found !";
            }
        }
        else if (paramOperation.equals("deleteNursing")) {
            if (playerID != null && nursingName != null) {
                boolean result = nursingMgr.deletePlayerNursing(Long.parseLong(playerID), nursingName);
                resultOperation = "Delete status : " + result;
                listPlayerNursing = nursingMgr.listPlayerNursing(Long.parseLong(playerID));
            } else {
                resultOperation = "Error param playerid or nursingname not found !";
            }
        }
        else if (paramOperation.equals("addNursing")) {
            if (playerID != null && nursingName != null) {
                nursingMgr.addNursingForPlayer(Long.parseLong(playerID), nursingName);
                resultOperation = "Nursing " + nursingName + " added for player " + playerID;
                listPlayerNursing = nursingMgr.listPlayerNursing(Long.parseLong(playerID));
            } else {
                resultOperation = "Error param playerid or nursingname not found !";
            }
        }
        else if (paramOperation.equals("checkNursing")) {
            if (playerID != null && nursingName != null) {
                try {
                    nursingAvailability = nursingMgr.isNursingAvailable(nursingName, Long.parseLong(playerID));
                } catch (Exception e) {
                    resultOperation = "Error checking availability Nursing " + nursingName + " for player " + playerID;
                }
                listPlayerNursing = nursingMgr.listPlayerNursing(Long.parseLong(playerID));
            } else {
                resultOperation = "Error param playerid or nursingname not found !";
            }
        }
	}
%>

<head>
	<title>Funbridge Server - Administration Live Comments</title>
	<script type="text/javascript">
        function clickSearchPlayer() {
            if (document.forms["formPlayer"].searchlogin.value.replace(/^\s+/g,'').replace(/\s+$/g,'') == '') {
                alert("Login search is not valid !");
            } else {
                document.forms["formPlayer"].operation.value = "searchPlayer";
                document.forms["formPlayer"].submit();
            }
        }
        function clickSelectPlayer() {
            if (document.forms["formPlayer"].selplaid.value.replace(/^\s+/g,'').replace(/\s+$/g,'') == '') {
                alert("Player ID is not valid !");
            } else {
                document.forms["formPlayer"].operation.value = "selectionPlayer";
                document.forms["formPlayer"].submit();
            }
        }
        function clickShowNursing(plaID) {
            document.forms["formPlayer"].operation.value = "showNursing";
            document.forms["formPlayer"].playerid.value = plaID;
            document.forms["formPlayer"].submit();
        }
        function clickResetNursing(plaID) {
            if (confirm("Reset nursing for player")) {
                document.forms["formPlayer"].operation.value = "resetNursing";
                document.forms["formPlayer"].playerid.value = plaID;
                document.forms["formPlayer"].submit();
            }
        }
        function clickDeleteNursing(plaID, name) {
            if (confirm("Delete nursing " + name)) {
                document.forms["formPlayer"].operation.value = "deleteNursing";
                document.forms["formPlayer"].playerid.value = plaID;
                document.forms["formPlayer"].nursingName.value = name;
                document.forms["formPlayer"].submit();
            }
        }
        function clickAddNursing(plaID) {
            document.forms["formPlayer"].operation.value = "addNursing";
            document.forms["formPlayer"].playerid.value = plaID;
            document.forms["formPlayer"].submit();
        }
        function clickCheckNursing(plaID) {
            document.forms["formPlayer"].operation.value = "checkNursing";
            document.forms["formPlayer"].playerid.value = plaID;
            document.forms["formPlayer"].submit();
        }
	</script>
</head>
<body>
<h1>ADMINISTRATION NURSING</h1>
<a href="admin.jsp">Administration</a><br/><br/>
<%if (resultOperation.length() > 0) {%>
<b>RESULTAT OPERATION : <%= paramOperation%></b>
<br/>Result = <%=resultOperation %>
<%} %>
<hr width="95%"/>
<b>Current date=</b><%=Constantes.timestamp2StringDateHour(System.currentTimeMillis()) %><br/>

[<a href="adminNursing.jsp">Refresh</a>]<br/><br/>

<hr width="95%"/>
<b>Select player</b><br/>
<form name="formPlayer" method="post" action="adminNursing.jsp">
	<input type="hidden" name="operation" value=""/>
	<input type="hidden" name="playerid" value=""/>

	Login : <input type="text" name="searchlogin">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Search Player" onclick="clickSearchPlayer()"/><br/>
	Player ID : <input type="text" name="selplaid">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Select Player" onclick="clickSelectPlayer()"/><br/>

    <%if (listPlayer != null) {%>
    <br/>
    <b>List Player</b><br/>
    Nb Result found : <%=listPlayer.size() %>
    <br/><br/>
    <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
        <tr>
            <th>ID</th><th>Login</th><th>Pseudo</th>
            <th>Operations</th>
        </tr>
        <%for (Player p : listPlayer) { %>
        <tr>
            <td><%=p.getID() %></td>
            <td><%=p.getMail() %></td>
            <td><%=p.getNickname() %></td>
            <td>
                <input type="button" onclick="clickShowNursing(<%=p.getID() %>)" value="show nursing"/><br/>
                <input type="button" onclick="clickResetNursing(<%=p.getID() %>)" value="reset nursing"/>
            </td>
        </tr>
        <%} %>
    </table>
    <%} %>

    <%if (listPlayerNursing != null) {%>
    <hr width="95%"/>
    <br/>
    <b>List Nursing for player <%=playerID%></b><br/>
    Nb Result found : <%=listPlayerNursing.size() %><br/>
    Name  :
    <select name="nursingName">
        <%for (String nursing : nursingList) {%>
        <option value="<%=nursing%>"><%=nursing%></option>
        <%}%>
    </select>
    <input type="button" value="add" onclick="clickAddNursing(<%=playerID%>)"/>
    <input type="button" value="check" onclick="clickCheckNursing(<%=playerID%>)"/>
    <%if (nursingAvailability != null) {%>
    <br/>
    Nursing <%=nursingName%> availability : <%=nursingAvailability%>
    <%} %>
    <br/><br/>
	<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
		<tr><th>Name</th><th>Date</th><th>Read</th><th>NotifID</th><th>Operations</th></tr>
		<%for (PlayerNursing pn : listPlayerNursing) {
		%>
		<tr>
			<td><%=pn.nursingName%></td>
			<td><%=Constantes.timestamp2StringDateHour(pn.date)%></td>
			<td><%=pn.read%></td>
			<td><%=pn.notifID%></td>
			<td>
				<input type="button" onclick="clickDeleteNursing(<%=pn.playerID%>, '<%=pn.nursingName%>')" value="delete"/><br/>
			</td>
		</tr>
		<%}%>
	</table>
    <%} %>

    <%if (nursingList != null && !nursingList.isEmpty()) {%>
    <br/>
    <hr width="95%"/>
    <b>List nursing in order</b><br/>
    Nb Result found : <%=nursingList.size() %>
    <br/><br/>
    <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
        <tr><th>Ordre</th><th>Nom</th></tr>
        <%for (int i = 0; i < nursingList.size(); i++) {
        %>
        <tr>
            <td><%=i+1%></td>
            <td><%=nursingList.get(i)%></td>
        </tr>
        <%}%>
    </table>
    <%} %>
</form>

</body>
</html>