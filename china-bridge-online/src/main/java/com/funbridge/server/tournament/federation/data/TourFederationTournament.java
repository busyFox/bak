package com.funbridge.server.tournament.federation.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.DealGenerator;
import com.funbridge.server.tournament.federation.TourFederationMgr;
import com.funbridge.server.tournament.game.Tournament;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.tournament.WSTournament;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.util.*;

/**
 * Created by ldelbarre on 02/08/2017.
 */
public abstract class TourFederationTournament extends Tournament {
    @Id
    private ObjectId ID;
    private String name;
    private int resultType;
    private long startDate = 0;
    private long endDate = 0;

    private List<TourFederationDeal> deals = new ArrayList<>();
    private Date creationDateISO = null;
    private Map<Long, TourFederationRegistration> playerRegistration = new HashMap<>();
    private long registrationStartDate = 0;
    private long registrationEndDate = 0;
    private String info = "";
    private long processPrepareDate = 0;
    private boolean enableToPlay = false;
    private int nbDealsToPlay = this.getTourFederationMgr().getNbDealsToPlay();
    private boolean free = false;
    private boolean endowed = false;
    private boolean special = false;
    private String subName;
    private float coef = 1;
    private String author;
    private boolean freeNotif;
    private boolean endowedNotif;
    private boolean notifDone;
    private float coefPF = 1;

    public abstract TourFederationMgr getTourFederationMgr();

    public String toString() {
        return super.toString()+" - name="+name+" - startDate="+ Constantes.timestamp2StringDateHour(startDate)+" - endDate="+ Constantes.timestamp2StringDateHour(endDate)+" - registrationStartDate="+Constantes.timestamp2StringDateHour(registrationStartDate)+" - registrationEndDate="+Constantes.timestamp2StringDateHour(registrationEndDate)+" - nb player register="+countNbPlayersRegistered()+" - nbDeals="+getNbDeals()+" - nbDealsToPlay="+nbDealsToPlay+" - date process prepare="+Constantes.timestamp2StringDateHour(processPrepareDate)+" - info="+info;
    }

    @Override
    public List<TourFederationDeal> getDeals() {
        return deals;
    }

    @Override
    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }

    public ObjectId getID() {
        return ID;
    }

    public void setID(ObjectId ID) {
        this.ID = ID;
    }

    public int getResultType() {
        return resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    @Override
    public boolean isDateValid(long dateTS) {
        return (startDate < dateTS) && (dateTS < endDate);
    }

    public long getRegistrationStartDate() {
        return registrationStartDate;
    }

    public void setRegistrationStartDate(long registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    public long getRegistrationEndDate() {
        return registrationEndDate;
    }

    public void setRegistrationEndDate(long registrationEndDate) {
        this.registrationEndDate = registrationEndDate;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getProcessPrepareDate() {
        return processPrepareDate;
    }

    public void setProcessPrepareDate(long processPrepareDate) {
        this.processPrepareDate = processPrepareDate;
    }

    public boolean isEnableToPlay() {
        return enableToPlay;
    }

    public void setEnableToPlay(boolean enableToPlay) {
        this.enableToPlay = enableToPlay;
    }

    public int countNbPlayersRegistered() {
        return playerRegistration.size();
    }

    public TourFederationRegistration getPlayerRegistration(long playerID) {
        return playerRegistration.get(playerID);
    }

    public boolean isFreeNotif() {
        return freeNotif;
    }

    public void setFreeNotif(boolean freeNotif) {
        this.freeNotif = freeNotif;
    }

    public boolean isEndowedNotif() {
        return endowedNotif;
    }

    public void setEndowedNotif(boolean endowedNotif) {
        this.endowedNotif = endowedNotif;
    }

    public void addPlayerRegistration(long playerID) throws FBWSException {
        TourFederationRegistration registration = new TourFederationRegistration();
        registration.tsRegisterDate = System.currentTimeMillis();
        registration.playerFederation = this.getTourFederationMgr().getOrCreatePlayerFederation(playerID, this.getTourFederationMgr().getFederationName());
        playerRegistration.put(playerID, registration);
    }

    public void removePlayerRegistration(long playerID) {
        playerRegistration.remove(playerID);
    }

    public Map<Long, TourFederationRegistration> getMapRegistration() {
        return playerRegistration;
    }

    public void setDealGenerate(DealGenerator.DealGenerate[] tabDealGen) {
        deals.clear();
        if (tabDealGen != null) {
            for (DealGenerator.DealGenerate dealGen : tabDealGen) {
                TourFederationDeal deal = new TourFederationDeal();
                deal.index = deals.size()+1;
                deal.dealer = dealGen.dealer;
                deal.vulnerability = dealGen.vulnerability;
                deal.cards = dealGen.distrib;
                deal.paramGenerator = dealGen.paramGenerator;
                deals.add(deal);
            }
        }
    }

    public void addInfo(String text) {
        if (text != null && text.length() > 0) {
            if (info.length() > 0) {
                info += " - ";
            }
            info += text;
        }
    }

    public String getInfo() {
        return info;
    }

    @Override
    public int getNbDealsToPlay() {
        return nbDealsToPlay;
    }

    public void setNbDealsToPlay(int nbDealsToPlay) {
        this.nbDealsToPlay = nbDealsToPlay;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public boolean isEndowed() { return this.endowed; }

    public void setEndowed(boolean endowed) { this.endowed = endowed; }

    public boolean isSpecial() { return special; }

    public void setSpecial(boolean special) { this.special = special; }

    public String getSubName() { return subName; }

    public void setSubName(String subName) { this.subName = subName; }

    public float getCoef() {
        return coef;
    }

    public void setCoef(float coef) {
        this.coef = coef;
    }

    public String getAuthor() { return this.author; }

    public void setAuthor(String author) { this.author = author; }

    public boolean isNotifDone() {
        return notifDone;
    }

    public void setNotifDone(boolean notifDone) {
        this.notifDone = notifDone;
    }

    public float getCoefPF() {
        return coefPF;
    }

    public void setCoefPF(float coefPF) {
        this.coefPF = coefPF;
    }

    /**
     * Return a tournament object for WS. !! Field not set : resultPlayer, currentDealIndex, nbTotalPlayer (for tournament in progress) and arrayDealIDstr is set to null
     *??WS??????? !! ??????resultPlayer?currentDealIndex?nbTotalPlayer????????????arrayDealIDstr???null
     *  @return
     */
    public WSTournament toWS() {
        WSTournament wsTour = new WSTournament();
        wsTour.tourIDstr = getIDStr();
        wsTour.beginDate = startDate;
        wsTour.categoryID = getCategory();
        wsTour.countDeal = nbDealsToPlay;
        wsTour.endDate = endDate;
        wsTour.name = name;
        wsTour.resultType = resultType;
        wsTour.nbTotalPlayer = nbPlayers;
        wsTour.arrayDealIDstr = new String[nbDealsToPlay];
        for (int i=0; i < wsTour.arrayDealIDstr.length; i++) {
            wsTour.arrayDealIDstr[i] = null;
        }
        return wsTour;
    }

    @Override
    public int getNbPlayersOnDeal(String dealID) {
        TourFederationDeal deal = (TourFederationDeal)getDeal(dealID);
        if (deal != null) {
            return deal.getNbPlayers();
        }
        return super.getNbPlayersOnDeal(dealID);
    }
}
