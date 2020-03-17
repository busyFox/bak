package com.funbridge.server.team;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.message.ChatMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.Chatroom;
import com.funbridge.server.message.data.MessageNotif;
import com.funbridge.server.message.data.MessageNotifGroup;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.player.data.PlayerHandicap;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.team.cache.TeamCache;
import com.funbridge.server.team.cache.TeamCacheMgr;
import com.funbridge.server.team.data.Team;
import com.funbridge.server.team.data.TeamListAggregate;
import com.funbridge.server.team.data.TeamPlayer;
import com.funbridge.server.team.data.TeamRequest;
import com.funbridge.server.texts.TextUIData;
import com.funbridge.server.tournament.team.TourTeamMgr;
import com.funbridge.server.tournament.team.data.TeamPeriod;
import com.funbridge.server.tournament.team.memory.TeamMemDivisionResult;
import com.funbridge.server.tournament.team.memory.TeamMemDivisionResultTeam;
import com.funbridge.server.tournament.team.memory.TeamMemTournament;
import com.funbridge.server.tournament.team.memory.TeamMemTournamentPlayer;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.event.Event;
import com.funbridge.server.ws.team.*;
import com.funbridge.server.ws.team.response.ListTeamsResponse;
import com.gotogames.common.tools.NumericalTools;
import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;

/**
 * Created by pserent on 12/10/2016.
 */
@Component(value="teamMgr")
public class TeamMgr extends FunbridgeMgr{
    @Resource(name="mongoTeamTemplate")
    private MongoTemplate mongoTemplate;
    @Resource(name="teamCacheMgr")
    private TeamCacheMgr teamCacheMgr;
    @Resource(name="playerCacheMgr")
    private PlayerCacheMgr playerCacheMgr;
    @Resource(name="playerMgr")
    private PlayerMgr playerMgr;
    @Resource(name="messageNotifMgr")
    private MessageNotifMgr notifMgr;
    @Resource(name="presenceMgr")
    private PresenceMgr presenceMgr;
    @Resource(name="chatMgr")
    private ChatMgr chatMgr;
    @Resource(name="tourTeamMgr")
    private TourTeamMgr tourTeamMgr;

    private int teamSize = 0, nbLeadPlayers = 0;

    public int getTeamSize() {
        if (teamSize == 0) {
            teamSize = getConfigIntValue("teamSize", 6);
        }
        return teamSize;
    }
    public int getNbLeadPlayers() {
        if (nbLeadPlayers == 0) {
            nbLeadPlayers = getConfigIntValue("nbLeadPlayers", 4);
        }
        return nbLeadPlayers;
    }

    @PostConstruct
    @Override
    public void init() {
        teamSize = getConfigIntValue("teamSize", 6);
        nbLeadPlayers = getConfigIntValue("nbLeadPlayers", 4);
    }

    @PreDestroy
    @Override
    public void destroy() {

    }

    @Override
    public void startUp() {

    }

    /**
     * Read string value for parameter in name (team.paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public String getConfigStringValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringValue("team." + paramName, defaultValue);
    }

    public String getStringResolvEnvVariableValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringResolvEnvVariableValue("team." + paramName, defaultValue);
    }

    /**
     * Read int value for parameter in name (team.paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public int getConfigIntValue(String paramName, int defaultValue) {
        return FBConfiguration.getInstance().getIntValue("team." + paramName, defaultValue);
    }

    /**
     * Read double value for parameter in name (team.paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public double getConfigDoubleValue(String paramName, double defaultValue) {
        return FBConfiguration.getInstance().getDoubleValue("team." + paramName, defaultValue);
    }

    public boolean isDevMode() {
        return FBConfiguration.getInstance().getIntValue("general.devMode", 0) == 1;
    }

    /**
     * Create team, set player to captain and if country code is null use country code of player
     * @param session
     * @param name
     * @param countryCode
     * @param description
     * @return
     * @throws FBWSException
     */
    public synchronized Team createTeam(FBSession session, String name, String countryCode, String description) throws FBWSException {
        if (session == null || session.getPlayer() == null) {
            log.error("session not valid ... session="+session);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Player player = session.getPlayer();
        // check player has not yet another team
        if (getTeamForPlayer(player.getID()) != null) {
            log.error("Player is already in a team : player="+player);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_ALREADY_IN_TEAM);
        }
        // check team name
        if (getTeamForName(name) != null) {
            log.error("Team with name already existing : name="+name);
            throw new FBWSException(FBExceptionType.TEAM_NAME_ALREADY_EXISTING);
        }
        Team team = new Team();
        team.setName(name);
        if (countryCode == null || countryCode.length() == 0) {
            team.setCountryCode(player.getDisplayCountryCode());
        } else {
            team.setCountryCode(countryCode);
        }
        // Set fields
        team.setDescription(description);
        team.setDivision(TourTeamMgr.DIVISION_NO);
        team.addPlayer(player.getID(), session.getPlayerCache().handicap);
        team.setDateCreation(System.currentTimeMillis());
        // Create chatroom
        Chatroom chatroom = (Chatroom) chatMgr.createChatroom(team.getName(), null, null, Constantes.CHATROOM_TYPE_TEAM);
        chatroom.addParticipant(player.getID());
        chatMgr.saveChatroom(chatroom);
        team.setChatroomID(chatroom.getIDStr());
        // Insert team in DB
        mongoTemplate.insert(team);
        log.info("New team created - team="+team);
        // Update TeamCache
        teamCacheMgr.updateTeamData(team);
        // Delete all pending requests for this player
        removeAllRequestsForPlayer(player.getID());
        // send notif to captain
        MessageNotif notif = createNotifCreateTeam(team);
        if (notif != null) {
            session.pushEvent(notifMgr.buildEvent(notif, player.getID()));
        }
        return team;
    }

    /**
     * Create team for TEST !
     * @param playerCache
     * @param name
     * @return
     */
    public Team createTeam(PlayerCache playerCache, String name) {
        // check team name
        if (getTeamForName(name) == null && getTeamForPlayer(playerCache.ID) == null) {
            Team team = new Team();
            team.setName(name);
            team.setCountryCode(playerCache.countryCode);
            team.setDivision(TourTeamMgr.DIVISION_NO);
            team.addPlayer(playerCache.ID, playerCache.handicap);
            team.setDateCreation(System.currentTimeMillis());

            // Insert team in DB
            mongoTemplate.insert(team);
            log.info("New team created - team=" + team);
            // Update TeamCache
            teamCacheMgr.updateTeamData(team);
            // Delete all pending requests for this player
            removeAllRequestsForPlayer(playerCache.ID);
            return team;
        }
        return null;
    }

    public List<Team> createTeamsTEST() {
        List<Team> teamsCreated = new ArrayList<>();
        if (isDevMode()) {
            int idxPlayer = 0;
            int idxTeam = 1;
            int nbTeam = getConfigIntValue("test.nbTeam", 10);
            List<Player> playersTest = playerMgr.listPlayersTest();
            while (teamsCreated.size() <= nbTeam && idxPlayer < playersTest.size()) {
                String teamName = "Team_TEST_" + String.format("%03d", idxTeam);
                if (getTeamForName(teamName) == null) {
                    Player pCaptain = playersTest.get(idxPlayer);
                    idxPlayer++;
                    PlayerCache pcCaptain = playerCacheMgr.getPlayerCache(pCaptain.getID());
                    if (pcCaptain != null) {
                        Team team = createTeam(pcCaptain, teamName);
                        if (team != null) {
                            while (!team.isFull()) {
                                if (idxPlayer >= playersTest.size()) {
                                    break;
                                }
                                Player p = playersTest.get(idxPlayer);
                                idxPlayer++;
                                PlayerCache pc = playerCacheMgr.getPlayerCache(p.getID());
                                if (pc != null) {
                                    team.addPlayer(pc.ID, pc.handicap);
                                }
                            }
                            mongoTemplate.save(team);
                            // Update TeamCache
                            teamCacheMgr.updateTeamData(team);
                            teamsCreated.add(team);
                            idxTeam++;
                        }
                    }
                } else {
                    log.error("Team already existed with name="+teamName);
                    idxTeam++;
                }
            }
        }
        return teamsCreated;
    }

    /**
     * Return team where player is member
     * @param playerID
     * @return
     */
    public Team getTeamForPlayer(long playerID) {
        Query q = Query.query(Criteria.where("players.playerID").is(playerID));
        return mongoTemplate.findOne(q, Team.class);
    }

    /**
     * Create WSTeam object associated to this team
     * @param team
     * @param playerID
     * @param nbFriends -1 then compute nb friends for playerID in this team else set value
     * @return
     */
    public WSTeam toWSTeam(Team team, long playerID, int nbFriends) {
        if (team != null) {
            WSTeam wsTeam = new WSTeam();
            wsTeam.ID = team.getIDStr();
            wsTeam.name = team.getName();
            wsTeam.countryCode = team.getCountryCode();
            wsTeam.division = team.getDivision();
            wsTeam.players = new ArrayList<>();
            wsTeam.description = team.getDescription();
            List<Long> listFriendID = null;
            if (nbFriends == -1) {
                listFriendID = playerMgr.listFriendIDForPlayer(playerID);
            } else if (nbFriends >= 0){
                wsTeam.nbFriends = nbFriends;
            }
            for (TeamPlayer e : team.getPlayers()) {
                wsTeam.players.add(toWSTeamPlayer(e));
                if (listFriendID != null) {
                    if (listFriendID.contains(e.getPlayerID())) {
                        wsTeam.nbFriends++;
                    }
                }
            }
            if (!team.getDivision().equals(TourTeamMgr.DIVISION_NO)) {
                TeamMemDivisionResult divisionResult = tourTeamMgr.getMemoryMgr().getMemDivisionResult(team.getDivision());
                if (divisionResult != null) {
                    wsTeam.divisionNbTeam = divisionResult.getNbTeams();
                    TeamMemDivisionResultTeam resultTeam = divisionResult.getTeamResult(team.getIDStr());
                    if (resultTeam != null) {
                        wsTeam.points = resultTeam.points;
                        wsTeam.rank = resultTeam.rank;
                        if (team.getNbPlayers() < TourTeamMgr.GROUP_TAB.length) {
                            wsTeam.trend = -1;
                            wsTeam.trendDivision = TourTeamMgr.DIVISION_NO;
                        } else {
                            wsTeam.trend = resultTeam.trend;
                            wsTeam.trendDivision = TourTeamMgr.computeDivisionEvolution(team.getDivision(), resultTeam.trend);
                        }
                    }
                }
            }
            wsTeam.divisionHistory = tourTeamMgr.getDivisionHistoryForTeam(team);

            return wsTeam;
        }
        return null;
    }

