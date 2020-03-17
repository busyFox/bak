package com.gotogames.common.bridge;


public class BridgeBid implements Comparable<BridgeBid>{
	private char owner;
	private BidColor color;
	private BidValue value;
    private boolean alert = false;
	
	private BridgeBid(BidColor color, BidValue value) {
		this.color = color;
		this.value = value;
	}

    private BridgeBid(BidColor color, BidValue value, boolean alert) {
        this.color = color;
        this.value = value;
        this.alert = alert;
    }
	
	public static BridgeBid createBid(String str, char position) {
		BridgeBid bid = null;
		if (str != null && str.length() == 2) {
			if (str.equals(BridgeConstantes.BID_PASS)) {
				bid = new BridgeBid(BidColor.Other, BidValue.PASS);
			}
			else if (str.equals(BridgeConstantes.BID_X1)) {
				bid = new BridgeBid(BidColor.Other, BidValue.X1);
			}
			else if (str.equals(BridgeConstantes.BID_X2)) {
				bid = new BridgeBid(BidColor.Other, BidValue.X2);
			}
			else {
				BidValue val = null;
				switch (str.charAt(0)) {
				case '1':
					val = BidValue.V1;
					break;
				case '2':
					val = BidValue.V2;
					break;
				case '3':
					val = BidValue.V3;
					break;
				case '4':
					val = BidValue.V4;
					break;
				case '5':
					val = BidValue.V5;
					break;
				case '6':
					val = BidValue.V6;
					break;
				case '7':
					val = BidValue.V7;
					break;
				default:
					break;
				}
				BidColor color = null;
				switch (str.charAt(1)) {
				case 'C':
					color = BidColor.Club;
					break;
				case 'D':
					color = BidColor.Diamond;
					break;
				case 'H':
					color = BidColor.Heart;
					break;
				case 'S':
					color = BidColor.Spade;
					break;
				case 'N':
					color = BidColor.NoTrump;
					break;
				default:
					break;
				}
				
				if (color != null && val != null) {
					bid = new BridgeBid(color, val);
				}
			}
			if (bid != null) {
				bid.setOwner(position);
			}
		}
		return bid;
	}

    public static BridgeBid createBid(String str, char position, boolean alert) {
        BridgeBid bid = createBid(str, position);
        if (bid != null) {
            bid.setAlert(alert);
        }
        return bid;
    }
	
	public boolean isPass() {
		return color.equals(BidColor.Other) && value.equals(BidValue.PASS);
	}
	
	public boolean isX1() {
		return color.equals(BidColor.Other) && value.equals(BidValue.X1);
	}
	
	public boolean isX2() {
		return color.equals(BidColor.Other) && value.equals(BidValue.X2);
	}
	
	public int compareTo(BridgeBid o) {
		
		// same color
		if (this.color == o.color) {
			// same value
			if (this.value.getVal() == o.value.getVal()) {
				return 0;
			}
			if (this.value.getVal() > o.value.getVal()) {
				return 1;
			} else {
				return -1;
			}
			
		}
		
		// color different
		// X1 and X2 are specific bid. Could nt be compare with other
		if (this.isX1() || this.isX2()) {
			return -1;
		}
		if (o.isX1() || o.isX2()) {
			return 1;
		}
		if (this.isPass()) {
			return -1;
		}
		if (o.isPass()) {
			return 1;
		}
		
		// same value
		if (this.value.getVal() == o.value.getVal()) {
			if (this.color.getVal() > o.color.getVal()) {
				return 1;
			}
			return -1;
		}
		// this value > o value
		else if (this.value.getVal() > o.value.getVal()) {
			return 1;
		}
		// o value > this value
		else {
			return -1;
		}
	}

	public char getOwner() {
		return owner;
	}

	public void setOwner(char owner) {
		this.owner = owner;
	}

	@Override
	public String toString() {
		return getString();
	}
	
	public BidColor getColor() {
		return color;
	}
	
	public BidValue getValue() {
		return value;
	}
	
	public int getRequiredNbTrick() {
		if (isPass() || isX1() || isX2()) {
			return 0;
		}
		return 6 + value.getVal();
	}
	
	public String getString() {
		if (value.compareTo(BidValue.PASS) == 0) {
			return "PA";
		}
		if (value.compareTo(BidValue.X1) == 0) {
			return "X1";
		}
		if (value.compareTo(BidValue.X2) == 0) {
			return "X2";
		}
		return Character.toString(value.getChar())+Character.toString(color.getChar());
	}
	
	public String getStringWithOwner() {
		return getString()+Character.toString(owner);
	}

    public String getStringWithOwnerAndAlert() {
        String s =  getString()+Character.toString(owner);
        if (alert) {
            s += "A";
        }
        return s;
    }

    public boolean isAlert() {
        return alert;
    }

    public void setAlert(boolean alert) {
        this.alert = alert;
    }
}
