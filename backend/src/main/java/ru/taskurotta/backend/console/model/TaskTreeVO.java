package ru.taskurotta.backend.console.model;

import java.util.Arrays;
import java.util.UUID;

/**
 * Task dependecncies tree representation object
 * User: dimadin
 * Date: 31.05.13 16:23
 */
public class TaskTreeVO {

    public static final int STATE_UNDEFINED = -1;
    public static final int STATE_NOT_ANSWERED = 0;
    public static final int STATE_SUCCESS = 1;
    public static final int STATE_ERROR = 2;

    private UUID id;//task uuid of current node
    private UUID parent;//parent task tree
    private String desc;
    private int state=STATE_UNDEFINED;
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

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "TaskTreeVO{" +
                "id=" + id +
                ", parent=" + parent +
                ", desc='" + desc + '\'' +
                ", state=" + state +
                ", children=" + Arrays.toString(children) +
                "} ";
    }
}
