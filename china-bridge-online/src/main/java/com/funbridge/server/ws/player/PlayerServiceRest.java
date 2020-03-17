package com.funbridge.server.ws.player;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.ws.FBWSResponse;

import javax.ws.rs.*;
import java.util.List;
import java.util.Map;

@Path("/player")
public interface PlayerServiceRest {
    /**------------------------------------------------------------------------------------------**/
    /**
     * Return the player info associated to this session
     */
    @POST
    @Path("/getPlayerInfo")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getPlayerInfo(@HeaderParam("sessionID") String sessionID);

    /**
     * Set the value for a parameter of player info
     */
    @POST
    @Path("/setPlayerInfoSettings")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse setPlayerInfoSettings(@HeaderParam("sessionID") String sessionID, SetPlayerInfoSettingsParam param);

    /**------------------------------------------------------------------------------------------**/

    /**
     * Return the player profile associated to the playerID.
     * ???ID????????
     */
    @POST
    @Path("/getPlayerProfile")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getPlayerProfile(@HeaderParam("sessionID") String sessionID, GetPlayerProfileParam param);

    /**
     * Set / modify player profile for current player
     */
    @POST
    @Path("/setPlayerProfile")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse setPlayerProfile(@HeaderParam("sessionID") String sessionID, SetPlayerProfileParam param);

    /**
     * Change the player mail
     */
    @POST
    @Path("/changePlayerMail")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse changePlayerMail(@HeaderParam("sessionID") String sessionID, ChangePlayerMailParam param);


    /**------------------------------------------------------------------------------------------**/

    /**
     * Change the player password
     */
    @POST
    @Path("/changePlayerPassword2")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse changePlayerPassword2(@HeaderParam("sessionID") String sessionID, ChangePlayerPasswordParam param);

    /**
     * Set image for player avatar. The image is a string base 64.
     */
    @POST
    @Path("/setPlayerAvatar")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse setPlayerAvatar(@HeaderParam("sessionID") String sessionID, SetPlayerAvatarParam param);

    /**
     * return the image for player avatar. The return value is the string base 64 for image data bytes.
     */
    @POST
    @Path("/getPlayerAvatar")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getPlayerAvatar(@HeaderParam("sessionID") String sessionID, GetPlayerAvatarParam param);

    /**------------------------------------------------------------------------------------------**/

    /**
     * Return the context information
     */
    @POST
    @Path("/getContextInfo")
    @Produces("application/json")
    FBWSResponse getContextInfo(@HeaderParam("sessionID") String sessionID);

    /**
     * Return the list of player linked
     */
    @POST
    @Path("/getPlayerLinked")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getPlayerLinked(@HeaderParam("sessionID") String sessionID, GetPlayerLinkedParam param);

    /**
     * search for player
     */
    @POST
    @Path("/searchPlayer")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse searchPlayer(@HeaderParam("sessionID") String sessionID, SearchPlayerParam param);

    /**------------------------------------------------------------------------------------------**/

    /**
     * set the link between player
     */
    @POST
    @Path("/setLink")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse setLink(@HeaderParam("sessionID") String sessionID, SetLinkParam param);

    /**
     * getPlayer : return player object with data set according to player who asks
     */
    @POST
    @Path("/getPlayer")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getPlayer(@HeaderParam("sessionID") String sessionID, GetPlayerParam param);

    /**
     * getProfile : return profile object with data set according to player who asks
     */
    @POST
    @Path("/getProfile")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getProfile(@HeaderParam("sessionID") String sessionID, GetProfileParam param);

    /**------------------------------------------------------------------------------------------**/

    /**
     * getLinkedPlayers : return list of player linked to the player who asks
     */
    @POST
    @Path("/getLinkedPlayers")
    @Produces("application/json")
    FBWSResponse getLinkedPlayers(@HeaderParam("sessionID") String sessionID, GetLinkedPlayersParam param);

    /**
     * getLinkedPlayersLight : return list of player linked to the player who asks (light object returned)
     */
    @POST
    @Path("/getLinkedPlayersLight")
    @Produces("application/json")
    FBWSResponse getLinkedPlayersLight(@HeaderParam("sessionID") String sessionID);

    /**
     * Set the player location (GPS)
     *
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/setLocation")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse setLocation(@HeaderParam("sessionID") String sessionID, SetLocationParam param);

    /**------------------------------------------------------------------------------------------**/

