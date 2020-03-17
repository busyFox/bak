package com.funbridge.server.ws.tournament;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.learning.data.LearningProgression;
import com.funbridge.server.ws.FBWSResponse;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.List;

@Path("/tournament")
public interface TournamentServiceRest {
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * The player leave this tournament. It can not played anymore this tournament later
	 */
	@POST
	@Path("/leaveTournament")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse leaveTournament(@HeaderParam("sessionID")String sessionID, LeaveTournamentParam param);
	class LeaveTournamentParam {
		public long tournamentID;
		public int categoryID = 0;
        public String tournamentIDstr = "";
		@JsonIgnore
		public String toString() {
			return "categoryID="+categoryID+" - tournamentID="+tournamentID+" - tournamentIDstr="+tournamentIDstr;
		}
	}
	class LeaveTournamentResponse {
		public int nbCredit;
	}

    /**------------------------------------------------------------------------------------------**/
    /**
     * Get pastilles values for each tournament category
     */
    @POST
    @Path("/getTournamentBadges")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getTournamentBadges(@HeaderParam("sessionID")String sessionID);
    class GetTournamentBadgesResponse {
        public WSTournamentBadges tournamentBadges = new WSTournamentBadges();
    }

    /**------------------------------------------------------------------------------------------**/
    /**
     * Game by category and ID
     */
    @POST
    @Path("/getGameByCategoryAndID")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getGameByCategoryAndID(@HeaderParam("sessionID")String sessionID, final GetGameByCategoryAndIDParam param);
    class GetGameByCategoryAndIDParam {
    	public int categoryID;
		public String gameID;
    }
    class GetGameByCategoryAndIDResponse {
    	public Game game;
	}


	/**------------------------------------------------------------------------------------------**/
	/**
	 * List of tournament in progress for this player
	 */
	@POST
	@Path("/getTournamentInProgress")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse getTournamentInProgress(@HeaderParam("sessionID")String sessionID);
	class GetTournamentInProgressResponse {
		public List<WSTournamentInProgress> tournamentsInProgress;
	}


	/**------------------------------------------------------------------------------------------**/
	/**
	 * Method to create challenge with partner to play tournament
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/createChallenge")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse createChallenge(@HeaderParam("sessionID")String sessionID, CreateChallengeParam param);
	class CreateChallengeParam {
		public long categoryID;
		public long partnerID;
		public String settings;
		@JsonIgnore
		public boolean isValid() {
			return true;
		}
		@JsonIgnore
		public String toString() {
			return "categoryID="+categoryID+" - partnerID="+partnerID+" - settings="+settings;
		}
	}
	class CreateChallengeResponse {
		public long challengeID;
		public long currentTS;
		public long expirationTS;
		@JsonIgnore
		public String toString() {
			return "challengeID="+challengeID+" - currentTS="+currentTS+" - expirationTS="+expirationTS;
		}
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Method to set challenge response
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/setChallengeResponse")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse setChallengeResponse(@HeaderParam("sessionID")String sessionID, SetChallengeResponseParam param);
	class SetChallengeResponseParam {
		public long challengeID;
		public boolean response;
		@JsonIgnore
		public boolean isValid() {
			return true;
		}
		@JsonIgnore
		public String toString() {
			return "challengeID="+challengeID+" - response="+response;
		}
	}
	class SetChallengeResponseResponse {
		public boolean result;
		@JsonIgnore
		public String toString() {
			return "result="+result;
		}
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Method to play tournament for challenge
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/playTournamentChallenge")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse playTournamentChallenge(@HeaderParam("sessionID")String sessionID, PlayTournamentChallengeParam param);
	class PlayTournamentChallengeParam {
		public long challengeID;
		@JsonIgnore
		public boolean isValid() {
			return true;
		}
		@JsonIgnore
		public String toString() {
			return "challengeID="+challengeID;
		}
	}
	class PlayTournamentChallengeResponse {
		public WSTableTournament tableTournament;
		@JsonIgnore
		public String toString() {
			return "tableTournament="+tableTournament;
		}
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Get list of partner for training tournament. Set existing challenge if found. 
	 */
	@POST
	@Path("/getTrainingPartners")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse getTrainingPartners(@HeaderParam("sessionID")String sessionID, GetTrainingPartnersParam param);
	class GetTrainingPartnersParam {
		public int offset;
		public int nbMax;
		@JsonIgnore
		public boolean isValid() {
			return true;
		}
		@JsonIgnore
		public String toString() {
			return "offset="+offset+" - nbMax="+nbMax;
		}
	}
	class GetTrainingPartnersResponse {
		public int totalSize;
		public int offset;
		public List<WSTrainingPartner> results;
	}

