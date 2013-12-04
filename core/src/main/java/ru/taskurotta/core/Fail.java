package ru.taskurotta.core;

/**
 * Created by void 25.11.13 19:51
 */
public class Fail {
    private String[] types;
    private String message;

    public Fail(String type, String message) {
        this(new String[]{type}, message);
    }

    public Fail(String[] types, String message) {
        if (null == types || types.length < 1) {
            throw new IllegalArgumentException("Type of the fail cannot be empty");
        }
        this.types = types;
        this.message = message;
    }

    public String getType() {
        return types[0];
    }

    public String getMessage() {
        return message;
    }

    public boolean instanceOf(String className) {
        for (String type : types) {
            if (type.equals(className)) {
                return true;
            }
        }
        return false;
    }
}
