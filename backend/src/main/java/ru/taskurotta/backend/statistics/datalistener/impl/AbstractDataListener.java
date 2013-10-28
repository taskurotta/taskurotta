package ru.taskurotta.backend.statistics.datalistener.impl;

import com.google.common.util.concurrent.AtomicDoubleArray;
import ru.taskurotta.backend.statistics.datalistener.DataListener;

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * User: stukushin
 * Date: 09.09.13
 * Time: 13:47
 */
public abstract class AbstractDataListener implements DataListener {

    private final int SECONDS_IN_MINUTE = 60;
    private final int MINUTES_IN_HOUR = 60;
    private final int SECONDS_IN_HOUR = MINUTES_IN_HOUR * SECONDS_IN_MINUTE;
    private final int MINUTES_IN_23_HOURS = 23 * MINUTES_IN_HOUR;

    private final int MILLISECONDS_IN_SECONDS = 1000;
    private final int MILLISECONDS_IN_HOUR = SECONDS_IN_HOUR * MILLISECONDS_IN_SECONDS;
    private final int MILLISECONDS_IN_DAY = MILLISECONDS_IN_HOUR * 24;

    private final AtomicLongArray hourCounts = new AtomicLongArray(SECONDS_IN_HOUR);
    private final AtomicLongArray dayCounts = new AtomicLongArray(MINUTES_IN_23_HOURS);

    private final AtomicDoubleArray hourMeans = new AtomicDoubleArray(SECONDS_IN_HOUR);
    private final AtomicDoubleArray dayMeans = new AtomicDoubleArray(MINUTES_IN_23_HOURS);

    public void handleIntData(String metricName, String datasetName, int value, long currentTime){
        //do nothing
    }

    @Override
    public void handle(String metricName, String datasetName, long count, double mean, long time) {

        long period = System.currentTimeMillis() - time;

        if (period < MILLISECONDS_IN_HOUR) {
            saveDataForSeconds(time, count, mean);
        }

        if (period > MILLISECONDS_IN_HOUR && period < MILLISECONDS_IN_DAY) {
            saveDataForMinutes(time, count, mean);
        }
    }

    private void saveDataForSeconds(long time, long count, double mean) {
        int position = hourPosition(time); // second

        hourCounts.addAndGet(position, count);
        hourMeans.addAndGet(position, mean);

        if (position % SECONDS_IN_MINUTE == 0) {
            transferToDayCount(time - MILLISECONDS_IN_HOUR + 1, position, position + SECONDS_IN_MINUTE);
        }
    }

    private void saveDataForMinutes(long time, long count, double mean) {
        int position = dayPosition(time); // minute

        dayCounts.addAndGet(position, count);
        dayMeans.addAndGet(position, mean);

        if (position % MINUTES_IN_HOUR == 0) {
            removeItemsFromDayData(position, position + MINUTES_IN_HOUR);
        }
    }

    private void transferToDayCount(long time, int from, int to) {
        long counts = 0;
        double means = 0;
        int counter;

        int length = SECONDS_IN_HOUR;

        if (from < to) {
            counter = to - from;

            if (to < length) {
                for (int i = from; i < to; i++) {
                    counts += hourCounts.getAndSet(i, 0);
                    means += hourMeans.getAndAdd(i, 0);
                }
            } else {
                for (int i = from; i < length; i++) {
                    counts += hourCounts.getAndSet(i, 0);
                    means += hourMeans.getAndAdd(i, 0);
                }

                for (int i = 0; i < to - length; i++) {
                    counts += hourCounts.getAndSet(i, 0);
                    means += hourMeans.getAndAdd(i, 0);
                }
            }
        } else {
            counter = length - from + to;

            for (int i = from; i < length; i++) {
                counts += hourCounts.getAndSet(i, 0);
                means += hourMeans.getAndAdd(i, 0);
            }

            for (int i = 0; i < to; i++) {
                counts += hourCounts.getAndSet(i, 0);
                means += hourMeans.getAndAdd(i, 0);
            }
        }

        saveDataForMinutes(time, counts / counter, means / counter);
    }

    private void removeItemsFromDayData(int from, int to) {
        int length = hourCounts.length();

        if (from < to) {
            if (to < length) {
                for (int i = from; i < to; i++) {
                    hourCounts.set(i, 0);
                    hourMeans.set(i, 0);
                }
            } else {
                for (int i = from; i < length; i++) {
                    hourCounts.set(i, 0);
                    hourMeans.set(i, 0);
                }

                for (int i = 0; i < to - length; i++) {
                    hourCounts.set(i, 0);
                    hourMeans.set(i, 0);
                }
            }
        } else {
            for (int i = from; i < length; i++) {
                hourCounts.set(i, 0);
                hourMeans.set(i, 0);
            }

            for (int i = 0; i < to; i++) {
                hourCounts.set(i, 0);
            }
        }
    }

    private int hourPosition(long time) {
        return (int) ((time / MILLISECONDS_IN_SECONDS) % SECONDS_IN_HOUR);
    }

    private int dayPosition(long time) {
        return (int) ((time / MILLISECONDS_IN_SECONDS / SECONDS_IN_MINUTE) % MINUTES_IN_23_HOURS);
    }

    public long[] getHourCounts() {
        long[] data = new long[SECONDS_IN_HOUR];

        int i = 0;

        long now = System.currentTimeMillis();

        while (i < SECONDS_IN_HOUR) {
            int position = hourPosition(now + i * MILLISECONDS_IN_SECONDS);

            data[i] = hourCounts.get(position);

            i++;
        }

        return data;
    }

    public long[] getDayCounts() {
        long[] data = new long[MINUTES_IN_23_HOURS];

        int i = 0;

        long now = System.currentTimeMillis();

        while (i < MINUTES_IN_23_HOURS) {
            int position = dayPosition(now + i * MILLISECONDS_IN_SECONDS * SECONDS_IN_MINUTE);

            data[i] = dayCounts.get(position);

            i++;
        }

        return data;
    }

    public double[] getHourMeans() {
        double[] data = new double[SECONDS_IN_HOUR];

        int i = 0;

        long now = System.currentTimeMillis();

        while (i < SECONDS_IN_HOUR) {
            int position = hourPosition(now + i * MILLISECONDS_IN_SECONDS);

            data[i] = hourMeans.get(position);

            i++;
        }

        return data;
    }

    public double[] getDayMeans() {
        double[] data = new double[MINUTES_IN_23_HOURS];

        int i = 0;

        long now = System.currentTimeMillis();

        while (i < MINUTES_IN_23_HOURS) {
            int position = dayPosition(now + i * MILLISECONDS_IN_SECONDS * SECONDS_IN_MINUTE);

            data[i] = dayMeans.get(position);

            i++;
        }

        return data;
    }
}