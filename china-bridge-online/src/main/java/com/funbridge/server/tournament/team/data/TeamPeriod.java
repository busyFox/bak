package com.funbridge.server.tournament.team.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.team.TourTeamMgr;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by pserent on 08/11/2016.
 */
@Document(collection="team_period")
public class TeamPeriod {
    @Id
    private String ID;
    private long dateStart = 0;
    private long dateEnd = 0;
    private boolean finished = false;
    private List<TeamTour> tours = new ArrayList<>();

    @Transient
    private TeamTour currentTour = null;

    public TeamPeriod() {}

    public TeamPeriod(String periodID, boolean devMode) {
        this.ID = periodID;
        dateStart = TourTeamMgr.transformPeriodID2TS(ID, true, devMode);
        dateEnd = TourTeamMgr.transformPeriodID2TS(ID, false, devMode);
    }

    public String getID() {
        return ID;
    }

    public String toString() {
        return "ID="+ID+" - dateStart="+ Constantes.timestamp2StringDateHour(dateStart)+" - dateEnd="+Constantes.timestamp2StringDateHour(dateEnd)+" - nb tour="+(tours!=null?tours.size():"null")+" - currentTour={"+currentTour+" - nbToursPlayed="+ getNbPlayedTours()+"}";
    }

    public List<TeamTour> getTours() {
        return tours;
    }

    public int getNbTours() {
        return tours.size();
    }

    public void createTours(int nbTours, int durationTourNbHours, boolean devMode) throws Exception{
        if (!tours.isEmpty()) {
            throw new Exception("List tours not empty !");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateStart);
        if (devMode) {
            calendar.set(Calendar.HOUR_OF_DAY, 9);
        }
        while (tours.size() < nbTours) {
            TeamTour tpt = new TeamTour();
            tpt.index = tours.size()+1;
            if (calendar.getTimeInMillis() < dateEnd) {
                tpt.dateStart = calendar.getTimeInMillis();
                calendar.add(Calendar.HOUR_OF_DAY, durationTourNbHours);
                tpt.dateEnd = calendar.getTimeInMillis() - 1000;
                if (tpt.dateEnd > dateEnd) {
                    throw new Exception("Tour index="+tpt.index+" - Date end not valid !!  - period="+this);
                }
            } else {
                throw new Exception("Tour index="+tpt.index+" - Date start not valid !!  - period="+this);
            }
            tours.add(tpt);
        }
    }

    public TeamTour getTour(int index) {
        if (tours != null) {
            for (TeamTour e : tours) {
                if (e.index == index) {
                    return e;
                }
            }
        }
        return null;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isPeriodValidForTS(long ts) {
        if (ContextManager.getTourTeamMgr().getConfigIntValue("checkPeriodTrue", 1) != 1) {
            return (ts > dateStart) && (ts < dateEnd);
        }
        return true;
    }

    public long getDateStart() {
        return dateStart;
    }

    public long getDateEnd() {
        return dateEnd;
    }

    public boolean isAllTourFinished() {
        for (TeamTour e : tours) {
            if (!e.finished) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return the number of tours finished including the tour in progress
     * @return
     */
    public int getNbPlayedTours() {
        int result = 0;
        for (TeamTour e : tours) {
            result++;
            if (!e.finished) {
                break;
            }
        }
        return result;
    }

    /**
     * Return tour in progress or not yet finished
     * @return
     */
    public TeamTour getCurrentTour() {
        return currentTour;
    }

    public int getCurrentTourIndex() {
        if (currentTour != null) {
            return currentTour.index;
        }
        return -1;
    }

    public String getPeriodTour() {
        if (ID != null && currentTour != null) {
            return TourTeamMgr.buildPeriodTour(getID(), currentTour.index);
        }
        return null;
    }

    /**
     * Change current tour : if current tour is null or finished, loop on tours to find the next tour not yet finished
     * @return the current tour, or the next one not yet finished or null (all tour finished)
     */
    public TeamTour updateCurrentTour() {
        if (currentTour == null || currentTour.finished) {
            currentTour = null;
            for (TeamTour e : tours) {
                if (!e.finished) {
                    currentTour = e;
                    break;
                }
            }
        }
        return currentTour;
    }

    /**
     * Get date for the next tour.
     * @return
     */
    public long getDateNextTour() {
        if (currentTour != null) {
            if (currentTour.dateStart > System.currentTimeMillis()) {
                return currentTour.dateStart;
            }
            TeamTour nextTour = getTour(currentTour.index+1);
            if (nextTour != null) {
                return nextTour.dateStart;
            }
            return 0;
        }
        return 0;
    }
}
