package com.funbridge.server.ws.game;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.engine.ArgineEngineMgr;
import com.funbridge.server.engine.ArgineProfile;
import com.funbridge.server.engine.ArgineTextElement;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.FilterEvent;
import com.funbridge.server.tournament.TournamentGame2Mgr;
import com.funbridge.server.tournament.category.TournamentTrainingPartnerMgr;
import com.funbridge.server.tournament.data.SpreadGameData;
import com.funbridge.server.tournament.data.TournamentChallenge;
import com.funbridge.server.tournament.data.TournamentTable2;
import com.funbridge.server.tournament.duel.DuelMgr;
import com.funbridge.server.tournament.duel.data.DuelGame;
import com.funbridge.server.tournament.duel.data.DuelTournament;
import com.funbridge.server.tournament.federation.cbo.TourCBOMgr;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.game.GameMgr;
import com.funbridge.server.tournament.game.Table;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.learning.TourLearningMgr;
import com.funbridge.server.tournament.learning.data.LearningGame;
import com.funbridge.server.tournament.privatetournament.PrivateTournamentMgr;
import com.funbridge.server.tournament.serie.SerieEasyChallengeMgr;
import com.funbridge.server.tournament.serie.SerieTopChallengeMgr;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.serie.data.TourSerieGame;
import com.funbridge.server.tournament.serie.data.TourSerieTournament;
import com.funbridge.server.tournament.team.TourTeamMgr;
import com.funbridge.server.tournament.team.data.TeamGame;
import com.funbridge.server.tournament.team.data.TeamTournament;
import com.funbridge.server.tournament.timezone.TimezoneMgr;
import com.funbridge.server.tournament.training.TrainingMgr;
import com.funbridge.server.tournament.training.data.TrainingGame;
import com.funbridge.server.tournament.training.data.TrainingTournament;
import com.funbridge.server.ws.*;
import com.funbridge.server.ws.event.Event;
import com.funbridge.server.ws.tournament.WSTableTournament;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.tools.StringVersion;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service(value = "gameService")
@Scope(value = "singleton")
public class GameServiceRestImpl extends FunbridgeMgr implements GameServiceRest {
    @Resource(name = "tournamentTrainingPartnerMgr")
    private TournamentTrainingPartnerMgr tournamentTrainingPartnerMgr = null;
    @Resource(name = "tournamentGame2Mgr")
    private TournamentGame2Mgr tournamentGame2Mgr = null;
    @Resource(name = "trainingMgr")
    private TrainingMgr trainingMgr = null;
    @Resource(name = "timezoneMgr")
    private TimezoneMgr timezoneMgr = null;
    @Resource(name = "tourCBOMgr")
    private TourCBOMgr tourCBOMgr = null;
    @Resource(name = "tourSerieMgr")
    private TourSerieMgr tourSerieMgr = null;
    @Resource(name = "serieTopChallengeMgr")
    private SerieTopChallengeMgr serieTopChallengeMgr = null;
    @Resource(name = "serieEasyChallengeMgr")
    private SerieEasyChallengeMgr serieEasyChallengeMgr = null;
    @Resource(name = "duelMgr")
    private DuelMgr duelMgr = null;
    @Resource(name = "tourTeamMgr")
    private TourTeamMgr tourTeamMgr = null;
    @Resource(name = "privateTournamentMgr")
    private PrivateTournamentMgr privateTournamentMgr = null;
    @Resource(name = "tourLearningMgr")
    private TourLearningMgr tourLearningMgr = null;
    @Resource(name = "playerMgr")
    private PlayerMgr playerMgr = null;
    private LockWeakString lockPlayerGameAction = new LockWeakString();

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

    public LockWeakString getLockPlayerGameAction() {
        return lockPlayerGameAction;
    }

