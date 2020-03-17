package com.funbridge.server.tournament.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.engine.ArgineProfile;
import com.gotogames.common.bridge.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="tournament_game2")
@NamedQueries({
	@NamedQuery(name="tournamentGame2.listNotFinishedAndTournamentFinished", query="select data from TournamentGame2 data where data.deal.tournament.finished=1 and data.finished=0"),
	@NamedQuery(name="tournamentGame2.listForDeal", query="select data from TournamentGame2 data where data.deal.ID=:dealID"),
	@NamedQuery(name="tournamentGame2.countNbGameForDeal", query="select count(data) from TournamentGame2 data where data.deal.ID=:dealID and data.finished=1"),
	@NamedQuery(name="tournamentGame2.averageScoreForDeal", query="select avg(data.score) from TournamentGame2 data where data.deal.ID=:dealID and data.finished=1 and data.score != :scoreToExclude"),
	@NamedQuery(name="tournamentGame2.deleteForTable", query="delete from TournamentGame2 data where data.table.ID=:tableID")
})
public class TournamentGame2 {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id", nullable=false)
	private long ID;
	
	@ManyToOne
	@JoinColumn(name="deal_id", nullable=false)
	private TournamentDeal deal;
	
	@ManyToOne
	@JoinColumn(name="table_id", nullable=false)
	private TournamentTable2 table = null;
	
	@Column(name="finished")
	private boolean finished = false;
	
	@Column(name="rank")
	private int rank;
	
	@Column(name="result")
	private double result;
	
	@Column(name="bids", length=256)
	private String bids = "";
	
	@Column(name="cards", length=256)
	private String cards = "";
	
	@Column(name="tricks_winner", length=13)
	private String tricksWinner = "";
	
	@Column(name="contract_type")
	private int contractType;
	
	@Column(name="contract", length=2)
	private String contract = "";
	
	@Column(name="declarer", length=1)
	private char declarer = BridgeConstantes.POSITION_NOT_VALID;
	
	@Column(name="tricks")
	private int tricks;
	
	@Column(name="score")
	private int score;
	
	@Column(name="convention")
	private String convention;
	
	@Column(name="start_date")
	private long startDate;
	
	@Column(name="last_date")
	private long lastDate;
	
	@Transient
	private BridgeBid bidContract = null;
	
	@Transient
	private List<BridgeBid> listBid = new ArrayList<BridgeBid>();
	
	@Transient
	private List<BridgeCard> listCard = new ArrayList<BridgeCard>();
	
	@Transient
	private boolean isEndBid;
	
	@Transient
	private boolean isReplay = false;
	
	@Transient
	private int modeRobot = Constantes.GAME_MODE_ROBOT_UNKNOWN; // 0 : unknown, 1 : normal, 2 : pass 
	
	@Transient
	private int conventionProfile = 0;
	
	@Transient
	private String conventionEngine = Constantes.GAME_CONVENTION_ENGINE_ARGINE;
	
	@Transient
	private String conventionData = "";
	
	public long getID() {
		return ID;
	}
	public void setID(long iD) {
		ID = iD;
	}
	
	public TournamentDeal getDeal() {
		return deal;
	}
	public void setDeal(TournamentDeal deal) {
		this.deal = deal;
	}
	
	public TournamentTable2 getTable() {
		return table;
	}
	public void setTable(TournamentTable2 tbl) {
		this.table = tbl;
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public int getRank() {
		return rank;
	}
	public void setRank(int rank) {
		this.rank = rank;
	}
	
	public double getResult() {
		return result;
	}
	public void setResult(double result) {
		this.result = result;
	}
	
	public String getBids() {
		return bids;
	}
	public void setBids(String bids) {
		this.bids = bids;
	}
	
	public String getCards() {
		return cards;
	}
	
	public void setCards(String cards) {
		this.cards = cards;
	}
	
	public String getTricksWinner() {
		return tricksWinner;
	}

	public void setTricksWinner(String tricksWinner) {
		this.tricksWinner = tricksWinner;
	}
	
	public int getContractType() {
		return contractType;
	}
	public void setContractType(int contractType) {
		this.contractType = contractType;
	}
	
	public String getContract() {
		return contract;
	}
	public void setContract(String contract) {
		this.contract = contract;
	}
	
	public char getDeclarer() {
		return declarer;
	}
	public void setDeclarer(char declarer) {
		this.declarer = declarer;
	}
	
	public int getTricks() {
		return tricks;
	}
	public void setTricks(int tricks) {
		this.tricks = tricks;
	}
	
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public long getStartDate() {
		return startDate;
	}

	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}

	public long getLastDate() {
		return lastDate;
	}

