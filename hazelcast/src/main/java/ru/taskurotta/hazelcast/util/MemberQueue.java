package ru.taskurotta.hazelcast.util;

import com.hazelcast.core.*;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.LocalCachedQueueStats;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * User: romario
 * Date: 8/15/13
 * Time: 12:40 PM
 */
public class MemberQueue<E> implements CachedQueue<E> {

    private static final char MAP_INDEX_DELIMITER = '_';

    private Map<Integer, CachedQueue<E>> id2queue;
    private volatile MemberState state;

    private String name;
    private HazelcastInstance hzInstance;
    private com.hazelcast.core.PartitionService partitionService;

    private String partiotionKey;

    private class MemberState {

        Set<Integer> allMemberPartitions;
        int[] memberPartitions;
        volatile int counter = 0;

        @Override
        public String toString() {
            return "MemberState{" +
                    "memberPartitions=" + Arrays.toString(memberPartitions) +
                    ", counter=" + counter +
                    '}';
        }
    }

    public MemberQueue(String name, HazelcastInstance hzInstance) {

        this.name = name;
        this.hzInstance = hzInstance;
        this.partitionService = hzInstance.getPartitionService();

        hzInstance.getPartitionService().addMigrationListener(new MigrationListener() {
            @Override
            public void migrationStarted(MigrationEvent migrationEvent) {
                updateState();
            }

            @Override
            public void migrationCompleted(MigrationEvent migrationEvent) {
                updateState();
            }

            @Override
            public void migrationFailed(MigrationEvent migrationEvent) {
                updateState();
            }
        });

        updateState();

        // cache all partition queues
        id2queue = new HashMap<>();
        int maxPartitionsIndex = partitionService.getPartitions().size();
        for (int i = 0; i < maxPartitionsIndex; i++) {

            CachedQueue queue = hzInstance.getDistributedObject(CachedQueue.class.getName(), name + MAP_INDEX_DELIMITER + i);
            id2queue.put(i, queue);

            // progrev! :(

            queue.add(1L);
            queue.add(2L);
            queue.add(3L);
        }
    }


    private void updateState() {

        MemberState newState = new MemberState();

        newState.allMemberPartitions = new HashSet<>();

        // WARN: partition owner can be changed during loop and we can lost our partition or get not our.
        for (Partition partition : partitionService.getPartitions()) {

            if (partition.getOwner().localMember()) {
                newState.allMemberPartitions.add(partition.getPartitionId());
            }
        }

        newState.memberPartitions = new int[newState.allMemberPartitions.size()];

        int i = 0;
        for (int partitionId : newState.allMemberPartitions) {
            newState.memberPartitions[i++] = partitionId;
        }

        state = newState;

    }

    public boolean isPartitionOwner(int partitionId) {
        return state.allMemberPartitions.contains(partitionId);
    }

    @Override
    public String toString() {
        return "MemberQueue{" +
                "state=" + state +
                ", name='" + name + '\'' +
                '}';
    }


    @Override
    public boolean offer(E e) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public E remove() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public E poll() {

        MemberState st = state;
        int memberPartitionsQuantity = st.memberPartitions.length;


        for (int i = 0; i < memberPartitionsQuantity; i++) {
            int partitionId = st.memberPartitions[st.counter++ % memberPartitionsQuantity];

            CachedQueue queue = id2queue.get(partitionId);

            Object item = queue.poll();

            if (item != null) {
                return (E) item;
            }
        }


        return null;
    }

    @Override
    public E element() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public E peek() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public E take() throws InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getServiceName() {
        return null;
    }


    @Override
    public int size() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LocalCachedQueueStats getLocalQueueStats() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean contains(Object o) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Iterator<E> iterator() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object[] toArray() {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean add(E e) {

//        long smt = System.currentTimeMillis();

        Partition partition = partitionService.getPartition(e);

//        System.err.println("MemberQueue.add() - getPartition partition = " + partition + ". time: " + (System
//                .currentTimeMillis() - smt));
//
//        smt = System.currentTimeMillis();

        int partitionId = partition.getPartitionId();

//        System.err.println("MemberQueue.add() - getPartitionId = " + partitionId + ". time: " + (System
//                .currentTimeMillis() - smt));
//
//        smt = System.currentTimeMillis();

        CachedQueue partitionQueue = id2queue.get(partitionId);

//        System.err.println("MemberQueue.add() - get partitionQueue = " + partitionQueue + ". time: " + (System
//                .currentTimeMillis() - smt));
//
//
//        smt = System.currentTimeMillis();

        boolean isOk = partitionQueue.add(e);

//        System.err.println("MemberQueue.add() - put value to partitionQueue = " + partitionQueue + ". time: " +
//                (System
//                .currentTimeMillis() - smt));

        return isOk;
    }

    @Override
    public boolean remove(Object o) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void clear() {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    //    @Override
    public String getPartitionKey() {
        return partiotionKey;
    }

    public void setPartiotionKey(String partiotionKey) {
        this.partiotionKey = partiotionKey;
    }
}
