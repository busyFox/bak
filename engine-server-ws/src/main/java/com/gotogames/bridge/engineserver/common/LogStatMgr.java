package com.gotogames.bridge.engineserver.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 08/01/2016.
 */
@Component(value = "logStatMgr")
@Scope(value = "singleton")
public class LogStatMgr {
    private Logger log = LogManager.getLogger(this.getClass());

    /**
     * Read int value for parameter in name (logStat + paranName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public int getConfigIntValue(String paramName, int defaultValue) {
        return EngineConfiguration.getInstance().getIntValue("logStat." + paramName, defaultValue);
    }

    /**
     * Read long value for parameter in name (logStat + paranName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public long getConfigLongValue(String paramName, long defaultValue) {
        return EngineConfiguration.getInstance().getLongValue("logStat." + paramName, defaultValue);
    }

    /**
     * Is log stat enable (GENERAL)
     * @return
     */
    public boolean isLogEnable() {
        return getConfigIntValue("enable", 0) == 1;
    }

    /**
     * Is log stat enable for timestamp (bigger than value in config)
     * @param ts
     * @return
     */
    public boolean isLogEnableForTS(long ts) {
        if (isLogEnable()) {
            int threshold = getConfigIntValue("thresholdTS", 0);
            if (threshold > 0 && ts > threshold) {
                return true;
            }
        }
        return false;
    }

    /**
     * Log in info mode. if forceLog is true, log is done, else check ts value
     * @param forceLog
     * @param ts
     * @param extraData
     */
    public void logInfo(boolean forceLog, long ts, Object extraData) {
        if (isLogEnable()) {
            if (forceLog || isLogEnableForTS(ts)) {
                String caller = null;
                if (Thread.currentThread().getStackTrace().length > 2) {
                    StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
                    caller = ste.getClassName() + "." + ste.getMethodName();
                }
                log.info("LOG - " + caller + " - forceLog=" + forceLog + " - extraData={" + extraData + "} - TS=" + ts);
                long thresholdTsDeadlockThread = getConfigLongValue("thresholdTsDeadlockThread", 0);
                if (thresholdTsDeadlockThread > 0 && ts >= thresholdTsDeadlockThread) {
                    getInfoDeadlockedThreads(true);
                }
            }
        }
    }

    /**
     * Get nb deadlocked thread and info
     * @param logIt true to log it
     * @return
     */
    public List<String> getInfoDeadlockedThreads(boolean logIt) {
        List<String> results = new ArrayList<>();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] ids = threadMXBean.findDeadlockedThreads();
        results.add("DeadLocks : nb deadlocked thread="+((ids!=null)?ids.length:0));
        if (ids != null && ids.length > 0) {
            ThreadInfo[] infos = threadMXBean.getThreadInfo(ids,true,true);
            for (ThreadInfo info : infos) {
                log.info("Thread blocked : "+info);
            }
        }
        if (logIt) {
            for (String e : results) {
                log.info(e);
            }
        }
        return results;
    }
}
