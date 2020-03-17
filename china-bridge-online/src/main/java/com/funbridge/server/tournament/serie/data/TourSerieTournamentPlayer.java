package com.funbridge.server.tournament.serie.data;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.tournament.game.TournamentPlayer;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 08/07/2014.
 * Data for player on tournament
 */
@Document(collection="serie_tournament_player")
public class TourSerieTournamentPlayer extends TournamentPlayer {
    @Id
    private ObjectId ID;

    @DBRef
    private TourSerieTournament tournament;

    private String periodID;

    public TourSerieTournament getTournament() {
        return tournament;
    }

    public void setTournament(TourSerieTournament tournament) {
        this.tournament = tournament;
    }

    @Override
    public WSResultTournamentPlayer toWSResultTournamentPlayer(Player p, long playerAsk) {
        WSResultTournamentPlayer resPla = new WSResultTournamentPlayer();
        resPla.setDateLastPlay(lastDate);
        resPla.setNbDealPlayed(tournament.getNbDeals());
        if (p != null) {
            resPla.setPlayerID(p.getID());
            resPla.setPlayerPseudo(p.getNickname());
            resPla.setConnected(ContextManager.getPresenceMgr().isSessionForPlayerID(p.getID()));
            if (p.getID() == playerAsk) {
                resPla.setAvatarPresent(p.isAvatarPresent());
            }
            resPla.setPlayerSerie(tournament.getSerie());
            resPla.setCountryCode(p.getDisplayCountryCode());
        }
        resPla.setRank(rank);
        resPla.setResult(result);
        resPla.setNbTotalPlayer(getTournament().getNbPlayers());
        return resPla;
    }

    @Override
    public WSResultTournamentPlayer toWSResultTournamentPlayer(PlayerCache pc, long playerAsk) {
        WSResultTournamentPlayer resPla = new WSResultTournamentPlayer();
        resPla.setDateLastPlay(lastDate);
        resPla.setNbDealPlayed(tournament.getNbDeals());
        if (pc != null) {
            resPla.setPlayerID(pc.ID);
            resPla.setPlayerPseudo(pc.getPseudo());
            resPla.setConnected(ContextManager.getPresenceMgr().isSessionForPlayerID(pc.ID));
            if (pc.ID == playerAsk) {
                resPla.setAvatarPresent(pc.avatarPresent);
            } else {
                resPla.setAvatarPresent(pc.avatarPublic);
            }
            resPla.setPlayerSerie(pc.serie);
            resPla.setCountryCode(pc.countryCode);
        }
        resPla.setRank(rank);
        resPla.setResult(result);
        resPla.setNbTotalPlayer(getTournament().getNbPlayers());
        return resPla;
    }

    public String getPeriodID() {
        return periodID;
    }

    public void setPeriodID(String periodID) {
        this.periodID = periodID;
    }

}
