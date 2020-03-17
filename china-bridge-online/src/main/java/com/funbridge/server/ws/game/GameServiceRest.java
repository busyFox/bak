package com.funbridge.server.ws.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.ws.FBWSResponse;
import com.funbridge.server.ws.tournament.WSTableTournament;

import javax.ws.rs.*;

@Path("/game")
public interface GameServiceRest {

	/**------------------------------------------------------------------------------------------**/
	/**
	 * The player leave this game and will never come back on this deal
	 */
	@POST
	@Path("/leaveGame")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse leaveGame(@HeaderParam("sessionID") String sessionID, LeaveGameParam param);

	/**
	 * Play a bid
	 */
	@POST
	@Path("/playBid")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse playBid(@HeaderParam("sessionID") String sessionID, PlayBidParam param);

	/**
	 * Return the string information of this bid on the game
	 */
	@POST
	@Path("/getPlayBidInformation")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse getPlayBidInformation(@HeaderParam("sessionID") String sessionID, GetPlayBidInformationParam param);

	/**------------------------------------------------------------------------------------------**/

	/**
	 * Return the information for a bid with the sequence
	 *
	 * @param param
	 * @return
	 */
	@POST
	@Path("/getBidInformation")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse getBidInformation(GetBidInformationParam param);

	/**
	 * Play a card
	 */
	@POST
	@Path("/playCard")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse playCard(@HeaderParam("sessionID") String sessionID, PlayCardParam param);

	/**
	 * Set the response of spread claim for this game
	 */
	@POST
	@Path("/setClaimSpreadResponse")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse setClaimSpreadResponse(@HeaderParam("sessionID") String sessionID, SetClaimSpreadResponseParam param);

	/**------------------------------------------------------------------------------------------**/

	/**
	 * Return the gameDeal to view the game. The player must have play the deal !
	 */
	@POST
	@Path("/viewGame")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse viewGame(@HeaderParam("sessionID") String sessionID, ViewGameParam param);

	/**
	 * Return the game deal to view a game on this deal for this contract and score.
	 * If the player has played this deal, return the game of this player else return the first game played found.
	 */
	@POST
	@Path("/viewGameForDealScoreAndContract")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse viewGameForDealScoreAndContract(@HeaderParam("sessionID") String sessionID, ViewGameForDealScoreAndContractParam param);

	/**
	 * Restart the game at the beginning. Only for training with partner
	 *
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/resetGame")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse resetGame(@HeaderParam("sessionID") String sessionID, ResetGameParam param);

	/**------------------------------------------------------------------------------------------**/

	/**
	 * Player claim nb tricks on game. If accepted, game is ended.
	 *
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/claim")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse claim(@HeaderParam("sessionID") String sessionID, ClaimParam param);

	/**
	 * Method to start thread play game after doing playTournamentXXX
	 *
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/startGame")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse startGame(@HeaderParam("sessionID") String sessionID, StartGameParam param);

	/**------------------------------------------------------------------------------------------**/

	/**
	 * Method to replay a deal
	 *
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/replay")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse replay(@HeaderParam("sessionID") String sessionID, ReplayParam param);

	/**
	 * Method to get distribution cards for dummy player
	 *
	 * @param sessionID
	 * @param param
	 * @return
	 */
	@POST
	@Path("/getDummyCards")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse getDummyCards(@HeaderParam("sessionID") String sessionID, GetDummyCardsParam param);

	@POST
	@Path("/getArgineAdvice")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse getArgineAdvice(@HeaderParam("sessionID") String sessionID);

	/**
	 * ------------------------------------------------------------------------------------------
	 **/

	class LeaveGameParam {
		public long gameID = -1;
		public long tableID = -1;
        public String gameIDstr = "";

		@JsonIgnore
		public boolean isValid() {
			if (gameID <= 0) {
				return gameIDstr != null && gameIDstr.length() != 0;
			}
			return true;
		}

		@JsonIgnore
		public String toString() {
			return "gameID=" + gameID + " - gameIDstr=" + gameIDstr + " - tableID=" + tableID;
		}
	}

	class LeaveGameResponse {
		public boolean status;
	}

	class PlayBidParam {
		public long gameID;
		public String bid;
		public long tableID = -1;
        public String gameIDstr = "";
        public int step = -1;

