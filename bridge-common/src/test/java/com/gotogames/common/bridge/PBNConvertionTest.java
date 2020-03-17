package com.gotogames.common.bridge;

import junit.framework.TestCase;

public class PBNConvertionTest extends TestCase {
	
	public void testGameToPBN() {
		String deal = "ENNNWNNENWEWSEWEWEWESWWESWWEENNSNSWNSESESSSEWNWSNSNS";
		String bids = "PAPA2C2DPAPAPA";
		String cards = "3C2CAC6CAH2H5H3HAS7S8S2S7HTH6HKH6S3S9SJS8H4H9H3D";
		char dealer = 'N';
		char vulnerability = 'L';
		BridgeGame bg = BridgeGame.create(deal, dealer, vulnerability, bids, cards);
		String pbn = PBNConvertion.gameToPBN(bg, null, "\r\n");
		System.out.println(pbn);
	}

    public void testGameToPBN2() {
        String deal = "NSESSSWNSNNWEESNEWENWEEWSWSEWNNNSNNSSSWESENWWEWNEWWE";
        String bids = "PAPAPA1SPA2C2H3D4H4NPA5CPA5DPA6CX1";
        char dealer = 'N';
        char vulnerability = 'L';
        BridgeGame bg = BridgeGame.create(deal, dealer, vulnerability, bids, null);
        String pbn = PBNConvertion.gameToPBN(bg, null, "\r\n");
        System.out.println(pbn);
    }

    public void testGameToPBN3() {
        String deal = "SWESNNNEENSEEWWNESSENENWSSNSWWEWESWSNENNWNWSSNWESEWW";
        String bids = "PAPA1CPA1SPA1NPA2HPA2SPAPAPA";
        String cards = "2STSJSASKS4SQS6S9S8S4C7S3S4D9CQC6D2D9D5D6CTC2C3CKC5C3D7CAC7DQD8C8DKD4HJDAD5H";
        char dealer = 'W';
        char vulnerability = 'L';
        BridgeGame bg = BridgeGame.create(deal, dealer, vulnerability, bids, cards);
        String pbn = PBNConvertion.gameToPBN(bg, null, "\r\n");
        System.out.println(pbn);
    }
	
	public void testPBNToGame() {
		String pbn = "[Event \"Funbridge\"]\r\n" +
				"[Site \"Funbridge\"]\r\n"+
				"[Date \"2011.08.03\"]\r\n"+
				"[Board \"\"1]\r\n"+
				"[Dealer \"N\"]\r\n"+
				"[Vulnerable \"None\"]\r\n"+
				"[Deal \"N:KJ8.J865..T87543 62.K43.Q8642.Q92 AQT543.AQ97.K9.A 97.T2.AJT753.KJ6\"]\r\n"+
				"[Declarer \"W\"]\r\n" +
				"[Contract \"2D\"]\r\n" +
				"[Auction \"N\"]\r\n" +
				"Pass Pass 2C 2D\r\n" +
				"Pass Pass Pass\r\n" +
				"[Play \"N\"]\r\n" +
				"C3 C2 CA C6\r\n" +
				"H5 H3 HA H2\r\n" +
				"S8 S2 SA S7\r\n" +
				"H6 HK H7 HT\r\n" +
				"SJ S6 S3 S9\r\n" +
				"H8 H4 H9 D3\r\n" +
				"*\r\n" +
				"[Scoring \"IMP\"]";
		BridgeGame bg = PBNConvertion.PBNToGame(pbn);
		assertNotNull(bg);
		assertTrue(bg.isBidListValid());
		assertTrue(bg.isCardListValid());
		assertEquals("ENNNWNNENWEWSEWEWEWESWWESWWEENNSNSWNSESESSSEWNWSNSNS", bg.getDistributionString());
	}
	
	public void testPBNToGame2() {
		String pbn="[Dealer \"E\"]\r\n"+
		 "[Vulnerable \"NONE\"]\r\n"+
		 "[Deal \"E:T65..J.JT9876543 874.KQ6.AT83.AKQ AQJ92.J752.KQ72. K3.AT9843.9654.2\"]\r\n"+
		 "[Contract \"4H\"]\r\n"+
		 "[Declarer \"N\"]\r\n"+
		 "[Result \"6\"]\r\n"+
		 "[Auction \"E\"]\r\n"+
		 "Pass 1D 1S X\r\n"+
		 "2S X Pass 4H\r\n"+
		 "Pass Pass Pass\r\n"+
		 "[Play \"E\"]\r\n"+
		 "S5 S4 SA SK\r\n"+
		 "ST S7 S2 S3\r\n"+
		 "C3 CA H7 C2\r\n"+
		 "S6 S8 S9 H3\r\n"+
		 "C4 HK H2 H4\r\n"+
		 "C5 HQ H5 H8\r\n"+
		 "C6 H6 HJ HA\r\n"+
		 "*";
		
		BridgeGame bg = PBNConvertion.PBNToGame(pbn);
		assertNotNull(bg);
		assertTrue(bg.isBidListValid());
		assertTrue(bg.isCardListValid());
		System.out.println(bg.getDistributionString());
		assertEquals("NEEEEEEEEESSSWSNNNWSNSEWWSWNNWSWNNNWSSNWNSEESSWEWWNW", bg.getDistributionString());
	}
	
