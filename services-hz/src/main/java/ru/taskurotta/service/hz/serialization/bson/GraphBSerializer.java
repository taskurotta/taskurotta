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

public class GraphBSerializer implements StreamBSerializer<Graph> {

    public static final CString VERSION = new CString("version");
    public static final CString NOT_FINISHED = new CString("notFinished");
    public static final CString KEY = new CString("k");
    public static final CString VALUE = new CString("v");
    public static final CString LINKS = new CString("links");
    public static final CString FINISHED = new CString("finished");
    public static final CString LAST_APPLY_TIME = new CString("lastApplyTime");
    public static final CString LAST_TOUCH_TIME = new CString("lastTouchTime");


    @Override
    public Class<Graph> getObjectClass() {
        return Graph.class;
    }

    @Override
    public void write(BDataOutput out, Graph object) {
        out.writeUUID(_ID, object.getGraphId());
        out.writeInt(VERSION, object.getVersion());

        Map<UUID, Long> notFinishedItems = object.getNotFinishedItems();
        if (notFinishedItems.size() != 0) {
            int i = 0;
            int notFinishedLabel = out.writeArray(NOT_FINISHED);
            for (Map.Entry<UUID, Long> entry : notFinishedItems.entrySet()) {
                int entryLabel = out.writeObject(i);
                out.writeUUID(KEY, entry.getKey());
                out.writeLong(VALUE, entry.getValue(), 0l);
                out.writeObjectStop(entryLabel);
                i++;
            }
            out.writeArrayStop(notFinishedLabel);
        }

        serializeLinks(out, object.getLinks());

        Set<UUID> finishedItems = object.getFinishedItems();
        if (finishedItems.size() != 0) {
            int finishedArrayLabel = out.writeArray(FINISHED);
            int i = 0;
            for (UUID uuid : finishedItems) {
                out.writeUUID(i, uuid);
                i++;
            }
            out.writeArrayStop(finishedArrayLabel);
        }

        out.writeLong(LAST_APPLY_TIME, object.getLastApplyTimeMillis());
        out.writeLong(LAST_TOUCH_TIME, object.getTouchTimeMillis());
    }

    @Override
    public Graph read(BDataInput in) {
        UUID graphId = in.readUUID(_ID);
        int version = in.readInt(VERSION);

        int notFinishedLabel = in.readArray(NOT_FINISHED);
        Map<UUID, Long> notFinishedItems = null;
        if (notFinishedLabel != -1) {
            int notFinishedArraySize = in.readArraySize();
            notFinishedItems = new HashMap<>(notFinishedArraySize);
            for (int i = 0; i < notFinishedArraySize; i++) {
                int objRead = in.readObject(i);
                UUID key = in.readUUID(KEY);
                Long value = in.readLong(VALUE, 0l);
                notFinishedItems.put(key, value);
                in.readObjectStop(objRead);
            }
            in.readArrayStop(notFinishedLabel);
        }

        Map<UUID, Set<UUID>> links = deserializeLinks(in);

        int finishedLabel = in.readArray(FINISHED);
        Set<UUID> finishedItems = null;
        if (finishedLabel != -1) {
            int finishedSize = in.readArraySize();
            finishedItems = new HashSet<>(finishedSize);
            for (int i = 0; i < finishedSize; i++) {
                finishedItems.add(in.readUUID(i));
            }
            in.readArrayStop(finishedLabel);
        }

        long lastApplyTimeMillis = in.readLong(LAST_APPLY_TIME);
        long lastTouchTimeMillis = in.readLong(LAST_TOUCH_TIME);

        return new Graph(version, graphId, notFinishedItems, links, finishedItems, lastApplyTimeMillis, lastTouchTimeMillis);
    }

    protected static void serializeLinks(BDataOutput out, Map<UUID, Set<UUID>> links) {
        if (links == null || links.size() == 0) {
            return;
        }

        int rootArrayLabel = out.writeArray(LINKS);
        int i = 0;
        for (Map.Entry<UUID, Set<UUID>> entry : links.entrySet()) {
            int rootObjLabel = out.writeObject(i);
            out.writeUUID(KEY, entry.getKey());
            Set<UUID> set = entry.getValue();
            {
                int valueLabel = out.writeArray(VALUE);
                int j = 0;
                for (UUID uuid : set) {
                    out.writeUUID(j, uuid);
                    j++;
                }
                out.writeArrayStop(valueLabel);
            }
            out.writeObjectStop(rootObjLabel);
            i++;
        }
        out.writeArrayStop(rootArrayLabel);

    }

    protected static Map<UUID, Set<UUID>> deserializeLinks(BDataInput in) {
        int rootArrayLabel = in.readArray(LINKS);
        if (rootArrayLabel == -1) {
            return new HashMap<>();
        }

        int mapSize = in.readArraySize();

        Map<UUID, Set<UUID>> links = new HashMap<>(mapSize);
        for (int i = 0; i < mapSize; i++) {
            int rootObjLabel = in.readObject(i);
            UUID key = in.readUUID(KEY);
            {
                int valueLabel = in.readArray(VALUE);
                int valueSize = in.readArraySize();

                Set<UUID> set = new HashSet<>(valueSize);
                for (int j = 0; j < valueSize; j++) {
                    set.add(in.readUUID(j));
                }
                in.readArrayStop(valueLabel);
                links.put(key, set);
            }
            in.readObjectStop(rootObjLabel);

        }
        in.readArrayStop(rootArrayLabel);

        return links;
    }


}
