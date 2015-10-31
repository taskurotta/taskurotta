package ru.taskurotta.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 */
public abstract class DaemonThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(DaemonThread.class);

    TimeUnit sleepUnit;
    long sleepValue;

    public DaemonThread(String name, TimeUnit sleepUnit, long sleepValue) {
        super(name);

        this.sleepUnit = sleepUnit;
        this.sleepValue = sleepValue;

    }

    public static class StopSignal extends RuntimeException {}


    @Override
    public final void run() {

        while (true) {

            if (Shutdown.isTrue() || isInterrupted()) {
                break;
            }

            try {
                daemonJob();
            } catch (StopSignal signal) {
                return;
            } catch (Throwable ignore) {
                logger.error("Error while doing daemon job", ignore);
            }

            if (sleepUnit != null) {

                try {
                    sleepUnit.sleep(sleepValue);
                } catch (InterruptedException e) {
                    // ignore
                }
            }

        }

    }

    public abstract void daemonJob();
}
