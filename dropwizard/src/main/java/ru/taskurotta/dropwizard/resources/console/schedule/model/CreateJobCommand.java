package ru.taskurotta.dropwizard.resources.console.schedule.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ru.taskurotta.service.schedule.JobConstants;

import java.io.Serializable;

/**
 * Created on 17.07.2014.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateJobCommand extends TaskCommand implements Serializable {

    private long id = -1l;
    private String name;
    private String cron;
    private int maxErrors = JobConstants.DEFAULT_MAX_CONSEQUENTIAL_ERRORS;
    private int status = JobConstants.STATUS_UNDEFINED;
    private int limit;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public int getMaxErrors() {
        return maxErrors;
    }

    public void setMaxErrors(int maxErrors) {
        this.maxErrors = maxErrors;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public String toString() {
        return "CreateJobCommand{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", cron='" + cron + '\'' +
                ", maxErrors=" + maxErrors +
                ", status=" + status +
                ", limit=" + limit +
                "} " + super.toString();
    }

}
