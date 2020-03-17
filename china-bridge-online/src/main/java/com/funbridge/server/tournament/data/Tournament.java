package com.funbridge.server.tournament.data;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.ws.tournament.WSTournament;

@Entity
@Table(name="tournament")
@NamedQueries({
	@NamedQuery(name="tournament.listByNumberDesc", query="select tour from Tournament tour order by tour.number desc"),
	@NamedQuery(name="tournament.listAfterDateOrderAsc", query="select tour from Tournament tour where tour.endDate > :date and tour.valid=1 order by tour.beginDate asc"),
	@NamedQuery(name="tournament.listFinishedForCategoryOrderDesc", query="select tour from Tournament tour where tour.category.ID=:catID and tour.finished=1 and tour.valid=1 order by tour.beginDate desc"),
	@NamedQuery(name="tournament.listFinishedForCategoryBeforeDateOrderAsc", query="select tour from Tournament tour where tour.category.ID=:catID and tour.finished=1 and tour.endDate <= :date order by tour.beginDate asc"),
	@NamedQuery(name="tournament.countFinishedForCategoryBeforeDateOrderAsc", query="select count(tour) from Tournament tour where tour.category.ID=:catID and tour.finished=1 and tour.endDate <= :date order by tour.beginDate asc"),
	@NamedQuery(name="tournament.listFinishedBeforeDateOrderAsc", query="select tour from Tournament tour where tour.finished=1 and tour.endDate <= :date order by tour.beginDate asc"),
	@NamedQuery(name="tournament.countFinishedForCategory", query="select count(tour) from Tournament tour where tour.category.ID=:catID and tour.finished=1 and tour.valid=1"),
	@NamedQuery(name="tournament.listFutureForCategoryOrderDesc", query="select tour from Tournament tour where tour.category.ID=:catID and tour.beginDate > :date and tour.valid=1 order by tour.beginDate desc"),
	@NamedQuery(name="tournament.listFutureForCategoryOrderAsc", query="select tour from Tournament tour where tour.category.ID=:catID and tour.beginDate > :date and tour.valid=1 order by tour.beginDate asc"),
	@NamedQuery(name="tournament.listForCategoryInProgressOrderDesc", query="select tour from Tournament tour where tour.category.ID=:catID and tour.beginDate < :curDate and :curDate < tour.endDate and tour.valid=1 order by tour.beginDate desc"),
	@NamedQuery(name="tournament.listForCategoryInProgressOrderAsc", query="select tour from Tournament tour where tour.category.ID=:catID and tour.beginDate < :curDate and :curDate < tour.endDate and tour.valid=1 order by tour.beginDate asc"),
	@NamedQuery(name="tournament.listForCategoryInProgressOrderRandom", query="select tour from Tournament tour where tour.category.ID=:catID and tour.beginDate < :curDate and :curDate < tour.endDate and tour.valid=1 order by rand()"),
	@NamedQuery(name="tournament.countForCategoryInProgress", query="select count(tour) from Tournament tour where tour.category.ID=:catID and tour.beginDate < :curDate and :curDate < tour.endDate and tour.valid=1"),
	@NamedQuery(name="tournament.listNotFinished", query="select tour from Tournament tour where tour.finished=0 and tour.valid=1"),
	@NamedQuery(name="tournament.listForCategoryNotFinished", query="select tour from Tournament tour where tour.category.ID=:catID and tour.finished=0 and tour.valid=1"),
	@NamedQuery(name="tournament.listForCategoryNotFinishedAfterDate", query="select tour from Tournament tour where tour.category.ID=:catID and tour.finished=0 and tour.endDate < :date and tour.valid=1"),
	@NamedQuery(name="tournament.listForCategoryNotFinishedBetweenDate", query="select tour from Tournament tour where tour.category.ID=:catID and tour.beginDate < :curDate and :curDate < tour.endDate and tour.finished=0 and tour.valid=1"),
	@NamedQuery(name="tournament.listFinishedForCategorySerieAndPeriod", query="select tour from Tournament tour where tour.category.ID=:catID and tour.beginDate = :beginDate and tour.endDate = :endDate and tour.serie= :serie and tour.finished=1 and tour.valid=1"),
	@NamedQuery(name="tournament.countForCategorySerieAndPeriod", query="select count(tour) from Tournament tour where tour.category.ID = :catID and tour.beginDate = :beginDate and tour.endDate = :endDate and tour.serie= :serie and tour.valid=1"),
	@NamedQuery(name="tournament.deleteTour", query="delete from Tournament tour where tour.ID = :tourID")
})
public class Tournament {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id", nullable=false)
	private long ID;
	
