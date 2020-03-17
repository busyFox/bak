package com.funbridge.server.tournament.dao;

import com.funbridge.server.tournament.data.TournamentDeal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.util.List;

@Repository(value="tournamentDealDAO")
public class TournamentDealDAOJpa {
	@PersistenceContext
	private EntityManager em;
	
	private Logger log = LogManager.getLogger(this.getClass());
	
	public TournamentDeal getDealForTournamentAndIndex(long tournamentID,
			int index) {
		try {
			Query query = em.createNamedQuery("tournamentDeal.selectForTournamentAndIndex");
			query.setParameter("dealIndex", index);
			query.setParameter("tourID", tournamentID);
			return (TournamentDeal) query.getSingleResult();
		} catch (NoResultException e) {
			// no deal found for this tournament and index
            if (log.isDebugEnabled()) {
                log.debug("No deal found for tournamentID:" + tournamentID + " and index:" + index);
            }
		} catch (NonUniqueResultException e) {
			// many deal for this tournament and index
			log.error("Many deals found for tournamentID:"+tournamentID+" and index:"+index);
		} catch (Exception e) {
			log.error("Exception to retrieve a deal on tournament : tourID="+tournamentID+" - index="+index);
		}
		return null;
	}

	public TournamentDeal getDeal(long id) {
		try {
			return em.find(TournamentDeal.class, id);
		} catch (Exception e) {
			log.error("Error to get deal="+id,e);
		}
		return null;
	}

	public boolean addDeal(TournamentDeal deal) {
		if (deal != null) {
			try {
				em.persist(deal);
				return true;
			} catch (Exception e) {
				log.error("Error to persist deal="+deal,e);
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public List<TournamentDeal> getDealForTournament(long tourID) {
		try {
			Query query = em.createNamedQuery("tournamentDeal.selectForTournament");
			query.setParameter("tourID", tourID);
			return (List<TournamentDeal>) query.getResultList();
		} catch (Exception e) {
			log.error("Exception to retrieve deals on tournament : tourID="+tourID);
		}
		return null;
	}

	public boolean delete(TournamentDeal deal) {
		if (deal != null) {
			try {
				em.remove(deal);
				return true;
			} catch (Exception e) {
				log.error("Exception on remove deal="+deal.getID());
			}
		}
		return false;
	}
	
	public boolean deleteAllForTournament(long tourID) {
		try {
			Query q = em.createNamedQuery("tournamentDeal.deleteForTournament");
			q.setParameter("tourID", tourID);
			q.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("Exception to delete dela for tourID="+tourID,e);
		}
		return false;
	}
}
