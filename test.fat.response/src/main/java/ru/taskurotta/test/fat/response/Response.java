package ru.taskurotta.test.fat.response;

import java.io.Serializable;

/**
 * Created on 28.05.2015.
 */
public class Response implements Serializable {

    private int size;

    private String message;

    public Response(){}

    public Response(int size, String message) {
        this.size = size;
        this.message = message;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Response{" +
                "size=" + size +
                ", message='" + message + '\'' +
                '}';
    }
}
