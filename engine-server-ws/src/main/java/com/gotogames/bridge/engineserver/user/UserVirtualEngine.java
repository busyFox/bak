package com.gotogames.bridge.engineserver.user;

import com.codahale.metrics.Meter;
import com.gotogames.bridge.engineserver.common.Constantes;
import com.gotogames.bridge.engineserver.common.ContextManager;
import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import com.gotogames.bridge.engineserver.ws.compute.WSEngineStat;
import com.gotogames.bridge.engineserver.ws.servlet.EngineWebsocketEndpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by pserent on 02/11/2016.
 */
public class UserVirtualEngine extends UserVirtual {
    private boolean fbMoteur = false;
    private List<Integer> listEngine = null;
    private WSEngineStat engineStat = new WSEngineStat();
    private EngineWebsocketEndpoint webSocket = null;
    private AtomicInteger maxThread = new AtomicInteger(0);
    private AtomicInteger nbThread = new AtomicInteger(0);
    private AtomicInteger queueSize = new AtomicInteger(0);
    private AtomicInteger computeTime = new AtomicInteger(0);
    private AtomicInteger nbRequestsInProgress = new AtomicInteger(0);
    private AtomicLong nbCompute = new AtomicLong(0);
    private Meter metricsRequest = null;
    private int nbMinutesBeforeRestart = 0;
    private String version = null;
    private boolean enable = true;
    private AtomicLong dateLastResult = new AtomicLong(0);
    private AtomicLong dateLastStat = new AtomicLong(0);
    private boolean isForTest = false;
    private boolean isForCompare = false;
    private boolean restartEnable = true;

    public UserVirtualEngine(String login, String loginPrefix, String password, long id) {
        super(login, loginPrefix, password, id);
        fbMoteur = UserVirtualMgr.getConfigIntValue(login, loginPrefix, "fbmoteur", 0) == 1;
        nbMinutesBeforeRestart = UserVirtualMgr.getConfigIntValue(login, loginPrefix, "nbMinutesBeforeRestart", 1440);
        maxThread.set(UserVirtualMgr.getConfigIntValue(login, loginPrefix, "maxThread", 0));
        metricsRequest = Constantes.getMetricRegistry().meter(getLogin());
        enable = UserVirtualMgr.getConfigIntValue(login, loginPrefix, "enable", 1) == 1;
        isForTest = UserVirtualMgr.getConfigIntValue(login, loginPrefix, "isForTest", 0) == 1;
        isForCompare = UserVirtualMgr.getConfigIntValue(login, loginPrefix, "isForCompare", 0) == 1;
        restartEnable = UserVirtualMgr.getConfigIntValue(login, loginPrefix, "restartEnable", 1) == 1;
    }

    @Override
    public boolean isEngine() {
        return true;
    }

    public boolean isFbMoteur() {
        return fbMoteur;
    }

    public String toString() {
        return super.toString()+" - version="+getVersion()+" - engine={"+listEngineToString()+"} - isForTest="+isForTest+" - enable="+enable+" - WS="+isWebSocketEnable()+" - maxThread="+maxThread.get()+" - nbThread="+nbThread.get()+" - queueSize="+queueSize.get()+" - nbCompute="+nbCompute.get()+" - nbMinutesBeforeRestart="+getNbMinutesBeforeRestart()+" - dateLastResult="+Constantes.timestamp2StringDateHour(getDateLastResult());
    }

    public void setListEngine(List<Integer> listEngineVersion) {
        if (listEngineVersion != null) {
            listEngine = new ArrayList<Integer>();
            listEngine.addAll(listEngineVersion);
            Collections.sort(listEngine);
        }
    }
    public boolean containsEngineVersion(int engineVersion) {
        if (listEngine == null || listEngine.size() == 0) {
            return false;
        } else {
            return listEngine.contains(engineVersion);
        }
    }

    public String listEngineToString() {
        String listEngineStr = "";
        if (listEngine != null && listEngine.size() > 0) {
            for (int i=0; i < listEngine.size(); i++) {
                if (listEngineStr.length() > 0) {
                    listEngineStr += ";";
                }
                listEngineStr += ""+listEngine.get(listEngine.size()-1-i);
                if (i >= 3) {
                    break;
                }
            }
            listEngineStr +=" - nbDLL="+listEngine.size();
        } else {
            listEngineStr = "empty";
        }
        return listEngineStr;
    }

    public WSEngineStat getEngineStat() {
        return engineStat;
    }

    public void setEngineStat(WSEngineStat engineStat) {
        this.engineStat = engineStat;
    }

    public EngineWebsocketEndpoint getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(EngineWebsocketEndpoint wsMessage) {
        this.webSocket = wsMessage;
    }

    public void removeWebSocket(EngineWebsocketEndpoint ws) {
        if (webSocket == ws) {
            if (ContextManager.getUserMgr().getLogger().isDebugEnabled()) {
                ContextManager.getUserMgr().getLogger().debug("Remove websocket for user=" + this);
            }
            webSocket = null;
        } else {
            if (ContextManager.getUserMgr().getLogger().isDebugEnabled()) {
                ContextManager.getUserMgr().getLogger().debug("current websocket and param not same => no remove is done for user=" + this);
            }
        }
    }

