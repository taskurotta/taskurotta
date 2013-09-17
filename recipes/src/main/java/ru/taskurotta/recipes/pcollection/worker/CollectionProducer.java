package ru.taskurotta.recipes.pcollection.worker;

import ru.taskurotta.annotation.Worker;
import ru.taskurotta.recipes.pcollection.model.ModelObjectVO;

import java.util.List;

/**
 * User: dimadin
 * Date: 22.07.13 14:12
 */
@Worker
public interface CollectionProducer {

    public List<ModelObjectVO> produceList(Integer size);

    public ModelObjectVO[] produceArray(Integer size);

}
