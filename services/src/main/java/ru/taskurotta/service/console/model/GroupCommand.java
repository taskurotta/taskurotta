package ru.taskurotta.service.console.model;

/**
 * User: dimadin
 * Date: 16.10.13 15:40
 */
public class GroupCommand extends SearchCommand {

    public static final String GROUP_STARTER = "starter";
    public static final String GROUP_ACTOR = "actor";
    public static final String GROUP_EXCEPTION = "exception";

    private String group;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return "GroupCommand{" +
                "group='" + group + '\'' +
                "} " + super.toString();
    }
}
