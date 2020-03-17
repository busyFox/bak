<%@page import="java.util.List"
        import="java.net.Authenticator"
        import="java.net.PasswordAuthentication"
        import="com.gotogames.common.tools.NetTools"%><%
Authenticator.setDefault(new Authenticator() {
    protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication("tomcat", "ToM444".toCharArray());
    }
});
//status thread
int currentThreadsBusy=0, currentThreadCount=0;
List<String> listStatusThread= NetTools.getURLContentAllList("http://localhost:8080/manager/jmxproxy?qry=Catalina:type=ThreadPool,*", 10*1000);
for (String e : listStatusThread) {
    if (e.contains("currentThreadsBusy")) {
        currentThreadsBusy = Integer.parseInt(e.substring("currentThreadsBusy:".length()).trim());
    }
    else if (e.contains("currentThreadCount")) {
        currentThreadCount = Integer.parseInt(e.substring("currentThreadCount:".length()).trim());
    }
}
%><%=currentThreadsBusy %>;<%=currentThreadCount %>