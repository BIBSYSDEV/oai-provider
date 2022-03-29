package no.sikt.oai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.concurrent.TimeUnit;

public class TimeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TimeUtils.class);
//    public static String AUTHORITY_TIMESTAMP = "yyyyMMddHHmmss";
//    public static String FORMAT_ISO8601 = "yyyyMMddHHmmss'.0'";
    public static String FORMAT_ZULU_LONG = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static String FORMAT_ZULU_SHORT = "yyyy-MM-dd";
//    public static String FORMAT_BIBSYS_DATO = "yyyy-MM-dd";
//    public static String FORMAT_MARC21_008_DATO = "yyyyMMdd";
//    public static String FORMAT_MARC21_008_DATO_SHORT = "yyMMdd";

//    public static Date BibsysDato2Date(String bibsysdato) {
//        SimpleDateFormat sf = new SimpleDateFormat(FORMAT_BIBSYS_DATO);
//        sf.setLenient(false);
//        Date myDate = null;
//
//        try {
//            myDate = sf.parse(bibsysdato);
//            return myDate;
//        } catch (ParseException var4) {
//            LOG.debug("", var4);
//            return myDate;
//        }
//    }

//    public static Date String2Date(String sDato, String format) {
//        SimpleDateFormat sf = new SimpleDateFormat(format);
//        sf.setLenient(false);
//        Date myDate = null;
//
//        try {
//            myDate = sf.parse(sDato);
//            return myDate;
//        } catch (ParseException var5) {
//            LOG.debug("", var5);
//            return myDate;
//        }
//    }

    public static String Date2String(Date date, String format) {
        if (date != null) {
            SimpleDateFormat sf = new SimpleDateFormat(format);
            return sf.format(date);
        } else {
            return "";
        }
    }

    public static String utcDateTime() {
        Date myDate = new Date();
        SimpleDateFormat sf = new SimpleDateFormat(FORMAT_ZULU_LONG);
        return sf.format(myDate);
    }

    public static String getResponseTime() {
        return utcDateTime();
    }

//    public static boolean verifyDate(String s) {
//        SimpleDateFormat sf = new SimpleDateFormat(FORMAT_BIBSYS_DATO);
//        sf.setLenient(false);
//
//        try {
//            sf.parse(s);
//            return true;
//        } catch (ParseException var3) {
//            LOG.debug("", var3);
//            return false;
//        }
//    }

//    public static boolean verifyDate(String s, String format) {
//        SimpleDateFormat sf = new SimpleDateFormat(format);
//        sf.setLenient(false);
//
//        try {
//            sf.parse(s);
//            return true;
//        } catch (ParseException var4) {
//            LOG.debug("", var4);
//            return false;
//        }
//    }

    public static boolean verifyUTCdate(String s) {
        try {
            SimpleDateFormat sf;
            if (s.length() == 10) {
                sf = new SimpleDateFormat(FORMAT_ZULU_SHORT);
                sf.setLenient(false);
                sf.parse(s);
            } else {
                sf = new SimpleDateFormat(FORMAT_ZULU_LONG);
                sf.setLenient(false);
                sf.parse(s);
            }

            return true;
        } catch (ParseException var2) {
            LOG.debug("", var2);
            return false;
        }
    }

//    public static String formatElapsedNanos(long duration) {
//        TimeUnit scale = TimeUnit.NANOSECONDS;
//        long days = scale.toDays(duration);
//        duration -= TimeUnit.DAYS.toMillis(days);
//        long hours = scale.toHours(duration);
//        duration -= TimeUnit.HOURS.toMillis(hours);
//        long minutes = scale.toMinutes(duration);
//        duration -= TimeUnit.MINUTES.toMillis(minutes);
//        long seconds = scale.toSeconds(duration);
//        duration -= TimeUnit.SECONDS.toMillis(seconds);
//        long millis = scale.toMillis(duration);
//        duration -= TimeUnit.MILLISECONDS.toMillis(seconds);
//        long nanos = scale.toNanos(duration);
//        return String.format("%d days, %d hrs, %d min, %d sec, %d ms, %d ns", days, hours, minutes, seconds, millis, nanos);
//    }
}

