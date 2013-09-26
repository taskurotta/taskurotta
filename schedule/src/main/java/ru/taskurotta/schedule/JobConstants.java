package ru.taskurotta.schedule;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 24.09.13 18:26
 */
public interface JobConstants {

    public static final String ACTION_LIST = "list";
    public static final String ACTION_CREATE = "create";
    public static final String ACTION_EDIT = "edit";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_ACTIVATE = "activate";
    public static final String ACTION_DEACTIVATE = "deactivate";
    public static final String ACTION_VALIDATE = "validate";

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_INACTIVE = -1;
    public static final int STATUS_UNDEFINED = 0;
    public static final int STATUS_ERROR = -2;




}
