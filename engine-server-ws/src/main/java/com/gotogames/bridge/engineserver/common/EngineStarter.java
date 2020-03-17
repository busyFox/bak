package com.gotogames.bridge.engineserver.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EngineStarter implements SmartLifecycle {
    private Logger log = LogManager.getLogger(this.getClass());
    private boolean isRunning = false;
    private Scheduler scheduler;

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

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void start() {
        log.warn("Start begin");
        List<String> listTSStart = new ArrayList<String>();
        long ts = System.currentTimeMillis();
        // argine mgr
        ContextManager.getAlertMgr().startup();
        listTSStart.add("AlertMgr "+(System.currentTimeMillis()-ts));
        // UserVirtual mgr
        ts = System.currentTimeMillis();
        ContextManager.getUserMgr().startup();
        listTSStart.add("UserVirtualMgr "+(System.currentTimeMillis()-ts));
        // Queue mgr
        ts = System.currentTimeMillis();
        ContextManager.getQueueMgr().startup();
        listTSStart.add("QueueMgr "+(System.currentTimeMillis()-ts));
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
    }

    @Override
    public void stop() {
        log.warn("Stop begin");
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
}
