package ru.taskurotta.backend.hz.console;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 04.07.13 15:52
 */
public class HzInfoVo {

    private List<String> mapNames = new ArrayList<>();
    private List<String> setNames = new ArrayList<>();
    private List<String> queueNames = new ArrayList<>();
    private List<String> multimapNames = new ArrayList<>();
    private List<String> listNames = new ArrayList<>();

    public List<String> getMapNames() {
        return mapNames;
    }

    public void setMapNames(List<String> mapNames) {
        this.mapNames = mapNames;
    }

    public List<String> getSetNames() {
        return setNames;
    }

    public void setSetNames(List<String> setNames) {
        this.setNames = setNames;
    }

    public List<String> getQueueNames() {
        return queueNames;
    }

    public void setQueueNames(List<String> queueNames) {
        this.queueNames = queueNames;
    }

    public List<String> getMultimapNames() {
        return multimapNames;
    }

    public void setMultimapNames(List<String> multimapNames) {
        this.multimapNames = multimapNames;
    }

    public List<String> getListNames() {
        return listNames;
    }

    public void setListNames(List<String> listNames) {
        this.listNames = listNames;
    }

    @Override
    public String toString() {
        return "HzInfoVO{" +
                "mapNames=" + mapNames +
                ", setNames=" + setNames +
                ", queueNames=" + queueNames +
                ", multimapNames=" + multimapNames +
                ", listNames=" + listNames +
                "} " + super.toString();
    }
}