    /**
     * Check if the email have an account on FunBridge
     *
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/checkEmails")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse checkEmails(@HeaderParam("sessionID") String sessionID, CheckEmailsParam param);

    /**
     * Return list of suggested players
     *
     * @param sessionID
     * @return
     */
    @POST
    @Path("/getSuggestedPlayers")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getSuggestedPlayers(@HeaderParam("sessionID") String sessionID);

    /**
     * Change the player pseudo
     */
    @POST
    @Path("/changePlayerPseudo")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse changePlayerPseudo(@HeaderParam("sessionID") String sessionID, ChangePlayerPseudoParam param);

    /**------------------------------------------------------------------------------------------**/

    /**
     * Get list country player
     */
    @POST
    @Path("/getListCountryPlayer")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getListCountryPlayer(@HeaderParam("sessionID") String sessionID);

    /**
     * Get map of playerID => serieStatus for each friend of player
     *
     * @param sessionID
     * @return
     */
    @POST
    @Path("/getFriendsSerieStatus")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getFriendsSerieStatus(@HeaderParam("sessionID") String sessionID);

    /**
     * Get list of connected players
     *
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/getConnectedPlayers")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getConnectedPlayers(@HeaderParam("sessionID") String sessionID, GetConnectedPlayersParam param);

    /**------------------------------------------------------------------------------------------**/

