package ru.taskurotta.hazelcast;

/**
 * Date: 17.02.14 12:38
 */
public interface ItemIdAware {

    /**
     * @return min id value of the stored items
     */
    public long getMinItemId();

}
