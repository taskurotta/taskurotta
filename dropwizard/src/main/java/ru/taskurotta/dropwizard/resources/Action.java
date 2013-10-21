package ru.taskurotta.dropwizard.resources;

/**
 * List of console actions to use
 * User: dimadin
 * Date: 04.10.13 10:28
 */
public enum Action {

    LIST("list"), CARD("card"), CREATE("create"), COMPARE("compare"), BLOCK("block"), UNBLOCK("unblock"),
    DELETE("delete"), EDIT("edit"), UPDATE("update"), ACTIVATE("activate"), DEACTIVATE("deactivate"),
    GROUP("group");

    private String value;

    Action(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
