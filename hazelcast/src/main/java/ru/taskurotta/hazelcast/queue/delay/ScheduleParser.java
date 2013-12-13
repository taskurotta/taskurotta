package ru.taskurotta.hazelcast.queue.delay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 12.12.13
 * Time: 16:01
 */
public class ScheduleParser {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleParser.class);

    public static long getScheduleMillis(String schedule) {
        if (!StringUtils.hasText(schedule)) {
            throw new RuntimeException("Schedule is empty");
        }

        String[] params = schedule.split("_");
        if (params.length != 2) {
            throw new RuntimeException("Schedule [" + schedule + "] is incorrect");
        }

        long delay = Long.valueOf(params[0]);
        TimeUnit timeUnit = TimeUnit.valueOf(params[1].toUpperCase());

        logger.info("Set schedule delay = [{}] TimeUnit = [{}] for search ready processes for GC", delay, timeUnit);

        return timeUnit.toMillis(delay);
    }

}
