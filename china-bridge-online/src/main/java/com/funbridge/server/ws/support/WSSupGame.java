package com.funbridge.server.ws.support;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.engine.ArgineProfile;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.serie.data.TourSerieGame;
import com.gotogames.common.bridge.BridgeGame;
import com.gotogames.common.bridge.PBNConvertion;

import java.util.HashMap;
import java.util.Map;

public class WSSupGame {
	public long gameID;
    public String gameIDstr;
	public boolean finished;
	public String dateStart;
	public String dateLast;
	public int rank;
	public double result;
	public int score;
	public String contract;
	public long deviceID;
	public String declarer;
	public int nbTricks;
	public String bids;
	public String cards;
	public String conventions;
    public String conventionsCards;
	public String tricksWinner;
	public String pbnData;
    public int resultType;

	public WSSupGame() {
	}
	
	public WSSupGame(TourSerieGame tg) {
        this.gameIDstr = tg.getIDStr();
        this.finished = tg.isFinished();
        this.dateStart = Constantes.timestamp2StringDateHour(tg.getStartDate());
        this.dateLast = Constantes.timestamp2StringDateHour(tg.getLastDate());
        this.rank = tg.getRank();
        this.result = tg.getResult();
        this.score = tg.getScore();
        this.contract = tg.getContractWS();
        this.deviceID = tg.getDeviceID();
        this.declarer = Character.toString(tg.getDeclarer());
        this.nbTricks = tg.getTricks();
        this.bids = tg.getBids();
        this.cards = tg.getCards();
        this.resultType = Constantes.TOURNAMENT_RESULT_PAIRE;
        String strConvBids = "Unknown";
        ArgineProfile ap = ContextManager.getArgineEngineMgr().getProfile(tg.getConventionProfile());
        if (ap != null) {
            strConvBids = ap.name;
            if (ap.isFree()) {
                strConvBids += " - "+tg.getConventionData();
            } else {
                strConvBids += " - "+ap.value;
            }
        }
        this.conventions = strConvBids;
        String strConvCards = "Unknown";
        ArgineProfile apCards = ContextManager.getArgineEngineMgr().getProfileCards(tg.getCardsConventionProfile());
        if (apCards != null) {
            strConvCards = apCards.name;
            if (apCards.isFree()) {
                strConvCards += " - "+tg.getCardsConventionData();
            } else {
                strConvCards += " - "+apCards.value;
            }
        }
        this.conventionsCards = strConvCards;
        this.tricksWinner = tg.getTricksWinner();
        BridgeGame bg = null;
        if (tg != null) {
            bg = BridgeGame.create(tg.getDeal().getString(),
                    tg.getListBid(),
                    tg.getListCard());
        }
        Map<String, String> metadata = new HashMap<>();
        metadata.put("date", this.dateStart);
        metadata.put("scoring", "MP");
        metadata.put("engineVersion", ""+tg.getEngineVersion());
        metadata.put("Player_profile", strConvBids);
        metadata.put("conventionsCards", strConvCards);
        pbnData = PBNConvertion.gameToPBN(bg, metadata, "\r");
    }

    public WSSupGame(Game tg) {
        this.gameIDstr = tg.getIDStr();
        this.finished = tg.isFinished();
        this.dateStart = Constantes.timestamp2StringDateHour(tg.getStartDate());
        this.dateLast = Constantes.timestamp2StringDateHour(tg.getLastDate());
        this.rank = tg.getRank();
        this.result = tg.getResult();
        this.score = tg.getScore();
        this.contract = tg.getContractWS();
        this.deviceID = tg.getDeviceID();
        this.declarer = Character.toString(tg.getDeclarer());
        this.nbTricks = tg.getTricks();
        this.bids = tg.getBids();
        this.cards = tg.getCards();
        this.resultType = tg.getTournament().getResultType();
        String strConvBids = "Unknown";
        ArgineProfile ap = ContextManager.getArgineEngineMgr().getProfile(tg.getConventionProfile());
        if (ap != null) {
            strConvBids = ap.name;
            if (ap.isFree()) {
                strConvBids += " - "+tg.getConventionData();
            } else {
                strConvBids += " - "+ap.value;
            }
        }
        this.conventions = strConvBids;
        String strConvCards = "Unknown";
        ArgineProfile apCards = ContextManager.getArgineEngineMgr().getProfileCards(tg.getCardsConventionProfile());
        if (apCards != null) {
            strConvCards = apCards.name;
            if (apCards.isFree()) {
                strConvCards += " - "+tg.getCardsConventionData();
            } else {
                strConvCards += " - "+apCards.value;
            }
        }
        this.conventionsCards = strConvCards;
        this.tricksWinner = tg.getTricksWinner();
        BridgeGame bg = null;
        if (tg != null) {
            bg = BridgeGame.create(tg.getDeal().getString(),
                    tg.getListBid(),
                    tg.getListCard());
        }
        Map<String, String> metadata = new HashMap<>();
        metadata.put("date", this.dateStart);
        metadata.put("scoring", "MP");
        metadata.put("engineVersion", ""+tg.getEngineVersion());
        metadata.put("Player_profile", strConvBids);
        metadata.put("conventionsCards", strConvCards);
        pbnData = PBNConvertion.gameToPBN(bg, metadata, "\r");
    }
}
