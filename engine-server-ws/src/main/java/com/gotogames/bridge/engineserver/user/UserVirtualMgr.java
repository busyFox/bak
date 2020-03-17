package com.gotogames.bridge.engineserver.user;

import com.gotogames.bridge.engineserver.common.AlertMgr;
import com.gotogames.bridge.engineserver.common.ContextManager;
import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import com.gotogames.bridge.engineserver.request.QueueMgr;
import com.gotogames.bridge.engineserver.request.data.QueueData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Singleton;
import java.util.*;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Component(value="userVirtualMgr")
@Singleton
public class UserVirtualMgr {
    @Resource(name = "alertMgr")
    private AlertMgr alertMgr = null;

    private Logger log = LogManager.getLogger(this.getClass());
	// map of user : loginID => userVirtual
	private ConcurrentHashMap<String, UserVirtual> mapUser = new ConcurrentHashMap<String, UserVirtual>();
    private CopyOnWriteArrayList<UserVirtualEngine> listEngineWS = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<UserVirtualEngine> listEngineForTest = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<UserVirtualEngine> listEngineForCompare = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<UserVirtualFBServer> listFBServerWS = new CopyOnWriteArrayList<>();
	private AtomicLong currentID = new AtomicLong(0);
    private Scheduler scheduler = null;
    private JobDetail taskRestartUpdateEngineWS = null, taskCheckEngineNoResult = null, taskRemoveUserNoActivity = null;
    private Random randomSelectEngine = new Random(System.nanoTime());
    private Random randomTestEngine = new Random(System.nanoTime());
    public boolean taskRestartUpdateEngineWSRunning = false, taskCheckEngineNoResultRunning = false, taskRemoveUserNoActivityRunning = false;

    public static final int CONFIG_INT_DEFAULVALUE = 999999999;
    public long lastQueueIndex = 0;
    public Set<String> listEngineNoResultStep1 = new HashSet<>();
    public Set<String> listEngineNoResultStep2 = new HashSet<>();
    public Set<String> listEngineNoResultAlert = new HashSet<>();

    public Set<String> getListEngineNoResultStep1() {
        return listEngineNoResultStep1;
    }

    public Set<String> getListEngineNoResultStep2() {
        return listEngineNoResultStep2;
    }

    public Set<String> getListEngineNoResultAlert() {
        return listEngineNoResultAlert;
    }

    @PostConstruct
	public void init() {
    }

