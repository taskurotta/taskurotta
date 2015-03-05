package ru.taskurotta.util;

import java.util.concurrent.TimeUnit;

/**
 */
public abstract class DaemonThread extends Thread {

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
