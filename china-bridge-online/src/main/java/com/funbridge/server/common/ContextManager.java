package com.funbridge.server.common;

import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.engine.ArgineEngineMgr;
import com.funbridge.server.message.ChatMgr;
import com.funbridge.server.message.MessageMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.nursing.NursingMgr;
import com.funbridge.server.operation.OperationMgr;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.dao.PlayerDuelDAO;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.store.StoreMgr;
import com.funbridge.server.store.dao.TransactionDAO;
import com.funbridge.server.team.TeamMgr;
import com.funbridge.server.team.cache.TeamCacheMgr;
import com.funbridge.server.texts.TextUIMgr;
import com.funbridge.server.tournament.TournamentChallengeMgr;
import com.funbridge.server.tournament.TournamentGame2Mgr;
import com.funbridge.server.tournament.TournamentGenerator;
import com.funbridge.server.tournament.TournamentMgr;
import com.funbridge.server.tournament.category.TournamentTrainingPartnerMgr;
import com.funbridge.server.tournament.duel.DuelMgr;
import com.funbridge.server.tournament.federation.TourFederationMgr;
import com.funbridge.server.tournament.federation.TourFederationStatPeriodMgr;
import com.funbridge.server.tournament.federation.cbo.TourCBOMgr;
import com.funbridge.server.tournament.game.GameMgr;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.learning.TourLearningMgr;
import com.funbridge.server.tournament.privatetournament.PrivateTournamentMgr;
import com.funbridge.server.tournament.serie.SerieEasyChallengeMgr;
import com.funbridge.server.tournament.serie.SerieTopChallengeMgr;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.team.TourTeamMgr;
import com.funbridge.server.tournament.timezone.TimezoneMgr;
import com.funbridge.server.tournament.training.TrainingMgr;
import com.funbridge.server.ws.engine.EngineService;
import com.funbridge.server.ws.event.EventServiceRestImpl;
import com.funbridge.server.ws.game.GameServiceRestImpl;
import com.funbridge.server.ws.message.MessageService;
import com.funbridge.server.ws.notification.NotificationService;
import com.funbridge.server.ws.player.PlayerServiceRestImpl;
import com.funbridge.server.ws.presence.PresenceServiceRestImpl;
import com.funbridge.server.ws.result.ResultServiceRestImpl;
import com.funbridge.server.ws.servlet.ClientWebSocketMgr;
import com.funbridge.server.ws.support.SupportService;
import com.funbridge.server.ws.tournament.TournamentServiceRestImpl;
import com.funbridge.server.ws.pay.OrderServiceRestImpl;
import com.gotogames.common.tools.JSONTools;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ??bean??
 */

@Component
public class ContextManager implements ApplicationContextAware {
	private static ApplicationContext context = null;
	private static JSONTools jsonTools = new JSONTools();
	private static List<GameMgr> listGameMgr = new ArrayList<>();

    public void destroy() {
    	// No used in this component
	}
	
	public static ApplicationContext getContext() {
		return context;
	}
	
