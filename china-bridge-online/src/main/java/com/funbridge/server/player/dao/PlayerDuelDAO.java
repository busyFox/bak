package com.funbridge.server.player.dao;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.player.data.PlayerDuel;
import com.funbridge.server.player.data.PlayerDuelStat;
import com.funbridge.server.ws.game.WSGamePlayer;
import com.funbridge.server.ws.player.WSPlayerDuel;
import com.funbridge.server.ws.tournament.WSDuelHistory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Repository(value="playerDuelDAO")
public class PlayerDuelDAO {
	@PersistenceContext
	private EntityManager em;
	
	protected Logger log = LogManager.getLogger(this.getClass());
	
	/**
	 * Return the playerDuel between 2 players
	 * @param plaID1
	 * @param plaID2
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public PlayerDuel getPlayerDuelBetweenPlayer(long plaID1, long plaID2) {
		Query q = em.createNamedQuery("playerDuel.selectBetweenPlayer");
		q.setParameter("plaID1", plaID1);
		q.setParameter("plaID2", plaID2);
		List<PlayerDuel> list = q.getResultList();
		if (list != null && list.size() > 0) {
			if (list.size() > 1) {
				log.error("Many duel for player1="+plaID1+" - player2="+plaID2);
			}
			return list.get(0);
		}
		return null;
	}
	
	/**
	 * Return list of playerDuel for a player
	 * @param plaID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<PlayerDuel> listDuelForPlayer(long plaID) {
		Query q = em.createNamedQuery("playerDuel.listForPlayer");
		q.setParameter("plaID", plaID);
		return q.getResultList();
	}
	
	/**
	 * Return playerDuel associated to this ID
	 * @param playerDuelID
	 * @return
	 */
	public PlayerDuel getPlayerDuelForID(long playerDuelID) {
		return em.find(PlayerDuel.class, playerDuelID);
	}
	
	/**
	 * Add new playerDuel in DB
	 * @param pd
	 */
	@Transactional
	public void persistPlayerDuel(PlayerDuel pd) {
		if (pd != null) {
			em.persist(pd);
		}
	}
	
	/**
	 * Update the playerDuel in DB
	 * @param pd
	 */
	@Transactional
	public void updatePlayerDuel(PlayerDuel pd) {
		if (pd != null) {
			em.merge(pd);
		}
	}
	
	/**
	 * Remove this playerDuel
	 * @param duelID
	 * @return
	 */
    @Transactional
	public boolean removePlayerDuel(long duelID) {
		try {
			Query q = em.createNamedQuery("playerDuel.deleteDuel");
			q.setParameter("duelID", duelID);
			return (q.executeUpdate() > 0);
		} catch (Exception e) {
			log.error("Exception to delete playerDuel with id="+duelID, e);
		}
		return false;
	}

