package com.funbridge.server.tournament.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="dealPlayerSettings")
public class DealPlayerSettings {
	public DealPlayerSettingsItem mins;
	public DealPlayerSettingsItem maxs;
	
	public String toString() {
		return "mins=("+mins+") - maxs=("+maxs+")";
	}
	
	public boolean isValid() {
		if (mins != null && maxs != null) {
            return mins.nbC >= 0 && mins.nbC <= 13 && maxs.nbC >= 0 && maxs.nbC <= 13 && maxs.nbC >= mins.nbC &&
                    mins.nbD >= 0 && mins.nbD <= 13 && maxs.nbD >= 0 && maxs.nbD <= 13 && maxs.nbD >= mins.nbD &&
                    mins.nbH >= 0 && mins.nbH <= 13 && maxs.nbH >= 0 && maxs.nbH <= 13 && maxs.nbH >= mins.nbH &&
                    mins.nbS >= 0 && mins.nbS <= 13 && maxs.nbS >= 0 && maxs.nbS <= 13 && maxs.nbS >= mins.nbS &&
                    mins.nbPoints >= 0 && mins.nbPoints <= 40 && maxs.nbPoints >= 0 && maxs.nbPoints <= 40 && mins.nbPoints <= maxs.nbPoints;
		}
		return false;
	}
}
