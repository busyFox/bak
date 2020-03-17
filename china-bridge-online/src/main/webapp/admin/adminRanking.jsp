<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.message.MessageNotifMgr" %>
<%@ page import="com.funbridge.server.player.cache.PlayerCache" %>
<%@ page import="com.funbridge.server.player.cache.PlayerCacheMgr" %>
<%@ page import="com.funbridge.server.tournament.duel.DuelMgr" %>
<%@ page import="com.funbridge.server.tournament.federation.TourFederationStatPeriodMgr" %>
<%@ page import="com.funbridge.server.tournament.federation.cbo.TourCBOMgr" %>
<%@ page import="com.funbridge.server.tournament.serie.TourSerieMgr" %>
<%@ page import="com.funbridge.server.ws.result.ResultServiceRest" %>
<%@ page import="com.funbridge.server.ws.result.WSMainRankingPlayer" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    int offset = 0;
    int nbMax = 3;
    String operation = request.getParameter("operation");
    String resultOperation = "";
    String paramType = request.getParameter("type");
    String paramPeriod = request.getParameter("period");
    String paramCountry = request.getParameter("country");
    String paramPlayerID = request.getParameter("playerID");
    String paramOffset = request.getParameter("offset");
    if (paramOffset != null && paramOffset.length() > 0) {
        offset = Integer.parseInt(paramOffset);
    } else {
        paramOffset = "";
    }
    PlayerCacheMgr playerCacheMgr = ContextManager.getPlayerCacheMgr();
    DuelMgr duelMgr = ContextManager.getDuelMgr();
    TourSerieMgr serieMgr = ContextManager.getTourSerieMgr();
    TourCBOMgr tourCBOMgr = ContextManager.getTourCBOMgr();
    TourFederationStatPeriodMgr tourFederationStatPeriodMgr = ContextManager.getTourFederationStatPeriodMgr();
    MessageNotifMgr notifMgr = ContextManager.getMessageNotifMgr();

    ResultServiceRest.GetMainRankingResponse rankingResponse = null;
    if (paramType == null) {
        paramType = "";
    }
    if (paramPeriod == null) {
        paramPeriod = "";
    }
    if (paramCountry == null) {
        paramCountry = "";
    }
    if (paramPlayerID == null || paramPlayerID.length() == 0) {
        paramPlayerID = ""+Constantes.PLAYER_ARGINE_ID;
    }

    PlayerCache playerCache = playerCacheMgr.getPlayerCache(Long.parseLong(paramPlayerID));

    if (paramType.equals("DUEL")) {
        String period = "";
        if (paramPeriod.equals("CURRENT")) {
            period = duelMgr.getStatCurrentPeriodID();
        } else if (paramPeriod.equals("PREVIOUS")) {
            period = duelMgr.getStatPreviousPeriodID();
        }
        rankingResponse = duelMgr.getRanking(playerCache, period, null, paramCountry, offset, nbMax);
    } else if (paramType.equals("DUEL_ARGINE")) {
        String period = "";
        if (paramPeriod.equals("CURRENT")) {
            period = duelMgr.getStatCurrentPeriodID();
        } else if (paramPeriod.equals("PREVIOUS")) {
            period = duelMgr.getStatPreviousPeriodID();
        }
        rankingResponse = duelMgr.getRankingArgine(playerCache, period, null, paramCountry, offset, nbMax);
    } else if (paramType.equals("SERIE")) {
        String period = "";
        if (paramPeriod.equals("PREVIOUS")) {
            period = serieMgr.getPeriodIDPrevious();
        } else {
            period = serieMgr.getCurrentPeriod().getPeriodID();
        }
        rankingResponse = serieMgr.getRanking(playerCache, period, null, paramCountry, offset, nbMax);
    } else if (paramType.equals("POINTS_FB")) {
        String period = "";
        if (paramPeriod.equals("PREVIOUS")) {
            period = serieMgr.getPeriodIDPrevious();
        } else {
            period = serieMgr.getCurrentPeriod().getPeriodID();
        }
        rankingResponse = serieMgr.getRanking(playerCache, period, null, paramCountry, offset, nbMax);
    }

    List<WSMainRankingPlayer> listRanking = null;
    if (rankingResponse != null) {
        listRanking = rankingResponse.ranking;
    }

    if ("notifRankingDuel".equals(operation)) {
        duelMgr.processNotifRanking();
    } else if ("notifRankingDuelArgine".equals(operation)) {
        duelMgr.processNotifRankingArgine();
    } else if ("notifRankingAllPodiums".equals(operation)) {
        notifMgr.processNotifRanking();
    }
