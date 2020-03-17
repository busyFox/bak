package com.gotogames.common.bridge;


public class BridgeCard implements Comparable<BridgeCard>{
	private char owner;
	private CardColor color;
	private CardValue value;
	
	private BridgeCard(CardColor color, CardValue value) {
		this.color = color;
		this.value = value;
	}
	
	public static BridgeCard createCard(String str, char position) {
		BridgeCard card = null;
		if (str != null && str.length() == 2) {
			CardValue val = null;
			switch (str.charAt(0)) {
			case '2':
				val = CardValue.C2;
				break;
			case '3':
				val = CardValue.C3;
				break;
			case '4':
				val = CardValue.C4;
				break;
			case '5':
				val = CardValue.C5;
				break;
			case '6':
				val = CardValue.C6;
				break;
			case '7':
				val = CardValue.C7;
				break;
			case '8':
				val = CardValue.C8;
				break;
			case '9':
				val = CardValue.C9;
				break;
			case 'T':
				val = CardValue.CT;
				break;
			case 'J':
				val = CardValue.CJ;
				break;
			case 'Q':
				val = CardValue.CQ;
				break;
			case 'K':
				val = CardValue.CK;
				break;
			case 'A':
				val = CardValue.CA;
				break;
			default:
				break;
			}
			CardColor color = null;
			switch (str.charAt(1)) {
			case 'C':
				color = CardColor.Club;
				break;
			case 'D':
				color = CardColor.Diamond;
				break;
			case 'H':
				color = CardColor.Heart;
				break;
			case 'S':
				color = CardColor.Spade;
				break;
			default:
				break;
			}
				
			if (color != null && val != null) {
				card = new BridgeCard(color, val);
				card.setOwner(position);
			}
			
		}
		return card;
	}
	
	public char getOwner() {
		return owner;
	}

	public void setOwner(char owner) {
		this.owner = owner;
	}

	@Override
	public String toString() {
//		StringBuffer sb = new StringBuffer();
//		sb.append(value.getChar()).append(color.getChar());
//		return sb.toString();
		return getString();
	}

	public int compareTo(BridgeCard o) {
		if (this.getColor() == o.getColor()) {
			if (this.value == o.value) {
				return 0;
			} else if (this.value.getVal() < o.value.getVal()) {
				return -1;
			} else if (this.value.getVal() > o.value.getVal()) {
				return 1;
			}
		}
		return 1;
	}
	
	public boolean compareColorTo(BridgeCard o) {
		return this.color.equals(o.color);
	}
	
	public int compareTo(BridgeCard o, BidColor colorContract, CardColor colorAsk) {
		if (this.compareColorTo(o)) {
			return this.compareTo(o);
		} else {
			if (this.color.toString().equals(colorContract.toString())) {
				return 1;
			}
			if (o.color.toString().equals(colorContract.toString())) {
				return -1;
			}
			if (this.color == colorAsk) {
				return 1;
			}
			if (o.color == colorAsk) {
				return -1;
			}
		}
		return 0;
	}
	
	public CardColor getColor() {
		return color;
	}
	
	public CardValue getValue() {
		return value;
	}
	
	public String getString() {
		return Character.toString(value.getChar())+Character.toString(color.getChar());
	}
	
	public String getStringWithOwner() {
		return getString()+Character.toString(owner);
	}
}
