package com.gotogames.common.bridge;

import java.util.ArrayList;
import java.util.List;

import com.gotogames.common.bridge.BridgeBid;
import com.gotogames.common.bridge.BridgeCard;
import com.gotogames.common.bridge.CardColor;
import com.gotogames.common.bridge.GameBridgeRule;

import junit.framework.TestCase;

public class GameBridgeRuleTest extends TestCase {
	public void testGetNextPositionToPlay() {
		String deal = "SENNNSSENSWEEESWWENNEWNWNSEWSEWNSSESWEWESWWWNNNWNSSE";
		char dealer = 'N';
		char vul = 'L';
		String bids= "PAN-1CE-PAS-1HW-PAN-2HE-PAS-3HW-PAN-4HE-PAS-PAW-PAN";
		String cards = "5CN-3CE-JCS-QCW-3HW-7HN-KHE-4HS-ACE-2CS-5DW-6CN-KCE-8CS-4SW-4CN-2HE-8HS-AHW-8DN-5SW-8SN-ASE-3SS-9CE-7CS-6HW-TCN-6SW-7SN-2SE-QSS-ADS-4DW-7DN-2DE-3DS-TDW-JDN-6DE-KDN-9DE-KSS-QDW-9SN-5HE-9HS-TSW-JHS-QHW-JSN-THE";
		
		String[] bidsSplit = bids.split("-");
		String[] cardsSplit = cards.split("-");
		String bidsTemp = "", cardsTemp = "";
		List<BridgeBid> bidsList = new ArrayList<BridgeBid>();
		List<BridgeCard> cardsList = new ArrayList<BridgeCard>();
		for (int i = 0; i < bidsSplit.length; i++) {
			if (bidsTemp.length() > 0) {
				bidsList = GameBridgeRule.convertPlayBidsStringToList(bidsTemp, dealer);
			}
			assertEquals(GameBridgeRule.getNextPositionToPlay(dealer, bidsList, cardsList, null), bidsSplit[i].substring(2).charAt(0));
			bidsTemp += bidsSplit[i].substring(0, 2);
		}
		bidsTemp = "";
		for (int i = 0; i < bidsSplit.length; i++) {
			bidsTemp += bidsSplit[i].substring(0, 2);
		}
		bidsList = GameBridgeRule.convertPlayBidsStringToList(bidsTemp, dealer);
		BridgeBid contract = GameBridgeRule.getHigherBid(bidsList);
		contract.setOwner(GameBridgeRule.getWinnerBids(bidsList));
		for (int i = 0; i < cardsSplit.length; i++) {
			if (cardsTemp.length() > 0) {
				cardsList = GameBridgeRule.convertPlayCardsStringToList(cardsTemp, contract);
			}
			assertEquals(GameBridgeRule.getNextPositionToPlay(dealer, bidsList, cardsList, contract), cardsSplit[i].substring(2).charAt(0));
			cardsTemp += cardsSplit[i].substring(0, 2);
		}
	}
	
	public void testGetWinnersTrick() {
		String deal = "SENNNSSENSWEEESWWENNEWNWNSEWSEWNSSESWEWESWWWNNNWNSSE";
		char dealer = 'N';
		char vul = 'L';
		String bids= "PAN-1CE-PAS-1HW-PAN-2HE-PAS-3HW-PAN-4HE-PAS-PAW-PAN";
		String cards = "5CN-3CE-JCS-QCW-3HW-7HN-KHE-4HS-ACE-2CS-5DW-6CN-KCE-8CS-4SW-4CN-2HE-8HS-AHW-8DN-5SW-8SN-ASE-3SS-9CE-7CS-6HW-TCN-6SW-7SN-2SE-QSS-ADS-4DW-7DN-2DE-3DS-TDW-JDN-6DE-KDN-9DE-KSS-QDW-9SN-5HE-9HS-TSW-JHS-QHW-JSN-THE";
		String winners="WEEEWEWSSNNSW";
		String bidsTemp = "", cardsTemp = "";
		String[] bidsSplit = bids.split("-");
		for (int i = 0; i < bidsSplit.length; i++) {
			bidsTemp += bidsSplit[i].substring(0, 2);
		}
		String[] cardsSplit = cards.split("-");
		for (int i = 0; i < cardsSplit.length; i++) {
			cardsTemp += cardsSplit[i].substring(0, 2);
		}
		List<BridgeBid> bidsList = GameBridgeRule.convertPlayBidsStringToList(bidsTemp, dealer);
		BridgeBid contract = GameBridgeRule.getHigherBid(bidsList);
		contract.setOwner(GameBridgeRule.getWinnerBids(bidsList));
		List<BridgeCard> cardsList = GameBridgeRule.convertPlayCardsStringToList(cardsTemp, contract);
		List<BridgeCard> winnersCard = GameBridgeRule.getWinnersTrick(cardsList, contract);
		assertEquals(winnersCard.size(), winners.length());
		for (int i=0; i < winners.length(); i++) {
			assertEquals(winners.charAt(i), winnersCard.get(i).getOwner());
		}
	}
	
