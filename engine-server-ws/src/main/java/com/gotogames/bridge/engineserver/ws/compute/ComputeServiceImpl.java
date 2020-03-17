package com.gotogames.bridge.engineserver.ws.compute;

import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import com.gotogames.bridge.engineserver.common.LogStatMgr;
import com.gotogames.bridge.engineserver.request.QueueMgr;
import com.gotogames.bridge.engineserver.session.EngineSessionMgr;
import com.gotogames.bridge.engineserver.test.QueryTest;
import com.gotogames.bridge.engineserver.user.UserVirtual;
import com.gotogames.bridge.engineserver.user.UserVirtualEngine;
import com.gotogames.bridge.engineserver.user.UserVirtualMgr;
import com.gotogames.bridge.engineserver.ws.ServiceException;
import com.gotogames.bridge.engineserver.ws.ServiceExceptionType;
import com.gotogames.bridge.engineserver.ws.WSException;
import com.gotogames.bridge.engineserver.ws.WSResponse;
import com.gotogames.common.session.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component(value="computeServiceImpl")
@Scope(value="singleton")
public class ComputeServiceImpl implements ComputeService {
	private long nbResultFbMoteur = 0, nbResultOthers = 0;
	private Logger log = LogManager.getLogger(this.getClass());
	
	@Resource(name="sessionMgr")
	private EngineSessionMgr sessionMgr;
	
	@Resource(name="userVirtualMgr")
	private UserVirtualMgr userVirtualMgr;
	
	@Resource(name="queueMgr")
	private QueueMgr queueMgr;

    @Resource(name="logStatMgr")
    private LogStatMgr logStatMgr;

    @PostConstruct
	public void init() {
	}
	
	@PreDestroy
	public void destroy() {
	}
	
	public long getNbResultFbMoteur() {
		return nbResultFbMoteur;
	}
	
	public long getNbResultFbOthers() {
		return nbResultOthers;
	}
	
