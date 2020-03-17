package com.funbridge.server.tournament.dao;

import com.funbridge.server.player.data.Player;
import com.funbridge.server.tournament.data.Tournament;
import com.funbridge.server.tournament.data.TournamentTable2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Repository(value="tourTable2DAO")
public class TournamentTable2DAO {
	@PersistenceContext
	private EntityManager em;
	
	private Logger log = LogManager.getLogger(this.getClass());

	/**
	 * Create table
	 * @param tour
	 * @param player
	 * @param partner
	 * @return
	 */
	public TournamentTable2 createTable(Tournament tour, Player player, Player partner) {
		if (tour != null && player != null && partner != null) {
			TournamentTable2 table = new TournamentTable2();
			table.setCreatorID(player.getID());
			table.setPlayerSouth(player);
			table.setPlayerNorth(partner);
			table.setPlayerEast(null);
			table.setPlayerWest(null);
			table.setTournament(tour);
			try {
				em.persist(table);
				return table;
			} catch (Exception e) {
				log.error("Error persist table for tournament:"+tour.getID()+" - player:"+player.getID()+" - partner:"+partner.getID(),e);
			}
		}
		return null;
	}
	
	/**
	 * Update table
	 * @param table
	 * @return
	 */
	public TournamentTable2 updateTable(TournamentTable2 table) {
		if (table != null) {
			try {
				if (log.isDebugEnabled()) {
					log.debug("update table="+table);
				}
				return em.merge(table);
			} catch (Exception e) {
				log.error("Exception to merge table="+table);
			}
		}
		return null;
	}
	
	/**
	 * Retrieve table with ID
	 * @param tableID
	 * @return
	 */
	public TournamentTable2 getTableForID(long tableID) {
		try {
			return em.find(TournamentTable2.class, tableID);
		} catch (Exception e) {
			log.error("Exception to search table with id="+tableID);
		}
		return null;
	}
	
	/**
	 * Return list of table for tournament not finished
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<TournamentTable2> listForTournamentNotFinished() {
		try {
			Query q = em.createNamedQuery("tournamentTable2.listForTournamentNotFinished");
			return q.getResultList();
		} catch (Exception e) {
			log.error("Exception to list for tournament not finished", e);
		}
		return null;
	}
	
	/**
	 * Return list of table where player is present
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<TournamentTable2> listForPlayer(long playerID) {
		try {
			Query q = em.createNamedQuery("tournamentTable2.listForPlayer");
			q.setParameter("plaID", playerID);
			return q.getResultList();
		} catch (Exception e) {
			log.error("Exception to list table for player="+playerID, e);
		}
		return null;
	}
}
