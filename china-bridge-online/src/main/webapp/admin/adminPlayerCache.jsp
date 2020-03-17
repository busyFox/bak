<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.player.cache.PlayerCache" %>
<%@ page import="com.funbridge.server.player.cache.PlayerCacheMgr" %>
<%@ page import="com.funbridge.server.tournament.serie.TourSerieMgr" %>
<%@ page import="com.gotogames.common.tools.NumericalTools" %>
<%@ page import="com.gotogames.common.tools.StringTools" %>
<%@ page import="java.util.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    class CacheSerieComputeHandicap {
        private int nbPlayers = 0;
        private double cumulHandicap = 0;
        private String serie;
        public CacheSerieComputeHandicap(String serie) {
            this.serie = serie;
        }
        public void addHandicap(double value) {
            this.cumulHandicap += value;
            nbPlayers++;
        }
        public double computeHandicap() {
            if (nbPlayers > 0) {
                return NumericalTools.round(cumulHandicap / nbPlayers, 4);
            }
            return 0;
        }
    }
    String paramOperation = request.getParameter("operation");
    String resultOperation = "";
    PlayerCacheMgr playerCacheMgr = ContextManager.getPlayerCacheMgr();
    List<PlayerCache> listPlayerCache = new ArrayList<PlayerCache>();
    boolean showAllPlayer = true;
    if (paramOperation != null) {
        if (paramOperation.equals("searchPlayerPseudo")) {
            String searchPseudo = request.getParameter("searchpseudo");
            if (searchPseudo != null) {
                resultOperation = "ok - searchPseudo="+searchPseudo;
                long ts = System.currentTimeMillis();
                PlayerCache pc = playerCacheMgr.getPlayerWithPseudo(searchPseudo);
                if (pc != null) {
                    resultOperation += " - result found - ts="+(System.currentTimeMillis() - ts);
                    listPlayerCache.add(pc);
                    showAllPlayer = false;
                } else {
                    resultOperation += " - No result found - ts="+(System.currentTimeMillis() - ts);
                }
            } else {
                resultOperation = "Error param searchpseudo not found !";
            }
        }
        else if (paramOperation.equals("searchPlayerID")) {
            String searchPlayerID = request.getParameter("searchplaid");
            if (searchPlayerID != null) {
                resultOperation = "ok - searchPlayerID="+searchPlayerID;
                long ts = System.currentTimeMillis();
                PlayerCache pc = playerCacheMgr.getPlayerCache(Long.parseLong(searchPlayerID));
                if (pc != null) {
                    resultOperation += " - result found - ts="+(System.currentTimeMillis() - ts);
                    listPlayerCache.add(pc);
                    showAllPlayer = false;
                } else {
                    resultOperation += " - No result found - ts="+(System.currentTimeMillis() - ts);
                }
            } else {
                resultOperation = "Error param searchpseudo not found !";
            }
        }
        else if (paramOperation.equals("updatePlayerData")) {
            String paramPlayerID = request.getParameter("playerid");
            if (paramPlayerID != null) {
                long playerID = Long.parseLong(paramPlayerID);
                PlayerCache pc = playerCacheMgr.updatePlayerAllData(playerID);
                if (pc != null) {
                    resultOperation += "Update player cache for playerID="+playerID+" - playerCache="+pc;
                } else {
                    resultOperation += "Failed to update player cache for playerID="+playerID;
                }
            } else {
                resultOperation = "Error param playerid not found !";
            }
        }
        else if (paramOperation.equals("removePlayer")) {
            String paramPlayerID = request.getParameter("playerid");
            if (paramPlayerID != null) {
                long playerID = Long.parseLong(paramPlayerID);
                PlayerCache pc = playerCacheMgr.removePlayerCache(playerID);
                if (pc != null) {
                    resultOperation += "Remove player cache for playerID="+playerID+" - playerCache="+pc;
                } else {
                    resultOperation += "Failed to remove player cache for playerID="+playerID;
                }
            } else {
                resultOperation = "Error param playerid not found !";
            }
        }
        else if (paramOperation.equals("reloadAllPlayerAllData")) {
            long ts = System.currentTimeMillis();
            playerCacheMgr.updateAllPlayerAllData();
            resultOperation = "Reload all player ALL date with success - ts="+(System.currentTimeMillis() - ts);
        }
        else if (paramOperation.equals("reloadAllPlayerSerieData")) {
            long ts = System.currentTimeMillis();
            playerCacheMgr.updateAllPlayerSerieData();
            resultOperation = "Reload all player SERIE date with success - ts="+(System.currentTimeMillis() - ts);
        }
        else if (paramOperation.equals("computeAverageHandicapSerie")) {
            List<PlayerCache> listTemp = new ArrayList<PlayerCache>(playerCacheMgr.getCollectionCacheValue());
            Map<String, CacheSerieComputeHandicap> mapComputeHandicap = new HashMap<String, CacheSerieComputeHandicap>();
            for (PlayerCache e : listTemp) {
                if (e.serie != null) {
                    CacheSerieComputeHandicap temp = mapComputeHandicap.get(e.serie);
                    if (temp == null) {
                        temp = new CacheSerieComputeHandicap(e.serie);
                        mapComputeHandicap.put(e.serie, temp);
                    }
                    temp.addHandicap(e.handicap);
                }
            }
            resultOperation = "Average Handicap SERIE:<br>";
            for (CacheSerieComputeHandicap e : mapComputeHandicap.values()) {
                resultOperation += e.serie+" -> "+e.computeHandicap()+" - nbPlayers="+e.nbPlayers+"<br>";
            }
        }
        else if (paramOperation.equals("specialSelection")) {
            List<PlayerCache> listTemp = new ArrayList<PlayerCache>(playerCacheMgr.getCollectionCacheValue());
            List<String> langs = new ArrayList<String>();
            langs.add("de");
            langs.add("it");
            langs.add("nb");
            langs.add("pt");
            langs.add("pt-pt");
            langs.add("tr");
            langs.add("zh");
            langs.add("zh-hant");
            langs.add("zh-hans");
            List<String> seriesToUse = new ArrayList<String>();
            TourSerieMgr serieMgr = ContextManager.getTourSerieMgr();
            seriesToUse.add(TourSerieMgr.SERIE_TOP);
            seriesToUse.add(TourSerieMgr.SERIE_01);
            seriesToUse.add(TourSerieMgr.SERIE_02);
            seriesToUse.add(TourSerieMgr.SERIE_03);
            String currentPeriodSerie = serieMgr.getCurrentPeriod().getPeriodID();
            String previousPeriodSerie = serieMgr.getPeriodIDPrevious();
            List<Long> listPlayerID = new ArrayList<Long>();
            for (PlayerCache e : listTemp) {
                if (e.serie != null && seriesToUse.contains(e.serie) && e.serieLastPeriodPlayed != null && (e.serieLastPeriodPlayed.equals(currentPeriodSerie) || e.serieLastPeriodPlayed.equals(previousPeriodSerie))) {
                    if (langs.contains(e.lang)) {
                        listPlayerID.add(e.ID);
                    }
                }
            }
            resultOperation = "Extract players in series TOP, S01, S02, S03 : nbPlayers="+listPlayerID.size();
            resultOperation += "<br><b>langs</b> :<br/>"+ StringTools.listToString(listPlayerID, ",");
        }
    }

    long ts = System.currentTimeMillis();
    if (showAllPlayer) {
        listPlayerCache = new ArrayList<PlayerCache>(playerCacheMgr.getCollectionCacheValue());
    }
    Collections.sort(listPlayerCache, new Comparator<PlayerCache>() {
        @Override
        public int compare(PlayerCache o1, PlayerCache o2) {
            return Long.compare(o1.ID, o2.ID);
        }
    });
    int idxStart = 0;
    String paramOffset = request.getParameter("listoffset");
    if (paramOffset != null) {
        idxStart = Integer.parseInt(paramOffset);
        if (idxStart < 0) {
            idxStart = 0;
        }
    }
    int nbMax = 50;
    int idxEnd = idxStart+nbMax;
    if (listPlayerCache.size() <= idxEnd) {
        idxEnd = listPlayerCache.size();
    }
    listPlayerCache = listPlayerCache.subList(idxStart, idxEnd);
    long tsListPlayerCache = System.currentTimeMillis() - ts;
