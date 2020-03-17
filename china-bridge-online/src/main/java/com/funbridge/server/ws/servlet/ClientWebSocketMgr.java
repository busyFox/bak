package com.funbridge.server.ws.servlet;

import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.presence.PresenceMgr;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(value="clientWebSocketMgr")
@Scope(value="singleton")
public class ClientWebSocketMgr extends FunbridgeMgr {
	private ConcurrentHashMap<String, ClientWebSocketEndpoint> mapWebSocket = new ConcurrentHashMap<String, ClientWebSocketEndpoint>();
	@Resource(name="presenceMgr")
	private PresenceMgr presenceMgr = null;
	@Override
	public void startUp() {
	}

	@PostConstruct
	@Override
	public void init() {
		log.debug("Init");
	}

	@PreDestroy
	@Override
	public void destroy() {
		log.debug("destroy");
	}
	
	public Map<String, ClientWebSocketEndpoint> getMapWebSocket() {
		return mapWebSocket;
	}
	
	public void removeWebSocketForSession(String sessionID, boolean closeWebsocket) {
		if (sessionID != null) {
			ClientWebSocketEndpoint ws = mapWebSocket.remove(sessionID);
			if (ws != null && closeWebsocket) {
				ws.resetSession();
			}
		} else {
			log.error("key sessionID is null !");
		}
	}

	public void addWebSocketForSession(ClientWebSocketEndpoint ws, String sessionID) {
		if (sessionID != null) {
//			removeWebSocketForSession(sessionID);
			mapWebSocket.put(sessionID, ws);
		} else {
			log.error("key sessionID is null !");
		}
	}
}
