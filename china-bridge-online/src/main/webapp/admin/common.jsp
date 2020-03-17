<%@ page import="com.funbridge.server.common.Constantes" %><%--
  Created by IntelliJ IDEA.
  User: pserent
  Date: 02/03/2017
  Time: 14:12
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%!
   public String getAdminTourPage(int category) {
       if (category == Constantes.TOURNAMENT_CATEGORY_PRIVATE) {
           return "adminTourPrivate.jsp";
       }
       if (category == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
           return "adminTimezone.jsp";
       }
       if (category == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
           return "adminTraining.jsp";
       }
       return null;
   }
%>
