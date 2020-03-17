<%@ page import="com.gotogames.bridge.engineserver.user.UserVirtual" %>
<%@ page import="com.gotogames.bridge.engineserver.common.ContextManager" %>
<%@ page import="com.gotogames.bridge.engineserver.user.UserVirtualEngine" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String login = request.getParameter("login");
    String result = "";
    if (login != null && login.length() > 0) {
        UserVirtual user = ContextManager.getUserMgr().getUserVirtualByLogin(login);
        if (user != null && user instanceof UserVirtualEngine) {
            UserVirtualEngine userEngine = (UserVirtualEngine) user;
            String command = request.getParameter("command");
            if (command != null && command.length() > 0) {
                if (userEngine.getWebSocket() != null) {
                    if (command.equals("stop")) {
                        result = "Command STOP result="+userEngine.getWebSocket().sendCommandStop()+" for engine="+userEngine;
                    }
                    else if (command.equals("restart")) {
                        result = "Command RESTART result="+userEngine.getWebSocket().sendCommandRestart()+" for engine="+userEngine;
                    }
                    else if (command.equals("reboot")) {
                        result = "Command REBOOT result="+userEngine.getWebSocket().sendCommandReboot()+" for engine="+userEngine;
                    }
                    else if (command.equals("update")) {
                        result = "Command UPDATE result="+userEngine.getWebSocket().sendCommandUpdate()+" -  for engine="+userEngine;
                        } else {
                            result = "No parameter downloadURL found for update command - engine="+userEngine;
                        }
                    }
                    else {
                        result = "Command not supported - command="+command;
                    }
                } else {
                    result = "FAILED - User engine websocket is null !! - engine="+userEngine;
                }
            } else {
                result = "FAILED - COMMAND (command) PARAMETER NOT FOUND";
            }
        } else {
            result = "FAILED - No user engine found with login="+login;
        }
    } else {
        result = "FAILED - LOGIN (login) PARAMETER NOT FOUND";
    }
%>
<%=result%>
