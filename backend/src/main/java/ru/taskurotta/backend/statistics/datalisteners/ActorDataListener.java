package ru.taskurotta.backend.statistics.datalisteners;

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * User: stukushin
 * Date: 09.09.13
 * Time: 13:47
 */
abstract class ActorDataListener implements DataListener {

    private final int SECONDS_IN_MINUTE = 60;
    private final int MINUTES_IN_HOUR = 60;
    private final int SECONDS_IN_HOUR = MINUTES_IN_HOUR * SECONDS_IN_MINUTE;
    private final int MINUTES_IN_23_HOURS = 23 * MINUTES_IN_HOUR;

    private final int MILLISECONDS_IN_SECONDS = 1000;
    private final int MILLISECONDS_IN_HOUR = SECONDS_IN_HOUR * MILLISECONDS_IN_SECONDS;
    private final int MILLISECONDS_IN_DAY = MILLISECONDS_IN_HOUR * 24;

    private final AtomicLongArray hourCounts = new AtomicLongArray(SECONDS_IN_HOUR);
    private final AtomicLongArray dayCounts = new AtomicLongArray(MINUTES_IN_23_HOURS);

    @Override
    public void handle(String name, long count, double value, long time) {

        long period = System.currentTimeMillis() - time;

        if (period < MILLISECONDS_IN_HOUR) {
            addCountToSecond(time, count);
        }

        if (period > MILLISECONDS_IN_HOUR && period < MILLISECONDS_IN_DAY) {
            addCountToMinute(time, count);
        }
    }

    private void addCountToSecond(long time, long count) {
        int position = hourPosition(time); // second

        hourCounts.addAndGet(position, count);

        if (position % SECONDS_IN_MINUTE == 0) {
            transferToDayCount(time - MILLISECONDS_IN_HOUR + 1, position, position + SECONDS_IN_MINUTE);
        }
    }

    private void addCountToMinute(long time, long count) {
        int position = dayPosition(time); // minute

        dayCounts.addAndGet(position, count);

        if (position % MINUTES_IN_HOUR == 0) {
            removeItems(dayCounts, position, position + MINUTES_IN_HOUR);
        }
    }

    private void transferToDayCount(long time, int from, int to) {
        long sum = 0;
        int counter;

        int length = SECONDS_IN_HOUR;

        if (from < to) {
            counter = to - from;

            if (to < length) {
                for (int i = from; i < to; i++) {
                    sum += hourCounts.getAndSet(i, 0);
                }
            } else {
                for (int i = from; i < length; i++) {
                    sum += hourCounts.getAndSet(i, 0);
                }

                for (int i = 0; i < to - length; i++) {
                    sum += hourCounts.getAndSet(i, 0);
                }
            }
        } else {
            counter = length - from + to;

            for (int i = from; i < length; i++) {
                sum += hourCounts.getAndSet(i, 0);
            }

            for (int i = 0; i < to; i++) {
                sum += hourCounts.getAndSet(i, 0);
            }
        }

        addCountToMinute(time, sum / counter);
    }

    private void removeItems(AtomicLongArray data, int from, int to) {
        int length = data.length();

        if (from < to) {
            if (to < length) {
                for (int i = from; i < to; i++) {
                    data.set(i, 0);
                }
            } else {
                for (int i = from; i < length; i++) {
                    data.set(i, 0);
                }

                for (int i = 0; i < to - length; i++) {
                    data.set(i, 0);
                }
            }
        } else {
            for (int i = from; i < length; i++) {
                data.set(i, 0);
            }

            for (int i = 0; i < to; i++) {
                data.set(i, 0);
            }
        }

    }

    private int hourPosition(long time) {
        return (int) ((time / MILLISECONDS_IN_SECONDS) % SECONDS_IN_HOUR);
    }

    private int dayPosition(long time) {
        return (int) ((time / MILLISECONDS_IN_SECONDS / SECONDS_IN_MINUTE) % MINUTES_IN_23_HOURS);
    }

    @Override
    public long[] getHourCount() {
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

    @Override
    public long[] getDayCount() {
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
}