	/**
	 * List all duels for a player. Add if necessary friend to complete the list to nbMax size.
	 * @param gamePlayer
	 * @param nbMax
	 * @return
	 */
	public List<WSDuelHistory> listDuelHistory(WSGamePlayer gamePlayer, int offset, int nbMax) {
		if (gamePlayer != null) {
            try {
				/*
				Exemple de requete :
				select p.id, p.nickname, p.lang, p.avatar_present, t.duel_id, t.duel_creation, t.duelStatusForPlayer, t.duelResetForPlayer, t.nb_played, t.nbWinPLayer, nbWinOther, t.date_last_duel, if (duelStatusForPlayer=0,0,(if (duelStatusForPlayer=1,4,if(duelStatusForPlayer=2,1,if(duelStatusForPlayer=3,3,if(duelStatusForPlayer=4,2,0)))))) as duelStatusOrder
				from (
				select * from (
					select (if (player1=2,player2,player1)) as playerFriend
					from (select * from player_link where (player1=2 or player2=2) and type_mask1&2=2)tempFriend) tblFriend
				left join
					(select (if (player1=2,player2,player1)) as playerDuel, id as duel_id, player1 as duel_pla1, player2 as duel_pla2, date_creation as duel_creation, duel_status, (if((duel_status=2 and player1=2) or (duel_status=3 and player2=2),2,if((duel_status=3 and player1=2)or(duel_status=2 and player2=2),3,duel_status))) as duelStatusForPlayer, (if((reset_request=1 and player1=2) or (duel_status=2 and player2=2),1,if((duel_status=2 and player1=2)or(duel_status=1 and player2=2),2,reset_request))) as duelResetForPlayer, nb_played, (if (player1=2,nb_win_pla1,nb_win_pla2)) as nbWinPlayer, (if (player1=2,nb_win_pla2,nb_win_pla1)) as nbWinOther, date_last_duel
					from (select * from player_duel where (player1=2 or player2=2))tempDuel)tblDuel
					on tblDuel.playerDuel=tblFriend.playerFriend

				union
				select * from (
					select (if (player1=2,player2,player1)) as playerFriend
					from (select * from player_link where (player1=2 or player2=2) and type_mask1&2=2)tempFriend) tblFriend
				right join
					(select (if (player1=2,player2,player1)) as playerDuel, id as duel_id, player1 as duel_pla1, player2 as duel_pla2, date_creation as duel_creation, duel_status, (if((duel_status=2 and player1=2) or (duel_status=3 and player2=2),2,if((duel_status=3 and player1=2)or(duel_status=2 and player2=2),3,duel_status))) as duelStatusForPlayer, (if((reset_request=1 and player1=2) or (duel_status=2 and player2=2),1,if((duel_status=2 and player1=2)or(duel_status=1 and player2=2),2,reset_request))) as duelResetForPlayer, nb_played, (if (player1=2,nb_win_pla1,nb_win_pla2)) as nbWinPlayer, (if (player1=2,nb_win_pla2,nb_win_pla1)) as nbWinOther, date_last_duel
					from (select * from player_duel where (player1=2 or player2=2))tempDuel)tblDuel
					on tblDuel.playerDuel=tblFriend.playerFriend
				)t join player p on (if (t.playerFriend is not null, playerFriend, playerDuel)=p.id)
				order by duelStatusOrder desc,
				case when duelStatusOrder in (1,2,3,4) then duel_creation else
                case when duelStatusOrder=0 and date_last_duel is null then 0 else -date_last_duel end end ASC,
                pseudo ASC
                limit 50;
                Use conditionnal order by to have sort by duel_creation for status 1,2,3,4 and sort by date_last_duel for status 0
				 */
                // ORDER : 4=REQUEST_RECEIVE - 3=PLAYING - 2=PLAYED - 1=REQUEST_SEND
                List<WSDuelHistory> listDuel = new ArrayList<>();
				String strQuery = "select p.id, p.nickname, p.lang, p.country_code, p.avatar_present, " +
						"t.duel_id, t.duel_creation, t.duelStatusForPlayer, t.duelResetForPlayer, t.nb_played, t.nbWinPLayer, nbWinOther, t.date_last_duel, " +
						"if (duelStatusForPlayer=0,0," +
                            "(if (duelStatusForPlayer=:duelStatusPlaying,3," +
                            "if(duelStatusForPlayer=:duelStatusRequestPlayer1,2," +
                            "if(duelStatusForPlayer=:duelStatusRequestPlayer2,4," +
                            "if(duelStatusForPlayer=:duelStatusPlayed,1,0)))))) as duelStatusOrder " +
						"from ";
				// list duels
				strQuery = prepareDuelHistoryQuery(strQuery);
				strQuery += " order by duelStatusOrder desc, " +
                        "case when duelStatusOrder in (1,2,3,4) then duel_creation else " +
                        "case when duelStatusOrder=0 and date_last_duel is null then 0 else -date_last_duel end end, " +
                        "nickname ASC limit :nbMax offset :offset";
				Query q = em.createNativeQuery(strQuery);

				q.setParameter("duelStatusPlaying", Constantes.PLAYER_DUEL_STATUS_PLAYING);
				q.setParameter("duelStatusRequestPlayer1", Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1);
				q.setParameter("duelStatusRequestPlayer2", Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2);
				q.setParameter("duelStatusPlayed", Constantes.PLAYER_DUEL_STATUS_PLAYED);
				q.setParameter("duelResetRequestPlayer1", Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER1);
				q.setParameter("duelResetRequestPlayer2", Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER2);
				q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
				q.setParameter("playerID", gamePlayer.playerID);
				q.setParameter("nbMax", nbMax);
				q.setParameter("offset", offset);
				List l = q.getResultList();
 				if (l != null && l.size() > 0) {
					for (int i = 0; i < l.size(); i++) {
						Object[] temp = (Object[])(l.get(i));
						if (temp.length == 14) {
							long friendID = ((BigInteger)temp[0]).longValue();
							String pseudo = (String)temp[1];
							String lang = (String)temp[2];
							String countryCode = (String)temp[3];
							boolean avatar = ((Boolean)temp[4]).booleanValue();
							WSDuelHistory duelHistory = new WSDuelHistory();
							duelHistory.player1 = gamePlayer;
							duelHistory.player2 = WSGamePlayer.createGamePlayerHuman(friendID, pseudo, avatar, lang, countryCode);
							if (temp[5] != null) {
								duelHistory.setPlayerDuelID(((BigInteger)temp[5]).longValue());
								duelHistory.creationDate = ((BigInteger)temp[6]).longValue();
								if(temp[7] instanceof BigInteger) {
									duelHistory.status = ((BigInteger) temp[7]).intValue();
								}
								int nbPlayed = ((Integer)temp[9]).intValue();
								if (temp[10] instanceof BigInteger) {
                                    duelHistory.nbWinPlayer1 = ((BigInteger) temp[10]).intValue();
                                } else if (temp[10] instanceof Integer) {
                                    duelHistory.nbWinPlayer1 = ((Integer)temp[10]).intValue();
                                }
                                if (temp[11] instanceof BigInteger) {
                                    duelHistory.nbWinPlayer2 = ((BigInteger) temp[11]).intValue();
                                } else if (temp[11] instanceof Integer) {
                                    duelHistory.nbWinPlayer2 = ((Integer) temp[11]).intValue();
                                }
								duelHistory.dateLastDuel = ((BigInteger)temp[12]).longValue();
								duelHistory.nbDraw = nbPlayed - duelHistory.nbWinPlayer1 - duelHistory.nbWinPlayer2;
							}
							listDuel.add(duelHistory);
						} else {
							log.error("Length of tab result is not valid ! temp length="+temp.length);
						}
					}
				}
                return listDuel;
			} catch (Exception e) {
				log.error("Exception to list duel history for gamePlayer="+gamePlayer+" - nbMax="+nbMax, e);
			}
		} else {
			log.error("Param gamePlayer is null");
		}
		return null;
	}

