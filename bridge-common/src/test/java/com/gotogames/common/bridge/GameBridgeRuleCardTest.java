package com.gotogames.common.bridge;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.gotogames.common.bridge.BridgeBid;
import com.gotogames.common.bridge.BridgeCard;
import com.gotogames.common.bridge.GameBridgeRule;

public class GameBridgeRuleCardTest extends TestCase {
	public void testCardValid() {
		String distrib1 = "WSSSNEEENSWEEWNNNWWSNWNSSWWWNSENNESSESENSENSEWWWEEWN";
		char dealer1 = 'N';
		String played1 = "4HAH5H2H7CJCQC6CAD3D6H8D2CTCKC3C8C4C3H2S7HQHKH9S2D4D4SQD9HJHKS8H6D5D7SKDAC5CTS9DTDJDJSTH9C6S8S5S7DASQS3S";
		char declarer1 = 'W';
		String contract1 = "4S";
		char lastWinner = 'N';
		
		BridgeBid bidContract = BridgeBid.createBid(contract1, declarer1);
		
		List<BridgeCard> listDeal = GameBridgeRule.convertCardDealToList(distrib1);
		assertNotNull(listDeal);
		List<BridgeCard> listPlay = new ArrayList<BridgeCard>();
		int nbTrick = 0;
		for (int i = 0; i < played1.length(); i = i+2) {
			BridgeCard newCard = BridgeCard.createCard(played1.substring(i, i+2), lastWinner);
			assertTrue(GameBridgeRule.isCardValid(listPlay, newCard, listDeal));
			listPlay.add(newCard);
			if (GameBridgeRule.isEndTrick(listPlay)) {
				nbTrick++;
				BridgeCard cardWinner = GameBridgeRule.getLastWinnerTrick(listPlay, bidContract);
				assertNotNull(cardWinner);
				lastWinner = cardWinner.getOwner();
				System.out.println("Trick "+nbTrick+" : "+lastWinner);
			} else {
				lastWinner = GameBridgeRule.getNextPosition(lastWinner);
			}
		}
		
	}
	
	public void testCardValid2() {
		String distrib1 = "SWNEWESENNNNNSWSWWNWSWSWEEESWNSENNWENSENESEWSNSEWWES";
		char dealer1 = 'W';
		String played1 = "AD2D6D7DKD4D3D4CTC7C2C6CJC5C8C3CQC9C4S5DKC5S3H8D2S3SAS6S6H4H5H7HAHKHTH8HTS7SJS";
		char declarer1 = 'N';
		String contract1 = "1C";
		char lastWinner = 'N';
		
		BridgeBid bidContract = BridgeBid.createBid(contract1, declarer1);
		List<BridgeCard> listDeal = GameBridgeRule.convertCardDealToList(distrib1);
		assertNotNull(listDeal);
		List<BridgeCard> listPlay = GameBridgeRule.convertPlayCardsStringToList(played1, bidContract);
		BridgeCard newCard = BridgeCard.createCard("9S", 'S');
		assertTrue(GameBridgeRule.isCardValid(listPlay, newCard, listDeal));
	}
}