	public void testGetWinnersTrick2() {
		String cards = "5C3CJCQC3H7HKH4HAC2C5D6CKC8C4S4C2H8HAH8D5S8SAS3S9C7C6HTC6S7S2SQSAD4D7D2D3DTDJD6DKD9DKSQD9S5H9HTSJHQHJSTH";
		BridgeBid contract = BridgeBid.createBid("4H", 'W');
		assertNotNull(GameBridgeRule.getWinnersTrick(GameBridgeRule.convertPlayCardsStringToList("", contract), contract));
		assertEquals(0,GameBridgeRule.getWinnersTrick(GameBridgeRule.convertPlayCardsStringToList("5C3C", contract), contract).size());
		assertEquals(0,GameBridgeRule.getWinnersTrick(GameBridgeRule.convertPlayCardsStringToList("5C3CJC", contract), contract).size());
		assertEquals(1,GameBridgeRule.getWinnersTrick(GameBridgeRule.convertPlayCardsStringToList("5C3CJCQC", contract), contract).size());
		assertEquals(1,GameBridgeRule.getWinnersTrick(GameBridgeRule.convertPlayCardsStringToList("5C3CJCQC3H7HKH", contract), contract).size());
	}
	
//	public void testBizarre() {
//		String deal = "WNWWEWNNENWES" +
//				"NWSSSWNEEWEWE" +
//				"SSNWNWNSEWSWS" +
//				"SNNEESNEENSSE";
//		String dealer = "E";
//		String vul = "N";
//		String bids = "1SPA1NPA2D2H3DPAPAPA";
//		String game="";
//		List<BridgeBid> listBids = GameBridgeRule.convertPlayBidsStringToList(bids, dealer);
//		BridgeBid higher = GameBridgeRule.getHigherBid(listBids);
//		System.out.println("higher="+higher.toString());
//		String winner = GameBridgeRule.getWinnerBids(listBids);
//		System.out.println("winner="+winner);
//		BridgeBid declarer = BridgeBid.createBid(higher.toString(), winner);
//		List<BridgeCard> listCards = GameBridgeRule.convertPlayCardsStringToList(game, declarer);
//		List<BridgeCard> distrib = GameBridgeRule.convertCardDealToList(deal);
//	}
	
