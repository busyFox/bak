package com.funbridge.server.ws.game;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.data.Player;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class WSGamePlayer {
    public int type;
    public String pseudo;
    public long playerID;
    public boolean avatar = false;
    public int playerStatus = 0;
    public boolean connected = false;
    public String lang = "";
    public String countryCode = "";

    public static WSGamePlayer createGamePlayerHuman(long plaID, String pseudo, boolean avatar, String lang, String countryCode) {
        WSGamePlayer gp = new WSGamePlayer();
        gp.playerID = plaID;
        gp.pseudo = pseudo;
        gp.type = Constantes.PLAYER_TYPE_HUMAN;
        gp.avatar = avatar;
        gp.lang = lang;
        gp.countryCode = countryCode;
        return gp;
    }

    public static WSGamePlayer createGamePlayerHuman(Player p, long playerAsk) {
        WSGamePlayer gp = new WSGamePlayer();
        if (p != null) {
            gp.playerID = p.getID();
            gp.pseudo = p.getNickname();
            gp.type = Constantes.PLAYER_TYPE_HUMAN;
            gp.lang = p.getDisplayLang();
            gp.countryCode = p.getDisplayCountryCode();
            if (p.getID() == playerAsk) {
                gp.avatar = p.isAvatarPresent();
            }
        }
        return gp;
    }

    public static WSGamePlayer createGamePlayerHuman(PlayerCache pc, long playerAsk) {
        WSGamePlayer gp = new WSGamePlayer();
        if (pc != null) {
            gp.playerID = pc.ID;
            gp.pseudo = pc.getPseudo();
            gp.type = Constantes.PLAYER_TYPE_HUMAN;
            gp.lang = pc.lang;
            gp.countryCode = pc.countryCode;
            if (pc.ID == playerAsk) {
                gp.avatar = pc.avatarPresent;
            } else {
                gp.avatar = pc.avatarPublic;
            }
        }
        return gp;
    }

    public static WSGamePlayer createGamePlayerHuman(PlayerCache pc, int playerStatus, long playerAsk) {
        WSGamePlayer gp = new WSGamePlayer();
        if (pc != null) {
            gp.playerID = pc.ID;
            gp.pseudo = pc.getPseudo();
            gp.type = Constantes.PLAYER_TYPE_HUMAN;
            gp.lang = pc.lang;
            gp.countryCode = pc.countryCode;
            gp.playerStatus = playerStatus;
            if (pc.ID == playerAsk) {
                gp.avatar = pc.avatarPresent;
            } else {
                gp.avatar = pc.avatarPublic;
            }
        }
        return gp;
    }

    public static WSGamePlayer createGamePlayerHuman(Player p, int playerStatus, long playerAsk) {
        WSGamePlayer gp = new WSGamePlayer();
        if (p != null) {
            gp.playerID = p.getID();
            gp.pseudo = p.getNickname();
            gp.type = Constantes.PLAYER_TYPE_HUMAN;
            gp.playerStatus = playerStatus;
            gp.lang = p.getDisplayLang();
            gp.countryCode = p.getDisplayCountryCode();
            if (p.getID() == playerAsk) {
                gp.avatar = p.isAvatarPresent();
            }
        }
        return gp;
    }

    public static WSGamePlayer createGamePlayerRobot() {
        WSGamePlayer gp = new WSGamePlayer();
        gp.playerID = Constantes.PLAYER_ID_ROBOT;
        gp.pseudo = Constantes.PLAYER_ROBOT_PSEUDO;
        gp.type = Constantes.PLAYER_TYPE_ROBOT;
        gp.playerStatus = Constantes.TABLE_PLAYER_STATUS_PRESENT;
        return gp;
    }

    public String toString() {
        return "playerID=" + playerID + " - pseudo=" + pseudo + " - playerStatus=" + playerStatus + " - lang=" + lang;
    }
}
