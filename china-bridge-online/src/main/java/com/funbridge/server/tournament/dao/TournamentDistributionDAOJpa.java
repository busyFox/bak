package com.funbridge.server.tournament.dao;

import com.funbridge.server.tournament.data.TournamentDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Repository(value="tournamentDistributionDAO")
public class TournamentDistributionDAOJpa {
	@PersistenceContext
	private EntityManager em;
	
	private Logger log = LogManager.getLogger(this.getClass());
	
	public boolean addDistribution(TournamentDistribution td) {
		if (td != null) {
			try {
				em.persist(td);
				return true;
			} catch (Exception e) {
				log.error("Error to persist distribution = "+td.toString(),e);
			}
		}
		return false;
	}

	public boolean delete(TournamentDistribution distrib) {
		if (distrib != null) {
			try {
				em.remove(distrib);
				return true;
			} catch (Exception e) {
				log.error("Exception on remove distrib="+distrib.getID(), e);
			}
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public List<TournamentDistribution> listForCards(String cards, int nbMax) {
		try {
			Query q = em.createNamedQuery("tournamentDistribution.selectForCards");
			q.setParameter("cards", cards);
			if (nbMax > 0) q.setMaxResults(nbMax);
			return q.getResultList();
		} catch (Exception e) {
			log.error("Error to list distribution for cards="+cards, e);
		}
		return null;
	}
}
