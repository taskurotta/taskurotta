package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.dependency.links.Modification;
import ru.taskurotta.service.hz.dependency.HzGraphDao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * User: romario
 * Date: 9/12/13
 * Time: 2:52 PM
 */
public class DecisionRowStreamSerializer implements StreamSerializer<HzGraphDao.DecisionRow> {

    @Override
    public void write(ObjectDataOutput out, HzGraphDao.DecisionRow decisionRow) throws IOException {

        UUIDSerializer.write(out, decisionRow.getItemId());

        Modification modification = decisionRow.getModification();
        UUIDSerializer.write(out, modification.getCompletedItem());

        UUID waitForItemId = modification.getWaitForAfterRelease();
        if (waitForItemId == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            UUIDSerializer.write(out, waitForItemId);
        }

        Set<UUID> newItems = modification.getNewItems();
        if (newItems == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(newItems.size());
            for (UUID item : newItems) {
                UUIDSerializer.write(out, item);
            }
        }

        Map<UUID, Set<UUID>> links = modification.getLinks();
        GraphStreamSerializer.serializeLinks(out, links);

        int readyItemsCount = (decisionRow.getReadyItems() != null) ? decisionRow.getReadyItems().length : 0;

        if (readyItemsCount > 0) {
            out.writeInt(readyItemsCount);
            for (int i = 0; i < readyItemsCount; i++) {
                UUIDSerializer.write(out, decisionRow.getReadyItems()[i]);
            }
        } else {
            out.writeInt(-1);
        }
    }

    @Override
    public HzGraphDao.DecisionRow read(ObjectDataInput in) throws IOException {
        UUID itemId = UUIDSerializer.read(in);

        Modification modification = new Modification();
        modification.setCompletedItem(UUIDSerializer.read(in));
        boolean hasWaitForAfterRelease = in.readBoolean();
        if (hasWaitForAfterRelease) {
            modification.setWaitForAfterRelease(UUIDSerializer.read(in));
        }
        int countOfNewItems = in.readInt();

        if (countOfNewItems != -1) {
            Set<UUID> newItems = new HashSet<>();
            for (int i = 0; i < countOfNewItems; i++) {
                newItems.add(UUIDSerializer.read(in));
            }
            modification.setNewItems(newItems);
        }
        Map<UUID, Set<UUID>> links = GraphStreamSerializer.deserializeLinks(in);
        modification.setLinks(links);

        int readyItemCount = in.readInt();

        if (readyItemCount != -1) {
            List<UUID> list = new ArrayList<>();

            for (int i = 0; i < readyItemCount; i++) {
                list.add(UUIDSerializer.read(in));
            }
            UUID[] arrayOfUUID = new UUID[list.size()];
            list.toArray(arrayOfUUID);

            return new HzGraphDao.DecisionRow(itemId, modification, arrayOfUUID);
        }
        return new HzGraphDao.DecisionRow(itemId, modification, null);
    }


    @Override
    public int getTypeId() {
        return ObjectTypes.DECISION_ROW_STREAM;
    }

    @Override
    public void destroy() {
    }
}
