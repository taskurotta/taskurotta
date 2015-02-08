package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.dependency.links.Modification;
import ru.taskurotta.service.hz.dependency.HzGraphDao;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DecisionRowBSerializer implements StreamBSerializer<HzGraphDao.DecisionRow> {

    public static final CString WAIT_FOR_AFTER_RELEASE = new CString("wait");
    public static final CString NEW_ITEMS = new CString("newItems");
    public static final CString READY_ITEMS = new CString("readyItems");

    @Override
    public Class<HzGraphDao.DecisionRow> getObjectClass() {
        return HzGraphDao.DecisionRow.class;
    }

    @Override
    public void write(BDataOutput out, HzGraphDao.DecisionRow decisionRow) {
        out.writeUUID(_ID, decisionRow.getItemId());

        Modification modification = decisionRow.getModification();
        out.writeUUID(WAIT_FOR_AFTER_RELEASE, modification.getWaitForAfterRelease());

        Set<UUID> newItems = modification.getNewItems();
        if (newItems != null) {
            int newItemsLabel = out.writeArray(NEW_ITEMS);
            int i = 0;
            for (UUID item : newItems) {
                out.writeUUID(i, item);
                i++;
            }
            out.writeArrayStop(newItemsLabel);
        }

        Map<UUID, Set<UUID>> links = modification.getLinks();
        GraphBSerializer.serializeLinks(out, links);

        UUID[] readyItems = decisionRow.getReadyItems();
        if (readyItems != null) {
            int i = 0;
            int readyItemsLabel = out.writeArray(READY_ITEMS);
            for (i = 0; i < readyItems.length; i++) {
                out.writeUUID(i, decisionRow.getReadyItems()[i]);
            }
            out.writeArrayStop(readyItemsLabel);
        }
    }

    @Override
    public HzGraphDao.DecisionRow read(BDataInput in) {
        UUID itemId = in.readUUID(_ID);

        UUID waitForAfterRelease = in.readUUID(WAIT_FOR_AFTER_RELEASE);
        Set<UUID> newItems = null;

        int newItemsLabel = in.readArray(NEW_ITEMS);
        if (newItemsLabel != -1) {
            int newItemsSize = in.readArraySize();
            newItems = new HashSet<>(newItemsSize);
            for (int i = 0; i < newItemsSize; i++) {
                newItems.add(in.readUUID(i));
            }
            in.readArrayStop(newItemsLabel);
        }

        Map<UUID, Set<UUID>> links = GraphBSerializer.deserializeLinks(in);

        int readyItemsLabel = in.readArray(READY_ITEMS);
        UUID[] readyItems = null;
        if (readyItemsLabel != 0) {
            int readyItemsSize = in.readArraySize();
            readyItems = new UUID[readyItemsSize];
            for (int i = 0; i < readyItemsSize; i++) {
                readyItems[i] = in.readUUID(i);
            }
        }

        Modification modification = new Modification(itemId, waitForAfterRelease, links, newItems);

        return new HzGraphDao.DecisionRow(itemId, modification, readyItems);
    }
}
