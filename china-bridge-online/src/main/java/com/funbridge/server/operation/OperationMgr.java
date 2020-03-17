package com.funbridge.server.operation;

import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.operation.connection.OperationConnection;
import com.funbridge.server.operation.connection.OperationConnectionNotif;
import com.funbridge.server.operation.connection.OperationConnectionNotifPlayerList;
import com.funbridge.server.presence.FBSession;
import com.mongodb.client.result.DeleteResult;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by pserent on 13/03/2014.
 */
@Component(value="operationMgr")
@Scope(value="singleton")
public class OperationMgr extends FunbridgeMgr{
    private List<OperationConnection> listMemoryOperationConnection = new ArrayList<>();
    private final ReentrantReadWriteLock rwLockListOperation = new ReentrantReadWriteLock();
    private final Lock lockReadListOperation = rwLockListOperation.readLock();
    private final Lock lockWriteListOperation = rwLockListOperation.writeLock();
    private ScheduledExecutorService schedulerPurgeOperationConnection = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> schedulerPurgeConnectionFuture = null;
    private PurgeOperationConnectionTask purgeOperationConnectionTask = new PurgeOperationConnectionTask();
    @Resource(name="mongoTemplate")
    private MongoTemplate mongoTemplate;
    @Resource(name="messageNotifMgr")
    private MessageNotifMgr notifMgr;

