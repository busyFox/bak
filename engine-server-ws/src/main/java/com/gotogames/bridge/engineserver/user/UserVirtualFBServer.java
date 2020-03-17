package com.gotogames.bridge.engineserver.user;

import com.gotogames.bridge.engineserver.common.ContextManager;
import com.gotogames.bridge.engineserver.ws.servlet.FBServerWebsocketEndpoint;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by pserent on 02/11/2016.
 */
public class UserVirtualFBServer extends UserVirtual{
    private String urlFBSetResult = null;
    private FBServerWebsocketEndpoint webSocket = null;
    private AtomicLong nbRequest = new AtomicLong(0);
    private AtomicLong nbRequestWebSocket = new AtomicLong(0);

    public UserVirtualFBServer(String login, String loginPrefix, String password, long id) {
        super(login, loginPrefix, password, id);
    }

    @Override
    public boolean isEngine() {
        return false;
    }

    public String toString() {
        return super.toString()+" - urlFBSetResult="+urlFBSetResult+" nbRequest="+getNbRequest()+" - websocket="+isWebSocketEnable()+" - nbRequestWebSocket="+getNbRequestWebSocket();
    }

    public String getUrlFBSetResult() {
        return urlFBSetResult;
    }

    public void setUrlFBSetResult(String urlFBSetResult) {
        this.urlFBSetResult = urlFBSetResult;
    }

    public FBServerWebsocketEndpoint getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(FBServerWebsocketEndpoint wsMessage) {
        this.webSocket = wsMessage;
    }

    public void removeWebSocket(FBServerWebsocketEndpoint ws) {
        if (webSocket == ws) {
            if (ContextManager.getUserMgr().getLogger().isDebugEnabled()) {
                ContextManager.getUserMgr().getLogger().debug("Remove websocket for user=" + this);
            }
            webSocket = null;
        } else {
            if (ContextManager.getUserMgr().getLogger().isDebugEnabled()) {
                ContextManager.getUserMgr().getLogger().debug("current websocket and param not same => no remove is done for user=" + this);
            }
        }
    }

    public void closeWebSocket() {
        if (webSocket != null) {
            webSocket.closeSession();
        }
    }

    public boolean isWebSocketEnable() {
        return webSocket != null;
    }

    public long incrementNbRequestWebSocket() {
        return this.nbRequestWebSocket.incrementAndGet();
    }

    public long getNbRequestWebSocket() {
        return this.nbRequestWebSocket.get();
    }
    public long incrementNbRequest() {
        return this.nbRequest.incrementAndGet();
    }

    public long getNbRequest() {
        return this.nbRequest.get();
    }
}