		@JsonIgnore
		public boolean isValid() {
			if (gameID <= 0) {
                if (gameIDstr == null || gameIDstr.length() == 0) {
                    return false;
				}
			}
			return bid != null && bid.length() != 0;
		}

		@JsonIgnore
		public String toString() {
			return "gameID=" + gameID + " - gameIDstr=" + gameIDstr + " - bid=" + bid + " - step=" + step;
		}
	}

	/**
	 * ------------------------------------------------------------------------------------------
	 **/

	class PlayBidResponse {
		public boolean status;
	}

	class GetPlayBidInformationParam {
		public long gameID;
		public String bids;
		public long tableID = -1;
        public String gameIDstr = "";

		@JsonIgnore
		public boolean isValid() {
			if (gameID <= 0) {
                if (gameIDstr == null || gameIDstr.length() == 0) {
                    return false;
				}
			}
			return bids != null && bids.length() != 0;
		}

		@JsonIgnore
		public String toString() {
			return "gameID=" + gameID + " - gameIDstr=" + gameIDstr + " - bids=" + bids + " - tableID=" + tableID;
		}
	}

	class GetPlayBidInformationResponse {
		public String information;
		public String text;
		public String forcing = "";
		public boolean alert = false;
        public String numPointsText;
        public String clubText;
        public String diamondText;
        public String heartText;
		public String spadeText;
    }

    /**------------------------------------------------------------------------------------------**/

    class GetBidInformationParam {
        public String bids;
        public String lang;
        public long playerID = 0;
        public long dealID = 0;
        public String dealIDstr = "";
        public int tournamentCategory = 0;

        @JsonIgnore
        public boolean isValid() {
            if (bids == null || bids.length() == 0) {
                return false;
            }
            if (lang == null || lang.length() == 0) {
                return false;
            }
            if (dealID == 0) {
                if (dealIDstr == null || dealIDstr.length() == 0) {
                    return false;
				}
			}
			return tournamentCategory != 0;
        }

        @JsonIgnore
		public String toString() {
			return "bids=" + bids + " - lang=" + lang + " - playerID=" + playerID + " - dealID=" + dealIDstr + " - tournamentCategory=" + tournamentCategory;
		}
	}

	class PlayCardParam {
		public long gameID;
		public String card;
		public long tableID = -1;
        public String gameIDstr = "";
		public int step = -1;

		@JsonIgnore
		public boolean isValid() {
			if (gameID <= 0) {
                if (gameIDstr == null || gameIDstr.length() == 0) {
                    return false;
				}
			}
			return card != null && card.length() != 0;
		}

		@JsonIgnore
		public String toString() {
			return "gameID=" + gameID + " - gameIDstr=" + gameIDstr + " - card=" + card + " - tableID=" + tableID + " - step=" + step;
		}
	}

	class PlayCardResponse {
		public boolean status;
	}

	/**
	 * ------------------------------------------------------------------------------------------
	 **/

	class SetClaimSpreadResponseParam {
		public long gameID;
		public boolean response;
		public long tableID = -1;
		public String gameIDstr = "";

		@JsonIgnore
		public boolean isValid() {
			if (gameID <= 0) {
				return gameIDstr != null && gameIDstr.length() != 0;
			}
			return true;
		}

		@JsonIgnore
		public String toString() {
			return "gameID=" + gameID + " - gameIDstr=" + gameIDstr + " - response=" + response + " - tableID=" + tableID;
		}
	}

	class SetClaimSpreadResponseResponse {
		public boolean status;
	}

	class ViewGameParam {
		public long gameID;
        public long categoryID;
		public String gameIDstr;

		@JsonIgnore
		public boolean isValid() {
			if (gameID <= 0) {
				return gameIDstr != null && gameIDstr.length() != 0;
			}
			return true;
		}

		@JsonIgnore
		public String toString() {
			return "gameID=" + gameID + " - gameIDstr=" + gameIDstr + " - categoryID=" + categoryID;
		}
	}

	/**
	 * ------------------------------------------------------------------------------------------
	 **/

	class ViewGameResponse {
		public WSGameView gameView;
	}

	class ViewGameForDealScoreAndContractParam {
		public long dealID;
		public String dealIDstr;
		public int score;
		public String contract;
		public long categoryID;
		public String lead;

		@JsonIgnore
		public String toString() {
			return "categoryID=" + categoryID + " - dealIDstr=" + dealIDstr + " - score=" + score + " - contract=" + contract + " - lead=" + lead;
		}
	}