	public void setLastDate(long lastDate) {
		this.lastDate = lastDate;
	}

	public boolean isEndBid() {
		return isEndBid;
	}
	
	public String toString() {
		return "Game - id="+ID+" - dealID="+deal.getID()+" - table="+table.getID()+" - finished="+finished+" - contract="+contract+" contractType="+contractType+" - score="+score+" - bids="+bids+" - cards="+cards;
	}
	
	public boolean isLeaved() {
		return (score == Constantes.GAME_SCORE_LEAVE);
	}

	public int getNbTricks() {
		return tricksWinner.length();
	}
	
	public boolean isReplay() {
		return isReplay; 
	}
	
	public void setReplay(boolean value) {
		isReplay = value;
	}
	
	/**
	 * Set claim player to cards list and update tricks winner list
	 * @param playerWinner
	 */
	public void claimAllForPlayer(char playerWinner) {
		if (playerWinner != BridgeConstantes.POSITION_NOT_VALID) {
			cards += Constantes.GAME_INDICATOR_CLAIM+Character.toString(playerWinner);
			while (tricksWinner.length() < 13) {
				tricksWinner += Character.toString(playerWinner);
			}
		}
	}
	
	/**
	 * Set claim player for nb tricks.
	 * @param claimer
	 * @param nbTricks
	 */
	public void claimForPlayer(char claimer, int nbTricks) {
		if (claimer != BridgeConstantes.POSITION_NOT_VALID) {
			cards += Constantes.GAME_INDICATOR_CLAIM+Character.toString(claimer);
			cards += ""+nbTricks;
			// add claimer winner for nbTricks
			for (int i = 0; i < nbTricks; i++) {
				if (tricksWinner.length() < 13) {
					tricksWinner += Character.toString(claimer);
				}
			}
			// add other tricks win by next claimer
			char nextClaimer = GameBridgeRule.getNextPosition(claimer);
			while (tricksWinner.length() < 13) {
				tricksWinner += Character.toString(nextClaimer);
			}
		}
	}
	
	/**
	 * Return the number of tricks win by this player and partenaire
	 * @param player
	 * @return
	 */
	public int getNbTricksWinByPlayerAndPartenaire(char player) {
		int val = 0;
		if (tricksWinner.length() > 0) {
			for (int i = 0; i < tricksWinner.length(); i++) {
				if (tricksWinner.charAt(i) == player ||
						tricksWinner.charAt(i) == GameBridgeRule.getPositionPartenaire(player))
					val++;
			}
		}
		return val;
	}

	public String getConvention() {
		return convention;
	}
	public void setConvention(String convention) {
		this.convention = convention;
	}
	
	public String getBidListStrWithoutPosition() {
		String str = "";
		if (listBid != null) {
			for (BridgeBid bid : listBid) {
				str += bid.toString();
			}
		}
		return str;
	}
	
	public String getCardListStrWithoutPosition() {
		String str = "";
		if (listCard != null) {
			for (BridgeCard card : listCard) {
				str += card.toString();
			}
		}
		return str;
	}
	
	public void addTrickWinner(char winnerPosition) {
		tricksWinner += Character.toString(winnerPosition);
	}
	
	public String getContractWS() {
		String contractWS = "";
		if (isEndBid /*&& !isLeaved()*/) {
			contractWS = Constantes.contractToString(contract, contractType);
		}
		return contractWS;
	}
	
	public void addCard(BridgeCard card) {
		if (card != null) {
			cards+=BridgeTransformData.convertBridgeCard(card);
			listCard.add(card);
		}
	}
	
	public void addBid(BridgeBid bid) {
		if (bid != null) {
			bids+=BridgeTransformData.convertBridgeBid(bid);
			listBid.add(bid);
		}
	}
	
	public List<BridgeBid> getListBid() {
		return listBid;
	}
	
	public List<BridgeCard> getListCard() {
		return listCard;
	}
	
	public void setEndBid(boolean b) {
		isEndBid = b;
	}
	
	public BridgeBid getBidContract() {
		if (!isEndBid) {
			return null;
		} else {
			if (contractType == Constantes.CONTRACT_TYPE_PASS) {
				return BridgeBid.createBid(BridgeConstantes.BID_PASS, declarer);
			} else { 
				if (contract != null && contract.length() > 0 && declarer != BridgeConstantes.POSITION_NOT_VALID) {
					return BridgeBid.createBid(contract, declarer);
				}
			}
		}
		return null;
	}
	
