package ru.taskurotta.dropwizard.resources.console.broken;

import ru.taskurotta.service.console.model.GroupCommand;
import ru.taskurotta.service.console.model.ProcessGroupVO;

import java.io.Serializable;
import java.util.Collection;

/**
 * Wraps POJO response on Broken processes list adding some additional information for UI
 * User: dimadin
 * Date: 17.10.13 11:50
 */
public class ProcessGroupWrapper implements Serializable {

    protected Collection<ProcessGroupVO> groups;
    protected GroupCommand command;


    public ProcessGroupWrapper(){}

    public ProcessGroupWrapper(Collection<ProcessGroupVO> groups, GroupCommand command){
        this.groups = groups;
        this.command = command;
    }

    public Collection<ProcessGroupVO> getGroups() {
        return groups;
    }

    public void setGroups(Collection<ProcessGroupVO> groups) {
        this.groups = groups;
    }

    public GroupCommand getCommand() {
        return command;
    }

    public void setCommand(GroupCommand command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return "ProcessGroupWrapper{" +
                "groups=" + groups +
                ", command=" + command +
                "} ";
    }
}