    /**
     * Check if the game in session has same ID and return the type of game : Normal, Replay or Patner.
     *
     * @param session
     * @param gameID
     * @param gameIDStr
     * @return Normal, Replay or Patner if a game with same ID exist else Unknown (no game in session or not same ID)
     */
    public GameType checkGameSessionAndReturnGameType(FBSession session, long gameID, String gameIDStr) throws FBWSException {
        GameType gameType = GameType.Unknown;
        if (session != null) {
            if (gameIDStr != null && gameIDStr.length() > 0 && session.getCurrentGameTable() != null && session.getCurrentGameTable().getGame() != null && session.getCurrentGameTable().getGame().getIDStr().equals(gameIDStr)) {
                if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
                    return GameType.Timezone;
                } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
                    return GameType.Training;
                } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
                    return GameType.Serie;
                } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
                    return GameType.SerieTopChallenge;
                } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
                    return GameType.SerieEasyChallenge;
                } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_DUEL) {
                    return GameType.Duel;
                } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TOUR_CBO) {
                    return GameType.TourCBO;
                } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TEAM) {
                    return GameType.TourTeam;
                } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_PRIVATE) {
                    return GameType.Private;
                } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_LEARNING) {
                    return GameType.TourLearning;
                }
            }
            if (session.getCurrentTrainingPartnerTableID() > 0) {
                if (tournamentTrainingPartnerMgr.getForTableAndGame(session.getCurrentTrainingPartnerTableID(), gameID) != null) {
                    return GameType.Partner;
                }
            }
            // game not found in session => try to find game in different mode and check tournament associated
            Game game = trainingMgr.getGame(gameIDStr);
            if (game == null) {
                game = tourSerieMgr.getGame(gameIDStr);
            }
            if (game == null) {
                game = serieTopChallengeMgr.getGame(gameIDStr);
            }
            if (game == null) {
                game = serieEasyChallengeMgr.getGame(gameIDStr);
            }
            if (game == null) {
                game = timezoneMgr.getGame(gameIDStr);
            }
            if (game == null) {
                game = duelMgr.getGame(gameIDStr);
            }
            if (game == null) {
                game = tourCBOMgr.getGame(gameIDStr);
            }
            if (game == null) {
                game = tourTeamMgr.getGame(gameIDStr);
            }
            if (game == null) {
                game = privateTournamentMgr.getGame(gameIDStr);
            }
            if (game == null) {
                game = tourLearningMgr.getGame(gameIDStr);
            }
            if (game != null && (game.getTournament().isFinished() || !game.getTournament().isDateValid(System.currentTimeMillis()))) {
                throw new FBWSException(FBExceptionType.TOURNAMENT_FINISHED);
            }
        }
        return gameType;
    }

    /**
     * Return the gameMgr associated to this gameType. TrainingPartner has not GameMgr object, it return null for this type.
     *
     * @param type
     * @return
     */
    public GameMgr getGameMgr(GameType type) {
        switch (type) {
            case Training:
                return trainingMgr.getGameMgr();
            case Serie:
                return tourSerieMgr.getGameMgr();
            case SerieTopChallenge:
                return serieTopChallengeMgr.getGameMgr();
            case SerieEasyChallenge:
                return serieEasyChallengeMgr.getGameMgr();
            case Duel:
                return duelMgr.getGameMgr();
            case TourTeam:
                return tourTeamMgr.getGameMgr();
            case TourLearning:
                return tourLearningMgr.getGameMgr();
            default:
                return ContextManager.getTournamentGameMgr(type);
        }
    }

    @Override
    public FBWSResponse leaveGame(String sessionID, LeaveGameParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                response.setData(processLeaveGame(session, param));
            } catch (FBWSException e) {
                log.error("FBWSException : " + e.getMessage() + " - param=" + param, e);
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public LeaveGameResponse processLeaveGame(FBSession session, LeaveGameParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        boolean bReturn = false;
        long gameID = Constantes.getIDLongValue(param.gameIDstr, param.gameID);
        synchronized (lockPlayerGameAction.getLock("" + session.getPlayer().getID())) {
            GameType gameType = checkGameSessionAndReturnGameType(session, gameID, param.gameIDstr);
            if (gameType == GameType.Partner) {
                TournamentTable2 table2 = tournamentTrainingPartnerMgr.getForTableAndGame(session.getCurrentTrainingPartnerTableID(), gameID);
                if (table2 != null) {
                    tournamentTrainingPartnerMgr.checkTournament(table2);
                    bReturn = tournamentTrainingPartnerMgr.leaveGame(table2, session.getPlayer());
                } else {
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
            } else {
                GameMgr gameMgr = getGameMgr(gameType);
                if (gameMgr != null) {
                    gameMgr.getTournamentMgr().checkTournamentMode(session.getCurrentGameTable().isReplay(), 0);
                    bReturn = gameMgr.leaveGame(session);
                } else {
                    log.error("No gameMgr found for gameType=" + gameType + " - param=" + param + " - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
            }
        }
        LeaveGameResponse resp = new LeaveGameResponse();
        resp.status = bReturn;
        return resp;
    }

    @Override
    public FBWSResponse playBid(String sessionID, PlayBidParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            String sessionStr = null;
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                response.setData(processPlayBid(session, param));
            } catch (FBWSException e) {
                log.error("FBWSException : " + e.getMessage() + " - session=" + sessionStr + " - param=" + param, e);
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public PlayBidResponse processPlayBid(FBSession session, PlayBidParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        long gameID = Constantes.getIDLongValue(param.gameIDstr, param.gameID);
        synchronized (lockPlayerGameAction.getLock("" + session.getPlayer().getID())) {
            GameType gameType = checkGameSessionAndReturnGameType(session, gameID, param.gameIDstr);
            if (gameType == GameType.Partner) {
                TournamentTable2 table2 = tournamentTrainingPartnerMgr.getForTableAndGame(session.getCurrentTrainingPartnerTableID(), gameID);
                if (table2 != null) {
                    tournamentTrainingPartnerMgr.checkTournament(table2);
                    boolean bStartThread = tournamentGame2Mgr.playBid(table2, param.bid, session.getPlayer().getID(), param.step);
                    if (bStartThread) {
                        tournamentGame2Mgr.startGameThread(table2, false);
                    }
                } else {
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
            } else {
                GameMgr gameMgr = getGameMgr(gameType);
                if (gameMgr != null) {
                    boolean bStartThread = gameMgr.playBid(session, param.bid, param.step);
                    if (bStartThread) {
                        gameMgr.startThreadPlay(session, false, 0, null, 0, null);
                    }
                } else {
                    log.error("No gameMgr found for gameType=" + gameType + " - param=" + param + " - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
            }
        }
        PlayBidResponse resp = new PlayBidResponse();
        resp.status = true;
        return resp;
    }

    @Override
    public FBWSResponse getPlayBidInformation(String sessionID, GetPlayBidInformationParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            String sessionStr = null;
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                response.setData(processGetPlayBidInformation(session, param));
            } catch (FBWSException e) {
                log.error("FBWSException : " + e.getMessage() + " - session=" + sessionStr + " - param=" + param, e);
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetPlayBidInformationResponse processGetPlayBidInformation(FBSession session, GetPlayBidInformationParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.warn("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        GetPlayBidInformationResponse resp = new GetPlayBidInformationResponse();
        long gameID = Constantes.getIDLongValue(param.gameIDstr, param.gameID);
        GameType gameType = checkGameSessionAndReturnGameType(session, gameID, param.gameIDstr);
        if (gameType == GameType.Partner) {
            TournamentTable2 table2 = tournamentTrainingPartnerMgr.getForTableAndGame(session.getCurrentTrainingPartnerTableID(), gameID);
            if (table2 != null) {
                tournamentTrainingPartnerMgr.checkTournament(table2);
                resp.information = tournamentGame2Mgr.getBidInfo(table2, param.bids);
            } else {
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        } else {
            GameMgr gameMgr = getGameMgr(gameType);
            if (gameMgr != null) {
                resp.information = gameMgr.getBidInfo(session, param.bids);
            } else {
                log.error("No gameMgr found for gameType=" + gameType + " - param=" + param + " - session=" + session);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
        }
        String[] dataReglette = resp.information.split(";");
        if (dataReglette.length >= 13) {
            ArgineEngineMgr engineMgr = ContextManager.getArgineEngineMgr();
            if (FBConfiguration.getInstance().getIntValue("engine.bidInfoBuildAllText", 0) == 1) {
                // NB CARD CLUB
                resp.clubText = engineMgr.getTextColor(dataReglette[0], dataReglette[1], session.getPlayer().getDisplayLang());
                // NB CARD DIAMOND
                resp.diamondText = engineMgr.getTextColor(dataReglette[2], dataReglette[3], session.getPlayer().getDisplayLang());
                // NB CARD HEART
                resp.heartText = engineMgr.getTextColor(dataReglette[4], dataReglette[5], session.getPlayer().getDisplayLang());
                // NB CARD SPADE
                resp.spadeText = engineMgr.getTextColor(dataReglette[6], dataReglette[7], session.getPlayer().getDisplayLang());
                // POINTS
                resp.numPointsText = engineMgr.getTextPoint(dataReglette[8], dataReglette[9], session.getPlayer().getDisplayLang());
            }
            if (dataReglette[12].startsWith("#")) {
                // argine text begins with '#'
                resp.text = engineMgr.decodeText(dataReglette[12].substring(1), session.getPlayer().getDisplayLang());
                // get info forcing
                if (dataReglette.length >= 14) {
                    int forcingValue = 0;
                    try {
                        forcingValue = Integer.parseInt(dataReglette[13]);
                    } catch (Exception e) {
                        log.error("Failed to parseInt for string=" + dataReglette[13]);
                    }
                    if (forcingValue > 0) {
                        ArgineTextElement ate = engineMgr.getArgineText("forcing" + forcingValue);
                        if (ate != null) {
                            resp.forcing = ate.getTextForLang(session.getPlayer().getDisplayLang());
                            resp.alert = forcingValue >= 20;
                        }
                    }
                }
            }
        }
        return resp;
    }

    @Override
    public FBWSResponse getBidInformation(GetBidInformationParam param) {
        FBWSResponse response = new FBWSResponse();
        if (param != null && param.isValid()) {
            if (log.isDebugEnabled()) {
                log.debug("param=" + param);
            }
            try {
                response.setData(processGetBidInformation(param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.error("Parameter not valid - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetPlayBidInformationResponse processGetBidInformation(GetBidInformationParam param) throws FBWSException {
        GetPlayBidInformationResponse resp = new GetPlayBidInformationResponse();
        long dealID = Constantes.getIDLongValue(param.dealIDstr, param.dealID);
        // retrieve the game object with this ID
        switch (param.tournamentCategory) {
            // NEW SERIE
            case Constantes.TOURNAMENT_CATEGORY_NEWSERIE: {
                String tourID = TourSerieMgr.extractTourIDFromDealID(param.dealIDstr);
                int dealIndex = TourSerieMgr.extractDealIndexFromDealID(param.dealIDstr);
                TourSerieGame game = tourSerieMgr.getGameOnTournamentAndDealForPlayer(tourID, dealIndex, param.playerID);
                if (game == null) {
                    log.error("No game Serie found - param=" + param + " - tourID=" + tourID + " - dealIndex=" + dealIndex);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                int engineVersion = ContextManager.getArgineEngineMgr().getEngineVersion(game.getTournament().getCategory());
                resp.information = tourSerieMgr.getGameMgr().getEngine().getBidInfoFullData(
                        game.getDeal().dealer, game.getDeal().vulnerability, param.bids,
                        game.getConventionProfile(), game.getConventionData(), game.getCardsConventionProfile(), game.getCardsConventionData(),
                        Constantes.TOURNAMENT_RESULT_PAIRE, engineVersion);
            }
            break;
            // SERIE_TOP_CHALLENGE
            case Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE: {
                String tourID = TourSerieMgr.extractTourIDFromDealID(param.dealIDstr);
                int dealIndex = TourSerieMgr.extractDealIndexFromDealID(param.dealIDstr);
                Game game = serieTopChallengeMgr.getGameOnTournamentAndDealForPlayer(tourID, dealIndex, param.playerID);
                if (game == null) {
                    // no game found for this playerID in top_challenge collection => try in serie collection
                    game = tourSerieMgr.getGameOnTournamentAndDealForPlayer(tourID, dealIndex, param.playerID);
                    if (game == null) {
                        log.error("No game serie top challenge or serie found - param=" + param + " - tourID=" + tourID + " - dealIndex=" + dealIndex);
                        throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                    }
                }
                int engineVersion = ContextManager.getArgineEngineMgr().getEngineVersion(game.getTournament().getCategory());
                resp.information = serieTopChallengeMgr.getGameMgr().getEngine().getBidInfoFullData(
                        game.getDeal().dealer, game.getDeal().vulnerability, param.bids,
                        game.getConventionProfile(), game.getConventionData(), game.getCardsConventionProfile(), game.getCardsConventionData(),
                        Constantes.TOURNAMENT_RESULT_PAIRE, engineVersion);
            }
            break;
            // SERIE_EASY_CHALLENGE
            case Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE: {
                final String tourID = TourSerieMgr.extractTourIDFromDealID(param.dealIDstr);
                final int dealIndex = TourSerieMgr.extractDealIndexFromDealID(param.dealIDstr);
                Game game = this.serieEasyChallengeMgr.getGameOnTournamentAndDealForPlayer(tourID, dealIndex, param.playerID);
                if (game == null) {
                    // no game found for this playerID in top_challenge collection => try in serie collection
                    game = this.tourSerieMgr.getGameOnTournamentAndDealForPlayer(tourID, dealIndex, param.playerID);
                    if (game == null) {
                        log.error("No game serie easy challenge or serie found - param=" + param + " - tourID=" + tourID + " - dealIndex=" + dealIndex);
                        throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                    }
                }
                int engineVersion = ContextManager.getArgineEngineMgr().getEngineVersion(game.getTournament().getCategory());
                resp.information = serieEasyChallengeMgr.getGameMgr().getEngine().getBidInfoFullData(
                        game.getDeal().dealer, game.getDeal().vulnerability, param.bids,
                        game.getConventionProfile(), game.getConventionData(), game.getCardsConventionProfile(), game.getCardsConventionData(),
                        Constantes.TOURNAMENT_RESULT_PAIRE, engineVersion);
            }
            break;
            // TRAINING
            case Constantes.TOURNAMENT_CATEGORY_TRAINING: {
                String tourID = TrainingMgr.extractTourIDFromDealID(param.dealIDstr);
                int dealIndex = TrainingMgr.extractDealIndexFromDealID(param.dealIDstr);
                TrainingGame game = trainingMgr.getGameOnTournamentAndDealForPlayer(tourID, dealIndex, param.playerID);
                if (game == null) {
                    log.error("No game TRAINING found - param=" + param);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                int engineVersion = ContextManager.getArgineEngineMgr().getEngineVersion(game.getTournament().getCategory());
                resp.information = trainingMgr.getGameMgr().getEngine().getBidInfoFullData(
                        game.getDeal().getDealer(), game.getDeal().getVulnerability(), param.bids,
                        game.getConventionProfile(), game.getConventionData(), game.getCardsConventionProfile(), game.getCardsConventionData(),
                        game.getTournament().getResultType(), engineVersion);
            }
            break;
            // TIMEZONE
            case Constantes.TOURNAMENT_CATEGORY_TIMEZONE: {
                String tourID = TimezoneMgr.extractTourIDFromDealID(param.dealIDstr);
                int dealIndex = TimezoneMgr.extractDealIndexFromDealID(param.dealIDstr);
                Game game = timezoneMgr.getGameOnTournamentAndDealForPlayer(tourID, dealIndex, param.playerID);
                int engineVersion = ContextManager.getArgineEngineMgr().getEngineVersion(game.getTournament().getCategory());
                if (game == null) {
                    log.error("No game TIMEZONE found - param=" + param + " - tourID=" + tourID + " - dealIndex=" + dealIndex);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                resp.information = timezoneMgr.getGameMgr().getEngine().getBidInfoFullData(
                        game.getDeal().dealer, game.getDeal().vulnerability, param.bids,
                        game.getConventionProfile(), game.getConventionData(), game.getCardsConventionProfile(), game.getCardsConventionData(),
                        game.getTournament().getResultType(), engineVersion);
            }
            break;
            // DUEL
            case Constantes.TOURNAMENT_CATEGORY_DUEL: {
                String tourID = DuelMgr.extractTourIDFromDealID(param.dealIDstr);
                int dealIndex = DuelMgr.extractDealIndexFromDealID(param.dealIDstr);
                DuelGame game = duelMgr.getGameOnTournamentAndDealForPlayer(tourID, dealIndex, param.playerID);
                if (game == null) {
                    log.error("No game DUEL found - param=" + param);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                int engineVersion = ContextManager.getArgineEngineMgr().getEngineVersion(game.getTournament().getCategory());
                resp.information = duelMgr.getGameMgr().getEngine().getBidInfoFullData(
                        game.getDeal().getDealer(), game.getDeal().getVulnerability(), param.bids,
                        game.getConventionProfile(), game.getConventionData(), game.getCardsConventionProfile(), game.getCardsConventionData(),
                        game.getTournament().getResultType(), engineVersion);
            }
            break;
            // TOUR TEAM
            case Constantes.TOURNAMENT_CATEGORY_TEAM: {
                String tourID = TourTeamMgr.extractTourIDFromDealID(param.dealIDstr);
                int dealIndex = TourTeamMgr.extractDealIndexFromDealID(param.dealIDstr);
                TeamGame game = tourTeamMgr.getGameOnTournamentAndDealForPlayer(tourID, dealIndex, param.playerID);
                if (game == null) {
                    log.error("No game Team found - param=" + param + " - tourID=" + tourID + " - dealIndex=" + dealIndex);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                int engineVersion = ContextManager.getArgineEngineMgr().getEngineVersion(game.getTournament().getCategory());
                resp.information = tourSerieMgr.getGameMgr().getEngine().getBidInfoFullData(
                        game.getDeal().dealer, game.getDeal().vulnerability, param.bids,
                        game.getConventionProfile(), game.getConventionData(), game.getCardsConventionProfile(), game.getCardsConventionData(),
                        Constantes.TOURNAMENT_RESULT_PAIRE, engineVersion);
            }
            break;
            // TOUR LEARNING
            case Constantes.TOURNAMENT_CATEGORY_LEARNING: {
                String tourID = TourLearningMgr.extractTourIDFromDealID(param.dealIDstr);
                int dealIndex = TourLearningMgr.extractDealIndexFromDealID(param.dealIDstr);
                LearningGame game = tourLearningMgr.getGameOnTournamentAndDealForPlayer(tourID, dealIndex, param.playerID);
                if (game == null) {
                    log.error("No game Learning found - param=" + param + " - tourID=" + tourID + " - dealIndex=" + dealIndex);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                int engineVersion = ContextManager.getArgineEngineMgr().getEngineVersion(game.getTournament().getCategory());
                resp.information = tourLearningMgr.getGameMgr().getEngine().getBidInfoFullData(
                        game.getDeal().dealer, game.getDeal().vulnerability, param.bids,
                        game.getConventionProfile(), game.getConventionData(), game.getCardsConventionProfile(), game.getCardsConventionData(),
                        game.getTournament().getResultType(), engineVersion);
            }
            break;
            // GENERIC OR NOT SUPPORTED
            default: {
                TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(param.tournamentCategory);
                if (tournamentMgr != null) {
                    String tourID = TournamentGenericMgr.extractTourIDFromDealID(param.dealIDstr);
                    int dealIndex = TournamentGenericMgr.extractDealIndexFromDealID(param.dealIDstr);
                    Game game = tournamentMgr.getGameOnTournamentAndDealForPlayer(tourID, dealIndex, param.playerID);
                    int engineVersion = ContextManager.getArgineEngineMgr().getEngineVersion(game.getTournament().getCategory());
                    if (game == null) {
                        log.error("No game " + tournamentMgr.getTournamentCategoryName() + " found - param=" + param + " - tourID=" + tourID + " - dealIndex=" + dealIndex);
                        throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                    }
                    resp.information = tournamentMgr.getGameMgr().getEngine().getBidInfoFullData(
                            game.getDeal().dealer, game.getDeal().vulnerability, param.bids,
                            game.getConventionProfile(), game.getConventionData(), game.getCardsConventionProfile(), game.getCardsConventionData(),
                            game.getTournament().getResultType(), engineVersion);
                } else {
                    log.error("Tournament Category not supported - category=" + param.tournamentCategory);
                }
            }
            break;
        }

        if (resp.information != null) {
            String[] dataReglette = resp.information.split(";");
            if (dataReglette.length >= 13) {
                ArgineEngineMgr engineMgr = ContextManager.getArgineEngineMgr();
                if (FBConfiguration.getInstance().getIntValue("engine.bidInfoBuildAllText", 0) == 1) {
                    // NB CARD CLUB
                    resp.clubText = engineMgr.getTextColor(dataReglette[0], dataReglette[1], param.lang);
                    // NB CARD DIAMOND
                    resp.diamondText = engineMgr.getTextColor(dataReglette[2], dataReglette[3], param.lang);
                    // NB CARD HEART
                    resp.heartText = engineMgr.getTextColor(dataReglette[4], dataReglette[5], param.lang);
                    // NB CARD SPADE
                    resp.spadeText = engineMgr.getTextColor(dataReglette[6], dataReglette[7], param.lang);
                    // POINTS
                    resp.numPointsText = engineMgr.getTextPoint(dataReglette[8], dataReglette[9], param.lang);
                }
                if (dataReglette[12].startsWith("#")) {
                    // argine text begins with '#'
                    resp.text = ContextManager.getArgineEngineMgr().decodeText(dataReglette[12].substring(1), param.lang);
                    // get info forcing
                    if (dataReglette.length >= 14) {
                        int forcingValue = 0;
                        try {
                            forcingValue = Integer.parseInt(dataReglette[13]);
                        } catch (Exception e) {
                            log.error("Failed to parseInt for string=" + dataReglette[13]);
                        }
                        if (forcingValue > 0) {
                            ArgineTextElement ate = ContextManager.getArgineEngineMgr().getArgineText("forcing" + forcingValue);
                            if (ate != null) {
                                resp.forcing = ate.getTextForLang(param.lang);
                                resp.alert = forcingValue >= 20;
                            }
                        }
                    }
                } else {
                    log.error("NOT Argine text info !! Need to start ");
                }
            }
        } else {
            log.error("No bid information found ... param=" + param);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        return resp;
    }

    @Override
    public FBWSResponse playCard(String sessionID, PlayCardParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            String sessionStr = null;
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                response.setData(processPlayCard(session, param));
            } catch (FBWSException e) {
                log.error("FBWSException : " + e.getMessage() + " - session=" + sessionStr + " - param=" + param, e);
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public PlayCardResponse processPlayCard(FBSession session, PlayCardParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        long gameID = Constantes.getIDLongValue(param.gameIDstr, param.gameID);
        synchronized (lockPlayerGameAction.getLock("" + session.getPlayer().getID())) {
            GameType gameType = checkGameSessionAndReturnGameType(session, gameID, param.gameIDstr);
            if (gameType == GameType.Partner) {
                TournamentTable2 table2 = tournamentTrainingPartnerMgr.getForTableAndGame(session.getCurrentTrainingPartnerTableID(), gameID);
                if (table2 != null) {
                    tournamentTrainingPartnerMgr.checkTournament(table2);
                    boolean bStartThread = tournamentGame2Mgr.playCard(table2, param.card, session.getPlayer().getID(), param.step);
                    if (bStartThread) {
                        tournamentGame2Mgr.startGameThread(table2, false);
                    }
                } else {
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
            } else {
                GameMgr gameMgr = getGameMgr(gameType);
                if (gameMgr != null) {
                    gameMgr.getTournamentMgr().checkTournamentMode(session.getCurrentGameTable().isReplay(), 0);
                    boolean bStartThread = gameMgr.playCard(session, param.card, param.step);
                    if (bStartThread) {
                        gameMgr.startThreadPlay(session, false, 0, null, 0, null);
                    }
                } else {
                    log.error("No gameMgr found for gameType=" + gameType + " - param=" + param + " - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
            }
        }
        PlayCardResponse resp = new PlayCardResponse();
        resp.status = true;
        return resp;
    }

    @Override
    public FBWSResponse setClaimSpreadResponse(String sessionID, SetClaimSpreadResponseParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                response.setData(processSetClaimSpreadResponse(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public SetClaimSpreadResponseResponse processSetClaimSpreadResponse(FBSession session, SetClaimSpreadResponseParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        long gameID = Constantes.getIDLongValue(param.gameIDstr, param.gameID);
        synchronized (lockPlayerGameAction.getLock("" + session.getPlayer().getID())) {
            GameType gameType = checkGameSessionAndReturnGameType(session, gameID, param.gameIDstr);
            if (gameType == GameType.Partner) {
                TournamentTable2 table2 = tournamentTrainingPartnerMgr.getForTableAndGame(session.getCurrentTrainingPartnerTableID(), gameID);
                if (table2 != null) {
                    boolean bStartThread = tournamentGame2Mgr.setClaimSpreadResponse(table2, session.getPlayer().getID(), param.response);
                    if (bStartThread) {
                        tournamentGame2Mgr.startGameThread(table2, false);
                    }
                } else {
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
            } else {
                GameMgr gameMgr = getGameMgr(gameType);
                if (gameMgr != null) {
                    gameMgr.getTournamentMgr().checkTournamentMode(session.getCurrentGameTable().isReplay(), 0);
                    boolean bStartThread = gameMgr.setClaimSpreadResponse(session, param.response);
                    if (bStartThread) {
                        gameMgr.startThreadPlay(session, false, 0, null, 0, null);
                    }
                } else {
                    log.error("No gameMgr found for gameType=" + gameType + " - param=" + param + " - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
            }
        }
        SetClaimSpreadResponseResponse resp = new SetClaimSpreadResponseResponse();
        resp.status = true;
        return resp;
    }

    @Override
    public FBWSResponse viewGame(String sessionID, ViewGameParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                response.setData(processViewGame(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public ViewGameResponse processViewGame(FBSession session, ViewGameParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        ViewGameResponse resp = new ViewGameResponse();
        long gameID = Constantes.getIDLongValue(param.gameIDstr, param.gameID);
        if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
            tourSerieMgr.checkPeriodValid();
            resp.gameView = tourSerieMgr.viewGame(param.gameIDstr, session.getPlayer());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
            serieTopChallengeMgr.checkSerieTopChallengeEnable();
            resp.gameView = serieTopChallengeMgr.viewGame(param.gameIDstr, session.getPlayer());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
            serieEasyChallengeMgr.checkSerieEasyChallengeEnable();
            resp.gameView = serieEasyChallengeMgr.viewGame(param.gameIDstr, session.getPlayer());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_DUEL) {
            resp.gameView = duelMgr.viewGame(param.gameIDstr, session.getPlayer());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER) {
            resp.gameView = tournamentTrainingPartnerMgr.viewGame(gameID, session.getPlayer().getID());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
            resp.gameView = trainingMgr.viewGame(param.gameIDstr, session.getPlayer());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
            resp.gameView = timezoneMgr.viewGame(param.gameIDstr, session.getPlayer());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TEAM) {
            tourTeamMgr.checkPeriodValid();
            resp.gameView = tourTeamMgr.viewGame(param.gameIDstr, session.getPlayer());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_LEARNING) {
            resp.gameView = tourLearningMgr.viewGame(param.gameIDstr, session.getPlayer());
        } else {
            TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory((int) param.categoryID);
            if (tournamentMgr != null) {
                resp.gameView = tournamentMgr.viewGame(param.gameIDstr, session.getPlayer());
            }
        }

        // check profile with client version. For only new profile (> 14)
        if (resp.gameView != null && resp.gameView.game != null) {
            if (resp.gameView.game.conventionProfil > 14 && FBConfiguration.getInstance().getIntValue("general.checkProfileAndVersion.enable", 1) == 1) {
                String clientVersionRequired = FBConfiguration.getInstance().getStringValue("general.checkProfileAndVersion." + session.getDeviceType(), null);
                if (clientVersionRequired != null) {
                    if (StringVersion.compareVersion(clientVersionRequired, session.getClientVersion()) > 0) {
                        String convValue = "";
                        ArgineProfile argineProfile = ContextManager.getArgineEngineMgr().getProfile(resp.gameView.game.conventionProfil);
                        if (argineProfile != null) {
                            convValue = argineProfile.value;
                        }
                        // client version of connection data < client version required
                        resp.gameView.game.conventionProfil = 14;
                        resp.gameView.game.conventionValue = convValue;
                    }
                }
            }
        }
        return resp;
    }

    @Override
    public FBWSResponse viewGameForDealScoreAndContract(String sessionID, ViewGameForDealScoreAndContractParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                response.setData(processViewGameForDealScoreAndContract(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public ViewGameForDealScoreAndContractResponse processViewGameForDealScoreAndContract(FBSession session, ViewGameForDealScoreAndContractParam param) throws FBWSException {
        if (param == null) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        ViewGameForDealScoreAndContractResponse resp = new ViewGameForDealScoreAndContractResponse();
        if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
            tourSerieMgr.checkPeriodValid();
            resp.gameView = tourSerieMgr.viewGameForDealScoreAndContract(param.dealIDstr, param.score, param.contract, param.lead, session.getPlayer());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
            serieTopChallengeMgr.checkSerieTopChallengeEnable();
            resp.gameView = serieTopChallengeMgr.viewGameForDealScoreAndContract(param.dealIDstr, param.score, param.contract, param.lead, session.getPlayer());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
            serieEasyChallengeMgr.checkSerieEasyChallengeEnable();
            resp.gameView = serieEasyChallengeMgr.viewGameForDealScoreAndContract(param.dealIDstr, param.score, param.contract, param.lead, session.getPlayer());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
            resp.gameView = trainingMgr.viewGameForDealScoreAndContract(param.dealIDstr, param.score, param.contract, param.lead, session.getPlayerCache());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
            resp.gameView = timezoneMgr.viewGameForDealScoreAndContract(param.dealIDstr, param.score, param.contract, param.lead, session.getPlayerCache());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_LEARNING) {
            resp.gameView = tourLearningMgr.viewGameForDealScoreAndContract(param.dealIDstr, param.score, param.contract, param.lead, session.getPlayerCache());
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TEAM) {
            tourTeamMgr.checkPeriodValid();
            resp.gameView = tourTeamMgr.viewGameForDealScoreAndContract(param.dealIDstr, param.score, param.contract, param.lead, session.getPlayer());
        } else {
            TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory((int) param.categoryID);
            if (tournamentMgr != null) {
                resp.gameView = tournamentMgr.viewGameForDealScoreAndContract(param.dealIDstr, param.score, param.contract, param.lead, session.getPlayerCache());
            }
        }
        // check profile with client version. For only new profile (> 14)
        if (resp.gameView != null && resp.gameView.game != null) {
            if (resp.gameView.game.conventionProfil > 14 && FBConfiguration.getInstance().getIntValue("general.checkProfileAndVersion.enable", 1) == 1) {
                String clientVersionRequired = FBConfiguration.getInstance().getStringValue("general.checkProfileAndVersion." + session.getDeviceType(), null);
                if (clientVersionRequired != null) {
                    if (StringVersion.compareVersion(clientVersionRequired, session.getClientVersion()) > 0) {
                        String convValue = "";
                        ArgineProfile argineProfile = ContextManager.getArgineEngineMgr().getProfile(resp.gameView.game.conventionProfil);
                        if (argineProfile != null) {
                            convValue = argineProfile.value;
                        }
                        // client version of connection data < client version required
                        resp.gameView.game.conventionProfil = 14;
                        resp.gameView.game.conventionValue = convValue;
                    }
                }
            }
        }
        return resp;
    }

    @Override
    public FBWSResponse resetGame(String sessionID, ResetGameParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                response.setData(processResetGame(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public ResetGameResponse processResetGame(FBSession session, ResetGameParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        ResetGameResponse resp = new ResetGameResponse();
        long gameID = Constantes.getIDLongValue(param.gameIDstr, param.gameID);
        switch (checkGameSessionAndReturnGameType(session, gameID, param.gameIDstr)) {
            case Serie: {
                log.error("No reset on this game ... only for training-partner - param=" + param + " - session=" + session);
                throw new FBWSException(FBExceptionType.COMMON_FEATURE_UNAVAILABLE);
            }
            case Partner: {
                TournamentChallenge tc = tournamentTrainingPartnerMgr.resetGame(session.getCurrentTrainingPartnerTableID(), gameID, session);
                if (tc == null) {
                    log.error("Challenge is null for param=" + param);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                resp.challengeID = tc.getID();
                resp.currentTS = System.currentTimeMillis();
                resp.expirationTS = tc.getDateExpiration();
            }
            break;
            case Unknown:
            default: {
                log.error("No game found in progress for param=" + param + " - session=" + session);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
        }
        return resp;
    }

    @Override
    public FBWSResponse claim(String sessionID, ClaimParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                response.setData(processClaim(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public ClaimResponse processClaim(FBSession session, ClaimParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        ClaimResponse resp = new ClaimResponse();
        long gameID = Constantes.getIDLongValue(param.gameIDstr, param.gameID);
        if (FBConfiguration.getInstance().getIntValue("general.enableClaim", 0) == 1) {
            GameType gameType = checkGameSessionAndReturnGameType(session, gameID, param.gameIDstr);
            if (gameType == GameType.Partner) {
                TournamentTable2 table2 = tournamentTrainingPartnerMgr.getForTableAndGame(session.getCurrentTrainingPartnerTableID(), gameID);
                if (table2 != null) {
                    tournamentTrainingPartnerMgr.checkTournament(table2);
                    resp.result = tournamentGame2Mgr.checkClaim(table2, session.getPlayer().getID(), param.numTricks);
                } else {
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
            } else {
                GameMgr gameMgr = getGameMgr(gameType);
                if (gameMgr != null) {
                    gameMgr.getTournamentMgr().checkTournamentMode(session.getCurrentGameTable().isReplay(), 0);
                    resp.result = gameMgr.checkClaim(session, param.numTricks, param.step);
                } else {
                    log.error("No gameMgr found for gameType=" + gameType + " - param=" + param + " - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
            }
        } else {
            resp.result = false;
        }
        return resp;
    }

    @Override
    public FBWSResponse startGame(String sessionID, StartGameParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                response.setData(processStartGame(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public StartGameResponse processStartGame(FBSession session, StartGameParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // purge existing event for this player and on this game
        FilterEvent filter = new FilterEvent();
        filter.receiverID = session.getPlayer().getID();
        filter.category = Constantes.EVENT_CATEGORY_GAME;
        session.purgeEvent(filter);
        long gameID = Constantes.getIDLongValue(param.gameIDstr, param.gameID);
        synchronized (lockPlayerGameAction.getLock("" + session.getPlayer().getID())) {
            GameType gameType = checkGameSessionAndReturnGameType(session, gameID, param.gameIDstr);
            if (gameType == GameType.Partner) {
                TournamentTable2 table = tournamentTrainingPartnerMgr.getForTableAndGame(session.getCurrentTrainingPartnerTableID(), gameID);
                if (table != null) {
                    synchronized (tournamentGame2Mgr.getLockDataForTable(table.getID())) {
                        // player is ready to play
                        table.setPlayerPlay(session.getPlayer().getID(), true);
                        tournamentTrainingPartnerMgr.changePlayerStatus(table, session.getPlayer().getID(), Constantes.TABLE_PLAYER_STATUS_PRESENT);
                        // begin game ?
                        if (table.isAllPlayerPlay()) {
                            if (FBConfiguration.getInstance().getIntValue("tournament.gameEventAfterStart.enable", 1) == 0) {
                                List<Event> listEvent = new ArrayList<Event>();
                                SpreadGameData spreadData = tournamentGame2Mgr.getSpreadGame(gameID);
                                boolean sendBeginGameWithCurrentPlayer = true;
                                if (spreadData != null) {
                                    // waiting for spread ?
                                    if (!spreadData.isWaitingForCheckSpread()) {
                                        // send BEGIN GAME without current player
                                        TournamentGame2Mgr.addEventGameToList(table, Constantes.EVENT_TYPE_GAME_BEGIN_GAME, null, null, listEvent);
                                        // a spread claim is possible. Send event to ask player agreement
                                        TournamentGame2Mgr.addEventGameToList(table, Constantes.EVENT_TYPE_GAME_SPREAD, Character.toString(spreadData.getRequester()),
                                                null, listEvent);
                                        sendBeginGameWithCurrentPlayer = false;
                                    }
                                }
                                if (sendBeginGameWithCurrentPlayer) {
                                    // send event BEGIN_GAME with current player
                                    TournamentGame2Mgr.addEventGameToList(table, Constantes.EVENT_TYPE_GAME_BEGIN_GAME, Character.toString(table.getCurrentGame().getCurrentPlayer()), null, listEvent);
                                }

                                TournamentGame2Mgr.pushListEvent(listEvent, session, ContextManager.getPresenceMgr().getSessionForPlayer(table.getPartner(session.getPlayer().getID())));
                            }
                            // start thread play
                            tournamentGame2Mgr.startGameThread(table, true);
                        }
                    }
                }
            } else {
                GameMgr gameMgr = getGameMgr(gameType);
                if (gameMgr != null) {
                    gameMgr.getTournamentMgr().checkTournamentMode(session.getCurrentGameTable().isReplay(), session.getPlayer().getID());
                    gameMgr.startThreadPlay(session, true, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
                } else {
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
            }
        }
        StartGameResponse resp = new StartGameResponse();
        resp.result = true;
        return resp;
    }

    @Override
    public FBWSResponse replay(String sessionID, ReplayParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                response.setData(processReplay(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public ReplayResponse processReplay(FBSession session, ReplayParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        WSTableTournament tableTour = new WSTableTournament();
        // if a game is present in session => save it (just set replay parameter to true)
        ContextManager.getTournamentService().checkGameInSessionOnPlayTournament(session, 0, null, null, 0, true);
        if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
            tourSerieMgr.checkPeriodValid();
            Game gamePlayed = tourSerieMgr.getGameOnTournamentAndDealForPlayer(TourSerieMgr.extractTourIDFromDealID(param.dealIDstr), TourSerieMgr.extractDealIndexFromDealID(param.dealIDstr), session.getPlayer().getID());
            // the player must have played this deal
            if (gamePlayed == null || !gamePlayed.isFinished()) {
                throw new FBWSException(FBExceptionType.GAME_REPLAY_DEAL_NOT_PLAYED);
            }
            Table table = tourSerieMgr.createReplayTable(session.getPlayer(), gamePlayed, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
            if (table == null) {
                log.error("Failed to build table ! param=" + param);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            if (param.skipBids) {
                table.getGame().copyBidsFromGame(gamePlayed);
            }
            session.setCurrentGameTable(table);
            tableTour.tournament = tourSerieMgr.toWSTournament((TourSerieTournament) table.getTournament(), session.getPlayerCache());
            tableTour.currentDeal = new WSGameDeal();
            tableTour.currentDeal.setDealData(table.getGame().getDeal(), table.getTournament().getIDStr());
            tableTour.currentDeal.setGameData(table.getGame());
            tableTour.table = table.toWSTableGame();
            tableTour.gameIDstr = table.getGame().getIDStr();
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
            serieTopChallengeMgr.checkSerieTopChallengeEnable();
            Game gamePlayed = serieTopChallengeMgr.getGameOnTournamentAndDealForPlayer(TourSerieMgr.extractTourIDFromDealID(param.dealIDstr), TourSerieMgr.extractDealIndexFromDealID(param.dealIDstr), session.getPlayer().getID());
            // the player must have played this deal
            if (gamePlayed == null || !gamePlayed.isFinished()) {
                throw new FBWSException(FBExceptionType.GAME_REPLAY_DEAL_NOT_PLAYED);
            }
            Table table = serieTopChallengeMgr.createReplayTable(session.getPlayer(), gamePlayed, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
            if (table == null) {
                log.error("Failed to build table ! param=" + param);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            if (param.skipBids) {
                table.getGame().copyBidsFromGame(gamePlayed);
            }
            session.setCurrentGameTable(table);
            tableTour.tournament = serieTopChallengeMgr.serieTournamentToWS((TourSerieTournament) table.getTournament(), session.getPlayerCache());
            tableTour.currentDeal = new WSGameDeal();
            tableTour.currentDeal.setDealData(table.getGame().getDeal(), table.getTournament().getIDStr());
            tableTour.currentDeal.setGameData(table.getGame());
            tableTour.table = table.toWSTableGame();
            tableTour.gameIDstr = table.getGame().getIDStr();
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
            serieEasyChallengeMgr.checkSerieEasyChallengeEnable();
            Game gamePlayed = serieEasyChallengeMgr.getGameOnTournamentAndDealForPlayer(TourSerieMgr.extractTourIDFromDealID(param.dealIDstr), TourSerieMgr.extractDealIndexFromDealID(param.dealIDstr), session.getPlayer().getID());
            // the player must have played this deal
            if (gamePlayed == null || !gamePlayed.isFinished()) {
                throw new FBWSException(FBExceptionType.GAME_REPLAY_DEAL_NOT_PLAYED);
            }
            Table table = serieEasyChallengeMgr.createReplayTable(session.getPlayer(), gamePlayed, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
            if (table == null) {
                log.error("Failed to build table ! param=" + param);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            if (param.skipBids) {
                table.getGame().copyBidsFromGame(gamePlayed);
            }
            session.setCurrentGameTable(table);
            tableTour.tournament = serieEasyChallengeMgr.serieTournamentToWS((TourSerieTournament) table.getTournament(), session.getPlayerCache());
            tableTour.currentDeal = new WSGameDeal();
            tableTour.currentDeal.setDealData(table.getGame().getDeal(), table.getTournament().getIDStr());
            tableTour.currentDeal.setGameData(table.getGame());
            tableTour.table = table.toWSTableGame();
            tableTour.gameIDstr = table.getGame().getIDStr();
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_DUEL) {
            Game gamePlayed = duelMgr.getGameOnTournamentAndDealForPlayer(DuelMgr.extractTourIDFromDealID(param.dealIDstr), DuelMgr.extractDealIndexFromDealID(param.dealIDstr), session.getPlayer().getID());
            // the player must have played this deal
            if (gamePlayed == null || !gamePlayed.isFinished()) {
                throw new FBWSException(FBExceptionType.GAME_REPLAY_DEAL_NOT_PLAYED);
            }
            Table table = duelMgr.createReplayTable(session.getPlayer(), gamePlayed, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
            if (table == null) {
                log.error("Failed to build table ! param=" + param);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            if (param.skipBids) {
                table.getGame().copyBidsFromGame(gamePlayed);
            }
            session.setCurrentGameTable(table);
            tableTour.tournament = duelMgr.toWSTournament((DuelTournament) table.getTournament(), session.getPlayerCache());
            tableTour.currentDeal = new WSGameDeal();
            tableTour.currentDeal.setDealData(table.getGame().getDeal(), table.getTournament().getIDStr());
            tableTour.currentDeal.setGameData(table.getGame());
            tableTour.table = table.toWSTableGame();
            tableTour.gameIDstr = table.getGame().getIDStr();
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
            Game gamePlayed = trainingMgr.getGameOnTournamentAndDealForPlayer(TrainingMgr.extractTourIDFromDealID(param.dealIDstr), TrainingMgr.extractDealIndexFromDealID(param.dealIDstr), session.getPlayer().getID());
            // the player must have played this deal
            if (gamePlayed == null || !gamePlayed.isFinished()) {
                throw new FBWSException(FBExceptionType.GAME_REPLAY_DEAL_NOT_PLAYED);
            }
            Table table = trainingMgr.createReplayTable(session.getPlayer(), gamePlayed, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
            if (table == null) {
                log.error("Category not supported ! param=" + param);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            if (param.skipBids) {
                table.getGame().copyBidsFromGame(gamePlayed);
            }
            session.setCurrentGameTable(table);
            tableTour.tournament = trainingMgr.toWSTournament((TrainingTournament) table.getTournament(), session.getPlayerCache());
            tableTour.currentDeal = new WSGameDeal();
            tableTour.currentDeal.setDealData(table.getGame().getDeal(), table.getTournament().getIDStr());
            tableTour.currentDeal.setGameData(table.getGame());
            tableTour.table = table.toWSTableGame();
            tableTour.gameIDstr = table.getGame().getIDStr();
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TEAM) {
            tourTeamMgr.checkTeamTournamentsEnable();
            Game gamePlayed = tourTeamMgr.getGameOnTournamentAndDealForPlayer(TourTeamMgr.extractTourIDFromDealID(param.dealIDstr), TourTeamMgr.extractDealIndexFromDealID(param.dealIDstr), session.getPlayer().getID());
            // the player must have played this deal
            if (gamePlayed == null || !gamePlayed.isFinished()) {
                throw new FBWSException(FBExceptionType.GAME_REPLAY_DEAL_NOT_PLAYED);
            }
            Table table = tourTeamMgr.createReplayTable(session.getPlayer(), gamePlayed, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
            if (table == null) {
                log.error("Failed to build table ! param=" + param);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            if (param.skipBids) {
                table.getGame().copyBidsFromGame(gamePlayed);
            }
            session.setCurrentGameTable(table);
            tableTour.tournament = tourTeamMgr.toWSTournament((TeamTournament) table.getTournament(), session.getPlayerCache());
            tableTour.currentDeal = new WSGameDeal();
            tableTour.currentDeal.setDealData(table.getGame().getDeal(), table.getTournament().getIDStr());
            tableTour.currentDeal.setGameData(table.getGame());
            tableTour.table = table.toWSTableGame();
            tableTour.gameIDstr = table.getGame().getIDStr();
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
            Game gamePlayed = timezoneMgr.getGameOnTournamentAndDealForPlayer(TimezoneMgr.extractTourIDFromDealID(param.dealIDstr), TimezoneMgr.extractDealIndexFromDealID(param.dealIDstr), session.getPlayer().getID());
            // the player must have played this deal
            if (gamePlayed == null || !gamePlayed.isFinished()) {
                throw new FBWSException(FBExceptionType.GAME_REPLAY_DEAL_NOT_PLAYED);
            }
            Table table = timezoneMgr.createReplayTable(session.getPlayer(), gamePlayed, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
            if (table == null) {
                log.error("Failed to build table ! param=" + param);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            if (param.skipBids) {
                table.getGame().copyBidsFromGame(gamePlayed);
            }
            session.setCurrentGameTable(table);
            tableTour.tournament = timezoneMgr.toWSTournament(table.getTournament(), session.getPlayerCache());
            tableTour.currentDeal = new WSGameDeal();
            tableTour.currentDeal.setDealData(table.getGame().getDeal(), table.getTournament().getIDStr());
            tableTour.currentDeal.setGameData(table.getGame());
            tableTour.table = table.toWSTableGame();
            tableTour.gameIDstr = table.getGame().getIDStr();
        } else {
            TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(param.categoryID);
            if (tournamentMgr != null) {
                Game gamePlayed = tournamentMgr.getGameOnTournamentAndDealForPlayer(TournamentGenericMgr.extractTourIDFromDealID(param.dealIDstr), TournamentGenericMgr.extractDealIndexFromDealID(param.dealIDstr), session.getPlayer().getID());
                // the player must have played this deal
                if (gamePlayed == null || !gamePlayed.isFinished()) {
                    throw new FBWSException(FBExceptionType.GAME_REPLAY_DEAL_NOT_PLAYED);
                }
                Table table = tournamentMgr.createReplayTable(session.getPlayer(), gamePlayed, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
                if (table == null) {
                    log.error("Failed to build table ! param=" + param);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                if (param.skipBids) {
                    table.getGame().copyBidsFromGame(gamePlayed);
                }
                session.setCurrentGameTable(table);
                tableTour.tournament = tournamentMgr.toWSTournament(table.getTournament(), session.getPlayerCache());
                tableTour.currentDeal = new WSGameDeal();
                tableTour.currentDeal.setDealData(table.getGame().getDeal(), table.getTournament().getIDStr());
                tableTour.currentDeal.setGameData(table.getGame());
                tableTour.table = table.toWSTableGame();
                tableTour.gameIDstr = table.getGame().getIDStr();
            } else {
                log.error("Category not supported ! - param=" + param);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
        }
        tableTour.conventionProfil = param.conventionProfil;
        tableTour.conventionValue = param.conventionValue;
        tableTour.creditAmount = session.getPlayer().getTotalCreditAmount();

        tableTour.nbPlayedDeals = session.getPlayer().getNbPlayedDeals();

        tableTour.replayEnabled = playerMgr.isReplayEnabledForPlayer(session);
        tableTour.freemium = session.isFreemium();
        ReplayResponse resp = new ReplayResponse();
        resp.tableTournament = tableTour;
        session.incrementNbDealReplay(param.categoryID, 1);
        return resp;
    }

    public FBWSResponse getDummyCards(String sessionID, GetDummyCardsParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                String distrib = null;
                char declarer = '?';
                char playerPosition = '?';
                if (session.getCurrentGameTable() != null &&
                        session.getCurrentGameTable().getGame() != null &&
                        session.getCurrentGameTable().getGame().getIDStr().equals(param.gameIDstr)) {
                    // check game status : bid end and first card played
                    if (!session.getCurrentGameTable().getGame().isEndBid() || session.getCurrentGameTable().getGame().getListCard().size() == 0) {
                        log.error("Game status not valid : bids not ended or first card not yet played - param=" + param + " - game=" + session.getCurrentGameTable().getGame());
                        throw new FBWSException(FBExceptionType.GAME_DISPLAY_DUMMY_NOT_VALID);
                    }
                    distrib = session.getCurrentGameTable().getGame().getDeal().cards;
                    declarer = session.getCurrentGameTable().getGame().getDeclarer();
                    playerPosition = session.getCurrentGameTable().getPlayerPosition(session.getPlayer().getID());
                }
                if (distrib == null) {
                    log.error("No game found in progress for param=" + param);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (declarer == '?' || playerPosition == '?') {
                    log.error("declarer or game not valid for param=" + param + " - declarer=" + declarer + " - playerPosition=" + playerPosition);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

                if (declarer == BridgeConstantes.POSITION_EAST) {
                    distrib = distrib.replaceAll(Character.toString(BridgeConstantes.POSITION_EAST), "-");
                    distrib = distrib.replaceAll(Character.toString(BridgeConstantes.POSITION_NORTH), "-");
                } else if (declarer == BridgeConstantes.POSITION_WEST) {
                    distrib = distrib.replaceAll(Character.toString(BridgeConstantes.POSITION_WEST), "-");
                    distrib = distrib.replaceAll(Character.toString(BridgeConstantes.POSITION_NORTH), "-");
                } else if (declarer == BridgeConstantes.POSITION_NORTH || declarer == BridgeConstantes.POSITION_SOUTH) {
                    distrib = distrib.replaceAll(Character.toString(BridgeConstantes.POSITION_EAST), "-");
                    distrib = distrib.replaceAll(Character.toString(BridgeConstantes.POSITION_WEST), "-");
                }
                GetDummyCardsResponse resp = new GetDummyCardsResponse();
                resp.distributionDummy = distrib;
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @Override
    public FBWSResponse getArgineAdvice(String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID());
                }
                response.setData(processGetArgineAdvice(session));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("sessionID is null");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetArgineAdviceResponse processGetArgineAdvice(FBSession session) throws FBWSException {
        GetArgineAdviceResponse resp = new GetArgineAdviceResponse();
        Table table = session.getCurrentGameTable();
        if (table != null) {
            Game game = table.getGame();
            if (game != null) {
                synchronized (lockPlayerGameAction.getLock("" + session.getPlayer().getID())) {
                    GameType gameType = checkGameSessionAndReturnGameType(session, 0, game.getIDStr()); // No GameID (long) : Training_partner is not supported
                    GameMgr gameMgr = getGameMgr(gameType);
                    if (gameMgr != null) {
                        resp.result = gameMgr.getArgineAdvice(session);
                    } else {
                        log.warn("No gameMgr found for gameType=" + gameType + " - session=" + session);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR, "No gameMgr found for gameType=" + gameType + " - session=" + session);
                    }
                }
            } else {
                log.warn("No game found in session=" + session);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR, "No game found in session=" + session);
            }
        } else {
            log.warn("No table found in session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR, "No table found in session=" + session);
        }
        return resp;
    }

    public enum GameType {
        Unknown, Partner, Serie, SerieTopChallenge, SerieEasyChallenge, Timezone, Training, Duel, TourTeam, TourCBO, Private, TourLearning
    }
}