	public void testPBNToGame3() {
		String pbn="[Dealer \"W\"]\r\n"+
		 "[Vulnerable \"ALL\"]\r\n"+
		 "[Deal \"N:AKQT8.84.K86.T83 J975.AJT9.T73.76 3.7.Q542.AKQJ542 642.KQ6532.AJ9.9\"]\r\n"+
		 "[Auction \"W\"]\r\n"+
		 "1N 2S Pass Pass\r\n"+
		 "\r\n";
		
		BridgeGame bg = PBNConvertion.PBNToGame(pbn);
		assertNotNull(bg);
		assertTrue(bg.isBidListValid());
		assertTrue(bg.isCardListValid());
		assertNotNull(bg.getBidList());
		assertEquals(4, bg.getBidList().size());
		assertEquals('A', bg.getVulnerability());
		assertEquals('W', bg.getDealer());
	}
	
	public void testPBNToGame4() {
		String pbn="[Dealer \"S\"]\r\n"+
				"[Vulnerable \"ALL\"]\r\n"+
				"[Deal \"S:AKQT8.84.K86.T83 J975.AJT9.T73.76 3.7.Q542.AKQJ542 642.KQ6532.AJ9.9\"]\r\n"+
				"[Contract \"4C\"]\r\n"+
				"[Auction \"S\"]\r\n"+
				"Pass Pass 1NT Pass\r\n"+
				"2S 3H 3S Pass\r\n"+
				"4C Pass Pass Pass\r\n"+
				"[Play \"W\"]\r\n"+
				"D3 D2 DJ DK\r\n"+
				"- - - SA";
		BridgeGame bg = PBNConvertion.PBNToGame(pbn);
		assertNotNull(bg);
		assertNotNull(bg.getBidList());
		assertTrue(bg.isBidListValid());
		assertTrue(bg.isCardListValid());
		assertEquals(5, bg.getCardList().size());
	}
	
	public void testPBNToGame5() {
		String pbn="[Dealer \"N\"]\r\n"+
				"[Vulnerable \"None\"]\r\n"+
				"[Deal \"N:A32.975.T53.KQ84 JT96.J42.QJ9.T76 KQ7.863.AK72.AJ5 854.AKQT.864.932\"]\r\n"+
				"[Contract \"3NT\"]\r\n"+
				"[Auction \"N\"]\r\n"+
				"Pass Pass 1NT Pass\r\n"+
				"2NT Pass 3D Pass\r\n"+
				"3NT Pass Pass Pass\r\n";
		BridgeGame bg = PBNConvertion.PBNToGame(pbn);
		assertNotNull(bg);
		assertNotNull(bg.getBidList());
		assertTrue(bg.isBidListValid());
		assertTrue(bg.getBidList().get(8).getStringWithOwner().equals("3NN"));
	}
	
	public void testPBNToGame6() {
		String distrib = "SWNNWSSSENWWSNNESEESSNWWNWSEENSEWNNSEEWNWNEESEWNESWW";
		String bids = "PA2CPA2DPA2NPA3CPA3SPA4SPAPAPA";
		String cards = "TH3H6HAH8H9HQHJHTCAC3C4C2H6C5H7H5S7SKS4SQC5C4D2CKCJC6D9CAD3D7D5DJD2D6S9D8SQSAS2S3STSJS";
		BridgeGame bg = BridgeGame.create(distrib, 'S', 'L', bids, cards);
		assertNotNull(bg);
		assertEquals("4S", bg.getContract().toString());
		assertEquals('W', bg.getDeclarer());
		String pbn = PBNConvertion.gameToPBN(bg, null, "\r");
		assertNotNull(pbn);
		System.out.println(pbn);
	}


    public void testDistributionRemaining1() {
        String deal = "ENNNWNNENWEWSEWEWEWESWWESWWEENNSNSWNSESESSSEWNWSNSNS";
        String bids = "PAPA2C2DPAPAPA";
        String cards = "3C2CAC6CAH2H5H3HAS7S8S2S7HTH6HKH6S3S9SJS8H4H9H3D";
        char dealer = 'N';
        char vulnerability = 'L';
        BridgeGame bg = BridgeGame.create(deal, dealer, vulnerability, bids, cards);
        assertNotNull(bg);
        assertEquals("--NN-NNENWEW-E-EWEWESWWESW---------NS----SS----S-SN-", bg.getDistributionStringRemaining('-'));
    }

    public void testDistributionRemaining2() {
        String deal = "ENNNWNNENWEWSEWEWEWESWWESWWEENNSNSWNSESESSSEWNWSNSNS";
        String bids = "PAPA2C2DPAPAPA";
        String cards = "3C2CAC6CAH2H5H3HAS7S8S2S";
        char dealer = 'N';
        char vulnerability = 'L';
        BridgeGame bg = BridgeGame.create(deal, dealer, vulnerability, bids, cards);
        assertNotNull(bg);
        assertEquals("--NN-NNENWEW-EWEWEWESWWESW--E-NSNSWNSE--SSSE--WSNSN-", bg.getDistributionStringRemaining('-'));
    }

    public void testDistributionRemaining3() {
        String deal = "ENNNWNNENWEWSEWEWEWESWWESWWEENNSNSWNSESESSSEWNWSNSNS";
        String bids = "PAPA2C2DPAPAPA";
        String cards = "";
        char dealer = 'N';
        char vulnerability = 'L';
        BridgeGame bg = BridgeGame.create(deal, dealer, vulnerability, bids, cards);
        assertNotNull(bg);
        assertEquals("ENNNWNNENWEWSEWEWEWESWWESWWEENNSNSWNSESESSSEWNWSNSNS", bg.getDistributionStringRemaining('-'));
    }

}
