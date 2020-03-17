package com.gotogames.bridge.engineserver.ws.session;

import com.gotogames.bridge.engineserver.common.Constantes;
import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import com.gotogames.bridge.engineserver.session.EngineSessionMgr;
import com.gotogames.bridge.engineserver.user.UserVirtual;
import com.gotogames.bridge.engineserver.user.UserVirtualEngine;
import com.gotogames.bridge.engineserver.user.UserVirtualFBServer;
import com.gotogames.bridge.engineserver.user.UserVirtualMgr;
import com.gotogames.bridge.engineserver.ws.ServiceException;
import com.gotogames.bridge.engineserver.ws.ServiceExceptionType;
import com.gotogames.bridge.engineserver.ws.WSException;
import com.gotogames.bridge.engineserver.ws.WSResponse;
import com.gotogames.common.crypt.Encryption;
import com.gotogames.common.session.Session;
import com.gotogames.common.tools.StringTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component(value="sessionServiceImpl")
@Scope(value="singleton")
public class SessionServiceImpl implements SessionService {
	private Logger log = LogManager.getLogger(this.getClass());
	
	@Resource(name="sessionMgr")
	private EngineSessionMgr sessionMgr;
	
	@Resource(name="userVirtualMgr")
	private UserVirtualMgr userVirtualMgr;

	@PostConstruct
	public void init() {
		if (sessionMgr == null || userVirtualMgr == null) {
			log.error("Parameters null : sessionMgr | userVirtualMgr");
		}
	}
	
	@PreDestroy
	public void destroy() {
		log.info("Nothing to destroy ...");
	}

    @Override
    public WSResponse hello(HelloParam param) {
        WSResponse response = new WSResponse();
        HelloResponse resp = new HelloResponse();
        resp.response = "Hello "+param.name+". Time is : "+Constantes.timestamp2StringDateHour(System.currentTimeMillis());
        response.setData(resp);
        return response;
    }

	@Override
    public WSResponse checkChallenge(CheckChallengeParam param) {
        WSResponse response = new WSResponse();
        if (param != null && param.isValid()) {
            try {
                // retrieve user with this challenge
                UserVirtual u = userVirtualMgr.getUserByChallenge(param.challenge);
                if (u == null || !u.getLogin().equals(param.login)) {
                    // response doesn't correspond to a valid challenge
                    log.warn("login not corresponding to challenge : login="+param.login);
                    throw new ServiceException(ServiceExceptionType.SESSION_INVALID_CHALLENGE);
                }

                log.debug("loginID="+u.getLoginID()+" - challenge OK");
                Session s = sessionMgr.createSession(u);
                if (s == null) {
                    throw new ServiceException(ServiceExceptionType.COMMON_INTERNAL_SERVER_ERROR, "ERROR CREATING SESSION");
                }
                if (param.urlFBSetResult != null && param.urlFBSetResult.length() > 0) {
                    if (u instanceof UserVirtualFBServer) {
                        ((UserVirtualFBServer)u).setUrlFBSetResult(param.urlFBSetResult);
                    }
                }
                CheckChallengeResponse resp = new CheckChallengeResponse();
                resp.sessionID = s.getID();
                response.setData(resp);
            } catch (ServiceException e) {
                response.setException(new WSException(e.getType()));
            }
        }
        else {
            log.error("Parameter not valid");
            response.setException(new WSException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

	@Override
    public WSResponse closeSession(CloseSessionParam param) {
        WSResponse response = new WSResponse();
        if (param != null && param.isValid()) {
            try {
                Session s = sessionMgr.getAndCheckSession(param.sessionID);
                log.debug("login="+s.getLogin());

                // close the session
                CloseSessionResponse resp = new CloseSessionResponse();
                resp.result = sessionMgr.closeSession(param.sessionID);
                response.setData(resp);
            } catch (ServiceException e) {
                response.setException(new WSException(e.getType()));
            }
        }
        else {
            log.error("Parameter not valid - param="+param);
            response.setException(new WSException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

	@Override
    public WSResponse isSessionValid(IsSessionValidParam param) {
        WSResponse response = new WSResponse();
        if (param != null && param.isValid()) {
            IsSessionValidResponse resp = new IsSessionValidResponse();
            resp.result = sessionMgr.isSessionValid(param.sessionID);
            response.setData(resp);
        }
        else {
            log.error("Parameter not valid");
            response.setException(new WSException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

	@Override
    public WSResponse openSession(OpenSessionParam param) {
        WSResponse response = new WSResponse();
        if (param != null && param.isValid()) {
            try {
                UserVirtual u = userVirtualMgr.createUserVirtual(param.login);
                if (u == null) {
                    log.error("user type unknown : login="+param.login);
                    throw new ServiceException(ServiceExceptionType.SESSION_UNKNOWN_LOGIN);
                }

                // user is found
                // generate challenge using random string crypt by password
                String challenge = StringTools.generateRandomString(EngineConfiguration.getInstance().getIntValue("session.challengeRandomLength", 30));
                challenge = StringTools.strToHexa(challenge);
                // set challenge to user
                u.setChallenge(challenge);
                // crypt the challenge using user password
                String challengeCrypt = Encryption.simpleCrypt(u.getPassword(), challenge);
                // return challenge crypt in hexadecimal format
                OpenSessionResponse resp = new OpenSessionResponse();
                resp.challenge = StringTools.strToHexa(challengeCrypt);
                response.setData(resp);
            } catch (ServiceException e) {
                response.setException(new WSException(e.getType()));
            }
        }
        else {
            log.error("Parameter not valid");
            response.setException(new WSException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

	@Override
    public WSResponse openSessionEngineVersion(OpenSessionEngineVersionParam param) {
        WSResponse response = new WSResponse();
        if (param != null && param.isValid()) {
            try {
                UserVirtualEngine u = (UserVirtualEngine)userVirtualMgr.createUserVirtual(param.login);
                if (u == null) {
                    log.error("user type unknown : login="+param.login);
                    throw new ServiceException(ServiceExceptionType.SESSION_UNKNOWN_LOGIN);
                }
                // scan engine version list
                List<Integer> listVersion = new ArrayList<Integer>();
                if (param.listEngineVersion != null && param.listEngineVersion.length() > 0) {
                    try {
                        String[] temp = param.listEngineVersion.split(Constantes.REQUEST_FIELD_SEPARATOR);
                        for (String v : temp) {
                            listVersion.add(Integer.parseInt(v));
                        }
                    } catch (Exception e) {
                        log.error("An engine version is not valid ! listEngineVersion="+param.listEngineVersion+" - login="+param.login, e);
                        throw new ServiceException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID);
                    }
                }
                // set list engine supported
                u.setListEngine(listVersion);

                // user is found
                // generate challenge using random string crypt by password
                String challenge = StringTools.generateRandomString(EngineConfiguration.getInstance().getIntValue("session.challengeRandomLength", 30));
                challenge = StringTools.strToHexa(challenge);
                // set challenge to user
                u.setChallenge(challenge);
                // crypt the challenge using user password
                String challengeCrypt = Encryption.simpleCrypt(u.getPassword(), challenge);
                // return challenge crypt in hexadecimal format
                OpenSessionEngineVersionResponse resp = new OpenSessionEngineVersionResponse();
                resp.challenge = StringTools.strToHexa(challengeCrypt);
                response.setData(resp);
            } catch (ServiceException e) {
                response.setException(new WSException(e.getType()));
            }
        }
        else {
            log.error("Parameter not valid");
            response.setException(new WSException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

}
