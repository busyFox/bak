package com.funbridge.server.player;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.funbridge.server.common.*;
import com.funbridge.server.engine.ArgineProfile;
import com.funbridge.server.message.ChatMgr;
import com.funbridge.server.message.MessageMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.MessageNotif;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.dao.*;
import com.funbridge.server.player.data.*;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.store.StoreMgr;
import com.funbridge.server.team.TeamMgr;
import com.funbridge.server.team.data.Team;
import com.funbridge.server.tournament.data.TournamentChallenge;
import com.funbridge.server.tournament.federation.FederationMgr;
import com.funbridge.server.tournament.federation.TourFederationMgr;
import com.funbridge.server.tournament.federation.cbo.TourCBOMgr;
import com.funbridge.server.tournament.privatetournament.PrivateTournamentMgr;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.serie.data.TourSeriePlayer;
import com.funbridge.server.tournament.team.TourTeamMgr;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.event.Event;
import com.funbridge.server.ws.event.EventPlayerChangeLinkData;
import com.funbridge.server.ws.game.WSGamePlayer;
import com.funbridge.server.ws.player.*;
import com.funbridge.server.ws.presence.ConnectionData;
import com.funbridge.server.ws.result.ResultServiceRest;
import com.funbridge.server.ws.result.WSMainRankingCountry;
import com.funbridge.server.ws.result.WSMainRankingPlayer;
import com.funbridge.server.ws.team.WSTeamResult;
import com.funbridge.server.ws.tournament.WSDuelHistory;
import com.gotogames.common.crypt.AESCrypto;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.tools.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component(value="playerMgr")
@Scope(value="singleton")
public class PlayerMgr extends FunbridgeMgr{


    @Resource(name="playerDAO")
	private PlayerDAOJpa playerDAO = null;
	@Resource(name="playerDeviceDAO")
	private PlayerDeviceDAO playerDeviceDAO = null;
	@Resource(name="deviceDAO")
	private DeviceDAO deviceDAO = null;

	private List<String> listPasswordFr = new ArrayList<>();
	private List<String> listPasswordEn = new ArrayList<>();
	@Resource(name="playerLinkDAO")
	private PlayerLinkDAO playerLinkDAO = null;
	@Resource(name="messageMgr")
	private MessageMgr messageMgr = null;
	private LockWeakString lockPlayerLink = new LockWeakString();
	private LockString lockDevice = new LockString();

	@Resource(name="playerDuelDAO")
	private PlayerDuelDAO playerDuelDAO = null;
	private JSONTools jsontools = new JSONTools();
	@Resource(name="messageNotifMgr")
	private MessageNotifMgr notifMgr = null;
    @Resource(name = "storeMgr")
    private StoreMgr storeMgr = null;
	@Resource(name = "presenceMgr")
	private PresenceMgr presenceMgr = null;
	@Resource(name="chatMgr")
	private ChatMgr chatMgr = null;
	@Resource(name="privateTournamentMgr")
	private PrivateTournamentMgr privateTournamentMgr;

	private int communityNbActivePlayers = 0;
	private int communityNbCountryCode = 0;
	private List<WSCommunityCountryPlayer> communityListCountryPlayer = null;
	
	@Resource(name="mongoTemplate")
	private MongoTemplate mongoTemplate;
	@Resource(name="mongoHandicapTemplate")
	private MongoTemplate mongoHandicapTemplate;

	private LockWeakString lockSetLocation = new LockWeakString();
	
	private LockWeakString lockPlayerBonusQuiz = new LockWeakString();

    @Resource(name="teamMgr")
    private TeamMgr teamMgr = null;
    private PlayerCacheMgr playerCacheMgr = null;
    private Scheduler scheduler;
    private boolean communityDataTaskRunning = false;
    private CommunityDataTask communityTask = new CommunityDataTask();
    private String handicapCurrentPeriodID;
    private int countHandicapCurrentPeriodID;

	/**
	 * Call by spring on initialisation of bean
	 */
	@PostConstruct
	@Override
	public void init() {
		getHandicapCurrentPeriodID();
	}
	
	@Override
	public void startUp() {
	    playerCacheMgr = ContextManager.getPlayerCacheMgr();
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        try {
            scheduler = schedulerFactory.getScheduler();
            // start community timer - every day at 0h05
            JobDetail jobCommunity = JobBuilder.newJob(communityTask.getClass()).withIdentity("communityDataTask", "Player").build();
            CronTrigger triggerCommunity = TriggerBuilder.newTrigger().withIdentity("triggerCommunityDataTask", "Player").withSchedule(CronScheduleBuilder.cronSchedule("0 5 0 * * ?")).build();
			Date dateNextJobProcess = scheduler.scheduleJob(jobCommunity, triggerCommunity);
            log.warn("CommunityDataTask - Sheduled for job=" + jobCommunity.getKey() + " run at="+dateNextJobProcess+" - cron expression=" + triggerCommunity.getCronExpression() + " - next fire=" + triggerCommunity.getNextFireTime());
            // run community to init value ...
            communityTask.execute(null);

        } catch (Exception e) {
            log.error("Exception to start BirthdayTask or CommunityDataTask", e);
        }
	}
	
	@PreDestroy
	@Override
	public void destroy() {
	    log.warn("destroy playerMgr");
		listPasswordFr.clear();
		listPasswordEn.clear();
	}

