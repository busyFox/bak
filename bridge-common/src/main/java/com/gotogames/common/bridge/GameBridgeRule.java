package com.gotogames.common.bridge;

import java.util.ArrayList;
import java.util.List;

public class GameBridgeRule {

	
	/**
	 * Convert the string bids to list of BridgeBid
	 * @param bids the bids as string (PA1C2TPAPA)
	 * @param dealerPosition dealer position of player who distribute cards (S, N, W or E)
	 * @return
	 */
	public static List<BridgeBid> convertPlayBidsStringToList(String bids, char dealerPosition) {
		List<BridgeBid> list = new ArrayList<BridgeBid>();
		if (bids != null && bids.length() %2 == 0){
			char pos = dealerPosition;
			for (int i = 0; i < bids.length(); i=i+2) {
				BridgeBid bid = BridgeBid.createBid(bids.substring(i, i+2), pos);
				if (bid != null) {
					list.add(bid);
					pos = getNextPosition(pos);
				} else {
					return null;
				}
			}
			return list;
		} else {
			return null;
		}
	}
	
	
	/**
	 * Convert the deal string to list of Bridgecard
	 * @param deal the deal as string. 52 char.
	 * @return the list of card or nul if a problem occurs
	 */
	public static List<BridgeCard> convertCardDealToList(String deal) {
		List<BridgeCard> list = null;
		if (deal.length() == 52) {
			list = new ArrayList<BridgeCard>();
			for (int i = 0; i < deal.length(); i++) {
				BridgeCard card = BridgeCard.createCard(BridgeConstantes.TAB_CARD[i], deal.charAt(i));
				if (card != null) {
					list.add(card);
				} else {
					return null;
				}
			}
		}
		return list;
	}
	
	/**
	 * Convert a string with card played (without position) to list with position (computing using contract)
	 * @param playString
	 * @param contract
	 * @return
	 */
	public static List<BridgeCard> convertPlayCardsStringToList(String playString, BridgeBid contract) {
		List <BridgeCard> list = new ArrayList<BridgeCard>();
		if (playString.length() > 0 && (playString.length() % 2 == 0) && (contract != null)) {
			int nbCard = 0;
			int index = 0;
			char curPos = GameBridgeRule.getNextPosition(contract.getOwner());
			while (index < playString.length()) {
				BridgeCard card = BridgeCard.createCard(playString.substring(index, index+2), curPos);
				if (card == null) {
					list.clear();
					break;
				}
				list.add(card);
				nbCard++;
				index = index +2;
				if (nbCard == 4) {
					BridgeCard winner = getLastWinnerTrick(list, contract);
					if (winner == null) {
						list.clear();
						break;
					}
					curPos = winner.getOwner();
					nbCard = 0;
				} else {
					curPos = GameBridgeRule.getNextPosition(curPos);
				}
			}
		}
		return list;
	}
	/**
	 * Return the next position after the position pos
	 * @param pos the current position
	 * @return a char of the next position
	 */
	public static char getNextPosition(char pos) {
		if (pos == BridgeConstantes.POSITION_SOUTH) {
			return BridgeConstantes.POSITION_WEST;
		}
		if (pos == BridgeConstantes.POSITION_WEST) {
			return BridgeConstantes.POSITION_NORTH;
		}
		if (pos == BridgeConstantes.POSITION_NORTH) {
			return BridgeConstantes.POSITION_EAST;
		}
		if (pos == BridgeConstantes.POSITION_EAST) {
			return BridgeConstantes.POSITION_SOUTH;
		}
		return BridgeConstantes.POSITION_NOT_VALID;
	}
	
	/**
	 * Return the next player position
	 * @param dealer
	 * @param bids
	 * @param cardsPlayed
	 * @param contract
	 * @return
	 */
	public static char getNextPositionToPlay(char dealer, List<BridgeBid> bids, List<BridgeCard> cardsPlayed, BridgeBid contract) {
		if (bids == null || bids.size() == 0) {
			return dealer;
		}
		if (!isBidsFinished(bids)) {
			return getNextPosition(bids.get(bids.size()-1).getOwner());
		}
		if (cardsPlayed == null || cardsPlayed.size() == 0) {
			char winner = getWinnerBids(bids);
			if (winner != BridgeConstantes.POSITION_NOT_VALID) {
				return getNextPosition(winner);
			}
		} else if (contract != null){
			if (cardsPlayed.size() % 4 == 0) {
				BridgeCard lastWinner = getLastWinnerTrick(cardsPlayed, contract);
				if (lastWinner != null) {
					return lastWinner.getOwner();
				}
			} else {
				return getNextPosition(cardsPlayed.get(cardsPlayed.size() - 1).getOwner());
			}
		}
		return BridgeConstantes.POSITION_NOT_VALID;
	}
	
	/**
	 * Return the position of the dead player
	 * @param declarer the string position of the declarer
	 * @return the string position of dead player
	 */
	public static char getPositionDeadPlayer(char declarer) {
		return getPositionPartenaire(declarer);
	}
	
	/**
	 * Return the position of the partenaire
	 * @param player
	 * @return
	 */
	public static char getPositionPartenaire(char player) {
		if (player == BridgeConstantes.POSITION_NORTH) {
			return BridgeConstantes.POSITION_SOUTH;
		}
		if (player == BridgeConstantes.POSITION_SOUTH) {
			return BridgeConstantes.POSITION_NORTH;
		}
		if (player == BridgeConstantes.POSITION_WEST) {
			return BridgeConstantes.POSITION_EAST;
		}
		if (player == BridgeConstantes.POSITION_EAST) {
			return BridgeConstantes.POSITION_WEST;
		}
		return BridgeConstantes.POSITION_NOT_VALID;
	}
	
