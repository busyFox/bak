<%@page import="com.funbridge.server.common.Constantes"%>
<%@page import="com.funbridge.server.common.ContextManager"%>
<%@page import="com.funbridge.server.engine.ArgineProfile"%>
<%@page import="com.funbridge.server.tournament.duel.DuelMgr"%>
<%@page import="com.funbridge.server.tournament.duel.data.DuelTournament"%>
<%@ page import="com.funbridge.server.tournament.federation.cbo.TourCBOMgr" %>
<%@ page import="com.funbridge.server.tournament.federation.cbo.data.TourCBOTournament" %>
<%@ page import="com.funbridge.server.tournament.game.Game" %>
<%@ page import="com.funbridge.server.tournament.privatetournament.PrivateTournamentMgr" %>
<%@ page import="com.funbridge.server.tournament.privatetournament.data.PrivateTournament" %>
<%@ page import="com.funbridge.server.tournament.serie.TourSerieMgr" %>
<%@ page import="com.funbridge.server.tournament.serie.data.TourSerieGame" %>
<%@ page import="com.funbridge.server.tournament.serie.data.TourSerieTournament" %>
<%@ page import="com.funbridge.server.tournament.team.TourTeamMgr" %>
<%@ page import="com.funbridge.server.tournament.team.data.TeamTournament" %>
<%@ page import="com.funbridge.server.tournament.timezone.TimezoneMgr" %>
<%@ page import="com.funbridge.server.tournament.timezone.data.TimezoneTournament" %>
<%@ page import="com.gotogames.common.bridge.BridgeGame" %>
<%@ page import="com.gotogames.common.bridge.PBNConvertion" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>

<html>
<%
    long playerID = 0;
    String dealID = request.getParameter("dealID");
    int category = 0;
    playerID = Long.parseLong(request.getParameter("playerID"));
    category = Integer.parseInt(request.getParameter("category"));
    String strTournament = "";
    Game game = null;
    TourSerieGame gameSerie = null;

if (category == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
        String tourID = TimezoneMgr.extractTourIDFromDealID(dealID);
        int dealIndex = TimezoneMgr.extractDealIndexFromDealID(dealID);
        game = ContextManager.getTimezoneMgr().getGameOnTournamentAndDealForPlayer(tourID, dealIndex, playerID);
        TimezoneTournament tour = ContextManager.getTimezoneMgr().getTournament(tourID);
        if (tour != null) {
            strTournament = tour.toString();
        }
    }
    else if (category == Constantes.TOURNAMENT_CATEGORY_DUEL) {
        String tourID = DuelMgr.extractTourIDFromDealID(dealID);
        int dealIndex = DuelMgr.extractDealIndexFromDealID(dealID);
        game = ContextManager.getDuelMgr().getGameOnTournamentAndDealForPlayer(tourID, dealIndex, playerID);
        DuelTournament tour = ContextManager.getDuelMgr().getTournament(tourID);
        if (tour != null) {
            strTournament = tour.toString();
        }
    } else if (category == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
        String tourID = TourSerieMgr.extractTourIDFromDealID(dealID);
        int dealIndex = TourSerieMgr.extractDealIndexFromDealID(dealID);
        gameSerie = ContextManager.getTourSerieMgr().getGameOnTournamentAndDealForPlayer(tourID, dealIndex, playerID);
        TourSerieTournament tour = ContextManager.getTourSerieMgr().getTournament(tourID);
        if (tour != null) {
            strTournament = tour.toString();
        }
    } else if (category == Constantes.TOURNAMENT_CATEGORY_TEAM) {
        String tourID = TourTeamMgr.extractTourIDFromDealID(dealID);
        int dealIndex = TourTeamMgr.extractDealIndexFromDealID(dealID);
        game = ContextManager.getTourTeamMgr().getGameOnTournamentAndDealForPlayer(tourID, dealIndex, playerID);
        TeamTournament tour = ContextManager.getTourTeamMgr().getTournament(tourID);
        if (tour != null) {
            strTournament = tour.toString();
        }
    } else if (category == Constantes.TOURNAMENT_CATEGORY_TOUR_CBO) {
		String tourID = TourCBOMgr.extractTourIDFromDealID(dealID);
		int dealIndex = TourCBOMgr.extractDealIndexFromDealID(dealID);
		game = ContextManager.getTourCBOMgr().getGameOnTournamentAndDealForPlayer(tourID, dealIndex, playerID);
		TourCBOTournament tour = (TourCBOTournament)ContextManager.getTourCBOMgr().getTournament(tourID);
		if (tour != null) {
			strTournament = tour.toString();
		}
	}  else if (category == Constantes.TOURNAMENT_CATEGORY_PRIVATE) {
        String tourID = PrivateTournamentMgr.extractTourIDFromDealID(dealID);
        int dealIndex = PrivateTournamentMgr.extractDealIndexFromDealID(dealID);
        game = ContextManager.getPrivateTournamentMgr().getGameOnTournamentAndDealForPlayer(tourID, dealIndex, playerID);
        PrivateTournament tour = (PrivateTournament)ContextManager.getPrivateTournamentMgr().getTournament(tourID);
        if (tour != null) {
            strTournament = tour.toString();
        }
    }


