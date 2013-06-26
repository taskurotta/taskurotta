package ru.taskurotta.backend.snapshot;

import com.google.common.base.Objects;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.core.Task;
import ru.taskurotta.transport.model.DecisionContainer;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * User: greg
 */
public class Snapshot implements Serializable {
    private UUID snapshotId = UUID.randomUUID();
    private Task task;
    private Graph graph;
    private DependencyDecision dependencyDecision;
    private Date createdDate = new Date();

    public Snapshot() {

    }

    public Snapshot(Task task, Graph graph, DependencyDecision dependencyDecision) {
        this.task = task;
        this.graph = graph;
        this.dependencyDecision = dependencyDecision;
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public DependencyDecision getDependencyDecision() {
        return dependencyDecision;
    }

    public void setDependencyDecision(DependencyDecision dependencyDecision) {
        this.dependencyDecision = dependencyDecision;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Snapshot snapshot = (Snapshot) o;

        if (createdDate != null ? !createdDate.equals(snapshot.createdDate) : snapshot.createdDate != null)
            return false;
        if (dependencyDecision != null ? !dependencyDecision.equals(snapshot.dependencyDecision) : snapshot.dependencyDecision != null) {
            return false;
        }
        if (graph != null ? !graph.equals(snapshot.graph) : snapshot.graph != null) return false;
        if (snapshotId != null ? !snapshotId.equals(snapshot.snapshotId) : snapshot.snapshotId != null) return false;
        if (task != null ? !task.equals(snapshot.task) : snapshot.task != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = snapshotId != null ? snapshotId.hashCode() : 0;
        result = 31 * result + (task != null ? task.hashCode() : 0);
        result = 31 * result + (graph != null ? graph.hashCode() : 0);
        result = 31 * result + (dependencyDecision != null ? dependencyDecision.hashCode() : 0);
        result = 31 * result + (createdDate != null ? createdDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("snapshotId", snapshotId)
                .add("task", task)
                .add("graph", graph)
                .add("dependencyDecision", dependencyDecision)
                .add("createdDate", createdDate)
                .toString();
    }
}