	/**------------------------------------------------------------------------------------------**/
	/**
	 * Request a friend to play a duel
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/requestDuel")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse requestDuel(@HeaderParam("sessionID")String sessionID, RequestDuelParam param);
	class RequestDuelParam {
		public long playerID;
		@JsonIgnore
		public boolean isValid() {
//			if (playerID <= 0) {
//				return false;
//			}
			return true;
		}
		@JsonIgnore
		public String toString() {
			return "playerID="+playerID;
		}
	}
	class RequestDuelResponse {
		public WSDuelHistory duelHistory;
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Set answer to player for a request to play a duel
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/answerDuelRequest")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse answerDuelRequest(@HeaderParam("sessionID")String sessionID, AnswerDuelRequestParam param);
	class AnswerDuelRequestParam {
		public long playerDuelID;
        public String playerDuelIDstr;
		public boolean answer;
		@JsonIgnore
		public boolean isValid() {
			if (playerDuelID <= 0) {
                return playerDuelIDstr != null && playerDuelIDstr.length() != 0;
			}
			return true;
		}
		@JsonIgnore
		public String toString() {
			return "playerDuelID="+playerDuelID+" - answer="+answer;
		}
	}
	class AnswerDuelRequestResponse {
		public WSDuelHistory duelHistory;
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * List tournament duel between two players
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/getDuelHistory")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse getDuelHistory(@HeaderParam("sessionID")String sessionID, GetDuelHistoryParam param);
	class GetDuelHistoryParam {
		public long playerDuelID=0;
        public String playerDuelIDstr;
		@JsonIgnore
		public boolean isValid() {
			if (playerDuelID <= 0) {
                return playerDuelIDstr != null && playerDuelIDstr.length() != 0;
			}
			return true;
		}
		@JsonIgnore
		public String toString() {
			return "playerDuelID="+playerDuelID+" - playerDuelIDstr="+playerDuelIDstr;
		}
	}
	class GetDuelHistoryResponse {
		public WSDuelHistory duelHistory;
		public List<WSTournamentDuelResult> listTournamentResultDuel;
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Get detail for a tournament in a duel
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/getDuel")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse getDuel(@HeaderParam("sessionID")String sessionID, GetDuelParam param);
    class GetDuelParam {
		public long tourID;
        public String tourIDstr;
        public boolean showStatus = false; // status to indicate deal play status
		@JsonIgnore
		public boolean isValid() {
			if (tourID <= 0) {
                return tourIDstr != null && tourIDstr.length() != 0;
			}
			return true;
		}
		@JsonIgnore
		public String toString() {
			return "tourID="+tourID+" - showStatus="+showStatus;
		}
	}
	class GetDuelResponse {
		public WSTournamentDuel tournamentDuel;
	}
	

	/**------------------------------------------------------------------------------------------**/
	/**
	 * Start to play a tournament TRAINING and get all data to start game
	 * @param sessionID
	 * @return
	 */
	@POST
	@Path("/playTournamentTraining")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse playTournamentTraining(@HeaderParam("sessionID")String sessionID, PlayTournamentTrainingParam param);
	class PlayTournamentTrainingParam {
		public int conventionProfil;
		public int resultType = 0;
		public String conventionValue = null;
        public int cardsConventionProfil = 0;
        public String cardsConventionValue = null;
        @JsonIgnore
        public boolean isValid() {
            return true;
        }
        @JsonIgnore
        public String toString() {
            return "resultType="+resultType+" - conventionProfil="+conventionProfil+" - conventionValue="+conventionValue+" - cardsConventionProfil="+cardsConventionProfil+" - cardsConventionValue="+cardsConventionValue;
        }
	}
	class PlayTournamentTrainingResponse {
		public WSTableTournament tableTournament;
	}
	
