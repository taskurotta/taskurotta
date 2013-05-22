package ru.taskurotta.adapter;

/**
 * User: stukushin
 * Date: 22.05.13
 * Time: 11:52
 */
public interface TaskCreator {

    public static final String START_RESOURCE = "/tasks/start";

    public String createTask(String actorId, String method, Object[] args, String customId, long startTime, String taskList);
}
