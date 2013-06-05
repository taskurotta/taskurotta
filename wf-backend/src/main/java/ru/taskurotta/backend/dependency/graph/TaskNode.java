package ru.taskurotta.backend.dependency.graph;

import ru.taskurotta.transport.model.TaskType;

import java.util.List;
import java.util.UUID;

/**
 * POJO representing simple Node for process dependency graph
 * User: dimadin
 * Date: 03.06.13 10:23
 */
public class TaskNode {

    private UUID id;
    private UUID processId;
    private TaskType type;
    private List<UUID> depends;

    private boolean released = false;
    private boolean scheduled = false;


    public TaskNode() {
    }

    public TaskNode(UUID id, UUID processId) {
        this.id = id;
        this.processId = processId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TaskType getType() {
        return type;
    }

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public List<UUID> getDepends() {
        return depends;
    }

    public void setDepends(List<UUID> depends) {
        this.depends = depends;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }

    @Override
    public String toString() {
        return "TaskNode{" +
                "id=" + id +
                ", processId=" + processId +
                ", type=" + type +
                ", depends=" + depends +
                ", released=" + released +
                ", scheduled=" + scheduled +
                '}';
    }

}
