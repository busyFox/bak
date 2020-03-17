package com.gotogames.common.bridge;

import java.util.Random;

import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.bridge.BridgeBid;

import junit.framework.TestCase;

public class BridgeBidTest extends TestCase {
	private String[] tabBid = new String[] { "PA", "X1", "X2",
			"1C", "2C",	"3C", "4C", "5C", "6C", "7C",
			"1D", "2D", "3D", "4D", "5D", "6D",	"7D",
			"1H", "2H", "3H", "4H", "5H", "6H", "7H",
			"1S", "2S",	"3S", "4S", "5S", "6S", "7S",
			"1N", "2N", "3N", "4N", "5N", "6N",	"7N" };
	private String getRandomBid() {
		Random random = new Random();
		return tabBid[random.nextInt(tabBid.length)];
	}

	public void testCreateBid() {
		String sBid1 = getRandomBid();
		System.out.println("random bid : "+sBid1);
		BridgeBid bid1 = BridgeBid.createBid(sBid1, BridgeConstantes.POSITION_NOT_VALID);
		if (!sBid1.equals(bid1.toString())) {
			assertTrue(false);
		}
		System.out.println("Bridge bid : "+bid1.toString());
	}
	
	public void testComparaison() {
		// Same color
		// 3C < 5C
		assertEquals(-1,BridgeBid.createBid("3C", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("5C", BridgeConstantes.POSITION_NOT_VALID)));
		// 3C = 3C
		assertEquals(0,BridgeBid.createBid("3C", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("3C", BridgeConstantes.POSITION_NOT_VALID)));
		// 5C > 3C
		assertEquals(1,BridgeBid.createBid("5C", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("3C", BridgeConstantes.POSITION_NOT_VALID)));
		
		// Same value
		// 1S > 1D 
		assertEquals(1,BridgeBid.createBid("1S", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("1D", BridgeConstantes.POSITION_NOT_VALID)));
		// 1H < 1S 
		assertEquals(-1,BridgeBid.createBid("1H", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("1S", BridgeConstantes.POSITION_NOT_VALID)));
		// 1N > 1C 
		assertEquals(1,BridgeBid.createBid("1N", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("1C", BridgeConstantes.POSITION_NOT_VALID)));

		// PASS
		// 1S > PA 
		assertEquals(1,BridgeBid.createBid("1S", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("PA", BridgeConstantes.POSITION_NOT_VALID)));
		// PA < 2D 
		assertEquals(-1,BridgeBid.createBid("PA", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("2D", BridgeConstantes.POSITION_NOT_VALID)));

		// X1
		// X1 < 1D 
		assertEquals(-1,BridgeBid.createBid("X1", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("1D", BridgeConstantes.POSITION_NOT_VALID)));
		// X1 < 7N 
		assertEquals(-1,BridgeBid.createBid("X1", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("7N", BridgeConstantes.POSITION_NOT_VALID)));
		// 2S > X1 
		assertEquals(1,BridgeBid.createBid("2S", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("X1", BridgeConstantes.POSITION_NOT_VALID)));
		// X2
		// X1 < X2
		assertEquals(-1,BridgeBid.createBid("X1", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("X2", BridgeConstantes.POSITION_NOT_VALID)));
		// X2 < 5D 
		assertEquals(-1,BridgeBid.createBid("X2", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("5D", BridgeConstantes.POSITION_NOT_VALID)));
		
		// color and value different		
		// 1S < 3C
		assertEquals(-1,BridgeBid.createBid("1S", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("3C", BridgeConstantes.POSITION_NOT_VALID)));
		// 1N < 3C
		assertEquals(-1,BridgeBid.createBid("1N", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("3C", BridgeConstantes.POSITION_NOT_VALID)));
		// 7N > 3C
		assertEquals(1,BridgeBid.createBid("7N", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeBid.createBid("3C", BridgeConstantes.POSITION_NOT_VALID)));
	}
	
	public void testIsPass() {
		assertEquals(true, BridgeBid.createBid(BridgeConstantes.BID_PASS, BridgeConstantes.POSITION_NOT_VALID).isPass());
		assertEquals(false, BridgeBid.createBid("1N", BridgeConstantes.POSITION_NOT_VALID).isPass());
	}
	
	public void testIsX1X2() {
		assertEquals(false, BridgeBid.createBid(BridgeConstantes.BID_PASS, BridgeConstantes.POSITION_NOT_VALID).isX1());
		assertEquals(true, BridgeBid.createBid(BridgeConstantes.BID_X1, BridgeConstantes.POSITION_NOT_VALID).isX1());
		assertEquals(true, BridgeBid.createBid(BridgeConstantes.BID_X2, BridgeConstantes.POSITION_NOT_VALID).isX2());
		assertEquals(false, BridgeBid.createBid("1N", BridgeConstantes.POSITION_NOT_VALID).isX2());
		assertEquals(false, BridgeBid.createBid("1N", BridgeConstantes.POSITION_NOT_VALID).isX1());
	}
	
	public void testGetRequiredNbTrick() {
		assertEquals(0, BridgeBid.createBid(BridgeConstantes.BID_PASS, BridgeConstantes.POSITION_NOT_VALID).getRequiredNbTrick());
		assertEquals(0, BridgeBid.createBid(BridgeConstantes.BID_X1, BridgeConstantes.POSITION_NOT_VALID).getRequiredNbTrick());
		assertEquals(0, BridgeBid.createBid(BridgeConstantes.BID_X2, BridgeConstantes.POSITION_NOT_VALID).getRequiredNbTrick());
		assertEquals(7, BridgeBid.createBid("1N", BridgeConstantes.POSITION_NOT_VALID).getRequiredNbTrick());
		assertEquals(8, BridgeBid.createBid("2H", BridgeConstantes.POSITION_NOT_VALID).getRequiredNbTrick());
		assertEquals(10, BridgeBid.createBid("4S", BridgeConstantes.POSITION_NOT_VALID).getRequiredNbTrick());
		assertEquals(11, BridgeBid.createBid("5D", BridgeConstantes.POSITION_NOT_VALID).getRequiredNbTrick());
		assertEquals(13, BridgeBid.createBid("7N", BridgeConstantes.POSITION_NOT_VALID).getRequiredNbTrick());
	}
}