	public void testGetLastCardForPlayer() {
		List<BridgeCard> deal = GameBridgeRule.convertCardDealToList("WNEEEEEENNNENSSWSNSSWWSSWEWWWNNNESWSEWSNSNESEWEWWNNS");
		String cards = "6DAD2D4D4C9H2C3CAS8S4S5S3SJSQS7S5H8HJHKHKD6HQH3D5C5DTSTCAC6C6STDKS9S7D3H2H7H7CAHQD9D2S8CJDTHQCKC";
		BridgeBid contract = BridgeBid.createBid("5H", 'W');
		List<BridgeCard> listPlayed = GameBridgeRule.convertPlayCardsStringToList(cards, contract);
		BridgeCard cardW = GameBridgeRule.getLastCardForPlayer(listPlayed, deal, 'W');
		assertTrue(cardW.compareTo(BridgeCard.createCard("4H", 'W')) == 0);
		BridgeCard cardN = GameBridgeRule.getLastCardForPlayer(listPlayed, deal, 'N');
		assertTrue(cardN.compareTo(BridgeCard.createCard("JC", 'N')) == 0);
		BridgeCard cardS = GameBridgeRule.getLastCardForPlayer(listPlayed, deal, 'S');
		assertTrue(cardS.compareTo(BridgeCard.createCard("8D", 'S')) == 0);
		BridgeCard cardE = GameBridgeRule.getLastCardForPlayer(listPlayed, deal, 'E');
		assertTrue(cardE.compareTo(BridgeCard.createCard("9C", 'E')) == 0);
		
		// ERROR CASE
		String cards2 = "6DAD2D4D4C9H2C3CAS8S4S5S3SJSQS7S5H8HJHKHKD6HQH3D5C5DTSTCAC6C6STDKS9S7D3H2H7H7CAHQD9D2S8CJDTHQCKC4H";
		assertNull(GameBridgeRule.getLastCardForPlayer(GameBridgeRule.convertPlayCardsStringToList(cards2, contract), deal, 'W'));
		
		String cards3 = "6DAD2D4D4C9H2C3CAS8S4S5S3SJSQS7S5H8HJHKHKD6HQH3D5C5DTSTCAC6C6STDKS9S7D3H2H7H7CAHQD9D2S8CJDTHQCKC4HJC";
		assertNull(GameBridgeRule.getLastCardForPlayer(GameBridgeRule.convertPlayCardsStringToList(cards3, contract), deal, 'N'));
	}
	
	public void testGetOnlyOneCardForPlayerAndColor() {
		List<BridgeCard> deal = GameBridgeRule.convertCardDealToList("SSSSWWWNNEEEESSSSWWWNNNEEESSSSWWWNNNEEESSSSWWWNNNNEE");
		BridgeBid contract = BridgeBid.createBid("1C", 'N');
		List<BridgeCard> listPlayed = new ArrayList<BridgeCard>();
		assertTrue(GameBridgeRule.isBeginTrick(listPlayed));
		assertNull(GameBridgeRule.getOnlyOneCardForPlayerAndColor(listPlayed, deal, 'E', CardColor.Spade));
		listPlayed = GameBridgeRule.convertPlayCardsStringToList("KS2S3S4S", contract);
		assertTrue(GameBridgeRule.isBeginTrick(listPlayed));
		BridgeCard card = GameBridgeRule.getOnlyOneCardForPlayerAndColor(listPlayed, deal, 'E', CardColor.Spade);
		assertNotNull(card);
		listPlayed = GameBridgeRule.convertPlayCardsStringToList("KS2S", contract);
		assertFalse(GameBridgeRule.isBeginTrick(listPlayed));
		card = GameBridgeRule.getOnlyOneCardForPlayerAndColor(listPlayed, deal, 'E', CardColor.Spade);
		assertNotNull(card);
	}
	
	public void testGetFirstCardOnCurrentTrick() {
		BridgeBid contract = BridgeBid.createBid("1C", 'N');
		assertNull(GameBridgeRule.getFirstCardOnCurrentTrick(GameBridgeRule.convertPlayCardsStringToList("KS2S3S4S", contract)));
		BridgeCard card = GameBridgeRule.getFirstCardOnCurrentTrick(GameBridgeRule.convertPlayCardsStringToList("KS2S3S", contract));
		assertTrue(BridgeCard.createCard("KS", 'E').compareTo(card) == 0);
		card = GameBridgeRule.getFirstCardOnCurrentTrick(GameBridgeRule.convertPlayCardsStringToList("KS2S3S4S5S", contract));
		assertTrue(BridgeCard.createCard("5S", 'E').compareTo(card) == 0);
		card = GameBridgeRule.getFirstCardOnCurrentTrick(GameBridgeRule.convertPlayCardsStringToList("KS2S3S4SAS5S6S7SQS8S9STSJS", contract));
		assertTrue(BridgeCard.createCard("JS", 'E').compareTo(card) == 0);
		assertNull(GameBridgeRule.getFirstCardOnCurrentTrick(GameBridgeRule.convertPlayCardsStringToList("KS2S3S4SAS5S6S7SQS8S9STS", contract)));
		
		contract = BridgeBid.createBid("2S", 'N');
		BridgeCard bc = GameBridgeRule.getFirstCardOnCurrentTrick(GameBridgeRule.convertPlayCardsStringToList("5C6CTCQCQSKSAS", contract));
		assertEquals("QS", bc.toString());
		BridgeCard smallCard = GameBridgeRule.getSmallestCardForPlayerAndColor(GameBridgeRule.convertPlayCardsStringToList("5C6CTCQCQSKSAS", contract),
				GameBridgeRule.convertCardDealToList("SENESSWEWENSNNESEEWWENNENWWNNSEWSWSWEWSNSWSEWNSWNNES"),
				'W',
				bc.getColor());
		assertEquals("4S", smallCard.toString());
	}
	
