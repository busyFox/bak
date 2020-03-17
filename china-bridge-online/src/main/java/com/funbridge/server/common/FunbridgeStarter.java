package com.funbridge.server.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Component used to initialize funbridge managers and stopping them.
 * ?????funbridge????????????
 * @author pascal
 *
 */
@Component
public class FunbridgeStarter implements SmartLifecycle {
	private Logger log = LogManager.getLogger(this.getClass());
	private boolean isRunning = false;
	private boolean isTest = false;
    private Scheduler scheduler;
	
	public void setTest(boolean testValue) {
		this.isTest = testValue;
	}
	
	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public void start() {
        if (!isTest) {
			log.warn("Start begin");
			List<String> listTSStart = new ArrayList<String>();
			long ts = System.currentTimeMillis();
			// argine mgr
			ContextManager.getArgineEngineMgr().startUp();
			listTSStart.add("ArgineEngineMgr "+(System.currentTimeMillis()-ts));
			// message mgr
			ts = System.currentTimeMillis();
			ContextManager.getMessageMgr().startUp();
			listTSStart.add("MessageMgr "+(System.currentTimeMillis()-ts));
			// messageNotif mgr
			ts = System.currentTimeMillis();
			ContextManager.getMessageNotifMgr().startUp();
			listTSStart.add("MessageNotifMgr "+(System.currentTimeMillis()-ts));
            // Text UI mgr
            ts = System.currentTimeMillis();
            ContextManager.getTextUIMgr().startUp();
            listTSStart.add("TextUIMgr "+(System.currentTimeMillis()-ts));
			// mail mgr
			ts = System.currentTimeMillis();
			ContextManager.getMailMgr().startUp();
			listTSStart.add("MailMgr "+(System.currentTimeMillis()-ts));
			// player mgr
			ts = System.currentTimeMillis();
			ContextManager.getPlayerMgr().startUp();
			listTSStart.add("PlayerMgr "+(System.currentTimeMillis()-ts));
            // player cache mgr
            ts = System.currentTimeMillis();
            ContextManager.getPlayerCacheMgr().startUp();
            listTSStart.add("PlayerCacheMgr "+(System.currentTimeMillis()-ts));
            // operation mgr
            ts = System.currentTimeMillis();
            ContextManager.getOperationMgr().startUp();
            listTSStart.add("OperationMgr "+(System.currentTimeMillis()-ts));
            // Store manager
            ts = System.currentTimeMillis();
            ContextManager.getStoreMgr().startUp();
            listTSStart.add("StoreMgr "+(System.currentTimeMillis()-ts));
			// tour timezone mgr
			ts = System.currentTimeMillis();
			ContextManager.getTimezoneMgr().startUp();
			listTSStart.add("TimezoneMgr "+(System.currentTimeMillis()-ts));
            // Training tournaments
            ts = System.currentTimeMillis();
            ContextManager.getTrainingMgr().startUp();
            listTSStart.add("TrainingMgrNew "+(System.currentTimeMillis()-ts));
            // tour newserie
            ts = System.currentTimeMillis();
            ContextManager.getTourSerieMgr().startUp();
            listTSStart.add("TourNewSerie "+(System.currentTimeMillis()-ts));
            // tour serie top challenge
            ts = System.currentTimeMillis();
            ContextManager.getSerieTopChallengeMgr().startUp();
            listTSStart.add("SerieTopChallenge "+(System.currentTimeMillis()-ts));
			// tour serie easy challenge
			ts = System.currentTimeMillis();
			ContextManager.getSerieEasyChallengeMgr().startUp();
			listTSStart.add("SerieEasyChallenge "+(System.currentTimeMillis()-ts));
            // tour duel
            ts = System.currentTimeMillis();
            ContextManager.getDuelMgr().startUp();
            listTSStart.add("DuelMgr "+(System.currentTimeMillis()-ts));
			// tour training partner
			ts = System.currentTimeMillis();
			ContextManager.getTournamentTrainingPartnerMgr().startUp();
			listTSStart.add("TourTrainingPartner "+(System.currentTimeMillis()-ts));
			// tour challenge mgr
			ts = System.currentTimeMillis();
			ContextManager.getTournamentChallengeMgr().startUp();
			listTSStart.add("TourChallenge "+(System.currentTimeMillis()-ts));
			// tour CBO mgr
			ts = System.currentTimeMillis();
			ContextManager.getTourCBOMgr().startUp();
			listTSStart.add("TourCBO "+(System.currentTimeMillis()-ts));
			// tour Federation stat period mgr
			ts = System.currentTimeMillis();
			ContextManager.getTourFederationStatPeriodMgr().startUp();
			listTSStart.add("TourFederationStatPeriod "+(System.currentTimeMillis()-ts));
			// service game
			ts = System.currentTimeMillis();
			ContextManager.getGameService().startUp();
			listTSStart.add("GameService "+(System.currentTimeMillis()-ts));
            // Team manager
            ts = System.currentTimeMillis();
            ContextManager.getTeamMgr().startUp();
            listTSStart.add("TeamMgr "+(System.currentTimeMillis()-ts));
            // TeamCache manager
            ts = System.currentTimeMillis();
            ContextManager.getTeamCacheMgr().startUp();
            listTSStart.add("TeamCacheMgr "+(System.currentTimeMillis()-ts));
            // TourTeam manager
            ts = System.currentTimeMillis();
            ContextManager.getTourTeamMgr().startUp();
            listTSStart.add("TourTeamMgr "+(System.currentTimeMillis()-ts));
            // PrivateTournament manager
            ts = System.currentTimeMillis();
            ContextManager.getPrivateTournamentMgr().startUp();
            listTSStart.add("PrivateTournamentMgr "+(System.currentTimeMillis()-ts));
			// tour learning mgr
			ts = System.currentTimeMillis();
			ContextManager.getTourLearningMgr().startUp();
			listTSStart.add("TourLearningMgr "+(System.currentTimeMillis()-ts));
			// nursing mgr
			ts = System.currentTimeMillis();
			ContextManager.getNursingMgr().startUp();
			listTSStart.add("NursingMgr "+(System.currentTimeMillis()-ts));

			// at the end start quartz scheduler
            try {
                SchedulerFactory schedulerFactory = new StdSchedulerFactory();
                scheduler = schedulerFactory.getScheduler();
                scheduler.start();
            } catch (Exception e) {
                log.error("Failed to start quartz scheduler !", e);
            }
			isRunning = true;
			
			log.warn("list time to start="+listTSStart);
			log.warn("Start end");
		} else {
			log.info("TEST - NO STARTUP");
		}
	}

	@Override
	public void stop() {
		log.warn("Stop begin");
		ContextManager.getPresenceMgr().closeAllSession(Constantes.EVENT_VALUE_DISCONNECT_MAINTENANCE);
		isRunning = false;
		// stop scheduler
        if (scheduler != null) {
            try {
                scheduler.shutdown(true);
            } catch (Exception e) {
                log.error("Failed to stop scheduler", e);
            }
        }
		log.warn("Stop end");
	}

	@Override
	public int getPhase() {
		// MIN_VALUE => first to start and last to stop
		// MAX_VALUE => last to start and first to stop
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

}
