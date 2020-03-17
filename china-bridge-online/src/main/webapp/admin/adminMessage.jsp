<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.message.MessageMgr"%>
<%@page import="com.funbridge.server.message.MessageNotifMgr"%>
<%@page import="com.funbridge.server.message.data.MessageNotif"%>
<%@page import="com.funbridge.server.message.data.MessageNotifAll"%>
<%@ page import="com.funbridge.server.message.data.MessageNotifBuffer" %>
<%@ page import="com.funbridge.server.message.data.MessageNotifGroup" %>
<%@ page import="com.funbridge.server.player.data.Player" %>
<%@ page import="com.funbridge.server.presence.FBSession" %>
<%@ page import="com.funbridge.server.texts.TextUIMgr" %>
<%@ page import="com.funbridge.server.ws.event.WSChatHistory" %>
<%@ page import="com.funbridge.server.ws.event.WSMessage" %>
<%@ page import="com.gotogames.common.session.Session" %><%@ page import="com.gotogames.common.tools.StringTools"%><%@ page import="java.util.*"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<%
    request.setCharacterEncoding("UTF-8");
    String resultOperation = "";
    String operation = request.getParameter("operation");
    MessageMgr msgMgr = ContextManager.getMessageMgr();
    MessageNotifMgr notifMgr = ContextManager.getMessageNotifMgr();
    TextUIMgr textUIMgr = ContextManager.getTextUIMgr();
    List<WSMessage> listWSMessageForPlayer = null;
    List<WSChatHistory> listChatHistory = null;
    List<MessageNotifBuffer> listNotifBuffer = null;
    Player pla = null;
    int offsetMsgPla = 0;
    int nbMaxMsgPla = 20;
    int offsetNotifBuffer = 0;
    int nbMaxNotifBuffer = 50;
    long tsCurrent = System.currentTimeMillis();
    if (operation != null) {
        if (operation.equals("listChatHistoryForPlayer")) {
            String playerID = request.getParameter("chatPlayerID");
            listChatHistory = msgMgr.getChatHistory(Long.parseLong(playerID));
            resultOperation = "List chat history for playerID="+playerID;
        }
        else if (operation.equals("listWSMessageForPlayer")) {
            String playerID = request.getParameter("notifPlayerID");
            offsetMsgPla = Integer.parseInt(request.getParameter("offset"));
            pla = ContextManager.getPlayerMgr().getPlayer(Long.parseLong(playerID));
            if (pla != null) {
                listWSMessageForPlayer = notifMgr.listWSMessageForPlayer(pla, offsetMsgPla, nbMaxMsgPla, tsCurrent);
            } else {
                resultOperation = "Failed to list notif for playerID="+playerID+" - no player found !";
            }
        }
        else if (operation.equals("setNotifRead")) {
            String notifType = request.getParameter("notifType");
            String notifID = request.getParameter("notifID");
            String playerID = request.getParameter("notifReadPlayerID");
            List<String> listNotifID = new ArrayList<String>();
            listNotifID.add(notifID);
            // SIMPLE
            if (notifType.equals("0")) {
                if (notifMgr.setNotifSimpleRead(Long.parseLong(playerID), listNotifID)) {
                    resultOperation = "Success to set notif simple read for player=" + playerID + " - notif=" + notifID;
                } else {
                    resultOperation = "Failed to set notif simple read for player=" + playerID + " - notif=" + notifID;
                }
            }
            // ALL
            else if (notifType.equals("1")) {
                if (notifMgr.setNotifAllRead(Long.parseLong(playerID), listNotifID)) {
                    resultOperation = "Success to set notif ALL read for player=" + playerID + " - notif=" + notifID;
                } else {
                    resultOperation = "Failed to set notif ALL read for player=" + playerID + " - notif=" + notifID;
                }
            }
            // GROUP
            else if (notifType.equals("2")) {
                if (notifMgr.setNotifGroupRead(Long.parseLong(playerID), listNotifID)) {
                    resultOperation = "Success to set notif GROUP read for player=" + playerID + " - notif=" + notifID;
                } else {
                    resultOperation = "Failed to set notif GROUP read for player=" + playerID + " - notif=" + notifID;
                }
            }
        } else if (operation.equals("listNotifBuffer")) {
            offsetNotifBuffer = Integer.parseInt(request.getParameter("offset"));
            listNotifBuffer = notifMgr.getListNotifBufferToTransform(0, nbMaxNotifBuffer, offsetNotifBuffer);
        } else if (operation.equals("addNotif")) {
            String notifType = request.getParameter("notifType");
            String recipient = request.getParameter("notifRecipient");
            String notifTemplate = request.getParameter("notifTemplate");
            String notifTextFR = request.getParameter("notifTextFR");
            String notifTextEN = request.getParameter("notifTextEN");
            String notifRichBodyFR = request.getParameter("notifRichBodyFR");
            String notifRichBodyEN = request.getParameter("notifRichBodyEN");
            String notifTitleFR = request.getParameter("notifTitleFR");
            String notifTitleEN = request.getParameter("notifTitleEN");
            String notifTitleIcon = request.getParameter("notifTitleIcon");
            String notifTitleBackGroundColor = request.getParameter("notifTitleBackGroundColor");
            String notifTitleColor = request.getParameter("notifTitleColor");
            String notifActionButtonTextFR = request.getParameter("notifActionButtonTextFR");
            String notifActionButtonTextEN = request.getParameter("notifActionButtonTextEN");
            String notifActionTextFR = request.getParameter("notifActionTextFR");
            String notifActionTextEN = request.getParameter("notifActionTextEN");
            String notifExpiration = request.getParameter("notifExpiration");
            String notifTemplateParamNames = request.getParameter("notifTemplateParamNames");
            String notifTemplateParamValues = request.getParameter("notifTemplateParamValues");
            String notifDateParam = request.getParameter("notifDate");
            String notifExtraFieldNames = request.getParameter("notifExtraFieldNames");
            String notifExtraFieldValues = request.getParameter("notifExtraFieldValues");
            long notifDate = System.currentTimeMillis();

            Map<String, String> mapTemplateParam = new HashMap<String, String>();
            long tsExpiration = 0;
            int displayMask = 0;
            int category = 0;
            boolean bParamOK = true;
            if (notifDateParam != null && notifDateParam.length() > 0) {
                try {
                    notifDate = Constantes.stringDateHour2Timestamp(notifDateParam);
                } catch (Exception e) {
                    resultOperation = "Bad format for notif date creation - "+notifDateParam;
                    bParamOK = false;
                }
            }
            if (bParamOK) {
                try {
                    tsExpiration = Constantes.stringDateHour2Timestamp(notifExpiration);
                } catch (Exception e) {
                    resultOperation = "Bad format for expirationDate - " + notifExpiration;
                    bParamOK = false;
                }
            }
            if (bParamOK) {
                try {
                    displayMask = Integer.parseInt(request.getParameter("notifDisplayMask"));
                } catch (Exception e) {
                    resultOperation = "Bad format for displayMask";
                    bParamOK = false;
                }
            }
            if (bParamOK) {
                try {
                    category = Integer.parseInt(request.getParameter("notifCategory"));
                } catch (Exception e) {
                    resultOperation = "Bad format for category";
                    bParamOK = false;
                }
            }
            if (bParamOK && (recipient == null || recipient.length() == 0)) {
                if (notifType.equals("0")) {
                    resultOperation = "ERROR - recipient is empty";
                    bParamOK = false;
                }
            }
            if (bParamOK && (notifTemplateParamNames != null && notifTemplateParamNames.length() > 0 && notifTemplateParamValues != null && notifTemplateParamValues.length() > 0)) {
                String[] tabParamName = notifTemplateParamNames.split(";");
                String[] tabParamValue = notifTemplateParamValues.split(";");
                if (tabParamName.length == tabParamValue.length) {
                    for (int i = 0; i < tabParamName.length; i++) {
                        mapTemplateParam.put(tabParamName[i], tabParamValue[i]);
                    }
                } else {
                    resultOperation = "ERROR - template parameter names length != values length";
                    bParamOK = false;
                }
            }
            if (bParamOK && (notifTemplate == null || notifTemplate.length() == 0) && (notifTextFR == null || notifTextFR.length() == 0)) {
                resultOperation = "ERROR - notifTextFR is empty";
                bParamOK = false;
            }
            if (bParamOK && (notifTemplate == null || notifTemplate.length() == 0) && (notifTextEN == null || notifTextEN.length() == 0)) {
                resultOperation = "ERROR - notifTextEN is empty";
                bParamOK = false;
            }
            if (bParamOK && (tsExpiration < System.currentTimeMillis())) {
                resultOperation = "ERROR - expirationDate is before now ! - "+notifExpiration;
                bParamOK = false;
            }
            if (bParamOK) {
                if (notifTextFR.contains("\r\n")) {
                    notifTextFR = notifTextFR.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
                }
                if (notifTextEN.contains("\r\n")) {
                    notifTextEN = notifTextEN.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
                }
                if (notifRichBodyFR.contains("\r\n")) {
                    notifRichBodyFR = notifRichBodyFR.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
                }
                if (notifRichBodyEN.contains("\r\n")) {
                    notifRichBodyEN = notifRichBodyEN.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
                }
                // SIMPLE
                if (notifType.equals("0")) {
                    String[] tabRecip = recipient.split(";");
                    for (String s : tabRecip) {
                        long playerID = Long.parseLong(s);
                        MessageNotif notif = notifMgr.createNotif(notifDate, playerID, category, displayMask, tsExpiration,
                                notifTemplate, mapTemplateParam, notifTextEN, notifTextFR, null, notifExtraFieldNames, notifExtraFieldValues,
                                notifTitleFR, notifTitleEN, notifTitleIcon, notifTitleBackGroundColor, notifTitleColor,
                                notifRichBodyFR, notifRichBodyEN, notifActionButtonTextFR, notifActionButtonTextEN,
                                notifActionTextFR, notifActionTextEN);
                        if (resultOperation.length() > 0) {
                            resultOperation += "<br/>";
                        }
                        if (notif != null) {
                            FBSession sessionPlayer = ContextManager.getPresenceMgr().getSessionForPlayerID(playerID);
                            if (sessionPlayer != null) {
                                sessionPlayer.pushEvent(notifMgr.buildEvent(notif, sessionPlayer.getPlayer()));
                            }
                            resultOperation += "Add notif with success for playerID="+s+" - notif="+notif;
                        } else {
                            resultOperation += "Failed to add notif for playerID="+s;
                        }
                    }
                }
                // ALL
                else if (notifType.equals("1")) {
                    MessageNotifAll notifAll = notifMgr.createNotifAll(category, displayMask, tsExpiration,
                            notifTemplate, mapTemplateParam, notifTextEN, notifTextFR, null, notifExtraFieldNames, notifExtraFieldValues,
                            notifTitleFR, notifTitleEN, notifTitleIcon, notifTitleBackGroundColor, notifTitleColor,
                            notifRichBodyFR, notifRichBodyEN, notifActionButtonTextFR, notifActionButtonTextEN,
                            notifActionTextFR, notifActionTextEN);
                    if (notifAll != null) {
                        List<Session> listSession = ContextManager.getPresenceMgr().getAllCurrentSession();
                        for (Session s : listSession) {
                            if (s instanceof FBSession) {
                                FBSession fbs = (FBSession) s;
                                fbs.pushEvent(notifMgr.buildEvent(notifAll, fbs.getPlayer()));
                            }
                        }
                        resultOperation = "Add notifAll with success : notifAll="+notifAll;
                    } else {
                        resultOperation = "Failed to add notifAll";
                    }
                }
                // GROUP
                else if (notifType.equals("2")) {
                    List<Long> listPlayerID = new ArrayList<Long>();
                    if (recipient.length() > 0) {
                        String[] tabRecip = recipient.split(";");
                        for (String s : tabRecip) {
                            listPlayerID.add(Long.parseLong(s));
                        }
                    }
                    MessageNotifGroup notif = notifMgr.createNotifGroup(listPlayerID, category, displayMask, 0, tsExpiration,
                            notifTemplate, mapTemplateParam, notifTextEN, notifTextFR, null, notifExtraFieldNames, notifExtraFieldValues,
                            notifTitleFR, notifTitleEN, notifTitleIcon, notifTitleBackGroundColor, notifTitleColor,
                            notifRichBodyFR, notifRichBodyEN, notifActionButtonTextFR, notifActionButtonTextEN,
                            notifActionTextFR, notifActionTextEN);
                    if (notif != null) {
                        for (Long plaID : listPlayerID) {
                            FBSession sessionPlayer = ContextManager.getPresenceMgr().getSessionForPlayerID(plaID);
                            if (sessionPlayer != null) {
                                sessionPlayer.pushEvent(notifMgr.buildEvent(notif, sessionPlayer.getPlayer()));
                            }
                        }
                        resultOperation += "Add notifGroup with success for playerID="+recipient+" - notif="+notif;
                    } else {
                        resultOperation += "Failed to add notifGroup for playerID="+recipient;
                    }
                }
            }
        } else if (operation.equals("addNotifMaintenance")) {
            String notifExpiration = request.getParameter("notifExpirationMaintenance");
            boolean bParamOK = true;
            long tsExpiration = 0;
            try {
                tsExpiration = Constantes.stringDateHour2Timestamp(notifExpiration);
            } catch (Exception e) {
                resultOperation = "Bad format for expirationDate - " + notifExpiration;
                bParamOK = false;
            }
            if (bParamOK) {
                MessageNotifAll notifAll = notifMgr.createNotifAll(1, 1, tsExpiration, "maintenance", null, null, null, null, null, null,
                        null, null, null, null, null,
                        null, null, null, null, null, null);
                if (notifAll != null) {
                    List<Session> listSession = ContextManager.getPresenceMgr().getAllCurrentSession();
                    for (Session s : listSession) {
                        if (s instanceof FBSession) {
                            FBSession fbs = (FBSession) s;
                            fbs.pushEvent(notifMgr.buildEvent(notifAll, fbs.getPlayer()));
                        }
                    }
                    resultOperation = "Add notifAll with success : notifAll=" + notifAll;
                } else {
                    resultOperation = "Failed to add notifAll";
                }
            }
        } else if (operation.equals("addRandomDuelResult")) {
            long recipientID = Long.parseLong(request.getParameter("ramdomDuelResultPlayerID"));
            MessageNotif temp = notifMgr.createNotifFriendDuelFinish(recipientID, ContextManager.getPlayerMgr().generateRandomPassword("fr"), new Random().nextInt(50), ContextManager.getPlayerMgr().generateRandomPassword("fr"), new Random().nextInt(50));
            resultOperation = "Add notif for recipientID="+recipientID+" - messageNotif="+temp;
        }
    }
    List<String> listTemplate = textUIMgr.getListTemplateNameNotif();
