package com.gotogames.bridge.engineserver.common;

import com.gotogames.bridge.engineserver.cache.RedisCache;
import com.gotogames.bridge.engineserver.cache.TreeMgr;
import com.gotogames.bridge.engineserver.request.QueueMgr;
import com.gotogames.bridge.engineserver.session.EngineSessionMgr;
import com.gotogames.bridge.engineserver.user.UserVirtualMgr;
import com.gotogames.bridge.engineserver.ws.compute.ComputeServiceImpl;
import com.gotogames.bridge.engineserver.ws.request.RequestServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class ContextManager implements ApplicationContextAware {
	private static ApplicationContext context = null;
	private Logger log = LogManager.getLogger(this.getClass());
	
	@PostConstruct
	public void init() {
		log.debug("init");
	}
	
	@PreDestroy
	public void destroy() {
		log.info("End destroy");
	}
	public static ApplicationContext getContext() {
		return context;
	}
	
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		context = ctx;
	}
	
	public static EngineSessionMgr getSessionMgr() {
		return (EngineSessionMgr) context.getBean("sessionMgr");
	}
	
	public static QueueMgr getQueueMgr() {
		return (QueueMgr) context.getBean("queueMgr");
	}
	
	public static LockMgr getLockMgr() {
		return (LockMgr) context.getBean("lockMgr");
	}
	
	public static TreeMgr getTreeMgr() {
		return (TreeMgr) context.getBean("treeMgr");
	}
	public static UserVirtualMgr getUserMgr() {
		return (UserVirtualMgr) context.getBean("userVirtualMgr");
	}
	public static RequestServiceImpl getRequestService() {
		return (RequestServiceImpl) context.getBean("requestServiceImpl");
	}
	public static ComputeServiceImpl getComputeService() {
		return (ComputeServiceImpl) context.getBean("computeServiceImpl");
	}
    public static AlertMgr getAlertMgr() {
        return (AlertMgr)context.getBean("alertMgr");
    }
    public static LogStatMgr getLogStatMgr() {
        return (LogStatMgr)context.getBean("logStatMgr");
    }
    public static RedisCache getRedisCache() { return (RedisCache)context.getBean("redisCache");}
}