    /**------------------------------------------------------------------------------------------**/
    /**
     * Start to play a tournament SERIE and get all data to start game
     * @param sessionID
     * @return
     */
    @POST
    @Path("/playTournamentSerie2")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse playTournamentSerie2(@HeaderParam("sessionID")String sessionID, PlayTournamentSerieParam param);
    class PlayTournamentSerieParam {
        public int conventionProfil;
        public String conventionValue = null;
        public int cardsConventionProfil = 0;
        public String cardsConventionValue = null;
        @JsonIgnore
        public boolean isValid() {
            return true;
        }
        @JsonIgnore
        public String toString() {
            return "conventionProfil="+conventionProfil+" - conventionValue="+conventionValue+" - cardsConventionProfil="+cardsConventionProfil+" - cardsConventionValue="+cardsConventionValue;
        }
    }
    class PlayTournamentSerieResponse {
        public WSTableTournament tableTournament;
    }

	/**------------------------------------------------------------------------------------------**/
	/**
	 * Start to play a tournament DUEL and get all data to start game
	 * @param sessionID
	 * @return
	 */
	@POST
	@Path("/playTournamentDuel")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse playTournamentDuel(@HeaderParam("sessionID")String sessionID, PlayTournamentDuelParam param);
	class PlayTournamentDuelParam {
		public int conventionProfil;
		public long tournamentID;
        public String tournamentIDstr;
		public String conventionValue = null;
        public int cardsConventionProfil = 0;
        public String cardsConventionValue = null;
		@JsonIgnore
		public boolean isValid() {
			return true;
		}
		@JsonIgnore
		public String toString() {
			return "conventionProfil="+conventionProfil+" - tournamentID="+tournamentID+" - conventionValue="+conventionValue+" - cardsConventionProfil="+cardsConventionProfil+" - cardsConventionValue="+cardsConventionValue;
		}
	}
	class PlayTournamentDuelResponse {
		public WSTableTournament tableTournament;
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Start to play a tournament TRAINING-PARTNER and get all data to start game
	 * @param sessionID
	 * @return
	 */
	@POST
	@Path("/playTournamentTrainingPartner")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse playTournamentTrainingPartner(@HeaderParam("sessionID")String sessionID, PlayTournamentTrainingPartnerParam param);
	class PlayTournamentTrainingPartnerParam {
		public long challengeID;
		@JsonIgnore
		public boolean isValid() {
			return true;
		}
		@JsonIgnore
		public String toString() {
			return "challengeID="+challengeID;
		}
	}
	class PlayTournamentTrainingPartnerResponse {
		public WSTableTournament tableTournament;
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Start to play a tournament TIMEZONE and get all data to start game
	 * @param sessionID
	 * @return
	 */
	@POST
	@Path("/playTournamentTimezone")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse playTournamentTimezone(@HeaderParam("sessionID")String sessionID, PlayTournamentTimezoneParam param);
	class PlayTournamentTimezoneParam {
		public long tournamentID = 0;
        public String tournamentIDstr;
		public int conventionProfil;
		public String conventionValue = null;
        public int cardsConventionProfil = 0;
        public String cardsConventionValue = null;
        @JsonIgnore
        public boolean isValid() {
            return true;
        }
        @JsonIgnore
        public String toString() {
            return "conventionProfil="+conventionProfil+" - tournamentIDstr="+tournamentIDstr+" conventionValue="+conventionValue+" - cardsConventionProfil="+cardsConventionProfil+" - cardsConventionValue="+cardsConventionValue;
        }
	}
	class PlayTournamentTimezoneResponse {
		public WSTableTournament tableTournament;
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Get list of tournament TIMEZONE.
	 * 0 : WEST AMERIC - 1 : EAST AMERIC - 2 : EUROPE - 3 : ASIA
	 * 0 : IMP - 1 : PAIRE
	 * @param sessionID
	 * @return
	 */
	@POST
	@Path("/getTimezoneTournaments")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse getTimezoneTournaments(@HeaderParam("sessionID")String sessionID);
	class GetTimezoneTournamentsResponse {
		public WSTournament[] tournaments;
	}

