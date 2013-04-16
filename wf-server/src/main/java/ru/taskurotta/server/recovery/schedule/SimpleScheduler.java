package ru.taskurotta.server.recovery.schedule;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple scheduler with non-cron schedule String
 * Schedule template: "<Integer> value <TimeUnit> unit", ex.: "50 SECONDS" means "run every 50 seconds"
 */
public class SimpleScheduler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SimpleScheduler.class);

    protected String schedule;//shedule String, ex "100 SECONDS"
    protected Runnable scheduledProcess;//Process to run on every schedule iteration
    protected String name;//TODO: Make Runnable Interface with getName() method for all daemon processes?

    /**
     * starts the scheduler
     */
    public void start() {
        Thread runner = new Thread();
        runner.setDaemon(true);
        runner.setName(name);
        runner.start();
    }

    @Override
    public void run() {
        logger.debug("SimpleScheduler daemon started. Schedule[{}]", schedule);
        while(repeat(schedule)) {
            scheduledProcess.run();
        }
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    protected boolean repeat(String schedule) {
        if (schedule == null) {
            return false;
        }
        Integer number = Integer.valueOf(schedule.replaceAll("\\D", "").trim());
        TimeUnit unit = TimeUnit.valueOf(schedule.replaceAll("\\d", "").trim());
        try {
            Thread.sleep(unit.toMillis(number));
        } catch (InterruptedException e) {
            logger.error(getClass().getName() + " schedule interrupted", e);
        }
        return true;
    }

    public void setScheduledProcess(Runnable scheduledProcess) {
        this.scheduledProcess = scheduledProcess;
    }

    public void setName(String name) {
        this.name = name;
    }

}
