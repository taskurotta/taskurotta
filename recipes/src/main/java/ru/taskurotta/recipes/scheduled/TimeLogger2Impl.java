package ru.taskurotta.recipes.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.internal.RuntimeContext;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created on 21.07.2014.
 */
public class TimeLogger2Impl implements TimeLogger2 {
    private static final Logger logger = LoggerFactory.getLogger(TimeLogger2Impl.class);

    @Override
    public void log() {//TimeLogger without arguments
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date taskDate = new Date(RuntimeContext.getCurrent().getStartTime());

        logger.info("Current time[{}], task time is[{}]", sdf.format(new Date()), sdf.format(taskDate));

    }
}
