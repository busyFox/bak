package com.gotogames.common.bridge;

public class BridgeDealParam {
	
	public static int getIndexForPlayer(char position) {
		switch (position) {
		case 'S':
			return INDEX_S;
		case 'N':
			return INDEX_N;
		case 'W':
			return INDEX_W;
		case 'E':
			return INDEX_E;
		}
		return -1;
	}
	
	public static final int INDEX_S=0;
	public static final int INDEX_N=1;
	public static final int INDEX_E=2;
	public static final int INDEX_W=3;
	public static final int INDEX_NS=4;
	public static final int INDEX_EW=5;
	public int index;
	public int[] ptsHonMin = new int[6];
	public int[] ptsHonMax = new int[6];
	public int[] nbCardCMin = new int[6];
	public int[] nbCardCMax = new int[6];
	public int[] nbCardDMin = new int[6];
	public int[] nbCardDMax = new int[6];
	public int[] nbCardHMin = new int[6];
	public int[] nbCardHMax = new int[6];
	public int[] nbCardSMin = new int[6];
	public int[] nbCardSMax = new int[6];
	public String paramName = "Unknown";
	public String dealer = null;
	
	/**
	 * Chechk if all tab param are not null
	 * @return
	 */
	public boolean isValid() {
		return (ptsHonMin != null && ptsHonMax != null && nbCardCMin != null && nbCardCMax != null &&
				nbCardDMin != null && nbCardDMax != null && nbCardHMin != null && nbCardHMax != null &&
				nbCardSMin != null && nbCardSMax != null);
	}
	
	public int getNbCardMin(char color, char position) {
		int value = -1;
		int idxPosition = getIndexForPlayer(position);
		if (idxPosition >= 0) {
			switch (color) {
			case BridgeConstantes.CARD_COLOR_CLUB: {value=nbCardCMin[idxPosition];break;}
			case BridgeConstantes.CARD_COLOR_DIAMOND: {value=nbCardDMin[idxPosition];break;}
			case BridgeConstantes.CARD_COLOR_HEART: {value=nbCardHMin[idxPosition];break;}
			case BridgeConstantes.CARD_COLOR_SPADE: {value=nbCardSMin[idxPosition];break;}
			default:
			}
		}
		return value;
	}
	
	public int getNbCardMax(char color, char position) {
		int value = -1;
		int idxPosition = getIndexForPlayer(position);
		if (idxPosition >= 0) {
			switch (color) {
			case BridgeConstantes.CARD_COLOR_CLUB: {value=nbCardCMax[idxPosition];break;}
			case BridgeConstantes.CARD_COLOR_DIAMOND: {value=nbCardDMax[idxPosition];break;}
			case BridgeConstantes.CARD_COLOR_HEART: {value=nbCardHMax[idxPosition];break;}
			case BridgeConstantes.CARD_COLOR_SPADE: {value=nbCardSMax[idxPosition];break;}
			default:
			}
		}
		return value;
	}
	
	public String toString() {
		String club="", diamond="", heart="", spade="",points="";
		for (int i=0; i < 6; i++) {
			if (club.length() > 0) {club += ",";}
			club+=nbCardCMin[i]+"-"+nbCardCMax[i];
			if (diamond.length() > 0) {diamond += ",";}
			diamond+=nbCardDMin[i]+"-"+nbCardDMax[i];
			if (heart.length() > 0) {heart += ",";}
			heart+=nbCardHMin[i]+"-"+nbCardHMax[i];
			if (spade.length() > 0) {spade += ",";}
			spade+=nbCardSMin[i]+"-"+nbCardSMax[i];
			if (points.length() > 0) {points += ",";}
			points+=ptsHonMin[i]+"-"+ptsHonMax[i];
		}
		return "index="+index+" - dealer="+dealer+" - paramName="+paramName+" - C=["+club+"]"+" - D=["+diamond+"]"+" - H=["+heart+"]"+" - S=["+spade+"]"+" - pts=["+points+"]";
	}
}
