package com.gotogames.common.bridge;

//import com.gotogames.common.tools.RandomTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BridgeDeal {
	private static final char[] player = new char[] {'S','W','N','E'};
	private static final char[] vulnerability = new char[] {'L','N','E','A'};
	
	private static final int DEAL_INDEX_MAX = 100;
//	public static final char CARD_COLOR_CLUB = 'C';
//	public static final char CARD_COLOR_DIAMOND = 'D';
//	public static final char CARD_COLOR_HEART = 'H';
//	public static final char CARD_COLOR_SPADE = 'S';
//	public static final char PLAYER_POSITION_NORTH = 'N';
//	public static final char PLAYER_POSITION_SOUTH = 'S';
//	public static final char PLAYER_POSITION_EAST = 'E';
//	public static final char PLAYER_POSITION_WEST = 'W';
	
	/**
	 * Generate a random deal. The deal return is a string of 54 characters. Dealer, Vulnerability and position of the 52 cards
	 * @return
	 */
	public static String generateRandomDeal() {
		Random random = new Random(System.nanoTime());
		StringBuffer sb = new StringBuffer();
		sb.append(player[random.nextInt(player.length)]);
		sb.append(vulnerability[random.nextInt(vulnerability.length)]);
		int[] countCardPlayer = new int[player.length];
		for (int i = 0; i < countCardPlayer.length; i++) {
			countCardPlayer[i] = 0;
		}
		for (int i = 0; i < BridgeConstantes.NB_CARD_DEAL; i++) {
			int randomPlayer = random.nextInt(player.length);
			while (countCardPlayer[randomPlayer] > (BridgeConstantes.NB_CARD_PLAYER-1)) {
				randomPlayer = random.nextInt(player.length);
			}
			countCardPlayer[randomPlayer] = countCardPlayer[randomPlayer] + 1; 
			sb.append(player[randomPlayer]);
		}
		return sb.toString();
	}
	
