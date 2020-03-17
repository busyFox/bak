package com.funbridge.server.engine;

public class ArgineProfile {
	public int id;
	public String name;
	public String value;
	
	public boolean isFree() {
		return value == null || value.length() == 0;
	}
}
