package ru.taskurotta.server.json;

/**
 * Created by void 18.07.13 14:34
 */
public class ParentObject {
    String name;
    TestObject child;

    public ParentObject(String name, TestObject child) {
        this.name = name;
        this.child = child;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TestObject getChild() {
        return child;
    }

    public void setChild(TestObject child) {
        this.child = child;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParentObject that = (ParentObject) o;

        if (child != null ? !child.equals(that.child) : that.child != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (child != null ? child.hashCode() : 0);
        return result;
    }
}
