package com.gotogames.common.bridge;

import com.gotogames.common.bridge.BridgeDeal;
import com.gotogames.common.bridge.BridgeDealParam;

import junit.framework.TestCase;

public class BridgeDealTest extends TestCase {

	public void testGenerateRandomDeal() {
		String deal = BridgeDeal.generateRandomDeal();
		assertEquals(54, deal.length());
		assertTrue(BridgeDeal.isDealValid(deal));

		assertFalse(BridgeDeal.isDealValid(deal.substring(1)));
		assertTrue(BridgeDeal.isDealValid("ENWNWWEWNNENWESNWSSSWNEEWEWESSNWNWNSEWSWSSNNEESNEENSSE"));
		assertFalse(BridgeDeal.isDealValid("ENZNWWEWNNENWESNWSSSWNEEWEWESSNWNWNSEWSWSSNNEESNEENSSE"));
		assertFalse(BridgeDeal.isDealValid("ENZNWWEWNNENWESNWSSSWNEEWEWESSNWNWNSEWSWSSNNEESNEERSSE"));
		assertFalse(BridgeDeal.isDealValid("ENZNWWEWNNENWESNWSSSWNEEWEWESSNWNWNSEWSWSSNNEESNEENSEE"));
		assertFalse(BridgeDeal.isDealValid("ENZNWWEWNNENWESNWSSSWNEEWEWESSNWWWNSEWSWSSNNEESNEENSSE"));
	}
	
