package com.funbridge.server.engine;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="checkChallengeParam")
public class EngineRestCheckChallengeParam {
	public String login;
	public String challenge;
    public String urlFBSetResult;
}
