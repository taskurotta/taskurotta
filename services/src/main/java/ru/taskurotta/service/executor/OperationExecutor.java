package ru.taskurotta.service.executor;

public interface OperationExecutor<T> {

    public void enqueue(Operation<T> operation);

    public int size();

    public boolean isEmpty();
}