	/**
	 * Return true if the position is in the declarer side
	 * @param position
	 * @param declarerPosition
	 * @return
	 */
	public static boolean isPositionInDeclarerSide(char position, char declarerPosition) {
		if (position == declarerPosition) {
			return true;
		}
		if (position == BridgeConstantes.POSITION_EAST && declarerPosition == BridgeConstantes.POSITION_WEST ||
			position == BridgeConstantes.POSITION_WEST && declarerPosition == BridgeConstantes.POSITION_EAST){
			return true;
		}
		if (position == BridgeConstantes.POSITION_NORTH && declarerPosition == BridgeConstantes.POSITION_SOUTH||
			position == BridgeConstantes.POSITION_SOUTH && declarerPosition == BridgeConstantes.POSITION_NORTH){
			return true;
		}
		return false;
	}

	/**
	 * Check if bids are finished
	 * The 3 or 4 last bid must be PASS 
	 * @param listPlayed
	 * @return true if bids are finished
	 */
	public static boolean isBidsFinished(List<BridgeBid> listPlayed) {
		if (listPlayed != null) {
			// bid list ends with 3 or 4 consecutive pass
			if (listPlayed.size() >= 4) {
				int size = listPlayed.size();
				if (listPlayed.get(size-1).isPass() &&
					listPlayed.get(size-2).isPass() &&
					listPlayed.get(size-3).isPass()) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Check if sequence bid is valid
	 * @param listPlayed
	 * @return
	 */
	public static boolean isBidsSequenceValid(List<BridgeBid> listPlayed) {
		if (listPlayed != null) {
			int nbPA = 0;
			BridgeBid prevBid = null;
			for (BridgeBid bid : listPlayed) {
				if (nbPA == 3 && prevBid != null) {
					// already 3 PA consecutive and not PAPAPAPA
					return false;
				}
				if (bid.isPass()) {
					nbPA++;
				} else if (bid.isX1() || bid.isX2()) {
					if (prevBid == null) {
						// no current bid valid
						return false;
					}
					nbPA = 0;
					continue;
				} else {
					if (prevBid != null) {
						if (bid.compareTo(prevBid) <= 0) {
							return false;
						}
					}
					prevBid = bid;
					nbPA = 0;
				}
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Check if bids are finished. The game parameter contains the game bids and cards with 2 char for each. (1CPAPAPAAC....)
	 * @param game
	 * @return true if game contains 3 consecutifs PA
	 */
	public static boolean isEndBids(String game) {
		if (game.length() < 8) return false;
		// the game contains PAPAPA and not start with this sequence!
		if (game.lastIndexOf("PAPAPA") > 0) return true;
		return false;
	}
	
	/**
	 * Check if the newBid bid is valid in the sequence of bids
	 * @param listPlayed list of bids played
	 * @param newBid the new bid
	 * @return true if the new bid is valid
	 */
	public static boolean isBidValid(List<BridgeBid> listPlayed, BridgeBid newBid) {
		// first bid ?
		if (listPlayed.size() == 0) {
			return true;
		}
		// check if bilds not finished
		if (isBidsFinished(listPlayed)) {
			return false;
		}
		// bid = pass --> OK!
		if (newBid.isPass()) {
			return true;
		}
		
		BridgeBid higher = getHigherBid(listPlayed);
		if (newBid.isX1()) {
            // not first bid!
            if (listPlayed.size() == 0) {
                return false;
            }
			// possible only if there is no X1
			if (getX1(listPlayed) != null) {
				return false;
			}
			// current higher is not own pair
			if (isPartenaire(newBid.getOwner(), higher.getOwner())) {
				return false;
			}
			// current higher is PASS
			if (higher.isPass()) {
				return false;
			}
			// X1 is possible !
			return true;
		}
		else if (newBid.isX2()) {
            // not first bid!
            if (listPlayed.size() == 0) {
                return false;
            }
			// possible only is there is one X1
			BridgeBid bidX1 = getX1(listPlayed);
			if (bidX1 == null) {
				return false;
			}
			// possible only if there is not already one X2
			BridgeBid bidX2 = getX2(listPlayed);
			if (bidX2 != null) {
				return false;
			}
			// current bidX1 doesn't be partenaire
			if (isPartenaire(bidX1.getOwner(), newBid.getOwner())) {
				return false;
			}
			// current higher must be partenaire
			if (isPartenaire(higher.getOwner(), newBid.getOwner())) {
				return true;
			}
			// X2 is not possible
			return false;
		}
		else {
			// all other bid must be > to the current higher bid
			if (newBid.compareTo(higher) > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the X1 bid from the list
	 * @param listPlayed list to search X1 bid
	 * @return the X1 bid or null if not found
	 */
	public static BridgeBid getX1(List<BridgeBid> listPlayed) {
		BridgeBid higherBid = getHigherBid(listPlayed);
		int idxHigher = listPlayed.indexOf(higherBid);
		if (idxHigher < (listPlayed.size() - 1)) {
			for (int i = idxHigher; i < listPlayed.size(); i++) {
				if (listPlayed.get(i).isX1()) {
					return listPlayed.get(i);
				}
			}
		}
		return null;
	}
	
	/**
	 * Get the X2 bid from the list
	 * Only one X2 bid is possible
	 * @param listPlayed list to search X2 bid
	 * @return the X2 bid or null if not found
	 */
	public static BridgeBid getX2(List<BridgeBid> listPlayed) {
		BridgeBid higherBid = getHigherBid(listPlayed);
		int idxHigher = listPlayed.indexOf(higherBid);
		if (idxHigher < (listPlayed.size() - 1)) {
			for (int i = idxHigher; i < listPlayed.size(); i++) {
				if (listPlayed.get(i).isX2()) {
					return listPlayed.get(i);
				}
			}
		}
		return null;
	}
	
	/**
	 * Test if a bid X1 is present in the list of bid
	 * @param listPlayed
	 * @return true if bid X1 is found
	 */
	public static boolean isX1(List<BridgeBid> listPlayed) {
		return getX1(listPlayed) != null;
	}
	
	/**
	 * Test if a bid X2 is present in the list of bid
	 * @param listPlayed
	 * @return true if bid X2 is found
	 */
	public static boolean isX2(List<BridgeBid> listPlayed) {
		return getX2(listPlayed) != null;
	}
	
	/**
	 * Test if the positions at pos1 and pos2 are partenaire
	 * The partenaire are NORTH - SOUTH and WEST - EAST
	 * @param pos1
	 * @param pos2
	 * @return true if the positions are partenaire
	 */
	public static boolean isPartenaire(char pos1, char pos2) {
		if ((pos1 == BridgeConstantes.POSITION_NORTH && pos2 == BridgeConstantes.POSITION_SOUTH) ||
			(pos1 == BridgeConstantes.POSITION_SOUTH && pos2 == BridgeConstantes.POSITION_NORTH) ||
			(pos1 == BridgeConstantes.POSITION_WEST && pos2 == BridgeConstantes.POSITION_EAST) ||
			(pos1 == BridgeConstantes.POSITION_EAST && pos2 == BridgeConstantes.POSITION_WEST) ||
			(pos1 == pos2))
			return true;
		return false;
	}
	
	/**
	 * Return the current higher bid
	 * @param listBids
	 * @return the higher bid or null if list is empty
	 */
	public static BridgeBid getHigherBid(List<BridgeBid> listBids) {
		BridgeBid higher = null;
		for (BridgeBid bridgeBid : listBids) {
			if (higher == null || (bridgeBid.compareTo(higher)>0)) {
				higher = bridgeBid;
			}
		}
		return higher;
	}
	
	/**
	 * Return the winner of bids
	 * Search the higher bid and search if the partenaire play the same color bid. 
	 * @param listBids list bids 
	 * @return string position of winner
	 */
	public static char getWinnerBids(List<BridgeBid> listBids) {
		if (listBids != null) {
			BridgeBid higher = getHigherBid(listBids);
			if (higher != null){
				for (BridgeBid bid : listBids) {
					if (bid.getColor() != BidColor.Other) {
						if (bid.getColor() == higher.getColor() && isPartenaire(bid.getOwner(), higher.getOwner())){
							return bid.getOwner();
						}
					}
				}
				return higher.getOwner();
			}
		}
		return BridgeConstantes.POSITION_NOT_VALID;
	}
	
	/**
	 * Check if the card is contains in the list of cards
	 * @param listCards
	 * @param card
	 * @return true if the list contains the card
	 */
	public static boolean isCardPresentInList(List<BridgeCard> listCards, BridgeCard card) {
		if (listCards != null) {
			for (BridgeCard bc : listCards) {
				if (bc.compareTo(card) == 0) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Check if the list contains the card and owner are the same
	 * @param listCards
	 * @param card
	 * @return true if card is found and owner are the same
	 */
	public static boolean isCardPlayer(List<BridgeCard> listCards, BridgeCard card) {
		for (BridgeCard bc : listCards) {
			if (bc.compareTo(card) == 0) {
				return bc.getOwner() == card.getOwner();
			}
		}
		return false;
	}
	
	/**
	 * Get the list of cards not yet played
	 * @param listDeal
	 * @param listPlayed
	 * @return
	 */
	public static List<BridgeCard> getRemainingCards(List<BridgeCard> listDeal, List<BridgeCard> listPlayed) {
		List<BridgeCard> listRemaining = new ArrayList<BridgeCard>();
		for (BridgeCard bc : listDeal) {
			if (!isCardPresentInList(listPlayed, bc)) {
				listRemaining.add(bc);
			}
		}
		return listRemaining;
	}
	
	/**
	 * Get the list of cards not yet played for a color
	 * @param listDeal
	 * @param listPlayed
	 * @param color
	 * @return
	 */
	public static List<BridgeCard> getRemainingCardsOnColor(List<BridgeCard> listDeal, List<BridgeCard> listPlayed, CardColor color) {
		List<BridgeCard> listRemaining = new ArrayList<BridgeCard>();
		for (BridgeCard bc : listDeal) {
			if (bc.getColor().equals(color) && !isCardPresentInList(listPlayed, bc)) {
				listRemaining.add(bc);
			}
		}
		return listRemaining;
	}
	
	/**
	 * Get the list of cards not yet played for a player
	 * @param listDeal
	 * @param listPlayed
	 * @return
	 */
	public static List<BridgeCard> getRemainingCardsForPlayer(List<BridgeCard> listDeal, List<BridgeCard> listPlayed, char position) {
		List<BridgeCard> listRemaining = new ArrayList<BridgeCard>();
		for (BridgeCard bc : listDeal) {
			if (!isCardPresentInList(listPlayed, bc) && bc.getOwner() == position) {
				listRemaining.add(bc);
			}
		}
		return listRemaining;
	}
	
	/**
	 * Get the list of cards not yet played for a player and a color
	 * @param listDeal
	 * @param listPlayed
	 * @param position
	 * @parma color
	 * @return
	 */
	public static List<BridgeCard> getRemainingCardsOnColorForPlayer(List<BridgeCard> listDeal, List<BridgeCard> listPlayed, char position, CardColor color) {
		List<BridgeCard> listRemaining = new ArrayList<BridgeCard>();
		if (listDeal != null) {
			for (BridgeCard bc : listDeal) {
				if (!isCardPresentInList(listPlayed, bc) && (bc.getOwner() == position) && bc.getColor().equals(color)) {
					listRemaining.add(bc);
				}
			}
		}
		return listRemaining;
	}
	
	/**
	 * Return the number of cards not yet played for a player and a color
	 * @param listDeal
	 * @param listPlayed
	 * @param position
	 * @param color
	 * @return
	 */
	public static int getRemainingNbCardsOnColorForPlayer(List<BridgeCard> listDeal, List<BridgeCard> listPlayed, char position, CardColor color) {
		List<BridgeCard> listRemaining = getRemainingCardsOnColorForPlayer(listDeal, listPlayed, position, color);
		if (listRemaining != null) {
			return listRemaining.size();
		}
		return 0;
	}
	
	/**
	 * Check if there is a card of color for the player at position
	 * @param listCards
	 * @param color
	 * @param position
	 * @return
	 */
	public static boolean isColorForPlayerInList(List<BridgeCard> listCards, CardColor color, char position) {
		for (BridgeCard bc : listCards) {
			if (bc.getColor() == color && bc.getOwner() == position){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check if the newCard bid is valid in the sequence of cards played
	 * @param listPlayed
	 * @param newCard
	 * @param listDeal
	 * @return
	 */
	public static boolean isCardValid(List<BridgeCard> listPlayed, BridgeCard newCard, List<BridgeCard> listDeal) {
		// the card could not be played two times !
		if (isCardPresentInList(listPlayed, newCard)) {
			return false;
		}
		
		// list of card remaining
		List<BridgeCard> listRemaining = getRemainingCards(listDeal, listPlayed);
		
		// is card of player !
		if (!isCardPlayer(listRemaining, newCard)) {
			return false;
		}

        // none card played => card is valid
        if (listPlayed == null || listPlayed.size() == 0) {
            return true;
        }

		// current trick
		int nbCardTrick = listPlayed.size() % 4;
		// fisrt card of the trick
		if (nbCardTrick == 0) {
			return true;
		}
		// color must be same as first card of the last trick
		BridgeCard first = listPlayed.get(listPlayed.size()-nbCardTrick);
		if (first.compareColorTo(newCard)) {
			return true;
		}
		
		// or player have no card in this color
		if (!isColorForPlayerInList(listRemaining, first.getColor(), newCard.getOwner())){
			return true;
		}
		
		return false;
	}

	/**
	 * Return the last card for a player to play the last trick 
	 * @param listPlayed
	 * @param listDeal
	 * @param playerPosition
	 * @return null if the condition of last trick are not valid
	 */
	public static BridgeCard getLastCardForPlayer(List<BridgeCard> listPlayed, List<BridgeCard> listDeal, char playerPosition) {
		if (listPlayed != null && listDeal != null) {
			if ((listPlayed.size() / 4) == 12) {
				// list of card remaining
				List<BridgeCard> listRemaining = getRemainingCardsForPlayer(listDeal, listPlayed, playerPosition);
				if (listRemaining != null && listRemaining.size() == 1) {
					return listRemaining.get(0);
				}
			}
		}
		return null;
	}
	
	/**
	 * Return the only one card for a color. It is the last card for this player on this color
	 * @param listPlayed
	 * @param listDeal
	 * @param playerPosition
	 * @param color
	 * @return null if the player has no or many cards on this color
	 */
	public static BridgeCard getOnlyOneCardForPlayerAndColor(List<BridgeCard> listPlayed, List<BridgeCard> listDeal, char playerPosition, CardColor color) {
		if (listPlayed != null && listDeal != null) {
			// list of card remaining
			List<BridgeCard> listRemaining = getRemainingCardsOnColorForPlayer(listDeal, listPlayed, playerPosition, color);
			if (listRemaining != null && listRemaining.size() == 1) {
				return listRemaining.get(0);
			}
		}
		return null;
	}
	
	/**
	 * Return the smallest card for player on a color. If color is null or player has no more card on this color, play another one !
	 * @param listPlayed
	 * @param listDeal
	 * @param playerPosition
	 * @param color
	 * @return null if player has no more card !
	 */
	public static BridgeCard getSmallestCardForPlayerAndColor(List<BridgeCard> listPlayed, List<BridgeCard> listDeal, char playerPosition, CardColor color) {
		if (listPlayed != null && listDeal != null) {
			if (color != null) {
				// list of card remaining
				List<BridgeCard> listRemainingOnColor = getRemainingCardsOnColorForPlayer(listDeal, listPlayed, playerPosition, color);
				if (listRemainingOnColor != null && listRemainingOnColor.size() >= 1) {
					return listRemainingOnColor.get(0);
				}
			}
			
			// list of card remaining
			List<BridgeCard> listRemaining = getRemainingCardsForPlayer(listDeal, listPlayed, playerPosition);
			if (listRemaining != null && listRemaining.size() >= 1) {
				return listRemaining.get(0);
			}
		}
		return null;
	}
	
	/**
	 * Return the card played by player at the trick index.
	 * @param listPlayed
	 * @param player
	 * @param trickIndex starts at 0 for first trick and 12 for the last trick
	 * @return card played or null if list and trick index not compatible
	 */
	public static BridgeCard getCardForPlayerAtTrick(List<BridgeCard> listPlayed, char player, int trickIndex) {
		if (listPlayed != null && trickIndex >=0 && trickIndex < 13) {
			int posBeginTrick = 4*trickIndex;
			if (posBeginTrick < listPlayed.size()) {
				// begin of the trick
				// only 4 positions possibles !
				int posCardPlayer = posBeginTrick;
				BridgeCard card = null;
				if (posCardPlayer < listPlayed.size()) {
					card = listPlayed.get(posCardPlayer);
					if (card.getOwner() == player) return card;
				}
				posCardPlayer++;
				if (posCardPlayer < listPlayed.size()) {
					card = listPlayed.get(posCardPlayer);
					if (card.getOwner() == player) return card;
				}
				posCardPlayer++;
				if (posCardPlayer < listPlayed.size()) {
					card = listPlayed.get(posCardPlayer);
					if (card.getOwner() == player) return card;
				}
				posCardPlayer++;
				if (posCardPlayer < listPlayed.size()) {
					card = listPlayed.get(posCardPlayer);
					if (card.getOwner() == player) return card;
				}
				// card not found !!
			}
		}
		return null;
	}
	
	/**
	 * Return the first card of the current trick (similar as last trick)
	 * @param listPlayed
	 * @return null if there is no current trick
	 */
	public static BridgeCard getFirstCardOnCurrentTrick(List<BridgeCard> listPlayed) {
		if (listPlayed != null && listPlayed.size() > 0) {
			if (!isBeginTrick(listPlayed) && !isEndTrick(listPlayed)) {
				// get the first card just after the last 4 card for the previous trick
				return listPlayed.get((listPlayed.size()/4)*4);
			}
		}
		return null;
	}
	
	/**
	 * Check if it is the end of a trick
	 * @param listPlay
	 * @return true if it is the end of trick
	 */
	public static boolean isEndTrick(List<BridgeCard> listPlay) {
		if ((listPlay != null) && (listPlay.size() > 0)) {
			return (listPlay.size()%4 == 0);
		}
		return false;
	}

	/**
	 * Check if it is the begin of a trick
	 * @param listPlay
	 * @return true if it is the begin of a trick
	 */
	public static boolean isBeginTrick(List<BridgeCard> listPlay) {
		if (listPlay != null && listPlay.size() < 52) {
			return  (listPlay.size() % 4 == 0);
		}
		return false;
	}
	/**
	 * Check if it is the end of game
	 * @param listPlay
	 * @return true if all cards has been played
	 */
	public static boolean isEndGame(List<BridgeCard> listPlay) {
		return ((listPlay != null) && (listPlay.size() == 52));
	}
	
	/**
	 * Return the card who wins the last trick.
	 * @param listPlay
	 * @param bidContract
	 * @return the card who wins the last trick
	 */
	public static BridgeCard getLastWinnerTrick(List<BridgeCard> listPlay, BridgeBid bidContract) {
		BridgeCard winCard = null;
		if(isEndTrick(listPlay) && bidContract != null) {
			BridgeCard card1 = listPlay.get(listPlay.size() - 4);
			BridgeCard card2 = listPlay.get(listPlay.size() - 3);
			BridgeCard card3 = listPlay.get(listPlay.size() - 2);
			BridgeCard card4 = listPlay.get(listPlay.size() - 1);
			winCard = getWinner(card1, card2, card3, card4, bidContract);
		}
		return winCard;
	}
	
	/**
	 * Return the list of card who wins the trick
	 * @param listPlay
	 * @param bidContract
	 * @return
	 */
	public static List<BridgeCard> getWinnersTrick(List<BridgeCard> listPlay, BridgeBid bidContract) {
		List<BridgeCard> winners = new ArrayList<BridgeCard>();
		if (listPlay.size() > 0) {
			int curIdx = 0;
			while (curIdx+4 <= listPlay.size()) {
				winners.add(getWinner(listPlay.get(curIdx), listPlay.get(curIdx+1), listPlay.get(curIdx+2), listPlay.get(curIdx+3), bidContract));
				curIdx += 4;
			}
		}
		return winners;
	}
	
	/**
	 * Return the winner between these 4 cards according to the contract
	 * @param card1
	 * @param card2
	 * @param card3
	 * @param card4
	 * @param bidContract
	 * @return
	 */
	public static BridgeCard getWinner(BridgeCard card1, BridgeCard card2, BridgeCard card3, BridgeCard card4, BridgeBid bidContract) {
		BridgeCard winCard = null;
		if (card1 == null || card2 == null || card3 == null || card4 == null || bidContract == null) {
			return null;
		}
		if (card1.compareTo(card2, bidContract.getColor(), card1.getColor()) == -1) {
			winCard = card2;
		} else {
			winCard = card1;
		}
		if (winCard.compareTo(card3, bidContract.getColor(), card1.getColor()) == -1) {
			winCard = card3;
		}
		if (winCard.compareTo(card4, bidContract.getColor(), card1.getColor()) == -1) {
			winCard = card4;
		}
		return winCard;
	}
	
	/**
	 * Return the number of trick win by the declaring team
	 * @param listPlay
	 * @param bidContract
	 * @return
	 */
	public static int getNbTrickForDeclarer(List<BridgeCard> listPlay, BridgeBid bidContract) {
		if (bidContract.isPass() || bidContract.isX1() || bidContract.isX2()) {
			return 0;
		}
		if (listPlay.size() % 4 != 0) {
			return 0;
		}
		int nbTrick = 0;
		
		for (int i = 0; i < listPlay.size(); i=i+4) {
			BridgeCard winCard = getWinner(listPlay.get(i), listPlay.get(i+1), listPlay.get(i+2), listPlay.get(i+3), bidContract);
			if (isPartenaire(winCard.getOwner(), bidContract.getOwner())){
				nbTrick++;
			}
		}
		return nbTrick;
	}
	
	
	/**
	 * Compute score of game
	 * The computing is based on http://fr.wikipedia.org/wiki/Marque_du_bridge
	 * 
	 * @param nbTrickWin - nb trick win by player and 
	 * @param bidContract - contract of the game
	 * @param contreValue
	 * @param vulnerability
	 * @return The score of the declaring team
	 */
	public static int getGameScore(int nbTrickWin, BridgeBid bidContract, int contreValue, char vulnerability) {
		int score = 0;
		int pointsMancheMultiplier = 1;
		int pointsPrimePartielleMultiplier = 1;
		boolean isVulnerable = false;
		if (vulnerability == 'L') {
			isVulnerable = false;
		} else if (vulnerability == 'N') {
			isVulnerable = isPartenaire(bidContract.getOwner(), 'N');
		} else if (vulnerability == 'E') {
			isVulnerable = isPartenaire(bidContract.getOwner(), 'E');
		} else {
			isVulnerable = true;
		}
		
		if (contreValue == 1) {
			pointsMancheMultiplier = 2;
		} else if (contreValue == 2) {
			pointsMancheMultiplier = 4;
			pointsPrimePartielleMultiplier = 2;
		}
		int nbRequiredTrick = bidContract.getRequiredNbTrick();
		
		if (nbTrickWin >= nbRequiredTrick) {
			// contract is winned
			
			// points of manche
			// only for tricks > contract
			int pointManche = 0;
			int numberContract = nbRequiredTrick - 6;
			if (bidContract.getColor().equals(BidColor.Club) || bidContract.getColor().equals(BidColor.Diamond)) {
				pointManche += (numberContract*20);
			} else if (bidContract.getColor().equals(BidColor.Heart) || bidContract.getColor().equals(BidColor.Spade)) {
				pointManche += (numberContract*30);
			} else {
				pointManche += 40 + ((numberContract-1) *30);
			}
			
			// if X1 or X2 points *2
			pointManche = pointManche * pointsMancheMultiplier;
			
			score += pointManche;
			
			// prime contrat reussi X1 ou X2
			if (contreValue == 1 || contreValue == 2) {
				score += (50*pointsPrimePartielleMultiplier);
			}
			
			// prime partielle
			if (pointManche < 100) {
				score += 50;
			}
			
			// prime manche
			if (pointManche >= 100) {
				if (isVulnerable) {
					score += 500;
				} else {
					score += 300;
				}
			}
			
			// prime chelem
			if (bidContract.getValue().getVal() == 6) {
				if (isVulnerable) {
					score += 750;
				} else {
					score += 500;
				}
			} else if (bidContract.getValue().getVal() == 7) {
				if (isVulnerable) {
					score += 1500;
				} else {
					score += 1000;
				}
			}
			
			// extra tricks
			int nbExtraTricks = nbTrickWin - nbRequiredTrick;
			int pointsExtraTricks = 0;
			if (isVulnerable) {
				if (contreValue == 0) {
					if (bidContract.getColor().equals(BidColor.Club) || bidContract.getColor().equals(BidColor.Diamond)) {
						pointsExtraTricks = nbExtraTricks * 20;
					} else {
						pointsExtraTricks = nbExtraTricks * 30;
					}
				} else if (contreValue == 1) {
					pointsExtraTricks = nbExtraTricks * 200;
				} else if (contreValue == 2) {
					pointsExtraTricks = nbExtraTricks * 400;
				}
			} else {
				if (contreValue == 0) {
					if (bidContract.getColor().equals(BidColor.Club) || bidContract.getColor().equals(BidColor.Diamond)) {
						pointsExtraTricks = nbExtraTricks * 20;
					} else {
						pointsExtraTricks = nbExtraTricks * 30;
					}
				} else if (contreValue == 1) {
					pointsExtraTricks = nbExtraTricks * 100;
				} else if (contreValue == 2) {
					pointsExtraTricks = nbExtraTricks * 200;
				}
			}
			
			score += pointsExtraTricks;
		}
		else
		{
			// contrat chute
			int nbChuteTricks = nbRequiredTrick - nbTrickWin;
			int pointsChute = 0;
			if (isVulnerable) {
				if (contreValue == 0) {
					pointsChute = nbChuteTricks * 100;
				} else if (contreValue == 1) {
					if (nbChuteTricks == 1) {
						pointsChute = 200;
					} else {
						pointsChute = 200 + 300 * (nbChuteTricks - 1);
					}
				} else if (contreValue == 2) {
					if (nbChuteTricks == 1) {
						pointsChute = 400;
					} else {
						pointsChute = 400 + 600 * (nbChuteTricks - 1);
					}
				}
			} else {
				if (contreValue == 0) {
					pointsChute = nbChuteTricks * 50;
				} else if (contreValue == 1) {
					if (nbChuteTricks == 1) {
						pointsChute = 100;
					} else if (nbChuteTricks == 2 || nbChuteTricks == 3) {
						pointsChute = 100 + 200 * (nbChuteTricks-1);
					} else if (nbChuteTricks > 3) {
						pointsChute = 100 + 200 + 200 + 300 * (nbChuteTricks - 3);
					}
				} else if (contreValue == 2) {
					if (nbChuteTricks == 1) {
						pointsChute = 200;
					} else if (nbChuteTricks == 2 || nbChuteTricks == 3) {
						pointsChute = 200 + 400 * (nbChuteTricks - 1);
					} else if (nbChuteTricks > 3) {
						pointsChute = 200 + 400 + 400 + 600 * (nbChuteTricks - 3);
					}
				}
			}
			score -= pointsChute;
		}
		
		return score;
	}
	
	/**
	 * Return the number of trick win in future by a player for a color
	 * @param game
	 * @param color
	 * @param playerPosition
	 * @return
	 */
	public static int getNbFutureTrickForPlayerOnColor(BridgeGame game, CardColor color, char playerPosition) {
		if (game != null && playerPosition != BridgeConstantes.POSITION_NOT_VALID && color != null) {
			List<BridgeCard> listCardColor = getRemainingCardsOnColor(game.distribution, game.cardPlayed, color);
			if (listCardColor.size() == 0) {return 0;}
			int idx = 0, nbTrickPlayer = 0, nbCardPlayer = 0, nbCardOther = 0;
			boolean bCountNbTrick = true;
			while (idx < listCardColor.size()) {
				BridgeCard card = listCardColor.get(listCardColor.size()-1-idx);
				if (card.getOwner() == playerPosition) {
					nbCardPlayer++;
					if (bCountNbTrick) nbTrickPlayer++;
				} else bCountNbTrick = false;
				idx++;
			}
			nbCardOther = listCardColor.size() - nbCardPlayer;
			if (nbTrickPlayer >= nbCardOther) return nbCardPlayer;
			else return Math.min(nbCardPlayer, nbTrickPlayer);
		}
		return 0;
		
	}
	
	/**
	 * Verifie si un joueur peut revendiquer la fin de partie en remportant tous les plis
	 * @param game
	 * @return la position du joueur pouvant tout remporter ou POSITION_NOT_VALID si aucun joueur peut revendiquer
	 */
	public static char claimGame(BridgeGame game) {
		if (game != null) {
			// le jeu doit etre en phase de carte
			if (!game.isPhaseCard) return BridgeConstantes.POSITION_NOT_VALID;
			
			// Le jeu doit etre en debut de pli
			if (!GameBridgeRule.isBeginTrick(game.cardPlayed)) return BridgeConstantes.POSITION_NOT_VALID;
			
			char nextPla = game.currentPlayer;
			char curPla = game.currentPlayer;
			boolean curPlaClaim = true;
			int nbFutureTrickWin = 0;
			
			// cas du contrat sans atout
			if (game.contract.getColor() == BidColor.NoTrump) {
				// test d'abord avec le joueur ayant la main 
				for (CardColor color : CardColor.values()) {
					// test si le joueur a des cartes maitresses dans la couleur
					nbFutureTrickWin = getNbFutureTrickForPlayerOnColor(game, color, curPla);
					if (nbFutureTrickWin != game.getNbCardForColor(curPla, color)) {
						// carte non maitresse
						curPlaClaim = false;
						break;
					}
				}
				if (curPlaClaim) return curPla;
				
				// test sur les autres joueurs
				curPla = GameBridgeRule.getNextPosition(curPla);
				while (curPla != game.currentPlayer) {
					curPlaClaim = true;
					for (CardColor color : CardColor.values()) {
						// test si le joueur a des cartes de la couleur
						if (game.getNbCardForColor(curPla, color) == 0) {
							// test si le joueur qui va jouer a des cartes de la cette couleur qu'on n'a pas
							if (game.getNbCardForColor(nextPla, color) != 0){
								curPlaClaim = false;
								break;
							}
						} else {
							// test si que des cartes maitresses
							nbFutureTrickWin = getNbFutureTrickForPlayerOnColor(game, color, curPla);
							if (nbFutureTrickWin != game.getNbCardForColor(curPla, color)) {
								curPlaClaim = false;
								break;
							}
						}
					}
					if (curPlaClaim) return curPla;
					
					// on prend le joueur suivant 
					curPla = GameBridgeRule.getNextPosition(curPla);
				}
			}
			// cas de l'aout
			else {
				CardColor colorContract = bidColor2CardColor(game.contract.getColor());
				// test sur le joueur ayant la main pour toutes les couleurs
				for (CardColor color : CardColor.values()) {
					// test que des cartes maitresses sur la couleur
					nbFutureTrickWin = getNbFutureTrickForPlayerOnColor(game, color, curPla);
					if (nbFutureTrickWin != game.getNbCardForColor(curPla, color)) {
						if (color == colorContract) {
							// si on a plus d'atout que le partenaire et qu'on a la main => OK sinon KO
							if (game.getNbCardForColor(curPla, color) < game.getNbCardForColor(getPositionPartenaire(curPla), color) + 1) {
								curPlaClaim = false;
								break;
							}
						} else {
							// a card is not winner
							curPlaClaim = false;
							break;
						}
					}
				}
				
				nbFutureTrickWin = getNbFutureTrickForPlayerOnColor(game, colorContract, curPla);
				int nbCardContract = game.getNbCardForColor(curPla, colorContract);
				int nbCardContractPartenaire = game.getNbCardForColor(getPositionPartenaire(curPla), colorContract);
				int nbCardContractAdverse1 = game.getNbCardForColor(getNextPosition(curPla), colorContract);
				int nbCardContractAdverse2 = game.getNbCardForColor(getPositionPartenaire(getNextPosition(curPla)), colorContract);
				// s'il reste de l'atout aux adversaires et si ils en ont encore trop !
				if ((nbCardContractAdverse1 != 0 || nbCardContractAdverse2 != 0) &&
					nbFutureTrickWin < (nbCardContractAdverse1+nbCardContractAdverse2)) {
					curPlaClaim = false;
				}
				
				// test si le partenaire a de l'atout
				if (nbCardContractPartenaire > 0) {
					// le joueur n'a pas que des atouts
					if (nbCardContract != game.getNbCardForPlayer(curPla)) {
						// si pas autant d'atout maitre que le partenaire
						if (nbFutureTrickWin < nbCardContractPartenaire) {
							if (nbCardContract < nbCardContractPartenaire) curPlaClaim = false;
						} else {
							// le partenaire a plus d'atout que nous
							if (nbCardContract < nbCardContractPartenaire) curPlaClaim = false;
						}
					}
				}
				
				if (curPlaClaim) return curPla;
				
				// test sur les autres joueurs
				curPla = GameBridgeRule.getNextPosition(curPla);
				while (curPla != game.currentPlayer) {
					curPlaClaim = true;
					nbCardContract = game.getNbCardForColor(curPla, colorContract);
					nbCardContractPartenaire = game.getNbCardForColor(getPositionPartenaire(curPla), colorContract);
					nbCardContractAdverse1 = game.getNbCardForColor(getNextPosition(curPla), colorContract);
					nbCardContractAdverse2 = game.getNbCardForColor(getPositionPartenaire(getNextPosition(curPla)), colorContract);
					// parcours de toutes les couleurs
					for (CardColor color : CardColor.values()) {
						// le joueur possède des cartes dans cette couleur
						if (game.getNbCardForColor(curPla, color) > 0) {
							// que des cartes maitresses ?
							nbFutureTrickWin = getNbFutureTrickForPlayerOnColor(game, color, curPla);
							if (nbFutureTrickWin != game.getNbCardForColor(curPla, color)) {
								if (color != colorContract || nbCardContractAdverse1 > 0 || nbCardContractAdverse2 > 0 || nbCardContractPartenaire >= nbCardContract){
									curPlaClaim = false;
									break;
								}
							}
						}
						// aucune carte dans la couleur
						else {
							// test si je joueur qui va debuter le pli possède de la couleur qu'on n'a pas ou si on a de l'atout
							if (game.getNbCardForColor(nextPla, color) != 0 && nbCardContract == 0) {
								curPlaClaim = false;
								break;
							}
						}
					}// fin boucle couleur
					
					// test si les adversaires ont de l'atout => on continuer de joueur sauf si que atouts maites
					if (nbCardContractAdverse1 != 0 || nbCardContractAdverse2 != 0) {
						// test si le joueur a que des atouts
						if (nbCardContract == game.getNbCardForPlayer(curPla)) {
							// si le plus petit des atouts du joueur = plus petit des atouts restants 
							List<BridgeCard> cardsContractPlayer = getRemainingCardsOnColorForPlayer(game.distribution, game.cardPlayed, curPla, colorContract);
							List<BridgeCard> cardsContract = getRemainingCardsOnColor(game.distribution, game.cardPlayed, colorContract);
							if (cardsContract != null && cardsContract.size() > 0 && cardsContractPlayer != null && cardsContractPlayer.size() > 0 &&
								cardsContract.size() > cardsContractPlayer.size() &&
								cardsContractPlayer.get(0).compareTo(cardsContract.get(cardsContract.size() - cardsContractPlayer.size())) == 0) {
								// les derniers atouts restant doivent être ceux du joueur
								return curPla;
							}
							curPlaClaim = false;
						} else 	curPlaClaim = false;
					} else {
						// test si partenaire a encore de l'atout
						if (nbCardContractPartenaire > 0) {
							// si il reste uniquement des atouts on revendique
							if (nbCardContract == game.getNbCardForPlayer(curPla)) curPlaClaim = true;
							else {
								nbFutureTrickWin = getNbFutureTrickForPlayerOnColor(game, colorContract, curPla);
								if (nbFutureTrickWin < (nbCardContractPartenaire + 1)) {
									if (nextPla != getPositionPartenaire(curPla)) {
										if (nbCardContract < nbCardContractPartenaire+2) curPlaClaim = false;
									} else {
										if (nbFutureTrickWin < nbCardContractPartenaire && nbCardContract < nbCardContractPartenaire+1) curPlaClaim = false;
									}
								}
							}
						}
					}
					if (curPlaClaim) return curPla;
					
					// on prend le joueur suivant 
					curPla = GameBridgeRule.getNextPosition(curPla);
				} // fin boucle joueur
			} // fin autre atout
		} // fin game null
		return BridgeConstantes.POSITION_NOT_VALID;
	}
	
	/**
	 * Convert bid color to card color
	 * @param c
	 * @return
	 */
	public static CardColor bidColor2CardColor(BidColor c) {
		switch (c) {
		case Club: return CardColor.Club;
		case Diamond: return CardColor.Diamond;
		case Heart: return CardColor.Heart;
		case Spade: return CardColor.Spade;
		default: return null;
		}
	}
	
	/**
	 * Return the number of trick remaining to play. Do not count the current trick.
	 * @param bg
	 * @return
	 */
	public static int getNbTrickRemaining(BridgeGame bg) {
		if (bg != null) {
			if (bg.cardPlayed == null) {
				return 13;
			}
			if (bg.cardPlayed.size() >= BridgeConstantes.NB_CARD_DEAL) {
				return 0;
			}
			int nbCardRemaining = BridgeConstantes.NB_CARD_DEAL - bg.cardPlayed.size();
			return nbCardRemaining / 4;
		}
		return -1;
	}
}
