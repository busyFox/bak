package com.funbridge.server.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.gotogames.common.bridge.BridgeBid;
import com.gotogames.common.bridge.GameBridgeRule;
import com.gotogames.common.crypt.Encryption;
import com.gotogames.common.services.JSONService;
import com.gotogames.common.services.JSONServiceException;
import com.gotogames.common.tools.StringTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EngineRest {
//	private JSONService engineService = new JSONService();
	private String sessionID = null;
	private int timeoutGetResult = 0;
	private String urlServiceSession = "", urlServiceRequest = "";
	private String login, password;
    private String urlSetResult;
	private Logger log = LogManager.getLogger(this.getClass());
	private String name = "UNKNOWN";
	private EngineWebsocketMgr websocketMgr = null;
	private boolean engineServerEnable = true;

	public String toString() {
		return "name="+name+" - urlServiceSession="+urlServiceSession+" - urlServiceRequest="+urlServiceRequest+" - login="+login+" - password="+password+" - sessionID="+sessionID+" - websocketMgr="+websocketMgr;
	}

	public EngineRest(String name) {
	    this.name = name;
        websocketMgr = new EngineWebsocketMgr(name);
    }

	public String getName() {
		return name;
	}

    public void setEngineServerEnable(boolean enable) {
	    engineServerEnable = enable;
    }
	
	public void init() {
        loadConfig();
	}

	public void loadConfig() {
		timeoutGetResult = FBConfiguration.getInstance().getIntValue("enginerest.request.timeoutGetResult", 120)*1000;
		login = FBConfiguration.getInstance().getStringValue("enginerest.login", "fbserver");
		password = FBConfiguration.getInstance().getStringValue("enginerest.password", "themaster");
        urlSetResult = FBConfiguration.getInstance().getStringResolvEnvVariableValue("enginerest.urlSetResult", null);
		String servHost = FBConfiguration.getInstance().getStringValue("enginerest.host", "localhost");
		String servPort = FBConfiguration.getInstance().getStringValue("enginerest.port", "8080");
		String serverApp = FBConfiguration.getInstance().getStringValue("enginerest.serverApp", "engine-server-ws");
		String serverVersion = FBConfiguration.getInstance().getStringValue("enginerest.serverVersion", "");
		String serviceURL = "http://"+servHost;
		if (servPort != null && servPort.length() > 0) {
			serviceURL += ":"+servPort;
		}
		serviceURL += "/"+serverApp;
		if (serverVersion != null && serverVersion.length() > 0) {
			serviceURL += "-"+serverVersion;
		}
		urlServiceSession = serviceURL+"/"+FBConfiguration.getInstance().getStringValue("enginerest.serviceSession", "session");
		urlServiceRequest = serviceURL+"/"+FBConfiguration.getInstance().getStringValue("enginerest.serviceRequest", "request");
		log.info("name=" + name + " - urlServiceSession=" + urlServiceSession);
        log.info("name="+name+" - urlServiceRequest="+urlServiceRequest);
	}

	public String getURLServiceSession() {
		return urlServiceSession;
	}
	
	public String getURLServiceRequest() {
		return urlServiceRequest;
	}
	
	public String getLogin() {
		return login;
	}
	
	public String getPassword() {
		return password;
	}

    public String getUrlSetResult() {
        return urlSetResult;
    }
	
	public int getTimeoutGetResult() {
		return timeoutGetResult;
	}

    public EngineWebsocketMgr getWebsocketMgr() {
        return websocketMgr;
    }

    public boolean isEngineServerEnable() {
	    if (FBConfiguration.getInstance().getIntValue("engine.server.enable", 1) == 1) {
	        return engineServerEnable;
        }
        return false;
    }

    public void openSession() {
		try {
			init();
			JSONService engineService = new JSONService(false);
			log.info("name="+name+" - Try to open session ... ");
			// method openSession to get challenge
			String urlOpenSession = urlServiceSession + "/openSession";
			EngineRestOpenSessionParam sessionParam = new EngineRestOpenSessionParam();
			sessionParam.login = login;
			EngineRestResponse<EngineRestOpenSessionResponse> respOpenSession = engineService.callService(
					urlOpenSession,
					sessionParam,
					null,
					new TypeReference<EngineRestResponse<EngineRestOpenSessionResponse>>(){},
					timeoutGetResult);
			
			if (respOpenSession == null) {
				throw new Exception("name="+name+" - respOpenSession is null - login="+login);
			} 
			if (respOpenSession.isException()) {
				log.error("Exception on openSession : "+respOpenSession.getExceptionString());
				throw new Exception("name="+name+" - Exception on openSession - "+respOpenSession.getExceptionString());
			}
			String challenge = respOpenSession.data.challenge;
			
			// decrypt challenge
			challenge = StringTools.hexaToStr(challenge);
			challenge = Encryption.simpleDecrypt(password, challenge);
			
			// methode checkChallenge to get sessionID
			EngineRestCheckChallengeParam challengeParam = new EngineRestCheckChallengeParam();
			challengeParam.challenge = challenge;
			challengeParam.login = login;
            challengeParam.urlFBSetResult = urlSetResult;
			String urlCheckChallenge = urlServiceSession + "/checkChallenge";
			EngineRestResponse<EngineRestCheckChallengeResponse> respCheckChallenge = engineService.callService(
					urlCheckChallenge,
					challengeParam,
					null,
					new TypeReference<EngineRestResponse<EngineRestCheckChallengeResponse>>(){},
					timeoutGetResult);
			if (respCheckChallenge == null) {
				throw new Exception("name="+name+" - respCheckChallenge is null - login="+login);
			}
			if (respCheckChallenge.isException()) {
				log.error("name="+name+" - Exception on checkChallenge : "+respOpenSession.getExceptionString());
				throw new Exception("name="+name+" - Exception on checkChallenge - "+respOpenSession.getExceptionString());
			}
			sessionID = respCheckChallenge.data.sessionID;
		} catch (JSONServiceException e) {
			log.error("Error on JSONService", e);
		} catch (Exception e) {
			log.error("Exception ", e);
		}
	}

	private EngineRestGetResultResponse getResultWebSocket(BridgeEngineParam param, int requestType) {
        if (param != null) {
            if (log.isDebugEnabled()) {
                log.debug("Use websocket - requestType="+requestType);
            }
            if (websocketMgr == null) {
                return getResult(param, requestType);
            }
            if (!websocketMgr.sendCommandGetResult(param, requestType)) {
                return getResult(param, requestType);
            }
            EngineRestGetResultResponse response = new EngineRestGetResultResponse();
            response.result = "ASYNC";
            return response;
        } else {
            log.error("name="+name+" - Param is null");
        }
        return null;
    }


	private EngineRestGetResultResponse getResult(BridgeEngineParam param, int requestType) {
		if (param != null) {
		    if (log.isDebugEnabled()) {
		        log.debug("Use service REST - requestType="+requestType);
            }
			try {
				if (sessionID == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("SessionID is null - openSession - param="+param+" - requestType="+requestType);
                    }
					// init to get sessionID
					openSession();
				}
				long ts = System.currentTimeMillis();
				JSONService engineService = new JSONService(false);
				log.debug("Time to create JSONService : "+(ts-System.currentTimeMillis()));
				EngineRestGetResultParam getResultParam = new EngineRestGetResultParam();
				getResultParam.conventions = param.getConventions();
				getResultParam.deal = param.getDealerStr()+param.getVulStr()+param.getDistrib();
				getResultParam.game = param.getBidList()+param.getCardList();
				getResultParam.options = param.getOptions();
				getResultParam.requestType = requestType;
                getResultParam.useCache = param.isUseCache();
                getResultParam.asyncID = param.getAsyncID();
                getResultParam.nbTricksForClaim = param.getNbTricksForClaim();
                getResultParam.claimPlayer = Character.toString(param.getClaimPlayer());
				Map<String, String> headerParams = new HashMap<String, String>();
				headerParams.put("sessionID", sessionID);
				EngineRestResponse<EngineRestGetResultResponse> getResultResponse = engineService.callService(
						urlServiceRequest+"/getResult",
						getResultParam,
						headerParams,
						new TypeReference<EngineRestResponse<EngineRestGetResultResponse>>(){},
						timeoutGetResult);
				if (getResultResponse != null) {
					// Response contains exception
					if (getResultResponse.isException()) {
						// search exception type SESSION ID NOT VALID
						if (getResultResponse.getExceptionType().equals("SESSION_INVALID_SESSION_ID")) {
							// openSession to get new sessionID
                            if (log.isDebugEnabled()) {
                                log.debug("Session is invalid - openSession - param="+param+" - requestType="+requestType);
                            }
							openSession();
							headerParams.put("sessionID", sessionID);
							getResultResponse = engineService.callService(
									urlServiceRequest+"/getResult",
									getResultParam,
									headerParams,
									new TypeReference<EngineRestResponse<EngineRestGetResultResponse>>(){},
									timeoutGetResult);
							if (getResultResponse != null) {
								// exception => stop retry
								if (getResultResponse.isException()) {
									log.error("name="+name+" - Exception on getResult : exception="+getResultResponse.getExceptionString()+" - param="+param+" - getResultParam="+getResultParam+" - getResultResponse="+getResultResponse);
								}
								// response OK
								else {
									if (getResultResponse.data == null) {
										log.error("name="+name+" - getResultResponse.data is null - param="+param+" - getResultParam="+getResultParam+" - getResultResponse="+getResultResponse);
									}
									return getResultResponse.data;
								}
							} else {
								log.error("name="+name+" - getResultResponse is null param="+param+" - requestType="+requestType+" - getResultParam="+getResultParam+" - getResultResponse="+getResultResponse);
							}
						}
						// other exception
						else {
							log.error("name="+name+" - Exception on getResult : exception= "+getResultResponse.getExceptionString()+" - param="+param+" - getResultParam="+getResultParam+" - getResultResponse="+getResultResponse);
						}
					}
					// response OK
					else {
						if (getResultResponse.data == null) {
							log.error("name="+name+" - getResultResponse.data is null - param="+param+" - getResultParam="+getResultParam+" - getResultResponse="+getResultResponse);
						}
						return getResultResponse.data;
					}
				} else {
					log.error("name="+name+" - getResultResponse is null param="+param+" - requestType="+requestType+" - getResultParam="+getResultParam+" - getResultResponse="+getResultResponse);
				}
				
				
			} catch (JSONServiceException e) {
				log.error("name="+name+" - Error on JSONService - param="+param, e);
			} catch (Exception e) {
				log.error("name="+name+" - Exception to getResult param="+param+" - requestType="+requestType, e);
			}
		} else {
			log.error("name="+name+" - Param is null");
		}
		return null;
	}
	
	public void destroy() {
		if (websocketMgr != null) {
		    websocketMgr.destroy();
        }
	}

	public BridgeEngineResult getNextCard(BridgeEngineParam param) {
		if (param != null)  {
		    if (isEngineServerEnable()) {
                if (param.isEndBid() && !param.isEndGame()) {
                    EngineRestGetResultResponse resp = null;
                    if (param.isUseWebsocket()) {
                        resp = getResultWebSocket(param, Constantes.ENGINE_REQUEST_TYPE_CARD);
                    } else {
                        resp = getResult(param, Constantes.ENGINE_REQUEST_TYPE_CARD);
                    }
                    if (resp != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("name=" + name + " - Engine result=" + resp.result);
                        }
                        return new BridgeEngineResult(resp.result);
                    } else {
                        log.error("name=" + name + " - Response is null ! param=" + param);
                    }
                } else {
                    log.error("name=" + name + " - STATUS GAME NOT VALID ! param=" + param);
                }
            } else {
                log.error("name="+name+" - Engine server not enable");
            }
		} else {
			log.error("name="+name+" - Param null");
		}
		return new BridgeEngineResult(null);
	}

	public BridgeEngineResult getNextBid(BridgeEngineParam param) {
		if (param != null)  {
		    if (isEngineServerEnable()) {
                if (!param.isEndBid()) {
                    EngineRestGetResultResponse resp = null;
                    if (param.isUseWebsocket()) {
                        resp = getResultWebSocket(param, Constantes.ENGINE_REQUEST_TYPE_BID);
                    } else {
                        resp = getResult(param, Constantes.ENGINE_REQUEST_TYPE_BID);
                    }
                    if (resp != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("name=" + name + " - Engine result=" + resp.result);
                        }
                        return new BridgeEngineResult(resp.result);
                    } else {
                        log.error("name=" + name + " - Response is null ! param=" + param);
                    }
                } else {
                    log.error("name=" + name + " - BIDS ARE FINISHED ! param=" + param);
                }
            } else {
		        log.error("name="+name+" - Engine server not enable");
            }
		} else {
			log.error("name="+name+" - Param null");
		}
		return new BridgeEngineResult(null);
	}

	public BridgeEngineResult getBidInformation(BridgeEngineParam param) {
		if (param != null) {
			if (isEngineServerEnable()) {
                if (param.getCardList().length() == 0) {
                    EngineRestGetResultResponse resp = getResult(param, Constantes.ENGINE_REQUEST_TYPE_BID_INFO);
                    if (resp != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("name=" + name + " - Engine result=" + resp.result);
                        }
                        return new BridgeEngineResult(resp.result);
                    } else {
                        log.error("name=" + name + " - Response is null ! param=" + param);
                    }
                } else {
                    log.error("name=" + name + " - STATUS GAME NOT VALID ! param=" + param);
                }
            } else {
                log.error("name="+name+" - Engine server not enable");
            }
		} else {
			log.error("name="+name+" - Param null");
		}
		return new BridgeEngineResult(null);
	}

	public BridgeEngineResult checkClaim(BridgeEngineParam param) {
        if (param != null) {
            if (isEngineServerEnable()) {
                EngineRestGetResultResponse resp = getResult(param, Constantes.ENGINE_REQUEST_TYPE_CLAIM);
                if (resp != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("name=" + name + " - Engine result=" + resp.result);
                    }
                    return new BridgeEngineResult(resp.result);
                } else {
                    log.error("name=" + name + " - Response is null ! param=" + param);
                }
            } else {
                log.error("name="+name+" - Engine server not enable");
            }
        } else {
            log.error("name="+name+" - Param null");
        }
        return new BridgeEngineResult(null);
    }
	
	public BridgeEngineResult getPar(BridgeEngineParam param) {
        if (param != null) {
            if (isEngineServerEnable()) {
                EngineRestGetResultResponse resp = null;
                if (param.isUseWebsocket()) {
                    resp = getResultWebSocket(param, Constantes.ENGINE_REQUEST_TYPE_PAR);
                } else {
                    resp = getResult(param, Constantes.ENGINE_REQUEST_TYPE_PAR);
                }
                if (resp != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("name=" + name + " - Engine result=" + resp.result);
                    }
                    return new BridgeEngineResult(resp.result);
                } else {
                    log.error("name=" + name + " - Response is null ! param=" + param);
                }
            } else {
                log.error("name="+name+" - Engine server not enable");
            }
        } else {
            log.error("name="+name+" - Param null");
        }
        return new BridgeEngineResult(null);
    }

    /**
     * Get the bid info for a bid sequence. Method with full data in parameter.
     * @param dealer
     * @param vulnerability
     * @param bids sequence of bids without position
     * @param conventionProfile convention profile bids
     * @param conventionData convention data bids (case of free profile)
     * @param cardsConventionProfile convention profile cards
     * @param cardsConventionData convention data cards (case of free profile)
     * @param tournamentResultType 0:PAIRE - 1:IMP
     * @param engineVersion
     * @return
     * @throws com.funbridge.server.ws.FBWSException
     */
    public String getBidInfoFullData(char dealer, char vulnerability, String bids, int conventionProfile, String conventionData, int cardsConventionProfile, String cardsConventionData, int tournamentResultType, int engineVersion) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("Param : bids=" + bids + " - deader=" + dealer + " - vulnerability=" + vulnerability + " - engineVersion=" + engineVersion + " - conventionProfile=" + conventionProfile + " - conventionData=" + conventionData);
        }

        List<BridgeBid> listBid = GameBridgeRule.convertPlayBidsStringToList(bids, 'N');
        if (listBid == null) {
            log.error("Bids sequence not valid ! bids="+bids);
            throw new FBWSException(FBExceptionType.GAME_BID_INFO_ERROR);
        }

        // if bids sequence not valid => no bid info !
        if (!GameBridgeRule.isBidsSequenceValid(listBid)) {
            log.error("Bids sequence is not valid ! bids="+bids);
            throw new FBWSException(FBExceptionType.GAME_BID_INFO_ERROR);
        }

        String info = "";
        boolean bFixBUG4PA = false;
        if (FBConfiguration.getInstance().getIntValue("game.fixBug4PA", 0) == 1) {
            if (bids.equals("PAPAPAPA")) {
                bFixBUG4PA = true;
                log.error("BUG 4PA => Exception");
                info = "0;13;0;13;0;13;0;13;0;40;0;40;#faible;0";
            }
        }
        if (!bFixBUG4PA) {
            if (!isEngineServerEnable()) {
                log.error("name="+name+" - Engine server not enable");
                throw new FBWSException(FBExceptionType.GAME_BID_INFO_ERROR);
            }
            BridgeEngineParam paramEngine = BridgeEngineParam.createParamFullData(
                    "SSSSSSSSSSSSSWWWWWWWWWWWWWNNNNNNNNNNNNNEEEEEEEEEEEEE",
                    bids, "", dealer ,vulnerability, Constantes.ENGINE_REQUEST_TYPE_BID_INFO,
                    engineVersion, tournamentResultType, conventionProfile, conventionData, cardsConventionProfile, cardsConventionData);
            BridgeEngineResult result = getBidInformation(paramEngine);
            if (result.isError()) {
                log.error("Error on result : " + result.getContent()+" - bids="+bids+" - deader="+ dealer +" - vulnerability="+ vulnerability +" - engineVersion="+engineVersion+" - conventionProfile="+conventionProfile+" - conventionData="+conventionData);
            } else {
                info = result.getContent();
            }
            if (log.isDebugEnabled()) {
                log.debug("Bid info=" + info);
            }
            if (info == null || info.length() == 0 || info.equals(Constantes.ENGINE_RESULT_NULL)) {
                log.error("Info for bid not valid : info=" + info + " - bids="+bids+" - deader="+ dealer +" - vulnerability="+ vulnerability +" - engineVersion="+engineVersion+" - conventionProfile="+conventionProfile+" - conventionData="+conventionData);
                throw new FBWSException(FBExceptionType.GAME_BID_INFO_ERROR);
            }
        }
        return info;
    }
}
