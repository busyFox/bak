package com.funbridge.server.ws.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.ws.FBWSResponse;

import javax.ws.rs.*;
import java.util.List;

@Path("/result")
public interface ResultServiceRest {
    /**------------------------------------------------------------------------------------------**/
    /**
     * Return a summary of result for a deal and player in replay mode. The summary contains the initial result for the player, the replay result,
     * the result must played, and the list of previous result for this player
     */
    @POST
    @Path("/getResultSummaryForReplayDeal")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getResultSummaryForReplayDeal(@HeaderParam("sessionID") String sessionID, GetResultSummaryForReplayDealParam param);

    /**
     * Return an element with the list of result for this deal and the index of element in the list for this player.
     * An element for each game finished by a player on this deal.
     */
    @POST
    @Path("/getResultForDeal")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getResultForDeal(@HeaderParam("sessionID") String sessionID, GetResultForDealParam param);

    /**
     * Reset the result for a player on a category. A date is stored to indicate at what point results sould be taken into account.
     */
    @POST
    @Path("/resetLastResultForPlayer")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse resetLastResultForPlayer(@HeaderParam("sessionID") String sessionID, ResetLastResultForPlayerParam param);

    /**------------------------------------------------------------------------------------------**/

    /**
     * Return the list of tournament played (archive)
     */
    @POST
    @Path("/getResultTournamentArchiveForCategory")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getResultTournamentArchiveForCategory(@HeaderParam("sessionID") String sessionID, GetResultTournamentArchiveForCategoryParam param);

    /**
     * Return result detail for tournament : list of result deal and tournament info with player result
     */
    @POST
    @Path("/getResultDealForTournament")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getResultDealForTournament(@HeaderParam("sessionID") String sessionID, GetResultDealForTournamentParam param);

    /**
     * Return the result player for a tournament. The return list is sorted by ranking
     */
    @POST
    @Path("/getResultForTournament")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getResultForTournament(@HeaderParam("sessionID") String sessionID, GetResultForTournamentParam param);


    /**------------------------------------------------------------------------------------------**/

    /**
     * Return the information of serie for player
     */
    @POST
    @Path("/getSerieSummary2")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getSerieSummary2(@HeaderParam("sessionID") String sessionID, GetSerieSummaryParam param);

    /**
     * ------------------------------------------------------------------------------------------
     **/
    @POST
    @Path("/getRankingSerie2")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getRankingSerie2(@HeaderParam("sessionID") String sessionID, GetRankingSerie2Param param);

    /**
     * Return the ranking for player on the preivous period
     */
    @POST
    @Path("/getPreviousRankingSerie")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getPreviousRankingSerie(@HeaderParam("sessionID") String sessionID, GetPreviousRankingSerieParam param);


    /**------------------------------------------------------------------------------------------**/

    /**
     * Return the information of training category for player
     */
    @POST
    @Path("/getTrainingSummary")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getTrainingSummary(@HeaderParam("sessionID") String sessionID, GetTrainingSummaryParam param);

    /**
     * Return the information of training category for player
     */
    @POST
    @Path("/getTrainingSummary2")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getTrainingSummary2(@HeaderParam("sessionID") String sessionID);

    /**
     * Return data for ending deal
     */
    @POST
    @Path("/getDealResultSummary")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getDealResultSummary(@HeaderParam("sessionID") String sessionID, GetDealResultSummaryParam param);

    /**------------------------------------------------------------------------------------------**/

    /**
     * Return list of tournament serie Top challenge
     */
    @POST
    @Path("/getSerieTopChallengeSummary")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getSerieTopChallengeSummary(@HeaderParam("sessionID") String sessionID, GetSerieTopChallengeSummaryParam param);

    /**
     * Return list of tournament serie Easy challenge
     */
    @POST
    @Path("/getSerieEasyChallengeSummary")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getSerieEasyChallengeSummary(@HeaderParam("sessionID") String sessionID, GetSerieEasyChallengeSummaryParam param);

    /**
     * Return list of tournament archive for a category
     */
    @POST
    @Path("/getTournamentArchives")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getTournamentArchives(@HeaderParam("sessionID") String sessionID, GetTournamentArchivesParam param);

    /**------------------------------------------------------------------------------------------**/

    /**
     * Return main ranking
     */
    @POST
    @Path("/getMainRanking")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getMainRanking(@HeaderParam("sessionID") String sessionID, GetMainRankingParam param);

    /**
     * Return Duel Best Score Ever
     */
    @POST
    @Path("/getDuelBestScoreEver")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getDuelBestScoreEver(GetDuelBestScoreParam param);