    /**
     * Create WSTeamLight object associated to this team
     * @param team
     * @param playerID
     * @param nbFriends -1 then compute nb friends for playerID in this team else set value
     * @return
     */
    public WSTeamLight toWSTeamLight(Team team, long playerID, int nbFriends) {
        if (team != null) {
            WSTeamLight wsTeam = new WSTeamLight();
            wsTeam.ID = team.getIDStr();
            wsTeam.name = team.getName();
            wsTeam.countryCode = team.getCountryCode();
            wsTeam.division = team.getDivision();
            wsTeam.nbPlayers = team.getNbPlayers();
            if (team.getCaptain() != null) {
                PlayerCache playerCache = playerCacheMgr.getOrLoadPlayerCache(team.getCaptain().getPlayerID());
                if (playerCache != null) {
                    wsTeam.captainPseudo = playerCache.getPseudo();
                }
            }
            List<Long> listFriendID = null;
            if (nbFriends == -1) {
                listFriendID = playerMgr.listFriendIDForPlayer(playerID);
                for (TeamPlayer e : team.getPlayers()) {
                    if (listFriendID != null) {
                        if (listFriendID.contains(e.getPlayerID())) {
                            wsTeam.nbFriends++;
                        }
                    }
                }
            } else if (nbFriends >= 0){
                wsTeam.nbFriends = nbFriends;
            }

            return wsTeam;
        }
        return null;
    }

    /**
     * Create WS object associated to this teamPlayer
     * @param teamPlayer
     * @return
     */
    public WSTeamPlayer toWSTeamPlayer(TeamPlayer teamPlayer) {
        if (teamPlayer != null) {
            return toWSTeamPlayer(teamPlayer.getPlayerID(), teamPlayer.isCaptain(), teamPlayer.isSubstitute(), teamPlayer.getGroup());
        }
        return null;
    }

    public WSTeamPlayer toWSTeamPlayer(long playerID, boolean captain, boolean substitute, String group) {
        PlayerCache playerCache = playerCacheMgr.getPlayerCache(playerID);
        if (playerCache == null) {
            playerCache = playerCacheMgr.getOrLoadPlayerCache(playerID);
        }
        WSTeamPlayer wsTeamPlayer = new WSTeamPlayer();
        wsTeamPlayer.playerID = playerID;
        wsTeamPlayer.captain = captain;
        wsTeamPlayer.substitute = substitute;
        wsTeamPlayer.group = group;
        if (playerCache != null) {
            wsTeamPlayer.avatar = playerCache.avatarPresent;
            wsTeamPlayer.pseudo = playerCache.getPseudo();
            wsTeamPlayer.countryCode = playerCache.countryCode;
            wsTeamPlayer.serie = playerCache.serie;
        }
        PlayerHandicap handicap = playerMgr.getPlayerHandicap(playerID);
        if (handicap != null) {
            wsTeamPlayer.averagePerformance = NumericalTools.round(handicap.getWSAveragePerformanceMP(), 4);
        } else {
            wsTeamPlayer.averagePerformance = 50;
        }
        FBSession session = presenceMgr.getSessionForPlayerID(playerID);
        if(session != null){
            wsTeamPlayer.connected = true;
        }
        return wsTeamPlayer;
    }

    private WSTeamRequest toWSTeamRequest(TeamRequest request) {
        if(request != null){
            WSTeamRequest wsRequest = new WSTeamRequest();
            wsRequest.ID = request.getIDStr();
            wsRequest.date = request.getDate();
            wsRequest.author = request.getAuthor();
            wsRequest.playerID = request.getPlayerID();
            PlayerCache playerCache = playerCacheMgr.getPlayerCache(request.getPlayerID());
            if (playerCache == null) {
                playerCache = playerCacheMgr.getOrLoadPlayerCache(request.getPlayerID());
            }
            wsRequest.playerID = request.getPlayerID();
            if (playerCache != null){
                wsRequest.avatar = playerCache.avatarPresent;
                wsRequest.pseudo = playerCache.getPseudo();
                wsRequest.countryCode = playerCache.countryCode;
            }
            return wsRequest;
        }
        return null;
    }

    private WSPlayerRequest toWSPlayerRequest(TeamRequest request) {
        if(request != null){
            WSPlayerRequest wsRequest = new WSPlayerRequest();
            wsRequest.ID = request.getIDStr();
            wsRequest.date = request.getDate();
            wsRequest.author = request.getAuthor();
            wsRequest.teamID = request.getTeamID();
            Team team = findTeamByID(request.getTeamID());
            if (team != null){
                wsRequest.name = team.getName();
                wsRequest.countryCode = team.getCountryCode();
                wsRequest.nbPlayers = team.getNbPlayers();
                TeamPlayer captain = team.getCaptain();
                if(captain != null){
                    PlayerCache playerCache = playerCacheMgr.getPlayerCache(captain.getPlayerID());
                    if (playerCache == null) {
                        playerCache = playerCacheMgr.getOrLoadPlayerCache(captain.getPlayerID());
                    }
                    if(playerCache != null){
                        wsRequest.captainPseudo = playerCache.getPseudo();
                    }
                }
            }
            return wsRequest;
        }
        return null;
    }

    /**
     * Return team with nam
     * @param name
     * @return
     */
    public Team getTeamForName(String name) {
        return mongoTemplate.findOne(Query.query(Criteria.where("name").regex("^"+name+"$", "i").andOperator(Criteria.where("nbPlayers").gt(0))), Team.class);
    }

    public List<Team> getTeamsForDivision(String division) {
        return mongoTemplate.find(Query.query(Criteria.where("division").is(division)), Team.class);
    }

    public List<Team> getActiveTeamsForDivision(String division, String periodID) {
        return mongoTemplate.find(Query.query(Criteria.where("division").is(division).andOperator(Criteria.where("lastPeriodWithCompleteTeam").is(periodID))), Team.class);
    }

    public List<Team> getInactiveAndIncompleteTeams(TeamPeriod period){
        Criteria cNbLeadPlayers = Criteria.where("nbPlayers").lt(getNbLeadPlayers());
        Criteria cCreationDate = Criteria.where("dateCreation").lt(period.getDateStart());
        Criteria cNeverComplete = Criteria.where("lastPeriodWithCompleteTeam").exists(false);
        Criteria cLastPeriodWithCompleteTeam = Criteria.where("lastPeriodWithCompleteTeam").is(period.getID());
        // NbPlayers < nbLeadPlayers AND (lastPeriodWithCompleteTeam = period OR (neverComplete AND dateCreation < period.startDate))
        Query q = Query.query(cNbLeadPlayers.andOperator(cLastPeriodWithCompleteTeam.orOperator(cNeverComplete.andOperator(cCreationDate))));
        return mongoTemplate.find(q, Team.class);
    }

    public Team findTeamByID(String ID){
        return mongoTemplate.findOne(Query.query(Criteria.where("ID").is(new ObjectId(ID))), Team.class);
    }

    private TeamRequest findRequestByID(String ID){
        return mongoTemplate.findOne(Query.query(Criteria.where("ID").is(new ObjectId(ID))), TeamRequest.class);
    }

    public List<WSTeamRequest> getRequestsForTeam(String teamID){
        List<TeamRequest> requests = mongoTemplate.find(Query.query(Criteria.where("teamID").is(teamID)), TeamRequest.class);
        List<WSTeamRequest> result = new ArrayList<>();
        for(TeamRequest e : requests){
            result.add(toWSTeamRequest(e));
        }
        return result;
    }

    public List<WSPlayerRequest> getRequestsForPlayer(long playerID){
        List<TeamRequest> requests = mongoTemplate.find(Query.query(Criteria.where("playerID").is(playerID)), TeamRequest.class);
        List<WSPlayerRequest> result = new ArrayList<>();
        for(TeamRequest e : requests){
            result.add(toWSPlayerRequest(e));
        }
        return result;
    }

