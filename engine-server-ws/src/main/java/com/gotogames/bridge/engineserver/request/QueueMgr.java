package com.gotogames.bridge.engineserver.request;

import com.gotogames.bridge.engineserver.cache.TreeMgr;
import com.gotogames.bridge.engineserver.common.*;
import com.gotogames.bridge.engineserver.request.data.FBSetResultParam;
import com.gotogames.bridge.engineserver.request.data.QueueData;
import com.gotogames.bridge.engineserver.session.EngineSessionMgr;
import com.gotogames.bridge.engineserver.user.UserVirtual;
import com.gotogames.bridge.engineserver.user.UserVirtualEngine;
import com.gotogames.bridge.engineserver.user.UserVirtualFBServer;
import com.gotogames.bridge.engineserver.user.UserVirtualMgr;
import com.gotogames.bridge.engineserver.ws.compute.WSComputeQuery;
import com.gotogames.bridge.engineserver.ws.request.RequestService;
import com.gotogames.bridge.engineserver.ws.request.RequestServiceImpl;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.services.JSONService;
import com.gotogames.common.tools.NumericalTools;
import com.gotogames.common.tools.StringTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component(value="queueMgr")
@Scope(value="singleton")
public class QueueMgr {
	public class PollingThreshold {
		public int threshold;
		public int value;
		public String toString() {
			return "val="+value+";threshold="+threshold;
		}
	}
	public class PollingFunction {
		public int b;
		public int a;
		public String toString() {
			return "y="+a+"x + "+b;
		}
		public double getValue(double x) {
			return (double)a*x + b;
		}
	}
	public class PollingUserSettings {
		public boolean compute = false;
		public int defaultValue = 0;
		public String toString() {
			return "compute="+compute+";defaultValue="+defaultValue;
		}
	}
	private Logger log = LogManager.getLogger(this.getClass());
	private ConcurrentHashMap<Long, QueueData> queueRequest = new ConcurrentHashMap<Long, QueueData>();
    private ConcurrentHashMap<Long, QueueData> queueRequestTest = new ConcurrentHashMap<Long, QueueData>();
    private ConcurrentHashMap<Long, QueueData> queueRequestCompare = new ConcurrentHashMap<Long, QueueData>();
	private long index = 0;
	private long indexTest = 0;
    private long indexCompare = 0;
//	private LockMgr lockMgr = new LockMgr();
	private LockWeakString lockData = new LockWeakString();
    private LockWeakString lockDataTest = new LockWeakString();
    private LockWeakString lockDataCompare = new LockWeakString();
    private ConcurrentLinkedQueue<Long> meterTimeGet = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Long> meterTimeGetEmpty = new ConcurrentLinkedQueue<>();
	private ArrayDeque<Long> stackTimeGet = new ArrayDeque<Long>();
	private ArrayDeque<Long> stackTimeGetEmpty = new ArrayDeque<Long>();
	private int stackMaxSize = 1000;
	private Map<String, PollingUserSettings> mapPollingUser = new HashMap<String, PollingUserSettings>();
	private String[] pollingUsers = null;
	private boolean pollingCompute = false;
	private int pollingDefaultValue = 0;
	private boolean loadingPollingData = false;
	private PollingThreshold pollingThresholdMiddle = new PollingThreshold();
	private PollingThreshold pollingThresholdSmaller = new PollingThreshold();
	private PollingThreshold pollingThresholdGreater = new PollingThreshold();
	private PollingFunction pollingComputeFunctionSmaller = new PollingFunction();
	private PollingFunction pollingComputeFunctionGreater = new PollingFunction();
	private Random randomUseWS = new Random(System.nanoTime());
    private Random randomTestEngine = new Random(System.nanoTime());
	public boolean taskRemoveOldestTestRunning = false;
    public boolean taskRemoveOldestCompareRunning = false;
	@Resource(name="treeMgr")
	private TreeMgr treeMgr;
	
	@Resource(name="sessionMgr")
	private EngineSessionMgr sessionMgr = null;
    @Resource(name = "alertMgr")
    private AlertMgr alertMgr = null;

    @Resource(name="logStatMgr")
    private LogStatMgr logStatMgr;

    private JSONService serviceSetResult = null;

    private ExecutorService threadPoolSetResult = null;
    private final ReentrantReadWriteLock rwLockThreadPoolSetResult = new ReentrantReadWriteLock();
    private final Lock lockReadThreadPoolSetResult = rwLockThreadPoolSetResult.readLock();
    private final Lock lockWriteThreadPoolSetResult = rwLockThreadPoolSetResult.writeLock();

    @Resource(name="requestServiceImpl")
    private RequestServiceImpl requestServiceImpl = null;
    @Resource(name = "userVirtualMgr")
    private UserVirtualMgr userVirtualMgr = null;

    private Scheduler scheduler;
	
	@PostConstruct
	public void init() {
        log.warn("init");
		if (treeMgr == null || sessionMgr == null) {
			log.error("Parameters null : lockMgr | treeMgr | sessionMgr");
		} else {
			loadPollingData();
		}

        serviceSetResult = new JSONService(false);
		initThreadPoolSetResult();
	}

