package ru.taskurotta.dropwizard.client.serialization;

public interface Constants {

    public static final String ACTOR_DEFINITION_NAME = "name";
    public static final String ACTOR_DEFINITION_VERSION = "version";

    public static final String TASK_ID = "taskId";
    public static final String TASK_PROCESS_ID = "processId";
    public static final String TASK_TARGET = "target";
    public static final String TASK_ARGS = "args";
    public static final String TASK_OPTIONS = "options";
    public static final String TASK_START_TIME = "startTime";
    public static final String TASK_NUMBER_OF_ATTEMPTS = "numberOfAttempts";
    public static final String TASK_METHOD = "method";
    public static final String TASK_ACTOR_ID = "actorId";
    public static final String TASK_TYPE = "type";

    public static final String OPTIONS_ARG_TYPES = "argTypes";
    public static final String TASK_TARGET_NAME = "name";
    public static final String TASK_TARGET_TYPE = "type";
    public static final String TASK_TARGET_METHOD = "method";
    public static final String TASK_TARGET_VERSION = "version";

    public static final String ARG_CLASSNAME = "className";
    public static final String ARG_IS_PROMISE = "promise";
    public static final String ARG_TASK_ID = "taskId";
    public static final String ARG_IS_READY = "ready";
    public static final String ARG_JSON_VALUE = "jsonvalue";

    public static final String RESULT_TASK_ID = "taskId";
    public static final String RESULT_PROCESS_ID = "processId";
    public static final String RESULT_VALUE = "value";
    public static final String RESULT_IS_ERROR = "error";
    public static final String RESULT_TASKS = "tasks";
    public static final String RESULT_ERROR_CONTAINER = "ErrorContainer";

    public static final String ERR_CLASS_NAME = "className";
    public static final String ERR_MESSAGE = "message";
    public static final String ERR_RESTART_TIME = "restartTime";
    public static final String ERR_SHOULD_BE_RESTARTED = "shouldBeRestarted";
    public static final String ERR_STACK_TRACE = "stackTrace";

    public static final String STE_DECLARING_CLASS = "declaringClass";
    public static final String STE_FILE_NAME = "fileName";
    public static final String STE_LINE_NUMBER = "lineNumber";
    public static final String STE_METHOD_NAME = "methodName";
}