    /**
     * Task to transform buffer to notif
     * @author pserent
     *
     */
    public class PurgeOperationConnectionTask implements Runnable {
        boolean isRunning = false;
        @Override
        public void run() {
            if (FBConfiguration.getInstance().getIntValue("operation.onConnection.purgeTaskEnable", 1) == 1) {
                if (!isRunning) {
                    isRunning = true;
                    try {
                        purgeExpiredOperationConnection();
                        // wait 2s before reload operation in memory
                        Thread.sleep(2000);
                        loadListMemoryOperationConnection();
                    } catch (Exception e) {
                        ContextManager.getAlertMgr().addAlert("OPERATION_MGR", FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to purge expired operation connection", e.getMessage(), null);
                        log.error("Exception to purge expired operation connection", e);
                    }
                    isRunning = false;
                }
            } else {
                ContextManager.getAlertMgr().addAlert("OPERATION_MGR", FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for expired operation connection", null, null);
                if (log.isDebugEnabled()) {
                    log.debug("Purge task is not enable !");
                }
            }
        }

    }

    @PostConstruct
    @Override
    public void init() {

    }

    @PreDestroy
    @Override
    public void destroy() {
        stopScheduler(schedulerPurgeOperationConnection);
        listMemoryOperationConnection.clear();
    }

    @Override
    public void startUp() {
        loadListMemoryOperationConnection();

        // start notifBufferTransform timer task
        try {
            int periodTask = FBConfiguration.getInstance().getIntValue("operation.onConnection.purgeTaskPeriod", 60);
            schedulerPurgeConnectionFuture = schedulerPurgeOperationConnection.scheduleWithFixedDelay(purgeOperationConnectionTask, 30, periodTask, TimeUnit.MINUTES);
            log.warn("Schedule purgeOperationConnection - next run at " + Constantes.getStringDateForNextDelayScheduler(schedulerPurgeConnectionFuture) + " - period (minute)=" + periodTask);
        } catch (Exception e) {
            log.error("Exception to start notifBufferTransform task", e);
        }
    }

    public String getDateNextTaskSchedulerPurgeConnection() {
        return Constantes.getStringDateForNextDelayScheduler(schedulerPurgeConnectionFuture);
    }

    /**
     * Check class with this classname exist in the class loader of OperationMgr
     * @param classname full name of the class (my.package.MyClass)
     * @return
     */
    public boolean isBeanOperationLoadInClasspath(String classname) {
        try {
            Class.forName(classname, false, OperationMgr.class.getClassLoader());
            // no exception => class found
            return true;
        } catch (ClassNotFoundException e) {
            // not existing
            log.warn("Class not existing with name="+classname);
        }
        return false;
    }

    /**
     * add classpath to current classloader (invoke addURL)
     * @param path
     */
    private void addClasspath(String path) {
        try {
        URLClassLoader sysloader = (URLClassLoader)OperationMgr.class.getClassLoader();
        boolean alreadyPresent = false;

        if (!alreadyPresent) {
            File fpath = new File(path);
            try {
                Method methodAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                methodAddUrl.setAccessible(true);
                methodAddUrl.invoke(sysloader, fpath.toURI().toURL());
                log.warn("Success to addURL for path="+path);
            } catch (IllegalAccessException e) {
                log.error("IllegalAccessException - failed to invoke addURL for path="+path, e);
            } catch (InvocationTargetException e) {
                log.error("InvocationTargetException - failed to invoke addURL for path="+path, e);
            } catch (MalformedURLException e) {
                log.error("MalformedURLException - failed to invoke addURL for path="+path, e);
            } catch (NoSuchMethodException e) {
                log.error("NoSuchMethodException - failed to invoke addURL for path=" + path, e);
            }
        }
        } catch (Exception e) {
            log.error("Failed to addClasspath path=" + path, e);
        }
    }

    /**
     * Load operation - clear the current list and reload all from config
     */
    public boolean loadListMemoryOperationConnection() {
        log.debug("Load list operationConnection - Begin");

        lockWriteListOperation.lock();
        try {

            List<OperationConnection> listTemp = listOperationConnection(true, 0, 0);
            if (listTemp != null) {
                listMemoryOperationConnection.clear();
                listMemoryOperationConnection.addAll(listTemp);
            }

            // sort using execution order
            Collections.sort(listMemoryOperationConnection, new Comparator<OperationConnection>() {
                @Override
                public int compare(OperationConnection o1, OperationConnection o2) {
                    return Integer.compare(o1.executionOrder, o2.executionOrder);
                }
            });
            return true;
        } catch (Exception e) {
            log.error("Failed to load list operation connection", e);
        } finally {
            lockWriteListOperation.unlock();
        }
        return false;
    }

    /**
     * Create a new instance of this class and cast into Operation
     * @param classname
     * @return
     */
    public Operation getBeanOperationInstance(String classname) {
        try {
            return (Operation)Class.forName(classname, false, OperationMgr.class.getClassLoader()).newInstance();
        } catch (InstantiationException e) {
            log.error("InstantiationException - failed to create new instance of bean="+classname, e);
        } catch (IllegalAccessException e) {
            log.error("IllegalAccessException - failed to create new instance of bean="+classname, e);
        } catch (ClassNotFoundException e) {
            log.error("ClassNotFoundException - failed to create new instance of bean="+classname, e);
        }
        return null;
    }

    /**
     * Process operation on connection for this session
     * @param session
     */
    public void processOperationOnConnection(FBSession session) {
        if (isEnableOnConnection()) {
            if (session != null) {
                lockReadListOperation.lock();
                try {
                    for (OperationConnection op : listMemoryOperationConnection) {
                        if (op.enable) {
                            try {
                                long ts1 = System.currentTimeMillis();
                                boolean opResult = op.process(session);
                                if (System.currentTimeMillis() - ts1 > FBConfiguration.getInstance().getIntValue("operation.processLogLimit", 1000)) {
                                    log.error("Too long to process OperationConnection op="+op+" for player="+session.getPlayer()+" - ts="+(System.currentTimeMillis() - ts1));
                                }

                                if (log.isDebugEnabled()) {
                                    log.debug("Process op="+op+" for player="+session.getPlayer()+" - result="+opResult);
                                }
                            } catch (Exception e) {
                                log.error("Exception to process operation="+op+" - session="+session, e);
                            }
                        } else {
                            log.debug("Operation not enable or not on connection - op="+op);
                        }
                    }
                } finally {
                    lockReadListOperation.unlock();
                }
            } else {
                log.error("Param session is null");
            }
        } else {
            log.debug("Operation on connection not enable in config");
        }
    }

    /**
     * Check if operation on connection is enable
     * @return
     */
    public boolean isEnableOnConnection() {
        return FBConfiguration.getInstance().getIntValue("operation.onConnection.enable", 0) == 1;
    }


    /**
     * Return list of operation on connection
     * @return
     */
    public List<OperationConnection> getListMemoryOperationConnection() {
        return listMemoryOperationConnection;
    }

    /**
     * List operation connection
     * @param onlyEnable
     * @param offset
     * @param nbMax if 0 => return all
     * @return
     */
    public List<OperationConnection> listOperationConnection(boolean onlyEnable, int offset, int nbMax) {
        Query q = new Query();
        if (onlyEnable) {
            q.addCriteria(Criteria.where("enable").is(true));
        }
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        return mongoTemplate.find(q, OperationConnection.class);
    }

    /**
     * Purge expired operation connection (use checkDateExpiration)
     * @return
     */
    public int purgeExpiredOperationConnection() {
        int nbOpRemove = 0;
        List<OperationConnection> listAll = listOperationConnection(false, 0, 0);
        if (listAll != null && listAll.size() > 0) {
            for (OperationConnection e : listAll) {
                if (!e.checkDateExpiration()) {
                    DeleteResult wr = mongoTemplate.remove(e);
                    if (wr != null) {
                        if (log.isInfoEnabled()) {
                            log.info("Remove existing operationConnection=" + e);
                        }
                        nbOpRemove++;
                    }
                }
            }
        }
        log.warn("Nb expired operationConnection removed="+nbOpRemove);
        return nbOpRemove;
    }

    public OperationConnection getOperationConnection(String opID) {
        return mongoTemplate.findById(new ObjectId(opID), OperationConnection.class);
    }

    public void saveOperationConnection(OperationConnection oc) {
        if (oc != null) {
            if (oc.ID == null) {
                mongoTemplate.insert(oc);
                if (log.isInfoEnabled()) {
                    log.info("insert new operationConnection="+oc);
                }
            } else {
                mongoTemplate.save(oc);
                if (log.isInfoEnabled()) {
                    log.info("Update existing operationConnection="+oc);
                }
            }
        }
    }

    public boolean removeOperationConnection(OperationConnection oc) {
        if (oc != null && oc.ID != null) {
            try {
                DeleteResult wr = mongoTemplate.remove(oc);
                if (wr != null) {
                    if (log.isInfoEnabled()) {
                        log.info("Remove existing operationConnection=" + oc);
                    }
                    return true;
                }
            } catch (Exception e) {
                log.error("Failed to remove operatinoConnection="+oc, e);
            }
        }
        log.error("Failed to remove existing operationConnection="+oc);
        return false;
    }

    public OperationConnectionNotif createOperationConnectionNotif(String name, boolean enable,
                                                                   long dateExpiration, long dateStart, long dateLastConnectionBefore,
                                                                   String notifGroupID, String playerLang, String deviceType, String playerCountry,
                                                                   String playerSegmentation) {
        if (name != null && name.length() > 0) {
            if (dateExpiration > 0 && dateExpiration > System.currentTimeMillis()) {
                OperationConnectionNotif op = new OperationConnectionNotif(name);
                op.enable = enable;
                op.dateExpiration = dateExpiration;
                op.dateStart = dateStart;
                op.dateLastConnectionBefore = dateLastConnectionBefore;
                op.notifGroupID = notifGroupID;
                op.playerLang = playerLang;
                op.deviceType = deviceType;
                op.playerCountry = playerCountry;
                op.playerSegmentation = playerSegmentation;
                mongoTemplate.save(op);
                return op;
            } else {
                log.error("dateExpiration is not valid - dateExpiration="+dateExpiration+"("+ Constantes.timestamp2StringDateHour(dateExpiration)+") name="+name);
            }
        } else {
            log.error("name is null");
        }
        return null;
    }

    public OperationConnectionNotifPlayerList createOperationConnectionNotifPlayerList(String name, boolean enable, List<Long> listPlayerID,
                                                                   long dateExpiration, long dateStart, long dateLastConnectionBefore,
                                                                   String notifGroupID) {
        if (name != null && name.length() > 0) {
            if (listPlayerID != null && listPlayerID.size() > 0) {
                if (dateExpiration > 0 && dateExpiration > System.currentTimeMillis()) {
                    OperationConnectionNotifPlayerList op = new OperationConnectionNotifPlayerList(name);
                    op.enable = enable;
                    op.dateExpiration = dateExpiration;
                    op.dateStart = dateStart;
                    op.dateLastConnectionBefore = dateLastConnectionBefore;
                    op.notifGroupID = notifGroupID;
                    op.listPlayerID.addAll(listPlayerID);
                    mongoTemplate.save(op);
                    return op;
                } else {
                    log.error("dateExpiration is not valid - dateExpiration=" + dateExpiration + "(" + Constantes.timestamp2StringDateHour(dateExpiration) + ") name=" + name);
                }
            } else {
                log.error("listPlayerID null or empty - name="+name);
            }
        } else {
            log.error("name is null");
        }
        return null;
    }
}
