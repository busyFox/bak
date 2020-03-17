package com.gotogames.common.bridge;

import java.util.*;

public class BridgeGame {
	List<BridgeCard> distribution = null;
	List<BridgeCard> cardPlayed = null;
	List<BridgeBid> bidPlayed = null;
	char dealerPosition;
	char vulnerability;
//	String trickWinner = "";
	BridgeBid contract = null;
	char currentPlayer;
	boolean isPhaseCard = false;
	int[][] nbPlayerCard = new int[4][4];
	String event = "";
    String result = "";
    public int contractType = -1; //0=>PASS, 1=>NORMAL, 2=>X1, 3=>X2
    public Map<String, String> mapMetaData = new HashMap<>();


    public static final int CONTRACT_TYPE_PASS = 0;
    public static final int CONTRACT_TYPE_NORMAL = 1;
    public static final int CONTRACT_TYPE_X1 = 2;
    public static final int CONTRACT_TYPE_X2 = 3;


	public BridgeGame() {
		for (int i = 0; i < nbPlayerCard.length; i++) {
			Arrays.fill(nbPlayerCard[i], 0);
		}
	}
	
	public static int position2Index(char pos) {
		switch (pos) {
		case BridgeConstantes.POSITION_SOUTH : return 0;
		case BridgeConstantes.POSITION_WEST : return 1;
		case BridgeConstantes.POSITION_NORTH : return 2;
		case BridgeConstantes.POSITION_EAST : return 3;
		}
		return -1;
	}
	
	public static char index2Pos(int idx) {
		switch (idx) {
		case 0 : return BridgeConstantes.POSITION_SOUTH;
		case 1 : return BridgeConstantes.POSITION_WEST;
		case 2 : return BridgeConstantes.POSITION_NORTH;
		case 3 : return BridgeConstantes.POSITION_EAST;
		}
		return BridgeConstantes.POSITION_NOT_VALID;
	}
	
	public static int color2Index(CardColor col) {
		switch (col) {
		case Club : return 0;
		case Diamond : return 1;
		case Heart : return 2;
		case Spade : return 3;
		}
		return -1;
	}
	
	public static CardColor index2Color(int idx) {
		switch (idx) {
		case 0 : return CardColor.Club;
		case 1 : return CardColor.Diamond;
		case 2 : return CardColor.Heart;
		case 3 : return CardColor.Spade;
		}
		return CardColor.Club;
	}
	
	public static BridgeGame create(String deal, List<BridgeBid> bids, List<BridgeCard> cards) {
		if (deal != null && deal.length() == 54 && bids != null && cards != null) {
			BridgeGame game = new BridgeGame();
			game.dealerPosition = deal.charAt(0);
			game.vulnerability = deal.charAt(1);
			game.distribution = GameBridgeRule.convertCardDealToList(deal.substring(2));
			game.bidPlayed = bids;
			game.cardPlayed = cards;
			if (GameBridgeRule.isBidsFinished(game.bidPlayed)) {
				game.isPhaseCard = true;
				BridgeBid higherBid = GameBridgeRule.getHigherBid(game.bidPlayed);
				if (higherBid != null) {
					game.contract = BridgeBid.createBid(higherBid.getString(), higherBid.getOwner());
					game.contract.setOwner(GameBridgeRule.getWinnerBids(game.bidPlayed));
                    if (higherBid.isPass()) {
                        game.contractType = CONTRACT_TYPE_PASS;
                    } else {
                        if (GameBridgeRule.isX2(game.getBidList())) {
                            game.contractType = CONTRACT_TYPE_X2;
                        } else if (GameBridgeRule.isX1(game.getBidList())) {
                            game.contractType = CONTRACT_TYPE_X1;
                        } else {
                            game.contractType = CONTRACT_TYPE_NORMAL;
                        }
                    }
				}
//				game.contract = GameBridgeRule.getHigherBid(game.bidPlayed);
//				game.contract.setOwner(GameBridgeRule.getWinnerBids(game.bidPlayed));
			}
			game.currentPlayer = GameBridgeRule.getNextPositionToPlay(game.dealerPosition, game.bidPlayed, game.cardPlayed, game.contract);
			for (int i = 0; i <game.nbPlayerCard.length; i++) {
				char curPla = index2Pos(i);
				for (int j = 0; j < game.nbPlayerCard[i].length; j++) {
					CardColor curColor = index2Color(j);
					game.nbPlayerCard[i][j] = GameBridgeRule.getRemainingNbCardsOnColorForPlayer(game.distribution, game.cardPlayed, curPla, curColor);
				}
			}
			return game;
		}
		return null;
	}
	
