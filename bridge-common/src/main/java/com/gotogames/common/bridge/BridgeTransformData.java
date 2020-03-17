package com.gotogames.common.bridge;

/**
 * Class used to convert string bridge data to char tree data
 * 1C=# - 2C=$ - 3C=% - 4C=& - 5C=' - 6C=( - 7C=) - 1D=* - 2D=+ - 3D=, - 4D=- - 5D=. - 6D=/ - 7D=0 - 1H=1 - 2H=2 - 3H=3 - 4H=4 - 5H=5 - 6H=6 - 7H=7 - 1S=8 - 2S=9 - 3S=: - 4S=; - 5S=< - 6S== - 7S=> - 1N=? - 2N=@ - 3N=A - 4N=B - 5N=C - 6N=D - 7N=E - PA=F - X1=G - X2=H
 * 2C=I - 3C=J - 4C=K - 5C=L - 6C=M - 7C=N - 8C=O - 9C=P - TC=Q - JC=R - QC=S - KC=T - AC=U - 2D=V - 3D=W - 4D=X - 5D=Y - 6D=Z - 7D=[ - 8D=\ - 9D=] - TD=^ - JD=_ - QD=` - KD=a - AD=b - 2H=c - 3H=d - 4H=e - 5H=f - 6H=g - 7H=h - 8H=i - 9H=j - TH=k - JH=l - QH=m - KH=n - AH=o - 2S=p - 3S=q - 4S=r - 5S=s - 6S=t - 7S=u - 8S=v - 9S=w - TS=x - JS=y - QS=z - KS={ - AS=|
 * @author pascal
 *
 */
public class BridgeTransformData {
	private static final char bidStart = '#';
	private static final char cardStart = (char)(bidStart+BridgeConstantes.TAB_BID.length);
	
	/**
	 * Transform a bridge card to string of 2 char : card + position
	 * @param card
	 * @return
	 */
	public static String convertBridgeCard(BridgeCard card) {
		if (card != null) {
			return Character.toString(convertBridgeCard2Char(card.getString())) + Character.toString(card.getOwner());
		}
		return "";
	}
	
	/**
	 * Transform a bridge bid to string of 2 char : bid + position
	 * @param card
	 * @return
	 */
	public static String convertBridgeBid(BridgeBid bid) {
		if (bid != null) {
			return Character.toString(convertBridgeBid2Char(bid.getString())) + Character.toString(bid.getOwner());
		}
		return "";
	}
	
	/**
	 * Convert card to char value
	 * @param card
	 * @return
	 */
	public static char convertBridgeCard2Char(String card) {
		for (int i = 0; i < BridgeConstantes.TAB_CARD.length; i++) {
			if (BridgeConstantes.TAB_CARD[i].equals(card)) {
				return (char)(cardStart+i);
			}
		}
		return cardStart;
	}
	
	/**
	 * Convert char value of card to string value
	 * @param card
	 * @return
	 */
	public static String convertBridgeCard2String(char card) {
		int idx = (int)(card) - (int)cardStart;
		if (idx >= 0 && idx < BridgeConstantes.TAB_CARD.length) {
			return BridgeConstantes.TAB_CARD[idx];
		}
		return null;
	}
	
	/**
	 * Convert bid to char value
	 * @param bid
	 * @return
	 */
	public static char convertBridgeBid2Char(String bid) {
		for (int i = 0; i < BridgeConstantes.TAB_BID.length; i++) {
			if (BridgeConstantes.TAB_BID[i].equals(bid)) {
				return (char)(bidStart+i);
			}
		}
		return bidStart;
	}
	
	/**
	 * Convert char value of bid to string value
	 * @param bid
	 * @return
	 */
	public static String convertBridgeBid2String(char bid) {
		int idx = (int)(bid) - (int)bidStart;
		if (idx >= 0 && idx < BridgeConstantes.TAB_BID.length) {
			return BridgeConstantes.TAB_BID[idx];
		}
		return null;
	}
	
	/**
	 * Transform the game with card and bid in string format to char format
	 * @param game
	 * @return
	 */
	public static String transformGame(String game) {
		StringBuffer sb = new StringBuffer();
		int idx = 0;
		boolean isEndBid = false;
		String curGame = "";
		while (idx < game.length()) {
			String temp = game.substring(idx, idx+2);
			curGame += temp;
			if (isEndBid) {
				sb.append(convertBridgeCard2Char(temp));
			} else {
				sb.append(convertBridgeBid2Char(temp));
			}
			if (GameBridgeRule.isEndBids(curGame)) {
				isEndBid = true;
			}
			idx = idx + 2;
		}
		return sb.toString();
	}
	
	public static void main(String[] args) {
		String bid2Char = "";
		for (String bid : BridgeConstantes.TAB_BID) {
			if (bid2Char.length() != 0) {
				bid2Char += " - ";
			}
			bid2Char += bid+"="+convertBridgeBid2Char(bid);
		}
		System.out.println(bid2Char);
		String card2Char = "";
		for (String card : BridgeConstantes.TAB_CARD) {
			if (card2Char.length() != 0) {
				card2Char += " - ";
			}
			card2Char += card+"="+convertBridgeCard2Char(card);
		}
		System.out.println(card2Char);
		for (int i = 0; i < 255; i++) {
			System.out.println(i+"="+Character.toString((char)(i)));
		}
	}
}
