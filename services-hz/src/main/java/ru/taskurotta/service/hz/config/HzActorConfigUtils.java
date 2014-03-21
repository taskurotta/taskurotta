package ru.taskurotta.service.hz.config;

import ru.taskurotta.service.console.model.MetricsStatDataVO;
import ru.taskurotta.service.metrics.model.QueueBalanceVO;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 08.10.13 17:26
 */
public class HzActorConfigUtils {

    public static QueueBalanceVO sumQueueStates(QueueBalanceVO to, QueueBalanceVO from) {
        if (from == null) {
            return to;
        } else if (to == null) {
            return from;
        } else {
            to.setTotalOutHour(getSummedValue(to.getTotalOutHour(), from.getTotalOutHour()));
            to.setOutHourPeriod(getMergedPeriod(to.getOutHourPeriod(), from.getOutHourPeriod()));

            to.setTotalOutDay(getSummedValue(to.getTotalOutDay(), from.getTotalOutDay()));
            to.setOutDayPeriod(getMergedPeriod(to.getOutDayPeriod(), from.getOutDayPeriod()));

            to.setTotalInHour(getSummedValue(to.getTotalInHour(), from.getTotalInHour()));
            to.setInHourPeriod(getMergedPeriod(to.getInHourPeriod(), from.getInHourPeriod()));

            to.setTotalInDay(getSummedValue(to.getTotalInDay(), from.getTotalInDay()));
            to.setInDayPeriod(getMergedPeriod(to.getInDayPeriod(), from.getInDayPeriod()));

            return to;
        }
    }

    public static int getSummedValue(int val1, int val2) {
        if (val1 < 0) {
            return val2;
        } else if(val2 < 0) {
            return val1;
        } else {
            return val1 + val2;
        }
    }


    public static long[] getMergedPeriod(long[] val1, long[] val2) {
        long[] result = {-1l, -1l};

        if ((val2[0] < 0) || (val1[0]>0 && val1[0]<val2[0])) {
            result[0] = val1[0];
        } else {
            result[0] = val2[0];
        }

        if ((val2[1] < 0) || (val1[1]>0 && val1[1]>val2[1])) {
            result[1] = val1[1];
        } else {
            result[1] = val2[1];
        }

        return result;
    }

//    public static void setNodeCount(Collection<MetricsStatDataVO> target, int nodes) {
//        if (target!=null && !target.isEmpty()) {
//            for (MetricsStatDataVO item : target) {
//                item.setNodes(nodes);
//            }
//        }
//    }

    public static void mergeValues(Collection<MetricsStatDataVO> to, Collection<MetricsStatDataVO> from) {


    }


}