	/**
	 * return nb duel historic
	 * @param playerId
	 * @return
	 */
	public int countDuelHistoric(long playerId){
		Query q = em.createNativeQuery(prepareDuelHistoryQuery( "select count(p.id) from "));
		q.setParameter("duelStatusRequestPlayer1", Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1);
		q.setParameter("duelStatusRequestPlayer2", Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2);
		q.setParameter("duelResetRequestPlayer1", Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER1);
		q.setParameter("duelResetRequestPlayer2", Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER2);
		q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
		q.setParameter("playerID", playerId);
		return ((BigInteger) q.getSingleResult()).intValue();
	}

	/**
	 * Prepare duel history query (common part)
	 * @param strQuery
	 * @return
	 */
	private String prepareDuelHistoryQuery(String strQuery){
		String strQueryDuels = " select (if (player1=:playerID,player2,player1)) as playerDuel, " +
				"id as duel_id, player1 as duel_pla1, player2 as duel_pla2, date_creation as duel_creation, duel_status, " +
				"(if((duel_status=:duelStatusRequestPlayer1 and player1=:playerID) or (duel_status=:duelStatusRequestPlayer2 and player2=:playerID),:duelStatusRequestPlayer1,if((duel_status=:duelStatusRequestPlayer2 and player1=:playerID)or(duel_status=:duelStatusRequestPlayer1 and player2=:playerID),:duelStatusRequestPlayer2,duel_status))) as duelStatusForPlayer, " +
				"(if((reset_request & :duelResetRequestPlayer1=:duelResetRequestPlayer1 and player1=:playerID) or (reset_request & :duelResetRequestPlayer2=:duelResetRequestPlayer2 and player2=:playerID),:duelResetRequestPlayer1,if((reset_request & :duelResetRequestPlayer2=:duelResetRequestPlayer2 and player1=:playerID)or(reset_request & :duelResetRequestPlayer1=:duelResetRequestPlayer1 and player2=:playerID),:duelResetRequestPlayer2,reset_request))) as duelResetForPlayer, " +
				"nb_played, " +
				"(if (player1=:playerID,nb_win_pla1,nb_win_pla2)) as nbWinPlayer, " +
				"(if (player1=:playerID,nb_win_pla2,nb_win_pla1)) as nbWinOther, " +
				"date_last_duel " +
				"from (select * from player_duel where (player1=:playerID or player2=:playerID) and (nb_played > 0 or duel_status > 0))tempDuel";
		// list friends
		String strQueryFriends = "select (if (player1=:playerID,player2,player1)) as playerFriend " +
				"from (select * from player_link where (player1=:playerID or player2=:playerID) and type_mask1&:typeFriend=:typeFriend)tempFriend";

		strQuery += "(select * from (" + strQueryFriends + ")tblFriend left join (" + strQueryDuels + ")tblDuel on tblDuel.playerDuel=tblFriend.playerFriend";
		strQuery += " union ";
		strQuery += "select * from (" + strQueryFriends + ")tblFriend right join (" + strQueryDuels + ")tblDuel on tblDuel.playerDuel=tblFriend.playerFriend";
		strQuery += ") t join player p on (if (t.playerFriend is not null, playerFriend, playerDuel)=p.id)";
		strQuery += " where duelResetForPlayer!=:duelResetRequestPlayer1 ";
		return strQuery;
	}

