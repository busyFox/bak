package com.gotogames.bridge.engineserver.ws.servlet;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gotogames.bridge.engineserver.common.Constantes;
import com.gotogames.bridge.engineserver.common.ContextManager;
import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import com.gotogames.bridge.engineserver.request.QueueMgr;
import com.gotogames.bridge.engineserver.request.data.QueueData;
import com.gotogames.bridge.engineserver.user.UserVirtualEngine;
import com.gotogames.bridge.engineserver.user.UserVirtualMgr;
import com.gotogames.bridge.engineserver.ws.ServiceException;
import com.gotogames.bridge.engineserver.ws.ServiceExceptionType;
import com.gotogames.common.tools.JSONTools;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component(value="engineWebsocketEndpoint")
@ServerEndpoint(value = "/wsengine")
public class EngineWebsocketEndpoint {
    private Session session;
    private Logger logger = ContextManager.getUserMgr().getLogger();
    private String clientID;
    private UserVirtualEngine userEngine;
    private long tsLastActivity = 0;
    private JSONTools jsonTools = new JSONTools();
    private EngineConfiguration config = EngineConfiguration.getInstance();
    private QueueMgr queueMgr = ContextManager.getQueueMgr();

    public void closeSession() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (IOException e) {
            logger.error("Failed to close session", e);
        }
    }

    @OnOpen
    public void onOpen(Session session) throws IOException {
        this.session = session;
        tsLastActivity = System.currentTimeMillis();
        String clientID = session.getRequestParameterMap().get("clientID").get(0);
        if (clientID == null) {
            clientID = "engine";
        }
        UserVirtualMgr userVirtualMgr = ContextManager.getUserMgr();
        userEngine = (UserVirtualEngine)userVirtualMgr.createUserVirtual(clientID);
        if (userEngine == null) {
            logger.error("EngineWebsocketEndpoint:onOpen - Failed to create user - unknown login="+clientID);
            throw new IOException("Failed to create user - unknown login="+clientID);
        }
        userEngine.setWebSocket(this);
        userVirtualMgr.addUserEngine(userEngine);
        if (logger.isDebugEnabled()) {
            logger.debug("EngineWebsocketEndpoint:onOpen - Create websocket for user={"+userEngine+"} - clientID="+clientID);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason status) {
        tsLastActivity = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("WebSocket - onClose for user="+userEngine+" - status="+status);
        }
        userEngine.removeWebSocket(this);
        ContextManager.getUserMgr().removeUser(userEngine, false);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        if (config.getIntValue("general.websocketLogOnError", 0) == 1) {
            logger.error("WebSocket - Error - e=" + throwable.getMessage() + " - userEngine=" + userEngine, throwable);
        }
    }

    @OnMessage
    public void onMessage(String message) throws IOException {
        tsLastActivity = System.currentTimeMillis();
        if (session == null) {
            logger.error("WebSocket - session is null => throw IOException");
            throw new IOException("Session is null");
        }
        if (userEngine == null) {
            logger.error("WebSocket - user is null => throw IOException");
            throw new IOException("User is null");
        }
//        if (logger.isInfoEnabled()) {
//            logger.info("WebSocket - onTextMessage - text received for user="+userEngine+" - message="+message);
//        }
        userEngine.setDateLastActivity(System.currentTimeMillis());
        if (message != null && message.length() > 0) {
            if (message.equals("GOTO")) {
                synchronized (session) {
                    if (session.getBasicRemote() != null) {
                        session.getBasicRemote().sendText(message);
                    }
                }
            }
            else {
                try {
                    EngineWSCommand command = jsonTools.mapData(message, EngineWSCommand.class);
                    if (logger.isDebugEnabled()) {
                        logger.debug("WebSocket - onTextMessage - user="+userEngine+" - command="+command+" - message="+message);
                    }
                    if (command.command != null && command.command.length() > 0) {
                        /**-----------------
                         * COMMAND STATS
                         * -----------------**/
                        if (command.command.equals("stats")) {
                            // update user stats value
                            try {
                                EngineWSCommandStats stats = command.parseCommandStats();
                                boolean isFirstStats = userEngine.getDateLastStat() == 0;
                                userEngine.setNbThread(stats.nbThread);
                                userEngine.setMaxThread(stats.maxThread);
                                userEngine.setQueueSize(stats.queueSize);
                                userEngine.setComputeTime((int)stats.computeTime);
                                userEngine.setDateLastStat(System.currentTimeMillis());
                                boolean needToUpdate = false;
                                if (stats.version != null && stats.version.length() > 0 && (userEngine.getVersion() == null || userEngine.getVersion().equals(stats.version))) {
                                    userEngine.setVersion(stats.version);
                                    // check version
                                    String versionMin = config.getStringValue("user.engineVersionMinimum", null);
                                    if (versionMin != null && versionMin.length() > 0) {
                                        // version MINIMUM greater than stats.version => need to update !
                                        if (versionMin.compareTo(stats.version) > 0) {
                                            logger.warn("Engine version "+stats.version+" is obsolete => send command to update userEngine="+userEngine);
                                            if (sendCommandUpdate()) {
                                                needToUpdate = true;
                                            }
                                        }
                                    }

                                }
                                // set list engine supported
                                if (!needToUpdate && stats.engineList != null && stats.engineList.length() > 0) {
                                    // scan engine version list
                                    List<Integer> listVersion = new ArrayList<Integer>();
                                    try {
                                        String[] temp = stats.engineList.split(Constantes.REQUEST_FIELD_SEPARATOR);
                                        for (String v : temp) {
                                            listVersion.add(Integer.parseInt(v));
                                        }
                                        userEngine.setListEngine(listVersion);
                                    } catch (Exception e) {
                                        logger.error("An engine version is not valid ! engineList="+stats.engineList+" - userEngine="+userEngine, e);
                                        throw new ServiceException(ServiceExceptionType.COMMON_PARAMETER_NOT_VALID);
                                    }
                                }
                                if (stats.values != null) {
                                    userEngine.getEngineStat().totalRequest = stats.values.count;
                                    userEngine.getEngineStat().averageTimeRequest = stats.values.computeComputeTimeRequest();
                                    userEngine.getEngineStat().date = System.currentTimeMillis();
                                    userEngine.getEngineStat().averageNbRequestHour = stats.values.computeNbRequestByHour();
                                }

                                if (stats.discard != null && stats.discard.size() > 0) {
                                    if (config.getIntValue("user.engineDiscardProcess", 1) == 1) {
                                        if (queueMgr != null && userEngine != null) {
                                            for (Long rqtId : stats.discard) {
                                                queueMgr.removeEngineOnData(rqtId, userEngine.getID());
                                            }
                                        }
                                    }
                                }
                                // First connexion => send command
                                if (!needToUpdate && isFirstStats) {
                                    sendCommandInit();
                                }
                                // First connexion of this engine (nb compute=0) OR data are waiting => send list of waiting data
                                if (!needToUpdate && !userEngine.isForTest() && !userEngine.isForCompare() && (userEngine.getNbCompute() == 0 || ContextManager.getQueueMgr().getQueueSizeNoEngine() > 0)) {
                                    List<QueueData> dataWaiting = ContextManager.getQueueMgr().getNextDataList(userEngine, userEngine.getNbThread() > 0?userEngine.getNbThread():1);
                                    for (QueueData e : dataWaiting) {
                                        sendCommandCompute(e);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("Failed to update user stats - command="+command+" - user="+userEngine, e);
                            }
                        }
                        /**-----------------
                         * COMMAND RESULT
                         * -----------------**/
                        else if (command.command.equals("result")) {
                            // set result for request
                            try {
                                EngineWSCommandResult result = command.parseCommandResult();
                                if (config.getIntValue("user.engineNbThreadIncrementOnResult", 0) == 1) {
                                    result.nbThread++;
                                }
                                userEngine.setNbThread(result.nbThread);
                                userEngine.setMaxThread(result.maxThread);
                                userEngine.setQueueSize(result.queueSize);
                                userEngine.setComputeTime((int)result.computeTime);
                                userEngine.incrementNbCompute();
                                userEngine.decrementNbRequestsInProgress();
                                userEngine.setDateLastResult(System.currentTimeMillis());
                                if (config.getIntValue("general.metrics.userEngine.enable", 1) == 1) {
                                    userEngine.markMetricsRequest();
                                }
                                if (userEngine.isForTest()) {
                                    // process result
                                    ContextManager.getQueueMgr().setRequestTestResult(result.computeID, result.answer, userEngine);
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("WebSocket - onMessage command result TEST engine - result="+result+" - userEngine="+userEngine);
                                    }
                                }
                                else if (userEngine.isForCompare()) {
                                    // process result
                                    ContextManager.getQueueMgr().setRequestCompareResult(result.computeID, result.answer, userEngine);
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("WebSocket - onMessage command result COMPARE engine - result="+result+" - userEngine="+userEngine);
                                    }
                                }
                                else {
                                    // process result
                                    ContextManager.getQueueMgr().setRequestResult(result.computeID, result.answer, userEngine);
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("WebSocket - onMessage command result - after setRequestResult - result="+result+" - userEngine="+userEngine);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("Failed to process result command - command="+command+" - userEngine="+userEngine, e);
                            }
                        }
                        /**-----------------
                         * COMMAND LEAVE
                         * -----------------**/
                        else if (command.command.equals("leave")) {
                            // close web socket
                            userEngine.removeWebSocket(this);
                        } else {
                            logger.error("WebSocket - command value not supported - message="+message);
                        }
                    }
                    else {
                        logger.error("WebSocket - command field null - message="+message);
                    }
                } catch (JsonGenerationException e) {
                    logger.error("WebSocket - JsonGenerationException - e="+e.getMessage()+" - userEngine="+userEngine, e);
                } catch (JsonMappingException e) {
                    logger.error("WebSocket - JsonMappingException - e="+e.getMessage()+" - userEngine="+userEngine, e);
                } catch (IOException e) {
                    logger.error("WebSocket - IOException - e="+e.getMessage()+" - userEngine="+userEngine, e);
                } catch (Exception e) {
                    logger.error("WebSocket - Exception - e="+e.getMessage()+" - userEngine="+userEngine, e);
                }
            }
        }
    }

    private boolean sendCommand(EngineWSCommand command) {
        if (command != null) {
            try {
                String data = jsonTools.transform2String(command, false);
                if (logger.isDebugEnabled()) {
                    logger.debug("Send command data="+data);
                }
                if (session == null) {
                    throw new Exception("Session is null !");
                }
                synchronized (session) {
                    if (data != null && data.length() > 0 && session.getBasicRemote() != null) {
                        session.getBasicRemote().sendText(data);
                    }
                }
                return true;
            } catch (JsonGenerationException e) {
                logger.error("WebSocket - sendCommand - JsonGenerationException - command=" + command, e);
            } catch (JsonMappingException e) {
                logger.error("WebSocket - sendCommand - JsonMappingException - command=" + command, e);
            } catch (IOException e) {
                logger.error("WebSocket - sendCommand - IOException - command=" + command, e);
            } catch (Exception e) {
                logger.error("WebSocket - sendCommand - Exception - command=" + command, e);
            }
        }
        return false;
    }

    /**
     * Write on websocket to send compute command
     * @param queueData
     * @return
     * @throws Exception
     */
    public boolean sendCommandCompute(QueueData queueData) throws Exception {
        if (userEngine == null) {
            logger.debug("WebSocket - user is null => throw IOException");
            throw new Exception("User is null");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("WebSocket - sendCommandCommpute - send command compute for userEngine="+userEngine+" - queueData="+queueData);
        }
        if (queueData != null) {
            EngineWSCommand cmdCompute = EngineWSCommand.buildCommandCompute(
                    queueData.ID,
                    queueData.getDeal(),
                    queueData.getGame(),
                    queueData.getConventions(),
                    queueData.getOptions(),
                    queueData.getRequestType(),
                    queueData.getNbTricksForClaim(),
                    queueData.getClaimPlayer());
            boolean result = sendCommand(cmdCompute);
            if (result && userEngine != null) {
                userEngine.incrementNbRequestsInProgress();
            }
            return result;
        }
        return false;
    }

    /**
     * Write on websocket to send stop command
     * @return
     * @throws Exception
     */
    public boolean sendCommandStop() throws Exception {
        if (userEngine == null) {
            logger.debug("WebSocket - user is null => throw IOException");
            throw new Exception("User is null");
        }
        logger.warn("WebSocket - sendCommandStop - send command stop for user="+userEngine);
        return sendCommand(EngineWSCommand.buildCommandStop());
    }

    /**
     * Write on websocket to send reboot command
     * @return
     * @throws Exception
     */
    public boolean sendCommandReboot() throws Exception {
        if (userEngine == null) {
            logger.debug("WebSocket - user is null => throw IOException");
            throw new Exception("User is null");
        }
        logger.warn("WebSocket - sendCommandReboot - send command reboot for user="+userEngine);
        return sendCommand(EngineWSCommand.buildCommandReboot());
    }

    /**
     * Write on websocket to send restart command
     * @return
     * @throws Exception
     */
    public boolean sendCommandRestart() throws Exception {
        if (userEngine == null) {
            logger.debug("WebSocket - user is null => throw IOException");
            throw new Exception("User is null");
        }
        logger.warn("WebSocket - sendCommandRestart - send command update for userEngine="+userEngine);
        return sendCommand(EngineWSCommand.buildCommandRestart(0));
    }

    /**
     * Write on websocket to send update command
     * @return
     * @throws Exception
     */
    public boolean sendCommandUpdate() throws Exception {
        if (userEngine == null) {
            logger.debug("WebSocket - user is null => throw IOException");
            throw new Exception("User is null");
        }
        // use value from config file
//        String downloadURL = config.getStringValue("user."+userEngine.getLoginPrefix()+".updateDownloadURL", null);
        String downloadURL = UserVirtualMgr.getConfigStringValue(userEngine.getLogin(), userEngine.getLoginPrefix(),"updateDownloadURL", null);
        if (downloadURL == null || downloadURL.length() == 0) {
            downloadURL = config.getStringValue("user.updateDownloadURL", null);
        }
        if (downloadURL == null || downloadURL.length() == 0) {
            logger.error("FAILED sendCommandUpdate ! No downloadURL define for userEngine="+userEngine);
            return false;
        } else {
            logger.warn("WebSocket - sendCommandUpdate - send command update for userEngine="+userEngine+" - downloadURL="+downloadURL);
            return sendCommand(EngineWSCommand.buildCommandUpdate(downloadURL));
        }
    }

    /**
     * Send a dummy request to force update of DLL engine version
     * @param engineDLL
     * @return
     * @throws Exception
     */
    public boolean sendCommandUpdateDLL(int engineDLL) throws Exception {
        if (userEngine == null) {
            logger.debug("WebSocket - user is null => throw IOException");
            throw new Exception("User is null");
        }

        String optionEngineDLL = Constantes.buildOptionsForEngine(0, engineDLL, 0, 0, 0);
        EngineWSCommand cmdCompute = EngineWSCommand.buildCommandCompute(
                -1,
                Constantes.REQUEST_DEAL_BIDINFO,
                "",
                "",
                optionEngineDLL,
                100,
                0,
                "?");
        return sendCommand(cmdCompute);
    }

    /**
     * Write on websocket to send init command
     * @return
     * @throws Exception
     */
    public boolean sendCommandInit() throws Exception {
        if (config.getIntValue("user.commandInitEnable", 1) == 0) {
            logger.warn("Command init is disable - userEngine="+userEngine);
            return false;
        } else {
            if (UserVirtualMgr.getConfigIntValue(userEngine.getLogin(), userEngine.getLoginPrefix(), "commandInitEnable", 1) == 0) {
//            if (config.getIntValue("user."+userEngine.getLoginPrefix()+".commandInitEnable", 1) == 0) {
                logger.warn("Command init is disable for this userEngine="+userEngine);
                return false;
            }
        }
        if (userEngine == null) {
            logger.debug("WebSocket - user is null => throw IOException");
            throw new Exception("User is null");
        }
//        String httpEngineURL = config.getStringValue("user."+userEngine.getLoginPrefix()+".updateArgineDownloadURL", null);
        String httpEngineURL = UserVirtualMgr.getConfigStringValue(userEngine.getLogin(), userEngine.getLoginPrefix(),"updateArgineDownloadURL", null);
        if (httpEngineURL == null || httpEngineURL.length() == 0) {
            httpEngineURL = config.getStringValue("user.updateArgineDownloadURL", null);
        }
        if (httpEngineURL != null && httpEngineURL.length() > 0) {
            logger.warn("WebSocket - sendCommandInit - send command init for userEngine="+userEngine+" - httpEngineURL="+httpEngineURL);
            return sendCommand(EngineWSCommand.buildCommandInit(httpEngineURL));
        }
        logger.error("FAILED sendCommandInit - httpEngineURL is null or empty - userEngine="+userEngine);
        return false;
    }
}