%>
<html>
<head>
    <title>Funbridge Server - Administration</title>
        <script type="text/javascript">
            function clickRunOperation(operation) {
                if (confirm("Run operation "+operation+" ?")) {
                    document.forms["formRanking"].operation.value = operation;
                    document.forms["formRanking"].submit();
                }
            }
            function clickOffset(offset) {
                document.forms["formRanking"].offset.value=offset;
                document.forms["formRanking"].submit();
            }
            function clickOffset(offset) {
                document.forms["formRanking"].offset.value=offset;
                document.forms["formRanking"].submit();
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION RANKING</h1>
            [<a href="admin.jsp">Administration</a>]
            <br/><br/>
            <%if (resultOperation.length() > 0) {%>
            <b>RESULTAT OPERATION : <%= operation%></b>
            <br/>Result = <%=resultOperation %>
            <hr width="90%"/>
            <%} %>
            <form name="formRanking" method="post" action="adminRanking.jsp">
                <input type="hidden" name="operation" value="<%=operation%>">
                <b>Ranking type :</b>
                DUEL <input type="radio" name="type" value="DUEL" <%=paramType.equals("DUEL")?"checked=\"checked\"":""%>>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                DUEL_ARGINE <input type="radio" name="type" value="DUEL_ARGINE" <%=paramType.equals("DUEL_ARGINE")?"checked=\"checked\"":""%>>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                SERIE <input type="radio" name="type" value="SERIE" <%=paramType.equals("SERIE")?"checked=\"checked\"":""%>>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                POINTS_FB <input type="radio" name="type" value="POINTS_FB" <%=paramType.equals("POINTS_FB")?"checked=\"checked\"":""%>>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                PERFORMANCE <input type="radio" name="type" value="PERFORMANCE" <%=paramType.equals("PERFORMANCE")?"checked=\"checked\"":""%>>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                COUNTRY <input type="radio" name="type" value="COUNTRY" <%=paramType.equals("COUNTRY")?"checked=\"checked\"":""%>><br/>
                <b>Period :</b>
                CURRENT <input type="radio" name="period" value="CURRENT" <%=paramPeriod.equals("CURRENT")?"checked=\"checked\"":""%>>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                PREVIOUS <input type="radio" name="period" value="PREVIOUS" <%=paramPeriod.equals("PREVIOUS")?"checked=\"checked\"":""%>>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                TOTAL <input type="radio" name="period" value="TOTAL" <%=(paramPeriod == null || paramPeriod.length() == 0)?"checked=\"checked\"":""%>><br/>
                <br/>
                Player : <input type="text" name="playerID" size="10" value="<%=paramPlayerID%>"><br/>
                Country : <input type="text" name="country" size="10" value="<%=paramCountry%>"><br/>
                Offset : <input type="text" name="offset" size="10" value="<%=paramOffset%>"><br/>
                <input type="button" value="Get Ranking" onclick="clickRunOperation('getRanking')">
                <br/>
                <br/>
                <%if (paramType.equals("DUEL") || paramType.equals("DUEL_ARGINE")) {%>
                Current period = <%=duelMgr.getStatCurrentPeriodID()%> - previous period = <%=duelMgr.getStatPreviousPeriodID()%><br/>
                <%} else if (paramType.equals("SERIE")) {%>
                Current period = <%=serieMgr.getCurrentPeriod().getPeriodID()%> - previous period = <%=serieMgr.getPeriodIDPrevious()%><br/>
                <%} else if (paramType.equals("POINTS_FB")) {%>
                Current period = <%=tourFederationStatPeriodMgr.getStatCurrentPeriodID()%> - previous period = <%=tourFederationStatPeriodMgr.getStatPreviousPeriodID()%><br/>
                <%}%>
                <% if (rankingResponse != null) {%>
                Player cache = <%=playerCache%><br/>
                Ranking Player = <%="ID:"+rankingResponse.rankingPlayer.playerID+" pseudo:"+rankingResponse.rankingPlayer.playerPseudo+" country:"+rankingResponse.rankingPlayer.countryCode+" value:"+rankingResponse.rankingPlayer.value+" rank:"+rankingResponse.rankingPlayer.rank%><br/>
                Response offset = <%=rankingResponse.offset%><br/>
                Total Size = <%=rankingResponse.totalSize%><br/>
                Nb ranked players = <%=rankingResponse.nbRankedPlayers%><br/>
                <br/>
                <%if(offset > 0) {%>
                <input type="button" value="Previous" onclick="clickOffset(<%=offset - nbMax%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                <%}%>
                <input type="button" value="Next" onclick="clickOffset(<%=offset + nbMax%>)">
                <%}%>
                <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                    <tr><th>Rank</th><th>Player</th><th>Country</th><th>Value</th></tr>
                    <%if (rankingResponse != null) {
                        for (WSMainRankingPlayer e : rankingResponse.ranking) {
                    %>
                    <tr>
                        <td><%=e.rank%></td>
                        <td><%=e.playerID%> - <%=e.playerPseudo%></td>
                        <td><%=e.countryCode%></td>
                        <td><%=e.getStringValue()%></td>
                    </tr>
                    <%}}%>
                </table>

                <hr width="90%"/><br/>
                <b>Date next job notif ranking POINTS FB</b> = <%=Constantes.timestamp2StringDateHour(tourCBOMgr.getDateNextJobNotifRanking())%><br/>
                <b>Date next job notif ranking DUEL</b> = <%=Constantes.timestamp2StringDateHour(duelMgr.getDateNextJobDuelNotifRanking())%><br/>
                <b>Date next job notif ranking DUEL ARGINE </b> = <%=Constantes.timestamp2StringDateHour(duelMgr.getDateNextJobDuelArgineNotifRanking())%><br/>
                <b>Date next job notif ranking ALL PODIUMS</b> = <%=Constantes.timestamp2StringDateHour(notifMgr.getDateNextJobNotifRankingAllPodiums())%><br/>
                <br/>
                <input type="button" value="Notif Ranking DUEL" onclick="clickRunOperation('notifRankingDuel')"><br/>
                <input type="button" value="Notif Ranking DUEL ARGINE" onclick="clickRunOperation('notifRankingDuelArgine')"><br/>
                <input type="button" value="Notif Ranking ALL PODIUMS" onclick="clickRunOperation('notifRankingAllPodiums')"><br/>
            </form>
    </body>
</html>
