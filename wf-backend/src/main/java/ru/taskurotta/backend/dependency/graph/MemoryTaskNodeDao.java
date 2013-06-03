package ru.taskurotta.backend.dependency.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In memory implementation of TaskNodeDao
 * User: dimadin
 * Date: 03.06.13 12:09
 */
public class MemoryTaskNodeDao implements TaskNodeDao {

    private static final Logger logger = LoggerFactory.getLogger(MemoryTaskNodeDao.class);

    //TODO: Break into separate storages per process?
    private Map<UUID, TaskNode> simpleStorage = new ConcurrentHashMap<>();

    @Override
    public TaskNode getNode(UUID taskId, UUID processId) {
        return simpleStorage.get(taskId);
    }

    @Override
    public synchronized void addNode(TaskNode taskNode) {
        TaskNode node = simpleStorage.get(taskNode.getId());
        if(node!=null) { //Node already exists
            return;
        } else{
            simpleStorage.put(taskNode.getId(), taskNode);
            logger.debug("Added node [{}]", taskNode);
        }
    }

    @Override
    public boolean releaseNode(UUID id, UUID processId) {
        TaskNode taskNode = getNode(id, processId);
        if(taskNode!=null) {
            if(taskNode.isReleased()) {//was previously released
                return false;
            }
            taskNode.setReleased(true);
            simpleStorage.put(id, taskNode);
            logger.debug("Node released id[{}], processId[{}]", id, processId);
        } else {
           logger.error("Cannot release node for id["+id+"], processId["+processId+"]: node not found in storage!");
           throw new IllegalStateException("Cannot release node for id["+id+"], processId["+processId+"]: node not found in storage!");
        }
        return true;
    }

    @Override
    public UUID[] getReadyTasks(UUID processId) {
        List<UUID> result = new ArrayList<>();
        List<TaskNode> nodes = getProcessNodes(processId);

        for(TaskNode node: nodes) {
            if(!node.isReleased() && isAllReady(node.getDepends(), processId)) {
                result.add(node.getId());
            }
        }

        UUID[] resultAsArray = result.toArray(new UUID[result.size()]);
        logger.debug("Ready tasks for processId[{}] are[{}]", processId, resultAsArray);

        return resultAsArray;
    }

    @Override
    public boolean isProcessReady(UUID processId) {
        boolean result = true;
        List<TaskNode> nodes = getProcessNodes(processId);

        if(nodes!=null && !nodes.isEmpty()) {
            for(TaskNode node: nodes) {
                if(!node.isReleased()) {
                    result = false;
                    break;
                }
            }
        }

        logger.debug("Process[{}] ready is[{}]", processId, result);
        return result;
    }

    private boolean isAllReady(List<UUID> nodeIds, UUID processId) {
        boolean result = true;//null or empty nodeIds list means AllReady==true

        if(nodeIds!=null && !nodeIds.isEmpty()) {
            for(UUID nodeId: nodeIds) {
                TaskNode node = getNode(nodeId, processId);
                if(node!=null && !node.isReleased()) {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }

    private List<TaskNode> getProcessNodes(UUID processId) {
        List<TaskNode> result = new ArrayList<>();
        for(TaskNode node: simpleStorage.values()) {
            if(processId.equals(node.getProcessId())) {
                 result.add(node);
            }
        }
        logger.debug("Got [{}] nodes for process[{}]", result.size(), processId);
        return result;
    }

}
