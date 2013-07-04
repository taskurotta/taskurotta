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
    private Graph graph;
    private Date createdDate = new Date();

    public Snapshot() {

    }

    public Snapshot(Graph graph) {
        this.graph = graph;
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
    }


    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
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
        if (graph != null ? !graph.equals(snapshot.graph) : snapshot.graph != null) return false;
        if (snapshotId != null ? !snapshotId.equals(snapshot.snapshotId) : snapshot.snapshotId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = snapshotId != null ? snapshotId.hashCode() : 0;
        result = 31 * result + (graph != null ? graph.hashCode() : 0);
        result = 31 * result + (createdDate != null ? createdDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Snapshot{");
        sb.append("snapshotId=").append(snapshotId);
        sb.append(", graph=").append(graph);
        sb.append(", createdDate=").append(createdDate);
        sb.append('}');
        return sb.toString();
    }
}