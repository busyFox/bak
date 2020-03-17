package com.funbridge.server.tournament;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.tournament.category.TournamentType;
import com.funbridge.server.tournament.dao.*;
import com.funbridge.server.tournament.data.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Component(value="tournamentMgr")
@Scope(value="singleton")
public class TournamentMgr extends FunbridgeMgr{
	@Resource(name="tournamentDAO")
	private TournamentDAOJpa tournamentDAO = null;
	@Resource(name="tournamentCategoryDAO")
	private TournamentCategoryDAOJpa tournamentCategoryDAO = null;
	@Resource(name="tournamentDealDAO")
	private TournamentDealDAOJpa tournamentDealDAO = null;
	@Resource(name="tournamentGame2DAO")
	private TournamentGame2DAO tournamentGame2DAO = null;
	@Resource(name="tournamentDistributionDAO")
	private TournamentDistributionDAOJpa tournamentDistribDAO = null;
	@Resource(name="tourTable2DAO")
	private TournamentTable2DAO tournamentTable2DAO = null;
	
	public TournamentMgr() {
        if (log.isDebugEnabled()) {
            log.debug("TournamentMgr - creation instance");
        }
	}

	/**
	 * Call by spring on initialisation of bean
	 */
	@PostConstruct
	@Override
	public void init() {

	}
	
	@PreDestroy
	@Override
	public void destroy() {
	}
	
	
	@Override
	public void startUp() {
		
	}

	/**
	 * Return list of tournament finished and end_date < tsDateBefore
	 * @param nbMax
	 * @return
	 */
	public List<Tournament> getTournamentListFinishedBeforeDateOrderAsc(int nbMax, long tsDateBefore) {
		return tournamentDAO.listFinishedBeforeDateOrderAsc(tsDateBefore, nbMax);
	}
	
	/**
	 * Return list of tournament of category finished and end_date < tsDateBefore
	 * @param catID
	 * @param nbMax
	 * @return
	 */
	public List<Tournament> getTournamentListFinishedForCategoryBeforeDateOrderAsc(long catID, int nbMax, long tsDateBefore) {
		return tournamentDAO.listFinishedForCategoryBeforeDateOrderAsc(catID, tsDateBefore, nbMax);
	}
	
	/**
	 * Count tournament finished for category and end_date < tsDateBefore
	 * @param catID
	 * @param tsDateBefore
	 * @return
	 */
	public int countTournamentFinishedForCategoryBeforeDate(long catID, long tsDateBefore) {
		return tournamentDAO.countFinishedForCategoryBeforeDate(catID, tsDateBefore);
	}
	
	/**
	 * Return the list of tournament in progress for a category. begin date < current date < end date. Order by date asc 
	 * @param catID
	 * @return
	 */
	public List<Tournament> getTournamentListInProgressForCategoryOrderAsc(long catID) {
		return tournamentDAO.listForCategoryInProgressOrderAsc(catID, System.currentTimeMillis());
	}
	
	/**
	 * Return the deal for tournament and index
	 * @param tour
	 * @param dealIndex
	 * @return
	 */
	public TournamentDeal getDealForTournamentAndIndex(Tournament tour, int dealIndex) {
		if (tour != null){
			return tournamentDealDAO.getDealForTournamentAndIndex(tour.getID(), dealIndex);
		}
		return null;
	}
	
	/**
	 * Return the tournament object with this ID
	 * @param tourID
	 * @return
	 */
	public Tournament getTournament(long tourID) {
		return tournamentDAO.getTournament(tourID);
	}
	
	/**
	 * Return the tournament category with this ID
	 * @param catID
	 * @return
	 */
	public TournamentCategory getCategory(long catID) {
		return tournamentCategoryDAO.getCategory(catID);
	}
	

	/**
	 * Return the list of deal on this tournament
	 * @param tourID
	 * @return
	 */
	public List<TournamentDeal> getDealForTournament(long tourID) {
		return tournamentDealDAO.getDealForTournament(tourID);
	}
	
	/**
	 * Return an array with all deal id on this tournament
	 * @param tourID
	 * @return
	 */
	public long[] getArrayDealIDForTournament(long tourID) {
		List<TournamentDeal> listDeal = tournamentDealDAO.getDealForTournament(tourID);
		if (listDeal != null && listDeal.size() > 0) {
			long[] tab = new long[listDeal.size()];
			for (int i = 0; i < listDeal.size(); i++) {
				tab[i] = listDeal.get(i).getID();
			}
			return tab;
		}
		return null;
	}
	
	/**
	 * Return a list of tournament for category with status not finished and end date < current date
	 * @param catID
	 * @return
	 */
	public List<Tournament> getTournamentListForCategoryNotFinishedAndDateExpired(long catID) {
		return tournamentDAO.listForCategoryNotFinishedAfterDate(catID, System.currentTimeMillis());
	}
	
	/**
	 * Delete all tournament from list
	 * @param listTour
	 * @return
	 */
	@Transactional
	public int deleteTournamentList(List<Tournament> listTour) {
		int nbTourDelete = 0;
		if (listTour != null && listTour.size() > 0) {
			for (Tournament t : listTour) {
				if (t.isFinished()) {
					try {
						if (deleteTournament(t)){
							nbTourDelete++;
						}
					} catch (Exception e) {
						log.error("Exception to delete tournament="+t);
					}
				}
			}
		}
		return nbTourDelete;
	}
	
