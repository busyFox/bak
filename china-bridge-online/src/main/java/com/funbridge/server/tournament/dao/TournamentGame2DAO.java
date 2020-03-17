package com.funbridge.server.tournament.dao;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.data.TournamentGame2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Repository(value="tournamentGame2DAO")
public class TournamentGame2DAO {
	@PersistenceContext
	private EntityManager em;
	
	private Logger log = LogManager.getLogger(this.getClass());
	
	/**
	 * Create game in DB
	 * @param game
	 * @return
	 */
	public TournamentGame2 persistGame(TournamentGame2 game) {
		if (game != null) {
			try {
				em.persist(game);
				return game;
			} catch (Exception e) {
				log.error("Error persists game="+game,e);
			}
		}
		return null;
	}
	
	/**
	 * Return the game with this ID
	 * @param gameID
	 * @return
	 */
	public TournamentGame2 getForID(long gameID) {
		return em.find(TournamentGame2.class, gameID);
	}
	
	/**
	 * Update game in DB
	 * @param game
	 * @return
	 */
	public TournamentGame2 updateGame(TournamentGame2 game) {
		if (game != null) {
			try {
				if (log.isDebugEnabled()) {
					log.debug("update game="+game);
				}
				return em.merge(game);
			} catch (Exception e) {
				log.error("Exception to merge game="+game);
			}
		}
		return null;
	}
	
	/**
	 * Count game for deal
	 * @param dealID
	 * @return
	 */
	public int getNbGameForDeal(long dealID) {
		try {
			Query query = em.createNamedQuery("tournamentGame2.countNbGameForDeal");
			query.setParameter("dealID", dealID);
			long nbPlayer = (Long) query.getSingleResult();
			return (int) nbPlayer;
		} catch (Exception e) {
			// no data for this deal
		}
		return 0;
	}
	
	/**
	 * return the average score on this deal (Leaved games are excluded) 
	 * @param dealID
	 * @return
	 */
	public double getScoreAverageForDeal(long dealID) {
		double avg = 0;
		try {
			Query query = em.createNamedQuery("tournamentGame2.averageScoreForDeal");
			query.setParameter("dealID", dealID);
			query.setParameter("scoreToExclude", Constantes.GAME_SCORE_LEAVE);
			avg = (Double)query.getSingleResult();
		} catch (NoResultException e) {
			//log.warn("No result for this dealID="+dealID);
		} catch (NullPointerException e) {
			//log.warn("NullPointerException - No result for this dealID="+dealID);
		} catch (Exception e) {
			log.error("Error to compute average score on dealID="+dealID, e);
		}
		return avg;
	}
	
	/**
	 * List of game not finish and tournament is closed !
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<TournamentGame2> listNotFinishedAndTournamentClosed() {
		try {
			Query q = em.createNamedQuery("tournamentGame2.listNotFinishedAndTournamentFinished");
			return q.getResultList();
		} catch (Exception e) {
			log.error("Exception to list game not finished and tournament close", e);
		}
		return null;
	}
	
	/**
	 * List of game for deal
	 * @param dealID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<TournamentGame2> listForDeal(long dealID) {
		try {
			Query q = em.createNamedQuery("tournamentGame2.listForDeal");
			q.setParameter("dealID", dealID);
			return q.getResultList();
		} catch (Exception e) {
			log.error("Exception to list game for deal="+dealID, e);
		}
		return null;
	}
	
	public int deleteForTable(long tableID) {
		try {
			Query q = em.createNamedQuery("tournamentGame2.deleteForTable");
			q.setParameter("tableID", tableID);
			return q.executeUpdate();
		} catch (Exception e) {
			log.error("Exception to delete game for tableID="+tableID, e);
		}
		return -1;
	}
}
