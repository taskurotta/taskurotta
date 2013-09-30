package ru.taskurotta.schedule.storage.hz;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IdGenerator;
import ru.taskurotta.schedule.storage.JobStore;
import ru.taskurotta.schedule.JobVO;

import java.util.Collection;

/**
 * Hazelcast implementation of Scheduled task storage
 * User: dimadin
 * Date: 23.09.13 11:20
 */
@Deprecated //in favour of some persistent storage implementation
public class HzJobStore implements JobStore {

    private HazelcastInstance hzInstance;
    private IMap<Long, JobVO> taskStore;
    private String jobStoreMapName = "jobStoreMap";
    private ILock jobStoreLock;
    private IdGenerator idGenerator;

    public HzJobStore(HazelcastInstance hzInstance, String jobStoreMapName) {
        this.hzInstance = hzInstance;
        this.jobStoreMapName = jobStoreMapName;
        taskStore = hzInstance.getMap(this.jobStoreMapName);
        jobStoreLock = hzInstance.getLock(this.jobStoreMapName);
        idGenerator = hzInstance.getIdGenerator(this.jobStoreMapName +"id.generator");
    }

    @Override
    public long addJob(JobVO task) {
        try {
            jobStoreLock.lock();

            if (task.getId()<0) {
                task.setId(idGenerator.newId());
            } else if (taskStore.containsKey(task.getId())) {
                throw new IllegalArgumentException("Scheduled task with id["+task.getId()+"] already stored");
            }

            taskStore.put(task.getId(), task);
        } finally {
            jobStoreLock.unlock();
        }
        return task.getId();
    }

    @Override
    public void removeJob(long id) {
        taskStore.remove(id);
    }

    @Override
    public Collection<Long> getJobIds() {
        return taskStore.keySet();
    }

    @Override
    public JobVO getJob(long id) {
        return taskStore.get(id);
    }

    @Override
    public void updateJobStatus(long id, int status) {
        try {
            jobStoreLock.lock();

            JobVO item= taskStore.get(id);
            item.setStatus(status);
            taskStore.put(id, item);

        } finally {
            jobStoreLock.unlock();
        }

    }


}
