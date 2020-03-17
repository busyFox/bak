package com.funbridge.server.message;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.message.data.*;
import com.funbridge.server.nursing.data.PlayerNursing;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.texts.TextUIData;
import com.funbridge.server.texts.TextUIMgr;
import com.funbridge.server.tournament.duel.DuelMgr;
import com.funbridge.server.tournament.duel.data.DuelTournamentPlayer;
import com.funbridge.server.tournament.federation.FederationMgr;
import com.funbridge.server.tournament.federation.TourFederationStatPeriodMgr;
import com.funbridge.server.tournament.federation.data.TourFederationTournament;
import com.funbridge.server.tournament.federation.data.TourFederationTournamentPlayer;
import com.funbridge.server.tournament.federation.cbo.TourCBOMgr;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;
import com.funbridge.server.tournament.generic.memory.GenericMemTournamentPlayer;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.serie.memory.TourSerieMemPeriodRankingPlayer;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.event.*;
import com.funbridge.server.ws.result.ResultServiceRest;
import com.funbridge.server.ws.result.WSMainRankingPlayer;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.session.Session;
import com.gotogames.common.tools.JSONTools;
import com.gotogames.common.tools.StringTools;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.bson.types.ObjectId;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component(value="messageNotifMgr")
@Scope(value="singleton")
public class MessageNotifMgr extends FunbridgeMgr{
    /** ----------- **/
    /** MESSAGE TYPE **/
    public static final int MESSAGE_CATEGORY_GENERAL = 1;
    public static final int MESSAGE_CATEGORY_FRIEND = 2;
    public static final int MESSAGE_CATEGORY_SERIE = 3;
    public static final int MESSAGE_CATEGORY_TIMEZONE = 4;
    public static final int MESSAGE_CATEGORY_TRAINING = 5;
    public static final int MESSAGE_CATEGORY_CHALLENGE = 6;
    public static final int MESSAGE_CATEGORY_DUEL = 7;
    public static final int MESSAGE_CATEGORY_PROMO = 8;
    public static final int MESSAGE_CATEGORY_GIFT = 9;
    public static final int MESSAGE_CATEGORY_TEAM = 11;
    public static final int MESSAGE_CATEGORY_PRIVATE = 15;
    public static final int MESSAGE_CATEGORY_TOP_CHALLENGE = 16;
	public static final int MESSAGE_CATEGORY_EASY_CHALLENGE = 36;
	public static final int MESSAGE_CATEGORY_STORE = 21;
    public static final int MESSAGE_CATEGORY_CBO = 22;
	public static final int MESSAGE_CATEGORY_LIN = 23;
	public static final int MESSAGE_CATEGORY_SUPPORT = 24;
	public static final int MESSAGE_CATEGORY_RANKING = 25;
	public static final int MESSAGE_CATEGORY_MY_PROFILE = 26;
	public static final int MESSAGE_CATEGORY_TIPS = 30;
	public static final int MESSAGE_CATEGORY_NEWS = 31;

	public static final Map<Integer, String> mapMessageCategory = new HashMap<>();
	static {
		mapMessageCategory.put(MESSAGE_CATEGORY_GENERAL, "GENERAL");
		mapMessageCategory.put(MESSAGE_CATEGORY_FRIEND, "FRIEND");
		mapMessageCategory.put(MESSAGE_CATEGORY_SERIE, "SERIE");
		mapMessageCategory.put(MESSAGE_CATEGORY_TIMEZONE, "TIMEZONE");
		mapMessageCategory.put(MESSAGE_CATEGORY_TRAINING, "TRAINING");
		mapMessageCategory.put(MESSAGE_CATEGORY_CHALLENGE, "CHALLENGE");
		mapMessageCategory.put(MESSAGE_CATEGORY_DUEL, "DUEL");
		mapMessageCategory.put(MESSAGE_CATEGORY_PROMO, "PROMO");
		mapMessageCategory.put(MESSAGE_CATEGORY_GIFT, "GIFT");
		mapMessageCategory.put(MESSAGE_CATEGORY_TEAM, "TEAM");
		mapMessageCategory.put(MESSAGE_CATEGORY_PRIVATE, "PRIVATE");
		mapMessageCategory.put(MESSAGE_CATEGORY_TOP_CHALLENGE, "TOP_CHALLENGE");
		mapMessageCategory.put(MESSAGE_CATEGORY_EASY_CHALLENGE, "EASY_CHALLENGE");
		mapMessageCategory.put(MESSAGE_CATEGORY_STORE, "STORE");
		mapMessageCategory.put(MESSAGE_CATEGORY_CBO, Constantes.TOURNAMENT_CBO);
		mapMessageCategory.put(MESSAGE_CATEGORY_LIN, "LIN");
		mapMessageCategory.put(MESSAGE_CATEGORY_SUPPORT, "SUPPORT");
		mapMessageCategory.put(MESSAGE_CATEGORY_RANKING, "RANKING");
		mapMessageCategory.put(MESSAGE_CATEGORY_MY_PROFILE, "MY_PROFILE");
		mapMessageCategory.put(MESSAGE_CATEGORY_TIPS, "TIPS");
		mapMessageCategory.put(MESSAGE_CATEGORY_NEWS, "NEWS");
	}

    public static boolean isMessageCategoryValid(int value) {
		return mapMessageCategory.containsKey(value);
	}

    /** DISPLAY MODE **/
    public static final int MESSAGE_DISPLAY_NORMAL = 0;
    public static final int MESSAGE_DISPLAY_TOP_BAR = 1;
    public static final int MESSAGE_DISPLAY_DIALOG_BOX = 2;

	/** ----------- **/
	/** NOTFI PARAM **/
	public static final String NOTIF_PARAM_ACTION = "ACTION";
	public static final String NOTIF_PARAM_PLAYER_ID = "PLAYER_ID";
    public static final String NOTIF_PARAM_PSEUDO = "PSEUDO";
    public static final String NOTIF_PARAM_AVATAR = "AVATAR";
	public static final String NOTIF_PARAM_TOURNAMENT_ID = "TOURNAMENT_ID";
	public static final String NOTIF_PARAM_TOURNAMENT_CATEGORY_ID = "TOURNAMENT_CATEGORY_ID";
	public static final String NOTIF_PARAM_URL_FULL = "URL_FULL";
    public static final String NOTIF_PARAM_PREVIOUS_SERIE = "PREVIOUS_SERIE";
    public static final String NOTIF_PARAM_SERIE = "SERIE";
    public static final String NOTIF_PARAM_PERIOD = "PERIOD";
    public static final String NOTIF_PARAM_TEMPLATE = "TEMPLATE";
    public static final String NOTIF_PARAM_MATCH_MAKING = "MATCH_MAKING";
    public static final String NOTIF_PARAM_TEAM_TYPE = "TEAM_TYPE";
	public static final String NOTIF_PARAM_RANKING_TYPE = "RANKING_TYPE";
	public static final String NOTIF_PARAM_RANKING_OPTIONS = "RANKING_OPTIONS";
	public static final String NOTIF_PARAM_CATEGORY_ID = "CATEGORY_ID";
	public static final String NOTIF_PARAM_PAGE = "PAGE";
	/** ----------- **/
	/** NOTIF PARAM VALUE **/
	public static final String NOTIF_ACTION_OPEN_PROFILE = "OPEN_PROFILE";
	public static final String NOTIF_ACTION_OPEN_URL = "OPEN_URL";
	public static final String NOTIF_ACTION_OPEN_CATEGORY_PAGE = "OPEN_CATEGORY_PAGE";
	public static final String NOTIF_ACTION_OPEN_TOURNAMENT = "OPEN_TOURNAMENT";
    public static final String NOTIF_ACTION_SERIE_PREVIOUS_RANKING = "SERIES_PREVIOUS_RANKING";
    public static final String NOTIF_ACTION_TEAM_TYPE_CHECK = "CHECK";
    public static final String NOTIF_ACTION_TEAM_TYPE_CROSS = "CROSS";
    public static final String NOTIF_ACTION_TEAM_TYPE_CAPTAIN = "CAPTAIN";
    public static final String NOTIF_ACTION_TEAM_TYPE_DELETE = "DELETE";
    public static final String NOTIF_ACTION_TEAM_TYPE_LEAVE = "LEAVE";
    public static final String NOTIF_ACTION_TEAM_TYPE_RANKING = "RANKING";
	public static final String NOTIF_ACTION_OPEN_CATEGORY = "OPEN_CATEGORY";
	public static final String NOTIF_ACTION_OPEN_SETTINGS = "OPEN_SETTINGS";
	public static final String NOTIF_ACTION_OPEN_FRIENDS = "OPEN_FRIENDS";

	/** ----------- **/
	/** NOTIF DUEL **/
	public static final String NOTIF_DUEL_REQUEST = "duelRequest";
	public static final String NOTIF_DUEL_ANSWER_OK = "duelAnswerOK";
	public static final String NOTIF_DUEL_PARTNER_PLAY_ALL = "duelPartnerPlayAll";
	public static final String NOTIF_DUEL_FINISH_PLAYALL_WIN = "duelFinishPlayAllWin";
	public static final String NOTIF_DUEL_FINISH_PLAYALL_LOST = "duelFinishPlayAllLost";
	public static final String NOTIF_DUEL_FINISH_PLAYALL_DRAW = "duelFinishPlayAllDraw";
	public static final String NOTIF_DUEL_FINISH_CLOSE_WIN = "duelFinishCloseWin";
	public static final String NOTIF_DUEL_FINISH_CLOSE_LOST = "duelFinishCloseLost";
	public static final String NOTIF_DUEL_FINISH_CLOSE_DRAW = "duelFinishCloseDraw";
	public static final String NOTIF_DUEL_REMINDER = "duelReminder";
    public static final String NOTIF_DUEL_MATCH_MAKING = "duelMatchMaking";

    /** ------------ **/
	/** NOTIF FRIEND **/
	public static final String NOTIF_FRIEND_REQUEST_ANSWER_OK = "friendRequestAnswerOK";
	public static final String NOTIF_FRIEND_REQUEST_ANSWER_KO = "friendRequestAnswerKO";
	public static final String NOTIF_FRIEND_BIRTHDAY = "friendBirthday";
	public static final String NOTIF_FRIEND_DUEL_FINISH = "friendDuelFinish";
    public static final String NOTIF_FRIEND_DUEL_RESULT = "friendDuelResult";
    public static final String NOTIF_FRIEND_DUEL_RESULT_BUFFER = "friendDuelResultBuffer";
	public static final String NOTIF_FRIEND_EVENT_CONNECTED = "friendEventConnected";
	
	/** ------------ **/
	/** NOTIF BONUS  **/
	public static final String NOTIF_BONUS_BIRTHDAY = "bonusBirthday";
	public static final String NOTIF_BONUS_CREDIT_MINIMUM = "bonusCreditMinimum";
	public static final String NOTIF_BONUS_PROFILE = "bonusProfile";
	public static final String NOTIF_BONUS_VALID_MAIL = "bonusValidMail";
    public static final String NOTIF_BONUS_SPECIAL = "bonusSpecial";
	
	/** ------------- **/
	/** NOTIF CREDIT  **/
	public static final String NOTIF_CREDIT_BUY_DEAL = "creditBuyDeal";
	public static final String NOTIF_CREDIT_BUY_SUBSCRIPTION = "creditBuySubscription";
    public static final String NOTIF_CREDIT_BUY_COMMENTED_TOURNAMENT = "creditBuyCommentedTournament";

    public static final String NOTIF_CREDIT_SUBSCRIPTION_EXPIRATION_J2 ="creditSubscriptionExpirationJ2";
    public static final String NOTIF_CREDIT_SUBSCRIPTION_EXPIRATION_J7 ="creditSubscriptionExpirationJ7";
	
	/** -------------- **/
	/** NOTIF WELCOME  **/
	public static final String NOTIF_WELCOME_BONUS = "welcomeBonus";

	/** ---------------- **/
	/** NOTIF RANKING    **/
	public static final String NOTIF_RANKING_PTS_FB = "rankingPtsFB";
	public static final String NOTIF_RANKING_TOP10_PERCENT_PTS_FB = "rankingTop10PercentPtsFB";
	public static final String NOTIF_RANKING_TOP3_PTS_FB = "rankingTop3PtsFB";

	public static final String NOTIF_RANKING_DUEL = "rankingDuel";
	public static final String NOTIF_RANKING_TOP10_PERCENT_DUEL = "rankingTop10PercentDuel";
	public static final String NOTIF_RANKING_TOP3_DUEL = "rankingTop3Duel";

	public static final String NOTIF_RANKING_DUEL_ARGINE = "rankingDuelArgine";
	public static final String NOTIF_RANKING_TOP10_PERCENT_DUEL_ARGINE = "rankingTop10PercentDuelArgine";
	public static final String NOTIF_RANKING_TOP3_DUEL_ARGINE = "rankingTop3DuelArgine";

	public static final String NOTIF_RANKING_ALL_PODIUMS = "rankingAllPodiums";

	
	/** -------------- **/
	/** NOTIF SERIE    **/
	public static final String NOTIF_SERIE_UP = "serieUp";
	public static final String NOTIF_SERIE_UP_ELITE = "serieUpElite";
    public static final String NOTIF_SERIE_UP_NEW = "serieUpNew";
    public static final String NOTIF_SERIE_MAINTAIN = "serieMaintain";
    public static final String NOTIF_SERIE_MAINTAIN_11 = "serieMaintain11";
	public static final String NOTIF_SERIE_MAINTAIN_ELITE = "serieMaintainElite";
	public static final String NOTIF_SERIE_MAINTAIN_TOP_10 = "serieMaintainTop10";
    public static final String NOTIF_SERIE_NEW_MAINTAIN_NEW = "serieNewMaintainNew";
    public static final String NOTIF_SERIE_NEW_MAINTAIN_NO_PLAY = "serieNewMaintainNoPlay";
    public static final String NOTIF_SERIE_NEW_MAINTAIN_NO_PLAY_11 = "serieNewMaintainNoPlay11";
	public static final String NOTIF_SERIE_NEW_MAINTAIN_NO_PLAY_ELITE = "serieNewMaintainNoPlayElite";
    public static final String NOTIF_SERIE_DOWN = "serieDown";
    public static final String NOTIF_SERIE_NEW_DOWN_NO_PLAY = "serieNewDownNoPlay";
    public static final String NOTIF_SERIE_NEW_NEW_NO_PLAY = "serieNewNewNoPlay";
    public static final String NOTIF_SERIE_NEW_UPDATE_BEST_RESULT = "serieNewUpdateBestResult";

	public static final String NOTIF_SERIE_REMINDER_DOWN = "serieReminderDown";
	public static final String NOTIF_SERIE_REMINDER_MAINTAIN = "serieReminderMaintain";
	public static final String NOTIF_SERIE_REMINDER_MAINTAIN_TOP = "serieReminderMaintainTop";
	public static final String NOTIF_SERIE_REMINDER_UP = "serieReminderUp";
	public static final String NOTIF_SERIE_REMINDER = "serieReminder";

    public static final String VARIABLE_SERIE_START = "{SERIE_", VARIABLE_SERIE_END = "}";
    public static final String VARIABLE_TEAM_DIVISION_START = "{DIVISION_", VARIABLE_DIVISION_END = "}";
	public static final String VARIABLE_PRICE_START = "{PRICE_";
	public static final String VARIABLE_TIMESTAMP_FORMAT_DATE = "{TIMESTAMP_DATE_";
	public static final String VARIABLE_TIMESTAMP_FORMAT_DATETIME = "{TIMESTAMP_DATETIME_";
	public static final String VARIABLE_TIMESTAMP_FORMAT_TIME = "{TIMESTAMP_TIME_";
	public static final String VARIABLE_TEXT_UI = "{TEXTUI_";
	public static final String VARIABLE_TIMESTAMP_FORMAT_DAYMONTH = "{TIMESTAMP_DAYMONTH_";
	public static final String VARIABLE_NUMBER_START = "{NUMBER_";
	public static final String VARIABLE_END = "}";

    public static final String VARIABLE_PLAYER_PSEUDO = "PLA_PSEUDO";
    public static final String VARIABLE_PLAYER_ID = "PLAYER_ID";
    public static final String VARIABLE_PLAYER_EXPIRATION = "EXPIRATION";
    public static final String VARIABLE_LINE_BREAK = "LINE_BREAK";
    public static final String VARIABLE_PLAYER_SERIE = "PLA_SERIE";
	public static final String VARIABLE_DATEPLUS7 = "DATEPLUS7";
	public static final String VARIABLE_DATEPLUS2 = "DATEPLUS2";

	private JSONTools jsontools = new JSONTools();
	@Resource(name="mongoTemplate")
	private MongoTemplate mongoTemplate;
    @Resource(name="playerCacheMgr")
    private PlayerCacheMgr playerCacheMgr;
    @Resource(name="textUIMgr")
    private TextUIMgr textUIMgr;
	@Resource(name = "tourCBOMgr")
	private TourCBOMgr tourCBOMgr;
	@Resource(name = "tourFederationStatPeriodMgr")
	private TourFederationStatPeriodMgr tourFederationStatPeriodMgr;
	@Resource(name = "tourSerieMgr")
	private TourSerieMgr tourSerieMgr;
	@Resource(name = "playerMgr")
	private PlayerMgr playerMgr;
	@Resource(name = "duelMgr")
	private DuelMgr duelMgr;
	@Resource(name="presenceMgr")
	private PresenceMgr presenceMgr = null;

	public static Pattern pattern;

