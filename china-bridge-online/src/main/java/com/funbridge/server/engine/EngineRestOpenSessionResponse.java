package com.funbridge.server.engine;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="openSessionResponse")
public class EngineRestOpenSessionResponse {
	public String challenge;
}
