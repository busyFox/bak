package com.funbridge.server.common;

import java.util.Calendar;

public class DateUtils {

    /**
     * Update Hours / Minutes / secondes to ZERO
     * ???/??/?????
     * @param cal
     */
    public static void initAtStartOfDay(Calendar cal){
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
    }

    /**
     * Init calendar and add nb of days
     * ??????????
     * @param nbDays
     * @return
     */
    public static Calendar initCalendarWithAdditionalDays(int nbDays){
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, nbDays);
        return cal;
    }
}