	/**
	 * List WSDuelHistory for a player with only friend
	 * @param gamePlayer
	 * @param offset
	 * @param nbMax
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<WSDuelHistory> listDuelHistoryFriend(WSGamePlayer gamePlayer, int offset, int nbMax) {
		if (gamePlayer != null) {
			try {
				List<WSDuelHistory> listDuel = new ArrayList<WSDuelHistory>();
				String strQuery = "select p.id, p.nickname, p.lang, p.country_code, p.avatar_present, temp.duel_id, temp.duel_pla1, temp.duel_pla2, temp.duel_date_creation, temp.duel_status, temp.duel_reset, temp.duel_nb_played, temp.duel_nb_win_pla1, temp.duel_nb_win_pla2, temp.duel_date_last_duel ";
				strQuery +=	"from (" +
						"(select pl.player2 as pla_id, pd.id as duel_id, pd.player1 as duel_pla1, pd.player2 as duel_pla2, pd.date_creation as duel_date_creation, pd.duel_status as duel_status, pd.reset_request as duel_reset, pd.nb_played as duel_nb_played, pd.nb_win_pla1 as duel_nb_win_pla1, pd.nb_win_pla2 as duel_nb_win_pla2, pd.date_last_duel as duel_date_last_duel " +
						"from player_link pl left join player_duel pd on ((pd.player1=pl.player1 and pd.player2=pl.player2) or (pd.player2=pl.player1 and pd.player1=pl.player2)) where pl.player1=:playerID and (pl.type_mask1 & :typeFriend) = :typeFriend) " +
						"union " +
						"(select pl.player1 as pla_id, pd.id as duel_id, pd.player1 as duel_pla1, pd.player2 as duel_pla2, pd.date_creation as duel_date_creation, pd.duel_status as duel_status, pd.reset_request as duel_reset, pd.nb_played as duel_nb_played, pd.nb_win_pla1 as duel_nb_win_pla1, pd.nb_win_pla2 as duel_nb_win_pla2, pd.date_last_duel as duel_date_last_duel " +
						"from player_link pl left join player_duel pd on ((pd.player1=pl.player2 and pd.player2=pl.player1) or (pd.player2=pl.player2 and pd.player1=pl.player1)) where pl.player2=:playerID and (pl.type_mask1 & :typeFriend) = :typeFriend)" +
						")temp join player p on p.id=temp.pla_id order by p.nickname asc";
				Query q = em.createNativeQuery(strQuery);
				q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
				q.setParameter("playerID", gamePlayer.playerID);
                if (offset > 0) {q.setFirstResult(offset);}
				if (nbMax > 0) {q.setMaxResults(nbMax);}
				List l = q.getResultList();
				if (l != null && l.size() > 0) {
					for (int i = 0; i < l.size(); i++) {
						Object[] temp = (Object[])(l.get(i));
						if (temp.length == 15) {
							long friendID = ((BigInteger)temp[0]).longValue();
							String pseudo = (String)temp[1];
                            String lang = (String)temp[2];
							String countryCode = (String)temp[3];
							boolean avatar = ((Boolean)temp[4]).booleanValue();
							WSDuelHistory duelHistory = new WSDuelHistory();
							duelHistory.player1 = gamePlayer;
							duelHistory.player2 = WSGamePlayer.createGamePlayerHuman(friendID, pseudo, avatar, lang, countryCode);
							if (temp[5] != null) {
								duelHistory.setPlayerDuelID(((BigInteger)temp[5]).longValue());
								long duelPla1 = ((BigInteger)temp[6]).longValue();
								long duelPla2 = ((BigInteger)temp[7]).longValue();
								duelHistory.creationDate = ((BigInteger)temp[8]).longValue();
								int duelStatus = ((Integer)temp[9]).intValue();
								int resetRequest = ((Integer)temp[10]).intValue();
								int nbPlayed = ((Integer)temp[11]).intValue();
								int nbWinPla1 = ((Integer)temp[12]).intValue();
								int nbWinPla2 = ((Integer)temp[13]).intValue();
                                duelHistory.dateLastDuel = ((BigInteger)temp[14]).longValue();
								// status
								if (duelStatus == Constantes.PLAYER_DUEL_STATUS_PLAYING) {
									duelHistory.status = Constantes.PLAYER_DUEL_STATUS_PLAYING;
								}
								else if (duelStatus == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1) {
									if (duelPla1 == gamePlayer.playerID) {duelHistory.status = Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1;}
									else {duelHistory.status = Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2;}
								}
								else if (duelStatus == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2) {
									if (duelPla2 == gamePlayer.playerID) {duelHistory.status = Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1;}
									else {duelHistory.status = Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2;}
								}
								// nb win
								if (duelPla1 == gamePlayer.playerID) {duelHistory.nbWinPlayer1 = nbWinPla1;duelHistory.nbWinPlayer2 = nbWinPla2;}
								else{duelHistory.nbWinPlayer1 = nbWinPla2;duelHistory.nbWinPlayer2 = nbWinPla1;}
								// nb draw
								duelHistory.nbDraw = nbPlayed - nbWinPla1 - nbWinPla2;
							}
							listDuel.add(duelHistory);
						} else {
							log.error("Length of tab result is not valid ! temp length="+temp.length);
						}
					}
				}
				return listDuel;
			} catch (Exception e) {
				log.error("Exception to list duel history for gamePlayer="+gamePlayer+" - offset="+offset+" - nbMax="+nbMax, e);
			}
		} else {
			log.error("Param gamePlayer is null");
		}
		return null;
	}

	public List<Long> listOpponentsIDForPlayer(long playerID, String countryCode, int nbMax){
		List<Long> opponentsIDs = new ArrayList<>();
		try {
			String strQuery = "select opponents.id from (select (if (player1=:playerID,player2,player1)) as id " +
					"from (select * from player_duel where (player1=:playerID or player2=:playerID) and nb_played > 0) tempDuel) opponents " +
					"join player on opponents.id = player.id where opponents.id > 0";
			if(countryCode != null && !countryCode.isEmpty()){
				strQuery += " and country_code like :countryCode";
			}
			Query q = em.createNativeQuery(strQuery);
			q.setParameter("playerID", playerID);
			if(countryCode != null && !countryCode.isEmpty()){
				q.setParameter("countryCode",countryCode);
			}
			if (nbMax > 0) {q.setMaxResults(nbMax);}
			List listOpponents = q.getResultList();
			if (listOpponents != null && listOpponents.size() > 0) {
				for (int i = 0; i < listOpponents.size(); i++) {
                    BigInteger e = (BigInteger)listOpponents.get(i);
                    opponentsIDs.add(e.longValue());
				}
			}
		} catch (Exception e){
			log.error("Exception to list opponents for playerID="+playerID+" - countryCode="+countryCode+" - nbMax="+nbMax, e);
		}
		return opponentsIDs;
	}

	/**
	 * List playerDuel for playerID with status PLAYING 
	 * @param playerID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<PlayerDuel> listInProgressForPlayer(long playerID) {
		try {
			Query q = em.createNamedQuery("playerDuel.listWithStatusForPlayer");
			q.setParameter("playerID", playerID);
			q.setParameter("status", Constantes.PLAYER_DUEL_STATUS_PLAYING);
			return q.getResultList();
		} catch (Exception e) {
			log.error("Exception to list playerDuel in progress for playerID="+playerID, e);
		}
		return null;
	}
	
	/**
	 * List playerDuel waiting answer from playerID for request duel 
	 * @param playerID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<PlayerDuel> listRequestWaitingForPlayer(long playerID) {
		try {
			Query q = em.createNamedQuery("playerDuel.listRequestWaitingForPlayer");
			q.setParameter("playerID", playerID);
			q.setParameter("statusRequestPla1", Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1);
			q.setParameter("statusRequestPla2", Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2);
			return q.getResultList();
		} catch (Exception e) {
			log.error("Exception to list playerDuel request waiting for playerID="+playerID, e);
		}
		return null;
	}
	
	/**
	 * List playerDuel waiting answer from playerID for reset duel 
	 * @param playerID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<PlayerDuel> listResetWaitingForPlayer(long playerID) {
		try {
			Query q = em.createNamedQuery("playerDuel.listResetWaitingForPlayer");
			q.setParameter("playerID", playerID);
			q.setParameter("resetPla1", Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER1);
			q.setParameter("resetPla2", Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER2);
			return q.getResultList();
		} catch (Exception e) {
			log.error("Exception to list playerDuel reset waiting for playerID="+playerID, e);
		}
		return null;
	}
	
	/**
	 * Count playerDuel with status request and expired : status = REQUEST_PLAYER1 or REQUEST_PLAYER2 and dateCreation < dateCreationLimit
	 * @param dateCreationLimit
	 * @return
	 */
	public int countDuelRequestExpired(long dateCreationLimit) {
		try {
			Query q = em.createNamedQuery("playerDuel.countRequestDateExpired");
			q.setParameter("dateCreationLimit", dateCreationLimit);
			q.setParameter("statusRequestPla1", Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1);
			q.setParameter("statusRequestPla2", Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2);
			long result = (Long)q.getSingleResult();
			return (int)result;
		} catch (Exception e) {
			log.error("Exception to count duel with status request and date expired", e);
		}
		return 0;
	}
	