	/**------------------------------------------------------------------------------------------**/
	/**
	 * Set answer for request duel sent by a playerID
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/getDuels")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse getDuels(@HeaderParam("sessionID")String sessionID, GetDuelsParam param);
    class GetDuelsParam {
        public boolean friends = false;
        public int offset = 0;
        public int nbMax = 50;
        @JsonIgnore
        public boolean isValid() {
            return true;
        }
        @JsonIgnore
        public String toString() {
            return "friends="+friends;
        }
    }
	class GetDuelsResponse {
		public List<WSDuelHistory> results;
        public int offset;
        public int totalSize;
	}

	/**------------------------------------------------------------------------------------------**/
	/**
	 * Set flag to enable match making for player
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/setMatchMakingEnabled")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse setMatchMakingEnabled(@HeaderParam("sessionID")String sessionID, SetMatchMakingEnabledParam param);
	class SetMatchMakingEnabledParam {
		public boolean enabled;
		@JsonIgnore
		public boolean isValid() {
			return true;
		}
		@JsonIgnore
		public String toString() {
			return "enabled="+enabled;
		}
	}
	class SetMatchMakingEnabledResponse {
		public long dateExpiration;
	}

    /**------------------------------------------------------------------------------------------**/
    /**
     * Remove a player from the duel list
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/removePlayerFromDuelList")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse removePlayerFromDuelList(@HeaderParam("sessionID")String sessionID, RemovePlayerFromDuelListParam param);
    class RemovePlayerFromDuelListParam {
        public long playerID;
        @JsonIgnore
        public boolean isValid() {
            return true;
        }
        @JsonIgnore
        public String toString() {
            return "playerID="+playerID;
        }
    }
    class RemovePlayerFromDuelListResponse {
        public boolean result;
    }

	/**
	 * Play a serie Top Challenge Tournament
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/playSerieTopChallengeTournament")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse playSerieTopChallengeTournament(@HeaderParam("sessionID")String sessionID, PlaySerieTopChallengeTournamentParam param);
	class PlaySerieTopChallengeTournamentParam{
		public int conventionProfil;
		public String conventionValue = null;
		public int cardsConventionProfil = 0;
		public String cardsConventionValue = null;
		@JsonIgnore
		public boolean isValid() {
			return true;
		}
		@JsonIgnore
		public String toString() {
			return "conventionProfil="+conventionProfil+" - conventionValue="+conventionValue+" - cardsConventionProfil="+cardsConventionProfil+" - cardsConventionValue="+cardsConventionValue;
		}
	}
	class PlaySerieTopChallengeTournamentResponse {
		public WSTableTournament tableTournament;
	}

	/**
	 * Play a serie Easy Challenge Tournament
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/playSerieEasyChallengeTournament")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse playSerieEasyChallengeTournament(@HeaderParam("sessionID")String sessionID, PlaySerieEasyChallengeTournamentParam param);
	class PlaySerieEasyChallengeTournamentParam{
		public int conventionProfil;
		public String conventionValue = null;
		public int cardsConventionProfil = 0;
		public String cardsConventionValue = null;
		@JsonIgnore
		public boolean isValid() {
			return true;
		}
		@JsonIgnore
		public String toString() {
			return "conventionProfil="+conventionProfil+" - conventionValue="+conventionValue+" - cardsConventionProfil="+cardsConventionProfil+" - cardsConventionValue="+cardsConventionValue;
		}
	}
	class PlaySerieEasyChallengeTournamentResponse {
		public WSTableTournament tableTournament;
	}

    /**------------------------------------------------------------------------------------------**/
    /**
     * Return summary for a federation tournament
     */
    @POST
    @Path("/getFederationSummary")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getFederationSummary(@HeaderParam("sessionID")String sessionID, GetFederationSummaryParam param);
	class GetFederationSummaryParam {
		public String federation;
		@JsonIgnore
		public boolean isValid(){ return federation != null && !federation.isEmpty(); }
		@JsonIgnore
		public String toString(){ return "federation="+federation; }
	}

