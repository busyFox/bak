<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.tournament.serie.data.TourSerieGame" %>
<%@ page import="com.funbridge.server.tournament.serie.memory.TourSerieMemTour" %>
<%@ page import="com.funbridge.server.tournament.serie.memory.TourSerieMemTourPlayer" %>
<%@ page import="org.bson.types.ObjectId" %>
<%@ page import="org.springframework.data.mongodb.core.MongoTemplate" %>
<%@ page import="org.springframework.data.mongodb.core.query.Criteria" %>
<%@ page import="org.springframework.data.mongodb.core.query.Query" %>
<%--
  Created by IntelliJ IDEA.
  User: pserent
  Date: 19/02/2015
  Time: 09:03
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%!
    public int countNbGameBDD(String tourID) {
        MongoTemplate mongoTemplate = (MongoTemplate) ContextManager.getContext().getBean("mongoSerieTemplate");
        return (int) mongoTemplate.count(new Query(Criteria.where("tournament.$id").is(new ObjectId(tourID)).andOperator(Criteria.where("finished").is(true))), TourSerieGame.class);
    }

    public int countNbGameMem(TourSerieMemTour tourSerieMemTour) {
        int nbGameMem = 0;
        for (TourSerieMemTourPlayer e : tourSerieMemTour.ranking.values()) {
            nbGameMem += e.getNbDealsPlayed();
        }
        return nbGameMem;
    }
%>