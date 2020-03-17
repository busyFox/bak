<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.common.FBConfiguration"%>
<%@page import="com.funbridge.server.message.ChatMgr"%>
<%@page import="com.funbridge.server.player.PlayerMgr"%>
<%@page import="com.funbridge.server.player.cache.PlayerCache"%>
<%@page import="com.funbridge.server.player.cache.PlayerCacheMgr"%>
<%@page import="com.funbridge.server.player.data.*"%>
<%@page import="com.funbridge.server.store.StoreMgr"%>
<%@ page import="com.funbridge.server.tournament.serie.TourSerieMgr" %>
<%@ page import="com.funbridge.server.ws.FBWSResponse" %>
<%@ page import="com.funbridge.server.ws.player.WSPlayer" %>
<%@ page import="com.funbridge.server.ws.presence.CreateAccountParam" %>
<%@ page import="com.funbridge.server.ws.presence.PresenceServiceRest" %>
<%@ page import="com.funbridge.server.ws.presence.PresenceServiceRestImpl" %>
<%@ page import="com.gotogames.common.crypt.AESCrypto" %>
<%@ page import="com.gotogames.common.tools.JSONTools" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<html>
<%
    PlayerMgr playerMgr = ContextManager.getPlayerMgr();
    PlayerCacheMgr playerCacheMgr = ContextManager.getPlayerCacheMgr();
    TourSerieMgr serieMgr = ContextManager.getTourSerieMgr();
    String paramOperation = request.getParameter("operation");
    String resultOperation = "";
    String playerId = request.getParameter("playerid");
    if (playerId == null) { playerId = ""; }
    List<Player> listPlayer = null;
    List<WSPlayer> listSuggestion = null;
    List<Device> listDevice = null;
    Player player = null;
    Device device = null;
    Privileges privileges = null;
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
        else if (paramOperation.equals("createPlayer")) {
            String pseudo = request.getParameter("pseudo");
            String password = request.getParameter("password");
            String mail = request.getParameter("mail");
            String lang = request.getParameter("lang");
            PresenceServiceRestImpl presenceServ = (PresenceServiceRestImpl)ContextManager.getContext().getBean("presenceService");
            CreateAccountParam param = new CreateAccountParam();
            param.deviceID = "support";
            param.deviceInfo = "support";
            param.deviceType = "ios";
            param.lang = lang;
            param.mail = mail;
            param.pseudo = pseudo;
            param.password = password;
            JSONTools jsonTools = new JSONTools();
            String strParam = jsonTools.transform2String(param, false);
            PresenceServiceRest.CreateAccount3Param paramCrypt = new PresenceServiceRest.CreateAccount3Param();
            paramCrypt.data = AESCrypto.crypt(strParam,Constantes.CRYPT_KEY);
            FBWSResponse resp = presenceServ.createAccount3(paramCrypt);
            if (resp.getException() != null) {
                resultOperation = "create account failed : "+resp.getException().message;
            } else {
                playerMgr = ContextManager.getPlayerMgr();
                player = playerMgr.getPlayerByMail(mail);
                resultOperation = "create account sucess - player="+player;
                listPlayer = new ArrayList<Player>();
                listPlayer.add(player);
            }
        }
        else if (paramOperation.equals("searchDevice")) {
            String searchDeviceID = request.getParameter("searchdeviceid");
            if (searchDeviceID != null) {
                resultOperation = "ok - searchDeviceID="+searchDeviceID;
                device = playerMgr.getDeviceForStrID(searchDeviceID);
                if (device != null) {
                    listDevice = new ArrayList<Device>();
                    resultOperation += " - 1 device found";
                    listDevice.add(device);
                } else {
                    resultOperation += " - no device found";
                }
            } else {
                resultOperation = "Error param searchdeviceid not found !";
            }
        }
        else if (paramOperation.equals("selectionDevice")) {
            String seldeviceid = request.getParameter("seldeviceid");
            if (seldeviceid != null) {
                resultOperation = "ok - seldeviceid="+seldeviceid;
                device = playerMgr.getDevice(Long.parseLong(seldeviceid));
                if (device != null) {
                    listDevice = new ArrayList<Device>();
                    resultOperation += " - 1 device found";
                    listDevice.add(device);
                } else {
                    resultOperation += " - no device found";
                }
            } else {
                resultOperation = "Error param seldeviceid not found !";
            }
        }
        else if (paramOperation.equals("listDeviceForPlayer")) {
            String playerID = request.getParameter("playerid");
            if (playerID != null) {
                player = playerMgr.getPlayer(Long.parseLong(playerID));
                if (player != null) {
                    listDevice = playerMgr.getListDeviceForPlayer(Long.parseLong(playerID));
                    resultOperation = "List device for player="+player+" - nb device="+(listDevice!=null?listDevice.size():0);
                } else {
                    resultOperation = "Error player ID not valid !";
                }
            } else {
                resultOperation = "Error param playerid not found !";
            }
        }
        else if (paramOperation.equals("removeDeviceForPlayer")) {
            String playerID = request.getParameter("playerid");
            String deviceID = request.getParameter("deviceid");
            if (playerID != null && deviceID != null) {
                if (playerMgr.deletePlayerDevice(Long.parseLong(playerID), Long.parseLong(deviceID))) {
                    resultOperation = "Success to remove device for player - playerID="+playerID+" - deviceID="+deviceID;
                } else {
                    resultOperation = "Failed to remove device for player - playerID="+playerID+" - deviceID="+deviceID;
                }
            } else {
                resultOperation = "Error param playerid or deviceid not found !";
            }
        }
        else if (paramOperation.equals("listPlayerForDevice")) {
            String deviceID = request.getParameter("deviceid");
            if (deviceID != null) {
                device = playerMgr.getDevice(Long.parseLong(deviceID));
                if (device != null) {
                    listPlayer = playerMgr.getListPlayerForDevice(device.getID());
                    resultOperation = "List player for device="+device+" - nb player="+(listPlayer!=null?listPlayer.size():0);
                } else {
                    resultOperation = "Error device ID not valid !";
                }
            } else {
                resultOperation = "Error param deviceid not found !";
            }
        }
        else if (paramOperation.equals("deleteDevice")) {
            String deviceID = request.getParameter("deviceid");
            if (deviceID != null) {
                device = playerMgr.getDevice(Long.parseLong(deviceID));
                if (device != null) {
                    if (playerMgr.deleteDevice(device.getID())) {
                        resultOperation = "Delete sucess for device="+device;
                    } else {
                        resultOperation = "Error to remove device="+device;
                    }
                    device = null;
                } else {
                    resultOperation = "Error device ID not valid !";
                }
            } else {
                resultOperation = "Error param deviceid not found !";
            }
        }
        else if (paramOperation.equals("listFriendForPlayer")) {
            String playerID = request.getParameter("playerid");
            if (playerID != null) {
                player = playerMgr.getPlayer(Long.parseLong(playerID));
                listPlayer = playerMgr.listFriendForPlayer(Long.parseLong(playerID));
                resultOperation = "List friend for player={"+player+"} - nbFriend="+listPlayer.size();
            } else {
                resultOperation = "Error param playerid not found !";
            }
        }
        else if (paramOperation.equals("infoAutoRenewalSubscription")) {
            String playerID = request.getParameter("playerid");
            if (playerID != null) {
                player = playerMgr.getPlayer(Long.parseLong(playerID));

            } else {
                resultOperation = "Error param playerid not found !";
            }
        }
        else if (paramOperation.equals("privilegePlayer")) {
            String playerID = request.getParameter("playerid");
            if (playerID != null) {
                player = playerMgr.getPlayer(Long.parseLong(playerID));
                if (player != null) {
                    privileges = playerMgr.getPrivilegesForPlayer(player.getID());
                    if (privileges == null) {
                        privileges = new Privileges();
                        privileges.playerID = player.getID();
                        resultOperation = "No privilege data found for this player - use default : "+privileges;
                    } else {
                        resultOperation = "Privilege data found for this player - "+privileges;
                    }
                } else {
                    resultOperation = "Error no player found with ID="+playerID;
                }
            }
        }
        else if (paramOperation.equals("savePrivilegePlayer")) {
            String playerID = request.getParameter("playerid");
            if (playerID != null) {
                player = playerMgr.getPlayer(Long.parseLong(playerID));
                if (player != null) {
                    privileges = playerMgr.getPrivilegesForPlayer(player.getID());
                    if (privileges == null || privileges.playerID == 0) {
                        privileges = new Privileges();
                        privileges.playerID = player.getID();
                    }
                    privileges.serverChange = request.getParameter("privilegeServerChange")!=null;
                    playerMgr.setPrivilegesForPlayer(privileges);
                    resultOperation = "Set privilege for player = "+privileges;
                } else {
                    resultOperation = "Error no player found with ID="+playerID;
                }
            }
        }
        else if (paramOperation.equals("createPlayersTEST")) {
            List<Player> playersTest = ContextManager.getPlayerMgr().createPlayersTest();
            resultOperation = "Nb players TEST created="+playersTest.size();
        }
        else if (paramOperation.equals("createPlayerHandicapTEST")) {
            List<Player> playersTest = ContextManager.getPlayerMgr().listPlayersTest();
            List<PlayerHandicap> playerHandicapList = playerMgr.createPlayerHandicapForTest(playersTest);
            resultOperation = "Nb players TEST ="+playersTest.size()+" - nb player handicap created="+playerHandicapList.size();
        }
        else if (paramOperation.equals("suggestionFriend")) {
            String playerID = request.getParameter("playerid");
            if (playerID != null) {
                player = playerMgr.getPlayer(Long.parseLong(playerID));
                if (player != null) {
                    List<WSPlayer> suggestions = playerMgr.getSuggestionForPlayer(player);
                    resultOperation = "Nb suggestions="+suggestions.size()+"<br/>";
                    for (WSPlayer e : suggestions) {
                        resultOperation += e.playerID+" - pseudo="+e.pseudo+"<br/>";
                    }
                } else {
                    resultOperation = "Error no player found with ID="+playerID;
                }
            }
        }
        else if (paramOperation.equals("advertisingPlayer")) {
            String playerID = request.getParameter("playerid");
            if (playerID != null) {
                player = playerMgr.getPlayer(Long.parseLong(playerID));
                if (player != null) {
                    resultOperation = "Player="+player+" - nbDealPlayed="+player.getNbPlayedDeals();
                } else {
                    resultOperation = "Error no player found with ID="+playerID;
                }
            }
        }
        else if (paramOperation.equals("getAdvertising")) {
            int nbDeals = Integer.parseInt(request.getParameter("advertisingNbDeals"));
            resultOperation = "For nbDeals="+nbDeals;
        }
        else if (paramOperation.equals("createPlayerArgine")) {
            Player playerArgine = playerMgr.getPlayer(Constantes.PLAYER_ARGINE_ID);
            if (playerArgine == null) {
                resultOperation = "Create player argine result="+ContextManager.getPlayerMgr().createPlayerWithNativeMethod(Constantes.PLAYER_ARGINE_ID, "00000000000",0, "argine@goto-games.com", "Arg1n8", "Argine", "fr", "fr", Constantes.PLAYER_FUNBRIDGE_COUNTRY, Constantes.stringDate2Timestamp("21/06/2017"));
            } else {
                resultOperation = "Player argine already exist = "+playerArgine;
            }
        }
    }
