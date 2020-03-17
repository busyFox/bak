package com.gotogames.common.bridge;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Class to convert bridge game to PBN. 
 * Game => PBN
 * PBN => Game
 * See http://www.tistis.nl/pbn/
 * @author pascal
 *
 */
public class PBNConvertion {
	
	/**
	 * Return the value for this key or default value if not found. If the key is present in the map, the key is added to the list of used key
	 * @param map
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	private static String getMapDataValue(Map<String, String> map, String key, String defaultValue, List<String> listKeyUsed) {
		if (map != null) {
			if (map.containsKey(key)) {
                listKeyUsed.add(key);
				return map.get(key);
			}
			return defaultValue;
		}
		return defaultValue;
	}
	
	/**
	 * Convert vulnerability to PBN format
	 * @param vul
	 * @return
	 */
	private static String vulnerableGameToPBN(char vul) {
		switch (vul) {
		case 'L':
			return "None";
		case 'N':
			return "NS";
		case 'E':
			return "EW";
		case 'A':
			return "All";
		}
		return "None";
	}
	
	/**
	 * Convert vulnerability from PBN format
	 * @param vul
	 * @return
	 */
	public static char vulnerablePBNToGame(String vul) {
		if (vul.equalsIgnoreCase("None")) {
			return 'L';
		}
		if (vul.equalsIgnoreCase("NS")) {
			return 'N';
		}
		if (vul.equalsIgnoreCase("EW")) {
			return 'E';
		}
		if (vul.equalsIgnoreCase("All")) {
			return 'A';
		}
		return 'L';
	}
	
	/**
	 * Convert deal distribution from PBN format
	 * @param dealPbn
	 * @return
	 */
	public static String dealPBNToGame(String dealPbn) {
		char[] dealGame = new char[52];
		char dealer = dealPbn.charAt(0);
		dealPbn = dealPbn.substring(2);
		//dealPbn = dealPbn.substring(0, dealPbn.indexOf('"'));
		char[] plaOrder = new char[4];
		plaOrder[0] = dealer;
		plaOrder[1] = GameBridgeRule.getNextPosition(plaOrder[0]);
		plaOrder[2] = GameBridgeRule.getNextPosition(plaOrder[1]);
		plaOrder[3] = GameBridgeRule.getNextPosition(plaOrder[2]);
		String[] distribPlayer = dealPbn.split("\\s+");
		for (int p = 0; p < distribPlayer.length; p++) {
			String[] cardColorPlayer = distribPlayer[p].split("\\.");
			for (int c=0; c < cardColorPlayer.length; c++) {
				for (int i = 0; i < cardColorPlayer[c].length(); i++) {
					int idxDistrib = -1;
					switch (cardColorPlayer[c].charAt(i)) {
					case '2': idxDistrib = 0;break;
					case '3': idxDistrib = 1;break;
					case '4': idxDistrib = 2;break;
					case '5': idxDistrib = 3;break;
					case '6': idxDistrib = 4;break;
					case '7': idxDistrib = 5;break;
					case '8': idxDistrib = 6;break;
					case '9': idxDistrib = 7;break;
					case 'T': idxDistrib = 8;break;
					case 'J': idxDistrib = 9;break;
					case 'Q': idxDistrib = 10;break;
					case 'K': idxDistrib = 11;break;
					case 'A': idxDistrib = 12;break;
					}
					idxDistrib = ((3 - c) * 13) + idxDistrib;
					dealGame[idxDistrib] = plaOrder[p];
				}
				
			}
		}
		return new String(dealGame);
	}
	
