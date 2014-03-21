package ru.taskurotta.service.executor;

public interface OperationExecutor {

    public void enqueue(Operation operation);

    public int size();

    public boolean isEmpty();
}
