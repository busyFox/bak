package com.funbridge.server.ws.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.ws.tournament.WSPrivateTournamentProperties;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.ArrayList;
import java.util.List;

@Path("/support")
public interface SupportService {
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Search for player. Parameter can contain ID, mail or pseudo. In the case of ID, return only 1 player. 
	 * For mail & pseudo, return list of player with mail or pseudo like these values.
	 * @param param
	 * @return
	 */
	@POST
	@Path("/searchPlayer")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse searchPlayer(WSSupSearchPlayerParam param);
	class WSSupSearchPlayerParam {
		public long playerID = -1;
		public String pseudo = "";
		public String mail = "";
		@JsonIgnore
		public boolean isPlayerIDValid() {return playerID != -1;}
		@JsonIgnore
		public boolean isPseudoValid() {return pseudo != null && pseudo.length() > 0;}
		@JsonIgnore
		public boolean isMailValid() {return mail != null && mail.length() > 0;}
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1 ||
                    (pseudo != null && pseudo.length() != 0) ||
                    (mail != null && mail.length() != 0);
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Search for player. Parameter can contain ID, mail, pseudo . In the case of ID, return only 1 player.
	 * For mail & pseudo, return list of player with mail or pseudo like these values.
	 * @param param
	 * @return
	 */
	@POST
	@Path("/getPlayer")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse getPlayer(WSSupGetPlayerParam param);
	class WSSupGetPlayerParam {
		public long playerID = -1;
		public String pseudo = "";
		public String mail = "";
		@JsonIgnore
		public boolean isPlayerIDValid() {return playerID != -1;}
		@JsonIgnore
		public boolean isPseudoValid() {return pseudo != null && pseudo.length() > 0;}
		@JsonIgnore
		public boolean isMailValid() {return mail != null && mail.length() > 0;}
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1 ||
                    (pseudo != null && pseudo.length() != 0) ||
                    (mail != null && mail.length() != 0);
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * List of player for device
	 * @param param
	 * @return
	 */
	@POST
	@Path("/listPlayerForDevice")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse listPlayerForDevice(WSSupListPlayerForDeviceParam param);
	class WSSupListPlayerForDeviceParam {
		public long deviceID = -1;
		@JsonIgnore
		public boolean isValid() {
            return deviceID != -1;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * List of device for player (id)
	 * @param param
	 * @return
	 */
	@POST
	@Path("/listDeviceForPlayer")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse listDeviceForPlayer(WSSupListDeviceForPlayerParam param);
	class WSSupListDeviceForPlayerParam {
		public long playerID = -1;
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * List of device for player (mail)
	 * @param param
	 * @return
	 */
	@POST
	@Path("/listDeviceForPlayerMail")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse listDeviceForPlayerMail(WSSupListDeviceForPlayerMailParam param);
	class WSSupListDeviceForPlayerMailParam {
		public String playerMail;
		@JsonIgnore
		public boolean isValid() {
            return playerMail != null && playerMail.length() != 0;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * list of transaction for player
	 * @param param
	 * @return
	 */
	@POST
	@Path("/listTransactionForPlayer")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse listTransactionForPlayer(WSSupListTransactionForPlayerParam param);
	class WSSupListTransactionForPlayerParam {
		public long playerID = -1;
		public int offset = 0;
		public int nbMax = 50;
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * List of player connection
	 * @param param
	 * @return
	 */
	@POST
	@Path("/listConnectionForPlayer")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse listConnectionForPlayer(WSSupListConnectionForPlayerParam param);
	class WSSupListConnectionForPlayerParam {
		public long playerID = -1;
		public int offset = 0;
		public int nbMax = 50;
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Get device info
	 * @param param
	 * @return
	 */
	@POST
	@Path("/getDevice")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse getDevice(WSSupGetDeviceParam param);
	class WSSupGetDeviceParam {
		public long deviceID = -1;
		@JsonIgnore
		public boolean isValid() {
            return deviceID != -1;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Get player avatar
	 * @param param
	 * @return
	 */
	@POST
	@Path("/getPlayerAvatar")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse getPlayerAvatar(WSSupGetPlayerAvatarParam param);
	class WSSupGetPlayerAvatarParam {
		public long playerID = -1;
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * remove player avatar
	 * @param param
	 * @return
	 */
	@POST
	@Path("/removePlayerAvatar")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse removePlayerAvatar(WSSupRemovePlayerAvatarParam param);
	class WSSupRemovePlayerAvatarParam {
		public long playerID = -1;
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * validate player avatar
	 * @param param
	 * @return
	 */
	@POST
	@Path("/validatePlayerAvatar")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse validatePlayerAvatar(WSSupValidatePlayerAvatarParam param);
	class WSSupValidatePlayerAvatarParam {
		public long playerID = -1;
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Modify player some fields
	 * @param param
	 * @return
	 */
	@POST
	@Path("/modifyPlayer")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse modifyPlayer(WSSupModifyPlayerParam param);
	class WSSupModifyPlayerParam {
		public long playerID = -1;
		public String pseudo;
		public String firstName;
		public String lastName;
		public String mail;
		public String password;
		public String town;
		public String presentation;
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Reset password of player
	 * @param param
	 * @return
	 */
	@POST
	@Path("/resetPassword")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse resetPassword(WSSupResetPasswordParam param);
	class WSSupResetPasswordParam {
		public long playerID = -1;
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Certify the player
	 * @param param
	 * @return
	 */
	@POST
	@Path("/certifyPlayer")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse certifyPlayer(WSSupCertifyPlayerParam param);
	class WSSupCertifyPlayerParam {
		public long playerID = -1;
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Check if a session exist for player
	 * @param param
	 * @return
	 */
	@POST
	@Path("/isPlayerConnected")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse isPlayerConnected(WSSupIsPlayerConnectedParam param);
	class WSSupIsPlayerConnectedParam {
		public long playerID = -1;
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * If a session exist for player, close it
	 * @param param
	 * @return
	 */
	@POST
	@Path("/disconnectPlayer")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse disconnectPlayer(WSSupDisconnectPlayerParam param);
	class WSSupDisconnectPlayerParam {
		public long playerID = -1;
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1;
        }
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Send message from support
	 * @param param
	 * @return
	 */
	@POST
	@Path("/sendMessageSupport")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse sendMessageSupport(WSSupSendMessageSupportParam param);
	class WSSupSendMessageSupportResponse {
		public boolean result = false;
		public String toString() {
			return "result="+result;
		}
	}
	class WSSupSendMessageSupportParam {
		public long recipient = -1;
		public String message = "";
		public boolean push = false;
		
		@JsonIgnore
		public boolean isValid() {
			if (recipient <= 0) {
				return false;
			}
            return message != null && message.length() != 0;
        }
		public String toString() {
			return "recipient="+recipient+" - message="+message+" - push="+push;
		}
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * List period result for player
	 * @param param
	 * @return
	 */
	@POST
	@Path("/listPeriodResultForPlayer")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse listPeriodResultForPlayer(WSSupListPeriodResultForPlayerParam param);
	class WSSupListPeriodResultForPlayerParam {
		public long playerID = -1;
		public int offset = 0;
		public int nbMax = 50;
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1;
        }
	}

    /**------------------------------------------------------------------------------------------**/
    /**
     * Get serie historic for player
     * @param param
     * @return
     */
    @POST
    @Path("/getSerieHistoricForPlayer")
    @Consumes("application/json")
    @Produces("application/json")
	WSSupResponse getSerieHistoricForPlayer(WSSupGetSerieHistoricForPlayerParam param);
    class WSSupGetSerieHistoricForPlayerParam {
        public long playerID = -1;
        @JsonIgnore
        public boolean isValid() {
            return playerID != -1;
        }
    }

	/**------------------------------------------------------------------------------------------**/
	/**
	 * List tournament player for a player
	 * @param param
	 * @return
	 */
	@POST
	@Path("/listTournamentPlayForPlayer")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse listTournamentPlayForPlayer(WSSupListTournamentPlayForPlayerParam param);
	class WSSupListTournamentPlayForPlayerParam {
		public long playerID = -1;
		public int offset = 0;
		public int nbMax = 30;
		public int category = 0;
		@JsonIgnore
		public boolean isValid() {
			if (playerID == -1) {
				return false;
			}
            return offset >= 0;
        }
		public String toString() {
			return "playerID="+playerID+" - category="+category+" - offset="+offset+" - nbMax="+nbMax;
		}
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * List game played on tournament for player
	 * @param param
	 * @return
	 */
	@POST
	@Path("/listGamePlayForTournamentAndPlayer")
	@Consumes("application/json")
	@Produces("application/json")
	WSSupResponse listGamePlayForTournamentAndPlayer(WSSupListGamePlayForTournamentAndPlayerParam param);
	class WSSupListGamePlayForTournamentAndPlayerParam {
		public long playerID = -1;
		public long tourID = -1;
        public String tourIDstr = null;
		public int category = 0;
		@JsonIgnore
		public boolean isValid() {
            return playerID != -1;
        }
		public String toString() {
			return "playerID="+playerID+" - category="+category+" - tourID="+tourID+" - tourIDstr="+tourIDstr;
		}
	}

    /**------------------------------------------------------------------------------------------**/
    /**
     * Change serie for player
     * @param param
     * @return
     */
    @POST
    @Path("/changePlayerSerie")
    @Consumes("application/json")
    @Produces("application/json")
    WSSupResponse changePlayerSerie(WSSupChangePlayerSerieParam param);
    class WSSupChangePlayerSerieParam {
        public long playerID = -1;
        public String newSerie = null;
        public String messageNotif = "";
        @JsonIgnore
        public boolean isValid() {
            if (playerID == -1) {
                return false;
            }
            if (newSerie == null || newSerie.length() == 0) {
                return false;
            }
            return TourSerieMgr.isSerieValid(newSerie);
        }
        public String toString() {
            return "playerID="+playerID+" - newSerie="+newSerie+" - messageNotif="+messageNotif;
        }
    }
    class WSSupChangePlayerSerieResponse {
        public boolean result;
        public String info;
        public String toString() {
            return "result="+result+" - info="+info;
        }
    }

    @POST
    @Path("/listChatroomsForTournament")
    @Consumes("application/json")
    @Produces("application/json")
    WSSupResponse listChatroomsForTournament(WSListChatroomsForTournamentParam param);
    class WSListChatroomsForTournamentParam {
        public String tourID;
        public int tourCategory;
        @JsonIgnore
        public boolean isValid() {
            return tourID != null && tourID.length() != 0;
        }
        public String toString() {
            return "tourID="+tourID+" - tourCategory="+tourCategory;
        }
    }

    @POST
    @Path("/listMessagesForChatroom")
    @Consumes("application/json")
    @Produces("application/json")
    WSSupResponse listMessagesForChatroom(WSListMessageForChatroomParam param);
    class WSListMessageForChatroomParam {
        public String chatroomID;
        public int tourCategory;
        public int offset;
        public int nbMax;
        @JsonIgnore
        public boolean isValid() {
            return chatroomID != null && chatroomID.length() != 0;
        }
        public String toString() {
            return "chatroomID="+chatroomID+" - tourCategory="+tourCategory;
        }
    }
    class WSListMessageForChatroomResponse {
        public List<WSSupChatroomMessage> messages = new ArrayList<>();
        public int nbTotalMessages=0;
    }

    @POST
    @Path("/moderateChatroomMessage")
    @Consumes("application/json")
    @Produces("application/json")
    WSSupResponse moderateChatroomMessage(WSModerateChatrommMessageParam param);
    class WSModerateChatrommMessageParam {
        public String chatroomID;
        public int tourCategory;
        public String messageID;
        public boolean moderated;
        @JsonIgnore
        public boolean isValid() {
            if (chatroomID == null || chatroomID.length() == 0) {
                return false;
            }
            return messageID != null && messageID.length() != 0;
        }
        public String toString() {
            return "chatroomID="+chatroomID+" - tourCategory="+tourCategory+" - messageID="+messageID+" - moderated="+moderated;
        }
    }

    @POST
    @Path("/listPrivateTournaments")
    @Consumes("application/json")
    @Produces("application/json")
    WSSupResponse listPrivateTournaments(WSListPrivateTournamentsParam param);
    class WSListPrivateTournamentsParam {
        public String search;
        public int offset;
        public int nbMax;
        public long ownerID = 0;
        @JsonIgnore
        public boolean isValid() {
            return true;
        }
        public String toString() {
            return "search="+search+" - offset="+offset+" - nbMax="+nbMax;
        }
    }
    class WSListPrivateTournamentsResponse {
        public int totalSize;
        public List<WSSupPrivateTournament> tournaments = new ArrayList<>();
    }

    @POST
    @Path("/createPrivateTournament")
    @Consumes("application/json")
    @Produces("application/json")
    WSSupResponse createPrivateTournament(WSCreatePrivateTournamentParam param);
    class WSCreatePrivateTournamentParam {
        public String name;
        public int nbDeals;
        public String startDate; // format dd/MM/yyyy - HH:mm:ss
        public String recurrence;
        public String accessRule;
        public String password;
        public int rankingType;
        public int duration;
        public String description;
        @JsonIgnore
        public boolean isValid() {
            if (name == null || name.length() == 0) {
                return false;
            }
            return nbDeals > 0;

        }
        public String toString() {
            return "name="+name+" - nbDeals="+nbDeals+" - startDate="+startDate+" - recurrence="+recurrence+" - accessRule="+accessRule+" - password="+password+" - rankingType="+rankingType+" - duration="+duration+" - description="+description;
        }
        public WSPrivateTournamentProperties toWSProperties() throws Exception {
            WSPrivateTournamentProperties ws = new WSPrivateTournamentProperties();
            ws.name = name;
            ws.nbDeals = nbDeals;
            ws.startDate = Constantes.stringDateHour2Timestamp(startDate);
            ws.recurrence = recurrence;
            ws.accessRule = accessRule;
            ws.password = password;
            ws.rankingType = rankingType;
            ws.duration = duration;
            ws.description = description;
            return ws;
        }
    }

    @POST
    @Path("/setPrivateTournamentProperties")
    @Consumes("application/json")
    @Produces("application/json")
    WSSupResponse setPrivateTournamentProperties(WSSetPrivateTournamentPropertiesParam param);
    class WSSetPrivateTournamentPropertiesParam {
        public String propertiesID;
        public String description;
        public String accessRule;
        public String password;
        @JsonIgnore
        public boolean isValid() {
            return propertiesID != null && propertiesID.length() != 0;
        }
        public String toString() {
            return "propertiesID="+propertiesID+" - description="+description+" - accessRule="+accessRule+" - password="+password;
        }
    }

    @POST
    @Path("/removePrivateTournamentProperties")
    @Consumes("application/json")
    @Produces("application/json")
    WSSupResponse removePrivateTournamentProperties(WSRemovePrivateTournamentPropertiesParam param);
    class WSRemovePrivateTournamentPropertiesParam {
        public String propertiesID;
        @JsonIgnore
        public boolean isValid() {
            return propertiesID != null && propertiesID.length() != 0;
        }
        public String toString() {
            return "propertiesID="+propertiesID;
        }
    }
}
