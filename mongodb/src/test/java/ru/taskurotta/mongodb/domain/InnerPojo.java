package ru.taskurotta.mongodb.domain;

import java.util.UUID;

/**
 */
public class InnerPojo {
    String name = "dsdsfdsf";
    UUID id = UUID.randomUUID();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
