package ru.taskurotta.server.config.expiration.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.config.model.ExpirationPolicy;

/**
 * ExpirationPolicy having knowledge of holidays, i.e. days which are excluded at expiration time evaluation.
 *
 * List of holidays should be passed as a path to a file containing new-line separated list of
 * Date strings .
 *
 * Example (formatted as "dd.MM.yyyy" - defaults):
 * 01.01.2013
 * #comment starting with hash symbol permitted
 * 02.01.2013
 * ...
 * 08.01.2013
 */
public class SkippingHolidaysTimeoutPolicy implements ExpirationPolicy {

    public static final String PROP_TIMEOUT = "timeout";
    public static final String PROP_LOCATION = "location";
    public static final String PROP_FORMAT = "format";

    private static final Logger logger = LoggerFactory.getLogger(SkippingHolidaysTimeoutPolicy.class);

    private int timeout = 1;
    private TimeUnit timeoutUnit = TimeUnit.DAYS;
    private String format = "dd.MM.yyyy";
    private List<Date> holidays;

    public SkippingHolidaysTimeoutPolicy(Properties properties) throws IOException, ParseException {
        String timeout = properties.getProperty(PROP_TIMEOUT);
        if(timeout != null) {
            String intStr = timeout.replaceAll("\\D", "").trim();
            String timeUnitStr = timeout.replaceAll("\\d", "").trim();
            if(intStr.length() > 0) {
                this.timeout = Integer.valueOf(intStr);
            }
            if(timeUnitStr.length() > 0) {
                this.timeoutUnit = TimeUnit.valueOf(timeUnitStr.toUpperCase());
            }
        }
        String format =properties.getProperty(PROP_FORMAT);
        if(format!=null) {
            this.format = format;
        }

        initHolidaysList(properties.getProperty(PROP_LOCATION));
    }

    public SkippingHolidaysTimeoutPolicy(int timeout, TimeUnit timeoutUnit, List<Date> holidays) {
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.holidays = holidays;
    }

    private void initHolidaysList(String fileLocation) throws IOException, ParseException {
        if(fileLocation!=null) {
            holidays = new ArrayList<Date>();
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            sdf.setLenient(false);
            BufferedReader br = new BufferedReader(new FileReader(fileLocation));
            String line;
            while((line=br.readLine()) != null) {
                if(!line.startsWith("#") && line.trim().length()>0) {
                    holidays.add(sdf.parse(line.trim()));
                }

            }
            br.close();
            //Collections.sort(holidays);
            logger.debug("Holidays dates getted are [{}]", listHolidays());
        }
    }

    private String listHolidays() {
        String result = null;
        if(holidays != null) {
            StringBuffer sb = new StringBuffer();
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            for(Date date: holidays ){
                if(sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(sdf.format(date));
            }
            result = sb.toString();
        }
        return result;
    }


    //TODO: upgrade algorithm?
    private boolean isHoliday(long date, List<Date> holidays) {
        boolean result = false;
        if(holidays!=null && !holidays.isEmpty()) {
            Date targetDate = new Date(date);
            for(Date holiday: holidays) {
                if(isSameDay(holiday, targetDate)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    //keep only future holidays in list
    private List<Date> getActualHolidays(long forDate) {
        List<Date> result = null;
        if(holidays!=null && !holidays.isEmpty()) {
            result = new ArrayList<Date>();
            Date target = new Date(forDate);
            for(Date date: holidays) {
                if(date.after(target) && !isSameDay(date, target)) {
                    result.add(date);
                }
            }
        }
        return result;
    }

    public static boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    @Override
    public long getExpirationTime(UUID taskUuid, long forTime) {
        long result = forTime;
        List<Date> actualHolidays = getActualHolidays(forTime);
        do {
            result = result + timeoutUnit.toMillis(timeout);
        } while(isHoliday(result, actualHolidays));

        logger.debug("Expiration time getted for time[{}] is [{}]", new Date(forTime), new Date(result));

        return result;
    }

    @Override
    public boolean readyToRecover(UUID uuid) {
        return true;
    }

}
