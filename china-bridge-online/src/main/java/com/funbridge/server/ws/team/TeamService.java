package com.funbridge.server.ws.team;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.team.TeamMgr;
import com.funbridge.server.team.cache.TeamCacheMgr;
import com.funbridge.server.team.data.Team;
import com.funbridge.server.tournament.team.TourTeamMgr;
import com.funbridge.server.tournament.team.data.TeamPeriod;
import com.funbridge.server.tournament.team.data.TeamTournament;
import com.funbridge.server.tournament.team.memory.TeamMemTournamentPlayer;
import com.funbridge.server.ws.*;
import com.funbridge.server.ws.team.param.*;
import com.funbridge.server.ws.team.response.*;
import com.funbridge.server.ws.tournament.TournamentServiceRestImpl;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ws.rs.*;
import java.util.List;

/**
 * Created by pserent on 12/10/2016.
 */
@Service(value = "teamService")
@Path(value = "/team")
public class TeamService extends FunbridgeMgr {
    @Resource(name = "teamMgr")
    private TeamMgr teamMgr = null;
    @Resource(name = "tourTeamMgr")
    private TourTeamMgr tourTeamMgr = null;
    @Resource(name = "teamCacheMgr")
    private TeamCacheMgr teamCacheMgr = null;
    @Resource(name = "tournamentService")
    private TournamentServiceRestImpl tournamentService = null;

    /**
     * Call by spring on initialisation of bean
     */
    @PostConstruct
    @Override
    public void init() {
    }

    @PreDestroy
    @Override
    public void destroy() {
    }

    @Override
    public void startUp() {

    }

