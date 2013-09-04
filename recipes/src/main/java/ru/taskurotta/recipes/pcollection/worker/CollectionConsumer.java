package ru.taskurotta.recipes.pcollection.worker;

import ru.taskurotta.annotation.Worker;
import ru.taskurotta.recipes.pcollection.model.ModelObjectVO;

import java.util.List;

/**
 * User: dimadin
 * Date: 22.07.13 11:35
 */
@Worker
public interface CollectionConsumer {

    public void consumeCollection(List<ModelObjectVO> objects, Boolean doConsume);

    public void consumeArray(ModelObjectVO[] objects, Boolean doConsume);

}