	/**
	 * Convert game to PBN format
	 * @param distribution
	 * @return
	 */
	private static String dealGameToPBN(List<BridgeCard> distribution) {
		String dealPBN = "N:";
		List<BridgeCard> listCard = null;
		char[] tabPosition = new char[]{'N','E','S','W'};
		for (int i=0; i < tabPosition.length; i++) {
			if (i > 0) {
				dealPBN += " ";
			}
			listCard = GameBridgeRule.getRemainingCardsOnColorForPlayer(distribution, null, tabPosition[i], CardColor.Spade);
			if (listCard != null && listCard.size() > 0) {
				for (int j=listCard.size()-1; j>=0; j--) {
					dealPBN += Character.toString(listCard.get(j).getValue().getChar());
				}
			}
			dealPBN += ".";
			listCard = GameBridgeRule.getRemainingCardsOnColorForPlayer(distribution, null, tabPosition[i], CardColor.Heart);
			if (listCard != null && listCard.size() > 0) {
				for (int j=listCard.size()-1; j>=0; j--) {
					dealPBN += Character.toString(listCard.get(j).getValue().getChar());
				}
			}
			dealPBN += ".";
			listCard = GameBridgeRule.getRemainingCardsOnColorForPlayer(distribution, null, tabPosition[i], CardColor.Diamond);
			if (listCard != null && listCard.size() > 0) {
				for (int j=listCard.size()-1; j>=0; j--) {
					dealPBN += Character.toString(listCard.get(j).getValue().getChar());
				}
			}
			dealPBN += ".";
			listCard = GameBridgeRule.getRemainingCardsOnColorForPlayer(distribution, null, tabPosition[i], CardColor.Club);
			if (listCard != null && listCard.size() > 0) {
				for (int j=listCard.size()-1; j>=0; j--) {
					dealPBN += Character.toString(listCard.get(j).getValue().getChar());
				}
			}
//			dealPBN += ".";
		}
		return dealPBN;
	}
	
	/**
	 * Convert bid to PBN format
	 * @param bid
	 * @return
	 */
	private static String bidGameToPBN(BridgeBid bid) {
		String bidPBN = "";
		if (bid != null) {
			if (bid.isPass()) {
				return "Pass";
			}
			if (bid.isX1()) {
				return "X";
			}
			if (bid.isX2()) {
				return "XX";
			}
			if (bid.getColor() == BidColor.NoTrump) {
				return bid.getValue().getVal()+"NT";
			}
			return bid.toString();
		}
		return bidPBN;
	}
	
	/**
	 * Convert bid from PBN format
	 * @param bid
	 * @return
	 */
	private static String bidPBNToGame(String bid) {
		if (bid.equals("Pass")) return "PA";
		if (bid.equals("X")) return "X1";
		if (bid.equals("XX")) return "X2";
		if (bid.length() == 2) return bid;
		if (bid.length() == 3) return bid.substring(0,2);
		return null;
	}
	
	/**
	 * Convert card to PBN format
	 * @param card
	 * @return
	 */
	private static String cardGameToPBN(BridgeCard card) {
		String cardPBN = "-";
		if (card != null) {
			cardPBN = Character.toString(card.getColor().getChar())+Character.toString(card.getValue().getChar());
		}
		return cardPBN;
	}
	
	/**
	 * Convert card from PBN format
	 * @param cardPBN
	 * @return
	 */
	private static String cardPBNToGame(String cardPBN) {
		if (cardPBN != null && cardPBN.length() == 2) {
			return Character.toString(cardPBN.charAt(1))+Character.toString(cardPBN.charAt(0));
		}
		return "";
	}
	
	/***
	 * Return card index from winner trick
	 * @param card0
	 * @param card1
	 * @param card2
	 * @param card3
	 * @param contract
	 * @return
	 */
	private static int getTrickWinner(String card0, String card1, String card2, String card3, String contract) {
		BridgeCard bc0 = BridgeCard.createCard(card0, BridgeConstantes.POSITION_NOT_VALID);
		BridgeCard bc1 = BridgeCard.createCard(card1, BridgeConstantes.POSITION_NOT_VALID);
		BridgeCard bc2 = BridgeCard.createCard(card2, BridgeConstantes.POSITION_NOT_VALID);
		BridgeCard bc3 = BridgeCard.createCard(card3, BridgeConstantes.POSITION_NOT_VALID);
		BridgeBid bb = BridgeBid.createBid(contract, BridgeConstantes.POSITION_NOT_VALID);
		BridgeCard bcw = GameBridgeRule.getWinner(bc0, bc1, bc2, bc3, bb);
		if (bcw == bc0)return 0;
		if (bcw == bc1)return 1;
		if (bcw == bc2)return 2;
		if (bcw == bc3)return 3;
		return -1;
	}
	
