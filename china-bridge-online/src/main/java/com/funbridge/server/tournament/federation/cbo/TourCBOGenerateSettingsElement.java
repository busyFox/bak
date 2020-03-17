package com.funbridge.server.tournament.federation.cbo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.federation.TourFederationGenerateSettingsElement;
import com.funbridge.server.tournament.federation.TourFederationMgr;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by ldelbarre on 21/12/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourCBOGenerateSettingsElement extends TourFederationGenerateSettingsElement {
    public String frequencyMinute;
    public String durationMinute;

    @Override
    public TourFederationMgr getTourFederationMgr() {
        return ContextManager.getTourCBOMgr();
    }

    public List<TourFederationGenerateSettingsElement> getElements(long tsDate) {
        List<TourFederationGenerateSettingsElement> elements = new ArrayList<>();
        try {
            Calendar startCalCurrent = getDateFromHourProperty(tsDate, startHour);
            Calendar endCalLast = getDateFromHourProperty(tsDate, endHour);
            if (endCalLast.getTimeInMillis() <= startCalCurrent.getTimeInMillis()) {
                endCalLast.add(Calendar.DAY_OF_MONTH, 1);
            }

            while (startCalCurrent.before(endCalLast)) {
                Calendar endCalCurrent = (Calendar)startCalCurrent.clone();
                endCalCurrent.add(Calendar.MINUTE, Integer.parseInt(durationMinute));
                TourCBOGenerateSettingsElement element = new TourCBOGenerateSettingsElement();
                element.name = this.name;
                element.startCal = (Calendar)startCalCurrent.clone();
                element.endCal = (Calendar)endCalCurrent.clone();
                element.resultType = this.resultType;
                element.registrationDurationHour = this.registrationDurationHour;
                element.fromDate = this.fromDate;
                element.toDate = this.toDate;
                element.free = this.free;

                elements.add(element);

                startCalCurrent.add(Calendar.MINUTE, Integer.parseInt(frequencyMinute));
            }
        } catch (Exception e) {
            this.getTourFederationMgr().getLogger().error("Failed to build start date - this="+this.toString(), e);
        }

        return elements;
    }

    @Override
    public long getStartDateForDate(long tsDate) {
        if (startCal != null) {
            return startCal.getTimeInMillis();
        }
        return super.getStartDateForDate(tsDate);
    }

    @Override
    public long getEndDateForDate(long tsDate) {
        if (endCal != null) {
            return endCal.getTimeInMillis();
        }
        return super.getEndDateForDate(tsDate);
    }
}
