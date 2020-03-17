package com.gotogames.bridge.engineserver.ws.servlet;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gotogames.bridge.engineserver.common.ContextManager;
import com.gotogames.bridge.engineserver.user.UserVirtualFBServer;
import com.gotogames.bridge.engineserver.user.UserVirtualMgr;
import com.gotogames.bridge.engineserver.ws.ServiceException;
import com.gotogames.bridge.engineserver.ws.ServiceExceptionType;
import com.gotogames.bridge.engineserver.ws.request.RequestService;
import com.gotogames.bridge.engineserver.ws.request.RequestServiceImpl;
import com.gotogames.common.tools.JSONTools;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

@Component(value="fbserverWebsocketEndpoint")
@ServerEndpoint(value = "/wsfbserver")
public class FBServerWebsocketEndpoint {
    private Session session;
    private Logger logger = ContextManager.getUserMgr().getLogger();
    private long tsLastActivity = 0;
    private UserVirtualFBServer userServer = null;
    private JSONTools jsonTools = new JSONTools();
    private RequestServiceImpl requestService = ContextManager.getRequestService();

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
        String clientID = session.getRequestParameterMap().get("clientID").get(0);
        if (clientID == null) {
            clientID = "chinabridge";
        }
        UserVirtualMgr userVirtualMgr = ContextManager.getUserMgr();
        userServer = (UserVirtualFBServer)userVirtualMgr.createUserVirtual(clientID);
        if (userServer == null) {
            logger.error("FBServerWebsocketEndpoint:onOpen - Failed to create user - unknown login="+clientID);
            throw new IOException("Failed to create user - unknown login="+clientID);
        }
        userServer.setWebSocket(this);
        if (logger.isDebugEnabled()) {
            logger.debug("FBServerWebsocketEndpoint:onOpen - Create websocket for user={"+userServer+"} - clientID="+clientID);
        }
        userVirtualMgr.addUserFBServer(userServer);
    }

    @OnClose
    public void onClose(Session session, CloseReason status) {
        tsLastActivity = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("WebSocket - onClose for user="+userServer+" - status="+status);
        }
        userServer.removeWebSocket(this);
    }

    @OnMessage
    public void onMessage(String message) throws IOException {
        tsLastActivity = System.currentTimeMillis();
        if (session == null) {
            logger.error("WebSocket - session is null => throw IOException");
            throw new IOException("Session is null");
        }
        if (userServer == null) {
            logger.debug("WebSocket - user is null => throw IOException");
            throw new IOException("User is null");
        }
//        if (logger.isInfoEnabled()) {
//            logger.info("WebSocket - onTextMessage - text received for user="+userServer+" - msg="+message);
//        }
        userServer.setDateLastActivity(System.currentTimeMillis());
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
                    FBServerWSCommand command = jsonTools.mapData(message, FBServerWSCommand.class);
                    if (logger.isDebugEnabled()) {
                        logger.debug("WebSocket - onTextMessage - user="+userServer+" - command="+command);
                    }
                    if (command.command != null && command.command.equals("getResult")) {
                        userServer.incrementNbRequestWebSocket();
                        RequestService.GetResultParam param = command.parseCommandGetResult();
                        String result = requestService.processGetResult(param, userServer);
                        if (!result.equals("ASYNC") && result.length() > 0) {
                            List<String> listAsyncID = new ArrayList<>();
                            listAsyncID.add(param.asyncID);
                            sendResult(result, listAsyncID);
                        }
                    } else {
                        logger.error("Command not supported ! - command="+command+" - msg="+message);
                    }
                } catch (JsonGenerationException e) {
                    logger.error("WebSocket - JsonGenerationException - e="+e.getMessage()+" - user="+userServer, e);
                } catch (JsonMappingException e) {
                    logger.error("WebSocket - JsonMappingException - e="+e.getMessage()+" - user="+userServer, e);
                } catch (IOException e) {
                    logger.error("WebSocket - IOException - e="+e.getMessage()+" - user="+userServer, e);
                } catch (Exception e) {
                    logger.error("WebSocket - Exception - e="+e.getMessage()+" - user="+userServer, e);
                }
            }
        }
    }

    public boolean sendResult(String result, List<String> listAsyncID) {
        FBServerWSResult wsResult = new FBServerWSResult();
        wsResult.command = "setResult";
        wsResult.data.put("result", result);
        wsResult.data.put("listAsyncID", listAsyncID);

        try {
            if (session == null) {
                throw new Exception("Session is null !");
            }
            String data = jsonTools.transform2String(wsResult, false);
            if (data != null && data.length() > 0) {
                boolean sendTextResult = false;
                synchronized (session) {
                    if (session.getBasicRemote() != null) {
                        session.getBasicRemote().sendText(data);
                        sendTextResult = true;
                    }
                }
                return sendTextResult;
            }
        } catch (JsonGenerationException e) {
            logger.error("WebSocket - sendResult - JsonGenerationException - wsResult=" + wsResult, e);
        } catch (JsonMappingException e) {
            logger.error("WebSocket - sendResult - JsonMappingException - wsResult=" + wsResult, e);
        } catch (IOException e) {
            logger.error("WebSocket - sendResult - IOException - wsResult=" + wsResult, e);
        } catch (Exception e) {
            logger.error("WebSocket - sendResult - Exception - wsResult=" + wsResult, e);
        }
        return false;
    }
}
