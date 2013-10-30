package ru.taskurotta.backend.dependency.links;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * smart constructor for deserializer
     */
    public Graph(int version, UUID graphId, Map<UUID, Long> notFinishedItems, Map<UUID, Set<UUID>> links,
                 Set<UUID> finishedItems, long lastApplyTimeMillis) {

        this.version = version;
        this.graphId = graphId;

        if (notFinishedItems != null) {
            this.notFinishedItems = notFinishedItems;
        }

        if (links != null) {
            this.links = links;
        }

        if (finishedItems != null) {
            this.finishedItems = finishedItems;
        }

        this.lastApplyTimeMillis = lastApplyTimeMillis;
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
        for (Map.Entry<UUID, Set<UUID>> entry : links.entrySet()) {
            for (UUID toItem : entry.getValue()) {
                Set<UUID> fromItems = reverseResult.get(toItem);
                if (fromItems == null) {
                    fromItems = new HashSet<>();
                    reverseResult.put(toItem, fromItems);
                }

                fromItems.add(entry.getKey());
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

    public Map<UUID, Set<UUID>> getLinks() {
        return links;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setNotFinishedItems(Map<UUID, Long> notFinishedItems) {
        this.notFinishedItems = notFinishedItems;
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
        long currentTime = System.currentTimeMillis();
        logger.debug("apply() modification = [{}]", modification);

        this.modification = modification;
        version++;

        // process finished item
        UUID finishedItem = modification.getCompletedItem();
        notFinishedItems.remove(finishedItem);
        finishedItems.add(finishedItem);

        // get new items without dependencies
        List<UUID> readyItemsList = applyNewItems();

        // update links collection and get release candidates
        Set<UUID> reverseItemLinks = updateLinks();

        // update readyItemList with old items that become ready
        findReadyItems(readyItemsList, reverseItemLinks);

        // return empty or full array of new ready items.
        readyItems = readyItemsList.toArray(new UUID[readyItemsList.size()]);
        for (UUID readyItemsId : readyItems) {
            notFinishedItems.put(readyItemsId, currentTime);
        }

        touchTimeMillis = lastApplyTimeMillis = currentTime;
    }

    private List<UUID> applyNewItems() {
        List<UUID> readyItemsList = new LinkedList<>();

        Collection<UUID> newItems = modification.getNewItems();

        if (newItems != null) {

            // add all new items to set
            for (UUID newItemId : newItems) {
                notFinishedItems.put(newItemId, 0L);
            }

            // add all new items without links to readyItemsList
            Map<UUID, Set<UUID>> newLinks = modification.getLinks();
            for (UUID newItem : newItems) {
                if (newLinks == null || newLinks.get(newItem) == null) {
                    logger.debug("apply() new item [{}] has no links and added to readyItemsList [{}]", newItem, readyItemsList);

                    readyItemsList.add(newItem);
                }
            }
        }
        return readyItemsList;
    }

    /**
     * add all new links to "links" map. update reverseLinks
     *
     * @return set of items dependent from finished one
     */
    private Set<UUID> updateLinks() {

        // update reverse map with new links
        Map<UUID, Set<UUID>> reverseLinks = reverseIt(links);

        Map<UUID, Set<UUID>> newLinks = modification.getLinks();

        if (newLinks != null) {

            for (Map.Entry<UUID, Set<UUID>> entry : newLinks.entrySet()) {
                Set<UUID> newItemLinks = entry.getValue();

                for (UUID newItemLink : newItemLinks) {

                    // prevent link to already finished item.
                    // it is possible case for @NoWait Promise which are used on deep child task
                    if (!notFinishedItems.containsKey(newItemLink)) {
                        continue;
                    }

                    Set<UUID> itemLinks = links.get(entry.getKey());

                    if (itemLinks == null) {
                        itemLinks = new HashSet<>();
                        links.put(entry.getKey(), itemLinks);
                    }

                    itemLinks.add(newItemLink);
                }

                // update reverse map
                for (UUID thatItem : newItemLinks) {
                    Set<UUID> reverseItemLinks = getOrCreateReverseItemLinks(reverseLinks, thatItem);
                    reverseItemLinks.add(entry.getKey());
                }
            }
        }
        return reverseLinks.get(modification.getCompletedItem());
    }

    /**
     * remove finished item from all set.
     * find items without dependencies.
     *
     * @param readyItemsList   - collection for ready items found
     * @param reverseItemLinks - collection of release candidates
     */
    private void findReadyItems(List<UUID> readyItemsList, Set<UUID> reverseItemLinks) {

        if (reverseItemLinks == null) {
            return;
        }

        UUID finishedItem = modification.getCompletedItem();

        for (UUID releaseCandidate : reverseItemLinks) {
            Set<UUID> candidateLinks = links.get(releaseCandidate);
            candidateLinks.remove(finishedItem);

            UUID dependencySubstitution = modification.getWaitForAfterRelease();
            // update changed dependency
            if (dependencySubstitution != null) {
                candidateLinks.add(dependencySubstitution);
            }

            if (candidateLinks.isEmpty()) {
                // GC items without dependencies
                links.remove(releaseCandidate);

                logger.debug("apply() after remove [{}], item [{}] has no dependencies and added to" +
                        " readyItemsList [{}]", finishedItem, releaseCandidate, readyItemsList);

                readyItemsList.add(releaseCandidate);
            }
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Graph)) return false;

        Graph graph = (Graph) o;

        if (lastApplyTimeMillis != graph.lastApplyTimeMillis) return false;
        if (touchTimeMillis != graph.touchTimeMillis) return false;
        if (version != graph.version) return false;
        if (finishedItems != null ? !finishedItems.equals(graph.finishedItems) : graph.finishedItems != null) {
            return false;
        }
        if (graphId != null ? !graphId.equals(graph.graphId) : graph.graphId != null) return false;
        if (links != null ? !links.equals(graph.links) : graph.links != null) return false;
        if (notFinishedItems != null ? !notFinishedItems.equals(graph.notFinishedItems) : graph.notFinishedItems != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = version;
        result = 31 * result + (graphId != null ? graphId.hashCode() : 0);
        result = 31 * result + (notFinishedItems != null ? notFinishedItems.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        result = 31 * result + (finishedItems != null ? finishedItems.hashCode() : 0);
        result = 31 * result + (int) (touchTimeMillis ^ (touchTimeMillis >>> 32));
        result = 31 * result + (int) (lastApplyTimeMillis ^ (lastApplyTimeMillis >>> 32));
        return result;
    }
}

