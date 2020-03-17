package com.funbridge.server.engine;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by pserent on 28/04/2017.
 */
public class EngineWebsocketPoolableFactory extends BasePooledObjectFactory<EngineWebsocket> {
    private String category;
    private Logger log = LogManager.getLogger(this.getClass());

    public EngineWebsocketPoolableFactory(String tournamentCategory) {
        this.category = tournamentCategory;
    }

    @Override
    public EngineWebsocket create() throws Exception {
        log.warn("Create websocket for category="+category);
        return new EngineWebsocket(category);
    }

    @Override
    public PooledObject<EngineWebsocket> wrap(EngineWebsocket obj) {
        if (log.isDebugEnabled()) {
            log.debug("wrap - websocket=" + obj);
        }
        return new DefaultPooledObject<>(obj);
    }


    @Override
    public PooledObject<EngineWebsocket> makeObject() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("makeObject");
        }
        return super.makeObject();
    }

    @Override
    public void destroyObject(PooledObject<EngineWebsocket> p) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("destroyObject - websocket=" + p.getObject());
        }
        if (p.getObject() != null) {
            p.getObject().onClose();
        }
        super.destroyObject(p);
    }

    @Override
    public boolean validateObject(PooledObject<EngineWebsocket> p) {
        if (log.isDebugEnabled()) {
            log.debug("validateObject - websocket=" + p.getObject());
        }
        return super.validateObject(p);
    }

    @Override
    public void activateObject(PooledObject<EngineWebsocket> p) throws Exception {
        super.activateObject(p);
    }

    @Override
    public void passivateObject(PooledObject<EngineWebsocket> p) throws Exception {
        super.passivateObject(p);
    }
}
