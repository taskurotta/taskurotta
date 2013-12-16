package ru.taskurotta.service.statistics.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 07.10.13 14:26
 */
public class RateUtils {

    private static final Logger logger = LoggerFactory.getLogger(RateUtils.class);

    public static double getOverallRate(int inTotal, long[] inPeriod, int outTotal, long[] outPeriod) {
        double incomeRate = round(getRate(inTotal, inPeriod), 4);
        double outcomeRate = round(getRate(outTotal, outPeriod), 4);

        return round(incomeRate-outcomeRate, 2);
    }


    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        try {
            BigDecimal bd = new BigDecimal(String.valueOf(value));
            bd = bd.setScale(places, BigDecimal.ROUND_HALF_UP);
            return bd.doubleValue();
        } catch(NumberFormatException e) {
            logger.debug("Exception at rounding value [{}] by [{}] palces", value, places);
            return 0;
        }
    }

    public static Double getRate(int count, long[] period) {
        Double result = 0d;

        if (count>0 && period[0]>0 && period[1]>0) {
            long time = period[1]-period[0];
            result = Double.valueOf(count*1000)/Double.valueOf(time);
        }

        return result;
    }

}
