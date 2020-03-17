package com.funbridge.server.tournament.dao;

import com.funbridge.server.tournament.data.Tournament;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Repository(value="tournamentDAO")
public class TournamentDAOJpa {
	@PersistenceContext
	private EntityManager em;
	
	private Logger log = LogManager.getLogger(this.getClass());
	
	public Tournament getTournament(long tournamentID) {
		return em.find(Tournament.class, tournamentID);
	}

	public boolean addTournament(Tournament tour) {
		if (tour != null) {
			try {
				em.persist(tour);
				return true;
			} catch (Exception e) {
				log.error("Error to persist tournament = "+tour.toString(), e);
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public int getLastTournamentNumber() {
		Query query = em.createNamedQuery("tournament.listByNumberDesc");
		query.setMaxResults(1);
		List<Tournament> l = query.getResultList();
		if (l != null && l.size() ==1) {
			return l.get(0).getNumber();
		}
		return 0;
	}

	/**
	 * Return a list of tournament for this category and begin date < currentDate < end date. Order by date begin asc
	 * @param catID
	 * @param currentDate
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Tournament> listForCategoryInProgressOrderAsc(long catID, long currentDate) {
		try {
			Query query = em.createNamedQuery("tournament.listForCategoryInProgressOrderAsc");
			query.setParameter("curDate", currentDate);
			query.setParameter("catID", catID);
			return query.getResultList();
		} catch (Exception e) {
			log.error("Error to retrieve not finished tournament for currentDate="+currentDate, e);
		}
		return null;
	}
	


	/**
	 * Return list of tournament for category not finished and date end < date
	 * @param catID
	 * @param date
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Tournament> listForCategoryNotFinishedAfterDate(long catID, long date) {
		try {
			Query query = em.createNamedQuery("tournament.listForCategoryNotFinishedAfterDate");
			query.setParameter("catID", catID);
			query.setParameter("date", date);
			return query.getResultList();
		} catch (Exception e) {
			log.error("Error to retrieve not finished tournament for category="+catID+" and after date="+date,e);
		}
		return null;
	}
	

	public boolean delete(Tournament tour) {
		if (tour != null) {
			try {
				Query q = em.createNamedQuery("tournament.deleteTour");
				q.setParameter("tourID", tour.getID());
				q.executeUpdate();
				return true;
			} catch (Exception e) {
				log.error("Exception on remove tour="+tour.toString(), e);
			}
		}
		return false;
	}
	
	/**
	 * Return list of finished tournament on category with date end < dateRef. NbMax is the max number of element in list.
	 * @param categoryID
	 * @param dateRef
	 * @param nbMax
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Tournament> listFinishedForCategoryBeforeDateOrderAsc(long categoryID, long dateRef, int nbMax) {
		Query query = em.createNamedQuery("tournament.listFinishedForCategoryBeforeDateOrderAsc");
		query.setParameter("catID", categoryID);
		query.setParameter("date", dateRef);
		if (nbMax > 0) {
			query.setMaxResults(nbMax);
		}
		return query.getResultList();
	}
	
	/**
	 * Count finished tournament on category with date end < dateRef.
	 * @param categoryID
	 * @param dateRef
	 * @return
	 */
	public int countFinishedForCategoryBeforeDate(long categoryID, long dateRef) {
		try {
			Query query = em.createNamedQuery("tournament.countFinishedForCategoryBeforeDateOrderAsc");
			query.setParameter("catID", categoryID);
			query.setParameter("date", dateRef);
			return ((Long)query.getSingleResult()).intValue();
		} catch (Exception e) {
			log.error("Exception to count FinishedForCategoryBeforeDateOrderAsc for categoryID="+categoryID, e);
		}
		return 0;
	}
	
	/**
	 * Return list of finished tournament with date end < dateRef. NbMax is the max number of element in list.
	 * @param dateRef
	 * @param nbMax
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Tournament> listFinishedBeforeDateOrderAsc(long dateRef, int nbMax) {
		Query query = em.createNamedQuery("tournament.listFinishedBeforeDateOrderAsc");
		query.setParameter("date", dateRef);
		if (nbMax > 0) {
			query.setMaxResults(nbMax);
		}
		return query.getResultList();
	}
}
