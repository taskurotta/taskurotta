package ru.taskurotta.backend.dependency.links;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is not thread safe object. It should be synchronized with backend by version value.
 * <p/>
 * User: romario
 * Date: 4/5/13
 * Time: 11:35 AM
 */
@SuppressWarnings("UnusedDeclaration")
public class Graph implements Serializable {

    private final static Logger logger = LoggerFactory.getLogger(Graph.class);

    public static UUID[] EMPTY_ARRAY = new UUID[0];

    private int version = 0;
    private UUID graphId;       //should be equal to process ID

    /**
     * Map of all not finished items in this process and its time of start in milliseconds.
     * It has 0 value if item is not started yet.
     */
    private Map<UUID, Long> notFinishedItems = new HashMap<>();


    /**
     * Key UUID is a waiting items and value is Set of ready but frozen.
     */
    private Map<UUID, Set<UUID>> frozenReadyItems = new HashMap<>();

    /**
     * Links map where keys are tasks which depends from value set of other tasks.
     * For example, A(B, C) - A is a key and {B, C} is a set value of map.
     */
    private Map<UUID, Set<UUID>> links = new HashMap<>();

    //todo convention name
    private Set<UUID> finishedItems = new HashSet<>();


    // modification stuff.

    private Modification modification;
    private UUID[] readyItems;


    private long touchTimeMillis;
    private long lastApplyTimeMillis;

    /**
     * generic constructor for deserializer
     */
    public Graph() {
        touchTimeMillis = System.currentTimeMillis();
        lastApplyTimeMillis = 0;
    }

    /**
     * Create new graph
     *
     * @param graphId   - should be equal to process ID
     * @param startItem - ID of the first task in process
     */
    public Graph(UUID graphId, UUID startItem) {
        this.graphId = graphId;
        notFinishedItems.put(startItem, 0L);
    }


    private static Map<UUID, Set<UUID>> reverseIt(Map<UUID, Set<UUID>> links) {
        Map<UUID, Set<UUID>> reverseResult = new HashMap<>();

        if (links.isEmpty()) {
            return reverseResult;
        }

        for (UUID fromItem : links.keySet()) {
            for (UUID toItem : links.get(fromItem)) {

                Set<UUID> fromItems = reverseResult.get(toItem);
                if (fromItems == null) {
                    fromItems = new HashSet<>();
                    reverseResult.put(toItem, fromItems);
                }

                fromItems.add(fromItem);
            }
        }

        return reverseResult;
    }


    public int getVersion() {
        return version;
    }


    @JsonIgnore
    public Modification getModification() {
        return modification;
    }


    public boolean hasNotFinishedItem(UUID itemId) {
        return notFinishedItems.containsKey(itemId);
    }

    public Map<UUID, Long> getNotFinishedItems() {
        return notFinishedItems;
    }

    public Map<UUID, Set<UUID>> getFrozenReadyItems() {
        return frozenReadyItems;
    }

