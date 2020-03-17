package com.gotogames.bridge.engineserver.ws.request;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.gotogames.bridge.engineserver.cache.TreeMgr;
import com.gotogames.bridge.engineserver.common.Constantes;
import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import com.gotogames.bridge.engineserver.common.LogStatMgr;
import com.gotogames.bridge.engineserver.request.QueueMgr;
import com.gotogames.bridge.engineserver.request.data.QueueData;
import com.gotogames.bridge.engineserver.session.EngineSessionMgr;
import com.gotogames.bridge.engineserver.user.*;
import com.gotogames.bridge.engineserver.ws.ServiceException;
import com.gotogames.bridge.engineserver.ws.ServiceExceptionType;
import com.gotogames.bridge.engineserver.ws.WSException;
import com.gotogames.bridge.engineserver.ws.WSResponse;
import com.gotogames.common.crypt.AESCrypto;
import com.gotogames.common.lock.LockMgr;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.session.Session;
import com.gotogames.common.tools.NumericalTools;
import com.gotogames.common.tools.StringTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component(value="requestServiceImpl")
@Scope(value="singleton")
public class RequestServiceImpl implements RequestService {
    private Logger log = LogManager.getLogger(this.getClass());
    private LockMgr lockMgr = new LockMgr();
    private LockWeakString lockGetResult = new LockWeakString();
//    private ArrayDeque<Long> stackTimeCache = new ArrayDeque<Long>();
//    private ArrayDeque<Long> stackTimeNoCache = new ArrayDeque<Long>();
//    private int stackMaxSize = 1000;
    private long nbResultCache = 0, nbResultNoCache = 0, nbResultGlobal = 0;
    private ConcurrentLinkedQueue<Long> meterTimeNoCache = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Long> meterTimeCache = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Long> meterTimeGlobal = new ConcurrentLinkedQueue<>();
    private Meter metricsRequestsAll = Constantes.getMetricRegistry().meter("requestAll");
    private Meter metricsRequestsBidInfo = Constantes.getMetricRegistry().meter("requestBidInfo");
    private Meter metricsRequestsBid = Constantes.getMetricRegistry().meter("requestBid");
    private Meter metricsRequestsCard = Constantes.getMetricRegistry().meter("requestCard");
    private Meter metricsRequestsPar = Constantes.getMetricRegistry().meter("requestPar");
    private Meter metricsRequestsClaim = Constantes.getMetricRegistry().meter("requestClaim");
    private Meter metricsRequestsNoCache = Constantes.getMetricRegistry().meter("requestNoCache");
    private Meter metricsRequestsEngine = Constantes.getMetricRegistry().meter("requestEngine");
    private Meter metricsRequestsEngineBid = Constantes.getMetricRegistry().meter("requestEngineBid");
    private Meter metricsRequestsEngineBidInfo = Constantes.getMetricRegistry().meter("requestEngineBidInfo");
    private Meter metricsRequestsEngineCard = Constantes.getMetricRegistry().meter("requestEngineCard");
    private Meter metricsRequestsEnginePar = Constantes.getMetricRegistry().meter("requestEnginePar");
    private Meter metricsRequestsEngineClaim = Constantes.getMetricRegistry().meter("requestEngineClaim");
    private Meter metricsRequestsEngineNoCache = Constantes.getMetricRegistry().meter("requestEngineNoCache");

    @Resource(name="sessionMgr")
    private EngineSessionMgr sessionMgr;

    @Resource(name="queueMgr")
    private QueueMgr queueMgr;

    @Resource(name="treeMgr")
    private TreeMgr treeMgr;

    @Resource(name="userVirtualMgr")
    private UserVirtualMgr userVirtualMgr;

    @Resource(name="logStatMgr")
    private LogStatMgr logStatMgr;


    @PostConstruct
    public void init() {
        if (sessionMgr == null || lockMgr == null || queueMgr == null || treeMgr == null) {
            log.error("Parameters null : sessionMgr | cacheMgr | lockMgr | queueMgr | treeMgr");
        }
//        queueMgr.setRequestServiceImpl(this);
    }

