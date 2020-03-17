package com.funbridge.server.ws.result;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.engine.ArgineEngineMgr;
import com.funbridge.server.message.data.GenericChatroom;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.texts.TextUIMgr;
import com.funbridge.server.tournament.duel.DuelMgr;
import com.funbridge.server.tournament.federation.FederationMgr;
import com.funbridge.server.tournament.federation.TourFederationMgr;
import com.funbridge.server.tournament.federation.TourFederationStatPeriodMgr;
import com.funbridge.server.tournament.federation.cbo.TourCBOMgr;
import com.funbridge.server.tournament.game.Deal;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.game.Tournament;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.privatetournament.PrivateTournamentMgr;
import com.funbridge.server.tournament.privatetournament.data.PrivateDeal;
import com.funbridge.server.tournament.serie.SerieEasyChallengeMgr;
import com.funbridge.server.tournament.serie.SerieTopChallengeMgr;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.serie.data.*;
import com.funbridge.server.tournament.serie.memory.TourSerieMemPeriodRanking;
import com.funbridge.server.tournament.serie.memory.TourSerieMemTour;
import com.funbridge.server.tournament.team.TourTeamMgr;
import com.funbridge.server.tournament.team.data.TeamDeal;
import com.funbridge.server.tournament.team.data.TeamTournament;
import com.funbridge.server.tournament.timezone.TimezoneMgr;
import com.funbridge.server.tournament.training.TrainingMgr;
import com.funbridge.server.tournament.training.data.TrainingDeal;
import com.funbridge.server.tournament.training.data.TrainingPlayerHisto;
import com.funbridge.server.ws.*;
import com.funbridge.server.ws.player.WSSerieStatus;
import com.funbridge.server.ws.tournament.WSTournament;
import com.gotogames.common.bridge.BridgeBid;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.bridge.GameBridgeRule;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

@Service(value="resultService")
@Scope(value="singleton")
public class ResultServiceRestImpl extends FunbridgeMgr implements ResultServiceRest {
	@Resource(name="playerMgr")
	private PlayerMgr playerMgr = null;
    @Resource(name="timezoneMgr")
    private TimezoneMgr timezoneMgr = null;
    @Resource(name="trainingMgr")
    private TrainingMgr trainingMgr = null;
    @Resource(name="tourSerieMgr")
    private TourSerieMgr tourSerieMgr = null;
    @Resource(name = "textUIMgr")
    private TextUIMgr textUIMgr = null;
    @Resource(name = "playerCacheMgr")
    private PlayerCacheMgr playerCacheMgr = null;
    @Resource(name = "argineEngineMgr")
    private ArgineEngineMgr argineEngineMgr;
    @Resource(name="presenceMgr")
    private PresenceMgr presenceMgr;
    @Resource(name="duelMgr")
    private DuelMgr duelMgr;
    @Resource(name="serieTopChallengeMgr")
    private SerieTopChallengeMgr serieTopChallengeMgr;
    @Resource(name="serieEasyChallengeMgr")
    private SerieEasyChallengeMgr serieEasyChallengeMgr;
    @Resource(name="tourTeamMgr")
    private TourTeamMgr tourTeamMgr;
    @Resource(name="privateTournamentMgr")
    private PrivateTournamentMgr privateTournamentMgr;
    @Resource(name="tourCBOMgr")
    private TourCBOMgr tourCBOMgr;
    @Resource(name = "tourFederationStatPeriodMgr")
    protected TourFederationStatPeriodMgr tourFederationStatPeriodMgr;

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