%>
<head>
    <title>Funbridge Server - Administration Player</title>
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
        function clickSuggestionPlayer(plaID) {
            document.forms["formPlayer"].operation.value = "suggestionFriend";
            document.forms["formPlayer"].playerid.value = plaID;
            document.forms["formPlayer"].submit();
        }
        function clickCreatePlayer() {
            document.forms["formPlayer"].operation.value = "createPlayer";
            document.forms["formPlayer"].submit();
        }
        function clickCreatePlayersTEST() {
            if (confirm("Create players for TEST ?")) {
                document.forms["formPlayer"].operation.value = "createPlayersTEST";
                document.forms["formPlayer"].submit();
            }
        }
        function clickCreatePlayerHandicapTEST() {
            if (confirm("Create players for TEST ?")) {
                document.forms["formPlayer"].operation.value = "createPlayerHandicapTEST";
                document.forms["formPlayer"].submit();
            }
        }
        function clickSearchDevice() {
            document.forms["formPlayer"].operation.value = "searchDevice";
            document.forms["formPlayer"].submit();
        }
        function clickSelectDevice() {
            document.forms["formPlayer"].operation.value = "selectionDevice";
            document.forms["formPlayer"].submit();
        }
        function clickRemoveDeviceForPlayer(deviceID, playerID) {
            if (confirm("Remove device="+deviceID+" for player="+playerID+" ?")) {
                document.forms["formPlayer"].operation.value = "removeDeviceForPlayer";
                document.forms["formPlayer"].playerid.value = playerID;
                document.forms["formPlayer"].deviceid.value = deviceID;
                document.forms["formPlayer"].submit();
            }
        }
        function clickListDeviceForPlayer(plaID) {
            document.forms["formPlayer"].operation.value = "listDeviceForPlayer";
            document.forms["formPlayer"].playerid.value = plaID;
            document.forms["formPlayer"].submit();
        }
        function clickListPlayerForDevice(deviceID) {
            document.forms["formPlayer"].operation.value = "listPlayerForDevice";
            document.forms["formPlayer"].deviceid.value = deviceID;
            document.forms["formPlayer"].submit();
        }
        function clickDeleteDevice(deviceID) {
            if (confirm("Delete device="+deviceID+" ?")) {
                document.forms["formPlayer"].operation.value = "deleteDevice";
                document.forms["formPlayer"].deviceid.value = deviceID;
                document.forms["formPlayer"].submit();
            }
        }
        function clickCreationSessionForPlayer(playerID) {
            if (confirm("Create session for player="+playerID+" ?")) {
                document.forms["formPlayer"].operation.value = "creationSession";
                document.forms["formPlayer"].playerid.value = playerID;
                document.forms["formPlayer"].submit();
            }
        }
        function clickListFriendForPlayer(playerID) {
            document.forms["formPlayer"].operation.value = "listFriendForPlayer";
            document.forms["formPlayer"].playerid.value = playerID;
            document.forms["formPlayer"].submit();
        }
        function clickInfoAutoRenewalSubscription(playerID) {
            document.forms["formPlayer"].operation.value = "infoAutoRenewalSubscription";
            document.forms["formPlayer"].playerid.value = playerID;
            document.forms["formPlayer"].submit();
        }
        function clickSeriePlayer(playerID) {
            window.open("adminSeriePlayer.jsp?playerID="+playerID);
            return false;
        }
        function clickPrivilegePlayer(plaID) {
            document.forms["formPlayer"].operation.value = "privilegePlayer";
            document.forms["formPlayer"].playerid.value = plaID;
            document.forms["formPlayer"].submit();
        }
        function clickSavePrivilege(plaID) {
            if (confirm("Save privilege for player="+plaID+" ?")) {
                document.forms["formPlayer"].operation.value = "savePrivilegePlayer";
                document.forms["formPlayer"].playerid.value = plaID;
                document.forms["formPlayer"].submit();
            }
        }
        function clickAdvertisingPlayer(plaID) {
            document.forms["formPlayer"].operation.value = "advertisingPlayer";
            document.forms["formPlayer"].playerid.value = plaID;
            document.forms["formPlayer"].submit();
        }
        function clickGetAdvertising() {
            document.forms["formPlayer"].operation.value = "getAdvertising";
            document.forms["formPlayer"].submit();
        }

        function clickCreatePlayerArgine() {
            document.forms["formPlayer"].operation.value = "createPlayerArgine";
            document.forms["formPlayer"].submit();
        }
    </script>
