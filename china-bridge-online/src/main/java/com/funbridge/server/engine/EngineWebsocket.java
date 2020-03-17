package com.funbridge.server.engine;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.ws.engine.EngineService;
import com.funbridge.server.ws.engine.SetResultParam;
import com.gotogames.common.tools.JSONTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by pserent on 13/03/2017.
 */
@ClientEndpoint
public class EngineWebsocket {
    private WebSocketContainer wsContainer = null;
    private Logger log = LogManager.getLogger(this.getClass());
    private String websocketURI = null;
    private Session websocketSession;
    private String name;
    private JSONTools jsonTools = new JSONTools();
    private EngineService engineService = null; // use to set result
    private AtomicLong nbRequestGetResult = new AtomicLong(0);
    private AtomicLong nbReponseSetResult = new AtomicLong(0);

    public EngineWebsocket(String name) {
        this.name = name;
    }

    public String toString() {
        return "name="+name+" - nbRequestGetResult="+nbRequestGetResult.get()+" - nbReponseSetResult="+nbReponseSetResult.get();
    }

    public AtomicLong getNbRequestGetResult() {
        return nbRequestGetResult;
    }

    public AtomicLong getNbReponseSetResult() {
        return nbReponseSetResult;
    }

    public EngineService getEngineService() {
        if (engineService == null) {
            engineService = ContextManager.getEngineService();
        }
        return engineService;
    }

    public void init() {
        loadConfig();
    }

    public void loadConfig() {
        websocketURI = FBConfiguration.getInstance().getStringValue("enginerest.serviceWebsocket", null);
    }

    public void openWebsocket() {
        if (websocketURI == null || websocketURI.length() == 0) {
            loadConfig();
        }
        if (websocketURI != null && websocketURI.length() > 0) {
            try {
                String clientID = FBConfiguration.getInstance().getStringValue("enginerest.serviceWebsocketClientID", "fbserver");
                wsContainer = ContainerProvider.getWebSocketContainer();
                String uri = websocketURI+"?clientID="+clientID+"-"+name;
                log.warn("Try to open websocket to uri="+uri);
                websocketSession = wsContainer.connectToServer(this, URI.create(uri));
            } catch (Exception e) {
                log.error("Failed to open websocket", e);
            }
        } else {
            log.error("Websocket URI is null or empty !");
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        log.warn("Open websocket name="+name+" - param session="+session+" - websocketSession="+websocketSession);
    }

    @OnClose
    public void onClose() {
        log.warn("On close - websocketSession="+websocketSession);
        this.websocketSession = null;
        this.wsContainer = null;
    }

    @OnMessage
    public void onMessage(String msg) {
        if (log.isDebugEnabled()) {
            log.debug("onMessage - name=" + name + " - msg=" + msg);
        }
        try {
            EngineWebsocketCommand command = jsonTools.mapData(msg, EngineWebsocketCommand.class);
            if (command.command != null && command.command.equals("setResult") && command.data != null) {
                nbReponseSetResult.incrementAndGet();
                SetResultParam param = command.parseCommandSetResult();
                getEngineService().setResult(param);
            } else {
                log.error("Command not supported ! - command="+command+" - msg="+msg);
            }
        } catch (JsonGenerationException e) {
            log.error("onMessage - msg="+msg+" - JsonGenerationException - e="+e.getMessage()+" - name="+name, e);
        } catch (JsonMappingException e) {
            log.error("onMessage - msg="+msg+" - JsonMappingException - e="+e.getMessage()+" - name="+name, e);
        } catch (IOException e) {
            log.error("onMessage - msg="+msg+" - IOException - e="+e.getMessage()+" - name="+name, e);
        } catch (Exception e) {
            log.error("onMessage - msg="+msg+" - Exception - e="+e.getMessage()+" - name="+name, e);
        }
    }

    public void sendPing() {
        if (websocketSession != null) {
            try {
                websocketSession.getBasicRemote().sendText("GOTO");
            } catch (Exception e) {
                log.error("Exception to send text GOTO - session="+websocketSession, e);
            }
        }
    }

    public boolean sendCommandGetResult(BridgeEngineParam param, int requestType) {
        if (websocketSession == null || !websocketSession.isOpen()) {
            openWebsocket();
        }
        if (websocketSession != null && websocketSession.isOpen()) {
            EngineWebsocketCommand command = new EngineWebsocketCommand();
            command.command = "getResult";
            command.data.put("conventions", param.getConventions());
            command.data.put("deal", param.getDealerStr() + param.getVulStr() + param.getDistrib());
            command.data.put("game", param.getBidList() + param.getCardList());
            command.data.put("options", param.getOptions());
            command.data.put("requestType", requestType);
            command.data.put("useCache", param.isUseCache());
            command.data.put("asyncID", param.getAsyncID());
            try {
                String data = jsonTools.transform2String(command, false);
                if (data != null && data.length() > 0 && websocketSession != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Send text : " + data);
                    }
                    websocketSession.getBasicRemote().sendText(data);
                    nbRequestGetResult.incrementAndGet();
                    return true;
                } else {
                    log.error("Data not valid - data=" + data + " - websocketSession=" + websocketSession);
                }
            } catch (JsonGenerationException e) {
                log.error("WebSocket - sendResult - JsonGenerationException - command=" + command, e);
            } catch (JsonMappingException e) {
                log.error("WebSocket - sendResult - JsonMappingException - command=" + command, e);
            } catch (IOException e) {
                log.error("WebSocket - sendResult - IOException - command=" + command, e);
            } catch (Exception e) {
                log.error("WebSocket - sendResult - Exception - command=" + command, e);
            }
        } else {
            log.error("websocket null or not open ! - websocketSession="+websocketSession);
        }
        return false;
    }
}