    /**------------------------------------------------------------------------------------------**/
    /**
     * Player register on a federation tournament
     */
    @POST
    @Path("/registerTournamentFederation")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse registerTournamentFederation(@HeaderParam("sessionID")String sessionID, RegisterTournamentFederationParam param);
    class RegisterTournamentFederationParam {
        public String tourID;
		public String federation;
		public boolean register=true;
        @JsonIgnore
        public boolean isValid() {
            return tourID!=null && tourID.length() > 0 && federation != null && !federation.isEmpty();
        }
        @JsonIgnore
        public String toString() {
            return "tourID="+tourID+" - federation="+federation;
        }
    }
    class RegisterTournamentFederationResponse {
        public boolean result = true;
        public int credit = 0;
        public int nbPlayersRegistered = 0;
    }

    /**------------------------------------------------------------------------------------------**/
    /**
     * Start to play a federation tournament and get all data to start game
     * @param sessionID
     * @return
     */
    @POST
    @Path("/playTournamentFederation")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse playTournamentFederation(@HeaderParam("sessionID")String sessionID, PlayTournamentFederationParam param);
    class PlayTournamentFederationParam {
        public String tournamentID;
        public String federation;
		public int conventionProfil;
        public String conventionValue = null;
        public int cardsConventionProfil = 0;
        public String cardsConventionValue = null;
        @JsonIgnore
        public boolean isValid() {
            return federation !=null && tournamentID != null;
        }
        @JsonIgnore
        public String toString() {
            return "federation="+federation+" - tournamentID="+tournamentID+" - conventionProfil="+conventionProfil+" conventionValue="+conventionValue+" - cardsConventionProfil="+cardsConventionProfil+" - cardsConventionValue="+cardsConventionValue;
        }
    }
    class PlayTournamentFederationResponse {
        public WSTableTournament tableTournament;
    }

    /**------------------------------------------------------------------------------------------**/
    /**
     * Home page for private tournaments
     * @param sessionID
     * @return
     */
    @POST
    @Path("/getPrivateTournamentSummary")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getPrivateTournamentSummary(@HeaderParam("sessionID")String sessionID, GetPrivateTournamentSummaryParam param);
    class GetPrivateTournamentSummaryParam {
        public int nbMaxTournaments = 3;
        @JsonIgnore
        public boolean isValid() {
            return true;
        }
        @JsonIgnore
        public String toString() {
            return "nbMaxTournaments="+nbMaxTournaments;
        }
    }
    class GetPrivateTournamentSummaryResponse {
        public List<WSPrivateTournament> tournaments;
        public int tournamentsTotalSize = 0;
        public int nbPlayerTournaments;
        public int nbTotalTournaments = 0;
        public int nbTotalUnreadMessages = 0;
        public boolean tournamentCreationAllowed = true;
        public int nbTournamentPerPlayer = 0;
    }

