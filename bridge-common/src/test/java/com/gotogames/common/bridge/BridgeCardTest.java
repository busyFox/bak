package com.gotogames.common.bridge;

import java.util.Random;

import junit.framework.TestCase;

import com.gotogames.common.bridge.BidColor;
import com.gotogames.common.bridge.BridgeCard;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.bridge.CardColor;

public class BridgeCardTest extends TestCase {
	private String[] tabCard = new String[] {
			"2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "TC", "JC", "QC", "KC", "AC",
			"2D", "3D", "4D", "5D", "6D", "7D", "8D", "9D", "TD", "JD", "QD", "KD", "AD",
			"2H", "3H", "4H", "5H", "6H", "7H", "8H", "9H", "TH", "VH", "QH", "KH",	"AH",
			"2S", "3S", "4S", "5S", "6S", "7S", "8S", "9S", "TS", "VS",	"QS", "KS", "AS"};

	private String getRandomCard() {
		Random random = new Random();
		return tabCard[random.nextInt(tabCard.length)];
	}
	
	public void testCreateCard() {
		String sCard1 = getRandomCard();
		BridgeCard card1 = BridgeCard.createCard(sCard1, BridgeConstantes.POSITION_NOT_VALID);
		if (!sCard1.equals(card1.toString())) {
			assertTrue(false);
		}
	}
	
	public void testComparator() {
		assertTrue(BridgeCard.createCard("3C", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeCard.createCard("JC", BridgeConstantes.POSITION_NOT_VALID)) == -1);
		assertTrue(BridgeCard.createCard("AC", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeCard.createCard("JC", BridgeConstantes.POSITION_NOT_VALID)) == 1);
		assertTrue(BridgeCard.createCard("3C", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeCard.createCard("3C", BridgeConstantes.POSITION_NOT_VALID)) == 0);
		
		assertTrue(BridgeCard.createCard("3C", BridgeConstantes.POSITION_NOT_VALID).compareColorTo(BridgeCard.createCard("9C", BridgeConstantes.POSITION_NOT_VALID)));
		assertFalse(BridgeCard.createCard("3C", BridgeConstantes.POSITION_NOT_VALID).compareColorTo(BridgeCard.createCard("3H", BridgeConstantes.POSITION_NOT_VALID)));
	}
	
	public void testComparatorWithBid() {
		assertTrue(BridgeCard.createCard("3C", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeCard.createCard("JC", BridgeConstantes.POSITION_NOT_VALID), BidColor.Heart, CardColor.Club) == -1);
		assertTrue(BridgeCard.createCard("3C", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeCard.createCard("JC", BridgeConstantes.POSITION_NOT_VALID), BidColor.NoTrump, CardColor.Club) == -1);
		assertTrue(BridgeCard.createCard("3C", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeCard.createCard("2H", BridgeConstantes.POSITION_NOT_VALID), BidColor.Heart, CardColor.Club) == -1);
		assertTrue(BridgeCard.createCard("3C", BridgeConstantes.POSITION_NOT_VALID).compareTo(BridgeCard.createCard("2H", BridgeConstantes.POSITION_NOT_VALID), BidColor.NoTrump, CardColor.Club) == 1);
	}
}
