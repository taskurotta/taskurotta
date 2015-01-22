package ru.taskurotta.test.stress;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public abstract class DaemonThread extends Thread {

    AtomicBoolean isShutdown = new AtomicBoolean(false);
    TimeUnit sleepUnit;
    long sleepValue;

    public DaemonThread(String name, TimeUnit sleepUnit, long sleepValue) {
        super(name);

        this.sleepUnit = sleepUnit;
        this.sleepValue = sleepValue;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                isShutdown.set(true);
            }
        });
    }

    public static class StopSignal extends RuntimeException {}


    @Override
    public final void run() {

        while (true) {

            if (isShutdown.get() || isInterrupted()) {
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