	class ViewGameForDealScoreAndContractResponse {
		public WSGameView gameView;
	}

	/**
	 * ------------------------------------------------------------------------------------------
	 **/

	class ResetGameParam {
		public long tableID;
		public long gameID;
		public String gameIDstr;

		@JsonIgnore
		public boolean isValid() {
			if (tableID <= 0) {
				return false;
			}
			if (gameID <= 0) {
				return gameIDstr != null && gameIDstr.length() != 0;
			}
			return true;
		}

		@JsonIgnore
		public String toString() {
			return "tableID=" + tableID + " - gameID=" + gameID + " - gameIDstr=" + gameIDstr;
		}
	}

	class ResetGameResponse {
		public long challengeID;
		public long currentTS;
		public long expirationTS;

		@JsonIgnore
		public String toString() {
			return "challengeID=" + challengeID + " - currentTS=" + currentTS + " - expirationTS=" + expirationTS;
		}
	}

	class ClaimParam {
		public long tableID;
        public long gameID;
        public int numTricks;
		public String gameIDstr;
		public int step = -1;

		@JsonIgnore
        public boolean isValid() {
            if (gameID <= 0) {
                if (gameIDstr == null || gameIDstr.length() == 0) {
					return false;
				}
			}
			return numTricks >= 0 && numTricks <= 13;
		}

		@JsonIgnore
		public String toString() {
			return "tableID=" + tableID + " - gameID=" + gameID + " - gameIDstr=" + gameIDstr + " - numTricks=" + numTricks + " - step=" + step;
		}
	}

	/**
	 * ------------------------------------------------------------------------------------------
	 **/

	class ClaimResponse {
		public boolean result = false;

		@JsonIgnore
		public String toString() {
			return "result=" + result;
		}
    }

    class StartGameParam {
        public long gameID;
        public String gameIDstr;
        public int conventionProfil = 0;
        public String conventionValue = null;
        public int cardsConventionProfil = 0;
		public String cardsConventionValue = null;

		@JsonIgnore
		public boolean isValid() {
			if (gameID <= 0) {
				return gameIDstr != null && gameIDstr.length() != 0;
			}
			return true;
		}

		@JsonIgnore
		public String toString() {
			return "gameID=" + gameID + " - gameIDstr=" + gameIDstr + " - conventionProfil=" + conventionProfil + " - conventionValue=" + conventionValue + " - cardsConventionProfil=" + cardsConventionProfil + " - cardsConventionValue=" + cardsConventionValue;
		}
	}

	class StartGameResponse {
		public boolean result = false;

		@JsonIgnore
		public String toString() {
			return "result=" + result;
		}
	}

	/**------------------------------------------------------------------------------------------**/

	class ReplayParam {
		public long dealID;
		public String dealIDstr;
		public int categoryID;
        public int conventionProfil;
        public String conventionValue = null;
        public int cardsConventionProfil = 0;
        public String cardsConventionValue = null;
		public boolean skipBids = false;

		@JsonIgnore
		public boolean isValid() {
			if (dealID <= 0) {
				return dealIDstr != null && dealIDstr.length() != 0;
			}
			return true;
		}

		@JsonIgnore
		public String toString() {
			return "dealID=" + dealID + " - categoryID=" + categoryID + " - conventionProfil=" + conventionProfil + " - conventionValue=" + conventionValue + " - cardsConventionProfil=" + cardsConventionProfil + " - cardsConventionValue=" + cardsConventionValue + " - skipBids=" + skipBids;
		}
	}

	class ReplayResponse {
		public WSTableTournament tableTournament;

		@JsonIgnore
		public String toString() {
			return "tableTournament=" + tableTournament;
        }
    }

    class GetDummyCardsParam {
		public long gameID;
		public String gameIDstr;

		@JsonIgnore
		public boolean isValid() {
			if (gameID <= 0) {
				return gameIDstr != null && gameIDstr.length() != 0;
			}
			return true;
		}

		@JsonIgnore
		public String toString() {
			return "gameID=" + gameID + " - gameIDstr=" + gameIDstr;
		}
	}

	class GetDummyCardsResponse {
		public String distributionDummy;

		@JsonIgnore
		public String toString() {
			return "distributionDummy=" + distributionDummy;
		}
	}

	class GetArgineAdviceResponse {
		public String result;

		@JsonIgnore
		public String toString() {
			return "result=" + result;
        }
    }
}