	/**
	 * Set status none for request expired : playerDuel with status = REQUEST_PLAYER1 or REQUEST_PLAYER2 and dateCreation < dateCreationLimit
	 * @param dateCreationLimit
	 */
    @Transactional
	public int updateDuelRequestExpired(long dateCreationLimit) {
		try {
			Query q = em.createNamedQuery("playerDuel.updateRequestDateExpired");
			q.setParameter("dateCreationLimit", dateCreationLimit);
			q.setParameter("statusRequestPla1", Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1);
			q.setParameter("statusRequestPla2", Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2);
			q.setParameter("statusNone", Constantes.PLAYER_DUEL_STATUS_NONE);
			return q.executeUpdate();
		} catch (Exception e) {
			log.error("Exception to update duel with status request and date expired", e);
		}
		return 0;
	}
	
	/**
	 * return stat for duels played by player
	 * @param playerID
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public PlayerDuelStat getStat(long playerID) {
		String strQuery = "select sum(nb_played) as nbPlayed, sum(nb_win) as nbWin, sum(nb_lost) as nbLost, max(date_last_duel) as dateLastDuel from " +
				"(select nb_played, if(player1=:plaID, nb_win_pla1,nb_win_pla2) as nb_win, if (player1=:plaID, nb_win_pla2, nb_win_pla1) as nb_lost, date_last_duel from player_duel " +
				"where player1=:plaID or player2=:plaID)t;";
		try {
			Query q = em.createNativeQuery(strQuery);
			q.setParameter("plaID", playerID);
			List l = q.getResultList();
			if (l != null && l.size() > 0) {
				Object[] temp = (Object[])l.get(0);
				if (temp != null && temp.length == 4) {
                    PlayerDuelStat stat = new PlayerDuelStat();
					if (temp[0] != null) {stat.nbPlayed = ((BigDecimal)temp[0]).intValue();}
					if (temp[1] != null) {stat.nbWin = ((BigDecimal)temp[1]).intValue();}
					if (temp[2] != null) {stat.nbLost = ((BigDecimal)temp[2]).intValue();}
                    if (temp[3] != null) {stat.dateLastDuel = ((BigInteger)temp[3]).longValue();}
                    return stat;
				}
			}
		} catch (Exception e) {
			log.error("Failed to get duel stat for playerID="+playerID, e);
		}
		return null;
	}

    /**
     * Count duels for a player with nb played > 0
     * @param playerID
     * @param excludeArgine
     * @return
     */
    public int countDuelsForPlayerWithNbPlayedSup0(long playerID, boolean excludeArgine) {
        try {
            Query q = null;
            if (excludeArgine) {
                q = em.createNamedQuery("playerDuel.countDuelsNoArgineForPlayerWithNbPlayedSup0");
            } else {
                q = em.createNamedQuery("playerDuel.countDuelsForPlayerWithNbPlayedSup0");
            }
            q.setParameter("plaID", playerID);
            long result = (Long)q.getSingleResult();
            return (int)result;
        } catch (Exception e) {
            log.error("Exception to count duel for player="+playerID, e);
        }
        return 0;
    }

