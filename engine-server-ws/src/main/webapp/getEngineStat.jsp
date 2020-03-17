<%@ page import="com.gotogames.bridge.engineserver.common.ContextManager" %>
<%@ page import="com.gotogames.bridge.engineserver.user.UserVirtual" %>
<%@ page import="com.gotogames.bridge.engineserver.user.UserVirtualEngine" %>
<%@ page import="com.codahale.metrics.Meter" %>
<%@ page import="com.gotogames.common.tools.NumericalTools" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String login = request.getParameter("login");
    String type = request.getParameter("type");
    boolean userFound=false;
    String result = "";
    if (login != null) {
        UserVirtual user = ContextManager.getUserMgr().getUserVirtualByLogin(login);
        if (user != null && user instanceof UserVirtualEngine) {
            UserVirtualEngine userEngine = (UserVirtualEngine)user;
            userFound = true;
            if (type.equals("REQUEST")) {
                Meter meter = userEngine.getMetricsRequest();
                if (meter != null) {
                    result = ""+NumericalTools.round(meter.getOneMinuteRate(),2);
                }
            }
            else if (type.equals("QUEUE")) {
                result = ""+userEngine.getQueueSize();
            }
            else if (type.equals("THREAD")) {
                result = ""+userEngine.getNbThread();
            }
            else if (type.equals("THREAD_MAX")) {
                result = ""+userEngine.getMaxThread();
            }
            else if (type.equals("THREAD_PERCENT")) {
                result = ""+userEngine.getAvailableThreadPercent();
            }
            else if (type.equals("TIME") && userEngine.getEngineStat() != null) {
                result = ""+userEngine.getEngineStat().averageTimeRequest;
            }
            else if (type.equals("COUNT")) {
                result = ""+userEngine.getNbCompute();
            }
            else if (type.equals("INPROGRESS")) {
                result = ""+userEngine.getNbRequestsInProgress();
            }
            else if (type.equals("VERSION")) {
                result = ""+userEngine.getVersion();
            }
            else if (type.equals("NEED_RESTART")) {
                result = ""+userEngine.needToRestart();
            }
            else if (type.equals("NB_MINUTES_BEFORE_RESTART")) {
                result = ""+userEngine.computeNbMinutesBeforeRestart();
            }
            else if (type.equals("PERCENT_USE")) {
                int nbMax = userEngine.getMaxThread();
                if (nbMax > 0) {
                    double value = (double)(nbMax - userEngine.getNbThread()) / nbMax;
                    result = "" + value;
                } else {
                    result = "1";
                }
            }
            else if (type.equals("PERF") && userEngine.getEngineStat() != null) {
                result = ""+NumericalTools.round(100*userEngine.computePerformanceIndex(), 6);
//                int nbMax = userEngine.getMaxThread();
//                double avg = userEngine.getEngineStat().averageTimeRequest;
//                if (nbMax > 0 && avg > 0) {
//                    double value = (double)(nbMax - userEngine.getNbThread()) / nbMax;
//                    result = "" + NumericalTools.round(100*value/avg, 6);
//                } else {
//                    result = "1";
//                }
            }
            else if (type.equals("WAITING_RESULT")) {
                result = ""+ContextManager.getQueueMgr().getNbWaitingForEngine(userEngine.getID(), 30*1000);
            }
        }
    }
%>
<%=userFound?"1":"0"%>;<%=result%>