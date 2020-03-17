package com.funbridge.server.ws.player;

import com.funbridge.server.common.*;
import com.funbridge.server.engine.ArgineProfile;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.PlayerMgr.PlayerUpdateType;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.tournament.duel.DuelMgr;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.ws.*;
import com.gotogames.common.crypt.AESCrypto;
import com.gotogames.common.session.Session;
import com.gotogames.common.tools.JSONTools;
import com.gotogames.common.tools.StringVersion;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.util.*;

@Service(value = "playerService")
@Scope(value = "singleton")
public class PlayerServiceRestImpl extends FunbridgeMgr implements PlayerServiceRest {
    @Resource(name = "playerMgr")
    private PlayerMgr playerMgr;
    @Resource(name = "playerCacheMgr")
    private PlayerCacheMgr playerCacheMgr;
    @Resource(name = "tourSerieMgr")
    private TourSerieMgr tourSerieMgr;
    @Resource(name = "presenceMgr")
    private PresenceMgr presenceMgr;
    @Resource(name = "messageNotifMgr")
    private MessageNotifMgr notifMgr;
    @Resource(name = "duelMgr")
    private DuelMgr duelMgr;
    @Resource(name = "playerUtilities")
    private PlayerUtilities playerUtilities = null;

    private JSONTools jsonTools = new JSONTools();

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

