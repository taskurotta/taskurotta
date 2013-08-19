package ru.taskurotta.recipes.pcollection.worker;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.pcollection.model.ModelObjectVO;

import java.util.List;

/**
 * User: dimadin
 * Date: 22.07.13 11:36
 */
@WorkerClient(worker = CollectionConsumer.class)
public interface CollectionConsumerClient {

    //public Promise<Void> consumeCollection(Promise<List<ModelObjectVO>> pObjects);

    public Promise<Void> consumeCollection(Promise<List<ModelObjectVO>> pObjects, Promise<Boolean> waitFor);

    //public Promise<Void> consumeArray(Promise<ModelObjectVO[]> pObjects);

    public Promise<Void> consumeArray(Promise<ModelObjectVO[]> pObjects, Promise<Boolean> waitFor);

}
