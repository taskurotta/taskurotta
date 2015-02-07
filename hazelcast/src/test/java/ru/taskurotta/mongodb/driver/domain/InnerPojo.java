package ru.taskurotta.mongodb.driver.domain;

import java.util.UUID;

/**
 */
public class InnerPojo {
    String name;
    UUID id;

    public InnerPojo() {
    }

    public InnerPojo(String str2, UUID uuid2) {
        this.name = str2;
        this.id = uuid2;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InnerPojo innerPojo = (InnerPojo) o;

        if (id != null ? !id.equals(innerPojo.id) : innerPojo.id != null) return false;
        if (name != null ? !name.equals(innerPojo.name) : innerPojo.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