    @PreDestroy
    public void destroy() {
        log.info("Destroy lockMgr");
        lockMgr.destroy();
    }

    public boolean isMeterTimeEnable() {
        return EngineConfiguration.getInstance().getIntValue("request.meterTimeEnable", 1) ==1;
    }

    public int getMeterTimeMaxSize() {
        return EngineConfiguration.getInstance().getIntValue("request.meterTimeMaxSize", 1000);
    }

    public void addMeterTimeCache(long ts) {
        nbResultCache++;
        if (isMeterTimeEnable()) {
            meterTimeCache.add(ts);
            if (meterTimeCache.size() > 2 * getMeterTimeMaxSize()) {
                synchronized (meterTimeCache) {
                    while (meterTimeCache.size() > getMeterTimeMaxSize()) {
                        meterTimeCache.remove();
                    }
                }
            }
        }
        addMeterTimeGlobal(ts);
    }

    public void addMeterTimeNoCache(long ts) {
        nbResultNoCache++;
        if (isMeterTimeEnable()) {
            meterTimeNoCache.add(ts);
            if (meterTimeNoCache.size() > 2 * getMeterTimeMaxSize()) {
                synchronized (meterTimeNoCache) {
                    while (meterTimeNoCache.size() > getMeterTimeMaxSize()) {
                        meterTimeNoCache.remove();
                    }
                }
            }
        }
        addMeterTimeGlobal(ts);
    }

    public void addMeterTimeGlobal(long ts) {
        nbResultGlobal++;
        if (isMeterTimeEnable()) {
            meterTimeGlobal.add(ts);
            if (meterTimeGlobal.size() > 2 * getMeterTimeMaxSize()) {
                synchronized (meterTimeGlobal) {
                    while (meterTimeGlobal.size() > getMeterTimeMaxSize()) {
                        meterTimeGlobal.remove();
                    }
                }
            }
        }
    }

    public Queue<Long> getMeterTimeNoCache() {
        return meterTimeNoCache;
    }

    public Queue<Long> getMeterTimeCache() {
        return meterTimeCache;
    }

    public Queue<Long> getMeterTimeGlobal() {
        return meterTimeGlobal;
    }

    public long getNbResultCache() {
        return nbResultCache;
    }

    public long getNbResultNoCache() {
        return nbResultNoCache;
    }

    public long getNbResultGlobal() {
        return nbResultGlobal;
    }

    public double getMeterAverageTimeNoCache() {
        try {
            Long[] tabTime = meterTimeNoCache.toArray(new Long[meterTimeNoCache.size()]);
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
            log.error("Failed to compute average time no cache !", e);
        }
        return 0;
    }

    public double getMeterAverageTimeCache() {
        try {
            Long[] tabTime = meterTimeCache.toArray(new Long[meterTimeCache.size()]);
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
            log.error("Failed to compute average time cache !", e);
        }
        return 0;
    }

    public double getMeterAverageTimeGlobal() {
        try {
            Long[] tabTime = meterTimeGlobal.toArray(new Long[meterTimeGlobal.size()]);
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
            log.error("Failed to compute average time global !", e);
        }
        return 0;
    }

    @Override
    public WSResponse clearCache(String sessionID) {
        WSResponse response = new WSResponse();
        try {
            Session session = sessionMgr.getAndCheckSession(sessionID);
            if (log.isDebugEnabled()) {
                log.debug("login=" + session.getLogin());
            }
            treeMgr.clearAll();
            ClearCacheResponse resp = new ClearCacheResponse();
            resp.result = true;
            response.setData(resp);
        } catch (ServiceException e) {
            response.setException(new WSException(e.getType()));
        } catch (Exception e) {
            log.error("Exception !!! ", e);
            response.setException(new WSException(ServiceExceptionType.COMMON_INTERNAL_SERVER_ERROR));
        }
        return response;
    }

    public Object getLockOnRequest(String request) {
        return lockGetResult.getLock(request);
    }

