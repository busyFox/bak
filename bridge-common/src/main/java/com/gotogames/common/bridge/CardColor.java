package com.gotogames.common.bridge;

public enum CardColor {
	Club('C',1),Diamond('D',2),Heart('H',3),Spade('S',4);
	private char cardC;
	private int cardVal;
	
	CardColor(char s, int v) {
		cardC = s;
		cardVal = v;
	}
	
	public char getChar() {
		return cardC;
	}
	
	public int getVal() {
		return cardVal;
	}
	
//	public String toString() {
//		return str;
//	}
}