	@Override
	public FBWSResponse getResultSummaryForReplayDeal(String sessionID,	GetResultSummaryForReplayDealParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetResultSummaryForReplayDeal(session, param));
			} catch (FBWSException e) {
				response.setException(new FBWSExceptionRest(e.getType()));
			} catch (Exception e) {
				log.error("Exception : "+e.getMessage(), e);
				response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
			}
		} else {
            log.warn("SessionID not valid - param="+param+" - sessionID="+sessionID);
			response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
		}
		return response;
	}

	public GetResultSummaryForReplayDealResponse processGetResultSummaryForReplayDeal(FBSession session, GetResultSummaryForReplayDealParam param) throws FBWSException {
	    if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID()+" - param="+param);
        }
        GetResultSummaryForReplayDealResponse resp = new GetResultSummaryForReplayDealResponse();
        if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
            tourSerieMgr.checkPeriodValid();
            resp.resultReplayDealSummary = tourSerieMgr.resultReplaySummary(session, param.dealIDstr);
        }
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
            serieTopChallengeMgr.checkSerieTopChallengeEnable();
            resp.resultReplayDealSummary = serieTopChallengeMgr.resultReplaySummary(session, param.dealIDstr);
        }
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
            serieEasyChallengeMgr.checkSerieEasyChallengeEnable();
            resp.resultReplayDealSummary = serieEasyChallengeMgr.resultReplaySummary(session, param.dealIDstr);
        }
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TRAINING){
            resp.resultReplayDealSummary = trainingMgr.resultReplaySummary(session, param.dealIDstr);
        }
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TIMEZONE){
            resp.resultReplayDealSummary = timezoneMgr.resultReplaySummary(session, param.dealIDstr);
        }
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_DUEL){
            resp.resultReplayDealSummary = duelMgr.resultReplaySummary(session, param.dealIDstr);
        }
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TEAM) {
            tourTeamMgr.checkTeamTournamentsEnable();
            resp.resultReplayDealSummary = tourTeamMgr.resultReplaySummary(session, param.dealIDstr);
        }
        else {
            TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(param.categoryID);
            if (tournamentMgr != null) {
                resp.resultReplayDealSummary = tournamentMgr.resultReplaySummary(session, param.dealIDstr);
            } else {
                log.error("CategoryID not supported - param=" + param);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
        }
        return resp;
    }

	@Override
	public FBWSResponse getResultForDeal(String sessionID, GetResultForDealParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
				response.setData(processGetResultForDeal(session, param));
			} catch (FBWSException e) {
				response.setException(new FBWSExceptionRest(e.getType()));
			} catch (Exception e) {
				log.error("Exception : "+e.getMessage(), e);
				response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
			}
		} else {
			log.warn("SessionID not valid - sessionID=" + sessionID+" - param="+param);
			response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
		}
		return response;
	}

	public GetResultForDealResponse processGetResultForDeal(FBSession session, GetResultForDealParam param) throws FBWSException {
        	    if (log.isDebugEnabled()) {
            log.debug("session="+session+" - param="+param);
        }
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        GetResultForDealResponse resp = new GetResultForDealResponse();

        // if filter enabled => not group
        List<Long> listFollower = null;
        if (param.followed) {
            param.groupByContract = false;

            listFollower = playerMgr.getListPlayerIDLinkFollower(session.getPlayer().getID());
            if (listFollower != null) {
                listFollower.add(session.getPlayer().getID());
            }
            // no offset & nbMax => returns all data
            param.offset = 0;
            param.nbMaxResult = 0;
        }
        switch (param.categoryID) {
            case Constantes.TOURNAMENT_CATEGORY_TRAINING:
                if (param.groupByContract) {
                    resp.resultDealTournament = trainingMgr.getWSResultDealTournamentGroupped(param.dealIDstr, session.getPlayerCache(), true);
                } else {
                    resp.resultDealTournament = trainingMgr.getWSResultDealTournament(param.dealIDstr, session.getPlayerCache(), listFollower, param.offset, param.nbMaxResult);
                }
                break;
            case Constantes.TOURNAMENT_CATEGORY_NEWSERIE:
                tourSerieMgr.checkPeriodValid();
                if (param.groupByContract) {
                    resp.resultDealTournament = tourSerieMgr.resultDealGroup(param.dealIDstr, session.getPlayerCache(), true);
                } else {
                    resp.resultDealTournament = tourSerieMgr.resultDealNotGroup(param.dealIDstr, session.getPlayerCache(), listFollower, param.offset, param.nbMaxResult);
                }
                break;
            case Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE:
                serieTopChallengeMgr.checkSerieTopChallengeEnable();
                if (param.groupByContract) {
                    resp.resultDealTournament = serieTopChallengeMgr.resultDealGroup(param.dealIDstr, session.getPlayerCache(), true);
                } else {
                    resp.resultDealTournament = serieTopChallengeMgr.resultDealNotGroup(param.dealIDstr, session.getPlayerCache(), listFollower, param.offset, param.nbMaxResult);
                }
                break;
            case Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE:
                serieEasyChallengeMgr.checkSerieEasyChallengeEnable();
                if (param.groupByContract) {
                    resp.resultDealTournament = serieEasyChallengeMgr.resultDealGroup(param.dealIDstr, session.getPlayerCache(), true);
                } else {
                    resp.resultDealTournament = serieEasyChallengeMgr.resultDealNotGroup(param.dealIDstr, session.getPlayerCache(), listFollower, param.offset, param.nbMaxResult);
                }
                break;
            case Constantes.TOURNAMENT_CATEGORY_TEAM:
                tourTeamMgr.checkTeamTournamentsEnable();
                if (param.groupByContract) {
                    resp.resultDealTournament = tourTeamMgr.getWSResultDealTournamentGroupped(param.dealIDstr, session.getPlayerCache(), true);
                } else {
                    resp.resultDealTournament = tourTeamMgr.getWSResultDealTournament(param.dealIDstr, session.getPlayerCache(), listFollower, param.offset, param.nbMaxResult);
                }
                break;
            case Constantes.TOURNAMENT_CATEGORY_TIMEZONE:
                if (param.groupByContract) {
                    resp.resultDealTournament = timezoneMgr.getWSResultDealTournamentGroupped(param.dealIDstr, session.getPlayerCache(), true);
                } else {
                    resp.resultDealTournament = timezoneMgr.getWSResultDealTournament(param.dealIDstr, session.getPlayerCache(), listFollower, param.offset, param.nbMaxResult);
                }
                break;
            default:
                TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(param.categoryID);
                if (tournamentMgr != null) {
                    if (param.groupByContract) {
                        resp.resultDealTournament = tournamentMgr.getWSResultDealTournamentGroupped(param.dealIDstr, session.getPlayerCache(), true);
                    } else {
                        resp.resultDealTournament = tournamentMgr.getWSResultDealTournament(param.dealIDstr, session.getPlayerCache(), listFollower, param.offset, param.nbMaxResult);
                    }
                } else {
                    if(param.categoryID != Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER){
                        log.warn("tournament category not supported !! - param=" + param + " - playerID="+session.getPlayer().getID()+" - device="+session.getDeviceType());
                    }
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
        }
        return resp;
    }

	@Override
	public FBWSResponse resetLastResultForPlayer(String sessionID, ResetLastResultForPlayerParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processResetLastResultForPlayer(session, param));
			} catch (FBWSException e) {
				response.setException(new FBWSExceptionRest(e.getType()));
			} catch (Exception e) {
				log.error("Exception : "+e.getMessage(), e);
				response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
			}
		} else {
            log.warn("SessionID not valid - param="+param+" - sessionID="+sessionID);
			response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
		}
		return response;
	}

	public ResetLastResultForPlayerResponse processResetLastResultForPlayer(FBSession session, ResetLastResultForPlayerParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID()+" - param="+param);
        }
        // result type
        if (param.resultType != Constantes.TOURNAMENT_RESULT_PAIRE && param.resultType != Constantes.TOURNAMENT_RESULT_IMP) {
            // by default IMP
            param.resultType = Constantes.TOURNAMENT_RESULT_IMP;
        }
        trainingMgr.resetPlayerHisto(session.getPlayer().getID(), param.resultType);
        ResetLastResultForPlayerResponse resp = new ResetLastResultForPlayerResponse();
        resp.status = true;
        return resp;
    }

    public FBWSResponse getTournamentArchives(String sessionID,	GetTournamentArchivesParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetTournamentArchives(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param="+param+" - sessionID="+sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetTournamentArchivesResponse processGetTournamentArchives(FBSession session, GetTournamentArchivesParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID()+" - param="+param);
        }
        if (param.count <= 0) {
            log.error("Invalid parameter : count=" + param.count);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        GetTournamentArchivesResponse resp = new GetTournamentArchivesResponse();
        resp.offset = param.offset;
        // TRAINING
        if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
            ResultListTournamentArchive rlta = trainingMgr.listTournamentArchive(session, param.offset, param.count);
            resp.totalSize = rlta.nbTotal;
            resp.archives = rlta.archives;
        }
        // SERIE
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
            ResultListTournamentArchive rlta = tourSerieMgr.listTournamentArchive(session.getPlayer().getID(), session.getSerie(), param.offset, param.count);
            resp.totalSize = rlta.nbTotal;
            resp.archives = rlta.archives;
        }
        // SERIE TOP CHALLENGE
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
            ResultListTournamentArchive rlta = serieTopChallengeMgr.listTournamentArchive(session.getPlayer().getID(), param.offset, param.count);
            resp.totalSize = rlta.nbTotal;
            resp.archives = rlta.archives;
        }
        // SERIE EASY CHALLENGE
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
            ResultListTournamentArchive rlta = serieEasyChallengeMgr.listTournamentArchive(session.getPlayer().getID(), param.offset, param.count);
            resp.totalSize = rlta.nbTotal;
            resp.archives = rlta.archives;
        }
        // TEAM
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TEAM) {
            ResultListTournamentArchive rlta = tourTeamMgr.listTournamentArchive(session.getPlayer().getID(), param.offset, param.count);
            resp.totalSize = rlta.nbTotal;
            resp.archives = rlta.archives;
        }
        // TIMEZONE
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
            ResultListTournamentArchive rlta = timezoneMgr.listTournamentArchive(session, param.offset, param.count);
            resp.totalSize = rlta.nbTotal;
            resp.archives = rlta.archives;
        }
        else {
            TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(param.categoryID);
            if (tournamentMgr != null) {
                ResultListTournamentArchive rlta = tournamentMgr.listTournamentArchive(session, param.offset, param.count);
                resp.totalSize = rlta.nbTotal;
                resp.archives = rlta.archives;
            } else {
                log.error("Method not supported for this category ! param=" + param + " - session=" + session);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        return resp;
    }

	@Override
	public FBWSResponse getResultTournamentArchiveForCategory(String sessionID,	GetResultTournamentArchiveForCategoryParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetResultTournamentArchiveForCategory(session, param));
			} catch (FBWSException e) {
				response.setException(new FBWSExceptionRest(e.getType()));
			} catch (Exception e) {
				log.error("Exception : "+e.getMessage(), e);
				response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
			}
		} else {
            log.warn("SessionID not valid - param="+param+" - sessionID="+sessionID);
			response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
		}
		return response;
	}

	public GetResultTournamentArchiveForCategoryResponse processGetResultTournamentArchiveForCategory(FBSession session, GetResultTournamentArchiveForCategoryParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (param.count <= 0) {
            log.error("Invalid parameter : count=" + param.count);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID()+" - param="+param);
        }
        GetResultTournamentArchiveForCategoryResponse resp = new GetResultTournamentArchiveForCategoryResponse();
        if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
            resp.resultArchive = trainingMgr.getWSResultArchiveTournament(session.getPlayerCache(), param.offset, param.count);
        }
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
            resp.resultArchive = timezoneMgr.getWSResultArchiveTournament(session.getPlayerCache(), param.offset, param.count);
        }
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
            tourSerieMgr.checkPeriodValid();
            resp.resultArchive = tourSerieMgr.resultArchiveTournament(session.getPlayerCache(), session.getSerie(), param.offset, param.count);
        }
        else {
            log.error("Method not supported for this category ! param="+param+" - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        return resp;
    }

	@Override
	public FBWSResponse getResultDealForTournament(String sessionID, GetResultDealForTournamentParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetResultDealForTournament(session, param));
			} catch (FBWSException e) {
				response.setException(new FBWSExceptionRest(e.getType()));
			} catch (Exception e) {
				log.error("Exception : "+e.getMessage(), e);
				response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
			}
		} else {
            log.warn("SessionID not valid - param="+param+" - sessionID="+sessionID);
			response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
		}
		return response;
	}

	public GetResultDealForTournamentResponse processGetResultDealForTournament(FBSession session, GetResultDealForTournamentParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID()+" - param="+param);
        }
        GetResultDealForTournamentResponse resp = new GetResultDealForTournamentResponse();
        if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TRAINING){
            Tournament tour = trainingMgr.getTournament(param.tournamentIDstr);
            if (tour != null) {
                resp.resultDealTournament = new WSResultDealTournament();
                resp.resultDealTournament.listResultDeal = trainingMgr.resultListDealForTournamentForPlayer(tour, session.getPlayer().getID());
                resp.resultDealTournament.tournament = trainingMgr.toWSTournament(tour, session.getPlayerCache());
            }
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
            tourSerieMgr.checkPeriodValid();
            TourSerieTournament tour = tourSerieMgr.getTournament(param.tournamentIDstr);
            if (tour == null) {
                log.error("Tournament not found : tournamentIDstr = "+param.tournamentIDstr);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            resp.resultDealTournament = new WSResultDealTournament();
            resp.resultDealTournament.listResultDeal = tourSerieMgr.resultListDealForTournamentForPlayer(tour, session.getPlayer().getID());
            resp.resultDealTournament.tournament = tourSerieMgr.toWSTournament(tour, session.getPlayerCache());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
            serieTopChallengeMgr.checkSerieTopChallengeEnable();
            TourSerieTournament tour = tourSerieMgr.getTournament(param.tournamentIDstr);
            if (tour == null) {
                log.error("Tournament not found : tournamentIDstr = "+param.tournamentIDstr);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            resp.resultDealTournament = new WSResultDealTournament();
            resp.resultDealTournament.listResultDeal = serieTopChallengeMgr.resultListDealForTournamentForPlayer(tour, session.getPlayer().getID());
            resp.resultDealTournament.tournament = serieTopChallengeMgr.serieTournamentToWS(tour, session.getPlayerCache());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
            serieEasyChallengeMgr.checkSerieEasyChallengeEnable();
            TourSerieTournament tour = tourSerieMgr.getTournament(param.tournamentIDstr);
            if (tour == null) {
                log.error("Tournament not found : tournamentIDstr = "+param.tournamentIDstr);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            resp.resultDealTournament = new WSResultDealTournament();
            resp.resultDealTournament.listResultDeal = serieEasyChallengeMgr.resultListDealForTournamentForPlayer(tour, session.getPlayer().getID());
            resp.resultDealTournament.tournament = serieEasyChallengeMgr.serieTournamentToWS(tour, session.getPlayerCache());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TEAM) {
            tourTeamMgr.checkTeamTournamentsEnable();
            TeamTournament tour = tourTeamMgr.getTournament(param.tournamentIDstr);
            if (tour == null) {
                log.error("Tournament not found : tourID = "+param.tournamentIDstr);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            resp.resultDealTournament = new WSResultDealTournament();
            resp.resultDealTournament.listResultDeal = tourTeamMgr.resultListDealForTournamentForPlayer(tour, session.getPlayer().getID());
            resp.resultDealTournament.tournament = tourTeamMgr.toWSTournament(tour, session.getPlayerCache());
        }
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
            Tournament tour = timezoneMgr.getTournament(param.tournamentIDstr);
            if (tour == null) {
                log.error("Tournament not found : tourID = "+param.tournamentIDstr);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            resp.resultDealTournament = new WSResultDealTournament();
            resp.resultDealTournament.listResultDeal = timezoneMgr.resultListDealForTournamentForPlayer(tour, session.getPlayer().getID());
            resp.resultDealTournament.tournament = timezoneMgr.toWSTournament(tour, session.getPlayerCache());
        }
        else {
            TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(param.categoryID);
            if (tournamentMgr != null) {
                Tournament tour = tournamentMgr.getTournament(param.tournamentIDstr);
                if (tour == null) {
                    log.error("Tournament not found : tourID = "+param.tournamentIDstr);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                resp.resultDealTournament = new WSResultDealTournament();
                resp.resultDealTournament.listResultDeal = tournamentMgr.resultListDealForTournamentForPlayer(tour, session.getPlayer().getID());
                resp.resultDealTournament.tournament = tournamentMgr.toWSTournament(tour, session.getPlayerCache());
            } else {
                log.error("Tournament category not supported ! - param=" + param);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        return resp;
    }

	@Override
	public FBWSResponse getResultForTournament(String sessionID, GetResultForTournamentParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetResultForTournament(session, param));
			} catch (FBWSException e) {
				response.setException(new FBWSExceptionRest(e.getType()));
			} catch (Exception e) {
				log.error("Exception : "+e.getMessage(), e);
				response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
			}
		} else {
			log.warn("SessionID not valid param="+param+" - sessionID="+sessionID);
			response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
		}
		return response;
	}

	public GetResultForTournamentResponse processGetResultForTournament(FBSession session, GetResultForTournamentParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID()+" - param="+param);
        }
        long tournamentID = Constantes.getIDLongValue(param.tournamentIDstr, param.tournamentID);
        GetResultForTournamentResponse resp = new GetResultForTournamentResponse();
        List<Long> listFollower = null;
        if (param.followed) {
            listFollower = playerMgr.getListPlayerIDLinkFollower(session.getPlayer().getID());
            if (listFollower != null) {
                listFollower.add(session.getPlayer().getID());
            }
            // TODO Fix bug compute offset with param followed
            if (param.offset == -1) {
                param.offset = 0;
            }
        }
        //----------------------
        // TRAINING
        if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
            Tournament tournament = trainingMgr.getTournament(param.tournamentIDstr);
            if (tournament != null) {
                resp.resultTournament = new WSResultTournament();
                resp.resultTournament.listResultPlayer = trainingMgr.getListWSResultTournamentPlayer(tournament, param.offset, param.nbMaxResult, listFollower, session.getPlayer().getID(), param.orderFinished, resp.resultTournament.resultPlayer);
                resp.resultTournament.resultPlayer = trainingMgr.getWSResultTournamentPlayer(tournament.getIDStr(), session.getPlayerCache(), param.orderFinished);
                resp.resultTournament.offset = param.offset;
                if (resp.resultTournament.resultPlayer != null) {
                    if (listFollower != null) {
                        if (tournament.isFinished()) {
                            resp.resultTournament.totalSize = tournament.getNbPlayers();
                        } else {
                            resp.resultTournament.totalSize = trainingMgr.getMemoryMgr().getNbPlayerOnTournament(tournament.getIDStr(), listFollower, param.orderFinished);
                        }
                    } else {
                        resp.resultTournament.totalSize = resp.resultTournament.resultPlayer.getNbTotalPlayer();
                    }
                } else {
                    if (tournament.isFinished()) {
                        resp.resultTournament.totalSize = tournament.getNbPlayers();
                    } else {
                        resp.resultTournament.totalSize = trainingMgr.getMemoryMgr().getNbPlayerOnTournament(tournament.getIDStr(), listFollower, param.orderFinished);
                    }
                }
            }
        }
        //----------------------
        // TIMEZONE
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
            Tournament tour = timezoneMgr.getTournament(param.tournamentIDstr);
            if (tour == null) {
                log.error("Tournament not found : tourID = "+tournamentID);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            resp.resultTournament = new WSResultTournament();
            resp.resultTournament.resultPlayer = timezoneMgr.getWSResultTournamentPlayer(tour.getIDStr(), session.getPlayerCache(), param.orderFinished);
            int offset = param.offset;
            if (resp.resultTournament.resultPlayer != null && param.offset == -1) {
                if (tour.isFinished()) {
                    offset = resp.resultTournament.resultPlayer.getRank() - (param.nbMaxResult / 2);
                } else {
                    if (param.orderFinished) {
                        offset = resp.resultTournament.resultPlayer.getNbPlayerFinishWithBestResult() - (param.nbMaxResult / 2);
                    } else {
                        offset = resp.resultTournament.resultPlayer.getRankHidden() - (param.nbMaxResult / 2);
                    }
                }
            }
            if (offset < 0) {
                offset = 0;
            }
            resp.resultTournament.listResultPlayer = timezoneMgr.getListWSResultTournamentPlayer(tour, offset, param.nbMaxResult, listFollower, session.getPlayer().getID(), param.orderFinished, resp.resultTournament.resultPlayer);
            resp.resultTournament.offset = offset;
            resp.resultTournament.totalSize = timezoneMgr.getNbPlayersOnTournament(tour, listFollower, param.orderFinished);
            if (resp.resultTournament.resultPlayer != null) {
                if (param.orderFinished && resp.resultTournament.resultPlayer.getNbDealPlayed() < tour.getNbDealsToPlay()) {
                    // player has not yet played all deals but its result is included
                    resp.resultTournament.totalSize++;
                }
            }
        }
        //----------------------
        // SERIE
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
            tourSerieMgr.checkPeriodValid();
            TourSerieTournament tour = tourSerieMgr.getTournament(param.tournamentIDstr);
            if (tour == null) {
                log.error("Tournament not found : tourID = "+param.tournamentIDstr);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            resp.resultTournament = new WSResultTournament();
            resp.resultTournament.resultPlayer = tourSerieMgr.resultPlayerOnTournament(tour, session.getPlayerCache(), param.orderFinished);
            int offset = param.offset;
            if (resp.resultTournament.resultPlayer != null && param.offset == -1) {
                if (tour.isFinished()) {
                    offset = resp.resultTournament.resultPlayer.getRank() - (param.nbMaxResult / 2);
                } else {
                    if (param.orderFinished) {
                        offset = resp.resultTournament.resultPlayer.getNbPlayerFinishWithBestResult() - (param.nbMaxResult / 2);
                    } else {
                        offset = resp.resultTournament.resultPlayer.getRankHidden() - (param.nbMaxResult / 2);
                    }
                }
            }
            if (offset < 0) {
                offset = 0;
            }
            resp.resultTournament.listResultPlayer = tourSerieMgr.resultListTournament(tour, offset, param.nbMaxResult, listFollower, session.getPlayer().getID(), param.orderFinished, resp.resultTournament.resultPlayer);
            resp.resultTournament.offset = offset;
            resp.resultTournament.totalSize = tourSerieMgr.getNbPlayerOnTournament(tour, listFollower, param.orderFinished);
            if (param.orderFinished && resp.resultTournament.resultPlayer != null && resp.resultTournament.resultPlayer.getNbDealPlayed() < tour.getNbDeals()) {
                // player has not yet played all deals but its result is included
                resp.resultTournament.totalSize++;
            }
        }
        //----------------------
        // SERIE TOP CHALLENGE
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
            serieTopChallengeMgr.checkSerieTopChallengeEnable();
            TourSerieTournament tour = tourSerieMgr.getTournament(param.tournamentIDstr);
            if (tour == null) {
                log.error("Tournament not found : tourID = "+param.tournamentIDstr);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            resp.resultTournament = new WSResultTournament();
            SerieTopChallengeTournamentPlayer tournamentPlayer = serieTopChallengeMgr.getTournamentPlayer(session.getPlayer().getID(), tour.getIDStr());
            if (tournamentPlayer != null) {
                resp.resultTournament.resultPlayer = tournamentPlayer.toWSResultTournamentPlayer(session.getPlayerCache(), tour, session.getPlayer().getID());
            }
            int offset = param.offset;
            if (resp.resultTournament.resultPlayer != null && param.offset == -1) {
                if (tour.isFinished()) {
                    offset = resp.resultTournament.resultPlayer.getRank() - (param.nbMaxResult / 2);
                } else {
                    offset = resp.resultTournament.resultPlayer.getNbPlayerFinishWithBestResult() - (param.nbMaxResult / 2);
                }
            }
            if (offset < 0) {
                offset = 0;
            }
            resp.resultTournament.listResultPlayer = serieTopChallengeMgr.resultListTournament(tour, offset, param.nbMaxResult, listFollower, session.getPlayer().getID(), resp.resultTournament.resultPlayer);
            resp.resultTournament.offset = offset;
            resp.resultTournament.totalSize = tourSerieMgr.getNbPlayerOnTournament(tour, listFollower, true);
            resp.resultTournament.totalSize++;
        }
        //----------------------
        // SERIE EASY CHALLENGE
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
            serieEasyChallengeMgr.checkSerieEasyChallengeEnable();
            TourSerieTournament tour = tourSerieMgr.getTournament(param.tournamentIDstr);
            if (tour == null) {
                log.error("Tournament not found : tourID = "+param.tournamentIDstr);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            resp.resultTournament = new WSResultTournament();
            SerieEasyChallengeTournamentPlayer tournamentPlayer = serieEasyChallengeMgr.getTournamentPlayer(tour.getIDStr(), session.getPlayer().getID());
            if (tournamentPlayer != null) {
                resp.resultTournament.resultPlayer = tournamentPlayer.toWSResultTournamentPlayer(session.getPlayerCache(), tour, session.getPlayer().getID());
            }
            int offset = param.offset;
            if (resp.resultTournament.resultPlayer != null && param.offset == -1) {
                if (tour.isFinished()) {
                    offset = resp.resultTournament.resultPlayer.getRank() - (param.nbMaxResult / 2);
                } else {
                    offset = resp.resultTournament.resultPlayer.getNbPlayerFinishWithBestResult() - (param.nbMaxResult / 2);
                }
            }
            if (offset < 0) {
                offset = 0;
            }
            resp.resultTournament.listResultPlayer = serieEasyChallengeMgr.resultListTournament(tour, offset, param.nbMaxResult, listFollower, session.getPlayer().getID(), resp.resultTournament.resultPlayer);
            resp.resultTournament.offset = offset;
            resp.resultTournament.totalSize = tourSerieMgr.getNbPlayerOnTournament(tour, listFollower, true);
            resp.resultTournament.totalSize++;
        }
        //----------------------
        // TEAM
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TEAM) {
            TeamTournament tour = tourTeamMgr.getTournament(param.tournamentIDstr);
            if (tour == null) {
                log.error("Tournament not found : tourID = "+param.tournamentIDstr);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            resp.resultTournament = new WSResultTournament();
            resp.resultTournament.resultPlayer = tourTeamMgr.getWSResultTournamentPlayer(tour.getIDStr(), session.getPlayerCache(), param.orderFinished);
            int offset = param.offset;
            if (resp.resultTournament.resultPlayer != null && param.offset == -1) {
                if (tour.isFinished()) {
                    offset = resp.resultTournament.resultPlayer.getRank() - (param.nbMaxResult / 2);
                } else {
                    if (param.orderFinished) {
                        offset = resp.resultTournament.resultPlayer.getNbPlayerFinishWithBestResult() - (param.nbMaxResult / 2);
                    } else {
                        offset = resp.resultTournament.resultPlayer.getRankHidden() - (param.nbMaxResult / 2);
                    }
                }
            }
            if (offset < 0) {
                offset = 0;
            }
            resp.resultTournament.listResultPlayer = tourTeamMgr.getListWSResultTournamentPlayer(tour, offset, param.nbMaxResult, listFollower, session.getPlayer().getID(), param.orderFinished, resp.resultTournament.resultPlayer);
            resp.resultTournament.offset = offset;
            resp.resultTournament.totalSize = tourTeamMgr.getNbPlayersOnTournament(tour, listFollower, param.orderFinished);
            if (param.orderFinished && resp.resultTournament.resultPlayer != null && resp.resultTournament.resultPlayer.getNbDealPlayed() < tour.getNbDeals()) {
                // player has not yet played all deals but its result is included
                resp.resultTournament.totalSize++;
            }
        }
        //----------------------
        // OTHER TOURNAMENTS
        else {
            TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(param.categoryID);
            if (tournamentMgr != null) {
                Tournament tour = tournamentMgr.getTournament(param.tournamentIDstr);
                if (tour == null) {
                    log.error("Tournament not found : tourID = "+tournamentID);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                resp.resultTournament = new WSResultTournament();
                resp.resultTournament.resultPlayer = tournamentMgr.getWSResultTournamentPlayer(tour.getIDStr(), session.getPlayerCache(), param.orderFinished);
                int offset = param.offset;
                if (resp.resultTournament.resultPlayer != null && param.offset == -1) {
                    if (tour.isFinished()) {
                        offset = resp.resultTournament.resultPlayer.getRank() - (param.nbMaxResult / 2);
                    } else {
                        if (param.orderFinished) {
                            offset = resp.resultTournament.resultPlayer.getNbPlayerFinishWithBestResult() - (param.nbMaxResult / 2);
                        } else {
                            offset = resp.resultTournament.resultPlayer.getRankHidden() - (param.nbMaxResult / 2);
                        }
                    }
                }
                if (offset < 0) {
                    offset = 0;
                }
                resp.resultTournament.listResultPlayer = tournamentMgr.getListWSResultTournamentPlayer(tour, offset, param.nbMaxResult, listFollower, session.getPlayer().getID(), param.orderFinished, resp.resultTournament.resultPlayer);
                resp.resultTournament.offset = offset;
                resp.resultTournament.totalSize = tournamentMgr.getNbPlayersOnTournament(tour, listFollower, param.orderFinished);
                if (resp.resultTournament.resultPlayer != null) {
                    if (param.orderFinished && resp.resultTournament.resultPlayer.getNbDealPlayed() < tour.getNbDealsToPlay()) {
                        // player has not yet played all deals but its result is included
                        resp.resultTournament.totalSize++;
                    }
                }
            } else {
                log.error("Tournament category not supported - param=" + param);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        return resp;
    }

    @Override
    public FBWSResponse getSerieSummary2(String sessionID, GetSerieSummaryParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID="+session.getLoginID());
                }
				response.setData(processGetSerieSummary(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid - param="+param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetSerieSummary2Response processGetSerieSummary(FBSession session, GetSerieSummaryParam param) throws FBWSException {
        tourSerieMgr.checkPeriodValid();
        WSSerieSummary2 resSerie = new WSSerieSummary2();
        Player player = session.getPlayer();
        String serie = session.getSerie();
        TourSeriePeriod period = tourSerieMgr.getCurrentPeriod();
        resSerie.datePeriodStart = period.getTsDateStart();
        resSerie.datePeriodEnd = period.getTsDateEnd();
        resSerie.playerSerie = tourSerieMgr.buildWSSerie(session.getPlayerCache());
        TourSerieMemPeriodRanking rankSerie = tourSerieMgr.getSerieRanking(serie);
        if (rankSerie == null) {
            log.error("No ranking found for serie="+serie);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        WSSerieStatus serieStatus = tourSerieMgr.buildSerieStatusForPlayer(session.getPlayerCache());
        resSerie.nbTournamentPlayed = serieStatus.nbTournamentPlayed;
        resSerie.rank = serieStatus.rank;
        resSerie.resultPlayer = serieStatus.result;
        resSerie.trend = serieStatus.trend;
        resSerie.trendSerie = TourSerieMgr.computeSerieEvolution(serie, serieStatus.trend, true);
        if (tourSerieMgr.isPlayerReserve(serie, session.getPlayerCache().serieLastPeriodPlayed, true)) {
            resSerie.trendSerie = TourSerieMgr.buildSerieReserve(resSerie.trendSerie);
        }
        resSerie.playerOffset = serieStatus.rank;
        // no rank => offset = nbPlayerActive in serie + 1
        if (resSerie.playerOffset == -1) {
            // only if player is not in reserve
            if (!tourSerieMgr.isPlayerReserve(serie, session.getPlayerCache().serieLastPeriodPlayed, false)) {
                if (!serie.equals(TourSerieMgr.SERIE_NC) && rankSerie.getNbPlayerActive(null, null) > 0) {
                    resSerie.playerOffset = rankSerie.getNbPlayerActive(null, null) + 1;
                } else {
                    resSerie.playerOffset = 0;
                }
            }
        }
        resSerie.nbPlayerSerie = serieStatus.nbPlayerInSerie;
        resSerie.trendText = textUIMgr.getTextSerieTrend(player.getDisplayLang(),
                serie, resSerie.trend, resSerie.nbTournamentPlayed, session.getPlayerCache().serieLastPeriodPlayed,
                rankSerie.thresholdNbUp, rankSerie.thresholdNbDown, rankSerie.thresholdResultUp, rankSerie.thresholdResultDown);
        resSerie.thresholdResultDown = rankSerie.thresholdResultDown;
        resSerie.thresholdResultUp = rankSerie.thresholdResultUp;
        if (resSerie.nbTournamentPlayed < tourSerieMgr.getBonusNbTour()) {
            resSerie.bonusText = textUIMgr.getTextSerieBonusFirst(player.getDisplayLang(), tourSerieMgr.getBonusNbTour() - resSerie.nbTournamentPlayed);
        } else {
            if (tourSerieMgr.getBonusNbTour() > 0) {
                resSerie.bonusText = textUIMgr.getTextSerieBonusNext(player.getDisplayLang(),
                        (resSerie.nbTournamentPlayed / tourSerieMgr.getBonusNbTour()) * tourSerieMgr.getBonusRemove(),
                        tourSerieMgr.getBonusRemove(),
                        tourSerieMgr.getBonusNbTour());
            }
        }
        resSerie.bonusDescriptionText = textUIMgr.getTextSerieBonusDescription(player.getDisplayLang(), tourSerieMgr.getBonusRemove(), tourSerieMgr.getBonusNbTour());
        if (param.rankingExtract) {
            resSerie.rankingExtract = tourSerieMgr.getWSRankingExtractForSerie(serie,
                    tourSerieMgr.buildWSRankingForPlayer(player.getID(), rankSerie.getPlayerRank(player.getID()), session.getPlayerCache(), player.getID()),
                    tourSerieMgr.isPlayerReserve(serie, session.getPlayerCache().serieLastPeriodPlayed, false),
                    param.nbRankingItems);
        }
        TourSerieMemTour memTourInProgress = tourSerieMgr.getMemTourInProgressForPlayer(session);
        if (memTourInProgress != null) {
            TourSerieTournament tourInProgress = tourSerieMgr.getTournament(memTourInProgress.tourID);
            WSTournament wsTour = tourInProgress.toWS();
            wsTour.remainingTime = tourInProgress.getTsDateEnd() - System.currentTimeMillis();
            wsTour.nbTotalPlayer = memTourInProgress.getNbPlayersForRanking();
            wsTour.resultPlayer = memTourInProgress.getWSResultPlayer(session.getPlayerCache(), true);
            if (wsTour.resultPlayer != null) {
                wsTour.playerOffset = wsTour.resultPlayer.getRank();
                wsTour.currentDealIndex = memTourInProgress.getCurrentDealForPlayer(session.getPlayer().getID());
            }
            resSerie.currentTournament = wsTour;
        }
        resSerie.previousPlayerSerie = tourSerieMgr.getSerieOnPreviousPeriodForPlayer(player.getID());
        if (resSerie.previousPlayerSerie == null) {
            resSerie.previousPlayerSerie = session.getPlayerCache().serie;
        }
        GetSerieSummary2Response resp = new GetSerieSummary2Response();
        resp.serieSummary = resSerie;
        return resp;
    }

    @Override
    public FBWSResponse getRankingSerie2(String sessionID, GetRankingSerie2Param param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetRankingSerie(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param="+param+" - sessionID="+sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetRankingSerieResponse processGetRankingSerie(FBSession session, GetRankingSerie2Param param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID()+" - param="+param);
        }
        tourSerieMgr.checkPeriodValid();
        Player pla = session.getPlayer();
        String serie = session.getSerie();
        WSRankingSerie rankSerie = new WSRankingSerie();
        TourSerieMemPeriodRanking sr = tourSerieMgr.getSerieRanking(serie);
        if (sr == null) {
            log.error("No serie ranking found for serie="+serie);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        rankSerie.rankingPlayer = tourSerieMgr.buildWSRankingForPlayer(pla.getID(), sr.getPlayerRank(pla.getID()), session.getPlayerCache(), pla.getID());
        int offset = param.offset;
        if (offset == -1 && rankSerie.rankingPlayer != null) {
            offset = rankSerie.rankingPlayer.getRank() - (param.nbMaxResult/2);
        }
        if (offset <= 0) {
            offset = 0;
        }
        rankSerie.offset = offset;
        rankSerie.listRankingPlayer = tourSerieMgr.getWSRankingForSerie(serie, rankSerie.rankingPlayer, tourSerieMgr.isPlayerReserve(serie, session.getPlayerCache().serieLastPeriodPlayed, false), rankSerie.offset, param.nbMaxResult);
        rankSerie.nbPlayerSerie = sr.getNbPlayerForRanking();
        rankSerie.totalSize = sr.getNbPlayerForRanking();
        GetRankingSerieResponse resp = new GetRankingSerieResponse();
        resp.rankingSerie = rankSerie;
        return resp;
    }

    @Override
    public FBWSResponse getPreviousRankingSerie(String sessionID, GetPreviousRankingSerieParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetPreviousRankingSerie(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param="+param+" - sessionID="+sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetPreviousRankingSerieResponse processGetPreviousRankingSerie(FBSession session, GetPreviousRankingSerieParam param) throws FBWSException{
        if (param == null) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID()+" - param="+param);
        }
        tourSerieMgr.checkPeriodValid();
        GetPreviousRankingSerieResponse resp = new GetPreviousRankingSerieResponse();
        resp.rankingSerie = new WSPreviousRankingSerie();
        Player pla = session.getPlayer();
        String previousPeriodID = tourSerieMgr.getPeriodIDPrevious();
        if (previousPeriodID != null) {
            resp.rankingSerie.periodStart = TourSerieMgr.transformPeriodID2TS(previousPeriodID, true);
            resp.rankingSerie.periodEnd = TourSerieMgr.transformPeriodID2TS(previousPeriodID, false);
            TourSeriePeriodResult pr = tourSerieMgr.getPeriodResultForPlayer(previousPeriodID, pla.getID());
            String serie = param.previousSerie;
            if (pr != null) {
                serie = pr.getSerie();
            }
            resp.rankingSerie.serie = serie;
            resp.rankingSerie.totalSize = tourSerieMgr.countPlayerOnPeriodResult(previousPeriodID, serie);
            resp.rankingSerie.nbPlayerSerie = resp.rankingSerie.totalSize;
            int offset = param.offset;
            if (offset == -1 && pr != null) {
                offset = pr.getRank() - (param.nbMaxResult/2);
            }
            if (offset <= 0) {
                offset = 0;
            }
            resp.rankingSerie.offset = offset;
            if (pr != null) {
                resp.rankingSerie.rankingPlayer = new WSRankingSeriePlayer(pr, pla, pla.getID(), presenceMgr.isSessionForPlayerID(pla.getID()));
            }
            resp.rankingSerie.listRankingPlayer = new ArrayList<>();
            List<TourSeriePeriodResult> listPR = tourSerieMgr.listPeriodResult(previousPeriodID, serie, offset, param.nbMaxResult);
            if (listPR != null) {
                for (TourSeriePeriodResult e : listPR) {
                    if (resp.rankingSerie.listRankingPlayer.size() == param.nbMaxResult) {break;}
                    if (e.getPlayerID() != pla.getID()) {
                        resp.rankingSerie.listRankingPlayer.add(new WSRankingSeriePlayer(e, playerCacheMgr.getPlayerCache(e.getPlayerID()), pla.getID(), presenceMgr.isSessionForPlayerID(e.getPlayerID())));
                    } else if (resp.rankingSerie.rankingPlayer != null) {
                        resp.rankingSerie.listRankingPlayer.add(resp.rankingSerie.rankingPlayer);
                    }
                }
            }
        } else {
            log.error("No previous period found ...");
        }
        return resp;
    }

    @Override
    public FBWSResponse getTrainingSummary2(String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetTrainingSummary2(session));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public WSTrainingSummary2 processGetTrainingSummary2(FBSession session) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID());
        }
        WSTrainingSummary2 trainingSummary = new WSTrainingSummary2();
        // reload player data from DB (to be sure to have all last data values)
        TrainingPlayerHisto trainingPlayerHisto = trainingMgr.getPlayerHisto(session.getPlayer().getID());
        if (trainingPlayerHisto != null) {
            Calendar dateBefore = Calendar.getInstance();
            dateBefore.add(Calendar.DAY_OF_YEAR, -(trainingMgr.getConfigIntValue("resultStatNbDayBefore", 30)));
            // IMP
            long tsDateRefIMP = dateBefore.getTimeInMillis();
            if (trainingPlayerHisto.getResetIMPDate() > dateBefore.getTimeInMillis()) {
                tsDateRefIMP = trainingPlayerHisto.getResetIMPDate();
            }
            trainingSummary.cumulResultIMP = trainingPlayerHisto.getResultIMP();
            trainingSummary.nbPlayedDealIMP = trainingPlayerHisto.getNbResultIMP();
            List<Double> listResultTourNotFinished = null;
            listResultTourNotFinished = trainingMgr.getPlayerResultsOnNotFinishedTournaments(session.getPlayer().getID(), Constantes.TOURNAMENT_RESULT_IMP, tsDateRefIMP);
            if (listResultTourNotFinished != null) {
                for (Double e : listResultTourNotFinished) {
                    trainingSummary.cumulResultIMP += e;
                    if (trainingSummary.nbPlayedDealIMP >= 0) {
                        // warning, if nbPlayerDealIMP = -1 => player never reset IMP result => nb played deals is not correct
                        trainingSummary.nbPlayedDealIMP++;
                    }
                }
            }
            long tsDateRefPaires = dateBefore.getTimeInMillis();
            if (trainingPlayerHisto.getResetPairesDate() > tsDateRefPaires) {
                tsDateRefPaires = trainingPlayerHisto.getResetPairesDate();
            }
            double temp = trainingPlayerHisto.getResultPaires();
            int nbTemp = trainingPlayerHisto.getNbResultPaires();
            listResultTourNotFinished = trainingMgr.getPlayerResultsOnNotFinishedTournaments(session.getPlayer().getID(), Constantes.TOURNAMENT_RESULT_PAIRE, tsDateRefPaires);
            if (listResultTourNotFinished != null) {
                for (Double e : listResultTourNotFinished) {
                    temp += e;
                    nbTemp++;
                }
            }
            if (nbTemp > 0) {
                trainingSummary.cumulResultMP = temp / nbTemp;
            }
            trainingSummary.nbPlayedDealMP = nbTemp;
        }
        return trainingSummary;
    }

    @Override
	public FBWSResponse getTrainingSummary(String sessionID, GetTrainingSummaryParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetTrainingSummary(session, param));
			} catch (FBWSException e) {
				response.setException(new FBWSExceptionRest(e.getType()));
			} catch (Exception e) {
				log.error("Exception : "+e.getMessage(), e);
				response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
			}
		} else {
            log.warn("SessionID not valid - param="+param+" - sessionID="+sessionID);
			response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
		}
		return response;
	}

	public GetTrainingSummaryResponse processGetTrainingSummary(FBSession session, GetTrainingSummaryParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID()+" - param="+param);
        }
        // result type
        if (param.resultType != Constantes.TOURNAMENT_RESULT_PAIRE && param.resultType != Constantes.TOURNAMENT_RESULT_IMP) {
            // by default IMP
            param.resultType = Constantes.TOURNAMENT_RESULT_IMP;
        }
        WSTrainingSummary trainingSummary = new WSTrainingSummary();
        Player p = session.getPlayer();
        // reload player data from DB (to be sure to have all last data values)
        TrainingPlayerHisto trainingPlayerHisto = trainingMgr.getPlayerHisto(p.getID());

        // compute date to list tournamentPlay
        Calendar dateBefore = Calendar.getInstance();
        dateBefore.add(Calendar.DAY_OF_YEAR, -(trainingMgr.getConfigIntValue("resultStatNbDayBefore", 30)));
        long dateRef = dateBefore.getTimeInMillis();
        if (param.resultType == Constantes.TOURNAMENT_RESULT_IMP) {
            if (trainingPlayerHisto.getResetIMPDate() > dateRef) {
                dateRef = trainingPlayerHisto.getResetIMPDate();
            }
            trainingSummary.cumulResult = trainingPlayerHisto.getResultIMP();
            int nbTemp = trainingPlayerHisto.getNbResultIMP();
            if (trainingMgr.getConfigIntValue("cumulUseNotFinished", 0) == 1) {
                List<Double> listResultTourNotFinished = trainingMgr.getPlayerResultsOnNotFinishedTournaments(p.getID(), param.resultType, dateRef);
                if (listResultTourNotFinished != null) {
                    for (Double e : listResultTourNotFinished) {
                        trainingSummary.cumulResult += e;
                        if (nbTemp >= 0) {
                            nbTemp++;
                        }
                    }
                }
            }
            trainingSummary.nbPlayedDeal = nbTemp;
        } else if (param.resultType == Constantes.TOURNAMENT_RESULT_PAIRE) {
            if (trainingPlayerHisto.getResetPairesDate() > dateRef) {
                dateRef = trainingPlayerHisto.getResetPairesDate();
            }
            int nbTemp = trainingPlayerHisto.getNbResultPaires();
            if (trainingMgr.getConfigIntValue("cumulUseNotFinished", 0) == 1) {
                double temp = trainingPlayerHisto.getResultPaires();
                List<Double> listResultTourNotFinished = trainingMgr.getPlayerResultsOnNotFinishedTournaments(p.getID(), param.resultType, dateRef);
                if (listResultTourNotFinished != null) {
                    for (Double e : listResultTourNotFinished) {
                        temp += e;
                        nbTemp++;
                    }
                }
                if (nbTemp > 0) {
                    trainingSummary.cumulResult = temp / nbTemp;
                }
            } else {
                if (trainingPlayerHisto.getNbResultPaires() > 0) {
                    trainingSummary.cumulResult = trainingPlayerHisto.getResultPaires() / trainingPlayerHisto.getNbResultPaires();
                }
            }
            trainingSummary.nbPlayedDeal = nbTemp;
        }

        // list the last tournament played
        trainingSummary.listLastTournament = trainingMgr.getListWSTournamentForPlayer(session.getPlayerCache(), param.resultType, dateRef, param.nbLastTournament);
        GetTrainingSummaryResponse resp = new GetTrainingSummaryResponse();
        resp.trainingSummary = trainingSummary;
        return resp;
    }

    @Override
    public FBWSResponse getDealResultSummary(String sessionID, GetDealResultSummaryParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetDealResultSummary(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
		} else {
			log.warn("SessionID not valid - param=" + param+" - sessionID="+sessionID);
			response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public WSResultDealSummary processGetDealResultSummary(FBSession session, GetDealResultSummaryParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID()+" - param="+param);
        }
        PlayerCache playerCache = session.getPlayerCache();
        WSResultDealSummary resultDealSummary = new WSResultDealSummary();
        resultDealSummary.result = new WSResultDealTournament();

        // some variable to process
        WSResultDealTournament resultDealGroup = null;
        int nbContractGroup = 0;
        boolean setDataList = true, setDataPar = true, setDataAnalysis = true, setDataCardPlay = true;
        Game gamePlayer = null;

        //*******************************
        // SERIE
        if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
            TourSerieTournament tour = tourSerieMgr.getTournamentWithDeal(param.dealID);
            if (tour == null) {
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            TourSerieDeal deal = (TourSerieDeal)tour.getDeal(param.dealID);
            if (deal == null) {
                log.error("No deal found with this ID="+param.dealID+" - tour="+tour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            // check deal played by this player
            gamePlayer = tourSerieMgr.getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
            if (gamePlayer == null) {
                log.error("Game not found for tour="+tour+" - deal index="+deal.index+" - playerID="+playerCache.ID);
                // if game not found, try to get last game of the tournament
                List<TourSerieGame> listGame = tourSerieMgr.listGameOnTournamentForPlayer(tour.getIDStr(), playerCache.ID);
                if (listGame != null && !listGame.isEmpty()) {
                    gamePlayer = listGame.get(listGame.size()-1);
                }
                if (gamePlayer == null) {
                    log.error("No game found for tour="+tour+" - playerID="+playerCache.ID);
                    throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
                }
            }
            resultDealSummary.result.listResultDeal = tourSerieMgr.resultListDealForTournamentForPlayer(tour, playerCache.ID);
            resultDealGroup = tourSerieMgr.resultDealGroup(param.dealID, playerCache, false);
            nbContractGroup = tourSerieMgr.countContractGroup(tour.getIDStr(), deal.index, true);
        }
        //*******************************
        // TEAM TOUR
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TEAM) {
            TeamTournament tour = tourTeamMgr.getTournamentWithDeal(param.dealID);
            if (tour == null) {
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            TeamDeal deal = (TeamDeal)tour.getDeal(param.dealID);
            if (deal == null) {
                log.error("No deal found with this ID="+param.dealID+" - tour="+tour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            // check deal played by this player
            gamePlayer = tourTeamMgr.getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
            if (gamePlayer == null) {
                log.error("Game not found for tour="+tour+" - deal index="+deal.index+" - playerID="+playerCache.ID);
                throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
            }
            resultDealSummary.result.listResultDeal = tourTeamMgr.resultListDealForTournamentForPlayer(tour, playerCache.ID);
            resultDealGroup = tourTeamMgr.getWSResultDealTournamentGroupped(param.dealID, playerCache, false);
            nbContractGroup = tourTeamMgr.countContractGroup(tour.getIDStr(), deal.index, true);
        }
        //*******************************
        // SERIE TOP CHALLENGE
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
            TourSerieTournament tour = tourSerieMgr.getTournamentWithDeal(param.dealID);
            if (tour == null) {
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            TourSerieDeal deal = (TourSerieDeal)tour.getDeal(param.dealID);
            if (deal == null) {
                log.error("No deal found with this ID="+param.dealID+" - tour="+tour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            // check deal played by this player
            gamePlayer = serieTopChallengeMgr.getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
            if (gamePlayer == null) {
                log.error("Game not found for tour="+tour+" - deal index="+deal.index+" - playerID="+playerCache.ID);
                throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
            }
            resultDealSummary.result.listResultDeal = serieTopChallengeMgr.resultListDealForTournamentForPlayer(tour, playerCache.ID);
            resultDealGroup = serieTopChallengeMgr.resultDealGroup(param.dealID, playerCache, false);
            nbContractGroup = serieTopChallengeMgr.countContractGroup(tour.getIDStr(), deal.index, true, gamePlayer.getScore(), gamePlayer.getContract(), gamePlayer.getContractType());
        }
        //*******************************
        // SERIE EASY CHALLENGE
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
            TourSerieTournament tour = tourSerieMgr.getTournamentWithDeal(param.dealID);
            if (tour == null) {
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            TourSerieDeal deal = (TourSerieDeal)tour.getDeal(param.dealID);
            if (deal == null) {
                log.error("No deal found with this ID="+param.dealID+" - tour="+tour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            // check deal played by this player
            gamePlayer = serieEasyChallengeMgr.getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
            if (gamePlayer == null) {
                log.error("Game not found for tour="+tour+" - deal index="+deal.index+" - playerID="+playerCache.ID);
                throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
            }
            resultDealSummary.result.listResultDeal = serieEasyChallengeMgr.resultListDealForTournamentForPlayer(tour, playerCache.ID);
            resultDealGroup = serieEasyChallengeMgr.resultDealGroup(param.dealID, playerCache, false);
            nbContractGroup = serieEasyChallengeMgr.countContractGroup(tour.getIDStr(), deal.index, true, gamePlayer.getScore(), gamePlayer.getContract(), gamePlayer.getContractType());
        }
        //*******************************
        // TRAINING
        else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
            Tournament tour = trainingMgr.getTournamentWithDeal(param.dealID);
            if (tour == null || playerCache == null) {
                log.error("No tournament found for dealID=" + param.dealID);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            TrainingDeal deal = (TrainingDeal) tour.getDeal(param.dealID);
            if (deal == null) {
                log.error("No deal found in this tournament for dealID=" + param.dealID);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            // check deal played by this player
            gamePlayer = trainingMgr.getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.getIndex(), playerCache.ID);
            if (gamePlayer == null) {
                log.error("Game not found for deal=" + deal.getDealID(tour.getIDStr()) + " - playerID=" + playerCache.ID);
                throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
            }
            resultDealSummary.result.listResultDeal = trainingMgr.resultListDealForTournamentForPlayer(tour, playerCache.ID);
            resultDealGroup = trainingMgr.getWSResultDealTournamentGroupped(param.dealID, playerCache, false);
            nbContractGroup = trainingMgr.countContractGroup(tour.getIDStr(), deal.index, true);
        }
        //*******************************
        // OTHER
        else {
            TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(param.categoryID);
            if (tournamentMgr != null) {
                Tournament tour= tournamentMgr.getTournamentWithDeal(param.dealID);
                if (tour == null) {
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                Deal deal = tour.getDeal(param.dealID);
                if (deal == null) {
                    log.error("No deal found with this ID="+param.dealID+" - tour="+tour);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                // check deal played by this player
                gamePlayer = tournamentMgr.getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
                if (gamePlayer == null) {
                    log.error("Game not found for tour="+tour+" - deal index="+deal.index+" - playerID="+playerCache.ID);
                    throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
                }
                resultDealSummary.result.listResultDeal = tournamentMgr.resultListDealForTournamentForPlayer(tour, playerCache.ID);
                resultDealGroup = tournamentMgr.getWSResultDealTournamentGroupped(param.dealID, playerCache, false);
                nbContractGroup = tournamentMgr.countContractGroup(tour.getIDStr(), deal.index, true);

                /***** specific datas *****/
                // if category federation and tour not finished, we don't set dataList and dataPar
                if (!tour.isFinished() && FederationMgr.isCategoryFederation(param.categoryID)) {
                    setDataList = false;
                    setDataPar = false;
                }
                // if category private, we add chatroom
                if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_PRIVATE) {
                    GenericChatroom chatroom = privateTournamentMgr.getChatMgr().findChatroomByID(((PrivateDeal) deal).getChatroomID());
                    if(chatroom != null){
                        resultDealSummary.chatroom = privateTournamentMgr.getChatMgr().toWSChatroom(chatroom, chatroom.getParticipant(playerCache.ID), false, false);
                    }
                }
            } else {
                log.error("Catgeory not implemented ! - param=" + param);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
        }
        int nbPlayerSameColor = 0;
        String leadGamePlayer = gamePlayer.getBegins();
        char declarerGamePlayer = gamePlayer.getDeclarer();
        boolean isGamePlayerLeave = gamePlayer.isLeaved();
        String gameIDstr = gamePlayer.getIDStr();
        int scoreGamePlayer = gamePlayer.getScore();
        String contractWSGamePlayer = gamePlayer.getContractWS();
        BridgeBid bidGamePlayer = gamePlayer.getBidContract();
        String analyzeBid = gamePlayer.getAnalyzeBid();
        String dealPar = gamePlayer.getDeal().getEngineParInfo();
        int nbTrickGamePlayer = gamePlayer.getNbTricksWinByPlayerAndPartenaire(BridgeConstantes.POSITION_SOUTH);

        if (resultDealGroup != null) {
            resultDealSummary.nbContracts = nbContractGroup;
            if (resultDealGroup.listResultDeal.size() > 0) {
                resultDealSummary.nbPlayers = resultDealGroup.listResultDeal.get(0).getNbTotalPlayer();
            }
            boolean gamePlayerAttack = false;
            boolean analyzeCardColorEnable = false;
            resultDealSummary.result.tournament = resultDealGroup.tournament;
            resultDealSummary.gameID = gameIDstr;
            if (scoreGamePlayer != Constantes.GAME_SCORE_LEAVE && bidGamePlayer != null && !bidGamePlayer.isPass()) {
                analyzeCardColorEnable = true;
                gamePlayerAttack = GameBridgeRule.isPositionInDeclarerSide(BridgeConstantes.POSITION_SOUTH, bidGamePlayer.getOwner());
            }
            // most played contracts => sort resultDealGroup on nb player same game DESC
            Collections.sort(resultDealGroup.listResultDeal, new Comparator<WSResultDeal>() {
                @Override
                public int compare(WSResultDeal o1, WSResultDeal o2) {
                    if (o1.getNbPlayerSameGame() > o2.getNbPlayerSameGame()) {
                        return -1;
                    } else if (o1.getNbPlayerSameGame() < o2.getNbPlayerSameGame()) {
                        return 1;
                    }
                    return 0;
                }
            });
            // Analyze card color & find result for player
            int nbPlayerBestTrickColor = 0, nbPlayerWorseTrickColor = 0;
            WSResultDeal resultDealPlayer = null;
            boolean showLeave = FBConfiguration.getInstance().getIntValue("general.dealResultSummayShowLeave", 0) == 1;
            if (!showLeave && isGamePlayerLeave) {
                showLeave = true;
            }
            for (WSResultDeal e : resultDealGroup.listResultDeal) {
                // ignore leave result ?
                if (e.getScore() == Constantes.GAME_SCORE_LEAVE && !showLeave) {
                    continue;
                }
                // analyze card color
                if (analyzeCardColorEnable && e.getContract().length() >= 2 && e.getDeclarer().length() > 0) {
                    String tempContract = e.getContract().substring(0, 2); // contractWS => only kep first two char
                    BridgeBid bidTemp = BridgeBid.createBid(tempContract, e.getDeclarer().charAt(0));
                    if (!bidTemp.isPass()) {
                        // same color & same side (attack or defense)
                        if (bidTemp.getColor() == bidGamePlayer.getColor()) {
                            boolean southAttack = GameBridgeRule.isPositionInDeclarerSide(BridgeConstantes.POSITION_SOUTH, bidTemp.getOwner());
                            if ((southAttack && gamePlayerAttack) || (!southAttack && !gamePlayerAttack)) {
                                nbPlayerSameColor += e.getNbPlayerSameGame();
                                int nbTricksNS = e.getNbTricks();
                                if (!southAttack) {
                                    nbTricksNS = 13 - nbTricksNS;
                                }
                                if (nbTricksNS > nbTrickGamePlayer) {
                                    nbPlayerBestTrickColor += e.getNbPlayerSameGame();
                                }
                                if (nbTricksNS < nbTrickGamePlayer) {
                                    nbPlayerWorseTrickColor += e.getNbPlayerSameGame();
                                }
                            }
                        }
                    }
                }
                if (resultDealPlayer == null) {
                    if (e.getScore() == scoreGamePlayer && e.getScore() == Constantes.GAME_SCORE_LEAVE) {
                        resultDealPlayer = e;
                    } else if (e.getScore() == scoreGamePlayer && e.getContract().equals(contractWSGamePlayer)) {
                        // FIX bug of result for player with not the same lead
                        if (FBConfiguration.getInstance().getIntValue("general.resultDealSummary.fixBugLead", 1) == 1 &&
                                leadGamePlayer != null && e.getLead() !=null && !e.getLead().equals(leadGamePlayer)) {
                            e.setLead(leadGamePlayer);
                        }
                        // FIX bug of result for player with not the same declarer
                        if (FBConfiguration.getInstance().getIntValue("general.resultDealSummary.fixBugDeclarer", 1) == 1 &&
                                e.getDeclarer() !=null && !e.getDeclarer().equals(Character.toString(declarerGamePlayer))) {
                            e.setDeclarer(Character.toString(declarerGamePlayer));
                        }
                        resultDealPlayer = e;
                    }
                }
                // add to list of most played contract
                resultDealSummary.mostPlayedContracts.add(e);
            }
            // limit the size of list most played
            if (resultDealSummary.mostPlayedContracts.size() > param.nbMaxMostPlayedContracts) {
                resultDealSummary.mostPlayedContracts = resultDealSummary.mostPlayedContracts.subList(0, param.nbMaxMostPlayedContracts);
            }
            // find player result
            boolean idxPlayerFound = false;
            for (final WSResultDeal e : resultDealSummary.mostPlayedContracts) {
                final String gameDeclarer = e.getDeclarer();
                if ((e.getScore() == scoreGamePlayer && e.getScore() == Constantes.GAME_SCORE_LEAVE)
                        || (e.getScore() == scoreGamePlayer && e.getContract().equals(contractWSGamePlayer) && gameDeclarer != null && declarerGamePlayer == gameDeclarer.charAt(0)) ) {
                    idxPlayerFound = true;
                    break;
                }
            }
            // if gamePlayer is not in list of most played => insert the result player at the correct position
            if (!idxPlayerFound && resultDealPlayer != null) {
                resultDealSummary.mostPlayedContracts.remove(param.nbMaxMostPlayedContracts-1);
                resultDealSummary.mostPlayedContracts.add(resultDealPlayer);
            }
            // now sort the list of most played by rank
            Collections.sort(resultDealSummary.mostPlayedContracts, new Comparator<WSResultDeal>() {
                @Override
                public int compare(WSResultDeal o1, WSResultDeal o2) {
                    if (o1.getRank() > o2.getRank()) {
                        return 1;
                    } else if (o1.getRank() < o2.getRank()) {
                        return -1;
                    }
                    return 0;
                }
            });
            int idxPlayer = 0;
            for (final WSResultDeal e : resultDealSummary.mostPlayedContracts) {
                final String gameDeclarer = e.getDeclarer();
                if ((e.getScore() == scoreGamePlayer && e.getScore() == Constantes.GAME_SCORE_LEAVE)
                        || (e.getScore() == scoreGamePlayer && e.getContract().equals(contractWSGamePlayer) && gameDeclarer != null && declarerGamePlayer == gameDeclarer.charAt(0)) ) {
                    break;
                }
                idxPlayer ++;
            }
            resultDealSummary.indexPlayerResult = idxPlayer;
            // par
            resultDealSummary.par = textUIMgr.getTextAnalyzePar(dealPar, session.getPlayer().getDisplayLang());
            // analysis
            if (isGamePlayerLeave) {
                resultDealSummary.analysis = textUIMgr.getTextAnalyzeBid("bidAnalyzeLeave", session.getPlayer().getDisplayLang());
                resultDealSummary.cardPlay = textUIMgr.getTextAnalyzePlay("cardAnalyzeLeave", session.getPlayer().getDisplayLang());
            } else {
                resultDealSummary.analysis = textUIMgr.getTextAnalyzeBid(analyzeBid, session.getPlayer().getDisplayLang());
                resultDealSummary.cardPlay = textUIMgr.getTextAnalyzePlay(
                        argineEngineMgr.buildTextAnalyzePlayColor(analyzeCardColorEnable,
                                bidGamePlayer,
                                nbTrickGamePlayer,
                                nbPlayerSameColor - 1,
                                nbPlayerBestTrickColor,
                                nbPlayerWorseTrickColor),
                        session.getPlayer().getDisplayLang());
            }
            if (!setDataList) {
                resultDealSummary.mostPlayedContracts = new ArrayList<>();
                resultDealSummary.nbContracts = 0;
                resultDealSummary.nbPlayers = 0;
                resultDealSummary.indexPlayerResult = 0;
            }
            if (!setDataPar) {
                resultDealSummary.par = "";
            }
            if (!setDataCardPlay) {
                resultDealSummary.cardPlay = "";
            }
            if (!setDataAnalysis) {
                resultDealSummary.analysis = "";
            }

        } else {
            log.error("ResultDealGroup is null !! param="+param);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        return resultDealSummary;
    }

    @Override
    public FBWSResponse getSerieTopChallengeSummary(String sessionID, GetSerieTopChallengeSummaryParam param) {
        FBWSResponse response = new FBWSResponse();

        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetSerieTopChallengeSummary(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("sessionID not valid or param not valid - sessionID="+sessionID+" - param="+param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @Override
    public FBWSResponse getSerieEasyChallengeSummary(String sessionID, GetSerieEasyChallengeSummaryParam param) {
        FBWSResponse response = new FBWSResponse();

        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetSerieEasyChallengeSummary(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("sessionID not valid or param not valid - sessionID="+sessionID+" - param="+param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetSerieTopChallengeSummaryResponse processGetSerieTopChallengeSummary(FBSession session, GetSerieTopChallengeSummaryParam param) throws FBWSException {
        if (param == null) {
            param = new GetSerieTopChallengeSummaryParam();
            param.nbArchives = 10;
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID()+" - param="+param);
        }
        if (!param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        GetSerieTopChallengeSummaryResponse resp = new GetSerieTopChallengeSummaryResponse();
        serieTopChallengeMgr.checkSerieTopChallengeEnable();
        resp.nbTotalTournaments = serieTopChallengeMgr.countTournamentAvailable();
        resp.nbPlayedTournaments = serieTopChallengeMgr.countTournamentPlayerFinishedOnPeriod(serieTopChallengeMgr.getPeriodTopChallenge().getPeriodID(), session.getPlayer().getID());
        resp.archives = new ArrayList<>();
        int nbArchives = param.nbArchives;
        if (nbArchives < 0 || nbArchives > 10) {
            nbArchives = 5;
        }
        if (nbArchives == 0) {
            nbArchives = 5;
        }
        ResultListTournamentArchive rlta = serieTopChallengeMgr.listTournamentArchive(session.getPlayer().getID(), 0, nbArchives);
        if (param.nbArchives > 0) {
            resp.archives = rlta.archives;
        }
        resp.nbTotalArchives = rlta.nbTotal;
        return resp;
    }

    public GetSerieEasyChallengeSummaryResponse processGetSerieEasyChallengeSummary(FBSession session, GetSerieEasyChallengeSummaryParam param) throws FBWSException {
        if (param == null) {
            param = new GetSerieEasyChallengeSummaryParam();
            param.nbArchives = 10;
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID()+" - param="+param);
        }
        if (!param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        GetSerieEasyChallengeSummaryResponse resp = new GetSerieEasyChallengeSummaryResponse();
        serieEasyChallengeMgr.checkSerieEasyChallengeEnable();
        resp.nbTotalTournaments = serieEasyChallengeMgr.countTournamentAvailable(session.getPlayer().getID());
        resp.nbPlayedTournaments = serieEasyChallengeMgr.countTournamentPlayerFinishedOnPeriod(serieEasyChallengeMgr.getPeriodEasyChallenge().getPeriodID(), session.getPlayer().getID());
        resp.archives = new ArrayList<>();
        int nbArchives = param.nbArchives;
        if (nbArchives < 0 || nbArchives > 10) {
            nbArchives = 5;
        }
        if (nbArchives == 0) {
            nbArchives = 5;
        }
        ResultListTournamentArchive rlta = serieEasyChallengeMgr.listTournamentArchive(session.getPlayer().getID(), 0, nbArchives);
        if (param.nbArchives > 0) {
            resp.archives = rlta.archives;
        }
        resp.nbTotalArchives = rlta.nbTotal;
        return resp;
    }

    @Override
    public FBWSResponse getMainRanking(String sessionID, GetMainRankingParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetMainRanking(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("sessionID not valid or param not valid - sessionID="+sessionID+" - param="+param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetMainRankingResponse processGetMainRanking(FBSession session, GetMainRankingParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getLoginID()+" - param="+param);
        }
        GetMainRankingResponse resp = null;
        List<Long> listFollower = null;
        // filter with player friends
        if (param.options != null && param.options.contains(Constantes.RANKING_OPTIONS_FRIENDS)) {
            listFollower = playerMgr.getListPlayerIDLinkFollower(session.getPlayer().getID());
            if (listFollower != null) {
                listFollower.add(session.getPlayer().getID());
            }
        }
        String countryCode = null;
        // filter with player country
        if (param.options != null && param.options.contains(Constantes.RANKING_OPTIONS_COUNTRY)) {
            countryCode = session.getPlayer().getDisplayCountryCode();
        }

        // ranking DUEL
        if (param.type.equals(Constantes.RANKING_TYPE_DUEL)) {
            String periodID = "";
            if (param.options.contains(Constantes.RANKING_OPTIONS_CURRENT_PERIOD)) {
                periodID = duelMgr.getStatCurrentPeriodID();
            } else if (param.options.contains(Constantes.RANKING_OPTIONS_PREVIOUS_PERIOD)){
                periodID = duelMgr.getStatPreviousPeriodID();
            }
            resp = duelMgr.getRanking(session.getPlayerCache(), periodID, listFollower, countryCode, param.offset, param.nbMaxResult);
        }
        // ranking DUEL ARGINE
        else if (param.type.equals(Constantes.RANKING_TYPE_DUEL_ARGINE)) {
            String periodID = "";
            if (param.options.contains(Constantes.RANKING_OPTIONS_CURRENT_PERIOD)) {
                periodID = duelMgr.getStatCurrentPeriodID();
            } else if (param.options.contains(Constantes.RANKING_OPTIONS_PREVIOUS_PERIOD)){
                periodID = duelMgr.getStatPreviousPeriodID();
            }
            resp = duelMgr.getRankingArgine(session.getPlayerCache(), periodID, listFollower, countryCode, param.offset, param.nbMaxResult);
        }
        // ranking SERIE
        else if (param.type.equals(Constantes.RANKING_TYPE_SERIE)) {
            String periodID = "";
            if (param.options.contains(Constantes.RANKING_OPTIONS_PREVIOUS_PERIOD)) {
                periodID = tourSerieMgr.getPeriodIDPrevious();
            } else {
                periodID = tourSerieMgr.getCurrentPeriod().getPeriodID();
            }
            resp = tourSerieMgr.getRanking(session.getPlayerCache(), periodID, listFollower, countryCode, param.offset, param.nbMaxResult);
        }
        // Ranking Funbridge Points
        else if (param.type.equals(Constantes.RANKING_TYPE_PTS_CBO)) {
            String periodID = "";
            if (param.options != null && param.options.contains(Constantes.RANKING_OPTIONS_CURRENT_PERIOD)) {
                periodID = tourFederationStatPeriodMgr.getStatCurrentPeriodID();
            } else if (param.options != null && param.options.contains(Constantes.RANKING_OPTIONS_PREVIOUS_PERIOD)){
                periodID = tourFederationStatPeriodMgr.getStatPreviousPeriodID();
            }
            resp = tourCBOMgr.getRankingFunbridgePoints(session.getPlayerCache(), periodID, listFollower, countryCode, param.offset, param.nbMaxResult);
        }
        // Ranking Average performance
        else if (param.type.equals(Constantes.RANKING_TYPE_PERFORMANCE)) {
            resp = playerMgr.getRankingAveragePerformance(session.getPlayerCache(), listFollower, countryCode, param.offset, param.nbMaxResult);
        }
        // Ranking country
        else if (param.type.equals(Constantes.RANKING_TYPE_COUNTRY)) {
            int nbMinPlayers = 0;
            String[] tabOptions = param.options.split(";");
            for (String option : tabOptions) {
                if (option.startsWith(Constantes.RANKING_OPTIONS_COUNTRY_FILTER)) {
                    try {
                        nbMinPlayers = Integer.valueOf(option.split(":")[1]);
                    } catch (Exception e) {
                        log.error("Option " + option + " is invalid");
                    }
                }
            }
            resp = playerMgr.getRankingCountry(session.getPlayerCache(), param.offset, param.nbMaxResult, nbMinPlayers);
        }
        // Ranking Federation
        else if (FederationMgr.isNameFederation(param.type)) {
            TourFederationMgr tourMgr = FederationMgr.getTourFederationMgr(param.type);
            String periodID = "";
            if (param.options != null && param.options.contains(Constantes.RANKING_OPTIONS_CURRENT_PERIOD)) {
                periodID = tourFederationStatPeriodMgr.getStatCurrentPeriodID();
            } else if (param.options != null && param.options.contains(Constantes.RANKING_OPTIONS_PREVIOUS_PERIOD)){
                periodID = tourFederationStatPeriodMgr.getStatPreviousPeriodID();
            }
            resp = tourMgr.getRankingFederation(session.getPlayerCache(), periodID, listFollower, countryCode, param.offset, param.nbMaxResult);
        }

        if (resp == null) {
            log.error("No ranking process for session="+session+" - param="+param);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // swap player position if player is not first for his rank
        if (resp != null && resp.ranking != null && resp.ranking.size() > 0) {
            int playerRank = 0;
            if (resp.rankingPlayer != null) {
                playerRank = resp.rankingPlayer.rank;
            }
            // player rank must be > 0 and between the first and the last of the list
            if (playerRank > 0 && resp.ranking.get(0).rank <= playerRank && resp.ranking.get(resp.ranking.size()-1).rank >= playerRank) {
                int idxFirstWithPlayerRank = -1;
                int idxPlayer = -1;
                for (int i = 0; i < resp.ranking.size(); i++) {
                    WSMainRankingPlayer e = resp.ranking.get(i);
                    // save idx with same player rank
                    if (idxFirstWithPlayerRank == -1 && e.rank == playerRank && e.playerID != resp.rankingPlayer.getPlayerID() && idxPlayer == -1) {
                        idxFirstWithPlayerRank = i;
                    }
                    // save idx of player
                    if (e.playerID == session.getPlayer().getID()) {
                        idxPlayer = i;
                    }
                    // rank is greater than player rank => stop loop
                    if (e.rank > playerRank) {
                        break;
                    }
                }
                if (idxFirstWithPlayerRank >= 0) {
                    if (idxFirstWithPlayerRank != idxPlayer) {
                        if (idxPlayer == -1) {
                            // player not found in the list => just replace the first idx with same rank with rankingPlayer
                            resp.ranking.set(idxFirstWithPlayerRank, resp.rankingPlayer);
                        } else {
                            // swap player index with first idx with same player rank
                            Collections.swap(resp.ranking, idxPlayer, idxFirstWithPlayerRank);
                        }
                    }
                }
            }
        }
        return resp;
    }

    public FBWSResponse getDuelBestScoreEver(GetDuelBestScoreParam param){
        FBWSResponse response = new FBWSResponse();
        try {
            response.setData(processGetDuelBestScoreEver(param));
        } catch (FBWSException e) {
            response.setException(new FBWSExceptionRest(e.getType()));
        } catch (Exception e) {
            log.error("Exception : "+e.getMessage(), e);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
        }
        return response;
    }

    public GetDuelBestScoreResponse processGetDuelBestScoreEver(GetDuelBestScoreParam param) throws FBWSException {
        GetDuelBestScoreResponse response = new GetDuelBestScoreResponse();
        response.podium = convertPodiumMapToPlayerDuelScoring(duelMgr.getBestScoreEver("scoring.bestEver", param.rivalId == 0 ? -2 :  param.rivalId));
        return response;
    }

    public FBWSResponse getDuelBestScoreMonthly(GetDuelBestScoreMonthlyParam param){
        FBWSResponse response = new FBWSResponse();
        try {
            response.setData(processGetDuelBestScoreMonthly(param));
        } catch (FBWSException e) {
            response.setException(new FBWSExceptionRest(e.getType()));
        } catch (Exception e) {
            log.error("Exception : "+e.getMessage(), e);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
        }
        return response;
    }

    public GetDuelBestScoreResponse processGetDuelBestScoreMonthly(GetDuelBestScoreMonthlyParam param) throws FBWSException {
        GetDuelBestScoreResponse response = new GetDuelBestScoreResponse();
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"]");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        response.podium = convertPodiumMapToPlayerDuelScoring(duelMgr.getBestScoreMonthly("scoring.bestMonthly", param.periodId, param.rivalId == 0 ? -2 :  param.rivalId));
        return response;
    }

    public FBWSResponse getDuelBestScoreWeekly(GetDuelBestScoreWeeklyParam param){
        FBWSResponse response = new FBWSResponse();
        try {
            response.setData(processGetDuelBestScoreWeekly(param));
        } catch (FBWSException e) {
            response.setException(new FBWSExceptionRest(e.getType()));
        } catch (Exception e) {
            log.error("Exception : "+e.getMessage(), e);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
        }
        return response;
    }

    public GetDuelBestScoreResponse processGetDuelBestScoreWeekly(GetDuelBestScoreWeeklyParam param) throws FBWSException {
        GetDuelBestScoreResponse response = new GetDuelBestScoreResponse();

        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"]");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        try{
            startDate.setTime(sdf.parse(param.startDate));
            endDate.setTime(sdf.parse(param.endDate));
        }catch(Exception e){
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        response.podium = convertPodiumMapToPlayerDuelScoring(duelMgr.getBestScoreWeekly(param.rivalId == 0 ? -2 :  param.rivalId, startDate, endDate));
        return response;
    }

    private List<PlayerDuelScoring> convertPodiumMapToPlayerDuelScoring(Map<Long, Double> podiumMap){
        List<PlayerDuelScoring> podium = new ArrayList<>();
        podiumMap.entrySet().forEach(p -> {
            Player player = playerMgr.getPlayer(p.getKey());
            PlayerDuelScoring playerDuelScoring = new PlayerDuelScoring();
            playerDuelScoring.pseudo = player.getNickname();
            playerDuelScoring.playerId = player.getID();
            playerDuelScoring.score = p.getValue();
            podium.add(playerDuelScoring);
        });
        return podium;
    }
}