	public static BridgeGame create(String distrib, char dealer, char vul, String bids, String cards) {
		BridgeGame game = new BridgeGame();
		game.dealerPosition = dealer;
		game.vulnerability = vul;
		game.distribution = GameBridgeRule.convertCardDealToList(distrib);
		game.bidPlayed = GameBridgeRule.convertPlayBidsStringToList(bids, dealer);
		if (GameBridgeRule.isBidsFinished(game.bidPlayed)) {
			game.isPhaseCard = true;
			BridgeBid higherBid = GameBridgeRule.getHigherBid(game.bidPlayed);
			if (higherBid != null) {
				game.contract = BridgeBid.createBid(higherBid.getString(), higherBid.getOwner());
				game.contract.setOwner(GameBridgeRule.getWinnerBids(game.bidPlayed));
				game.cardPlayed = GameBridgeRule.convertPlayCardsStringToList(cards, game.contract);
                if (higherBid.isPass()) {
                    game.contractType = CONTRACT_TYPE_PASS;
                } else {
                    if (GameBridgeRule.isX2(game.getBidList())) {
                        game.contractType = CONTRACT_TYPE_X2;
                    } else if (GameBridgeRule.isX1(game.getBidList())) {
                        game.contractType = CONTRACT_TYPE_X1;
                    } else {
                        game.contractType = CONTRACT_TYPE_NORMAL;
                    }
                }
			}
		}
		game.currentPlayer = GameBridgeRule.getNextPositionToPlay(dealer, game.bidPlayed, game.cardPlayed, game.contract);
		
		for (int i = 0; i <game.nbPlayerCard.length; i++) {
			char curPla = index2Pos(i);
			for (int j = 0; j < game.nbPlayerCard[i].length; j++) {
				CardColor curColor = index2Color(j);
				game.nbPlayerCard[i][j] = GameBridgeRule.getRemainingNbCardsOnColorForPlayer(game.distribution, game.cardPlayed, curPla, curColor);
			}
		}
		
		return game;
	}
	
	public int getNbCardForColor(char player, CardColor color) {
		int idxPla = position2Index(player);
		int idxCol = color2Index(color);
		if (idxPla != -1 && idxCol != -1) {
			return nbPlayerCard[idxPla][idxCol];
		}
		return 0;
	}
	
	public int getNbCardForColor2(CardColor color) {
		int idxCol = color2Index(color);
		int val = 0;
		if (idxCol != -1) {
			for (int i = 0; i < nbPlayerCard.length; i++) {
				val += nbPlayerCard[i][idxCol];
			}
		}
		return val;
	}
	
	public int getNbCardForPlayer(char player) {
		int val = 0;
		int idxPla = position2Index(player);
		if (idxPla != -1) {
			for (int i = 0; i <nbPlayerCard[idxPla].length; i++) {
				val += nbPlayerCard[idxPla][i];
			}
		}
		return val;
	}
	
	public String toString() {
		String CRLF = System.getProperty("line.separator");
		String deal = "";
		
		if (distribution != null) {
			for (BridgeCard c: distribution) {
				if (deal.length() > 0) deal+="-";
				deal += c.toString()+c.getOwner();
			}
		}
		String bids = "";
		if (bidPlayed != null) {
			for (BridgeBid b: bidPlayed) {
				if (bids.length() > 0) bids+="-";
				bids += b.toString()+b.getOwner();
			}
		}
		String cards = "";
		if (cardPlayed != null) {
			for (BridgeCard c : cardPlayed) {
				if (cards.length() > 0) cards+="-";
				cards += c.toString()+c.getOwner();
			}
		}
		String contractDeclarer = "";
		if (contract != null) {
			contractDeclarer=contract.toString()+"-"+Character.toString(contract.getOwner());
		}
		String str = "GAME:"+CRLF+"Deal=dealer:"+Character.toString(dealerPosition)+" vul:"+Character.toString(vulnerability)+" distrib:"+deal+CRLF+"Contract="+contractDeclarer+CRLF;
		str += "Bids="+bids+CRLF+"Cards="+cards;
		return str;
	}
	
	public char getDeclarer() {
		if (bidPlayed != null && GameBridgeRule.isBidsFinished(bidPlayed)) {
			return GameBridgeRule.getWinnerBids(bidPlayed);
		}
		return BridgeConstantes.POSITION_NOT_VALID;
	}
	
	public BridgeBid getContract() {
		return contract;
	}

