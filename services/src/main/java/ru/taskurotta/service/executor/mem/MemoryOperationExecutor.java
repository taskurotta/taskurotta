package ru.taskurotta.service.executor.mem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.executor.Operation;
import ru.taskurotta.service.executor.OperationExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MemoryOperationExecutor implements OperationExecutor {

    private BlockingQueue<Operation> queue;

    public MemoryOperationExecutor(String name, Object nativePoint, int queueCapacity, int poolSize) {
        this.queue = new LinkedBlockingQueue<>(queueCapacity);

        MemoryOperationExecutor.startWorkers(name, nativePoint, queue, poolSize);

    }

    public static void startWorkers(final String name, final Object nativePoint, final BlockingQueue<Operation> queue,
                                    int poolSize) {

        final Logger logger = LoggerFactory.getLogger(OperationExecutor.class);

        for (int i = 0; i < poolSize; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (true) {
                        Operation operation = null;

                        try {
                            operation = queue.poll(1, TimeUnit.SECONDS);

                            if (operation == null) {
                                continue;
                            }

                            operation.init(nativePoint);

                            try {
                                operation.run();
                            } catch (Throwable e) {
                                logger.error("Can not execute operation [{}]", name, e);
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (Thread.currentThread().isInterrupted()) return;

                    }
                }
            });

            thread.setName("Operation executor [" + name + "] " + i);
            thread.start();
        }
    }

    @Override
    public void enqueue(Operation operation) {
        queue.add(operation);
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