	public void startup() {
	    log.warn("startup");
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();

            // schedule remove oldest test request
            JobDetail jobRemoveTest = JobBuilder.newJob(QueueTestRemoveOldest.class).withIdentity("removeOldestTestTask", "QueueMgr").build();
            CronTrigger triggerRemoveTest = TriggerBuilder.newTrigger().withIdentity("trigerRemoveOldestTestTask", "QueueMgr").withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 * * * ?")).build();
            Date dateNextJobRemoveTest = scheduler.scheduleJob(jobRemoveTest, triggerRemoveTest);
            log.warn("Sheduled for job=" + jobRemoveTest.getKey() + " run at="+dateNextJobRemoveTest+" - cron expression=" + triggerRemoveTest.getCronExpression() + " - next fire=" + triggerRemoveTest.getNextFireTime());

            // schedule remove oldest test request
            JobDetail jobRemoveCompare = JobBuilder.newJob(QueueTestRemoveOldest.class).withIdentity("removeOldestCompareTask", "QueueMgr").build();
            CronTrigger triggerRemoveCompare = TriggerBuilder.newTrigger().withIdentity("trigerRemoveOldestCompareTask", "QueueMgr").withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 * * * ?")).build();
            Date dateNextJobRemoveCompare = scheduler.scheduleJob(jobRemoveCompare, triggerRemoveCompare);
            log.warn("Sheduled for job=" + jobRemoveCompare.getKey() + " run at="+dateNextJobRemoveCompare+" - cron expression=" + triggerRemoveCompare.getCronExpression() + " - next fire=" + triggerRemoveCompare.getNextFireTime());

        } catch (Exception e) {
            log.error("Exception to init scheduler", e);
        }
    }
	
	@PreDestroy
	public void destroy() {
        log.warn("destroy");
        // shutdown thread pool
        threadPoolSetResult.shutdown();
        try {
            if (threadPoolSetResult.awaitTermination(30, TimeUnit.SECONDS)) {
                threadPoolSetResult.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPoolSetResult.shutdownNow();
        }
		synchronized (queueRequest) {
			queueRequest.clear();
		}
		mapPollingUser.clear();
		stackTimeGet.clear();
		stackTimeGetEmpty.clear();
	}

	public void initThreadPoolSetResult() {
	    lockWriteThreadPoolSetResult.lock();
	    try {
            if (threadPoolSetResult != null) {
                // shutdown thread pool
                threadPoolSetResult.shutdown();
                try {
                    if (threadPoolSetResult.awaitTermination(30, TimeUnit.SECONDS)) {
                        threadPoolSetResult.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    threadPoolSetResult.shutdownNow();
                }
            }
            threadPoolSetResult = Executors.newFixedThreadPool(EngineConfiguration.getInstance().getIntValue("queue.sendResult.poolSizeFBSetResult", 10));
        } finally {
	        lockWriteThreadPoolSetResult.unlock();
        }
    }

    /**
     * Return next TS for next job remove oldest Test request
     * @return
     */
    public long getDateNextJobRemoveOldestTest() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("trigerRemoveOldestTestTask", "QueueMgr"));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    public ExecutorService getThreadPoolSetResult() {
        return threadPoolSetResult;
    }

    public boolean isMeterTimeEnable() {
        return EngineConfiguration.getInstance().getIntValue("queue.meterTimeEnable", 0) ==1;
    }

    public int getMeterTimeMaxSize() {
        return EngineConfiguration.getInstance().getIntValue("queue.meterTimeMaxSize", 1000);
    }

    public Logger getLog() {
        return log;
    }

    /**
	 * add timestamp to stack of time for get method
	 * @param ts
	 */
	private void pushStackTimeGet(long ts) {
        if (isMeterTimeEnable()) {
            meterTimeGet.add(ts);
            if (meterTimeGet.size() > 2 * getMeterTimeMaxSize()) {
                synchronized (meterTimeGet) {
                    while (meterTimeGet.size() > getMeterTimeMaxSize()) {
                        meterTimeGet.remove();
                    }
                }
            }
        }
	}
	
	/**
	 * add timestamp to stack of time for get method
	 * @param ts
	 */
	private void pushStackTimeGetEmpty(long ts) {
        if (isMeterTimeEnable()) {
            meterTimeGetEmpty.add(ts);
            if (meterTimeGetEmpty.size() > 2 * getMeterTimeMaxSize()) {
                synchronized (meterTimeGetEmpty) {
                    while (meterTimeGetEmpty.size() > getMeterTimeMaxSize()) {
                        meterTimeGetEmpty.remove();
                    }
                }
            }
        }
	}
	
	/**
	 * Return average time for add method. Loop on stack time for get method to compute average timestamp
	 * @return
	 */
	public double getAverageTimeGet() {
        try {
            Long[] tabTime = meterTimeGet.toArray(new Long[meterTimeGet.size()]);
            long total = 0;
            int nbProcess = 0;
            if (tabTime.length > 0) {
                while (true) {
                    total += tabTime[tabTime.length - 1 - nbProcess];
                    nbProcess++;
                    if (nbProcess >= tabTime.length) {
                        break;
                    }
                    if (nbProcess > getMeterTimeMaxSize()) {
                        break;
                    }
                }
            }
            if (nbProcess > 0) {
                return NumericalTools.round(total / nbProcess, 3);
            }
        } catch (Exception e) {
            log.error("Failed to compute average time Get !", e);
        }
        return 0;
	}
	
	/**
	 * Return average time for add method. Loop on stack time for get method to compute average timestamp
	 * @return
	 */
	public double getAverageTimeGetEmpty() {
        try {
            Long[] tabTime = meterTimeGetEmpty.toArray(new Long[meterTimeGetEmpty.size()]);
            long total = 0;
            int nbProcess = 0;
            if (tabTime.length > 0) {
                while (true) {
                    total += tabTime[tabTime.length - 1 - nbProcess];
                    nbProcess++;
                    if (nbProcess >= tabTime.length) {
                        break;
                    }
                    if (nbProcess > getMeterTimeMaxSize()) {
                        break;
                    }
                }
            }
            if (nbProcess > 0) {
                return NumericalTools.round(total / nbProcess, 3);
            }
        } catch (Exception e) {
            log.error("Failed to compute average time GetEmpty !", e);
        }
        return 0;
	}
	
	public List<QueueData> getQueueDataList() {
		List<QueueData> listData = new ArrayList<QueueData>(queueRequest.values()); 
		Collections.sort(listData);
		return listData;
	}

    public List<QueueData> getTestQueueDataList() {
        List<QueueData> listData = new ArrayList<QueueData>(queueRequestTest.values());
        Collections.sort(listData);
        return listData;
    }

    public List<QueueData> getQueueCompareDataList() {
        List<QueueData> listData = new ArrayList<QueueData>(queueRequestCompare.values());
        Collections.sort(listData);
        return listData;
    }

    public void removeOldestTestQueueDataList() {
	    int nbRequestTestRemove = 0;
	    int nbSeconds = EngineConfiguration.getInstance().getIntValue("user.engineForTest.removeOldestAfterNbSeconds", 2*60);
	    for (Iterator<Map.Entry<Long, QueueData>> it = queueRequestTest.entrySet().iterator(); it.hasNext();) {
	        Map.Entry<Long, QueueData> e = it.next();
	        if (System.currentTimeMillis() - e.getValue().timestamp > (nbSeconds*1000)) {
	            it.remove();
                nbRequestTestRemove++;
            }
        }
        log.warn("Nb oldest request TEST nbSeconds="+nbSeconds+" - nbRequestTestRemove="+nbRequestTestRemove);
    }

    public void removeOldestQueueCompareDataList() {
        int nbRequestCompareRemove = 0;
        int nbSeconds = EngineConfiguration.getInstance().getIntValue("user.engineForCompare.removeOldestAfterNbSeconds", 2*60);
        for (Iterator<Map.Entry<Long, QueueData>> it = queueRequestCompare.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Long, QueueData> e = it.next();
            if (System.currentTimeMillis() - e.getValue().timestamp > (nbSeconds*1000)) {
                it.remove();
                nbRequestCompareRemove++;
            }
        }
        log.warn("Nb oldest request COMPARE nbSeconds="+nbSeconds+" - nbRequestCompareRemove="+nbRequestCompareRemove);
    }
	
	/**
	 * Total size of the queue
	 * @return
	 */
	public int getQueueTestSize() {
		return queueRequestTest.size();
	}

	public ConcurrentHashMap getQueueTestMap() {
	    return queueRequestTest;
    }

    /**
     * Total size of the queue
     * @return
     */
    public int getQueueCompareSize() {
        return queueRequestCompare.size();
    }

    public ConcurrentHashMap getQueueCompareMap() {
        return queueRequestCompare;
    }

    /**
     * Total size of the queue
     * @return
     */
    public int getQueueSize() {
        return queueRequest.size();
    }

    public ConcurrentHashMap getQueueMap() {
        return queueRequest;
    }
	
	/**
	 * Queue size for elements with no engine
	 * @return
	 */
	public int getQueueSizeNoEngine() {
		int size = 0;
		List<QueueData> listData = new ArrayList<QueueData>(queueRequest.values());
		for (QueueData qd : listData) {
			if (qd.getNbEngine() == 0) {
				size++;
			}
		}
		return size;
	}

    /**
     * Return nb waiting request since nbMasWaiting. If engineID = 0, no check engineID
     * @param engineID
     * @param nbMsWaiting
     * @return
     */
	public int getNbWaitingForEngine(long engineID, long nbMsWaiting) {
	    int result = 0;
        List<QueueData> listData = new ArrayList<QueueData>(queueRequest.values());
        for (QueueData qd : listData) {
            if ((engineID == 0 || qd.isEngineComputing(engineID)) && (System.currentTimeMillis() - qd.timestamp) > nbMsWaiting) {
                result++;
            }
        }
	    return result;
    }

	/**
	 * Queue size for elements with at least 1 engine
	 * @return
	 */
	public int getQueueSizeEngine() {
		int size = 0;
		List<QueueData> listData = new ArrayList<QueueData>(queueRequest.values());
		for (QueueData qd : listData) {
			if (qd.getNbEngine() > 0) {
				size++;
			}
		}
		return size;
	}

	/**
     * Return list of data to compute.
     * @param user
     * @param nbMax max size of list to return
     * @return
     */
    public List<QueueData> getNextDataList(UserVirtualEngine user, int nbMax) {
        List<QueueData> listNextData = new ArrayList<>();
        if (user != null) {
            int nbMaxEngineByRequest = EngineConfiguration.getInstance().getIntValue("request.nbMaxEngineComputingRequest", 2);
            if (nbMaxEngineByRequest == 0) {
                nbMaxEngineByRequest = 1;
            }
            int nbSecondsBeforeAddEngine = EngineConfiguration.getInstance().getIntValue("request.nbSecondsBeforeAddEngine",0);
            List<QueueData> listData = new ArrayList<QueueData>(queueRequest.values());
            Collections.sort(listData);
            Iterator<QueueData> it = listData.iterator();
            boolean bDataFound = false;
            while (it.hasNext()) {
                QueueData data = it.next();
                if (data != null) {
                    long dataID = data.ID;
                    synchronized (lockData.getLock("" + dataID)) {
                        try {
                            if (data.getNbEngine() < nbMaxEngineByRequest &&
                                    !data.isEngineComputing(user.getID()) &&
                                    isEngineCompatibleWithRequest(data, user)) {
                                // if an engine already compute it, check if the request is too long waiting
                                if (data.getNbEngine() >= 1) {
                                    if ((System.currentTimeMillis() - data.timestamp) > (nbSecondsBeforeAddEngine * 1000)) {
                                        // request waiting time is long ... try with this user !
                                        bDataFound = true;
                                    }
                                }
                                // no engine => it is for this user !
                                else {
                                    bDataFound = true;
                                }
                                if (bDataFound) {
                                    data.addEngineComputing(user.getID());
                                    user.incrementNbRequestsInProgress();
                                    listNextData.add(data);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Exception on addEngineComputing - exception=" + e.getMessage(), e);
                        }
                    }
                }
                if (listNextData.size() >= nbMax) {
                    // stop, the list of request is full
                    break;
                }
            }
        }
        return listNextData;
    }

	/**
	 * Return a data to compute. The engine associated to this userID doesn't have to already compute it in another thread
	 * @param user
	 * @return
	 */
	private QueueData getNextData(UserVirtualEngine user) {
		if (user != null) {
			int nbMaxEngineByRequest = EngineConfiguration.getInstance().getIntValue("request.nbMaxEngineComputingRequest", 2);
			if (nbMaxEngineByRequest == 0) {
				nbMaxEngineByRequest = 1;
			}
			int nbSecondsBeforeAddEngine = EngineConfiguration.getInstance().getIntValue("request.nbSecondsBeforeAddEngine",0);
			List<QueueData> listData = new ArrayList<QueueData>(queueRequest.values()); 
			Collections.sort(listData);
			Iterator<QueueData> it = listData.iterator();
			boolean bDataFound = false;
			while (it.hasNext()) {
				QueueData data = it.next();
				long dataID = data.ID;
				synchronized (lockData.getLock(""+dataID)) {
					try {
						if (data != null && data.getNbEngine() < nbMaxEngineByRequest &&
							!data.isEngineComputing(user.getID()) &&
							isEngineCompatibleWithRequest(data, user)) {
							// if an engine already compute it, check if the request is too long waiting
							if (data.getNbEngine() >= 1) {
								if ((System.currentTimeMillis() - data.timestamp) > (nbSecondsBeforeAddEngine*1000)) {
									// request waiting time is long ... try with this user !
									bDataFound = true;
								}
							}
							// no engine => it is for this user !
							else {
								bDataFound = true;
							}
							if (bDataFound) {
								data.addEngineComputing(user.getID());
								user.incrementNbRequestsInProgress();
							}
						}
					} catch (Exception e) {
						log.error("Exception on addEngineComputing - exception="+e.getMessage(), e);
					}
				}
				if (bDataFound) {
					return data;
				}
			}
		}
		return null;
	}
	
	/**
	 * Return a queue data associated to this request
	 * @param request
	 * @return null if queue has no entry for this request
	 */
	private QueueData getDataForRequest(String request) {
		Iterator<QueueData> it = queueRequest.values().iterator();
		while (it.hasNext()) {
			QueueData data = it.next();
			if (data.getRequest().equals(request)) {
				return data;
			}
		}
		return null;
	}
	
	/**
	 * Return the next index
	 * @return
	 */
	private synchronized long getNextIndex() {
		index++;
		return index;
	}

    /**
     * Return the next index for TEST
     * @return
     */
    private synchronized long getNextIndexTest() {
        indexTest++;
        return indexTest;
    }

    /**
     * Return the next index for COMPARE
     * @return
     */
    private synchronized long getNextIndexCompare() {
        indexCompare++;
        return indexCompare;
    }

	public boolean useEngineWS() {
	    int percent = EngineConfiguration.getInstance().getIntValue("queue.useEngineWSpercent", 0);
	    if (percent == 0) {
	        return false;
        }
        if (percent == 100) {
	        return true;
        }
        return randomUseWS.nextInt(100) <= percent;
    }

    /**
     * Cretae queue data
     * @param request
     * @param saveInCache
     * @param asyncID
     * @param urlSetResult
     * @param user
     * @param logStat
     * @return
     */
    public QueueData createData(String request, boolean saveInCache, String asyncID, String urlSetResult, UserVirtualFBServer user, boolean logStat) {
        QueueData newdata = new QueueData();
        newdata.ID = getNextIndex();
        newdata.saveInCache = saveInCache;
        newdata.addAsyncID(asyncID);
        newdata.setRequest(request);
        newdata.timestamp = System.currentTimeMillis();
        newdata.setUrlSetResult(urlSetResult);
        newdata.setUser(user);
        newdata.setLogStat(logStat);
        return newdata;
    }

    /**
     * Add data to the queue
     * @param data
     */
    public void addDataToQueue(QueueData data) {
        if (data != null) {
            queueRequest.put(data.ID, data);
            UserVirtualEngine u = userVirtualMgr.findUserEngineWS(data.getEngineVersion());
            if (u != null) {
                data.addEngineComputing(u.getID());
                try {
                    if (!u.getWebSocket().sendCommandCompute(data)) {
                        data.removeEngineComputing(u.getID());
                    } else {
                        sendDataForTestEngine(data, u);
                    }
                } catch (Exception e) {
                    log.error("Exception to send compute to user=" + u + " - data=" + data, e);
                    data.removeEngineComputing(u.getID());
                }
            }
        }
    }

	/**
	 * Create data and add it in the queue.
	 * @param request
     * @param saveInCache
     * @param asyncID
     * @param urlSetResult
	 * @return data added to the queue
	 */
	public QueueData createAndAddData(String request, boolean saveInCache, String asyncID, String urlSetResult, UserVirtualFBServer user, boolean logStat) {
        QueueData newdata = new QueueData();
        newdata.ID = getNextIndex();
        newdata.saveInCache = saveInCache;
        newdata.addAsyncID(asyncID);
        newdata.setRequest(request);
        newdata.timestamp = System.currentTimeMillis();
        newdata.setUrlSetResult(urlSetResult);
        newdata.setUser(user);
        newdata.setLogStat(logStat);
        queueRequest.put(newdata.ID, newdata);
        if (useEngineWS()) {
            UserVirtualEngine u = userVirtualMgr.findUserEngineWS(newdata.getEngineVersion());
            if (u != null) {
                newdata.addEngineComputing(u.getID());
                try {
                    if (!u.getWebSocket().sendCommandCompute(newdata)) {
                        newdata.removeEngineComputing(u.getID());
                    } else {
                        sendDataForTestEngine(newdata, u);
                    }
                } catch (Exception e) {
                    log.error("Exception to send compute to user=" + u + " - data=" + newdata, e);
                    newdata.removeEngineComputing(u.getID());
                }
            }
        }
        return newdata;
	}

    public void sendDataForTestEngine(QueueData data, UserVirtualEngine engineOrigin) {
        if (EngineConfiguration.getInstance().getIntValue("user.engineForTest.enable", 1) == 1) {
            int randomBase = EngineConfiguration.getInstance().getIntValue("user.engineForTest.randomBase", 100);
            int randomSet = EngineConfiguration.getInstance().getIntValue("user.engineForTest.randomSet", 1);
            String copyFromOrigin = EngineConfiguration.getInstance().getStringValue("user.engineForTest.copyFromOrigin", null);
            boolean sendData = false;
            if (copyFromOrigin == null || copyFromOrigin.length() == 0 || engineOrigin == null || engineOrigin.getLogin() == null) {
                sendData = (int)(Math.random() * randomBase) <= randomSet;
            } else {
                sendData = copyFromOrigin.equals(engineOrigin.getLogin());
            }
            if (sendData) {
                QueueData dataToSend = data;
                String strChangeEngineVersion = EngineConfiguration.getInstance().getStringValue("user.engineForTest.changeEngineVersion", null);
                if (strChangeEngineVersion != null) {
                    int changeEngineVersion = extractEngineVersionForTest(strChangeEngineVersion);
                    if (changeEngineVersion > 0 && changeEngineVersion != data.getEngineVersion()) {
                        dataToSend = new QueueData();
                        dataToSend.copyFrom(data);
                        dataToSend.ID = getNextIndexTest();
                        dataToSend.changeRequestEngineVersion(changeEngineVersion);
                    }
                }
                boolean dataInQueue = false;
                for (UserVirtualEngine engine : userVirtualMgr.getListEngineForTest()) {
                    String strChangeEngineVersionUser = EngineConfiguration.getInstance().getStringValue("user.engineForTest.changeEngineVersion."+engine.getLogin(), null);
                    if (strChangeEngineVersionUser != null) {
                        int changeEngineVersion = extractEngineVersionForTest(strChangeEngineVersion);
                        if (changeEngineVersion > 0 && changeEngineVersion != data.getEngineVersion()) {
                            dataToSend = new QueueData();
                            dataToSend.copyFrom(data);
                            dataToSend.ID = getNextIndexTest();
                            dataToSend.changeRequestEngineVersion(changeEngineVersion);
                        }
                    }
                    try {
                        if (engine.getWebSocket() != null && engine.containsEngineVersion(dataToSend.getEngineVersion())) {
                            if (!dataInQueue) {
                                queueRequestTest.put(dataToSend.ID, dataToSend);
                                dataInQueue = true;
                            }
                            engine.getWebSocket().sendCommandCompute(dataToSend);
                        }
                    } catch (Exception e) {
                        log.error("Failed to send command compute for engine=[" + engine + "] - data=[" + dataToSend + "]", e);
                    }
                }
            }
        }
    }

    private int extractEngineVersionForTest(String strEngine) {
        if (strEngine != null && strEngine.length() > 0) {
            String tabStrEngine[] = strEngine.split(";");
            if (tabStrEngine.length > 1){
                return Integer.parseInt(tabStrEngine[randomTestEngine.nextInt(tabStrEngine.length)]);
            } else if (tabStrEngine.length == 1) {
                return Integer.parseInt(tabStrEngine[0]);
            }
        }
        return 0;
    }
	
	/**
	 * Retrieve the different field for a request : 0=>DEAL, 1=>GAME, 2=>CONV, 3=>OPTIONS, 4=>TYPE, 5=>NB_TRICKS_FOR_CLAIM
	 * @param request
	 * @return
	 */
	private String[] getFieldsFromRequest(String request) {
		String[] result = new String[Constantes.REQUEST_NB_FIELD];
		if (request != null && request.length() > 0) {
			String[] temp = request.split(Constantes.REQUEST_FIELD_SEPARATOR);
			if (temp.length == Constantes.REQUEST_NB_FIELD) {
				result[Constantes.REQUEST_INDEX_FIELD_DEAL] = temp[Constantes.REQUEST_INDEX_FIELD_DEAL];
				result[Constantes.REQUEST_INDEX_FIELD_GAME] = temp[Constantes.REQUEST_INDEX_FIELD_GAME];
				result[Constantes.REQUEST_INDEX_FIELD_CONV] = temp[Constantes.REQUEST_INDEX_FIELD_CONV];
				result[Constantes.REQUEST_INDEX_FIELD_OPTIONS] = temp[Constantes.REQUEST_INDEX_FIELD_OPTIONS];
				result[Constantes.REQUEST_INDEX_FIELD_TYPE] = temp[Constantes.REQUEST_INDEX_FIELD_TYPE];
                result[Constantes.REQUEST_INDEX_FIELD_NB_TRICKS_FOR_CLAIM] = temp[Constantes.REQUEST_INDEX_FIELD_NB_TRICKS_FOR_CLAIM];
                result[Constantes.REQUEST_INDEX_FIELD_CLAIM_PLAYER] = temp[Constantes.REQUEST_INDEX_FIELD_CLAIM_PLAYER];
			}
		}
		return result;
	}
	
	/**
	 * Return a query to compute for this user
	 * @param user
	 * @return null if no query present in the queue for this user
	 */
	public WSComputeQuery getQueryForUser(UserVirtualEngine user) {
		long ts1 = System.currentTimeMillis();
		QueueData data = getNextData(user);
        logStatMgr.logInfo(false, (System.currentTimeMillis() - ts1), "getNextData - user="+user);
		long ts2 = System.currentTimeMillis();
		if (data != null) {
			pushStackTimeGet(ts2-ts1);
			WSComputeQuery query = new WSComputeQuery();
			query.setComputeID(data.ID);
			String[] temp = getFieldsFromRequest(data.getRequest());
			if (temp.length == Constantes.REQUEST_NB_FIELD) {
				query.setConventions(temp[Constantes.REQUEST_INDEX_FIELD_CONV]);
				query.setDeal(temp[Constantes.REQUEST_INDEX_FIELD_DEAL]);
				query.setGame(temp[Constantes.REQUEST_INDEX_FIELD_GAME]);
				query.setOptions(temp[Constantes.REQUEST_INDEX_FIELD_OPTIONS]);
				query.setQueryType(Integer.parseInt(temp[Constantes.REQUEST_INDEX_FIELD_TYPE]));
				query.setNbTricksForClaim(Integer.parseInt(temp[Constantes.REQUEST_INDEX_FIELD_NB_TRICKS_FOR_CLAIM]));
                query.setClaimPlayer(temp[Constantes.REQUEST_INDEX_FIELD_CLAIM_PLAYER]);
				return query;
			}
		} else {
			pushStackTimeGetEmpty(ts2-ts1);
		}
		return null;
	}

    /**
     * Return list of query to compute for this user
     * @param user
     * @param nbMax
     * @return null if no query present in the queue for this user
     */
    public List<WSComputeQuery> getListQueryForUser(UserVirtualEngine user, int nbMax) {
        List<WSComputeQuery> listResult = new ArrayList<>();
        long ts1 = System.currentTimeMillis();
        List<QueueData> listData = getNextDataList(user, nbMax);
        logStatMgr.logInfo(false, (System.currentTimeMillis() - ts1), "getNextDataList - user="+user+" - listData size="+listData!=null?listData.size():"-1");
        long ts2 = System.currentTimeMillis();
        if (listData != null && listData.size() > 0) {
            pushStackTimeGet(ts2-ts1);
            for (QueueData e : listData) {
                WSComputeQuery query = new WSComputeQuery();
                query.setComputeID(e.ID);
                String[] temp = getFieldsFromRequest(e.getRequest());
                if (temp.length == Constantes.REQUEST_NB_FIELD) {
                    query.setConventions(temp[Constantes.REQUEST_INDEX_FIELD_CONV]);
                    query.setDeal(temp[Constantes.REQUEST_INDEX_FIELD_DEAL]);
                    query.setGame(temp[Constantes.REQUEST_INDEX_FIELD_GAME]);
                    query.setOptions(temp[Constantes.REQUEST_INDEX_FIELD_OPTIONS]);
                    query.setQueryType(Integer.parseInt(temp[Constantes.REQUEST_INDEX_FIELD_TYPE]));
                    query.setNbTricksForClaim(Integer.parseInt(temp[Constantes.REQUEST_INDEX_FIELD_NB_TRICKS_FOR_CLAIM]));
                    query.setClaimPlayer(temp[Constantes.REQUEST_INDEX_FIELD_CLAIM_PLAYER]);
                    listResult.add(query);
                }
            }
        } else {
            pushStackTimeGetEmpty(ts2-ts1);
        }
        return listResult;
    }

    /**
     * Just check result and add alert if result is not valid.
     * Nothing to do with the result
     * @param requestID
     * @param result
     * @param user
     */
    public void setRequestTestResult(long requestID, String result, UserVirtual user) {
        QueueData data = null;
        synchronized (lockDataTest.getLock(""+requestID)) {
            data = queueRequestTest.get(requestID);
            queueRequestTest.remove(requestID);
        }

        if (data != null) {
            if (result == null) {
                alertMgr.saveAlert("TEST - Result is NULL", user.getLogin(), data);
            } else {
                if (result.startsWith(Constantes.REQUEST_ENGINE_NO_RESULT)) {
                    alertMgr.saveAlert("TEST - Result is " + result, user.getLogin(), data);
                }
            }
        }
    }

    /**
     * Just check result, compare it with previous value and add alert if result is the same.
     * Nothing to do with the result
     * @param requestID
     * @param result
     * @param user
     */
    public void setRequestCompareResult(long requestID, String result, UserVirtual user) {
        QueueData data = null;
        // retrieve data from queue
        synchronized (lockDataCompare.getLock(""+requestID)) {
            data = queueRequestCompare.get(requestID);
            queueRequestCompare.remove(requestID);
        }

        if (data != null) {
            if (result == null) {
                alertMgr.saveAlert("COMPARE - Result is NULL - previous result="+data.getResultOriginal(), user.getLogin(), data);
            } else if (result.startsWith(Constantes.REQUEST_ENGINE_NO_RESULT)) {
                alertMgr.saveAlert("COMPARE - Result is " + result+" - previous result="+data.getResultOriginal(), user.getLogin(), data);
            } else if (!result.equals(data.getResultOriginal())){
                alertMgr.saveAlert("COMPARE - Result is no the same ! previous result=" + data.getResultOriginal()+" - new result="+result, user.getLogin(), data);
            }
        }
    }

	/**
	 * Save result in cache and remove request from queue
	 * @param requestID
	 * @param result
     * @param user
	 * @return true if result added to cache, false if result already present
	 */
	public boolean setRequestResult(long requestID, String result, UserVirtual user) {
		boolean bReturn = false;
        if (log.isDebugEnabled()) {
            log.debug("lock data requestID=" + requestID + " - u=" + user+" - result="+result);
        }
        RequestService.GetResultParam paramBidInfo = null;
        String resultBidInfo = null;
        String resultCardSpread = null;
        long ts = System.currentTimeMillis();
        QueueData data = null;
		synchronized (lockData.getLock(""+requestID)) {
            data = queueRequest.get(requestID);
            logStatMgr.logInfo(data!=null?data.isLogStat():false, System.currentTimeMillis() - ts, "setRequestResult - getLock + queueRequest.get - result=" + result + " - data=" + data);
            if (data != null && result != null) {
                // delete request for queue
                queueRequest.remove(requestID);
            }
        }

        if (data != null) {
            data.setResultOriginal(result);
            // request always present in queue
			try {
                if (result == null) {
                    log.error("Result computed is null for data=" + data + " - user=" + user);
                    if (EngineConfiguration.getInstance().getIntValue("useAlertBDD", 1) == 1) {
                        alertMgr.saveAlert("Result is NULL", user.getLogin(), data);
                    } else {
                        alertMgr.sendAlertBlocage("Result is NULL - engine=" + user, data);
                    }
                } else {
                    boolean resultValid = true;
                    if (result.startsWith(Constantes.REQUEST_ENGINE_NO_RESULT)) {
                        resultValid = false;
                        String resultBefore = result;
                        log.warn("Result is not valid for data=" + data + " - user=" + user + " - result=" + result);
                        if (result.length() > Constantes.REQUEST_ENGINE_NO_RESULT.length()) {
                            result = result.substring(Constantes.REQUEST_ENGINE_NO_RESULT.length()).trim();
                            log.warn("Use default value from result=" + result + " - data=" + data);
                            resultValid = true;
                        } else if (data.getRequestType() == Constantes.REQUEST_TYPE_BID) {
                            if (EngineConfiguration.getInstance().getIntValue("general.resultNotValid.bidReplaceByPASS", 1) == 1) {
                                log.warn("Result is not valid for data=" + data + " - user=" + user + " - replace it by PA");
                                result = "PA";
                                resultValid = true;
                            }
                        } else if (data.getRequestType() == Constantes.REQUEST_TYPE_CLAIM) {

                        }
                        if (EngineConfiguration.getInstance().getIntValue("useAlertBDD", 1) == 1) {
                            alertMgr.saveAlert("Result is " + resultBefore, user.getLogin(), data);
                        } else {
                            alertMgr.sendAlertBlocage("Result is NOT_RESULT - engine=" + user + " - result=" + result, data);
                        }
                    }
                    if (resultValid) {
                        String resultToSave = result;
                        int idxSpace = result.indexOf(' ');
                        boolean bidAlert = false;
                        boolean cardSpread = false;
                        if (data.getRequestType() == Constantes.REQUEST_TYPE_BID &&
                                idxSpace > 0 &&
                                result.length() > (idxSpace + 1)) {
                            resultToSave = result.substring(0, idxSpace);
                            resultBidInfo = result.substring(idxSpace + 1);
                            paramBidInfo = new RequestService.GetResultParam();
                            paramBidInfo.deal = data.getDeal();
                            paramBidInfo.game = data.getGame() + resultToSave;
                            paramBidInfo.conventions = data.getConventions();
                            paramBidInfo.options = data.getOptions();
                            if (EngineConfiguration.getInstance().getIntValue("general.bidInfoResultType0", 1) == 1 &&
                                    paramBidInfo.options.length() == 10 &&
                                    paramBidInfo.options.charAt(5) == '1') {
                                // replace result type at position 5
                                paramBidInfo.options = paramBidInfo.options.substring(0, 5) + '0' + paramBidInfo.options.substring(6);
                            }
                            paramBidInfo.requestType = Constantes.REQUEST_TYPE_BID_INFO;
                            String[] dataReglette = resultBidInfo.split(";");
                            if (dataReglette.length == 14) {
                                int forcingValue = 0;
                                try {
                                    forcingValue = Integer.parseInt(dataReglette[13]);
                                } catch (Exception e) {
                                    log.error("Failed to parseInt for string=" + dataReglette[13]);
                                }
                                if (forcingValue >= 20) {
                                    bidAlert = true;
                                }
                            }
                            if (log.isDebugEnabled()) {
                                log.debug("Remove data from value=" + result + " - resultToSave=" + resultToSave + " - resultBidInfo=" + resultBidInfo);
                            }
                        }
                        else if (data.getRequestType() == Constantes.REQUEST_TYPE_CARD &&
                                idxSpace > 0 &&
                                result.length() > (idxSpace + 1)) {
                            resultToSave = result.substring(0, idxSpace);
                            resultCardSpread = result.substring(idxSpace + 1);
                            if (resultCardSpread.equals("1")) {
                                cardSpread = true;
                            }
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Result for request=" + data.getRequest() + " - resultToSave=" + resultToSave+" - bidAlert="+bidAlert+" - cardSpread="+cardSpread);
                        }
                        try {
                            if (data.saveInCache) {
                                // add result in cache
                                ts = System.currentTimeMillis();
                                boolean bAddData = treeMgr.addCacheData(data.getRequest(), resultToSave, bidAlert, cardSpread);
                                logStatMgr.logInfo(data.isLogStat(), System.currentTimeMillis() - ts, "setRequestResult - addCacheData - result=" + result + " - data=" + data);
                                if (!bAddData) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Data not added to cache ?! ...");
                                    }
                                }
                                if (EngineConfiguration.getInstance().getIntValue("general.logDebugAddCacheData", 0) == 1) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Data added bAddData=" + bAddData + " - data=" + data + " - resultToSave=" + resultToSave);
                                    }
                                }
                            } else {
                                if (EngineConfiguration.getInstance().getIntValue("general.logDebugAddCacheData", 0) == 1) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("No Data added - data=" + data + " - resultToSave=" + resultToSave);
                                    }
                                }
                            }
                            data.resultValue = resultToSave;
                            // set alert flag
                            if (bidAlert && EngineConfiguration.getInstance().getIntValue("general.setBidAlert", 1) == 1) {
                                data.resultValue += "a";
                            }
                            // set spread flag
                            if (cardSpread && EngineConfiguration.getInstance().getIntValue("general.setSpreadResult", 1) == 1) {
                                data.resultValue += "s";
                            }
                            // set result value to funbrige server
                            if (data.getSetAsyncID() != null) {
                                // asynchro method => call setResult on funbridge server
                                if (log.isDebugEnabled()) {
                                    log.debug("Call set  result - data=" + data);
                                }
                                ts = System.currentTimeMillis();
                                FBSetResultThread threadSetResult = new FBSetResultThread(this, data.resultValue, new ArrayList<>(data.getSetAsyncID()), data.getUrlSetResult(), data.getUser(), System.currentTimeMillis() - data.timestamp, data.isLogStat());
                                lockReadThreadPoolSetResult.lock();
                                try {
                                    threadPoolSetResult.execute(threadSetResult);
                                } catch (Exception e) {
                                    log.error("Exception to execute thread setResult", e);
                                }
                                finally{
                                    lockReadThreadPoolSetResult.unlock();
                                }
                                logStatMgr.logInfo(data.isLogStat(), System.currentTimeMillis() - ts, "setRequestResult - execute thread setResult - result=" + result + " - data=" + data);
                            } else {
                                // synchro method => notify thread waiting from getResult service
                                ts = System.currentTimeMillis();
                                if (log.isDebugEnabled()) {
                                    log.debug("Before notify data="+data);
                                }
                                synchronized (data) {
                                    data.notifyAll();
                                }
                                if (log.isDebugEnabled()) {
                                    log.debug("After notify data="+data);
                                }
                                logStatMgr.logInfo(data.isLogStat(), System.currentTimeMillis() - ts, "setRequestResult - data.notify - result=" + result + " - data=" + data);
                            }
                            bReturn = true;
                        } catch (Exception e) {
                            log.error("Exception setRequestResult request=" + requestID + " result=" + result + " - message : " + e.getMessage(), e);
                        }
                    }
                }
			}catch (Exception e){
				log.error("Exception setRequestResult request="+requestID+" result="+result+" - message : "+e.getMessage(), e);
			}
		} else {
            if (log.isDebugEnabled()) {
                log.debug("Data is null ! requestID=" + requestID + " - data=" + data+" - result="+result);
            }
        }
        if (paramBidInfo != null && paramBidInfo.isValid() && resultBidInfo != null && resultBidInfo.length() > 0) {
            if (EngineConfiguration.getInstance().getIntValue("queue.bidInfoTreeAddData", 1) == 1) {
                boolean bAddCacheData = treeMgr.addCacheData(paramBidInfo.getKey(), resultBidInfo, false, false);
                if (log.isDebugEnabled()) {
                    log.debug("AddCacheData return : " + bAddCacheData + " - key=" + paramBidInfo.getKey() + " - resultBidInfo=" + resultBidInfo);
                }
            } else {
                ts = System.currentTimeMillis();
                ContextManager.getRequestService().setResultBidInfoForBid(paramBidInfo, resultBidInfo);
                logStatMgr.logInfo(data!=null?data.isLogStat():false, System.currentTimeMillis() - ts, "setResultBidInfoForBid - paramBidInfo="+paramBidInfo+" - resultBidInfo=" + resultBidInfo);
            }
        }

        // Use data for compare
        if (EngineConfiguration.getInstance().getIntValue("user.engineForCompare.enable", 0) == 1) {
            // only for data computed by a engine
            String copyFrom = EngineConfiguration.getInstance().getStringValue("user.engineForTest.copyFromOrigin", null);
            if (copyFrom != null && user.getLogin().equals(copyFrom)) {
                // need to change engine version ?
                int engineVersionForCompare = EngineConfiguration.getInstance().getIntValue("user.engineForCompare.changeEngineVersion", data.getEngineVersion());
                // Find engine to compare
                UserVirtualEngine engineForCompare = userVirtualMgr.getEngineForCompare(engineVersionForCompare);
                if (engineForCompare != null) {
                    QueueData dataForCompare = new QueueData();
                    dataForCompare.copyFrom(data);
                    dataForCompare.changeRequestEngineVersion(engineVersionForCompare);
                    dataForCompare.setForCompare(true);
                    dataForCompare.ID = getNextIndexCompare();
                    // add data in queue compare
                    queueRequestCompare.put(dataForCompare.ID, dataForCompare);
                    try {
                        // send command compute to engine for compare
                        engineForCompare.getWebSocket().sendCommandCompute(dataForCompare);
                    } catch (Exception e) {
                        log.error("Exception to send command compute for engine=["+engineForCompare+"] - dataForCompare=["+dataForCompare+"]", e);
                    }
                }
            }

        }
        return bReturn;
	}
	
	/**
	 * Remove a data from queue
	 * @param dataID
	 */
	public void removeData(long dataID) {
		synchronized (lockData.getLock(""+dataID)) {
			try {
                if (log.isDebugEnabled()) {
                    log.debug("Remove queueRequest dataID=" + dataID);
                }
				queueRequest.remove(dataID);
			} catch (Exception e) {
				log.error("Exception on remove dataID="+dataID+" - exception="+e.getMessage(), e);
			}
		}
	}

    /**
     * Remove a data from queue
     * @param dataID
     */
    public void removeTestData(long dataID) {
        synchronized (lockDataTest.getLock(""+dataID)) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Remove queueRequestTest dataID=" + dataID);
                }
                queueRequestTest.remove(dataID);
            } catch (Exception e) {
                log.error("Exception on remove TEST dataID="+dataID+" - exception="+e.getMessage(), e);
            }
        }
    }

    /**
     * Remove a data from queue
     * @param dataID
     */
    public void removeCompareData(long dataID) {
        synchronized (lockDataCompare.getLock(""+dataID)) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Remove queueRequestCompare dataID=" + dataID);
                }
                queueRequestCompare.remove(dataID);
            } catch (Exception e) {
                log.error("Exception on remove COMPARE dataID="+dataID+" - exception="+e.getMessage(), e);
            }
        }
    }
	
	/**
	 * Return the value of current index
	 * @return
	 */
	public long getCurrentIndex() {
		return index;
	}

    /**
     * Return the value of current index TEST
     * @return
     */
    public long getCurrentIndexTest() {
        return indexTest;
    }
	
	/**
	 * Remove engine computing this data.
	 * @param dataID
	 * @param userID
	 */
	public void removeEngineOnData(long dataID, long userID) {
		synchronized (lockData.getLock(""+dataID)) {
			try {
				QueueData data = queueRequest.get(dataID);
				if (data != null) {
					data.removeEngineComputing(userID);
				}
			} catch (Exception e) {
				log.error("Exception to get dataID="+dataID+" from queue or to remove engine computing", e);
			}
		}
	}
	
	/**
	 * Clear engine list computing this data. The data can be used by other engine.
	 * @param dataID
	 */
	public void resetEngineOnData(long dataID) {
		synchronized (lockData.getLock(""+dataID)) {
			try {
				QueueData data = queueRequest.get(dataID);
				if (data != null) {
					data.resetEngineComputing();
				}
			} catch (Exception e) {
				log.error("Exception to get dataID="+dataID+" from queue or to reset engine computing", e);
			}
		}
	}
	
	private boolean isEngineCompatibleWithRequest(QueueData data, UserVirtualEngine user) {
		if (data != null && user != null) {
			int version = data.getEngineVersion();
			if (version > 0) {
				return user.containsEngineVersion(version);
			}
		} else {
			log.error("Request or user is null ! data="+data+" - user="+user);
		}
		return false;
	}
	
	/**
	 * Load polling data from configuration file
	 */
	public void loadPollingData() {
		loadingPollingData = true;
		try {
			Thread.sleep(100);
			mapPollingUser.clear();
			pollingCompute = true;
			// default value
			pollingDefaultValue = EngineConfiguration.getInstance().getIntValue("queue.polling.defaultValue", 0);
			// polling users
			pollingUsers = EngineConfiguration.getInstance().getStringValue("queue.poling.users", "").split(";");
			if (pollingUsers != null) {
				for (int i = 0; i < pollingUsers.length; i++) {
					PollingUserSettings pus = new PollingUserSettings();
					pus.compute = EngineConfiguration.getInstance().getIntValue("queue.polling."+pollingUsers[i]+".compute", 0) == 1;
					pus.defaultValue = EngineConfiguration.getInstance().getIntValue("queue.polling."+pollingUsers[i]+".defaultValue", 0);
					mapPollingUser.put(pollingUsers[i], pus);
				}
			}
			
			// polling functions
			pollingThresholdMiddle.threshold = EngineConfiguration.getInstance().getIntValue("queue.polling.compute.middle", 1);
			pollingThresholdMiddle.value = EngineConfiguration.getInstance().getIntValue("queue.polling.compute.middleValue", 5);
			pollingThresholdSmaller.threshold = EngineConfiguration.getInstance().getIntValue("queue.polling.compute.limitSmaller", 0);
			pollingThresholdSmaller.value = EngineConfiguration.getInstance().getIntValue("queue.polling.compute.limitSmallerValue", 100);
			pollingThresholdGreater.threshold = EngineConfiguration.getInstance().getIntValue("queue.polling.compute.limitGreater", 2);
			pollingThresholdGreater.value = EngineConfiguration.getInstance().getIntValue("queue.polling.compute.limitGreaterValue", 1);
			pollingComputeFunctionSmaller = new PollingFunction();
			pollingComputeFunctionSmaller.a = EngineConfiguration.getInstance().getIntValue("queue.polling.compute.functionSmaller.a", 0);
			pollingComputeFunctionSmaller.b = EngineConfiguration.getInstance().getIntValue("queue.polling.compute.functionSmaller.b", 0);
			
			pollingComputeFunctionGreater = new PollingFunction();
			pollingComputeFunctionGreater.a = EngineConfiguration.getInstance().getIntValue("queue.polling.compute.functionGreater.a", 0);
			pollingComputeFunctionGreater.b = EngineConfiguration.getInstance().getIntValue("queue.polling.compute.functionGreater.b", 0);

		} catch (Exception e) {
			pollingCompute = false;
			log.error("Exception loading polling data ... pollingCompute="+pollingCompute, e);
		}
		loadingPollingData = false;
	}
	
	/**
	 * Return the current polling value. Compute it or return default value. According to queue size and polling values defined in configuration
	 * @param user user type
	 * @return
	 */
	public int getPollingValue(String user) {
		try {
			while (loadingPollingData) {
				Thread.sleep(10);
			}
		} catch (Exception e) {
			log.error("Exception waiting loading polling data ...", e);
		}
		int pollingVal = pollingDefaultValue;
		if (pollingCompute) {
			PollingUserSettings pus = getPollingSettingForUser(user);
			if (pus != null) {
				if (pus.compute) {
					int nbEngine = sessionMgr.getNbEngineConnected();
					if (nbEngine == 0 || nbEngine == 1) {
						pollingVal = pollingThresholdMiddle.value;
					}
					else {
						int queueSize = getQueueSize();
						double computeValue = (double)queueSize / nbEngine;
						if (computeValue <= pollingThresholdSmaller.threshold) {
							pollingVal = pollingThresholdSmaller.value;
						} else if (computeValue < pollingThresholdMiddle.threshold) {
							pollingVal = (int)pollingComputeFunctionSmaller.getValue(computeValue);
						} else if (computeValue == pollingThresholdMiddle.threshold) {
							pollingVal = pollingThresholdMiddle.value;
						} else if (computeValue > pollingThresholdMiddle.threshold && computeValue < pollingThresholdGreater.threshold) {
							pollingVal = (int)pollingComputeFunctionGreater.getValue(computeValue);
						} else if (computeValue >= pollingThresholdGreater.threshold) {
							pollingVal = pollingThresholdGreater.value;
						}
					}
				} else {
					pollingVal = pus.defaultValue;
				}
			}
		}
		return pollingVal;
	}
	
	public PollingUserSettings getPollingSettingForUser(String user) {
		return mapPollingUser.get(user);
	}
	
	public String[] getPollingUsers() {
		return pollingUsers;
	}
	
	public PollingFunction getPollingFunctionSmaller() {
		return pollingComputeFunctionSmaller;
	}
	
	public PollingFunction getPollingFunctionGreater() {
		return pollingComputeFunctionGreater;
	}
	
	public PollingThreshold getPollingThresholdMiddle() {
		return pollingThresholdMiddle;
	}
	
	public PollingThreshold getPollingThresholdSmaller() {
		return pollingThresholdSmaller;
	}
	
	public PollingThreshold getPollingThresholdGreater() {
		return pollingThresholdGreater;
	}
	
	public boolean isPollingCompute() {
		return pollingCompute;
	}
	
	public int getPollingDefaultValue() {
		return pollingDefaultValue;
	}

    public void sendResult(String result, List<String> listAsyncID, String url, UserVirtualFBServer userFBServer, long ts, boolean logStat) {
	    long beginTS = System.currentTimeMillis();
        if (url == null || url.length() == 0) {
            url = EngineConfiguration.getInstance().getStringValue("queue.sendResult.urlFBSetResult", null);
        }
        int timeout = EngineConfiguration.getInstance().getIntValue("queue.sendResult.timeoutFBSetResult", 10) * 1000;
        if (log.isDebugEnabled()) {
            log.debug("Send result - url="+url+" - result="+result+" - listAsyncID="+ StringTools.listToString(listAsyncID));
        }
        requestServiceImpl.addMeterTimeNoCache(ts);
        boolean callService = true;
        if (userFBServer != null) {
            try {
                if (userFBServer.isWebSocketEnable() && userFBServer.getWebSocket().sendResult(result, listAsyncID)) {
                    callService = false;
                }
            } catch (Exception e) {
                log.error("Exception webscoket to sendResult", e);
            }
        }
        boolean callServiceOK = false;
        if (callService) {
            if (url != null && url.length() > 0) {
                FBSetResultParam param = new FBSetResultParam();
                param.result = result;
                param.listAsyncID = new ArrayList<>();
                param.listAsyncID.addAll(listAsyncID);
                try {
                    serviceSetResult.callService(url, param, null, Object.class, timeout);
                    callServiceOK = true;
                } catch (Exception e) {
                    log.error("Exception to call setResult on FB server", e);
                }
            } else {
                log.error("URL is null !!");
            }
        }
        logStatMgr.logInfo(logStat, System.currentTimeMillis() - beginTS, "sendResult - data.notify - result=" + result + " - callService=" + callService+" - callServiceOK="+callServiceOK);
    }
}

