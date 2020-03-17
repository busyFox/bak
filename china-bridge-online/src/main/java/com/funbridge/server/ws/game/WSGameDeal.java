package com.funbridge.server.ws.game;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.engine.ArgineProfile;
import com.funbridge.server.tournament.data.TournamentDeal;
import com.funbridge.server.tournament.data.TournamentGame2;
import com.funbridge.server.tournament.data.TournamentSettings;
import com.funbridge.server.tournament.game.Deal;
import com.funbridge.server.tournament.game.Game;
import com.gotogames.common.crypt.AESCrypto;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class WSGameDeal {
    public long dealID;
    public String dealIDstr;
    public String distribution;
    public String dealer;
    public String vulnerability;
    public String bidList;
    public String playList;
    public String declarer;
    public String contract;
    public String tricksWinner;
    public int score;
    public int nbTricks;
    public double result;
    public int index;
    public int step = 0;
    public String currentPlayer = "";
    public int conventionProfil = 0;
    public String conventionValue = null;
    public int cardsConventionProfil = 0;
    public String cardsConventionValue = null;
    public int engineVersion = 0;

    public String toString() {
        return "ID=" + dealID + "-dealer=" + dealer + "-vul=" + vulnerability + "-distrib=" + distribution + "-step=" + step + "-bids=" + bidList + "-contract=" + contract + "-declarer=" + declarer + "-cards=" + playList + "-winner=" + tricksWinner;
    }

    public void setDealData(TournamentDeal deal) {
        if (deal != null) {
            this.dealID = deal.getID();
            this.dealIDstr = "" + deal.getID();
            this.dealer = deal.getDistribution().getDealerStr();
            this.vulnerability = deal.getDistribution().getVulnerabilityStr();
            this.index = deal.getIndex();
            this.distribution = AESCrypto.crypt(deal.getDistribution().getCards(), Constantes.CRYPT_KEY);
        }
    }

    public void setDealData(Deal deal, String tourID) {
        if (deal != null) {
            this.dealIDstr = deal.getDealID(tourID);
            this.dealer = deal.getStrDealer();
            this.vulnerability = deal.getStrVulnerability();
            this.index = deal.getIndex();
            this.distribution = AESCrypto.crypt(deal.getCards(), Constantes.CRYPT_KEY);
        }
    }

//    public void setDealData(TourSerieDeal deal, String tourID) {
//        if (deal != null) {
//            this.dealIDstr = deal.getDealID(tourID);
//            this.dealer = deal.getStrDealer();
//            this.vulnerability = deal.getStrVulnerability();
//            this.index = deal.index;
//            this.distribution = AESCrypto.crypt(deal.cards, Constantes.CRYPT_KEY);
//        }
//    }

//	public void setGameData(TourSerieGame game) {
//        if (game != null) {
//            this.declarer = Character.toString(game.getDeclarer());
//            this.contract = game.getContractWS();
//            this.bidList = game.getBids();
//            this.playList = game.getCards();
//            this.tricksWinner = game.getTricksWinner();
//            this.step = game.getStep();
//            this.currentPlayer = Character.toString(game.getCurrentPlayer());
//            this.conventionProfil = game.getConventionProfile();
//            ArgineProfile ap = ContextManager.getArgineEngineMgr().getProfile(conventionProfil);
//            if (ap != null) {
//                if (ap.isFree()) {
//                    this.conventionValue = game.getConventionData();
//                } else {
//                    this.conventionValue = ap.value;
//                }
//            }
//            this.cardsConventionProfil = game.getCardsConventionProfile();
//            ArgineProfile apCards = ContextManager.getArgineEngineMgr().getProfileCards(cardsConventionProfil);
//            if (apCards != null) {
//                if (apCards.isFree()) {
//                    this.cardsConventionValue = game.getCardsConventionData();
//                } else {
//                    this.cardsConventionValue = apCards.value;
//                }
//            }
////            this.conventionValue = game.getConventionData();
//        }
//    }

    public void setGameData(Game game) {
        if (game != null) {
            this.declarer = Character.toString(game.getDeclarer());
            this.contract = game.getContractWS();
            this.bidList = game.getBids();
            this.playList = game.getCards();
            this.tricksWinner = game.getTricksWinner();
            this.score = game.getScore();
            this.nbTricks = game.getTricks();
            this.result = game.getResult();
            this.step = game.getStep();
            this.currentPlayer = Character.toString(game.getCurrentPlayer());
            this.conventionProfil = game.getConventionProfile();
            this.engineVersion = game.getEngineVersion();
            ArgineProfile ap = ContextManager.getArgineEngineMgr().getProfile(conventionProfil);
            if (ap != null) {
                if (ap.isFree()) {
                    this.conventionValue = game.getConventionData();
                } else {
                    this.conventionValue = ap.value;
                }
            }
            this.cardsConventionProfil = game.getCardsConventionProfile();
            ArgineProfile apCards = ContextManager.getArgineEngineMgr().getProfileCards(cardsConventionProfil);
            if (apCards != null) {
                if (apCards.isFree()) {
                    this.cardsConventionValue = game.getCardsConventionData();
                } else {
                    this.cardsConventionValue = apCards.value;
                }
            }
//            this.conventionValue = game.getConventionData();
        }
    }

    public void setGameData(TournamentGame2 game) {
        if (game != null) {
            this.declarer = Character.toString(game.getDeclarer());
            this.contract = game.getContractWS();
            this.bidList = game.getBidsWS();
            this.playList = game.getCardsWS();
            this.tricksWinner = game.getTricksWinner();
            this.step = game.getStep();
            this.currentPlayer = Character.toString(game.getCurrentPlayer());
            this.conventionProfil = game.getConventionProfile();
            this.conventionValue = game.getConventionData();
            TournamentSettings tourSettings = ContextManager.getTournamentMgr().getTournamentSettings(game.getDeal().getTournament().getSettings());
            if (tourSettings != null) {
                this.cardsConventionProfil = tourSettings.cardsConvention;
                ArgineProfile apCards = ContextManager.getArgineEngineMgr().getProfileCards(cardsConventionProfil);
                if (apCards != null) {
                    if (apCards.isFree()) {
                        this.cardsConventionValue = tourSettings.cardsConventionValue;
                    } else {
                        this.cardsConventionValue = apCards.value;
                    }
                }
            }
        }
    }
}
