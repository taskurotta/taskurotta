package ru.taskurotta.backend.console.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * POJO representing workflow process
 * User: dimadin
 * Date: 21.05.13 11:09
 */
public class ProcessVO implements Serializable {

    private UUID processUuid;
    private UUID startTaskUuid;

    private String customId;

    long startTime = -1l;
    long endTime = -1l;

    private String returnValueJson;

    public UUID getProcessUuid() {
        return processUuid;
    }

    public void setProcessUuid(UUID processUuid) {
        this.processUuid = processUuid;
    }

    public UUID getStartTaskUuid() {
        return startTaskUuid;
    }

    public void setStartTaskUuid(UUID startTaskUuid) {
        this.startTaskUuid = startTaskUuid;
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

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

    public String getReturnValueJson() {
        return returnValueJson;
    }

    public void setReturnValueJson(String returnValueJson) {
        this.returnValueJson = returnValueJson;
    }
}