    public int getNbThread() {
        return nbThread.get();
    }

    public void setNbThread(int nbThread) {
        this.nbThread.set(nbThread);
    }

    public int getMaxThread() {
        return maxThread.get();
    }

    public void setMaxThread(int value) {
        if (value != maxThread.get()) {
            maxThread.set(value);
        }
    }

    public int getQueueSize() {
        return queueSize.get();
    }

    public void setQueueSize(int queueSize) {
        this.queueSize.set(queueSize);
    }

    public int getComputeTime() {
        return computeTime.get();
    }

    public void setComputeTime(int value) {
        this.computeTime.set(value);
    }

    public long getDateLastStat() {
        return dateLastStat.get();
    }

    public void setDateLastStat(long value) {
        this.dateLastStat.set(value);
    }

    public boolean isWebSocketEnable() {
        return webSocket != null;
    }

    public boolean isEngineWebSocketReady() {
        return isWebSocketEnable() && listEngine != null && listEngine.size() > 0;
    }

    public long incrementNbCompute() {
        return this.nbCompute.incrementAndGet();
    }

    public long getNbCompute() {
        return this.nbCompute.get();
    }

    public void closeWebSocket() {
        if (webSocket != null) {
            webSocket.closeSession();
        }
    }

    public void markMetricsRequest() {
        metricsRequest.mark();
    }

    public Meter getMetricsRequest() {
        return metricsRequest;
    }

    public int incrementNbRequestsInProgress() {
        return nbRequestsInProgress.incrementAndGet();
    }

    public int decrementNbRequestsInProgress() {
        return nbRequestsInProgress.decrementAndGet();
    }

    public int getNbRequestsInProgress() {
        return nbRequestsInProgress.get();
    }

    public void setNbRequestsInProgress(int value) {
        nbRequestsInProgress.set(value);
    }

    /**
     * Return percent of thread available for compute => nbThreadAvailable * 100 / nbMaxThread
     * @return
     */
    public int getAvailableThreadPercent() {
        int nbThreadAvailable = getNbThread();
        int nbMaxThread = getMaxThread();
        int value = 0;
        if (nbMaxThread <= 0) {
            value = nbThreadAvailable*10;
        }
        else if (nbMaxThread < nbThreadAvailable) {
            value = 100;
        }
        else {
            value = (nbThreadAvailable * 100)/ nbMaxThread;
        }
        if (value > 100) {
            value = 100;
        }
        return value;
    }

    public long getNbMinutesSinceCreation() {
        return (System.currentTimeMillis() - dateCreation) / (60*1000);
    }

    public int getNbMinutesBeforeRestart() {
        return nbMinutesBeforeRestart;
    }

    public long computeNbMinutesBeforeRestart() {
        if (nbMinutesBeforeRestart > 0) {
            if (getNbMinutesSinceCreation() > nbMinutesBeforeRestart) {
                return 0;
            }
            return nbMinutesBeforeRestart - getNbMinutesSinceCreation();
        }
        return -1;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public boolean needToRestart() {
        if (!restartEnable) {
            return false;
        }
        if (getNbMinutesBeforeRestart() > 0 && getNbMinutesSinceCreation() >= getNbMinutesBeforeRestart()) {
            return true;
        }
        return false;
    }

    public boolean isNoResultSinceALongTime() {
        int nbMinuteWithNoResult = EngineConfiguration.getInstance().getIntValue("user.engineNbMinuteWithNoResult", 5);
        if (nbMinuteWithNoResult > 0) {
            long tsThresholdLastResult = System.currentTimeMillis() - (nbMinuteWithNoResult * 60 * 1000);
            if (getDateLastResult() > 0 && getDateLastResult() < tsThresholdLastResult) {
                return true;
            }
        }
        return false;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public long getDateLastResult() {
        return dateLastResult.get();
    }

    public void setDateLastResult(long dateLastActivity) {
        this.dateLastResult.set(dateLastActivity);
    }

    public boolean isRestartEnable() {
        return restartEnable;
    }

    public void setRestartEnable(boolean restartEnable) {
        this.restartEnable = restartEnable;
    }

    /**
     * Compute index using nb thread available, nb max thread and compute time : ((nbTMax - nbT) / nbTMax) / time
     * @return
     */
    public double computePerformanceIndex() {
        int nbTMax = getMaxThread();
        int nbT = getNbThread();
        double time = getComputeTime();
        if (nbTMax > 0 && time > 0) {
            double value = (double)(nbTMax - nbT) / nbTMax; // % thread working
            return value/time; // divide by compute time
        } else {
            return 0.5;
        }
    }

    public boolean isForTest() {
        return isForTest;
    }

    public boolean isForCompare() {
        return isForCompare;
    }
}
