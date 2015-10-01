package ru.taskurotta.documentation;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * User: stukushin
 * Date: 29.09.2015
 * Time: 13:39
 */

public class MenuItem {
    @JsonIgnore
    private int level;
    private String anchor;
    private String caption;
    private List<MenuItem> children = new ArrayList<>();

    public MenuItem(int level, String anchor, String caption) {
        this.level = level;
        this.anchor = anchor;
        this.caption = caption;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getAnchor() {
        return anchor;
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public List<MenuItem> getChildren() {
        return children;
    }

    public void setChildren(List<MenuItem> children) {
        this.children = children;
    }
}
