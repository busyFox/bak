package com.funbridge.server.ws.support;

import javax.xml.bind.annotation.XmlRootElement;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.ws.team.WSTeam;

@XmlRootElement(name="player")
public class WSSupPlayer {
	public long plaID;
	public String pseudo;
	public String mail;
	public String firstName;
	public String lastName;
	public String password;
	public String presentation;
	public String town;
	public String lang;
    public String displayLang;
	public String country;
    public String displayCountry;
	public String serie;
    public int serieBestRank = -1;
    public String serieBestSerie;
    public String serieBestPeriod;
	public boolean avatar;
	public int credit;
	public int creditBonusFlag;
	public int nbDealPlayed;
	public String dateBirthday;
	public String dateCreation;
	public String dateLastConnection;
	public String dateLastCreditUpdate;
	public boolean mailCertif;
	public String dateSubscriptionExpiration;
	public String avatarDateUpload;
	public String avatarDateValidation;
	public boolean buyCredit = false;
	public int sex = Constantes.PLAYER_SEX_NOT_DEFINED;
	public double handicap;
    public int conventionProfil;
    public String conventionValue;
    public int cardsConventionProfil;
    public String cardsConventionValue;
	public WSTeam team;
	public WSSupPlayerLocation location;
	
	public WSSupPlayer() {}

}