    /**
     * Get list of duels for a player
     *
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/getPlayerDuels")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getPlayerDuels(@HeaderParam("sessionID") String sessionID, GetPlayerDuelsParam param);

    /**
     * Save stats from application. Set it in the session memory object and store in DB at the end of session.
     *
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/setApplicationStats")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse setApplicationStats(@HeaderParam("sessionID") String sessionID, SetApplicationStatsParam param);

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetPlayerInfoResponse {
        public WSPlayerInfo playerInfo;
    }

    class SetPlayerInfoSettingsParam {
        public String key;
        public String value;

        @JsonIgnore
        public String toString() {
            return "key=" + key + " - value=" + value;
        }
    }

    class SetPlayerInfoSettingsResponse {
        public WSPlayerInfo playerInfo;
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetPlayerProfileParam {
        public long playerID;

        @JsonIgnore
        public String toString() {
            return "playerID=" + playerID;
        }
    }

    class GetPlayerProfileResponse {
        public WSPlayerProfile playerProfile;
    }

    class SetPlayerProfileParam {
        public WSPlayerProfile profile;

        @JsonIgnore
        public String toString() {
            return "profile=" + profile;
        }
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class SetPlayerProfileResponse {
        public WSPlayerInfo playerInfo;
    }

    class ChangePlayerMailParam {
        public String newmail;
        public String password;

        @JsonIgnore
        public String toString() {
            return "newmail=" + newmail + " - password=" + password;
        }
    }

    class ChangePlayerMailResponse {
        public boolean result;
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class ChangePlayerPasswordParam {
        public String oldPassword;
        public String newPassword;

        @JsonIgnore
        public boolean isValid() {
            if (oldPassword == null || oldPassword.length() == 0) {
                return false;
            }
            return newPassword != null && newPassword.length() != 0;
        }

        @JsonIgnore
        public String toString() {
            return "oldPassword=" + oldPassword + " - newPassword=" + newPassword;
        }
    }

    class ChangePlayerPasswordResponse {
        public boolean result;
    }

    class SetPlayerAvatarParam {
        public String imageData;

        @JsonIgnore
        public String toString() {
            return "data=...";
        }
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class SetPlayerAvatarResponse {
        public WSPlayerInfo playerInfo;
    }

    class GetPlayerAvatarParam {
        public long playerID;

        @JsonIgnore
        public String toString() {
            return "playerID=" + playerID;
        }
    }

    class GetPlayerAvatarResponse {
        public String imageData;
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetContextInfoResponse {
        public WSContextInfo contextInfo;
    }

    class GetPlayerLinkedParam {
        public long playerID;
        public int offset = 0;
        public int nbMax = 50;
        public int type = 0;

        @JsonIgnore
        public boolean isValid() {
            if (playerID <= 0) {
                return false;
            }
            return type == 0 || type == 1 || type == 2;
        }

        @JsonIgnore
        public String toString() {
            return "playerID=" + playerID + " - type=" + type + " - offset=" + offset + " - nbMax=" + nbMax;
        }
    }

    class GetPlayerLinkedResponse {
        public List<WSPlayerLinked> results;
        public int offset;
        public int totalSize;
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class SearchPlayerParam {
        public String pattern;
        public int type;

        @JsonIgnore
        public boolean isValid() {
            if (pattern == null || pattern.trim().length() == 0) {
                return false;
            }
            return type >= 0 && type <= 3;
        }

        @JsonIgnore
        public String toString() {
            return "pattern=" + pattern.trim() + " - type=" + type;
        }
    }

    class SearchPlayerResponse {
        public List<WSPlayerLinked> results;
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class SetLinkParam {
        public long playerID;
        public int mask;
        public String message;

        @JsonIgnore
        public boolean isValid() {
            if (playerID <= 0) {
                return false;
            }
            return mask >= 0 && mask <= 35;
        }

        @JsonIgnore
        public String toString() {
            return "playerID=" + playerID + " - mask=" + mask;
        }
    }

    class SetLinkResponse {
        public int mask;
    }

    class GetPlayerParam {
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
            return "playerID=" + playerID;
        }
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetPlayerResponse {
        public WSPlayer player;
    }

    class GetProfileParam {
        public long playerID;

        @JsonIgnore
        public boolean isValid() {
            return playerID > 0;
        }

        @JsonIgnore
        public String toString() {
            return "playerID=" + playerID;
        }
    }

    class GetProfileResponse {
        public WSProfile profile;
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetLinkedPlayersParam {
        public boolean loadDuel = true;

        @JsonIgnore
        public boolean isValid() {
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "loadDuel=" + loadDuel;
        }
    }

    class GetLinkedPlayersResponse {
        public List<WSPlayer> results;
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetLinkedPlayersLightResponse {
        public List<WSPlayerLight> results;
    }

    class SetLocationParam {
        public double latitude;
        public double longitude;

        @JsonIgnore
        public boolean isValid() {
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "latitude=" + latitude + " - longitude=" + longitude;
        }
    }

    class SetLocationResponse {
        public boolean result;

        public SetLocationResponse(boolean result) {
            this.result = result;
        }
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class CheckEmailsParam {
        public String listEmails;

        @JsonIgnore
        public boolean isValid() {
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "listEmails data crypt ...";
        }
    }

    class CheckEmailsResponse {
        public List<WSPlayer> listResultPresent;
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetSuggestedPlayersResponse {
        public List<WSPlayer> listPlayer;
    }

    class ChangePlayerPseudoParam {
        public String pseudo; // new pseudo

        @JsonIgnore
        public String toString() {
            return "pseudo=" + pseudo;
        }

        public boolean isValid() {
            return pseudo != null && pseudo.length() != 0;
        }
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class ChangePlayerPseudoResponse {
        public WSPlayerInfo playerInfo;
    }

    class GetListCountryPlayerResponse {
        public List<WSCommunityCountryPlayer> listCountryPlayer;
    }

    class GetFriendsSerieStatusResponse {
        public Map<Long, WSSerieStatus> mapSerieStatus;
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetConnectedPlayersParam {
        public int offset;
        public int nbMax;
        public String serie = null;
        public String countryCode = null;
        public boolean myFriends = false;

        @JsonIgnore
        public String toString() {
            return "serie=" + serie + " - countryCode=" + countryCode + " - myFriends=" + myFriends + " - offset=" + offset + " - nbMax=" + nbMax;
        }

        @JsonIgnore
        public boolean isValid() {
            return true;
        }
    }

    class GetConnectedPlayersResponse {
        public int totalSize;
        public int offset;
        public List<WSPlayerConnected> players;

        @JsonIgnore
        public String toString() {
            return "totalSize=" + totalSize + " - list size=" + players.size() + " - offset=" + offset;
        }
    }

    class GetPlayerDuelsParam {
        public long playerID;
        public int offset;
        public int nbMax;

        @JsonIgnore
        public String toString() {
            return "playerID=" + playerID + " - offset=" + offset + " - nbMax=" + nbMax;
        }

        @JsonIgnore
        public boolean isValid() {
            return true;
        }

    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetPlayerDuelsResponse {
        public int totalSize;
        public int offset;
        public List<WSPlayerDuel> playerDuels;
    }

    class SetApplicationStatsParam {
        public String data;

        @JsonIgnore
        public String toString() {
            return "data=" + data;
        }

        @JsonIgnore
        public boolean isValid() {
            return data != null && data.length() != 0;
        }
    }

    class SetApplicationStatsResponse {
        public boolean result;
    }
}
