<%@page import="java.util.List"
        import="java.net.Authenticator"
        import="java.net.PasswordAuthentication"
        import="com.gotogames.common.tools.NetTools"%><%
Authenticator.setDefault(new Authenticator() {
    protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication("tomcat", "ToM444".toCharArray());
    }
});
//memory status
long memoryUsed=0, memoryMax=0;
String memoryStatus = "";
List<String> listStatusMemory= NetTools.getURLContentAllList("http://localhost:8080/manager/jmxproxy?get=java.lang:type=Memory&att=HeapMemoryUsage", 10*1000);
for (String e : listStatusMemory) {
	if (e.contains("contents=")) {
    	memoryStatus += e.substring(e.indexOf('{')+1, e.indexOf('}'));
        break;
    }
}
String[] temp = memoryStatus.split(",");
for (int i = 0; i < temp.length; i++) {
	temp[i] = temp[i].trim();
	if (temp[i].startsWith("used")) {
		memoryUsed=Long.parseLong(temp[i].substring("used=".length()))/(1024*1024);
	} else if (temp[i].startsWith("max")) {
		memoryMax=Long.parseLong(temp[i].substring("max=".length()))/(1024*1024);
	}
}
//status thread
int maxThreads=0, currentThreadsBusy=0, currentThreadCount=0, connectionCount = 0;
boolean running=false, paused=false;
List<String> listStatusThread= NetTools.getURLContentAllList("http://localhost:8080/manager/jmxproxy?qry=Catalina:type=ThreadPool,*", 10*1000);
for (String e : listStatusThread) {
	if (e.startsWith("maxThreads:")) {
		maxThreads = Integer.parseInt(e.substring("maxThreads:".length()).trim());
	}
	else if (e.startsWith("currentThreadsBusy:")) {
		currentThreadsBusy = Integer.parseInt(e.substring("currentThreadsBusy:".length()).trim());
	}
	else if (e.startsWith("currentThreadCount:")) {
		currentThreadCount = Integer.parseInt(e.substring("currentThreadCount:".length()).trim());
	}
	else if (e.startsWith("running:")) {
		running = Boolean.parseBoolean(e.substring("running:".length()).trim());
	}
	else if (e.startsWith("paused:")) {
		paused = Boolean.parseBoolean(e.substring("paused:".length()).trim());
	}
	else if (e.startsWith("connectionCount:")) {
		connectionCount = Integer.parseInt(e.substring("connectionCount:".length()).trim());
	}
}
%>connectionCount=<%=connectionCount%>, currentThreadsBusy=<%=currentThreadsBusy %>, currentThreadCount=<%=currentThreadCount %>, running=<%=running %>, paused=<%=paused %>, memoryUsed=<%=memoryUsed %>(Mo), memoryMax=<%=memoryMax %>(Mo), memoryPercent=<%=(memoryMax>0?(100*memoryUsed)/memoryMax:0)%>%
