package ru.taskurotta.checkpoint.model;

/**
 * Command for listing Checkpoints from CheckpointService,
 * can contain multiple search/filter/sorting criteria
 */
public class CheckpointCommand {

    //filter by min checkpoint time
    private long minTime;

    //filter by max checkpoint time
    private long maxTime;

    //filter by entity type
    private String type;

    public long getMinTime() {
        return minTime;
    }

    public void setMinTime(long minTime) {
        this.minTime = minTime;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "CheckpointCommand [minTime=" + minTime + ", maxTime=" + maxTime
                + ", type=" + type + "]";
    }
}
