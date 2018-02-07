package nl.trivento.fastdata.travelclear.gtfs;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class GtfsTime {
    public static int now() {
        Calendar calendar = new GregorianCalendar();
        return of(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }

    public static int of(int h, int m, int s) {
        return h * 3600 + m * 60 + s;
    }

    public static String format(int time) {
        return String.format("%02d:%02d:%02d",
                time / 3600,
                (time / 60) % 60,
                time % 60);
    }
}