    /**
     * Return Duel Best Score Monthly
     */
    @POST
    @Path("/getDuelBestScoreMonthly")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getDuelBestScoreMonthly(GetDuelBestScoreMonthlyParam param);

    /**------------------------------------------------------------------------------------------**/

    /**
     * Return Duel Best Score Weekly
     */
    @POST
    @Path("/getDuelBestScoreWeekly")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getDuelBestScoreWeekly(GetDuelBestScoreWeeklyParam param);

    class GetResultSummaryForReplayDealParam {
        public long dealID;
        public String dealIDstr;
        public int categoryID = 0;

        @JsonIgnore
        public boolean isValid() {
            if (dealID <= 0) {
                return dealIDstr != null && dealIDstr.length() != 0;
            }
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "dealID=" + dealID + " - categoryID=" + categoryID;
        }
    }

    class GetResultSummaryForReplayDealResponse {
        public WSResultReplayDealSummary resultReplayDealSummary;
    }

    class GetResultForDealParam {
        public long dealID;
        public String dealIDstr;
        public boolean groupByContract;
        public boolean followed = false;
        public int categoryID = 0;
        public int offset = 0;
        public int nbMaxResult = 0;

        @JsonIgnore
        public boolean isValid() {
            if (dealID <= 0) {
                return dealIDstr != null && dealIDstr.length() != 0;
            }
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "categoryID=" + categoryID + " - dealIDstr=" + dealIDstr + " - groupByContract=" + groupByContract + " - followed=" + followed + " - offset=" + offset + " - nbMaxResult=" + nbMaxResult;
        }
    }

    class GetResultForDealResponse {
        public WSResultDealTournament resultDealTournament;
    }

    class ResetLastResultForPlayerParam {
        public long categoryID;
        public int resultType = 0;

        @JsonIgnore
        public boolean isValid() {
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "categoryID=" + categoryID + " - resultType=" + resultType;
        }
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class ResetLastResultForPlayerResponse {
        public boolean status;
    }

    class GetResultTournamentArchiveForCategoryParam {
        public long categoryID;
        public int offset;
        public int count;
        public String commentedAuthorID;

        @JsonIgnore
        public boolean isValid() {
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "categoryID=" + categoryID + " - offset=" + offset + " - count=" + count + " - commentedAuthorID=" + commentedAuthorID;
        }
    }

    class GetResultTournamentArchiveForCategoryResponse {
        public WSResultArchive resultArchive;
    }


    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetResultDealForTournamentParam {
        public long tournamentID;
        public String tournamentIDstr;
        public int categoryID = 0;

        @JsonIgnore
        public boolean isValid() {
            if (tournamentID <= 0) {
                return tournamentIDstr != null && tournamentIDstr.length() != 0;
            }
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "categoryID=" + categoryID + " - tournamentID=" + tournamentID + " - tournamentIDstr=" + tournamentIDstr;
        }
    }

    class GetResultDealForTournamentResponse {
        public WSResultDealTournament resultDealTournament;
    }

    class GetResultForTournamentParam {
        public long tournamentID;
        public String tournamentIDstr;
        public int offset;
        public int nbMaxResult;
        public boolean followed = false;
        public int categoryID = 0;
        public boolean orderFinished = false;

        @JsonIgnore
        public boolean isValid() {
            if (tournamentID <= 0) {
                return tournamentIDstr != null && tournamentIDstr.length() != 0;
            }
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "categoryID=" + categoryID + " - tournamentIDstr=" + tournamentIDstr + " - offset=" + offset + " - nbMaxResult=" + nbMaxResult + " - followed=" + followed + " - orderFinished=" + orderFinished;
        }
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetResultForTournamentResponse {
        public WSResultTournament resultTournament;
    }

    /**------------------------------------------------------------------------------------------**/

    class GetSerieSummaryParam {
        public boolean rankingExtract;
        public int nbRankingItems;
    }

    class GetSerieSummary2Response {
        public WSSerieSummary2 serieSummary;
    }

    /**------------------------------------------------------------------------------------------**/

    class GetRankingSerie2Param {
        public String serie;
        public int offset;
        public int nbMaxResult;

        @JsonIgnore
        public boolean isValid() {
            return serie != null && serie.length() != 0;
        }

        @JsonIgnore
        public String toString() {
            return "serie=" + serie + " - offset=" + offset + " - nbMaxResult=" + nbMaxResult;
        }
    }

