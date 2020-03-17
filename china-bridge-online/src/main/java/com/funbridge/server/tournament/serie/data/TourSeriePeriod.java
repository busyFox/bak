package com.funbridge.server.tournament.serie.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 26/05/2014.
 * Object to define a period for serie. periodID is build as a string using the start date and end date.
 */
@Document(collection="serie_period")
public class TourSeriePeriod {
    @Id
    private ObjectId ID;
    @Indexed(unique = true)
    private String periodID;
    private long tsDateStart = 0;
    private long tsDateEnd = 0;
    private long tsDateCreation = 0;
    private boolean finished = false;

    public TourSeriePeriod(String periodID) {
        this.periodID = periodID;
        this.tsDateCreation = System.currentTimeMillis();
        computeTSDate();
    }

    public String toString() {
        return "periodID="+ periodID+" - dateStart="+ Constantes.timestamp2StringDateHour(tsDateStart)+" - dateEnd="+Constantes.timestamp2StringDateHour(tsDateEnd)+" - finished="+finished;
    }

    public void initData() {
        ContextManager.getTourSerieMgr().getLogger().debug("Construct period="+toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TourSeriePeriod that = (TourSeriePeriod) o;

        return periodID != null ? periodID.equals(that.periodID) : that.periodID == null;
    }

    /**
     * compute the timestamp value for start and end date of period
     */
    private void computeTSDate() {
        tsDateStart = TourSerieMgr.transformPeriodID2TS(periodID, true);
        tsDateEnd = TourSerieMgr.transformPeriodID2TS(periodID, false);
    }

    public String getPeriodID() {
        return periodID;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isPeriodValidForTS(long ts) {
        if (!ContextManager.getTourSerieMgr().getConfigBooleanValue("checkPeriodTrue")) {
            return (ts > tsDateStart) && (ts < tsDateEnd);
        }
        return true;
    }

    public long getTsDateStart() {
        return tsDateStart;
    }

    public long getTsDateEnd() {
        return tsDateEnd;
    }
}