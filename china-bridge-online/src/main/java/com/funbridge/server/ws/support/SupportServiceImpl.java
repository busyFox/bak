package com.funbridge.server.ws.support;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.GenericMessage;
import com.funbridge.server.message.data.MessageNotif;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.PlayerMgr.PlayerUpdateType;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.data.*;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.store.dao.TransactionDAO;
import com.funbridge.server.store.data.Transaction;
import com.funbridge.server.team.data.Team;
import com.funbridge.server.tournament.duel.DuelMgr;
import com.funbridge.server.tournament.duel.data.DuelGame;
import com.funbridge.server.tournament.duel.data.DuelTournamentPlayer;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.game.TournamentPlayer;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;
import com.funbridge.server.tournament.generic.memory.GenericMemTournamentPlayer;
import com.funbridge.server.tournament.privatetournament.PrivateTournamentMgr;
import com.funbridge.server.tournament.privatetournament.data.PrivateDeal;
import com.funbridge.server.tournament.privatetournament.data.PrivateTournament;
import com.funbridge.server.tournament.privatetournament.data.PrivateTournamentProperties;
import com.funbridge.server.tournament.serie.SerieEasyChallengeMgr;
import com.funbridge.server.tournament.serie.SerieTopChallengeMgr;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.serie.data.*;
import com.funbridge.server.tournament.serie.memory.*;
import com.funbridge.server.tournament.team.TourTeamMgr;
import com.funbridge.server.tournament.team.data.TeamGame;
import com.funbridge.server.tournament.team.data.TeamTournamentPlayer;
import com.funbridge.server.tournament.team.memory.TeamMemTournamentPlayer;
import com.funbridge.server.tournament.timezone.TimezoneMgr;
import com.funbridge.server.tournament.timezone.data.TimezoneGame;
import com.funbridge.server.tournament.timezone.data.TimezoneTournamentPlayer;
import com.funbridge.server.tournament.training.TrainingMgr;
import com.funbridge.server.tournament.training.data.TrainingGame;
import com.funbridge.server.tournament.training.data.TrainingTournamentPlayer;
import com.funbridge.server.tournament.training.memory.TrainingMemTournamentPlayer;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.player.PlayerSettingsData;
import com.funbridge.server.ws.tournament.WSPrivateTournamentProperties;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service(value="supportService")
@Scope(value="singleton")
public class SupportServiceImpl extends FunbridgeMgr implements SupportService {
	@Resource(name="playerMgr")
	private PlayerMgr playerMgr = null;
	@Resource(name="presenceMgr")
	private PresenceMgr presenceMgr = null;
	@Resource(name="transactionDAO")
	private TransactionDAO transactionDAO = null;
	@Resource(name="messageNotifMgr")
	private MessageNotifMgr notifMgr = null;
    @Resource(name = "tourSerieMgr")
    private TourSerieMgr serieMgr = null;
    @Resource(name = "trainingMgr")
    private TrainingMgr trainingMgr = null;
    @Resource(name = "timezoneMgr")
    private TimezoneMgr timezoneMgr = null;
    @Resource(name="serieTopChallengeMgr")
    private SerieTopChallengeMgr serieTopChallengeMgr = null;
	@Resource(name="serieEasyChallengeMgr")
	private SerieEasyChallengeMgr serieEasyChallengeMgr = null;
    @Resource(name = "duelMgr")
    private DuelMgr duelMgr = null;
    @Resource(name="tourTeamMgr")
    private TourTeamMgr tourTeamMgr = null;
    @Resource(name = "privateTournamentMgr")
    private PrivateTournamentMgr privateMgr = null;

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

    public WSSupPlayer buildWSSupPlayer(Player p) {
        WSSupPlayer supPlayer = new WSSupPlayer();
        supPlayer.plaID = p.getID();
        supPlayer.pseudo = p.getNickname();
        supPlayer.mail = p.getMail();
        supPlayer.password = p.getPassword();
        supPlayer.firstName = p.getFirstName();
        supPlayer.lastName = p.getLastName();
        supPlayer.presentation = p.getDescription();
        supPlayer.town = p.getTown();
        supPlayer.lang = p.getLang();
        supPlayer.displayLang = p.getDisplayLang();
        supPlayer.country = p.getCountryCode();
        supPlayer.displayCountry = p.getDisplayCountryCode();
        supPlayer.avatar = p.isAvatarPresent();
        supPlayer.credit = p.getCreditAmount();

        if (p.getCreationDate() > 0) {
            supPlayer.dateCreation = Constantes.timestamp2StringDateHour(p.getCreationDate());
        }
        if (p.getLastConnectionDate() > 0) {
            supPlayer.dateLastConnection = Constantes.timestamp2StringDateHour(p.getLastConnectionDate());
        }
        if (p.getDateLastCreditUpdate() > 0) {
            supPlayer.dateLastCreditUpdate = Constantes.timestamp2StringDateHour(p.getDateLastCreditUpdate());
        }
        if (p.getBirthday() != null) {
            supPlayer.dateBirthday = Constantes.timestamp2StringDate(p.getBirthday().getTime());
        }
        if (p.isDateSubscriptionValid()) {
            supPlayer.dateSubscriptionExpiration = Constantes.timestamp2StringDateHour(p.getSubscriptionExpirationDate());
        }
        supPlayer.buyCredit = ContextManager.getStoreMgr().getNbTransactionsForPlayer(supPlayer.plaID, false) > 0;
        supPlayer.sex = p.getSex();
        TourSeriePlayer tsp = serieMgr.getTourSeriePlayer(p.getID());
        if (tsp != null) {
            supPlayer.serie = tsp.getSerie();
            supPlayer.serieBestPeriod = tsp.getBestPeriod();
            supPlayer.serieBestSerie = tsp.getBestSerie();
            supPlayer.serieBestRank = tsp.getBestRank();
        }
		PlayerHandicap ph = playerMgr.getPlayerHandicap(p.getID());
		if (ph != null) {
			supPlayer.handicap = ph.getHandicap();
		}
        supPlayer.conventionProfil = p.getConventionProfile();
        if (supPlayer.conventionProfil == 0) {
            supPlayer.conventionProfil = ContextManager.getArgineEngineMgr().getDefaultProfile();
        }
        supPlayer.conventionValue = playerMgr.getConventionFreeDataForPlayer(p);
        PlayerSettingsData playerSettingsData = playerMgr.getPlayerSettingsData(p);
        if (playerSettingsData != null) {
            supPlayer.cardsConventionProfil = playerSettingsData.conventionCards;
            if (supPlayer.cardsConventionProfil == 0) {
                supPlayer.cardsConventionProfil = ContextManager.getArgineEngineMgr().getDefaultProfileCards();
            }
            supPlayer.cardsConventionValue = playerSettingsData.conventionCardsFreeProfile;
        } else {
            supPlayer.cardsConventionProfil = ContextManager.getArgineEngineMgr().getDefaultProfileCards();
        }
		Team team = ContextManager.getTeamMgr().getTeamForPlayer(supPlayer.plaID);
		if (team != null){
			supPlayer.team = ContextManager.getTeamMgr().toWSTeam(team, supPlayer.plaID, -1);
		}
		PlayerLocation playerLocation = playerMgr.getPlayerLocation(p.getID());
		if (playerLocation != null) {
            supPlayer.location = new WSSupPlayerLocation(playerLocation);
        }
        return supPlayer;
    }

