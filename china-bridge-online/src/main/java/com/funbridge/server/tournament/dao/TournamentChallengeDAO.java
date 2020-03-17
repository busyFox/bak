package com.funbridge.server.tournament.dao;

import com.funbridge.server.tournament.data.TournamentChallenge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Repository(value="tournamentChallengeDAO")
public class TournamentChallengeDAO {
	@PersistenceContext
	private EntityManager em;
	
	private Logger log = LogManager.getLogger(this.getClass());

	/**
	 * Add new challenge
	 * @param tc
	 * @return
	 */
	public boolean addChallenge(TournamentChallenge tc) {
		if (tc != null) {
			try {
				em.persist(tc);
				return true;
			} catch (Exception e) {
				log.error("Error to persist tournamentChallenge : "+tc, e);
			}
		}
		return false;
	}
	
	/**
	 * Return tournament challenge for this ID
	 * @param tcID
	 * @return
	 */
	public TournamentChallenge getForID(long tcID) {
		return em.find(TournamentChallenge.class, tcID);
	}
	
	/**
	 * Update existing challenge
	 * @param tc
	 * @return
	 */
	public TournamentChallenge updateChallenge(TournamentChallenge tc) {
		if (tc != null) {
			try {
				return em.merge(tc);
			} catch (Exception e) {
				log.error("Error to merge tournamentChallenge : "+tc, e);
			}
		}
		return null;
	}
	
	/**
	 * List all challenge
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<TournamentChallenge> listAll() {
		try {
			Query q = em.createNamedQuery("tourChallenge.listAll");
			return q.getResultList();
		} catch (Exception e) {
			log.error("Error to list all", e);
		}
		return null;
	}
	
	/**
	 * Delete tournament challenge
	 * @param listDelete
	 * @return
	 */
	public boolean deleteTournamentChallenge(List<Long> listDelete) {
		if (listDelete != null && listDelete.size() > 0) {
			try {
				String strID = "";
				for (Long l : listDelete) {
					if (strID.length() > 0) {strID+=",";}
					strID += ""+l;
				}
				String strQuery = "delete from tournament_challenge where id in ("+strID+")";
				Query q = em.createNativeQuery(strQuery);
				q.executeUpdate();
				return true;
			} catch (Exception e) {
				log.error("Exception to delete challenge for list size="+listDelete.size(), e);
			}
		}
		return false;
	}
	
	/**
	 * delete list of tournamentChallenge where player is creator or partner
	 * @param playerID
	 * @return
	 */
	public int deleteForPlayer(long playerID) {
		try {
			Query q = em.createNamedQuery("tourChallenge.deleteForPlayer");
			q.setParameter("plaID", playerID);
			return q.executeUpdate();
		} catch (Exception e) {
			log.error("Error to delete for player="+playerID, e);
		}
		return -1;
	}
	
	/**
	 * Return list of tournamentChallenge where player is creator or partner
	 * @param playerID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<TournamentChallenge> listForPlayer(long playerID) {
		try {
			Query q = em.createNamedQuery("tourChallenge.listForPlayer");
			q.setParameter("plaID", playerID);
			return q.getResultList();
		} catch (Exception e) {
			log.error("Error to list for player="+playerID, e);
		}
		return null;
	}

	/**
	 * Return challenge for creator & partner
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<TournamentChallenge> getNotExpiredForPlayers(long pla1ID, long pla2ID) {
		try {
			Query q = em.createNamedQuery("tourChallenge.getNotExpiredForPlayers");
			q.setParameter("pla1", pla1ID);
			q.setParameter("pla2", pla2ID);
			q.setParameter("currentTS", System.currentTimeMillis());
			return q.getResultList();
		} catch (Exception e) {
			log.error("Error to get for pla1ID="+pla1ID+" - pla2ID="+pla2ID, e);
		}
		return null;
	}
	
	/**
	 * Return challenge for creator & partner
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<TournamentChallenge> getNotExpiredForPartner(long partnerID) {
		try {
			Query q = em.createNamedQuery("tourChallenge.getNotExpiredForPartner");
			q.setParameter("partner", partnerID);
			q.setParameter("currentTS", System.currentTimeMillis());
			return q.getResultList();
		} catch (Exception e) {
			log.error("Error to get for partner="+partnerID, e);
		}
		return null;
	}
	
	/**
	 * Return challenge for table
	 * @param tableID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public TournamentChallenge getForTable(long tableID) {
		try {
			Query q = em.createNamedQuery("tourChallenge.getForTable");
			q.setParameter("tableID", tableID);
			List<TournamentChallenge> l = q.getResultList();
			if (l != null && l.size() > 0) {
				if (l.size() > 1) {
					log.error("Many challenge for tableID="+tableID);
				}
				return l.get(0);
			}
		} catch (Exception e) {
			log.error("Error to get for tableID="+tableID, e);
		}
		return null;
	}
}
