package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.backend.dependency.links.Modification;
import ru.taskurotta.backend.hz.dependency.HzGraphDao;

import java.io.IOException;
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
        UUIDSerializer.write(out, modification.getCompletedItem();

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
            for (UUID item: newItems) {
                UUIDSerializer.write(out, item);
            }
        }

        Map<UUID, Set<UUID>> links = modification.getLinks();
        GraphStreamSerializer.serializeLinks(out, links);
    }

    @Override
    public HzGraphDao.DecisionRow read(ObjectDataInput in) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.GRAPH_DECISION_ROW;
    }

    @Override
    public void destroy() {
    }
}
