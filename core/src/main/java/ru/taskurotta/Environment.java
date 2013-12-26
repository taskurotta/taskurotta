package ru.taskurotta;

/**
 * User: greg
 * <p/>
 * Singleton for detecting run Environment
 * For catching exception while running test add "-Dtaskurotta.env=TEST" to java process or add it to surefire plugin in maven
 */
public class Environment {

    public static final String TASKUROTTA_ENV_PROPERTY = "taskurotta.env";
    private static Environment instance;
    private Type type;

    private Environment(Type type) {
        this.type = type;
    }

    public static Environment getInstance() {
        if (instance == null) {
            String property = System.getProperty(TASKUROTTA_ENV_PROPERTY);
            if (property != null && property.equals("TEST")) {
                instance = new Environment(Type.TEST);
            } else {
                instance = new Environment(Type.NONE);
            }
        }
        return instance;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        TEST, NONE
    }
}
