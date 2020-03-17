package com.gotogames.common.bridge;

public enum BidValue {
	V1('1',1),V2('2',2),V3('3',3),V4('4',4),V5('5',5),V6('6',6),V7('7',7),PASS('P',0),X1('X',1),X2('Y',2);
	
	private char valC;
	private int val;
	
	BidValue(char s, int val) {
		this.valC = s;
		this.val = val;
	}
	
	public char getChar() {
		return valC;
	}
	
	public int getVal() {
		return val;
	}
}