	@Column(name="name", nullable=false, length=50)
	private String name = "";
	
	@Column(name="creation_date", nullable=false)
	private long creationDate;
	
	@Column(name="begin_date", nullable=false)
	private long beginDate;
	
	@Column(name="end_date", nullable=false)
	private long endDate;
	
	@Column(name="last_start_date", nullable=false)
	private long lastStartDate;
	
	@Column(name="deal_count", nullable=false)
	private int countDeal;
	
	@ManyToOne
	@JoinColumn(name="category")
	private TournamentCategory category;
	
	@Column(name="result_type", nullable=false)
	private int resultType = Constantes.TOURNAMENT_RESULT_UNKNOWN;
	
	@Column(name="number")
	private int number;
	
	@Column(name="valid", nullable=false)
	private boolean valid = false;
	
	@Column(name="finished", nullable=false)
	private boolean finished = false;
	
	@Column(name="engine_version", nullable=false)
	private int engineVersion = 0;
	
	@Column(name="nb_player")
	private int nbPlayer = 0;
	
	@Column(name="nb_max_player")
	private int nbMaxPlayer = 0;
	
	@Column(name="serie")
	private String serie = Constantes.SERIE_NOT_DEFINED;
	
	@Column(name="nb_credit_play_deal")
	private int nbCreditPlayDeal = 1;
	
	@Column(name="settings")
	private String settings = null;
	
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

	public long getCreationDate() {
		return creationDate;
	}
	
	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}
	
	public long getBeginDate() {
		return beginDate;
	}
	
	public void setBeginDate(long beginDate) {
		this.beginDate = beginDate;
	}
	
	public long getEndDate() {
		return endDate;
	}
	
	public void setEndDate(long endDate) {
		this.endDate = endDate;
	}
	
	public long getLastStartDate() {
		return lastStartDate;
	}

	public void setLastStartDate(long lastStartDate) {
		this.lastStartDate = lastStartDate;
	}

	public int getCountDeal() {
		return countDeal;
	}
	
	public void setCountDeal(int countDeal) {
		this.countDeal = countDeal;
	}
	
	public String toString() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
		return "[ID="+ID+" - number="+number+" - name="+name+" - serie="+serie+" - finished="+finished+" - (category:"+category.toString()+") - beginDate="+sdf.format(new Date(getBeginDate()))+" - endDate="+sdf.format(new Date(getEndDate()))+"]";
	}
	
	public TournamentCategory getCategory() {
		return category;
	}
	
	public void setCategory(TournamentCategory cat) {
		category = cat;
	}

	/**
	 * Return a tournament object for WS. !! Field not set : arrayDealID, resultPlayer, currentDealIndex, nbTotalPlayer (for tournament in progress)
	 * @return
	 */
	public WSTournament toWS() {
		WSTournament wsTour = new WSTournament();
		wsTour.beginDate = beginDate;
		wsTour.categoryID = category.getID();
		wsTour.countDeal = countDeal;
		wsTour.endDate = endDate;
		wsTour.setTourID(ID);
		wsTour.name = name;
		wsTour.resultType = resultType;
		wsTour.number = number;
		wsTour.nbTotalPlayer = nbPlayer;
        wsTour.engineVersion = engineVersion;
		if (this.settings != null && this.settings.length() > 0) {
			wsTour.settings = ContextManager.getTournamentMgr().getTournamentSettings(this.settings);
		}
		return wsTour;
	}

	public boolean isDateValid(long dateTS) {
        return (beginDate < dateTS) && (dateTS < endDate);
    }

	public int getResultType() {
		return resultType;
	}

	public void setResultType(int resultType) {
		this.resultType = resultType;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public int getEngineVersion() {
		return engineVersion;
	}

	public void setEngineVersion(int engineVersion) {
		this.engineVersion = engineVersion;
	}

	public int getNbPlayer() {
		return nbPlayer;
	}

	public void setNbPlayer(int nbPlayer) {
		this.nbPlayer = nbPlayer;
	}

	public String getSerie() {
		return serie;
	}

	public void setSerie(String serie) {
		this.serie = serie;
	}

	public int getNbMaxPlayer() {
		return nbMaxPlayer;
	}

	public void setNbMaxPlayer(int nbMaxPlayer) {
		this.nbMaxPlayer = nbMaxPlayer;
	}

	public int getNbCreditPlayDeal() {
		return nbCreditPlayDeal;
	}

	public void setNbCreditPlayDeal(int nbCreditPlayDeal) {
		this.nbCreditPlayDeal = nbCreditPlayDeal;
	}

	public String getSettings() {
		return settings;
	}

	public void setSettings(String val) {
		this.settings = val;
	}
}
