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
           logger.warn("Cannot release node for id[" + id + "], processId[" + processId + "]: node not found in storage!");
            return false;
        }
        return true;
    }

    @Override
    public boolean scheduleNode(UUID id, UUID processId) {
        TaskNode taskNode = getNode(id, processId);
        if(taskNode!=null) {
            if(taskNode.isScheduled()) {//was previously scheduled
                return false;
            }
            taskNode.setScheduled(true);
            simpleStorage.put(id, taskNode);
            logger.debug("Node scheduled id[{}], processId[{}]", id, processId);
        } else {
            logger.error("Cannot schedule node for id["+id+"], processId["+processId+"]: node not found in storage!");
            return false;
        }
        return true;
    }

    @Override
    public List<TaskNode> getActiveProcessNodes(UUID processId) {
        List<TaskNode> result = new ArrayList<>();
        for(TaskNode node: simpleStorage.values()) {
            if(processId.equals(node.getProcessId()) && !node.isReleased()) {
                 result.add(node);
            }
        }
        logger.debug("Got [{}] nodes for process[{}]", result.size(), processId);
        return result;
    }

    @Override
    public int deleteProcessNodes(UUID processId) {
        int result = 0;
        for(UUID nodeId: simpleStorage.keySet()) {
            TaskNode node = simpleStorage.get(nodeId);
            if(processId.equals(node.getProcessId())) {
                simpleStorage.remove(nodeId);
                result++;
            }
        }
        return result;
    }

}
