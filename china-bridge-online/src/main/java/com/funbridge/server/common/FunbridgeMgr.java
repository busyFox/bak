package com.funbridge.server.common;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class FunbridgeMgr {
    protected Logger log = LogManager.getLogger(this.getClass());
	
	public abstract void init();
	
	public abstract void destroy();
	
	public abstract void startUp();
	
	public Logger getLogger() {
		return log;
	}
	
	/**
	 * Stop scheduler.
	 * ???????
	 *
	 * @param scheduler
	 */
	protected void stopScheduler(ScheduledExecutorService scheduler) {
		if (scheduler != null) {
			scheduler.shutdown();
			try {
				if (scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				scheduler.shutdownNow();
			}
		}
	}
}
