package com.funbridge.server.tournament.data;

import com.funbridge.server.common.Constantes;

public class TournamentSettings {
	public int convention;
	public String conventionValue = "";
    public int cardsConvention;
    public String cardsConventionValue = "";
	public String mode;
	public String theme;
	public DealSettings advancedSettings;
	
	public String toString() {
		return "convention="+convention+" - conventionValue="+conventionValue+" - cardsConvention="+cardsConvention+" - cardsConventionValue="+cardsConventionValue+" - mode="+mode+" - theme="+theme+" - advancedSettings="+advancedSettings;
	}
	
	public boolean isValid() {
		if (mode != null) {
			if (mode.equals(Constantes.TOURNAMENT_SETTINGS_MODE_ENCHERIE_A2) ||
				mode.equals(Constantes.TOURNAMENT_SETTINGS_MODE_POINTS_FOR_NS) ||
				mode.equals(Constantes.TOURNAMENT_SETTINGS_MODE_RANDOM)) {
				return true;
			}
			if (mode.equals(Constantes.TOURNAMENT_SETTINGS_MODE_ADVANCED)) {
				return (advancedSettings != null && advancedSettings.isValid());
			}
            return mode.equals(Constantes.TOURNAMENT_SETTINGS_MODE_THEME) && theme != null && theme.length() > 0;
		}
		return false;
	}
}