    /**
     * Create a private tournament
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/createPrivateTournamentProperties")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse createPrivateTournamentProperties(@HeaderParam("sessionID")String sessionID, CreatePrivateTournamentPropertiesParam param);
    class CreatePrivateTournamentPropertiesParam {
        public WSPrivateTournamentProperties properties;
        public boolean isValid() {
            if (properties == null) {
                return false;
            }
            return properties.isValid();
        }
        public String toString() {
            return "properties="+properties;
        }
    }
    class CreatePrivateTournamentPropertiesResponse {
        public WSPrivateTournament tournament;
    }

    /**
     * Remove a private tournament
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/removePrivateTournamentProperties")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse removePrivateTournamentProperties(@HeaderParam("sessionID")String sessionID, RemovePrivateTournamentPropertiesParam param);
    class RemovePrivateTournamentPropertiesParam {
        public String propertiesID;
        public boolean isValid() {
            return propertiesID != null && propertiesID.length() != 0;
        }
        public String toString() {
            return "propertiesID="+propertiesID;
        }
    }
    class RemovePrivateTournamentPropertiesResponse {
        public boolean result;
    }

    /**
     * Change password for a private tournament
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/changePasswordPrivateTournament")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse changePasswordPrivateTournament(@HeaderParam("sessionID")String sessionID, ChangePasswordPrivateTournamentParam param);
    class ChangePasswordPrivateTournamentParam {
        public String propertiesID;
        public String password;
        public boolean isValid() {
            if (propertiesID == null || propertiesID.length() == 0) {
                return false;
            }
            return password != null && password.length() != 0;
        }
        public String toString() {
            return "propertiesID="+propertiesID+" - password="+password;
        }
    }
    class ChangePasswordPrivateTournamentResponse {
        public boolean result;
    }

    /**
     * Add or remove private tournament properties to player's favorite
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/setPrivateTournamentFavorite")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse setPrivateTournamentFavorite(@HeaderParam("sessionID")String sessionID, SetPrivateTournamentFavoriteParam param);
    class SetPrivateTournamentFavoriteParam {
        public String propertiesID;
        public boolean favorite;
        public boolean isValid() {
            return propertiesID != null && propertiesID.length() != 0;
        }
        public String toString() {
            return "propertiesID="+propertiesID+" - favorite="+favorite;
        }
    }
    class SetPrivateTournamentFavoriteResponse {
        public boolean result;
    }

    /**
     * List priavte tournaments
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/listPrivateTournaments")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse listPrivateTournaments(@HeaderParam("sessionID")String sessionID, ListPrivateTournamentsParam param);
    class ListPrivateTournamentsParam {
        public String search = "";
        public String nbDeals = ""; //"" si pas de filtre
        public int rankingType; //0 si pas de filtre
        public String accessRule = ""; //""  si pas de filtre
        public String countryCode = ""; //""  si pas de filtre
        public int offset;
        public int nbMax;
        public boolean favorite;

        public boolean isValid() {
            return true;
        }
        public String toString() {
            return "search="+search+" - nbDeals="+nbDeals+" - rankingType="+rankingType+" - accessRule="+accessRule+" - countryCode="+countryCode+" - offset="+offset+" - nbMax="+nbMax;
        }
        public int getNbDealsMinimum() {
            if (nbDeals != null) {
                if (nbDeals.equals("<5")) {
                    return 0;
                }
                if (nbDeals.equals("5-10")) {
                    return 5;
                }
                if (nbDeals.equals("10-20")) {
                    return 10;
                }
                if (nbDeals.equals(">20")) {
                    return 20;
                }
            }
            return 0;
        }
        public int getNbDealsMaximum() {
            if (nbDeals != null) {
                if (nbDeals.equals("<5")) {
                    return 5;
                }
                if (nbDeals.equals("5-10")) {
                    return 10;
                }
                if (nbDeals.equals("10-20")) {
                    return 20;
                }
                if (nbDeals.equals(">20")) {
                    return 0;
                }
            }
            return 0;
        }
    }
    class ListPrivateTournamentsResponse {
        public List<WSPrivateTournament> results;
        public int offset = 0;
        public int totalSize = 0;
    }

    /**
     * Return list of private tournament properties with owner is player
     * @param sessionID
     * @return
     */
    @POST
    @Path("/getPlayerPrivateTournamentProperties")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getPlayerPrivateTournamentProperties(@HeaderParam("sessionID")String sessionID);
    class GetPlayerPrivateTournamentPropertiesResponse {
        public List<WSPrivateTournamentProperties> properties = new ArrayList<>();
    }