	public String getCardsWS() {
		String temp = "";
		for (int i=0; i < cards.length(); i = i+2) {
			if (temp.length() > 0) {
				temp += "-";
			}
			if (cards.charAt(i) == Constantes.GAME_INDICATOR_CLAIM) {
				temp += cards.substring(i);
				// stop it is a claim
				break; 
			}
			else {
				temp += BridgeTransformData.convertBridgeCard2String(cards.charAt(i));
				temp += Character.toString(cards.charAt(i+1));
			}
		}
		return temp;
	}
	
	public String getBidsWS() {
		String temp = "";
		for (int i=0; i < bids.length(); i = i+2) {
			if (temp.length() > 0) {
				temp += "-";
			}
			temp += BridgeTransformData.convertBridgeBid2String(bids.charAt(i));
			temp += Character.toString(bids.charAt(i+1));
		}
		return temp;
	}
	
	@PostLoad
	public void onLoadGameData() throws Exception {
		parseConventionSelection();
        Logger log = LogManager.getLogger(this.getClass());
		// load list of bid played
		listBid.clear();
		if (bids != null && bids.length() > 0 && bids.length() % 2 == 0) {
			for (int i = 0; i < bids.length(); i = i+2) {
				BridgeBid bid = BridgeBid.createBid(BridgeTransformData.convertBridgeBid2String(bids.charAt(i)),
						bids.charAt(i+1));
				if (bid != null) {
					listBid.add(bid);
				} else {
					log.error("Error to load bid from bids = "+bids);
					throw new Exception("BID NOT VALID game="+bids);
				}
			}
		}
		// load list of car played
		listCard.clear();
		if (cards != null && cards.length() > 0) {
			for (int i = 0; i < cards.length(); i = i+2) {
				if (cards.charAt(i) == Constantes.GAME_INDICATOR_CLAIM) {
					// stop it is a claim !
					break;
				} else {
					BridgeCard card = BridgeCard.createCard(BridgeTransformData.convertBridgeCard2String(cards.charAt(i)),
							cards.charAt(i+1));
					if (card != null) {
						listCard.add(card);
					} else {
						log.error("Error to load card from cards = "+cards);
						throw new Exception("CARD NOT VALID game="+cards);
					}
				}
			}
		}
		isEndBid = GameBridgeRule.isBidsFinished(listBid);
	}
	
	public char getCurrentPlayer() {
		if (!finished) {
			return GameBridgeRule.getNextPositionToPlay(deal.getDistribution().getDealer(),
					listBid, listCard,
					getBidContract());
		}
		return BridgeConstantes.POSITION_NOT_VALID;
	}
	
	public int getNbBidCardPlayed() {
		return listBid.size() + listCard.size();
	}
	
	public int getStep() {
		return getNbBidCardPlayed();
	}
	
	public void resetData() {
		bidContract = null;
		bids = "";
		cards = "";
		contract = "";
		contractType = 0;
		declarer = BridgeConstantes.POSITION_NOT_VALID;
		isEndBid = false;
		finished = false;
		listBid.clear();
		listCard.clear();
		rank = 0;
		result = 0;
		score = 0;
		tricks = 0;
		tricksWinner = "";
	}
	
	public int getModeRobot() {
		return modeRobot;
	}
	public void setModeRobot(int modeRobot) {
		this.modeRobot = modeRobot;
	}
	
	public void setConventionSelection(String engine, int profile, String conv) {
		this.convention = engine+Constantes.GAME_CONVENTION_SEPARATOR+profile+Constantes.GAME_CONVENTION_SEPARATOR+conv;
		parseConventionSelection();
	}
	
	public void parseConventionSelection() {
		if (this.convention != null) {
            String[] temp = this.convention.split(Constantes.GAME_CONVENTION_SEPARATOR);
            if (temp.length >= 2) {
                conventionEngine = temp[0];
                conventionProfile = Integer.parseInt(temp[1]);
                // costel => get convention data from type
                if (conventionEngine.equals(Constantes.GAME_CONVENTION_ENGINE_COSTEL)) {
                    conventionData = BridgeConventions.getConventionStrForProfil(conventionProfile);
                }
                // argine => get convention data from type or if free profile, get convention from data
                else if (conventionEngine.equals(Constantes.GAME_CONVENTION_ENGINE_ARGINE)) {
                    ArgineProfile ap = ContextManager.getArgineEngineMgr().getProfile(conventionProfile);
                    if (ap != null) {
                        if (ap.isFree()) {
                            if (temp.length == 3) {
                                conventionData = temp[2];
                            }
                        } else {
                            conventionData = ap.value;
                        }
                    }
                }
            }
		}
	}

	public int getConventionProfile() {
		return conventionProfile;
	}

	public String getConventionData() {
		return conventionData;
	}
}