	@Override
	public WSResponse getQuery(String sessionID) {
        WSResponse response = new WSResponse();
		try {
            Session session = sessionMgr.getAndCheckSession(sessionID);
            UserVirtualEngine u = (UserVirtualEngine)userVirtualMgr.getUserVirtual(session.getLogin());
            boolean onlyFbMoteur = EngineConfiguration.getInstance().getIntValue("request.onlyFbMoteur", 0) == 1;
            if (u == null) {
                log.error("login unknown : login=" + session.getLogin());
                throw new ServiceException(ServiceExceptionType.SESSION_UNKNOWN_LOGIN);
            }
            u.setDateLastActivity(System.currentTimeMillis());
            WSComputeQuery query = null;
            // user for test ?
            if (u.isTest()) {
                query = QueryTest.createQuery();
            }
            // option only fbmoteur &  user not fb moteur => return null
            else if (onlyFbMoteur && !u.isFbMoteur()) {
                return null;
            }
            // search query for user ...
            else {
                query = queueMgr.getQueryForUser(u);
            }
            if (query == null) {
                query = new WSComputeQuery();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Query found for user=" + u + " - query=" + query);
                }
            }
            query.setPollingValue(queueMgr.getPollingValue(u.getLogin()));
            if (log.isDebugEnabled() && query.getComputeID() > 0) {
                log.debug(query.toString());
            }
            GetQueryResponse resp = new GetQueryResponse();
            resp.computeID = query.getComputeID();
            resp.conventions = query.getConventions();
            resp.deal = query.getDeal();
            resp.game = query.getGame();
            resp.options = query.getOptions();
            resp.pollingValue = query.getPollingValue();
            resp.queryType = query.getQueryType();
            resp.nbTricksForClaim = query.getNbTricksForClaim();
            resp.claimPlayer = query.getClaimPlayer();
            response.setData(resp);
        } catch (ServiceException e) {
            response.setException(new WSException(e.getType()));
		} catch (Exception e) {
			log.error("Exception on getQuery : "+e.getMessage(), e);
            response.setException(new WSException(ServiceExceptionType.COMMON_INTERNAL_SERVER_ERROR));
		}
		return response;
	}

    @Override
    public WSResponse getQueries(String sessionID, GetQueriesParam param) {
        WSResponse response = new WSResponse();
        try {
            Session session = sessionMgr.getAndCheckSession(sessionID);
            UserVirtualEngine u = (UserVirtualEngine)userVirtualMgr.getUserVirtual(session.getLogin());
            boolean onlyFbMoteur = EngineConfiguration.getInstance().getIntValue("request.onlyFbMoteur", 0) == 1;
            if (u == null) {
                log.error("login unknown : login=" + session.getLogin());
                throw new ServiceException(ServiceExceptionType.SESSION_UNKNOWN_LOGIN);
            }
            List<WSComputeQuery> listQuery = null;
            // user for test ?
            if (u.isTest()) {
                for (int i=0; i < param.nbMax; i++) {
                    listQuery.add(QueryTest.createQuery());
                }
            }
            // option only fbmoteur &  user not fb moteur => return null
            else if (onlyFbMoteur && !u.isFbMoteur()) {
                return null;
            }
            // search query for user ...
            else {
                int nbMaxQueries = EngineConfiguration.getInstance().getIntValue("request.nbMaxQueries", 1);
                int nbQueries = param.nbMax;
                if (nbQueries > nbMaxQueries) {
                    nbQueries = nbMaxQueries;
                }
                listQuery = queueMgr.getListQueryForUser(u, nbQueries);
                List<GetQueryResponse> listResp = new ArrayList<>();
                for (WSComputeQuery q : listQuery) {
                    GetQueryResponse resp = new GetQueryResponse();
                    resp.computeID = q.getComputeID();
                    resp.conventions = q.getConventions();
                    resp.deal = q.getDeal();
                    resp.game = q.getGame();
                    resp.options = q.getOptions();
                    resp.pollingValue = queueMgr.getPollingValue(u.getLogin());
                    resp.queryType = q.getQueryType();
                    resp.nbTricksForClaim = q.getNbTricksForClaim();
                    resp.claimPlayer = q.getClaimPlayer();
                    listResp.add(resp);
                }
                response.setData(listResp);
            }
            if (param.engineStat != null) {
                u.setEngineStat(param.engineStat);
            }
        } catch (ServiceException e) {
            response.setException(new WSException(e.getType()));
        } catch (Exception e) {
            log.error("Exception on getQuery : "+e.getMessage(), e);
            response.setException(new WSException(ServiceExceptionType.COMMON_INTERNAL_SERVER_ERROR));
        }
        return response;
    }

	@Override
	public WSResponse setResult(String sessionID, SetResultParam param) {
        WSResponse response = new WSResponse();
        if (param != null && param.isValid()) {
            try {
                Session session = sessionMgr.getAndCheckSession(sessionID);
                UserVirtualEngine u = (UserVirtualEngine)userVirtualMgr.getUserVirtual(session.getLogin());
                if (u == null) {
                    log.error("login unknown : login=" + session.getLogin());
                    throw new ServiceException(ServiceExceptionType.SESSION_UNKNOWN_LOGIN);
                }
                u.setDateLastResult(System.currentTimeMillis());
                if (!u.isTest()) {
                    long ts = System.currentTimeMillis();
                        try {
                            if (queueMgr.setRequestResult(param.computeID, param.result, u)) {
                                if (u.isFbMoteur()) {
                                    nbResultFbMoteur++;
                                    u.incrementNbCompute();
                                    u.decrementNbRequestsInProgress();
                                } else {
                                    nbResultOthers++;
                                }
                            }
                        } catch (Exception e) {
                            log.error("Exception to set result for request=" + param.computeID + " - exception=" + e.getMessage(), e);
                        }
                    logStatMgr.logInfo(false, (System.currentTimeMillis() - ts), "setResult - login="+session.getLogin()+" - result="+param.result);
                }
            } catch (ServiceException e) {
                response.setException(new WSException(e.getType()));
            } catch (Exception e) {
                log.error("Exception on setResult : " + e.getMessage(), e);
                response.setException(new WSException(ServiceExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.error("Parameter not valid");
            response.setException(new WSException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @Override
    public WSResponse setResults(String sessionID, SetResultsParam param) {
        WSResponse response = new WSResponse();
        if (param != null && param.isValid()) {
            try {
                Session session = sessionMgr.getAndCheckSession(sessionID);
                UserVirtualEngine u = (UserVirtualEngine)userVirtualMgr.getUserVirtual(session.getLogin());
                if (u == null) {
                    log.error("login unknown : login=" + session.getLogin());
                    throw new ServiceException(ServiceExceptionType.SESSION_UNKNOWN_LOGIN);
                }
                u.setDateLastResult(System.currentTimeMillis());
                if (!u.isTest()) {
                    long ts = System.currentTimeMillis();
                    if (param.results != null) {
                        for (SetResultParam res : param.results) {
                            try {
                                if (queueMgr.setRequestResult(res.computeID, res.result, u)) {
                                    if (u.isFbMoteur()) {
                                        nbResultFbMoteur++;
                                    } else {
                                        nbResultOthers++;
                                    }
                                }
                            } catch (Exception e) {
                                log.error("Exception to set result for request=" + res.computeID + " result="+res.result+" - exception=" + e.getMessage(), e);
                            }
                        }
                    }
                    logStatMgr.logInfo(false, (System.currentTimeMillis() - ts), "setResult - login="+session.getLogin()+" - nb result="+(param.results!=null?param.results.size():"null"));
                }
                if (param.engineStat != null) {
                    u.setEngineStat(param.engineStat);
                }
            } catch (ServiceException e) {
                response.setException(new WSException(e.getType()));
            } catch (Exception e) {
                log.error("Exception on setResult : " + e.getMessage(), e);
                response.setException(new WSException(ServiceExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.error("Parameter not valid");
            response.setException(new WSException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }
}