%>
<html>
    <head>
        <title>Funbridge Server - Administration Player Cache</title>
        <script type="text/javascript">
            function clickSearchPlayerWithPseudo() {
                if (document.forms["formPlayerCache"].searchpseudo.value.replace(/^\s+/g,'').replace(/\s+$/g,'') == '') {
                    alert("Pseudo search is not valid !");
                } else {
                    document.forms["formPlayerCache"].operation.value = "searchPlayerPseudo";
                    document.forms["formPlayerCache"].submit();
                }
            }
            function clickSearchPlayerWithID() {
                if (document.forms["formPlayerCache"].searchplaid.value.replace(/^\s+/g,'').replace(/\s+$/g,'') == '') {
                    alert("Player ID is not valid !");
                } else {
                    document.forms["formPlayerCache"].operation.value = "searchPlayerID";
                    document.forms["formPlayerCache"].submit();
                }
            }
            function clickUpdatePlayerData(plaID) {
                if (confirm("Update data for player with ID="+plaID)) {
                    document.forms["formPlayerCache"].operation.value = "updatePlayerData";
                    document.forms["formPlayerCache"].playerid.value = plaID;
                    document.forms["formPlayerCache"].submit();
                }
            }
            function clickRemovePlayerFromCache(plaID) {
                if (confirm("Remove player with ID="+plaID)) {
                    document.forms["formPlayerCache"].operation.value = "removePlayer";
                    document.forms["formPlayerCache"].playerid.value = plaID;
                    document.forms["formPlayerCache"].submit();
                }
            }
            function clickListOffset(offset) {
                document.forms["formPlayerCache"].listoffset.value = offset;
                document.forms["formPlayerCache"].submit();
            }
            function clickReloadAllPlayerAllData() {
                if (confirm("Reload all player ALL data ?")) {
                    document.forms["formPlayerCache"].operation.value = "reloadAllPlayerAllData";
                    document.forms["formPlayerCache"].submit();
                }
            }
            function clickReloadAllPlayerSerieData() {
                if (confirm("Reload all player Serie data ?")) {
                    document.forms["formPlayerCache"].operation.value = "reloadAllPlayerSerieData";
                    document.forms["formPlayerCache"].submit();
                }
            }
            function clickComputeAverageHandicapSerie() {
                if (confirm("Compute average handicap on serie ?")) {
                    document.forms["formPlayerCache"].operation.value = "computeAverageHandicapSerie";
                    document.forms["formPlayerCache"].submit();
                }
            }
            function clickSpecialSelection() {
                if (confirm("Do special selection on cache ?")) {
                    document.forms["formPlayerCache"].operation.value = "specialSelection";
                    document.forms["formPlayerCache"].submit();
                }
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION PLAYER CACHE</h1>
        <a href="admin.jsp">Administration</a><br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= paramOperation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formPlayerCache" method="post" action="adminPlayerCache.jsp">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="playerid" value=""/>
            <input type="hidden" name="listoffset" value="<%=idxStart%>"/>
            Cache size (nb elements) = <%=playerCacheMgr.getCacheSize()%> - Bytes size = <%=playerCacheMgr.computeByteSizeMapPlayer()%><br/>
            <hr width="95%"/>
            <b>Special selection</b>
            <br/>
            <input type="button" value="Do special selection" onclick="clickSpecialSelection()">
            <hr width="95%"/>
            <b>Find player</b>
            <br/>
            Pseudo : <input type="text" name="searchpseudo">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Search Player with pseudo" onclick="clickSearchPlayerWithPseudo()"/><br/>
            Player ID : <input type="text" name="searchplaid">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Search Player With ID" onclick="clickSearchPlayerWithID()"/><br/>
            <hr width="95%"/>
            <b>Reload cache data</b>
            <br/>
            <input type="button" value="Compute average handicap on serie" onclick="clickComputeAverageHandicapSerie()"/><br/>
            <input type="button" value="Reload all player ALL data" onclick="clickReloadAllPlayerAllData()"/><br/>
            <input type="button" value="Reload all player Serie data" onclick="clickReloadAllPlayerSerieData()"/><br/>
            <hr width="95%"/>
            <b>List Player</b> - Time to process list : <%=tsListPlayerCache%>
            <br/>
            <%if(idxStart > 0) {%>
            <input type="button" value="Previous" onclick="clickListOffset(<%=idxStart - nbMax%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (listPlayerCache.size() >= nbMax) {%>
            <input type="button" value="Next" onclick="clickListOffset(<%=idxStart + nbMax%>)">
            <%}%>
            <br/>
            Push flag :
             - SUBSCRIPTION_PUSH_FB_NOTIFICATION = 1
             - SUBSCRIPTION_PUSH_FRIENDS = 2
             - SUBSCRIPTION_PUSH_TEAMS = 4
             - SUBSCRIPTION_PUSH_PRIVATE = 8
            <br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr>
                    <th>ID</th><th>Pseudo</th><th>CountryCode</th>
                    <th>Serie</th><th>Serie LastPeriodPlayed</th><th>Avatar (present - public)</th>
                    <th>Push flag</th>
                    <th>Operations</th>
                </tr>
                <%for (PlayerCache pc : listPlayerCache) {%>
                <tr>
                    <td><%=pc.ID %></td>
                    <td><%=pc.pseudo %></td>
                    <td><%=pc.countryCode %></td>
                    <td><%=pc.serie %></td>
                    <td><%=pc.serieLastPeriodPlayed %></td>
                    <td><%=pc.avatarPresent%> - <%=pc.avatarPublic%></td>
                    <td>
                        <input type="button" onclick="clickUpdatePlayerData(<%=pc.ID %>)" value="Update All Data"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                        <input type="button" onclick="clickRemovePlayerFromCache(<%=pc.ID %>)" value="Remove From Cache"/>
                    </td>
                </tr>
                <%} %>
            </table>
        </form>
    </body>
</html>