%>
	<head>
		<title>Funbridge Server - View Game</title>
	</head>
	<body>
		<h1>ADMINISTRATION GAME VIEWER</h1>
		<a href="admin.jsp">Administration</a><br/><br/>
		<hr width="90%"/>
        <b>Tournament</b><br/>
        <%=strTournament%>
        <br/>
        <%if (game != null || gameSerie != null) {%>
			<hr width="90%"/>
			<b>Deal</b><br>
            <%=game!=null?game.getDeal().toString():gameSerie.getDeal().toString()%>
			<%
			String distrib = game!=null?game.getDeal().getCards():gameSerie.getDeal().cards;
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
			<hr width="90%"/>
			<b>Game Player Info</b><br/>
            <%=game!=null?game.toString():gameSerie.toString()%>
			<%
                int conventionProfile = game!=null?game.getConventionProfile():gameSerie.getConventionProfile();
                String conventionData = game!=null?game.getConventionData():gameSerie.getConventionData();
                String strConv = "";
                ArgineProfile ap = ContextManager.getArgineEngineMgr().getProfile(conventionProfile);
                if (ap != null) {
                    strConv += ap.name;
                    if (ap.isFree()) {
                        strConv += " - "+conventionData;
                    } else {
                        strConv += " - "+ap.value;
                    }
                }
			%>
			
			Conventions = <%=strConv %><br/>
			Bids :
			<%
			String bids = "";
			String[] tempBids = game!=null?game.getBids().split("-"):gameSerie.getBids().split("-");
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
			String[] tempCards = game!=null?game.getCards().split("-"):gameSerie.getCards().split("-");
			for (int i = 0; i < tempCards.length; i++) {
				if (i != 0 && i%4 == 0) {
					cards += "<br/>";
				}
				cards += tempCards[i]+" ";
			}
			String winner = "";
			String tempWinner = game!=null?game.getTricksWinner():gameSerie.getTricksWinner();
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
            metadata.put("date", Constantes.timestamp2StringDateHour(game!=null?game.getStartDate():gameSerie.getStartDate()));
            metadata.put("scoring", (game!=null?(game.getTournament().getResultType()==Constantes.TOURNAMENT_RESULT_PAIRE?"PAIRE":"IMP"):(gameSerie.getTournament().getResultType()==Constantes.TOURNAMENT_RESULT_PAIRE?"PAIRE":"IMP")));
            metadata.put("engineVersion", "" + (game!=null?game.getEngineVersion():gameSerie.getEngineVersion()));
            metadata.put("conventions", strConv);
            bg = BridgeGame.create(game!=null?game.getDeal().getString():gameSerie.getDeal().getString(),
                    game!=null?game.getListBid():gameSerie.getListBid(),
                    game!=null?game.getListCard():gameSerie.getListCard());
			String pbnData = PBNConvertion.gameToPBN(bg, metadata, "\r");
            %>
			<textarea rows="15" cols="80" readonly="readonly"><%=pbnData %></textarea>
		<%} %>
	</body>
</html>