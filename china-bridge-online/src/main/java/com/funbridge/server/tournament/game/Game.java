package com.funbridge.server.tournament.game;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.ws.result.WSResultDeal;
import com.gotogames.common.bridge.BridgeBid;
import com.gotogames.common.bridge.BridgeCard;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.bridge.GameBridgeRule;
import com.gotogames.common.tools.StringTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.annotation.Transient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pserent on 20/02/2015.
 */
public abstract class Game {
    protected long playerID;

    protected int dealIndex;
    protected boolean finished = false;
    protected int rank;
    protected double result;
    protected String bids = "";
    protected String cards = "";
    protected String tricksWinner = "";
    protected int contractType;
    protected String contract = "";
    protected char declarer = BridgeConstantes.POSITION_NOT_VALID;
    protected int tricks;
    protected int score;
    protected String convention;
    protected String cardsConvention;
    protected long startDate = 0;
    protected long lastDate = 0;
    protected long deviceID;
    protected String analyzeBid = null;
    protected int engineVersion = 0;

    @Transient
    protected BridgeBid bidContract = null;

    @Transient
    protected List<BridgeBid> listBid = new ArrayList<>();

    @Transient
    protected List<BridgeCard> listCard = new ArrayList<>();

    @Transient
    protected boolean isEndBid;

    @Transient
    protected boolean isReplay = false;

    @Transient
    protected int conventionProfile = -1;
    @Transient
    protected int cardsConventionProfile = -1;

    @Transient
    protected String conventionData = "";
    @Transient
    protected String cardsConventionData = "";

    @Transient
    protected boolean initDone = false;

    @Transient
    protected boolean initFailed = false;

    @Transient
    protected String replayGameID = null;

    @Transient
    protected boolean spreadResultArgine = false;
    @Transient
    protected int spreadRefuseStep = -1; // use to store the stpe when the spread has been refuse by the player

    protected Map<String, String> mapBidInfoSouth = new HashMap<>();

    protected boolean analyzeSouthBidDone = false;

    protected String tricksWinnerHistoric = null;
    protected boolean isEngineFailed = false;

    public abstract Tournament getTournament();

    public abstract String getIDStr();

    public void setReplayGameID(String replayGameID) {
        this.replayGameID = replayGameID;
    }

    public long getPlayerID() {
        return playerID;
    }

    public int getDealIndex() {
        return dealIndex;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
        if (finished && !isEngineFailed) {
            tricksWinnerHistoric = null;
        }
    }

    public char getDeclarer() {
        return declarer;
    }

    public void setDeclarer(char declarer) {
        this.declarer = declarer;
    }

    public boolean isReplay() {
        return isReplay;
    }
    public void setReplay(boolean value) {
        isReplay = value;
    }

    public boolean isEndBid() {
        return isEndBid;
    }

    public void setEndBid(boolean isEndBid) {
        this.isEndBid = isEndBid;
    }

    public List<BridgeBid> getListBid() {
        return listBid;
    }

    public List<BridgeCard> getListCard() {
        return listCard;
    }

    public BridgeBid getBidContract() {
        if (bidContract != null) {
            boolean resetContract = false;
            if (contract != null) {
                if (bidContract.isPass() && contractType != Constantes.CONTRACT_TYPE_PASS) {
                    resetContract = true;
                }
                if (!bidContract.getString().equals(contract)) {
                    resetContract = true;
                }
            } else {
                resetContract = true;
            }
            if (resetContract) {
                bidContract = null;
            }
        }
        if (bidContract == null) {
            if (isEndBid) {
                if (contractType == Constantes.CONTRACT_TYPE_PASS) {
                    bidContract = BridgeBid.createBid(BridgeConstantes.BID_PASS, declarer);
                } else {
                    if (contract != null && contract.length() > 0 && declarer != BridgeConstantes.POSITION_NOT_VALID) {
                        bidContract = BridgeBid.createBid(contract, declarer);
                    }
                }
            }
        }
        return bidContract;
    }

    public int getContractType() {
        return contractType;
    }

    public void setContractType(int contractType) {
        this.contractType = contractType;
    }

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public String getBids() {
        return bids;
    }

