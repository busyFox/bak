package com.funbridge.server.ws.result;

import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.tournament.serie.data.TourSeriePeriodResult;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSRankingSeriePlayer extends WSMainRankingPlayer {
    public double result = 0;
    public int nbTournamentPlayed = 0;
    public int trend;
    public String serie;

    public WSRankingSeriePlayer(){}

    public String toString() {
        return super.toString()+"serie="+serie+" - nbTournamentPlayed="+nbTournamentPlayed+" - trend="+trend;
    }

    public WSRankingSeriePlayer(PlayerCache pc, long playerAsk, boolean connected){
        super(pc, connected, playerAsk);
        if (pc != null) {
            this.serie = pc.serie;
        }
    }
    public WSRankingSeriePlayer(TourSeriePeriodResult pr, Player p, long playerAsk, boolean connected) {
        if (pr != null) {
            this.rank = pr.getRank();
            this.result = pr.getResult();
            this.nbTournamentPlayed = pr.getNbTournamentPlayed();
            this.trend = pr.getEvolution();
            this.serie = pr.getSerie();
            this.connected = connected;
            if (p != null) {
                this.playerID = p.getID();
                this.playerPseudo = p.getNickname();
                if (playerAsk == p.getID()) {
                    this.avatarPresent = p.isAvatarPresent();
                }
                this.countryCode = p.getDisplayCountryCode();
            }
        }
    }
    public WSRankingSeriePlayer(TourSeriePeriodResult pr, PlayerCache pc, long playerAsk, boolean connected) {
        if (pr != null) {
            this.rank = pr.getRank();
            this.result = pr.getResult();
            this.nbTournamentPlayed = pr.getNbTournamentPlayed();
            this.trend = pr.getEvolution();
            this.connected = connected;
            if (pc != null) {
                this.serie = pc.serie;
                this.playerID = pc.ID;
                this.playerPseudo = pc.getPseudo();
                if (playerAsk == pc.ID) {
                    this.avatarPresent = pc.avatarPresent;
                } else {
                    this.avatarPresent = pc.avatarPublic;
                }
                this.countryCode = pc.countryCode;
            }
        }
    }

	public int getRank() {
		return rank;
	}
	public void setRank(int rank) {
		this.rank = rank;
	}
	public double getResult() {
		return result;
	}
	public void setResult(double result) {
		this.result = result;
	}
	public int getNbTournamentPlayed() {
		return nbTournamentPlayed;
	}
	public void setNbTournamentPlayed(int nbTournamentPlayed) {
		this.nbTournamentPlayed = nbTournamentPlayed;
	}
	public int getTrend() {
		return trend;
	}
	public void setTrend(int trend) {
		this.trend = trend;
	}

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    @Override
    public String getStringValue() {
        return serie;
    }
}
