package ru.taskurotta.server.model;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 12:48 PM
 */
public class TaskStateObject {

    public static enum STATE {
        wait, process, depend, done
    }

    protected long time;
    protected STATE state;
    protected String actorId;

    public TaskStateObject(String actorId, STATE state, long time) {
        this.actorId = actorId;
        this.state = state;
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public STATE getValue() {
        return state;
    }

    public String getActorId() {
        return actorId;
    }

    @Override
    public String toString() {
        return "TaskStateMemory{" +
                "time=" + time +
                ", state=" + state +
                ", actorId='" + actorId + '\'' +
                '}';
    }

}