	public void testIsEndBids() {
		assertFalse(GameBridgeRule.isEndBids("PAPAPA1C2C"));
		assertFalse(GameBridgeRule.isEndBids("PAPA1C2C"));
		assertFalse(GameBridgeRule.isEndBids("PAPAPA"));
		assertFalse(GameBridgeRule.isEndBids("1C2CPA"));
		assertTrue(GameBridgeRule.isEndBids("PAPAPA1CPAPAPA"));
		assertTrue(GameBridgeRule.isEndBids("PAPAPA1CPAPAPA"));
	}
	
	public void testIsBidsSequenceValid() {
		char dealer = 'N';
		String bids= "PA1CPA1HPA2HPA3HPA4HPAPAPA";
		assertTrue(GameBridgeRule.isBidsSequenceValid(GameBridgeRule.convertPlayBidsStringToList(bids, dealer)));
		bids= "PAPAPA";
		assertTrue(GameBridgeRule.isBidsSequenceValid(GameBridgeRule.convertPlayBidsStringToList(bids, dealer)));
		bids= "PAPAPAPA";
		assertTrue(GameBridgeRule.isBidsSequenceValid(GameBridgeRule.convertPlayBidsStringToList(bids, dealer)));
		bids= "PAPAPA1C";
		assertTrue(GameBridgeRule.isBidsSequenceValid(GameBridgeRule.convertPlayBidsStringToList(bids, dealer)));
		bids= "1NPA2DPA2HPAPAPA";
		assertTrue(GameBridgeRule.isBidsSequenceValid(GameBridgeRule.convertPlayBidsStringToList(bids, dealer)));
		
		bids= "PA1DPAPAPA1S";
		assertFalse(GameBridgeRule.isBidsSequenceValid(GameBridgeRule.convertPlayBidsStringToList(bids, dealer)));
		bids= "PAPAPA1C3C2CPAPAPA";
		assertFalse(GameBridgeRule.isBidsSequenceValid(GameBridgeRule.convertPlayBidsStringToList(bids, dealer)));
		bids= "PAPA1C1DX12S3H5DPAPAPAPA5H";
		assertFalse(GameBridgeRule.isBidsSequenceValid(GameBridgeRule.convertPlayBidsStringToList(bids, dealer)));
		bids= "PA1DPA1SPA2CPA4SPAPAPAPA";
		assertFalse(GameBridgeRule.isBidsSequenceValid(GameBridgeRule.convertPlayBidsStringToList(bids, dealer)));
		bids= "1CPA1H1SX12S3H3S4HPAPA4S5HPAPAPAPA";
		assertFalse(GameBridgeRule.isBidsSequenceValid(GameBridgeRule.convertPlayBidsStringToList(bids, dealer)));
		
		bids = "1SPAPAX1PA3CPA3S4S";
		assertTrue(GameBridgeRule.isBidsSequenceValid(GameBridgeRule.convertPlayBidsStringToList(bids, dealer)));
	}
	
	public void testGetNbTrickRemaining() {
		String deal = "SENNNSSENSWSSESWWENNEWNWNSEWSEWNSSESWEWESWWWNNNWNEEE";
		BridgeGame game =  BridgeGame.create(deal, 'S', 'A', "1CPAPAPA", "");
		assertEquals(13, GameBridgeRule.getNbTrickRemaining(game));
		game =  BridgeGame.create(deal, 'S', 'A', "1CPAPAPA", "AC");
		assertEquals(12, GameBridgeRule.getNbTrickRemaining(game));
		game =  BridgeGame.create(deal, 'S', 'A', "1CPAPAPA", "AC3C4CQC");
		assertEquals(12, GameBridgeRule.getNbTrickRemaining(game));
		game =  BridgeGame.create(deal, 'S', 'A', "1CPAPAPA", "ACTC9C");
		assertEquals(12, GameBridgeRule.getNbTrickRemaining(game));
		game =  BridgeGame.create(deal, 'S', 'A', "1CPAPAPA", "AC3C4CQC");
		assertEquals(12, GameBridgeRule.getNbTrickRemaining(game));
		game =  BridgeGame.create(deal, 'S', 'A', "1CPAPAPA", "AC3C4CQCKC");
		assertEquals(11, GameBridgeRule.getNbTrickRemaining(game));
	}