	@Override
	public WSSupResponse searchPlayer(WSSupSearchPlayerParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			if (param.isPlayerIDValid()) {
				Player p = playerMgr.getPlayer(param.playerID);
				if (p != null) {
					response.setData(buildWSSupPlayer(p));
				}
			} else {
				List<Player> listPla = null;
				if (param.isMailValid()) {
					listPla = playerMgr.searchPlayerWithMailOrPseudo(param.mail);
				} else if (param.isPseudoValid()) {
					listPla = playerMgr.searchPlayerWithMailOrPseudo(param.pseudo);
				} else {
					response.setException(WSSupConstantes.EXCEPTION_COMMON_SERVER_ERROR);
				}
				if (listPla != null) {
					List<WSSupPlayer> listSupPla = new ArrayList<WSSupPlayer>();
					for (Player p : listPla) {
						listSupPla.add(buildWSSupPlayer(p));
					}
					response.setData(listSupPla);
				}
			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse getPlayer(WSSupGetPlayerParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			Player p = null;
			if (param.isPlayerIDValid()) {
				p = playerMgr.getPlayer(param.playerID);
			} else if (param.isMailValid()) {
				p = playerMgr.getPlayerByMail(param.mail);
			} else if (param.isPseudoValid()) {
				p = playerMgr.getPlayerByPseudo(param.pseudo);
			}
			if (p != null) {
				response.setData(buildWSSupPlayer(p));
			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse listDeviceForPlayer(WSSupListDeviceForPlayerParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
//			List<Device> listPlaDev = playerMgr.getListDeviceForPlayer(param.playerID);
            List<PlayerDevice> listPlaDev = playerMgr.getListPlayerDeviceForPlayer(param.playerID);
			List<WSSupDevice> listDevice = new ArrayList<WSSupDevice>();
			if (listPlaDev != null) {
//				for (Device d : listPlaDev) {
                for (PlayerDevice pd : listPlaDev) {
					listDevice.add(new WSSupDevice(pd));
				}
				response.setData(listDevice);
			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse listDeviceForPlayerMail(WSSupListDeviceForPlayerMailParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			Player p = playerMgr.getPlayerByMail(param.playerMail);
			if (p != null) {
//				List<Device> listPlaDev = playerMgr.getListDeviceForPlayer(p.getID());
                List<PlayerDevice> listPlaDev = playerMgr.getListPlayerDeviceForPlayer(p.getID());
				List<WSSupDevice> listDevice = new ArrayList<WSSupDevice>();
				if (listPlaDev != null) {
//                    for (Device d : listPlaDev) {
					for (PlayerDevice pd : listPlaDev) {
						listDevice.add(new WSSupDevice(pd));
					}
					response.setData(listDevice);
				}
			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse listTransactionForPlayer(WSSupListTransactionForPlayerParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			List<Transaction> temp = transactionDAO.listForPlayer(param.playerID, param.offset, param.nbMax);
			List<WSSupTransaction> listTransaction = new ArrayList<WSSupTransaction>();
			if (temp != null) {
				for (Transaction st : temp) {
					listTransaction.add(new WSSupTransaction(st));
				}
				response.setData(listTransaction);
			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse listConnectionForPlayer(WSSupListConnectionForPlayerParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
            List<WSSupConnection> listConnection = new ArrayList<WSSupConnection>();
            List<PlayerConnectionHistory2> temp2 = playerMgr.getListConnectionHistory2(param.playerID, param.offset, param.nbMax);
            if (temp2 != null) {
                for (PlayerConnectionHistory2 pch : temp2) {
                    listConnection.add(new WSSupConnection(pch));
                }
            }
            response.setData(listConnection);
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse getDevice(WSSupGetDeviceParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			Device dev = playerMgr.getDevice(param.deviceID);
			if (dev != null) {
				response.setData(new WSSupDevice(dev));
			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse getPlayerAvatar(WSSupGetPlayerAvatarParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			Player p = playerMgr.getPlayer(param.playerID);
			if (p != null && p.isAvatarPresent()) {
				try {
					File fileAvatar = playerMgr.getAvatarFileForPlayer(p.getID());
					// avatar for this player exists ?
					if (!fileAvatar.exists()) {
						log.error("No avatar found for player="+p.getID());
					} else {
						byte[] byData = FileUtils.readFileToByteArray(fileAvatar);
						// transform byte data to string using base64
						String imageData = Base64.encodeBase64String(byData);
						response.setData(imageData);
					}
				} catch (Exception e) {
					log.error("Error to read avatar image for player="+p.getID(),e);
					response.setException(WSSupConstantes.EXCEPTION_COMMON_SERVER_ERROR);
				}
			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse validatePlayerAvatar(com.funbridge.server.ws.support.SupportService.WSSupValidatePlayerAvatarParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			Player p = playerMgr.getPlayer(param.playerID);
			if (p != null) {
				FBSession session = presenceMgr.getSessionForPlayer(p);
				if (session == null) {
					if (p.isAvatarPresent()) {
						try {
							playerMgr.updatePlayerToDB(p, PlayerUpdateType.PROFILE);
							response.setData(new Boolean(true));
						} catch (Exception e) {
							log.error("Exception to update player in DB", e);
							response.setException(WSSupConstantes.EXCEPTION_COMMON_SERVER_ERROR);
						}
					} else {
						response.setData(new Boolean(false));
					}
				} else {
					response.setException(WSSupConstantes.EXCEPTION_COMMON_PLAYER_CONNECTED);
				}
			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse removePlayerAvatar(WSSupRemovePlayerAvatarParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			Player p = playerMgr.getPlayer(param.playerID);
			if (p != null) {
				FBSession session = presenceMgr.getSessionForPlayer(p);
				if (session == null) {
					response.setData(new Boolean(playerMgr.removeAvatarFileForPlayer(p)));
				} else {
					response.setException(WSSupConstantes.EXCEPTION_COMMON_PLAYER_CONNECTED);
				}
			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse modifyPlayer(WSSupModifyPlayerParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			Player p = playerMgr.getPlayer(param.playerID);
			if (p != null) {
				FBSession session = presenceMgr.getSessionForPlayer(p);
				if (session == null) {
					boolean bUpdateOK = true;
					if (!param.mail.equals(p.getMail())) {
						// check mail not existing !
						if (playerMgr.existMail(param.mail)) {
							bUpdateOK = false;
							response.setException(WSSupConstantes.EXCEPTION_PLAYER_MAIL_EXISTING);
						}
						p.setMail(param.mail);
					}
					if (!param.pseudo.equals(p.getNickname())) {
						param.pseudo = param.pseudo.trim();
                        if (!PlayerMgr.checkPseudoFormat(param.pseudo)) {
                            log.error("Peusdo format not valid - pseudo="+param.pseudo);
							bUpdateOK = false;
							response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
						}
						// check pseudo not existing !
						else if (playerMgr.existPseudo(param.pseudo)) {
							bUpdateOK = false;
							response.setException(WSSupConstantes.EXCEPTION_PLAYER_PSEUDO_EXISTING);
						} else {
							p.setNickname(param.pseudo);
						}
					}
                    if (!param.password.equals(p.getPassword())) {
                        if (!PlayerMgr.checkPasswordFormat(param.password)) {
                            log.error("Password format not valid - password="+param.password);
                            bUpdateOK = false;
                            response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
                        } else {
                            p.setPassword(param.password);
                        }
                    }
					if (bUpdateOK) {
						p.setFirstName(param.firstName);
						p.setLastName(param.lastName);
						p.setTown(param.town);
						p.setDescription(param.presentation);
						
						try {
							playerMgr.updatePlayerToDB(p, PlayerUpdateType.PROFILE);
							response.setData(buildWSSupPlayer(p));
						} catch (Exception e) {
							log.error("Exception to update player in DB", e);
							response.setException(WSSupConstantes.EXCEPTION_COMMON_SERVER_ERROR);
						}
					}
				} else {
					response.setException(WSSupConstantes.EXCEPTION_COMMON_PLAYER_CONNECTED);
				}
			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse isPlayerConnected(WSSupIsPlayerConnectedParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			FBSession session = null;
			Player p = playerMgr.getPlayer(param.playerID);
			if (p != null) {
				session = presenceMgr.getSessionForPlayer(p);
			}
			response.setData(new Boolean(session != null));
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse disconnectPlayer(WSSupDisconnectPlayerParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			Player p = playerMgr.getPlayer(param.playerID);
			if (p != null) {
                presenceMgr.closeSessionForPlayer(p.getID(), null);
				response.setData(new Boolean(true));
			} else {
				response.setData(new Boolean(false));
			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse listPlayerForDevice(WSSupListPlayerForDeviceParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			List<Player> lisPla = playerMgr.getListPlayerForDevice(param.deviceID);
			List<WSSupPlayer> listSupPla = new ArrayList<WSSupPlayer>();
			if (lisPla != null) {
				for(Player p : lisPla) {
					listSupPla.add(buildWSSupPlayer(p));
				}
				response.setData(listSupPla);
			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}
	
	@Override
	public WSSupResponse sendMessageSupport(WSSupSendMessageSupportParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			try {
				WSSupSendMessageSupportResponse resp = new WSSupSendMessageSupportResponse();
                if (log.isDebugEnabled()) {
                    log.debug("Param="+param);
                }
				Player recipient = playerMgr.getPlayer(param.recipient);
				if (recipient == null) {
					throw new Exception(WSSupConstantes.EXCEPTION_PLAYER_NOT_FOUND);
				}
				MessageNotif notif = notifMgr.createNotifMessageSupport(recipient.getID(), param.message);
				if (notif != null) {
					FBSession sessionPlayer = presenceMgr.getSessionForPlayerID(recipient.getID());
					if (sessionPlayer != null) {
						sessionPlayer.pushEvent(notifMgr.buildEvent(notif, recipient));
					}
					resp.result = true;
				}
				response.setData(resp);
			} catch (Exception e) {
				response.setException(e.getMessage());
			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}

	@Override
	public WSSupResponse getSerieHistoricForPlayer(WSSupGetSerieHistoricForPlayerParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
		    response.setData(serieMgr.getWSSerieHistoricForPlayer(param.playerID));
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}

    @Override
    public WSSupResponse listPeriodResultForPlayer(WSSupListPeriodResultForPlayerParam param) {
        WSSupResponse response = new WSSupResponse();
        if (param != null && param.isValid()) {
            List<WSSupPeriodResult> listSupPeriodResult = new ArrayList<WSSupPeriodResult>();
            List<TourSeriePeriodResult> listNewSeriePeriodResult = serieMgr.listPeriodResultForPlayer(param.playerID, param.offset, param.nbMax);
            for (TourSeriePeriodResult tpr : listNewSeriePeriodResult) {
                if (listSupPeriodResult.size() == param.nbMax) {
                    break;
                }
                listSupPeriodResult.add(new WSSupPeriodResult(tpr));
            }
            response.setData(listSupPeriodResult);
        } else {
            response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
        }
        return response;
    }

	@Override
	public WSSupResponse resetPassword(WSSupResetPasswordParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
            response.setException(WSSupConstantes.EXCEPTION_COMMON_DEPRECATED);
//			Player p = playerMgr.getPlayer(param.playerID);
//			if (p != null) {
//				response.setData(new Boolean(playerMgr.resetPassword(p)));
//			} else {
//				response.setException(WSSupConstantes.EXCEPTION_PLAYER_NOT_FOUND);
//			}
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}

	@Override
	public WSSupResponse certifyPlayer(WSSupCertifyPlayerParam param) {
		return null;
	}

	@Override
	public WSSupResponse listTournamentPlayForPlayer(WSSupListTournamentPlayForPlayerParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			log.debug(param);
			List<WSSupTournamentPlay> listTourPlay = new ArrayList<WSSupTournamentPlay>();
			if (param.category == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
                List<TrainingMemTournamentPlayer> listMemTourPlayer = trainingMgr.getMemoryMgr().listMemTournamentWithPlayer(param.playerID, param.offset, param.nbMax);
                for (TrainingMemTournamentPlayer e : listMemTourPlayer) {
                    if (listTourPlay.size() >= param.nbMax) {break;}
                    WSSupTournamentPlay wse = new WSSupTournamentPlay();
                    wse.tourIDstr = e.getMemTournament().getTournamentID();
                    wse.name = "TRAINING - "+Constantes.tournamentResultType2String(e.getMemTournament().getResultType());
                    wse.finished = true;
                    wse.dateStart = Constantes.timestamp2StringDateHour(e.getMemTournament().getStartDate());
                    wse.dateEnd = Constantes.timestamp2StringDateHour(e.getMemTournament().getEndDate());
                    wse.category = Constantes.TOURNAMENT_CATEGORY_TRAINING;
                    wse.nbDeal = e.getMemTournament().getNbDeals();
                    wse.nbPlayer = e.getMemTournament().getNbPlayersWhoFinished();
                    wse.playerDateStart = Constantes.timestamp2StringDateHour(e.getStartDate());
                    wse.playerDateLast = Constantes.timestamp2StringDateHour(e.getLastPlayDate());
                    wse.playerRank = e.ranking;
                    wse.playerResult = e.result;
                    wse.playerFinished = false;
                    wse.resultType = e.getMemTournament().getResultType();
                    listTourPlay.add(wse);
                }
                List<TrainingTournamentPlayer> listPlayTraining = trainingMgr.listTournamentPlayer(param.playerID, param.offset, param.nbMax - listTourPlay.size());
                if (listPlayTraining != null && listPlayTraining.size() > 0) {
                    for (TrainingTournamentPlayer e : listPlayTraining) {
                        if (listTourPlay.size() >= param.nbMax) {break;}
                        WSSupTournamentPlay wse = new WSSupTournamentPlay();
                        wse.tourIDstr = e.getTournament().getIDStr();
                        wse.category = e.getTournament().getCategory();
                        wse.name = "TRAINING - "+Constantes.tournamentResultType2String(e.getTournament().getResultType());
                        wse.finished = e.getTournament().isFinished();
                        wse.dateStart = Constantes.timestamp2StringDateHour(e.getTournament().getStartDate());
                        wse.dateEnd = Constantes.timestamp2StringDateHour(e.getTournament().getEndDate());
                        wse.nbPlayer = e.getTournament().getNbPlayers();
                        wse.nbDeal = e.getTournament().getNbDeals();
                        wse.playerDateStart = Constantes.timestamp2StringDateHour(e.getStartDate());
                        wse.playerDateLast = Constantes.timestamp2StringDateHour(e.getLastDate());
                        wse.playerFinished = true;
                        wse.playerRank = e.getRank();
                        wse.playerResult = e.getResult();
                        wse.resultType = e.getTournament().getResultType();
                        listTourPlay.add(wse);
                    }
                }
			}
			else if (param.category == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
                List<GenericMemTournamentPlayer> listMemTourPlayer = timezoneMgr.getMemoryMgr().listMemTournamentWithPlayer(param.playerID, param.offset, param.nbMax);
                for (GenericMemTournamentPlayer e : listMemTourPlayer) {
                    GenericMemTournament tour = timezoneMgr.getMemoryMgr().getTournament(e.memTour.tourID);
                    if (tour != null) {
                        if (listTourPlay.size() >= param.nbMax) {
                            break;
                        }
                        WSSupTournamentPlay wse = new WSSupTournamentPlay();
                        wse.tourIDstr = e.memTour.tourID;
                        wse.name = tour.name;
                        wse.finished = false;
                        wse.dateStart = Constantes.timestamp2StringDateHour(tour.startDate);
                        wse.dateEnd = Constantes.timestamp2StringDateHour(tour.endDate);
                        wse.category = Constantes.TOURNAMENT_CATEGORY_TRAINING;
                        wse.nbDeal = tour.getNbDeal();
                        wse.nbPlayer = tour.getNbPlayerFinishAll();
                        wse.playerDateStart = Constantes.timestamp2StringDateHour(e.dateStart);
                        wse.playerDateLast = Constantes.timestamp2StringDateHour(e.dateLastPlay);
                        wse.playerRank = e.ranking;
                        wse.playerResult = e.result;
                        wse.playerFinished = e.isPlayerFinish();
                        wse.resultType = e.memTour.resultType;
                        listTourPlay.add(wse);
                    }
                }
                List<TimezoneTournamentPlayer> listPlayTimezone = timezoneMgr.listTournamentPlayer(param.playerID, param.offset, param.nbMax - listTourPlay.size());
                if (listPlayTimezone != null && listPlayTimezone.size() > 0) {
                    for (TimezoneTournamentPlayer e : listPlayTimezone) {
                        if (listTourPlay.size() >= param.nbMax) {break;}
                        WSSupTournamentPlay wse = new WSSupTournamentPlay();
                        wse.tourIDstr = e.getTournament().getIDStr();
                        wse.category = e.getTournament().getCategory();
                        wse.name = e.getTournament().getName();
                        wse.finished = e.getTournament().isFinished();
                        wse.dateStart = Constantes.timestamp2StringDateHour(e.getTournament().getStartDate());
                        wse.dateEnd = Constantes.timestamp2StringDateHour(e.getTournament().getEndDate());
                        wse.nbPlayer = e.getTournament().getNbPlayers();
                        wse.nbDeal = e.getTournament().getNbDeals();
                        wse.playerDateStart = Constantes.timestamp2StringDateHour(e.getStartDate());
                        wse.playerDateLast = Constantes.timestamp2StringDateHour(e.getLastDate());
                        wse.playerFinished = true;
                        wse.playerRank = e.getRank();
                        wse.playerResult = e.getResult();
                        wse.resultType = e.getTournament().getResultType();
                        listTourPlay.add(wse);
                    }
                }
            }
            else if (param.category == Constantes.TOURNAMENT_CATEGORY_DUEL) {
                List<DuelTournamentPlayer> listPlayDuel = duelMgr.listTournamentPlayer(param.playerID, param.offset, param.nbMax);
                if (listPlayDuel != null && listPlayDuel.size() > 0) {
                    for (DuelTournamentPlayer e : listPlayDuel) {
                        if (listTourPlay.size() >= param.nbMax) {break;}
                        WSSupTournamentPlay wse = new WSSupTournamentPlay();
                        wse.tourIDstr = e.getTournament().getIDStr();
                        wse.category = e.getTournament().getCategory();
                        wse.name = "DUEL WITH "+e.getPartner(param.playerID);
                        wse.finished = e.getTournament().isFinished();
                        wse.dateStart = Constantes.timestamp2StringDateHour(e.getTournament().getCreationDate());
                        wse.dateEnd = Constantes.timestamp2StringDateHour(e.getTournament().getEndDate());
                        wse.nbPlayer = e.getTournament().getNbPlayers();
                        wse.nbDeal = e.getTournament().getNbDeals();
                        wse.playerDateStart = Constantes.timestamp2StringDateHour(e.getCreationDate());
                        wse.playerDateLast = Constantes.timestamp2StringDateHour(e.isFinished()?e.getFinishDate():e.getCreationDate());
                        wse.playerFinished = e.isFinished();
                        wse.playerRank = e.getRankForPlayer(param.playerID);
                        wse.playerResult = e.getResultForPlayer(param.playerID);
                        wse.opponentResult = e.getResultForPlayer(e.getPlayer2IDForAsk(param.playerID));
                        wse.resultType = e.getTournament().getResultType();
                        listTourPlay.add(wse);
                    }
                }
            }
            else if (param.category == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
                TourSeriePlayer tsp = serieMgr.getTourSeriePlayer(param.playerID);
                if (tsp != null) {
                    TourSerieMemPeriodRanking ranking = serieMgr.getSerieRanking(tsp.getSerie());
                    if (ranking != null) {
                        // current tournament in progress ?
                        TourSerieMemTour mtInProgress = serieMgr.getMemoryMgr().getTournamentInProgressForPlayer(tsp.getSerie(), param.playerID);
                        WSSupTournamentPlay eInProgress = null;
                        if (mtInProgress != null) {
                            TourSerieMemTourPlayer mtp = mtInProgress.getRankingPlayer(param.playerID);
                            if (mtp != null) {
                                eInProgress = new WSSupTournamentPlay();
                                eInProgress.tourIDstr = mtInProgress.tourID;
                                eInProgress.name = tsp.getSerie();
                                eInProgress.finished = false;
                                eInProgress.dateStart = Constantes.timestamp2StringDateHour(TourSerieMgr.transformPeriodID2TS(mtInProgress.periodID, true));
                                eInProgress.dateEnd = Constantes.timestamp2StringDateHour(TourSerieMgr.transformPeriodID2TS(mtInProgress.periodID, false));
                                eInProgress.category = Constantes.TOURNAMENT_CATEGORY_NEWSERIE;
                                eInProgress.nbDeal = mtInProgress.getNbDeals();
                                eInProgress.nbPlayer = mtInProgress.getNbPlayersForRanking();
                                eInProgress.playerDateLast = Constantes.timestamp2StringDateHour(mtp.dateLastPlay);
                                eInProgress.playerRank = mtp.ranking;
                                eInProgress.playerResult = mtp.result;
                                eInProgress.playerFinished = false;
                                eInProgress.resultType = Constantes.TOURNAMENT_RESULT_PAIRE;
                            }
                        }

                        // add current tournament in progress
                        if (param.offset == 0 && eInProgress != null) {
                            listTourPlay.add(eInProgress);
                        }
                        // other tournament played
                        TourSerieMemPeriodRankingPlayer rankingPlayer = ranking.getPlayerRank(param.playerID);
                        int offsetBDD = 0;
                        if (rankingPlayer != null) {
                            if (param.offset > rankingPlayer.getNbTournamentPlayed()) {
                                // not used data from memory ...
                                offsetBDD = param.offset - rankingPlayer.getNbTournamentPlayed();
                            } else if (rankingPlayer.listData.size() > 0) {
                                // use data from memory
                                int idx = rankingPlayer.listData.size() - param.offset - 1;
                                if (param.offset > 0) {
                                    if (eInProgress != null) {
                                        idx ++;
                                    }
                                }
                                while (true) {
                                    if (listTourPlay.size() == param.nbMax) {
                                        break;
                                    }
                                    if (idx >= 0 && idx < rankingPlayer.listData.size()) {
                                        TourSerieMemPeriodRankingPlayerData data = rankingPlayer.listData.get(idx);
                                        TourSerieMemTour mt = serieMgr.getMemoryMgr().getMemTour(tsp.getSerie(), data.tourID);
                                        if (mt != null) {
                                            WSSupTournamentPlay e = new WSSupTournamentPlay();
                                            e.tourIDstr = data.tourID;
                                            e.name = tsp.getSerie();
                                            e.finished = false;
                                            e.dateStart = Constantes.timestamp2StringDateHour(TourSerieMgr.transformPeriodID2TS(mt.periodID, true));
                                            e.dateEnd = Constantes.timestamp2StringDateHour(TourSerieMgr.transformPeriodID2TS(mt.periodID, false));
                                            e.category = Constantes.TOURNAMENT_CATEGORY_NEWSERIE;
                                            e.nbDeal = mt.getNbDeals();
                                            e.nbPlayer = mt.getNbPlayersForRanking();
                                            e.playerDateLast = Constantes.timestamp2StringDateHour(data.dateResult);
                                            e.playerRank = data.rank;
                                            e.playerResult = data.result;
                                            e.playerFinished = true;
                                            e.resultType = Constantes.TOURNAMENT_RESULT_PAIRE;
                                            listTourPlay.add(e);
                                        }
                                        idx--;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                        if (offsetBDD > 0 && eInProgress != null) {
                            offsetBDD --;
                        }
                        // if list not full, use data from BDD
                        if (listTourPlay.size() < param.nbMax) {
                            List<TourSerieTournamentPlayer> listTP = serieMgr.listTournamentPlayerAfterDate(param.playerID, 0, offsetBDD, param.nbMax - listTourPlay.size());
                            for (TourSerieTournamentPlayer tp : listTP) {
                                WSSupTournamentPlay e = new WSSupTournamentPlay();
                                e.tourIDstr = tp.getTournament().getIDStr();
                                e.name = tsp.getSerie();
                                e.finished = tp.getTournament().isFinished();
                                e.dateStart = Constantes.timestamp2StringDateHour(tp.getTournament().getTsDateStart());
                                e.dateEnd = Constantes.timestamp2StringDateHour(tp.getTournament().getTsDateEnd());
                                e.category = Constantes.TOURNAMENT_CATEGORY_NEWSERIE;
                                e.nbDeal = tp.getTournament().getNbDeals();
                                e.nbPlayer = tp.getTournament().getNbPlayers();
                                e.playerDateLast = Constantes.timestamp2StringDateHour(tp.getLastDate());
                                e.playerRank = tp.getRank();
                                e.playerResult = tp.getResult();
                                e.playerFinished = true;
                                e.resultType = Constantes.TOURNAMENT_RESULT_PAIRE;
                                listTourPlay.add(e);
                            }
                        }
                    } else {
                        log.error("No ranking found for serie="+tsp.getSerie()+" - param="+param);
                    }
                } else {
                    log.error("No TourSeriePlayer found for player="+param.playerID);
                }
            }
            else if (param.category == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
                List<SerieTopChallengeTournamentPlayer> listPlay = serieTopChallengeMgr.listTournamentPlayerForPlayer(param.playerID, param.offset, param.nbMax);
                if (listPlay != null && listPlay.size() > 0) {
                    for (SerieTopChallengeTournamentPlayer e : listPlay) {
                        if (listTourPlay.size() >= param.nbMax) {break;}
                        WSSupTournamentPlay wse = new WSSupTournamentPlay();
                        wse.tourIDstr = e.getTournament().getIDStr();
                        wse.category = Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE;
                        wse.name = e.getTournament().getSerie();
                        wse.finished = e.getTournament().isFinished();
                        wse.dateStart = Constantes.timestamp2StringDateHour(e.getTournament().getTsDateStart());
                        wse.dateEnd = Constantes.timestamp2StringDateHour(e.getTournament().getTsDateEnd());
                        wse.nbPlayer = e.getTournament().getNbPlayers() + 1;
                        wse.nbDeal = e.getTournament().getNbDeals();
                        wse.playerDateStart = Constantes.timestamp2StringDateHour(e.getStartDate());
                        wse.playerDateLast = Constantes.timestamp2StringDateHour(e.getLastDate());
                        wse.playerFinished = true;
                        wse.playerRank = e.getRank();
                        wse.playerResult = e.getResult();
                        wse.resultType = e.getTournament().getResultType();
                        listTourPlay.add(wse);
                    }
                }
            }
			else if (param.category == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
				List<SerieEasyChallengeTournamentPlayer> listPlay = serieEasyChallengeMgr.listTournamentPlayerForPlayer(param.playerID, param.offset, param.nbMax);
				if (listPlay != null && listPlay.size() > 0) {
					for (SerieEasyChallengeTournamentPlayer e : listPlay) {
						if (listTourPlay.size() >= param.nbMax) {break;}
						WSSupTournamentPlay wse = new WSSupTournamentPlay();
						wse.tourIDstr = e.getTournament().getIDStr();
						wse.category = Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE;
						wse.name = e.getTournament().getSerie();
						wse.finished = e.getTournament().isFinished();
						wse.dateStart = Constantes.timestamp2StringDateHour(e.getTournament().getTsDateStart());
						wse.dateEnd = Constantes.timestamp2StringDateHour(e.getTournament().getTsDateEnd());
						wse.nbPlayer = e.getTournament().getNbPlayers() + 1;
						wse.nbDeal = e.getTournament().getNbDeals();
						wse.playerDateStart = Constantes.timestamp2StringDateHour(e.getStartDate());
						wse.playerDateLast = Constantes.timestamp2StringDateHour(e.getLastDate());
						wse.playerFinished = true;
						wse.playerRank = e.getRank();
						wse.playerResult = e.getResult();
						wse.resultType = e.getTournament().getResultType();
						listTourPlay.add(wse);
					}
				}
			}
            else if (param.category == Constantes.TOURNAMENT_CATEGORY_TEAM) {
			    int memOffset = 0;
			    TeamMemTournamentPlayer memTournamentPlayer = tourTeamMgr.getMemoryMgr().getMemTournamentPlayer(param.playerID);
                if (memTournamentPlayer != null) {
                    if (param.offset == 0) {
                        WSSupTournamentPlay wse = new WSSupTournamentPlay();
                        wse.tourIDstr = memTournamentPlayer.memTour.tourID;
                        wse.name = memTournamentPlayer.memTour.name;
                        wse.finished = memTournamentPlayer.isPlayerFinish();
                        wse.dateStart = Constantes.timestamp2StringDateHour(memTournamentPlayer.memTour.endDate - (24 * 60 * 60 * 1000));
                        wse.dateEnd = Constantes.timestamp2StringDateHour(memTournamentPlayer.memTour.endDate);
                        wse.category = Constantes.TOURNAMENT_CATEGORY_TEAM;
                        wse.nbDeal = memTournamentPlayer.memTour.getNbDeals();
                        wse.nbPlayer = memTournamentPlayer.memTour.getNbPlayers();
                        wse.playerDateStart = Constantes.timestamp2StringDateHour(memTournamentPlayer.dateStart);
                        wse.playerDateLast = Constantes.timestamp2StringDateHour(memTournamentPlayer.dateLastPlay);
                        wse.playerRank = memTournamentPlayer.ranking;
                        wse.playerResult = memTournamentPlayer.result;
                        wse.playerFinished = false;
                        wse.resultType = Constantes.TOURNAMENT_RESULT_IMP;
                        listTourPlay.add(wse);
                    } else {
                        memOffset = 1;
                    }
                }
                List<TeamTournamentPlayer> listPlay = tourTeamMgr.listTournamentPlayerAfterDate(param.playerID, 0, param.offset - memOffset, param.nbMax - listTourPlay.size());
                if (listPlay != null && listPlay.size() > 0) {
                    for (TeamTournamentPlayer e : listPlay) {
                        if (listTourPlay.size() >= param.nbMax) {
                            break;
                        }
                        WSSupTournamentPlay wse = new WSSupTournamentPlay();
                        wse.tourIDstr = e.getTournament().getIDStr();
                        wse.category = e.getTournament().getCategory();
                        wse.name = e.getTournament().getName();
                        wse.finished = e.getTournament().isFinished();
                        wse.dateStart = Constantes.timestamp2StringDateHour(e.getTournament().getStartDate());
                        wse.dateEnd = Constantes.timestamp2StringDateHour(e.getTournament().getEndDate());
                        wse.nbPlayer = e.getTournament().getNbPlayers();
                        wse.nbDeal = e.getTournament().getNbDeals();
                        wse.playerDateStart = Constantes.timestamp2StringDateHour(e.getStartDate());
                        wse.playerDateLast = Constantes.timestamp2StringDateHour(e.getLastDate());
                        wse.playerFinished = true;
                        wse.playerRank = e.getRank();
                        wse.playerResult = e.getResult();
                        wse.resultType = e.getTournament().getResultType();
                        listTourPlay.add(wse);
                    }
                }
            }
            else {
				TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(param.category);
				if (tournamentMgr != null) {
					List<GenericMemTournamentPlayer> listMemTourPlayer = tournamentMgr.getMemoryMgr().listMemTournamentWithPlayer(param.playerID, param.offset, param.nbMax);
					for (GenericMemTournamentPlayer e : listMemTourPlayer) {
						if (listTourPlay.size() >= param.nbMax) {break;}
						WSSupTournamentPlay wse = new WSSupTournamentPlay();
						wse.tourIDstr = e.memTour.tourID;
						wse.name = e.memTour.name;
						wse.finished = e.isPlayerFinish();
						wse.dateStart = Constantes.timestamp2StringDateHour(e.memTour.endDate - (24*60*60*1000));
						wse.dateEnd = Constantes.timestamp2StringDateHour(e.memTour.endDate);
						wse.category = param.category;
						wse.nbDeal = e.memTour.getNbDeal();
						wse.nbPlayer = e.memTour.getNbPlayer();
						wse.playerDateStart = Constantes.timestamp2StringDateHour(e.dateStart);
						wse.playerDateLast = Constantes.timestamp2StringDateHour(e.dateLastPlay);
						wse.playerRank = e.ranking;
						wse.playerResult = e.result;
						wse.playerFinished = false;
						wse.resultType = e.memTour.resultType;
						listTourPlay.add(wse);
					}
					List<TournamentPlayer> listPlay = tournamentMgr.listTournamentPlayer(param.playerID, param.offset, param.nbMax - listTourPlay.size());
					if (listPlay != null && listPlay.size() > 0) {
						for (TournamentPlayer e : listPlay) {
							if (listTourPlay.size() >= param.nbMax) {break;}
							WSSupTournamentPlay wse = new WSSupTournamentPlay();
							wse.tourIDstr = e.getTournament().getIDStr();
							wse.category = e.getTournament().getCategory();
							wse.name = e.getTournament().getName();
							wse.finished = e.getTournament().isFinished();
							wse.dateStart = Constantes.timestamp2StringDateHour(e.getTournament().getStartDate());
							wse.dateEnd = Constantes.timestamp2StringDateHour(e.getTournament().getEndDate());
							wse.nbPlayer = e.getTournament().getNbPlayers();
							wse.nbDeal = e.getTournament().getNbDeals();
							wse.playerDateStart = Constantes.timestamp2StringDateHour(e.getStartDate());
							wse.playerDateLast = Constantes.timestamp2StringDateHour(e.getLastDate());
							wse.playerFinished = true;
							wse.playerRank = e.getRank();
							wse.playerResult = e.getResult();
							wse.resultType = e.getTournament().getResultType();
							listTourPlay.add(wse);
						}
					}
				}
			}
			response.setData(listTourPlay);
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}

	@Override
	public WSSupResponse listGamePlayForTournamentAndPlayer(WSSupListGamePlayForTournamentAndPlayerParam param) {
		WSSupResponse response = new WSSupResponse();
		if (param != null && param.isValid()) {
			List<WSSupGamePlay> listGamePlay = new ArrayList<WSSupGamePlay>();
			if (param.category == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
				TrainingGame game = trainingMgr.getGameOnTournamentAndDealForPlayer(param.tourIDstr, 1, param.playerID);
				if (game != null) {
					listGamePlay.add(new WSSupGamePlay(game));
				}
			} else if (param.category == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
                try {
                    List<TourSerieGame> listGame = serieMgr.listGameOnTournamentForPlayer(param.tourIDstr, param.playerID);
                    for (TourSerieGame g : listGame) {
                        listGamePlay.add(new WSSupGamePlay(g));
                    }
                } catch (Exception e) {
                    log.error("Failed to list game on tournament for player - param="+param);
                }
            } else if (param.category == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
                try {
                    List<SerieTopChallengeGame> listGame = serieTopChallengeMgr.listGameForPlayerOnTournament(param.tourIDstr, param.playerID);
                    for (SerieTopChallengeGame g : listGame) {
                        listGamePlay.add(new WSSupGamePlay(g));
                    }
                } catch (Exception e) {
                    log.error("Failed to list game on tournament for player - param="+param);
                }
			} else if (param.category == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
				try {
					List<SerieEasyChallengeGame> listGame = serieEasyChallengeMgr.listGameForPlayerOnTournament(param.tourIDstr, param.playerID);
					for (SerieEasyChallengeGame g : listGame) {
						listGamePlay.add(new WSSupGamePlay(g));
					}
				} catch (Exception e) {
					log.error("Failed to list game on tournament for player - param="+param);
				}
            } else if (param.category == Constantes.TOURNAMENT_CATEGORY_DUEL) {
                List<DuelGame> listGame = duelMgr.listGameOnTournamentForPlayer(param.tourIDstr, param.playerID);
                if (listGame != null) {
                    for (DuelGame g : listGame) {
                        listGamePlay.add(new WSSupGamePlay(g));
                    }
                }
            } else if (param.category == Constantes.TOURNAMENT_CATEGORY_TEAM) {
                List<TeamGame> listGame = tourTeamMgr.listGameOnTournamentForPlayer(param.tourIDstr, param.playerID);
                if (listGame != null) {
                    for (TeamGame g : listGame) {
                        listGamePlay.add(new WSSupGamePlay(g));
                    }
                }
            } else if (param.category == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
				List<TimezoneGame> listGame = timezoneMgr.listGameOnTournamentForPlayer(param.tourIDstr, param.playerID);
				if (listGame != null) {
					for (TimezoneGame g : listGame) {
						listGamePlay.add(new WSSupGamePlay(g));
					}
				}
            } else {
				TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(param.category);
				if (tournamentMgr != null) {
					final List<Game> listGame = tournamentMgr.listGameOnTournamentForPlayer(param.tourIDstr, param.playerID);
					if (listGame != null) {
						for (Game g : listGame) {
							listGamePlay.add(new WSSupGamePlay(g));
						}
					}
				} else {
					log.error("Category not supported ! param=" + param);
					response.setException(WSSupConstantes.EXCEPTION_COMMON_DEPRECATED);
				}
            }
			response.setData(listGamePlay);
		} else {
			response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
		}
		return response;
	}

    @Override
    public WSSupResponse changePlayerSerie(WSSupChangePlayerSerieParam param) {
        WSSupResponse response = new WSSupResponse();
        if (param != null && param.isValid()) {
            String resultOperation = "";
            boolean result = false;
            TourSeriePlayer seriePlayer = serieMgr.getTourSeriePlayer(param.playerID);
            if (seriePlayer != null) {
                String previousSerie = seriePlayer.getSerie();
                if (previousSerie.equals(param.newSerie)) {
                    resultOperation = "Same serie ! nothing to do !!";
                } else {
                    resultOperation = "Change serie " + previousSerie + " to " + param.newSerie + " for player=" + param.playerID;
                    // close session for player
                    ContextManager.getPresenceMgr().closeSessionForPlayer(param.playerID, null);
                    int nbGameRemoved = serieMgr.removeAllResultForTournamentInProgress(previousSerie, param.playerID);
                    resultOperation += " - Remove all results for tournament in progress for player=" + param.playerID + " - nbGameRemoved=" + nbGameRemoved;

                    List<Long> listPlayerID = new ArrayList<Long>();
                    listPlayerID.add(param.playerID);
                    // change player serie
                    if (serieMgr.updateSerieForPlayers(listPlayerID, param.newSerie, true)) {
                        resultOperation += " - Success to set serie " + param.newSerie;
                        TourSerieMemPeriodRanking memPeriodRankingPrevious = serieMgr.getSerieRanking(previousSerie);
                        if (memPeriodRankingPrevious != null) {
                            // remove player ranking
                            memPeriodRankingPrevious.mapRankingPlayer.remove(param.playerID);
                            // update nb players
                            resultOperation += " - update nb players in serie=" + previousSerie + " before=" + memPeriodRankingPrevious.getNbPlayerForRanking();
                            memPeriodRankingPrevious.updateNbPlayerSerie(serieMgr.countPlayerInSerieExcludeReserve(previousSerie, true));
                            resultOperation += " after=" + memPeriodRankingPrevious.getNbPlayerForRanking();
                            // update ranking
                            memPeriodRankingPrevious.computeRanking();
                        }
                        if (!param.newSerie.equals(TourSerieMgr.SERIE_NC)) {
                            TourSerieMemPeriodRanking memPeriodRanking = serieMgr.getSerieRanking(param.newSerie);
                            if (memPeriodRanking != null) {
                                // add player in period ranking (else player not in ranking !)
                                memPeriodRanking.getOrCreatePlayerRank(param.playerID);
                                resultOperation += " - update nb players in serie=" + param.newSerie + " before=" + memPeriodRanking.getNbPlayerForRanking();
                                // update nb players
                                memPeriodRanking.updateNbPlayerSerie(serieMgr.countPlayerInSerieExcludeReserve(param.newSerie, true));
                                resultOperation += " after=" + memPeriodRanking.getNbPlayerForRanking();
                                // update ranking
                                memPeriodRanking.computeRanking();
                            } else {
                                resultOperation += "Failed to find memPeriodRanking for serie " + param.newSerie;
                            }
                        } else {
                            resultOperation += " - No need to reload nbPlayer for serie " + param.newSerie;
                        }
                        PlayerCache playerCache = ContextManager.getPlayerCacheMgr().getAndLoadPlayerCache(param.playerID);
                        resultOperation += " - Player cache after update = " + playerCache;
                        result = true;
                    } else {
                        resultOperation += " - Failed to set serie " + param.newSerie;
                    }
                    if (result && param.messageNotif != null && param.messageNotif.length() > 0) {
                        notifMgr.createNotifMessageSupport(param.playerID, param.messageNotif);
                        resultOperation += " - Notif sent : " + param.messageNotif;
                    }
                    log.warn("ChangePlayerSerie - Param=" + param + " - resultOperation=" + resultOperation);
                }
            } else {
                resultOperation = "FAILED to get serie for player=" + param.playerID;
            }
            WSSupChangePlayerSerieResponse resp= new WSSupChangePlayerSerieResponse();
            resp.result = result;
            resp.info = resultOperation;
            response.setData(resp);
        } else {
            log.error("Parameter not valid - param="+param);
            response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
        }
        return response;
    }

    @Override
    public WSSupResponse listChatroomsForTournament(WSListChatroomsForTournamentParam param) {
        WSSupResponse response = new WSSupResponse();
        if (param != null && param.isValid()) {
            if (log.isDebugEnabled()) {
                log.debug("Param="+param);
            }
            List<String> listChatroom = new ArrayList<>();
            if (param.tourCategory == Constantes.TOURNAMENT_CATEGORY_PRIVATE) {
                PrivateTournament tour = (PrivateTournament)privateMgr.getTournament(param.tourID);
                if (tour != null) {
                    listChatroom.add(tour.getChatroomID());
                    for (PrivateDeal d : tour.getDeals()) {
                        listChatroom.add(d.getChatroomID());
                    }
                } else {
                    log.error("No tournament found for this tourID="+param.tourID);
                }
            } else {
                log.error("Category not supported - tourCategory="+param.tourCategory);
                response.setException(WSSupConstantes.EXCEPTION_COMMON_NOT_IMPLEMENTE);
            }
            response.setData(listChatroom);
        } else {
            log.error("Parameter not valid - param=" + param);
            response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
        }
        return response;
    }

    @Override
    public WSSupResponse listMessagesForChatroom(WSListMessageForChatroomParam param) {
        WSSupResponse response = new WSSupResponse();
        if (param != null && param.isValid()) {
            if (log.isDebugEnabled()) {
                log.debug("Param="+param);
            }
            WSListMessageForChatroomResponse resp = new WSListMessageForChatroomResponse();
            List<GenericMessage> listMsg = null;
            if (param.tourCategory == Constantes.TOURNAMENT_CATEGORY_PRIVATE) {
                resp.nbTotalMessages = (int)privateMgr.getChatMgr().countMessagesForChatroom(param.chatroomID);
                listMsg = (List<GenericMessage>)privateMgr.getChatMgr().listMessagesForChatroom(param.chatroomID, param.offset, param.nbMax);
            } else {
                log.error("Category not supported - tourCategory="+param.tourCategory);
            }
            if (listMsg != null) {
                for (GenericMessage e : listMsg) {
                    resp.messages.add(new WSSupChatroomMessage(e));
                }
            }
            response.setData(resp);
        } else {
            log.error("Parameter not valid - param=" + param);
            response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
        }
        return response;
    }

    @Override
    public WSSupResponse moderateChatroomMessage(WSModerateChatrommMessageParam param) {
        WSSupResponse response = new WSSupResponse();
        if (param != null && param.isValid()) {
            if (log.isDebugEnabled()) {
                log.debug("Param="+param);
            }
            if (param.tourCategory == Constantes.TOURNAMENT_CATEGORY_PRIVATE) {
                try {
                    privateMgr.getChatMgr().moderateMessage(param.messageID, param.chatroomID, param.moderated, Constantes.PLAYER_FUNBRIDGE_ID);
                    response.setData(true);
                } catch (FBWSException e) {
                    log.error("Failed to moderate message - param="+param);
                    response.setException(e.type.getMessage());
                }
            }
        } else {
            log.error("Parameter not valid - param=" + param);
            response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
        }
        return response;
    }

    @Override
    public WSSupResponse listPrivateTournaments(WSListPrivateTournamentsParam param) {
        WSSupResponse response = new WSSupResponse();
        if (param != null && param.isValid()) {
            if (log.isDebugEnabled()) {
                log.debug("Param="+param);
            }
            PrivateTournamentMgr.ListTournamentResult ltr = privateMgr.listTournament(param.search, param.ownerID, 0, 0, 0, null, null, false, -1, param.offset, param.nbMax);
            WSListPrivateTournamentsResponse resp = new WSListPrivateTournamentsResponse();
            resp.totalSize = ltr.count;
            for (PrivateTournament e : ltr.tournamentList) {
                PrivateTournamentProperties properties = privateMgr.getPrivateTournamentProperties(e.getPropertiesID());
                if (properties != null) {
                    resp.tournaments.add(new WSSupPrivateTournament(e, properties));
                }
            }
            response.setData(resp);
        } else {
            log.error("Parameter not valid - param=" + param);
            response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
        }
        return response;
    }

    @Override
    public WSSupResponse createPrivateTournament(WSCreatePrivateTournamentParam param) {
        WSSupResponse response = new WSSupResponse();
        if (param != null && param.isValid()) {
            if (log.isDebugEnabled()) {
                log.debug("Param="+param);
            }
            try {
                WSPrivateTournamentProperties wsProperties = param.toWSProperties();
                if (wsProperties.isValid()) {
                    PrivateTournamentProperties ptp = privateMgr.createPropertiesForFunbridge(wsProperties);
                    if (ptp != null) {
                        PrivateTournament tour = privateMgr.getNextTournamentForProperties(ptp.getIDStr());
                        response.setData(new WSSupPrivateTournament(tour, ptp));
                    } else {
                        log.error("Failed to create private tournament from param="+param);
                        response.setException(WSSupConstantes.EXCEPTION_COMMON_SERVER_ERROR);
                    }
                } else {
                    log.error("Properties not valid for param="+param);
                    response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
                }

            } catch (Exception e) {
                log.error("Failed to build properties from param="+param, e);
                response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
            }

        } else {
            log.error("Parameter not valid - param=" + param);
            response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
        }
        return response;
    }

    @Override
    public WSSupResponse removePrivateTournamentProperties(WSRemovePrivateTournamentPropertiesParam param) {
        WSSupResponse response = new WSSupResponse();
        if (param != null && param.isValid()) {
            if (log.isDebugEnabled()) {
                log.debug("Param="+param);
            }
            try {
                response.setData(privateMgr.removeProperties(param.propertiesID, Constantes.PLAYER_FUNBRIDGE_ID));
            } catch (FBWSException e) {
                log.error("Exception to remove properties !!", e);
                response.setException(e.getMessage());
            }
        } else {
            log.error("Parameter not valid - param=" + param);
            response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
        }
        return response;
    }

    @Override
    public WSSupResponse setPrivateTournamentProperties(WSSetPrivateTournamentPropertiesParam param) {
        WSSupResponse response = new WSSupResponse();
        if (param != null && param.isValid()) {
            if (log.isDebugEnabled()) {
                log.debug("Param="+param);
            }
            try {
                PrivateTournamentProperties properties = privateMgr.getPrivateTournamentProperties(param.propertiesID);
                if (properties != null) {
                    properties.setPassword(param.password);
                    properties.setAccessRule(param.accessRule);
                    properties.setDescription(param.description);
                    privateMgr.getMongoTemplate().save(properties);
                    response.setData(true);
                } else {
                    log.error("No properties found for param=" + param);
                    response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
                }
            } catch (Exception e) {
                log.error("Failed to update properties - param="+param, e);
                response.setException(WSSupConstantes.EXCEPTION_COMMON_SERVER_ERROR);
            }
        } else {
            log.error("Parameter not valid - param=" + param);
            response.setException(WSSupConstantes.EXCEPTION_COMMON_PARAM_NOT_VALID);
        }
        return response;
    }
}