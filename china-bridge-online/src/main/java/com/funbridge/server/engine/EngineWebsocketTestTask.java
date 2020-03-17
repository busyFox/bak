package com.funbridge.server.engine;

/**
 * Created by pserent on 03/05/2017.
 */
public class EngineWebsocketTestTask implements Runnable {
    EngineWebsocketMgr websocketMgr;

    public EngineWebsocketTestTask(EngineWebsocketMgr mgr) {
        this.websocketMgr = mgr;
    }
    @Override
    public void run() {
        websocketMgr.testPool();
    }
}
