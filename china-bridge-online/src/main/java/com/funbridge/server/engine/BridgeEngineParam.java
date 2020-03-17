package com.funbridge.server.engine;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.tournament.data.TournamentDeal;
import com.funbridge.server.tournament.data.TournamentGame2;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.ws.engine.PlayGameStepData;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.bridge.GameBridgeRule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BridgeEngineParam {
	private char dealer;
	private char vul;
	private String distrib = "";
	private String bidList = "";
	private String cardList = "";
	private String conventions = "";
	private String options = "";
    private boolean useCache = true;
    private String asyncID = null;
    private boolean useWebsocket = false;
    private int nbTricksForClaim = 0;
    private char claimPlayer = BridgeConstantes.POSITION_NOT_VALID;
	
	public char getDealer() {
		return dealer;
	}
	public String getDealerStr() {
		return Character.toString(dealer);
	}
	public void setDealer(char dealer) {
		this.dealer = dealer;
	}
	public char getVul() {
		return vul;
	}
	public String getVulStr() {
		return Character.toString(vul);
	}
	public void setVul(char vul) {
		this.vul = vul;
	}
	public String getDistrib() {
		return distrib;
	}
	public void setDistrib(String distrib) {
		this.distrib = distrib;
	}
	public String getBidList() {
		return bidList;
	}
	public void setBidList(String bidList) {
		this.bidList = bidList;
	}
	public String getCardList() {
		return cardList;
	}
	public void setCardList(String cardList) {
		this.cardList = cardList;
	}
	public String getConventions() {
		return conventions;
	}
	public void setConventions(String conventions) {
		this.conventions = conventions;
	}
	public String getOptions() {
		return options;
	}
	public void setOptions(String options) {
		this.options = options;
	}

    public int getNbTricksForClaim() {
        return nbTricksForClaim;
    }

    public void setNbTricksForClaim(int nbTricksForClaim) {
        this.nbTricksForClaim = nbTricksForClaim;
    }

    public char getClaimPlayer() {
        return claimPlayer;
    }

    public void setClaimPlayer(char claimPlayer) {
        this.claimPlayer = claimPlayer;
    }

    public String getAsyncID() {
        return asyncID;
    }

    public void setAsyncID(String asyncID) {
        this.asyncID = asyncID;
    }

    public boolean isUseWebsocket() {
        return useWebsocket;
    }

    public void setUseWebsocket(boolean useWebsocket) {
        this.useWebsocket = useWebsocket;
    }

    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public boolean isEndBid() {
		if (bidList.length() >= 8) {
			return GameBridgeRule.isEndBids(bidList);
		}
		return false;
	}
	
	public boolean isBeginTrick() {
		return isEndBid() && (cardList.length() % 8 == 0);
	}
	
	public boolean isEndGame() {
		return isEndBid() && (cardList.length() == 52*2);
	}
	
	public String toString() {
		return "dealer="+ dealer +" - vul="+ vul +" - distrib="+distrib+" - conv="+conventions+
			" - options="+options+" - bids="+bidList+" - cards="+cardList+" - nbTricksForClaim="+nbTricksForClaim+" - claimPlayer="+ claimPlayer +" - asyncID="+asyncID+" - useWebsocket="+useWebsocket;
	}
	
	/**
	 * Convert int value to string hexa
	 * @param value must be > 0
	 * @return
	 */
	public static String intToHexaString(int value) {
		StringBuilder sb = new StringBuilder();
		sb.append(Integer.toHexString(value));
		if (sb.length() %2 == 1) {
			sb.insert(0, "0");
		}
		return sb.toString().toUpperCase();
	}
	
	/**
	 * Build options according to result type and engine version
	 * @param resultType
	 * @param engineVersion
	 * @return
	 */
	public static String buildOptionsForEngine(int resultType, int engineVersion, boolean enableSpread) {
		int engineSel = FBConfiguration.getInstance().getIntValue("tournament.engine.options.selection", 0);
		int engineSpeed = FBConfiguration.getInstance().getIntValue("tournament.engine.options.speed", 0);
		// options field : 0=engine selection - 1=engine speed - 2=result type - 3&4=engine version
		String result = "";
		result += intToHexaString(engineSel & 0xFF);
		result += intToHexaString(engineSpeed & 0xFF);
		result += intToHexaString(resultType & 0xFF);
		if (engineVersion >= FBConfiguration.getInstance().getIntValue("engine.version.limite", 600)) {
			engineVersion = FBConfiguration.getInstance().getIntValue("engine.argine.version", 0);
		}
		result += intToHexaString(engineVersion & 0xFF); // lowbyte
		result += intToHexaString((engineVersion & 0xFF00) >> 8); // highbyte
        result += intToHexaString((enableSpread?1:0)&0xFF);
		return result;
	}

    /**
     * Build an engine param
     * @param distrib 52 char, use "SSSSSSSSSSSSSWWWWWWWWWWWWWNNNNNNNNNNNNNEEEEEEEEEEEEE" in the case bof bidinfo request type
     * @param bids sequence of bids without position
     * @param cards sequence of cards without position
     * @param dealer
     * @param vulnerability
     * @param requestType 0:BID - 1:CARD - 2:BIDINFO
     * @param engineVersion
     * @param resultType 0:PAIRE - 1:IMP
     * @param conventionProfile convention profile bids
     * @param conventionData convention data bids (case of free profile)
     * @param cardsConventionProfile convention profile cards
     * @param cardsConventionData convention data cards (case of free profile)
     * @return
     */
    public static BridgeEngineParam createParamFullData(String distrib, String bids, String cards, char dealer, char vulnerability,
                                                        int requestType, int engineVersion, int resultType,
                                                        int conventionProfile, String conventionData, int cardsConventionProfile, String cardsConventionData) {
        Log log = LogFactory.getLog(BridgeEngineParam.class);
        if (log.isDebugEnabled()) {
            log.debug("Param : distrib=" + distrib + " - bids=" + bids + " - cards=" + cards + " - deader=" + dealer + " - vulnerability=" + vulnerability + " - requestType=" + requestType + " - engineVersion=" + engineVersion + " - resultType=" + resultType + " - conventionProfile=" + conventionProfile + " - conventionData=" + conventionData);
        }
        if (distrib != null && distrib.length() == 52) {
            String conv = ContextManager.getArgineEngineMgr().buildConvention(conventionProfile, conventionData, cardsConventionProfile, cardsConventionData, "createParamFullData - Param : distrib="+distrib+" - bids="+bids+" - cards="+cards+" - deader="+ dealer +" - vulnerability="+ vulnerability +" - requestType="+requestType+" - engineVersion="+engineVersion+" - resultType="+resultType+" - conventionProfile="+conventionProfile+" - conventionData="+conventionData);
            boolean bTypeValid = false;
            boolean useCache = true;
            int useCacheLimitNbTricks = FBConfiguration.getInstance().getIntValue("engine.useCacheLimitNbTricks", 0);
            // 1 trick => 8 chars
            if (useCacheLimitNbTricks > 0 && cards.length() > (useCacheLimitNbTricks*8)) {
                useCache = false;
            }
            switch (requestType) {
                // BID, CARD
                case Constantes.ENGINE_REQUEST_TYPE_BID:
                case Constantes.ENGINE_REQUEST_TYPE_CARD:
                case Constantes.ENGINE_REQUEST_TYPE_CLAIM:
                {
                    bTypeValid = true;
                    break;
                }
                // BID INFO
                case Constantes.ENGINE_REQUEST_TYPE_BID_INFO:
                {
                    // no need distrib. Set always same bid to optimize cache size
                    bTypeValid = true;
                    if (dealer == 'N' || dealer == 'S') {
                        dealer = 'S';
                    } else {
                        dealer = 'E';
                    }
                    resultType = 0;
                    distrib = "SSSSSSSSSSSSSWWWWWWWWWWWWWNNNNNNNNNNNNNEEEEEEEEEEEEE";
                    break;
                }
            }
            String options = buildOptionsForEngine(resultType, engineVersion, false);
            if (bTypeValid) {
                BridgeEngineParam param = new BridgeEngineParam();
                param.setDealer(dealer);
                param.setVul(vulnerability);
                param.setDistrib(distrib);
                param.setBidList(bids);
                param.setCardList(cards);
                param.setConventions(conv);
                param.setOptions(options);
                param.setUseCache(useCache);
                log.debug("Engine param : "+param.toString());
                return param;
            } else {
                log.error("ERROR request type not valid ... - distrib=" + distrib + " - bids=" + bids + " - cards=" + cards + " - deader=" + dealer + " - vulnerability=" + vulnerability + " - requestType=" + requestType + " - engineVersion=" + engineVersion + " - resultType=" + resultType + " - conventionProfile=" + conventionProfile + " - conventionData=" + conventionData);
            }
        } else {
            log.error("ERROR distrib param not valid : distrib="+distrib);
        }
        return null;
    }

	/**
	 * Create a BridgeEngineParam object associated to the game
	 * @param game
	 * @param requestType
	 * @return
	 */
	public static BridgeEngineParam createParam(TournamentGame2 game, int requestType) {
		if (game != null) {
            TournamentDeal deal = game.getDeal();
			String options = "", distrib = "", bids = "", cards = "", conv = "";
			char dealer = BridgeConstantes.POSITION_NOT_VALID, vul = BridgeConstantes.POSITION_NOT_VALID;
            int engineVersion = ContextManager.getArgineEngineMgr().getEngineVersion(Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER);
			int resultType = deal.getTournament().getResultType();
			boolean bTypeValid = false;
            boolean useCache = true;
            int useCacheLimitNbTricks = FBConfiguration.getInstance().getIntValue("engine.useCacheLimitNbTricks", 0);
            if (useCacheLimitNbTricks > 0 && game.isEndBid() && game.getNbTricks() > useCacheLimitNbTricks) {
                useCache = false;
            }
			switch (requestType) {
			// BID, CARD
			case Constantes.ENGINE_REQUEST_TYPE_BID:
			case Constantes.ENGINE_REQUEST_TYPE_CARD:
			case Constantes.ENGINE_REQUEST_TYPE_CLAIM:
			{
				bTypeValid = true;
				dealer = deal.getDistribution().getDealer();
				vul = deal.getDistribution().getVulnerability();
				distrib = deal.getDistribution().getCards();
				bids = game.getBidListStrWithoutPosition();
				cards = game.getCardListStrWithoutPosition();
                conv = ContextManager.getArgineEngineMgr().buildConvention(game.getConventionProfile(), game.getConventionData());
				break;
			}
			// BID INFO
			case Constantes.ENGINE_REQUEST_TYPE_BID_INFO:
			{
				// no need distrib. Set always same bid to optimize cache size
				bTypeValid = true;
				if (deal.getDistribution().getDealer() == 'N' || deal.getDistribution().getDealer() == 'S') {
					dealer = 'S';
				} else {
					dealer = 'E';
				}
				resultType = 0;
				vul = 'A';
				distrib = "SSSSSSSSSSSSSWWWWWWWWWWWWWNNNNNNNNNNNNNEEEEEEEEEEEEE";
				// not set bids because it is done by called method (getBidInfo)
                conv = ContextManager.getArgineEngineMgr().buildConvention(game.getConventionProfile(), game.getConventionData());
				break;
			}
			}
			boolean enableSpread = false;
            if (requestType == Constantes.ENGINE_REQUEST_TYPE_CARD && game.getNbTricks() >=5 && (game.getCurrentPlayer() == BridgeConstantes.POSITION_EAST || game.getCurrentPlayer() == BridgeConstantes.POSITION_WEST)) {
                enableSpread = true;
            }
			options = buildOptionsForEngine(Constantes.tournamentResult2option(resultType), engineVersion, enableSpread);
			if (bTypeValid) {
				BridgeEngineParam param = new BridgeEngineParam();
				param.setDealer(dealer);
				param.setVul(vul);
				param.setDistrib(distrib);
				param.setBidList(bids);
				param.setCardList(cards);
				param.setConventions(conv);
				param.setOptions(options);
                param.setUseCache(useCache);
				return param;
			}
		}
		return null;
	}

	/**
	 * Create a BridgeEngineParam object associated to the game
	 * @param game
	 * @param requestType
	 * @return
	 */
	public static BridgeEngineParam createParam(Game game, int requestType, boolean synchroMethod) {
		if (game != null) {
			String options = "", distrib = "", bids = "", cards = "", conv = "";
			char dealer = BridgeConstantes.POSITION_NOT_VALID, vul = BridgeConstantes.POSITION_NOT_VALID;
            int engineVersion = ContextManager.getArgineEngineMgr().getEngineVersion(game.getTournament().getCategory());
			int resultType = game.getTournament().getResultType();
			boolean bTypeValid = false;
            boolean useCache = true;
            int useCacheLimitNbTricks = FBConfiguration.getInstance().getIntValue("engine.useCacheLimitNbTricks", 0);
            if (useCacheLimitNbTricks > 0 && game.isEndBid() && game.getNbTricks() > useCacheLimitNbTricks) {
                useCache = false;
            }

            switch (requestType) {
				// BID, CARD
				case Constantes.ENGINE_REQUEST_TYPE_BID:
				case Constantes.ENGINE_REQUEST_TYPE_CARD:
                case Constantes.ENGINE_REQUEST_TYPE_CLAIM:
				{
					bTypeValid = true;
					dealer = game.getDeal().getDealer();
					vul = game.getDeal().getVulnerability();
					distrib = game.getDeal().getCards();
					bids = game.getBidListStrWithoutPosition();
					cards = game.getCardListStrWithoutPosition();
					conv = ContextManager.getArgineEngineMgr().buildConvention(game.getConventionProfile(), game.getConventionData(), game.getCardsConventionProfile(), game.getCardsConventionData(), game.toString());

                    if (useCache && FBConfiguration.getInstance().getIntValue("engine.useCacheBidCardForCategory."+game.getTournament().getCategory(), 1) == 0) {
                        useCache = false;
                    }
					break;
				}
				// BID INFO
				case Constantes.ENGINE_REQUEST_TYPE_BID_INFO:
				{
					// no need distrib. Set always same bid to optimize cache size
					bTypeValid = true;
					if (game.getDeal().getDealer() == 'N' || game.getDeal().getDealer() == 'S') {
						dealer = 'S';
					} else {
						dealer = 'E';
					}
					resultType = 0;
					vul = 'A';
					distrib = "SSSSSSSSSSSSSWWWWWWWWWWWWWNNNNNNNNNNNNNEEEEEEEEEEEEE";
					// not set bids because it is done by called method (getBidInfo)
					conv = ContextManager.getArgineEngineMgr().buildConvention(game.getConventionProfile(), game.getConventionData(), game.getCardsConventionProfile(), game.getCardsConventionData(), game.toString());
					break;
				}
			}
            boolean enableSpread = false;
            if (FBConfiguration.getInstance().getIntValue("game.enableSpreadArgine", 1) == 1 &&
                    requestType == Constantes.ENGINE_REQUEST_TYPE_CARD && game.getNbTricks() >= FBConfiguration.getInstance().getIntValue("game.spreadArgineNbTricks", 2) &&
                    (game.getCurrentPlayer() == BridgeConstantes.POSITION_EAST || game.getCurrentPlayer() == BridgeConstantes.POSITION_WEST)) {
                enableSpread = true;
            }
			options = buildOptionsForEngine(Constantes.tournamentResult2option(resultType), engineVersion, enableSpread);
			if (bTypeValid) {
				BridgeEngineParam param = new BridgeEngineParam();
				param.setDealer(dealer);
				param.setVul(vul);
				param.setDistrib(distrib);
				param.setBidList(bids);
				param.setCardList(cards);
				param.setConventions(conv);
				param.setOptions(options);
                param.setUseCache(useCache);
                if (!synchroMethod) {
                    // not use async methos for BID INFO
                    if (requestType != Constantes.ENGINE_REQUEST_TYPE_BID_INFO) {
                        param.setAsyncID(PlayGameStepData.buildAsyncID(game.getIDStr(), game.getStep()));
                    }
                }
				return param;
			}
		}
		return null;
	}

    /**
     * Create a BridgeEngineParam object to get PAR info
     * @param dealer
     * @param vulnerability
     * @param distrib
     * @param resultType
     * @param engineVersion
     * @return
     */
    public static BridgeEngineParam createParamPar(char dealer, char vulnerability, String distrib, int resultType, int engineVersion) {
        BridgeEngineParam param = new BridgeEngineParam();
        param.setDealer(dealer);
        param.setVul(vulnerability);
        param.setDistrib(distrib);
        param.setOptions(buildOptionsForEngine(Constantes.tournamentResult2option(resultType), engineVersion, false));
        return param;
    }
}
