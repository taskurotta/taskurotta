package ru.taskurotta.checkpoint;

public class CheckpointCommand {

    private long startTime;
    private long endTime;
    private String type;

    public long getStartTime() {
        return startTime;
    }
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    public long getEndTime() {
        return endTime;
    }
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "CheckpointCommand [startTime=" + startTime + ", endTime="
                + endTime + ", type=" + type + "]";
    }
}
