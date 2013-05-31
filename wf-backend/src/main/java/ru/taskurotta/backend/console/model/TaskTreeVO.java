package ru.taskurotta.backend.console.model;

import java.util.Arrays;
import java.util.UUID;

/**
 * Task dependecncies tree representation object
 * User: dimadin
 * Date: 31.05.13 16:23
 */
public class TaskTreeVO {

    private UUID id;//task uuid of current node
    private UUID parent;//parent task tree
    private String desc;
    private TaskTreeVO[] children;//child task trees

    public TaskTreeVO(){
    }

    public TaskTreeVO(UUID id){
       this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getParent() {
        return parent;
    }

    public void setParent(UUID parent) {
        this.parent = parent;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public TaskTreeVO[] getChildren() {
        return children;
    }

    public void setChildren(TaskTreeVO[] children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "TaskTreeVO{" +
                "id=" + id +
                ", parent=" + parent +
                ", desc='" + desc + '\'' +
                ", children=" + Arrays.toString(children) +
                '}';
    }
}