    /**
     * List duels for a player
     * @param playerID
     * @param excludeArgine
     * @param offset
     * @param nbMax
     * @return
     */
    public List<WSPlayerDuel> listDuelsForPlayer(long playerID, boolean excludeArgine, int offset, int nbMax) {
        /*
        select (if (player1=2,player2,player1)) as playerPartner, nb_played, (if (player1=2,nb_win_pla1,nb_win_pla2)) as nbWin, (if (player1=2,nb_win_pla2,nb_win_pla1)) as nbLost from player_duel where (player1=2 or player2=2) order by date_last_duel desc;
         */
        String strQuery = "select p.id, p.nickname, p.avatar_present, p.display_country_code, t.nbWin, t.nbLost, t.nb_played from " +
                "(select (if (player1=:plaID,player2,player1)) as playerPartner, nb_played, (if (player1=:plaID,nb_win_pla1,nb_win_pla2)) as nbWin, (if (player1=:plaID,nb_win_pla2,nb_win_pla1)) as nbLost " +
                    "from player_duel where ";
        if (excludeArgine) {
            strQuery += "player1 != "+Constantes.PLAYER_ARGINE_ID+" and player2 != "+Constantes.PLAYER_ARGINE_ID+" and ";
        }
        strQuery += "(player1=:plaID or player2=:plaID) and nb_played > 0 order by nb_played desc, date_last_duel desc "+(nbMax>0?"limit "+nbMax:"")+" "+(offset > 0?"offset "+offset:"")+") t join " +
                "player p on t.playerPartner=p.id";

        Query q = em.createNativeQuery(strQuery);
        q.setParameter("plaID", playerID);
        List l = q.getResultList();
        List<WSPlayerDuel> results = new ArrayList<>();
        if (l != null && l.size() > 0) {
            for (int i = 0; i < l.size(); i++) {
                Object[] temp = (Object[]) (l.get(i));
                if (temp.length == 7) {
                    WSPlayerDuel e = new WSPlayerDuel();
                    e.playerID = ((BigInteger) temp[0]).longValue();
                    e.pseudo = (String) temp[1];
                    e.avatar = ((Boolean) temp[2]).booleanValue();
                    e.countryCode = (String) temp[3];
                    e.nbDuelWin = ((BigInteger) temp[4]).intValue();
                    e.nbDuelLost = ((BigInteger) temp[5]).intValue();
                    e.nbDuelDraw = (Integer) temp[6] - e.nbDuelWin - e.nbDuelLost;
                    if (e.nbDuelDraw < 0) {e.nbDuelDraw = 0;}
                    results.add(e);
                }
            }
        }
        return results;
    }
}
