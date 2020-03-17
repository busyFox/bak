<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="adminFunction.jsp" %>
<%
  long timeToProcess = System.currentTimeMillis();
  Player pla = playerMgr.getPlayer(15);
  List<Long> listPla = playerMgr.getListPlayerIDLinkedToPlayer(15);
  Map<String,List<Long>> mapTS = new HashMap<String, List<Long>>();
  if (listPla != null) {
    for (Long l : listPla) {
      Player p = playerMgr.getPlayer(l);
      if (p != null) {
        List<String> listTS = playerToWSPlayer(p, pla);
        for (String e : listTS) {
          String[] temp = e.split(";");
          if (temp.length == 2) {
            List<Long> list = mapTS.get(temp[0]);
            if (list == null) {
              list = new ArrayList<Long>();
              mapTS.put(temp[0], list);
            }
            list.add(Long.parseLong(temp[1]));
          }
        }

      }
    }
  }
  timeToProcess = System.currentTimeMillis() - timeToProcess;
%>
<html>
  <head>
    <title>TEST</title>
  </head>
    <body>
    Time to process=<%=timeToProcess%> ms<br/>
    Nb player=<%=listPla.size()%><br/>
    MapTS size=<%=mapTS.size()%><br/>
    <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
      <tr><th>Function</th><th>count</th><th>Total</th><th>Average</th></tr>
      <%
        for (Map.Entry<String, List<Long>> entry:mapTS.entrySet()) {
          String funtion = entry.getKey();
          List<Long> listTS = entry.getValue();
          long total = 0;
          for (Long e : listTS) { total+=e;}
      %>
      <tr>
        <td><%=funtion%></td>
        <td><%=listTS.size()%></td>
        <td><%=total%></td>
        <td><%=total/listTS.size()%></td>
      </tr>
      <%}%>
    </table>
  </body>
</html>
