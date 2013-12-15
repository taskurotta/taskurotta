package ru.taskurotta.service.dependency.links;

import java.io.Serializable;
import java.util.Collections;
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
            links = new HashMap<>();
        }

        Set<UUID> knownWaitForItems = links.get(thisItem);

        if (knownWaitForItems == null) {
            knownWaitForItems = new HashSet<>();
            links.put(thisItem, knownWaitForItems);
        }

		Collections.addAll(knownWaitForItems, waitForItems);
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
            newItems = new HashSet<>();
        }
        newItems.add(newItem);
    }

    public void setNewItems(Set<UUID> newItems) {
        this.newItems = newItems;
    }

    public void setLinks(Map<UUID, Set<UUID>> links) {
        this.links = links;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Modification that = (Modification) o;

        if (completedItem != null ? !completedItem.equals(that.completedItem) : that.completedItem != null)
            return false;
        if (links != null ? !links.equals(that.links) : that.links != null) return false;
        if (newItems != null ? !newItems.equals(that.newItems) : that.newItems != null) return false;
        if (waitForAfterRelease != null ? !waitForAfterRelease.equals(that.waitForAfterRelease) : that.waitForAfterRelease != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = completedItem != null ? completedItem.hashCode() : 0;
        result = 31 * result + (waitForAfterRelease != null ? waitForAfterRelease.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        result = 31 * result + (newItems != null ? newItems.hashCode() : 0);
        return result;
    }
}

