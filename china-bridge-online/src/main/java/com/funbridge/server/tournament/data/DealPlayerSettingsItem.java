package com.funbridge.server.tournament.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="dealPlayerSettingsItem")
public class DealPlayerSettingsItem {
	public int nbS;
	public int nbH;
	public int nbD;
	public int nbC;
	public int nbPoints;
	
	public String toString() {
		return "nbS="+nbS+" nbH="+nbH+" nbD="+nbD+" nbC="+nbC+" nbPoints="+nbPoints;
	}
}
