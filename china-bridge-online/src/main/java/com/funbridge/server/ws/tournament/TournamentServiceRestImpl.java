package com.funbridge.server.ws.tournament;

import com.funbridge.server.common.*;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.player.data.PlayerDuel;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.tournament.TournamentChallengeMgr;
import com.funbridge.server.tournament.TournamentMgr;
import com.funbridge.server.tournament.category.TournamentTrainingPartnerMgr;
import com.funbridge.server.tournament.data.TournamentChallenge;
import com.funbridge.server.tournament.data.TournamentDeal;
import com.funbridge.server.tournament.data.TournamentGame2;
import com.funbridge.server.tournament.data.TournamentTable2;
import com.funbridge.server.tournament.duel.DuelMgr;
import com.funbridge.server.tournament.duel.data.DuelGame;
import com.funbridge.server.tournament.duel.data.DuelTournamentPlayer;
import com.funbridge.server.tournament.federation.FederationMgr;
import com.funbridge.server.tournament.federation.RegisterTourFederationResult;
import com.funbridge.server.tournament.federation.TourFederationMgr;
import com.funbridge.server.tournament.federation.data.PlayerFederation;
import com.funbridge.server.tournament.federation.cbo.TourCBOMgr;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.game.GameMgr;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;
import com.funbridge.server.tournament.learning.TourLearningMgr;
import com.funbridge.server.tournament.learning.data.LearningGame;
import com.funbridge.server.tournament.learning.data.LearningProgression;
import com.funbridge.server.tournament.learning.data.LearningTournament;
import com.funbridge.server.tournament.privatetournament.PrivateTournamentMgr;
import com.funbridge.server.tournament.privatetournament.data.PrivateTournament;
import com.funbridge.server.tournament.privatetournament.data.PrivateTournamentProperties;
import com.funbridge.server.tournament.serie.SerieEasyChallengeMgr;
import com.funbridge.server.tournament.serie.SerieTopChallengeMgr;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.serie.data.SerieEasyChallengeGame;
import com.funbridge.server.tournament.serie.data.SerieTopChallengeGame;
import com.funbridge.server.tournament.serie.data.TourSerieGame;
import com.funbridge.server.tournament.serie.data.TourSerieTournament;
import com.funbridge.server.tournament.serie.memory.TourSerieMemTour;
import com.funbridge.server.tournament.team.TourTeamMgr;
import com.funbridge.server.tournament.team.data.TeamGame;
import com.funbridge.server.tournament.team.memory.TeamMemTournament;
import com.funbridge.server.tournament.timezone.TimezoneMgr;
import com.funbridge.server.tournament.training.TrainingMgr;
import com.funbridge.server.ws.*;
import com.funbridge.server.ws.game.WSGameDeal;
import com.funbridge.server.ws.game.WSGamePlayer;
import com.funbridge.server.ws.player.WSPlayerLinked;
import com.gotogames.common.bridge.GameBridgeRule;
import com.gotogames.common.tools.StringTools;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service(value="tournamentService")
@Scope(value="singleton")
public class TournamentServiceRestImpl extends FunbridgeMgr implements TournamentServiceRest {
	@Resource(name="tournamentMgr")
	private TournamentMgr tournamentMgr = null;
	@Resource(name="tournamentChallengeMgr")
	private TournamentChallengeMgr tournamentChallengeMgr = null;
	@Resource(name="playerMgr")
	private PlayerMgr playerMgr = null;
	@Resource(name="tournamentTrainingPartnerMgr")
	private TournamentTrainingPartnerMgr tournamentTrainingPartnerMgr = null;
    @Resource(name="duelMgr")
    private DuelMgr duelMgr = null;
	@Resource(name="timezoneMgr")
	private TimezoneMgr timezoneMgr = null;
    @Resource(name="trainingMgr")
    private TrainingMgr trainingMgr = null;
    @Resource(name="tourSerieMgr")
    private TourSerieMgr tourSerieMgr = null;
    @Resource(name = "playerCacheMgr")
    private PlayerCacheMgr playerCacheMgr = null;
	@Resource(name="serieTopChallengeMgr")
	private SerieTopChallengeMgr serieTopChallengeMgr = null;
    @Resource(name="serieEasyChallengeMgr")
    private SerieEasyChallengeMgr serieEasyChallengeMgr = null;
	@Resource(name="tourCBOMgr")
	private TourCBOMgr tourCBOMgr = null;
    @Resource(name="tourTeamMgr")
    private TourTeamMgr tourTeamMgr = null;
    @Resource(name = "privateTournamentMgr")
    private PrivateTournamentMgr privateTournamentMgr = null;
	@Resource(name = "tourLearningMgr")
	private TourLearningMgr tourLearningMgr = null;

	private LockString lockPlayTournament = new LockString();
	
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

	public FBWSResponse getGameByCategoryAndID(final String sessionID, final GetGameByCategoryAndIDParam param) {
        final FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetGameByCategoryAndID(session, param));
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