    class GetRankingSerieResponse {
        public WSRankingSerie rankingSerie;
    }

    class GetPreviousRankingSerieParam {
        public int offset;
        public int nbMaxResult;
        public String previousSerie;

        @JsonIgnore
        public String toString() {
            return "previousSerie=" + previousSerie + " - offset=" + offset + " - nbMaxResult=" + nbMaxResult;
        }
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetPreviousRankingSerieResponse {
        public WSPreviousRankingSerie rankingSerie;
    }

    class GetTrainingSummaryParam {
        public int nbLastTournament;
        public int resultType = 0;

        @JsonIgnore
        public boolean isValid() {
            return nbLastTournament > 0;
        }

        @JsonIgnore
        public String toString() {
            return "nbLastTournament=" + nbLastTournament + " - resultType=" + resultType;
        }
    }

    class GetTrainingSummaryResponse {
        public WSTrainingSummary trainingSummary;
    }

    /**------------------------------------------------------------------------------------------**/

    class GetDealResultSummaryParam {
        public int categoryID;
        public String dealID;
        public int nbMaxMostPlayedContracts = 5;

        @JsonIgnore
        public boolean isValid() {
            if (categoryID <= 0) {
                return false;
            }
            if (dealID == null || dealID.length() == 0) {
                return false;
            }
            return nbMaxMostPlayedContracts > 0;
        }

        @JsonIgnore
        public String toString() {
            return "categoryID=" + categoryID + " - dealID=" + dealID + " - nbMaxMostPlayedContracts=" + nbMaxMostPlayedContracts;
        }
    }

    class GetSerieTopChallengeSummaryParam {
        public int nbArchives = -1;

        @JsonIgnore
        public boolean isValid() {
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "nbArchives=" + nbArchives;
        }
    }

    class GetSerieTopChallengeSummaryResponse {
        public int nbPlayedTournaments;
        public int nbTotalTournaments;
        public int nbTotalArchives;
        public List<WSTournamentArchive> archives;
    }


    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetSerieEasyChallengeSummaryParam {
        public int nbArchives = -1;

        @JsonIgnore
        public boolean isValid() {
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "nbArchives=" + nbArchives;
        }
    }

    class GetSerieEasyChallengeSummaryResponse {
        public int nbPlayedTournaments;
        public int nbTotalTournaments;
        public int nbTotalArchives;
        public List<WSTournamentArchive> archives;
    }

    class GetTournamentArchivesParam {
        public int categoryID;
        public String commentedAuthorID;
        public int offset;
        public int count;

        @JsonIgnore
        public boolean isValid() {
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "categoryID=" + categoryID + " - offset=" + offset + " - count=" + count + " - commentedAuthorID=" + commentedAuthorID;
        }
    }

    /**------------------------------------------------------------------------------------------**/

    class GetTournamentArchivesResponse {
        public int offset;
        public int totalSize;
        public List<WSTournamentArchive> archives;
    }

    class GetMainRankingParam {
        public String type;
        public String options;
        public int offset;
        public int nbMaxResult;

        @JsonIgnore
        public boolean isValid() {
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "type=" + type + " - options=" + options + " - offset=" + offset + " - nbMaxResult=" + nbMaxResult;
        }
    }

    /**------------------------------------------------------------------------------------------**/

    class GetMainRankingResponse {
        public int offset;
        public int totalSize;
        public int nbRankedPlayers;
        public List<WSMainRankingPlayer> ranking;
        public WSMainRankingPlayer rankingPlayer;
    }

    class GetDuelBestScoreParam {
        public long rivalId;

        @JsonIgnore
        public String toString() {
            return "rivalId=" + rivalId;
        }
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetDuelBestScoreMonthlyParam {
        public long rivalId;
        public String periodId;

        @JsonIgnore
        public boolean isValid() {
            return periodId != null && !periodId.isEmpty();
        }

        @JsonIgnore
        public String toString() {
            return "rivalId=" + rivalId + " - periodId=" + periodId;
        }
    }

    class GetDuelBestScoreWeeklyParam {
        public long rivalId;
        public String startDate;
        public String endDate;

        @JsonIgnore
        public boolean isValid() {
            return startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty();
        }

        @JsonIgnore
        public String toString() {
            return "rivalId=" + rivalId + " - startDate=" + startDate + " - endDate=" + endDate;
        }
    }

    class GetDuelBestScoreResponse {
        public List<PlayerDuelScoring> podium;
    }

    class PlayerDuelScoring {
        public long playerId;
        public String pseudo;
        public Double score;
    }

}
