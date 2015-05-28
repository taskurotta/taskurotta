package ru.taskurotta.service.hz.dependency;

import ru.taskurotta.service.dependency.links.Modification;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

/**
 * Table of row contains decision stuff
 */
public class DecisionRow implements Serializable {
    protected UUID taskId;
    protected UUID processId;
    protected Modification modification;
    protected UUID[] readyItems;

    public DecisionRow(UUID taskId, UUID processId, Modification modification, UUID[] readyItems) {
        this.taskId = taskId;
        this.processId = processId;
        this.modification = modification;
        this.readyItems = readyItems;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DecisionRow)) return false;

        DecisionRow that = (DecisionRow) o;

        if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;
        if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;
        if (modification != null ? !modification.equals(that.modification) : that.modification != null)
            return false;
        if (!Arrays.equals(readyItems, that.readyItems)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = taskId != null ? taskId.hashCode() : 0;
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + (modification != null ? modification.hashCode() : 0);
        result = 31 * result + (readyItems != null ? Arrays.hashCode(readyItems) : 0);
        return result;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getProcessId() {
        return processId;
    }

    public Modification getModification() {
        return modification;
    }

    public UUID[] getReadyItems() {
        return readyItems;
    }

    @Override
    public String toString() {
        return "DecisionRow{" +
                "taskId=" + taskId +
                ", processId=" + processId +
                ", modification=" + modification +
                ", readyItems=" + Arrays.toString(readyItems) +
                '}';
    }
}