package test.gotogames.common.tools;

import junit.framework.TestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by pserent on 25/01/2017.
 */
public class CalendarTools extends TestCase {

    private static SimpleDateFormat sdfDateHour = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
    private static SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");
    public static String timestamp2StringDateHour(long ts) {
        return sdfDateHour.format(new Date(ts));
    }
    public static String timestamp2StringDate(long ts) {
        return sdfDate.format(new Date(ts));
    }
    public static long stringDateHour2Timestamp(String val) throws ParseException {
        return sdfDateHour.parse(val).getTime();
    }
    public static long stringDate2Timestamp(String val) throws ParseException {
        return sdfDate.parse(val).getTime();
    }
    public void testCalendarSet() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        System.out.println("Current date="+timestamp2StringDateHour(calendar.getTimeInMillis()));
        System.out.println("dayOfWeek="+dayOfWeek+" - hour="+hour+" - minute="+minute);

        Calendar nextDate = Calendar.getInstance();
        nextDate.set(Calendar.HOUR_OF_DAY, hour-1);
        nextDate.set(Calendar.MINUTE, minute);
        nextDate.set(Calendar.MILLISECOND, 0);
        nextDate.set(Calendar.SECOND, 0);

        System.out.println("Next date="+timestamp2StringDateHour(nextDate.getTimeInMillis()));
        while (nextDate.getTimeInMillis() < System.currentTimeMillis() || nextDate.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            nextDate.add(Calendar.DAY_OF_YEAR, 1);
        }
        System.out.println("Next date="+timestamp2StringDateHour(nextDate.getTimeInMillis()));
    }

    public void testDateTimeFormatter() {
        DateTimeFormatter dtfAppleDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss VV");
        String date1 = "2018-07-05 02:55:42 America/Los_Angeles";
        ZonedDateTime dateTime1 = ZonedDateTime.parse(date1, dtfAppleDate);
        long ts1 = dateTime1.toEpochSecond();

        String date2 = "2018-07-05 09:55:42 Etc/GMT";
        ZonedDateTime dateTime2 = ZonedDateTime.parse(date2, dtfAppleDate);
        long ts2 = dateTime2.toEpochSecond();
    }
}
