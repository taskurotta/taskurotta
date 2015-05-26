package ru.taskurotta.dropwizard.resources.console;

import java.io.Serializable;

/**
 * Created on 25.05.2015.
 */
public class Status implements Serializable {

    private int code;
    private String msg;

    public Status(){}

    public Status(int code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "Status{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }
}
