package ru.taskurotta.util;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class DuplicationErrorSuppressorTest {

    public static class CustomException extends RuntimeException {

    }

    @Test
    public void test() throws InterruptedException {
        final DuplicationErrorSuppressor duplicationErrorSuppressor = new DuplicationErrorSuppressor();

        final AtomicInteger counter = new AtomicInteger(0);

        final int threads = 10;
        final int attempts = 500;
        final CountDownLatch countDownLatch = new CountDownLatch(threads);

        for (int j = 0; j < threads; j++) {

            new DaemonThread("error thread " + j, null, 0) {
                @Override
                public void daemonJob() {
                    for (int i = 0; i < attempts; i++) {
                        if (duplicationErrorSuppressor.isLastErrorEqualsTo("Message !", new CustomException())) {
                            counter.incrementAndGet();
                        }
                    }

                    countDownLatch.countDown();
                    throw DaemonThread.STOP;
                }
            }.start();
        }

        countDownLatch.await();
        assertEquals(attempts * threads - 1, counter.get());
    }
}