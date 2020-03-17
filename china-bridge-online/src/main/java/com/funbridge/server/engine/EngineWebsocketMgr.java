package com.funbridge.server.engine;

import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by pserent on 28/04/2017.
 */
public class EngineWebsocketMgr {
    private String categoryName;
    private GenericObjectPool<EngineWebsocket> pool = null;
    private GenericObjectPoolConfig poolConfig = null;
    private Logger log = LogManager.getLogger(this.getClass());
    private AtomicLong nbCommandGetResult = new AtomicLong(0);

    public EngineWebsocketMgr(String category) {
        this.categoryName = category;
        initPool();
    }

    public void initPool() {
        if (pool != null) {
            pool.close();
        }
        pool = new GenericObjectPool<EngineWebsocket>(new EngineWebsocketPoolableFactory(categoryName));
        initPoolConfig();
    }

    public void closePool() {
        if (pool != null) {
            log.info("Close pool websocket for "+categoryName);
            pool.close();
            pool = null;
        }
    }

    public void initPoolConfig() {
        if (pool != null) {
            if (poolConfig == null) {
                poolConfig = new GenericObjectPoolConfig();
            }
            int nbMax = TournamentGenericMgr.getConfigIntValue(categoryName, "websocket.poolMaxTotal", -1);
            if (nbMax == -1) {
                nbMax = FBConfiguration.getInstance().getIntValue("game.engineWebSocket.poolMaxTotal", 20);
            }
            poolConfig.setMaxTotal(nbMax);
            long tsMaxWait = TournamentGenericMgr.getConfigIntValue(categoryName, "websocket.poolTimeMaxWait", -1);
            if (tsMaxWait == -1) {
                tsMaxWait = FBConfiguration.getInstance().getIntValue("game.engineWebSocket.poolTimeMaxWait", 5000);
            }
            poolConfig.setMaxWaitMillis(tsMaxWait);
            int nbMinIdle = TournamentGenericMgr.getConfigIntValue(categoryName, "websocket.poolMinIdle", -1);
            if (nbMinIdle == -1) {
                nbMinIdle = FBConfiguration.getInstance().getIntValue("game.engineWebSocket.poolMinIdle", 0);
            }
            poolConfig.setMinIdle(nbMinIdle);
            int nbMaxIdle = TournamentGenericMgr.getConfigIntValue(categoryName, "websocket.poolMaxIdle", -1);
            if (nbMaxIdle == -1) {
                nbMaxIdle = FBConfiguration.getInstance().getIntValue("game.engineWebSocket.poolMaxIdle", 2);
            }
            poolConfig.setMaxIdle(nbMaxIdle);
            long tsMinEvictableIdle = TournamentGenericMgr.getConfigIntValue(categoryName, "websocket.poolTimeMinEvictableIdle", -1);
            if (tsMinEvictableIdle == -1) {
                tsMinEvictableIdle = FBConfiguration.getInstance().getIntValue("game.engineWebSocket.poolTimeMinEvictableIdle", 10000);
            }
            poolConfig.setMinEvictableIdleTimeMillis(tsMinEvictableIdle);
            pool.setConfig(poolConfig);
            log.warn("Init pool for websocket Mgr - category=" + categoryName + " - poolStat=" + getPoolStat());
        } else {
            log.error("Pool is null ! => call method initPool");
        }
    }

    public String toString() {
        return "Category="+categoryName+" - pool stat="+getPoolStat()+" - nbGetResult="+nbCommandGetResult.get();
    }

    public void destroy() {
        stopTest();
        closePool();
    }

    public AtomicLong getNbCommandGetResult() {
        return nbCommandGetResult;
    }

    public GenericObjectPool<EngineWebsocket> getPool() {
        return pool;
    }

    public String getPoolStat() {
        return "{NumActive="+pool.getNumActive()+" - MinIdle="+pool.getMinIdle()+" - MaxIdle="+pool.getMaxIdle()+"  - MaxTotal="+pool.getMaxTotal()+" - tsMinEvictableIdle="+pool.getMinEvictableIdleTimeMillis()+" - tsMaxWait="+pool.getMaxWaitMillis()+" - NumWaiters="+pool.getNumWaiters()+"}";
    }

    public boolean sendCommandGetResult(BridgeEngineParam param, int requestType) {
        boolean result = false;
        if (pool != null) {
            EngineWebsocket websocket = null;
            try {
                // try to get a websocket
                websocket = pool.borrowObject();
                if (log.isDebugEnabled()) {
                    log.debug("category=" + categoryName + " Get websocket=" + websocket);
                }
                result = websocket.sendCommandGetResult(param, requestType);
                if (result) {
                    nbCommandGetResult.incrementAndGet();
                }
            } catch (Exception e) {
                log.error("Failed to get websocket or send command result - category=" + categoryName + " - poolStat=" + getPoolStat(), e);
            } finally {
                if (websocket != null) {
                    try {
                        pool.returnObject(websocket);
                    } catch (Exception e) {
                        log.error("Failed to returnObject websocket=" + websocket + " - category=" + categoryName + " - poolStat=" + getPoolStat(), e);
                    }
                }
            }
        } else {
            log.error("Pool is null ! => call method initPool");
        }
        return result;
    }


    public boolean testRun = false;

    ExecutorService testExecutor = Executors.newFixedThreadPool(10);
    public int nbThreadTest = 0;

    public boolean isDevMode() {
        return FBConfiguration.getInstance().getIntValue("game.engineWebSocket.devMode", 0) == 1;
    }

    public void startTest() {
        if (isDevMode()) {
            if (testExecutor == null) {
                testExecutor = Executors.newFixedThreadPool(10);
            }
            testRun = true;
            EngineWebsocketTestTask testTask = new EngineWebsocketTestTask(this);
            testExecutor.execute(testTask);
        }
    }

    public void stopTest() {
        testRun = false;
        if (testExecutor != null) {
            testExecutor.shutdown();
            try {
                if (testExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    testExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                testExecutor.shutdownNow();
            }
            testExecutor = null;
        }
        nbThreadTest = 0;
    }

    public void testPool() {
        if (isDevMode()) {
            Random random = new Random(System.nanoTime());
            nbThreadTest++;
            int threadNumber = nbThreadTest;
            int nbRun = random.nextInt(100);
            int idxRun = 0;
            log.debug("Start thread "+threadNumber+" to execute nbRun="+nbRun);
            while (testRun) {
                try {
                    // try to get a websocket
                    EngineWebsocket websocket = pool.borrowObject();
                    Thread.sleep(random.nextInt(500));
                    websocket.sendPing();
                    pool.returnObject(websocket);
                } catch (Exception e) {
                    log.error("Exeception testPool", e);
                    break;
                }
                idxRun++;
                if (idxRun >= nbRun) {
                    log.debug("Stop thread "+threadNumber+" after NbRun="+nbRun);
                    break;
                }
            }
        }
    }

}