    public Game processGetGameByCategoryAndID(final FBSession session, final GetGameByCategoryAndIDParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        final TournamentGenericMgr manager = ContextManager.getTournamentMgrForCategory(param.categoryID);
        if(manager == null)  {
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR, "The tournament manager is null!");
        } else {
            return manager.getGame(param.gameID);
        }
    }

	public void addPlayerSetCategory(int category, long playerID) {
        // remove player from others category
		if (category != Constantes.TOURNAMENT_CATEGORY_TRAINING) {trainingMgr.getGameMgr().removePlayerRunning(playerID);}
		if (category != Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {timezoneMgr.getGameMgr().removePlayerRunning(playerID);}
        if (category != Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {tourSerieMgr.getGameMgr().removePlayerRunning(playerID);}
        if (category != Constantes.TOURNAMENT_CATEGORY_DUEL) {duelMgr.getGameMgr().removePlayerRunning(playerID);}
		if (category != Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {serieTopChallengeMgr.getGameMgr().removePlayerRunning(playerID);}
        if (category != Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {serieEasyChallengeMgr.getGameMgr().removePlayerRunning(playerID);}
		if (category != Constantes.TOURNAMENT_CATEGORY_TOUR_CBO) {
            tourCBOMgr.getGameMgr().removePlayerRunning(playerID);}
        if (category != Constantes.TOURNAMENT_CATEGORY_PRIVATE) {privateTournamentMgr.getGameMgr().removePlayerRunning(playerID);}
		if (category != Constantes.TOURNAMENT_CATEGORY_LEARNING) {tourLearningMgr.getGameMgr().removePlayerRunning(playerID);}
		// add player in the category set
        GameMgr gameMgr = ContextManager.getTournamentGameMgr(category);
        if (gameMgr != null) {
            gameMgr.addPlayerRunning(playerID);
        } else {
            if(category != Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER){
                log.error("No gameMgr found for category="+category);
            }
        }
	}
	
	@Override
	public FBWSResponse leaveTournament(String sessionID, LeaveTournamentParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);

				response.setData(processLeaveTournament(session, param));
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

	public LeaveTournamentResponse processLeaveTournament(FBSession session, LeaveTournamentParam param) throws FBWSException{
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        LeaveTournamentResponse resp = new LeaveTournamentResponse();
        if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
            tourSerieMgr.checkPeriodValid();
            resp.nbCredit = tourSerieMgr.leaveTournament(session, param.tournamentIDstr);
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
            serieTopChallengeMgr.checkSerieTopChallengeEnable();
            resp.nbCredit = serieTopChallengeMgr.leaveTournament(session, param.tournamentIDstr);
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
            serieEasyChallengeMgr.checkSerieEasyChallengeEnable();
            resp.nbCredit = serieEasyChallengeMgr.leaveTournament(session, param.tournamentIDstr);
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
            log.error("No leave tournament for training ... leave deal only");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_DUEL) {
            resp.nbCredit = duelMgr.leaveTournament(session, param.tournamentIDstr);
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TEAM) {
            tourTeamMgr.checkTeamTournamentsEnable();
            resp.nbCredit = tourTeamMgr.leaveTournament(session, param.tournamentIDstr);
        } else if (param.categoryID == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
            resp.nbCredit = timezoneMgr.leaveTournament(session, param.tournamentIDstr);
        } else {
            TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(param.categoryID);
            if (tournamentMgr != null) {
                resp.nbCredit = tournamentMgr.leaveTournament(session, param.tournamentIDstr);
            }
        }
        return resp;
    }

	@Override
	public FBWSResponse createChallenge(String sessionID, CreateChallengeParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processCreateChallenge(session, param));
			} catch (FBWSException e) {
			    if (e.getLocalizedMessage() != null && e.getLocalizedMessage().length() > 0) {
                    response.setException(new FBWSExceptionRest(e.getType(), e.localizedMessage));
                } else {
                    response.setException(new FBWSExceptionRest(e.getType()));
                }
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

	public CreateChallengeResponse processCreateChallenge(FBSession session, CreateChallengeParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        if (FBConfiguration.getInstance().getIntValue("tournament.TRAINING_PARTNER.enable", 0) == 0) {
            throw new FBWSException(FBExceptionType.COMMON_DISABLED, ContextManager.getTextUIMgr().getTextUIForLang("trainingPartnerUnavailable", session.getPlayer().getDisplayLang()));
        }
        if (param.categoryID != Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        Player partner = playerMgr.getPlayer(param.partnerID);
        if (partner == null) {
            throw new FBWSException(FBExceptionType.PLAYER_NOT_EXISTING);
        }

        TournamentChallenge tourCha = tournamentChallengeMgr.createChallenge(session.getPlayer(), partner, param.settings);
        CreateChallengeResponse resp = new CreateChallengeResponse();
        resp.challengeID = tourCha.getID();
        resp.currentTS = System.currentTimeMillis();
        resp.expirationTS = tourCha.getDateExpiration();
        return resp;
    }

	@Override
	public FBWSResponse setChallengeResponse(String sessionID,	SetChallengeResponseParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetChallengeResponse(session, param));
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

	public SetChallengeResponseResponse processSetChallengeResponse(FBSession session, SetChallengeResponseParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        tournamentChallengeMgr.setChallengeResponse(session.getPlayer(), param.challengeID, param.response);
        SetChallengeResponseResponse resp = new SetChallengeResponseResponse();
        resp.result = true;
        return resp;
    }

	@Override
	public FBWSResponse playTournamentChallenge(String sessionID, PlayTournamentChallengeParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
				response.setData(processPlayTournamentChallenge(session, param));
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

	public PlayTournamentChallengeResponse processPlayTournamentChallenge(FBSession session, PlayTournamentChallengeParam param) throws FBWSException{
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        Player p = session.getPlayer();

        // check challenge exist
        TournamentChallenge tc = tournamentChallengeMgr.getChallengeForID(param.challengeID);
        if (tc == null) {
            log.error("No challenge with challengeID="+param.challengeID);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (tc.isEnded()) {
            log.error("Challenge has expired ! tc="+tc);
            throw new FBWSException(FBExceptionType.TOURNAMENT_CHALLENGE_EXPIRATION);
        }

        // check player is valid for this challenge
        if (tc.getPartner().getID() != p.getID() && tc.getCreator().getID() != p.getID()) {
            log.error("Player is not for this challenge ! challenge="+tc+" - player="+p.getID());
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        // check challenge status
        if (tc.getStatus() != Constantes.TOURNAMENT_CHALLENGE_STATUS_WAITING &&
                tc.getStatus() != Constantes.TOURNAMENT_CHALLENGE_STATUS_PLAY) {
            log.error("Challenge status not valid ! challenge="+tc+" - player="+p);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }

        PlayTournamentChallengeResponse resp = new PlayTournamentChallengeResponse();
        // treat only one response at time for each challenge
        synchronized (tc) {
            TournamentTable2 table = tournamentTrainingPartnerMgr.playChallenge(tc, session.getPlayer());
            resp.tableTournament = new WSTableTournament();
            resp.tableTournament.table = tournamentTrainingPartnerMgr.table2WS(table, session.getPlayer().getID());
            resp.tableTournament.tournament = table.getTournament().toWS();
            resp.tableTournament.tournament.setArrayDeal(tournamentMgr.getArrayDealIDForTournament(table.getTournament().getID()));
            resp.tableTournament.tournament.currentDealIndex = 1;
            resp.tableTournament.currentDeal = new WSGameDeal();
            resp.tableTournament.nbPlayedDeals = session.getPlayer().getNbPlayedDeals();
            resp.tableTournament.replayEnabled = playerMgr.isReplayEnabledForPlayer(session);
            resp.tableTournament.freemium = session.isFreemium();
            if (table.getCurrentGame() != null) {
                resp.tableTournament.currentDeal.setDealData(table.getCurrentGame().getDeal());
                resp.tableTournament.currentDeal.setGameData(table.getCurrentGame());
            } else {
                TournamentDeal deal = tournamentMgr.getDealForTournamentAndIndex(table.getTournament(), 1);
                resp.tableTournament.currentDeal.setDealData(deal);
                resp.tableTournament.currentDeal.currentPlayer = Character.toString(GameBridgeRule.getNextPositionToPlay(deal.getDistribution().getDealer(), null, null, null));
            }
        }
        return resp;
    }
	
	@Override
	public FBWSResponse getTrainingPartners(String sessionID, GetTrainingPartnersParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetTrainingPartners(session, param));
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

	public GetTrainingPartnersResponse processGetTrainingPartners(FBSession session, GetTrainingPartnersParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        GetTrainingPartnersResponse resp = new GetTrainingPartnersResponse();
        resp.offset = param.offset;
        resp.totalSize = playerMgr.countLinkForPlayerAndType(session.getPlayer().getID(), Constantes.PLAYER_LINK_TYPE_FRIEND);
        resp.results = new ArrayList<WSTrainingPartner>();
        // get list friend of player
        List<WSPlayerLinked> listFriend = playerMgr.getListTrainingPartner(session.getPlayer().getID(), param.offset, param.nbMax);
        // list of challenge with player
        List<TournamentChallenge> listChallenge = tournamentChallengeMgr.getChallengeForPlayer(session.getPlayer().getID());
        if (listFriend != null && listFriend.size() > 0) {
            for (WSPlayerLinked pl : listFriend) {
                WSTrainingPartner t = new WSTrainingPartner();
                t.playerID = pl.playerID;
                t.pseudo = pl.pseudo;
                t.avatar = pl.avatar;
                t.connected = pl.connected;
                if (listChallenge != null) {
                    // check if a challenge exist with this friend
                    for (TournamentChallenge tc : listChallenge) {
                        if (!tc.isEnded() && (tc.getPartner().getID() == pl.playerID || tc.getCreator().getID() == pl.playerID)) {
                            t.challengeID = tc.getID();
                            t.challengeStatus = tc.getStatus();
                            t.creatorID = tc.getCreator().getID();
                            t.dealSettings = tournamentMgr.getTournamentSettings(tc.getSettings());
                        }
                    }
                }
                resp.results.add(t);
            }
        }
        return resp;
    }

	@Override
	public FBWSResponse requestDuel(String sessionID, RequestDuelParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
				response.setData(processRequestDuel(session, param));
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

	public RequestDuelResponse processRequestDuel(FBSession session, RequestDuelParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        RequestDuelResponse resp = new RequestDuelResponse();
        if (param.playerID == Constantes.PLAYER_ARGINE_ID) {
            // case of duel with Argine
            resp.duelHistory = duelMgr.requestDuelWithArgine(session.getPlayer());
            if (!resp.duelHistory.arginePlayAll) {
                duelMgr.startPlayArgine(resp.duelHistory.tourIDstr);
            }
        } else {
            Player p2 = playerMgr.getPlayer(param.playerID);
            if (p2 == null) {
                log.error("No player found with id=" + param.playerID);
                throw new FBWSException(FBExceptionType.PLAYER_NOT_EXISTING);
            }
            resp.duelHistory = duelMgr.requestDuelBetweenPlayer(session.getPlayer(), p2);
        }
        return resp;
    }

	@Override
	public FBWSResponse answerDuelRequest(String sessionID, AnswerDuelRequestParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processAnswerDuelRequest(session, param));
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

	public AnswerDuelRequestResponse processAnswerDuelRequest(FBSession session, AnswerDuelRequestParam param) throws FBWSException{
	    if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        AnswerDuelRequestResponse resp = new AnswerDuelRequestResponse();
        long playerDuelID = Constantes.getIDLongValue(param.playerDuelIDstr, param.playerDuelID);
        resp.duelHistory = duelMgr.answerRequestDuel(playerDuelID, session.getPlayer(), param.answer);
        if (param.answer) {
            session.addDuelInProgress(playerDuelID);
        }
        session.removeDuelRequest(playerDuelID);
        return resp;
    }

	@Override
	public FBWSResponse getDuelHistory(String sessionID, GetDuelHistoryParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetDuelHistory(session, param));
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

	public GetDuelHistoryResponse processGetDuelHistory(FBSession session, GetDuelHistoryParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        long playerDuelID = Constantes.getIDLongValue(param.playerDuelIDstr, param.playerDuelID);
        PlayerDuel playerDuel = duelMgr.getPlayerDuel(playerDuelID);
        if (playerDuel == null) {
            log.error("No playerDuel found for ID="+playerDuelID);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (!playerDuel.isPlayerDuel(session.getPlayer().getID())) {
            log.error("Player not part of this duel playerDuel=" + playerDuel + " - player=" + session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        GetDuelHistoryResponse resp = new GetDuelHistoryResponse();
        resp.duelHistory = duelMgr.createDuelHistory(playerDuel, session.getPlayer().getID(), null, true);
        resp.listTournamentResultDuel = duelMgr.listTournamentDuelResult(playerDuel, session.getPlayer().getID());
        return resp;
    }

	@Override
	public FBWSResponse getDuel(String sessionID, GetDuelParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetDuel(session, param));
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

	public GetDuelResponse processGetDuel(FBSession session, GetDuelParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        // check tournamentDuel exist
        DuelTournamentPlayer duelTournamentPlayer = duelMgr.getTournamentPlayer(param.tourIDstr, session.getPlayer().getID());
        if (duelTournamentPlayer ==  null) {
            log.error("No tournamentDuel found for this tourID=" + param.tourIDstr + " and player=" + session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        GetDuelResponse resp = new GetDuelResponse();
        resp.tournamentDuel = new WSTournamentDuel();
        resp.tournamentDuel.player1 = WSGamePlayer.createGamePlayerHuman(playerCacheMgr.getPlayerCache(duelTournamentPlayer.getPlayer1IDForAsk(session.getPlayer().getID())), session.getPlayer().getID());
        resp.tournamentDuel.player1.connected = true;
        resp.tournamentDuel.player2 = WSGamePlayer.createGamePlayerHuman(playerCacheMgr.getPlayerCache(duelTournamentPlayer.getPlayer2IDForAsk(session.getPlayer().getID())), session.getPlayer().getID());
        resp.tournamentDuel.player2.connected = ContextManager.getPresenceMgr().isSessionForPlayerID(resp.tournamentDuel.player2.playerID);
        resp.tournamentDuel.tourIDstr = duelTournamentPlayer.getTournament().getIDStr();
        resp.tournamentDuel.setPlayerDuelID(duelTournamentPlayer.getPlayerDuelID());
        resp.tournamentDuel.nbDeal = duelTournamentPlayer.getTournament().getNbDeals();
        resp.tournamentDuel.listDeal = duelMgr.getListDealForTournament(duelTournamentPlayer, session.getPlayer().getID());
        return resp;
    }

    /**
     * Call on all playTournament methods. Save if necessary current game.
     * @param session
     * @param categoryPlayTournament
     * @param tourID
     * @param authorID only used for tounament comments
     * @param resultType
     * @param replay if replay mode, current game is save
     */
    public void checkGameInSessionOnPlayTournament(FBSession session, int categoryPlayTournament, String tourID, String authorID, int resultType, boolean replay) {
        // NEW TABLE & GAME
        if (session.getCurrentGameTable() != null && !session.getCurrentGameTable().isReplay() && session.getCurrentGameTable().getTournament() != null) {
            boolean saveGame = false;
            if (replay) {
                saveGame = true;
            } else {
                if (session.getCurrentGameTable().getTournament().getCategory() != categoryPlayTournament) {
                    saveGame = true;
                }
                // same category
                else {
                    if (categoryPlayTournament == Constantes.TOURNAMENT_CATEGORY_LEARNING){
                        if (authorID != null && !((LearningTournament)session.getCurrentGameTable().getTournament()).getChapterID().equals(authorID)) {
                            saveGame = true;
                        }
                    } else {
                        if (tourID != null && !tourID.equals(session.getCurrentGameTable().getTournament().getIDStr())) {
                            saveGame = true;
                        }
                        if (resultType != 0 && session.getCurrentGameTable().getTournament().getResultType() != resultType) {
                            saveGame = true;
                        }
                    }
                }
            }
            if (saveGame) {
                try {
                    if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_NEWSERIE &&
                            session.getCurrentGameTable().getGame() != null) {
                        tourSerieMgr.checkGame(session.getCurrentGameTable().getGame());
                        tourSerieMgr.updateGameDB((TourSerieGame) session.getCurrentGameTable().getGame());
                    } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TRAINING &&
                            session.getCurrentGameTable().getGame() != null) {
                        trainingMgr.checkGame(session.getCurrentGameTable().getGame());
                        trainingMgr.updateGameDB(session.getCurrentGameTable().getGame());
                    } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TIMEZONE &&
                            session.getCurrentGameTable().getGame() != null) {
                        timezoneMgr.checkGame(session.getCurrentGameTable().getGame());
                        timezoneMgr.updateGameDB(session.getCurrentGameTable().getGame());
                    } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_DUEL &&
                            session.getCurrentGameTable().getGame() != null) {
                        duelMgr.checkGame(session.getCurrentGameTable().getGame());
                        duelMgr.updateGameDB((DuelGame)session.getCurrentGameTable().getGame());
                    } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE &&
							session.getCurrentGameTable().getGame() != null) {
						serieTopChallengeMgr.updateGameDB((SerieTopChallengeGame)session.getCurrentGameTable().getGame());
					} else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE &&
                            session.getCurrentGameTable().getGame() != null) {
                        serieEasyChallengeMgr.updateGameDB((SerieEasyChallengeGame)session.getCurrentGameTable().getGame());
                    } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TEAM &&
                            session.getCurrentGameTable().getGame() != null) {
                        tourTeamMgr.checkGame(session.getCurrentGameTable().getGame());
                        tourTeamMgr.updateGameDB((TeamGame) session.getCurrentGameTable().getGame());
                    } else if (session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_LEARNING &&
                            session.getCurrentGameTable().getGame() != null) {
                        tourLearningMgr.updateGameDB(session.getCurrentGameTable().getGame());
                    } else if (session.getCurrentGameTable().getGame() != null) {
						TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(session.getCurrentGameTable().getTournament().getCategory());
						if (tournamentMgr != null) {
							tournamentMgr.checkGame(session.getCurrentGameTable().getGame());
							tournamentMgr.updateGameDB(session.getCurrentGameTable().getGame());
						}
					}
                } catch (Exception e) {
                    log.error("Exception to update game="+session.getCurrentGameTable().getGame(), e);
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("GameTable in session is null - table=" + session.getCurrentGameTable());
            }
        }
	}

	public Object getLockPlayTournament(long playerID) {
        return lockPlayTournament.getLock(""+playerID);
    }

	@Override
	public FBWSResponse playTournamentTraining(String sessionID, PlayTournamentTrainingParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processPlayTournamentTraining(session, param));
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

	public PlayTournamentTrainingResponse processPlayTournamentTraining(FBSession session, PlayTournamentTrainingParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        if (param.resultType != Constantes.TOURNAMENT_RESULT_IMP && param.resultType != Constantes.TOURNAMENT_RESULT_PAIRE) {
            log.error("Param resultType not valid ! param="+param);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // check if a current game exist in session for a different category
        checkGameInSessionOnPlayTournament(session, Constantes.TOURNAMENT_CATEGORY_TRAINING, null, null, param.resultType, false);
        PlayTournamentTrainingResponse resp = new PlayTournamentTrainingResponse();
        synchronized (getLockPlayTournament(session.getPlayer().getID())) {
            resp.tableTournament = trainingMgr.playTournament(session, param.resultType, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
        }
        addPlayerSetCategory(Constantes.TOURNAMENT_CATEGORY_TRAINING, session.getPlayer().getID());
        return resp;
    }

    @Override
    public FBWSResponse playTournamentSerie2(String sessionID, PlayTournamentSerieParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
				// check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processPlayTournamentSerie2(session, param));
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

    public PlayTournamentSerieResponse processPlayTournamentSerie2(FBSession session, PlayTournamentSerieParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        // check if a current game exist in session for a different category ??????????????????
        checkGameInSessionOnPlayTournament(session, Constantes.TOURNAMENT_CATEGORY_NEWSERIE, null, null, 0, false);
        tourSerieMgr.checkPeriodValid();
        PlayTournamentSerieResponse resp = new PlayTournamentSerieResponse();
        synchronized (getLockPlayTournament(session.getPlayer().getID())) {
            resp.tableTournament = tourSerieMgr.playTournament(session, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
        }
        addPlayerSetCategory(Constantes.TOURNAMENT_CATEGORY_NEWSERIE, session.getPlayer().getID());
        return resp;
    }

	@Override
	public FBWSResponse playTournamentDuel(String sessionID, PlayTournamentDuelParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processPlayTournamentDuel(session, param));
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

	public PlayTournamentDuelResponse processPlayTournamentDuel(FBSession session, PlayTournamentDuelParam param) throws FBWSException{
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        // check if a current game exist in session for a different category
        checkGameInSessionOnPlayTournament(session, Constantes.TOURNAMENT_CATEGORY_DUEL, param.tournamentIDstr, null, 0, false);
        PlayTournamentDuelResponse resp = new PlayTournamentDuelResponse();
        synchronized (getLockPlayTournament(session.getPlayer().getID())) {
            resp.tableTournament = duelMgr.playTournament(param.tournamentIDstr, session, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
            addPlayerSetCategory(Constantes.TOURNAMENT_CATEGORY_DUEL, session.getPlayer().getID());
        }
        return resp;
    }

	@Override
	public FBWSResponse playTournamentTrainingPartner(String sessionID,	PlayTournamentTrainingPartnerParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processPlayTournamentTrainingPartner(session, param));
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

	public PlayTournamentTrainingPartnerResponse processPlayTournamentTrainingPartner(FBSession session, PlayTournamentTrainingPartnerParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        // check if a current game exist in session for a different category
        checkGameInSessionOnPlayTournament(session, Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER, null, null, 0, false);

        // check challenge exist
        TournamentChallenge tc = tournamentChallengeMgr.getChallengeForID(param.challengeID);
        if (tc == null) {
            log.error("No challenge with challengeID="+param.challengeID);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        PlayTournamentTrainingPartnerResponse resp = new PlayTournamentTrainingPartnerResponse();
        synchronized (tc) {
            TournamentTable2 table = tournamentTrainingPartnerMgr.playChallenge(tc, session.getPlayer());
            if (table == null) {
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            session.setCurrentTrainingPartnerTableID(table.getID());
            TournamentGame2 game = tournamentTrainingPartnerMgr.getOrCreateGame(table, session);
            if (game == null) {
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            resp.tableTournament = new WSTableTournament();
            resp.tableTournament.table = table.toWSTableGame(session.getPlayer().getID());
            resp.tableTournament.tournament = table.getTournament().toWS();
            resp.tableTournament.tournament.setArrayDeal(new long[]{game.getDeal().getID()});
            resp.tableTournament.tournament.currentDealIndex = 1;
            resp.tableTournament.currentDeal = new WSGameDeal();
            resp.tableTournament.currentDeal.setDealData(game.getDeal());
            resp.tableTournament.currentDeal.setGameData(game);
            resp.tableTournament.setGameID(game.getID());
            resp.tableTournament.conventionProfil = game.getConventionProfile();
            resp.tableTournament.creditAmount = session.getPlayer().getCreditAmount();
            resp.tableTournament.nbPlayedDeals = session.getPlayer().getNbPlayedDeals();
            resp.tableTournament.replayEnabled = playerMgr.isReplayEnabledForPlayer(session);
            resp.tableTournament.freemium = session.isFreemium();
        }
        addPlayerSetCategory(Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER, session.getPlayer().getID());
        return resp;
    }

	@Override
	public FBWSResponse getTimezoneTournaments(String sessionID) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
				response.setData(processGetTimezoneTournaments(session));
			} catch (FBWSException e) {
				response.setException(new FBWSExceptionRest(e.getType()));
			} catch (Exception e) {
				log.error("Exception : "+e.getMessage(), e);
				response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
			}
		} else {
			log.warn("Parameter not valid");
			response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
		}
		return response;
	}

	public GetTimezoneTournamentsResponse processGetTimezoneTournaments(FBSession session) throws FBWSException{
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID());
        }
        GetTimezoneTournamentsResponse resp = new GetTimezoneTournamentsResponse();
        resp.tournaments = timezoneMgr.getTournamentsForPlayer(session.getPlayerCache());
        return resp;
    }
	
	@Override
	public FBWSResponse playTournamentTimezone(String sessionID, PlayTournamentTimezoneParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processPlayTournamentTimezone(session, param));
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

	public PlayTournamentTimezoneResponse processPlayTournamentTimezone(FBSession session, PlayTournamentTimezoneParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        // check if a current game exist in session for a different category
        checkGameInSessionOnPlayTournament(session, Constantes.TOURNAMENT_CATEGORY_TIMEZONE, param.tournamentIDstr, null, 0, false);
        PlayTournamentTimezoneResponse resp = new PlayTournamentTimezoneResponse();
        synchronized (getLockPlayTournament(session.getPlayer().getID())) {
            resp.tableTournament = timezoneMgr.playTournament(session, param.tournamentIDstr, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
        }
        addPlayerSetCategory(Constantes.TOURNAMENT_CATEGORY_TIMEZONE, session.getPlayer().getID());
        return resp;
    }

    @Override
	public FBWSResponse getDuels(String sessionID, GetDuelsParam param) {
		FBWSResponse response = new FBWSResponse();
        if (param == null) {
            param = new GetDuelsParam();
        }

		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
				response.setData(processGetDuels(session, param));
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

    public GetDuelsResponse processGetDuels(FBSession session, GetDuelsParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        GetDuelsResponse resp = new GetDuelsResponse();
        if (param.friends) {
            resp.results = duelMgr.listDuelHistoryFriend(session.getPlayer(), param.offset, param.nbMax);
            resp.offset = param.offset;
            resp.totalSize = playerMgr.countLinkForPlayerAndType(session.getPlayer().getID(), Constantes.PLAYER_LINK_TYPE_FRIEND);
        } else {
            resp.results = duelMgr.listDuelHistory(session.getPlayer(), param.offset > 0 ? param.offset : 0, param.nbMax > 0 ? param.nbMax : FBConfiguration.getInstance().getIntValue("tournament.DUEL.listDuelSize", 50));
            if (resp.results == null) {
                resp.results = new ArrayList<>();
            }
            resp.offset = param.offset;
            resp.totalSize = duelMgr.countListDuelHistory(session.getPlayer().getID());
        }
        return resp;
    }

	@Override
	public FBWSResponse setMatchMakingEnabled(String sessionID, SetMatchMakingEnabledParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetMatchMakingEnabled(session, param));
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

    public SetMatchMakingEnabledResponse processSetMatchMakingEnabled(FBSession session, SetMatchMakingEnabledParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        SetMatchMakingEnabledResponse resp = new SetMatchMakingEnabledResponse();
        if (param.enabled) {
            resp.dateExpiration = duelMgr.enableMatchMakingForPlayer(session.getPlayer().getID());
        } else {
            duelMgr.disableMatchMakingForPlayer(session.getPlayer().getID());
        }
        return resp;
    }

    @Override
    public FBWSResponse removePlayerFromDuelList(String sessionID, RemovePlayerFromDuelListParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processRemovePlayerFromDuelList(session, param));
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

    public RemovePlayerFromDuelListResponse processRemovePlayerFromDuelList(FBSession session, RemovePlayerFromDuelListParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        Player partner = playerMgr.getPlayer(param.playerID);
        if (partner == null) {
            log.error("No player found with ID=" + param.playerID);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        duelMgr.setResetStatusDuelBetweenPlayer(session.getPlayer(), partner);
        RemovePlayerFromDuelListResponse resp = new RemovePlayerFromDuelListResponse();
        resp.result = true;
        return resp;
    }

    @Override
    public FBWSResponse getTournamentBadges(String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetTournamentBadges(session));
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

    public GetTournamentBadgesResponse processGetTournamentBadges(FBSession session) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID());
        }
        GetTournamentBadgesResponse resp = new GetTournamentBadgesResponse();

        // Timezone
        resp.tournamentBadges.timezone = timezoneMgr.getMemoryMgr().listMemTournamentInProgressForPlayer(session.getPlayer().getID()).size();
        // Training
        resp.tournamentBadges.training = trainingMgr.getMemoryMgr().listMemTournamentInProgressForPlayer(session.getPlayer().getID()).size();
        // series
        if (tourSerieMgr.isEnable() && !tourSerieMgr.isPeriodChangeProcessing()) {
            resp.tournamentBadges.series = tourSerieMgr.getMemoryMgr().getTournamentInProgressForPlayer(session.getSerie(), session.getPlayer().getID()) != null ? 1 : 0;
        }
        // Serie Top Challenge
        resp.tournamentBadges.serieTopChallenge = serieTopChallengeMgr.getTournamentInProgressForPlayer(session.getPlayer().getID()) != null?1:0;
        // Serie Easy Challenge
        resp.tournamentBadges.serieEasyChallenge = serieEasyChallengeMgr.getTournamentInProgressForPlayer(session.getPlayer().getID()) != null?1:0;
        // Tour POINTS FUNBRIDGE
        resp.tournamentBadges.CBO = tourCBOMgr.getMemoryMgr().listMemTournamentInProgressForPlayer(session.getPlayer().getID()).size();
        // tour TEAM
        if (tourTeamMgr.isEnable() && !tourTeamMgr.isTourChangeProcessing() && !tourTeamMgr.isPeriodChangeProcessing()) {
            resp.tournamentBadges.teams = ContextManager.getTeamMgr().getNbRequestsForPlayer(session.getPlayer()) +
                    ContextManager.getTeamMgr().getNbMessagesForPlayer(session.getPlayer()) +
                    (tourTeamMgr.getMemoryMgr().getTournamentInProgressForPlayer(session.getPlayer().getID()) != null ? 1 : 0);
        }
        // Private
        resp.tournamentBadges.privateTournament = privateTournamentMgr.getMemoryMgr().listMemTournamentInProgressForPlayer(session.getPlayer().getID()).size();


        if (log.isDebugEnabled()) {
            log.debug("Badges="+resp.tournamentBadges+" - playerID="+session.getPlayer().getID());
        }
        return resp;
    }

    public FBWSResponse getTournamentInProgress(String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetTournamentInProgress(session));
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

    public GetTournamentInProgressResponse processGetTournamentInProgress(FBSession session) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID());
        }
        GetTournamentInProgressResponse resp = new GetTournamentInProgressResponse();
        resp.tournamentsInProgress = new ArrayList<>();
        // Timezone
        this.addTournamentInProgress(resp, Constantes.TOURNAMENT_CATEGORY_TIMEZONE, session.getPlayer().getID());
        // Training
        List<GenericMemTournament> listTrainingTour = trainingMgr.getMemoryMgr().listMemTournamentInProgressForPlayer(session.getPlayer().getID());
        if (listTrainingTour != null) {
            for (GenericMemTournament e : listTrainingTour) {
                WSTournamentInProgress tip = new WSTournamentInProgress();
                tip.category = Constantes.TOURNAMENT_CATEGORY_TRAINING;
                tip.tourID = e.tourID;
                tip.endDate = e.endDate;
                resp.tournamentsInProgress.add(tip);
            }
        }
        // series
        if (tourSerieMgr.isEnable() && !tourSerieMgr.isPeriodChangeProcessing()) {
            TourSerieMemTour serieMemTour = tourSerieMgr.getMemoryMgr().getTournamentInProgressForPlayer(session.getSerie(), session.getPlayer().getID());
            if (serieMemTour != null) {
                WSTournamentInProgress tip = new WSTournamentInProgress();
                tip.category = Constantes.TOURNAMENT_CATEGORY_NEWSERIE;
                tip.tourID = serieMemTour.tourID;
                tip.endDate = TourSerieMgr.transformPeriodID2TS(serieMemTour.periodID, false);
                resp.tournamentsInProgress.add(tip);
            }
        }
        // serieTopChallenge
        if (serieTopChallengeMgr.isEnable() && !serieTopChallengeMgr.isPeriodChangeProcessing()) {
            TourSerieTournament serieTopChallengeTour = serieTopChallengeMgr.getTournamentInProgressForPlayer(session.getPlayer().getID());
            if (serieTopChallengeTour != null) {
                WSTournamentInProgress tip = new WSTournamentInProgress();
                tip.category = Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE;
                tip.tourID = serieTopChallengeTour.getIDStr();
                tip.endDate = serieTopChallengeMgr.getPeriodSerie().getTsDateEnd();
                resp.tournamentsInProgress.add(tip);
            }
        }
        // serieEasyChallenge
        if (serieEasyChallengeMgr.isEnable() && !serieEasyChallengeMgr.isPeriodChangeProcessing()) {
            TourSerieTournament serieEasyChallengeTour = serieEasyChallengeMgr.getTournamentInProgressForPlayer(session.getPlayer().getID());
            if (serieEasyChallengeTour != null) {
                WSTournamentInProgress tip = new WSTournamentInProgress();
                tip.category = Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE;
                tip.tourID = serieEasyChallengeTour.getIDStr();
                tip.endDate = serieEasyChallengeMgr.getPeriodSerie().getTsDateEnd();
                resp.tournamentsInProgress.add(tip);
            }
        }
        this.addTournamentInProgress(resp, Constantes.TOURNAMENT_CATEGORY_TOUR_CBO, session.getPlayer().getID());

        // TEAM
        if (tourTeamMgr.isEnable() && !tourTeamMgr.isTourChangeProcessing() && !tourTeamMgr.isPeriodChangeProcessing()) {
            TeamMemTournament memTour = tourTeamMgr.getMemoryMgr().getTournamentInProgressForPlayer(session.getPlayer().getID());
            if (memTour != null) {
                WSTournamentInProgress tip = new WSTournamentInProgress();
                tip.category = Constantes.TOURNAMENT_CATEGORY_TEAM;
                tip.tourID = memTour.tourID;
                tip.endDate = memTour.endDate;
                resp.tournamentsInProgress.add(tip);
            }
        }
        // Private
        this.addTournamentInProgress(resp, Constantes.TOURNAMENT_CATEGORY_PRIVATE, session.getPlayer().getID());
        if (log.isDebugEnabled()) {
            log.debug("tournamentsInProgress="+ StringTools.listToString(resp.tournamentsInProgress)+" - playerID="+session.getPlayer().getID());
        }
        return resp;
    }

	/**
	 * Add tournament in progress to response
	 * @param resp
	 * @param category
	 * @param playerID
	 */
    private void addTournamentInProgress(GetTournamentInProgressResponse resp, int category, long playerID) {
		TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(category);
		if (tournamentMgr != null) {
			List<GenericMemTournament> listMemTour = tournamentMgr.getMemoryMgr().listMemTournamentInProgressForPlayer(playerID);
			if (listMemTour != null) {
				for (GenericMemTournament e : listMemTour) {
					WSTournamentInProgress tip = new WSTournamentInProgress();
					tip.category = category;
					tip.tourID = e.tourID;
					tip.endDate = e.endDate;
					resp.tournamentsInProgress.add(tip);
				}
			}
		}

	}

	@Override
	public FBWSResponse playSerieTopChallengeTournament(String sessionID, PlaySerieTopChallengeTournamentParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processPlaySerieTopChallengeTournament(session, param));
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

    @Override
    public FBWSResponse playSerieEasyChallengeTournament(String sessionID, PlaySerieEasyChallengeTournamentParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processPlaySerieEasyChallengeTournament(session, param));
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

    public PlaySerieTopChallengeTournamentResponse processPlaySerieTopChallengeTournament(FBSession session, PlaySerieTopChallengeTournamentParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        // check if a current game exist in session for a different category
        checkGameInSessionOnPlayTournament(session, Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE, null, null, 0, false);
        serieTopChallengeMgr.checkSerieTopChallengeEnable();
        PlaySerieTopChallengeTournamentResponse resp = new PlaySerieTopChallengeTournamentResponse();
        synchronized (getLockPlayTournament(session.getPlayer().getID())) {
            resp.tableTournament = serieTopChallengeMgr.playTournament(session, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
        }
        addPlayerSetCategory(Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE, session.getPlayer().getID());
        return resp;
    }

    public PlaySerieEasyChallengeTournamentResponse processPlaySerieEasyChallengeTournament(FBSession session, PlaySerieEasyChallengeTournamentParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        // check if a current game exist in session for a different category
        this.checkGameInSessionOnPlayTournament(session, Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE, null, null, 0, false);
        this.serieEasyChallengeMgr.checkSerieEasyChallengeEnable();
        PlaySerieEasyChallengeTournamentResponse resp = new PlaySerieEasyChallengeTournamentResponse();
        synchronized (getLockPlayTournament(session.getPlayer().getID())) {
            resp.tableTournament = this.serieEasyChallengeMgr.playTournament(session, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
        }
        addPlayerSetCategory(Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE, session.getPlayer().getID());
        return resp;
    }

	@Override
	public FBWSResponse getFederationSummary(String sessionID, GetFederationSummaryParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetFederationSummary(session, param));

			} catch (FBWSException e) {
				response.setException(new FBWSExceptionRest(e.getType()));
			} catch (Exception e) {
				log.error("Exception : "+e.getMessage(), e);
				response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
			}
		} else {
			log.warn("Session not valid");
			response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
		}
		return response;
	}

    public WSFederationSummary processGetFederationSummary(FBSession session, GetFederationSummaryParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        // prepare response object
        WSFederationSummary summary = new WSFederationSummary();
        // get the correct Manager for federation
        TourFederationMgr federationMgr = FederationMgr.getTourFederationMgr(param.federation);
        // get player info
        PlayerFederation playerFederation = federationMgr.getOrCreatePlayerFederation(session.getPlayer().getID(), param.federation);
        if (playerFederation == null) {
            log.error("Failed to get playerFederation for player="+session.getPlayer()+" and federation="+param.federation);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        summary.licence = playerFederation.licence;
        summary.credit = 0;
        if (playerFederation.firstname != null && playerFederation.firstname.length() > 0) {
            summary.firstname = playerFederation.firstname;
        }
        if (playerFederation.lastname != null && playerFederation.lastname.length() > 0) {
            summary.lastname = playerFederation.lastname;
        }
        if (playerFederation.club != null && playerFederation.club.length() > 0) {
            summary.club = playerFederation.club;
        }
        summary.tournaments = federationMgr.listTournamentForPlayer(session.getPlayer().getID());
        summary.federationURL = federationMgr.getConfigStringValue("federationURL", "");
        summary.lastTournament = federationMgr.getLastFinishedTournament(session);
        return summary;
    }

	@Override
	public FBWSResponse registerTournamentFederation(String sessionID, RegisterTournamentFederationParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processRegisterTournamentFederation(session, param));
			} catch (FBWSException e) {
				response.setException(new FBWSExceptionRest(e.getType()));
			} catch (Exception e) {
				log.error("Exception : "+e.getMessage(), e);
				response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
			}
		} else {
			log.warn("Parameter not valid - param=" + param);
			response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
		}
		return response;
	}

    public RegisterTournamentFederationResponse processRegisterTournamentFederation(FBSession session, RegisterTournamentFederationParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        RegisterTournamentFederationResponse resp = new RegisterTournamentFederationResponse();
        // get the correct Manager for federation ??????????
        TourFederationMgr federationMgr = FederationMgr.getTourFederationMgr(param.federation);
        RegisterTourFederationResult registerResult = null;
        if(param.register){
            registerResult = federationMgr.registerPlayerInTournament(session.getPlayer().getID(),  param.tourID, true);
        } else {
            registerResult = federationMgr.unregisterPlayerFromTournament(session.getPlayer().getID(),  param.tourID, true);
        }
        resp.credit = registerResult.credit;
        resp.nbPlayersRegistered = registerResult.nbPlayersRegistered;
        return resp;
    }

	@Override
	public FBWSResponse playTournamentFederation(String sessionID, PlayTournamentFederationParam param) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				// check sessionID
				FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processPlayTournamentFederation(session, param));
			} catch (FBWSException e) {
				response.setException(new FBWSExceptionRest(e.getType()));
			} catch (Exception e) {
				log.error("Exception : "+e.getMessage(), e);
				response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
			}
		} else {
			log.warn("Parameter not valid - param=" + param);
			response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
		}
		return response;
	}

    public PlayTournamentFederationResponse processPlayTournamentFederation(FBSession session, PlayTournamentFederationParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        // get the correct Manager for federation
        TourFederationMgr federationMgr = FederationMgr.getTourFederationMgr(param.federation);
        // check if a current game exist in session for a different category
        checkGameInSessionOnPlayTournament(session, federationMgr.getCategory(), param.tournamentID, null, 0, false);
        PlayTournamentFederationResponse resp = new PlayTournamentFederationResponse();
        synchronized (getLockPlayTournament(session.getPlayer().getID())) {
            resp.tableTournament = federationMgr.playTournament(session, param.tournamentID, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
        }
        addPlayerSetCategory(federationMgr.getCategory(), session.getPlayer().getID());
        return resp;
    }

    @Override
    public FBWSResponse getPrivateTournamentSummary(String sessionID, GetPrivateTournamentSummaryParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetPrivateTournamentSummary(session, param));
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

    public GetPrivateTournamentSummaryResponse processGetPrivateTournamentSummary(FBSession session, GetPrivateTournamentSummaryParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        GetPrivateTournamentSummaryResponse resp = new GetPrivateTournamentSummaryResponse();
        // list tournament started by player and in progress + next favorites tournament
        PrivateTournamentMgr.ResultListTournamentForPlayer resultListTournamentForPlayer = privateTournamentMgr.listTournamentForPlayer(session.getPlayer().getID(), 0, param.nbMaxTournaments);
        if (resultListTournamentForPlayer == null) {
            log.error("Result listTournamentForPlayer is null ! - param="+param);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        resp.tournaments = resultListTournamentForPlayer.tournaments;
        resp.tournamentsTotalSize = resultListTournamentForPlayer.tournamentsTotalSize;
        // nb tournaments created by player and availabe
        resp.nbPlayerTournaments = privateTournamentMgr.countPropertiesEnableForOwner(session.getPlayer().getID());
        // nb total tournaments
        resp.nbTotalTournaments = privateTournamentMgr.countTournamentAvailable();
        resp.tournamentCreationAllowed = privateTournamentMgr.isCreditValidForPlayer(session.getTsSubscriptionExpiration(), session.getPlayer().getCreditAmount());
        resp.nbTournamentPerPlayer = privateTournamentMgr.getConfigIntValue("nbMaxCreationByPlayer", 3);
        return resp;
    }

    @Override
    public FBWSResponse getPrivateTournament(String sessionID, GetPrivateTournamentParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetPrivateTournament(session, param));
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

    public GetPrivateTournamentResponse processGetPrivateTournament(FBSession session, GetPrivateTournamentParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        PrivateTournament tour = (PrivateTournament)privateTournamentMgr.getTournament(param.tournamentID);
        if (tour == null) {
            log.error("No tournament found with param="+param+" - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        GetPrivateTournamentResponse resp = new GetPrivateTournamentResponse();
        privateTournamentMgr.openTournament(param.tournamentID, session.getPlayer().getID(), param.password);
        resp.resultPlayer = privateTournamentMgr.getWSResultTournamentPlayer(param.tournamentID, session.getPlayerCache(), true);
        resp.tournament = privateTournamentMgr.toWSPrivateTournament(tour, session.getPlayer().getID(), playerMgr.listFriendIDForPlayer(session.getPlayer().getID()));
        return resp;
    }

    @Override
    public FBWSResponse createPrivateTournamentProperties(String sessionID, CreatePrivateTournamentPropertiesParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processCreatePrivateTournamentProperties(session, param));
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

    public CreatePrivateTournamentPropertiesResponse processCreatePrivateTournamentProperties(FBSession session, CreatePrivateTournamentPropertiesParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        if (!privateTournamentMgr.isCreditValidForPlayer(session.getTsSubscriptionExpiration(),  session.getPlayer().getCreditAmount())) {
            throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_CREDIT_NOT_VALID);
        }
        CreatePrivateTournamentPropertiesResponse resp = new CreatePrivateTournamentPropertiesResponse();
        PrivateTournamentProperties properties = privateTournamentMgr.createProperties(param.properties, session.getPlayer());
        if (properties == null) {
            log.error("Failed to create properties ! param="+param+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        resp.tournament = privateTournamentMgr.toWSPrivateTournament(privateTournamentMgr.getNextTournamentForProperties(properties.getIDStr()), session.getPlayer().getID(), playerMgr.listFriendIDForPlayer(session.getPlayer().getID()));
        return resp;
    }

    @Override
    public FBWSResponse removePrivateTournamentProperties(String sessionID, RemovePrivateTournamentPropertiesParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processRemovePrivateTournamentProperties(session, param));
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

    public RemovePrivateTournamentPropertiesResponse processRemovePrivateTournamentProperties(FBSession session, RemovePrivateTournamentPropertiesParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        RemovePrivateTournamentPropertiesResponse resp = new RemovePrivateTournamentPropertiesResponse();
        privateTournamentMgr.removeProperties(param.propertiesID, session.getPlayer().getID());
        resp.result = true;
        return resp;
    }

    @Override
    public FBWSResponse changePasswordPrivateTournament(String sessionID, ChangePasswordPrivateTournamentParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processChangePasswordPrivateTournament(session, param));
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

    public ChangePasswordPrivateTournamentResponse processChangePasswordPrivateTournament(FBSession session, ChangePasswordPrivateTournamentParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        privateTournamentMgr.changePasswordForProperties(session.getPlayer().getID(), param.propertiesID, param.password);
        ChangePasswordPrivateTournamentResponse resp = new ChangePasswordPrivateTournamentResponse();
        resp.result = true;
        return resp;
    }

    @Override
    public FBWSResponse setPrivateTournamentFavorite(String sessionID, SetPrivateTournamentFavoriteParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetPrivateTournamentFavorite(session, param));
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

    public SetPrivateTournamentFavoriteResponse processSetPrivateTournamentFavorite(FBSession session, SetPrivateTournamentFavoriteParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        SetPrivateTournamentFavoriteResponse resp = new SetPrivateTournamentFavoriteResponse();
        resp.result = privateTournamentMgr.setTournamentFavoriteForPlayer(param.propertiesID, session.getPlayer().getID(), param.favorite);
        return resp;
    }

    @Override
    public FBWSResponse listPrivateTournaments(String sessionID, ListPrivateTournamentsParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processListPrivateTournaments(session, param));
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

    public ListPrivateTournamentsResponse processListPrivateTournaments(FBSession session, ListPrivateTournamentsParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        ListPrivateTournamentsResponse resp = new ListPrivateTournamentsResponse();
        resp.offset = param.offset;
        if (param.search == null) {
            param.search = "";
        }
        if (param.nbMax <= 0) {
            param.nbMax = 50;
        }
        PrivateTournamentMgr.ListTournamentResult ltr = privateTournamentMgr.listTournament(param.search.trim(), 0, param.getNbDealsMinimum(), param.getNbDealsMaximum(), param.rankingType, param.accessRule, param.countryCode, param.favorite, session.getPlayer().getID(), param.offset, param.nbMax);
        resp.totalSize = ltr.count;
        List<PrivateTournament> tournaments = ltr.tournamentList;
        resp.results = new ArrayList<>();
        List<Long> listFriendID = playerMgr.listFriendIDForPlayer(session.getPlayer().getID());
        for (PrivateTournament e : tournaments) {
            resp.results.add(privateTournamentMgr.toWSPrivateTournament(e, session.getPlayer().getID(), listFriendID));
        }
        return resp;
    }

    @Override
    public FBWSResponse getPlayerPrivateTournamentProperties(String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetPlayerPrivateTournamentProperties(session));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid - sessionID="+sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetPlayerPrivateTournamentPropertiesResponse processGetPlayerPrivateTournamentProperties(FBSession session) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID());
        }
        GetPlayerPrivateTournamentPropertiesResponse resp = new GetPlayerPrivateTournamentPropertiesResponse();
        List<PrivateTournamentProperties> playerTourProperties = privateTournamentMgr.getPrivateTournamentPropertiesForOwner(session.getPlayer().getID(), true);
        List<Long> listFriendID = playerMgr.listFriendIDForPlayer(session.getPlayer().getID());
        for (PrivateTournamentProperties e : playerTourProperties) {
            resp.properties.add(privateTournamentMgr.toWSPrivateTournamentProperties(e, listFriendID));
        }
        return resp;
    }

    @Override
    public FBWSResponse playPrivateTournament(String sessionID, PlayPrivateTournamentParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processPlayPrivateTournament(session, param));
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

    public PlayPrivateTournamentResponse processPlayPrivateTournament(FBSession session, PlayPrivateTournamentParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        // check if a current game exist in session for a different category
        checkGameInSessionOnPlayTournament(session, Constantes.TOURNAMENT_CATEGORY_PRIVATE, param.tournamentID, param.tournamentID, 0, false);
        PlayPrivateTournamentResponse resp = new PlayPrivateTournamentResponse();
        synchronized (getLockPlayTournament(session.getPlayer().getID())) {
            resp.tableTournament = privateTournamentMgr.playTournament(session, param.tournamentID, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
        }
        addPlayerSetCategory(Constantes.TOURNAMENT_CATEGORY_PRIVATE, session.getPlayer().getID());
        return resp;
    }

	@Override
	public FBWSResponse playLearningTournament(String sessionID, PlayLearningTournamentParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processPlayLearningTournament(session, param));
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

    public PlayLearningTournamentResponse processPlayLearningTournament(FBSession session, PlayLearningTournamentParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        // check if a current game exist in session for a different category
        checkGameInSessionOnPlayTournament(session, Constantes.TOURNAMENT_CATEGORY_LEARNING, null, param.chapterID, 0, false);
        PlayLearningTournamentResponse resp = new PlayLearningTournamentResponse();
        synchronized (getLockPlayTournament(session.getPlayer().getID())) {
            resp.tableTournament = tourLearningMgr.playTournament(session, param.chapterID, param.conventionProfil, param.conventionValue, param.cardsConventionProfil, param.cardsConventionValue);
        }
        addPlayerSetCategory(Constantes.TOURNAMENT_CATEGORY_LEARNING, session.getPlayer().getID());
        return resp;
    }

	@Override
	public FBWSResponse getLearningDealCommented(String sessionID, GetLearningDealCommentedParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetLearningDealCommented(session, param));
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

    public GetLearningDealCommentedResponse processGetLearningDealCommented(FBSession session, GetLearningDealCommentedParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        GetLearningDealCommentedResponse resp = new GetLearningDealCommentedResponse();
        // get dealCommented
        resp.dealCommented = tourLearningMgr.getWSDealCommentedForDealID(param.dealID, session.getPlayer().getDisplayLang());
        if (resp.dealCommented == null) {
            log.error("No CommentsDeal found for param="+param);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // get contract and nbTricks played by the player asking
        LearningTournament tour= tourLearningMgr.getTournamentWithDeal(param.dealID);
        if (tour != null) {
            LearningGame gamePlayer = tourLearningMgr.getGameOnTournamentAndDealForPlayer(tour.getIDStr(), resp.dealCommented.deal.index, session.getPlayer().getID());
            if (gamePlayer != null) {
                resp.contractPlayer = gamePlayer.getContractWS();
                resp.declarerPlayer = Character.toString(gamePlayer.getDeclarer());
                resp.nbTricksPlayer = gamePlayer.getTricks();
            }
        }
        return resp;
    }

    @Override
    public FBWSResponse setLearningProgression(String sessionID, SetLearningProgressionParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetLearningProgression(session, param));
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

    public SetLearningProgressionResponse processSetLearningProgression(FBSession session, SetLearningProgressionParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=["+param+"] - session="+session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        tourLearningMgr.updateLearningProgression(session.getPlayer().getID(), param.sb, param.chapter, param.deal, param.status, param.step);
        session.incrementNbLearningDeals();
        return new SetLearningProgressionResponse();
    }

    @Override
    public FBWSResponse getLearningProgression(String sessionID){
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetLearningProgression(session));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - sessionID="+sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetLearningProgressionResponse processGetLearningProgression(FBSession session) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID());
        }
        LearningProgression learningProgression = tourLearningMgr.getLearningProgression(session.getPlayer().getID());
        GetLearningProgressionResponse resp = new GetLearningProgressionResponse();
        resp.learningProgression = learningProgression;
        return resp;
    }
}