    public Map<UUID, Set<UUID>> getLinks() {
        return links;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setNotFinishedItems(Map<UUID, Long> notFinishedItems) {
        this.notFinishedItems = notFinishedItems;
    }

    public void setFrozenReadyItems(Map<UUID, Set<UUID>> frozenReadyItems) {
        this.frozenReadyItems = frozenReadyItems;
    }

    public void setLinks(Map<UUID, Set<UUID>> links) {
        this.links = links;
    }

    @JsonIgnore
    public UUID[] getReadyItems() {
        return readyItems;
    }

    public UUID getGraphId() {
        return graphId;
    }

    public void setGraphId(UUID graphId) {
        this.graphId = graphId;
    }

    @JsonIgnore
    public boolean isFinished() {
        return notFinishedItems.isEmpty();
    }

    public Set<UUID> getFinishedItems() {
        return finishedItems;
    }

    public void setFinishedItems(Set<UUID> finishedItems) {
        this.finishedItems = finishedItems;
    }

    /**
     * Apply changes to the graph
     *
     * @param modification - diff object to apply
     */
    public void apply(Modification modification) {

        logger.debug("apply() modification = [{}]", modification);
        long smt = System.currentTimeMillis();

        this.modification = modification;

        version++;

        UUID finishedItem = modification.getCompletedItem();

        // remove finished item from set
        notFinishedItems.remove(finishedItem);
        finishedItems.add(finishedItem);

        UUID waitForAfterRelease = modification.getWaitForAfterRelease();

        // add all new links to "links" map.
        // update reverse map with new links
        Map<UUID, Set<UUID>> reverseLinks = reverseIt(links);
        Map<UUID, Set<UUID>> newLinks = modification.getLinks();

        Collection<UUID> newItems = modification.getNewItems();
        if (newItems != null) {

            // add all new items to set
            for (UUID newItemId : newItems) {
                notFinishedItems.put(newItemId, 0L);
            }
        }

        // add all new links
        if (newLinks != null) {

            for (UUID item : newLinks.keySet()) {
                Set<UUID> newItemLinks = newLinks.get(item);

                for (UUID newItemLink : newItemLinks) {

                    // prevent link to already finished item.
                    // it is possible case for @NoWait Promise which are used on deep child task
                    if (!notFinishedItems.containsKey(newItemLink)) {
                        continue;
                    }

                    Set<UUID> itemLinks = links.get(item);

                    if (itemLinks == null) {
                        itemLinks = new HashSet<>();
                        links.put(item, itemLinks);
                    }

                    itemLinks.add(newItemLink);

                }

                // update reverse map
                for (UUID thatItem : newItemLinks) {
                    Set<UUID> reverseItemLinks = getOrCreateReverseItemLinks(reverseLinks, thatItem);
                    reverseItemLinks.add(item);
                }
            }
        }


        // remove finished item from all set
        // find items without dependencies
        Set<UUID> reverseItemLinks = reverseLinks.get(finishedItem);

        List<UUID> readyItemsList = null;

        if (reverseItemLinks != null) {

            for (UUID releaseCandidate : reverseItemLinks) {
                Set<UUID> candidateLinks = links.get(releaseCandidate);
                candidateLinks.remove(finishedItem);

                if (!candidateLinks.isEmpty()) {
                    continue;
                }

                // GC items without dependencies
                links.remove(releaseCandidate);

                // hide items to stash
                if (waitForAfterRelease != null) {

                    Set<UUID> frozenItemsSet = frozenReadyItems.get(waitForAfterRelease);

                    if (frozenItemsSet == null) {
                        frozenItemsSet = new HashSet<>();
                        frozenReadyItems.put(waitForAfterRelease, frozenItemsSet);
                    }

                    logger.debug("apply() after remove [{}] this item [{}] has no dependencies and will be frozen ",
                            finishedItem, releaseCandidate);

                    frozenItemsSet.add(releaseCandidate);

                    // or add it to ready list directly
                } else {
                    if (readyItemsList == null) {
                        readyItemsList = new LinkedList<>();
                    }

                    logger.debug("apply() after remove [{}], item [{}] has no dependencies and added to " +
                            "readyItemsList [{}]", finishedItem, releaseCandidate, readyItemsList);

                    readyItemsList.add(releaseCandidate);
                }
            }
        }

        // add all new items without links to readyItemsList
        if (newItems != null) {

            for (UUID newItem : newItems) {
                if (newLinks == null || newLinks.get(newItem) == null) {

                    if (readyItemsList == null) {
                        readyItemsList = new LinkedList<>();
                    }

                    logger.debug("apply() new item [{}] has no links and added to readyItemsList [{}]", newItem, readyItemsList);

                    readyItemsList.add(newItem);
                }
            }

        }

        // add all new items with links to already finished items
        // in "add all new links" section we skip it because it was links to items not in notFinishedItems Set
        if (newLinks != null) {

            for (UUID item : newLinks.keySet()) {
                Set<UUID> itemLinks = links.get(item);

                if (itemLinks == null || itemLinks.isEmpty()) {

                    if (readyItemsList == null) {
                        readyItemsList = new LinkedList<>();
                    }

                    logger.debug("apply() new item [{}] has link to already finished item", item);

                    readyItemsList.add(item);
                }
            }
        }

        // add all frozen items to ready items set
        Set<UUID> frozenItemsSet = frozenReadyItems.remove(finishedItem);
        if (frozenItemsSet != null) {

            // hide again this items to stash with new frozen dependency
            if (waitForAfterRelease != null) {

                logger.debug("apply() hide again this frozen items [{}]", frozenItemsSet);

                Set<UUID> newFrozenItemsSet = frozenReadyItems.get(waitForAfterRelease);

                if (newFrozenItemsSet == null) {
                    newFrozenItemsSet = new HashSet<>();
                    frozenReadyItems.put(waitForAfterRelease, newFrozenItemsSet);
                }

                newFrozenItemsSet.addAll(frozenItemsSet);

//                logger.debug("apply() hide again this frozen items [{}]", frozenItemsSet);
//
//                frozenReadyItems.put(waitForAfterRelease, frozenItemsSet);
            } else {


                if (readyItemsList == null) {
                    readyItemsList = new LinkedList<>();
                }

                logger.debug("apply() new frozen items [{}]", frozenItemsSet);

                readyItemsList.addAll(frozenItemsSet);
            }
        }


        // return empty or full array of new ready items.
        if (readyItemsList == null) {
            readyItems = EMPTY_ARRAY;
        } else {
            readyItems = readyItemsList.toArray(new UUID[readyItemsList.size()]);
            for (UUID readyItemsId : readyItems) {
                notFinishedItems.put(readyItemsId, smt);
            }
        }

        touchTimeMillis = lastApplyTimeMillis = smt;

    }

    private static Set<UUID> getOrCreateReverseItemLinks(Map<UUID, Set<UUID>> reverseLinks, UUID item) {

        Set<UUID> reverseItemLinks = reverseLinks.get(item);

        if (reverseItemLinks != null) {
            return reverseItemLinks;
        }

        reverseItemLinks = new HashSet<>();
        reverseLinks.put(item, reverseItemLinks);

        return reverseItemLinks;
    }


    public boolean isTaskWaitOtherTasks(UUID taskId, int taskQuantity) {
        Set<UUID> waitForTasks = links.get(taskId);

        logger.debug("waitForTasks = " + waitForTasks);

        if (waitForTasks == null) {
            return false;
        }

        //noinspection SimplifiableIfStatement
        if (taskQuantity == -1 && !waitForTasks.isEmpty()) {
            return true;
        }

        return waitForTasks.size() == taskQuantity;
    }

    public void clearFinishedItems() {
        finishedItems.clear();
    }

    /**
     * Method for creating copy of the graph
     *
     * @return copy of the current graph
     */
    public Graph copy() {
        final Graph copy = new Graph();
        copy.setGraphId(graphId);
        copy.setLinks(copyMapUuidWithSetOfUuid(links));
        copy.setNotFinishedItems(new HashMap<>(notFinishedItems));
        copy.setFrozenReadyItems(copyMapUuidWithSetOfUuid(frozenReadyItems));
        copy.setFinishedItems(new HashSet<>(finishedItems));
        copy.setVersion(version);
        copy.setTouchTimeMillis(touchTimeMillis);
        copy.setLastApplyTimeMillis(lastApplyTimeMillis);
        return copy;
    }

    private static Map<UUID, Set<UUID>> copyMapUuidWithSetOfUuid(Map<UUID, Set<UUID>> original) {
        final Map<UUID, Set<UUID>> copy = new HashMap<>();
        for (Map.Entry<UUID, Set<UUID>> entry : original.entrySet()) {
            Set<UUID> copyOfSet = new HashSet<>(entry.getValue());
            copy.put(entry.getKey(), copyOfSet);
        }
        return copy;
    }


    @Override
    public String toString() {
        return "Graph{" +
                "version=" + version +
                ", graphId=" + graphId +
                ", notFinishedItems=" + notFinishedItems +
                ", frozenReadyItems=" + frozenReadyItems +
                ", links=" + links +
                ", finishedItems=" + finishedItems +
                ", modification=" + modification +
                ", readyItems=" + Arrays.toString(readyItems) +
                ", touchTimeMillis=" + touchTimeMillis +
                ", lastApplyTimeMillis=" + lastApplyTimeMillis +
                '}';
    }


    public Collection<UUID> getProcessTasks() {
        Collection<UUID> allProcessTasks = new HashSet<>();

        allProcessTasks.addAll(notFinishedItems.keySet());
        allProcessTasks.addAll(finishedItems);

        return allProcessTasks;
    }

    public long getTouchTimeMillis() {
        return touchTimeMillis;
    }

    public void setTouchTimeMillis(long touchTimeMillis) {
        this.touchTimeMillis = touchTimeMillis;
    }

    public long getLastApplyTimeMillis() {
        return lastApplyTimeMillis;
    }

    public void setLastApplyTimeMillis(long lastApplyTimeMillis) {
        this.lastApplyTimeMillis = lastApplyTimeMillis;
    }
}