    public void startup() {
        // create user simpleRequest
        loadSimpleRequestUserList();

        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();

            // remove user with no activity - trigger every 30 seconds
            taskRemoveUserNoActivity = JobBuilder.newJob(UserVirtualTaskRemoveUserNoActivity.class).withIdentity("taskRemoveUserNoActivity", "UserVirtual").build();
            CronTrigger triggerRemoveUserNoActivity = TriggerBuilder.newTrigger().withIdentity("triggerRemoveUserNoActivity", "UserVirtual").withSchedule(CronScheduleBuilder.cronSchedule("0/30 * * * * ?")).build();
            Date dateNextJobEnable = scheduler.scheduleJob(taskRemoveUserNoActivity, triggerRemoveUserNoActivity);
            log.warn("Sheduled for job=" + taskRemoveUserNoActivity.getKey() + " run at="+dateNextJobEnable+" - cron expression=" + triggerRemoveUserNoActivity.getCronExpression() + " - next fire=" + triggerRemoveUserNoActivity.getNextFireTime());

            // RestartUpdateEngine task - trigger every 5 mn
            taskRestartUpdateEngineWS = JobBuilder.newJob(UserVirtualTaskRestartUpdateEngineWS.class).withIdentity("taskRestartUpdateEngineWS", "UserVirtual").build();
            CronTrigger triggerRestartEngineWS = TriggerBuilder.newTrigger().withIdentity("triggerRestartUpdateEngineWS", "UserVirtual").withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?")).build();
            dateNextJobEnable = scheduler.scheduleJob(taskRestartUpdateEngineWS, triggerRestartEngineWS);
            log.warn("Sheduled for job=" + taskRestartUpdateEngineWS.getKey() + " run at="+dateNextJobEnable+" - cron expression=" + triggerRestartEngineWS.getCronExpression() + " - next fire=" + triggerRestartEngineWS.getNextFireTime());

            // CheckEngineNoResult task - trigger every 10 mn
            taskCheckEngineNoResult = JobBuilder.newJob(UserVirtualTaskCheckEngineNoResult.class).withIdentity("taskCheckEngineNoResult", "UserVirtual").build();
            CronTrigger triggerCheckEngineNoResult = TriggerBuilder.newTrigger().withIdentity("triggerCheckEngineNoResult", "UserVirtual").withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 * * * ?")).build();
            dateNextJobEnable = scheduler.scheduleJob(taskCheckEngineNoResult, triggerCheckEngineNoResult);
            log.warn("Sheduled for job=" + taskCheckEngineNoResult.getKey() + " run at="+dateNextJobEnable+" - cron expression=" + triggerCheckEngineNoResult.getCronExpression() + " - next fire=" + triggerCheckEngineNoResult.getNextFireTime());
        } catch (Exception e) {
            log.error("Exception to init scheduler", e);
        }
	}
	
	@PreDestroy
	public void destroy() {
		log.info("Clear map of user");
		mapUser.clear();
	}

    /**
     * Load simpleRequest users from configuration file
     */
	public void loadSimpleRequestUserList() {
        String listSimpleRequest = EngineConfiguration.getInstance().getStringValue("user.listSimpleRequest", null);
        if (listSimpleRequest != null) {
            String[] temp = listSimpleRequest.split(";");
            for (String e : temp) {
                UserVirtual u = getUserVirtualByLogin(e);
                if (u != null) {
                    removeUser(u, false);
                }
                u = createUserVirtual(e);
                log.warn("Create user simpleRequest - u="+u);
            }
        }
    }

    /**
     * Return next TS for startup JOB
     * @return
     */
    public long getDateNextJobRestartUpdateEngineWS() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerRestartUpdateEngineWS", "UserVirtual"));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    /**
     * Return next TS for startup JOB
     * @return
     */
    public long getDateNextJobCheckEngineNoResult() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerCheckEngineNoResult", "UserVirtual"));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    public Logger getLogger() {
        return log;
    }
	
	public Collection<UserVirtual> getListUser() {
		return mapUser.values();
	}
	
	public long getCurrentID() {
		return currentID.get();
	}
	
	public int getNbUser() {
		return mapUser.size();
	}
	
	public UserVirtual createUserVirtual(String login) {
        if (login == null) {
            return null;
        }
        String loginPrefix = login;
        if (login.startsWith("chinabridge")) {//fbserver
            loginPrefix = "chinabridge";//fbserver
        } else {
            while (loginPrefix.matches(".*\\d")) {
                loginPrefix = loginPrefix.substring(0, loginPrefix.length() - 1);
                if (loginPrefix.length() <= 1) {
                    break;
                }
            }
        }
		String password = getConfigStringValue(login, loginPrefix, "password", "");//EngineConfiguration.getInstance().getStringValue("user."+loginPrefix+".password", "");
		if (password == null || password.isEmpty()) {
			return null;
		}

		UserVirtual user = null;
        if (getConfigIntValue(login, loginPrefix, "engine",0) == 1) {
            user = new UserVirtualEngine(login, loginPrefix, password, currentID.incrementAndGet());
        } else {
            user = new UserVirtualFBServer(login, loginPrefix, password, currentID.incrementAndGet());
        }
        user.setTimeout(getConfigIntValue(login, loginPrefix, "timeout",-1));
        user.setTest(getConfigIntValue(login, loginPrefix, "test",0) == 1);
        user.setUseCache(getConfigIntValue(login, loginPrefix,"cache",1) == 1);
        mapUser.put(user.getLoginID(), user);
		return user;
	}

	public static String getConfigStringValue(String login, String loginPrefix, String param, String defaultValue) {
        String value = EngineConfiguration.getInstance().getStringValue("user."+login+"."+param, null);
        if (value != null) {
            return value;
        }
        value = EngineConfiguration.getInstance().getStringValue("user."+loginPrefix+"."+param, null);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    public static int getConfigIntValue(String login, String loginPrefix, String param, int defaultValue) {
        int value = EngineConfiguration.getInstance().getIntValue("user."+login+"."+param, CONFIG_INT_DEFAULVALUE);
        if (value != CONFIG_INT_DEFAULVALUE) {
            return value;
        }
        value = EngineConfiguration.getInstance().getIntValue("user."+loginPrefix+"."+param, CONFIG_INT_DEFAULVALUE);
        if (value != CONFIG_INT_DEFAULVALUE) {
            return value;
        }
        return defaultValue;
    }
	
	public boolean deleteUserVirtual(String loginID) {
        if (loginID == null || loginID.isEmpty()) {
			return false;
		}
		synchronized (mapUser) {
			boolean bDelete = (mapUser.remove(loginID) != null);
			log.debug("Delete user="+loginID+" - delete="+bDelete);
			return bDelete;
		}
	}

    public void removeUser(UserVirtual u, boolean closeWebSocket) {
        if (log.isDebugEnabled()) {
            log.debug("Remove user to compute - user="+u);
        }
        if (u != null) {
            if (u.isEngine()) {
                if (closeWebSocket) {
                    ((UserVirtualEngine) u).closeWebSocket();
                }
                listEngineWS.remove(u);
                listEngineForTest.remove(u);
                listEngineForCompare.remove(u);
            }
            else {
                if (u instanceof UserVirtualFBServer) {
                    if (closeWebSocket) {
                        ((UserVirtualFBServer) u).closeWebSocket();
                    }
                    listFBServerWS.remove(u);
                }
            }
            mapUser.remove(u.getLoginID());
        }
    }

	public UserVirtual getUserVirtual(String loginID) {
		if (loginID == null || loginID.isEmpty()) {
			return null;
		}
		return mapUser.get(loginID);
	}

    public UserVirtual getUserVirtualByLogin(String login) {
        if (login != null && login.length() > 0) {
            Collection<UserVirtual> colUser = mapUser.values();
            for (UserVirtual u : colUser) {
                if (u.getLogin() != null && u.getLogin().equals(login)) {
                    return u;
                }
            }
        }
        return null;
    }
	
	public UserVirtual getUserByChallenge(String challenge) {
		if (challenge == null || challenge.isEmpty()) {
			return null;
		}
		Collection<UserVirtual> colUser = mapUser.values();
		for (UserVirtual u : colUser) {
			if (u.getChallenge() != null && u.getChallenge().equals(challenge)) {
				return u;
			}
		}
		return null;
	}

	public void addUserEngine(UserVirtualEngine u) {
        if (log.isDebugEnabled()) {
            log.debug("Add user to compute - user="+u);
        }
        if (u.isWebSocketEnable()) {
            listEngineWS.add(u);
        }
        if (u.isForTest()) {
            listEngineForTest.add(u);
        }
        if (u.isForCompare()) {
            listEngineForCompare.add(u);
        }
    }

    public void addUserFBServer(UserVirtualFBServer u) {
        if (log.isDebugEnabled()) {
            log.debug("Add user to compute - user="+u);
        }
        if (u.isWebSocketEnable()) {
            listFBServerWS.add(u);
        }
    }

    public int removeAllUserEngine() {
        int nbRemove = 0;
        Iterator<UserVirtualEngine> it = listEngineWS.iterator();
        while(it.hasNext()) {
            UserVirtualEngine u = it.next();
            mapUser.remove(u.getLoginID());
            if (u.getWebSocket() != null) {
                u.getWebSocket().closeSession();
            }
            it.remove();
            nbRemove++;
            listEngineForTest.remove(u);
            listEngineForCompare.remove(u);
        }
        return nbRemove;
    }

    public int removeAllUserServer() {
        int nbRemove = 0;
        Iterator<UserVirtualFBServer> it = listFBServerWS.iterator();
        while(it.hasNext()) {
            UserVirtualFBServer u = it.next();
            mapUser.remove(u.getLoginID());
            if (u.getWebSocket() != null) {
                u.getWebSocket().closeSession();
            }
            it.remove();
            nbRemove++;
        }
        return nbRemove;
    }

    public CopyOnWriteArrayList<UserVirtualEngine> getListEngineForTest() {
        return listEngineForTest;
    }

    public UserVirtualEngine getEngineForCompare(int engineVersion) {
        Iterator<UserVirtualEngine> it = listEngineWS.iterator();
        while(it.hasNext()) {
            UserVirtualEngine e = it.next();
            if (!e.isEnable()) {
                continue;
            }
            if (!e.isWebSocketEnable()) {
                continue;
            }
            if (e.isForTest()) {
                continue;
            }
            if (!e.containsEngineVersion(engineVersion)) {
                continue;
            }
            if (e.isForCompare()) {
                return e;
            }
        }
        return null;
    }

    /**
     * Return user with FbMoteur type and websocket enable
     * @param engineVersion
     * @return
     */
    public UserVirtualEngine findUserEngineWS(int engineVersion) {
        String configMethod = EngineConfiguration.getInstance().getStringValue("user.methodFindUserEngineWS", "method1");
        if (configMethod.equals("method1")) {
            return findUserEngineWS_Method1(engineVersion);
        }
        else if (configMethod.equals("method2")) {
            return findUserEngineWS_Method2(engineVersion);
        }
        else if (configMethod.equals("method3")) {
            return findUserEngineWS_Method3(engineVersion);
        }
        else if (configMethod.equals("method4")) {
            return findUserEngineWS_Method4(engineVersion);
        }
        else if (configMethod.equals("methodRandom")) {
            return findUserEngineWS_MethodRandom(engineVersion);
        }
        return null;
    }

    /**
     * Search engine with higher nb thread available - small queue size and small nb compute
     * @param engineVersion
     * @return
     */
	public UserVirtualEngine findUserEngineWS_Method1(int engineVersion) {
        // loop on each user and compare to find one with the high nb thread or the low queue size
        UserVirtualEngine engineSelected = null;

        Iterator<UserVirtualEngine> it = listEngineWS.iterator();
        while(it.hasNext()) {
            UserVirtualEngine e = it.next();
            if (!e.isEnable()) {
                continue;
            }
            if (!e.isWebSocketEnable()) {
                continue;
            }
            if (!e.containsEngineVersion(engineVersion)) {
                continue;
            }
            if (e.isForTest()) {
                continue;
            }
            if (e.isForCompare()) {
                continue;
            }
            if (engineSelected == null) {
                engineSelected = e;
            } else {
                // choose engine with higher nbThread
                if (engineSelected.getNbThread() < e.getNbThread()) {
                    engineSelected = e;
                }
                // same nbThread => choose the one with small queue size
                else if (engineSelected.getNbThread() == e.getNbThread()) {
                    if (engineSelected.getQueueSize() > e.getQueueSize()) {
                        engineSelected = e;
                    }
                    // same queue size => choose the one with small nb compute
                    else if (engineSelected.getQueueSize() == e.getQueueSize()) {
                        if (engineSelected.getNbCompute() > e.getNbCompute()) {
                            engineSelected = e;
                        }
                    }
                }
            }
        }
        return engineSelected;
    }

    /**
     * Search engine with higher percent thread available - small nb request in progress and small nb compute
     * @param engineVersion
     * @return
     */
    public UserVirtualEngine findUserEngineWS_Method2(int engineVersion) {
        // loop on each user and compare to find one with the high nb thread or the low queue size
        UserVirtualEngine engineSelected = null;
        Iterator<UserVirtualEngine> it = listEngineWS.iterator();
        while(it.hasNext()) {
            UserVirtualEngine e = it.next();
            if (!e.isEnable()) {
                continue;
            }
            if (!e.isWebSocketEnable()) {
                continue;
            }
            if (!e.containsEngineVersion(engineVersion)) {
                continue;
            }
            if (e.isForTest()) {
                continue;
            }
            if (e.isForCompare()) {
                continue;
            }
            if (engineSelected == null) {
                engineSelected = e;
            } else {
                int currentThreadPercent = engineSelected.getAvailableThreadPercent();
                int threadPercent = e.getAvailableThreadPercent();
                // choose engine with available thread percent higher
                if (currentThreadPercent < threadPercent) {
                    engineSelected = e;
                }
                // same available thread percent
                else if (currentThreadPercent == threadPercent) {
                    int currentRequestInProgress = engineSelected.getNbRequestsInProgress();
                    int requestInProgress = e.getNbRequestsInProgress();
                    // select engine with lowest nb request in progress
                    if (currentRequestInProgress > requestInProgress) {
                        engineSelected = e;
                    }
                    // same nb request in progress
                    else if (currentRequestInProgress == requestInProgress) {
                        if (engineSelected.getNbCompute() > e.getNbCompute()) {
                            engineSelected = e;
                        }
                    }
                }
            }
        }
        return engineSelected;
    }

    /**
     * Search engine with small nb request in progress at first
     * @param engineVersion
     * @return
     */
    public UserVirtualEngine findUserEngineWS_Method3(int engineVersion) {
        // loop on each user and compare to find one with the high nb thread or the low queue size
        UserVirtualEngine engineSelected = null;
        Iterator<UserVirtualEngine> it = listEngineWS.iterator();
        while(it.hasNext()) {
            UserVirtualEngine e = it.next();
            if (!e.isEnable()) {
                continue;
            }
            if (!e.isWebSocketEnable()) {
                continue;
            }
            if (!e.containsEngineVersion(engineVersion)) {
                continue;
            }
            if (e.isForTest()) {
                continue;
            }
            if (e.isForCompare()) {
                continue;
            }
            if (engineSelected == null) {
                engineSelected = e;
            } else {
                // select engine with lowest nb request in progress
                int currentRequestInProgress = engineSelected.getNbRequestsInProgress();
                int requestInProgress = e.getNbRequestsInProgress();
                if (currentRequestInProgress > requestInProgress) {
                    engineSelected = e;
                }
                else if (currentRequestInProgress == requestInProgress) {
                    // select engine with available thread percent higher
                    int currentThreadPercent = engineSelected.getAvailableThreadPercent();
                    int threadPercent = e.getAvailableThreadPercent();
                    if (currentThreadPercent < threadPercent) {
                        engineSelected = e;
                    }
                    // same thread percent
                    else if (currentThreadPercent == threadPercent) {
                        if (engineSelected.getNbCompute() > e.getNbCompute()) {
                            engineSelected = e;
                        }
                    }
                }
            }
        }
        return engineSelected;
    }

    /**
     * Search engine with smaller performance indicator
     * @param engineVersion
     * @return
     */
    public UserVirtualEngine findUserEngineWS_Method4(int engineVersion) {
        // loop on each user and compare to find one with the high nb thread or the low queue size
        UserVirtualEngine engineSelected = null;
        Iterator<UserVirtualEngine> it = listEngineWS.iterator();
        while(it.hasNext()) {
            UserVirtualEngine e = it.next();
            if (!e.isEnable()) {
                continue;
            }
            if (!e.isWebSocketEnable()) {
                continue;
            }
            if (!e.containsEngineVersion(engineVersion)) {
                continue;
            }
            if (e.isForTest()) {
                continue;
            }
            if (e.isForCompare()) {
                continue;
            }
            if (engineSelected == null) {
                engineSelected = e;
            } else {
                // select engine with lowest index perf
                double currentPerf = engineSelected.computePerformanceIndex();
                double perf = e.computePerformanceIndex();
                if (currentPerf > perf) {
                    engineSelected = e;
                } else if (currentPerf == perf){
                    // select engine with lowest nb engine in progress
                    int currentRequestInProgress = engineSelected.getNbRequestsInProgress();
                    int requestInProgress = e.getNbRequestsInProgress();
                    if (currentRequestInProgress > requestInProgress) {
                        engineSelected = e;
                    }
                    else if (currentRequestInProgress == requestInProgress) {
                        // select engine with available thread percent higher
                        int currentThreadPercent = engineSelected.getAvailableThreadPercent();
                        int threadPercent = e.getAvailableThreadPercent();
                        if (currentThreadPercent < threadPercent) {
                            engineSelected = e;
                        }
                        else if (currentThreadPercent == threadPercent) {
                            // select engine with lowest nb compute
                            if (engineSelected.getNbCompute() > e.getNbCompute()) {
                                engineSelected = e;
                            }
                        }
                    }
                }
            }
        }
        return engineSelected;
    }

    /**
     * Search engine with random method !
     * @param engineVersion
     * @return
     */
    public UserVirtualEngine findUserEngineWS_MethodRandom(int engineVersion) {
        // loop on each user and compare to find one with the high nb thread or the low queue size
        int nbFailed = 0;
        while(true) {
            int idx = randomSelectEngine.nextInt(listEngineWS.size());
            if (idx >= 0 && idx < listEngineWS.size()) {
                UserVirtualEngine e = listEngineWS.get(idx);
                if (!e.isForTest() && !e.isForCompare() && e.isEnable() && e.containsEngineVersion(engineVersion)) {
                    return e;
                }
            }
            nbFailed++;
            if (nbFailed >= 5) {
                break;
            }
        }
        return null;
    }

    /**
     * Check if current hour is enable for restart engine WS
     * @return
     */
    public boolean isRestartEngineWSEnableForCurrentHour() {
        String hoursExcludeRestart = EngineConfiguration.getInstance().getStringValue("user.restartEngineWS.hoursExclude", "");
        if (hoursExcludeRestart.length() > 0) {
            List<String> listHoursToExclude = new ArrayList<>(Arrays.asList(hoursExcludeRestart.split(";")));
            Calendar currentCalendar = Calendar.getInstance();
            String strCurrentHour = ""+currentCalendar.get(Calendar.HOUR_OF_DAY);
            if (listHoursToExclude.contains(strCurrentHour)) {
                return false;
            }
        }
        return true;
    }

    /**
     * List engine WS to restart
     * @param nbMax
     * @return
     */
    public List<UserVirtualEngine> listUserEngineWSToRestart(int nbMax) {
        List<UserVirtualEngine> listUser = new ArrayList<>();
        boolean restartEnable = isRestartEngineWSEnableForCurrentHour();
        if (!restartEnable) {
            if (log.isDebugEnabled()) {
                log.debug("Current hour is in list of hours to exclude => no restart");
            }
        }
        if (restartEnable && nbMax > 0) {
            Iterator<UserVirtualEngine> it = listEngineWS.iterator();
            while (it.hasNext()) {
                UserVirtualEngine e = it.next();
                if (e.needToRestart()) {
                    listUser.add(e);
                }
                if (nbMax > 0 && listUser.size() >= nbMax) {
                    break;
                }
            }
        }
        return listUser;
    }

    /**
     * Run process to restart or update engine WS
     * @return
     */
    public void processRestartUpdateEngineWS() {
        Set<String> listEngineBusy = new HashSet<>();

        // restart engine
        int nbEngineSendCommandRestart = 0;
        List<UserVirtualEngine> listEngineToRestart = listUserEngineWSToRestart(EngineConfiguration.getInstance().getIntValue("user.restartEngineWS.nbMax", 5));
        if (listEngineToRestart != null) {
            for (UserVirtualEngine engine : listEngineToRestart) {
                if (engine.getWebSocket() != null) {
                    try {
                        if (EngineConfiguration.getInstance().getStringValue("user.restartEngineWS.operation", "restart").equals("reboot")) {
                            engine.getWebSocket().sendCommandReboot();
                        } else {
                            engine.getWebSocket().sendCommandRestart();
                        }
                        listEngineBusy.add(engine.getLoginID());
                        nbEngineSendCommandRestart++;
                    } catch (Exception e) {
                        log.error("Failed to send command restart to engine="+engine, e);
                    }
                }
            }
        }
        log.warn("Process restart engine WS result : nbEngineSendCommandRestart="+nbEngineSendCommandRestart+" - listEngineToRestart size="+(listEngineToRestart!=null?listEngineToRestart.size():null));

        // update engine FbMoteur
        int nbEngineUpdateFbMoteur = 0;
        if (EngineConfiguration.getInstance().getIntValue("user.updateEngineFbMoteur.processEnable", 1) == 1) {
            List<UserVirtualEngine> listEngineUpdateFbMoteur = listUserEngineWSToUpdateFbMoteur(EngineConfiguration.getInstance().getIntValue("user.updateEngineFbMoteur.nbMax", 5));
            if (listEngineUpdateFbMoteur != null) {
                for (UserVirtualEngine engine : listEngineUpdateFbMoteur) {
                    if (engine.getWebSocket() != null) {
                        if (!listEngineBusy.contains(engine.getLoginID())) {
                            try {
                                engine.getWebSocket().sendCommandUpdate();
                                listEngineBusy.add(engine.getLoginID());
                                nbEngineUpdateFbMoteur++;
                            } catch (Exception ex) {
                                log.error("Failed to send command updateFbMoteur for engine=" + engine, ex);
                            }
                        }
                    }
                }
            }
            log.warn("Process update engine FbMoteur : version="+getConfigEngineFbMoteurVersion()+" - nbEngineUpdateFbMoteur="+nbEngineUpdateFbMoteur+" - listEngineUpdateFbMoteur size="+(listEngineUpdateFbMoteur!=null?listEngineUpdateFbMoteur.size():null));
        }

        // update engine DLL
        int nbEngineUpdateDLL = 0;
        if (EngineConfiguration.getInstance().getIntValue("user.updateEngineDLL.processEnable", 1) == 1) {
            int currentVersion = getConfigEngineDLLVersion();
            List<UserVirtualEngine> listEngineUpdateDLL = listUserEngineWSToUpdateDLL(EngineConfiguration.getInstance().getIntValue("user.updateEngineDLL.nbMax", 5));
            for (UserVirtualEngine engine : listEngineUpdateDLL) {
                if (engine.getWebSocket() != null) {
                    if (!listEngineBusy.contains(engine.getLoginID())) {
                        try {
                            engine.getWebSocket().sendCommandUpdateDLL(currentVersion);
                            listEngineBusy.add(engine.getLoginID());
                            nbEngineUpdateDLL++;
                        } catch (Exception ex) {
                            log.error("Failed to send command updateDLL for engine=" + engine, ex);
                        }
                    }
                }
            }
            log.warn("Process update engineDLL : currentVersion=" + currentVersion + " - nbEngineUpdateDLL=" + nbEngineUpdateDLL + " - listEngineUpdateDLL size=" + (listEngineUpdateDLL != null ? listEngineUpdateDLL.size() : null));
        }

        listEngineBusy.clear();
    }

    /**
     * List engine WS with no result since a delay
     * @return
     */
    public List<UserVirtualEngine> listUserEngineWSWithNoResult() {
        List<UserVirtualEngine> listUser = new ArrayList<>();
        long queueIndex = ContextManager.getQueueMgr().getCurrentIndex();
        if ((lastQueueIndex + listEngineWS.size()) < queueIndex) {
            Iterator<UserVirtualEngine> it = listEngineWS.iterator();
            while (it.hasNext()) {
                UserVirtualEngine e = it.next();
                if (e.isNoResultSinceALongTime()) {
                    listUser.add(e);
                }
            }
            lastQueueIndex = queueIndex;
        } else {
            log.warn("No enough data computed ... LastQueueIndex="+lastQueueIndex+" - current queue index="+queueIndex);
        }
        return listUser;
    }

    /**
     * List engine with no result (since a delay)
     * Step1 => reset nb request in progress for this engine
     * Step2 => send command restart
     * Alert => send alert for engines with no result after these 2 steps
     */
    public void processEngineWithNoResult() {

        List<UserVirtualEngine> listUserToAlert = new ArrayList<>();
        List<String> listStep1Before = new ArrayList<>();
        listStep1Before.addAll(listEngineNoResultStep1);
        List<String> listStep2Before = new ArrayList<>();
        listStep2Before.addAll(listEngineNoResultStep2);
        List<String> listUserNoResultLogin = new ArrayList<>();

        // get list of user with no result
        List<UserVirtualEngine> listUserNoResult = listUserEngineWSWithNoResult();
        log.warn("List userNoResult size="+listUserNoResult.size());
        if (listUserNoResult != null && listUserNoResult.size() > 0) {
            if (EngineConfiguration.getInstance().getIntValue("user.useStepForProcessEngineWithNoResult", 1) == 1) {
                for (UserVirtualEngine e : listUserNoResult) {
                    listUserNoResultLogin.add(e.getLoginID());
                    // present in step1 reset already done => command restart !
                    if (listEngineNoResultStep1.contains(e.getLoginID())) {
                        listEngineNoResultStep1.remove(e.getLoginID());
                        listEngineNoResultStep2.add(e.getLoginID());

                        try {
                            if (e.getWebSocket() != null) {
                                e.getWebSocket().sendCommandRestart();
                                log.warn("Step2 - Send command restart for engine="+e.getLoginID());
                            } else {
                                log.error("Failed to send command restart - no websocket for engine="+e);
                            }
                        } catch (Exception ex) {
                            log.error("Failed to send command restart to engine=" + e, ex);
                        }
                    } else {
                        // no in step1, but present in step2 => send alert
                        if (listEngineNoResultStep2.contains(e.getLoginID())) {
                            listEngineNoResultStep2.remove(e.getLoginID());
                            listUserToAlert.add(e);
                            listEngineNoResultAlert.add(e.getLoginID());
                            log.warn("Send alert for engine="+e.getLoginID());
                        }
                        // no in step 1, not in step2 but already sent alert => continue to send alert !
                        else if (listEngineNoResultAlert.contains(e.getLoginID())) {
                            listUserToAlert.add(e);
                            log.warn("Continue to send alert for engine="+e.getLoginID());
                        }
                        // no in step1, not in step2 and not in alert => first time => send reset nb request in progress
                        else {
                            e.setNbRequestsInProgress(0);
                            log.warn("Step1 - Reset requests in progress for engine="+e.getLoginID());
                            listEngineNoResultStep1.add(e.getLoginID());
                        }
                    }
                }
                // send alert
                if (listUserToAlert.size() > 0) {
                    alertMgr.sendAlertEngineNoResult(listUserToAlert);
                } else {
                    log.warn("No alert to send (empty list)");
                }

                // remove element from alert list : no present in current list of engine with no result
                if (listEngineNoResultAlert.size() > 0) {
                    Iterator<String> itListAlert = listEngineNoResultAlert.iterator();
                    while (itListAlert.hasNext()) {
                        String temp = itListAlert.next();
                        if (!listUserNoResultLogin.contains(temp)) {
                            log.warn("Stop send alert for engine="+temp);
                            itListAlert.remove();
                        }
                    }
                }
            } else {
                alertMgr.sendAlertEngineNoResult(listUserNoResult);
            }
        } else {
            listEngineNoResultAlert.clear();
        }

        // remove element from previous execution
        for (String e : listStep1Before) {
            if (listEngineNoResultStep1.remove(e)) {
                if (!listEngineNoResultStep2.contains(e)) {
                    log.warn("Stop after step1 for engine=" + e);
                }
            }
        }
        for (String e : listStep2Before) {
            if (listEngineNoResultStep2.remove(e)) {
                if (!listEngineNoResultAlert.contains(e)) {
                    log.warn("Stop after step2 for engine=" + e);
                }
            }
        }
    }

    public String getConfigEngineFbMoteurVersion() {
        return EngineConfiguration.getInstance().getStringValue("user.engineFbMoteurVersion", null);
    }

    public List<UserVirtualEngine> listUserEngineWSToUpdateFbMoteur(int nbMax) {
        List<UserVirtualEngine> listUser = new ArrayList<>();
        String currentVersion = getConfigEngineFbMoteurVersion();
        if (currentVersion != null && currentVersion.length() > 0) {
            Iterator<UserVirtualEngine> it = listEngineWS.iterator();
            while (it.hasNext()) {
                UserVirtualEngine e = it.next();
                if (e.getVersion() != null && !e.getVersion().equals(currentVersion)) {
                    listUser.add(e);
                    if (nbMax > 0 && listUser.size() >= nbMax) {
                        break;
                    }
                }
            }
        }
        return listUser;
    }

    public void processUpdateEngineFbMoteur() {
        int nbMax = EngineConfiguration.getInstance().getIntValue("user.updateEngineFbMoteur.nbMax", 5);
        List<UserVirtualEngine> listUser = listUserEngineWSToUpdateFbMoteur(nbMax);
        if (listUser != null && listUser.size() > 0) {
            int nbProcessOK = 0;
            for (UserVirtualEngine e : listUser) {
                try {
                    e.getWebSocket().sendCommandUpdate();
                    nbProcessOK++;
                } catch (Exception ex) {
                    log.error("Failed to send command update for engine="+e, ex);
                }
            }
            log.warn("Process update engineFbMoteur nbProcessOK="+nbProcessOK+" - listUser.size()="+listUser.size());
        }
    }

    public int getConfigEngineDLLVersion() {
        return EngineConfiguration.getInstance().getIntValue("user.engineDLLVersion", 0);
    }

    public List<UserVirtualEngine> listUserEngineWSToUpdateDLL(int nbMax) {
        List<UserVirtualEngine> listUser = new ArrayList<>();
        int currentVersion = getConfigEngineDLLVersion();
        if (currentVersion > 0) {
            Iterator<UserVirtualEngine> it = listEngineWS.iterator();
            while (it.hasNext()) {
                UserVirtualEngine e = it.next();
                if (!e.containsEngineVersion(currentVersion)) {
                    listUser.add(e);
                    if (nbMax > 0 && listUser.size() >= nbMax) {
                        break;
                    }
                }
            }
        }
        return listUser;
    }

    public void processUpdateEngineDLL() {
        int nbMax = EngineConfiguration.getInstance().getIntValue("user.updateEngineDLL.nbMax", 5);
        int currentVersion = getConfigEngineDLLVersion();
        if (currentVersion > 0) {
            List<UserVirtualEngine> listUser = listUserEngineWSToUpdateDLL(nbMax);
            if (listUser != null && listUser.size() > 0) {
                int nbProcessOK = 0;
                for (UserVirtualEngine e : listUser) {
                    try {
                        e.getWebSocket().sendCommandUpdateDLL(currentVersion);
                        nbProcessOK++;
                    } catch (Exception ex) {
                        log.error("Failed to send command update for engine=" + e, ex);
                    }
                }
                log.warn("Process update engineFbMoteur nbProcessOK="+nbProcessOK+" - listUser.size()="+listUser.size());
            }
        }
    }

    public void sendDataForTestEngine(QueueMgr queueMgr, QueueData data, UserVirtualEngine engineOrigin) {
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
                        dataToSend.changeRequestEngineVersion(changeEngineVersion);
                    }
                }
                boolean dataInQueue = false;
                for (UserVirtualEngine engine : listEngineForTest) {
                    String strChangeEngineVersionUser = EngineConfiguration.getInstance().getStringValue("user.engineForTest.changeEngineVersion."+engine.getLogin(), null);
                    if (strChangeEngineVersionUser != null) {
                        int changeEngineVersion = extractEngineVersionForTest(strChangeEngineVersion);
                        if (changeEngineVersion > 0 && changeEngineVersion != data.getEngineVersion()) {
                            dataToSend = new QueueData();
                            dataToSend.copyFrom(data);
                            dataToSend.changeRequestEngineVersion(changeEngineVersion);
                        }
                    }
                    try {
                        if (engine.getWebSocket() != null && engine.containsEngineVersion(dataToSend.getEngineVersion())) {
                            if (!dataInQueue) {
                                queueMgr.getQueueTestMap().put(dataToSend.ID, dataToSend);
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

    public int extractEngineVersionForTest(String strEngine) {
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
     * Remove user with no activity since timeout expired
     * @return
     */
    public int processRemoseUserNoActivity() {
        List<UserVirtual> listUser = new ArrayList<UserVirtual>(mapUser.values());
        int nbUserRemove = 0;
        for (UserVirtual u : listUser) {
            if (u.getTimeout() > 0 && u.getDateLastActivity() > 0) {
                if ((u.getDateLastActivity() + (u.getTimeout()*1000)) < System.currentTimeMillis()) {
                    removeUser(u, true);
                    nbUserRemove++;
                }
            }
        }
        return nbUserRemove;
    }
}
