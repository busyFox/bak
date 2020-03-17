package com.funbridge.server.player.cache;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.player.data.PlayerHandicap;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.serie.data.TourSeriePlayer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.io.Serializable;

/**
 * Created by pserent on 08/07/2014.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class PlayerCache implements Serializable{
    public long ID;
    public String pseudo = "UNKNOWN";
    public boolean avatarPresent = false;
    public boolean avatarPublic = false;
    public String countryCode = Constantes.PLAYER_COUNTRY_NOT_VISIBLE;
    public String serie = TourSerieMgr.SERIE_NC;
    public String serieLastPeriodPlayed = null;
    public String lang = Constantes.PLAYER_LANG_EN;
    public double handicap = 0;

    public String toString() {
        return "ID="+ID+" - pseudo="+pseudo+" - lang="+lang+" - avatarPresent="+avatarPresent+" - serie="+serie+" - serieLastPeriodPlayed="+serieLastPeriodPlayed;
    }

    public PlayerCache(){}

    public PlayerCache(long playerID) {
        this.ID = playerID;
    }

    public void setPlayerData(Player player, PlayerHandicap playerHandicap) {
        if (player != null) {
            this.pseudo = player.getNickname();
            this.lang = player.getDisplayLang().toLowerCase(); // displayLang instead of lang
            this.countryCode = player.getDisplayCountryCode();
            this.avatarPresent = player.isAvatarPresent();
        }
        if (playerHandicap != null) {
            this.handicap = playerHandicap.getHandicap();
        } else {
            this.handicap = 0;
        }
    }

    public void setPlayerSerieData(TourSeriePlayer seriePlayer) {
        if (seriePlayer != null) {
            this.serie = seriePlayer.getSerie();
            this.serieLastPeriodPlayed = seriePlayer.getLastPeriodPlayed();
        }
    }

    public String getPseudo() {
        if (isDisabled()) {
            return ContextManager.getTextUIMgr().getTextUIForLang(Constantes.PLAYER_DELETED, lang);
        }
        return pseudo;
    }

    public boolean isDisabled() {
        return pseudo != null && pseudo.startsWith(Constantes.PLAYER_DISABLE_PATTERN);
    }
}
