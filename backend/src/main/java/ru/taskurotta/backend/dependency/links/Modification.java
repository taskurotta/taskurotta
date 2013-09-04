package ru.taskurotta.backend.dependency.links;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * This is Not thread safe implementation.
 * <p/>
 * User: romario
 * Date: 4/5/13
 * Time: 10:44 AM
 */
public class Modification implements Serializable {

    private UUID completedItem;

    private UUID waitForAfterRelease;

    private Map<UUID, Set<UUID>> links;
    private Set<UUID> newItems;

    public void linkItem(UUID thisItem, UUID... waitForItems) {

        if (links == null) {
            links = new HashMap<UUID, Set<UUID>>();
        }

        Set<UUID> knownWaitForItems = links.get(thisItem);

        if (knownWaitForItems == null) {
            knownWaitForItems = new HashSet<UUID>();
            links.put(thisItem, knownWaitForItems);
        }

        for (UUID waitForItem : waitForItems) {
            knownWaitForItems.add(waitForItem);
        }

    }

    public UUID getCompletedItem() {
        return completedItem;
    }

    public void setCompletedItem(UUID completedItem) {
        this.completedItem = completedItem;
    }

    public Map<UUID, Set<UUID>> getLinks() {
        return links;
    }

    public Set<UUID> getNewItems() {
        return newItems;
    }

    public void addNewItem(UUID newItem) {
        if (newItems == null) {
            newItems = new HashSet<UUID>();
        }
        newItems.add(newItem);
    }

    public UUID getWaitForAfterRelease() {
        return waitForAfterRelease;
    }

    public void setWaitForAfterRelease(UUID waitForAfterRelease) {
        this.waitForAfterRelease = waitForAfterRelease;
    }

    @Override
    public String toString() {
        return "Modification{" +
                "completedItem=" + completedItem +
                ", waitForAfterRelease=" + waitForAfterRelease +
                ", links=" + links +
                ", newItems=" + newItems +
                '}';
    }
}

