<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.engine.ArgineProfile"%>
<%@page import="com.funbridge.server.tournament.serie.TourSerieMgr"%>
<%@page import="com.funbridge.server.tournament.serie.data.TourSerieDeal"%>
<%@page import="com.funbridge.server.tournament.serie.data.TourSerieGame"%>
<%@ page import="com.funbridge.server.tournament.serie.data.TourSerieTournament" %>
<%@ page import="com.gotogames.common.bridge.BridgeGame" %>
<%@ page import="com.gotogames.common.bridge.PBNConvertion" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<html>
<%
    String resultOperation = "";
    String paramGameID = request.getParameter("gameID");
    String paramPlayerID = request.getParameter("playerID");
    String paramTourID = request.getParameter("tourID");
    String paramDealIndex = request.getParameter("dealIndex");
    TourSerieGame game = null;
    TourSerieTournament tour = null;
    TourSerieDeal deal = null;
    TourSerieMgr serieMgr = ContextManager.getTourSerieMgr();

    if (paramGameID != null) {
        game = serieMgr.getGame(paramGameID);
        if (game == null) {
            resultOperation = "No game found for gameID="+paramGameID;
        }
    } else if (paramPlayerID != null && paramTourID != null && paramDealIndex != null) {
        game = serieMgr.getGameOnTournamentAndDealForPlayer(paramTourID, Integer.parseInt(paramDealIndex), Long.parseLong(paramPlayerID));
        if (game == null) {
            resultOperation = "No game found for tourID="+paramTourID+" and dealIndex="+paramDealIndex+" and playerID="+paramPlayerID;
        }
    } else {
        resultOperation = "Parameters not valid ... paramGameID="+paramGameID+" - paramPlayerID="+paramPlayerID+" - paramTourID="+paramTourID+" - paramDealIndex="+paramDealIndex;
    }

    if (game != null) {
        deal = (TourSerieDeal)game.getDeal();
        tour = game.getTournament();
    }
%>
<head>
    <title>Funbridge Server - View Game</title>
    <script type="text/javascript">
        function clickViewGame() {
            document.forms["formView"].submit();
        }
    </script>
</head>
<body>
<h1>ADMINISTRATION GAME VIEWER</h1>
<a href="admin.jsp">Administration</a><br/><br/>
<%if (resultOperation.length() > 0) {%>
<br/><b>Result = <%=resultOperation %></b>
<%} %>
<hr width="90%"/>
<form name="formView" action="adminSerieViewGame.jsp" method="post">
    Player ID = <input type="text" name="playerID" value="<%=game!=null?game.getPlayerID():""%>"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
    Tour ID = <input type="text" name="tourID" value="<%=game!=null?game.getTournament().getIDStr():""%>"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
    Deal Index  = <input type="text" name="dealIndex" value="<%=game!=null?game.getDealIndex():""%>"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
    <input type="button" value="View Game" onclick="clickViewGame()"/>
</form>
<%if (tour != null) {%>
<hr width="90%"/>
<b>Tournament Info</b><br>
ID = <%=tour.getIDStr() %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Serie = <%=tour.getSerie()%><br/>
Period = <%=tour.getPeriod()%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Date Start = <%=Constantes.timestamp2StringDateHour(tour.getTsDateStart()) %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Date End = <%=Constantes.timestamp2StringDateHour(tour.getTsDateEnd()) %><br/>
Finished = <%=tour.isFinished() %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Nb Deal = <%=tour.getNbDeals() %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Nb Player = <%=tour.getNbPlayers() %><br/>
<%} %>
<%if (deal != null) {%>
<hr width="90%"/>
<b>Deal Distribution</b><br/>
Index = <%=deal.index%><br/>
Dealer = <%=deal.getStrDealer() %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Vulnerability = <%=deal.getStrVulnerability() %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Parameter generation = <%=deal.paramGenerator %><br/>
Cards = <%=deal.cards %><br/>
<%
    String distrib = deal.cards;
    String handNClub = "", handNDiamond = "", handNHeart = "", handNSpade = "";
    String handSClub = "", handSDiamond = "", handSHeart = "", handSSpade = "";
    String handWClub = "", handWDiamond = "", handWHeart = "", handWSpade = "";
    String handEClub = "", handEDiamond = "", handEHeart = "", handESpade = "";
    for (int i = 0; i < 52; i++) {
        int index = i%13;
        String temp = "";
        switch (index) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                temp = ""+(index+2);break;
            case 9:
                temp = "J";break;
            case 10:
                temp = "Q";break;
            case 11:
                temp = "K";break;
            case 12:
                temp = "A";break;
        }
        // club
        if ((i / 13) == 0) {
            if (distrib.charAt(i) == 'N') { handNClub+=temp+" ";}
            if (distrib.charAt(i) == 'W') { handWClub+=temp+" ";}
            if (distrib.charAt(i) == 'E') { handEClub+=temp+" ";}
            if (distrib.charAt(i) == 'S') { handSClub+=temp+" ";}
        }
        // diamond
        if ((i / 13) == 1) {
            if (distrib.charAt(i) == 'N') { handNDiamond+=temp+" ";}
            if (distrib.charAt(i) == 'W') { handWDiamond+=temp+" ";}
            if (distrib.charAt(i) == 'E') { handEDiamond+=temp+" ";}
            if (distrib.charAt(i) == 'S') { handSDiamond+=temp+" ";}
        }
        // heart
        if ((i / 13) == 2) {
            if (distrib.charAt(i) == 'N') { handNHeart+=temp+" ";}
            if (distrib.charAt(i) == 'W') { handWHeart+=temp+" ";}
            if (distrib.charAt(i) == 'E') { handEHeart+=temp+" ";}
            if (distrib.charAt(i) == 'S') { handSHeart+=temp+" ";}
        }
        // spade
        if ((i / 13) == 3) {
            if (distrib.charAt(i) == 'N') { handNSpade+=temp+" ";}
            if (distrib.charAt(i) == 'W') { handWSpade+=temp+" ";}
            if (distrib.charAt(i) == 'E') { handESpade+=temp+" ";}
            if (distrib.charAt(i) == 'S') { handSSpade+=temp+" ";}
        }
    }
