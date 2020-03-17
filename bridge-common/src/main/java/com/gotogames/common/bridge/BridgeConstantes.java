package com.gotogames.common.bridge;

public class BridgeConstantes {
	
	// bridge card : 52 cards
	public static final String[] TAB_CARD = new String[] {
		"2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "TC", "JC", "QC", "KC", "AC",
		"2D", "3D", "4D", "5D", "6D", "7D", "8D", "9D", "TD", "JD", "QD", "KD", "AD",
		"2H", "3H", "4H", "5H", "6H", "7H", "8H", "9H", "TH", "JH", "QH", "KH", "AH",
		"2S", "3S", "4S", "5S", "6S", "7S", "8S", "9S", "TS", "JS",	"QS", "KS", "AS"};
	
	// bridge bid : 35 + 3 => 38 bids
	public static final String[] TAB_BID = new String[] {
		"1C", "2C", "3C", "4C", "5C", "6C", "7C",
		"1D", "2D", "3D", "4D",	"5D", "6D", "7D",
		"1H", "2H",	"3H", "4H", "5H", "6H", "7H",
		"1S", "2S", "3S", "4S", "5S", "6S", "7S",
		"1N", "2N", "3N", "4N", "5N", "6N", "7N",
		"PA", "X1", "X2"};
	
	public static final int NB_CARD_DEAL = 52;
	public static final int NB_CARD_PLAYER = 13;
	
	public static final String BID_PASS = "PA";
	public static final String BID_X1 = "X1";
	public static final String BID_X2 = "X2";
	public static final char POSITION_SOUTH = 'S';
	public static final char POSITION_NORTH = 'N';
	public static final char POSITION_WEST = 'W';
	public static final char POSITION_EAST = 'E';
	public static final char POSITION_NOT_VALID = '?';
	public static final char CARD_COLOR_CLUB = 'C';
	public static final char CARD_COLOR_DIAMOND = 'D';
	public static final char CARD_COLOR_HEART = 'H';
	public static final char CARD_COLOR_SPADE = 'S';
}
