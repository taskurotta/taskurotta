package ru.taskurotta.service.notification.model;

import java.io.Serializable;
import java.net.URI;

/**
 * Created on 08.06.2015.
 */
public class EmailAttach implements Serializable {

    private String name;
    private URI location;
    private byte[] content;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getLocation() {
        return location;
    }

    public void setLocation(URI location) {
        this.location = location;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "EmailAttach{" +
                "name='" + name + '\'' +
                ", location=" + location +
                ", contentLength=" + (content!=null? content.length: null) +
                '}';
    }

}
