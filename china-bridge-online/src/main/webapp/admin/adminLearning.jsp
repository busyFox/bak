<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.tournament.learning.TourLearningMgr" %>
<%@ page import="com.funbridge.server.tournament.learning.data.LearningCommentsDeal" %>
<%@ page import="com.funbridge.server.tournament.learning.data.LearningTournament" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setCharacterEncoding("UTF-8");
    String operation = request.getParameter("operation");
    String resultOperation = "";
    TourLearningMgr learningMgr = ContextManager.getTourLearningMgr();
    String chapter = null;
    String tourID = null;
    int idxStart = 0;
    int nbMax = 20;
    String paramOffset = request.getParameter("offset");
    if (paramOffset != null) {
        idxStart = Integer.parseInt(paramOffset);
        if (idxStart < 0) {
            idxStart = 0;
        }
    }
    if (operation != null && operation.length() > 0) {
        if (operation.equals("import")) {
            String importPath = request.getParameter("importPath");
            if (importPath != null && importPath.length() > 0) {
                try {
                    TourLearningMgr.ImportDealResult importResult = learningMgr.importDealFromPath(importPath);
                    resultOperation = "Result of import deals from path ("+importPath+") : "+importResult;
                    if (importResult.filesError.size() > 0) {
                        resultOperation += "<br>File with error :";
                        for (String e : importResult.filesError) {
                            resultOperation+="<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+e;
                        }
                    }
                } catch (Exception e) {
                    resultOperation = "Exception to import deals path="+importPath+" - exception="+e.getMessage();
                    learningMgr.getLogger().error("Exception to import deals path="+importPath, e);
                }
            } else {
                resultOperation = "Param not valid importPath="+importPath;
            }
        }
        else if (operation.equals("createTournament")) {
            chapter = request.getParameter("chapter");
            if (chapter != null && chapter.length() > 0) {
                TourLearningMgr.CreateTournamentResult result = learningMgr.createTournamentForChapter(chapter);
                if (result.error != null && result.error.length() > 0) {
                    resultOperation = "Failed to create tournament for chapterID="+chapter+" - error="+result.error;
                } else {
                    resultOperation = "Success to create tournament for chapterID="+chapter+" - tour="+result.tournament;
                }
            } else {
                resultOperation = "No chapterID selected to create tournament";
            }
        }
    }

    List<LearningCommentsDeal> listImportDeals = learningMgr.listLearningCommentsDeal(idxStart, nbMax);
    int countImportDeals = (int) learningMgr.countLearningCommentsDeal();
    List<LearningTournament> listTour = learningMgr.listAllTournaments(0,50);
