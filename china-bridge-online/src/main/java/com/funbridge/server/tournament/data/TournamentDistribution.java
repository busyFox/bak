package com.funbridge.server.tournament.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name="tournament_distribution")
@NamedQueries({
	@NamedQuery(name="tournamentDistribution.deleteForTournament", query="delete from TournamentDistribution distrib where  distrib.ID in (select deal.id from TournamentDeal deal where deal.tournament.ID=:tourID)"),
	@NamedQuery(name="tournamentDistribution.selectForTournament", query="select td from TournamentDistribution td where td.ID in (select deal.distribution.ID from TournamentDeal deal where deal.tournament.ID=:tourID)"),
	@NamedQuery(name="tournamentDistribution.selectForCards", query="select td from TournamentDistribution td where td.cards=:cards")
})
public class TournamentDistribution {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id", nullable=false)
	private long ID;
	
	@Column(name="dealer", length=1, nullable=false)
	private char dealer;
	
	@Column(name="vul", length=1, nullable=false)
	private char vulnerability;
	
	@Column(name="cards", length=52, nullable=false)
	private String cards;

	@Column(name="param_generator")
	private String paramGenerator = null;
	
	public long getID() {
		return ID;
	}

	public void setID(long iD) {
		ID = iD;
	}

	public char getDealer() {
		return dealer;
	}
	
	public String getDealerStr() {
		return Character.toString(dealer);
	}

	public void setDealer(char dealer) {
		this.dealer = dealer;
	}

	public char getVulnerability() {
		return vulnerability;
	}
	
	public String getVulnerabilityStr() {
		return Character.toString(vulnerability);
	}

	public void setVulnerability(char vulnerability) {
		this.vulnerability = vulnerability;
	}

	public String getCards() {
		return cards;
	}

	public void setCards(String cards) {
		this.cards = cards;
	}
	
	public String getParamGenerator() {
		return paramGenerator;
	}

	public void setParamGenerator(String paramGenerator) {
		this.paramGenerator = paramGenerator;
	}

	public String getString() {
		return Character.toString(dealer)+ vulnerability +cards;
	}
	
	public String toString() {
		return "[id="+ID+" - dealer="+dealer+" - vulnerability="+vulnerability+" - cards="+cards+"]";
	}
}
