package com.funbridge.server.engine;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="checkChallengeResponse")
public class EngineRestCheckChallengeResponse {
	public String sessionID;
}
