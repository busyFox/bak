<%@ page import="com.gotogames.bridge.engineserver.common.ContextManager" %>
<%@ page import="com.gotogames.bridge.engineserver.user.UserVirtual" %>
<%@ page import="com.gotogames.bridge.engineserver.user.UserVirtualEngine" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String login = request.getParameter("login");
    String result = "";
    if (login != null) {
        UserVirtual user = ContextManager.getUserMgr().getUserVirtualByLogin(login);
        if (user != null && user instanceof UserVirtualEngine) {
            UserVirtualEngine userVirtualEngine = (UserVirtualEngine)user;
            if (userVirtualEngine.getDateLastStat() > 0) {
                // last stat is recent
                if ((userVirtualEngine.getDateLastStat() + (20 * 60 * 1000)) > System.currentTimeMillis()) {
                    // last result must be recent
                    if ((userVirtualEngine.getDateLastResult() + (20 * 60 * 1000)) > System.currentTimeMillis()) {
                        result = "OK";
                    } else {
                        // ... only if data are waiting !!
                        if (ContextManager.getQueueMgr().getNbWaitingForEngine(0, 60*1000) > 0) {
                            result = "FAILED";
                        } else {
                            result = "OK";
                        }
                    }
                } else {
                    result = "FAILED";
                }
            } else {
                result = "OK";
            }
        } else {
            result = "FAILED";
        }
    }
%>
<%=result%>