</head>
<body>
<h1>ADMINISTRATION PLAYER</h1>
<a href="admin.jsp">Administration</a><br/><br/>
<%if (resultOperation.length() > 0) {%>
<b>RESULTAT OPERATION : <%= paramOperation%></b>
<br/>Result = <%=resultOperation %>
<hr/>
<%} %>
<form name="formPlayer" method="post" action="adminPlayer.jsp">
    <input type="hidden" name="operation" value=""/>
    <input type="hidden" name="deviceid" value=""/>
    <input type="hidden" name="playerid" value=""/>
    <%if (player != null) { %>
    Player = <%=player %><br/>
    <%} %>
    <%if (device != null) { %>
    Device = <%=device %><br/><br/><hr />
    <%} %>
    <% if (privileges != null && player != null) {%>
    Privilege = Server change : <input type="checkbox" name="privilegeServerChange" <%=privileges.serverChange?"checked":""%>> --
    <br/>
    <input type="button" value="Save Privilege" onclick="clickSavePrivilege(<%=player.getID()%>)">
    <hr />
    <%}%>
    Date next birthday scheduler : <%=Constantes.timestamp2StringDateHour(playerMgr.getDateNextBirthdayTaskScheduler())%>Date next communityData scheduler : <%=Constantes.timestamp2StringDateHour(playerMgr.getDateNextCommunityTaskScheduler())%><br/>
    <br/>
    <b>Advertising</b>
    Advertising enable : <%=FBConfiguration.getInstance().getIntValue("general.advertising.enable", 1) == 1%><br/>
    Compute type for nb deals = <input type="number" name="advertisingNbDeals"><input type="button" value="Get advertising" onclick="clickGetAdvertising()"/><br/>
    <hr />
    <b>Find player</b>
    <br/>
    Login : <input type="text" name="searchlogin"><input type="button" value="Search Player" onclick="clickSearchPlayer()"/><br/>
    Player ID : <input type="text" name="selplaid" value="<%=playerId%>"><input type="button" value="Select Player" onclick="clickSelectPlayer()"/><br/>
    <hr />
    <b>Find Device</b>
    <br/>
    DeviceID String : <input type="text" name="searchdeviceid"><input type="button" value="Search Device" onclick="clickSearchDevice()"/><br/>
    Device ID : <input type="text" name="seldeviceid"><input type="button" value="Select Device" onclick="clickSelectDevice()"/><br/>
    <hr />
    <b>Create Player</b>
    <table>
        <tr><td>Pseudo</td><td><input type="text" name="pseudo"></td></tr>
        <tr><td>Password</td><td><input type="text" name="password"></td></tr>
        <tr><td>Mail</td><td><input type="text" name="mail"></td></tr>
        <tr><td>Lang (fr, en ...)</td><td><input type="text" name="lang"></td></tr>
    </table>
    <input type="button" value="Create Player" onclick="clickCreatePlayer()"/><br/>
    <br/>
    <input type="button" value="Create Player Argine" onclick="clickCreatePlayerArgine()"/><br/>
    <br/>
    <input type="button" value="Create Players TEST" onclick="clickCreatePlayersTEST()"/><input type="button" value="Create Player Handicap TEST" onclick="clickCreatePlayerHandicapTEST()"/>
    <hr />
    <%if (listPlayer != null) {%>
    <b>List Player</b><br/>
    Nb Result found : <%=listPlayer.size() %>
    <br/><br/>
    <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
        <tr>
            <th>ID</th><th>Login</th><th>Pseudo</th><th>Langue</th>
            <th>Certif Mail</th><th>Dernière connexion</th><th>Credit - Abo</th><th>Donnes</th>
            <th>PlayerCache</th><th>Bonus</th>
            <th>Info Serie</th><th>Location</th>
            <th>Nb Devices</th><th>Nb Friend</th>
            <th>Operations</th><th>Memory</th><th>Settings</th>
        </tr>
        <%for (Player p : listPlayer) {
            PlayerCache pc = playerCacheMgr.getPlayerCache(p.getID());
            PlayerLocation playerLocation = playerMgr.getPlayerLocation(p.getID());
            String urlGMLocation = "";
            if (playerLocation != null) {
                urlGMLocation = "http://maps.google.com/maps?q="+playerLocation.location.latitude+","+playerLocation.location.longitude;
            }
        %>
        <tr>
            <td><%=p.getID() %></td>
            <td><%=p.getMail() %></td>
            <td><%=p.getNickname() %></td>
            <td><%=p.getLang() %></td>
            <td><%=Constantes.timestamp2StringDateHour(p.getLastConnectionDate()) %></td>
            <td><%=p.getCreditAmount()%> - <%=p.getSubscriptionExpirationDate()>0?Constantes.timestamp2StringDateHour(p.getSubscriptionExpirationDate()):""%></td>
            <td><%=p.getNbPlayedDeals()%></td>
            <td><%=(pc!=null)?pc.toString():"null" %></td>
            <td><%=serieMgr.buildSerieStatusForPlayer(pc)%></td>
            <td><%=playerLocation!=null?"Date last update : "+Constantes.timestamp2StringDateHour(playerLocation.dateLastUpdate)+" - <a href=\""+urlGMLocation+"\" target=\"_blank\">GoogleMaps</a>":"No location"%></td>
            <td><%=playerMgr.countDeviceForPlayer(p.getID()) %></td>
            <td><%=playerMgr.countLinkForPlayerAndType(p.getID(), Constantes.PLAYER_LINK_TYPE_FRIEND)%></td>
            <td>
                <%if (device!=null) { %>
                <input type="button" onclick="clickRemoveDeviceForPlayer(<%=device.getID() %>,<%=p.getID() %>)" value="Remove Device for Player"/>
                <%} %>
                <input type="button" style="width:100%" onclick="clickListFriendForPlayer(<%=p.getID() %>)" value="List Friend"/>
                <input type="button" style="width:100%" onclick="clickListDeviceForPlayer(<%=p.getID() %>)" value="List Device"/>
                <input type="button" style="width:100%" onclick="clickSuggestionPlayer(<%=p.getID() %>)" value="Suggestion"/>
                <input type="button" style="width:100%" onclick="clickSeriePlayer(<%=p.getID() %>)" value="Info Serie"/>
                <input type="button" style="width:100%" onclick="clickInfoAutoRenewalSubscription(<%=p.getID() %>)" value="Info Subscription"/>
                <input type="button" style="width:100%" onclick="clickPrivilegePlayer(<%=p.getID() %>)" value="Privilege"/>
                <input type="button" style="width:100%" onclick="clickAdvertisingPlayer(<%=p.getID() %>)" value="Advertising"/>
            </td>
            <td>
                <a href="adminGenericMemoryTournamentPlayer.jsp?playerid=<%=p.getID() %>&category=<%=Constantes.TOURNAMENT_CATEGORY_TRAINING%>">TRAINING</a>
            </td>
            <td><%=p.getSettings()%></td>
        </tr>
        <%} %>
    </table>
    <%} %>
    <%if (listDevice != null) {%>
    <b>List Device</b><br/>
    Nb Result found : <%=listDevice.size() %>
    <br/><br/>
    <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
        <tr>
            <th>ID</th><th>DeviceID</th><th>Info</th><th>Type</th><th>Date création</th>
            <th>Date Dernière connexion</th><th>Bonus</th><th>Lang</th><th>Client Version</th><th>Nb Player</th>
            <th>Last PlayerID</th><th>Operations</th><th>Push Token</th>
        </tr>
        <%for (Device d : listDevice) {%>
        <tr>
            <td><%=d.getID() %></td>
            <td><%=d.getDeviceID() %></td>
            <td><%=d.getDeviceInfo() %></td>
            <td><%=d.getType() %></td>
            <td><%=Constantes.timestamp2StringDateHour(d.getDateCreation()) %></td>
            <td><%=Constantes.timestamp2StringDateHour(d.getDateLastConnection()) %>
            <td><%=d.getLang()%></td>
            <td><%=d.getClientVersion() %></td>
            <td><%=playerMgr.countPlayerForDevice(d.getID()) %></td>
            <td><%=d.getLastPlayerID()%></td>
            <td>
                <%if (player!=null) { %>
                <input type="button" onclick="clickRemoveDeviceForPlayer(<%=d.getID() %>,<%=player.getID() %>)" value="Remove Device for Player"/>
                <%} %>
                <input type="button" onclick="clickListPlayerForDevice(<%=d.getID() %>)" value="List Player"/>
                <input type="button" onclick="clickDeleteDevice(<%=d.getID() %>)" value="Delete"/>
            </td>
            <td><%=d.getPushToken() %></td>
        </tr>
        <%} %>
    </table>
    <%} %>
    <%if (listSuggestion != null) {%>
    Nb Suggestion = <%=listSuggestion.size() %>
    <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
        <tr><th>ID</th><th>Pseudo</th></tr>
        <%for (WSPlayer wsp : listSuggestion) {%>
        <tr><td><%=wsp.playerID %></td><td><%=wsp.pseudo %></td></tr>
        <%} %>
    </table>
    <%} %>
</form>
</body>
</html>