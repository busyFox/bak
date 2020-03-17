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
@Table(name="tournament_category")
@NamedQueries({
@NamedQuery(name="tournamentCategory.listCategory", query="select tc from TournamentCategory tc")
})
public class TournamentCategory {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id", nullable=false)
	private long ID;
	
	@Column(name="name", length=100, nullable=false)
	private String name;
	
	@Column(name="type", length=50, nullable=false)
	private String type;
	
	@Column(name="description", length=200, nullable=true)
	private String description;
	
	@Column(name="stat_result_type", nullable=false)
	private int statResultType = 0;
	
	public long getID() {
		return ID;
	}
	public void setID(long iD) {
		ID = iD;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String toString() {
		return name;
	}
}