	public void testGetLastWinnerTrick01() {
        String bids= "1HN-PAE-2DS-PAW-3DN-PAE-4HS-PAW-4NNA-PAE-5HSA-PAW-6HN-PAE-PAS-PAW";
        String cards = "QCE-3CS-5CW-ACN-AHN-7HE-3HS-6HW-2HN-6SE-KHS-9HW-ASS-5SW-JSN-3SE-KSS-2SW-9CN-4SE-TDS-3DW-2DN-QDE-4DE-8DS-KDW-ADN-JDN-7SE-6DS-9DW-5DN-9SE-7DS-QHW";
        String bidsTemp = "", cardsTemp = "";
        String[] bidsSplit = bids.split("-");
        for (int i = 0; i < bidsSplit.length; i++) {
            bidsTemp += bidsSplit[i].substring(0, 2);
        }
        String[] cardsSplit = cards.split("-");
        for (int i = 0; i < cardsSplit.length; i++) {
            cardsTemp += cardsSplit[i].substring(0, 2);
        }
        char dealer = 'N';
        List<BridgeBid> bidsList = GameBridgeRule.convertPlayBidsStringToList(bidsTemp, dealer);
        BridgeBid contract = GameBridgeRule.getHigherBid(bidsList);
        contract.setOwner(GameBridgeRule.getWinnerBids(bidsList));
        List<BridgeCard> cardsList = GameBridgeRule.convertPlayCardsStringToList(cardsTemp, contract);
        List<BridgeCard> temp = new ArrayList<>();
        String winner = "";
        for (BridgeCard e : cardsList) {
            temp.add(e);
            if (temp.size() > 0 && temp.size()%4 == 0) {
                winner += GameBridgeRule.getLastWinnerTrick(temp, contract).getOwner();
            }
        }
        assertEquals("NNSSSENNW", winner);
    }

    public void testGetLastWinnerTrick02() {
        String bids= "1DN-1SE-1NS-PAW-PAN-2SE-PAS-PAW-PAN";
        String cards = "5HS-3HW-JHN-AHE-QSE-KSS-3SW-2SN-6HS-KHW-8HN-2CE-JDW-QDN-ADE-2DS-TSE-6SS-4SW-9SN-JSE-8SS-3CW-5CN-5DE-9DS-TDW-KDN-ACN-8CE-7CS-6CW-KCN-TCE-JCS-9CW-4CN-5SE-QCS-4HW-THS-7HW-9HN";
        String bidsTemp = "", cardsTemp = "";
        String[] bidsSplit = bids.split("-");
        for (int i = 0; i < bidsSplit.length; i++) {
            bidsTemp += bidsSplit[i].substring(0, 2);
        }
        String[] cardsSplit = cards.split("-");
        for (int i = 0; i < cardsSplit.length; i++) {
            cardsTemp += cardsSplit[i].substring(0, 2);
        }
        char dealer = 'N';
        List<BridgeBid> bidsList = GameBridgeRule.convertPlayBidsStringToList(bidsTemp, dealer);
        BridgeBid contract = GameBridgeRule.getHigherBid(bidsList);
        contract.setOwner(GameBridgeRule.getWinnerBids(bidsList));
        List<BridgeCard> cardsList = GameBridgeRule.convertPlayCardsStringToList(cardsTemp, contract);
        List<BridgeCard> temp = new ArrayList<>();
        String winner = "";
        for (BridgeCard e : cardsList) {
            temp.add(e);
            if (temp.size() > 0 && temp.size()%4 == 0) {
                winner += GameBridgeRule.getLastWinnerTrick(temp, contract).getOwner();
            }
        }
        assertEquals("ESWEEENNNE", winner);
    }
}	