	/**
	 * Transform game to PBN format
	 * @param game
	 * @param metadata
	 * @param separator
	 * @return
	 */
	public static String gameToPBN(BridgeGame game, Map<String, String> metadata, String separator) {
		String pbn = "";
		if (game != null) {
            List<String> listKeyUsed = new ArrayList<>();
			// Event
			pbn +="[Event \""+getMapDataValue(metadata, "event", "Funbridge", listKeyUsed)+"\"]"+separator;
			// Site
			pbn +="[Site \""+getMapDataValue(metadata, "site", "Funbridge", listKeyUsed)+"\"]"+separator;
			// Date
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
			String currentDate = sdf.format(new Date());
			pbn +="[Date \""+getMapDataValue(metadata, "date", currentDate, listKeyUsed)+"\"]"+separator;
			// Board
			pbn +="[Board \""+getMapDataValue(metadata, "board", "1", listKeyUsed)+"\"]"+separator;
			// West
			pbn +="[West \""+getMapDataValue(metadata, "west", "West", listKeyUsed)+"\"]"+separator;
			// North
			pbn +="[North \""+getMapDataValue(metadata, "north", "North", listKeyUsed)+"\"]"+separator;
			// East
			pbn +="[East \""+getMapDataValue(metadata, "east", "East", listKeyUsed)+"\"]"+separator;
			// South
			pbn +="[South \""+getMapDataValue(metadata, "south", "South", listKeyUsed)+"\"]"+separator;
            // Conventions
            pbn +="[Player_profile \""+getMapDataValue(metadata, "Player_profile", "Unknown", listKeyUsed)+"\"]"+separator;
            // Engine version
            pbn +="[EngineVersion \""+getMapDataValue(metadata, "engineVersion", "Unknown", listKeyUsed)+"\"]"+separator;
            // Dealer
			pbn +="[Dealer \""+Character.toString(game.dealerPosition)+"\"]"+separator;
			// Vulnerability
			pbn +="[Vulnerable \""+vulnerableGameToPBN(game.vulnerability)+"\"]"+separator;
			// Deal
			pbn += "[Deal \""+dealGameToPBN(game.distribution)+"\"]"+separator;

			if (game.contract != null) {
				// Declarer
				pbn += "[Declarer \""+Character.toString(game.contract.getOwner())+"\"]"+separator;
				// Contract
				pbn += "[Contract \""+bidGameToPBN(game.contract);
				if (GameBridgeRule.isX1(game.bidPlayed)) {
					pbn += "X";
				}
				if (GameBridgeRule.isX2(game.bidPlayed)) {
					pbn += "XX";
				}
				pbn += "\"]"+separator;
			} else {
				pbn += "*"+separator;
			}
            // Scoring
            pbn +="[Scoring \""+getMapDataValue(metadata, "scoring", "IMP", listKeyUsed)+"\"]"+separator;

            // add all data from amp with key not used
            if (metadata != null) {
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    if (!listKeyUsed.contains(entry.getKey()) && entry.getKey().length() > 1) {
                        pbn += "[" + entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1) + " \"" + entry.getValue() + "\"]"+separator;
                    }
                }
            }
			if (game.bidPlayed != null && game.bidPlayed.size() > 0) {
				// Auction
				pbn += "[Auction \""+Character.toString(game.dealerPosition)+"\"]"+separator;
				int nbBid = 0;
				for (BridgeBid bid : game.bidPlayed) {
					if (nbBid == 4) {
						pbn += separator;
						nbBid = 0;
					}
					if (nbBid > 0) {
						pbn += " ";
					}
					pbn += bidGameToPBN(bid);
					nbBid++;
				}
				pbn += separator;
			}
			