//	/**
//	 * Return a random distrib of 52 cards
//	 * @return
//	 */
//	private static String getRandomDistrib() {
//		StringBuffer sb = new StringBuffer();
//		Random random = new Random(System.currentTimeMillis());
////		Random random = new Random(System.nanoTime());
//		int[] countCardPlayer = new int[player.length];
//		for (int i = 0; i < countCardPlayer.length; i++) {
//			countCardPlayer[i] = 0;
//		}
//		for (int i = 0; i < BridgeConstantes.NB_CARD_DEAL; i++) {
//			int randomPlayer = random.nextInt(player.length);
//			while (countCardPlayer[randomPlayer] > (BridgeConstantes.NB_CARD_PLAYER-1)) {
//				randomPlayer = random.nextInt(player.length);
//			}
//			countCardPlayer[randomPlayer] = countCardPlayer[randomPlayer] + 1; 
//			sb.append(player[randomPlayer]);
//		}
//		return sb.toString();
//	}
	
	/**
	 * Check distribution according to the param
	 * @param distrib
	 * @param param
	 * @return
	 */
	public static boolean checkDistribution(String distrib, BridgeDealParam param) {
		int valNS = 0, valEW;
		// points honnor 
		for (int i = 0; i < player.length; i++) {
			int val = getNbPointsHonForPlayer(distrib, player[i]);
			int idxPlayerParam = BridgeDealParam.getIndexForPlayer(player[i]);
			if ((val < param.ptsHonMin[idxPlayerParam]) ||
				(val > param.ptsHonMax[idxPlayerParam])){
				return false;
			}
		}
		valNS = getNbPointsHonForPlayer(distrib, BridgeConstantes.POSITION_NORTH) + getNbPointsHonForPlayer(distrib, BridgeConstantes.POSITION_SOUTH);
		if ((valNS < param.ptsHonMin[BridgeDealParam.INDEX_NS]) ||
			(valNS > param.ptsHonMax[BridgeDealParam.INDEX_NS])) {
			return false;
		}
		valEW = getNbPointsHonForPlayer(distrib, BridgeConstantes.POSITION_EAST) + getNbPointsHonForPlayer(distrib, BridgeConstantes.POSITION_WEST);
		if ((valEW < param.ptsHonMin[BridgeDealParam.INDEX_EW]) ||
			(valEW > param.ptsHonMax[BridgeDealParam.INDEX_EW])) {
			return false;
		}
		// NbCard Club 
		for (int i = 0; i < player.length; i++) {
			int val = getNbCardColorForPlayer(distrib, player[i], BridgeConstantes.CARD_COLOR_CLUB);
			int idxPlayerParam = BridgeDealParam.getIndexForPlayer(player[i]);
			if ((val < param.nbCardCMin[idxPlayerParam]) ||
				(val > param.nbCardCMax[idxPlayerParam])){
				return false;
			}
		}
		valNS = getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_NORTH, BridgeConstantes.CARD_COLOR_CLUB) + getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_SOUTH, BridgeConstantes.CARD_COLOR_CLUB);
		if ((valNS < param.nbCardCMin[BridgeDealParam.INDEX_NS]) ||
			(valNS > param.nbCardCMax[BridgeDealParam.INDEX_NS])) {
			return false;
		}
		valEW = getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_EAST, BridgeConstantes.CARD_COLOR_CLUB) + getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_WEST, BridgeConstantes.CARD_COLOR_CLUB);
		if ((valEW < param.nbCardCMin[BridgeDealParam.INDEX_EW]) ||
			(valEW > param.nbCardCMax[BridgeDealParam.INDEX_EW])) {
			return false;
		}
		// NbCard Diamond 
		for (int i = 0; i < player.length; i++) {
			int val = getNbCardColorForPlayer(distrib, player[i], BridgeConstantes.CARD_COLOR_DIAMOND);
			int idxPlayerParam = BridgeDealParam.getIndexForPlayer(player[i]);
			if ((val < param.nbCardDMin[idxPlayerParam]) ||
				(val > param.nbCardDMax[idxPlayerParam])){
				return false;
			}
		}
		valNS = getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_NORTH, BridgeConstantes.CARD_COLOR_DIAMOND) + getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_SOUTH, BridgeConstantes.CARD_COLOR_DIAMOND);
		if ((valNS < param.nbCardDMin[BridgeDealParam.INDEX_NS]) ||
			(valNS > param.nbCardDMax[BridgeDealParam.INDEX_NS])) {
			return false;
		}
		valEW = getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_EAST, BridgeConstantes.CARD_COLOR_DIAMOND) + getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_WEST, BridgeConstantes.CARD_COLOR_DIAMOND);
		if ((valEW < param.nbCardDMin[BridgeDealParam.INDEX_EW]) ||
			(valEW > param.nbCardDMax[BridgeDealParam.INDEX_EW])) {
			return false;
		}
		// NbCard Heart 
		for (int i = 0; i < player.length; i++) {
			int val = getNbCardColorForPlayer(distrib, player[i], BridgeConstantes.CARD_COLOR_HEART);
			int idxPlayerParam = BridgeDealParam.getIndexForPlayer(player[i]);
			if ((val < param.nbCardHMin[idxPlayerParam]) ||
				(val > param.nbCardHMax[idxPlayerParam])){
				return false;
			}
		}
		valNS = getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_NORTH, BridgeConstantes.CARD_COLOR_HEART) + getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_SOUTH, BridgeConstantes.CARD_COLOR_HEART);
		if ((valNS < param.nbCardHMin[BridgeDealParam.INDEX_NS]) ||
			(valNS > param.nbCardHMax[BridgeDealParam.INDEX_NS])) {
			return false;
		}
		valEW = getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_EAST, BridgeConstantes.CARD_COLOR_HEART) + getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_WEST, BridgeConstantes.CARD_COLOR_HEART);
		if ((valEW < param.nbCardHMin[BridgeDealParam.INDEX_EW]) ||
			(valEW > param.nbCardHMax[BridgeDealParam.INDEX_EW])) {
			return false;
		}
		// NbCard Spade 
		for (int i = 0; i < player.length; i++) {
			int val = getNbCardColorForPlayer(distrib, player[i], BridgeConstantes.CARD_COLOR_SPADE);
			int idxPlayerParam = BridgeDealParam.getIndexForPlayer(player[i]);
			if ((val < param.nbCardSMin[idxPlayerParam]) ||
				(val > param.nbCardSMax[idxPlayerParam])){
				return false;
			}
		}
		valNS = getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_NORTH, BridgeConstantes.CARD_COLOR_SPADE) + getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_SOUTH, BridgeConstantes.CARD_COLOR_SPADE);
		if ((valNS < param.nbCardSMin[BridgeDealParam.INDEX_NS]) ||
			(valNS > param.nbCardSMax[BridgeDealParam.INDEX_NS])) {
			return false;
		}
		valEW = getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_EAST, BridgeConstantes.CARD_COLOR_SPADE) + getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_WEST, BridgeConstantes.CARD_COLOR_SPADE);
		if ((valEW < param.nbCardSMin[BridgeDealParam.INDEX_EW]) ||
			(valEW > param.nbCardSMax[BridgeDealParam.INDEX_EW])) {
			return false;
		}
		return true;
	}
	
	/**
	 * Return string of 52 cards in order : Club, Diamond, Heart, Spade. 13 cards for each colour and position of each cards.
	 * @return
	 */
	private static String getNewRandomDistrib() {
		char[] randomDistrib = new char[BridgeConstantes.NB_CARD_DEAL];
		int[] randomCards = new int[BridgeConstantes.NB_CARD_DEAL];
		Random random = new Random(System.nanoTime());
		// build list remaining cards
		List<Integer> listCards = new ArrayList<Integer>();
		for (int i = 0; i < BridgeConstantes.NB_CARD_DEAL; i++) {
			listCards.add(i);
		}
		// mix cards
		for (int i = 0; i < BridgeConstantes.NB_CARD_DEAL; i++) {
			// get random card from remaining cards
			int idxCard = random.nextInt(listCards.size());
			// add it to the random
			randomCards[i] = listCards.get(idxCard);
			// remove if from remaining cards
			listCards.remove(idxCard);
		}
		
		// distribute the cards : S, W, N, E, S ....
		for (int i = 0; i < BridgeConstantes.NB_CARD_DEAL; i++) {
			char pla;
			switch (i%4) {
			case 0 : pla='S';break;
			case 1 : pla='W';break;
			case 2 : pla='N';break;
			default : pla='E';break;
			}
			// set player for the card (in random order)
			randomDistrib[randomCards[i]] = pla; 
		}
		
		return new String(randomDistrib);
	}
	
