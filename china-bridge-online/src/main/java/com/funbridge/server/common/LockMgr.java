package com.funbridge.server.common;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component(value="lockMgr")
@Scope(value="singleton")
public class LockMgr extends FunbridgeMgr {
	private LockString lockDevice = new LockString();
	
	@Override
	public void startUp() {
		
	}

	@Override
	public void init() {
		
	}

	@Override
	public void destroy() {
		lockDevice.destroy();
	}
}
