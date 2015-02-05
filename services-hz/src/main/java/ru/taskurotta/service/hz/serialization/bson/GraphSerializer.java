package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.dependency.links.Graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by greg on 05/02/15.
 */
public class GraphSerializer implements StreamBSerializer<Graph> {

    public static final CString VERSION = new CString("ver");
    public static final CString GRAPH_ID = new CString("gId");
    public static final CString NOT_FINISHED = new CString("notFin");
    public static final CString KEY = new CString("key");
    public static final CString VALUE = new CString("val");
    public static final CString LINKS = new CString("links");
    public static final CString FINISHED = new CString("fin");
    public static final CString LAST_APPLY_TIME = new CString("lastAppTime");

    @Override
    public Class<Graph> getObjectClass() {
        return Graph.class;
    }

    @Override
    public void write(BDataOutput out, Graph object) {
        out.writeInt(VERSION, object.getVersion());
        out.writeUUID(GRAPH_ID, object.getGraphId());

        Map<UUID, Long> notFinishedItems = object.getNotFinishedItems();

        int notFinishedLabel = out.writeArray(NOT_FINISHED);
        int i = 0;
        for (Map.Entry<UUID, Long> entry : notFinishedItems.entrySet()) {
            int entryLabel = out.writeObject(SerializerTools.createCString(i));
            out.writeUUID(KEY, entry.getKey());
            out.writeLong(VALUE, entry.getValue());
            out.writeObjectStop(entryLabel);
            i++;
        }
        out.writeArrayStop(notFinishedLabel);

        serializeLinks(out, object.getLinks());

        Set<UUID> finishedItems = object.getFinishedItems();
        int finishedArrayLabel = out.writeArray(FINISHED);
        i = 0;
        for (UUID uuid : finishedItems) {
            out.writeUUID(SerializerTools.createCString(i), uuid);
            i++;
        }
        out.writeArrayStop(finishedArrayLabel);

        out.writeLong(LAST_APPLY_TIME, object.getLastApplyTimeMillis());

    }

    @Override
    public Graph read(BDataInput in) {
        int version = in.readInt(VERSION);
        UUID graphId = in.readUUID(GRAPH_ID);
        int notFinishedLabel = in.readArray(NOT_FINISHED);
        Map<UUID, Long> notFinishedItems = null;
        if (notFinishedLabel > 0) {
            int notFinishedArraySize = in.readArraySize();
            notFinishedItems = new HashMap<>(notFinishedArraySize);
            for (int i = 0; i < notFinishedArraySize; i++) {
                int objRead = in.readObject(SerializerTools.createCString(i));
                UUID key = in.readUUID(KEY);
                Long value = in.readLong(VALUE);
                notFinishedItems.put(key, value);
                in.readObjectStop(objRead);
            }
            in.readArrayStop(notFinishedLabel);
        }
        Map<UUID, Set<UUID>> links = deserializeLinks(in);
        int finishedLabel = in.readArray(FINISHED);
        Set<UUID> finishedItems = null;
        if (finishedLabel > 0) {
            int finishedSize = in.readArraySize();
            finishedItems = new HashSet<>(finishedSize);
            for (int i = 0; i < finishedSize; i++) {
                finishedItems.add(in.readUUID(SerializerTools.createCString(i)));
            }
        }
        long lastApplyTimeMillis = in.readLong(LAST_APPLY_TIME);
        return new Graph(version, graphId, notFinishedItems, links, finishedItems, lastApplyTimeMillis);
    }

    protected static void serializeLinks(BDataOutput out, Map<UUID, Set<UUID>> links) {
        if (links != null) {
            int rootArrayLabel = out.writeArray(LINKS);
            int i = 0;
            for (Map.Entry<UUID, Set<UUID>> entry : links.entrySet()) {
                int rootObjLabel = out.writeObject(SerializerTools.createCString(i));
                out.writeUUID(KEY, entry.getKey());
                Set<UUID> set = entry.getValue();
                int valueLabel = out.writeArray(VALUE);
                int j = 0;
                for (UUID uuid : set) {
                    out.writeUUID(new CString(Integer.toString(j)), uuid);
                    j++;
                }
                out.writeArrayStop(valueLabel);
                out.writeObjectStop(rootObjLabel);
                i++;
            }
            out.writeArrayStop(rootArrayLabel);
        }

    }

    protected static Map<UUID, Set<UUID>> deserializeLinks(BDataInput in) {
        int rootArrayLabel = in.readArray(LINKS);
        Map<UUID, Set<UUID>> links = null;
        if (rootArrayLabel > 0) {
            int mapSize = in.readArraySize();
            links = new HashMap<>(mapSize);
            for (int i = 0; i < mapSize; i++) {
                int rootObjLabel = in.readObject(SerializerTools.createCString(i));
                UUID key = in.readUUID(KEY);
                Set<UUID> set = null;
                int valueLabel = in.readArray(VALUE);
                if (valueLabel > 0) {
                    int valueSize = in.readArraySize();
                    set = new HashSet<>(valueSize);
                    for (int j = 0; j < valueSize; j++) {
                        set.add(in.readUUID(new CString(Integer.toString(j))));
                    }
                    in.readArrayStop(valueLabel);
                }
                links.put(key, set);
                in.readObjectStop(rootObjLabel);

            }
            in.readArrayStop(rootArrayLabel);
        }

        return links;
    }


}
