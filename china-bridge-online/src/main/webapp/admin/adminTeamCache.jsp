<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.team.cache.TeamCache" %>
<%@ page import="com.funbridge.server.team.cache.TeamCacheMgr" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String paramOperation = request.getParameter("operation");
    String resultOperation = "";
    TeamCacheMgr teamCacheMgr = ContextManager.getTeamCacheMgr();
    List<TeamCache> listTeamCache = new ArrayList<TeamCache>();
    boolean showAllTeam = true;
    if (paramOperation != null) {
        if (paramOperation.equals("findTeamByName")) {
            String searchName = request.getParameter("searchName");
            if (searchName != null) {
                resultOperation = "ok - searchName="+searchName;
                long ts = System.currentTimeMillis();
                TeamCache tc = teamCacheMgr.findTeamCacheByName(searchName);
                if (tc != null) {
                    resultOperation += " - result found - ts="+(System.currentTimeMillis() - ts);
                    listTeamCache.add(tc);
                    showAllTeam = false;
                } else {
                    resultOperation += " - No result found - ts="+(System.currentTimeMillis() - ts);
                }
            } else {
                resultOperation = "Error param searchName not found !";
            }
        }
        else if (paramOperation.equals("findTeamByID")) {
            String searchID = request.getParameter("searchID");
            if (searchID != null) {
                resultOperation = "ok - searchID="+searchID;
                long ts = System.currentTimeMillis();
                TeamCache tc = teamCacheMgr.findTeamCacheByID(searchID);
                if (tc != null) {
                    resultOperation += " - result found - ts="+(System.currentTimeMillis() - ts);
                    listTeamCache.add(tc);
                    showAllTeam = false;
                } else {
                    resultOperation += " - No result found - ts="+(System.currentTimeMillis() - ts);
                }
            } else {
                resultOperation = "Error param searchID not found !";
            }
        }
        else if (paramOperation.equals("updateTeamData")) {
            String paramTeamID = request.getParameter("teamID");
            if (paramTeamID != null) {
                TeamCache tc = teamCacheMgr.updateTeamData(paramTeamID);
                if (tc != null) {
                    resultOperation += "Update team cache for teamID="+paramTeamID+" - teamCache="+tc;
                } else {
                    resultOperation += "Failed to update team cache for teamID="+paramTeamID;
                }
            } else {
                resultOperation = "Error param teamID not found !";
            }
        }
        else if (paramOperation.equals("removeTeam")) {
            String paramTeamID = request.getParameter("teamID");
            if (paramTeamID != null) {
                TeamCache tc = teamCacheMgr.removeTeamCache(paramTeamID);
                if (tc != null) {
                    resultOperation += "Remove team cache for teamID="+paramTeamID+" - teamCache="+tc;
                } else {
                    resultOperation += "Failed to remove team cache for teamID="+paramTeamID;
                }
            } else {
                resultOperation = "Error param teamID not found !";
            }
        }
        else if (paramOperation.equals("reloadAllTeamData")) {
            long ts = System.currentTimeMillis();
            teamCacheMgr.updateAllTeamData();
            resultOperation = "Reload ALL team data with success - ts="+(System.currentTimeMillis() - ts);
        }
        else if (paramOperation.equals("removeAllTeamData")) {
            teamCacheMgr.removeAllTeamCache();
            resultOperation = "Remove ALL team data with success";
        }
    }

    long ts = System.currentTimeMillis();
    if (showAllTeam) {
        listTeamCache = new ArrayList<TeamCache>(teamCacheMgr.getCollectionCacheValue());
    }
    int idxStart = 0;
    String paramOffset = request.getParameter("listOffset");
    if (paramOffset != null) {
        idxStart = Integer.parseInt(paramOffset);
        if (idxStart < 0) {
            idxStart = 0;
        }
    }
    int nbMax = 50;
    int idxEnd = idxStart+nbMax;
    if (listTeamCache.size() <= idxEnd) {
        idxEnd = listTeamCache.size();
    }
    listTeamCache = listTeamCache.subList(idxStart, idxEnd);
    long tsListTeamCache = System.currentTimeMillis() - ts;