	public static JSONTools getJSONTools() {
		return jsonTools;
	}
	
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		context = ctx;
	}
	
	public static PresenceMgr getPresenceMgr() {
		return (PresenceMgr) context.getBean("presenceMgr");
	}
	
	public static TournamentMgr getTournamentMgr() {
		return (TournamentMgr) context.getBean("tournamentMgr");
	}
	
	public static PlayerMgr getPlayerMgr() {
		return (PlayerMgr) context.getBean("playerMgr");
	}
	
	public static TournamentGenerator getTournamentGenerator() {
		return (TournamentGenerator) context.getBean("tournamentGenerator");
	}
	
	public static TourSerieMgr getTourSerieMgr() {
        return (TourSerieMgr) context.getBean("tourSerieMgr");
    }
	
	public static StoreMgr getStoreMgr() {
		return (StoreMgr) context.getBean("storeMgr");
	}
	
	public static TransactionDAO getTransactionDAO() {
		return (TransactionDAO) context.getBean("transactionDAO");
	}

	public static MessageMgr getMessageMgr() {
		return (MessageMgr) context.getBean("messageMgr");
	}
	
	public static SupportService getSupportService() {
		return (SupportService) context.getBean("supportService");
	}

	public static MailMgr getMailMgr() { return (MailMgr) context.getBean("mailMgr"); }
	
	public static TournamentChallengeMgr getTournamentChallengeMgr() {
		return (TournamentChallengeMgr) context.getBean("tournamentChallengeMgr");
	}
	
	public static TournamentTrainingPartnerMgr getTournamentTrainingPartnerMgr() {
		return (TournamentTrainingPartnerMgr) context.getBean("tournamentTrainingPartnerMgr");
	}
	
	public static TournamentGame2Mgr getTournamentGame2Mgr() {
		return (TournamentGame2Mgr) context.getBean("tournamentGame2Mgr");
	}
	
	public static DuelMgr getDuelMgr() {
        return (DuelMgr) context.getBean("duelMgr");
    }
    public static PlayerDuelDAO getPlayerDuelDAO() {
        return (PlayerDuelDAO) context.getBean("playerDuelDAO");
    }

	public static MessageNotifMgr getMessageNotifMgr() {
		return (MessageNotifMgr) context.getBean("messageNotifMgr");
	}

	public static GameServiceRestImpl getGameService() {
		return (GameServiceRestImpl) context.getBean("gameService");
	}

	public static TrainingMgr getTrainingMgr(){ return (TrainingMgr) context.getBean("trainingMgr"); }

	public static ClientWebSocketMgr getClientWebSocketMgr() {
		return (ClientWebSocketMgr) context.getBean("clientWebSocketMgr");
	}
	
	public static ArgineEngineMgr getArgineEngineMgr() {
		return (ArgineEngineMgr) context.getBean("argineEngineMgr");
	}

    public static FunbridgeAlertMgr getAlertMgr() {
        return (FunbridgeAlertMgr) context.getBean("alertMgr");
    }

    public static OperationMgr getOperationMgr() {
        return (OperationMgr) context.getBean("operationMgr");
    }

    public static MongoTemplate getMongoTemplate() {
        return (MongoTemplate) context.getBean("mongoTemplate");
    }

    public static TextUIMgr getTextUIMgr() {
        return (TextUIMgr) context.getBean("textUIMgr");
    }

    public static PresenceServiceRestImpl getPresenceService() {
        return (PresenceServiceRestImpl) context.getBean("presenceService");
    }

    public static PlayerCacheMgr getPlayerCacheMgr() {
        return (PlayerCacheMgr) context.getBean("playerCacheMgr");
    }

	public static TimezoneMgr getTimezoneMgr() {
		return (TimezoneMgr) context.getBean("timezoneMgr");
	}

    public static TournamentServiceRestImpl getTournamentService() {
        return (TournamentServiceRestImpl) context.getBean("tournamentService");
    }

    public static ResultServiceRestImpl getResultService() {
        return (ResultServiceRestImpl) context.getBean("resultService");
    }

    public static NotificationService getNotificationService() {
        return (NotificationService) context.getBean("notificationService");
    }

    public static EngineService getEngineService() {
        return (EngineService) context.getBean("engineService");
    }

    public static SerieTopChallengeMgr getSerieTopChallengeMgr() {return (SerieTopChallengeMgr) context.getBean("serieTopChallengeMgr");}

    public static SerieEasyChallengeMgr getSerieEasyChallengeMgr() {return (SerieEasyChallengeMgr) context.getBean("serieEasyChallengeMgr");}

	public static TourCBOMgr getTourCBOMgr() {
		return (TourCBOMgr) context.getBean("tourCBOMgr");
	}



	public static List<TourFederationMgr> getListTourFederationMgr() {
		List<TourFederationMgr> listTourFederationMgr = new ArrayList<>();
		listTourFederationMgr.add(getTourCBOMgr());
		return listTourFederationMgr;
	}

	public static TourFederationStatPeriodMgr getTourFederationStatPeriodMgr() {
		return (TourFederationStatPeriodMgr) context.getBean("tourFederationStatPeriodMgr");
	}

    public static TeamMgr getTeamMgr() {
        return (TeamMgr) context.getBean("teamMgr");
    }
    public static TeamCacheMgr getTeamCacheMgr() {
        return (TeamCacheMgr) context.getBean("teamCacheMgr");
    }

    public static TourTeamMgr getTourTeamMgr() {
        return (TourTeamMgr) context.getBean("tourTeamMgr");
    }

	public static ChatMgr getChatMgr() { return (ChatMgr) context.getBean("chatMgr"); }

	public static PrivateTournamentMgr getPrivateTournamentMgr() {
	    return (PrivateTournamentMgr) context.getBean("privateTournamentMgr");
    }

	public static TourLearningMgr getTourLearningMgr() {
		return (TourLearningMgr) context.getBean("tourLearningMgr");
	}

	public static NursingMgr getNursingMgr(){
		return (NursingMgr) context.getBean("nursingMgr");
	}

	/**
	 * Get a tournament manager by its category
	 * ?????????
	 * @param category
	 * @return
	 */
    public static TournamentGenericMgr getTournamentMgrForCategory(int category) {
		switch (category) {
            case Constantes.TOURNAMENT_CATEGORY_TRAINING:
                return getTrainingMgr();
			case Constantes.TOURNAMENT_CATEGORY_TIMEZONE:
				return getTimezoneMgr();
			case Constantes.TOURNAMENT_CATEGORY_PRIVATE:
				return getPrivateTournamentMgr();
			case Constantes.TOURNAMENT_CATEGORY_TOUR_CBO:
				return getTourCBOMgr();
			case Constantes.TOURNAMENT_CATEGORY_TEAM:
				return getTourTeamMgr();
			case Constantes.TOURNAMENT_CATEGORY_NEWSERIE:
				return getTourSerieMgr();
			case Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE:
				return getSerieTopChallengeMgr();
			case Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE:
				return getSerieEasyChallengeMgr();
			case Constantes.TOURNAMENT_CATEGORY_DUEL:
				return getDuelMgr();
			default:
				return null;
		}
    }

	/**
	 * Get a tournament manager by its type
	 * ?????????
	 * @param type
	 * @return
	 */
	public static GameMgr getTournamentGameMgr(GameServiceRestImpl.GameType type) {
		switch (type) {
            case Training:
                return getTrainingMgr().getGameMgr();
			case Timezone:
				return getTimezoneMgr().getGameMgr();
            case Serie:
                return getTourSerieMgr().getGameMgr();
            case SerieTopChallenge:
                return getSerieTopChallengeMgr().getGameMgr();
			case SerieEasyChallenge:
				return getSerieEasyChallengeMgr().getGameMgr();
			case Private:
				return getPrivateTournamentMgr().getGameMgr();
            case Duel:
                return getDuelMgr().getGameMgr();
            case TourTeam:
                return getTourTeamMgr().getGameMgr();
            case TourLearning:
                return getTourLearningMgr().getGameMgr();
			case TourCBO:
				return getTourCBOMgr().getGameMgr();
			default:
				return null;
		}

	}

	public static GameMgr getTournamentGameMgr(int category) {
        switch (category) {
            case Constantes.TOURNAMENT_CATEGORY_TRAINING:
                return getTrainingMgr().getGameMgr();
            case Constantes.TOURNAMENT_CATEGORY_TIMEZONE:
                return getTimezoneMgr().getGameMgr();
            case Constantes.TOURNAMENT_CATEGORY_NEWSERIE:
                return getTourSerieMgr().getGameMgr();
            case Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE:
                return getSerieTopChallengeMgr().getGameMgr();
			case Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE:
				return getSerieEasyChallengeMgr().getGameMgr();
            case Constantes.TOURNAMENT_CATEGORY_PRIVATE:
                return getPrivateTournamentMgr().getGameMgr();
            case Constantes.TOURNAMENT_CATEGORY_DUEL:
                return getDuelMgr().getGameMgr();
            case Constantes.TOURNAMENT_CATEGORY_TEAM:
                return getTourTeamMgr().getGameMgr();
            case Constantes.TOURNAMENT_CATEGORY_LEARNING:
                return getTourLearningMgr().getGameMgr();
            case Constantes.TOURNAMENT_CATEGORY_TOUR_CBO:
                return getTourCBOMgr().getGameMgr();
			default:
				return null;
        }
    }

    public static List<GameMgr> getListGameMgr() {
	    if (listGameMgr == null || listGameMgr.size() == 0) {
	        listGameMgr = new ArrayList<>();
            listGameMgr.add(getTrainingMgr().getGameMgr());
            listGameMgr.add(getTimezoneMgr().getGameMgr());
            listGameMgr.add(getTourSerieMgr().getGameMgr());
            listGameMgr.add(getDuelMgr().getGameMgr());
            listGameMgr.add(getSerieTopChallengeMgr().getGameMgr());
			listGameMgr.add(getSerieEasyChallengeMgr().getGameMgr());
            listGameMgr.add(getPrivateTournamentMgr().getGameMgr());
            listGameMgr.add(getTourTeamMgr().getGameMgr());
			listGameMgr.add(getTourCBOMgr().getGameMgr());
        }
        return listGameMgr;
    }

    public static EventServiceRestImpl getEventService() {
        return (EventServiceRestImpl) context.getBean("eventService");
    }

    public static MessageService getMessageService() {
        return (MessageService) context.getBean("messageService");
    }

    public static PlayerServiceRestImpl getPlayerService() {
        return (PlayerServiceRestImpl) context.getBean("playerService");
    }


	public static OrderServiceRestImpl getWXOrderService() {
		return (OrderServiceRestImpl) context.getBean("orderService");
	}
}
