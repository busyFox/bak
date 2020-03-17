<%@page import="java.util.concurrent.ThreadPoolExecutor"
		import="com.funbridge.server.common.ContextManager"%><%
ThreadPoolExecutor tpeDuel = (ThreadPoolExecutor)ContextManager.getDuelMgr().getGameMgr().getThreadPoolPlay();
ThreadPoolExecutor tpeTZ = (ThreadPoolExecutor)(ContextManager.getTimezoneMgr().getGameMgr().getThreadPoolPlay());
ThreadPoolExecutor tpeTraining = (ThreadPoolExecutor)(ContextManager.getTrainingMgr().getGameMgr().getThreadPoolPlay());
int threadGameActiveCount = tpeDuel.getActiveCount() + tpeTZ.getActiveCount() + tpeTraining.getActiveCount();
int threadGamePoolSize = tpeDuel.getPoolSize() + tpeTZ.getPoolSize() + tpeTraining.getPoolSize();
int threadGameMaxPoolSize = tpeDuel.getMaximumPoolSize() + tpeTZ.getMaximumPoolSize() + tpeTraining.getMaximumPoolSize();
int threadGameLargestPoolSize = tpeDuel.getLargestPoolSize() + tpeTZ.getLargestPoolSize() + tpeTraining.getLargestPoolSize();
int threadGameCorePoolSize = tpeDuel.getCorePoolSize() + tpeTZ.getCorePoolSize() + tpeTraining.getCorePoolSize();
int threadGameQueueSize = tpeDuel.getQueue().size() + tpeTZ.getQueue().size() + tpeTraining.getQueue().size();
%><%=threadGameActiveCount%>;<%=threadGameQueueSize%>;<%=threadGamePoolSize%>;<%=threadGameMaxPoolSize%>;<%=threadGameLargestPoolSize%>;<%=threadGameCorePoolSize%>