package com.funbridge.server.tournament.dao;

import com.funbridge.server.tournament.data.TournamentCategory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository(value="tournamentCategoryDAO")
public class TournamentCategoryDAOJpa {
	@PersistenceContext
	private EntityManager em;
	
	private Logger log = LogManager.getLogger(this.getClass());
	
	public TournamentCategory getCategory(long categoryID) {
		return em.find(TournamentCategory.class, categoryID);
	}

	@SuppressWarnings("unchecked")
	public List<TournamentCategory> listAllCategory() {
		return (List<TournamentCategory>) em.createNamedQuery("tournamentCategory.listCategory").getResultList();
	}

}
