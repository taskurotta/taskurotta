package ru.taskurotta.schedule;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 24.09.13 18:26
 */
public interface JobConstants {

    String ACTION_LIST = "list";
    String ACTION_CREATE = "create";
    String ACTION_EDIT = "edit";
    String ACTION_DELETE = "delete";
    String ACTION_ACTIVATE = "activate";
    String ACTION_DEACTIVATE = "deactivate";

    int STATUS_ACTIVE = 1;
    int STATUS_INACTIVE = -1;
    int STATUS_UNDEFINED = 0;
    int STATUS_ERROR = -2;




}
