package ru.taskurotta.recipes.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.internal.RuntimeContext;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: dimadin
 * Date: 26.09.13 16:18
 */
public class TimeLoggerImpl implements TimeLogger {

    private static final Logger logger = LoggerFactory.getLogger(TimeLoggerImpl.class);

    @Override
    public void log(String arg1, String arg2, int arg3) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date taskDate = new Date(RuntimeContext.getCurrent().getStartTime());

        logger.info("Current time[{}], task time is[{}], arg1[{}], arg2[{}], arg3[{}]", sdf.format(new Date()), sdf.format(taskDate), arg1, arg2, arg3);

    }
}
