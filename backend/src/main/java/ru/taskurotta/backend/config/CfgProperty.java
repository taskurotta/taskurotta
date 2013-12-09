package ru.taskurotta.backend.config;

import java.io.Serializable;

/**
 * Property item of a configuration module props
 * Date: 09.12.13 10:37
 */
public class CfgProperty implements Serializable {
    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_INTEGER = 2;
    public static final int TYPE_STRING = 3;
    public static final int TYPE_FLOAT = 4;

    protected int type;
    protected String name;
    protected String value;
    protected boolean hidden;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