//	private static String getNewRandomDistrib(BridgeDealParam param) {
//		if (param != null) {
//			char[] randomDistrib = new char[BridgeConstantes.NB_CARD_DEAL];
//			// CLUB
//			int nbClubN = -1, nbClubS = -1, nbClubW = -1, nbClubE = -1;
//			int tempMin = -1, tempMax = -1;
//			int nbClub = 13;
//			tempMin = param.getNbCardMin(BridgeConstantes.CARD_COLOR_CLUB, BridgeConstantes.POSITION_NORTH);
//			tempMax = param.getNbCardMax(BridgeConstantes.CARD_COLOR_CLUB, BridgeConstantes.POSITION_NORTH);
//			if (tempMin > 3 || tempMax < 3) {
//				nbClubN = RandomTools.getRandomInt(tempMin, tempMax<nbClub?tempMax:nbClub);
//				nbClub -= nbClubN;
//			}
//			tempMin = param.getNbCardMin(BridgeConstantes.CARD_COLOR_CLUB, BridgeConstantes.POSITION_EAST);
//			tempMax = param.getNbCardMax(BridgeConstantes.CARD_COLOR_CLUB, BridgeConstantes.POSITION_EAST);
//			if (tempMin > 3 || tempMax < 3) {
//				nbClubE = RandomTools.getRandomInt(tempMin, tempMax<nbClub?tempMax:nbClub);
//				nbClub -= nbClubE;
//			}
//			tempMin = param.getNbCardMin(BridgeConstantes.CARD_COLOR_CLUB, BridgeConstantes.POSITION_WEST);
//			tempMax = param.getNbCardMax(BridgeConstantes.CARD_COLOR_CLUB, BridgeConstantes.POSITION_WEST);
//			if (tempMin > 3 || tempMax < 3) {
//				nbClubW = RandomTools.getRandomInt(tempMin, tempMax<nbClub?tempMax:nbClub);
//				nbClub -= nbClubW;
//			}
//			tempMin = param.getNbCardMin(BridgeConstantes.CARD_COLOR_CLUB, BridgeConstantes.POSITION_SOUTH);
//			tempMax = param.getNbCardMax(BridgeConstantes.CARD_COLOR_CLUB, BridgeConstantes.POSITION_SOUTH);
//			if (tempMin > 3 || tempMax < 3) {
//				nbClubS = RandomTools.getRandomInt(tempMin, tempMax<nbClub?tempMax:nbClub);
//				nbClub -= nbClubS;
//			}
//
//			// DIAMOND
//
//			// HEART
//
//			// SPADE
//
//			// Now try to generate !
//
//			return new String(randomDistrib);
//		}
//		return null;
//	}
	
	/**
	 * return the vulnerability corresponding to dealIndex
	 * @param dealIndex must be <= 100 else use %100
	 * @return
	 */
	public static char getVulnerability(int dealIndex) {
		boolean bFound = false;
		boolean bNextVal = false;
		// Vul = L
		int temp = 1;
		if (dealIndex > 100) {
			dealIndex = dealIndex % 100;
		}
		while (!bFound && !bNextVal) {
			if (temp == dealIndex) {
				bFound = true;
			} else if (temp > DEAL_INDEX_MAX){
				bNextVal = true;
			} else {
				if (temp%4 == 1) {
					temp = temp + 7;
				} else {
					temp = temp + 3;
				}
			}
		}
		if (bFound) {
			return 'L';
		}
		bNextVal = false;
		// Vul = N
		temp = 2;
		while (!bFound && !bNextVal) {
			if (temp == dealIndex) {
				bFound = true;
			} else if (temp > DEAL_INDEX_MAX){
				bNextVal = true;
			} else {
				if (temp%4 == 1) {
					temp = temp + 7;
				} else {
					temp = temp + 3;
				}
			}
		}
		if (bFound) {
			return 'N';
		}
		bNextVal = false;
		// Vul = E
		temp = 3;
		while (!bFound && !bNextVal) {
			if (temp == dealIndex) {
				bFound = true;
			} else if (temp > DEAL_INDEX_MAX){
				bNextVal = true;
			} else {
				if (temp%4 == 1) {
					temp = temp + 7;
				} else {
					temp = temp + 3;
				}
			}
		}
		if (bFound) {
			return 'E';
		}
		bNextVal = false;
		// Vul = A
		temp = 4;
		while (!bFound && !bNextVal) {
			if (temp == dealIndex) {
				bFound = true;
			} else if (temp > DEAL_INDEX_MAX){
				bNextVal = true;
			} else {
				if (temp%4 == 1) {
					temp = temp + 7;
				} else {
					temp = temp + 3;
				}
			}
		}
		if (bFound) {
			return 'A';
		}
		
		// by default return nobody !
		return 'L';
	}

    public static char getDealer(int index) {
        switch (index%4) {
            case 1 : return 'N';
            case 2: return 'E';
            case 3: return 'S';
            case 0:
            default: return 'W';
        }
    }

	/**
	 * Generate a random deal according to the deal param
	 * @param param
	 * @return
	 */
	public static String generateDeal(BridgeDealParam param) {
		if (param != null) {
			StringBuffer sb = new StringBuffer();
			// DEALER
			String dealer = null;
			if (param.dealer != null && param.dealer.length() > 0) {
				if (param.dealer.equalsIgnoreCase("nord") || param.dealer.equalsIgnoreCase("n")) {dealer="N";}
				else if (param.dealer.equalsIgnoreCase("sud") || param.dealer.equalsIgnoreCase("s")) {dealer="S";}
				else if (param.dealer.equalsIgnoreCase("ouest") || param.dealer.equalsIgnoreCase("west") || param.dealer.equalsIgnoreCase("w")) {dealer="W";}
				else if (param.dealer.equalsIgnoreCase("est") || param.dealer.equalsIgnoreCase("east") || param.dealer.equalsIgnoreCase("e")) {dealer="E";}
			}
			if (dealer == null){
                dealer = ""+getDealer(param.index);
//				switch (param.index%4) {
//				case 1 : dealer = "N";break;
//				case 2: dealer = "E";break;
//				case 3: dealer = "S";break;
//				case 0:
//				default: dealer = "W";break;
//				}
			}
			sb.append(dealer);
			// VULNERABILITY
			sb.append(getVulnerability(param.index));
			
			// DISTRIB
			String distrib = "";
			// CHECK DISTRIB AND PARAM
			boolean bCheckOK = false;
			while (!bCheckOK) {
//				distrib = getRandomDistrib();
				distrib = getNewRandomDistrib();
				bCheckOK = checkDistribution(distrib, param);
//				bCheckOK = true;
//				// points honnor 
//				for (int i = 0; i < player.length; i++) {
//					int val = getNbPointsHonForPlayer(distrib, player[i]);
//					int idxPlayerParam = BridgeDealParam.getIndexForPlayer(player[i]);
//					if ((val < param.ptsHonMin[idxPlayerParam]) ||
//						(val > param.ptsHonMax[idxPlayerParam])){
//						bCheckOK = false;
//						break;
//					}
//				}
//				if (bCheckOK) {
//					// NbCard Club 
//					for (int i = 0; i < player.length; i++) {
//						int val = getNbCardColorForPlayer(distrib, player[i], BridgeConstantes.CARD_COLOR_CLUB);
//						int idxPlayerParam = BridgeDealParam.getIndexForPlayer(player[i]);
//						if ((val < param.nbCardCMin[idxPlayerParam]) ||
//							(val > param.nbCardCMax[idxPlayerParam])){
//							bCheckOK = false;
//							break;
//						}
//					}
//				}
//				if (bCheckOK) {
//					// NbCard Diamond 
//					for (int i = 0; i < player.length; i++) {
//						int val = getNbCardColorForPlayer(distrib, player[i], BridgeConstantes.CARD_COLOR_DIAMOND);
//						int idxPlayerParam = BridgeDealParam.getIndexForPlayer(player[i]);
//						if ((val < param.nbCardDMin[idxPlayerParam]) ||
//							(val > param.nbCardDMax[idxPlayerParam])){
//							bCheckOK = false;
//							break;
//						}
//					}
//				}
//				if (bCheckOK) {
//					// NbCard Heart 
//					for (int i = 0; i < player.length; i++) {
//						int val = getNbCardColorForPlayer(distrib, player[i], BridgeConstantes.CARD_COLOR_HEART);
//						int idxPlayerParam = BridgeDealParam.getIndexForPlayer(player[i]);
//						if ((val < param.nbCardHMin[idxPlayerParam]) ||
//							(val > param.nbCardHMax[idxPlayerParam])){
//							bCheckOK = false;
//							break;
//						}
//					}
//				}
//				if (bCheckOK) {
//					// NbCard Spade 
//					for (int i = 0; i < player.length; i++) {
//						int val = getNbCardColorForPlayer(distrib, player[i], BridgeConstantes.CARD_COLOR_SPADE);
//						int idxPlayerParam = BridgeDealParam.getIndexForPlayer(player[i]);
//						if ((val < param.nbCardSMin[idxPlayerParam]) ||
//							(val > param.nbCardSMax[idxPlayerParam])){
//							bCheckOK = false;
//							break;
//						}
//					}
//				}
			}
			sb.append(distrib);
			return sb.toString();
		}
		return null;
	}
	
	/**
	 * Check is the deal is valid. Must have 54 characters.
	 * @param deal
	 * @return true if the deal is valid for bridge game
	 */
	public static boolean isDealValid(String deal) {
		if (deal != null) {
			if (deal.length()==54) {
				if (deal.matches("[SWNE][LNEA][SWNE]*")) {
					String distrib = deal.substring(2);
					int nbS = distrib.replaceAll("[^S]", "").length();
					int nbN = distrib.replaceAll("[^N]", "").length();
					int nbE = distrib.replaceAll("[^E]", "").length();
					int nbW = distrib.replaceAll("[^W]", "").length();
					return (nbS==13 && nbN==13 && nbE==13 && nbW==13);
				}
			}
		}
		return false;
	}
	
	/**
	 * Return the number of card of color for player position
	 * @param distrib
	 * @param player
	 * @param color
	 * @return
	 */
	public static int getNbCardColorForPlayer(String distrib, char player, char color) {
		if (distrib.length() == 52) {
			int begin = -1, end = -1;
			switch (color) {
			case BridgeConstantes.CARD_COLOR_CLUB :
				begin = 0; end=12;
				break;
			case BridgeConstantes.CARD_COLOR_DIAMOND :
				begin = 13; end=25;
				break;
			case BridgeConstantes.CARD_COLOR_HEART :
				begin = 26; end=38;
				break;
			case BridgeConstantes.CARD_COLOR_SPADE:
				begin = 39; end=51;
				break;
			}
			int nbCard = 0;
			char[] tabDistrib = distrib.toCharArray();
			for (int i = begin; i <= end; i++) {
				if (tabDistrib[i] == player) {
					nbCard++;
				}
			}
			return nbCard;
		}
		return -1;
	}
	
	/**
	 * Return the number of honnour point for this player on this distribution
	 * @param distrib
	 * @param player
	 * @return
	 */
	public static int getNbPointsHonForPlayer(String distrib, char player) {
		if (distrib.length() == 52) {
			int nbPointsHon = 0;
			char[] tabDistrib = distrib.toCharArray();
			for (int i = 0; i < tabDistrib.length; i++) {
				if (tabDistrib[i] == player) {
					int temp = i - ((i / 13)*13);
					switch (temp) {
					case 9: // JACKET
						nbPointsHon += 1;
						break;
					case 10: // QUEEN
						nbPointsHon += 2;
						break;
					case 11: // KING
						nbPointsHon += 3;
						break;
					case 12: // AS
						nbPointsHon += 4;
						break;
					}
				}
			}
			return nbPointsHon;
		}
		return -1;
	}
}
