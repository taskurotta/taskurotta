package ru.taskurotta.backend.hz.console;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 04.07.13 16:26
 */
public class HzInstanceInfoVO {

    private String id;
    private String type;
    private String name;
    private int size;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "HzInstanceInfoVO{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", size=" + size +
                "} " + super.toString();
    }
}