    @Override
    public WSResponse getResult(String sessionID, GetResultParam param) {
        WSResponse response = new WSResponse();
        if (param != null && param.isValid()) {
            try {
                GetResultResponse resp = new GetResultResponse();
                Session session = sessionMgr.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("login=" + session.getLogin() + " - param=" + param);
                }

                UserVirtualFBServer u = (UserVirtualFBServer)userVirtualMgr.getUserVirtual(session.getLogin());
                if (u == null) {
                    log.error("login unknown : login=" + session.getLogin());
                    throw new ServiceException(ServiceExceptionType.SESSION_UNKNOWN_LOGIN);
                }
                u.incrementNbRequest();
                resp.result = processGetResult(param, u);
                response.setData(resp);
            } catch (ServiceException e) {
                response.setException(new WSException(e.getType()));
            } catch (Exception e) {
                log.error("Exception !!! param="+param, e);
                response.setException(new WSException(ServiceExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        }
        else {
            log.error("Parameter not valid - param="+param);
            response.setException(new WSException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @Override
    public WSResponse getNextBid(GetNextBidParam param) {
        WSResponse response = new WSResponse();
        if (param != null && param.isValid()) {
            try {
                GetNextBidResponse resp = new GetNextBidResponse();
                UserVirtualFBServer u = (UserVirtualFBServer)userVirtualMgr.getUserVirtualByLogin(param.user);
                if (log.isDebugEnabled()) {
                    log.debug("user=" + u.getLogin() + " - param=" + param);
                }
                if (u == null) {
                    log.error("User unknown : user=" + param.user);
                    throw new ServiceException(ServiceExceptionType.SESSION_UNKNOWN_LOGIN);
                }
                u.incrementNbRequest();
                GetResultParam getResultParam = new GetResultParam();
                getResultParam.deal = param.deal;
                getResultParam.game = param.game;
                getResultParam.requestType = Constantes.REQUEST_TYPE_BID;
                getResultParam.conventions = UserVirtualMgr.getConfigStringValue(u.getLogin(), u.getLoginPrefix(), "conventions", null);
                getResultParam.options = Constantes.buildOptionsForEngine(param.resultType, userVirtualMgr.getConfigEngineDLLVersion(), 0, 0, 0);
                getResultParam.useCache = u.isUseCache();
                resp.result = processGetResult(getResultParam, u);
                if (resp.result != null && resp.result.length() >=2) {
                    resp.result = resp.result.substring(0, 2);
                }
                response.setData(resp);
            } catch (ServiceException e) {
                response.setException(new WSException(e.getType()));
            } catch (Exception e) {
                log.error("Exception !!! param="+param, e);
                response.setException(new WSException(ServiceExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        }
        else {
            log.error("Parameter not valid - param="+param);
            response.setException(new WSException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @Override
    public WSResponse getNextCard(GetNextCardParam param) {
        WSResponse response = new WSResponse();
        if (param != null && param.isValid()) {
            try {
                GetNextBidResponse resp = new GetNextBidResponse();
                UserVirtualFBServer u = (UserVirtualFBServer)userVirtualMgr.getUserVirtualByLogin(param.user);
                if (log.isDebugEnabled()) {
                    log.debug("user=" + u.getLogin() + " - param=" + param);
                }
                if (u == null) {
                    log.error("User unknown : user=" + param.user);
                    throw new ServiceException(ServiceExceptionType.SESSION_UNKNOWN_LOGIN);
                }
                u.incrementNbRequest();
                GetResultParam getResultParam = new GetResultParam();
                getResultParam.deal = param.deal;
                getResultParam.game = param.game;
                getResultParam.requestType = Constantes.REQUEST_TYPE_CARD;
                getResultParam.conventions = UserVirtualMgr.getConfigStringValue(u.getLogin(), u.getLoginPrefix(), "conventions", null);
                getResultParam.options = Constantes.buildOptionsForEngine(param.resultType, userVirtualMgr.getConfigEngineDLLVersion(), 0, 0, 0);
                getResultParam.useCache = u.isUseCache();
                resp.result = processGetResult(getResultParam, u);
                if (resp.result != null && resp.result.length() >=2) {
                    resp.result = resp.result.substring(0, 2);
                }
                response.setData(resp);
            } catch (ServiceException e) {
                response.setException(new WSException(e.getType()));
            } catch (Exception e) {
                log.error("Exception !!! param="+param, e);
                response.setException(new WSException(ServiceExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        }
        else {
            log.error("Parameter not valid - param="+param);
            response.setException(new WSException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public String processGetResult(GetResultParam param, UserVirtualFBServer user) throws ServiceException {
        String result = "";
        if (param != null && param.isValid()) {
            try {
                long ts1 = System.currentTimeMillis();
                boolean resultCache = false;

                long ts = System.currentTimeMillis();
                logStatMgr.logInfo(param.logStat, System.currentTimeMillis() - ts, "getAndCheckSession - user="+user.getLogin()+" - param="+param);

                int dealSize = EngineConfiguration.getInstance().getIntValue("request.field.deal.size", 54);
                int optSize = EngineConfiguration.getInstance().getIntValue("request.field.opt.size", 10);
                int convSize = EngineConfiguration.getInstance().getIntValue("request.field.conv.size", 50);
                int gameSize = EngineConfiguration.getInstance().getIntValue("request.field.game.size", -1);

                if (dealSize != -1 && param.deal.length() != dealSize) {
                    log.error("Request parameter deal not valid : " + param.deal);
                    throw new ServiceException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID, "PARAMETER DEAL NOT VALID");
                }
                if (optSize != -1 && param.options.length() != optSize) {
                    log.error("Request parameter options not valid : " + param.options);
                    throw new ServiceException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID, "PARAMETER OPTIONS NOT VALID");
                }
                if (convSize != -1 && param.conventions.length() > convSize) {
                    log.error("Request parameter conventions not valid : " + param.conventions);
                    throw new ServiceException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID, "PARAMETER CONVENTIONS NOT VALID");
                }
                if (gameSize != -1 && param.game.length() != gameSize) {
                    log.error("Request parameter game not valid : " + param.game);
                    throw new ServiceException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID, "PARAMETER GAME NOT VALID");
                }
                if (param.game.length() %2 != 0) {
                    log.error("Request parameter game not valid (multiple by 2) : " + param.game);
                    throw new ServiceException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID, "PARAMETER GAME NOT VALID");
                }

                if ((EngineConfiguration.getInstance().getIntValue("general.check.userEngineConnected", 0) == 1) && !sessionMgr.isUserEngineConnected()) {
                    log.error("No engine user connected !");
                    throw new ServiceException(ServiceExceptionType.SESSION_NO_ENGINE_CONNECTED);
                }
                // metrics
                ts = System.currentTimeMillis();
                if (EngineConfiguration.getInstance().getIntValue("general.metrics.enable", 0) == 1) {
                    metricsRequestsAll.mark();
                    if (param.requestType == Constantes.REQUEST_TYPE_BID) {
                        metricsRequestsBid.mark();
                    }
                    else if (param.requestType == Constantes.REQUEST_TYPE_BID_INFO) {
                        metricsRequestsBidInfo.mark();
                    }
                    else if (param.requestType == Constantes.REQUEST_TYPE_CARD) {
                        metricsRequestsCard.mark();
                    }
                    else if (param.requestType == Constantes.REQUEST_TYPE_PAR) {
                        metricsRequestsPar.mark();
                    }
                    else if (param.requestType == Constantes.REQUEST_TYPE_CLAIM) {
                        metricsRequestsClaim.mark();
                    }
                    if (!param.useCache) {
                        metricsRequestsNoCache.mark();
                    }
                }

                String requestKey = param.getKey();
                user.setDateLastActivity(System.currentTimeMillis());
                try {
                    // retrive existing result in cache
                    String cacheVal = null;

                    boolean useCacheForRequest = true;
                    if (param.requestType == Constantes.REQUEST_TYPE_PAR || param.requestType == Constantes.REQUEST_TYPE_CLAIM) {
                        useCacheForRequest = false;
                    } else {
                        useCacheForRequest = param.useCache;
                    }

                    if (user.isUseCache() && useCacheForRequest) {
                        ts = System.currentTimeMillis();
                        cacheVal = treeMgr.getCacheData(requestKey);
                        logStatMgr.logInfo(param.logStat, System.currentTimeMillis() - ts, "getCacheData - user="+user.getLogin()+" - param="+param+" - requestKey="+requestKey+" - cacheVal="+cacheVal);
                    }

                    // no result found in cache => return empty result and add request to the queue
                    if (cacheVal == null || cacheVal.length() == 0) {
                        // metrics
                        if (EngineConfiguration.getInstance().getIntValue("general.metrics.enable", 0) == 1) {
                            metricsRequestsEngine.mark();
                            if (param.requestType == Constantes.REQUEST_TYPE_BID) {
                                metricsRequestsEngineBid.mark();
                            }
                            else if (param.requestType == Constantes.REQUEST_TYPE_BID_INFO) {
                                metricsRequestsEngineBidInfo.mark();
                            }
                            else if (param.requestType == Constantes.REQUEST_TYPE_CARD) {
                                metricsRequestsEngineCard.mark();
                            }
                            else if (param.requestType == Constantes.REQUEST_TYPE_PAR) {
                                metricsRequestsEnginePar.mark();
                            }
                            else if (param.requestType == Constantes.REQUEST_TYPE_CLAIM) {
                                metricsRequestsEngineClaim.mark();
                            }
                            if (!param.useCache) {
                                metricsRequestsEngineNoCache.mark();
                            }
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("No result found in cache => add data in queue");
                        }
                        int timeoutWait = EngineConfiguration.getInstance().getIntValue("request.waitingResult.timeout", 300);
                        boolean saveInCache = true;
                        if (user.isUseCache()) {
                            saveInCache = useCacheForRequest;
                        } else {
                            saveInCache = false;
                        }
                        ts = System.currentTimeMillis();
                        if (param.isAsync()) {
                            QueueData queueData = queueMgr.createAndAddData(requestKey, saveInCache, param.asyncID, user.getUrlFBSetResult(), user, param.logStat);
                            logStatMgr.logInfo(param.logStat, System.currentTimeMillis() - ts, "queue.addData - user="+user.getLogin()+" - param="+param+" - requestKey="+requestKey+" - saveInCache="+saveInCache+" - queueData="+queueData);
                            result = "ASYNC";
                        } else {
                            // create data but don't put yet in the queue
                            QueueData queueData = queueMgr.createData(requestKey, saveInCache, param.asyncID, user.getUrlFBSetResult(), user, param.logStat);
                            synchronized (queueData) {
                                try {
                                    // after synchronize, put it to the queue
                                    queueMgr.addDataToQueue(queueData);
                                    if (log.isDebugEnabled()) {
                                        log.debug("Wait for result ... queueDataID=" + queueData.ID);
                                    }
                                    // now wait for notify event
                                    queueData.wait(timeoutWait * 1000);
                                    if (log.isDebugEnabled()) {
                                        log.debug("Set result value for queueData=" + queueData);
                                    }
                                    // get result from queue data
                                    result = queueData.resultValue;
                                } catch (InterruptedException e) {
                                    log.error("Waiting request result InterruptedException", e);
                                }
                            }
                            logStatMgr.logInfo(param.logStat, System.currentTimeMillis() - ts, "Queue wait value - user=" + user.getLogin() + " - param=" + param + " - requestKey=" + requestKey + " - result=" + result + " - saveInCache=" + saveInCache);
                        }
                    }
                    // result found in cache => set value to result
                    else {
                        if (log.isDebugEnabled()) {
                            log.debug("Result found in cache => set result=" + cacheVal);
                        }
                        resultCache = true;
                        result = cacheVal;
                    }
                } catch (Exception e) {
                    log.error("Exception to get cache data for request=" + requestKey + " - exception=" + e.getMessage(), e);
                }
                long ts2 = System.currentTimeMillis();
                if (resultCache) {
                    addMeterTimeCache(ts2-ts1);
                } else {
                    if (!param.isAsync()) {
                        addMeterTimeNoCache(ts2-ts1);
                    }
                }
                return result;
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("Exception !!! param="+param, e);
                throw new ServiceException(ServiceExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        else {
            log.error("Parameter not valid - param="+param);
            throw new ServiceException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
    }

    @Override
    public WSResponse getResultWithCrypt(String sessionID, GetResultParam param) {
        WSResponse response = new WSResponse();
        if (param != null && param.isValid()) {
            try {
                String dealCrypt = param.deal;
                log.debug("Deal crypt="+dealCrypt);
                byte[] byDealCrypt = StringTools.string2byte(dealCrypt);
                byte[] byDealDecrypt = AESCrypto.decrypt(byDealCrypt, "FunBr1dg3ceStFun");
                //String dealDeCrypt = StringTools.byte2String(byDealDecrypt);
                String dealDeCrypt = new String(byDealDecrypt);
                if (log.isDebugEnabled()) {
                    log.debug("Deal decrypt="+dealDeCrypt);
                }
                param.deal = dealDeCrypt;
                return getResult(sessionID, param);
            } catch (Exception e) {
                log.error("Exception on decrypt deal - deal="+param, e);
                response.setException(new WSException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID));
            }
        }
        else {
            log.error("Parameter not valid");
            response.setException(new WSException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public Meter getMeterRequestAll() {
        return metricsRequestsAll;
    }

    public Meter getMeterRequestBid() {
        return metricsRequestsBid;
    }

    public Meter getMeterRequestBidInfo() {
        return metricsRequestsBidInfo;
    }

    public Meter getMeterRequestCard() {
        return metricsRequestsCard;
    }

    public Meter getMeterRequestPar() {
        return metricsRequestsPar;
    }
    public Meter getMeterRequestClaim() {
        return metricsRequestsClaim;
    }

    public Meter getMeterRequestNoCache() {
        return metricsRequestsNoCache;
    }

    public Meter getMeterRequestEngine() {
        return metricsRequestsEngine;
    }

    public Meter getMeterRequestEngineBid() {
        return metricsRequestsEngineBid;
    }
    public Meter getMeterRequestEngineBidInfo() {
        return metricsRequestsEngineBidInfo;
    }
    public Meter getMeterRequestEngineCard() {
        return metricsRequestsEngineCard;
    }
    public Meter getMeterRequestEnginePar() {
        return metricsRequestsEnginePar;
    }
    public Meter getMeterRequestEngineClaim() {
        return metricsRequestsEngineClaim;
    }
    public Meter getMeterRequestEngineNoCache() {
        return metricsRequestsEngineNoCache;
    }

    public void setResultBidInfoForBid(GetResultParam param, String result) {
        long ts = System.currentTimeMillis();
        if (param != null && param.isValid() && result != null && result.length() > 0) {
            String requestKey = param.getKey();
            synchronized (getLockOnRequest(requestKey)) {
                logStatMgr.logInfo(false, System.currentTimeMillis() - ts, "setResultBidInfoForBid time to get lock on requestKey="+requestKey+" - param="+param+" - result="+result);
                boolean bAddCacheData = treeMgr.addCacheData(param.getKey(), result, false, false);
                if (log.isDebugEnabled()) {
                    log.debug("AddCacheData return : " + bAddCacheData+" - key="+param.getKey()+" - result="+result);
                }
            }
        } else {
            log.error("Parameter not valid : param="+param+" - result="+result);
        }
        logStatMgr.logInfo(false, System.currentTimeMillis() - ts, "setResultBidInfoForBid - param="+param+" - result="+result);
    }


}
