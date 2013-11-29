package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.backend.dependency.links.Graph;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * User: romario
 * Date: 9/12/13
 * Time: 2:23 PM
 */
public class GraphStreamSerializer implements StreamSerializer<Graph> {

    public static void serializeLinks(ObjectDataOutput out, Map<UUID, Set<UUID>> links) throws IOException {
        if (links == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(links.size());
            for (Map.Entry<UUID, Set<UUID>> entry : links.entrySet()) {
                UUIDSerializer.write(out, entry.getKey());

                Set<UUID> set = entry.getValue();
                out.writeInt(set.size());
                for (UUID uuid : set) {
                    UUIDSerializer.write(out, uuid);
                }
            }
        }
    }


    public static Map<UUID, Set<UUID>> deserializeLinks(ObjectDataInput in) throws IOException {
        int mapSize = in.readInt();
        if (mapSize == -1) {
            return null;
        }


        Map<UUID, Set<UUID>> links = new HashMap<>(mapSize);
        while (mapSize > 0) {
            UUID key = UUIDSerializer.read(in);

            int setSize = in.readInt();
            Set<UUID> set = new HashSet<>(setSize);
            while (setSize > 0) {
                set.add(UUIDSerializer.read(in));

                setSize--;
            }

            links.put(key, set);

            mapSize--;
        }

        return links;

    }

    @Override
    public void write(ObjectDataOutput out, Graph graph) throws IOException {
        out.writeInt(graph.getVersion());
        UUIDSerializer.write(out, graph.getGraphId());

        Map<UUID, Long> notFinishedItems = graph.getNotFinishedItems();
        out.writeInt(notFinishedItems.size());

        for (Map.Entry<UUID, Long> entry : notFinishedItems.entrySet()) {
            UUIDSerializer.write(out, entry.getKey());
            out.writeLong(entry.getValue());
        }

        serializeLinks(out, graph.getLinks());

        Set<UUID> finishedItems = graph.getFinishedItems();
        out.writeInt(finishedItems.size());
        for (UUID uuid : finishedItems) {
            UUIDSerializer.write(out, uuid);
        }

        out.writeLong(graph.getLastApplyTimeMillis());
    }

    @Override
    public Graph read(ObjectDataInput in) throws IOException {

        int version = in.readInt();
        UUID graphId = UUIDSerializer.read(in);

        int mapSize = in.readInt();
        Map<UUID, Long> notFinishedItems = new HashMap<>(mapSize);

        while (mapSize > 0) {
            UUID key = UUIDSerializer.read(in);
            long value = in.readLong();
            notFinishedItems.put(key, value);

            mapSize--;
        }

        Map<UUID, Set<UUID>> links = deserializeLinks(in);

        int setSize = in.readInt();

        Set<UUID> finishedItems = new HashSet<>(setSize);
        while (setSize > 0) {
            finishedItems.add(UUIDSerializer.read(in));

            setSize--;
        }

        long lastApplyTimeMillis = in.readLong();

        return new Graph(version, graphId, notFinishedItems, links, finishedItems, lastApplyTimeMillis);
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.GRAPH;
    }

    @Override
    public void destroy() {
    }
}