package ru.taskurotta.recipes.pcollection.model;

import java.util.UUID;

/**
 * Simple POJO object for tests
 * User: dimadin
 * Date: 22.07.13 11:43
 */
public class ModelObjectVO {

    private int id = UUID.randomUUID().hashCode();
    private UUID guid = UUID.randomUUID();
    private String value = "modelValue";
    private String additionalData = "someDataHere";

    public ModelObjectVO() {

    }

    public ModelObjectVO(int counter) {
        this();
        setValue("Valie-"+counter);
        setAdditionalData("Data-"+counter);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
    }

    public UUID getGuid() {
        return guid;
    }

    public void setGuid(UUID guid) {
        this.guid = guid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModelObjectVO)) return false;

        ModelObjectVO that = (ModelObjectVO) o;

        if (id != that.id) return false;
        if (additionalData != null ? !additionalData.equals(that.additionalData) : that.additionalData != null)
            return false;
        if (guid != null ? !guid.equals(that.guid) : that.guid != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (guid != null ? guid.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (additionalData != null ? additionalData.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ModelObjectVO{" +
                "id=" + id +
                ", guid=" + guid +
                ", value='" + value + '\'' +
                ", additionalData='" + additionalData + '\'' +
                "} " + super.toString();
    }

}