    public TeamRequest getRequestForPlayerAndTeam(long playerID, String teamID){
        return mongoTemplate.findOne(Query.query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("teamID").is(teamID))), TeamRequest.class);
    }

    private void removeAllRequestsForTeam(String teamID){
        mongoTemplate.remove(Query.query(Criteria.where("teamID").is(teamID)), TeamRequest.class);
    }

    private void removeAllRequestsForPlayer(long playerID){
        mongoTemplate.remove(Query.query(Criteria.where("playerID").is(playerID)), TeamRequest.class);
    }

    public boolean answerRequest(String requestID, boolean accept, long playerID) throws FBWSException{
        // Find request
        TeamRequest request = findRequestByID(requestID);
        if(request == null){
            throw new FBWSException(FBExceptionType.TEAM_REQUEST_NOT_FOUND);
        }
        // Find team
        Team team = findTeamByID(request.getTeamID());
        if(team == null){
            throw new FBWSException(FBExceptionType.TEAM_UNKNOWN_TEAM);
        }
        // Check if player can answer it (request sent to the player or to the player's team and he's the captain)
        if((request.getAuthor().equalsIgnoreCase("team") && request.getPlayerID() == playerID) || (request.getAuthor().equalsIgnoreCase("player") && team.isCaptain(playerID))){
            // If answer is yes
            if(accept){
                // Add a player to a team
                team.addPlayer(request.getPlayerID(), playerCacheMgr.getOrLoadPlayerCache(request.getPlayerID()).handicap);
                mongoTemplate.save(team);
                // Update TeamCache
                teamCacheMgr.updateTeamData(team);
                // Add player to the team chatroom
                if(team.getChatroomID() != null && !team.getChatroomID().isEmpty()){
                    Chatroom chatroom = (Chatroom) chatMgr.findChatroomByID(team.getChatroomID());
                    if(chatroom != null){
                        chatroom.addParticipant(request.getPlayerID());
                        chatMgr.saveChatroom(chatroom);
                    }
                }
                // send message to chatroom
                PlayerCache playerCache = playerCacheMgr.getPlayerCache(request.getPlayerID());
                if (playerCache == null) {
                    playerCache = playerCacheMgr.getOrLoadPlayerCache(request.getPlayerID());
                }
                String pseudo = playerCache.getPseudo();
                HashMap<String, String> templateParameters = new HashMap<>();
                templateParameters.put("PSEUDO", pseudo);
                chatMgr.sendSystemMessageToChatroom("chat.teamNewPlayer", templateParameters, team.getChatroomID());

                // send a notif only to the player who made the request
                if (request.getAuthor().equalsIgnoreCase("player")) {
                    MessageNotif notif = createNotifRequestPlayerOK(team, request.getPlayerID());
                    FBSession sessionPlayer = presenceMgr.getSessionForPlayerID(request.getPlayerID());
                    if (sessionPlayer != null && notif != null) {
                        sessionPlayer.pushEvent(notifMgr.buildEvent(notif, request.getPlayerID()));
                    }
                }

                // Delete all remaining requests for that player
                removeAllRequestsForPlayer(request.getPlayerID());
                // If team is now full delete all remaining requests for that team
                if(team.isFull()){
                    removeAllRequestsForTeam(team.getIDStr());
                }

                // If team has now 4 lead players and can play the next period (and couldn't play the current one), send a notif
                if (team.getNbPlayers() == getNbLeadPlayers() && (team.getLastPeriodWithCompleteTeam() == null || tourTeamMgr.getCurrentPeriod() == null || !team.getLastPeriodWithCompleteTeam().equalsIgnoreCase(tourTeamMgr.getCurrentPeriod().getID()))) {
                    // send notif to each player
                    for (TeamPlayer tp : team.getPlayers()) {
                        MessageNotif notifPla = createNotifTeamNowComplete(tp.getPlayerID());
                        FBSession sessionPla = presenceMgr.getSessionForPlayerID(tp.getPlayerID());
                        if (sessionPla != null && notifPla != null) {
                            sessionPla.pushEvent(notifMgr.buildEvent(notifPla, tp.getPlayerID()));
                        }
                    }
                }

                // send team event to players
                for(TeamPlayer tp : team.getPlayers()){
                    sendTeamEventToPlayer(tp.getPlayerID());
                }
            } else {
                // If answer is no just delete the request
                mongoTemplate.remove(request);
            }
        } else {
            // Player has no right to answer this request
            log.error("Player has no right to answer this request ! playerID="+playerID+" - request="+request);
            return false;
        }
        return true;
    }

    public boolean cancelRequest(String requestID, Player player) {
        if(requestID == null || requestID.isEmpty() || player == null) return false;
        // Find request
        TeamRequest request = findRequestByID(requestID);
        if(request == null) return false;
        // Find team
        Team team = findTeamByID(request.getTeamID());
        if(team == null) return false;
        // Check if player has the right to cancel this request (request sent by himself or sent by the team he's captain of)
        if((request.getAuthor().equalsIgnoreCase("player") && request.getPlayerID() == player.getID()) || (request.getAuthor().equalsIgnoreCase("team") && team.isCaptain(player.getID()))){
            // Remove request
            mongoTemplate.remove(request);
            // Send team event to team captain
            sendTeamEventToPlayer(team.getCaptain().getPlayerID());
        } else {
            // Player has no right to cancel this request
            log.error("Player has no right to cancel this request ! playerID="+player.getID()+" - request="+request);
            return false;
        }
        return true;
    }

    public WSPlayerRequest sendRequestToTeam(String teamID, Player author) throws FBWSException{
        // Check if player is already in a team
        if(getTeamForPlayer(author.getID()) != null){
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_ALREADY_IN_TEAM);
        }
        // Check if a similar request is already pending
        TeamRequest pendingRequest = getRequestForPlayerAndTeam(author.getID(), teamID);
        if(pendingRequest != null){
            // If the request has been sent by the team, automatically accept it
            if(pendingRequest.getAuthor().equalsIgnoreCase("team")){
                if(answerRequest(pendingRequest.getIDStr(), true, author.getID())){
                    return new WSPlayerRequest(); // Returns an empty request as no request has been created
                }
            }
            throw new FBWSException(FBExceptionType.TEAM_REQUEST_ALREADY_EXISTING);
        }
        // Find team
        Team team = findTeamByID(teamID);
        if(team == null){
            throw new FBWSException(FBExceptionType.TEAM_UNKNOWN_TEAM);
        } else {
            if(team.isFull()){
                throw new FBWSException(FBExceptionType.TEAM_FULL_TEAM);
            } else {
                TeamRequest request = new TeamRequest();
                request.setAuthor("PLAYER");
                request.setCreationDateISO(new Date());
                request.setDate(System.currentTimeMillis());
                request.setTeamID(teamID);
                request.setPlayerID(author.getID());
                mongoTemplate.insert(request);
                // Send Team event to captain
                sendTeamEventToPlayer(team.getCaptain().getPlayerID());
                return toWSPlayerRequest(request);
            }
        }
    }

    public WSTeamRequest sendRequestToPlayer(long playerID, Player author) throws FBWSException{
        // Check if author is captain of his team and the team is not already full
        Team team = getTeamForPlayer(author.getID());
        if(team == null || !team.isCaptain(author.getID())){
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if(team.isFull()){
            throw new FBWSException(FBExceptionType.TEAM_FULL_TEAM);
        }
        // Check if a similar request is already pending
        TeamRequest pendingRequest = getRequestForPlayerAndTeam(playerID, team.getIDStr());
        if(pendingRequest != null){
            // If the request has been sent by the player, automatically accept it
            if(pendingRequest.getAuthor().equalsIgnoreCase("player")){
                if(answerRequest(pendingRequest.getIDStr(), true, author.getID())){
                    return new WSTeamRequest(); // Returns an empty request as no request has been created
                }
            }
            throw new FBWSException(FBExceptionType.TEAM_REQUEST_ALREADY_EXISTING);
        }
        // Check if player isn't already in a team
        Team playerTeam = getTeamForPlayer(playerID);
        if(playerTeam != null){
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_ALREADY_IN_TEAM);
        } else {
            TeamRequest request = new TeamRequest();
            request.setAuthor("TEAM");
            request.setCreationDateISO(new Date());
            request.setDate(System.currentTimeMillis());
            request.setTeamID(team.getIDStr());
            request.setPlayerID(playerID);
            mongoTemplate.insert(request);
            // Send Team event to player
            sendTeamEventToPlayer(playerID);
            // Send notif
            MessageNotif notifPla = createNotifTeamRequest(team, playerID);
            FBSession sessionPla = presenceMgr.getSessionForPlayerID(playerID);
            if (sessionPla != null && notifPla != null) {
                sessionPla.pushEvent(notifMgr.buildEvent(notifPla, playerID));
            }
            return toWSTeamRequest(request);
        }
    }

    /**
     * nb messages for player
     * @param player
     * @return
     */
    public int getNbMessagesForPlayer(Player player) {
        int nbMessagesForPlayer = 0;
        Team team = getTeamForPlayer(player.getID());
        // If player has a team
        if(team != null){
            String chatroomID = team.getChatroomID();
            if(chatroomID != null && !chatroomID.isEmpty()){
                nbMessagesForPlayer = chatMgr.getNbUnreadMessagesForPlayerAndChatroom(player.getID(), chatroomID, true);
            }
        }
        return nbMessagesForPlayer;
    }

    /**
     * Nb requests for player
     * @param player
     * @return
     */
    public int getNbRequestsForPlayer(Player player) {
        int nbRequestsForPlayer = 0;
        Team team = getTeamForPlayer(player.getID());
        // If player has a team and he's the captain
        if(team != null){
            if(team.isCaptain(player.getID())){
                List<WSTeamRequest> requests = getRequestsForTeam(team.getIDStr());
                // Return only nb of requests the player has to answer (requests for which he is not the author)
                for(WSTeamRequest request : requests){
                    if(request.author.equalsIgnoreCase("player")) nbRequestsForPlayer++;
                }
            }
        // If player doesn't have a team
        } else {
            List<WSPlayerRequest> requests = getRequestsForPlayer(player.getID());
            // Return only nb of requests the player has to answer (requests for which he is not the author)
            for(WSPlayerRequest request : requests){
                if(request.author.equalsIgnoreCase("team")) nbRequestsForPlayer++;
            }
        }
        return nbRequestsForPlayer;
    }

    public int countTeams(String search, String countryCode, String division) {
        Query q = new Query();
        if (search != null && search.length() > 0) {
            q.addCriteria(Criteria.where("name").regex(search, "i"));
        }
        if (countryCode != null && countryCode.length() > 0) {
            q.addCriteria(Criteria.where("countryCode").regex(countryCode, "i"));
        }
        if (division != null && division.length() > 0) {
            q.addCriteria(Criteria.where("division").is(division));
        }
        return (int)mongoTemplate.count(q, Team.class);
    }

    public List<Team> listTeams(String search, String countryCode, String division, int offset, int nbMax) {
        Query q = new Query();
        if (search != null && search.length() > 0) {
            q.addCriteria(Criteria.where("name").regex(search, "i"));
        }
        if (countryCode != null && countryCode.length() > 0) {
            q.addCriteria(Criteria.where("countryCode").regex(countryCode, "i"));
        }
        if (division != null && division.length() > 0) {
            q.addCriteria(Criteria.where("division").is(division));
        }
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        q.with(new Sort(Sort.Direction.ASC, "name"));
        return mongoTemplate.find(q, Team.class);
    }

    /**
     * List empty teams with nbPlayers = 0
     * @return
     */
    public List<Team> listEmptyTeams() {
        Query q = new Query();
        q.addCriteria(Criteria.where("nbPlayers").is(0));
        q.with(new Sort(Sort.Direction.ASC, "name"));
        return mongoTemplate.find(q, Team.class);
    }

    /**
     * Remove empty teams (nbPlayers=0)
     * @return
     */
    public List<Team> clearEmptyTeams() {
        List<Team> result = new ArrayList<>();
        List<Team> emptyTeams = listEmptyTeams();
        for (Team e : emptyTeams) {
            if (removeTeam(e)) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Remove a team
     * @param e
     * @return
     */
    public boolean removeTeam(Team e) {
        if (e.getNbPlayers() == 0 && e.getPlayers().isEmpty()) {
            // Delete chatroom
            if (e.getChatroomID() != null && !e.getChatroomID().isEmpty()) {
                chatMgr.deleteChatroom(e.getChatroomID());
            }
            // Delete all its pending requests
            removeAllRequestsForTeam(e.getIDStr());
            teamCacheMgr.removeTeamCache(e.getIDStr());
            // Delete team
            mongoTemplate.remove(e);
            return true;
        }
        return false;
    }

    /**
     * List teams with criteria ...
     * @param searchMode
     * @param search
     * @param countryCode
     * @param onlyNotFull
     * @param playerID
     * @param offset
     * @param nbMax
     * @return
     */
    public ListTeamsResponse listTeams(boolean searchMode, String search, String countryCode, boolean onlyNotFull, long playerID, int offset, int nbMax) {
        ListTeamsResponse result = new ListTeamsResponse();
        result.teams = new ArrayList<>();
        result.offset = offset;
        String strListFriendID = "";
        if (playerID > 0) {
            List<Long> listFriendID = playerMgr.listFriendIDForPlayer(playerID);
            for (Long e : listFriendID) {
                if (strListFriendID.length() > 0) {
                    strListFriendID += ",";
                }
                strListFriendID += e;
            }
        }
        List<BasicDBObject> pipeline = new ArrayList<>();
        // Don't find deleted team (team with 0 player)
        pipeline.add(BasicDBObject.parse("{$match:{\"nbPlayers\":{$gt:0}}}"));
        // Search by name
        if (search != null && search.length() >= getConfigIntValue("listSearchNbChar", 5)) {
            pipeline.add(BasicDBObject.parse("{$match:{\"name\":{$regex:\"" + search + "\",$options:\"i\"}}}"));
        }
        // Search by country
        if (countryCode != null && countryCode.length() > 0) {
            pipeline.add(BasicDBObject.parse("{$match:{\"countryCode\":{$regex:\"" + countryCode + "\",$options:\"i\"}}}"));
        }
        // only team with at least one place free
        if (searchMode && onlyNotFull) {
            // the 6th element of players array must not exist !
            pipeline.add(BasicDBObject.parse("{$match:{\"players.5\":{$exists:false}}}"));
        }
        // add group to compute total size
        pipeline.add(BasicDBObject.parse("{$group:{_id:0,size:{$sum:1}}}"));
        AggregateIterable<BasicDBObject> aggOuputSize = mongoTemplate.getCollection("team").aggregate(pipeline, BasicDBObject.class);
        BasicDBObject dboSize = aggOuputSize.first();
        if (dboSize != null) {
            result.totalSize = (Integer) dboSize.get("size");
        }
        pipeline.remove(pipeline.size()-1);

        if (result.totalSize > 0) {
            // perform projection to extract data
            pipeline.add(BasicDBObject.parse("{$project:{\"team\":\"$$ROOT\", \"sortName\":{$toLower:\"$name\"},\"playersFriends\":{$setIntersection:[\"$players.playerID\",[" + strListFriendID + "]]}}}"));// list of friends in a team : intersection between friend list and players
            pipeline.add(BasicDBObject.parse("{$project:{\"team\":1, \"sortName\":1,\"playersFriend\":1, \"nbFriends\":{$size:\"$playersFriends\"}}}"));// add field nbFriends

            if (searchMode) {
                // Search mode : add conditions for sort on nbFriends
                pipeline.add(BasicDBObject.parse("{$project:{\"team\":1, \"sortName\":1, \"playersFriend\":1, \"nbFriends\":1, " +
                        "sortNbPlayers:{ $cond:{if:{$eq:[\"$team.nbPlayers\",1]},then:3,else:" + /* condition 1 : nbPlayers=1 => 3 */
                        "{$cond:{if:{$eq:[\"$team.nbPlayers\",2]},then:2,else:" + /* condition 2 : nbPlayers=2 => 2 */
                        "{$cond:{if:{$eq:[\"$team.nbPlayers\",3]},then:1,else:" + /* condition 3 : nbPlayers=3 => 1 */
                        "{$cond:{if:{$eq:[\"$team.nbPlayers\",4]},then:4,else:" + /* condition 4 : nbPlayers=4 => 4 */
                        "{$cond:{if:{$eq:[\"$team.nbPlayers\",5]},then:5,else:6}}" + /* condition 5 : nbPlayers=5 => 5, else 6 */
                        "}}" +/*for close condition 1*/
                        "}}" +/*for close condition 2*/
                        "}}" +/*for close condition 3*/
                        "}}," +/*for close condition 4*/
                        "sortFriend:{$cond:{if:{$gt:[\"$nbFriends\",0]},then:1,else:0}}" + /* add field to sort on nbFriend*/
                        "}}"));
                // sort : friend or not and nbPlayers (3,2,1,4,5,6+)
                pipeline.add(BasicDBObject.parse("{$sort:{sortFriend:-1, sortNbPlayers:1}}"));
            } else {
                // list mode
                // sort : division and team name
                pipeline.add(BasicDBObject.parse("{$sort:{\"team.division\":1, sortName:1}}"));
            }
            if (offset > 0) {
                pipeline.add(BasicDBObject.parse("{$skip:" + offset + "}"));
            }
            if (nbMax > 0) {
                pipeline.add(BasicDBObject.parse("{$limit:" + nbMax + "}"));
            }
            AggregateIterable<Document> aggOuput = mongoTemplate.getCollection("team").aggregate(pipeline, Document.class);
            for (Document dbo : aggOuput) {
                TeamListAggregate teamList = mongoTemplate.getConverter().read(TeamListAggregate.class, dbo);
                result.teams.add(toWSTeamLight(teamList.team, playerID, teamList.nbFriends));
            }
        }
        return result;
    }

    /**
     * Player want to leave the team
     * @param sessionPlayer
     * @throws FBWSException
     */
    public boolean leaveTeam(FBSession sessionPlayer) throws FBWSException {
        if (sessionPlayer == null || sessionPlayer.getPlayer() == null) {
            log.error("Parameter session not valid ! - sessionPlayer=" + sessionPlayer);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        long playerID = sessionPlayer.getPlayer().getID();
        Team team = getTeamForPlayer(playerID);
        if (team == null) {
            log.error("No team found for playerID=" + playerID);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_NO_TEAM);
        }
        TeamPlayer teamPlayer = team.getPlayer(playerID);
        if (teamPlayer == null) {
            log.error("No teamPlayer found for playerID=" + playerID + " in team=" + team);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (teamPlayer.isCaptain()) {
            log.error("Team is captain ... not authorized to leave team ! - playerID="+playerID+" - team="+team);
        } else {
            // Check if the player to remove is not currently playing a tour
            TeamMemTournament tournamentInProgress = tourTeamMgr.getMemoryMgr().getTournamentInProgressForPlayer(playerID);
            if(tournamentInProgress != null){
                TeamMemTournamentPlayer resultPlayer = tournamentInProgress.getTournamentPlayer(playerID);
                if(resultPlayer != null /*&& resultPlayer.hasPlayedOneDeal()*/ && !resultPlayer.isPlayerFinish()){
                    throw new FBWSException(FBExceptionType.TEAM_LEAVE_FORBIDDEN);
                }
            }
            boolean result =  team.removePlayer(playerID);
            if (result) {
                mongoTemplate.save(team);
                // Update TeamCache
                teamCacheMgr.updateTeamData(team);
                // remove player from chatroom
                if(team.getChatroomID() != null && !team.getChatroomID().isEmpty()){
                    Chatroom chatroom = (Chatroom) chatMgr.findChatroomByID(team.getChatroomID());
                    if(chatroom != null){
                        chatroom.removeParticipant(playerID);
                        chatMgr.saveChatroom(chatroom);
                    }
                }
                // send system message to chatroom
                String pseudo = sessionPlayer.getPlayer().getNickname();
                HashMap<String, String> templateParameters = new HashMap<>();
                templateParameters.put("PSEUDO", pseudo);
                chatMgr.sendSystemMessageToChatroom("chat.teamPlayerLeave", templateParameters, team.getChatroomID());
                // send notif to player
                MessageNotif notif = createNotifPlayerLeave(team, playerID);
                if (notif != null) {
                    sessionPlayer.pushEvent(notifMgr.buildEvent(notif, playerID));
                }
                // If team has now less than 4 lead players and was playing the current period, send a notif to the captain to warn him
                if (tourTeamMgr.getCurrentPeriod() != null) {
                    if (team.getNbPlayers() == getNbLeadPlayers() - 1 && team.getLastPeriodWithCompleteTeam() != null && team.getLastPeriodWithCompleteTeam().equalsIgnoreCase(tourTeamMgr.getCurrentPeriod().getID())) {
                        MessageNotif notifPla = createNotifTeamBecameIncomplete(team.getCaptain().getPlayerID());
                        FBSession sessionPla = presenceMgr.getSessionForPlayerID(team.getCaptain().getPlayerID());
                        if (sessionPla != null && notifPla != null) {
                            sessionPla.pushEvent(notifMgr.buildEvent(notifPla, team.getCaptain().getPlayerID()));
                        }
                    }
                }
                // send team event to other players
                for(TeamPlayer tp : team.getPlayers()){
                    sendTeamEventToPlayer(tp.getPlayerID());
                }
            }
            return result;
        }
        return false;
    }

    /**
     * Clean the team (only the captain can do it). Team is not removed ! All players are removed.
     * @param sessionCaptain the ID of the player asking to delete his team
     * @throws FBWSException
     */
    public boolean emptyTeam(FBSession sessionCaptain) throws FBWSException {
        if (sessionCaptain == null || sessionCaptain.getPlayer() == null) {
            log.error("Parameter session not valid ! - sessionCaptain="+sessionCaptain);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        long playerID = sessionCaptain.getPlayer().getID();
        Team team = getTeamForPlayer(playerID);
        if (team == null) {
            log.error("No team found for playerID="+playerID);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_NO_TEAM);
        }
        if(!team.isCaptain(playerID)){
            log.error("Player can't delete the team, he's not the captain !");
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_NOT_CAPTAIN);
        }
        return emptyTeam(team);
    }

    /**
     * Empty the team (only the captain can do it). Team is not removed ! All players are removed.
     * @param team the team to empty
     * @throws FBWSException
     */
    public boolean emptyTeam(Team team) throws FBWSException {
        if (team != null) {

            // send notif to each player
            PlayerCache pcCaptain = playerCacheMgr.getPlayerCache(team.getCaptain().getPlayerID());
            if (pcCaptain != null) {
                for (TeamPlayer tp : team.getPlayers()) {
                    MessageNotif notifPla = createNotifPlayerDelete(team, tp.getPlayerID(), pcCaptain.getPseudo());
                    FBSession sessionPla = presenceMgr.getSessionForPlayerID(tp.getPlayerID());
                    if (sessionPla != null && notifPla != null) {
                        sessionPla.pushEvent(notifMgr.buildEvent(notifPla, tp.getPlayerID()));
                        sendTeamEventToPlayer(tp.getPlayerID());
                    }
                }
            }

            // Delete chatroom
            if (team.getChatroomID() != null && !team.getChatroomID().isEmpty()) {
                chatMgr.deleteChatroom(team.getChatroomID());
            }
            // Delete all its pending requests
            removeAllRequestsForTeam(team.getIDStr());
            // Delete team (actually we don't delete it, we just remove all the players so that the tournament rankings still work)
            team.getPlayers().clear();
            team.setNbPlayers(0);
            mongoTemplate.save(team);
            // Update TeamCache
            teamCacheMgr.updateTeamData(team);
            return true;
        }
        return false;
    }

    /**
     * Delete all the inactive teams having less than the minimum number of lead players
     * @param lastPeriodWithCompleteTeam the last period when team was complete (should be 3 months before today)
     * @return nb teams deleted (-1 => error !)
     */
    public int deleteInactiveIncompleteTeams(String lastPeriodWithCompleteTeam) {
        if (lastPeriodWithCompleteTeam == null || lastPeriodWithCompleteTeam.isEmpty()) {
            log.error("lastPeriodWithCompleteTeam is null or empty!");
            return -1;
        }
        // Check if period exist
        TeamPeriod period = tourTeamMgr.getPeriodForID(lastPeriodWithCompleteTeam);
        if(period == null){
            log.error("period not found for periodID="+lastPeriodWithCompleteTeam);
            return -1;
        }
        // Find teams to delete
        List<Team> teamsToDelete = getInactiveAndIncompleteTeams(period);
        // Delete teams
        int nbDeleteDone = 0;
        for(Team team : teamsToDelete){
            try{
                // Send notif to each player
                for (TeamPlayer tp : team.getPlayers()) {
                    MessageNotif notifPla = createNotifDeleteIncompleteInactiveTeam(tp.getPlayerID());
                    FBSession sessionPla = presenceMgr.getSessionForPlayerID(tp.getPlayerID());
                    if (sessionPla != null && notifPla != null) {
                        sessionPla.pushEvent(notifMgr.buildEvent(notifPla, tp.getPlayerID()));
                    }
                }
                // Delete chatroom
                if(team.getChatroomID() != null && !team.getChatroomID().isEmpty()){
                    chatMgr.deleteChatroom(team.getChatroomID());
                }
                // Delete all its pending requests
                removeAllRequestsForTeam(team.getIDStr());
                teamCacheMgr.removeTeamCache(team.getIDStr());
                // Delete team
                mongoTemplate.remove(team);
                nbDeleteDone++;
            } catch (Exception e){
                log.error("Failed to delete team "+team.getIDStr(), e);
            }
        }
        log.warn("Nb teams to delete="+teamsToDelete.size()+" - nb delete done="+nbDeleteDone);
        return nbDeleteDone;
    }

    /**
     * Change the team composition (only the captain can do it)
     * @param playerID the ID of the player asking to change his team composition
     * @param newComposition list of players including their substitute status
     * @return list of pseudo of players who can't become substitute because they already started (and not finished) the current tour
     * @throws FBWSException
     */
    public List<String> changeComposition(long playerID, List<TeamPlayer> newComposition) throws FBWSException {
        List<String> listProblemPseudo = new ArrayList<>();
        Team team = getTeamForPlayer(playerID);
        if (team == null) {
            log.error("No team found for playerID="+playerID);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_NO_TEAM);
        }
        if(!team.isCaptain(playerID)){
            log.error("Player can't delete the team, he's not the captain !");
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_NOT_CAPTAIN);
        }
        if(newComposition.size() > getTeamSize() || newComposition.size() < 1){
            log.error("Nb of players must be between 1 and "+getTeamSize()+" ! players.size="+ newComposition.size());
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if(newComposition.size() != team.getNbPlayers()){
            log.error("Nb of players in new composition is different from current number of players in the team!");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        // Check composition validity
        int nbSubstitutes = 0;
        int nbLeadPlayers = 0;
        for(TeamPlayer p : newComposition){
            if (team.isPlayerOfTeam(p.getPlayerID())) {
                if (p.isSubstitute()) {
                    nbSubstitutes++;
                } else {
                    nbLeadPlayers++;
                }
            }
        }
                // Check composition validity
        if((nbLeadPlayers > getNbLeadPlayers()) ||
                (nbSubstitutes > (getTeamSize() - getNbLeadPlayers())) ||
                (team.getNbPlayers() <= nbLeadPlayers && nbSubstitutes > 0)){
            log.error("Too many lead players or too many substitutes ! Composition invalid");
            throw new FBWSException(FBExceptionType.TEAM_INVALID_COMPOSITION);
        }
        // Check if a substitute hasn't started a tournament yet
        boolean problem = false;
        for (TeamPlayer p : newComposition) {
            TeamPlayer existingPlayer = team.getPlayer(p.getPlayerID());
            if (existingPlayer != null) {
                // player become substitute
                if (p.isSubstitute() && !existingPlayer.isSubstitute()) {
                    // check no tournament in progress
                    TeamMemTournament tournamentInProgress = tourTeamMgr.getMemoryMgr().getTournamentInProgressForPlayer(p.getPlayerID());
                    if (tournamentInProgress != null) {
                        TeamMemTournamentPlayer playerResult = tournamentInProgress.getTournamentPlayer(p.getPlayerID());
                        if (playerResult != null && !playerResult.isPlayerFinish() /*&& playerResult.hasPlayedOneDeal()*/) {
                            PlayerCache playerCache = playerCacheMgr.getPlayerCache(p.getPlayerID());
                            listProblemPseudo.add(playerCache.getPseudo());
                            problem = true;
                        }
                    }
                }
            }
        }
        if (problem){
            return listProblemPseudo;
        }
        // no problem => set modif
        for (TeamPlayer p : newComposition) {
            TeamPlayer existingPlayer = team.getPlayer(p.getPlayerID());
            if (existingPlayer != null) {
                existingPlayer.setSubstitute(p.isSubstitute());
                if (existingPlayer.isSubstitute()) {
                    existingPlayer.setGroup(null);
                }
            }
        }
        // If the team is currently playing a tour, we have to set a group for new lead players
        if(tourTeamMgr.getMemoryMgr().getTeamMemDivisionTourResultForTeam(team.getIDStr(), null) != null) {
            for (TeamPlayer p : team.getPlayers()) {
                // Player is a lead player but has no group
                if (!p.isSubstitute() && (p.getGroup() == null || p.getGroup().isEmpty())) {
                    // Let's find an empty group
                    for (String group : TourTeamMgr.GROUP_TAB) {
                        boolean playerAlreadyInGroup = false;
                        for (TeamPlayer tp : team.getPlayers()) {
                            if (!tp.isSubstitute()) {
                                if (tp.getGroup() != null && tp.getGroup().equalsIgnoreCase(group)) {
                                    playerAlreadyInGroup = true;
                                    break;
                                }
                            }
                        }
                        if (!playerAlreadyInGroup) {
                            p.setGroup(group);
                            break;
                        }
                    }
                }
            }
        }

        // Send message to chatroom
        PlayerCache playerCache = playerCacheMgr.getPlayerCache(team.getCaptain().getPlayerID());
        String pseudoCaptain = playerCache.getPseudo();
        HashMap<String, String> templateParameters = new HashMap<>();
        templateParameters.put("PSEUDO_CAPTAIN", pseudoCaptain);
        int leadPlayers = 0;
        for(TeamPlayer p : team.getPlayers()){
            if(!p.isSubstitute()){
                leadPlayers++;
                playerCache = playerCacheMgr.getOrLoadPlayerCache(p.getPlayerID());
                templateParameters.put("PSEUDO"+leadPlayers, playerCache.getPseudo());
            }
        }
        chatMgr.sendSystemMessageToChatroom("chat.teamNewComposition", templateParameters, team.getChatroomID());


        // send team event to players
        for(TeamPlayer tp : team.getPlayers()){
            sendTeamEventToPlayer(tp.getPlayerID());
        }
        // Save team
        mongoTemplate.save(team);
        // Update TeamCache
        teamCacheMgr.updateTeamData(team);
        return listProblemPseudo;
    }

    /**
     * Search player free (not yet in a team)
     * @param search
     * @param countryCode
     * @param askerID
     * @return
     * @throws FBWSException
     */
    public List<WSTeamPlayer> searchPlayerFree(String search, boolean friend, String countryCode, long askerID) throws FBWSException {
        List<WSTeamPlayer> result = new ArrayList<>();
        List<Long> listPlayerID;
        int nbMax = getConfigIntValue("searchPlayerNbMax", 100);
        // friend filter is on =>
        if(friend) {
            nbMax = 0;
            listPlayerID = playerMgr.listFriendIDForPlayer(askerID);
            // friend filter is off => search a list of player by pseudo, lastname, countryCode
        } else if(search != null && !search.isEmpty()){
            listPlayerID = playerMgr.searchPlayerID(search, countryCode, 0, getConfigIntValue("searchPlayerPatternNbMax", 2*nbMax));
        } else {
            listPlayerID = playerMgr.listSuggestedTeamMembers(askerID, countryCode, getConfigIntValue("searchPlayerPatternNbMax", 2*nbMax));
        }

        String strListPlayerID = "";
        for (Long e : listPlayerID) {
            if (strListPlayerID.length() > 0) {strListPlayerID += ",";}
            strListPlayerID += e;
        }
        if (listPlayerID.size() > 0) {

            List<BasicDBObject> pipeline = new ArrayList<>();
            // find players already in team {$match:{"players.playerID":{$in:[10,2]}}},{$project:{_id:0,temp:"$players.playerID"}},{$unwind:"$temp"},{$match:{temp:{$in:[10,2]}}}
            pipeline.add(BasicDBObject.parse("{$match:{\"players.playerID\":{$in:[" + strListPlayerID + "]}}}"));
            pipeline.add(BasicDBObject.parse("{$project:{_id:0,temp:\"$players.playerID\"}}"));
            pipeline.add(BasicDBObject.parse("{$unwind:\"$temp\"}"));
            pipeline.add(BasicDBObject.parse("{$match:{temp:{$in:[" + strListPlayerID + "]}}}"));
            AggregateIterable<BasicDBObject> aggOutput = mongoTemplate.getCollection("team").aggregate(pipeline, BasicDBObject.class);
            // remove player already in a team
            for (BasicDBObject dbo : aggOutput) {
                Long e = (Long) dbo.get("temp");
                listPlayerID.remove(e);
                if (listPlayerID.size() == 0) {
                    break;
                }
            }
        }

        // build WSTeamPlayer
        for (Long playerID : listPlayerID) {
            WSTeamPlayer teamPlayer = toWSTeamPlayer(playerID, false, false, null);
            // Check if there's already a request pending between this player and the team
            Team team = getTeamForPlayer(askerID);
            if(team != null){
                TeamRequest requestPending = getRequestForPlayerAndTeam(playerID, team.getIDStr());
                if(requestPending != null) teamPlayer.requestPending = true;
            }
            result.add(teamPlayer);
            if (nbMax != 0 && result.size() >= nbMax) {
                break;
            }
        }
        // sort on average performance
        Collections.sort(result, new Comparator<WSTeamPlayer>() {
            @Override
            public int compare(WSTeamPlayer o1, WSTeamPlayer o2) {
                return Double.compare(o2.averagePerformance, o1.averagePerformance);
            }
        });
        return result;
    }

    /**
     * Remove a player from a team
     * @param playerIDCaptain
     * @param playerIDToRemove
     * @return
     * @throws FBWSException
     */
    public boolean removePlayer(long playerIDCaptain, long playerIDToRemove) throws FBWSException{
        Team team = getTeamForPlayer(playerIDToRemove);
        if (team == null) {
            log.error("No team found for player="+playerIDToRemove);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_NO_TEAM);
        }
        // check captain of the player team
        if (team.getCaptain() != null && team.getCaptain().getPlayerID() != playerIDCaptain) {
            log.error("Current player is not the captain of the team="+team+" - playerIDCaptain="+playerIDCaptain);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_NOT_CAPTAIN);
        }
        // Check if the player to remove is not currently playing a tour
        TeamMemTournament tournamentInProgress = tourTeamMgr.getMemoryMgr().getTournamentInProgressForPlayer(playerIDToRemove);
        if(tournamentInProgress != null){
            TeamMemTournamentPlayer resultPlayer = tournamentInProgress.getTournamentPlayer(playerIDToRemove);
            if(resultPlayer != null && /*resultPlayer.hasPlayedOneDeal() &&*/ !resultPlayer.isPlayerFinish()){
                throw new FBWSException(FBExceptionType.TEAM_PLAYER_PLAYING_CURRENT_TOUR);
            }
        }

        if (team.removePlayer(playerIDToRemove)) {
            mongoTemplate.save(team);
            // Update TeamCache
            teamCacheMgr.updateTeamData(team);
            // remove player from chatroom
            if(team.getChatroomID() != null && !team.getChatroomID().isEmpty()){
                Chatroom chatroom = (Chatroom) chatMgr.findChatroomByID(team.getChatroomID());
                if(chatroom != null){
                    chatroom.removeParticipant(playerIDToRemove);
                    chatMgr.saveChatroom(chatroom);
                }
            }
            // send system message to chatroom
            PlayerCache playerCache = playerCacheMgr.getPlayerCache(playerIDToRemove);
            if (playerCache == null) {
                playerCache = playerCacheMgr.getOrLoadPlayerCache(playerIDToRemove);
            }
            String pseudo = playerCache.getPseudo();
            HashMap<String, String> templateParameters = new HashMap<>();
            templateParameters.put("PSEUDO", pseudo);
            chatMgr.sendSystemMessageToChatroom("chat.teamPlayerLeave", templateParameters, team.getChatroomID());
            // send notif to player remove
            MessageNotif notif = createNotifRemovePlayer(team, playerIDToRemove);
            FBSession session = presenceMgr.getSessionForPlayerID(playerIDToRemove);
            if (notif != null && session != null) {
                session.pushEvent(notifMgr.buildEvent(notif, playerIDToRemove));
            }
            // send team event to player remove
            sendTeamEventToPlayer(playerIDToRemove);
            // send team event to other players
            for(TeamPlayer tp : team.getPlayers()){
                sendTeamEventToPlayer(tp.getPlayerID());
            }
            return true;
        }
        return false;
    }

    /**
     * Change the captain of a team
     * @param sessionCurrentCaptain
     * @param playerIDNewCaptain
     * @return
     * @throws FBWSException
     */
    public boolean changeCaptain(FBSession sessionCurrentCaptain, long playerIDNewCaptain) throws FBWSException {
        if (sessionCurrentCaptain == null || sessionCurrentCaptain.getPlayer() == null) {
            log.error("Parmeter session is not valid - sessionCurrentCaptain="+sessionCurrentCaptain);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        long playerIDCurrentCaptain = sessionCurrentCaptain.getPlayer().getID();
        Team team = getTeamForPlayer(playerIDNewCaptain);
        if (team == null) {
            log.error("No team found for playerID="+playerIDNewCaptain);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_NO_TEAM);
        }
        // check captain of the player team
        if (team.getCaptain() != null && team.getCaptain().getPlayerID() != playerIDCurrentCaptain) {
            log.error("Current player is not the captain of the team="+team+" - playerIDCaptain="+playerIDCurrentCaptain);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_NOT_CAPTAIN);
        }
        TeamPlayer currentCaptain = team.getCaptain();
        TeamPlayer newCaptain = team.getPlayer(playerIDNewCaptain);
        if (currentCaptain != null) {
            currentCaptain.setCaptain(false);
            newCaptain.setCaptain(true);
            mongoTemplate.save(team);
            // Update TeamCache
            teamCacheMgr.updateTeamData(team);
            // send system message to chatroom
            PlayerCache playerCache = playerCacheMgr.getPlayerCache(playerIDNewCaptain);
            HashMap<String, String> templateParameters = new HashMap<>();
            templateParameters.put("PSEUDO_CAPTAIN", playerCache.getPseudo());
            chatMgr.sendSystemMessageToChatroom("chat.teamNewCaptain", templateParameters, team.getChatroomID());
            // send notif to old captain
            MessageNotif notifPreviousCaptain = createNotifChangeCaptainPrevious(team, playerIDCurrentCaptain);
            if (notifPreviousCaptain != null) {
                sessionCurrentCaptain.pushEvent(notifMgr.buildEvent(notifPreviousCaptain, playerIDCurrentCaptain));
            }
            // send notif to next captain
            MessageNotif notifNewCaptain = createNotifChangeCaptainNew(team, playerIDCurrentCaptain);
            FBSession sessionNewCaptain = presenceMgr.getSessionForPlayerID(playerIDNewCaptain);
            if (sessionNewCaptain != null && notifNewCaptain != null) {
                sessionNewCaptain.pushEvent(notifMgr.buildEvent(notifNewCaptain, playerIDNewCaptain));
            }
            // send team event to players
            for(TeamPlayer tp : team.getPlayers()){
                sendTeamEventToPlayer(tp.getPlayerID());
            }
            return true;
        }
        return false;
    }

    /**
     * Update the team (just the description)
     * @param sessionCaptain
     * @param description
     * @return
     * @throws FBWSException
     */
    public boolean updateTeam(FBSession sessionCaptain, String description) throws FBWSException {
        if (sessionCaptain == null || sessionCaptain.getPlayer() == null) {
            log.error("Parameter session is not valid - sessionCaptain="+sessionCaptain);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        long playerIDCaptain = sessionCaptain.getPlayer().getID();
        Team team = getTeamForPlayer(playerIDCaptain);
        if (team == null) {
            log.error("No team found for playerID="+playerIDCaptain);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_NO_TEAM);
        }
        // check captain of the player team
        if (team.getCaptain() != null && team.getCaptain().getPlayerID() != playerIDCaptain) {
            log.error("Current player is not the captain of the team="+team+" - playerIDCaptain="+playerIDCaptain);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_NOT_CAPTAIN);
        }
        team.setDescription(description);
        mongoTemplate.save(team);
        // Update TeamCache
        teamCacheMgr.updateTeamData(team);
        return true;
    }


    private boolean sendTeamEventToPlayer(long playerID){
        FBSession session = presenceMgr.getSessionForPlayerID(playerID);
        if (session != null) {
            Event evt = new Event();
            evt.timestamp = System.currentTimeMillis();
            evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
            evt.receiverID = playerID;
            evt.addFieldCategory(Constantes.EVENT_CATEGORY_TEAM, null);
            session.pushEvent(evt);
            return true;
        }
        return false;
    }

    /**
     * Send notif to captain for team creation
     * @param team
     * @return
     */
    public MessageNotif createNotifCreateTeam(Team team) {
        if (notifMgr.isNotifEnable()) {
            if (team != null && team.getCaptain() != null) {
                TextUIData notifTemplate = notifMgr.getTemplate("teamCreate");
                if (notifTemplate != null) {
                    MessageNotif notif = new MessageNotif(notifTemplate.name);
                    notif.recipientID = team.getCaptain().getPlayerID();
                    notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                    notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                    notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_TEAM_TYPE, MessageNotifMgr.NOTIF_ACTION_TEAM_TYPE_CHECK);
                    notif.dateExpiration = System.currentTimeMillis() + 30*MessageNotifMgr.DURATION_1J;
                    notif.templateParameters.put("TEAM_NAME", team.getName());
                    if (notifMgr.persistNotif(notif)) {
                        return notif;
                    }
                } else {
                    log.error("No template found for name teamCreate");
                }
            } else {
                log.error("Parameters not valid ... team="+team);
            }
        }
        return null;
    }

    /**
     * Send notif to all players of a team who has just been deleted because it was incomplete and inactive
     * @param recipientID
     * @return
     */
    public MessageNotif createNotifDeleteIncompleteInactiveTeam(long recipientID) {
        if (notifMgr.isNotifEnable()) {
            TextUIData notifTemplate = notifMgr.getTemplate("teamIncomplete3MonthsDelete");
            if (notifTemplate != null) {
                MessageNotif notif = new MessageNotif(notifTemplate.name);
                notif.recipientID = recipientID;
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_TEAM_TYPE, MessageNotifMgr.NOTIF_ACTION_TEAM_TYPE_DELETE);
                notif.dateExpiration = System.currentTimeMillis() + 30*MessageNotifMgr.DURATION_1J;
                if (notifMgr.persistNotif(notif)) {
                    return notif;
                }
            } else {
                log.error("No template found for name teamIncomplete3MonthsDelete");
            }
        }
        return null;
    }

    /**
     * Send notif to all players of a team who is now complete and will be able to compete in the next period
     * @param recipientID
     * @return
     */
    public MessageNotif createNotifTeamNowComplete(long recipientID) {
        if (notifMgr.isNotifEnable()) {
            TextUIData notifTemplate = notifMgr.getTemplate("teamNowComplete");
            if (notifTemplate != null) {
                MessageNotif notif = new MessageNotif(notifTemplate.name);
                notif.recipientID = recipientID;
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_TEAM_TYPE, MessageNotifMgr.NOTIF_ACTION_TEAM_TYPE_CHECK);
                notif.dateExpiration = System.currentTimeMillis() + 30*MessageNotifMgr.DURATION_1J;
                if (notifMgr.persistNotif(notif)) {
                    return notif;
                }
            } else {
                log.error("No template found for name teamNowComplete");
            }
        }
        return null;
    }

    /**
     * Send notif to the captain of a team who is now incomplete and is playing the current period
     * @param recipientID
     * @return
     */
    public MessageNotif createNotifTeamBecameIncomplete(long recipientID) {
        if (notifMgr.isNotifEnable()) {
            TextUIData notifTemplate = notifMgr.getTemplate("teamBecameIncomplete");
            if (notifTemplate != null) {
                MessageNotif notif = new MessageNotif(notifTemplate.name);
                notif.recipientID = recipientID;
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_TEAM_TYPE, MessageNotifMgr.NOTIF_ACTION_TEAM_TYPE_CROSS);
                notif.dateExpiration = System.currentTimeMillis() + 30*MessageNotifMgr.DURATION_1J;
                if (notifMgr.persistNotif(notif)) {
                    return notif;
                }
            } else {
                log.error("No template found for name teamBecameIncomplete");
            }
        }
        return null;
    }

    /**
     * Send notif to player for team delete by captain
     * @param team
     * @param playerID
     * @return
     */
    public MessageNotif createNotifPlayerDelete(Team team, long playerID, String captainPseudo) {
        if (notifMgr.isNotifEnable()) {
            if (team != null && team.getCaptain() != null) {
                TextUIData notifTemplate = notifMgr.getTemplate("teamPlayerDelete");
                if (notifTemplate != null) {
                    MessageNotif notif = new MessageNotif(notifTemplate.name);
                    notif.recipientID = playerID;
                    notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                    notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                    notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_TEAM_TYPE, MessageNotifMgr.NOTIF_ACTION_TEAM_TYPE_DELETE);
                    notif.dateExpiration = System.currentTimeMillis() + 30*MessageNotifMgr.DURATION_1J;
                    notif.templateParameters.put("TEAM_NAME", team.getName());
                    notif.templateParameters.put("PSEUDO_CAPTAIN", captainPseudo);
                    if (notifMgr.persistNotif(notif)) {
                        return notif;
                    }
                } else {
                    log.error("No template found for name teamPlayerDelete");
                }
            } else {
                log.error("Parameters not valid ... team="+team);
            }
        }
        return null;
    }

    /**
     * Send notif to previous captain for change captain
     * @param team
     * @param previousCaptainID
     * @return
     */
    public MessageNotif createNotifChangeCaptainPrevious(Team team, long previousCaptainID) {
        if (notifMgr.isNotifEnable()) {
            if (team != null && team.getCaptain() != null) {
                TextUIData notifTemplate = notifMgr.getTemplate("teamChangeCaptainPrevious");
                if (notifTemplate != null) {
                    PlayerCache pc = playerCacheMgr.getPlayerCache(team.getCaptain().getPlayerID());
                    if (pc != null) {
                        MessageNotif notif = new MessageNotif(notifTemplate.name);
                        notif.recipientID = previousCaptainID;
                        notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                        notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                        notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_TEAM_TYPE, MessageNotifMgr.NOTIF_ACTION_TEAM_TYPE_CAPTAIN);
                        notif.dateExpiration = System.currentTimeMillis() + 30 * MessageNotifMgr.DURATION_1J;
                        notif.templateParameters.put("TEAM_NAME", team.getName());
                        notif.templateParameters.put("PSEUDO_NEW_CAPTAIN", pc.getPseudo());
                        if (notifMgr.persistNotif(notif)) {
                            return notif;
                        }
                    } else {
                        log.error("No playerCache found for captain="+team.getCaptain());
                    }
                } else {
                    log.error("No template found for name teamChangeCaptainPrevious");
                }
            } else {
                log.error("Parameters not valid ... team="+team);
            }
        }
        return null;
    }

    /**
     * Send notif to captain for change of captain
     * @param team
     * @param previousCaptainID
     * @return
     */
    public MessageNotif createNotifChangeCaptainNew(Team team, long previousCaptainID) {
        if (notifMgr.isNotifEnable()) {
            if (team != null && team.getCaptain() != null) {
                TextUIData notifTemplate = notifMgr.getTemplate("teamChangeCaptainNew");
                if (notifTemplate != null) {
                    PlayerCache pc = playerCacheMgr.getPlayerCache(previousCaptainID);
                    if (pc != null) {
                        MessageNotif notif = new MessageNotif(notifTemplate.name);
                        notif.recipientID = team.getCaptain().getPlayerID();
                        notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                        notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                        notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_TEAM_TYPE, MessageNotifMgr.NOTIF_ACTION_TEAM_TYPE_CAPTAIN);
                        notif.dateExpiration = System.currentTimeMillis() + 30 * MessageNotifMgr.DURATION_1J;
                        notif.templateParameters.put("TEAM_NAME", team.getName());
                        notif.templateParameters.put("PSEUDO_PREVIOUS_CAPTAIN", pc.getPseudo());
                        if (notifMgr.persistNotif(notif)) {
                            return notif;
                        }
                    } else {
                        log.error("No playerCache found for ID="+previousCaptainID+" - team="+team);
                    }
                } else {
                    log.error("No template found for name teamChangeCaptainNew");
                }
            } else {
                log.error("Parameters not valid ... team="+team);
            }
        }
        return null;
    }

    /**
     * Send notif to player for remove by captain
     * @param team
     * @param playerIDRemove
     * @return
     */
    public MessageNotif createNotifRemovePlayer(Team team, long playerIDRemove) {
        if (notifMgr.isNotifEnable()) {
            if (team != null && team.getCaptain() != null) {
                TextUIData notifTemplate = notifMgr.getTemplate("teamRemovePlayer");
                if (notifTemplate != null) {
                    PlayerCache pc = playerCacheMgr.getPlayerCache(team.getCaptain().getPlayerID());
                    if (pc != null) {
                        MessageNotif notif = new MessageNotif(notifTemplate.name);
                        notif.recipientID = playerIDRemove;
                        notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                        notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                        notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_TEAM_TYPE, MessageNotifMgr.NOTIF_ACTION_TEAM_TYPE_DELETE);
                        notif.dateExpiration = System.currentTimeMillis() + 30 * MessageNotifMgr.DURATION_1J;
                        notif.templateParameters.put("TEAM_NAME", team.getName());
                        notif.templateParameters.put("PSEUDO_CAPTAIN", pc.getPseudo());
                        if (notifMgr.persistNotif(notif)) {
                            return notif;
                        }
                    } else {
                        log.error("No playerCache found for captain="+team.getCaptain());
                    }
                } else {
                    log.error("No template found for name teamRemovePlayer");
                }
            } else {
                log.error("Parameters not valid ... team="+team);
            }
        }
        return null;
    }

    /**
     * Send notif to player for leave team
     * @param team
     * @param playerIDLeave
     * @return
     */
    public MessageNotif createNotifPlayerLeave(Team team, long playerIDLeave) {
        if (notifMgr.isNotifEnable()) {
            if (team != null && team.getCaptain() != null) {
                TextUIData notifTemplate = notifMgr.getTemplate("teamPlayerLeave");
                if (notifTemplate != null) {
                    MessageNotif notif = new MessageNotif(notifTemplate.name);
                    notif.recipientID = playerIDLeave;
                    notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                    notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                    notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_TEAM_TYPE, MessageNotifMgr.NOTIF_ACTION_TEAM_TYPE_LEAVE);
                    notif.dateExpiration = System.currentTimeMillis() + 30 * MessageNotifMgr.DURATION_1J;
                    notif.templateParameters.put("TEAM_NAME", team.getName());
                    if (notifMgr.persistNotif(notif)) {
                        return notif;
                    }
                } else {
                    log.error("No template found for name teamPlayerLeave");
                }
            } else {
                log.error("Parameters not valid ... team="+team);
            }
        }
        return null;
    }

    /**
     * Send notif to player for request answer OK
     * @param team
     * @param playerID
     * @return
     */
    public MessageNotif createNotifRequestPlayerOK(Team team, long playerID) {
        if (notifMgr.isNotifEnable()) {
            if (team != null && team.getCaptain() != null) {
                TextUIData notifTemplate = notifMgr.getTemplate("teamRequestPlayerOK");
                if (notifTemplate != null) {
                    PlayerCache pc = playerCacheMgr.getPlayerCache(team.getCaptain().getPlayerID());
                    if (pc != null) {
                        MessageNotif notif = new MessageNotif(notifTemplate.name);
                        notif.recipientID = playerID;
                        notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                        notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                        notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_TEAM_TYPE, MessageNotifMgr.NOTIF_ACTION_TEAM_TYPE_CHECK);
                        notif.dateExpiration = System.currentTimeMillis() + 30 * MessageNotifMgr.DURATION_1J;
                        notif.templateParameters.put("TEAM_NAME", team.getName());
                        notif.templateParameters.put("PSEUDO_CAPTAIN", pc.getPseudo());
                        if (notifMgr.persistNotif(notif)) {
                            return notif;
                        }
                    } else {
                        log.error("No playerCache found for captain="+team.getCaptain());
                    }
                } else {
                    log.error("No template found for name teamRequestPlayerOK");
                }
            } else {
                log.error("Parameters not valid ... team="+team);
            }
        }
        return null;
    }

    /**
     * Send notif to player for request from a team
     * @param team
     * @param playerID
     * @return
     */
    public MessageNotif createNotifTeamRequest(Team team, long playerID) {
        if (notifMgr.isNotifEnable()) {
            if (team != null && team.getCaptain() != null) {
                TextUIData notifTemplate = notifMgr.getTemplate("teamRequest");
                if (notifTemplate != null) {
                    PlayerCache pc = playerCacheMgr.getPlayerCache(team.getCaptain().getPlayerID());
                    if (pc != null) {
                        MessageNotif notif = new MessageNotif(notifTemplate.name);
                        notif.recipientID = playerID;
                        notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                        notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                        notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_TEAM_TYPE, MessageNotifMgr.NOTIF_ACTION_TEAM_TYPE_CHECK);
                        notif.dateExpiration = System.currentTimeMillis() + 30 * MessageNotifMgr.DURATION_1J;
                        notif.templateParameters.put("TEAM_NAME", team.getName());
                        notif.templateParameters.put("PSEUDO_CAPTAIN", pc.getPseudo());
                        if (notifMgr.persistNotif(notif)) {
                            return notif;
                        }
                    } else {
                        log.error("No playerCache found for captain="+team.getCaptain());
                    }
                } else {
                    log.error("No template found for name teamRequest");
                }
            } else {
                log.error("Parameters not valid ... team="+team);
            }
        }
        return null;
    }

    /**
     * Send notif to player's team with incomplete teams
     * @param listTeamID
     * @param dateExpiration
     * @return
     */
    public int createAndSetNotifGroupTeamIncompleteEndLastTour(List<String> listTeamID, long dateExpiration) {
        if (notifMgr.isNotifEnable() && listTeamID.size() > 0) {
            TextUIData notifTemplate = notifMgr.getTemplate("teamIncompleteEndLastTour");
            if (notifTemplate != null) {
                MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                notif.dateExpiration = dateExpiration;
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                notif.templateParameters.put("NB_PLAYERS_MINIMUM", ""+TourTeamMgr.GROUP_TAB.length);
                if (notifMgr.persistNotif(notif)) {
                    List<Long> playerIDList = new ArrayList<>();
                    for (String t : listTeamID) {
                        TeamCache tc = teamCacheMgr.getOrLoadTeamCache(t);
                        if (tc != null) {
                            playerIDList.addAll(tc.players);
                        }
                    }
                    if (playerIDList.size() > 0) {
                        if (notifMgr.setNotifGroupForPlayer(notif, playerIDList)) {
                            return playerIDList.size();
                        } else {
                            log.error("Failed to set notif group for players - notif="+notif+" - list player size="+playerIDList.size());
                        }
                    }
                } else {
                    log.error("Failed to persist notifGroup="+notif);
                }
            } else {
                log.error("No template found for name teamIncompleteEndLastTour");
            }
        }  else {
            log.warn("Notif is disable ot list teamID empty !");
        }
        return 0;
    }

    /**
     * Send notif to player's team become incomplete at end period
     * @param listTeamID
     * @param dateExpiration
     * @return
     */
    public int createAndSetNotifGroupTeamBecomeIncompleteEndPeriod(List<String> listTeamID, long dateExpiration) {
        if (notifMgr.isNotifEnable() && listTeamID.size() > 0) {
            TextUIData notifTemplate = notifMgr.getTemplate("teamBecomeIncompleteEndPeriod");
            if (notifTemplate != null) {
                MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                notif.dateExpiration = dateExpiration;
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                notif.templateParameters.put("NB_PLAYERS_MINIMUM", ""+TourTeamMgr.GROUP_TAB.length);
                if (notifMgr.persistNotif(notif)) {
                    List<Long> playerIDList = new ArrayList<>();
                    for (String t : listTeamID) {
                        TeamCache tc = teamCacheMgr.getOrLoadTeamCache(t);
                        if (tc != null) {
                            playerIDList.addAll(tc.players);
                        }
                    }
                    if (playerIDList.size() > 0) {
                        if (notifMgr.setNotifGroupForPlayer(notif, playerIDList)) {
                            return playerIDList.size();
                        } else {
                            log.error("Failed to set notif group for players - notif="+notif+" - list player size="+playerIDList.size());
                        }
                    }
                } else {
                    log.error("Failed to persist notifGroup="+notif);
                }
            } else {
                log.error("No template found for name teamBecomeIncompleteEndPeriod");
            }
        }  else {
            log.warn("Notif is disable ot list teamID empty !");
        }
        return 0;
    }

    /**
     * Send notif to player's team start of period
     * @param listTeamID
     * @param dateExpiration
     * @return
     */
    public int createAndSetNotifGroupTeamIncompleteWarning(List<String> listTeamID, long dateExpiration) {
        if (notifMgr.isNotifEnable() && listTeamID.size() > 0) {
            TextUIData notifTemplate = notifMgr.getTemplate("teamIncompleteWarning");
            if (notifTemplate != null) {
                MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                notif.dateExpiration = dateExpiration;
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                if (notifMgr.persistNotif(notif)) {
                    List<Long> playerIDList = new ArrayList<>();
                    for (String t : listTeamID) {
                        TeamCache tc = teamCacheMgr.getOrLoadTeamCache(t);
                        if (tc != null) {
                            playerIDList.addAll(tc.players);
                        }
                    }
                    if (playerIDList.size() > 0) {
                        if (notifMgr.setNotifGroupForPlayer(notif, playerIDList)) {
                            return playerIDList.size();
                        } else {
                            log.error("Failed to set notif group for players - notif="+notif+" - list player size="+playerIDList.size());
                        }
                    }
                } else {
                    log.error("Failed to persist notifGroup="+notif);
                }
            } else {
                log.error("No template found for name teamIncompleteWarning");
            }
        }  else {
            log.warn("Notif is disable ot list teamID empty !");
        }
        return 0;
    }

    /**
     * Send notif to player's team at championship start
     * @param listTeamID
     * @param dateExpiration
     * @param division
     * @return
     */
    public int createAndSetNotifGroupTeamStartChampionshipForDivision(List<String> listTeamID, long dateExpiration, String division) {
        if (notifMgr.isNotifEnable() && listTeamID.size() > 0) {
            TextUIData notifTemplate = notifMgr.getTemplate("teamStartChampionship");
            if (notifTemplate != null) {
                MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                notif.dateExpiration = dateExpiration;
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                notif.templateParameters.put("DIVISION", MessageNotifMgr.VARIABLE_TEAM_DIVISION_START +division+MessageNotifMgr.VARIABLE_DIVISION_END);
                if (notifMgr.persistNotif(notif)) {
                    List<Long> playerIDList = new ArrayList<>();
                    for (String t : listTeamID) {
                        TeamCache tc = teamCacheMgr.getOrLoadTeamCache(t);
                        if (tc != null) {
                            playerIDList.addAll(tc.players);
                        }
                    }
                    if (playerIDList.size() > 0) {
                        if (notifMgr.setNotifGroupForPlayer(notif, playerIDList)) {
                            return playerIDList.size();
                        } else {
                            log.error("Failed to set notif group for players - notif="+notif+" - list player size="+playerIDList.size());
                        }
                    }
                } else {
                    log.error("Failed to persist notifGroup="+notif);
                }
            } else {
                log.error("No template found for name teamStartChampionship");
            }
        }  else {
            log.warn("Notif is disable ot list teamID empty !");
        }
        return 0;
    }

    /**
     * Send notif to player's team for trend limit (up or down)
     * @param listTeamID
     * @param dateExpiration
     * @param trend
     * @return
     */
    public int createAndSetNotifGroupTeamTrendLimit(List<String> listTeamID, long dateExpiration, int trend) {
        if (notifMgr.isNotifEnable() && listTeamID.size() > 0) {
            TextUIData notifTemplate = null;
            if (trend > 0) {
                notifTemplate = notifMgr.getTemplate("teamPeriodResultTrendUp");
            }
            else if (trend < 0) {
                notifTemplate = notifMgr.getTemplate("teamPeriodResultTrendDown");
            }
            if (notifTemplate != null) {
                MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                notif.dateExpiration = dateExpiration;
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                if (notifMgr.persistNotif(notif)) {
                    List<Long> playerIDList = new ArrayList<>();
                    for (String t : listTeamID) {
                        TeamCache tc = teamCacheMgr.getOrLoadTeamCache(t);
                        if (tc != null) {
                            playerIDList.addAll(tc.players);
                        }
                    }
                    if (playerIDList.size() > 0) {
                        if (notifMgr.setNotifGroupForPlayer(notif, playerIDList)) {
                            return playerIDList.size();
                        } else {
                            log.error("Failed to set notif group for players - notif="+notif+" - list player size="+playerIDList.size());
                        }
                    }
                } else {
                    log.error("Failed to persist notifGroup="+notif);
                }
            } else {
                log.error("No template found for name teamStartChampionship");
            }
        }  else {
            log.warn("Notif is disable ot list teamID empty !");
        }
        return 0;
    }

    /**
     * Send notif to player's team start of period
     * @param listTeamID
     * @param dateExpiration
     * @return
     */
    public int createAndSetNotifGroupStartPeriod(List<String> listTeamID, long dateExpiration) {
        if (notifMgr.isNotifEnable() && listTeamID.size() > 0) {
            TextUIData notifTemplate = notifMgr.getTemplate("teamStartPeriod");
            if (notifTemplate != null) {
                MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                notif.dateExpiration = dateExpiration;
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                if (notifMgr.persistNotif(notif)) {
                    List<Long> playerIDList = new ArrayList<>();
                    for (String t : listTeamID) {
                        TeamCache tc = teamCacheMgr.getOrLoadTeamCache(t);
                        if (tc != null) {
                            playerIDList.addAll(tc.players);
                        }
                    }
                    if (playerIDList.size() > 0) {
                        if (notifMgr.setNotifGroupForPlayer(notif, playerIDList)) {
                            return playerIDList.size();
                        } else {
                            log.error("Failed to set notif group for players - notif="+notif+" - list player size="+playerIDList.size());
                        }
                    }
                } else {
                    log.error("Failed to persist notifGroup="+notif);
                }
            } else {
                log.error("No template found for name teamStartPeriod");
            }
        }  else {
            log.warn("Notif is disable ot list teamID empty !");
        }
        return 0;
    }

    /**
     * Send notif to players for teams
     * @param newDivision
     * @param previousDivision
     * @param previousPeriodID
     * @param trend
     * @param dateExpiration
     * @param listTeamID
     * @return
     */
    public int createAndSetNotifGroupPeriodEnd(String newDivision, String previousDivision, String previousPeriodID, int trend, long dateExpiration, List<String> listTeamID) {
        if (notifMgr.isNotifEnable() && listTeamID.size() > 0) {
            TextUIData notifTemplate = null;
            if (trend == 0) {
                // case of last division
                if (previousDivision.equals(TourTeamMgr.DIVISION_TAB[TourTeamMgr.DIVISION_TAB.length-1])) {
                    notifTemplate = notifMgr.getTemplate("teamPeriodResultStayInLastDivision");
                } else {
                    notifTemplate = notifMgr.getTemplate("teamPeriodResultMaintain");
                }
            } else if (trend > 0) {
                notifTemplate = notifMgr.getTemplate("teamPeriodResultUp");
            } else if (trend < 0) {
                notifTemplate = notifMgr.getTemplate("teamPeriodResultDown");
            }
            if (notifTemplate != null) {
                MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_TEAM;
                notif.dateExpiration = dateExpiration;
                if(trend < 0){
                    notif.templateParameters.put("NEW_DIVISION", MessageNotifMgr.VARIABLE_TEAM_DIVISION_START +previousDivision+MessageNotifMgr.VARIABLE_DIVISION_END);
                }else{
                    notif.templateParameters.put("NEW_DIVISION", MessageNotifMgr.VARIABLE_TEAM_DIVISION_START +newDivision+MessageNotifMgr.VARIABLE_DIVISION_END);
                }
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_TEAM_TYPE, MessageNotifMgr.NOTIF_ACTION_TEAM_TYPE_RANKING);
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, "TEAM_PREVIOUS_RANKING");
                notif.addExtraField("DIVISION", previousDivision);
                notif.addExtraField("PERIODID", previousPeriodID);
                if (notifMgr.persistNotif(notif)) {
                    List<Long> playerIDList = new ArrayList<>();
                    for (String t : listTeamID) {
                        TeamCache tc = teamCacheMgr.getOrLoadTeamCache(t);
                        if (tc != null) {
                            playerIDList.addAll(tc.players);
                        }
                    }
                    if (playerIDList.size() > 0) {
                        if (notifMgr.setNotifGroupForPlayer(notif, playerIDList)) {
                            return playerIDList.size();
                        } else {
                            log.error("Failed to set notif group for players - notif="+notif+" - list player size="+playerIDList.size());
                        }
                    }
                } else {
                    log.error("Failed to persist notifGroup="+notif);
                }
            } else {
                log.error("No template found for name=teamPeriodResultMaintain or teamPeriodResultUp or teamPeriodResultDown - trend="+trend);
            }
        } else {
            log.warn("Notif is disable ot list teamID empty !");
        }
        return 0;
    }

    /**
     * Update field lastPeriodWithCompleteTeam for each team with nb players >= nb group
     * @param division
     * @return
     */
    public void updateLastPeriodWithCompleteTeamForDivision(String division, int nbPlayersForCompleteTeam, String periodID){
        Query q = new Query();
        q.addCriteria(Criteria.where("nbPlayers").gte(nbPlayersForCompleteTeam));
        q.addCriteria(Criteria.where("division").is(division));
        Update u = new Update().set("lastPeriodWithCompleteTeam", periodID);
        mongoTemplate.updateMulti(q, u, Team.class);
    }

    /**
     * Teams ready to integrate divisions : division=DNO and nbPlayers >= getNbLeadPlayers
     * @return
     */
    public List<Team> listTeamReadyToIntegrateDivisions() {
        return mongoTemplate.find(Query.query(Criteria.where("division").is(TourTeamMgr.DIVISION_NO).andOperator(Criteria.where("nbPlayers").gte(getNbLeadPlayers()))), Team.class);
    }

    /**
     * Count teams ready to integrate divisions : division=DNO and nbPlayers >= getNbLeadPlayers
     * @return
     */
    public int countTeamsReadyToIntegrateDivisions() {
        return (int)mongoTemplate.count(Query.query(Criteria.where("division").is(TourTeamMgr.DIVISION_NO).andOperator(Criteria.where("nbPlayers").gte(getNbLeadPlayers()))), Team.class);
    }

    /**
     * List of teams for division not DNO with nbPlayers < nbLeadPlayers and lastPeriodWithCompleteTeam=periodID (if periodID not null)
     * @param periodID
     * @return
     */
    public List<Team> listIncompleteTeamsWithDivision(String periodID) {
        Criteria cNbLeadPlayers = Criteria.where("nbPlayers").lt(getNbLeadPlayers());
        Criteria cDivision = Criteria.where("division").ne(TourTeamMgr.DIVISION_NO);
        List<Criteria> listCriteria = new ArrayList<>();
        listCriteria.add(cDivision);
        listCriteria.add(cNbLeadPlayers);
        if (periodID != null) {
            Criteria cLastPeriodWithCompleteTeam = Criteria.where("lastPeriodWithCompleteTeam").is(periodID);
            listCriteria.add(cLastPeriodWithCompleteTeam);
        }
        Query q = new Query().addCriteria(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
        return mongoTemplate.find(q, Team.class);
    }

    /**
     * List of team for division with nbPlayers < nbLeadPlayers and lastPeriodWithCompleteTeam=periodID (if periodID not null)
     * @param division
     * @param periodID
     * @return
     */
    public List<Team> listIncompleteTeamsForDivision(String division, String periodID){
        Criteria cNbLeadPlayers = Criteria.where("nbPlayers").lt(getNbLeadPlayers());
        Criteria cDivision = Criteria.where("division").is(division);
        List<Criteria> listCriteria = new ArrayList<>();
        listCriteria.add(cDivision);
        listCriteria.add(cNbLeadPlayers);
        if (periodID != null) {
            Criteria cLastPeriodWithCompleteTeam = Criteria.where("lastPeriodWithCompleteTeam").is(periodID);
            listCriteria.add(cLastPeriodWithCompleteTeam);
        }
        Query q = new Query().addCriteria(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
        return mongoTemplate.find(q, Team.class);
    }

    /**
     * Update field division for eachs with ID in listTeamID
     * @param listTeamID
     * @param division
     * @return
     */
    public void setTeamsDivision(List<String> listTeamID, String division) {
        if (listTeamID != null && listTeamID.size() > 0) {
            Query q = new Query(Criteria.where("_id").in(listTeamID));
            Update u = new Update().set("division", division);
            mongoTemplate.updateMulti(q, u, Team.class);
        }
    }
}
