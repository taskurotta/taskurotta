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

/**
 * Created by greg on 05/02/15.
 */
public class DecisionRowSerializer implements StreamBSerializer<HzGraphDao.DecisionRow> {

    public static final CString ITEM_ID = new CString("itId");
    public static final CString MODIFICATION = new CString("mod");
    public static final CString COMPLETED_ITEM = new CString("compIt");
    public static final CString WAIT_FOR_AFTER_RELEASE = new CString("waForAfRe");
    public static final CString NEW_ITEMS = new CString("nIt");
    public static final CString READY_ITEMS = new CString("redIt");

    @Override
    public Class<HzGraphDao.DecisionRow> getObjectClass() {
        return HzGraphDao.DecisionRow.class;
    }

    @Override
    public void write(BDataOutput out, HzGraphDao.DecisionRow decisionRow) {
        out.writeUUID(ITEM_ID, decisionRow.getItemId());
        int modObjectLabel = out.writeObject(MODIFICATION);
        Modification modification = decisionRow.getModification();
        out.writeUUID(COMPLETED_ITEM, modification.getCompletedItem());
        out.writeUUID(WAIT_FOR_AFTER_RELEASE, modification.getWaitForAfterRelease());
        Set<UUID> newItems = modification.getNewItems();
        int newItemsLabel = out.writeArray(NEW_ITEMS);
        int i = 0;
        for (UUID item : newItems) {
            out.writeUUID(SerializerTools.createCString(i), item);
            i++;
        }
        out.writeArrayStop(newItemsLabel);

        Map<UUID, Set<UUID>> links = modification.getLinks();
        GraphSerializer.serializeLinks(out, links);
        out.writeObjectStop(modObjectLabel);

        int readyItemsCount = (decisionRow.getReadyItems() != null) ? decisionRow.getReadyItems().length : 0;

        if (readyItemsCount > 0) {
            int readyItemsLabel = out.writeArray(READY_ITEMS);
            for (i = 0; i < readyItemsCount; i++) {
                out.writeUUID(SerializerTools.createCString(i), decisionRow.getReadyItems()[i]);
            }
            out.writeArrayStop(readyItemsLabel);
        }

    }

    @Override
    public HzGraphDao.DecisionRow read(BDataInput in) {
        UUID itemId = in.readUUID(ITEM_ID);
        int modObjectLabel = in.readObject(MODIFICATION);
        UUID completedItem = in.readUUID(COMPLETED_ITEM);
        UUID waitForAfterRelease = in.readUUID(WAIT_FOR_AFTER_RELEASE);
        Set<UUID> newItems = null;
        int newItemsLabel = in.readArray(NEW_ITEMS);
        if (newItemsLabel > 0) {
            int newItemsSize = in.readArraySize();
            newItems = new HashSet<>(newItemsSize);
            for (int i = 0; i < newItemsSize; i++) {
                newItems.add(in.readUUID(SerializerTools.createCString(i)));
            }
            in.readArrayStop(newItemsLabel);
        }

        Map<UUID, Set<UUID>> links = GraphSerializer.deserializeLinks(in);
        in.readObjectStop(modObjectLabel);

        Modification modification = new Modification();
        modification.setCompletedItem(completedItem);
        modification.setWaitForAfterRelease(waitForAfterRelease);
        modification.setNewItems(newItems);
        modification.setLinks(links);

        int readyItemsLabel = in.readArray(READY_ITEMS);
        UUID[] readyItems = null;
        if (readyItemsLabel > 0) {
            int readyItemsSize = in.readArraySize();
            readyItems = new UUID[readyItemsSize];
            for (int i = 0; i < readyItemsSize; i++) {
                readyItems[i] = in.readUUID(SerializerTools.createCString(i));
            }
        }

        return new HzGraphDao.DecisionRow(itemId, modification, readyItems);
    }
}