%>
<table border="0" style="background-color:white" cellpadding="2" cellspacing="0">
    <tr>
        <td></td>
        <td>C : <%=handNClub %><br/>D : <%=handNDiamond %><br/>H : <%=handNHeart %><br/>S : <%=handNSpade %></td>
        <td></td>
    </tr>
    <tr>
        <td>C : <%=handWClub %><br/>D : <%=handWDiamond %><br/>H : <%=handWHeart %><br/>S : <%=handWSpade %></td>
        <td></td>
        <td>C : <%=handEClub %><br/>D : <%=handEDiamond %><br/>H : <%=handEHeart %><br/>S : <%=handESpade %></td>
    </tr>
    <tr>
        <td></td>
        <td>C : <%=handSClub %><br/>D : <%=handSDiamond %><br/>H : <%=handSHeart %><br/>S : <%=handSSpade %></td>
        <td></td>
    </tr>
</table>
<%} %>
<%if (game != null) {%>
<hr width="90%"/>
<b>Game Player Info</b><br/>
ID = <%=game.getIDStr() %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Finished = <%=game.isFinished() %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Date start = <%=Constantes.timestamp2StringDateHour(game.getStartDate()) %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Date last = <%=Constantes.timestamp2StringDateHour(game.getLastDate()) %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Device ID = <%=game.getDeviceID() %><br/>
Contract = <%=game.getContractWS() %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Declarer = <%=Character.toString(game.getDeclarer()) %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Nb Tricks = <%=game.getTricks() %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
Score = <%=game.getScore() %><br/>
Rank = <%=game.getRank()%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Result = <%=game.getResult()%><br/>
<%
    String strConv = "Unknown";
    int conventionProfile = game.getConventionProfile();
    ArgineProfile ap = ContextManager.getArgineEngineMgr().getProfile(conventionProfile);
    if (ap != null) {
        strConv += ap.name;
        if (ap.isFree()) {
            strConv += " - "+game.getConventionData();
        } else {
            strConv += " - "+ap.value;
        }
    }
%>
Conventions = <%=strConv %><br/>
Bids :
<%
    String bids = "";
    String[] tempBids = game.getBids().split("-");
    for (int i = 0; i < tempBids.length; i++) {
        if (i != 0 && i%4 == 0) {
            bids += "<br/>";
        }
        bids += tempBids[i]+" ";
    }
%>
<%=bids %>
<br/>
<br/>
Cards :<br/>
<%
    String cards = "";
    String[] tempCards = game.getCards().split("-");
    for (int i = 0; i < tempCards.length; i++) {
        if (i != 0 && i%4 == 0) {
            cards += "<br/>";
        }
        cards += tempCards[i]+" ";
    }
    String winner = "";
    String tempWinner = game.getTricksWinner();
    for (int i = 0; i < tempWinner.length(); i++) {
        winner += tempWinner.charAt(i)+"<br/>";
    }
%>
<table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
    <tr>
        <td><%=cards %></td><td><b><%=winner %></b></td>
    </tr>
</table>
<br/>
<br/>
<b>PBN</b><br/>
<%
    BridgeGame bg = null;
    Map<String, String> metadata = new HashMap<String, String>();
    if (game!=null) {
        bg = BridgeGame.create(deal.getString(),
                game.getListBid(),
                game.getListCard());
        metadata.put("date", Constantes.timestamp2StringDateHour(game.getStartDate()));
        metadata.put("scoring", (game.getTournament().getResultType()==Constantes.TOURNAMENT_RESULT_PAIRE?"PAIRE":"IMP"));
        metadata.put("engineVersion", "" + game.getEngineVersion());
        metadata.put("conventions", game.getConventionData());
    }
    String pbnData = PBNConvertion.gameToPBN(bg, metadata, "\r"); %>
<textarea rows="15" cols="80" readonly="readonly"><%=pbnData %></textarea>
<%} %>
</body>
</html>