package com.gotogames.common.bridge;

public enum BidColor {
	Club('C',1),Diamond('D',2),Heart('H',3),Spade('S',4),NoTrump('N',5),Other('O',0);
	private char bidC;
	private int bidVal;
	
	BidColor(char c, int v) {
		bidC = c;
		bidVal = v;
	}
	
	public char getChar() {
		return bidC;
	}
	
	public int getVal() {
		return bidVal;
	}
	
//	public String toString() {
//		return bidC;
//	}
}