			if (game.cardPlayed != null && game.cardPlayed.size() > 0) {
				// Play
				pbn += "[Play \""+Character.toString(GameBridgeRule.getNextPosition(game.contract.getOwner()))+"\"]"+separator;
				char[] tabPosition = new char[4];
				tabPosition[0] = GameBridgeRule.getNextPosition(game.contract.getOwner());
				tabPosition[1] = GameBridgeRule.getNextPosition(tabPosition[0]);
				tabPosition[2] = GameBridgeRule.getNextPosition(tabPosition[1]);
				tabPosition[3] = GameBridgeRule.getNextPosition(tabPosition[2]);
				int nbTricks = game.cardPlayed.size() / 4 + (game.cardPlayed.size() % 4 > 0 ? 1 : 0);
				for (int i = 0; i < nbTricks; i++) {
					for (int j = 0; j < tabPosition.length; j++) {
						if (j > 0) pbn += " ";
						BridgeCard card = GameBridgeRule.getCardForPlayerAtTrick(game.cardPlayed, tabPosition[j], i);
						pbn += cardGameToPBN(card);
					}
					pbn += separator;
				}
				if (!GameBridgeRule.isEndGame(game.cardPlayed)) {
					pbn += "*"+separator;
				}
			} else {
				pbn += "*"+separator;
			}
		}
		return pbn;
	}
	
	public static BridgeGame PBNToGame(String pbn) {
		return PBNToGame(new BufferedReader(new StringReader(pbn)));
	}
	
	public static BridgeGame PBNToGame(File f) {
		try {
			return PBNToGame(new BufferedReader(new FileReader(f)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Transform game from PBN format
	 * @param reader
	 * @return
	 */
	public static BridgeGame PBNToGame(BufferedReader reader) {
		// on lit uniquement les champs dealer, vulnerabiliy, deal, auction et play
		String distrib = "", bidList = "",playList = "", contract = "", result = "";
		String event = "";
		char dealer = 'N', vul = 'A';
        Map<String, String> metaData = new HashMap<>();
//		BufferedReader reader = new BufferedReader(new StringReader(pbn));
		try {
			String line = reader.readLine();
			boolean bRead = true;
			if (line == null) {
				bRead = false;
			}
			while (bRead) {
				// EVENT
				if (line.startsWith("[Event \"")) {
					if (line.indexOf("\"]") > 0) {
						event = line.substring("[Event \"".length(), line.indexOf("\"]"));
					}
				}
				// DEALER
				else if (line.startsWith("[Dealer \"")) {
					dealer = line.charAt("[Dealer \"".length());
				}
				// VULNERABLE
				else if (line.startsWith("[Vulnerable \"")) {
					String vulnerable = line.substring("[Vulnerable \"".length());
					vulnerable = vulnerable.substring(0, vulnerable.indexOf('"'));
					vul = vulnerablePBNToGame(vulnerable);
				}
				// DEAL
				else if (line.startsWith("[Deal \"")) {
					String distribPbn = line.substring("[Deal \"".length());
					distrib = dealPBNToGame(distribPbn.substring(0, distribPbn.indexOf('"')));
				}
                // RESULT
                else if (line.startsWith("[Result \"")) {
                    result = line.substring("[Result \"".length());
                    result = result.substring(0, result.indexOf('"'));
                }
				// Contract
				else if (line.startsWith("[Contract \"")) {
					contract = bidPBNToGame(line.substring(line.indexOf('"')+1, line.lastIndexOf('"')));
				}
                // metadata
                else if (line.startsWith("[") && !line.startsWith("[Auction \"")) {
                    int idxValueStart = line.indexOf('"');
                    int idxValueEnd = line.indexOf("\"]");
                    if (idxValueStart > 1 && idxValueEnd > (idxValueStart+1)) {
                        String key = line.substring(1, idxValueStart).trim();
                        String value = line.substring(idxValueStart+1, idxValueEnd);
                        metaData.put(key, value);
                    }
                }
				line = reader.readLine();
				// attente balise auction
				if (line == null || line.startsWith("[Auction \"")) {
					bRead = false;
				}
			}
			
			// Bids
			if (line != null && line.startsWith("[Auction \"")) {
				line = reader.readLine();
				// Lecture jusque Play ou *
				while (line != null && !line.startsWith("[Play \"") && !line.startsWith("*")) {
					if (line.length() > 0) {
						String[] bidPbn = line.split("\\s+");
						for (int i = 0; i < bidPbn.length; i++) {
							String bid = bidPBNToGame(bidPbn[i]);
							if (bid != null) {
								bidList += bid;
							} else {
								throw new Exception("Failed to build bid with value="+bidPbn[i]);
							}
						}
					}
					line = reader.readLine();
				}
				// play card
				if (line != null && line.startsWith("[Play \"")) {
					// player position of trick beginnner
					int posTemp = 0;
					line = reader.readLine();
					// read until '*' or '['
					while (line != null && !line.startsWith("*") && line.length() > 0 && !line.startsWith("[") && posTemp >=0) {
						String[] cards = line.split("\\s+");
						if (cards.length == 4) {
							String[] cardsGame = new String[4];
							// convert card from PBN
							if (cards[0].trim().equals("-") || cards[0].trim().equals("+")) {
								cardsGame[0] = "";
							} else {
								cardsGame[0] = cardPBNToGame(cards[0]);
							}
							if (cards[1].trim().equals("-") || cards[1].trim().equals("+")) {
								cardsGame[1] = "";
							} else {
								cardsGame[1] = cardPBNToGame(cards[1]);
							}
							if (cards[2].trim().equals("-") || cards[2].trim().equals("+")) {
								cardsGame[2] = "";
							} else {
								cardsGame[2] = cardPBNToGame(cards[2]);
							}
							if (cards[3].trim().equals("-") || cards[3].trim().equals("+")) {
								cardsGame[3] = "";
							} else {
								cardsGame[3] = cardPBNToGame(cards[3]);
							}
							// add card to list
							if (cardsGame[posTemp].length() > 0) {
								playList += cardsGame[posTemp];
							} else {
								break;
							}
							if (cardsGame[(posTemp+1)%4].length() > 0) {
								playList += cardsGame[(posTemp+1)%4];
							} else {
								break;
							}
							if (cardsGame[(posTemp+2)%4].length() > 0) {
								playList += cardsGame[(posTemp+2)%4];
							} else {
								break;
							}
							if (cardsGame[(posTemp+3)%4].length() > 0) {
								playList += cardsGame[(posTemp+3)%4];
							} else {
								break;
							}

							// compute position of winner last trick
                            posTemp = (posTemp + getTrickWinner(cardsGame[posTemp], cardsGame[(posTemp+1)%4], cardsGame[(posTemp+2)%4], cardsGame[(posTemp+3)%4], contract))%4;
						}
						line = reader.readLine();
					}
				}
			}
			
			if (distrib.length() > 0) {
				BridgeGame bg = BridgeGame.create(distrib, dealer, vul, bidList, playList);
				bg.event = event;
                bg.result = result;
                bg.mapMetaData.putAll(metaData);
				return bg;
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) {
		//BridgeGame bg = PBNToGame(new File("/home/pascal/temp/pbn1.pbn"));
		//BridgeGame bg = PBNToGame(new File("/home/pascal/temp/pbn2.pbn"));
		//BridgeGame bg = PBNToGame(new File("/home/pascal/temp/Exemple1.pbn"));
		//BridgeGame bg = PBNToGame(new File("/home/pascal/temp/Exemple2.pbn"));
		//BridgeGame bg = PBNToGame(new File("/home/pascal/temp/Exemple3.pbn"));
		BridgeGame bg = PBNToGame(new File("/Users/pserent/temp/pbn/testClaim-5.pbn"));
		if (bg != null) {
			System.out.println(bg.toString());
		}
		
	}
}