    public void setBids(String bids) {
        this.bids = bids;
    }

    public String getCards() {
        return cards;
    }

    public long getLastDate() {
        return lastDate;
    }

    public void setLastDate(long lastDate) {
        this.lastDate = lastDate;
    }

    /**
     * Return number of winned tricks
     * @return
     */
    public int getTricks() {
        return tricks;
    }

    public void setTricks(int tricks) {
        this.tricks = tricks;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getTricksWinner() {
        return tricksWinner;
    }

    public void setTricksWinner(String tricksWinner) {
        this.tricksWinner = tricksWinner;
    }

    public String getConvention() {
        return convention;
    }

    public void setConvention(String convention) {
        this.convention = convention;
    }

    public String getCardsConvention() {
        return cardsConvention;
    }

    public void setCardsConvention(String cardsConvention) {
        this.cardsConvention = cardsConvention;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(long deviceID) {
        this.deviceID = deviceID;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }

    public boolean isSpreadResultArgine() {
        return spreadResultArgine;
    }

    public void setSpreadResultArgine(boolean spreadResultArgine) {
        this.spreadResultArgine = spreadResultArgine;
    }

    public String toString() {
        return "Game - id="+getIDStr()+" - plaID="+playerID+" - tournament={"+getTournament()+"} - dealIndex="+dealIndex+" - finished="+finished+" - currentPlayer="+getCurrentPlayer()+" - contract="+contract+" - contractType="+contractType+" - contractWS="+getContractWS()+" - declarer="+declarer+" - bids="+bids+" - cards="+cards+" - score="+score+" - tricks="+tricks+" - result="+result+" - rank="+rank+" - deviceID="+deviceID+" - convention="+getConvention()+" - cardsConvention="+getCardsConvention();
    }

    /**
     * Parse field convention and extract the profile and convention data
     */
    private void parseConvention() {
        // BIDS CONVENTION
        if (this.convention != null) {
            String[] temp = this.convention.split(Constantes.GAME_CONVENTION_SEPARATOR);
            if (temp.length > 0) {
                conventionProfile = Integer.parseInt(temp[0]);
                if (temp.length > 1) {
                    conventionData = temp[1];
                }
            }
        }
        // CARDS CONVENTION
        if (this.cardsConvention != null) {
            String[] temp = this.cardsConvention.split(Constantes.GAME_CONVENTION_SEPARATOR);
            if (temp.length > 0) {
                cardsConventionProfile = Integer.parseInt(temp[0]);
                if (temp.length > 1) {
                    cardsConventionData = temp[1];
                }
            } else {
                cardsConventionProfile = ContextManager.getArgineEngineMgr().getDefaultProfileCards();
            }
        }
    }

    public int getConventionProfile() {
        if (conventionProfile == -1) {
            parseConvention();
        }
        return conventionProfile;
    }

    public String getConventionData() {
        if (conventionData == null || conventionData.length() == 0) {
            parseConvention();
        }
        return conventionData;
    }

    public int getCardsConventionProfile() {
        if (cardsConventionProfile == -1) {
            parseConvention();
        }
        return cardsConventionProfile;
    }

    public String getCardsConventionData() {
        if (cardsConventionData == null || cardsConventionData.length() == 0) {
            parseConvention();
        }
        return cardsConventionData;
    }

    public void initData() {
        initDone = true;
        try {
            parseConvention();
            // load list of bid played
            listBid.clear();
            if (bids != null && bids.length() > 0) {
                String[] tempBids = bids.split(Constantes.GAME_BIDCARD_SEPARATOR);
                for (String e : tempBids) {
                    if (e.length() == 3 || e.length() == 4) {
                        BridgeBid bid = BridgeBid.createBid(e.substring(0, 2), e.charAt(2));
                        if (e.length() == 4 && e.charAt(3) == 'A') {
                            bid.setAlert(true);
                        }
                        if (bid != null) {
                            listBid.add(bid);
                        } else {
                            throw new Exception("Error - failed to createBid - currentBid=" + e);
                        }
                    } else {
                        throw new Exception("Error - bid length not valid - currentBid=" + e);
                    }
                }
            }
            isEndBid = GameBridgeRule.isBidsFinished(listBid);
            // init bid contract
            bidContract = null;
            if (isEndBid) {
                if (contractType == Constantes.CONTRACT_TYPE_PASS) {
                    bidContract = BridgeBid.createBid(BridgeConstantes.BID_PASS, declarer);
                } else {
                    if (contract != null && contract.length() > 0 && declarer != BridgeConstantes.POSITION_NOT_VALID) {
                        bidContract = BridgeBid.createBid(contract, declarer);
                    }
                }
            }
            // load list of car played
            listCard.clear();
            if (cards != null && cards.length() > 0) {
                if (bidContract == null) {
                    throw new Exception("Error no bid contract find - bids=" + bids);
                }
                int idxClaim = cards.indexOf(Constantes.GAME_INDICATOR_CLAIM);
                if (idxClaim > 0) {
                    if (cards.charAt(idxClaim-1) != Constantes.GAME_BIDCARD_SEPARATOR.charAt(0)) {
                        cards = cards.substring(0, idxClaim)+Constantes.GAME_BIDCARD_SEPARATOR+cards.substring(idxClaim);
                    }
                }
                String[] tempCards = cards.split(Constantes.GAME_BIDCARD_SEPARATOR);
                for (String e : tempCards) {
                    if (e.length() > 1) {
                        if (e.charAt(0) == Constantes.GAME_INDICATOR_CLAIM) {
                            // stop it is a claim !
                            break;
                        } else {
                            BridgeCard card = BridgeCard.createCard(e.substring(0, 2), e.charAt(2));
                            if (card != null) {
                                listCard.add(card);
                            } else {
                                throw new Exception("Error - failed to createCard - currentCard=" + e);
                            }
                        }
                    } else {
                        throw new Exception("Error - card length not valid - currentCard=" + e);
                    }
                }
            }
            initFailed = false;
        } catch (Exception e) {
            initFailed = true;
            Logger log = LogManager.getLogger(this.getClass());
            log.error("Failed to init data for game="+this, e);
        }
    }

    public boolean isInitFailed() {
        return initFailed;
    }

    /**
     * Return the deal associated to this game
     * @return
     */
    public Deal getDeal() {
        if (getTournament() != null) {
            return getTournament().getDealAtIndex(dealIndex);
        }
        return null;
    }

    public String getDealID() {
        if (getTournament() != null && getDeal() != null) {
            return getDeal().getDealID(getTournament().getIDStr());
        }
        return null;
    }

    /**
     * return the number of bids & cards played on this game
     * @return
     */
    public int getNbBidCardPlayed() {
        return listBid.size() + listCard.size();
    }

    /**
     * Return the current step of the game : equivalent to getNbBidCardPlayed
     * @return
     */
    public int getStep() {
        return getNbBidCardPlayed();
    }

    /**
     * Return the current player
     * @return
     */
    public char getCurrentPlayer() {
        if (!finished) {
            return GameBridgeRule.getNextPositionToPlay(getDeal().dealer, listBid, listCard, getBidContract());
        }
        return BridgeConstantes.POSITION_NOT_VALID;
    }

    /**
     * String with all bids played without player position
     * @return
     */
    public String getBidListStrWithoutPosition() {
        String str = "";
        if (listBid != null) {
            for (BridgeBid bid : listBid) {
                str += bid.toString();
            }
        }
        return str;
    }

    /**
     * String with all cards played without player position
     * @return
     */
    public String getCardListStrWithoutPosition() {
        String str = "";
        if (listCard != null) {
            for (BridgeCard card : listCard) {
                str += card.toString();
            }
        }
        return str;
    }

    /**
     * Return the number of tricks played
     * @return
     */
    public int getNbTricks() {
        return tricksWinner.length();
    }

    /**
     * Check if game is leaved (score=-32000)
     * @return
     */
    public boolean isLeaved() {
        return (score == Constantes.GAME_SCORE_LEAVE);
    }

    /**
     * Add card to the list
     * @param card
     */
    public void addCard(BridgeCard card) {
        if (card != null) {
            if (cards.length() > 0) {
                cards += Constantes.GAME_BIDCARD_SEPARATOR;
            }
            cards += card.getStringWithOwner();
            listCard.add(card);
        }
    }

    /**
     * Add bid to the list
     * @param bid
     */
    public void addBid(BridgeBid bid) {
        if (bid != null) {
            if (bids.length() > 0) {
                bids += Constantes.GAME_BIDCARD_SEPARATOR;
            }
            bids += bid.getStringWithOwnerAndAlert();
            listBid.add(bid);
        }
    }

    /**
     * Add winner to tricks winner
     * @param winnerPosition
     * @param logHistoric
     */
    public void addTrickWinner(char winnerPosition, boolean logHistoric) {
        if (logHistoric) {
            if (tricksWinnerHistoric == null) {
                tricksWinnerHistoric = "";
            } else {
                tricksWinnerHistoric += " - ";
            }
            tricksWinnerHistoric += "[step="+getStep()+" - before="+tricksWinner;
        }
        if (tricksWinner.length() < 13) {
            tricksWinner += Character.toString(winnerPosition);
        }
        if (logHistoric) {
            if (tricksWinnerHistoric == null) {
                tricksWinnerHistoric = "";
            }
            BridgeBid tempBidContract = getBidContract();
            tricksWinnerHistoric += " - cards=" + getCards() + " - listCard=" + StringTools.listToString(getListCard()) + " - bidContract="+(tempBidContract!=null?tempBidContract.getStringWithOwner():null)+" - winnerPosition=" + winnerPosition;
            tricksWinnerHistoric += " - after=" + tricksWinner + "]";
        }
    }

    /**
     * Return the contract string for application client using contract and contract type
     * @return
     */
    public String getContractWS() {
        String contractWS = "";
        if (isEndBid && !isLeaved()) {
            contractWS = Constantes.contractToString(contract, contractType);
        }
        return contractWS;
    }

    /**
     * Return the number of tricks win by this player and partenaire
     * @param player
     * @return
     */
    public int getNbTricksWinByPlayerAndPartenaire(char player) {
        int val = 0;
        if (tricksWinner.length() > 0) {
            for (int i = 0; i < tricksWinner.length(); i++) {
                if (tricksWinner.charAt(i) == player ||
                        tricksWinner.charAt(i) == GameBridgeRule.getPositionPartenaire(player))
                    val++;
            }
        }
        return val;
    }

    /**
     * Set data for game leaved : contract type, score, end bid, finished, lastDate
     */
    public void setLeaveValue() {
        setContractType(Constantes.CONTRACT_LEAVE);
        // set end of game
        setEndBid(true);
        setFinished(true);
        // set score
        setScore(Constantes.GAME_SCORE_LEAVE);
        setLastDate(System.currentTimeMillis());
    }

    /**
     * Set claim player for all remaining tricks
     * @param playerWinner
     */
    public void claimAllForPlayer(char playerWinner) {
        if (playerWinner != BridgeConstantes.POSITION_NOT_VALID) {
            if (cards.length() > 0) {
                cards += Constantes.GAME_BIDCARD_SEPARATOR;
            }
            cards += Constantes.GAME_INDICATOR_CLAIM+Character.toString(playerWinner);
            while (tricksWinner.length() < 13) {
                tricksWinner += Character.toString(playerWinner);
            }
        }
    }

    /**
     * Set claim player for nb tricks.
     * @param claimer
     * @param nbTricks
     */
    public void claimForPlayer(char claimer, int nbTricks) {
        if (claimer != BridgeConstantes.POSITION_NOT_VALID) {
            if (cards.length() > 0) {
                cards += Constantes.GAME_BIDCARD_SEPARATOR;
            }
            cards += Constantes.GAME_INDICATOR_CLAIM+Character.toString(claimer);
            cards += ""+nbTricks;
            // add claimer winner for nbTricks
            for (int i = 0; i < nbTricks; i++) {
                if (tricksWinner.length() < 13) {
                    tricksWinner += Character.toString(claimer);
                }
            }
            // add other tricks win by next claimer
            char nextClaimer = GameBridgeRule.getNextPosition(claimer);
            while (tricksWinner.length() < 13) {
                tricksWinner += Character.toString(nextClaimer);
            }
        }
    }

    public void setConventionSelection(int profil, String data) {
        setConvention(""+profil+Constantes.GAME_CONVENTION_SEPARATOR+data);
    }

    public void setCardsConventionSelection(int profil, String data) {
        setCardsConvention("" + profil + Constantes.GAME_CONVENTION_SEPARATOR + data);
    }

    public void changeConventionsSelection(int bidProfil, String bidData, int cardProfil, String cardData) {
        boolean isChanged = false;
        if (bidProfil > 0) {
            setConventionSelection(bidProfil, bidData);
            isChanged = true;
        }
        if (cardProfil > 0) {
            setCardsConventionSelection(cardProfil, cardData);
            isChanged = true;
        }
        if (isChanged) {
            parseConvention();
        }
    }

    public void resetData() {
        bidContract = null;
        bids = "";
        cards = "";
        contract = "";
        contractType = 0;
        convention = "";
        declarer = BridgeConstantes.POSITION_NOT_VALID;
        isEndBid = false;
        finished = false;
        listBid.clear();
        listCard.clear();
        rank = 0;
        result = 0;
        score = 0;
        tricks = 0;
        tricksWinner = "";
    }

    public void addBidInfoSouth(String bidList, String info) {
        mapBidInfoSouth.put(bidList, info);
    }

    public void clearBidInfoSouth() {
        mapBidInfoSouth.clear();
    }

    public Map<String, String> getMapBidInfoSouth() {
        return mapBidInfoSouth;
    }

    public String getAnalyzeBid() {
        return analyzeBid;
    }

    public void setAnalyzeBid(String analyzeBid) {
        this.analyzeBid = analyzeBid;
    }

    public String getBegins() {
        if (listCard != null && listCard.size() > 0) {
            return listCard.get(0).getStringWithOwner();
        }
        return null;
    }

    public int countBidForPlayerPosition(char playerPosition) {
        int count = 0;
        for (BridgeBid bid : listBid) {
            if (bid.getOwner() == playerPosition) {
                count++;
            }
        }
        return count;
    }

    public boolean checkAllBidInfoSouth() {
        String bidSequence = "";
        for (BridgeBid bid : listBid) {
            bidSequence += bid.getString();
            if (bid.getOwner() == BridgeConstantes.POSITION_SOUTH) {
                if (!mapBidInfoSouth.containsKey(bidSequence)) {
                    return false;
                }
            }
        }
        return true;
    }

    public synchronized void scanBidAlertForSouth() {
        if (mapBidInfoSouth.size() > 0) {
            boolean updateBidList = false;

            if (FBConfiguration.getInstance().getIntValue("game.newMethodScanBidAlertForSouth", 1) == 1) {
                // loop to search bids for south
                String bidsSequence = "";
                for (BridgeBid bid : listBid) {
                    bidsSequence += bid.getString();
                    if (bid.getOwner() == BridgeConstantes.POSITION_SOUTH) {
                        // get bidInfo
                        String resultBidInfo = mapBidInfoSouth.get(bidsSequence);
                        if (resultBidInfo != null && GameMgr.isAlertBidInfo(resultBidInfo)) {
                            bid.setAlert(true);
                            updateBidList = true;
                        }
                    }
                }
            } else {
                // loop on each bid info for south
                for (Map.Entry<String, String> entry : mapBidInfoSouth.entrySet()) {
                    String resultBidInfo = entry.getValue();
                    String bidList = entry.getKey();
                    if (bidList != null && bidList.length() > 0 && resultBidInfo != null) {
                        // check it is a bid alert
                        if (GameMgr.isAlertBidInfo(resultBidInfo)) {
                            int bidIdx = (bidList.length() / 2) - 1;
                            if (bidIdx >= 0 && bidIdx < listBid.size()) {
                                BridgeBid bid = listBid.get(bidIdx);
                                if (bid.getOwner() == BridgeConstantes.POSITION_SOUTH && !bid.isAlert()) {
                                    bid.setAlert(true);
                                    updateBidList = true;
                                }
                            }
                        }
                    }
                }
            }
            // bid list update =>
            if (updateBidList) {
                generateBidsField();
            }
        }
    }

    public void generateBidsField() {
        bids = "";
        for (BridgeBid bid : listBid) {
            if (bids.length() > 0) {
                bids += Constantes.GAME_BIDCARD_SEPARATOR;
            }
            bids += bid.getStringWithOwnerAndAlert();
        }
    }

    public boolean isAnalyzeSouthBidDone() {
        return analyzeSouthBidDone;
    }

    public void setAnalyzeSouthBidDone(boolean analyzeSouthBidDone) {
        this.analyzeSouthBidDone = analyzeSouthBidDone;
    }

    /**
     * Retrieve the bid player corresponding to the last bid for this sequence.
     * @param bidSequence
     * @return
     */
    public BridgeBid getBidPlayForSequence(String bidSequence) {
        String listBidStr = getBidListStrWithoutPosition();
        if (listBidStr.startsWith(bidSequence)) {
            int bidIdx = (bidSequence.length() / 2)-1;
            if (bidIdx >= 0 && bidIdx < listBid.size()) {
                return listBid.get(bidIdx);
            }
        }
        return null;
    }

    public String getBidInfoForSouth(String bidSequence) {
        return mapBidInfoSouth.get(bidSequence);
    }

    public String getTricksWinnerHistoric() {
        return tricksWinnerHistoric;
    }

    public void setTricksWinnerHistoric(String tricksWinnerHistoric) {
        this.tricksWinnerHistoric = tricksWinnerHistoric;
    }

    public boolean isEngineFailed() {
        return isEngineFailed;
    }

    public void setEngineFailed(boolean engineFailed) {
        isEngineFailed = engineFailed;
    }

    public WSResultDeal toWSResultDeal(){
        WSResultDeal resultPlayer = new WSResultDeal();
        Tournament tournament = getTournament();
        resultPlayer.setResultType(tournament.getResultType());
        resultPlayer.setDealIDstr(tournament.getDealAtIndex(dealIndex).getDealID(tournament.getIDStr()));
        resultPlayer.setDealIndex(dealIndex);
        resultPlayer.setPlayed(true);
        resultPlayer.setContract(getContractWS());
        resultPlayer.setDeclarer(Character.toString(getDeclarer()));
        resultPlayer.setNbTricks(getTricks());
        resultPlayer.setScore(getScore());
        resultPlayer.setRank(getRank());
        resultPlayer.setResult(getResult());
        resultPlayer.setLead(getBegins());
        return resultPlayer;
    }

    public boolean copyBidsFromGame(Game game) {
        if (game != null && game.isEndBid() && game.getContractType() != Constantes.CONTRACT_TYPE_PASS && game.getContractType() != Constantes.CONTRACT_LEAVE) {
            setBids(game.getBids());
            setEndBid(true);
            listBid.clear();
            listBid.addAll(game.getListBid());
            setContract(game.getContract());
            setContractType(game.getContractType());
            setDeclarer(game.getDeclarer());
            return true;
        }
        return false;
    }

    public boolean buildContract() {
        if (GameBridgeRule.isBidsFinished(getListBid())) {
            setEndBid(true);
            BridgeBid higherBid = GameBridgeRule.getHigherBid(getListBid());
            if (higherBid.isPass()) {
                setContractType(Constantes.CONTRACT_TYPE_PASS);
            } else{
                setContract(higherBid.toString());
                setDeclarer(GameBridgeRule.getWinnerBids(getListBid()));
                if (GameBridgeRule.isX2(getListBid())) {
                    setContractType(Constantes.CONTRACT_TYPE_X2);
                } else if (GameBridgeRule.isX1(getListBid())) {
                    setContractType(Constantes.CONTRACT_TYPE_X1);
                } else {
                    setContractType(Constantes.CONTRACT_TYPE_NORMAL);
                }
            }
            return true;
        }
        return false;
    }

    public int getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(int engineVersion) {
        this.engineVersion = engineVersion;
    }

    public int getSpreadRefuseStep() {
        return spreadRefuseStep;
    }

    public void setSpreadRefuseStep(int spreadRefuseStep) {
        this.spreadRefuseStep = spreadRefuseStep;
    }
}
