package ru.taskurotta.console.model;

/**
 * POJO object representing task queue
 * User: dimadin
 * Date: 21.05.13 10:45
 */
public class QueueVO {

    private String name;
    private int count;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
