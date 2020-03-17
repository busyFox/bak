package com.funbridge.server.engine;

public class BridgeEngineResult {
	private String content;
	
	public BridgeEngineResult(String content) {
		this.content = content;
	}
	
	public String getContent() {
		return content;
	}
	public boolean isError() {
		return ((content==null) || content.isEmpty());
	}

    public String toString() {
        return content;
    }
}
