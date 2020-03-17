package com.gotogames.common.bridge;

public enum CardValue {
	C2('2',2),C3('3',3),C4('4',4),C5('5',5),C6('6',6),C7('7',7),C8('8',8),C9('9',9),CT('T',10),
	CJ('J',11),CQ('Q',12),CK('K',13),CA('A',14);
	
	private char valC;
	private int val;
	
	CardValue(char s, int val) {
		this.valC = s;
		this.val = val;
	}
	
	public char getChar() {
		return valC;
	}
	
	public int getVal() {
		return val;
	}
	
//	public String toString() {
//		return str;
//	}
}