	public void testGetNbCardColorForPlayer() {
		String distrib="NNNNNNNNNNNNNSSSSSSSSSSSSSWWWWWWWWWWWWWEEEEEEEEEEEEE";
		assertEquals(BridgeDeal.getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_EAST, BridgeConstantes.CARD_COLOR_CLUB), 0);
		assertEquals(BridgeDeal.getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_NORTH, BridgeConstantes.CARD_COLOR_CLUB), 13);
		assertEquals(BridgeDeal.getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_SOUTH, BridgeConstantes.CARD_COLOR_CLUB), 0);
		assertEquals(BridgeDeal.getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_SOUTH, BridgeConstantes.CARD_COLOR_DIAMOND), 13);
		assertEquals(BridgeDeal.getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_WEST, BridgeConstantes.CARD_COLOR_HEART), 13);
		assertEquals(BridgeDeal.getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_NORTH, BridgeConstantes.CARD_COLOR_HEART), 0);
		assertEquals(BridgeDeal.getNbCardColorForPlayer(distrib, BridgeConstantes.POSITION_EAST, BridgeConstantes.CARD_COLOR_SPADE), 13);
		
		String distrib2="SWNNNNNNNNNNNSSNSSSSSSSSSSWWNWWWWWWWWWWEEEEEEEEEEEEE";
		assertEquals(BridgeDeal.getNbCardColorForPlayer(distrib2, BridgeConstantes.POSITION_NORTH, BridgeConstantes.CARD_COLOR_CLUB), 11);
		assertEquals(BridgeDeal.getNbCardColorForPlayer(distrib2, BridgeConstantes.POSITION_NORTH, BridgeConstantes.CARD_COLOR_DIAMOND), 1);
		assertEquals(BridgeDeal.getNbCardColorForPlayer(distrib2, BridgeConstantes.POSITION_NORTH, BridgeConstantes.CARD_COLOR_HEART), 1);
		assertEquals(BridgeDeal.getNbCardColorForPlayer(distrib2, BridgeConstantes.POSITION_NORTH, BridgeConstantes.CARD_COLOR_SPADE), 0);
		assertEquals(BridgeDeal.getNbCardColorForPlayer(distrib2, BridgeConstantes.POSITION_SOUTH, BridgeConstantes.CARD_COLOR_CLUB), 1);
		assertEquals(BridgeDeal.getNbCardColorForPlayer(distrib2, BridgeConstantes.POSITION_WEST, BridgeConstantes.CARD_COLOR_CLUB), 1);
	}
	
	public void testGetNbPointsHonForPlayer() {
		String distrib="NNNNNNNNNNNNNSSSSSSSSSSSSSWWWWWWWWWWWWWEEEEEEEEEEEEE";
		assertEquals(10,BridgeDeal.getNbPointsHonForPlayer(distrib, BridgeConstantes.POSITION_EAST));
		assertEquals(10,BridgeDeal.getNbPointsHonForPlayer(distrib, BridgeConstantes.POSITION_SOUTH));
		assertEquals(10,BridgeDeal.getNbPointsHonForPlayer(distrib, BridgeConstantes.POSITION_WEST));
		assertEquals(10,BridgeDeal.getNbPointsHonForPlayer(distrib, BridgeConstantes.POSITION_NORTH));
	}
	
	public void testGetVulnerability() {
		assertEquals('L', BridgeDeal.getVulnerability(1));
		assertEquals('L', BridgeDeal.getVulnerability(8));
		assertEquals('L', BridgeDeal.getVulnerability(11));
		assertEquals('L', BridgeDeal.getVulnerability(14));
		assertEquals('L', BridgeDeal.getVulnerability(17));
		assertEquals('L', BridgeDeal.getVulnerability(24));
		assertEquals('L', BridgeDeal.getVulnerability(27));
		assertEquals('L', BridgeDeal.getVulnerability(30));
		
		assertEquals('N', BridgeDeal.getVulnerability(2));
		assertEquals('N', BridgeDeal.getVulnerability(5));
		assertEquals('N', BridgeDeal.getVulnerability(12));
		assertEquals('N', BridgeDeal.getVulnerability(15));
		assertEquals('N', BridgeDeal.getVulnerability(18));
		assertEquals('N', BridgeDeal.getVulnerability(21));
		assertEquals('N', BridgeDeal.getVulnerability(28));
		assertEquals('N', BridgeDeal.getVulnerability(31));
		
		assertEquals('E', BridgeDeal.getVulnerability(3));
		assertEquals('E', BridgeDeal.getVulnerability(6));
		assertEquals('E', BridgeDeal.getVulnerability(9));
		assertEquals('E', BridgeDeal.getVulnerability(16));
		assertEquals('E', BridgeDeal.getVulnerability(19));
		assertEquals('E', BridgeDeal.getVulnerability(22));
		assertEquals('E', BridgeDeal.getVulnerability(25));
		assertEquals('E', BridgeDeal.getVulnerability(32));
		
		assertEquals('A', BridgeDeal.getVulnerability(4));
		assertEquals('A', BridgeDeal.getVulnerability(7));
		assertEquals('A', BridgeDeal.getVulnerability(10));
		assertEquals('A', BridgeDeal.getVulnerability(13));
		assertEquals('A', BridgeDeal.getVulnerability(20));
		assertEquals('A', BridgeDeal.getVulnerability(23));
		assertEquals('A', BridgeDeal.getVulnerability(26));
		assertEquals('A', BridgeDeal.getVulnerability(29));
		
		System.out.println("Vul 99="+Character.toString(BridgeDeal.getVulnerability(99)));
		System.out.println("Vul 100="+Character.toString(BridgeDeal.getVulnerability(100)));
		System.out.println("Vul 101="+Character.toString(BridgeDeal.getVulnerability(101)));
		System.out.println("Vul 1="+Character.toString(BridgeDeal.getVulnerability(1)));
	}
	
	public void testGenerateMultiDeal() {
		BridgeDealParam param = new BridgeDealParam();
		param.index = 1;
		param.ptsHonMin[0] = 0; param.ptsHonMin[1] = 0; param.ptsHonMin[2] = 0; param.ptsHonMin[3] = 0; param.ptsHonMin[4] = 0;param.ptsHonMin[5] = 0;
		param.ptsHonMax[0] = 40; param.ptsHonMax[1] = 40; param.ptsHonMax[2] = 40; param.ptsHonMax[3] = 40; param.ptsHonMax[4] = 40;param.ptsHonMax[5] = 40;
		param.nbCardCMin[0] = 0; param.nbCardCMin[1] = 0; param.nbCardCMin[2] = 0; param.nbCardCMin[3] = 0; param.nbCardCMin[4] = 0;param.nbCardCMin[5] = 0;
		param.nbCardCMax[0] = 13; param.nbCardCMax[1] = 13; param.nbCardCMax[2] = 13; param.nbCardCMax[3] = 13; param.nbCardCMax[4] = 13;param.nbCardCMax[5] = 13;
		param.nbCardDMin[0] = 0; param.nbCardDMin[1] = 0; param.nbCardDMin[2] = 0; param.nbCardDMin[3] = 0; param.nbCardDMin[4] = 0;param.nbCardDMin[5] = 0;
		param.nbCardDMax[0] = 13; param.nbCardDMax[1] = 13; param.nbCardDMax[2] = 13; param.nbCardDMax[3] = 13; param.nbCardDMax[4] = 13;param.nbCardDMax[5] = 13;
		param.nbCardHMin[0] = 0; param.nbCardHMin[1] = 0; param.nbCardHMin[2] = 0; param.nbCardHMin[3] = 0; param.nbCardHMin[4] = 0;param.nbCardHMin[5] = 0;
		param.nbCardHMax[0] = 13; param.nbCardHMax[1] = 13; param.nbCardHMax[2] = 13; param.nbCardHMax[3] = 13; param.nbCardHMax[4] = 13;param.nbCardHMax[5] = 13;
		param.nbCardSMin[0] = 0; param.nbCardSMin[1] = 0; param.nbCardSMin[2] = 0; param.nbCardSMin[3] = 0; param.nbCardSMin[4] = 0;param.nbCardSMin[5] = 0;
		param.nbCardSMax[0] = 13; param.nbCardSMax[1] = 13; param.nbCardSMax[2] = 13; param.nbCardSMax[3] = 13; param.nbCardSMax[4] = 13;param.nbCardSMax[5] = 13;
		String distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		param.index = 2;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		param.index = 3;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		param.index = 4;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		param.index = 5;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		param.index = 6;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		param.index = 7;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		param.index = 8;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		param.index = 9;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		param.index = 10;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
	}
	
	public void testGenerateDeal() {
//		[dujeuenSud]
//		 phmin=15 00 00 00 00 00
//		 phmax=40 40 40 40 40 40
//		 trmin=01 01 01 01 00 00
//		 trmax=13 13 13 13 13 13
//		 camin=01 01 01 01 00 00
//		 camax=13 13 13 13 13 13
//		 comin=01 01 01 01 00 00
//		 comax=13 13 13 13 13 13
//		 pimin=01 01 01 01 00 00
//		 pimax=13 13 13 13 13 13
		
		BridgeDealParam param = new BridgeDealParam();
		param.index = 1;
		param.ptsHonMin[0] = 15; param.ptsHonMin[1] = 0; param.ptsHonMin[2] = 0; param.ptsHonMin[3] = 0; param.ptsHonMin[4] = 0;param.ptsHonMin[5] = 0;
		param.ptsHonMax[0] = 40; param.ptsHonMax[1] = 40; param.ptsHonMax[2] = 40; param.ptsHonMax[3] = 40; param.ptsHonMax[4] = 40;param.ptsHonMax[5] = 40;
		param.nbCardCMin[0] = 1; param.nbCardCMin[1] = 1; param.nbCardCMin[2] = 1; param.nbCardCMin[3] = 1; param.nbCardCMin[4] = 0;param.nbCardCMin[5] = 0;
		param.nbCardCMax[0] = 13; param.nbCardCMax[1] = 13; param.nbCardCMax[2] = 13; param.nbCardCMax[3] = 13; param.nbCardCMax[4] = 13;param.nbCardCMax[5] = 13;
		param.nbCardDMin[0] = 1; param.nbCardDMin[1] = 1; param.nbCardDMin[2] = 1; param.nbCardDMin[3] = 1; param.nbCardDMin[4] = 0;param.nbCardDMin[5] = 0;
		param.nbCardDMax[0] = 13; param.nbCardDMax[1] = 13; param.nbCardDMax[2] = 13; param.nbCardDMax[3] = 13; param.nbCardDMax[4] = 13;param.nbCardDMax[5] = 13;
		param.nbCardHMin[0] = 1; param.nbCardHMin[1] = 1; param.nbCardHMin[2] = 1; param.nbCardHMin[3] = 1; param.nbCardHMin[4] = 0;param.nbCardHMin[5] = 0;
		param.nbCardHMax[0] = 13; param.nbCardHMax[1] = 13; param.nbCardHMax[2] = 13; param.nbCardHMax[3] = 13; param.nbCardHMax[4] = 13;param.nbCardHMax[5] = 13;
		param.nbCardSMin[0] = 1; param.nbCardSMin[1] = 1; param.nbCardSMin[2] = 1; param.nbCardSMin[3] = 1; param.nbCardSMin[4] = 0;param.nbCardSMin[5] = 0;
		param.nbCardSMax[0] = 13; param.nbCardSMax[1] = 13; param.nbCardSMax[2] = 13; param.nbCardSMax[3] = 13; param.nbCardSMax[4] = 13;param.nbCardSMax[5] = 13;
		String distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		assertTrue(BridgeDeal.getNbPointsHonForPlayer(distrib.substring(2), BridgeConstantes.POSITION_SOUTH)>=15);
		param.index = 2;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		assertTrue(BridgeDeal.getNbPointsHonForPlayer(distrib.substring(2), BridgeConstantes.POSITION_SOUTH)>=15);
		param.index = 3;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		assertTrue(BridgeDeal.getNbPointsHonForPlayer(distrib.substring(2), BridgeConstantes.POSITION_SOUTH)>=15);
		param.index = 4;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		assertTrue(BridgeDeal.getNbPointsHonForPlayer(distrib.substring(2), BridgeConstantes.POSITION_SOUTH)>=15);
		param.index = 5;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		assertTrue(BridgeDeal.getNbPointsHonForPlayer(distrib.substring(2), BridgeConstantes.POSITION_SOUTH)>=15);
		param.index = 6;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		assertTrue(BridgeDeal.getNbPointsHonForPlayer(distrib.substring(2), BridgeConstantes.POSITION_SOUTH)>=15);
		param.index = 7;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		assertTrue(BridgeDeal.getNbPointsHonForPlayer(distrib.substring(2), BridgeConstantes.POSITION_SOUTH)>=15);
		param.index = 8;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		assertTrue(BridgeDeal.getNbPointsHonForPlayer(distrib.substring(2), BridgeConstantes.POSITION_SOUTH)>=15);
		param.index = 9;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		assertTrue(BridgeDeal.getNbPointsHonForPlayer(distrib.substring(2), BridgeConstantes.POSITION_SOUTH)>=15);
		param.index = 10;
		distrib = BridgeDeal.generateDeal(param);
		System.out.println(distrib);
		assertTrue(BridgeDeal.isDealValid(distrib));
		assertTrue(BridgeDeal.getNbPointsHonForPlayer(distrib.substring(2), BridgeConstantes.POSITION_SOUTH)>=15);
	}
	
	public void testCheckDistribution() {
//		[TrainingPointsNS]
//		phmin=00 00 00 00 22 00
//		phmax=40 40 40 40 40 40
//		trmin=00 00 00 00 00 00
//		trmax=13 13 13 13 13 13
//		camin=00 00 00 00 00 00
//		camax=13 13 13 13 13 13
//		comin=00 00 00 00 00 00
//		comax=13 13 13 13 13 13
//		pimin=00 00 00 00 00 00
//		pimax=13 13 13 13 13 13
		BridgeDealParam param = new BridgeDealParam();
		param.ptsHonMin[0] = 0; param.ptsHonMin[1] = 0; param.ptsHonMin[2] = 0; param.ptsHonMin[3] = 0; param.ptsHonMin[4] = 22;param.ptsHonMin[5] = 0;
		param.ptsHonMax[0] = 40; param.ptsHonMax[1] = 40; param.ptsHonMax[2] = 40; param.ptsHonMax[3] = 40; param.ptsHonMax[4] = 40;param.ptsHonMax[5] = 40;
		param.nbCardCMin[0] = 0; param.nbCardCMin[1] = 0; param.nbCardCMin[2] = 0; param.nbCardCMin[3] = 0; param.nbCardCMin[4] = 0;param.nbCardCMin[5] = 0;
		param.nbCardCMax[0] = 13; param.nbCardCMax[1] = 13; param.nbCardCMax[2] = 13; param.nbCardCMax[3] = 13; param.nbCardCMax[4] = 13;param.nbCardCMax[5] = 13;
		param.nbCardDMin[0] = 0; param.nbCardDMin[1] = 0; param.nbCardDMin[2] = 0; param.nbCardDMin[3] = 0; param.nbCardDMin[4] = 0;param.nbCardDMin[5] = 0;
		param.nbCardDMax[0] = 13; param.nbCardDMax[1] = 13; param.nbCardDMax[2] = 13; param.nbCardDMax[3] = 13; param.nbCardDMax[4] = 13;param.nbCardDMax[5] = 13;
		param.nbCardHMin[0] = 0; param.nbCardHMin[1] = 0; param.nbCardHMin[2] = 0; param.nbCardHMin[3] = 0; param.nbCardHMin[4] = 0;param.nbCardHMin[5] = 0;
		param.nbCardHMax[0] = 13; param.nbCardHMax[1] = 13; param.nbCardHMax[2] = 13; param.nbCardHMax[3] = 13; param.nbCardHMax[4] = 13;param.nbCardHMax[5] = 13;
		param.nbCardSMin[0] = 0; param.nbCardSMin[1] = 0; param.nbCardSMin[2] = 0; param.nbCardSMin[3] = 0; param.nbCardSMin[4] = 0;param.nbCardSMin[5] = 0;
		param.nbCardSMax[0] = 13; param.nbCardSMax[1] = 13; param.nbCardSMax[2] = 13; param.nbCardSMax[3] = 13; param.nbCardSMax[4] = 13;param.nbCardSMax[5] = 13;
		String cards_bad="WWNNSWENESNNWEEEWWSSWWENENSSWNWWNSSWNWSESSNSNEESENEE";
		assertFalse(BridgeDeal.checkDistribution(cards_bad, param));
		String distrib = BridgeDeal.generateDeal(param);
		assertNotNull(distrib);
		assertTrue(distrib.length() > 2);
		assertTrue(BridgeDeal.checkDistribution(distrib.substring(2), param));
	}
}
