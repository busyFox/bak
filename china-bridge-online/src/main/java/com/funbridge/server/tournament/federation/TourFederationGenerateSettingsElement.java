package com.funbridge.server.tournament.federation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.common.Constantes;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.annotation.Transient;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by ldelbarre on 02/08/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public abstract class TourFederationGenerateSettingsElement {
    public String name;
    public String startHour; // format = xxhxx
    public String endHour; // format = xxhxx
    public String timezoneId; // see TimeZone.getAvailableIDs() (https://www.mkyong.com/java/java-display-list-of-timezone-with-gmt/)
    public int registrationDurationHour = 1;
    public int resultType = 1;
    public String fromDate; // format = dd/MM/yyyy
    public String toDate; // format = dd/MM/yyyy
    public boolean free = false;
    public boolean endowed = false;
    public boolean special = false;
    public String subName;
    public String firstFrequencyDate; // format = dd/MM/yyyy (first date used for weeks frequency)
    public int weeksFrequency; // nb weeks between tournaments
    public String endDay; // MONDAY, TUESDAY, WEDNESDAY...
    public float coef = 1;
    public float coefPF = 1;

    @Transient
    public Calendar startCal;
    @Transient
    public Calendar endCal;

    public abstract TourFederationMgr getTourFederationMgr();

    public String toString() {
        return "startHour="+startHour+" - endHour="+endHour+" - endDay="+endDay+" - timezoneId="+timezoneId+" - resultType="+resultType;
    }

    protected Calendar getDateFromHourProperty(long tsDate, String hourProperty) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(tsDate);
        try {
            if (StringUtils.isNotBlank(timezoneId)) {
                cal.setTimeZone(TimeZone.getTimeZone(timezoneId));
            }
        } catch (Exception e) {
            this.getTourFederationMgr().getLogger().warn("timezoneId not correct : " + timezoneId);
        }
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        String[] temp = hourProperty.split("h");
        int minute = 0;
        int hour = Integer.parseInt(temp[0]);
        if (temp.length == 2) {
            minute = Integer.parseInt(temp[1]);
        }
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        return cal;
    }

    protected boolean isWeeksFrequencyOK(Calendar cal) {
        if (StringUtils.isBlank(firstFrequencyDate)) {
            return true;
        }

        long tsFirstFrequency;
        try {
            tsFirstFrequency = Constantes.stringDate2Timestamp(firstFrequencyDate);
        } catch (Exception e) {
            this.getTourFederationMgr().getLogger().error("Date is not valid - this="+this.toString(), e);
            return true;
        }

        Calendar calFirstFrequency = Calendar.getInstance();
        calFirstFrequency.setTimeInMillis(tsFirstFrequency);

        if (calFirstFrequency.after(cal)) {
            return true;
        }

        int weeks = 0;
        while (calFirstFrequency.before(cal)) {
            // add another week
            calFirstFrequency.add(Calendar.WEEK_OF_YEAR, 1);
            weeks++;
        }

        return (weeks % weeksFrequency == 0);
    }

    public long getStartDateForDate(long tsDate) {
        if (startHour != null && startHour.length() > 0) {
            try {
                startCal = getDateFromHourProperty(tsDate, startHour);
                if (isWeeksFrequencyOK(startCal)) {
                    return startCal.getTimeInMillis();
                }
            } catch (Exception e) {
                this.getTourFederationMgr().getLogger().error("Failed to build start date - this="+this.toString(), e);
            }
        }
        return 0;
    }

    public long getEndDateForDate(long tsDate) {
        if (startCal == null) {
            getStartDateForDate(tsDate);
        }
        if (endHour != null && endHour.length() > 0) {
            try {
                endCal = getDateFromHourProperty(tsDate, endHour);
                int endDayInt = getIntForDay(endDay);
                if (endDayInt == 0) {
                    // No end day specified. End day is the start day or the day after
                    if (endCal.getTimeInMillis() <= startCal.getTimeInMillis()) {
                        endCal.add(Calendar.DAY_OF_MONTH, 1);
                    }
                } else {
                    // End day is specified
                    endCal.set(Calendar.DAY_OF_WEEK, endDayInt);
                    if (endCal.getTimeInMillis() <= startCal.getTimeInMillis()) {
                        endCal.add(Calendar.WEEK_OF_MONTH, 1);
                    }
                }
                return endCal.getTimeInMillis();
            } catch (Exception e) {
                this.getTourFederationMgr().getLogger().error("Failed to build end date - this="+this.toString(), e);
            }
        }
        return 0;
    }

    private int getIntForDay(String day) {
        if (day != null) {
            switch (day) {
                case "MONDAY" : return Calendar.MONDAY;
                case "TUESDAY" : return Calendar.TUESDAY;
                case "WEDNESDAY" : return Calendar.WEDNESDAY;
                case "THURSDAY" : return Calendar.THURSDAY;
                case "FRIDAY" : return Calendar.FRIDAY;
                case "SATURDAY" : return Calendar.SATURDAY;
                case "SUNDAY" : return Calendar.SUNDAY;
                default: return 0;
            }
        }
        return 0;
    }

    public long getRegistrationStartDate(long tsStartDate) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(tsStartDate);
            cal.add(Calendar.HOUR_OF_DAY, -registrationDurationHour);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            this.getTourFederationMgr().getLogger().error("Failed to build registration start date - this="+this.toString(), e);
        }
        return 0;
    }

    public long getRegistrationEndDate(long tsEndDate, int nbDealsToPlay, int endRegistrationMinutePerDeal) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(tsEndDate);
            cal.add(Calendar.MINUTE, -endRegistrationMinutePerDeal * nbDealsToPlay);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            this.getTourFederationMgr().getLogger().error("Failed to build registration end date - this="+this.toString(), e);
        }
        return tsEndDate;
    }

    public boolean isDateActive(long tsDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(tsDate);
        try {
            if (StringUtils.isNotBlank(timezoneId)) {
                cal.setTimeZone(TimeZone.getTimeZone(timezoneId));
            }
        } catch (Exception e) {
            this.getTourFederationMgr().getLogger().warn("timezoneId not correct : " + timezoneId);
        }
        try {
            boolean fromDateOk = true;
            boolean toDateOk = true;
            if (StringUtils.isNotBlank(fromDate) && tsDate < Constantes.stringDate2Timestamp(fromDate)) {
                fromDateOk = false;
            }
            if (StringUtils.isNotBlank(toDate) && tsDate >= Constantes.stringDate2Timestamp(toDate)) {
                toDateOk = false;
            }
            return fromDateOk && toDateOk;
        } catch (Exception e) {
            this.getTourFederationMgr().getLogger().error("Date is not valid - this="+this.toString(), e);
        }
        return false;
    }
}