%>
<html>
    <head>
        <title>Funbridge Server - Administration Team Cache</title>
        <script type="text/javascript">
            function clickFindTeamByName() {
                if (document.forms["formTeamCache"].searchName.value.replace(/^\s+/g,'').replace(/\s+$/g,'') == '') {
                    alert("Name is not valid !");
                } else {
                    document.forms["formTeamCache"].operation.value = "findTeamByName";
                    document.forms["formTeamCache"].submit();
                }
            }
            function clickFindTeamByID() {
                if (document.forms["formTeamCache"].searchID.value.replace(/^\s+/g,'').replace(/\s+$/g,'') == '') {
                    alert("Team ID is not valid !");
                } else {
                    document.forms["formTeamCache"].operation.value = "findTeamByID";
                    document.forms["formTeamCache"].submit();
                }
            }
            function clickUpdateTeamData(teamID) {
                if (confirm("Update data for team with ID="+teamID)) {
                    document.forms["formTeamCache"].operation.value = "updateTeamData";
                    document.forms["formTeamCache"].teamID.value = teamID;
                    document.forms["formTeamCache"].submit();
                }
            }
            function clickRemoveTeamFromCache(teamID) {
                if (confirm("Remove team with ID="+teamID)) {
                    document.forms["formTeamCache"].operation.value = "removeTeam";
                    document.forms["formTeamCache"].teamID.value = teamID;
                    document.forms["formTeamCache"].submit();
                }
            }
            function clickListOffset(offset) {
                document.forms["formTeamCache"].listOffset.value = offset;
                document.forms["formTeamCache"].submit();
            }
            function clickReloadAllTeamData() {
                if (confirm("Reload ALL team data ?")) {
                    document.forms["formTeamCache"].operation.value = "reloadAllTeamData";
                    document.forms["formTeamCache"].submit();
                }
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION TEAM CACHE</h1>
        <a href="admin.jsp">Administration</a><br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= paramOperation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="90%"/>
        <%} %>
        <form name="formTeamCache" method="post" action="adminTeamCache.jsp">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="teamID" value=""/>
            <input type="hidden" name="listOffset" value="<%=idxStart%>"/>
            Cache size (nb elements) = <%=teamCacheMgr.getCacheSize()%> - Bytes size = <%=teamCacheMgr.computeByteSizeMapTeam()%>
            <br/>
            <hr width="95%"/>
            <b>Find player</b>
            <br/>
            Name : <input type="text" name="searchName">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Find Team by name" onclick="clickFindTeamByName()"/><br/>
            Team ID : <input type="text" name="searchID">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="button" value="Find Team by ID" onclick="clickFindTeamByID()"/><br/>
            <hr width="95%"/>
            <b>Reload cache data</b>
            <br/>
            <input type="button" value="Reload ALL team data" onclick="clickReloadAllTeamData()"/><br/>
            <hr width="95%"/>
            <b>List Team</b> - Time to process list : <%=tsListTeamCache%>
            <br/>
            <%if(idxStart > 0) {%>
            <input type="button" value="Previous" onclick="clickListOffset(<%=idxStart - nbMax%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (listTeamCache.size() >= nbMax) {%>
            <input type="button" value="Next" onclick="clickListOffset(<%=idxStart + nbMax%>)">
            <%}%>
            <br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr>
                    <th>ID</th><th>Name</th><th>CountryCode</th><th>Division</th><th>Nb Players</th><th>Operations</th>
                </tr>
                <%for (TeamCache tc : listTeamCache) {%>
                <tr>
                    <td><%=tc.ID %></td>
                    <td><%=tc.name %></td>
                    <td><%=tc.countryCode %></td>
                    <td><%=tc.division %></td>
                    <td><%=tc.getNbPlayers() %></td>
                    <td>
                        <input type="button" onclick="clickUpdateTeamData('<%=tc.ID %>')" value="Update data"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                        <input type="button" onclick="clickRemoveTeamFromCache('<%=tc.ID %>')" value="Remove From cache"/>
                    </td>
                </tr>
                <%} %>
            </table>
        </form>
    </body>
</html>
