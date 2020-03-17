<%@page import="com.funbridge.server.common.Constantes"
        import="com.funbridge.server.common.ContextManager"
        import="com.funbridge.server.presence.FBSession"
        import="com.funbridge.server.presence.PresenceMgr"
        import="com.funbridge.server.store.dao.TransactionDAO"
        import="com.funbridge.server.store.data.Transaction"
        import="com.funbridge.server.tournament.game.GameMgr"
        import="com.gotogames.common.session.Session"
        import="com.gotogames.common.tools.NetTools"
        import="org.apache.commons.lang.StringUtils"
        import="java.net.Authenticator"
        import="java.net.PasswordAuthentication"
        import="java.util.*"
        import="java.util.Map.Entry"
%>
<%
    PresenceMgr presenceMgr = ContextManager.getPresenceMgr();
    List<Session> listSession = presenceMgr.getAllCurrentSession();
    Map<String, Integer> mapDeviceTypeNbSession = new HashMap<String, Integer>();
    Map<String, Integer> mapIosVersionNbSession = new HashMap<String, Integer>();
    Map<String, Integer> mapAndroidVersionNbSession = new HashMap<String, Integer>();
    Map<String, Integer> mapMacVersionNbSession = new HashMap<String, Integer>();
    Map<String, Integer> mapFacebookVersionNbSession = new HashMap<String, Integer>();
    Map<String, Integer> mapWindowsVersionNbSession = new HashMap<String, Integer>();
    Integer cptWebNbSession = 0;
    int nbWebSocket = 0;
    int nbFreemium = 0;
    int nbSessionRCP = 0;
    for (Session s : listSession) {
        if (s instanceof FBSession) {
            FBSession fbs = (FBSession) s;
            int curDeviceNbSession = 0;
            if (mapDeviceTypeNbSession.get(fbs.getDeviceType()) != null) {
                curDeviceNbSession = mapDeviceTypeNbSession.get(fbs.getDeviceType());
            }
            curDeviceNbSession++;
            mapDeviceTypeNbSession.put(fbs.getDeviceType(), curDeviceNbSession);
            int curVersionNbSession = 0;
            if (fbs.getDeviceType().equals(Constantes.DEVICE_TYPE_IOS)) {
    	        if (mapIosVersionNbSession.get(fbs.getClientVersion()) != null) {
    	            curVersionNbSession = mapIosVersionNbSession.get(fbs.getClientVersion());
    	        }
    	        curVersionNbSession++;
    	        mapIosVersionNbSession.put(fbs.getClientVersion(), curVersionNbSession);
            }
            else if (fbs.getDeviceType().equals(Constantes.DEVICE_TYPE_ANDROID)) {
    	        if (mapAndroidVersionNbSession.get(fbs.getClientVersion()) != null) {
    	            curVersionNbSession = mapAndroidVersionNbSession.get(fbs.getClientVersion());
    	        }
    	        curVersionNbSession++;
    	        mapAndroidVersionNbSession.put(fbs.getClientVersion(), curVersionNbSession);
            }
            else if (fbs.getDeviceType().equals(Constantes.DEVICE_TYPE_MAC)) {
    	        if (mapMacVersionNbSession.get(fbs.getClientVersion()) != null) {
    	            curVersionNbSession = mapMacVersionNbSession.get(fbs.getClientVersion());
    	        }
    	        curVersionNbSession++;
    	        mapMacVersionNbSession.put(fbs.getClientVersion(), curVersionNbSession);
            }
            else if (fbs.getDeviceType().equals(Constantes.DEVICE_TYPE_WINPC) || fbs.getDeviceType().equals(Constantes.DEVICE_TYPE_WINPHONE) || fbs.getDeviceType().equals(Constantes.DEVICE_TYPE_WINRT)) {
                if (mapWindowsVersionNbSession.get(fbs.getClientVersion()) != null) {
                    curVersionNbSession = mapWindowsVersionNbSession.get(fbs.getClientVersion());
                }
                curVersionNbSession++;
                mapWindowsVersionNbSession.put(fbs.getClientVersion(), curVersionNbSession);
            }
            if (fbs.getWebSocket() != null) {
                nbWebSocket++;
            }
            if (fbs.isFreemium()) {
                nbFreemium++;
            }
            if (fbs.isRpcEnabled()) {
                nbSessionRCP++;
            }
        }
    }

    TransactionDAO transacDAO = ContextManager.getTransactionDAO();
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY,0);cal.set(Calendar.MINUTE,0);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);
    long tsNowMin = cal.getTimeInMillis();
    cal.add(Calendar.DAY_OF_YEAR, 1);
    long tsNowMax = cal.getTimeInMillis();
    int nbTransBuy = 0, nbTransacApple = 0, nbTransacAndroid = 0, nbTransacAmazon = 0, nbTransacFacebook = 0;
    int nbTransDeal = 0, nbTransSub1y = 0, nbTransSub1m = 0, nbTransSub3m = 0, nbTransSub6m = 0, nbTransSub1yTR = 0, nbTransSub1mTR = 0, nbTransSub3mTR = 0, nbTransSub6mTR = 0;
    int nbTDC1 = 0, nbTDC5 = 0, nbTDC10 = 0, nbTDC20 = 0;
    int nbFede1 = 0, nbFede5 = 0, nbFede10 = 0, nbFede20 = 0;

    cal.add(Calendar.DAY_OF_YEAR, -2);


    int iosNbSessions = 0;
    int iosNbEngine = 0;

    int pushQueueSize = -1, nbPushSentToday=-1;
    double queueTimeCache = 0, queueTimeCompute = 0;
    int queueNoEngine = 0, queueEngine = 0, queueSize = 0;
    String COLOR_RED = "#FF0000", COLOR_ORANGE="#FFA500", COLOR_GREEN="#00FF00";
    String engineColor = COLOR_RED;
    String pushColor = COLOR_RED, tomcatThreadColor = COLOR_RED, tomcatMemoryColor = COLOR_RED;
    int currentThreadsBusy=0, currentThreadCount=0, maxThreads=0, minSpareThreads=0;
    long memoryUsed=0, memoryMax=0;
    String poolName = "";

    try {
        String tempNbSession = NetTools.getURLContent("http://ggfront02p.csgames.net:8080/engine-server-ws/getNbSession.jsp", 5*1000, 10);
        iosNbSessions = Integer.parseInt(tempNbSession);
        String tempNbEngine = NetTools.getURLContent("http://ggfront02p.csgames.net:8080/engine-server-ws/getNbEngine.jsp", 5*1000, 10);
        iosNbEngine = Integer.parseInt(tempNbEngine);

        String tempQueueTime = NetTools.getURLContent("http://ggfront02p.csgames.net:8080/engine-server-ws/getQueueTimeData.jsp", 5*1000, 10);
        String[] temp = tempQueueTime.split(";");
        if (temp.length == 2) {
            queueTimeCache = Double.parseDouble(temp[0]);
            queueTimeCompute = Double.parseDouble(temp[1]);
        }

        String tempQueueSize = NetTools.getURLContent("http://ggfront02p.csgames.net:8080/engine-server-ws/getRequestQueueSize.jsp", 5*1000, 10);
        temp = tempQueueSize.split(";");
        if (temp.length == 3) {
            queueNoEngine = Integer.parseInt(temp[0]);
            queueEngine = Integer.parseInt(temp[1]);
            queueSize = Integer.parseInt(temp[2]);
        }

        engineColor = COLOR_GREEN;
        if (iosNbEngine < 7) {
            engineColor = COLOR_ORANGE;
        }
        if (iosNbEngine < 5) {
            engineColor = COLOR_RED;
        }

        String strPushStatus = NetTools.getURLContentAll("http://ggfront02p.csgames.net:8081/push-server-ws/status.jsp", 5*1000);
        if (strPushStatus != null) {
            String[] tempPushStatus = strPushStatus.split(";");
            if (StringUtils.isNumeric(tempPushStatus[0])) {
                pushQueueSize = Integer.parseInt(tempPushStatus[0]);
            }
            if (tempPushStatus.length > 1 && StringUtils.isNumeric(tempPushStatus[1])) {
                nbPushSentToday = Integer.parseInt(tempPushStatus[1]);
            }
        }
        if (pushQueueSize >= 0) {
            pushColor = COLOR_GREEN;
        }
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("tomcat", "ToM444".toCharArray());
            }
        });
        // tomcat memory status
        String memoryStatus = "";
        List<String> listStatusMemory= NetTools.getURLContentAllList("http://localhost:8080/manager/jmxproxy?get=java.lang:type=Memory&att=HeapMemoryUsage", 10*1000);
        for (String e : listStatusMemory) {
            if (e.contains("contents=")) {
                memoryStatus += e.substring(e.indexOf('{')+1, e.indexOf('}'));
                break;
            }
        }
        String[] tempMemStatus = memoryStatus.split(",");
        for (int i = 0; i < tempMemStatus.length; i++) {
            tempMemStatus[i] = tempMemStatus[i].trim();
            if (tempMemStatus[i].startsWith("used")) {
                memoryUsed=Long.parseLong(tempMemStatus[i].substring("used=".length()))/(1024*1024);
            } else if (tempMemStatus[i].startsWith("max")) {
                memoryMax=Long.parseLong(tempMemStatus[i].substring("max=".length()))/(1024*1024);
            }
        }
        if (memoryMax > 0) {
            if ((100*memoryUsed)/memoryMax < 90) {
                tomcatMemoryColor = COLOR_GREEN;
            }
        }
        // tomcat status thread
        List<String> listStatusThread= NetTools.getURLContentAllList("http://localhost:8080/manager/jmxproxy?qry=Catalina:type=ThreadPool,*", 10*1000);
        for (String e : listStatusThread) {
            if (e.contains("currentThreadsBusy")) {
                currentThreadsBusy = Integer.parseInt(e.substring("currentThreadsBusy:".length()).trim());
            }
            else if (e.contains("currentThreadCount")) {
                currentThreadCount = Integer.parseInt(e.substring("currentThreadCount:".length()).trim());
            }
            else if (e.contains("maxThreads")) {
                maxThreads = Integer.parseInt(e.substring("maxThreads:".length()).trim());
            }
            else if (e.contains("minSpareThreads")) {
                minSpareThreads = Integer.parseInt(e.substring("minSpareThreads:".length()).trim());
            }
            else if (e.contains("name")) {
                poolName = e.substring("name:".length()).trim();
            }
        }
        if (currentThreadsBusy < (maxThreads-1000)) {
            tomcatThreadColor = COLOR_GREEN;
        }
    } catch (Exception e) {

    }
