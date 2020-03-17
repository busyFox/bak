package com.funbridge.server.player.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


/**
 * Created by ldelbarre on 02/02/2018.
 * Object to define a period for handicap. periodID is build as a string using the current date.
 */
@Document(collection="handicap_period")
public class HandicapPeriod {
    @Id
    private ObjectId ID;
    @Indexed(unique = true)
    private String periodID;
    private boolean finished = false;

    public HandicapPeriod(String periodID) {
        this.periodID = periodID;
    }

    public String toString() {
        return "periodID="+ periodID + " - finished="+finished;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HandicapPeriod that = (HandicapPeriod) o;

        return periodID != null ? periodID.equals(that.periodID) : that.periodID == null;
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

}