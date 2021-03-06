package no.sikt.oai;

import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TimeUtils.class);
    public static final int STANDARD_DATE_LENGTH = 10;
    public static final String FORMAT_ZULU_LONG = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String FORMAT_ZULU_SHORT = "yyyy-MM-dd";

    @JacocoGenerated
    public TimeUtils() {
    }

    public static Date string2Date(String stringDate, String format) {
        SimpleDateFormat sf = new SimpleDateFormat(format, Locale.getDefault());
        sf.setLenient(false);
        Date myDate = null;

        try {
            myDate = sf.parse(stringDate);
            return myDate;
        } catch (ParseException var5) {
            LOG.debug("", var5);
            return myDate;
        }
    }

    public static String date2String(Date date, String format) {
        if (date != null) {
            SimpleDateFormat sf = new SimpleDateFormat(format, Locale.getDefault());
            return sf.format(date);
        } else {
            return "";
        }
    }

    public static String utcDateTime() {
        Date myDate = new Date();
        SimpleDateFormat sf = new SimpleDateFormat(FORMAT_ZULU_LONG, Locale.getDefault());
        return sf.format(myDate);
    }

    public static String getResponseTime() {
        return utcDateTime();
    }

    public static boolean isUTCdate(String s) {
        try {
            SimpleDateFormat sf;
            if (STANDARD_DATE_LENGTH == s.length()) {
                sf = new SimpleDateFormat(FORMAT_ZULU_SHORT, Locale.getDefault());
                sf.setLenient(false);
                sf.parse(s);
            } else {
                sf = new SimpleDateFormat(FORMAT_ZULU_LONG, Locale.getDefault());
                sf.setLenient(false);
                sf.parse(s);
            }

            return true;
        } catch (ParseException var2) {
            LOG.debug("", var2);
            return false;
        }
    }

}