    private LockWeakString lockUpsertNotifBuffer = new LockWeakString();
    private ScheduledExecutorService schedulerNotifBufferTransform = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> schedulerNotifBufferTransformFuture = null;
    private NotifBufferTransformTask notifBufferTransformTask = new NotifBufferTransformTask();
    private String resultLastNotifBufferTransformTask = "";
    private long tsLastNotifBufferTransformTask = 0;
    private int nbNotifLastTransformTask = 0;
	private Scheduler scheduler;

    public static final long DURATION_1J = (24*60*60*1000);
    public static final long DURATION_7J = 7*DURATION_1J;
	public static final long DURATION_2J = 2*DURATION_1J;
	public static final long DURATION_14J = 14*DURATION_1J;
    public static final long DURATION_30J = 30*DURATION_1J;


    /**
     * Task to transform buffer to notif
     * @author pserent
     *
     */
    public class NotifBufferTransformTask implements Runnable {
        boolean isRunning = false;
        @Override
        public void run() {
            if (FBConfiguration.getInstance().getIntValue("message.notifBuffer.taskEnable", 0) == 1) {
                if (!isRunning) {
                    isRunning = true;
                    try {
                        processTransformNotifBuffer();
                    } catch (Exception e) {
                        log.error("Failed to processTransformNotifBuffer", e);
                        ContextManager.getAlertMgr().addAlert("MESSAGE_NOTIF_MGR", FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to transformNotifBuffer", e.getMessage(), null);
                    }
                    isRunning = false;
                }
            } else {
                ContextManager.getAlertMgr().addAlert("MESSAGE_NOTIF_MGR", FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for transformNotifBuffer", null, null);
                if (log.isDebugEnabled()) {
                    log.debug("Transform buffer to notif is disable !");
                }
            }
        }

    }

	@PostConstruct
	@Override
	public void init() {
		try {
			pattern = Pattern.compile("\\{(.+?)\\}");
		} catch (PatternSyntaxException e) {
			log.error("Pattern not valid !",e);
		}
	}

	@PreDestroy
	@Override
	public void destroy() {
		log.info("destroy");
		stopScheduler(schedulerNotifBufferTransform);
	}

	@Override
	public void startUp() {
        // start notifBufferTransform timer task
        try {
            int periodTask = FBConfiguration.getInstance().getIntValue("message.notifBuffer.taskPeriodMinute", 5);
            schedulerNotifBufferTransformFuture = schedulerNotifBufferTransform.scheduleWithFixedDelay(notifBufferTransformTask, 5, periodTask, TimeUnit.MINUTES);
            log.warn("Schedule notifBufferTransformTask - next run at " + Constantes.getStringDateForNextDelayScheduler(schedulerNotifBufferTransformFuture) + " - period (minute)=" + periodTask);
        } catch (Exception e) {
            log.error("Exception to start notifBufferTransform task", e);
        }

		// task thread for notif ranking all podiums
		try {
			SchedulerFactory schedulerFactory = new StdSchedulerFactory();
			scheduler = schedulerFactory.getScheduler();
			// Enable task - triger every first day of month
			JobDetail jobProcess = JobBuilder.newJob(NotifRankingAllPodiumsTask.class).withIdentity("notifRankingAllPodiumsTask", "Notif").build();
			CronTrigger triggerProcess = TriggerBuilder.newTrigger().withIdentity("triggerNotifRankingAllPodiumsTask", "Notif").withSchedule(CronScheduleBuilder.cronSchedule("0 0 1 1 * ?")).build();
			Date dateNextJobProcess = scheduler.scheduleJob(jobProcess, triggerProcess);
			log.warn("Sheduled for job=" + jobProcess.getKey() + " run at="+dateNextJobProcess+" - cron expression=" + triggerProcess.getCronExpression() + " - next fire=" + triggerProcess.getNextFireTime());
		} catch (Exception e) {
			log.error("Exception to start ranking all podiums process task", e);
		}
	}

	public long getDateNextJobNotifRankingAllPodiums() {
		try {
			Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerNotifRankingAllPodiumsTask", "Notif"));
			if (trigger != null) {
				return trigger.getNextFireTime().getTime();
			}
		} catch (Exception e) {
			log.error("Failed to get trigger", e);
		}
		return 0;
	}

	/**
	 * Check if notif is enable
	 * @return
	 */
	public boolean isNotifEnable() {
		return FBConfiguration.getInstance().getIntValue("message.notifEnable", 1) == 1;
	}

	public String[] getSupportedLang() {
        return textUIMgr.getSupportedLang();
	}

    /**
     * Return the date of the next notif buffer transform task run.
     * @return
     */
    public String getStringDateNextNotifBufferTransformScheduler() {
        return Constantes.getStringDateForNextDelayScheduler(schedulerNotifBufferTransformFuture);
    }

    public long getTsLastNotifBufferTransformTask() {
        return tsLastNotifBufferTransformTask;
    }

    public String getResultLastNotifBufferTransformTask() {
        return resultLastNotifBufferTransformTask;
    }

    public int getNbNotifLastTransformTask() {
        return nbNotifLastTransformTask;
    }

	/**
	 * Retrieve the value of key in config file
	 * @param fileConfig
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	private String getNotifText(FileConfiguration fileConfig, String key, String defaultValue) {
		String value = defaultValue;
		if (fileConfig != null && key != null && key.length() > 0) {
			value = fileConfig.getString(key, defaultValue);
		} else {
			if (fileConfig == null) {
				log.error("File config is null key="+key+" - defaultValue="+defaultValue);
			} else {
				log.error("key not valid ! key="+key);
			}
		}
		return value;
	}
	
	/**
	 * Return template for message with template name
	 * @param name
	 * @return
	 */
	public TextUIData getTemplate(String name) {
        return textUIMgr.getTextNotif(name);
	}

	/**
	 * Return template rich body for message with template name
	 * @param name
	 * @return
	 */
	public TextUIData getTemplateRichBody(String name) {
		return textUIMgr.getTextNotif(name+".richbody");
	}

	/**
	 * Return template title for message with template name
	 * @param name
	 * @return
	 */
	public TextUIData getTemplateTitle(String name) {
		return textUIMgr.getTextNotif(name+".title");
	}

	/**
	 * Return template button for message with template name
	 * @param name
	 * @return
	 */
	public TextUIData getTemplateButton(String name) {
		return textUIMgr.getTextNotif(name+".button");
	}

	/**
	 * Return template action for message with template name
	 * @param name
	 * @return
	 */
	public TextUIData getTemplateAction(String name) {
		return textUIMgr.getTextNotif(name+".action");
	}
	
	/**
	 * Return all template message
	 * @return
	 */
	public List<TextUIData> listAllTemplate() {
        List<String> listTemplateName = textUIMgr.getListTemplateNameNotif();
        List<TextUIData> listTemplate = new ArrayList<>();
        for (String e : listTemplateName) {
            TextUIData temp = textUIMgr.getTextNotif(e+".body");
            listTemplate.add(temp);
			temp = textUIMgr.getTextNotif(e+".richbody");
			listTemplate.add(temp);
			temp = textUIMgr.getTextNotif(e+".title");
			listTemplate.add(temp);
			temp = textUIMgr.getTextNotif(e+".button");
			listTemplate.add(temp);
			temp = textUIMgr.getTextNotif(e+".action");
			listTemplate.add(temp);
        }
        return listTemplate;
	}
	
	/**
	 * Replace the variables in text by the values contain in the map. Variable are like '{VAR_NAME}' and map contain pair value (VAR_NAME, value)
	 * @param msgText
	 * @param varVal
	 * @return
	 */
	public static String replaceTextVariables(String msgText, Map<String, String> varVal) {
		String strReturn = msgText;
        if (msgText != null && msgText.length() > 0) {
            if (msgText.indexOf("{") >= 0) {
                if (varVal != null && varVal.size() > 0) {
                    // replace all token by the value
                    Matcher matcher = pattern.matcher(msgText);
                    StringBuffer sb = new StringBuffer();
                    while (matcher.find()) {
                        String val = varVal.get(matcher.group(1));
                        if (val != null) {
                            matcher.appendReplacement(sb, "");
                            sb.append(val);
                        }
                    }
                    matcher.appendTail(sb);
                    strReturn = sb.toString();
                }
            }
        }
		return strReturn;
	}
	
	/**
	 * Check if text contains variable serie
	 * @param msgText
	 * @return
	 */
	public static boolean textContainVariableSerie(String msgText) {
		if (msgText != null) {
			return msgText.indexOf(VARIABLE_SERIE_START) >= 0;
		}
		return false;
	}

    /**
     * Check if text contains variable team division
     * @param msgText
     * @return
     */
    public static boolean textContainVariableTeamDivision(String msgText) {
        if (msgText != null) {
            return msgText.indexOf(VARIABLE_TEAM_DIVISION_START) >= 0;
        }
        return false;
    }

	/**
	 * Check if text contains variable expiration
	 * @param msgText
	 * @return
	 */
	public static boolean textContainVariableExpiration(String msgText) {
		if (msgText != null) {
			return msgText.contains(VARIABLE_PLAYER_EXPIRATION);
		}
		return false;
	}

	/**
	 * Check if text contains variable
	 * @param msgText
	 * @param var
	 * @return
	 */
	public static boolean textContainsVariable(String msgText, String var) {
		if (msgText != null) {
			return msgText.contains(var);
		}
		return false;
	}
	
	/**
	 * Replace variable in text notif with value from map. Variable are like '{VAR_NAME}' and map contain pair value (VAR_NAME, value)
	 * @param notif
	 * @param mapExtra
	 */
	private void replaceNotifTextVariables(MessageNotifBase notif,	Map<String, String> mapExtra) {
		if (notif != null && mapExtra != null && mapExtra.size() > 0) {
			notif.msgBodyEN = replaceTextVariables(notif.msgBodyEN, mapExtra);
			notif.msgBodyFR = replaceTextVariables(notif.msgBodyFR, mapExtra);
			notif.msgBodyNL = replaceTextVariables(notif.msgBodyNL, mapExtra);
		}
	}

    /**
	 * Return the current time stamp. A sleep time of 2 ms is do to be sure to have different TS
	 * @return
	 */
	private static synchronized long getTimeStamp() {
		try {
			Thread.sleep(2);
		} catch (InterruptedException e) {
		}
		return System.currentTimeMillis();
	}
	
	/**
	 * Build event associated to this notif.
	 * @param notif
	 * @param recipient recipient for the event
	 * @return
	 */
	public Event buildEvent(MessageNotifBase notif, Player recipient) {
		if (notif != null && recipient != null) {
			WSMessage wsmsg = buildWSMessage(notif, recipient);
			if (wsmsg != null) {
				try {
					Event evt = new Event();
					evt.timestamp = getTimeStamp();
					evt.senderID = wsmsg.senderID;
					evt.receiverID = recipient.getID();
					int maxLength = FBConfiguration.getInstance().getIntValue("message.fixBugEventTooBig", 0);
					if (maxLength > 0) {
						wsmsg.body = StringTools.truncate(wsmsg.body, maxLength, "...");
					}
					evt.addFieldCategory(Constantes.EVENT_CATEGORY_MESSAGE, jsontools.transform2String(wsmsg, false));
					return evt;
				} catch (JsonGenerationException e) {
					log.error("JsonGenerationException to transform msg="+wsmsg,e);
				} catch (JsonMappingException e) {
					log.error("JsonMappingException to transform msg="+wsmsg,e);
				} catch (IOException e) {
					log.error("IOException to transform msg="+wsmsg,e);
				}
			}
		} else {
			log.error("Parameter not valid !");
		}
		return null;
	}

    /**
     * Build event associated to this notif.
     * @param notif
     * @param recipientPlayerID
     * @return
     */
    public Event buildEvent(MessageNotifBase notif, long recipientPlayerID) {
        if (notif != null) {
            PlayerCache playerCache = ContextManager.getPlayerCacheMgr().getPlayerCache(recipientPlayerID);
            if (playerCache != null) {
				WSMessageNotif wsmsg = buildWSMessage(notif, playerCache);
                if (wsmsg != null) {
                    try {
                        Event evt = new Event();
                        evt.timestamp = getTimeStamp();
                        evt.senderID = wsmsg.senderID;
                        evt.receiverID = recipientPlayerID;
                        int maxLength = FBConfiguration.getInstance().getIntValue("message.fixBugEventTooBig", 0);
                        if (maxLength > 0) {
                            wsmsg.body = StringTools.truncate(wsmsg.body, maxLength, "...");
                            wsmsg.richBody = StringTools.truncate(wsmsg.richBody, maxLength, "...");
                        }
                        evt.addFieldCategory(Constantes.EVENT_CATEGORY_MESSAGE, jsontools.transform2String(wsmsg, false));
                        return evt;
                    } catch (JsonGenerationException e) {
                        log.error("JsonGenerationException to transform msg=" + wsmsg, e);
                    } catch (JsonMappingException e) {
                        log.error("JsonMappingException to transform msg=" + wsmsg, e);
                    } catch (IOException e) {
                        log.error("IOException to transform msg=" + wsmsg, e);
                    }
                }
            } else {
                log.error("No playerCache found for recipientPlayerID="+recipientPlayerID);
            }
        } else {
            log.error("Parameter not valid !");
        }
        return null;
    }

	/**
	 * List notifGroup for player
	 * @param playerID
	 * @param tsCurrent
	 * @return
	 */
	public List<MessageNotifGroupRead> listNotifGroupReadForPlayer(long playerID, long tsCurrent) {
		if (isNotifEnable()) {
			try {
				Criteria c = Criteria.where("playerID").is(playerID).andOperator(Criteria.where("dateNotifExpiration").gt(tsCurrent));
				return mongoTemplate.find(new Query(c), MessageNotifGroupRead.class);
			} catch (Exception e) {
				log.error("Failed to list notifGroup for player="+playerID, e);
			}
		}
		return null;
	}

    /**
     * List notif type ALL create after a specific ts and not expired
     * @param tsCreateAfter
     * @return
     */
	public List<MessageNotifAll> listNotifAll(long tsCreateAfter) {
	    // C1 msgType ALL
        Criteria cNotifAll = Criteria.where("msgType").is(MessageNotifAll.MSG_TYPE);
        // C2 not expired
        Criteria cNotExpired = Criteria.where("dateExpiration").gt(System.currentTimeMillis());
        // C3 date msg after ts
	    Criteria cDateMsgAfter = Criteria.where("dateMsg").gt(tsCreateAfter);

        return mongoTemplate.find(Query.query(new Criteria().andOperator(cNotifAll, cNotExpired, cDateMsgAfter)), MessageNotifAll.class);
    }

	/**
	 * List message for player
	 * @param p
	 * @param offset
	 * @param nbMax
	 * @param tsCurrent
	 * @return
	 */
	public List<WSMessage> listWSMessageForPlayer(Player p, int offset, int nbMax, long tsCurrent) {
		List<WSMessage> listReturn = new ArrayList<WSMessage>();
		if (isNotifEnable()) {
			try {
				Query q = new Query();
				// all message notif all
				Criteria cNotifAll = Criteria.where("msgType").is(MessageNotifAll.MSG_TYPE).andOperator(Criteria.where("dateExpiration").gt(tsCurrent), Criteria.where("dateMsg").gte(p.getCreationDate()));
				// all message notif for player
				Criteria cNotif = Criteria.where("msgType").is(MessageNotif.MSG_TYPE).andOperator(Criteria.where("recipientID").is(p.getID()), Criteria.where("dateExpiration").gt(tsCurrent));
				// all message notifGroup for player
				List<MessageNotifGroupRead> listNotifGroupRead = listNotifGroupReadForPlayer(p.getID(), tsCurrent);
				Map<String, Map<String, String>> mapNotifIdTemplateParameters = new HashMap<>();
				Map<String, MessageNotifGroupRead> mapNotifIdGroupRead = new HashMap<>();
				Criteria cNotifGroup = null;
				if (listNotifGroupRead != null && listNotifGroupRead.size() > 0) {
					List<ObjectId> listID = new ArrayList<>();
					for (MessageNotifGroupRead e : listNotifGroupRead) {
						if (e != null && e.notifGroup != null && e.notifGroup.ID != null) {
							listID.add(e.notifGroup.ID);
							if (e instanceof MessageNotifGroupReadWithParameters) {
								mapNotifIdTemplateParameters.put(e.notifGroup.getIDStr(), ((MessageNotifGroupReadWithParameters) e).templateParameters);
							}
							mapNotifIdGroupRead.put(e.notifGroup.getIDStr(), e);
						}
					}
					if (listID.size() > 0) {
						cNotifGroup = Criteria.where("msgType").is(MessageNotifGroup.MSG_TYPE).andOperator(Criteria.where("ID").in(listID));
					}
				}
				// OR operator to concat
				if (cNotifGroup != null) {
					q.addCriteria(new Criteria().orOperator(cNotifAll, cNotif, cNotifGroup));
				} else {
					q.addCriteria(new Criteria().orOperator(cNotifAll, cNotif));
				}

				// execute query to list message
				List<MessageNotifBase> listMsgNotif = mongoTemplate.find(q, MessageNotifBase.class);

				if (listMsgNotif != null && listMsgNotif.size() > 0) {
					// update notifs with notifGroupRead
					for (MessageNotifBase notif : listMsgNotif) {
						// Add player read specific template parameters
						notif.addTemplateParameters(mapNotifIdTemplateParameters.get(notif.getIDStr()));
						// Use player read specific date message if defined
						MessageNotifGroupRead notifGroupRead = mapNotifIdGroupRead.get(notif.getIDStr());
						if (notifGroupRead != null && notifGroupRead.dateMsg != 0) {
							notif.dateMsg = notifGroupRead.dateMsg;
						}
					}

					// sort using dateMsg
					Collections.sort(listMsgNotif, new Comparator<MessageNotifBase>() {
						@Override
						public int compare(MessageNotifBase o1, MessageNotifBase o2) {
							return o1.dateMsg > o2.dateMsg ? -1 : (o1.dateMsg == o2.dateMsg ? 0 : 1);
						}
					});

					// offset && limit
					if (offset > 0 || nbMax < listMsgNotif.size()) {
						int idxBegin = 0, idxEnd = listMsgNotif.size();
						if (offset > 0 && offset < listMsgNotif.size()) {
							idxBegin = offset;
						}
						if ((idxBegin+nbMax) < listMsgNotif.size()) {
							idxEnd = idxBegin+nbMax;
						}
						listMsgNotif = listMsgNotif.subList(idxBegin, idxEnd);
					}

					// Convert notifs to WSMessages
					for (MessageNotifBase notif : listMsgNotif) {
						listReturn.add(buildWSMessage(notif, p));
					}
				}
			} catch (Exception e) {
				log.error("Failed to list message for player p="+p+" - offset="+offset+" - nbMax="+nbMax, e);
			}
		}
		return listReturn;
	}
	
	/**
	 * Check if player with ID has read the notif with ID
	 * @param notifID
	 * @param playerID
	 * @return
	 */
	public boolean isNotifAllReadByPlayer(String notifID, long playerID) {
		if (isNotifEnable()) {
			try {
				Criteria c = Criteria.where("notifAll.$id").is(new ObjectId(notifID)).andOperator(Criteria.where("playerID").is(playerID));
				MessageNotifAllRead notifAllRead = mongoTemplate.findOne(new Query(c), MessageNotifAllRead.class);
				if (notifAllRead != null) {
					return notifAllRead.isRead();
				}
			} catch (Exception e) {
				log.error("Failed to check if notifAll is read for player="+playerID+" - notifID="+notifID, e);
			}
		}
		return false;
	}
	
	/**
	 * Check if player with ID has read the notif with ID
	 * @param notifID
	 * @param playerID
	 * @return
	 */
	public boolean isNotifGroupReadByPlayer(String notifID, long playerID) {
		if (isNotifEnable()) {
            MessageNotifGroupRead notifGroupRead = getNotifGroupReadForPlayer(notifID, playerID);
            if (notifGroupRead != null) {
			    return notifGroupRead.isRead();
			}
		}
		return false;
	}

    /**
     * Retrieve the notifGroupRead for a notif and a player. Null if not existing.
     * @param notifID
     * @param playerID
     * @return
     */
    public MessageNotifGroupRead getNotifGroupReadForPlayer(String notifID, long playerID) {
        try {
            Criteria c = Criteria.where("notifGroup.$id").is(new ObjectId(notifID)).andOperator(Criteria.where("playerID").is(playerID));
            return mongoTemplate.findOne(new Query(c), MessageNotifGroupRead.class);
        } catch (Exception e) {
            log.error("Failed to get notifGroupRead for player="+playerID+" - notifID="+notifID, e);
        }
        return null;
    }

    /**
     * Insert a notifGroupRead for player on notif
     * @param notifGroup
     * @param playerID
     * @return the MessageNotifGroupRead bean inserted
     */
    public MessageNotifGroupRead insertNotifGroupReadForPlayer(MessageNotifGroup notifGroup, long playerID) {
        if (notifGroup != null) {
            MessageNotifGroupRead ngr = new MessageNotifGroupRead();
            ngr.dateNotifExpiration = notifGroup.dateExpiration;
            ngr.notifGroup = notifGroup;
            ngr.playerID = playerID;
            try {
                mongoTemplate.insert(ngr);
            } catch (Exception e) {
                log.error("Failed to insert MessageNotifGroupRead="+ngr, e);
            }
        }
        return null;
    }

	/**
	 * Count notif for player (simple + all + group)
	 * @param playerID
	 * @param tsCurrent
	 * @return
	 */
	public int countNotifForPlayer(long playerID, long tsCurrent) {
		return countNotifSimpleForPlayer(playerID, tsCurrent) + countNotifAll(tsCurrent) + countNotifGroupForPlayer(playerID, tsCurrent);
	}
	
	/**
	 * Count notif simple for a player
	 * @param playerID
	 * @param tsCurrent
	 * @return
	 */
	public int countNotifSimpleForPlayer(long playerID, long tsCurrent) {
		if (isNotifEnable()) {
			Query q = new Query();
			q.addCriteria(Criteria.where("msgType").is(MessageNotif.MSG_TYPE).andOperator(Criteria.where("recipientID").is(playerID), Criteria.where("dateExpiration").gt(tsCurrent)));
			try {
				return (int)mongoTemplate.count(q, MessageNotif.class);
			} catch (Exception e) {
				log.error("Error to count notif for player="+playerID+" - query="+q, e);
			}
		}
		return 0;
	}
	
	/**
	 * Count notif group for a player
	 * @param playerID
	 * @param tsCurrent
	 * @return
	 */
	public int countNotifGroupForPlayer(long playerID, long tsCurrent) {
		if (isNotifEnable()) {
			Query q = new Query();
			q.addCriteria(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("dateNotifExpiration").gt(tsCurrent)));
			try {
				return (int)mongoTemplate.count(q, MessageNotifGroupRead.class);
			} catch (Exception e) {
				log.error("Error to count notif for player="+playerID+" - query="+q, e);
			}
		}
		return 0;
	}
	
