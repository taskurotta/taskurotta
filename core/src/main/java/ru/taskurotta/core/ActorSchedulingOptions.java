package ru.taskurotta.core;

/**
 * User: stukushin
 * Date: 15.04.13
 * Time: 16:45
 */
public interface ActorSchedulingOptions {

    public String getCustomId();
    public void setCustomId(String customId);

    public long getStartTime();
    public void setStartTime(long startTime);

    public String getTaskList();
    public void setTaskList(String taskList);
}