    public String getContractWithType() {
        if (contract != null) {
            String result = contract.getString();
            if (contractType == CONTRACT_TYPE_X1) {
                result += "X1";
            } else if (contractType == CONTRACT_TYPE_X2) {
                result += "X2";
            }
            return result;
        }
        return null;
    }
	
	public boolean isBeginTrick() {
		return GameBridgeRule.isBeginTrick(cardPlayed);
	}
	
	public boolean isPhaseCard() {
		return isPhaseCard;
	}
	
	public char getDealer() {
		return dealerPosition;
	}
	
	public char getVulnerability() {
		return vulnerability;
	}
	
	public List<BridgeCard> getDistribution() {
		return distribution;
	}
	
	public List<BridgeBid> getBidList() {
		return bidPlayed;
	}
	
	public List<BridgeCard> getCardList() {
		return cardPlayed;
	}
	
	public String getDistributionString() {
		char[] distrib = new char[52];
		Arrays.fill(distrib, BridgeConstantes.POSITION_NOT_VALID);
		for (BridgeCard card : distribution) {
			int idx = -1;
			for (int i = 0; i < BridgeConstantes.TAB_CARD.length; i++) {
				if (BridgeConstantes.TAB_CARD[i].equals(card.getString())) {
					idx = i;
					break;
				}
			}
			if (idx >=0 && idx < distrib.length) {
				distrib[idx] = card.getOwner();
			}
		}
		return new String(distrib);
	}

    public String getDistributionStringRemaining(char charCardPlayed) {
        char[] distrib = new char[52];
        Arrays.fill(distrib, charCardPlayed);
        List<BridgeCard> listCardRemaining = GameBridgeRule.getRemainingCards(distribution, cardPlayed);
        for (BridgeCard card : listCardRemaining) {
            int idx = -1;
            for (int i = 0; i < BridgeConstantes.TAB_CARD.length; i++) {
                if (BridgeConstantes.TAB_CARD[i].equals(card.getString())) {
                    idx = i;
                    break;
                }
            }
            if (idx >=0 && idx < distrib.length) {
                distrib[idx] = card.getOwner();
            }
        }
        return new String(distrib);
    }
	
	public static boolean isDistributionValid(String cards) {
		if (cards != null && cards.length() == 52) {
			if (cards.matches("[SWNE]*")) {
				int nbS = cards.replaceAll("[^S]", "").length();
				int nbN = cards.replaceAll("[^N]", "").length();
				int nbE = cards.replaceAll("[^E]", "").length();
				int nbW = cards.replaceAll("[^W]", "").length();
				return (nbS==13 && nbN==13 && nbE==13 && nbW==13);
			}
		}
		return false;
	}
	
	/**
	 * Check bid list is a valid bid sequence
	 * @return
	 */
	public boolean isBidListValid() {
		if (bidPlayed != null && bidPlayed.size() > 0) {
			List<BridgeBid> temp = new ArrayList<BridgeBid>();
			for (BridgeBid b : bidPlayed) {
				if (!GameBridgeRule.isBidValid(temp, b)) {
//					ContextManager.getDealMgr().getLog().error("Bid list is not valid list="+bids+" - error on bid="+b);
					return false;
				}
				temp.add(b);
			}
			temp.clear();
		}
		return true;
	}

    public String getStringBidsWithPosition() {
        String result = "";
        for (BridgeBid b : bidPlayed) {
            if (result.length() > 0) {result+="-";}
            result += b.getStringWithOwner();
        }
        return result;
    }

    public String getStringCardsWithPosition() {
        String result = "";
        for (BridgeCard c : cardPlayed) {
            if (result.length() > 0) {result+="-";}
            result += c.getStringWithOwner();
        }
        return result;
    }
	
	/**
	 * Check card list is a valid card sequence
	 * @return
	 */
	public boolean isCardListValid() {
		if (cardPlayed != null && cardPlayed.size() > 0) {
			List<BridgeCard> temp = new ArrayList<BridgeCard>();
			for (BridgeCard c : cardPlayed) {
				if (!GameBridgeRule.isCardValid(temp, c, distribution)) {
//					ContextManager.getDealMgr().getLog().error("Card list is not valid list="+cards+" - error on card="+c);
					return false;
				}
				temp.add(c);
			}
			temp.clear();
		}
		return true;
	}
	
	public String getEvent() {
		return event;
	}

    public String getResult() {
        return result;
    }

    public char getCardOwner(String card) {
        for (BridgeCard e : distribution) {
            if (e.getString().equals(card)) {
                return e.getOwner();
            }
        }
        return BridgeConstantes.POSITION_NOT_VALID;
    }

    public String getMetadata(String key) {
        return mapMetaData.get(key);
    }
}