    @Override
    public FBWSResponse getPlayerInfo(String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetPlayerInfo(session));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetPlayerInfoResponse processGetPlayerInfo(FBSession session) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID());
        }
        Player p = session.getPlayer();
        WSPlayerInfo playerInfo = playerMgr.playerToWSPlayerInfo(session);
        playerInfo.profile.nbFriendsAndFollowers = playerMgr.countFriendAndFollowerForPlayer(p.getID());
        GetPlayerInfoResponse resp = new GetPlayerInfoResponse();
        resp.playerInfo = playerInfo;
        return resp;
    }

    @Override
    public FBWSResponse setPlayerInfoSettings(String sessionID, SetPlayerInfoSettingsParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetPlayerInfoSettings(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public SetPlayerInfoSettingsResponse processSetPlayerInfoSettings(FBSession session, SetPlayerInfoSettingsParam param) throws FBWSException {
        if (param == null) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        Player p = session.getPlayer();

        if (param.key.equals("SETTINGS")) {
            if (param.value == null || param.value.length() == 0) {
                log.error("Value for SETTINGS is null or empty");
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            String newSettingsJson = "";
            // Only for TEST & DEV with Pierre C
            if (FBConfiguration.getInstance().getIntValue("player.setPlayerInfoSettingsUpdateJsonSettings", 1) == 0) {
                newSettingsJson = param.value;
            } else {
                if (p.getSettings() == null || p.getSettings().length() == 0) {
                    newSettingsJson = param.value;
                } else {
                    try {
                        newSettingsJson = JSONTools.updateJSONString(p.getSettings(), param.value);
                    } catch (Exception e) {
                        log.error("Exception to update player settings p=" + p + " - ori=" + p.getSettings() + " - new=" + param.value, e);
                        throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                    }
                }
            }

            PlayerSettingsData plaSettings = null;
            try {
                // retrieve player settings data from JSON value
                plaSettings = jsonTools.mapData(newSettingsJson, PlayerSettingsData.class);
                if (plaSettings == null) {
                    throw new Exception("Player settings is null after mapData");
                }
            } catch (Exception e) {
                log.error("Exception to read player settings data=" + newSettingsJson, e);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            // save settings
            p.setSettings(newSettingsJson);
            // extract specific data of convention profile
            p.setConventionProfile(plaSettings.convention);
            // extract specific data from notifications settings
            boolean updateSubscriptionMail = false;
            if (plaSettings.notifications != null) {
                PlayerNotificationsSettingsData notifSettings = plaSettings.notifications;
                if (notifSettings.notifyResultsToFriends) {
                    p.addFriendFlag(Constantes.FRIEND_MASK_NOTIFICATION_DUEL);
                } else {
                    p.removeFriendFlag(Constantes.FRIEND_MASK_NOTIFICATION_DUEL);
                }
                if (notifSettings.receiveChatFromFriendsOnly) {
                    p.addFriendFlag(Constantes.FRIEND_MASK_MESSAGE_ONLY_FRIEND);
                } else {
                    p.removeFriendFlag(Constantes.FRIEND_MASK_MESSAGE_ONLY_FRIEND);
                }
                if (notifSettings.receiveDuelsFromFriendsOnly) {
                    p.addFriendFlag(Constantes.FRIEND_MASK_DUEL_ONLY_FRIEND);
                } else {
                    p.removeFriendFlag(Constantes.FRIEND_MASK_DUEL_ONLY_FRIEND);
                }
            }
            try {
                playerMgr.updatePlayerToDB(p, PlayerUpdateType.PROFILE);
                // update player in cache
                playerCacheMgr.updatePlayerAllData(p);
            } catch (Exception e) {
                log.error("Exception to update player in DB p=" + p.toString(), e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        // DUEL ONLY WITH FRIEND
        else if (param.key.equals("DUEL_ONLY_FRIEND")) {
            try {
                if (param.value.equals("1")) {
                    p.addFriendFlag(Constantes.FRIEND_MASK_DUEL_ONLY_FRIEND);
                } else {
                    p.removeFriendFlag(Constantes.FRIEND_MASK_DUEL_ONLY_FRIEND);
                }
                playerMgr.updatePlayerToDB(p, PlayerUpdateType.PROFILE);
            } catch (Exception e) {
                log.error("Exception to update player in DB p=" + p.toString(), e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        // MESSAGE ONLY WITH FRIEND
        else if (param.key.equals("MESSAGE_ONLY_FRIEND")) {
            try {
                if (param.value.equals("1")) {
                    p.addFriendFlag(Constantes.FRIEND_MASK_MESSAGE_ONLY_FRIEND);
                } else {
                    p.removeFriendFlag(Constantes.FRIEND_MASK_MESSAGE_ONLY_FRIEND);
                }
                playerMgr.updatePlayerToDB(p, PlayerUpdateType.PROFILE);
            } catch (Exception e) {
                log.error("Exception to update player in DB p=" + p.toString(), e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        // DISPLAY LANGUAGE
        else if (param.key.equals("UPDATE_DISPLAY_LANGUAGE")) {
            try {
                p.setDisplayLang(param.value);
                playerMgr.updatePlayerToDB(p, PlayerUpdateType.PROFILE);
            } catch (Exception e) {
                log.error("Exception to update player in DB p=" + p.toString(), e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        // INFO UNKNOWN !
        else {
            log.error("Key invalid ! key=" + param.key);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        SetPlayerInfoSettingsResponse resp = new SetPlayerInfoSettingsResponse();
        resp.playerInfo = playerMgr.playerToWSPlayerInfo(session);
        session.addPlayerActions("SET_SETTINGS(key:" + param.key + ")");
        return resp;
    }

    @Override
    public FBWSResponse getPlayerProfile(String sessionID, GetPlayerProfileParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetPlayerProfile(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetPlayerProfileResponse processGetPlayerProfile(FBSession session, GetPlayerProfileParam param) throws FBWSException {
        if (param == null) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        Player p = session.getPlayer();
        Player plaProfile = null;
        if (p.getID() == param.playerID) {
            plaProfile = p;
        } else {
            plaProfile = playerMgr.getPlayer(param.playerID);
        }

        if (plaProfile == null) {
            log.error("No player found for playerID=" + param.playerID);
            throw new FBWSException(FBExceptionType.PLAYER_NOT_EXISTING);
        }

        String newSerie = null;
        newSerie = tourSerieMgr.buildWSSerie(playerCacheMgr.getOrLoadPlayerCache(plaProfile.getID()));
        WSPlayerProfile playerProfile = plaProfile.toWSPlayerProfile(p.getID(), newSerie);
        playerProfile.linkMask = playerMgr.getLinkMaskBetweenPlayer(p.getID(), plaProfile.getID());
        playerProfile.nbFriendsAndFollowers = playerMgr.countFriendAndFollowerForPlayer(plaProfile.getID());
        GetPlayerProfileResponse resp = new GetPlayerProfileResponse();
        resp.playerProfile = playerProfile;
        return resp;
    }

    @Override
    public FBWSResponse setPlayerProfile(String sessionID, SetPlayerProfileParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetPlayerProfile(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public SetPlayerProfileResponse processSetPlayerProfile(FBSession session, SetPlayerProfileParam param) throws FBWSException {
        if (param == null) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        Player p = session.getPlayer();
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        if (param.profile != null) {
            String displayCountryCodeBefore = p.getDisplayCountryCode();
            p.setDisplayCountryCode(param.profile.countryCode);
            if (p.getDisplayCountryCode() != null && displayCountryCodeBefore != null && !p.getDisplayCountryCode().equals(displayCountryCodeBefore)) {
                // update country code in main ranking
                playerMgr.updatePlayerDisplayCountryCodeForRanking(p);
            }
            p.setBirthday(param.profile.birthdate);
            p.setDescription(param.profile.description);
            if (param.profile.firstName != null) {
                if (p.getFirstName() != null && !p.getFirstName().equals(param.profile.firstName)) {
                    p.setFirstName(param.profile.firstName);
                }
            }
            if (param.profile.lastName != null) {
                if (p.getLastName() != null && !p.getLastName().equals(param.profile.lastName)) {
                    p.setLastName(param.profile.lastName);
                }
            }
            p.setSex(param.profile.sex);
            p.setTown(param.profile.town);
            try {
                playerMgr.updatePlayerToDB(p, PlayerUpdateType.PROFILE_CREDIT);
                if (p.getDisplayCountryCode() != null && displayCountryCodeBefore != null && !p.getDisplayCountryCode().equals(displayCountryCodeBefore)) {
                    // update country code in main ranking
                    playerMgr.updatePlayerDisplayCountryCodeForRanking(p);
                }
            } catch (Exception e) {
                log.error("Exception to update player in DB p=" + p.toString(), e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        } else {
            log.error("Profile is null ");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        SetPlayerProfileResponse resp = new SetPlayerProfileResponse();
        resp.playerInfo = playerMgr.playerToWSPlayerInfo(session);
        session.addPlayerActions("SET_PROFILE");
        return resp;
    }

    @Override
    public FBWSResponse changePlayerMail(String sessionID, ChangePlayerMailParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processChangePlayerMail(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;

    }

    public ChangePlayerMailResponse processChangePlayerMail(FBSession session, ChangePlayerMailParam param) throws FBWSException {
        if (param == null) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        Player p = session.getPlayer();
        String oldMail = p.getMail();
        String passwordCrypt = p.getPassword();
        if (!passwordCrypt.equals(param.password)) {
            if (log.isDebugEnabled()) {
                log.debug("Fail to change login for player=" + p.getID() + " - old mail=" + p.getMail() + " - new mail=" + param.newmail + " - password=" + param.password + " - password not valid !");
            }
            throw new FBWSException(FBExceptionType.PRESENCE_INVALID_PASSWORD);
        }
        if (log.isDebugEnabled()) {
            log.debug("Change login for player=" + p.getID() + " - old mail=" + p.getMail() + " - new mail=" + param.newmail);
        }

        // check if new login exist
        if (playerMgr.existMail(param.newmail)) {
            if (log.isDebugEnabled()) {
                log.debug("Mail already used : newmail=" + param.newmail);
            }
            throw new FBWSException(FBExceptionType.PRESENCE_ALREADY_USED_LOGIN);
        }

        p.setMail(param.newmail);

        try {
            playerMgr.updatePlayerToDB(p, PlayerUpdateType.PROFILE);
        } catch (Exception e) {
            log.error("Exception to change login for player=" + session.getLoginID() + " - new mail = " + param.newmail, e);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }

        ChangePlayerMailResponse resp = new ChangePlayerMailResponse();
        resp.result = true;
        session.addPlayerActions("CHANGE_MAIL(old:" + oldMail + ", new:" + param.newmail + ")");
        return resp;
    }

    @Override
    public FBWSResponse changePlayerPassword2(String sessionID, ChangePlayerPasswordParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processChangePlayerPassword2(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }


    public ChangePlayerPasswordResponse processChangePlayerPassword2(FBSession session, ChangePlayerPasswordParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        Player p = session.getPlayer();
        // to change login, check password !
        String oldPasswordDescrypt = PlayerUtilities.cryptPassword(AESCrypto.decrypt(param.oldPassword, Constantes.CRYPT_KEY));
        String newPasswordDescrypt = AESCrypto.decrypt(param.newPassword, Constantes.CRYPT_KEY);
        if (!p.checkPassword(oldPasswordDescrypt)) {
            if (log.isDebugEnabled()) {
                log.debug("Fail to change login for player=" + p.getID() + " - oldPassword=" + oldPasswordDescrypt + " - password not valid ! current=" + p.getPassword());
            }
            throw new FBWSException(FBExceptionType.PRESENCE_INVALID_PASSWORD);
        }
        session.getPlayer().setPassword(PlayerUtilities.cryptPassword(newPasswordDescrypt));
        try {
            playerMgr.updatePlayerToDB(p, PlayerUpdateType.PROFILE);
        } catch (Exception e) {
            log.error("Exception to change password for player=" + session.getLoginID(), e);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        ChangePlayerPasswordResponse resp = new ChangePlayerPasswordResponse();
        resp.result = true;
        session.addPlayerActions("CHANGE_PASSWORD(old:" + oldPasswordDescrypt + ", new:" + newPasswordDescrypt + ")");
        return resp;
    }


    @Override
    public FBWSResponse setPlayerAvatar(String sessionID, SetPlayerAvatarParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetPlayerAvatar(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public SetPlayerAvatarResponse processSetPlayerAvatar(FBSession session, SetPlayerAvatarParam param) throws FBWSException {
        SetPlayerAvatarResponse resp = new SetPlayerAvatarResponse();
        if (param == null) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        Player p = session.getPlayer();

        // image data null => remove avatar
        if (param.imageData == null || param.imageData.length() == 0) {
            playerMgr.removeAvatarFileForPlayer(p);
            resp.playerInfo = playerMgr.playerToWSPlayerInfo(session);
        }
        // save avatar
        else {
            try {
                // Decode string base 64 to bytes
                byte[] dataImage = Base64.decodeBase64(param.imageData);
                if (log.isDebugEnabled()) {
                    log.debug("Set avatar for player=" + p.getID() + " - image size=" + dataImage.length);
                }

                // check size
                int maxSize = FBConfiguration.getInstance().getIntValue("player.avatar.maxsize", 0);
                if (maxSize == 0) maxSize = 1024 * 1024;
                if (dataImage.length > maxSize) {
                    log.error("Avatar image size is too big ! size=" + dataImage.length + " - player=" + p.getID());
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

                // save to file
                File fileAvatar = playerMgr.getAvatarFileForPlayer(p.getID());
                if (fileAvatar == null) {
                    log.error("avatar file is null for player=" + p);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                FileUtils.writeByteArrayToFile(fileAvatar, dataImage);
                // update flag in player info to indicate a avatar change
                p.setNewAvatar();
                try {
                    playerMgr.updatePlayerToDB(p, PlayerUpdateType.PROFILE);
                } catch (Exception e) {
                    log.error("Exception to update player in DB", e);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                resp.playerInfo = playerMgr.playerToWSPlayerInfo(session);
            } catch (Exception e) {
                log.error("Error to save avatar image for player=" + p.getID(), e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        session.addPlayerActions("SET_AVATAR");
        return resp;
    }

    @Override
    public FBWSResponse getPlayerAvatar(String sessionID, GetPlayerAvatarParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetPlayerAvatar(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetPlayerAvatarResponse processGetPlayerAvatar(FBSession session, GetPlayerAvatarParam param) throws FBWSException {
        if (param == null) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        Player p = session.getPlayer();
        // check player exist !
        Player pAvatar = playerMgr.getPlayer(param.playerID);
        if (pAvatar == null) {
            log.error("No player found for ID=" + param.playerID);
            throw new FBWSException(FBExceptionType.PLAYER_NOT_EXISTING);
        }
        boolean avatarAvailable = false;
        if (p.getID() == pAvatar.getID()) {
            avatarAvailable = pAvatar.isAvatarPresent();
        }
        if (!avatarAvailable) {
            if (log.isDebugEnabled()) {
                log.debug("No avatar present for player=" + param.playerID);
            }
            throw new FBWSException(FBExceptionType.PLAYER_NO_AVATAR);
        }
        GetPlayerAvatarResponse resp = new GetPlayerAvatarResponse();
        try {
            // open image file
            File fileAvatar = playerMgr.getAvatarFileForPlayer(pAvatar.getID());
            if (!fileAvatar.exists()) {
                if (log.isDebugEnabled()) {
                    log.debug("No avatar found for player=" + param.playerID);
                }
                throw new FBWSException(FBExceptionType.PLAYER_NO_AVATAR);
            }
            // avatar for this player exists ?
            if (!fileAvatar.exists()) {
                if (log.isDebugEnabled()) {
                    log.debug("No avatar found for player=" + param.playerID);
                }
                throw new FBWSException(FBExceptionType.PLAYER_NO_AVATAR);
            }
            byte[] byData = FileUtils.readFileToByteArray(fileAvatar);
            // transform byte data to string using base64
            String imageData = Base64.encodeBase64String(byData);
            resp.imageData = imageData;
        } catch (FBWSException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error to read avatar image for player=" + p.getID(), e);
        }
        return resp;
    }

    @Override
    public FBWSResponse getContextInfo(String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetContextInfo(session));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetContextInfoResponse processGetContextInfo(FBSession session) throws FBWSException {
        GetContextInfoResponse resp = new GetContextInfoResponse();
        resp.contextInfo = presenceMgr.getContextInfo(session);
        return resp;
    }

    @Override
    public FBWSResponse getPlayerLinked(String sessionID, GetPlayerLinkedParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetPlayerLinked(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetPlayerLinkedResponse processGetPlayerLinked(FBSession session, GetPlayerLinkedParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        GetPlayerLinkedResponse resp = new GetPlayerLinkedResponse();
        // list friend
        if (param.type == 0) {
            List<WSPlayerLinked> listLink = playerMgr.getListLinkedForType(session.getPlayer().getID(),
                    Constantes.PLAYER_LINK_TYPE_FRIEND, param.offset, param.nbMax);
            resp.results = listLink;
            resp.totalSize = playerMgr.countLinkForPlayerAndType(session.getPlayer().getID(),
                    Constantes.PLAYER_LINK_TYPE_FRIEND);
        }
        // list followers
        else if (param.type == 1) {
            List<WSPlayerLinked> listLink = playerMgr.getListLinkedForType(session.getPlayer().getID(),
                    Constantes.PLAYER_LINK_TYPE_FOLLOWER, param.offset, param.nbMax);
            resp.results = listLink;
            resp.totalSize = playerMgr.countLinkForPlayerAndType(session.getPlayer().getID(),
                    Constantes.PLAYER_LINK_TYPE_FOLLOWER);
        }
        // list requests friend
        else if (param.type == 2) {
            List<WSPlayerLinked> listLink = playerMgr.getListLinkedForType(session.getPlayer().getID(),
                    Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING, param.offset, param.nbMax);
            resp.results = listLink;
            resp.totalSize = playerMgr.countLinkForPlayerAndType(session.getPlayer().getID(),
                    Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING);
        }
        resp.offset = param.offset;
        return resp;
    }

    @Override
    public FBWSResponse searchPlayer(String sessionID, SearchPlayerParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSearchPlayer(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public SearchPlayerResponse processSearchPlayer(FBSession session, SearchPlayerParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        SearchPlayerResponse resp = new SearchPlayerResponse();
        resp.results = playerMgr.searchPlayerLinked(session.getPlayer().getID(), param.pattern.trim(), param.type, 0, 50);
        return resp;
    }

    @Override
    public FBWSResponse setLink(String sessionID, SetLinkParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetLink(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public SetLinkResponse processSetLink(FBSession session, SetLinkParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        Player pLink = playerMgr.getPlayer(param.playerID);
        if (pLink == null) {
            log.error("No player exist with ID=" + param.playerID);
            throw new FBWSException(FBExceptionType.PLAYER_NOT_EXISTING);
        }
        SetLinkResponse resp = new SetLinkResponse();
        resp.mask = playerMgr.updateLinkBetweenPlayer(session.getPlayer(), pLink, param.mask, param.message);
        return resp;
    }

    @Override
    public FBWSResponse getPlayer(String sessionID, GetPlayerParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetPlayer(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetPlayerResponse processGetPlayer(FBSession session, GetPlayerParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        Player playerData = playerMgr.getPlayer(param.playerID);
        if (playerData == null || playerData.getNicknameDeactivated().contains(Constantes.PLAYER_DISABLE_PATTERN)) {
            throw new FBWSException(FBExceptionType.PLAYER_NOT_EXISTING);
        }

        GetPlayerResponse resp = new GetPlayerResponse();
        resp.player = playerMgr.playerToWSPlayer(playerData, session.getPlayer(), true);
        changeWSPlayerProfileForOldVersion(resp.player, session.getClientVersion(), session.getDeviceType());
        return resp;
    }

    @Override
    public FBWSResponse getProfile(String sessionID, GetProfileParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetProfile(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetProfileResponse processGetProfile(FBSession session, GetProfileParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        Player playerProfile = playerMgr.getPlayer(param.playerID);
        if (playerProfile == null) {
            throw new FBWSException(FBExceptionType.PLAYER_NOT_EXISTING);
        }
        GetProfileResponse resp = new GetProfileResponse();
        resp.profile = playerMgr.playerToWSProfile(playerProfile, session.getPlayer().getID());
        return resp;
    }

    @Override
    public FBWSResponse getLinkedPlayers(String sessionID, GetLinkedPlayersParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetLinkedPlayers(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetLinkedPlayersResponse processGetLinkedPlayers(FBSession session, GetLinkedPlayersParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        List<Long> list = playerMgr.getListPlayerIDLinkedToPlayer(session.getPlayer().getID());
        GetLinkedPlayersResponse resp = new GetLinkedPlayersResponse();
        resp.results = new ArrayList<WSPlayer>();
        if (list != null) {
            for (Long l : list) {
                Player p = playerMgr.getPlayer(l);
                if (p != null) {
                    WSPlayer wsp = playerMgr.playerToWSPlayer(p, session.getPlayer(), param.loadDuel);
                    if (wsp != null) {
                        changeWSPlayerProfileForOldVersion(wsp, session.getClientVersion(), session.getDeviceType());
                        resp.results.add(wsp);
                    }
                }
            }
        }
        return resp;
    }

    @Override
    public FBWSResponse getLinkedPlayersLight(String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetLinkedPlayersLight(session));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetLinkedPlayersLightResponse processGetLinkedPlayersLight(FBSession session) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID());
        }
        List<Long> list = playerMgr.getListPlayerIDLinkedToPlayer(session.getPlayer().getID());
        GetLinkedPlayersLightResponse resp = new GetLinkedPlayersLightResponse();
        resp.results = new ArrayList<WSPlayerLight>();
        if (list != null) {
            for (Long l : list) {
                WSPlayerLight wsp = playerMgr.playerToWSPlayerLight(l, session.getPlayer().getID());
                if (wsp != null) {
                    resp.results.add(wsp);
                }
            }
        }
        return resp;
    }

    @Override
    public FBWSResponse setLocation(String sessionID, SetLocationParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetLocation(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public SetLocationResponse processSetLocation(FBSession session, SetLocationParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        if (param.latitude == 0 && param.longitude == 0) {
            // remove player location
            if (FBConfiguration.getInstance().getIntValue("player.setLocation.removeWith0", 1) == 1) {
                playerMgr.removePlayerLocation(session.getPlayer().getID());
            }
        } else {
            playerMgr.setPlayerLocation(session.getPlayer().getID(), param.longitude, param.latitude);
        }
        return new SetLocationResponse(true);
    }

    @Override
    public FBWSResponse checkEmails(String sessionID, CheckEmailsParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processCheckEmails(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public CheckEmailsResponse processCheckEmails(FBSession session, CheckEmailsParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        int limit = FBConfiguration.getInstance().getIntValue("player.checkEmails.limit", -1);
        // decrypt param data
        String strParam = AESCrypto.decrypt(param.listEmails, Constantes.CRYPT_KEY);
        // Map json param to CheckEmailsData
        CheckEmailsData checkEmailsData = null;
        try {
            checkEmailsData = jsonTools.mapData(strParam, CheckEmailsData.class);
        } catch (Exception e) {
            log.error("Failed to mapData to checkEmailsData -  strParam=" + strParam, e);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (checkEmailsData == null || checkEmailsData.emails == null || checkEmailsData.emails.isEmpty()) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        CheckEmailsResponse resp = new CheckEmailsResponse();
        resp.listResultPresent = new ArrayList<WSPlayer>();
        for (String e : checkEmailsData.emails) {
            if (limit > 0 && resp.listResultPresent.size() >= limit) {
                break;
            }
            Player p = playerMgr.getPlayerByMail(e);
            if (p != null) {
                WSPlayer wsp = playerMgr.playerToWSPlayer(p, session.getPlayer(), true);
                changeWSPlayerProfileForOldVersion(wsp, session.getClientVersion(), session.getDeviceType());
                resp.listResultPresent.add(wsp);
            } else {
                resp.listResultPresent.add(null);
            }
        }
        return resp;
    }

    @Override
    public FBWSResponse getSuggestedPlayers(String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                // check sessionID
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetSuggestedPlayers(session));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID is null");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetSuggestedPlayersResponse processGetSuggestedPlayers(FBSession session) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID());
        }
        GetSuggestedPlayersResponse resp = new GetSuggestedPlayersResponse();
        resp.listPlayer = playerMgr.getSuggestionForPlayer(session.getPlayer());
        for (WSPlayer e : resp.listPlayer) {
            changeWSPlayerProfileForOldVersion(e, session.getClientVersion(), session.getDeviceType());
        }
        return resp;
    }

    @Override
    public FBWSResponse changePlayerPseudo(String sessionID, ChangePlayerPseudoParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processChangePlayerPseudo(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public ChangePlayerPseudoResponse processChangePlayerPseudo(FBSession session, ChangePlayerPseudoParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        Player p = session.getPlayer();
        if (log.isDebugEnabled()) {
            log.debug("Change pseudo for player=" + p + " - old pseudo=" + p.getNickname() + " - new pseudo=" + param.pseudo);
        }
        String oldPseudo = p.getNickname();
        // check player has not change too many times ...

        // check pseudo is valid
        if (!PlayerMgr.checkPseudoFormat(param.pseudo)) {
            throw new FBWSException(FBExceptionType.PRESENCE_BAD_FORMAT_PSEUDO);
        }
        if (playerMgr.existPseudo(param.pseudo)) {
            if (log.isDebugEnabled()) {
                log.debug("Pseudo already used : newPseudo=" + param.pseudo);
            }
            throw new FBWSException(FBExceptionType.PRESENCE_ALREADY_USED_PSEUDO);
        }
        playerMgr.changePlayerPseudo(p, param.pseudo, true);
        ChangePlayerPseudoResponse resp = new ChangePlayerPseudoResponse();
        resp.playerInfo = playerMgr.playerToWSPlayerInfo(session);
        session.addPlayerActions("CHANGE_PSEUDO(old:" + oldPseudo + ", new:" + param.pseudo + ")");
        return resp;
    }

    @Override
    public FBWSResponse getListCountryPlayer(String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetListCountryPlayer(session));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetListCountryPlayerResponse processGetListCountryPlayer(FBSession session) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID());
        }
        GetListCountryPlayerResponse resp = new GetListCountryPlayerResponse();
        resp.listCountryPlayer = playerMgr.getCommunitylistCountryPlayer();
        return resp;
    }

    @Override
    public FBWSResponse getFriendsSerieStatus(String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetFriendsSerieStatus(session));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetFriendsSerieStatusResponse processGetFriendsSerieStatus(FBSession session) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID());
        }
        GetFriendsSerieStatusResponse resp = new GetFriendsSerieStatusResponse();
        resp.mapSerieStatus = new HashMap<>();
        List<Long> listFriendID = playerMgr.getListPlayerIDLinkFollower(session.getPlayer().getID());
        for (Long l : listFriendID) {
            resp.mapSerieStatus.put(l, tourSerieMgr.buildSerieStatusForPlayer(playerCacheMgr.getPlayerCache(l)));
        }
        return resp;
    }

    public FBWSResponse getConnectedPlayers(String sessionID, GetConnectedPlayersParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetConnectedPlayers(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetConnectedPlayersResponse processGetConnectedPlayers(FBSession session, GetConnectedPlayersParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (param.offset < 0) {
            param.offset = 0; // To fix bug of empty list on iOS
        }
        GetConnectedPlayersResponse resp = new GetConnectedPlayersResponse();
        resp.players = new ArrayList<>();
        List<Long> listFriend = null;
        if (param.myFriends) {
            listFriend = playerMgr.listFriendIDForPlayer(session.getPlayer().getID());
        }
        // remove info RESERVE from serie "S02R" => "SO2"
        if (param.serie != null && param.serie.length() > 0) {
            if (param.serie.endsWith("R")) {
                param.serie = param.serie.substring(0, param.serie.length() - 1);
            }
        }
        List<Session> listSession = presenceMgr.getAllCurrentSession();
        for (Session s : listSession) {
            FBSession fbs = (FBSession) s;
            // exclude current player
            if (fbs.getPlayer().getID() == session.getPlayer().getID()) {
                continue;
            }
            boolean addToList = false;
            // friend selection
            if (listFriend != null) {
                if (listFriend.contains(fbs.getPlayer().getID())) {
                    // serie
                    if ((param.serie != null && param.serie.length() > 0)) {
                        if (fbs.getSerie() != null && fbs.getSerie().equals(param.serie)) {
                            // country
                            if (param.countryCode != null && param.countryCode.length() > 0) {
                                if (fbs.getPlayer().getDisplayCountryCode().equalsIgnoreCase(param.countryCode)) {
                                    // friend + serie + country
                                    addToList = true;
                                }
                            } else {
                                // friend + serie
                                addToList = true;
                            }
                        }
                    }
                    // no param serie
                    else {
                        // country
                        if (param.countryCode != null && param.countryCode.length() > 0) {
                            if (fbs.getPlayer().getDisplayCountryCode().equalsIgnoreCase(param.countryCode)) {
                                // friend + country
                                addToList = true;
                            }
                        }
                        // no param country
                        else {
                            // only friend
                            addToList = true;
                        }
                    }
                }
            }
            // no friend selection
            else {
                // serie
                if ((param.serie != null && param.serie.length() > 0)) {
                    if (fbs.getSerie() != null && fbs.getSerie().equals(param.serie)) {
                        // country
                        if (param.countryCode != null && param.countryCode.length() > 0) {
                            if (fbs.getPlayer().getDisplayCountryCode().equalsIgnoreCase(param.countryCode)) {
                                // serie + country
                                addToList = true;
                            }
                        } else {
                            // serie
                            addToList = true;
                        }
                    }
                }
                // no param serie
                else {
                    // country
                    if (param.countryCode != null && param.countryCode.length() > 0) {
                        if (fbs.getPlayer().getDisplayCountryCode().equalsIgnoreCase(param.countryCode)) {
                            // country
                            addToList = true;
                        }
                    }
                    // no param country
                    else {
                        // all players
                        addToList = true;
                    }
                }
            }

            if (addToList) {
                resp.players.add(new WSPlayerConnected(fbs));
            }
        }
        resp.totalSize = resp.players.size();
        resp.offset = param.offset;
        if (resp.players.size() > 0) {
            // order list by pseudo
            Collections.sort(resp.players, new Comparator<WSPlayerConnected>() {
                @Override
                public int compare(WSPlayerConnected o1, WSPlayerConnected o2) {
                    int compareSerie = 0;
                    try {
                        compareSerie = -TourSerieMgr.compareSerie(o1.serie, o2.serie);
                    } catch (Exception e) {
                    }
                    if (compareSerie == 0) {
                        return o1.pseudo.toLowerCase().compareTo(o2.pseudo.toLowerCase());
                    }
                    return compareSerie;
                }
            });
            // sublist
            if (param.offset >= 0 && param.offset < resp.players.size()) {
                resp.players = resp.players.subList(param.offset, ((param.offset + param.nbMax) > resp.players.size()) ? resp.players.size() : (param.offset + param.nbMax));
            } else {
                resp.players.clear();
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param + " - response=" + resp);
        }
        return resp;
    }

    @Override
    public FBWSResponse getPlayerDuels(String sessionID, GetPlayerDuelsParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetPlayerDuels(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetPlayerDuelsResponse processGetPlayerDuels(FBSession session, GetPlayerDuelsParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        GetPlayerDuelsResponse resp = new GetPlayerDuelsResponse();
        resp.offset = param.offset;
        resp.totalSize = duelMgr.countDuelsForPlayerWithNbPlayedSup0(param.playerID, false);
        resp.playerDuels = duelMgr.listDuelsForPlayer(param.playerID, false, param.offset, param.nbMax);
        return resp;
    }

    public void changeWSPlayerProfileForOldVersion(WSPlayer wsPlayer, String clientVersion, String clientType) {
        // check profile with client version. For only new profile (> 14)
        if (wsPlayer.conventionProfil > 14 && FBConfiguration.getInstance().getIntValue("general.checkProfileAndVersion.enable", 1) == 1) {
            String clientVersionRequired = FBConfiguration.getInstance().getStringValue("general.checkProfileAndVersion." + clientType, null);
            if (clientVersionRequired != null) {
                if (StringVersion.compareVersion(clientVersionRequired, clientVersion) > 0) {
                    String convValue = "";
                    ArgineProfile argineProfile = ContextManager.getArgineEngineMgr().getProfile(wsPlayer.conventionProfil);
                    if (argineProfile != null) {
                        convValue = argineProfile.value;
                    }
                    // client version of connection data < client version required
                    wsPlayer.conventionProfil = 14;
                    wsPlayer.conventionValue = convValue;
                }
            }
        }
    }

    @Override
    public FBWSResponse setApplicationStats(String sessionID, SetApplicationStatsParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetApplicationStats(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception to save application stats for param=" + param + " - exception=" + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public SetApplicationStatsResponse processSetApplicationStats(FBSession session, SetApplicationStatsParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        try {
            HashMap<String, Object> mapData = jsonTools.getMapper().readValue(param.data, HashMap.class);
            session.setApplicationStats(mapData);
            SetApplicationStatsResponse resp = new SetApplicationStatsResponse();
            resp.result = true;
            return resp;
        } catch (Exception e) {
            log.error("Failed to readValue - data=" + param.data, e);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }
}
