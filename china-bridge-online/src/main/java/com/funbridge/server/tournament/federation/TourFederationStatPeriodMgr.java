package com.funbridge.server.tournament.federation;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.tournament.federation.data.TourFederationStat;
import com.funbridge.server.tournament.federation.data.TourFunbridgePointsStat;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by luke on 23/01/2018.
 */
@Component(value="tourFederationStatPeriodMgr")
@Scope(value="singleton")
public class TourFederationStatPeriodMgr extends FunbridgeMgr {

    @Resource(name="mongoTemplate")
    protected MongoTemplate mongoTemplate;

    private Scheduler scheduler;
    private String statCurrentPeriodID, statPreviousPeriodID;
    public SimpleDateFormat sdfStatPeriod = new SimpleDateFormat("yyyyMM");
    private static final String PERIOD_ID_INDICE_SEPARATOR = "_";

    @Override
    public void startUp() {
        log.info("startup");

        initStatPeriod();

        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();

            // Change period stat task - triger every first day of month
            JobDetail jobPeriodStat = JobBuilder.newJob(TourFederationPeriodStatTask.class).withIdentity("periodStatTask", "TourFederation").build();
            CronTrigger triggerPeriodStat = TriggerBuilder.newTrigger().withIdentity("triggerPeriodStatTask", "TourFederation").withSchedule(CronScheduleBuilder.cronSchedule("0 1 0 1 * ?")).build();
            Date dateNextJobPeriodStat = scheduler.scheduleJob(jobPeriodStat, triggerPeriodStat);
            log.warn("Sheduled for job=" + jobPeriodStat.getKey() + " run at="+dateNextJobPeriodStat+" - cron expression=" + triggerPeriodStat.getCronExpression() + " - next fire=" + triggerPeriodStat.getNextFireTime());
        } catch (Exception e) {
            log.error("Exception to init scheduler", e);
        }
    }

    public long getDateNextJobTourFederationPeriodStat() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerPeriodStatTask", "TourFederation"));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    @Override
    public void init() {

    }

    @Override
    public void destroy() {

    }

    public boolean initStatPeriod() {
        boolean processOK = true;

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        String currentPeriodID = sdfStatPeriod.format(calendar.getTime());
        calendar.add(java.util.Calendar.MONTH, -1);
        String previousPeriodID = sdfStatPeriod.format(calendar.getTime());

        //  if devMode, add indice to value _XXX
        if (FBConfiguration.getInstance().getConfigBooleanValue("general.devMode", false)) {
            if (statCurrentPeriodID == null) {
                List<TourFunbridgePointsStat> stats = mongoTemplate.findAll(TourFunbridgePointsStat.class);
                TreeSet<String> keys = new TreeSet<>();
                for (TourFederationStat stat : stats) {
                    keys.addAll(stat.totalPeriod.keySet());
                }
                statCurrentPeriodID = keys.pollLast();
                statPreviousPeriodID = keys.pollLast();
                if (statCurrentPeriodID == null || statCurrentPeriodID.compareTo(currentPeriodID) < 0) {
                    statCurrentPeriodID = currentPeriodID;
                }
                if (statPreviousPeriodID == null || statPreviousPeriodID.compareTo(previousPeriodID) < 0) {
                    statPreviousPeriodID = previousPeriodID;
                }
            } else {
                if (statCurrentPeriodID.startsWith(currentPeriodID)) {
                    if (statCurrentPeriodID.contains(PERIOD_ID_INDICE_SEPARATOR)) {
                        int indice = Integer.parseInt(statCurrentPeriodID.substring(statCurrentPeriodID.indexOf(PERIOD_ID_INDICE_SEPARATOR)+1));
                        currentPeriodID += PERIOD_ID_INDICE_SEPARATOR+String.format("%03d", indice+1);
                    } else {
                        currentPeriodID += PERIOD_ID_INDICE_SEPARATOR+"001";
                    }
                }
                statPreviousPeriodID = statCurrentPeriodID;
                statCurrentPeriodID = currentPeriodID;
            }
        } else {
            statCurrentPeriodID = currentPeriodID;
            statPreviousPeriodID = previousPeriodID;
        }

        for (TourFederationMgr tourFederationMgr : ContextManager.getListTourFederationMgr()) {
            try {
                processOK = processOK & tourFederationMgr.initStatPeriod(statCurrentPeriodID, statPreviousPeriodID);
            } catch (Exception e) {
                tourFederationMgr.getLogger().error("Exception to initStatPeriod", e);
            }
        }

        return processOK;
    }

    public String getStatCurrentPeriodID() {
        return statCurrentPeriodID;
    }

    public String getStatPreviousPeriodID() {
        return statPreviousPeriodID;
    }

}