    /**
     * Start to play a tournament PRIVATE and get all data to start game
     * @param sessionID
     * @return
     */
    @POST
    @Path("/getPrivateTournament")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getPrivateTournament(@HeaderParam("sessionID")String sessionID, GetPrivateTournamentParam param);
    class GetPrivateTournamentParam {
        public String tournamentID;
        public String password;
        public boolean isValid() {
            return tournamentID != null && tournamentID.length() != 0;
        }
        public String toString() {
            return "tournamentID="+tournamentID+" - password="+password;
        }
    }
    class GetPrivateTournamentResponse {
        public WSPrivateTournament tournament;
        public WSResultTournamentPlayer resultPlayer;
    }
    /**
     * Start to play a tournament PRIVATE and get all data to start game
     * @param sessionID
     * @return
     */
    @POST
    @Path("/playPrivateTournament")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse playPrivateTournament(@HeaderParam("sessionID")String sessionID, PlayPrivateTournamentParam param);
    class PlayPrivateTournamentParam {
        public String tournamentID;
        public int conventionProfil;
        public String conventionValue = null;
        public int cardsConventionProfil = 0;
        public String cardsConventionValue = null;
        @JsonIgnore
        public boolean isValid() {
            return tournamentID != null && tournamentID.length() != 0;
        }
        @JsonIgnore
        public String toString() {
            return "tournamentID="+tournamentID+" - conventionProfil="+conventionProfil+" - conventionValue="+conventionValue+" - cardsConventionProfil="+cardsConventionProfil+" - cardsConventionValue="+cardsConventionValue;
        }
    }
    class PlayPrivateTournamentResponse {
        public WSTableTournament tableTournament;
    }

	/**------------------------------------------------------------------------------------------**/
	/**
	 * Start to play a learning tournament and get all data to start game
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/playLearningTournament")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse playLearningTournament(@HeaderParam("sessionID")String sessionID, PlayLearningTournamentParam param);
	class PlayLearningTournamentParam {
		// SB1-chapterNumber-deal
		public String chapterID;
		public int conventionProfil;
		public String conventionValue = null;
		public int cardsConventionProfil = 0;
		public String cardsConventionValue = null;
		@JsonIgnore
		public boolean isValid() {
            return chapterID != null && chapterID.length() != 0;
        }
		@JsonIgnore
		public String toString() {
			return "conventionProfil="+conventionProfil+" - chapterID="+chapterID+" conventionValue="+conventionValue+" - cardsConventionProfil="+cardsConventionProfil+" - cardsConventionValue="+cardsConventionValue;
		}
	}
	class PlayLearningTournamentResponse {
		public WSTableTournament tableTournament;
	}

	/**------------------------------------------------------------------------------------------**/
	/**
	 * Get comments for a learning deal
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/getLearningDealCommented")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse getLearningDealCommented(@HeaderParam("sessionID")String sessionID, GetLearningDealCommentedParam param);
	class GetLearningDealCommentedParam {
		public String dealID;
		@JsonIgnore
		public boolean isValid() {
            return dealID != null && dealID.length() != 0;
        }
		@JsonIgnore
		public String toString() {
			return "dealID="+dealID;
		}
	}
	class GetLearningDealCommentedResponse {
		public WSDealCommented dealCommented;
		public String contractPlayer;
		public String declarerPlayer;
		public int nbTricksPlayer;
	}

    /**------------------------------------------------------------------------------------------**/
    /**
     * Set learning progression for player
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/setLearningProgression")
    @Consumes("application/json")
    @Produces("application/json")
	FBWSResponse setLearningProgression(@HeaderParam("sessionID")String sessionID, SetLearningProgressionParam param);
	class SetLearningProgressionParam {
	    public String sb;
	    public int chapter;
	    public int deal;
	    public int status;
	    public int step;
        @JsonIgnore
        public boolean isValid() {
            return true;
        }
        @JsonIgnore
        public String toString() {
            return "sb="+sb+" - chapter="+chapter+" - deal="+deal+" - status="+status;
        }
    }
    class SetLearningProgressionResponse {
	    public boolean result = true;
    }

	/**------------------------------------------------------------------------------------------**/
	/**
	 * Get learning progression for player
	 * @param sessionID
	 * @return
	 */
	@POST
	@Path("/getLearningProgression")
	@Consumes("application/json")
	@Produces("application/json")
    FBWSResponse getLearningProgression(@HeaderParam("sessionID")String sessionID);
	class GetLearningProgressionResponse {
		public LearningProgression learningProgression;
	}
}