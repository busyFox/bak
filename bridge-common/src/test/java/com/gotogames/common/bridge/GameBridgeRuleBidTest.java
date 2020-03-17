package com.gotogames.common.bridge;

import java.util.List;

import junit.framework.TestCase;

import com.gotogames.common.bridge.BridgeBid;
import com.gotogames.common.bridge.GameBridgeRule;

public class GameBridgeRuleBidTest extends TestCase {
	public void testIsBidFinished() {
		String bids = "1CPA2CPAPAPA";
		List<BridgeBid> listBids = GameBridgeRule.convertPlayBidsStringToList(bids, 'S');
		assertNotNull(listBids);
		assertEquals((bids.length() / 2), listBids.size());
		assertTrue(GameBridgeRule.isBidsFinished(listBids));
		assertEquals("2C", GameBridgeRule.getHigherBid(listBids).toString());
		
		List<BridgeBid> listBids2 = GameBridgeRule.convertPlayBidsStringToList("1CPAPA2CPA3C4CPA", 'S');
		assertNotNull(listBids2);
		assertFalse(GameBridgeRule.isBidsFinished(listBids2));
	}
	
	public void testDeclarer() {
		String bids = "1CPA1HPAPAPA";
		List<BridgeBid> listBids = GameBridgeRule.convertPlayBidsStringToList(bids, 'S');
		assertNotNull(listBids);
		assertEquals((bids.length() / 2), listBids.size());
		assertTrue(GameBridgeRule.isBidsFinished(listBids));
		BridgeBid bidDeclarer = GameBridgeRule.getHigherBid(listBids);
		assertEquals("1H", bidDeclarer.toString());
		assertEquals('N', GameBridgeRule.getWinnerBids(listBids));
		
		bids = "PA1C2HX13HPAPAPA";
		listBids = GameBridgeRule.convertPlayBidsStringToList(bids, 'N');
		assertNotNull(listBids);
		assertEquals((bids.length() / 2), listBids.size());
		assertTrue(GameBridgeRule.isBidsFinished(listBids));
		bidDeclarer = GameBridgeRule.getHigherBid(listBids);
		assertEquals("3H", bidDeclarer.toString());
		assertEquals('S', GameBridgeRule.getWinnerBids(listBids));
	}
	
	public void testX1() {
		String bids = "1CPA2CPAPAX1PAPAPA";
		List<BridgeBid> listBids = GameBridgeRule.convertPlayBidsStringToList(bids, 'S');
		assertNotNull(listBids);
		assertEquals((bids.length() / 2), listBids.size());
		assertTrue(GameBridgeRule.isBidsFinished(listBids));
		assertEquals("2C", GameBridgeRule.getHigherBid(listBids).toString());
		assertTrue(GameBridgeRule.getX1(listBids) != null);
	}
	
	public void testX2() {
		String bids = "1CPA2CPAPAX1PAPAX2PAPAPA";
		List<BridgeBid> listBids = GameBridgeRule.convertPlayBidsStringToList(bids, 'S');
		assertNotNull(listBids);
		assertEquals((bids.length() / 2), listBids.size());
		assertTrue(GameBridgeRule.isBidsFinished(listBids));
		assertEquals("2C", GameBridgeRule.getHigherBid(listBids).toString());
		assertTrue(GameBridgeRule.getX1(listBids) != null);
		assertTrue(GameBridgeRule.getX2(listBids) != null);
	}
	
	public void testBidValid(){
		String bids = "1CPA1D1S1NPA";
		List<BridgeBid> listBids = GameBridgeRule.convertPlayBidsStringToList(bids, 'N');
		assertNotNull(listBids);
		assertEquals((bids.length() / 2), listBids.size());
		assertTrue(GameBridgeRule.isBidValid(listBids, BridgeBid.createBid("2C", 'S')));
		
		bids = "PAPA1C2HX1PA";
		listBids = GameBridgeRule.convertPlayBidsStringToList(bids, 'N');
		assertNotNull(listBids);
		assertEquals((bids.length() / 2), listBids.size());
		assertEquals(GameBridgeRule.getHigherBid(listBids).toString(), "2H");
		assertTrue(GameBridgeRule.isX1(listBids));
		assertTrue(GameBridgeRule.isBidValid(listBids, BridgeBid.createBid("2S", 'S')));
		
		bids = "PAPA1C2HX1PA2SX1X2";
		listBids = GameBridgeRule.convertPlayBidsStringToList(bids, 'N');
		assertEquals(GameBridgeRule.getHigherBid(listBids).toString(), "2S");
		assertTrue(GameBridgeRule.isX1(listBids));
		assertTrue(GameBridgeRule.isX2(listBids));
		
		bids = "PAPA1C2HX1PA2S";
		listBids = GameBridgeRule.convertPlayBidsStringToList(bids, 'N');
		assertFalse(GameBridgeRule.isBidValid(listBids, BridgeBid.createBid("X2", 'W')));
		assertTrue(GameBridgeRule.isBidValid(listBids, BridgeBid.createBid("X1", 'W')));
		bids = "PAPA1C2HX1PA2SX1PAPA";
		listBids = GameBridgeRule.convertPlayBidsStringToList(bids, 'N');
		assertTrue(GameBridgeRule.isBidValid(listBids, BridgeBid.createBid("X2", 'S')));
		
		bids = "PAPA1C2HX1PA2S";
		listBids = GameBridgeRule.convertPlayBidsStringToList(bids, 'N');
		assertEquals(GameBridgeRule.getHigherBid(listBids).toString(), "2S");
		assertFalse(GameBridgeRule.isX1(listBids));
		
		bids = "PAPA1C2HX1X22S";
		listBids = GameBridgeRule.convertPlayBidsStringToList(bids, 'N');
		assertEquals(GameBridgeRule.getHigherBid(listBids).toString(), "2S");
		assertFalse(GameBridgeRule.isX1(listBids));
		assertFalse(GameBridgeRule.isX2(listBids));
	}
	
}