%>
	<head>
		<title>Funbridge Server - Administration</title>
		<script type="text/javascript">
            function clickChatHistory() {
                document.forms["formMsg"].operation.value = "listChatHistoryForPlayer";
                document.forms["formMsg"].submit();
            }
            function clickListMessage(valOffset) {
                document.forms["formMsg"].operation.value = "listWSMessageForPlayer";
                document.forms["formMsg"].offset.value = valOffset;
                document.forms["formMsg"].submit();
            }
            function clickListNotifBuffer(valOffset) {
                document.forms["formMsg"].operation.value = "listNotifBuffer";
                document.forms["formMsg"].offset.value = valOffset;
                document.forms["formMsg"].submit();
            }
            function setNotifRead() {
                document.forms["formMsg"].operation.value = "setNotifRead";
                document.forms["formMsg"].submit();
            }
            function clickHideNotifBufferList() {
                document.forms["formMsg"].operation.value = "";
                document.forms["formMsg"].submit();
            }
            function clickShowNotifBufferList() {
                document.forms["formMsg"].operation.value = "listNotifBuffer";
                document.forms["formMsg"].offset.value = 0;
                document.forms["formMsg"].submit();
            }
            function clickAddNotif() {
                document.forms["formMsg"].operation.value = "addNotif";
                document.forms["formMsg"].submit();
            }
            function clickAddNotifMaintenance() {
                document.forms["formMsg"].operation.value = "addNotifMaintenance";
                document.forms["formMsg"].submit();
            }
            function clickAddRandomDuelResult() {
                document.forms["formMsg"].operation.value = "addRandomDuelResult";
                document.forms["formMsg"].submit();
            }
		</script>
	</head>
	<body>
	<h1>ADMINISTRATION MESSAGE, PUSH & NOTIF</h1>
	<a href="admin.jsp">Administration</a>&nbsp;&nbsp;--&nbsp;&nbsp;<a href="adminMessageTemplate.jsp">Template</a>
	<br/><br/>
	<hr width="95%"/>
	<%if (resultOperation.length() > 0) {%>
	<b>Result operation = <%=resultOperation %></b>
	<br/><hr width="95%"/>
	<%} %>
	<form name="formMsg" method="post" action="adminMessage.jsp">
        <input type="hidden" name="operation" value=""/>
        <input type="hidden" name="offset" value="0"/>

        <hr width="95%"/>
        <b>Chat history for player</b><br/>
        PlayerID = <input type="text" name="chatPlayerID" size="50" value="<%=(pla!=null?pla.getID():"")%>"/><br/>
        <input type="button" value="List message" onclick="clickChatHistory()">
        <br/>
        <% if (listChatHistory != null) {%>
        Chat history size = <%=listChatHistory.size() %><br/>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><th>Player ID</th><th>Pseudo</th><th>Avatar</th><th>Connected</th><th>Last message text</th><th>Last message date</th><th>last message read</th></tr>
            <% for (WSChatHistory wsc : listChatHistory) { %>
            <tr>
                <td><%=wsc.playerID %></td>
                <td><%=wsc.pseudo %></td>
                <td><%=wsc.avatar %></td>
                <td><%=wsc.connected %></td>
                <td><%=wsc.lastMessageText %></td>
                <td><%=Constantes.timestamp2StringDateHour(wsc.lastMessageDate) %></td>
                <td><%=wsc.lastMessageRead %></td>
            </tr>
            <%} %>
        </table>
        <%} %>

        <hr width="95%"/>
        <b>List Notif for player</b><br/>
        PlayerID = <input type="text" name="notifPlayerID" size="50" value="<%=(pla!=null?pla.getID():"")%>"/><br/>
        <input type="button" value="List message" onclick="clickListMessage(0)">
        <br/>
        <% if (listWSMessageForPlayer != null && pla != null) {%>
        List Message for player = <%=pla %><br/>
        Nb total message = <%=notifMgr.countNotifForPlayer(pla.getID(), tsCurrent) %><br/>
        <%if (offsetMsgPla > 0) {%>
        <input type="button" value="previous" onclick="clickListMessage(<%=offsetMsgPla-nbMaxMsgPla%>)"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <%} %>
        <%if (listWSMessageForPlayer.size() == nbMaxMsgPla) {%>
        <input type="button" value="next" onclick="clickListMessage(<%=offsetMsgPla+nbMaxMsgPla%>)"/>
        <%} %>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><th>ID</th><th>category</th><th>display</th><th>Body</th><th>Short</th><th>Date notif</th><th>Date expiration</th><th>Read</th><th>ExtraFields</th></tr>
            <% for (WSMessage wsm : listWSMessageForPlayer) { %>
            <tr>
                <td><%=wsm.ID %></td>
                <td><%=wsm.category %></td>
                <td><%=wsm.displayMask %></td>
                <td><%=wsm.body %></td>
                <td><%=Constantes.timestamp2StringDateHour(wsm.dateReceive) %></td>
                <td><%=Constantes.timestamp2StringDateHour(wsm.dateExpiration) %></td>
                <td><%=wsm.read %></td>
                <td><%=StringTools.listToString(wsm.extraFields) %></td>
            </tr>
            <%} %>
        </table>
        <%} %>

        <hr width="95%"/>
        <b>Set notif read</b><br/>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><td>PlayerID</td><td><input type="text" name="notifReadPlayerID" size="50"/></td></tr>
            <tr><td>Notif type</td><td>
                <input type="radio" name="notifType" value="0" checked="checked"/> Simple (for one player)&nbsp;-&nbsp;
                <input type="radio" name="notifType" value="1" /> ALL (for all players)&nbsp;-&nbsp;
                <input type="radio" name="notifType" value="2" /> GROUP
            </td></tr>
            <tr><td>NotifID</td><td><input type="text" name="notifID" size="100"/></td></tr>
        </table>
        <input type="button" value="Set read" onclick="setNotifRead()">

        <hr width="95%"/>
        <b>Add notif</b><br/>
        Add notif maintenance for all - expiration date = <input type="text" name="notifExpirationMaintenance" size="30" value="<%=Constantes.timestamp2StringDateHour(System.currentTimeMillis()+(5*60*1000))%>"/>
        <input type="button" value="Send Notif Maintenance" onclick="clickAddNotifMaintenance()"><br/>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><td>Notif Type</td><td>
                <input type="radio" name="notifType" value="0" checked="checked"/> Simple (for one player)&nbsp;-&nbsp;
                <input type="radio" name="notifType" value="1" /> ALL (for all players)&nbsp;-&nbsp;
                <input type="radio" name="notifType" value="2" /> GROUP</td>
            </tr>
            <tr><td>Display Mask</td><td><input type="text" name="notifDisplayMask" size="5" value="0"> 0 : NORMAL - 1 : TOPBAR - 2 : DIALOG BOX</td></tr>
            <tr>
                <td>Category</td>
                <td>
                    <select name="notifCategory">
                        <%for (Map.Entry<Integer, String> entry : MessageNotifMgr.mapMessageCategory.entrySet()) {%>
                        <option value="<%=entry.getKey()%>"><%=entry.getValue()%></option>
                        <%}%>
                    </select>
                    <br/>
                    <%--<input type="text" name="notifCategory" size="5" value="1"> 1 : GENERAL - 2 : FRIEND - 3 : SERIE - 4 : TIMEZONE - 5 : TRAINING - 6 : CHALLENGE - 7 : DUEL - 8 : PROMO - 9 : GIFT<br/>--%>
                    <%--Open category page : <input type="checkbox" name="notifOpenCategoryPage">--%>
                </td>
            </tr>
            <tr><td>Date creation</td><td><input type="text" name="notifDate" size="30"/> (Format : dd/MM/yyyy - HH:mm:ss)</td></tr>
            <tr><td>Recipient</td><td><input type="text" name="notifRecipient" size="50"/> (Separator : ";")</td></tr>
            <tr><td>Template</td><td><select name="notifTemplate"><option value="">-- No Template --</option><%for (String t : listTemplate) {%><option value="<%=t%>"><%=t%></option><%}%></select></td></tr>
            <tr><td>Template Parameters name</td><td><input type="text" name="notifTemplateParamNames" size="100"/> (Separator ';')</td></tr>
            <tr><td>Template Parameters value</td><td><input type="text" name="notifTemplateParamValues" size="100"/> (Separator ';')</td></tr>
            <tr><td>Text FR</td><td><textarea name="notifTextFR" rows="5" cols="80"></textarea><!--<input type="text" name="notifTextFR" size="200"/>--></td></tr>
            <tr><td>Text EN</td><td><textarea name="notifTextEN" rows="5" cols="80"></textarea><!--<input type="text" name="notifTextEN" size="200"/>--></td></tr>
            <tr><td>Rich Body FR</td><td><textarea name="notifRichBodyFR" rows="5" cols="80"></textarea></td></tr>
            <tr><td>Rich Body EN</td><td><textarea name="notifRichBodyEN" rows="5" cols="80"></textarea></td></tr>
            <tr><td>Title FR</td><td><input type="text" name="notifTitleFR" size="100"/></td></tr>
            <tr><td>Title EN</td><td><input type="text" name="notifTitleEN" size="100"/></td></tr>
            <tr><td>Title Icon</td><td><input type="text" name="notifTitleIcon" size="100"/></td></tr>
            <tr><td>Title Background Color</td><td><input type="text" name="notifTitleBackGroundColor" size="100"/></td></tr>
            <tr><td>Title Color</td><td><input type="text" name="notifTitleColor" size="100"/></td></tr>
            <tr><td>Action Button Text FR</td><td><input type="text" name="notifActionButtonTextFR" size="100"/></td></tr>
            <tr><td>Action Button Text EN</td><td><input type="text" name="notifActionButtonTextEN" size="100"/></td></tr>
            <tr><td>Action Text FR</td><td><input type="text" name="notifActionTextFR" size="100"/></td></tr>
            <tr><td>Action Text EN</td><td><input type="text" name="notifActionTextEN" size="100"/></td></tr>
            <tr><td>Expiration date</td><td><input type="text" name="notifExpiration" size="30" value="<%=Constantes.timestamp2StringDateHour(System.currentTimeMillis()+(60*60*1000))%>"/> (Format : dd/MM/yyyy - HH:mm:ss)</td></tr>
            <%--<tr>--%>
                <%--<td>Param ACTION-OPEN_URL</td><td><input type="text" name="notifParamActionOPENURL" size="100"/><br/>--%>
                <%--Option LEAVE_APP : <input type="checkbox" name="notifOptionLeaveApp" checked>--%>
                <%--</td>--%>
            <%--</tr>--%>
            <tr><td>Extra fields name</td><td><input type="text" name="notifExtraFieldNames" size="100"/> (Separator ';') <a href="https://gotogames.atlassian.net/wiki/spaces/DOCDEV/pages/2064451/Service+Event#ServiceEvent-ObjetMessageExtraField">doc extra fields</a></td></tr>
            <tr><td>Extra fields value</td><td><input type="text" name="notifExtraFieldValues" size="100"/> (Separator ';')</td></tr>
        </table>
        <input type="button" value="Send Notif" onclick="clickAddNotif()"><br/>

        <hr width="95%"/>
        <b>List notif buffer</b><br/>
        Date next notif buffer transform task : <%=notifMgr.getStringDateNextNotifBufferTransformScheduler()%><br/>
        Last execution : date=<%=Constantes.timestamp2StringDateHour(notifMgr.getTsLastNotifBufferTransformTask())%> - Nb notif process=<%=notifMgr.getNbNotifLastTransformTask()%> - Result=<%=notifMgr.getResultLastNotifBufferTransformTask()%><br/>
        Nb notif buffer : <%=listNotifBuffer!=null?listNotifBuffer.size():"null"%><br/>
        Add random duel result for playerID = <input type="text" name="ramdomDuelResultPlayerID" size="10">&nbsp;&nbsp;&nbsp;<input type="button" value="Add random duel result" onclick="clickAddRandomDuelResult()"><br/>
        <%if (listNotifBuffer != null){ %>
        <input type="button" value="Hide Notif Buffer List" onclick="clickHideNotifBufferList()"><br/>
        <%if (offsetNotifBuffer > 0) {%>
        <input type="button" value="previous" onclick="clickListNotifBuffer(<%=offsetNotifBuffer-nbMaxNotifBuffer%>)"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <%} %>
        <%if (listNotifBuffer.size() == nbMaxNotifBuffer) {%>
        <input type="button" value="next" onclick="clickListNotifBuffer(<%=offsetNotifBuffer+nbMaxNotifBuffer%>)"/>
        <%} %>
        <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
            <tr><th>ID</th><th>Date</th><th>recipientID</th><th>bufferType</th><th>nbUpdate</th><th>msgBodyFR</th><th>msgBodyEN</th></tr>
            <% for (MessageNotifBuffer notifBuffer : listNotifBuffer) {%>
            <tr>
                <td><%=notifBuffer.ID %></td>
                <td><%=Constantes.timestamp2StringDateHour(notifBuffer.dateCreation) %></td>
                <td><%=notifBuffer.recipientID %></td>
                <td><%=notifBuffer.bufferType %></td>
                <td><%=notifBuffer.nbUpdate %></td>
                <td><%=notifBuffer.msgBodyFR %></td>
                <td><%=notifBuffer.msgBodyEN %></td>
            </tr>
            <%}%>
        </table>
        <%} else {%>
        <input type="button" value="Show Notif Buffer List" onclick="clickShowNotifBufferList()"><br/>
        <%}%>
	</form>
	</body>
</html>