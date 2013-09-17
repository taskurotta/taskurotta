package ru.taskurotta.recipes.pcollection.worker;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.pcollection.model.ModelObjectVO;

import java.util.List;

/**
 * User: dimadin
 * Date: 22.07.13 14:29
 */
@WorkerClient(worker = CollectionProducer.class)
public interface CollectionProducerClient {

    public Promise<List<ModelObjectVO>> produceList(Promise<Integer> size);

    public Promise<ModelObjectVO[]> produceArray(Promise<Integer> size);

}