    @POST
    @Path("/createTeam")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse createTeam(@HeaderParam("sessionID") String sessionID, CreateTeamParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                teamMgr.createTeam(session, param.name, param.countryCode, param.description);
                response.setData(new BooleanResponse(true));

            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/getTeamSummary")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse getTeamSummary(@HeaderParam("sessionID") String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                tourTeamMgr.checkTeamTournamentsEnable();
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID());
                }

                TeamSummaryResponse resp = new TeamSummaryResponse();
                resp.championshipStarted = tourTeamMgr.isChampionshipStarted();
                resp.nbMaxPlayersPerTeam = teamMgr.getTeamSize();
                resp.nbLeadPlayersPerTeam = teamMgr.getNbLeadPlayers();
                resp.nbMessages = teamMgr.getNbMessagesForPlayer(session.getPlayer());
                resp.nbRequests = teamMgr.getNbRequestsForPlayer(session.getPlayer());
                TeamPeriod currentPeriod = tourTeamMgr.getCurrentPeriod();
                if (currentPeriod != null) {
                    resp.nbTourPerPeriod = currentPeriod.getTours().size();
                    resp.listAvailablePeriodID.add(currentPeriod.getID());
                    TeamPeriod previousPeriod = tourTeamMgr.getPreviousPeriod(1);
                    if (previousPeriod != null) {
                        resp.listAvailablePeriodID.add(0, previousPeriod.getID());
                    }

                    resp.period = tourTeamMgr.toWSTeamPeriod(currentPeriod);
                    resp.dateNextTour = currentPeriod.getDateNextTour();
                } else {
                    resp.period = null;
                    resp.tour = null;
                }
                resp.dateNextPeriod = tourTeamMgr.getDateNextPeriod();

                Team team = teamMgr.getTeamForPlayer(session.getPlayer().getID());
                if (team != null) {
                    resp.team = teamMgr.toWSTeam(team, session.getPlayer().getID(), 0);
                    resp.chatroomID = team.getChatroomID();
                    if (resp.championshipStarted) {
                        resp.tour = tourTeamMgr.toWSTeamTour(currentPeriod, team, session.getPlayerCache());
                        // If another player already played the tournament before the asking player became lead, don't return the tour : the player isn't allowed to play
                        if (resp.tour != null && team.getPlayer(session.getPlayer().getID()).isLead()) {
                            List<TeamMemTournamentPlayer> teamResults = tourTeamMgr.getMemoryMgr().getListMemTournamentPlayerForTeam(team.getIDStr());
                            // Loop on results to find the player's group
                            for (TeamMemTournamentPlayer result : teamResults) {
                                if (result.memTour.group.equalsIgnoreCase(team.getPlayer(session.getPlayer().getID()).getGroup())) {
                                    if (result.playerID != session.getPlayer().getID()) {
                                        resp.tour = null;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                // count nb teams with at least one player
                resp.nbTeams = teamCacheMgr.getNbTeamsWithNbPlayersGreater(1);
                response.setData(resp);

            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/getTeam")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse getTeam(@HeaderParam("sessionID") String sessionID, GetTeamParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled() && param != null && param.isValid()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                Team team = teamMgr.findTeamByID(param.teamID);
                if (team == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("No team found with ID=" + param.teamID);
                    }
                    throw new FBWSException(FBExceptionType.TEAM_UNKNOWN_TEAM);
                }
                GetTeamResponse resp = new GetTeamResponse();
                resp.team = teamMgr.toWSTeam(team, session.getPlayer().getID(), -1);
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/listTeams")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse listTeams(@HeaderParam("sessionID") String sessionID, ListTeamsParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled() && param != null && param.isValid()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                ListTeamsResponse resp = teamMgr.listTeams(param.searchMode, param.search, param.countryCode, true, session.getPlayer().getID(), param.offset, param.nbMax);
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/listRequestsForTeam")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse listRequestsForTeam(@HeaderParam("sessionID") String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID());
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                ListRequestsForTeamResponse resp = new ListRequestsForTeamResponse();
                Team team = teamMgr.getTeamForPlayer(session.getPlayer().getID());
                if (team != null && team.isCaptain(session.getPlayer().getID())) {
                    resp.requests = teamMgr.getRequestsForTeam(team.getIDStr());
                } else {
                    log.error("listRequestsForTeam called by a player having no team or not being the captain. playerID=" + session.getPlayer().getID());
                }
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/listRequestsForPlayer")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse listRequestsForPlayer(@HeaderParam("sessionID") String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID());
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                ListRequestsForPlayerResponse resp = new ListRequestsForPlayerResponse();
                Team team = teamMgr.getTeamForPlayer(session.getPlayer().getID());
                if (team == null) {
                    resp.requests = teamMgr.getRequestsForPlayer(session.getPlayer().getID());
                } else {
                    log.error("listRequestsForPlayer called by a player already having a team. playerID=" + session.getPlayer().getID());
                }
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/answerRequest")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse answerRequest(@HeaderParam("sessionID") String sessionID, AnswerRequestParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                BooleanResponse resp = new BooleanResponse(false);
                resp.result = teamMgr.answerRequest(param.requestID, param.accept, session.getPlayer().getID());
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/cancelRequest")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse cancelRequest(@HeaderParam("sessionID") String sessionID, CancelRequestParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                BooleanResponse resp = new BooleanResponse(false);
                resp.result = teamMgr.cancelRequest(param.requestID, session.getPlayer());
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/sendRequestToTeam")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse sendRequestToTeam(@HeaderParam("sessionID") String sessionID, SendRequestToTeamParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                PlayerRequestResponse resp = new PlayerRequestResponse();
                resp.request = teamMgr.sendRequestToTeam(param.teamID, session.getPlayer());
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/sendRequestToPlayer")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse sendRequestToPlayer(@HeaderParam("sessionID") String sessionID, SendRequestToPlayerParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                TeamRequestResponse resp = new TeamRequestResponse();
                resp.request = teamMgr.sendRequestToPlayer(param.playerID, session.getPlayer());
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/leaveTeam")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse leaveTeam(@HeaderParam("sessionID") String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID());
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                BooleanResponse resp = new BooleanResponse(teamMgr.leaveTeam(session));
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/deleteTeam")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse deleteTeam(@HeaderParam("sessionID") String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID());
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                BooleanResponse resp = new BooleanResponse(teamMgr.emptyTeam(session));
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/changeComposition")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse changeComposition(@HeaderParam("sessionID") String sessionID, ChangeCompositionParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                ChangeCompositionResponse resp = new ChangeCompositionResponse();
                resp.listPseudo = teamMgr.changeComposition(session.getPlayer().getID(), param.players);
                if (resp.listPseudo.size() > 0) {
                    resp.result = false;
                }
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/searchPlayer")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse searchPlayer(@HeaderParam("sessionID") String sessionID, SearchPlayerParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                SearchPlayerResponse resp = new SearchPlayerResponse();
                resp.players = teamMgr.searchPlayerFree(param.search, param.friend, param.countryCode, session.getPlayer().getID());
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/removePlayer")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse removePlayer(@HeaderParam("sessionID") String sessionID, RemovePlayerParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled() && param != null && param.isValid()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                response.setData(new BooleanResponse(teamMgr.removePlayer(session.getPlayer().getID(), param.playerID)));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/changeCaptain")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse changeCaptain(@HeaderParam("sessionID") String sessionID, ChangeCaptainParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled() && param != null && param.isValid()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                response.setData(new BooleanResponse(teamMgr.changeCaptain(session, param.playerID)));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/updateTeam")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse updateTeam(@HeaderParam("sessionID") String sessionID, UpdateTeamParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled() && param != null && param.isValid()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                response.setData(new BooleanResponse(teamMgr.updateTeam(session, param.description)));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/playTournament")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse playTournament(@HeaderParam("sessionID") String sessionID, PlayTournamentParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled() && param != null && param.isValid()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                // check if a current game exist in session for a different category
                tournamentService.checkGameInSessionOnPlayTournament(session, Constantes.TOURNAMENT_CATEGORY_TEAM, null, null, 0, false);
                tourTeamMgr.checkPeriodTourValid();
                PlayTournamentResponse resp = new PlayTournamentResponse();
                synchronized (tournamentService.getLockPlayTournament(session.getPlayer().getID())) {
                    resp.tableTournament = tourTeamMgr.playTournament(session, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
                }
                tournamentService.addPlayerSetCategory(Constantes.TOURNAMENT_CATEGORY_TEAM, session.getPlayer().getID());
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/getRanking")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse getRanking(@HeaderParam("sessionID") String sessionID, GetRankingParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                GetRankingResponse resp = new GetRankingResponse();
                if (param.group == null || param.group.isEmpty()) {
                    // TEAM RANKING //
                    int offset = param.offset;
                    // Find the team for asking player
                    Team team = teamMgr.getTeamForPlayer(session.getPlayer().getID());
                    if (team != null) {
                        // Player has a team, let's find its result
                        resp.resultTeam = tourTeamMgr.getTeamResult(team.getIDStr(), param.division, param.periodID, param.tourID);
                        // Define offset to center the ranking on the team of asking player
                        if (resp.resultTeam != null && param.offset == -1) {
                            offset = resp.resultTeam.rank - (param.nbMaxResult / 2);
                        }
                        if (offset < 0) {
                            offset = 0;
                        }
                    }
                    if (offset < 0) offset = 0;
                    resp.teamRanking = tourTeamMgr.getTeamRanking(session, param.division, param.periodID, param.tourID, offset, param.nbMaxResult);
                    resp.totalSize = tourTeamMgr.getTeamRankingSize(param.division, param.periodID, param.tourID);
                    resp.offset = offset;
                } else {
                    // PLAYER RANKING //
                    int offset = param.offset;
                    // Find the player's result
                    TeamTournament tournament = tourTeamMgr.getTournamentForDivisionPeriodTourAndGroup(param.division, param.periodID, param.tourID, param.group);
                    if (tournament != null) {
                        resp.resultPlayer = tourTeamMgr.getWSResultTournamentPlayer(tournament.getIDStr(), session.getPlayerCache(), false);
                        if (resp.resultPlayer != null && param.offset == -1) {
                            offset = resp.resultPlayer.getRank() - (param.nbMaxResult / 2);
                        }
                    }
                    if (offset < 0) {
                        offset = 0;
                    }
                    resp.playerRanking = tourTeamMgr.getPlayerRanking(session, param.division, param.periodID, param.tourID, param.group, offset, param.nbMaxResult);
                    resp.totalSize = tourTeamMgr.getNbPlayersOnTournament(tourTeamMgr.getTournamentForDivisionPeriodTourAndGroup(param.division, param.periodID, param.tourID, param.group), null, false);
                    resp.offset = offset;
                }
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/getTourResults")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse getTourResults(@HeaderParam("sessionID") String sessionID, GetTourResultsParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                GetTourResultsResponse resp = new GetTourResultsResponse();
                resp.results = tourTeamMgr.getTourResultsForTeam(param.teamID, param.tourID);
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @POST
    @Path("/getPeriodResults")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse getPeriodResults(@HeaderParam("sessionID") String sessionID, GetPeriodResultsParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                tourTeamMgr.checkTeamTournamentsEnable();
                GetPeriodResultsResponse resp = new GetPeriodResultsResponse();
                resp.results = tourTeamMgr.getPeriodResultsForTeam(param.teamID, param.periodID);
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Session not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }
}