	/**
	 * Count notifAll
	 * @param tsCurrent
	 * @return
	 */
	public int countNotifAll(long tsCurrent) {
		if (isNotifEnable()) {
			Query q = new Query();
			q.addCriteria(Criteria.where("msgType").is(MessageNotifAll.MSG_TYPE).andOperator(Criteria.where("dateExpiration").gt(tsCurrent)));
			try {
				return (int)mongoTemplate.count(q, MessageNotifAll.class);
			} catch (Exception e) {
				log.error("Error to count notifAll - query="+q, e);
			}
		}
		return 0;
	}

	/**
	 * Modify notif and set flag read to true
	 * @param playerID
	 * @param listNotifID
	 */
	public boolean setNotifSimpleRead(long playerID, List<String> listNotifID) {
		if (isNotifEnable()) {
			try {
				if (listNotifID != null && listNotifID.size() > 0) {
					Criteria c = Criteria.where("msgType").is(MessageNotif.MSG_TYPE).andOperator(Criteria.where("recipientID").is(playerID),Criteria.where("ID").in(listNotifID));
					Update update = new Update();
					update.set("dateRead", System.currentTimeMillis());
					long ts = System.currentTimeMillis();
					mongoTemplate.updateMulti(new Query(c), update, MessageNotif.class);
					if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
						log.error("Mongo update too long ! ts="+(System.currentTimeMillis() - ts));
					}

					// Update read field in PlayerNursing
					c = Criteria.where("playerID").is(playerID).and("notifID").in(listNotifID);
					update = new Update();
					update.set("read", true);
					ts = System.currentTimeMillis();
					mongoTemplate.updateMulti(new Query(c), update, PlayerNursing.class);
					if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
						log.error("Mongo update too long ! ts="+(System.currentTimeMillis() - ts));
					}
					return true;
				}
			} catch (Exception e) {
				log.error("Failed to set notif read for playerID="+playerID+" - listNotifID="+StringTools.listToString(listNotifID), e);
			}
		}
		return false;
	}
	
	/**
	 * Return the notifAll with ID
	 * @param notifID
	 * @return
	 */
	public MessageNotifAll getNotifAll(String notifID) {
		if (isNotifEnable()) {
			try {
				return mongoTemplate.findById(new ObjectId(notifID), MessageNotifAll.class);
			} catch (Exception e) {
				log.error("Failed to get notifAll with id="+notifID, e);
			}
		}
		return null;
	}
	
	/**
	 * Return the notifGroup with ID
	 * @param notifID
	 * @return
	 */
	public MessageNotifGroup getNotifGroup(String notifID) {
		if (isNotifEnable()) {
			try {
				return mongoTemplate.findById(new ObjectId(notifID), MessageNotifGroup.class);
			} catch (Exception e) {
				log.error("Failed to get notifGroup with id="+notifID, e);
			}
		}
		return null;
	}
	
	/**
	 * Set flag read to true for notifGroup read
	 * @param playerID
	 * @param listNotifGroupID
	 * @return
	 */
	public boolean setNotifGroupRead(long playerID, List<String> listNotifGroupID) {
		if (isNotifEnable()) {
			try {
				if (listNotifGroupID != null && listNotifGroupID.size() > 0) {
				    long tsDateRead = System.currentTimeMillis();
					for (String notifID : listNotifGroupID) {
						MessageNotifGroup notifGroup = getNotifGroup(notifID);
						if (notifGroup != null && notifGroup.checkType()) {
							Criteria c = Criteria.where("notifGroup.$id").is(new ObjectId(notifID)).andOperator(Criteria.where("playerID").is(playerID));
							MessageNotifGroupRead notifGroupRead = mongoTemplate.findOne(new Query(c), MessageNotifGroupRead.class);
							if (notifGroupRead != null) {
								notifGroupRead.dateRead = tsDateRead;
								long ts = System.currentTimeMillis();
								mongoTemplate.save(notifGroupRead);
								if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
									log.error("Mongo update too long ! ts="+(System.currentTimeMillis() - ts));
								}
							} else {
								log.error("No notifGroupRead found for notif="+notifGroup+" - playerID="+playerID);
							}
						}
					}
					return true;
				}
			} catch (Exception e) {
				log.error("Failed to set notifGroup read for playerID="+playerID+" - listNotifGroupID="+StringTools.listToString(listNotifGroupID), e);
			}
		}
		return false;
	}
	
	/**
	 * Set flag read to true for notifAll read
	 * @param playerID
	 * @param listNotifAllID
	 */
	public boolean setNotifAllRead(long playerID, List<String> listNotifAllID) {
		if (isNotifEnable()) {
			try {
				if (listNotifAllID != null && listNotifAllID.size() > 0) {
				    long tsDateRead = System.currentTimeMillis();
					for (String notifID : listNotifAllID) {
						MessageNotifAll notifAll = getNotifAll(notifID);
						if (notifAll != null && notifAll.checkType()) {
							Criteria c = Criteria.where("notifAll.$id").is(new ObjectId(notifID)).andOperator(Criteria.where("playerID").is(playerID));
							MessageNotifAllRead notifAllRead = mongoTemplate.findOne(new Query(c), MessageNotifAllRead.class);
							if (notifAllRead == null) {
								notifAllRead = new MessageNotifAllRead();
								notifAllRead.dateRead = tsDateRead;
								notifAllRead.playerID = playerID;
								notifAllRead.notifAll = notifAll;
								notifAllRead.dateNotifExpiration = notifAll.dateExpiration;
                                if (notifAll.getCreationDateISO() != null) {
                                    notifAllRead.creationDateISO = new Date(notifAll.getCreationDateISO().getTime());
                                } else {
                                    notifAllRead.creationDateISO = new Date();
                                }
								long ts = System.currentTimeMillis();
								mongoTemplate.insert(notifAllRead);
								if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
									log.error("Mongo update too long ! ts="+(System.currentTimeMillis() - ts));
								}
							} else {
								notifAllRead.dateRead = System.currentTimeMillis();
								long ts = System.currentTimeMillis();
								mongoTemplate.save(notifAllRead);
								if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
									log.error("Mongo update too long ! ts="+(System.currentTimeMillis() - ts));
								}
							}
						}
					}
					return true;
				}
			} catch (Exception e) {
				log.error("Failed to set notifAll read for playerID="+playerID+" - listNotifAllID="+StringTools.listToString(listNotifAllID), e);
			}
		}
		return false;
	}
	
	/**
	 * Persist the notif
	 * @param notif
	 * @return true if notif is correctly persist
	 */
	public boolean persistNotif(MessageNotifBase notif) {
		try {
			long ts = System.currentTimeMillis();
			mongoTemplate.insert(notif);
			if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
				log.error("Mongo insert too long ! ts="+(System.currentTimeMillis() - ts+" - notif="+notif));
			}
			return true;
		}
		catch (Exception e) {
			log.error("Failed to persist notif", e);
		}
		return false;
	}

    /**
     * Remove notif
     * @param notif
     * @return
     */
    public boolean removeNotif(MessageNotifBase notif) {
        if (notif != null) {
            mongoTemplate.remove(notif);
            return true;
        }
        return false;
    }

	/**
	 * Create notif for duel request.  Visuel H - Expiration=Date fin demande
	 * @param sender
	 * @param recipient
	 * @param duration
	 * @return
	 */
	public MessageNotif createNotifDuelRequest(Player sender, Player recipient, long duration) {
		if (isNotifEnable()) {
			if (sender != null && recipient != null) {
                TextUIData notifTemplate = getTemplate(NOTIF_DUEL_REQUEST);
				if (notifTemplate != null) {
					MessageNotif notif = new MessageNotif(notifTemplate.name);
					notif.recipientID = recipient.getID();
					notif.category = MESSAGE_CATEGORY_DUEL;
					notif.displayMask = MESSAGE_DISPLAY_TOP_BAR;
					notif.dateRead = notif.dateMsg;
					notif.dateExpiration = System.currentTimeMillis() + duration;
					notif.templateParameters.put("PARTNER_PSEUDO", sender.getNickname());
					notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);
					if (persistNotif(notif)) {
						return notif;
					}
				} else {
					log.error("Notif template not found for name="+NOTIF_DUEL_REQUEST);
				}
			} else {
				log.error("Parameters not valid ... sender="+sender+" - recipient="+recipient);
			}
		}
		return null;
	}
	
	/**
	 * Create notif for duel answer OK. Visuel H - Expiration=Date fin defi
	 * @param sender
	 * @param recipient
	 * @return
	 */
	public MessageNotif createNotifDuelAnswerOK(Player sender, Player recipient, long duration, String tournamentID) {
		if (isNotifEnable()) {
			if (sender != null && recipient != null) {
                TextUIData notifTemplate = getTemplate(NOTIF_DUEL_ANSWER_OK);
				if (notifTemplate != null) {
					MessageNotif notif = new MessageNotif(notifTemplate.name);
					notif.recipientID = recipient.getID();
					notif.category = MESSAGE_CATEGORY_DUEL;
					notif.displayMask = MESSAGE_DISPLAY_TOP_BAR;
					notif.dateRead = notif.dateMsg;
					notif.dateExpiration = System.currentTimeMillis() + duration;
					notif.templateParameters.put("PARTNER_PSEUDO", sender.getNickname());
					notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_TOURNAMENT);
					notif.addExtraField(NOTIF_PARAM_TOURNAMENT_ID, tournamentID);
                    notif.addExtraField(NOTIF_PARAM_TOURNAMENT_CATEGORY_ID, "" + Constantes.TOURNAMENT_CATEGORY_DUEL);
					if (persistNotif(notif)) {
						return notif;
					}
				} else {
					log.error("Notif template not found for name="+NOTIF_DUEL_ANSWER_OK);
				}
			} else {
				log.error("Parameters not valid ... sender="+ sender + " - recipient=" + recipient);
			}
		}
		return null;
	}
	
	/**
     * Create notif for duel finish with all deal played. Visuel C or A (flag displayTopBar) - Expiration=3j
     * @param duelTournamentPlayer
     * @param recipient
     * @param displayTopBar if true display top bar (VisualC)
     * @param notifRead
     * @return
     */
    public MessageNotif createNotifDuelFinishPlayAll(DuelTournamentPlayer duelTournamentPlayer, Player recipient, boolean displayTopBar, boolean notifRead) {
        if (isNotifEnable()) {
            if (duelTournamentPlayer != null && recipient != null) {
                TextUIData notifTemplate = null;
                long playerWinner = duelTournamentPlayer.getWinner();
                if (playerWinner == recipient.getID()) {
                    notifTemplate = getTemplate(NOTIF_DUEL_FINISH_PLAYALL_WIN);
                } else if (playerWinner == 0) {
                    notifTemplate = getTemplate(NOTIF_DUEL_FINISH_PLAYALL_DRAW);
                } else {
                    notifTemplate = getTemplate(NOTIF_DUEL_FINISH_PLAYALL_LOST);
                }
                PlayerCache partner = playerCacheMgr.getPlayerCache(duelTournamentPlayer.getPartner(recipient.getID()));
                if (partner != null) {
                    if (notifTemplate != null) {
                        MessageNotif notif = new MessageNotif(notifTemplate.name);
                        notif.recipientID = recipient.getID();
                        notif.category = MESSAGE_CATEGORY_DUEL;
                        if (displayTopBar) {
                            notif.displayMask = MESSAGE_DISPLAY_TOP_BAR;
                        }
                        if (notifRead) {
                            notif.dateRead = notif.dateMsg;
                        }
                        notif.dateExpiration = System.currentTimeMillis() + 3*DURATION_1J;
                        notif.templateParameters.put("PARTNER_PSEUDO", partner.getPseudo());
                        String strScore = ""+(int)duelTournamentPlayer.getResultWinner()+"-"+(int)duelTournamentPlayer.getResultLoser();
                        notif.templateParameters.put("DUEL_SCORE", strScore);
						notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_TOURNAMENT);
						notif.addExtraField(NOTIF_PARAM_TOURNAMENT_ID, duelTournamentPlayer.getTournament().getIDStr());
						notif.addExtraField(NOTIF_PARAM_TOURNAMENT_CATEGORY_ID, "" + Constantes.TOURNAMENT_CATEGORY_DUEL);
                        if (persistNotif(notif)) {
                            return notif;
                        }
                    } else {
                        log.error("Notif template is null - playerWinner="+playerWinner+" - recipient="+recipient+" - duelTournamentPlayer="+duelTournamentPlayer);
                    }
                } else {
                    log.error("Partner is null for player="+recipient+" - duelTournamentPlayer="+duelTournamentPlayer);
                }
            } else {
                log.error("Parameters not valid ... duelTournamentPlayer="+duelTournamentPlayer+" - recipient="+recipient);
            }
        }
        return null;
    }
	
	/**
     * Create notif for duel finish with close because duel expired. Visuel A - Expiration=3j
     * @param duel
     * @param recipient
     * @return
     */
    public MessageNotif createNotifDuelFinishClose(DuelTournamentPlayer duel, long recipient) {
        if (isNotifEnable()) {
            if (duel != null) {
                TextUIData notifTemplate = null;
                long playerWinner = duel.getWinner();
                if (playerWinner == recipient) {
                    notifTemplate = getTemplate(NOTIF_DUEL_FINISH_CLOSE_WIN);
                } else if (playerWinner == 0) {
                    notifTemplate = getTemplate(NOTIF_DUEL_FINISH_CLOSE_DRAW);
                } else {
                    notifTemplate = getTemplate(NOTIF_DUEL_FINISH_CLOSE_LOST);
                }
                PlayerCache partner = playerCacheMgr.getPlayerCache(duel.getPartner(recipient));
                if (partner != null) {
                    if (notifTemplate != null) {
                        MessageNotif notif = new MessageNotif(notifTemplate.name);
                        notif.recipientID = recipient;
                        notif.category = MESSAGE_CATEGORY_DUEL;
                        notif.dateExpiration = System.currentTimeMillis() + 3*DURATION_1J;
                        notif.templateParameters.put("PARTNER_PSEUDO", partner.getPseudo());
                        String strScore = ""+(int)duel.getResultWinner()+"-"+(int)duel.getResultLoser();
                        notif.templateParameters.put("DUEL_SCORE", strScore);
						notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_TOURNAMENT);
						notif.addExtraField(NOTIF_PARAM_TOURNAMENT_ID, duel.getTournament().getIDStr());
						notif.addExtraField(NOTIF_PARAM_TOURNAMENT_CATEGORY_ID, "" + Constantes.TOURNAMENT_CATEGORY_DUEL);
                        if (persistNotif(notif)) {
                            return notif;
                        }
                    } else {
                        log.error("Notif template is null - playerWinner="+playerWinner+" - recipient="+recipient+" - duel="+duel);
                    }
                } else {
                    log.error("Partner is null for player="+recipient+" - duel="+duel);
                }
            } else {
                log.error("Parameters not valid ... duel="+duel+" - recipient="+recipient);
            }
        }
        return null;
    }
	
	/**
	 * Create notif for duel partner has played all deals (and not the other player).  Visuel H - Expiration=date fin duel
	 * @param partner
	 * @param recipient
     * @param duration
	 * @return
	 */
	public MessageNotif createNotifDuelPartnerPlayAll(Player partner, Player recipient, long duration, String tournamentID) {
		if (isNotifEnable()) {
			if (partner != null && recipient != null) {
				TextUIData notifTemplate = getTemplate(NOTIF_DUEL_PARTNER_PLAY_ALL);
				if (notifTemplate != null) {
					MessageNotif notif = new MessageNotif(notifTemplate.name);
					notif.recipientID = recipient.getID();
					notif.category = MESSAGE_CATEGORY_DUEL;
					notif.displayMask = MESSAGE_DISPLAY_TOP_BAR;
					notif.dateRead = notif.dateMsg;
					notif.dateExpiration = System.currentTimeMillis() + duration;
					notif.templateParameters.put("PARTNER_PSEUDO", partner.getNickname());
					notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_TOURNAMENT);
					notif.addExtraField(NOTIF_PARAM_TOURNAMENT_ID, tournamentID);
					notif.addExtraField(NOTIF_PARAM_TOURNAMENT_CATEGORY_ID, "" + Constantes.TOURNAMENT_CATEGORY_DUEL);
					if (persistNotif(notif)) {
						return notif;
					}
				} else {
					log.error("Notif template not found for name="+NOTIF_DUEL_PARTNER_PLAY_ALL);
				}
			} else {
				log.error("Parameters not valid ... partner="+partner+" - recipient="+recipient);
			}
		}
		return null;
	}
	
	/**
	 * Create notif for duel reminder.  Visuel H - Expiration=date fin duel
	 * @param partner
	 * @param recipient
     * @param duration
	 * @return
	 */
	public MessageNotif createNotifDuelReminder(Player partner, Player recipient, long duration, String tournamentID) {
		if (isNotifEnable()) {
			if (partner != null && recipient != null) {
				TextUIData notifTemplate = getTemplate(NOTIF_DUEL_REMINDER);
				if (notifTemplate != null) {
					MessageNotif notif = new MessageNotif(notifTemplate.name);
					notif.recipientID = recipient.getID();
					notif.category = MESSAGE_CATEGORY_DUEL;
					notif.displayMask = MESSAGE_DISPLAY_TOP_BAR;
					notif.dateExpiration = System.currentTimeMillis() + duration;
					notif.templateParameters.put("PARTNER_PSEUDO", partner.getNickname());
					notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_TOURNAMENT);
					notif.addExtraField(NOTIF_PARAM_TOURNAMENT_ID, tournamentID);
					notif.addExtraField(NOTIF_PARAM_TOURNAMENT_CATEGORY_ID, "" + Constantes.TOURNAMENT_CATEGORY_DUEL);
					if (persistNotif(notif)) {
						return notif;
					}
				} else {
					log.error("Notif template not found for name="+NOTIF_DUEL_REMINDER);
				}
			} else {
				log.error("Parameters not valid ... partner="+partner+" - recipient="+recipient);
			}
		}
		return null;
	}
	
	/**
	 * Create notif friend for request answer. Visuel C - Expiration=14j
	 * @param friend
	 * @param recipient
	 * @param answer true for OK
	 * @return
	 */
	public MessageNotif createNotifFriendRequestAnswer(Player friend, Player recipient, boolean answer) {
		if (isNotifEnable()) {
			if (friend != null && recipient != null) {
				TextUIData notifTemplate = null;
				if (answer) {
					notifTemplate = getTemplate(NOTIF_FRIEND_REQUEST_ANSWER_OK);
				} else {
					notifTemplate = getTemplate(NOTIF_FRIEND_REQUEST_ANSWER_KO);
				}
				if (notifTemplate != null) {
					MessageNotif notif = new MessageNotif(notifTemplate.name);
					notif.recipientID = recipient.getID();
					notif.category = MESSAGE_CATEGORY_FRIEND;
					notif.displayMask = MESSAGE_DISPLAY_TOP_BAR;
					notif.dateExpiration = System.currentTimeMillis() + DURATION_14J;
                    if (answer) {
                        notif.dateRead = notif.dateMsg;
                    }
					notif.templateParameters.put("FRIEND_PSEUDO", friend.getNickname());
					notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_PROFILE);
					notif.addExtraField(NOTIF_PARAM_PLAYER_ID, ""+friend.getID());
                    notif.addExtraField(NOTIF_PARAM_PSEUDO, friend.getNickname());
                    notif.addExtraField(NOTIF_PARAM_AVATAR, ""+friend.isAvatarPresent());
					if (persistNotif(notif)) {
						return notif;
					}
				} else {
					log.error("Notif template is null - friend="+friend+" - recipient="+recipient+" - answer="+answer);
				}
			} else {
				log.error("Parameters not valid ... friend="+friend+" - recipient="+recipient);
			}
		}
		return null;
	}
	
	public MessageNotif createNotifFriendDuelFinish(Player recipient, DuelTournamentPlayer duelTournamentPlayer) {
        if (duelTournamentPlayer != null) {
            PlayerCache player1 = playerCacheMgr.getPlayerCache(duelTournamentPlayer.getPlayer1ID());
            PlayerCache player2 = playerCacheMgr.getPlayerCache(duelTournamentPlayer.getPlayer2ID());
            if (player1 != null && player2 != null) {
                return createNotifFriendDuelFinish(recipient.getID(),
                        player1.getPseudo(),
                        (int) duelTournamentPlayer.getResultPlayer1(),
                        player2.getPseudo(),
                        (int) duelTournamentPlayer.getResultPlayer2());
            }
        }
        return null;
    }
	public MessageNotif createNotifFriendDuelFinish(long playerRecipient, String player1Pseudo, int player1Score, String player2Pseudo, int player2Score) {
		if (isNotifEnable()) {
            Map<String, String> mapExtra = new HashMap<String, String>();
			if (player1Score > player2Score) {
				mapExtra.put("FRIEND1_PSEUDO", player1Pseudo);
				mapExtra.put("FRIEND1_SCORE", "" + player1Score);
				mapExtra.put("FRIEND2_PSEUDO", player2Pseudo);
				mapExtra.put("FRIEND2_SCORE", "" + player2Score);
			} else {
				mapExtra.put("FRIEND1_PSEUDO", player2Pseudo);
				mapExtra.put("FRIEND1_SCORE", "" + player2Score);
				mapExtra.put("FRIEND2_PSEUDO", player1Pseudo);
				mapExtra.put("FRIEND2_SCORE", "" + player1Score);
			}

            if (FBConfiguration.getInstance().getIntValue("message.notifBuffer.friendDuelResult.enable", 0) == 1) {
                TextUIData notifTemplateBuffer = getTemplate(NOTIF_FRIEND_DUEL_RESULT_BUFFER);
                if (notifTemplateBuffer != null) {
                    MessageNotifBuffer notifBuffer = upsertNotifBuffer(playerRecipient,
                            "friendDuelResult",
                            FBConfiguration.getInstance().getIntValue("message.notifBuffer.friendDuelResult.nbUpdateMax", 10),
                            replaceTextVariables(notifTemplateBuffer.getText(Constantes.PLAYER_LANG_FR), mapExtra),
                            replaceTextVariables(notifTemplateBuffer.getText(Constantes.PLAYER_LANG_EN), mapExtra),
                            replaceTextVariables(notifTemplateBuffer.getText(Constantes.PLAYER_LANG_NL), mapExtra));
                    if (notifBuffer != null) {
                        // create notif with the content of the notif buffer
                        TextUIData notifTemplate = getTemplate(NOTIF_FRIEND_DUEL_RESULT);
                        if (notifTemplate != null) {
							String separator = FBConfiguration.getInstance().getStringValue("message.notifBuffer.separator", "\n");
                            MessageNotif notif = new MessageNotif(notifTemplate.name);
                            notif.recipientID = playerRecipient;
                            notif.category = MESSAGE_CATEGORY_FRIEND;
                            notif.dateExpiration = System.currentTimeMillis() + DURATION_14J;
                            notif.msgBodyFR += separator+notifBuffer.msgBodyFR;
                            notif.msgBodyEN += separator+notifBuffer.msgBodyEN;
                            notif.msgBodyNL += separator+notifBuffer.msgBodyNL;
                            if (persistNotif(notif)) {
                                return notif;
                            }
                        } else {
                            log.error("Notif template is null - template=" + NOTIF_FRIEND_DUEL_RESULT);
                        }
                    }
                } else {
                    log.error("Notif template is null - template=" + NOTIF_FRIEND_DUEL_RESULT_BUFFER);
                }
            } else {
                TextUIData notifTemplate = getTemplate(NOTIF_FRIEND_DUEL_FINISH);
                if (notifTemplate != null) {
                    MessageNotif notif = new MessageNotif(notifTemplate.name);
                    notif.recipientID = playerRecipient;
                    notif.category = MESSAGE_CATEGORY_FRIEND;
                    notif.dateExpiration = System.currentTimeMillis() + DURATION_14J;
                    replaceNotifTextVariables(notif, mapExtra);
                    if (persistNotif(notif)) {
                        return notif;
                    }
                } else {
                    log.error("Notif template is null - template=" + NOTIF_FRIEND_DUEL_FINISH);
                }
            }

		}
		return null;
	}

    /**
     * List the notif buffer with date creation before the current time - nbHours
     * @param nbHoursBefore
     * @param nbMax max list size (0 to have all data)
     * @param offset nb data to skip
     * @return
     */
    public List<MessageNotifBuffer> getListNotifBufferToTransform(int nbHoursBefore, int nbMax, int offset) {
        long limitTime = System.currentTimeMillis() - ((long)nbHoursBefore*60 * 60 * 1000);
        Criteria c = Criteria.where("dateCreation").lt(limitTime);
        Query q = new Query();
        q.addCriteria(c);
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        if (offset > 0){
            q.skip(offset);
        }
        q.with(new Sort(Sort.Direction.ASC, "dateCreation"));
        return mongoTemplate.find(Query.query(c), MessageNotifBuffer.class);
    }

    /**
     * Do the job to transform notif buffer to notif. Extract notif buffer with date > 24h and create the notif associated.
     */
    private void processTransformNotifBuffer() {
        tsLastNotifBufferTransformTask = System.currentTimeMillis();
        nbNotifLastTransformTask = 0;
        try {
            List<MessageNotifBuffer> listNotifBuffer = getListNotifBufferToTransform(FBConfiguration.getInstance().getIntValue("message.notifBuffer.taskDateLimit", 24), 0, 0);
            if (listNotifBuffer != null && listNotifBuffer.size() > 0) {
                List<MessageNotif> listNotif = new ArrayList<>();
                TextUIData notifTemplateFriendDuelResult = getTemplate(NOTIF_FRIEND_DUEL_RESULT);
				String separator = FBConfiguration.getInstance().getStringValue("message.notifBuffer.separator", "\n");
                if (notifTemplateFriendDuelResult != null) {
                    for (MessageNotifBuffer notifBuffer : listNotifBuffer) {
                        // create notif with the content of the notif buffer
                        if (notifBuffer.bufferType.equals("friendDuelResult")) {
                            MessageNotif notif = new MessageNotif(notifTemplateFriendDuelResult.name);
                            notif.recipientID = notifBuffer.recipientID;
                            notif.category = MESSAGE_CATEGORY_FRIEND;
                            notif.dateExpiration = System.currentTimeMillis() + DURATION_14J;
                            notif.msgBodyFR += separator+notifBuffer.msgBodyFR;
                            notif.msgBodyEN += separator+notifBuffer.msgBodyEN;
                            notif.msgBodyNL += separator+notifBuffer.msgBodyNL;
                            listNotif.add(notif);

                            // delete the buffer notif
                            mongoTemplate.remove(notifBuffer);
                        }
                    }
                    resultLastNotifBufferTransformTask = "Transform buffer to notif success for "+listNotif.size()+" elements";
                } else {
                    resultLastNotifBufferTransformTask = "No template found for "+NOTIF_FRIEND_DUEL_RESULT;
                    log.error("Notif template is null - template=" + NOTIF_FRIEND_DUEL_RESULT);
                }
                nbNotifLastTransformTask = listNotif.size();
                // create all the notif
                if (listNotif.size() > 0) {
                    long ts = System.currentTimeMillis();
                    mongoTemplate.insertAll(listNotif);
                    if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
                        log.error("Mongo insert too long ! ts=" + (System.currentTimeMillis() - ts + " - nb notif=" + listNotif.size()));
                    }
                }
            } else {
                resultLastNotifBufferTransformTask = "List notif buffer is empty";
            }
        } catch (Exception e) {
            log.error("Exception to transform notif to buffer ...", e);
            resultLastNotifBufferTransformTask = "Exception to transform notif to buffer ... "+e.getMessage();
        }
        log.warn("Process transform result="+resultLastNotifBufferTransformTask+" - nbNotifLastTransformTask="+nbNotifLastTransformTask);
    }

    /**
     * Insert or update (if already existing) a notif buffer. If the nbUpdateMax is reached, the notifBufer is removed from DB and returned.
     * @param recipientID
     * @param bufferType
     * @param nbUpdateMax
     * @return not null if the notif buffer is full (nbUpdateMax reached) => must be transform to notif
     */
    private MessageNotifBuffer upsertNotifBuffer(long recipientID, String bufferType, int nbUpdateMax, String textBodyFr, String textBodyEn, String textBodyNl) {
        synchronized (lockUpsertNotifBuffer.getLock(bufferType+"-"+recipientID)) {
            try {
                // find existing notif buffer for this recipient and bufferType
                Criteria c = Criteria.where("recipientID").is(recipientID).andOperator(Criteria.where("bufferType").is(bufferType));
                MessageNotifBuffer notifBuffer = mongoTemplate.findOne(Query.query(c), MessageNotifBuffer.class);
                if (notifBuffer == null) {
                    // no notif buffer existing => create a new one
                    notifBuffer = new MessageNotifBuffer();
                    notifBuffer.recipientID = recipientID;
                    notifBuffer.bufferType = bufferType;
                    notifBuffer.dateCreation = System.currentTimeMillis();
                    notifBuffer.nbUpdate = 1;
                    notifBuffer.msgBodyFR = textBodyFr;
                    notifBuffer.msgBodyEN = textBodyEn;
                    notifBuffer.msgBodyNL = textBodyNl;
                    long ts = System.currentTimeMillis();
                    mongoTemplate.insert(notifBuffer);
                    if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
                        log.error("Mongo insert too long ! ts="+(System.currentTimeMillis() - ts+" - notifBuffer="+notifBuffer));
                    }
                } else {
					String separator = FBConfiguration.getInstance().getStringValue("message.notifBuffer.separator", "\n");
                    // motif buffer already existing
                    notifBuffer.nbUpdate++;
                    notifBuffer.msgBodyFR+=separator+textBodyFr;
                    notifBuffer.msgBodyEN+=separator+textBodyEn;
                    notifBuffer.msgBodyNL+=separator+textBodyNl;
                    if (nbUpdateMax == 0 || notifBuffer.nbUpdate < nbUpdateMax) {
                        long ts = System.currentTimeMillis();
                        mongoTemplate.save(notifBuffer);
                        if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
                            log.error("Mongo save too long ! ts="+(System.currentTimeMillis() - ts+" - notifBuffer="+notifBuffer));
                        }
                    } else {
                        // remove this object. It will be sent by method caller
                        mongoTemplate.remove(notifBuffer);
                        return notifBuffer;
                    }
                }
            } catch (Exception e) {
                log.error("Exception to upsert notif buffer ... recipientID=" + recipientID + " - bufferType=" + bufferType, e);
            }
        }
        return null;
    }
	
	/**
	 * Create notif credit buy deal. Visuel A - Expiration=7j
	 * @param recipient
	 * @param nbDealBuy
	 * @return
	 */
	public MessageNotif createNotifCreditBuyDeal(Player recipient, int nbDealBuy) {
		if (isNotifEnable()) {
			if (recipient != null) {
				TextUIData notifTemplate = getTemplate(NOTIF_CREDIT_BUY_DEAL);
				if (notifTemplate != null) {
					MessageNotif notif = new MessageNotif(notifTemplate.name);
					notif.recipientID = recipient.getID();
					notif.category = MESSAGE_CATEGORY_GENERAL;
					notif.dateExpiration = System.currentTimeMillis() + DURATION_30J;
					notif.templateParameters.put("NB_DEAL_BUY", ""+nbDealBuy);
					if (persistNotif(notif)) {
						return notif;
					}
				} else {
					log.error("Notif template not found for name="+NOTIF_CREDIT_BUY_DEAL);
				}
			} else {
				log.error("Parameters not valid ... recipient="+recipient);
			}
		}
		return null;
	}

	/**
	 * Create notif credit buy TourFederation. Visuel A - Expiration=7j
	 * @param recipient
	 * @param credit
	 * @return
	 */
	public MessageNotif createNotifCreditBuyTourFederation(Player recipient, int credit, int productCategory) {
		if (isNotifEnable()) {
			if (recipient != null) {
				String federationName = FederationMgr.getFederationNameFromProductCategory(productCategory);
				TextUIData notifTemplate = getTemplate("creditBuyTour"+federationName);
				if (notifTemplate != null) {
					MessageNotif notif = new MessageNotif(notifTemplate.name);
					notif.recipientID = recipient.getID();
					notif.category = MESSAGE_CATEGORY_GENERAL;
					notif.dateExpiration = System.currentTimeMillis() + DURATION_30J;
					notif.templateParameters.put("CREDIT_TOUR_"+federationName, ""+ credit);
					if (persistNotif(notif)) {
						return notif;
					}
				} else {
					log.error("Notif template not found for name=creditBuyTour"+federationName);
				}
			} else {
				log.error("Parameters not valid ... recipient="+recipient);
			}
		}
		return null;
	}
	
	/**
	 * Create notif credit buy subscription. Visuel A - Expiration=7j
	 * @param recipient
	 * @return
	 */
	public MessageNotif createNotifCreditBuySubscription(Player recipient) {
		if (isNotifEnable()) {
			if (recipient != null) {
				TextUIData notifTemplate = getTemplate(NOTIF_CREDIT_BUY_SUBSCRIPTION);
				if (notifTemplate != null) {
					MessageNotif notif = new MessageNotif(notifTemplate.name);
					notif.recipientID = recipient.getID();
					notif.category = MESSAGE_CATEGORY_GENERAL;
					notif.dateExpiration = System.currentTimeMillis() + DURATION_30J;
					notif.templateParameters.put("DATE_END_SUBSCRIPTION", VARIABLE_TIMESTAMP_FORMAT_DATE + recipient.getSubscriptionExpirationDate() + VARIABLE_END);
					if (persistNotif(notif)) {
						return notif;
					}
				} else {
					log.error("Notif template not found for name="+NOTIF_CREDIT_BUY_SUBSCRIPTION);
				}
			} else {
				log.error("Parameters not valid ... recipient="+recipient);
			}
		}
		return null;
	}

	/**
	 * Create event to indicate friend status connection
	 * @param recipient
	 * @param friend
	 * @param connected
	 * @return
	 */
	public Event buildEventFriendConnected(Player recipient, Player friend, boolean connected) {
		if (recipient != null && friend != null) {
			Event evt = new Event();
			evt.timestamp = System.currentTimeMillis();
			evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
			evt.receiverID = recipient.getID();
			evt.addFieldCategory(Constantes.EVENT_CATEGORY_PLAYER);
			// create change link data
			EventFriendConnected efd = new EventFriendConnected();
			efd.playerID = friend.getID();
			efd.connected = connected;
			if (connected) {
				TextUIData notifTemplate = getTemplate(NOTIF_FRIEND_EVENT_CONNECTED);
				if (notifTemplate != null) {
					Map<String, String> mapExtra = new HashMap<String, String>();
					mapExtra.put("FRIEND_PSEUDO", friend.getNickname());
					efd.msg = notifTemplate.getText(recipient.getDisplayLang());
					efd.msg = replaceTextVariables(efd.msg, mapExtra);
				} else {
					log.error("No template found for name="+NOTIF_FRIEND_EVENT_CONNECTED);
				}
			}
			// set data JSON in event
			try {
				evt.addFieldType(Constantes.EVENT_TYPE_PLAYER_FRIEND_CONNECTED, jsontools.transform2String(efd, false));
				return evt;
			} catch (JsonGenerationException e) {
				log.error("JsonGenerationException to transform data="+efd,e);
			} catch (JsonMappingException e) {
				log.error("JsonMappingException to transform data="+efd,e);
			} catch (IOException e) {
				log.error("IOException to transform data=" + efd, e);
            }
        } else {
            log.error("Parameters not valid - recipient=" + recipient + " - friend=" + friend);
		}
		return null;
	}
	
	/**
	 * Method to create notif and set all fields
     * @param notifDate
	 * @param recipientID
	 * @param category
	 * @param displayMask
	 * @param dateExpiration
	 * @param templateName
	 * @param templateParameters
	 * @param msgEn
	 * @param msgFr
	 * @param msgNl
	 * @param fieldName
	 * @param fieldValue
	 * @param titleFR
	 * @param titleEN
	 * @param titleIcon
	 * @param titleBackgroundColor
	 * @param titleColor
	 * @param richBodyFR
	 * @param richBodyEN
	 * @param actionButtonTextFR
	 * @param actionButtonTextEN
	 * @return
	 */
	public MessageNotif createNotif(long notifDate, long recipientID, int category, int displayMask, long dateExpiration,
									String templateName, Map<String, String> templateParameters,
									String msgEn, String msgFr, String msgNl, String fieldName, String fieldValue,
									String titleFR, String titleEN, String titleIcon, String titleBackgroundColor, String titleColor,
									String richBodyFR, String richBodyEN, String actionButtonTextFR, String actionButtonTextEN,
									String actionTextFR, String actionTextEN) {
		if (isNotifEnable()) {
			MessageNotif notif = new MessageNotif();
			notif.recipientID = recipientID;
			notif.category = category;
			notif.displayMask = displayMask;
			if (notifDate != 0) {
                notif.setDateMsg(notifDate);
            }
			notif.dateExpiration = dateExpiration;
			notif.msgBodyEN = msgEn;
			notif.msgBodyFR = msgFr;
			notif.msgBodyNL = msgNl;
			if (templateName != null && templateName.length() > 0) {
				notif.templateName = templateName;
			}
			if (templateParameters != null) {
				notif.templateParameters = templateParameters;
			}
			notif.fieldName = fieldName;
			notif.fieldValue = fieldValue;
			notif.titleFR = titleFR;
			notif.titleEN = titleEN;
			notif.titleIcon = titleIcon;
			notif.titleBackgroundColor = titleBackgroundColor;
			notif.titleColor = titleColor;
			notif.richBodyFR = richBodyFR;
			notif.richBodyEN = richBodyEN;
			notif.actionButtonTextFR = actionButtonTextFR;
			notif.actionButtonTextEN = actionButtonTextEN;
			notif.actionTextFR = actionTextFR;
			notif.actionTextEN = actionTextEN;
			if (persistNotif(notif)) {
				return notif;
			}
		} else {
            if (log.isDebugEnabled()) {
                log.debug("Notification is disabled !");
            }
		}
		return null;
	}
	
	/**
	 * Create a notif for all player
	 * @param category
	 * @param displayMask
	 * @param dateExpiration
	 * @param templateName
	 * @param templateParameters
	 * @param msgEn
	 * @param msgFr
	 * @param msgNl
	 * @param fieldName
	 * @param fieldValue
	 * @param titleFR
	 * @param titleEN
	 * @param titleIcon
	 * @param titleBackgroundColor
	 * @param titleColor
	 * @param richBodyFR
	 * @param richBodyEN
	 * @param actionButtonTextFR
	 * @param actionButtonTextEN
	 * @return
	 */
	public MessageNotifAll createNotifAll(int category, int displayMask, long dateExpiration,
										  String templateName, Map<String, String> templateParameters,
										  String msgEn, String msgFr, String msgNl, String fieldName, String fieldValue,
										  String titleFR, String titleEN, String titleIcon, String titleBackgroundColor, String titleColor,
										  String richBodyFR, String richBodyEN, String actionButtonTextFR, String actionButtonTextEN,
										  String actionTextFR, String actionTextEN) {
		if (isNotifEnable()) {
			MessageNotifAll notif = new MessageNotifAll();
			notif.category = category;
			notif.displayMask = displayMask;
			notif.dateExpiration = dateExpiration;
			notif.msgBodyEN = msgEn;
			notif.msgBodyFR = msgFr;
			notif.msgBodyNL = msgNl;
			if (templateName != null && templateName.length() > 0) {
				notif.templateName = templateName;
			}
			if (templateParameters != null) {
				notif.templateParameters = templateParameters;
			}
			notif.fieldName = fieldName;
			notif.fieldValue = fieldValue;
			notif.titleFR = titleFR;
			notif.titleEN = titleEN;
			notif.titleIcon = titleIcon;
			notif.titleBackgroundColor = titleBackgroundColor;
			notif.titleColor = titleColor;
			notif.richBodyFR = richBodyFR;
			notif.richBodyEN = richBodyEN;
			notif.actionButtonTextFR = actionButtonTextFR;
			notif.actionButtonTextEN = actionButtonTextEN;
			notif.actionTextFR = actionTextFR;
			notif.actionTextEN = actionTextEN;
			if (persistNotif(notif)) {
				return notif;
			}
		}
		return null;
	}
	
	/**
	 * Create a notif for a group of player
	 * @param category
	 * @param displayMask
     * @param dateNotif
	 * @param dateExpiration
	 * @param templateName
	 * @param templateParameters
	 * @param msgEn
	 * @param msgFr
	 * @param msgNl
	 * @param fieldName
	 * @param fieldValue
	 * @param titleFR
	 * @param titleEN
	 * @param titleIcon
	 * @param titleBackgroundColor
	 * @param titleColor
	 * @param richBodyFR
	 * @param richBodyEN
	 * @param actionButtonTextFR
	 * @param actionButtonTextEN
	 * @return
	 */
	public MessageNotifGroup createNotifGroup(List<Long> listPlayerID, int category, int displayMask, long dateNotif, long dateExpiration,
											  String templateName, Map<String, String> templateParameters,
											  String msgEn, String msgFr, String msgNl, String fieldName, String fieldValue,
											  String titleFR, String titleEN, String titleIcon, String titleBackgroundColor, String titleColor,
											  String richBodyFR, String richBodyEN, String actionButtonTextFR, String actionButtonTextEN,
											  String actionTextFR, String actionTextEN) {
		if (isNotifEnable()) {
			MessageNotifGroup notif = new MessageNotifGroup();
			notif.category = category;
			notif.displayMask = displayMask;
			if (dateNotif > 0) {
			    notif.setDateMsg(dateNotif);
            }
			notif.dateExpiration = dateExpiration;
			notif.msgBodyEN = msgEn;
			notif.msgBodyFR = msgFr;
			notif.msgBodyNL = msgNl;
			if (templateName != null && templateName.length() > 0) {
				notif.templateName = templateName;
			}
			if (templateParameters != null) {
				notif.templateParameters = templateParameters;
			}
			notif.fieldName = fieldName;
			notif.fieldValue = fieldValue;
			notif.titleFR = titleFR;
			notif.titleEN = titleEN;
			notif.titleIcon = titleIcon;
			notif.titleBackgroundColor = titleBackgroundColor;
			notif.titleColor = titleColor;
			notif.richBodyFR = richBodyFR;
			notif.richBodyEN = richBodyEN;
			notif.actionButtonTextFR = actionButtonTextFR;
			notif.actionButtonTextEN = actionButtonTextEN;
			notif.actionTextFR = actionTextFR;
			notif.actionTextEN = actionTextEN;
			if (persistNotif(notif)) {
                if (listPlayerID != null && listPlayerID.size() > 0) {
                    // add list of MessageNotifGroupRead for players
                    List<MessageNotifGroupRead> listNotifGroupRead = new ArrayList<MessageNotifGroupRead>();
                    for (Long l : listPlayerID) {
                        MessageNotifGroupRead notifGroupRead = new MessageNotifGroupRead();
                        notifGroupRead.playerID = l;
                        notifGroupRead.dateNotifExpiration = notif.dateExpiration;
                        notifGroupRead.notifGroup = notif;
                        listNotifGroupRead.add(notifGroupRead);
                    }
                    try {
                        mongoTemplate.insert(listNotifGroupRead, MessageNotifGroupRead.class);
                    } catch (Exception e) {
                        log.error("Failed to insert listNotifGroup", e);
                    }
                }
				return notif;
			}
		}
		return null;
	}
	
	/**
	 * Set recipients for notif group
	 * ?????????
	 * @param notif
	 * @param listPlayerID
	 * @return
	 */
	public boolean setNotifGroupForPlayer(MessageNotifGroup notif, List<Long> listPlayerID) {
		if (isNotifEnable()) {
			if (notif != null && listPlayerID != null && listPlayerID.size() > 0) {
				List<MessageNotifGroupRead> listNotifGroupRead = new ArrayList<MessageNotifGroupRead>();
				for (Long l : listPlayerID) {
					MessageNotifGroupRead notifGroupRead = new MessageNotifGroupRead();
					notifGroupRead.playerID = l;
					notifGroupRead.dateNotifExpiration = notif.dateExpiration;
					notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);
					notifGroupRead.notifGroup = notif;
					listNotifGroupRead.add(notifGroupRead);
				}
				try {
					mongoTemplate.insert(listNotifGroupRead, MessageNotifGroupRead.class);
                    return true;
				} catch (Exception e) {
					log.error("Failed to insert listNotifGroup", e);
				}
			} else {
				log.error("parameters not valid : notif="+notif+" - listPlayerID="+listPlayerID);
			}
		}
		return false;
	}
	
	/**
	 * Create notif message support. Visuel A - Expiration=30j
	 * @param playerID
	 * @param message
	 * @return
	 */
	public MessageNotif createNotifMessageSupport(long playerID, String message) {
		if (isNotifEnable()) {
            MessageNotif notif = new MessageNotif();
            notif.msgBodyEN = message;
            notif.msgBodyFR = message;
            notif.msgBodyNL = message;
            notif.recipientID = playerID;
            notif.category = MESSAGE_CATEGORY_GENERAL;
            notif.dateExpiration = System.currentTimeMillis() + DURATION_30J;
            if (persistNotif(notif)) {
                return notif;
            }
		}
        return null;
	}

	/**
	 * Create notif serie move up
	 * @param serieNext
	 * @param seriePrevious
	 * @param ranking
	 * @param nbPlayers
	 * @param podiumTemplateParameters
	 * @param dateExpiration
	 * @return
	 */
	public MessageNotif createNotifSerieNewUp(Long playerID, String serieNext, String seriePrevious, TourSerieMemPeriodRankingPlayer ranking, int nbPlayers,
											  Map<String, String> podiumTemplateParameters, long dateExpiration) {
		if (isNotifEnable()) {
			String templateName;
			if (serieNext.equals(TourSerieMgr.SERIE_TOP)) {
				templateName = NOTIF_SERIE_UP_ELITE;
			} else {
				templateName = NOTIF_SERIE_UP;
			}
			TextUIData notifTemplate = getTemplate(templateName);
			if (notifTemplate != null) {
				MessageNotif notif = new MessageNotif(notifTemplate.name);
				notif.recipientID = playerID;
				notif.category = MESSAGE_CATEGORY_SERIE;
				notif.dateExpiration = dateExpiration;
				notif.templateParameters.putAll(podiumTemplateParameters);
				notif.templateParameters.put("SERIE", VARIABLE_SERIE_START+seriePrevious+VARIABLE_SERIE_END);
				notif.templateParameters.put("SERIE_UP", VARIABLE_SERIE_START+serieNext+VARIABLE_SERIE_END);
				notif.templateParameters.put("RANK_PLAYER", VARIABLE_NUMBER_START + ranking.rank +VARIABLE_END);
				notif.templateParameters.put("NB_PLAYERS", VARIABLE_NUMBER_START + nbPlayers +VARIABLE_END);
				notif.templateParameters.put("RESULT", String.format(Locale.ENGLISH, "%.2f", ranking.result * 100));
				notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_SERIE_PREVIOUS_RANKING);
				notif.addExtraField(NOTIF_PARAM_PREVIOUS_SERIE, seriePrevious);
				notif.addExtraField(NOTIF_PARAM_SERIE, serieNext);
				if (persistNotif(notif)) {
					return notif;
				}
			} else {
				log.error("Notif template not found for name="+ templateName);
			}
		}
		return null;
	}

	/**
	 * Create notif serie down
	 * @param serieNext
	 * @param seriePrevious
	 * @param ranking
	 * @param nbPlayers
	 * @param podiumTemplateParameters
	 * @param dateExpiration
	 * @return
	 */
	public MessageNotif createNotifSerieNewDown(Long playerID, String serieNext, String seriePrevious, TourSerieMemPeriodRankingPlayer ranking, int nbPlayers,
												Map<String, String> podiumTemplateParameters, long dateExpiration) {
		if (isNotifEnable()) {
			TextUIData notifTemplate = getTemplate(NOTIF_SERIE_DOWN);
			if (notifTemplate != null) {
				MessageNotif notif = new MessageNotif(notifTemplate.name);
				notif.recipientID = playerID;
				notif.category = MESSAGE_CATEGORY_SERIE;
				notif.dateExpiration = dateExpiration;
				notif.templateParameters.putAll(podiumTemplateParameters);
				notif.templateParameters.put("SERIE", VARIABLE_SERIE_START+seriePrevious+VARIABLE_SERIE_END);
				notif.templateParameters.put("SERIE_DOWN", VARIABLE_SERIE_START+serieNext+VARIABLE_SERIE_END);
				notif.templateParameters.put("RANK_PLAYER", VARIABLE_NUMBER_START + ranking.rank +VARIABLE_END);
				notif.templateParameters.put("NB_PLAYERS", VARIABLE_NUMBER_START + nbPlayers +VARIABLE_END);
				notif.templateParameters.put("RESULT", String.format(Locale.ENGLISH, "%.2f", ranking.result * 100));
				notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_SERIE_PREVIOUS_RANKING);
				notif.addExtraField(NOTIF_PARAM_PREVIOUS_SERIE, seriePrevious);
				notif.addExtraField(NOTIF_PARAM_SERIE, serieNext);
				if (persistNotif(notif)) {
					return notif;
				}
			} else {
				log.error("Notif template not found for name="+ NOTIF_SERIE_DOWN);
			}
		}
		return null;
	}

	/**
	 * Create notif serie maintain
	 * @param serie
	 * @param ranking
	 * @param nbPlayers
	 * @param podiumTemplateParameters
	 * @param dateExpiration
	 * @return
	 */
	public MessageNotif createNotifSerieNewMaintain(Long playerID, String serie, TourSerieMemPeriodRankingPlayer ranking, int nbPlayers,
															  Map<String, String> podiumTemplateParameters, long dateExpiration) {
		if (isNotifEnable()) {
			String templateName;
			if (serie.equals(TourSerieMgr.SERIE_11)) {
				templateName = NOTIF_SERIE_MAINTAIN_11;
			} else if (serie.equals(TourSerieMgr.SERIE_TOP) && ranking.rank <= 10) {
				templateName = NOTIF_SERIE_MAINTAIN_TOP_10;
			} else if (serie.equals(TourSerieMgr.SERIE_TOP)) {
				templateName = NOTIF_SERIE_MAINTAIN_ELITE;
			} else {
				templateName = NOTIF_SERIE_MAINTAIN;
			}
			TextUIData notifTemplate = getTemplate(templateName);
			if (notifTemplate != null) {
				MessageNotif notif = new MessageNotif(notifTemplate.name);
				notif.recipientID = playerID;
				notif.category = MESSAGE_CATEGORY_SERIE;
				notif.dateExpiration = dateExpiration;
				notif.templateParameters.putAll(podiumTemplateParameters);
				notif.templateParameters.put("SERIE", VARIABLE_SERIE_START+serie+VARIABLE_SERIE_END);
				notif.templateParameters.put("RANK_PLAYER", VARIABLE_NUMBER_START + ranking.rank +VARIABLE_END);
				notif.templateParameters.put("NB_PLAYERS", VARIABLE_NUMBER_START + nbPlayers +VARIABLE_END);
				notif.templateParameters.put("RESULT", String.format(Locale.ENGLISH, "%.2f", ranking.result * 100));
				notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_SERIE_PREVIOUS_RANKING);
				notif.addExtraField(NOTIF_PARAM_PREVIOUS_SERIE, serie);
				notif.addExtraField(NOTIF_PARAM_SERIE, serie);
				if (persistNotif(notif)) {
					return notif;
				}
			} else {
				log.error("Notif template not found for name="+templateName);
			}
		}
		return null;
	}

	/**
	 * Create notif serie move up from NEW
	 * @param serieNext
	 * @param seriePrevious
	 * @param ranking
	 * @param nbPlayers
	 * @param podiumTemplateParameters
	 * @param dateExpiration
	 * @return
	 */
	public MessageNotif createNotifSerieNewUpNew(Long playerID, String serieNext, String seriePrevious, TourSerieMemPeriodRankingPlayer ranking, int nbPlayers,
												 Map<String, String> podiumTemplateParameters, long dateExpiration) {
		if (isNotifEnable()) {
			TextUIData notifTemplate = getTemplate(NOTIF_SERIE_UP_NEW);
			if (notifTemplate != null) {
				MessageNotif notif = new MessageNotif(notifTemplate.name);
				notif.recipientID = playerID;
				notif.category = MESSAGE_CATEGORY_SERIE;
				notif.dateExpiration = dateExpiration;
				notif.templateParameters.putAll(podiumTemplateParameters);
				notif.templateParameters.put("SERIE_UP", VARIABLE_SERIE_START+serieNext+VARIABLE_SERIE_END);
				notif.templateParameters.put("RANK_PLAYER", VARIABLE_NUMBER_START + ranking.rank +VARIABLE_END);
				notif.templateParameters.put("NB_PLAYERS", VARIABLE_NUMBER_START + nbPlayers +VARIABLE_END);
				notif.templateParameters.put("RESULT", String.format(Locale.ENGLISH, "%.2f", ranking.result * 100));
				notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_SERIE_PREVIOUS_RANKING);
				notif.addExtraField(NOTIF_PARAM_PREVIOUS_SERIE, seriePrevious);
				notif.addExtraField(NOTIF_PARAM_SERIE, serieNext);
				if (persistNotif(notif)) {
					return notif;
				}
			} else {
				log.error("Notif template not found for name="+ NOTIF_SERIE_UP_NEW);
			}
		}
		return null;
	}

	/**
	 * Create notif serie update best result in serie
	 * @param dateExpiration
	 * @return
	 */
	public MessageNotif createNotifSerieNewUpdateBestResult(long playerID, long dateExpiration) {
		if (isNotifEnable()) {
			TextUIData notifTemplate = getTemplate(NOTIF_SERIE_NEW_UPDATE_BEST_RESULT);
			if (notifTemplate != null) {
				MessageNotif notif = new MessageNotif(notifTemplate.name);
				notif.category = MESSAGE_CATEGORY_SERIE;
				notif.dateExpiration = dateExpiration;
				notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_PROFILE);
				notif.addExtraField(NOTIF_PARAM_PLAYER_ID, String.valueOf(playerID));
				notif.addExtraField(NOTIF_PARAM_TEMPLATE, NOTIF_SERIE_NEW_UPDATE_BEST_RESULT);
				if (persistNotif(notif)) {
					return notif;
				}
			} else {
				log.error("Notif template not found for name="+NOTIF_SERIE_NEW_UPDATE_BEST_RESULT);
			}
		}
		return null;
	}

    /**
     * Create notif group serie move up
     * @param serieNext
     * @param seriePrevious
     * @param dateExpiration
     * @return
     */
    public MessageNotifGroup createNotifGroupSerieNewUpNew(String serieNext, String seriePrevious, long dateExpiration) {
        if (isNotifEnable()) {
            TextUIData notifTemplate = getTemplate(NOTIF_SERIE_UP_NEW);
            if (notifTemplate != null) {
                MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
                notif.category = MESSAGE_CATEGORY_SERIE;
                notif.dateExpiration = dateExpiration;
				notif.templateParameters.put("SERIE_NEXT", VARIABLE_SERIE_START+serieNext+VARIABLE_SERIE_END);
                notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_SERIE_PREVIOUS_RANKING);
                notif.addExtraField(NOTIF_PARAM_PREVIOUS_SERIE, seriePrevious);
                notif.addExtraField(NOTIF_PARAM_SERIE, serieNext);
                if (persistNotif(notif)) {
                    return notif;
                }
            } else {
                log.error("Notif template not found for name="+ NOTIF_SERIE_UP_NEW);
            }
        }
        return null;
    }

    /**
     * Create notif group serie maintain
     * @param thresholdNbTourUp
     * @param dateExpiration
     * @return
     */
    public MessageNotifGroup createNotifGroupSerieNewMaintainNew(int thresholdNbTourUp, long dateExpiration) {
        if (isNotifEnable()) {
            TextUIData notifTemplate = getTemplate(NOTIF_SERIE_NEW_MAINTAIN_NEW);
            if (notifTemplate != null) {
                MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
                notif.category = MESSAGE_CATEGORY_SERIE;
                notif.dateExpiration = dateExpiration;
				notif.templateParameters.put("SERIE_THRESHOLD_NB_TOUR_UP", ""+thresholdNbTourUp);
                notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_SERIE_PREVIOUS_RANKING);
                notif.addExtraField(NOTIF_PARAM_PREVIOUS_SERIE, TourSerieMgr.SERIE_NC);
                notif.addExtraField(NOTIF_PARAM_SERIE, TourSerieMgr.SERIE_NC);
                if (persistNotif(notif)) {
                    return notif;
                }
            } else {
                log.error("Notif template not found for name="+ NOTIF_SERIE_NEW_MAINTAIN_NEW);
            }
        }
        return null;
    }

    /**
     * Create notif group serie down (no play)
     * @param serieNext (transform to reserve)
     * @param seriePrevious
     * @param dateExpiration
     * @return
     */
    public MessageNotifGroup createNotifGroupSerieNewDownNoPlay(String serieNext, String seriePrevious, long dateExpiration) {
        if (isNotifEnable()) {
            TextUIData notifTemplate = getTemplate(NOTIF_SERIE_NEW_DOWN_NO_PLAY);
            if (notifTemplate != null) {
                MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
                notif.category = MESSAGE_CATEGORY_SERIE;
                notif.dateExpiration = dateExpiration;
				notif.templateParameters.put("SERIE_NEXT", VARIABLE_SERIE_START+serieNext+VARIABLE_SERIE_END);
                notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_SERIE_PREVIOUS_RANKING);
                notif.addExtraField(NOTIF_PARAM_PREVIOUS_SERIE, seriePrevious);
                notif.addExtraField(NOTIF_PARAM_SERIE, TourSerieMgr.buildSerieReserve(serieNext)); // serieNext is reserve !
                if (persistNotif(notif)) {
                    return notif;
                }
            } else {
                log.error("Notif template not found for name="+NOTIF_SERIE_NEW_DOWN_NO_PLAY);
            }
        }
        return null;
    }

    /**
     * Create notif group serie maintain (no play)
     * @param currentSerie
     * @param serieDown
     * @param dateExpiration
     * @return
     */
    public MessageNotifGroup createNotifGroupSerieNewMaintainNoPlay(String currentSerie, String serieDown, long dateExpiration) {
        if (isNotifEnable()) {
			String templateName;
			if (currentSerie.equals(TourSerieMgr.SERIE_11)) {
				templateName = NOTIF_SERIE_NEW_MAINTAIN_NO_PLAY_11;
			} else if (currentSerie.equals(TourSerieMgr.SERIE_TOP)) {
				templateName = NOTIF_SERIE_NEW_MAINTAIN_NO_PLAY_ELITE;
			} else {
				templateName = NOTIF_SERIE_NEW_MAINTAIN_NO_PLAY;
			}
			TextUIData notifTemplate = getTemplate(templateName);
            if (notifTemplate != null) {
                MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
                notif.category = MESSAGE_CATEGORY_SERIE;
                notif.dateExpiration = dateExpiration;
				notif.templateParameters.put("SERIE_NEXT", VARIABLE_SERIE_START+ currentSerie +VARIABLE_SERIE_END);
				notif.templateParameters.put("SERIE_DOWN", VARIABLE_SERIE_START+serieDown+VARIABLE_SERIE_END);
                notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_SERIE_PREVIOUS_RANKING);
                notif.addExtraField(NOTIF_PARAM_PREVIOUS_SERIE, currentSerie);
                notif.addExtraField(NOTIF_PARAM_SERIE, currentSerie);
                if (persistNotif(notif)) {
                    return notif;
                }
            } else {
                log.error("Notif template not found for name="+templateName);
            }
        }
        return null;
    }

    /**
     * Create notif for player in serie New with no play during 1 period
     * @param dateExpiration
     * @param thresholdNbTourUp
     * @param currentPeriod
     * @return
     */
    public MessageNotifGroup createNotifGroupSerieNewNewNoPlay(long dateExpiration, int thresholdNbTourUp, String currentPeriod) {
        if (isNotifEnable()) {
            TextUIData notifTemplate = getTemplate(NOTIF_SERIE_NEW_NEW_NO_PLAY);
            if (notifTemplate != null) {
                MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
                notif.category = MESSAGE_CATEGORY_SERIE;
                notif.dateExpiration = dateExpiration;
				notif.templateParameters.put("SERIE_THRESHOLD_NB_TOUR_UP", ""+thresholdNbTourUp);
                notif.addExtraField(NOTIF_PARAM_TEMPLATE, NOTIF_SERIE_NEW_NEW_NO_PLAY);
                notif.addExtraField(NOTIF_PARAM_PERIOD, currentPeriod);
                notif.addExtraField(NOTIF_PARAM_SERIE, TourSerieMgr.SERIE_NC);
                if (persistNotif(notif)) {
                    return notif;
                }
            } else {
                log.error("Notif template not found for name="+NOTIF_SERIE_NEW_NEW_NO_PLAY);
            }
        }
        return null;
    }

    /**
     * return a messageNotifGroup with fieldName contains all elements from listFieldName and fieldValue contains all elements from listFieldValue.
     * WARNING : fields and value must be in the same order !
     * @param listFieldName
     * @param listFieldValue
     * @return
     */
    public MessageNotifGroup getLastNotifGroupWithField(List<String> listFieldName, List<String> listFieldValue) {
        if (listFieldName != null && listFieldName.size() > 0 && listFieldValue != null && listFieldValue.size() > 0) {
            Criteria cType = Criteria.where("msgType").is(MessageNotifGroup.MSG_TYPE);
            String strFieldName = "";
            for (String s : listFieldName) {
                if (strFieldName.length() > 0) {
                    strFieldName += Constantes.SEPARATOR_VALUE;
                }
                strFieldName += s;
            }
            String strFieldValue = "";
            for (String s : listFieldValue) {
                if (strFieldValue.length() > 0) {
                    strFieldValue += Constantes.SEPARATOR_VALUE;
                }
                strFieldValue += s;
            }
            Criteria cFieldName = Criteria.where("fieldName").regex(".*" + strFieldName + ".*");
            Criteria cFieldValue = Criteria.where("fieldValue").regex(".*" + strFieldValue + ".*");
            Query q = new Query();
            q.addCriteria(new Criteria().andOperator(cType, cFieldName, cFieldValue));
            q.with(new Sort(Sort.Direction.DESC, "dateMsg"));
            q.limit(1);
            return mongoTemplate.findOne(q, MessageNotifGroup.class);
        }
        return null;
    }

	/**
	 * Return the WSMessageID : build with the msg type
	 * @param msgType
	 * @param msgID
	 * @return
	 */
	public static String buildWSMessageID(String msgType, String msgID) {
		return msgType+"-"+msgID;
	}

    public WSMessageNotif buildWSMessage(MessageNotifBase notif, PlayerCache player) {
		return buildWSMessage(notif, player.ID, player.getPseudo(), player.lang, player.countryCode, 0);
    }

	public WSMessageNotif buildWSMessage(MessageNotifBase notif, Player player) {
		return buildWSMessage(notif, player.getID(), player.getNickname(), player.getDisplayLang(), player.getDisplayCountryCode(), player.getSubscriptionExpirationDate());
	}

	public WSMessageNotif buildWSMessage(MessageNotifBase notif, long playerID, String playerPseudo, String playerDisplayLang, String playerCountry, long playerDateSubscriptionExpiration) {
		WSMessageNotif wmsg = new WSMessageNotif();
		wmsg.ID = buildWSMessageID(notif.msgType,notif.getIDStr());
		wmsg.category = notif.category;
		wmsg.displayMask = notif.displayMask;
		wmsg.dateExpiration = notif.dateExpiration;
		wmsg.dateReceive = notif.dateMsg;
		wmsg.senderID = Constantes.PLAYER_FUNBRIDGE_ID;
		wmsg.senderAvatar = false;
		wmsg.senderPseudo = Constantes.PLAYER_FUNBRIDGE_PSEUDO;
		wmsg.titleIcon = notif.titleIcon;
		wmsg.titleBackgroundColor = notif.titleBackgroundColor;
		wmsg.titleColor = notif.titleColor;

		// Template parameters
		if (notif.templateParameters != null && notif.templateParameters.size() > 0) {
			NumberFormat formatter = NumberFormat.getInstance(new Locale(playerDisplayLang));

			for (Map.Entry<String, String> entry : notif.templateParameters.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();

				// Discount
				if (playerDisplayLang.substring(0, 2).equalsIgnoreCase("zh") && key.contains("DISCOUNT")) {
					notif.templateParameters.put(key, textUIMgr.formatDiscountForChinese(value));
				}

				// Date
				if (key.equals(VARIABLE_DATEPLUS7) || key.equals(VARIABLE_DATEPLUS2)) {
					if (NumberUtils.isNumber(value)) {
						notif.templateParameters.put(key, Constantes.formatShortDate(Long.valueOf(value), playerDisplayLang, playerCountry));
					}
				}

				if (value != null) {
					// Timestamp
					if (value.startsWith(VARIABLE_TIMESTAMP_FORMAT_DATE)) {
						String timestamp = value.substring(VARIABLE_TIMESTAMP_FORMAT_DATE.length(), value.indexOf(VARIABLE_END));
						if (NumberUtils.isNumber(timestamp)) {
							notif.templateParameters.put(key, Constantes.formatShortDate(Long.valueOf(timestamp), playerDisplayLang, playerCountry));
						}
					}
					if (value.startsWith(VARIABLE_TIMESTAMP_FORMAT_DAYMONTH)) {
						String timestamp = value.substring(VARIABLE_TIMESTAMP_FORMAT_DAYMONTH.length(), value.indexOf(VARIABLE_END));
						if (NumberUtils.isNumber(timestamp)) {
							notif.templateParameters.put(key, Constantes.formatDayMonthDate(Long.valueOf(timestamp), playerDisplayLang, playerCountry));
						}
					}
					if (value.startsWith(VARIABLE_TIMESTAMP_FORMAT_DATETIME)) {
						String timestamp = value.substring(VARIABLE_TIMESTAMP_FORMAT_DATETIME.length(), value.indexOf(VARIABLE_END));
						if (NumberUtils.isNumber(timestamp)) {
							notif.templateParameters.put(key, Constantes.formatShortDateTime(Long.valueOf(timestamp), playerDisplayLang, playerCountry));
						}
					}
					if (value.startsWith(VARIABLE_TIMESTAMP_FORMAT_TIME)) {
						String timestamp = value.substring(VARIABLE_TIMESTAMP_FORMAT_TIME.length(), value.indexOf(VARIABLE_END));
						if (NumberUtils.isNumber(timestamp)) {
							notif.templateParameters.put(key, Constantes.formatShortTime(Long.valueOf(timestamp), playerDisplayLang, playerCountry));
						}
					}
					if (value.startsWith(VARIABLE_TEXT_UI) && !value.equals(VARIABLE_TEXT_UI)) {
						String textUi = value.substring(VARIABLE_TEXT_UI.length(), value.indexOf(VARIABLE_END));
						if (!textUi.isEmpty()) {
							notif.templateParameters.put(key, textUIMgr.getTextUIForLang(textUi, playerDisplayLang));
						}
					}

					// Number
					if (value.startsWith(VARIABLE_NUMBER_START)) {
						String number = value.substring(VARIABLE_NUMBER_START.length(), value.indexOf(VARIABLE_END));
						if (NumberUtils.isNumber(number)) {
							BigDecimal bd = new BigDecimal(NumberUtils.toDouble(number));
							notif.templateParameters.put(key, formatter.format(bd.doubleValue()));
						} else {
							notif.templateParameters.put(key, number);
						}
					}
				}
			}
		}

		if (notif.templateName != null && notif.templateName.length() > 0) {
			// BODY TEXT
            TextUIData template = getTemplate(notif.templateName);
			if (template != null) {
                if (notif.templateName.equals(NOTIF_FRIEND_DUEL_RESULT)) {
                    wmsg.body = template.getText(playerDisplayLang);
                    wmsg.body += notif.getMsgBodyForLang(playerDisplayLang);
                } else {
                    wmsg.body = template.getText(playerDisplayLang);
					if (notif.templateParameters != null && notif.templateParameters.size() > 0) {
						wmsg.body = MessageNotifMgr.replaceTextVariables(wmsg.body, notif.templateParameters);
					}
                }
			}

			// RICH BODY TEXT
			template = getTemplateRichBody(notif.templateName);
			if (template != null) {
				if (notif.templateName.equals(NOTIF_FRIEND_DUEL_RESULT)) {
					wmsg.richBody = template.getText(playerDisplayLang);
					wmsg.richBody += notif.getRichBodyForLang(playerDisplayLang);
				} else {
					wmsg.richBody = template.getText(playerDisplayLang);
					if (notif.templateParameters != null && notif.templateParameters.size() > 0) {
						wmsg.richBody = MessageNotifMgr.replaceTextVariables(wmsg.richBody, notif.templateParameters);
					}
				}
			}

			// TITLE TEXT
			template = getTemplateTitle(notif.templateName);
			if (template != null) {
				wmsg.title = template.getText(playerDisplayLang);
				if (notif.templateParameters != null && notif.templateParameters.size() > 0) {
					wmsg.title = MessageNotifMgr.replaceTextVariables(wmsg.title, notif.templateParameters);
				}
			}

			// ACTION BUTTON TEXT
			template = getTemplateButton(notif.templateName);
			if (template != null) {
				wmsg.actionButtonText = template.getText(playerDisplayLang);
				if (notif.templateParameters != null && notif.templateParameters.size() > 0) {
					wmsg.actionButtonText = MessageNotifMgr.replaceTextVariables(wmsg.actionButtonText, notif.templateParameters);
				}
			}

			// ACTION TEXT
			template = getTemplateAction(notif.templateName);
			if (template != null) {
				wmsg.actionText = template.getText(playerDisplayLang);
				if (notif.templateParameters != null && notif.templateParameters.size() > 0) {
					wmsg.actionText = MessageNotifMgr.replaceTextVariables(wmsg.actionText, notif.templateParameters);
				}
			}
		} else {
			wmsg.body = notif.getMsgBodyForLang(playerDisplayLang);
			wmsg.richBody = notif.getRichBodyForLang(playerDisplayLang);
			wmsg.title = notif.getTitleForLang(playerDisplayLang);
			wmsg.actionButtonText = notif.getActionButtonTextForLang(playerDisplayLang);
			wmsg.actionText = notif.getActionTextForLang(playerDisplayLang);
		}
		if (StringUtils.isNotBlank(wmsg.actionText)) {
			wmsg.actionText = FBConfiguration.getInstance().getStringValue("message.notifTextBeforeAction", "\n") + wmsg.actionText;
		}

		// Fill map with vars
        Map<String, String> mapVarVal = new HashMap<String, String>();
        mapVarVal.put(MessageNotifMgr.VARIABLE_PLAYER_ID, ""+playerID);
        mapVarVal.put(MessageNotifMgr.VARIABLE_PLAYER_PSEUDO, playerPseudo);
        mapVarVal.put(MessageNotifMgr.VARIABLE_LINE_BREAK, FBConfiguration.getInstance().getStringValue("message.notifLineBreak", "\n"));
		if (MessageNotifMgr.textContainVariableSerie(wmsg.body)) {
			mapVarVal.putAll(Constantes.MESSAGE_SERIE_PARAM);
		}
        if (MessageNotifMgr.textContainVariableTeamDivision(wmsg.body)) {
            mapVarVal.putAll(Constantes.MESSAGE_TEAM_DIVISION_PARAM);
        }
		if (MessageNotifMgr.textContainVariableExpiration(wmsg.body) && playerDateSubscriptionExpiration != 0){
			Date expirationDate = new Date(playerDateSubscriptionExpiration);
			DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			String expirationDateString = dateFormat.format(expirationDate);
			mapVarVal.put(MessageNotifMgr.VARIABLE_PLAYER_EXPIRATION, expirationDateString);
		}
		if (MessageNotifMgr.textContainsVariable(wmsg.body, VARIABLE_PRICE_START)) {
			String prices = FBConfiguration.getInstance().getStringValue("message.notif.parameters.price", "price_1;price_3;price_6;price_12;price_50_deals");
			for (String price : prices.split(";")) {
				mapVarVal.put(price.toUpperCase(), FBConfiguration.getInstance().getStringValue("message.notif.parameter."+price+".default", ""));
				String[] priceValues = FBConfiguration.getInstance().getStringValue("message.notif.parameter."+price+".values", "").split(";");
				for (String value : priceValues) {
					String[] countryValue = value.split(":");
					if(countryValue.length != 2) continue;
					if (countryValue[0].equalsIgnoreCase(playerCountry)) {
						mapVarVal.put(price.toUpperCase(), countryValue[1]);
						break;
					}
				}
			}
		}

		// Replace body using map of vars
		wmsg.body = MessageNotifMgr.replaceTextVariables(wmsg.body, mapVarVal);
		wmsg.richBody = MessageNotifMgr.replaceTextVariables(wmsg.richBody, mapVarVal);
		wmsg.title = MessageNotifMgr.replaceTextVariables(wmsg.title, mapVarVal);
		wmsg.actionButtonText = MessageNotifMgr.replaceTextVariables(wmsg.actionButtonText, mapVarVal);
		wmsg.actionText = MessageNotifMgr.replaceTextVariables(wmsg.actionText, mapVarVal);

		// FIELDS
		if (notif.fieldName != null && notif.fieldName.length() > 0 &&
				notif.fieldValue != null && notif.fieldValue.length() > 0) {
			wmsg.extraFields = new ArrayList<WSMessageExtraField>();
			String[] tempFieldName = notif.fieldName.split(Constantes.SEPARATOR_VALUE);
			String[] tempFieldValue = notif.fieldValue.split(Constantes.SEPARATOR_VALUE);
			if (tempFieldName.length == tempFieldValue.length) {
				for (int i = 0; i < tempFieldName.length; i++) {
					WSMessageExtraField temp = new WSMessageExtraField();
					temp.name = tempFieldName[i];
					temp.value = MessageNotifMgr.replaceTextVariables(tempFieldValue[i], mapVarVal);
					wmsg.extraFields.add(temp);
				}
			}
		}

		// SPECIFIC FOR NOTIF TYPE
		if (notif instanceof MessageNotif) {
			wmsg.read = ((MessageNotif)notif).isReadByPlayer();
		}
		else if (notif instanceof MessageNotifGroup) {
			wmsg.read = isNotifGroupReadByPlayer(notif.ID.toString(), playerID);
		}
		else if (notif instanceof MessageNotifAll) {
			wmsg.read = isNotifAllReadByPlayer(notif.ID.toString(), playerID);
		}
		return wmsg;
	}

    /**
     * Create notif for duel match making. Visuel H - Expiration=Date fin defi
     * @param partner
     * @param recipient
     * @return
     */
    public MessageNotif createNotifDuelMatchMaking(Player partner, Player recipient, long duration) {
        if (isNotifEnable()) {
            if (partner != null && recipient != null) {
                TextUIData notifTemplate = getTemplate(NOTIF_DUEL_MATCH_MAKING);
                if (notifTemplate != null) {
                    MessageNotif notif = new MessageNotif(notifTemplate.name);
                    notif.recipientID = recipient.getID();
                    notif.category = MESSAGE_CATEGORY_DUEL;
                    notif.displayMask = MESSAGE_DISPLAY_DIALOG_BOX;
                    notif.dateRead = notif.dateMsg;
                    notif.dateExpiration = System.currentTimeMillis() + duration;
                    notif.templateParameters.put("PARTNER_PSEUDO", partner.getNickname());
                    notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                    notif.addExtraField(NOTIF_PARAM_MATCH_MAKING, "NEW");
                    notif.addExtraField(NOTIF_PARAM_PSEUDO, partner.getNickname());
                    if (persistNotif(notif)) {
                        return notif;
                    }
                } else {
                    log.error("Notif template not found for name="+NOTIF_DUEL_ANSWER_OK);
                }
            } else {
                log.error("Parameters not valid ... sender="+partner+" - recipient="+recipient);
            }
        }
        return null;
    }

	/**
	 * Create notifGroup for tourFederation ready to play
	 * @param tour
	 * @return
	 */
	public MessageNotifGroup createNotifGroupTourFederationReadyToPlay(TourFederationTournament tour, String federationName) {
		if (isNotifEnable()) {
			if (tour != null) {
				TextUIData notifTemplate = getTemplate("tour"+federationName+"ReadyToPlay");
				if (notifTemplate != null) {
					MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
					notif.category = FederationMgr.getMessageCategoryFromFederationName(federationName);
					notif.dateExpiration = System.currentTimeMillis() + DURATION_7J;
					notif.templateParameters.put("DATE_BEGIN_TOURNAMENT", VARIABLE_TIMESTAMP_FORMAT_DATETIME+tour.getStartDate()+VARIABLE_END);
					notif.templateParameters.put("DATE_END_TOURNAMENT", VARIABLE_TIMESTAMP_FORMAT_DATETIME+tour.getEndDate()+VARIABLE_END);
					notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);
					if (persistNotif(notif)) {
						return notif;
					}
				} else {
					log.error("No template found for name tour"+federationName+"ReadyToPlay");
				}
			} else {
				log.error("Parameters not valid ... tour="+tour);
			}
		}
		return null;
	}

	public MessageNotifGroup createNotifGroupTourFederationCanceled(TourFederationTournament tour, TourFederationTournament nextTournament, String federationName) {
		if (isNotifEnable()) {
			if (tour != null) {
				TextUIData notifTemplate = getTemplate("tour"+federationName+"Canceled");
				if (notifTemplate != null) {
					MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
					notif.category = FederationMgr.getMessageCategoryFromFederationName(federationName);
					notif.dateExpiration = System.currentTimeMillis() + DURATION_7J;
					notif.templateParameters.put("DATE_BEGIN_TOURNAMENT", FederationMgr.getSdfFromFederationName(federationName).format(new Date(tour.getStartDate())));
					String dateNextTournament = "??";
					if (nextTournament != null) {
						dateNextTournament = FederationMgr.getSdfFromFederationName(federationName).format(new Date(nextTournament.getStartDate()));
					}
					notif.templateParameters.put("DATE_NEXT_TOURNAMENT", dateNextTournament);
					notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);
					if (persistNotif(notif)) {
						return notif;
					}
				} else {
					log.error("No template found for name tour"+federationName+"Canceled");
				}
			} else {
				log.error("Parameters not valid ... tour="+tour);
			}
		}
		return null;
	}

	public MessageNotif createNotifTourFederationFinish(TourFederationTournament tourFederation, TourFederationTournamentPlayer tourPlayer, int nbPlayers, Map<Integer, String> mapPodium, TourFederationTournament nextTournament, boolean haveReward) {
		if (isNotifEnable()) {
			if (tourPlayer != null) {
				String federationName = tourFederation.getTourFederationMgr().getFederationName();
				String templateName = haveReward ? "tourFinishReward" :  "tourFinish";
				MessageNotif notif = new MessageNotif(templateName);
				notif.recipientID = tourPlayer.getPlayerID();
				notif.category = FederationMgr.getMessageCategoryFromFederationName(federationName);
				notif.dateExpiration = System.currentTimeMillis() + DURATION_7J;
				notif.templateParameters.put("DATE_TOURNAMENT", VARIABLE_TIMESTAMP_FORMAT_DATETIME+tourPlayer.getTournament().getStartDate()+VARIABLE_END);
				if (tourPlayer.getTournament().getResultType() == Constantes.TOURNAMENT_RESULT_PAIRE) {
					notif.templateParameters.put("SCORE", String.format(Locale.ENGLISH, "%.2f", tourPlayer.getResult() * 100) + "%");
				} else {
					String sign = (tourPlayer.getResult() > 0)?"+":"";
					notif.templateParameters.put("SCORE", sign + (int) tourPlayer.getResult() + " IMP");
				}
				notif.templateParameters.put("RANK_PLAYER", VARIABLE_NUMBER_START+tourPlayer.getRank()+VARIABLE_END);
				notif.templateParameters.put("NB_PLAYER", VARIABLE_NUMBER_START+nbPlayers+VARIABLE_END);
				notif.templateParameters.put("NB_POINTS", VARIABLE_NUMBER_START+tourPlayer.getStringPoints()+VARIABLE_END);
				notif.templateParameters.put("FED_POINTS", VARIABLE_TEXT_UI + "federation.points." + federationName + VARIABLE_END);

				if(mapPodium != null){
					for (int i =0 ; i < mapPodium.size() ;i++){
						addPodiumPlayer(
								mapPodium.get(i),
								notif.templateParameters,
								String.format("PSEUDO_%d",i+1),
								String.format("FEDPOINTS_WON_%d",i+1));
					}
				}

				String dateNextTournament = "??";
				if (nextTournament != null) {
					dateNextTournament = VARIABLE_TIMESTAMP_FORMAT_DATETIME+nextTournament.getStartDate()+VARIABLE_END;
				}
				notif.templateParameters.put("DATE_NEXT_TOURNAMENT", dateNextTournament);
				notif.templateParameters.put("NB_FED_POINTS", VARIABLE_NUMBER_START+(int)tourPlayer.getPoints() + ""+VARIABLE_END);
				notif.templateParameters.put("NB_PF_POINTS", tourPlayer.getFunbridgePoints() + "");
				notif.templateParameters.put("FED", federationName);
				notif.templateParameters.put("DATE",VARIABLE_TIMESTAMP_FORMAT_DATETIME+tourFederation.getStartDate()+VARIABLE_END);

				notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);

				if (persistNotif(notif)) {
					return notif;
				}
			} else {
				log.error("Parameters not valid ... tourPlayer="+tourPlayer);
			}
		}
		return null;
	}

	private void addPodiumPlayer(String player, Map<String, String> templateParameters, String playerParam, String pointParam){
		String[] splitPlayer = player != null ? player.split("_") : null;
		if(splitPlayer != null && splitPlayer.length == 2){
			templateParameters.put(playerParam,splitPlayer[0]);
			templateParameters.put(pointParam,splitPlayer[1]);
		}
	}

	/**
	 * Create notif on end timezone tournament
	 * @param tourPlayer
	 * @return
	 */
	public MessageNotif createNotifTourTimezoneFinish(GenericMemTournamentPlayer tourPlayer, GenericMemTournament tour) {
		if (isNotifEnable()) {
			if (tourPlayer != null && tour != null) {
				TextUIData notifTemplate = getTemplate("timezoneTournamentFinish");
				if (notifTemplate != null) {
					MessageNotif notif = new MessageNotif(notifTemplate.name);
					notif.recipientID = tourPlayer.playerID;
					notif.category = MESSAGE_CATEGORY_TIMEZONE;
					notif.dateExpiration = System.currentTimeMillis() + DURATION_1J;
					notif.templateParameters.put("RANK_PLAYER", VARIABLE_NUMBER_START + tourPlayer.ranking +VARIABLE_END);
					notif.templateParameters.put("NB_PLAYER", VARIABLE_NUMBER_START + tour.getNbPlayer() +VARIABLE_END);
					if (tour.resultType == Constantes.TOURNAMENT_RESULT_PAIRE) {
						notif.templateParameters.put("RESULT", String.format(Locale.ENGLISH, "%.2f", tourPlayer.result * 100) + "%");
					} else {
						notif.templateParameters.put("RESULT", (int) tourPlayer.result + " IMP");
					}
					notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);
					if (persistNotif(notif)) {
						return notif;
					}
				} else {
					log.error("No template found for name timezoneTournamentFinish");
				}
			} else {
				log.error("Parameters not valid ... tourPlayer="+tourPlayer);
			}
		}
		return null;
	}

	/**
	 * Create notif group serie reminder down
	 * @param serie
	 * @param serieNext
	 * @param dateExpiration
	 * @return
	 */
	public MessageNotifGroup createNotifGroupSerieReminderDown(String serie, String serieNext, long dateExpiration) {
		if (isNotifEnable()) {
			TextUIData notifTemplate = getTemplate(NOTIF_SERIE_REMINDER_DOWN);
			if (notifTemplate != null) {
				MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
				notif.category = MESSAGE_CATEGORY_SERIE;
				notif.dateExpiration = dateExpiration;
				notif.templateParameters.put("SERIE_DOWN", VARIABLE_SERIE_START+ serieNext +VARIABLE_SERIE_END);
				notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);
				notif.addExtraField(NOTIF_PARAM_SERIE, serie);
				if (persistNotif(notif)) {
					return notif;
				}
			} else {
				log.error("Notif template not found for name="+ NOTIF_SERIE_REMINDER_DOWN);
			}
		}
		return null;
	}

	/**
	 * Create notif group serie reminder maintain
	 * @param serie
	 * @param serieNext
	 * @param dateExpiration
	 * @return
	 */
	public MessageNotifGroup createNotifGroupSerieReminderMaintain(String serie, String serieNext, long dateExpiration) {
		if (isNotifEnable()) {
			TextUIData notifTemplate = getTemplate(NOTIF_SERIE_REMINDER_MAINTAIN);
			if (notifTemplate != null) {
				MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
				notif.category = MESSAGE_CATEGORY_SERIE;
				notif.dateExpiration = dateExpiration;
				notif.templateParameters.put("SERIE", VARIABLE_SERIE_START+ serie +VARIABLE_SERIE_END);
				notif.templateParameters.put("SERIE_UP", VARIABLE_SERIE_START+ serieNext +VARIABLE_SERIE_END);
				notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);
				notif.addExtraField(NOTIF_PARAM_SERIE, serie);
				if (persistNotif(notif)) {
					return notif;
				}
			} else {
				log.error("Notif template not found for name="+ NOTIF_SERIE_REMINDER_MAINTAIN);
			}
		}
		return null;
	}

	/**
	 * Create notif group serie reminder maintain top
	 * @param serie
	 * @param dateExpiration
	 * @return
	 */
	public MessageNotifGroup createNotifGroupSerieReminderMaintainTop(String serie, long dateExpiration) {
		if (isNotifEnable()) {
			TextUIData notifTemplate = getTemplate(NOTIF_SERIE_REMINDER_MAINTAIN_TOP);
			if (notifTemplate != null) {
				MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
				notif.category = MESSAGE_CATEGORY_SERIE;
				notif.dateExpiration = dateExpiration;
				notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);
				notif.addExtraField(NOTIF_PARAM_SERIE, serie);
				if (persistNotif(notif)) {
					return notif;
				}
			} else {
				log.error("Notif template not found for name="+ NOTIF_SERIE_REMINDER_MAINTAIN_TOP);
			}
		}
		return null;
	}

	/**
	 * Create notif group serie reminder up
	 * @param serie
	 * @param serieNext
	 * @param dateExpiration
	 * @return
	 */
	public MessageNotifGroup createNotifGroupSerieReminderUp(String serie, String serieNext, long dateExpiration) {
		if (isNotifEnable()) {
			TextUIData notifTemplate = getTemplate(NOTIF_SERIE_REMINDER_UP);
			if (notifTemplate != null) {
				MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
				notif.category = MESSAGE_CATEGORY_SERIE;
				notif.dateExpiration = dateExpiration;
				notif.templateParameters.put("SERIE_UP", VARIABLE_SERIE_START+ serieNext +VARIABLE_SERIE_END);
				notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);
				notif.addExtraField(NOTIF_PARAM_SERIE, serie);
				if (persistNotif(notif)) {
					return notif;
				}
			} else {
				log.error("Notif template not found for name="+ NOTIF_SERIE_REMINDER_UP);
			}
		}
		return null;
	}

	/**
	 * Create notif group serie reminder
	 * @param serie
	 * @param dateExpiration
	 * @return
	 */
	public MessageNotifGroup createNotifGroupSerieReminder(String serie, long dateExpiration) {
		if (isNotifEnable()) {
			TextUIData notifTemplate = getTemplate(NOTIF_SERIE_REMINDER);
			if (notifTemplate != null) {
				MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
				notif.category = MESSAGE_CATEGORY_SERIE;
				notif.dateExpiration = dateExpiration;
				notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);
				notif.addExtraField(NOTIF_PARAM_SERIE, serie);
				if (persistNotif(notif)) {
					return notif;
				}
			} else {
				log.error("Notif template not found for name="+ NOTIF_SERIE_REMINDER);
			}
		}
		return null;
	}

	/**
	 * Create notif group ranking for type
	 * @param templateName
	 * @param playerID
	 * @param pseudo
	 * @param rank
	 * @param nbPlayers
	 * @param result
	 * @param rankingType
	 * @param rankingOptions
	 * @return
	 */
	public MessageNotif createNotifGroupRankingTypeGeneric(String templateName, long playerID, String pseudo, int rank, int nbPlayers, double result, String rankingType, String rankingOptions) {
		if (isNotifEnable()) {
			TextUIData notifTemplate = getTemplate(templateName);
			if (notifTemplate != null) {
				MessageNotif notif = new MessageNotif(notifTemplate.name);
				notif.recipientID = playerID;
				notif.category = MESSAGE_CATEGORY_RANKING;
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MONTH, 1);
				notif.dateExpiration = cal.getTimeInMillis();
				notif.templateParameters.put("PLA_PSEUDO", pseudo);
				notif.templateParameters.put("RESULT", VARIABLE_NUMBER_START +(int)result+VARIABLE_END);
				notif.templateParameters.put("RANK_PLAYER", VARIABLE_NUMBER_START +rank+VARIABLE_END);
				notif.templateParameters.put("NB_PLAYERS", VARIABLE_NUMBER_START +nbPlayers+VARIABLE_END);
				notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);
				notif.addExtraField(NOTIF_PARAM_RANKING_TYPE, rankingType);
				notif.addExtraField(NOTIF_PARAM_RANKING_OPTIONS, rankingOptions);
				if (persistNotif(notif)) {
					return notif;
				}
			} else {
				log.error("Notif template not found for name="+ templateName);
			}
		}
		return null;
	}

	/**
	 * Create notif group ranking for type (for all players except top 3 and top 10%)
	 * @param playerID
	 * @param pseudo
	 * @param rank
	 * @param nbPlayers
	 * @param result
	 * @param rankingType
	 * @param rankingOptions
	 * @return
	 */
	public MessageNotif createNotifGroupRankingTypeOthers(long playerID, String pseudo, int rank, int nbPlayers, double result, String rankingType, String rankingOptions) {
		String templateName = null;
		if (rankingType.equals(Constantes.RANKING_TYPE_PTS_CBO)) {
			templateName = NOTIF_RANKING_PTS_FB;
		} else if (rankingType.equals(Constantes.RANKING_TYPE_DUEL)) {
			templateName = NOTIF_RANKING_DUEL;
		} else if (rankingType.equals(Constantes.RANKING_TYPE_DUEL_ARGINE)) {
			templateName = NOTIF_RANKING_DUEL_ARGINE;
		}

		if (templateName != null) {
			return createNotifGroupRankingTypeGeneric(templateName, playerID, pseudo, rank, nbPlayers, result, rankingType, rankingOptions);
		}

		return null;
	}

	/**
	 * Create notif group ranking for type (for top 3)
	 * @param playerID
	 * @param pseudo
	 * @param rank
	 * @param nbPlayers
	 * @param result
	 * @param rankingType
	 * @param rankingOptions
	 * @return
	 */
	public MessageNotif createNotifGroupRankingTypeTop3(long playerID, String pseudo, int rank, int nbPlayers, double result, String rankingType, String rankingOptions) {
		String templateName = null;
		if (rankingType.equals(Constantes.RANKING_TYPE_PTS_CBO)) {
			templateName = NOTIF_RANKING_TOP3_PTS_FB;
		} else if (rankingType.equals(Constantes.RANKING_TYPE_DUEL)) {
			templateName = NOTIF_RANKING_TOP3_DUEL;
		} else if (rankingType.equals(Constantes.RANKING_TYPE_DUEL_ARGINE)) {
			templateName = NOTIF_RANKING_TOP3_DUEL_ARGINE;
		}

		if (templateName != null) {
			return createNotifGroupRankingTypeGeneric(templateName, playerID, pseudo, rank, nbPlayers, result, rankingType, rankingOptions);
		}

		return null;
	}

	/**
	 * Create notif group ranking for type (for top 10%)
	 * @param playerID
	 * @param pseudo
	 * @param rank
	 * @param nbPlayers
	 * @param result
	 * @param rankingType
	 * @param rankingOptions
	 * @return
	 */
	public MessageNotif createNotifGroupRankingTypeTop10Percent(long playerID, String pseudo, int rank, int nbPlayers, double result, String rankingType, String rankingOptions) {
		String templateName = null;
		if (rankingType.equals(Constantes.RANKING_TYPE_PTS_CBO)) {
			templateName = NOTIF_RANKING_TOP10_PERCENT_PTS_FB;
		} else if (rankingType.equals(Constantes.RANKING_TYPE_DUEL)) {
			templateName = NOTIF_RANKING_TOP10_PERCENT_DUEL;
		} else if (rankingType.equals(Constantes.RANKING_TYPE_DUEL_ARGINE)) {
			templateName = NOTIF_RANKING_TOP10_PERCENT_DUEL_ARGINE;
		}

		if (templateName != null) {
			return createNotifGroupRankingTypeGeneric(templateName, playerID, pseudo, rank, nbPlayers, result, rankingType, rankingOptions);
		}

		return null;
	}

	/**
	 * Process notif ranking
	 */
	public void processNotifRanking() throws FBWSException {
		if (isNotifEnable()) {
			ResultServiceRest.GetMainRankingResponse resp;
			Map<String, String> templateParameters = new HashMap<>();

			// Pts FB
			String periodID = tourFederationStatPeriodMgr.getStatPreviousPeriodID();
			resp = tourCBOMgr.getRankingFunbridgePoints(new PlayerCache(0), periodID, null, null, 0, 3);
			if (resp != null) {
				for (int i = 0; i < 3; i++) {
					WSMainRankingPlayer rankingPlayer = new WSMainRankingPlayer();
					if (resp.ranking.size() > i) {
						rankingPlayer = resp.ranking.get(i);
					}
					templateParameters.put("PLA_PSEUDO"+i+"_PTS_FB", rankingPlayer.getPlayerPseudo());
					templateParameters.put("PLA"+i+"_NB_PTS_FB", VARIABLE_NUMBER_START +(int)rankingPlayer.value+VARIABLE_END);
				}
			}

			// Serie
			periodID = tourSerieMgr.getPeriodIDPrevious();
			resp = tourSerieMgr.getRanking(new PlayerCache(0), periodID, null, null, 0, 3);
			if (resp != null) {
				for (int i = 0; i < 3; i++) {
					WSMainRankingPlayer rankingPlayer = new WSMainRankingPlayer();
					if (resp.ranking.size() > i) {
						rankingPlayer = resp.ranking.get(i);
					}
					templateParameters.put("PLA_PSEUDO"+i+"_SERIE", rankingPlayer.getPlayerPseudo());
				}
			}

			// Performance
			resp = playerMgr.getRankingAveragePerformance(new PlayerCache(0), null, null, 0, 3);
			if (resp != null) {
				for (int i = 0; i < 3; i++) {
					WSMainRankingPlayer rankingPlayer = new WSMainRankingPlayer();
					if (resp.ranking.size() > i) {
						rankingPlayer = resp.ranking.get(i);
					}
					templateParameters.put("PLA_PSEUDO"+i+"_PERF", rankingPlayer.getPlayerPseudo());
					templateParameters.put("PLA"+i+"_PERF_MOY", String.format("%.2f", rankingPlayer.value * 100)+"%");
				}
			}

			// Duel
			periodID = duelMgr.getStatPreviousPeriodID();
			resp = duelMgr.getRanking(new PlayerCache(0), periodID, null, null, 0, 4);
			if (resp != null) {
				int rank = 0;
				for (int i = 0; i < 4; i++) {
					WSMainRankingPlayer rankingPlayer = new WSMainRankingPlayer();
					if (resp.ranking.size() > i) {
						rankingPlayer = resp.ranking.get(i);
					}
					if (rankingPlayer.playerID != -2) {
						templateParameters.put("PLA_PSEUDO" + rank + "_DUEL", rankingPlayer.getPlayerPseudo());
						templateParameters.put("PLA" + rank + "_DUEL_NB_WIN", VARIABLE_NUMBER_START +(int)rankingPlayer.value+VARIABLE_END);
						rank++;
					}
				}
			}

			// Duel Argine
			resp = duelMgr.getRankingArgine(new PlayerCache(0), periodID, null, null, 0, 3);
			if (resp != null) {
				for (int i = 0; i < 3; i++) {
					WSMainRankingPlayer rankingPlayer = new WSMainRankingPlayer();
					if (resp.ranking.size() > i) {
						rankingPlayer = resp.ranking.get(i);
					}
					templateParameters.put("PLA_PSEUDO"+i+"_DUEL_ARGINE", rankingPlayer.getPlayerPseudo());
					templateParameters.put("PLA"+i+"_DUEL_ARGINE_NB_WIN", VARIABLE_NUMBER_START +(int)rankingPlayer.value+VARIABLE_END);
				}
			}

			String rankingOptions = Constantes.RANKING_OPTIONS_PREVIOUS_PERIOD;

			TextUIData notifTemplate = getTemplate(NOTIF_RANKING_ALL_PODIUMS);
			if (notifTemplate != null) {
				MessageNotifAll notif = new MessageNotifAll();
				notif.category = MESSAGE_CATEGORY_RANKING;
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MONTH, 1);
				notif.dateExpiration = cal.getTimeInMillis();
				notif.templateName = notifTemplate.name;
				notif.templateParameters.putAll(templateParameters);
				notif.addExtraField(NOTIF_PARAM_ACTION, NOTIF_ACTION_OPEN_CATEGORY_PAGE);
				notif.addExtraField(NOTIF_PARAM_RANKING_OPTIONS, rankingOptions);
				if (persistNotif(notif)) {
					List<Session> listSession = presenceMgr.getAllCurrentSession();
					for (Session s : listSession) {
						if (s instanceof FBSession) {
							FBSession fbs = (FBSession) s;
							fbs.pushEvent(buildEvent(notif, fbs.getPlayer()));
						}
					}
				}
			} else {
				log.error("Notif template not found for name="+ NOTIF_RANKING_ALL_PODIUMS);
			}
		}
	}

	/**
	 * Method to create notif and set all fields
	 * @param notifDate
	 * @param recipientID
	 * @param category
	 * @param displayMask
	 * @param dateExpiration
	 * @param templateName
	 * @param templateParameters
	 * @param fieldName
	 * @param fieldValue
	 * @param titleIcon
	 * @param titleBackgroundColor
	 * @param titleColor
	 * @return
	 */
	public MessageNotif createNotifNursing(long notifDate, long recipientID, int category, int displayMask, long dateExpiration,
									String templateName, Map<String, String> templateParameters,
									String fieldName, String fieldValue, String titleIcon, String titleBackgroundColor, String titleColor) {
		if (isNotifEnable()) {
			TextUIData notifTemplate = getTemplate(templateName);
			if (notifTemplate != null) {
				MessageNotif notif = new MessageNotif();
				notif.recipientID = recipientID;
				notif.category = category;
				notif.displayMask = displayMask;
				if (notifDate != 0) {
					notif.setDateMsg(notifDate);
				}
				notif.dateExpiration = dateExpiration;
				if (templateName != null && templateName.length() > 0) {
					notif.templateName = templateName;
				}
				if (templateParameters != null) {
					notif.templateParameters = templateParameters;
				}
				notif.fieldName = fieldName;
				notif.fieldValue = fieldValue;
				notif.titleIcon = titleIcon;
				notif.titleBackgroundColor = titleBackgroundColor;
				notif.titleColor = titleColor;
				if (persistNotif(notif)) {
					return notif;
				}
			} else {
				log.error("Notif template not found for name="+ notifTemplate);
			}
		}
		return null;
	}
}