    public long getDateNextBirthdayTaskScheduler() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerBirthdayTask", "Player"));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    public long getDateNextCommunityTaskScheduler() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerCommunityDataTask", "Player"));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

	public Player getPlayer(long id) {
		return playerDAO.getPlayer(id);
	}

	public enum PlayerUpdateType {
		PROFILE("PROFILE"),
		PROFILE_CREDIT("PROFILE_CREDIT"),
		CREDIT_DEAL("CREDIT_DEAL"),
		CREDIT("CREDIT");
		private String msg;
		PlayerUpdateType(String txt) {
			this.msg = txt;
		}
		public String toString() {return msg;}
	}
	
	/**
	 * Update device data to DB
	 * @param d
	 */
	@Transactional
	public Device updateDeviceToDB(Device d) {
		if (d != null) {
			return deviceDAO.updateDevice(d);
		}
		return null;
	}

    /**
     * Update playerDevice data to DB
     * @param pd
     */
    @Transactional
    public PlayerDevice updatePlayerDeviceToDB(PlayerDevice pd) {
        if (pd != null) {
            return playerDeviceDAO.updatePlayerDevice(pd);
        }
        return null;
    }

    public PlayerDAOJpa getPlayerDAO() {
        return playerDAO;
    }

    public void setCommunityNbActivePlayers(int communityNbActivePlayers) {
        this.communityNbActivePlayers = communityNbActivePlayers;
    }

    public void setCommunityNbCountryCode(int communityNbCountryCode) {
        this.communityNbCountryCode = communityNbCountryCode;
    }

    public List<WSCommunityCountryPlayer> getCommunityListCountryPlayer() {
        return communityListCountryPlayer;
    }

    public void setCommunityListCountryPlayer(List<WSCommunityCountryPlayer> communityListCountryPlayer) {
        this.communityListCountryPlayer = communityListCountryPlayer;
    }

    public boolean isCommunityDataTaskRunning() {
        return communityDataTaskRunning;
    }

    public void setCommunityDataTaskRunning(boolean communityDataTaskRunning) {
        this.communityDataTaskRunning = communityDataTaskRunning;
    }

	/**
	 * Update the player object to database. To use after change on player. Serie player is not updated. Used existing player serie value.
	 * Never called this method from independant thread ! Only from managers of service => from client action.
	 * @param p
	 */
	@Transactional
	public void updatePlayerToDB(Player p, PlayerUpdateType updateType) {
		if (p != null) {
			Player pOri = playerDAO.getPlayer(p.getID());
            if (pOri == null) {
                log.error("No player found with this ID="+p.getID());
            } else {
                boolean bUpdate = true;
                switch (updateType) {
                case PROFILE:
                    pOri.updateProfileFields(p);
                    break;
                case PROFILE_CREDIT:
                    pOri.updateProfileFields(p);
                    pOri.updateCreditFields(p);
                    break;
                case CREDIT_DEAL:
                    pOri.updateCreditFields(p);
                    pOri.updateDealFields(p);
                    break;
                case CREDIT:
                    pOri.updateCreditFields(p);
                    break;
                default:
                    bUpdate = false;
                    break;
                }

                if (bUpdate) {
                    p = pOri;
                    if (log.isDebugEnabled()) {
                        log.debug("Update player data in DB for player=" + p.toString() + " - updateType=" + updateType.toString());
                    }
                    playerDAO.updatePlayer(p);
                } else {
                    log.error("Update type not valid for player="+p.toString()+" - updateType="+updateType.toString());
                }
            }
		}
	}

    /**
	 * Return the player with mail or pseudo = login
	 * @param login
	 * @return
	 */
	public Player getPlayerByLogin(String login) {
		Player p = getPlayerByMail(login);
		if (p == null) {
			p = getPlayerByPseudo(login);
		}
		return p;
	}
	
	/**
	 * Return player with this mail
	 * @param mail
	 * @return
	 */
	public Player getPlayerByMail(String mail) {
		return playerDAO.getPlayerByMail(mail);
	}

	/**
	 * Return player with this cert
	 * @param cert
	 * @return
	 */
	public Player getPlayerByCert(String cert) {
		return playerDAO.getPlayerByCert(cert);
	}
	
	/**
	 * Return player with this pseudo
	 * @param pseudo
	 * @return
	 */
	public Player getPlayerByPseudo(String pseudo) {
		return playerDAO.getPlayerByNickname(pseudo);
	}

	@Transactional
    public boolean createPlayerWithNativeMethod(long playerID, String cert,int type, String mail, String password, String pseudo, String lang, String displayLang, String countryCode, long creationDate) throws FBWSException {
	    return playerDAO.insertPlayerWithNativeMethod(playerID, cert ,type , mail, password, pseudo, lang, displayLang, countryCode, creationDate);
    }

    public boolean createPlayerArgine() throws FBWSException{
	    if (getPlayer(Constantes.PLAYER_ARGINE_ID) == null) {
            long tsCreationDate = 0;
            try {
                tsCreationDate = Constantes.stringDate2Timestamp("21/06/2017");
            } catch (Exception e) {}
	        return ContextManager.getPlayerMgr().createPlayerWithNativeMethod(Constantes.PLAYER_ARGINE_ID,"00000000000" ,0,"argine@goto-games.com", "Arg1n8", "Argine", "fr", "fr", Constantes.PLAYER_FUNBRIDGE_COUNTRY, tsCreationDate);
        }
        return false;
    }

    public Player getPlayerArgine() throws FBWSException {
	    Player p = getPlayer(Constantes.PLAYER_ARGINE_ID);//-2
	    if (p == null) {
	        log.error("No player Argine define");
	        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        return p;
    }

	/**
	 * Create a normal player
	 * @param mail
	 * @param password
	 * @param pseudo
	 * @param lang
	 * @param displayLang
	 * @return
	 * @throws FBWSException
	 */
	@Transactional
	public synchronized Player createPlayer2(String mail, String password, String pseudo, String lang, String displayLang) throws FBWSException {
		try {

			if (existMail(mail)) {
				if (log.isDebugEnabled()) {
					log.debug("mail already used : mail=" + mail);
				}
				throw new FBWSException(FBExceptionType.PRESENCE_ALREADY_USED_LOGIN);
			}
			if (existPseudo(pseudo)) {
				if (log.isDebugEnabled()) {
					log.debug("Pseudo already used : pseudo=" + pseudo);
				}
				throw new FBWSException(FBExceptionType.PRESENCE_ALREADY_USED_PSEUDO);
			}

			long currentDate = System.currentTimeMillis();
			Player p = new Player();
			p.setMail(mail);
			p.setPassword(PlayerUtilities.cryptPassword(password));
			p.setNickname(pseudo);
			p.setCreationDate(currentDate);
			p.setCreditAmount(Constantes.START_HANDS);
			p.setDayCreditAmount(Constantes.Day_credit);
			p.setLastConnectionDate(currentDate);
			p.setLang(lang);
			if(displayLang == null) displayLang = generateDisplayLang(lang);
			p.setDisplayLang(displayLang.toLowerCase());
			if (!playerDAO.add(p)) {
				log.error("Error to add new player mail="+mail+" - pseudo="+pseudo+" - lang="+lang);
				return null;
			}
			return p;
		} catch (FBWSException e) {
			throw e;
		} catch (Exception e) {
			log.error("Create player fail ! mail="+mail+" - pseudo="+pseudo,e);
			return null;
		}
	}

	/**
	 * Create a normal player
	 * @param cert
	 * @param password
	 * @param pseudo
	 * @param lang
	 * @param displayLang
	 * @return
	 * @throws FBWSException
	 */
	@Transactional
	public synchronized Player createPlayerForWX(String cert, int type , String password, String pseudo, String lang, String displayLang) throws FBWSException {
		try {

			if (existCert(cert)) {
				if (log.isDebugEnabled()) {
					log.debug("cert already used : login=" + cert);
				}
				throw new FBWSException(FBExceptionType.PRESENCE_ALREADY_USED_LOGIN);
			}
			if (existPseudo(pseudo)) {
				if (log.isDebugEnabled()) {
					log.debug("Pseudo already used : pseudo=" + pseudo);
				}
				throw new FBWSException(FBExceptionType.PRESENCE_ALREADY_USED_PSEUDO);
			}

			long currentDate = System.currentTimeMillis();
			Player p = new Player();
			p.setCert(cert);
			p.setType(type);
			p.setMail(cert+"@cbo.com");
			p.setPassword(PlayerUtilities.cryptPassword(password));
			p.setNickname(pseudo);
			p.setCreditAmount(Constantes.START_HANDS);
			p.setDayCreditAmount(Constantes.Day_credit);
			p.setCreationDate(currentDate);
			p.setLastConnectionDate(currentDate);
			p.setLang(lang);
			if(displayLang == null) displayLang = generateDisplayLang(lang);
			p.setDisplayLang(displayLang.toLowerCase());
			if (!playerDAO.add(p)) {
				log.error("Error to add new player cert="+cert+" - pseudo="+pseudo+" - lang="+lang);
				return null;
			}
			return p;
		} catch (FBWSException e) {
			throw e;
		} catch (Exception e) {
			log.error("Create player fail ! cert="+cert+" - pseudo="+pseudo,e);
			return null;
		}
	}

    @Transactional
	public List<Player> createPlayersTest() {
	    List<Player> listPlayers = new ArrayList<>();
	    if (FBConfiguration.getInstance().getIntValue("general.devMode", 0) == 1) {
            int nbPlayersTest = FBConfiguration.getInstance().getIntValue("player.test.nbPlayers", 100);
            long startID = FBConfiguration.getInstance().getLongValue("player.test.startID", 1000);
            int idxPlayer = 0;
            while (listPlayers.size() < nbPlayersTest) {
                String mail = "test" + (startID + idxPlayer) + "@test.fr";
                String pseudo = "test_" + (startID + idxPlayer);
                if (!existMail(mail) && !existPseudo(pseudo)) {
                    Player p = new Player();
                    p.setMail(mail);
                    p.setPassword("test");
                    p.setNickname(pseudo);
                    p.setCreationDate(System.currentTimeMillis());
                    p.setLastConnectionDate(System.currentTimeMillis());
                    p.setLang("fr");
                    p.setDisplayLang(generateDisplayLang("fr").toLowerCase());
                    p.setCountryCode("FR");
                    p.setDisplayCountryCode("FR");
                    if (playerDAO.add(p)) {
                        listPlayers.add(p);
                    }
                }
                idxPlayer++;
            }
        } else {
	        log.error("No general.devMode enable");
        }
        return listPlayers;
    }

    public List<Player> listPlayersTest() {
	    List<Player> players =  playerDAO.searchPlayerWithPseudo("test_%");
	    Iterator<Player> it = players.iterator();
	    while (it.hasNext()) {
	        Player p = it.next();
	        if (!p.getPassword().equals("test") || !(p.getMail().startsWith("test") && p.getMail().endsWith("@test.fr"))) {
	            it.remove();
            }
        }
        return players;
    }

    public List<PlayerHandicap> createPlayerHandicapForTest(List<Player> playersTest) {
        List<PlayerHandicap> listPlayerHandicap = new ArrayList<>();
        if (FBConfiguration.getInstance().getIntValue("general.devMode", 0) == 1) {
            Random random = new Random(System.nanoTime());
            for (Player p :playersTest) {
                if (getPlayerHandicap(p.getID()) == null) {
                    PlayerHandicap ph = new PlayerHandicap();
                    double handicap = (double) (random.nextInt(1000) - 500) / 1000; // handicap must be between -0.5 and 0.5
                    ph.setPlayerId(p.getID());
                    ph.setHandicap(handicap);
                    ph.setNbDeals(201);
                    listPlayerHandicap.add(ph);
                }
            }
            if (listPlayerHandicap.size() > 0) {
                mongoHandicapTemplate.insertAll(listPlayerHandicap);
            }
        } else {
            log.error("No general.devMode enable");
        }
        return listPlayerHandicap;
    }



	/**
	 * Set the flag to enable/disable the newsletter for a player
	 * @param plaID
	 * @param email mail of player identify by plaID. If check failed => operation failed
	 * @param enable
	 * @return true if operation success or false
	 */
	public boolean setPlayerNewsletter(long plaID, String email, boolean enable) {
		try {
			Player p = getPlayer(plaID);
			if (p.getMail().equalsIgnoreCase(email)) {
				ContextManager.getPlayerMgr().updatePlayerToDB(p, PlayerUpdateType.PROFILE);
				return true;
			} else {
				log.error("Player email not valid : in base="+p.getMail()+" - and not "+email);
			}
		} catch (Exception e) {
			log.error("Exception to set player newsletter plaID="+plaID+" - email="+email+" - enable="+enable, e);
		}
		return false;
	}
	
	/**
	 * Set the flag to enable/disable mail options for player
	 * @param plaID
	 * @param email
	 * @param newsletter
	 * @param report
	 * @param birthday
	 * @return
	 */
	public boolean setPlayerMailEnable(long plaID, String email, boolean newsletter, boolean report, boolean birthday) {
		try {
			Player p = getPlayer(plaID);
			if (p.getMail().equalsIgnoreCase(email)) {
				ContextManager.getPlayerMgr().updatePlayerToDB(p, PlayerUpdateType.PROFILE);
				return true;
			} else {
				log.error("Player email not valid : in base="+p.getMail()+" - and not "+email);
			}
		} catch (Exception e) {
			log.error("Exception to set player mail enable plaID="+plaID+" - email="+email+" - newsletter="+newsletter+" - report="+report+" - birthday="+birthday, e);
		}
		return false;
	}
	
	/**
	 * Return the list of device for this player
	 * @param plaID
	 * @return
	 */
	public List<Device> getListDeviceForPlayer(long plaID) {
		return playerDeviceDAO.getListDeviceForPlayer(plaID);
	}

	public Device getLastDeviceUsedForPlayer(long playerID) {
        PlayerDevice playerDevice = playerDeviceDAO.getLastPlayerDeviceForPlayer(playerID);
        if (playerDevice != null) {
            return playerDevice.getDevice();
        }
        return null;
    }

    /**
     * Return list of playerDevice for this player order by date last used DESC
     * @param plaID
     * @return
     */
    public List<PlayerDevice> getListPlayerDeviceForPlayer(long plaID) {
        return playerDeviceDAO.getListPlayerDeviceForPlayer(plaID);
    }

	/**
	 * Return the number of device for a player
	 * @param plaID
	 * @return
	 */
	public int countDeviceForPlayer(long plaID) {
		return playerDeviceDAO.countDeviceForPlayer(plaID);
	}
	
	/**
	 * Return the number of player for a device
	 * @param deviceID
	 * @return
	 */
	public int countPlayerForDevice(long deviceID) {
		return playerDeviceDAO.countPlayerForDevice(deviceID);
	}
	
	/**
	 * Return the list of player for this devices
	 * @param deviceID
	 * @return
	 */
	public List<Player> getListPlayerForDevice(long deviceID) {
		return playerDeviceDAO.getListPlayerForDeviceID(deviceID);
	}

	/**
	 * Return the device associated to this deviceID
	 * @param deviceID
	 * @return
	 */
	public Device getDevice(long deviceID) {
		return deviceDAO.getDevice(deviceID);
	}
	
	/**
	 * Retrieve device for string ID
	 * @param strID
	 * @return
	 */
	public Device getDeviceForStrID(String strID) {
		return deviceDAO.getDevice(strID);
	}
	
	/**
	 * Create a device. No test of existing device !
	 * @param deviceID
	 * @param deviceInfo
	 * @param deviceType
	 * @param lang
	 * @param clientVersion
     * @param playerID
	 * @return
	 * @throws FBWSException 
	 */
	@Transactional
	public Device createDevice(String deviceID, String deviceInfo, String deviceType, String lang, String clientVersion, long playerID, int bonusFlag) throws FBWSException {
		Device dev = deviceDAO.createDevice(deviceID, deviceType, deviceInfo, lang, clientVersion, playerID, bonusFlag);
		if (dev == null) {
			log.error("Error to create device for deviceID="+deviceID+" - type="+deviceType+" - info="+deviceInfo);
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		return dev;
	}
	
	/**
	 * Create a link between player and device. No check of existing link ! 
	 * @param p
	 * @param d
	 * @return
	 */
	@Transactional
	public PlayerDevice linkPlayerDevice(Player p, Device d) {
		return playerDeviceDAO.linkPlayerDevice(p, d);
	}

    /**
     * Return existing device with this deviceID or create a new one with these parameters
     * @param deviceID
     * @param deviceType
     * @param deviceInfo
     * @param clientVersion
     * @param lang
     * @return
     * @throws FBWSException
     */
	public Device findOrCreateDevice(String deviceID, String deviceType, String deviceInfo, String clientVersion, String lang) throws FBWSException{
	    synchronized (lockDevice.getLock(deviceID)) {
            Device dev = deviceDAO.getDevice(deviceID);
            if (dev == null) {
                // device not existing => create it
                dev = ContextManager.getPlayerMgr().createDevice(deviceID,
                        deviceInfo, deviceType,
                        lang, clientVersion, 0, 0);
            }
            return dev;
        }
    }

	/**
	 * Associated device to player. Create a new device if necessary
	 * @param connectionData
	 * @param p
	 * @return
	 * @throws FBWSException
	 */
	public Device setDeviceForPlayer2(ConnectionData connectionData, Player p) throws FBWSException {
		synchronized (lockDevice.getLock(connectionData.deviceID)) {
			Device dev = deviceDAO.getDevice(connectionData.deviceID);
			if (dev == null) {
				// device not existing => create it
				dev = ContextManager.getPlayerMgr().createDevice(connectionData.deviceID, 
						connectionData.deviceInfo, connectionData.deviceType,
						connectionData.displayLang, connectionData.clientVersion, p.getID(), 0);
			} else {
                dev.setDeviceInfo(connectionData.deviceInfo);
                dev.setType(connectionData.deviceType);
                dev.setLang(connectionData.displayLang);
                dev.setClientVersion(connectionData.clientVersion);
                dev.setDateLastConnection(System.currentTimeMillis());
                dev.setLastPlayerID(p.getID());

                dev = ContextManager.getPlayerMgr().updateDeviceToDB(dev);
			}
			
			if (dev == null) {
				log.error("No valid device !");
				throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
			}
			
			// check if player & device are linked
			PlayerDevice pd = playerDeviceDAO.getPlayerDevice(p.getID(), dev.getID());
			if (pd == null) {
				PlayerDevice pl = ContextManager.getPlayerMgr().linkPlayerDevice(p, dev);
				if (pl == null) {
					log.error("Error to create link ! player=" + p.getID() + " - device=" + dev.getID());
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
			} else {
				pd.setDateLastConnection(System.currentTimeMillis());
				ContextManager.getPlayerMgr().updatePlayerDeviceToDB(pd);
			}
			return dev;
		}
	}
	
	/**
	 * Update the player account for lang, country and date last connection
	 * @param playerID
	 * @param lang
	 * @param country
	 * @param dateLastConnection
	 */
	@Transactional
	public Player updatePlayerOnConnection(long playerID, String lang, String displayLang, String country, long dateLastConnection) {
		Player p = playerDAO.getPlayer(playerID);
		if (lang != null && country != null) {
			if (lang.equals(Constantes.VALUE_TEST) || country.equals(Constantes.VALUE_TEST)) {
				// value test are not recorded !
				return p;
			}
		}
		if (p != null) {
			p.setLang(lang);
			if(displayLang == null || displayLang.isEmpty()) displayLang = generateDisplayLang(lang);
			p.setDisplayLang(displayLang.toLowerCase());
			p.setLastConnectionDate(dateLastConnection);

			p.setCountryCode(country);
            // update the display country code only at the first time (country code is empty by default)
			if (p.getDisplayCountryCode() == null || p.getDisplayCountryCode().length() == 0) {
			    p.setDisplayCountryCode(country);
            }
		} else {
			log.error("No player found for id="+playerID);
		}
		return p;
	}

	/**
	 * Return list of player with MailOrPseudo containing the expected value
	 * @param value
	 * @return
	 */
	public List<Player> searchPlayerWithMailOrPseudo(String value) {
		if (value.length() < 3) {
			return null;
		}
		return playerDAO.searchPlayerWithMailOrPseudo(value, 100, FBConfiguration.getInstance().getIntValue("player.searchUseIndexFullText", 1) == 1);
	}

    /**
     * Return list of player with pseudo, name or firstname started wuth the expected value
     * @param value
     * @return
     */
    public List<Player> searchPlayerStartingPseudoOrName(String value) {
        if (value.length() < 3) {
            return null;
        }
        return playerDAO.searchPlayerStartingPseudoOrName(value, 100, FBConfiguration.getInstance().getIntValue("player.searchUseIndexFullText", 1) == 1);
    }
	
	/**
	 * Return nb player
	 * @return
	 */
	public int getNbPlayer() {return deviceDAO.getNbDevice();}
	
	/**
	 * Return nb player authentified
	 * @return
	 */
	public int getNbPlayerAuth() {return playerDAO.getNbPlayerAuth();}

	/**
     * Create a connection history associated to this session
     * @param fbs
     * @param dateLogout
     * @param insertDB
     */
    public PlayerConnectionHistory2 createConnectionHistory2(FBSession fbs, long dateLogout, boolean insertDB) {
        if (fbs != null) {
            PlayerConnectionHistory2 e = new PlayerConnectionHistory2();
            e.setPlayerID(fbs.getPlayer().getID());
            e.setClientVersion(fbs.getClientVersion());
            e.setDateLogin(fbs.getDateCreation());
            e.setDateLogout(dateLogout);
            e.setDeviceID(fbs.getDevice().getID());
            e.setDeviceInfo(fbs.getDevice().getDeviceInfo());
            e.setDeviceType(fbs.getDevice().getType());
            e.setLang(fbs.getPlayer().getLang());
            e.setCountry(fbs.getPlayer().getCountryCode());
            e.setNbDealPlayed(fbs.getNbDealPlayed());
            e.setProtocol(fbs.protocol);
            e.setDeviceCreationDate(fbs.getDevice().getDateCreation());
            e.setCreationDateISO(new Date());
            e.setMapCategoryPlay(fbs.getMapCategoryPlay());
            e.setMapCategoryReplay(fbs.getMapCategoryReplay());
            e.setNbDealReplayed(fbs.getNbDealReplay());
            e.setPlayerActions(fbs.getPlayerActions());
            e.setFreemium(fbs.isFreemium());
            e.setAvatarPresent(fbs.isAvatarPlayerPresent());
            e.setPlayerInTeam(fbs.isPlayerInTeam());
            e.setPlayerSerie(fbs.getSerie());
            e.setNbFriend(fbs.getNbFriends());
            e.setNbFollowing(fbs.getNbFollowing());
            e.setNbFollowers(fbs.getNbFollowers());
            e.setNbCallStoreGetProducts(fbs.getNbCallStoreGetProducts());
            e.setTotalNbDealPlayed(fbs.getPlayer().getNbPlayedDeals());
            e.setCreditDeal(fbs.getPlayer().getTotalCreditAmount());
            e.setNbMessagesSent(fbs.getNbMessagesSent());
            if (fbs.getPlayer().getSubscriptionExpirationDate() > 0) {
                long tsSubscriptionRemaining = fbs.getPlayer().getSubscriptionExpirationDate() - System.currentTimeMillis();
                e.setCreditSubscriptionDay((int)(tsSubscriptionRemaining/(1000*60*60*24)));
            }
            PlayerCache playerCache = playerCacheMgr.getPlayerCache(fbs.getPlayer().getID());
            if (playerCache != null) {
                e.setHandicap(playerCache.handicap);
            }
            e.setApplicationStats(fbs.getApplicationStats());
            e.setNbLearningDeals(fbs.getNbLearningDeals());
            e.setNursing(fbs.getNursing());
            if (insertDB) {
                long ts = System.currentTimeMillis();
                mongoTemplate.insert(e);
                if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
                    log.error("Mongo insert too long ! ts=" + (System.currentTimeMillis() - ts));
                }
            }
            return e;
        }
        return null;
    }
	
	/**
     * Persist in DB list of connection history
     * @param listConHist
     */
    public void saveDBListConnectionHistory2(List<PlayerConnectionHistory2> listConHist) {
        if (listConHist != null && listConHist.size() > 0) {
            long ts = System.currentTimeMillis();
            mongoTemplate.insertAll(listConHist);
            if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
                log.error("Mongo insert too long ! ts=" + (System.currentTimeMillis() - ts + " - list size=" + listConHist.size()));
            }
        }
    }
	
	/**
     * Return the connection historic for player
     * @param plaID
     * @param offset
     * @param nbMax
     * @return
     */
    public List<PlayerConnectionHistory2> getListConnectionHistory2(long plaID, int offset, int nbMax) {
        Query q = new Query();
        q.addCriteria(Criteria.where("playerID").is(plaID));
        q.limit(nbMax).skip(offset);
        q.with(new Sort(Sort.Direction.DESC, "dateLogin"));
        return mongoTemplate.find(q, PlayerConnectionHistory2.class);
    }
	
	/**
	 * Check a player with mail exist (like)
	 * @param mail
	 * @return
	 */
	public boolean existMail(String mail) {
		List<Player> l = playerDAO.searchPlayerWithMail(mail);
		return l != null && l.size() != 0;
	}

	/**
	 * Check a player with cert exist (like)
	 * @param cert
	 * @return
	 */
	public boolean existCert(String cert) {
		List<Player> l = playerDAO.searchPlayerWithCert(cert);
		return l != null && l.size() != 0;
	}
	
	/**
	 * Check if a player with pseudo exist
	 * @param pseudo
	 * @return
	 */
	public boolean existPseudo(String pseudo) {
		Player p = playerDAO.getPlayerByNickname(pseudo.trim());
		return p != null;
	}

	public String generateDisplayLang(String lang){
		String displayLang = Constantes.PLAYER_LANG_EN;
		if(lang != null){
			lang = lang.toLowerCase();
			lang = lang.replaceAll("-", "_");
			if(ContextManager.getTextUIMgr().isLangSupported(lang)){
				displayLang = lang;
			} else if(lang.contains("_")){
				lang = lang.substring(0, lang.lastIndexOf("_"));
				if(ContextManager.getTextUIMgr().isLangSupported(lang)){
					displayLang = lang;
				}
			}
		}
		return displayLang;
	}

	/**
	 * Check the credit for player and set free credit and bonus if necessary : welcome, birthday
	 *
	 */
	public void updateCreditOnConnection(FBSession s, boolean firstConnection) {
		if (s != null) {
			Player p = s.getPlayer();
			boolean bUpdate = false;

			// update player data
			if (bUpdate) {
				ContextManager.getPlayerMgr().updatePlayerToDB(p, PlayerUpdateType.CREDIT);
			}
		} else {
			log.error("Session is null !");
		}
	}
	
	/**
	 * Check if credit of player is >= amount. Needed to play a deal !
	 * @param p
	 * @param amount
	 * @throws FBWSException
	 */
	public void checkPlayerCredit(Player p, int amount) throws FBWSException {
		if (p != null) {
			// if no subcription, check credit amount
			if (!p.isDateSubscriptionValid()) {
				// credit is empty
				if (p.getTotalCreditAmount() == 0) {
                    if (log.isDebugEnabled()) {
                        log.debug("Credit not enough for player=" + p.getID() + " - credit=" + p.getTotalCreditAmount() + " - amount=" + amount);
                    }
					throw new FBWSException(FBExceptionType.GAME_PLAYER_CREDIT_EMPTY);
				}
				// check if player has credit to play !
				if (p.getTotalCreditAmount() < amount) {
                    if (log.isDebugEnabled()) {
                        log.debug("Credit not enough for player=" + p.getID() + " - credit=" + p.getTotalCreditAmount() + " - amount=" + amount);
                    }
					throw new FBWSException(FBExceptionType.GAME_PLAYER_CREDIT_NOT_ENOUGH);
				}
			}
		} else {
			log.error("Player is null !");
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Return the file path for playerID. If directory not existing in filesystem, create it
	 * @param playerID
	 * @return
	 */
	public String getPlayerFilePath(long playerID) {
		String path = FBConfiguration.getInstance().getStringResolvEnvVariableValue("player.file.path", "");
		if (path != null && path.length() > 0) {
			path = FilenameUtils.concat(path, ""+playerID);
			File filePath = new File(path);
			if (!filePath.exists()) {filePath.mkdirs();}
			return path;
		}
		return null;
	}
	
	/**
	 * Generate random password according to player language
	 * @return
	 */
	public String generateRandomPassword(String plaLang) {
		boolean bGenOk = false;
		String randomStr = "";
		Random rd = new Random(System.currentTimeMillis());
		if (plaLang.equalsIgnoreCase("fr")) {
			if (listPasswordFr.size() > 0) {
				randomStr = listPasswordFr.get(rd.nextInt(listPasswordFr.size()));
				bGenOk = true;
			}
		}
		else {
			if (listPasswordEn.size() > 0) {
				randomStr = listPasswordEn.get(rd.nextInt(listPasswordEn.size()));
				bGenOk = true;
			}
		}
		if (!bGenOk) {
			randomStr = RandomTools.generateRandomString(6);
			randomStr += ""+rd.nextInt(100);
		}
		return randomStr.toLowerCase();
	}
	
	/**
	 * Set inactiveApns flag to true for each device with token in list
	 * @param tokenInactiveDevices
	 */
	@Transactional
	public int updateInactiveDevices(List<String> tokenInactiveDevices) {
		int nbDeviceUpdate = 0;
		if (tokenInactiveDevices != null) {
			nbDeviceUpdate = deviceDAO.updateInactiveApns(tokenInactiveDevices);
		} else {
			log.error("inactiveDevices is null");
		}
		return nbDeviceUpdate;
	}

	/**
	 * Return list of partner for player
	 * @param playerID
	 * @param offset
	 * @param nbMax
	 * @return
	 */
	public List<WSPlayerLinked> getListTrainingPartner(long playerID, int offset, int nbMax) {
		List<WSPlayerLinked> listResult = playerLinkDAO.listWSPlayerLinkedFriend(playerID, 0, 0);
		if (listResult != null) {
			PresenceMgr presenceMgr = ContextManager.getPresenceMgr();
			for (WSPlayerLinked e : listResult) {
				FBSession s = presenceMgr.getSessionForPlayerID(e.playerID);
				if (s != null) {
						e.connected = true;
				}
			}
		}
		return listResult;
	}
	
	/**
	 * Return list of WSPlayerLinked for a player and a link type.
	 * @param playerID
	 * @param type
	 * @param offset
	 * @param nbMax
	 * @return
	 */
	public List<WSPlayerLinked> getListLinkedForType(long playerID, int type, int offset, int nbMax) {
		List<WSPlayerLinked> listResult = null;
		if (type == Constantes.PLAYER_LINK_TYPE_FRIEND) {
			listResult = playerLinkDAO.listWSPlayerLinkedFriend(playerID, 0, 0);
			if (listResult != null) {
				PresenceMgr presenceMgr = ContextManager.getPresenceMgr();
				for (WSPlayerLinked e : listResult) {
					e.connected = presenceMgr.isSessionForPlayerID(e.playerID);
				}
			}
		}
		else if (type == Constantes.PLAYER_LINK_TYPE_FOLLOWER) {
			listResult = playerLinkDAO.listWSPlayerLinkedFollower(playerID, offset, nbMax);
			if (listResult != null) {
				PresenceMgr presenceMgr = ContextManager.getPresenceMgr();
				for (WSPlayerLinked e : listResult) {
					e.connected = presenceMgr.isSessionForPlayerID(e.playerID);
				}
			}
		}
		else if (type == Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) {
			listResult = playerLinkDAO.listWSPlayerLinkedPending(playerID, offset, nbMax);
		}
		return listResult;
	}

	/**
	 * Return the nb of link of type for this player
	 * @param playerID
	 * @param type
	 * @return
	 */
	public int countLinkForPlayerAndType(long playerID, int type) {
		if (type == Constantes.PLAYER_LINK_TYPE_FRIEND) {
			return playerLinkDAO.countLinkFriend(playerID);
		}
		if (type == Constantes.PLAYER_LINK_TYPE_FOLLOWER) {
			return playerLinkDAO.countLinkFollower(playerID);
		}
		if (type == Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) {
			return playerLinkDAO.countLinkFriendPending(playerID);
		}
		return 0;
	}

	/**
	 * Count link friend and followers for this player
	 * @param playerID
	 * @return
	 */
	public int countFriendAndFollowerForPlayer(long playerID) {
		return playerLinkDAO.countFriendAndFollower(playerID);
	}
	
	/**
	 * Nb players who have a link follower (and not friend link) to this player
	 * @param playerID
	 * @return
	 */
	public int countFollowerForPlayer(long playerID) {
		return playerLinkDAO.countFollowerForPlayer(playerID);
	}

	/**
	 * Nb players for this player having a link follower (and not friend link)
	 * @param playerID
	 * @return
	 */
	public int countFollowingForPlayer(long playerID) {
		return playerLinkDAO.countFollowingForPlayer(playerID);
	}
	
	/**
	 * Return list of playerID linked to this player with link follower or friend (include follower) 
	 * @param playerID
	 * @return
	 */
	public List<Long> getListPlayerIDLinkFollower(long playerID) {
		return playerLinkDAO.listPlayerIDLinkedFollower(playerID);
	}

    /**
	 * Return list of playerLinked with pseudo, firstname or lastname containing pattern OR mail = pattern
	 * @param playerID
	 * @param pattern
	 * @param type
	 * @param offset
	 * @param nbMax
	 * @return
	 */
	public List<WSPlayerLinked> searchPlayerLinked(long playerID, String pattern, int type, int offset, int nbMax) {
		return playerDAO.searchPlayerLinked(pattern, playerID, type, offset, nbMax, FBConfiguration.getInstance().getIntValue("player.searchUseIndexFullText", 1) == 1);
	}

    /**
	 * Search a list of playerID by pattern on pseudo and lastname and by countryCode (works even if pattern or countryCode is null)
	 * @param pattern
	 * @param countryCode
	 * @param offset
	 * @param nbMax
	 * @return
	 */
	public List<Long> searchPlayerID(String pattern, String countryCode, int offset, int nbMax) {
		return playerDAO.searchPlayerID(pattern, countryCode, offset, nbMax, FBConfiguration.getInstance().getIntValue("player.searchUseIndexFullText", 1) == 1);
	}

	/**
	 * Returns a list of playerID to suggest new team members to a captain. CAREFUL : they may be in a team already, we have to check it before suggesting them to the captain.
	 * @param playerID
	 * @param countryCode
	 * @param nbMax
	 * @return
	 */
	public List<Long> listSuggestedTeamMembers(long playerID, String countryCode, int nbMax) {
		// Prepare the list
		List<Long> suggestedTeamMembers = new ArrayList<>();
		// Players met in duels
		suggestedTeamMembers.addAll(playerDuelDAO.listOpponentsIDForPlayer(playerID, countryCode, nbMax / 2));
		// The friends' friends
		List<WSPlayer> friendsOfFriends = getSuggestionFriendsOfFriends(playerID, nbMax/2, null, countryCode);
		if(friendsOfFriends != null){
			for(WSPlayer friendOfFriend : friendsOfFriends){
				if(!suggestedTeamMembers.contains(friendOfFriend.playerID)){
					suggestedTeamMembers.add(friendOfFriend.playerID);
				}
			}
		}
		// If we didn't reach the nbMax yet, complete the list with players in the same series
		if(suggestedTeamMembers.size() < nbMax){
			// Get the PlayerCache to find serie
			PlayerCache pc = playerCacheMgr.getOrLoadPlayerCache(playerID);
			if(pc != null && !pc.serie.equalsIgnoreCase(TourSerieMgr.SERIE_NC)){
				List<TourSeriePlayer> sameSeriePlayers = ContextManager.getTourSerieMgr().listTourSeriePlayerForSerie(pc.serie, 0, nbMax, countryCode);
				for(TourSeriePlayer sameSeriePlayer : sameSeriePlayers){
					if(suggestedTeamMembers.size() >= nbMax){
						break;
					}
					if(!suggestedTeamMembers.contains(sameSeriePlayer.getPlayerID())){
						suggestedTeamMembers.add(sameSeriePlayer.getPlayerID());
					}
				}
			}
		}
		return suggestedTeamMembers;
	}
	
	/**
	 * Create link this two players
	 * @param p1
	 * @param p2
	 * @param type1 mask between P1 & P2
	 * @param type2 mask between P2 & P1
	 * @return
	 */
	@Transactional
	public PlayerLink createLinkBetweenPlayer(Player p1, Player p2, int type1, int type2, String message) {
		if (p1 != null && p2 != null && (type1 > 0 || type2 > 0)) {
            if (log.isDebugEnabled()) {
                log.debug("Create link between p1=" + p1.getID() + " - p2=" + p2.getID() + " - type1=" + type1 + " - type2=" + type2);
            }
			if (message == null) {
				message = "";
			}
            if (FBConfiguration.getInstance().getIntValue("player.linkMessageRemoveNonBMPCharacters", 1) == 1) {
                message = StringTools.removeNonBMPCharacters(message, "?");
            }
			return playerLinkDAO.addLinkPlayer(p1, p2, type1, type2, message);
		} else {
			log.error("Parameter not valid : p1="+p1+" - p2="+p2+" - type1="+type1+" - type2="+type2);
		}
		return null;
	}
	
	/**
	 * Remove a player link
	 * @param pl
	 * @return
	 */
	@Transactional
	public boolean removePlayerLink(PlayerLink pl) {
		if (pl != null) {
            if (log.isDebugEnabled()) {
                log.debug("Remove link between player1=" + pl.getPlayer1().getID() + " - player2=" + pl.getPlayer2().getID() + " - typeMask1=" + pl.getTypeMask1() + " - typeMask2=" + pl.getTypeMask2());
            }
			return playerLinkDAO.remove(pl.getID());
		} else {
			log.error("Parameter is null !");
		}
		return false;
	}
	
	/**
	 * Update player link
	 * @param pl
	 * @return
	 */
	@Transactional
	public PlayerLink updatePlayerLink(PlayerLink pl) {
		if (pl != null) {
            if (log.isDebugEnabled()) {
                log.debug("Update link between player1=" + pl.getPlayer1().getID() + " - player2=" + pl.getPlayer2().getID() + " - typeMask1=" + pl.getTypeMask1() + " - typeMask2=" + pl.getTypeMask2());
            }
			return playerLinkDAO.update(pl);
		} else {
			log.error("Parameter is null !");
		}
		return null;
	}
	
	/**
	 * Return link mask between 2 players
	 * @param player1
	 * @param player2
	 * @return if link exist, value for mask between P1 and P2, else 0 
	 */
	public int getLinkMaskBetweenPlayer(long player1, long player2) {
		PlayerLink pl = playerLinkDAO.getLinkBetweenPlayer(player1, player2);
		if (pl != null) {
			if (pl.getPlayer1().getID() == player1) {
				return pl.getLinkMaskFor1();
			} else {
				return pl.getLinkMaskFor2();
			}
		}
		return 0;
	}
	
	/**
	 * Return player link object between 2 players
	 * @param player1
	 * @param player2
	 * @return if link exist, value for mask between P1 and P2, else 0 
	 */
	public PlayerLink getLinkBetweenPlayer(long player1, long player2) {
		return playerLinkDAO.getLinkBetweenPlayer(player1, player2);
	}
	
	/**
	 * Return true if players are friend 
	 * @param player1
	 * @param player2
	 * @return
	 */
	public boolean isPlayerFriend(long player1, long player2) {
		PlayerLink pl = playerLinkDAO.getLinkBetweenPlayer(player1, player2);
		if (pl != null) {
			return pl.isLinkFriend();
		}
		return false;
	}

	/**
	 * Update link between 2 players
	 * @param p1 player who asked
	 * @param p2
	 * @param linkMask
	 * @param message
	 * @return the value of link mask after creation or update
	 * @throws FBWSException 
	 */
	public int updateLinkBetweenPlayer(Player p1, Player p2, int linkMask, String message) throws FBWSException {
		if (p1 != null && p2 != null) {
			synchronized (lockPlayerLink.getLock(getPlayerLinkKey(p1.getID(), p2.getID()))) {
				try {
                    if (log.isDebugEnabled()) {
                        log.debug("p1=" + p1.getID() + " - p2=" + p2.getID() + " - linkMask=" + linkMask);
                    }
					PlayerLink pl = playerLinkDAO.getLinkBetweenPlayer(p1.getID(), p2.getID());
					boolean bAddNewFriendPending = false;
					boolean bRemoveNewFriendPending = false;
					boolean bRemoveFriend = false;
					boolean bRequestAnswerOK = false;
					boolean bRequestAnswerKO = false;
					//----------------------------------------------
					// existing links between players
					if (pl != null) {
						//----------------------------------------------
						// link blocked => remove others link type
						if ((linkMask & Constantes.PLAYER_LINK_TYPE_BLOCKED) == Constantes.PLAYER_LINK_TYPE_BLOCKED) {
							// set link with type block (add on existing or create new)
							boolean changed = false;
							if (pl.addTypeMask(p1.getID(), Constantes.PLAYER_LINK_TYPE_BLOCKED)) {changed = true;}
							if (pl.removeTypeMask(Constantes.PLAYER_LINK_TYPE_FRIEND)) {changed = true;bRemoveFriend = true;}
							if (pl.removeTypeMask(Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING)) {changed = true;bRemoveNewFriendPending = true;}
							if (pl.removeTypeMask(Constantes.PLAYER_LINK_TYPE_FOLLOWER)) {changed = true;}

							if (changed) {
								ContextManager.getPlayerMgr().updatePlayerLink(pl);
							}
						}
						//----------------------------------------------
						// other link type
						else {
							// check if p2 has blocked p1
							if (pl.hasBlocked(p2.getID()) && linkMask != 0) {
                                if (log.isDebugEnabled()) {
                                    log.debug("P2 blocked P1 => reject link with mask=" + linkMask + " - p1=" + p1.getID() + " - p2=" + p2.getID());
                                }
								throw new FBWSException(FBExceptionType.PLAYER_LINK_BLOCKED);
							}
							boolean update = false;
							// delete blocked link ?
							if ((linkMask & Constantes.PLAYER_LINK_TYPE_BLOCKED) != Constantes.PLAYER_LINK_TYPE_BLOCKED) {
								update = update | pl.removeTypeMask(p1.getID(), Constantes.PLAYER_LINK_TYPE_BLOCKED);
							}
							// delete follower link ?
							if ((linkMask & Constantes.PLAYER_LINK_TYPE_FOLLOWER) != Constantes.PLAYER_LINK_TYPE_FOLLOWER) {
								update = update | pl.removeTypeMask(p1.getID(), Constantes.PLAYER_LINK_TYPE_FOLLOWER);
							}
                            // delete friend pending
                            if (linkMask == 0 && pl.hasTypeMask(p1.getID(), Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING)) {
                                update = update | pl.removeTypeMask(p1.getID(), Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING);
                            }
							// add follower link
							if ((linkMask & Constantes.PLAYER_LINK_TYPE_FOLLOWER) == Constantes.PLAYER_LINK_TYPE_FOLLOWER) {
								update = update | pl.addTypeMask(p1.getID(), Constantes.PLAYER_LINK_TYPE_FOLLOWER);
							}
							// add friend pending link
							if ((linkMask & Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) == Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) {
								// fix bug with android version (send FRIEND + FRIEND_PENDING) => nothing to do with 
								if ((linkMask & Constantes.PLAYER_LINK_TYPE_FRIEND) != Constantes.PLAYER_LINK_TYPE_FRIEND) {
									bAddNewFriendPending = pl.addTypeMask(p1.getID(), Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING);
									// check p1 has not too many friend !
									if (bAddNewFriendPending && countLinkForPlayerAndType(p1.getID(), Constantes.PLAYER_LINK_TYPE_FRIEND) >= Constantes.PLAYER_FRIEND_MAX) {
										throw new FBWSException(FBExceptionType.PLAYER_LINK_TOO_MANY);
									}
									update = update | bAddNewFriendPending;
								}
							}
							// add friend
							if ((linkMask & Constantes.PLAYER_LINK_TYPE_FRIEND) == Constantes.PLAYER_LINK_TYPE_FRIEND) {
								// check p1 has not too many friend !
								if (countLinkForPlayerAndType(p1.getID(), Constantes.PLAYER_LINK_TYPE_FRIEND) >= Constantes.PLAYER_FRIEND_MAX) {
									throw new FBWSException(FBExceptionType.PLAYER_LINK_TOO_MANY);
								}
								// friend pending must exist
								if (pl.hasTypeMask(p2.getID(), Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING)) {
									bRemoveNewFriendPending = true;
									update = update | pl.addTypeMask(Constantes.PLAYER_LINK_TYPE_FRIEND);
									update = update | pl.addTypeMask(Constantes.PLAYER_LINK_TYPE_FOLLOWER);
									update = update | pl.removeTypeMask(Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING);
									bRequestAnswerOK = true;
								}
							} else {
								if (pl.hasTypeMask(p2.getID(), Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING)) {
									update = update | pl.removeTypeMask(Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING);
									bRemoveNewFriendPending = true;
									bRequestAnswerKO = true;
								}
								if (pl.isLinkFriend()) {
									bRemoveFriend = true;
								}
								update = update | pl.removeTypeMask(Constantes.PLAYER_LINK_TYPE_FRIEND);
							}
							
							// update link
							// delete player link if mask is 0
							if (pl.getTypeMask1() == 0 && pl.getTypeMask2() == 0) {
								ContextManager.getPlayerMgr().removePlayerLink(pl);
							}
							else if (update) {
								if (bAddNewFriendPending) {
									if (message == null) {
										message = "";
									}
                                    if (FBConfiguration.getInstance().getIntValue("player.linkMessageRemoveNonBMPCharacters", 1) == 1) {
                                        message = StringTools.removeNonBMPCharacters(message, "?");
                                    }
									pl.setMessage(message);
								}
								if (ContextManager.getPlayerMgr().updatePlayerLink(pl) == null) {
									log.error("PlayerLink null after update pl="+pl);
									throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
								}
                                if (bAddNewFriendPending && message != null && message.length() > 0 && FBConfiguration.getInstance().getIntValue("player.sendMessageOnFriendPending", 1) == 1) {
                                    try {
                                        chatMgr.sendMessageToPlayer(p1, p2.getID(), message, null, null, null);
                                    } catch (FBWSException e) {
                                        log.error("Failed to send message to player p1="+p1+" - p2="+p2+" - message="+message, e);
                                    }
                                }
							}
						}
					}
					//----------------------------------------------
					// no existing links between players
					else {
						if (linkMask == 0) {
							log.error("Mask is 0 - p1="+p1+" - p2="+p2+" - linkMask="+linkMask);
							throw new FBWSException(FBExceptionType.PLAYER_LINK_NOT_VALID);
						}
						// mask friend not possible (need pending link before)
						if ((linkMask & Constantes.PLAYER_LINK_TYPE_FRIEND) == Constantes.PLAYER_LINK_TYPE_FRIEND) {
							log.error("No link and try to set linkn with mask=FRIEND - p1="+p1+" - p2="+p2+" - linkMask="+linkMask);
							throw new FBWSException(FBExceptionType.PLAYER_LINK_NOT_VALID);
						}
						// check linkMask value
						int temp = linkMask & (~Constantes.PLAYER_LINK_TYPE_BLOCKED);
						temp = temp & (~Constantes.PLAYER_LINK_TYPE_FOLLOWER);
						temp = temp & (~Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING);
						if (temp != 0) {
							log.error("Mask has not valid value - p1="+p1+" - p2="+p2+" - linkMask="+linkMask+" - temp="+temp);
							throw new FBWSException(FBExceptionType.PLAYER_LINK_NOT_VALID);
						}
						
						if ((linkMask & Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) == Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) {
							bAddNewFriendPending = true;
						}
						// check p1 has not too many friend !
						if (bAddNewFriendPending && countLinkForPlayerAndType(p1.getID(), Constantes.PLAYER_LINK_TYPE_FRIEND) >= Constantes.PLAYER_FRIEND_MAX) {
							throw new FBWSException(FBExceptionType.PLAYER_LINK_TOO_MANY);
						}
						
						// create link
						pl = ContextManager.getPlayerMgr().createLinkBetweenPlayer(p1, p2, linkMask, 0, message);
						if (pl == null) {
							throw new Exception("Player link return is null ! - p1="+p1+" - p2="+p2+" - linkMask="+linkMask);
						}
						if (message != null && message.length() > 0 && FBConfiguration.getInstance().getIntValue("player.sendMessageOnFriendPending", 1) == 1) {
                            try {
                                chatMgr.sendMessageToPlayer(p1, p2.getID(), message, null, null, null);
                            } catch (FBWSException e) {
                                log.error("Failed to send message to player p1="+p1+" - p2="+p2+" - message="+message, e);
                            }
                        }
					}
					
					boolean eventPlayerChangeLinkSend = false;
					// new friendPending => push event to player2
					if (bAddNewFriendPending) {
						// send event to recipient
						FBSession sessionP2 = ContextManager.getPresenceMgr().getSessionForPlayerID(p2.getID());
						if (sessionP2 != null) {
							// send event change link
							sessionP2.pushEvent(buildEventPlayerChangeLink(p2.getID(), p1.getID(), pl.getLinkMaskForPlayer(p2.getID())));
							eventPlayerChangeLinkSend = true;
						}
					}
					// remove friendPending => update session counter
					if (bRemoveNewFriendPending) {
						// send event change link
						if (!eventPlayerChangeLinkSend) {
							FBSession sessionP2 = ContextManager.getPresenceMgr().getSessionForPlayerID(p2.getID());
							if (sessionP2 != null) {
								sessionP2.pushEvent(buildEventPlayerChangeLink(p2.getID(), p1.getID(), pl.getLinkMaskForPlayer(p2.getID())));
								eventPlayerChangeLinkSend = true;
							}
						}
						
					}
					// remove friend => remove data between players
					if (bRemoveFriend) {
						// remove messages
						FBSession sessionP2 = ContextManager.getPresenceMgr().getSessionForPlayerID(p2.getID());
						if (sessionP2 != null) {
							// send event change link
							if (!eventPlayerChangeLinkSend) {
								sessionP2.pushEvent(buildEventPlayerChangeLink(p2.getID(), p1.getID(), pl.getLinkMaskForPlayer(p2.getID())));
								eventPlayerChangeLinkSend = true;
							}
						}
					}
					
					if (bRequestAnswerOK) {
						// send notif friend answer OK
						MessageNotif notif = notifMgr.createNotifFriendRequestAnswer(p1, p2, true);
						if (notif != null) {
							FBSession sessionP2 = ContextManager.getPresenceMgr().getSessionForPlayerID(p2.getID());
							if (sessionP2 != null) {
								sessionP2.pushEvent(notifMgr.buildEvent(notif, p2));
							}
						}
					}
					return pl.getLinkMaskForPlayer(p1.getID());
				} catch (FBWSException e) {
					throw e;
				} catch (Exception e) {
					log.error("Exception to create link for p1="+p1+" - p2="+p2+" - linkMask="+linkMask, e);
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
			}
		} else {
			log.error("Parameter p1 or p2 null");
			throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
		}
	}
	
	private String getPlayerLinkKey(long plaID1, long plaID2) {
		String plKey = "pl-";
		if (plaID1 < plaID2) {
			plKey += plaID1+"-"+plaID2;
		} else {
			plKey += plaID2+"-"+plaID1;
		}
		return plKey;
	}

    public PlayerSettingsData getPlayerSettingsData(Player p) {
        try {
            return jsontools.mapData(p.getSettings(), PlayerSettingsData.class);
        } catch (Exception e) {
            log.error("Error while parsing player settings - p="+p, e);
        }
        return null;
    }

	public PlayerHandicap getPlayerHandicap(long playerID){
		try{
			Query q = new Query();
			q.addCriteria(Criteria.where("playerId").is(playerID));
			return mongoHandicapTemplate.findOne(q, PlayerHandicap.class);
		} catch(Exception e){
			log.error("Error while trying to get PlayerHandicap for playerId="+playerID, e);
			return null;
		}
	}

	public List<PlayerHandicap> getPlayersHandicap(List<Long> listPlayerID) {
        try{
            Query q = new Query();
            q.addCriteria(Criteria.where("playerId").in(listPlayerID));
            return mongoHandicapTemplate.find(q, PlayerHandicap.class);
        } catch(Exception e){
            log.error("Error while trying to get PlayerHandicap for players", e);
            return null;
        }
    }
	
	/**
	 * Check pseudo format [\\p{L}\\d\\-_ #. ]* 4 to 32 characters
	 * @param pseudo
	 * @return
	 */
	public static boolean checkPseudoFormat(String pseudo) {
		if (pseudo.startsWith(" ") || pseudo.endsWith(" ")) {
			// pseudo starts or ends with ' '
			return false;
		}
		if (pseudo.length() < 4 || pseudo.length() > 32) {
			// pseudo too small or too long
			return false;
		}
		// pseudo include not valid character
		return pseudo.matches("[\\p{L}\\d\\-_ #. ]*");
	}
	
	/**
	 * Check password format [\\p{L}\\d\\-_ #. ]* 4 to 32 characters
	 * @param password
	 * @return
	 */
	public static boolean checkPasswordFormat(String password) {
		if (password.startsWith(" ") || password.endsWith(" ")) {
			// password starts or ends with ' '
			return false;
		}
		if (password.length() < 4 || password.length() > 32) {
			// password too small or too long
			return false;
		}
		// password include not valid character
		return password.matches("[\\p{L}\\d]*");
	}

	/**
	 * Check phone format
	 * @param phone
	 * @return
	 */
	public static boolean checkPhoneFormat(String phone) {
		if (phone.startsWith(" ") || phone.endsWith(" ")) {
			// password starts or ends with ' '
			return false;
		}
		if (phone.length() != 11) {
			// password too small or too long
			return false;
		}
		// password include not valid character
		return phone.matches("^1[358]\\d{9}$");
	}
	
	/**
	 * Check if mail format is valid. Format user@host. 
	 * Contain @ 
	 * user not empty 
	 * host not empty
	 * user & host without space char 
	 * host not ended with .
	 * host not started with .
	 * host without ..
	 * @param mail
	 * @return
	 */
	public static boolean checkMailFormat(String mail) {
		// structure of mail : user@host
		if (mail != null && mail.length() > 0) {
			int idxArobase = mail.indexOf("@");
			
			if (idxArobase > 0) {
				// check part user
				String user = mail.substring(0, idxArobase);
				if (user == null || user.length() == 0) {
					return false;
				}
				
				// check part host
				String host = mail.substring(idxArobase+1);
				if (host == null || host.length() == 0) {
					return false;
				}
				
				// check no white space
				if (user.indexOf(' ') >= 0) {
					return false;
				}
				if (host.indexOf(' ') >= 0) {
					return false;
				}
				
				// host not started or ended with .
				if (host.indexOf('.') == 0) {
					return false;
				}
				if (host.lastIndexOf('.') == host.length()) {
					return false;
				}
				
				// get index of last point
				int idxLastPoint = host.lastIndexOf(".");
				if (idxLastPoint >= 0) {
					// search if host ends with 2 points : ..com
					String temp = host.substring(0, idxLastPoint);
					return !temp.endsWith(".");
				} else {
					// no point char in the host
					return false;
				}
			} else {
				// no arobase
				return false;
			}
		} else {
			return false;
		}
	}
	
	public int getLockDeviceSize() {
		return lockDevice.size();
	}
	
	/**
	 * Return the file avatar for player. The file can not existing ! The return file must check if not null and existing.
	 * @param playerID
	 * @return 
	 */
	public File getAvatarFileForPlayer(long playerID) {
		String path = getPlayerFilePath(playerID);
		String filename = FBConfiguration.getInstance().getStringValue("player.avatar.filename", "");
		if (filename == null || filename.length() == 0) {
			filename = "avatar.jpg";
		}
		path = FilenameUtils.concat(path, filename);
		File fileAvatar = new File(path);
		return fileAvatar;
	}
	
	/**
	 * Remove the avatar file associated to player
	 * @param p
	 * @return
	 */
	public boolean removeAvatarFileForPlayer(Player p) {
		boolean result = false;
		if (p!= null) {
			if (p.isAvatarPresent()) {
				File fileAvatar = getAvatarFileForPlayer(p.getID());
				try {
					int idx = 1;
					File newFileAvatar = null;
					while (true) {
						newFileAvatar = new File(fileAvatar.getPath()+idx);
						if (newFileAvatar.exists()) {
							idx++;
						} else {
							break;
						}
					}
					fileAvatar.renameTo(newFileAvatar);
				} catch (Exception e) {
					log.error("Exception to rename avatar for player="+p.getID()+" - path="+fileAvatar.getPath(),e);
				}
				p.removeAvatar();
				p.setAvatarPresent(false);
				try {
					ContextManager.getPlayerMgr().updatePlayerToDB(p, PlayerUpdateType.PROFILE);
					result = true;
				} catch (Exception e) {
					log.error("Exception to update player in DB", e);
				}
			}
		} else {
			log.error("Param player is null");
		}
		return result;
	}
	
	public Object getLockForBonusQuiz(String playerLogin){
		return lockPlayerBonusQuiz.getLock(playerLogin);
	}

	/**
	 * Check if device is excluded for update
	 * @param strDeviceID (string id of device)
	 * @return true if update for this device is exclude
	 */
	public boolean isDeviceExcludeForUpdate(String strDeviceID) {
		List<String> listStrDeviceIDToExclude = FBConfiguration.getInstance().getList("player.strDeviceIDExcludeForUpdate");
		if (listStrDeviceIDToExclude != null && listStrDeviceIDToExclude.size() > 0) {
			return listStrDeviceIDToExclude.contains(strDeviceID);
		}
		return false;
	}

	/**
	 * Build WSProfile for player p for player asking is playerAsk
	 * @param p
	 * @param playerAsk
	 * @return
	 */
	public WSProfile playerToWSProfile(Player p, long playerAsk) {
		if (p != null) {
			WSProfile profile = new WSProfile();
			profile.firstName = p.getFirstName();
			profile.lastName = p.getLastName();
			profile.sex = p.getSex();
			profile.countryCode = p.getDisplayCountryCode();
			profile.town = p.getTown();
			profile.description = p.getDescription();
			if (p.getBirthday() == null) {
				profile.birthdate = 0;
			} else {
				profile.birthdate = p.getBirthday().getTime();
			}
			if (p.getID() == playerAsk) {
				profile.avatar = p.isAvatarPresent();
			}
			profile.dateCreation = p.getCreationDate();
			return profile;
		}
		return null;
	}
	
	/**
	 * Build WSPlayerStat associated to this player
	 * @param session
	 * @return
	 */
	public WSPlayerStat playerToWSPlayerStat(FBSession session) {
        WSPlayerStat stat = new WSPlayerStat();
		if (session != null && session.getPlayer() != null) {
			stat.nbDealPlayed = session.getPlayer().getNbPlayedDeals();
			stat.setDuelStat(getDuelStat(session.getPlayer().getID()));

			TourSeriePlayer tsp = ContextManager.getTourSerieMgr().getTourSeriePlayer(session.getPlayer().getID());
			if (tsp != null && tsp.getBestSerie() != null && tsp.getSerie().length() > 0) {
				stat.serieBest = tsp.getBestSerie();
				stat.serieBestRank = tsp.getBestRank();
				stat.serieBestPeriodStart = TourSerieMgr.transformPeriodID2TS(tsp.getBestPeriod(), true);
				stat.serieBestPeriodEnd = TourSerieMgr.transformPeriodID2TS(tsp.getBestPeriod(), false);
			}
		}
		return stat;
	}

    /**
	 * Build WSPlayer for player playerToTransform for player asking. Not used this method if players are friends !
	 * @param playerToTransform
	 * @param playerIDAsk
	 * @return
	 */
	public WSPlayer playerToWSPlayer(Player playerToTransform, long playerIDAsk) {
		if (playerToTransform != null) {
            WSPlayer wsp = new WSPlayer();
            wsp.playerID = playerToTransform.getID();
            wsp.pseudo = playerToTransform.getNickname();
            if (playerToTransform.getID() == playerIDAsk) {
                wsp.avatar = playerToTransform.isAvatarPresent();
            }
            if (!playerToTransform.isArgine()) {
                wsp.nbDealPlayed = playerToTransform.getNbPlayedDeals();
                wsp.nbFriendsAndFollowers = countFriendAndFollowerForPlayer(playerToTransform.getID());
                wsp.nbFriends = countLinkForPlayerAndType(playerToTransform.getID(), Constantes.PLAYER_LINK_TYPE_FRIEND);
                wsp.nbFollowers = countFollowerForPlayer(playerToTransform.getID());
                wsp.connected = ContextManager.getPresenceMgr().isSessionForPlayer(playerToTransform);
            }
            wsp.profile = playerToWSProfile(playerToTransform, playerIDAsk);
            wsp.setDuelStat(getDuelStat(playerToTransform.getID()));
            // convention bids
            if (playerToTransform.isArgine()) {
                wsp.conventionProfil = ContextManager.getDuelMgr().getPlayArgineConventionBidsProfil();
            } else {
                wsp.conventionProfil = playerToTransform.getConventionProfile();
            }
            if (wsp.conventionProfil == 0) {
                wsp.conventionProfil = ContextManager.getArgineEngineMgr().getDefaultProfile();
            }
            wsp.conventionValue = getConventionFreeDataForPlayer(playerToTransform);
            // convention cards
            if (playerToTransform.isArgine()) {
                wsp.cardsConventionProfil = ContextManager.getDuelMgr().getPlayArgineConventionCardsProfil();
            } else {
                PlayerSettingsData playerSettingsData = getPlayerSettingsData(playerToTransform);
                if (playerSettingsData != null) {
                    wsp.cardsConventionProfil = playerSettingsData.conventionCards;
                    if (wsp.cardsConventionProfil == 0) {
                        wsp.cardsConventionProfil = ContextManager.getArgineEngineMgr().getDefaultProfileCards();
                    }
                    wsp.cardsConventionValue = playerSettingsData.conventionCardsFreeProfile;
                }
            }

            // add serie status
            if (!playerToTransform.isArgine()) {
                wsp.serieStatus = ContextManager.getTourSerieMgr().buildSerieStatusForPlayer(playerCacheMgr.getOrLoadPlayerCache(playerToTransform.getID()));
                TourSeriePlayer tsp = ContextManager.getTourSerieMgr().getTourSeriePlayer(playerToTransform.getID());
                if (tsp != null && tsp.getBestSerie() != null && tsp.getSerie().length() > 0) {
                    wsp.serieBest = tsp.getBestSerie();
                    wsp.serieBestRank = tsp.getBestRank();
                    wsp.serieBestPeriodStart = TourSerieMgr.transformPeriodID2TS(tsp.getBestPeriod(), true);
                    wsp.serieBestPeriodEnd = TourSerieMgr.transformPeriodID2TS(tsp.getBestPeriod(), false);
                }

                PlayerHandicap handicap = getPlayerHandicap(playerToTransform.getID());
                if (handicap != null) {
                    wsp.averagePerformanceMP = NumericalTools.round(handicap.getWSAveragePerformanceMP(), 2);
                    wsp.averagePerformanceIMP = NumericalTools.round(handicap.getAveragePerformanceIMP(), 2);
                    String currentPeriodID = getHandicapCurrentPeriodID();
                    if (currentPeriodID != null) {
						HandicapStatResult statResult = getHandicapStatResultForPlayer(playerToTransform.getID(), currentPeriodID);
						if (statResult != null) {
							wsp.averagePerformanceRank = statResult.rank;
							wsp.averagePerformanceNbPlayers = countRanking(null, null, currentPeriodID);
						}
					}
                }

                Team team = teamMgr.getTeamForPlayer(playerToTransform.getID());
                if (team != null) {
                    wsp.teamID = team.getIDStr();
                    wsp.teamName = team.getName();
                    wsp.teamDivision = team.getDivision();
					TourTeamMgr tourTeamMgr = ContextManager.getTourTeamMgr();
					try {
						WSTeamResult teamResult = tourTeamMgr.getTeamCurrentPeriodRankingForTeam(team.getIDStr(), team.getDivision());
						if (teamResult != null) {
							wsp.teamRank = teamResult.rank;
							wsp.teamNbTeams = tourTeamMgr.getTeamCurrentPeriodRankingSize(team.getDivision());
						}
					} catch (FBWSException e) {
						log.error("Exception : "+e.getMessage(), e);
					}
				}

				TourCBOMgr tourCBOMgr = ContextManager.getTourCBOMgr();
                WSMainRankingPlayer funbridgePointsRankingPlayer = tourCBOMgr.getCurrentPeriodRankingFunbridgePointsForPlayer(playerToTransform.getID());
                if (funbridgePointsRankingPlayer != null) {
                	wsp.funbridgePoints = Math.toIntExact(Math.round(funbridgePointsRankingPlayer.value));
                	wsp.funbridgePointsRank = funbridgePointsRankingPlayer.rank;
                	wsp.funbridgePointsNbPlayers = tourCBOMgr.countCurrentPeriodRankingFunbridgePoints();
				}
            }
			return wsp;
		}
		return null;
	}

    /**
     * Build WSPlayerLight for player playerToTransform for player asking.
     * @param playerToTransform
     * @param playerAsk
     * @return
     */
    public WSPlayerLight playerToWSPlayerLight(long playerToTransform, long playerAsk) {
        if (playerToTransform == Constantes.PLAYER_FUNBRIDGE_ID) {
            WSPlayerLight wsp = new WSPlayerLight();
            wsp.playerID = playerToTransform;
            wsp.pseudo = Constantes.PLAYER_FUNBRIDGE_PSEUDO;
            wsp.countryCode = Constantes.PLAYER_FUNBRIDGE_COUNTRY;
            wsp.avatar = true;
            return wsp;
        }
        PlayerCache playerCache = playerCacheMgr.getPlayerCache(playerToTransform);
        if (playerCache != null) {
            WSPlayerLight wsp = new WSPlayerLight();
            wsp.playerID = playerToTransform;
            wsp.pseudo = playerCache.getPseudo();
            wsp.countryCode = playerCache.countryCode;
            if (playerToTransform == playerAsk) {
                wsp.avatar = playerCache.avatarPresent;
            } else {
                wsp.avatar = playerCache.avatarPublic;
            }
            wsp.connected = ContextManager.getPresenceMgr().isSessionForPlayerID(playerToTransform);
            if (playerToTransform != playerAsk) {
                PlayerLink pl = getLinkBetweenPlayer(playerAsk, playerToTransform);
                if (pl != null) {
                    wsp.relationMask = pl.getLinkMaskForPlayer(playerAsk);
                    // check relation type FRIEND
                    if (pl.isLinkFriend()) {
                        // trainingPartner only between friend !
                        wsp.trainingPartnerStatus = getTrainingPartnerStatusBetweenPlayers(playerAsk, playerToTransform);
                    }
                }
            }
            return wsp;
        }
        return null;
    }

	/**
	 * Build WSPlayer for player playerToTransform for player asking is playerAsk
	 * @param playerToTransform player to transform
	 * @param playerAsk player who do the requested
     * @param addDuelHistory
	 * @return
	 */
	public WSPlayer playerToWSPlayer(Player playerToTransform, Player playerAsk, boolean addDuelHistory) {
		if (playerToTransform != null && playerAsk != null) {
		    WSPlayer wsp = playerToWSPlayer(playerToTransform, playerAsk.getID());

            // player to transform != player session => view CV player
			if (playerToTransform.getID() != playerAsk.getID()) {
			    if (!playerToTransform.isArgine()) {
                    PlayerLink pl = getLinkBetweenPlayer(playerAsk.getID(), playerToTransform.getID());
                    if (pl != null) {
                        wsp.relationMask = pl.getLinkMaskForPlayer(playerAsk.getID());
                        wsp.relationMaskOther = pl.getLinkMaskForPlayer(playerToTransform.getID());
                        if ((wsp.relationMask & Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) == Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) {
                            wsp.requestMessage = pl.getMessage();
                        }
                        wsp.nbMessageNotRead = messageMgr.getNbMessageNotReadForPlayerAndSender(playerAsk.getID(), playerToTransform.getID(), pl.getDateMessageReset(playerAsk.getID()));
                        // check relation type FRIEND
                        if (pl.isLinkFriend()) {
                            wsp.dateLastMessage = pl.getDateLastMessage();
                            // trainingPartner only between friend !
                            wsp.trainingPartnerStatus = getTrainingPartnerStatusBetweenPlayers(playerAsk.getID(), playerToTransform.getID());
                            wsp.messageOnlyFriend = false;
                            wsp.duelOnlyFriend = false;
						} else {
							wsp.messageOnlyFriend = playerToTransform.hasFriendFlag(Constantes.FRIEND_MASK_MESSAGE_ONLY_FRIEND);
							wsp.duelOnlyFriend = playerToTransform.hasFriendFlag(Constantes.FRIEND_MASK_DUEL_ONLY_FRIEND);
						}
                    } else {
						wsp.messageOnlyFriend = playerToTransform.hasFriendFlag(Constantes.FRIEND_MASK_MESSAGE_ONLY_FRIEND);
						wsp.duelOnlyFriend = playerToTransform.hasFriendFlag(Constantes.FRIEND_MASK_DUEL_ONLY_FRIEND);
					}
                }
                // duelHistory between these 2 players
                if (addDuelHistory) {
                    wsp.duelHistory = getDuelHistoryBetweenPlayers(playerAsk, playerToTransform, true);
                }
			} else {
				wsp.messageOnlyFriend = playerToTransform.hasFriendFlag(Constantes.FRIEND_MASK_MESSAGE_ONLY_FRIEND);
				wsp.duelOnlyFriend = playerToTransform.hasFriendFlag(Constantes.FRIEND_MASK_DUEL_ONLY_FRIEND);
			}
			return wsp;
		}
		return null;
	}
	
	/**
	 * Return the duelHistory associated to these 2 players
	 * @param playerAsk
	 * @param playerPartner
	 * @param createEmpty
	 * @return
	 */
	public WSDuelHistory getDuelHistoryBetweenPlayers(Player playerAsk, Player playerPartner, boolean createEmpty) {
		if (playerAsk != null && playerPartner != null) {
			PlayerDuel pd = playerDuelDAO.getPlayerDuelBetweenPlayer(playerAsk.getID(), playerPartner.getID());
			if (pd != null) {
				return ContextManager.getDuelMgr().createDuelHistory(pd, playerAsk.getID(), null, true);
			}
			if (createEmpty) {
				WSGamePlayer gamePlayer = WSGamePlayer.createGamePlayerHuman(playerAsk, playerAsk.getID());
				WSDuelHistory duelHistory = new WSDuelHistory();
				duelHistory.player1 = gamePlayer;
				duelHistory.player2 = WSGamePlayer.createGamePlayerHuman(playerPartner, playerAsk.getID());
				return duelHistory;
			}
		}
		return null;
	}
	
	/**
	 * Return the trainingPartnerStatus between players
	 * @param player1
	 * @param player2
	 * @return
	 */
	public WSTrainingPartnerStatus getTrainingPartnerStatusBetweenPlayers(long player1, long player2) {
		WSTrainingPartnerStatus tps = new WSTrainingPartnerStatus();
		TournamentChallenge tc = ContextManager.getTournamentChallengeMgr().getCurrentChallengeNotExpiredForPlayers(player1, player2);
		if (tc != null) {
			tps.challengeID = tc.getID();
			tps.challengeStatus = tc.getStatus();
			tps.creatorID = tc.getCreator().getID();
			tps.dealSettings = ContextManager.getTournamentMgr().getTournamentSettings(tc.getSettings());
		}
		return tps;
	}
	
	/**
	 * Return list of playerID linked to this player with link FRIEND, FOLLOWER (only followed by this player) or FRIEND_PENDING
	 * @param playerID
	 * @return
	 */
	public List<Long> getListPlayerIDLinkedToPlayer(long playerID) {
		List<Long> listPlaID = new ArrayList<Long>();
		List<Long> temp = playerLinkDAO.listPlayerIDLinkedFollower(playerID);
		if (temp != null) {
			for (Long l : temp) {
				if (!listPlaID.contains(l)) {
					listPlaID.add(l);
				}
			}
		}
		temp = playerLinkDAO.listPlayerIDLinkedFriend(playerID);
		if (temp != null) {
			for (Long l : temp) {
				if (!listPlaID.contains(l)) {
					listPlaID.add(l);
				}
			}
		}
		temp = playerLinkDAO.listPlayerIDLinkedFriendPendingRequest(playerID);
		if (temp != null) {
			for (Long l : temp) {
				if (!listPlaID.contains(l)) {
					listPlaID.add(l);
				}
			}
		}
		temp = playerLinkDAO.listPlayerIDLinkedFriendPendingWaiting(playerID);
		if (temp != null) {
			for (Long l : temp) {
				if (!listPlaID.contains(l)) {
					listPlaID.add(l);
				}
			}
		}
		return listPlaID;
	}
	
	/**
	 * Build an event with category PLAYER and type CHANGE_LINK
	 * @param playerID ID of player to send event
	 * @param friendPlayerID playerID of friend
	 * @param linkMask
	 * @return
	 */
	public Event buildEventPlayerChangeLink(long playerID, long friendPlayerID, int linkMask) {
		Event evt = new Event();
		evt.timestamp = System.currentTimeMillis();
		evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
		evt.receiverID = playerID;
		evt.addFieldCategory(Constantes.EVENT_CATEGORY_PLAYER);
		// create change link data
		EventPlayerChangeLinkData evd = new EventPlayerChangeLinkData();
		evd.playerID = friendPlayerID;
		evd.linkMask = linkMask;
		// set data JSON in event
		try {
			evt.addFieldType(Constantes.EVENT_TYPE_PLAYER_CHANGE_LINK, jsontools.transform2String(evd, false));
			return evt;
		} catch (JsonGenerationException e) {
			log.error("JsonGenerationException to transform data="+evd,e);
		} catch (JsonMappingException e) {
			log.error("JsonMappingException to transform data="+evd,e);
		} catch (IOException e) {
			log.error("IOException to transform data="+evd,e);
		}
		return null;
	}

    /**
     * Build an event with category PLAYER and type PLAYER_UPDATE
     * @param playerID
     * @param playerInfo
     * @return
     */
	public Event buildEventPlayerUpdate(long playerID, WSPlayerInfo playerInfo) {
        Event evt = new Event();
        evt.timestamp = System.currentTimeMillis();
        evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
        evt.receiverID = playerID;
        evt.addFieldCategory(Constantes.EVENT_CATEGORY_PLAYER);
        // set playerInfo data JSON in event
        try {
            evt.addFieldType(Constantes.EVENT_TYPE_PLAYER_UPDATE, jsontools.transform2String(playerInfo, false));
            return evt;
        } catch (JsonGenerationException e) {
            log.error("JsonGenerationException to transform data playerInfo="+playerInfo,e);
        } catch (JsonMappingException e) {
            log.error("JsonMappingException to transform data playerInfo="+playerInfo,e);
        } catch (IOException e) {
            log.error("IOException to transform data playerInfo="+playerInfo,e);
        }
        return null;
    }
	
	/**
	 * List common player between 2 players
	 * @param playerID1
	 * @param playerID2
	 * @return
	 */
	public List<Player> listCommonFriendForPlayers(long playerID1, long playerID2) {
		return playerLinkDAO.listCommonFriendForPlayers(playerID1, playerID2);
	}
	
	/**
	 * List player friend for player
	 * @param playerID
	 * @return
	 */
	public List<Player> listFriendForPlayer(long playerID) {
		return playerLinkDAO.listFriendForPlayer(playerID);
	}
	
	/**
	 * List of playerID friend for player
	 * @param playerID
	 * @return
	 */
	public List<Long> listFriendIDForPlayer(long playerID) {
		return playerLinkDAO.listPlayerIDLinkedFriend(playerID);
	}
	
	/**
	 * List of playerID with a friend request sent by this playerID
	 * @param playerID
	 * @return
	 */
	public List<Long> listPlayerIDFriendPendingForPlayer(long playerID) {
		return playerLinkDAO.listPlayerIDLinkedFriendPendingRequest(playerID);
	}

    /**
     * List of playerID with block link to playerID. Players has blocked this playerID + players blocked by this playerID.
     * @param playerID
     * @return
     */
    public List<Long> listPlayerIDBlockedForPlayer(long playerID) {
        return playerLinkDAO.listPlayerIDLinkedBlocked(playerID);
    }

    public int countLinkedBlockedForPlayer(long playerId){
    	return playerLinkDAO.countLinkedBlockedForPlayer(playerId);
	}

	public int getCommunityNbActivePlayers() {
		return communityNbActivePlayers;
	}
	
	public int getCommunityNbCountryCode() {
		return communityNbCountryCode;
	}
	
	public List<WSCommunityCountryPlayer> getCommunitylistCountryPlayer() {
		return communityListCountryPlayer;
	}
	
	/**
	 * Get player location
	 * @param playerID
	 * @return
	 */
	public PlayerLocation getPlayerLocation(long playerID) {
		if (isPlayerLocationEnable()) {
			Query q = new Query();
			q.addCriteria(Criteria.where("playerID").is(playerID));
			return mongoTemplate.findOne(q, PlayerLocation.class);
		}
		return null;
	}
	
	/**
	 * Set player location. If already exists, update only if distance between new and old > 50km. Else create a new
	 * @param playerID
	 * @param longitude
	 * @param latitude
	 * @return the player location
	 */
	public PlayerLocation setPlayerLocation(long playerID, double longitude, double latitude) {
		if (isPlayerLocationEnable()) {
			synchronized (lockSetLocation.getLock(""+playerID)) {
				PlayerLocation pl = getPlayerLocation(playerID);
				if (pl != null) {
					int limitKmToUpdate = FBConfiguration.getInstance().getIntValue("player.location.updateLimitKM", 50);
					// compute distance between new and previous location
					double distance = GeoLocTools.distance(pl.location.latitude, pl.location.longitude, latitude, longitude);
					if (distance > limitKmToUpdate) {
						pl.location.latitude = latitude;
						pl.location.longitude = longitude;
						long ts = System.currentTimeMillis();
						mongoTemplate.save(pl);
						if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
							log.error("Mongo save too long ! ts="+(System.currentTimeMillis() - ts));
						}
						
					}
				} else {
					pl = new PlayerLocation();
					pl.playerID = playerID;
					pl.dateLastUpdate = System.currentTimeMillis();
					pl.location.latitude = latitude;
					pl.location.longitude = longitude;
					long ts = System.currentTimeMillis();
					mongoTemplate.insert(pl);
					if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
						log.error("Mongo insert too long ! ts="+(System.currentTimeMillis() - ts));
					}
				}
				return pl;
			}
		}
		return null;
	}
	
	/**
	 * Remove player location for playerID
	 * @param playerID
	 */
	public void removePlayerLocation(long playerID) {
		if (isPlayerLocationEnable()) {
			Query q = new Query();
			q.addCriteria(Criteria.where("playerID").is(playerID));
			long ts = System.currentTimeMillis();
			mongoTemplate.remove(q, PlayerLocation.class);
			if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
				log.error("Mongo remove too long ! ts="+(System.currentTimeMillis() - ts));
			}
		}
	}
	
	/**
	 * Return list of suggestion with friends of friends for a player
	 * @param playerID
	 * @param nbMax
	 * @return
	 */
	public List<WSPlayer> getSuggestionFriendsOfFriends(long playerID, int nbMax, List<Long> listPlaToExclude, String countryCode) {
		if (FBConfiguration.getInstance().getIntValue("player.suggestion.friendsOfFriends", 0) == 1) {
			int limitFriendMax = FBConfiguration.getInstance().getIntValue("player.suggestion.friendsOfFriends.nbMax", 10);
			// get list of friend for playerID
			List<Long> listFriendID = playerLinkDAO.listFriendIDForPlayer(playerID, true, limitFriendMax);
			if (listFriendID != null && listFriendID.size() > 0) {
				List<WSPlayer> listSuggestion = new ArrayList<WSPlayer>();
				for (Long friendID : listFriendID) {
					// get list of friends of friend 
					List<Player> listFriendsOfFriend = playerLinkDAO.listFriendsOfFriend(friendID, playerID, true, limitFriendMax, listPlaToExclude, countryCode);
					for (Player p : listFriendsOfFriend) {
						// check this player is not already in the list of suggestion
						if (!listSuggestion.contains(new WSPlayer(p.getID())) && !Constantes.PLAYER_DISABLE_PATTERN.equals(p.getNicknameDeactivated())) {
							// normally this player is not a friend
							listSuggestion.add(playerToWSPlayer(p, playerID));
						}
						if (listSuggestion.size() >= nbMax) {
							break;
						}
					}
					if (listSuggestion.size() >= nbMax) {
						break;
					}
				}
				return listSuggestion;
			}
		}
		return null;
	}
	
	/**
	 * Return list of suggestion geoloc near a player
	 * @param playerID
	 * @param nbMax
	 * @param listPlaToExclude
	 * @return
	 */
	public List<WSPlayer> getSuggestionGeoloc(long playerID, int nbMax, List<Long> listPlaToExclude) {
		if (isPlayerLocationEnable() && FBConfiguration.getInstance().getIntValue("player.suggestion.geoloc", 0) == 1) {
			PlayerLocation playerLoc = getPlayerLocation(playerID);
			if (playerLoc != null) {
				int geoLocDistance = FBConfiguration.getInstance().getIntValue("player.suggestion.geoloc.distance", 50);
				NearQuery queryGeoloc = NearQuery.near(new Point(playerLoc.location.longitude, playerLoc.location.latitude));
				// set max distance in kilometers
				if (geoLocDistance <= 0) {
					geoLocDistance = 50;
				}
				queryGeoloc.maxDistance(new Distance(geoLocDistance, Metrics.KILOMETERS));
				// set nb max result
				if (nbMax > 0) {
					queryGeoloc.num(nbMax);
				}
				// exclude list of player from result
				if (listPlaToExclude != null && listPlaToExclude.size() > 0) {
					Criteria critExclude = Criteria.where("playerID").nin(listPlaToExclude);
					Query qExclude = new Query().addCriteria(critExclude);
					queryGeoloc.query(qExclude);
				}
				try {
                    GeoResults<PlayerLocation> results = mongoTemplate.geoNear(queryGeoloc, PlayerLocation.class);
                    List<WSPlayer> listResult = new ArrayList<WSPlayer>();
                    for (Iterator<org.springframework.data.geo.GeoResult<PlayerLocation>> it = results.iterator(); it.hasNext(); ) {
                        GeoResult<PlayerLocation> e = it.next();
                        Player p = getPlayer(e.getContent().playerID);
                        if (p != null) {
                            listResult.add(playerToWSPlayer(p, playerID));
                        }
                    }
                    return listResult;
                } catch (Exception e) {
				    log.error("Failed to execute geoNear - playerID="+playerID+" - nbMax="+nbMax+" - listPlaToExclude size="+(listPlaToExclude!=null?listPlaToExclude.size():0)+" - playerLoc="+playerLoc+" - queryGeoloc="+((queryGeoloc!=null)?queryGeoloc.toDocument():null));
                }
			}
		}
		return null;
	}
	
	/**
	 * Return list of suggestion player with same country and town
	 * @param player
	 * @param nbMax
	 * @param listPlaToExclude
	 * @return
	 */
	public List<WSPlayer> getSuggestionSameCountryTown(Player player, int nbMax, List<Long> listPlaToExclude) {
		if (FBConfiguration.getInstance().getIntValue("player.suggestion.sameCountryTown", 0) == 1) {
			// check country & town from player is valid
			if (player.getDisplayCountryCode() != null && player.getDisplayCountryCode().length() > 0 && player.getTown() != null && player.getTown().length() > 0) {
				List<Player> listPla = playerDAO.listPlayerWithCountryAndTown(player.getDisplayCountryCode(), player.getTown(), listPlaToExclude, nbMax);
				if (listPla != null && listPla.size() > 0) {
					List<WSPlayer> listResult = new ArrayList<WSPlayer>();
					for (Player p : listPla) {
						listResult.add(playerToWSPlayer(p, player.getID()));
					}
					return listResult;
				}
			}
		}
		return null;
	}
	
	/**
	 * Return list of suggestion player with same lastName
	 * @param player
	 * @param nbMax
	 * @param listPlaToExclude
	 * @return
	 */
	public List<WSPlayer> getSuggestionSameLastName(Player player, int nbMax, List<Long> listPlaToExclude) {
		if (FBConfiguration.getInstance().getIntValue("player.suggestion.sameName", 0) == 1) {
			// check country & town from player is valid
			if (player.getLastName() != null && player.getLastName().length() > 0) {
				List<Player> listPla = playerDAO.listPlayerWithLastName(player.getLastName(), listPlaToExclude, nbMax);
				if (listPla != null && listPla.size() > 0) {
					List<WSPlayer> listResult = new ArrayList<WSPlayer>();
					for (Player p : listPla) {
						listResult.add(playerToWSPlayer(p, player.getID()));
					}
					return listResult;
				}
			}
		}
		return null;
	}

    /**
     * Return list of suggestion player with same lastName
     * @param player
     * @param nbMax
     * @param listPlaToExclude
     * @return
     */
    public List<WSPlayer> getSuggestionRandom(Player player, int nbMax, List<Long> listPlaToExclude) {
        if (FBConfiguration.getInstance().getIntValue("player.suggestion.random", 1) == 1) {
            // check country & town from player is valid
            List<Player> listPla = playerDAO.listPlayerRandomWithDateConnection(System.currentTimeMillis() - 15*Constantes.TIMESTAMP_DAY, listPlaToExclude, nbMax);
            if (listPla != null && listPla.size() > 0) {
                List<WSPlayer> listResult = new ArrayList<WSPlayer>();
                for (Player p : listPla) {
                	if(!Constantes.PLAYER_DISABLE_PATTERN.equals(p.getNicknameDeactivated())){
						listResult.add(playerToWSPlayer(p, player.getID()));
					}
                }
                return listResult;
            }
        }
        return null;
    }
	
	/**
	 * Get suggestion friend for player
	 * @param p
	 * @return
	 */
	public List<WSPlayer> getSuggestionForPlayer(Player p) {
		List<WSPlayer> listSuggestion = new ArrayList<WSPlayer>();
		if (p != null) {
			// check suggestion is enabled
			if (FBConfiguration.getInstance().getIntValue("player.suggestion.enable", 0) == 1) {
				int nbMax = FBConfiguration.getInstance().getIntValue("player.suggestion.nbMax", 10);
				if (nbMax > 0) {
					List<WSPlayer> temp = null;
					List<Long> listPlaToExclude = new ArrayList<Long>();
					// current player is always exclude from result !
					listPlaToExclude.add(p.getID());
					// add to the exclude list all player with friend pending
					List<Long> listPlayerFriendPendingID = listPlayerIDFriendPendingForPlayer(p.getID());
					if (listPlayerFriendPendingID != null && listPlayerFriendPendingID.size() > 0) {
						for (Long l : listPlayerFriendPendingID) {
							if (!listPlaToExclude.contains(l)) {
								listPlaToExclude.add(l);
							}
						}
					}
					// add to the exclude list all friends
					List<Long> listFriendID = listFriendIDForPlayer(p.getID());
					if (listFriendID != null && listFriendID.size() > 0) {
						for (Long l : listFriendID) {
							if (!listPlaToExclude.contains(l)) {
								listPlaToExclude.add(l);
							}
						}
					}
                    // add to the exclude list list all blocked
                    List<Long> listBlockedID = listPlayerIDBlockedForPlayer(p.getID());
                    if (listBlockedID != null && listBlockedID.size() > 0) {
                        for (Long l : listBlockedID) {
                            if (!listPlaToExclude.contains(l)) {
                                listPlaToExclude.add(l);
                            }
                        }
                    }
					// suggestion friends of friends
					temp = getSuggestionFriendsOfFriends(p.getID(), nbMax, listPlaToExclude, null);
					if (temp != null && temp.size() > 0) {
						listSuggestion.addAll(temp);
						// add element find to listPlaToExclude
						for (WSPlayer e : temp) {
							if (!listPlaToExclude.contains(e.playerID)) {
								listPlaToExclude.add(e.playerID);
							}
						}
					}
					
					// suggestion geoloc
					temp = getSuggestionGeoloc(p.getID(), nbMax, listPlaToExclude);
					if (temp != null && temp.size() > 0) {
						listSuggestion.addAll(temp);
						// add element find to listPlaToExclude
						for (WSPlayer e : temp) {
							if (!listPlaToExclude.contains(e.playerID)) {
								listPlaToExclude.add(e.playerID);
							}
						}
					}
					// suggestion same country and town
					temp = getSuggestionSameCountryTown(p, nbMax, listPlaToExclude);
					if (temp != null && temp.size() > 0) {
						listSuggestion.addAll(temp);
						// add element find to listPlaToExclude
						for (WSPlayer e : temp) {
							if (!listPlaToExclude.contains(e.playerID)) {
								listPlaToExclude.add(e.playerID);
							}
						}
					}
					// suggestion same lastName
					temp = getSuggestionSameLastName(p, nbMax, listPlaToExclude);
					if (temp != null && temp.size() > 0) {
						listSuggestion.addAll(temp);
                        // add element find to listPlaToExclude
                        for (WSPlayer e : temp) {
                            if (!listPlaToExclude.contains(e.playerID)) {
                                listPlaToExclude.add(e.playerID);
                            }
                        }
					}

                    // complete if necessary with random player
                    if (listSuggestion.size() < nbMax) {
                        temp = getSuggestionRandom(p, nbMax - listSuggestion.size(), listPlaToExclude);
                        if (temp != null && temp.size() > 0) {
                            listSuggestion.addAll(temp);
                        }
                    }
					
					if (listSuggestion.size() > 0) {
						// random on list suggestion and take only the first nbMax elements
						Collections.shuffle(listSuggestion, new Random(System.nanoTime()));
						if (listSuggestion.size() > nbMax) {
							return listSuggestion.subList(0, nbMax);
						}
						return listSuggestion.stream().filter(x -> !x.pseudo.startsWith("DELETED")).collect(Collectors.toList());
					}
				} else {
					log.error("nbMax is not valid ! nbMax="+nbMax);
				}
			}
		} else {
			log.error("Param player is null !");
		}
		return listSuggestion.stream().filter(x -> !x.pseudo.startsWith("DELETED")).collect(Collectors.toList());
	}
	
	/**
	 * Change player pseudo and set the new pseudo in all memory result
	 * @param p
	 * @param newPseudo
	 * @param incrementCounter
	 * @throws FBWSException
	 */
	public void changePlayerPseudo(Player p, String newPseudo, boolean incrementCounter) throws FBWSException {
		// really change ?
		if (!p.getNickname().equals(newPseudo)) {
			// check pseudo is valid
			if (!checkPseudoFormat(newPseudo)) {
				throw new FBWSException(FBExceptionType.PRESENCE_BAD_FORMAT_PSEUDO);
			}
			
			// check if new login exist
			if (existPseudo(newPseudo)) {
                if (log.isDebugEnabled()) {
                    log.debug("Pseudo already used : newPseudo=" + newPseudo);
                }
				throw new FBWSException(FBExceptionType.PRESENCE_ALREADY_USED_PSEUDO);
			}

			p.setNickname(newPseudo);
			try {
				ContextManager.getPlayerMgr().updatePlayerToDB(p, PlayerUpdateType.PROFILE);
			} catch (Exception e) {
				log.error("Exception to change pseudo for player="+p+" - new pseudo = "+newPseudo, e);
				throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
			}
			playerCacheMgr.updatePlayerAllData(p);
		}
	}

	/**
	 * return stat for duels played by player
	 * @param playerID
	 * @return
	 */
	public PlayerDuelStat getDuelStat(long playerID) {
		return playerDuelDAO.getStat(playerID);
	}
	
	/**
	 * Build the valid key for mail. Used playerID+";"+ date creation player transform as string and all is crypt
	 * @param p
	 * @return
	 */
	public String buildKeyValidMail(Player p) {
		String key = null;
		if (p != null && p.getCreationDate() > 0) {
			try {
				StringBuffer sbTS = new StringBuffer();
				String temp = ""+p.getCreationDate();
				for (int i = 0; i < temp.length(); i++) {
					sbTS.append((char) ('A' + Integer.parseInt(temp.substring(i, i + 1))));
				}
				return AESCrypto.crypt(p.getID()+";"+sbTS.toString(), Constantes.CRYPT_KEY);
			} catch (Exception e) {
				log.error("Failed to build key valid mail for player="+p, e);
			}
		} else {
			log.error("Param not valid - p="+p);
		}
		return key;
	}
	
	/**
	 * Check if the key is valid for the player
	 * @param p
	 * @param keyToCkeck
	 * @return
	 */
	public boolean isKeyValidMail(Player p, String keyToCkeck) {
		if (p != null && keyToCkeck != null && keyToCkeck.length() > 0) {
			String keyPla = buildKeyValidMail(p);
			if (keyPla != null) {
				return keyPla.equals(keyToCkeck);
			} else {
				log.error("Key for player is null !!!! p="+p+" - keyToCkeck="+keyToCkeck);
			}
		} else {
			log.error("Param not valid - p="+p+" - keyToCkeck="+keyToCkeck);
		}
		return false;
	}

	/**
	 * Check if player location is enable in configuration
	 * @return
	 */
	public boolean isPlayerLocationEnable() {
		return FBConfiguration.getInstance().getIntValue("player.location.setLocationEnable", 1) == 1;
	}
	
	@Transactional
	public boolean deletePlayerDevice(long playerID, long deviceID) {
		return playerDeviceDAO.deleteForPlayerAndDevice(playerID, deviceID);
	}
	
	/**
	 * Delete a device : device all player device link and delete device
	 * @param deviceID
	 * @return
	 */
	@Transactional
	public boolean deleteDevice(long deviceID) {
		playerDeviceDAO.deleteForDevice(deviceID);
		return deviceDAO.deleteForID(deviceID);
	}

	/**
	 * Update the credit of player in DB
	 * @param player
	 * @param nbCredit
	 * @param nbDeal
	 * @throws FBWSException
	 */
	@Transactional
	public void updatePlayerCreditDeal(Player player, int nbCredit, int nbDeal) throws FBWSException{

		// update credit amount of player (decremnt nbDeal)
		player.decrementCreditAmount(nbCredit);

		// increment the counter of deal played
		player.incrementNbDealPlayed(nbDeal);
		// write all data in DB
		updatePlayerToDB(player, PlayerUpdateType.CREDIT_DEAL);
	}

    /**
     * Update the counter of nb deals played
     * @param player
     * @param nbDeal
     * @throws FBWSException
     */
    @Transactional
    public void updatePlayerNbDealPlayed(Player player, int nbDeal) throws FBWSException{
        // increment the counter of deal played
        player.incrementNbDealPlayed(nbDeal);
        // write all data in DB
        updatePlayerToDB(player, PlayerUpdateType.CREDIT_DEAL);
    }

	/**
	 * Save player before changing pseudo. Another player with same pseudo converge before this player. This player is so save before updating pseudo with random value
	 * @param player
	 */
	public void savePlayerToChangePseudo(Player player) {
		PlayerConvergenceChangePseudo e = new PlayerConvergenceChangePseudo();
		e.playerID = player.getID();
		e.nickname = player.getNickname();
		e.mail = player.getMail();
		e.password = player.getPassword();
		mongoTemplate.insert(e);
	}

	public List<Player> listPlayer(int offset, int nbMax) {
		if (offset < 0) {
			offset = 0;
		}
		if (nbMax == 0) {
			nbMax = 100;
		}
		return playerDAO.listPlayer(offset, nbMax);
	}


	public String getConventionFreeDataForPlayer(Player p) {
		String value = "";
		if (p != null && p.getConventionProfile() > 0) {
			ArgineProfile argineProfile = ContextManager.getArgineEngineMgr().getProfile(p.getConventionProfile());
			if (argineProfile != null) {
				if (argineProfile.isFree()) {
					try {
						// retrieve player settings data from JSON value
						PlayerSettingsData plaSettings = jsontools.mapData(p.getSettings(), PlayerSettingsData.class);
						if (plaSettings != null) {
                            if (plaSettings.conventionBidsFreeProfile != null) {
                                value = plaSettings.conventionBidsFreeProfile;
                            } else {
                                value = plaSettings.conventionFreeProfile;
                            }
							if (value == null) {
								log.error("ConventionFreeProfile is nul after mapData for player="+p);
								value = "";
							}
						} else {
							log.error("PlayerSettingsData is nul after mapData for player="+p);
						}
					} catch (Exception e) {
						log.error("Exception to read player settings for player="+p, e);
					}
				}
			}
		}
		return value;
	}

    /**
     * Transform to player info.
     *
     * @return
     */
    public WSPlayerInfo playerToWSPlayerInfo(FBSession session) {
        if (session != null) {
            Player p = session.getPlayer();
			if (playerDAO.getPlayer(p.getID()) != null ){
				p = playerDAO.getPlayer(p.getID()) ;
			}

            WSPlayerInfo playerInfo = new WSPlayerInfo();
            playerInfo.playerID = p.getID();
            playerInfo.pseudo = p.getNickname();
            playerInfo.cert = p.getCert() ;
            playerInfo.type = p.getType() ;
            playerInfo.mail = p.getMail();
            playerInfo.creditAmount = p.getTotalCreditAmount();

            playerInfo.serie = ContextManager.getTourSerieMgr().buildWSSerie(session.getPlayerCache());
            playerInfo.profile = p.toWSPlayerProfile(p.getID(), playerInfo.serie);
            playerInfo.settings = p.getSettings();
            playerInfo.accountExpirationDate = p.getDateSubscriptionValid();

            playerInfo.avatar = p.isAvatarPresent();
            playerInfo.stat = playerToWSPlayerStat(session);

            playerInfo.storePromo = session.storePromo;
            playerInfo.freemium = session.isFreemium();
            playerInfo.replayEnabled = isReplayEnabledForPlayer(session);

            return playerInfo;
        }
        return null;
    }

    public boolean isReplayEnabledForPlayer(FBSession session) {
        if (FBConfiguration.getInstance().getIntValue("player.replayEnabled", 1) == 1) {
            if (session.isFreemium()) {
                int limitNbDealPlayed = FBConfiguration.getInstance().getIntValue("player.replayEnabledLimitNbDeals", 50);
				return session.getPlayer().getNbPlayedDeals() <= limitNbDealPlayed;
            }
        }
        return true;
    }

    public Privileges getPrivilegesForPlayer(long playerID) {
        Privileges priv = mongoTemplate.findOne(Query.query(Criteria.where("playerID").is(playerID)), Privileges.class);
        if (priv == null) {
            // Get default privilege (playerID : 0)
            priv = mongoTemplate.findOne(Query.query(Criteria.where("playerID").is(0)), Privileges.class);
            if (priv == null) {
                priv = new Privileges();
            }
        }
        return priv;
    }

    public void setPrivilegesForPlayer(Privileges privilegesForPlayer) {
        if (privilegesForPlayer != null) {
            if (privilegesForPlayer.ID == null) {
                mongoTemplate.insert(privilegesForPlayer);
            } else {
                mongoTemplate.save(privilegesForPlayer);
            }
        }
    }

    public boolean isPlayer1BlockedByPlayer2(long player1ID, long player2ID) {
        PlayerLink pl = getLinkBetweenPlayer(player1ID, player2ID);
        if (pl != null) {
            return pl.hasBlocked(player2ID);
        }
        return false;
    }

    public void updatePlayerDisplayCountryCodeForRanking(Player player) {
        ContextManager.getDuelMgr().updatePlayerStatCountryCode(player);
        ContextManager.getTourSerieMgr().updatePlayerCountryCode(player);
		for (TourFederationMgr tourFederationMgr : ContextManager.getListTourFederationMgr()) {
			tourFederationMgr.updatePlayerFederationStatCountryCode(player);
		}
		ContextManager.getTourCBOMgr().updatePlayerFunbridgePointsStatCountryCode(player);
		this.updateHandicapStatCountryCode(player);
    }

	public void updateHandicapStatCountryCode(Player player) {
		if (player != null) {
			try {
				mongoHandicapTemplate.updateFirst(Query.query(Criteria.where("playerID").is(player.getID())),
						Update.update("countryCode", player.getDisplayCountryCode()),
						HandicapStat.class);
			} catch (Exception e) {
				log.error("Failed to update country code for player="+player, e);
			}
		}
	}

	/**
	 * Get handicap current periodID
	 * @return
	 */
	public String getHandicapCurrentPeriodID() {
		HandicapPeriod currentPeriod = mongoHandicapTemplate.findOne(Query.query(Criteria.where("finished").is(false)).with(new Sort(Sort.Direction.DESC, "periodID")), HandicapPeriod.class);
		if (currentPeriod != null && currentPeriod.getPeriodID() != null) {
			if (!currentPeriod.getPeriodID().equals(handicapCurrentPeriodID)) {
				handicapCurrentPeriodID = currentPeriod.getPeriodID();
				updateCountHandicapCurrentPeriodID();
			}
			return currentPeriod.getPeriodID();
		}
		return null;
	}

	/**
	 * Count player in current period ranking
	 * @return
	 */
	public void updateCountHandicapCurrentPeriodID() {
		Criteria criteria = Criteria.where("resultPeriod."+handicapCurrentPeriodID+".handicap").exists(true);
		int res = (int)mongoHandicapTemplate.count(Query.query(criteria), HandicapStat.class);
		countHandicapCurrentPeriodID = res;
	}

	/**
	 * Get handicap stat result for player
	 * @param playerID
	 * @return
	 */
	public HandicapStatResult getHandicapStatResultForPlayer(long playerID, String periodID) {
		Criteria criteria = Criteria.where("playerID").is(playerID).and("resultPeriod."+periodID+".handicap").exists(true);
		Query query = Query.query(criteria);
		query.fields().include("resultPeriod."+periodID);
		HandicapStat handicapStatPlayer = mongoHandicapTemplate.findOne(query, HandicapStat.class);
		if (handicapStatPlayer != null) {
			return handicapStatPlayer.resultPeriod.get(periodID);
		}
		return null;
	}

	/**
	 * Get average performance ranking for players with offset & limit
	 * @param playerAsk
	 * @param selectionPlayerID
	 * @param countryCode
	 * @param offset
	 * @param nbMax
	 * @return
	 */
	public ResultServiceRest.GetMainRankingResponse getRankingAveragePerformance(PlayerCache playerAsk, List<Long> selectionPlayerID, String countryCode, int offset, int nbMax) {
		String periodID = getHandicapCurrentPeriodID();
		if (periodID == null) {
			return new ResultServiceRest.GetMainRankingResponse();
		}

		if (nbMax == 0) {
			nbMax = 50;
		}
		ResultServiceRest.GetMainRankingResponse response = new ResultServiceRest.GetMainRankingResponse();
		response.totalSize = countRanking(selectionPlayerID, countryCode, periodID);
		response.nbRankedPlayers = countRanking(null, countryCode, periodID);
		response.ranking = new ArrayList<>();
		// rankingPlayer
		if (playerAsk != null) {
			WSMainRankingPlayer rankingPlayer = new WSMainRankingPlayer(playerAsk, true, playerAsk.ID);
			HandicapStat handicapStatPlayer = mongoHandicapTemplate.findOne(Query.query(Criteria.where("playerID").is(playerAsk.ID)), HandicapStat.class);
			if (handicapStatPlayer != null && handicapStatPlayer.resultPeriod.containsKey(periodID)) {
				HandicapStatResult statResult = handicapStatPlayer.resultPeriod.get(periodID);
				rankingPlayer.value = statResult.getAveragePerformanceMP();
				if (StringUtils.isNotBlank(countryCode)) {
					rankingPlayer.rank = countRankingBestAveragePerformance(countryCode, statResult.handicap, null, periodID) + 1;
				} else {
					rankingPlayer.rank = statResult.rank;
				}
				if (offset == -1) {
					int playerOffset = countRankingBestAveragePerformance(countryCode, statResult.handicap, selectionPlayerID, periodID) + 1;
					offset = playerOffset - (nbMax / 2);
				}
			}
			rankingPlayer.rank = (rankingPlayer.rank == 0)?-1:rankingPlayer.rank;
			response.rankingPlayer = rankingPlayer;
		}
		if (offset < 0) {
			offset = 0;
		}
		response.offset = offset;

		// list stat
		List<HandicapStat> listHandicap = this.listHandicapStat(offset, nbMax, selectionPlayerID, countryCode, periodID);

		// Filter country
		if (StringUtils.isNotBlank(countryCode)) {
			int currentRank = -1, nbWithSameAveragePerformance = 0;
			double currentAveragePerformance = -1;
			for (HandicapStat handicapStat : listHandicap) {
				HandicapStatResult statResult = handicapStat.resultPeriod.get(periodID);
				if (statResult != null) {
					WSMainRankingPlayer data = new WSMainRankingPlayer(playerCacheMgr.getPlayerCache(handicapStat.playerID), presenceMgr.isSessionForPlayerID(handicapStat.playerID), handicapStat.playerID);

					// init current counter
					if (currentRank == -1) {
						currentRank = countRankingBestAveragePerformance(countryCode, statResult.handicap, selectionPlayerID, periodID) + 1;
						nbWithSameAveragePerformance = (offset + 1) - currentRank + 1;
					} else {
						if (statResult.getAveragePerformanceMP() == currentAveragePerformance) {
							nbWithSameAveragePerformance++;
						} else {
							currentRank = currentRank + nbWithSameAveragePerformance;
							nbWithSameAveragePerformance = 1;
						}
					}
					currentAveragePerformance = statResult.getAveragePerformanceMP();

					data.value = statResult.getAveragePerformanceMP();
					data.rank = currentRank;

					response.ranking.add(data);
				}
			}
		}
		// No filter or filter selection players
		else {
			for (HandicapStat handicapStat : listHandicap) {
				HandicapStatResult statResult = handicapStat.resultPeriod.get(periodID);
				if (statResult != null) {
					WSMainRankingPlayer data = new WSMainRankingPlayer(playerCacheMgr.getPlayerCache(handicapStat.playerID), presenceMgr.isSessionForPlayerID(handicapStat.playerID), handicapStat.playerID);
					data.value = statResult.getAveragePerformanceMP();
					data.rank = statResult.rank;
					response.ranking.add(data);
				}
			}
		}

		return response;
	}

	/**
	 * Count player in ranking
	 * @param selectionPlayerID
	 * @param countryCode
	 * @param periodID
	 * @return
	 */
	public int countRanking(List<Long> selectionPlayerID, String countryCode, String periodID) {
		// If no filter, use memory
		if ((selectionPlayerID == null || selectionPlayerID.isEmpty())
				&& StringUtils.isBlank(countryCode)) {
			if (periodID != null && periodID.equals(handicapCurrentPeriodID)) {
				return countHandicapCurrentPeriodID;
			}
		}

		Criteria criteria = Criteria.where("resultPeriod."+periodID+".handicap").exists(true);
		if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
			criteria = criteria.and("playerID").in(selectionPlayerID);
		}
		if (StringUtils.isNotBlank(countryCode)) {
			criteria = criteria.and("countryCode").is(countryCode);
		}
		return (int)mongoHandicapTemplate.count(Query.query(criteria), HandicapStat.class);
	}

	/**
	 * Count nb player with handicap < value in parameter
	 * @param countryCode
	 * @param handicap
	 * @param selectionPlayerID
	 * @param periodID
	 * @return
	 */
	public int countRankingBestAveragePerformance(String countryCode, double handicap, List<Long> selectionPlayerID, String periodID) {
		Criteria criteria = Criteria.where("resultPeriod."+periodID+".handicap").lt(handicap);
		if (StringUtils.isNotBlank(countryCode)) {
			criteria = criteria.and("countryCode").is(countryCode);
		}
		if (selectionPlayerID != null && !selectionPlayerID.isEmpty()) {
			criteria = criteria.and("playerID").in(selectionPlayerID);
		}
		return (int)mongoHandicapTemplate.count(Query.query(criteria), HandicapStat.class);
	}

	/**
	 * List player handicaps with offset and nbMax.
	 * @param offset
	 * @param nbMax
	 * @param selectionPlayerID
	 * @param countryCode
	 * @param periodID
	 * @return
	 */
	public List<HandicapStat> listHandicapStat(int offset, int nbMax, List<Long> selectionPlayerID, String countryCode, String periodID) {
		Criteria criteria = Criteria.where("resultPeriod."+periodID+".handicap").exists(true);
		if (selectionPlayerID != null && !selectionPlayerID.isEmpty()) {
			criteria = criteria.and("playerID").in(selectionPlayerID);
		}
		if (countryCode != null && countryCode.length() > 0) {
			criteria = criteria.and("countryCode").is(countryCode);
		}
		Sort sort = new Sort(Sort.Direction.ASC, "resultPeriod."+periodID+".handicap");
		Query query = new Query(criteria).with(sort).skip(offset).limit(nbMax);
		return mongoHandicapTemplate.find(query, HandicapStat.class);
	}

	/**
	 * Get average performance ranking for countries with offset & limit
	 * @param playerAsk
	 * @param offset
	 * @param nbMax
	 * @param nbMinPlayers
	 * @return
	 */
	public ResultServiceRest.GetMainRankingResponse getRankingCountry(PlayerCache playerAsk, int offset, int nbMax, int nbMinPlayers) {
		String periodID = getHandicapCurrentPeriodID();
		if (periodID == null) {
			return new ResultServiceRest.GetMainRankingResponse();
		}

		if (nbMax == 0) {
			nbMax = 50;
		}
		ResultServiceRest.GetMainRankingResponse response = new ResultServiceRest.GetMainRankingResponse();
		response.totalSize = countRankingCountry(nbMinPlayers, periodID);
		response.nbRankedPlayers = response.totalSize;
		response.ranking = new ArrayList<>();
		// rankingPlayer
		if (playerAsk != null) {
			WSMainRankingCountry rankingCountry = new WSMainRankingCountry();
			CountryHandicapStat countryHandicapStat = mongoHandicapTemplate.findOne(Query.query(Criteria.where("countryCode").is(playerAsk.countryCode)), CountryHandicapStat.class);
			if (countryHandicapStat != null && countryHandicapStat.resultPeriod.containsKey(periodID)) {
				CountryHandicapStatResult statResult = countryHandicapStat.resultPeriod.get(periodID);
				rankingCountry.value = statResult.getAveragePerformanceMP();
				if (nbMinPlayers > 0) {
					rankingCountry.rank = countRankingCountryBestAveragePerformance(statResult.handicap, nbMinPlayers, periodID);
				} else {
					rankingCountry.rank = statResult.rank;
				}
				if (offset == -1) {
					int playerOffset = rankingCountry.rank;
					offset = playerOffset - (nbMax / 2);
				}
			}
			rankingCountry.rank = (rankingCountry.rank == 0)?-1:rankingCountry.rank;
			response.rankingPlayer = rankingCountry;
		}
		if (offset < 0) {
			offset = 0;
		}
		response.offset = offset;

		// list stat
		List<CountryHandicapStat> listHandicap = this.listCountryHandicapStat(offset, nbMax, nbMinPlayers, periodID);

		// Filter nbMinPlayers
		if (nbMinPlayers > 0) {
			int currentRank = -1, nbWithSameAveragePerformance = 0;
			double currentAveragePerformance = -1;
			for (CountryHandicapStat handicapStat : listHandicap) {
				CountryHandicapStatResult statResult = handicapStat.resultPeriod.get(periodID);
				if (statResult != null) {
					// init current counter
					if (currentRank == -1) {
						currentRank = countRankingCountryBestAveragePerformance(statResult.handicap, nbMinPlayers, periodID) + 1;
						nbWithSameAveragePerformance = (offset + 1) - currentRank + 1;
					} else {
						if (statResult.getAveragePerformanceMP() == currentAveragePerformance) {
							nbWithSameAveragePerformance++;
						} else {
							currentRank = currentRank + nbWithSameAveragePerformance;
							nbWithSameAveragePerformance = 1;
						}
					}
					currentAveragePerformance = statResult.getAveragePerformanceMP();

					WSMainRankingCountry data = new WSMainRankingCountry();
					data.value = statResult.getAveragePerformanceMP();
					data.rank = currentRank;
					data.countryCode = handicapStat.countryCode;
					data.nbPlayers = statResult.nbPlayers;

					response.ranking.add(data);
				}
			}
		}
		// No filter
		else {
			for (CountryHandicapStat handicapStat : listHandicap) {
				CountryHandicapStatResult statResult = handicapStat.resultPeriod.get(periodID);
				if (statResult != null) {
					WSMainRankingCountry data = new WSMainRankingCountry();
					data.value = statResult.getAveragePerformanceMP();
					data.rank = statResult.rank;
					data.countryCode = handicapStat.countryCode;
					data.nbPlayers = statResult.nbPlayers;
					response.ranking.add(data);
				}
			}
		}

		return response;
	}

	/**
	 * Count nb countries in ranking
	 * @param periodID
	 * @return
	 */
	public int countRankingCountry(int nbMinPlayers, String periodID) {
		Criteria criteria = Criteria.where("resultPeriod."+periodID+".handicap").exists(true);
		if (nbMinPlayers > 0) {
			criteria = criteria.and("resultPeriod."+periodID+".nbPlayers").gte(nbMinPlayers);
		}
		return (int)mongoHandicapTemplate.count(Query.query(criteria), CountryHandicapStat.class);
	}

	/**
	 * Count nb countries with handicap < value in parameter
	 * @param handicap
	 * @param periodID
	 * @return
	 */
	public int countRankingCountryBestAveragePerformance(double handicap, int nbMinPlayers, String periodID) {
		Criteria criteria = Criteria.where("resultPeriod."+periodID+".handicap").lt(handicap);
		if (nbMinPlayers > 0) {
			criteria = criteria.and("resultPeriod."+periodID+".nbPlayers").gte(nbMinPlayers);
		}
		return (int)mongoHandicapTemplate.count(Query.query(criteria), CountryHandicapStat.class);
	}

	/**
	 * List country handicaps with offset and nbMax.
	 * @param offset
	 * @param nbMax
	 * @param nbMinPlayers
	 * @param periodID
	 * @return
	 */
	public List<CountryHandicapStat> listCountryHandicapStat(int offset, int nbMax, int nbMinPlayers, String periodID) {
		Criteria criteria = Criteria.where("resultPeriod."+periodID+".handicap").exists(true);
		if (nbMinPlayers > 0) {
			criteria = criteria.and("resultPeriod."+periodID+".nbPlayers").gte(nbMinPlayers);
		}
		Sort sort = new Sort(Sort.Direction.ASC, "resultPeriod."+periodID+".handicap");
		Query query = new Query(criteria).with(sort).skip(offset).limit(nbMax);
		return mongoHandicapTemplate.find(query, CountryHandicapStat.class);
	}

	/**
	 * Get nb deals played in each category. ????????nb????
	 * @param playerID
	 */
	public PlayerDeals getPlayerDeals(long playerID) {
		return mongoTemplate.findOne(Query.query(Criteria.where("playerID").is(playerID)), PlayerDeals.class);
	}

	/**
	 * Create nb deals played in each category.
	 * @param playerID
	 */
	public PlayerDeals createPlayerDeals(long playerID) {
		PlayerDeals playerDeals = new PlayerDeals(playerID);
		// Fill with nbGames
		int nbDealsTraining = ContextManager.getTrainingMgr().countGamesForPlayer(playerID);
		playerDeals.nbPlayedDealsCategory.put(Constantes.TOURNAMENT_CATEGORY_TRAINING, nbDealsTraining);
		playerDeals.nbPlayedDeals += nbDealsTraining;

		int nbDealsDuel = ContextManager.getDuelMgr().countGamesForPlayer(playerID);
		playerDeals.nbPlayedDealsCategory.put(Constantes.TOURNAMENT_CATEGORY_DUEL, nbDealsDuel);
		playerDeals.nbPlayedDeals += nbDealsDuel;

		int nbDealsTimezone = ContextManager.getTimezoneMgr().countGamesForPlayer(playerID);
		playerDeals.nbPlayedDealsCategory.put(Constantes.TOURNAMENT_CATEGORY_TIMEZONE, nbDealsTimezone);
		playerDeals.nbPlayedDeals += nbDealsTimezone;

		int nbDealsSerie = ContextManager.getTourSerieMgr().countGamesForPlayer(playerID);
		playerDeals.nbPlayedDealsCategory.put(Constantes.TOURNAMENT_CATEGORY_NEWSERIE, nbDealsSerie);
		playerDeals.nbPlayedDeals += nbDealsSerie;

		int nbDealsSerieTopChallenge = ContextManager.getSerieTopChallengeMgr().countGamesForPlayer(playerID);
		playerDeals.nbPlayedDealsCategory.put(Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE, nbDealsSerieTopChallenge);
		playerDeals.nbPlayedDeals += nbDealsSerieTopChallenge;

		int nbDealsSerieEasyChallenge = ContextManager.getSerieEasyChallengeMgr().countGamesForPlayer(playerID);
		playerDeals.nbPlayedDealsCategory.put(Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE, nbDealsSerieEasyChallenge);
		playerDeals.nbPlayedDeals += nbDealsSerieEasyChallenge;

		int nbDealsTeam = ContextManager.getTourTeamMgr().countGamesForPlayer(playerID);
		playerDeals.nbPlayedDealsCategory.put(Constantes.TOURNAMENT_CATEGORY_TEAM, nbDealsTeam);
		playerDeals.nbPlayedDeals += nbDealsTeam;

		int nbDealsPrivate = ContextManager.getPrivateTournamentMgr().countGamesForPlayer(playerID);
		playerDeals.nbPlayedDealsCategory.put(Constantes.TOURNAMENT_CATEGORY_PRIVATE, nbDealsPrivate);
		playerDeals.nbPlayedDeals += nbDealsPrivate;

		int nbDealsLearning = ContextManager.getTourLearningMgr().countGamesForPlayer(playerID);
		playerDeals.nbPlayedDealsCategory.put(Constantes.TOURNAMENT_CATEGORY_LEARNING, nbDealsLearning);
		playerDeals.nbPlayedDeals += nbDealsLearning;

		for (int category : FederationMgr.categories) {
			int nbDealsFederation = ContextManager.getTournamentMgrForCategory(category).countGamesForPlayer(playerID);
			playerDeals.nbPlayedDealsCategory.put(category, nbDealsFederation);
			playerDeals.nbPlayedDeals += nbDealsFederation;
		}
		return playerDeals;
	}

    /**
     * Add deals played
     * @param playerID
     * @param mapCategoryPlay
     */
	public void addDealsPlayed(long playerID, Map<Integer, Integer> mapCategoryPlay) {
		boolean enableAddDealsPlayed = FBConfiguration.getInstance().getConfigBooleanValue("enableAddDealsPlayed", true);
		if (enableAddDealsPlayed) {
			try {
				PlayerDeals playerDeals = getPlayerDeals(playerID);
				if (playerDeals == null) {
					// Init
					playerDeals = createPlayerDeals(playerID);
				}
				playerDeals.update(mapCategoryPlay);
				mongoTemplate.save(playerDeals);
			} catch (Exception e) {
				boolean enableLogAddDealsPlayed = FBConfiguration.getInstance().getConfigBooleanValue("enableLogAddDealsPlayed", true);
				if (enableLogAddDealsPlayed) {
					log.error(e.getMessage(), e);
				}
			} finally {
				return;
			}
		}
	}

	/**
	 * get the new nickname
	 */

	public String getNewNickName(){
		String randomcode = "";
		// 用字符数组的方式随机
		String model = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		char[] m = model.toCharArray();
		for (int j = 0; j < 6; j++) {
			char c = m[(int) (Math.random() * 36)];
			// 保证六位随机数之间没有重复的
			if (randomcode.contains(String.valueOf(c))) {
				j--;
				continue;
			}
			randomcode = randomcode + c;
		}
		return randomcode ;
	}
}