	/**
	 * Delete the tournament. Delete tournamentPlay, table, tablePlayer, game, distribution, deal and tournament
	 * @param tour
	 * @return
	 */
	@Transactional
	public boolean deleteTournament(Tournament tour) {
		if (tour != null) {
			if (tour.isFinished()) {
				boolean bResult = true;

				List<TournamentDeal> listDeal = getDealForTournament(tour.getID());
				for (TournamentDeal td : listDeal) {
					TournamentDistribution distrib = td.getDistribution();
					// delete deal
					bResult = bResult && tournamentDealDAO.delete(td);
					if (!bResult) break;
					// delete distrib
					bResult = bResult && tournamentDistribDAO.delete(distrib);
					if (!bResult) break;
					
					// delete table (if not delete before ...)
					if (!bResult) break;
				}
				
				// delete tournament
				if (bResult) bResult = bResult && tournamentDAO.delete(tour);
				if (bResult) {
                    if (log.isDebugEnabled()) {
                        log.debug("Delete with success tournament=" + tour);
                    }
				} else {
					log.error("Delete failed for tournament="+tour);
				}
				return bResult;
			} else {
				log.error("Tournament not finished ! tour="+tour);
			}
		} else {
			log.error("Tournament parameter is null");
		}
		return false;
	}
	
	/**
	 * Delete the tournament with this ID. Delete tournamentPlay, table, tablePlayer, game, distribution, deal and tournament
	 * @param tourID
	 * @return true if delete operation success
	 */
	@Transactional
	public boolean deleteTournament(long tourID) {
		Tournament tour = getTournament(tourID);
		if (tour != null) {
			return deleteTournament(tour);
		} else {
			log.error("No tournament found for ID="+tourID);
		}
		return false;
	}
	
	/**
	 * Update the data linked with this tournament (nb player, score average).
	 * @param tourID
	 */
	@Transactional
	public void updateDataForFinishTournament(long tourID) {
		Tournament tour = getTournament(tourID);
		if (tour != null) {
			if (tour.isFinished()) {
				// loop on each deal to set stat data
				long[] tabDealID = getArrayDealIDForTournament(tourID);
				for (long dealID : tabDealID) {
                    TournamentDeal deal = tournamentDealDAO.getDeal(dealID);
                    if (deal != null) {
                        int nbPlayer = 0;
                        double scoreAverage = 0;
                        if (tour.getCategory().getID() == Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER) {
                            nbPlayer = tournamentGame2DAO.getNbGameForDeal(dealID);
                            scoreAverage = tournamentGame2DAO.getScoreAverageForDeal(dealID);
                        }
                        deal.setNbPlayer(nbPlayer);
                        deal.setScoreAverage(scoreAverage);
                    } else {
                        log.error("No deal found for dealID=" + dealID + " - tourID=" + tourID);
                    }
                }
			} else {
				log.error("Tournament is not finished ! tour="+tour.toString());
			}
		} else {
			log.error("No tournament found for ID="+tourID);
		}
	}
	
	/**
	 * Return the tournament type associated to the tournament category
	 * @return
	 */
	public TournamentType getTournamentType(long tourCategoryID) {
		if (tourCategoryID == Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER) {
			return ContextManager.getTournamentTrainingPartnerMgr();
		}

		log.error("No tournament type defined for categoryID="+tourCategoryID);
		return null;
	}
	
	/**
	 * Transform settings string to object TournamentSettings
	 * @param t
	 * @return
	 */
	public TournamentSettings getTournamentSettings(Tournament t) {
		if (t != null) {
			return getTournamentSettings(t.getSettings());
		} else {
			log.error("Tournament is null");
		}
		return null;
	}
	
	/**
	 * Transform settings string to object TournamentSettings
	 * @return
	 */
	public TournamentSettings getTournamentSettings(String strSettings) {
		if (strSettings != null && strSettings.length() > 0) {
			try {
				TournamentSettings ts = ContextManager.getJSONTools().mapData(strSettings, TournamentSettings.class);
				return ts;
			} catch (JsonParseException e) {
				log.error("JsonParseException to get TournamentSettings for strSettings="+strSettings, e);
			} catch (JsonMappingException e) {
				log.error("JsonMappingException to get TournamentSettings for strSettings="+strSettings, e);
			} catch (IOException e) {
				log.error("IOException to get TournamentSettings for strSettings="+strSettings, e);
			}
		} else {
			log.error("strSettings is null or empty");
		}
		return null;
	}
	
	/**
	 * 
	 * @param t
	 * @param ts
	 */
	public boolean setTournamentSettings(Tournament t, TournamentSettings ts) {
		if (t != null && ts != null) {
			try {
				String settings = ContextManager.getJSONTools().transform2String(ts, false);
				t.setSettings(settings);
				return true;
			} catch (JsonGenerationException e) {
				log.error("JsonGenerationException to get TournamentSettings for tour="+t+" - settings="+ts,e);
			} catch (JsonMappingException e) {
				log.error("JsonMappingException to get TournamentSettings for tour="+t+" - settings="+ts,e);
			} catch (IOException e) {
				log.error("IOException to get TournamentSettings for tour="+t+" - settings="+ts,e);
			}
		} else {
			log.error("Parameters tournament or settings is null - tour="+t+" - ts="+ts);
		}
		return false;
	}
}