%>
<html>
    <head>
        <title>Funbridge Server - Administration</title>
        <script type="text/javascript">
            function clickProcessImport() {
                document.forms["formTourComments"].operation.value="import";
                document.forms["formTourComments"].submit();
            }
            function clickCreateTournament(chapter) {
                if (confirm("Create tournament for chapter "+ chapter)) {
                    document.forms["formTourComments"].chapter.value = chapter;
                    document.forms["formTourComments"].operation.value = "createTournament";
                    document.forms["formTourComments"].submit();
                }
            }
            function clickListImportDealsOffset(offset) {
                document.forms["formTourComments"].offset.value = offset;
                document.forms["formTourComments"].operation.value="listImportDeals";
                document.forms["formTourComments"].submit();
            }
            function clickShowImportDealDetails(e) {
                var subRow = e.parentNode.parentNode.nextElementSibling;
                if (subRow.style.display === 'none') {
                    subRow.style.display = 'table-row';
                    e.value = 'Hide details';
                } else {
                    subRow.style.display = 'none';
                    e.value = 'Show details';
                }
            }
        </script>
    </head>
    <body>
        <h1>ADMINISTRATION TOURNAMENT LEARNING</h1>
        [<a href="admin.jsp">Administration</a>]
        <br/><br/>
        <%if (resultOperation.length() > 0) {%>
        <b>RESULTAT OPERATION : <%= operation%></b>
        <br/>Result = <%=resultOperation %>
        <hr width="95%"/>
        <%} %>
        <form name="formTourComments" method="post" action="adminLearning.jsp">
            <input type="hidden" name="operation" value=""/>
            <input type="hidden" name="tourID" value=""/>
            <input type="hidden" name="sizeListImportDeals" value="<%=listImportDeals!=null?listImportDeals.size():0 %>"/>
            <input type="hidden" name="selection" value=""/>
            <input type="hidden" name="chapter" value=""/>
            <input type="hidden" name="offset" value="<%=idxStart%>"/>
            <br/>

            <%--****************--%>
            <%--COMMENTS DEALS--%>
            <%--****************--%>
            <hr width="95%"/>
            <b>COMMENTS DEALS</b><br/>
            File path for deals to process import : <input type="text" name="importPath" size="40"> &nbsp;&nbsp;&nbsp;<input type="button" value="Process import" onclick="clickProcessImport()"><br/>
            <br/>
            <br/>
            <%if (listImportDeals != null) {if(idxStart > 0) {%>
            <input type="button" value="Previous" onclick="clickListImportDealsOffset(<%=idxStart - nbMax%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            <% if (listImportDeals.size() >= nbMax) {%>
            <input type="button" value="Next" onclick="clickListImportDealsOffset(<%=idxStart + nbMax%>)">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <%}%>
            Offset = <%=idxStart%> - NbMax = <%=nbMax%> - Count = <%=countImportDeals%> - list size=<%=listImportDeals.size()%><br/>
            <%}%>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Id</th><th>Chapter</th><th>File ID / Number</th><th>Deal./Vul</th><th>Distrib</th><th>Contract</th><th>NbTricks</th><th>Begins</th><th>Details</th></tr>
                <%if (listImportDeals != null ) {
                    for (LearningCommentsDeal e : listImportDeals) {
                        String bgcolor = "";
                        String checkData = e.checkData();
                        if (!checkData.equals("OK")) {
                            bgcolor = "#f08080";
                        }
                %>
                <tr <%=bgcolor.length() > 0?"bgcolor="+bgcolor:""%>>
                    <td><%=e.getIDStr()%></td>
                    <td><%=e.chapterID%></td>
                    <td><%=e.importDealFileID%> - <%=e.importDealNumber%></td>
                    <td><%=e.getStrDealer()%>/<%=e.getStrVulnerability()%></td>
                    <td><%=e.distribution%></td>
                    <td><%=e.contract%> <%=e.declarer%></td>
                    <td><%=e.nbTricks%></td>
                    <td><%=e.begins%></td>
                    <td>
                        <input type="button" value="Show details" onclick="clickShowImportDealDetails(this)"><br/>
                        <%if (learningMgr.getFirstTournamentForChapter(e.chapterID) == null) {%>
                        <input type="button" value="Create tournament for chapter" onclick="clickCreateTournament('<%=e.chapterID%>')">
                        <%}%>
                    </td>
                </tr>
                <tr <%=bgcolor.length() > 0?"bgcolor="+bgcolor:""%> style="display: none;">
                    <td colspan="12">
                        <b>Bids</b> : <%=e.bids%>&nbsp;&nbsp;-&nbsp;&nbsp;<b>Begins</b> : <%=e.begins%><br/>
                        <b>CommentsBids</b> : <%=learningMgr.getCommentsBids(e.chapterID, "fr")%><br/>
                        <b>CommentsCards</b> : <%=learningMgr.getCommentsCards(e.chapterID, "fr")%><br/>
                        <b>ImportProcessError</b> : <%=e.importProcessError%><br/>
                        <b>CheckData</b>:<%=checkData%>
                    </td>
                </tr>
                <%}}%>
            </table>

            <%--****************--%>
            <%--LIST TOURNAMENTS--%>
            <%--****************--%>
            <hr width="95%">
            <b>LIST TOURNAMENTS</b><br/>
            <br/>
            <table border="1" style="background-color:white" cellpadding="2" cellspacing="0">
                <tr><th>Chapter</th><th>Result Type</th><th>Name</th></tr>
                <% if (listTour != null) { for (LearningTournament t : listTour) {%>
                <tr>
                    <td><%=t.getChapterID()%></td>
                    <td><%=t.getResultType()%></td>
                    <td><%=t.getName()%></td>
                </tr>
                <%}}%>
            </table>
        </form>
    </body>
</html>