%>
<html>
    <head>
        <title>FUNBRIDGE SERVER STATUS</title>
        <!-- <meta name="viewport" content="user-scalable=no,width=device-width" /> -->
        <meta name="viewport" id="iphone-viewport" content="width=300" />
        <style>
            body, td{font-size:14px; font-family:Arial;}
        </style>
    </head>
    <body>
        <h3>FUNBRIDGE SERVER STATUS</h3>
        <b>Current date : </b> <%=Constantes.timestamp2StringDateHour(System.currentTimeMillis()) %><br/>
        <br/>
        <% boolean maintenanceMode = presenceMgr.isServiceMaintenance(); %>
        <table cellspacing="0" cellpadding="1" border="1">
            <tr>
                <td><b>Sessions</b></td><td><b><%=presenceMgr.getNbSession()%></b> - WS=<%=nbWebSocket %> - RPC=<%=nbSessionRCP%> - maintenance=<%=maintenanceMode %></b></td><td bgcolor="<%=maintenanceMode?COLOR_RED:COLOR_GREEN %>">&nbsp;&nbsp;&nbsp;&nbsp;</td>
            </tr>
            <tr>
                <td rowspan="2"><b>Tomcat Status</b></td>
                <td><b>Memory</b> Percent : <%=(memoryMax>0?(100*memoryUsed)/memoryMax:0)%>%</td><td bgcolor="<%=tomcatMemoryColor%>">&nbsp;&nbsp;&nbsp;&nbsp;</td>
            </tr>
            <tr>
                <td><b>Thread</b> busy=<%=currentThreadsBusy %> - count=<%=currentThreadCount%> - max=<%=maxThreads%> - minSpareThreads=<%=minSpareThreads%> - poolName=<%=poolName%></td><td bgcolor="<%=tomcatThreadColor%>">&nbsp;&nbsp;&nbsp;&nbsp;</td>
            </tr>
            <tr>
                <td rowspan="3"><b>ENGINE STATUS</b></td><td>Sessions=<%=iosNbSessions %> - Engine=<%=iosNbEngine %></td><td bgcolor="<%=engineColor%>">&nbsp;&nbsp;&nbsp;&nbsp;</td>
            </tr>
            <tr>
                <td>Time Cache=<%=queueTimeCache %> - Compute=<%=queueTimeCompute %></td><td bgcolor="<%=engineColor%>">&nbsp;&nbsp;&nbsp;&nbsp;</td>
            </tr>
            <tr>
                <td>Queue No Engine=<%=queueNoEngine %> - Engine=<%=queueEngine %> - Size=<%=queueSize %></td><td bgcolor="<%=engineColor%>">&nbsp;&nbsp;&nbsp;&nbsp;</td>
            </tr>
            <tr>
                <td><b>Push Service</b></td><td>Queue size=<%=pushQueueSize %> - Nb sent today=<%=nbPushSentToday%></td><td bgcolor="<%=pushColor %>">&nbsp;&nbsp;&nbsp;&nbsp;</td>
            </tr>
        </table>
        <b>Request Waiting</b>=<%=ContextManager.getEngineService().getMapStepData().size()%><br>
        <br/>
        <b>Nb Sessions=<%=presenceMgr.getNbSession() %></b>&nbsp;-&nbsp;Nb WebSocket=<%=nbWebSocket %>&nbsp;-&nbsp;
        <%
            Iterator<Entry<String, Integer>> itDev = mapDeviceTypeNbSession.entrySet().iterator();
            String sessionType = "";
            while (itDev.hasNext()) {
                Entry<String, Integer> e = itDev.next();
                if (sessionType.length() > 0){sessionType+=" - ";}
                sessionType += e.getKey() + "="+e.getValue().intValue();
            }
        %><%=sessionType%><br/>
        Versions IOS :&nbsp;
        <%
            Iterator<Entry<String, Integer>> itIosVer = mapIosVersionNbSession.entrySet().iterator();
            String iosVersionType = "";
            while (itIosVer.hasNext()) {
                Entry<String, Integer> e = itIosVer.next();
                if (iosVersionType.length() > 0){iosVersionType+= " - ";}
                iosVersionType += e.getKey() + "=" + e.getValue().intValue();
            }
        %><%=iosVersionType%><br/>
        Versions Mac :&nbsp;
        <%
            Iterator<Entry<String, Integer>> itMacVer = mapMacVersionNbSession.entrySet().iterator();
            String macVersionType = "";
            while (itMacVer.hasNext()) {
                Entry<String, Integer> e = itMacVer.next();
                if (macVersionType.length() > 0){macVersionType+= " - ";}
                macVersionType += e.getKey() + "=" + e.getValue().intValue();
            }
        %><%=macVersionType%><br/>
        Versions Android :&nbsp;
        <%
            Iterator<Entry<String, Integer>> itAndroidVer = mapAndroidVersionNbSession.entrySet().iterator();
            String androidVersionType = "";
            while (itAndroidVer.hasNext()) {
                Entry<String, Integer> e = itAndroidVer.next();
                if (androidVersionType.length() > 0){androidVersionType+= " - ";}
                androidVersionType += e.getKey() + "=" + e.getValue().intValue();
            }
        %><%=androidVersionType%><br/>
        Versions Facebook :&nbsp;
        <%
            Iterator<Entry<String, Integer>> itFacebookVer = mapFacebookVersionNbSession.entrySet().iterator();
            String facebookVersionType = "";
            while (itFacebookVer.hasNext()) {
                Entry<String, Integer> e = itFacebookVer.next();
                if (facebookVersionType.length() > 0){facebookVersionType+= " - ";}
                facebookVersionType += e.getKey() + "=" + e.getValue().intValue();
            }
        %><%=facebookVersionType%><br/>
        Versions Windows :&nbsp;
        <%
            Iterator<Entry<String, Integer>> itWindowsVer = mapWindowsVersionNbSession.entrySet().iterator();
            String windowsVersionType = "";
            while (itWindowsVer.hasNext()) {
                Entry<String, Integer> e = itWindowsVer.next();
                if (windowsVersionType.length() > 0){windowsVersionType+= " - ";}
                windowsVersionType += e.getKey() + "=" + e.getValue().intValue();
            }
        %><%=windowsVersionType%><br/>
        Versions Web :&nbsp;<%=cptWebNbSession %>
        <br/>
        <br/>
        <b>Nb Freemium=</b><%=nbFreemium%><br/>
        <br/>
        <b>Game Pool Thread status</b>
        <table cellspacing="0" cellpadding="1" border="1">
            <tr><th>Category</th><th>NbPlayers</th><!--<th>SynchroMode</th><th>Websocket</th>--><th>ThreadPool</th></tr>
            <% for (GameMgr g : ContextManager.getListGameMgr()) {%>
            <tr>
                <td><%=g.getTournamentMgr().getTournamentCategoryName()%></td>
                <td><%=g.getPlayerRunningSize()%></td>
                <!--<td><%=g.isPlaySynchroMethod()?"SYNC":"ASYNC"%></td>
                <td>
                    Websocket : <% if (g.isEngineWebSocketEnable()) {%>
                    ON - active:<%=g.getEngine().getWebsocketMgr().getPool().getNumActive()%> - Nb GetResult:<%=g.getEngine().getWebsocketMgr().getNbCommandGetResult()%>
                    <%} else {%>
                    OFF
                    <%}%>
                </td>-->
                <td>
                    <%=g.getThreadPoolDataPlay().activeCount%>
                </td>
            </tr>
            <%}%>
        </table>
        <b>Cache Player</b> : size=<%=ContextManager.getPlayerCacheMgr().getCacheSize()%><br/>
       day %></td></tr>
        </table>

    </body>
</html>
