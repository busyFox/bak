package com.funbridge.server.tournament.category;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.tournament.TournamentMgr;
import com.funbridge.server.tournament.data.Tournament;
import com.funbridge.server.tournament.data.TournamentCategory;
import com.gotogames.common.lock.LockMgr;
import com.gotogames.common.lock.LockWeakString;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class TournamentType extends FunbridgeMgr {
	@Resource(name="tournamentMgr")
	protected TournamentMgr tournamentMgr = null;
	@Resource(name="playerMgr")
	protected PlayerMgr playerMgr = null;
	protected String name;
	protected int nbCreditPlayDeal = 1;
	protected TournamentCategory category = null;
//	protected int nbMaxPlayerTournament = 100;
	protected boolean finishTaskEnable = false;
	private ScheduledExecutorService schedulerFinish = Executors.newScheduledThreadPool(1);
	private ScheduledExecutorService schedulerClean = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> schedulerFinishFuture = null, schedulerCleanFuture = null;
	private TournamentTypeFinisherTask finisherTask = null;
	private TournamentTypeCleanTask cleanTask = null;
	private LockMgr lockMgr = new LockMgr();
	protected int cleanNbDaysBeforeLimit = 30;
	protected LockWeakString lockTourID = new LockWeakString();
	
	/**
	 * Thread task to finish tournament
	 * @author pascal
	 *
	 */
	public class TournamentTypeFinisherTask implements Runnable {
		@Override
		public void run() {
			onEventFinishTask();
		}
	}
	
	/**
	 * Thread task to clean old tournament
	 * @author pascal
	 *
	 */
	public class TournamentTypeCleanTask implements Runnable {
		@Override
		public void run() {
			onEventCleanTask();
		}
	}
	
	public String getConfigStringValue(String paramName, String defaultValue) {
		return FBConfiguration.getInstance().getStringValue("tournament."+name+"."+paramName, defaultValue);
	}
	
	public int getConfigIntValue(String paramName, int defaultValue) {
		return FBConfiguration.getInstance().getIntValue("tournament."+name+"."+paramName, defaultValue);
	}
	
	public TournamentType(String name) {
		this.name = name;
	}
	
	public void init(long categoryID) {
		category = tournamentMgr.getCategory(categoryID);
	}
	
	public void createTimer() {
		// create task finisher
		if (finishTaskEnable) {
			finisherTask = new TournamentTypeFinisherTask();
			try {
				int finishPeriod = getConfigIntValue("finishPeriodSeconds", 300);
				int initDelay = getConfigIntValue("finishInitDelaySeconds", 60);
				schedulerFinishFuture = schedulerFinish.scheduleWithFixedDelay(finisherTask, initDelay, finishPeriod, TimeUnit.SECONDS);
				log.info("Schedule finisher - next run at "+Constantes.getStringDateForNextDelayScheduler(schedulerFinishFuture)+" - period (second)="+finishPeriod);
			} catch (Exception e) {
				log.error("Exception to start finisher task", e);
			}
		}
		
		// create task clean
		cleanTask = new TournamentTypeCleanTask();
		try {
			int cleanPeriod = getConfigIntValue("cleanPeriodSeconds", 300);
			int initDelay = getConfigIntValue("cleanInitDelaySeconds", 60);
			schedulerCleanFuture = schedulerClean.scheduleWithFixedDelay(cleanTask, initDelay, cleanPeriod, TimeUnit.SECONDS);
			log.info("Schedule clean - next run at "+Constantes.getStringDateForNextDelayScheduler(schedulerCleanFuture)+" - period (second)="+cleanPeriod);
		} catch (Exception e) {
			log.error("Exception to start clean task", e);
		}
	}
	
	public void destroy() {
		stopScheduler(schedulerClean);
		stopScheduler(schedulerFinish);
	}
	
	public TournamentCategory getCategory() {
		return category;
	}
	
	/**
	 * Check tournament is valid : not finished and date valid
	 * @param tourID
	 * @return
	 */
	public boolean checkTournamentValid(long tourID) {
		boolean valid = false;
		try {
			Tournament tour = tournamentMgr.getTournament(tourID);
			if (tour != null) {
				if (!tour.isFinished() && tour.isDateValid(System.currentTimeMillis())) {
					valid = true;
				}
			} else {
				log.error("No tournament found for tourID="+tourID);
			}
		} catch (Exception e) {
			log.error("Exception to check tournament valid tourID="+tourID, e);
		}
		return valid;
	}

	/**
	 * Return lock data to synchronize on this Tournament ID
	 * @param id
	 * @return
	 */
	public Object getLockForTourID(long tourID) {
		return lockTourID.getLock(""+tourID);
	}

	
	/**
	 * Finish the tournament with this ID. TournamentPlay is stored, all tournament game are updated and tournament is set to finished.
	 * @param tourID
	 * @return
	 */
	public boolean finishTournament(long tourID) {
		boolean result = false;
		// lock tournament
		synchronized (getLockForTourID(tourID)) {
			try {
				Tournament tour = tournamentMgr.getTournament(tourID);
				if (tour != null) {
					if (!tour.isFinished()) {
						// finish mode FILE
						if (isFinishFileEnable()) {
							log.error("No process tournament file for this tour="+tour);
						}
						// finish mode DIRECT DATABASE
						else {
							TournamentType tt = tournamentMgr.getTournamentType(tour.getCategory().getID());
							if (tt != null) {
								result = tt.storeTournament(tourID);
								if (result) {
									// update data for tournament (nb player ...)
									ContextManager.getTournamentMgr().updateDataForFinishTournament(tourID);
								}
							} else {
								log.error("No tournament type found for tourID="+tourID);
							}
						}
					} else {
						log.error("Tournament already finished ! tourID="+tourID);
					}
				} else {
					log.error("No tournament found for tourID="+tourID);
				}
			} catch (Exception e) {
				log.error("Exception during finish tournament tourID="+tourID, e);
			}
		}
		return result;
	}
	
	/**
	 * Store tournament data : tournament play, tournament game and tournament, and clean tables
	 * @param tourID
	 * @return true if success
	 */
	@Transactional
	public boolean storeTournament(long tourID) {
		boolean result = false;
		Tournament tour = tournamentMgr.getTournament(tourID);
		if (tour == null) {
			log.error("No tournament found for ID="+tourID);
		} else if (tour.isFinished()) {
			log.error("tournament already finished ! tourID="+tourID);
		} else {
			if (tour.getCategory().getID() == Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER) {
				int nbPlayer = ContextManager.getTournamentTrainingPartnerMgr().storeTournamentData(tour);
				tour.setFinished(true);
				tour.setNbPlayer(nbPlayer);
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * Check configuration to get finisher status
	 * @return
	 */
	protected boolean isFinishEnable() {
		if (FBConfiguration.getInstance().getIntValue("tournament.finish.defaultEnable", 0) == 0) {
			return false;
		}
        return getConfigIntValue("finishEnable", 0) != 0;
    }
	
	/**
	 * Check configuration to get clean status
	 * @return
	 */
	protected boolean isCleanEnable() {
		if (FBConfiguration.getInstance().getIntValue("tournament.clean.defaultEnable", 0) == 0) {
			return false;
		}
        return getConfigIntValue("cleanEnable", 0) != 0;
    }
	
	protected boolean isGeneratorEnable() {
		if (FBConfiguration.getInstance().getIntValue("tournament.generator.defaultEnable", 0) == 0) {
			return false;
		}
        return getConfigIntValue("generatorEnable", 0) != 0;
    }

	/**
	 * Callback to terminate tournament
	 */
	public abstract void onEventFinishTask();
	
	/**
	 * Callback to clean tournaments
	 */
	public abstract void onEventCleanTask();
	
	/**
	 * Clean tournament of category with end date over nbDaysBefore days
	 * @param nbMaxTour
	 * @param nbDaysBefore
	 */
	public void cleanTournament(int nbMaxTour, int nbDaysBefore) {
		if (isCleanEnable()) {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_YEAR, -nbDaysBefore);
				List<Tournament> listTour = tournamentMgr.getTournamentListFinishedForCategoryBeforeDateOrderAsc(category.getID(),
						nbMaxTour, 
						cal.getTimeInMillis());
				if (listTour != null) {
					int nbTourDelete = 0;
					long ts = System.currentTimeMillis();
					if (FBConfiguration.getInstance().getIntValue("tournament.clean.deleteListFunction", 0) == 1) {
						try {
							nbTourDelete = ContextManager.getTournamentMgr().deleteTournamentList(listTour);
						} catch (Exception e) {
							log.error("Exception to delete list of tournament", e);
						}
					} else {
						for (Tournament t : listTour) {
							try {
								if (ContextManager.getTournamentMgr().deleteTournament(t.getID())) {
									nbTourDelete++;
								} else {
									log.error("Failed to delete tour="+t.getName()+" - id="+t.getID());
								}
							} catch (Exception e) {
								log.error("Exception to delete tournament t="+t.toString(), e);
							}
						}
					}
                    if (log.isDebugEnabled()) {
                        log.debug("Category=" + category.getName() + " - Nb tour to delete=" + listTour.size() + " - nbTourDelete=" + nbTourDelete + " - ts=" + (System.currentTimeMillis() - ts));
                    }
				} else {
					log.error("List null for tournament finished before date="+Constantes.timestamp2StringDateHour(cal.getTimeInMillis()));
				}
		}
	}
	
	/**
	 * Check if config finish file is enable for this tournament category
	 * @return
	 */
	public boolean isFinishFileEnable() {
		return getConfigIntValue("finishFile", 0) == 1;
	}
}
