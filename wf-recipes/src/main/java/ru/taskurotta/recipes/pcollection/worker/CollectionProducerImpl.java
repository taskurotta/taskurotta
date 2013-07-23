package ru.taskurotta.recipes.pcollection.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.recipes.pcollection.PromiseCollectionArbiter;
import ru.taskurotta.recipes.pcollection.model.ModelObjectVO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: dimadin
 * Date: 22.07.13 14:32
 */
public class CollectionProducerImpl implements CollectionProducer {

    private static final Logger logger = LoggerFactory.getLogger(CollectionProducerImpl.class);
    private PromiseCollectionArbiter arbiter;

    @Override
    public List<ModelObjectVO> produceList(Integer size) {
        arbiter.notify("produceList");
        List<ModelObjectVO> result = new ArrayList<ModelObjectVO>();
        for(int i = 0; i<size; i++) {
            result.add(new ModelObjectVO(i));
        }
        logger.debug("Generated [{}] size list with elements [{}]", size, result);
        return result;
    }

    @Override
    public ModelObjectVO[] produceArray(Integer size) {
        arbiter.notify("produceArray");
        ModelObjectVO[] result = new ModelObjectVO[size];
        for(int i = 0; i<size; i++) {
            result[i] = new ModelObjectVO(i);
        }
        logger.debug("Generated [{}] size array with elements [{}]", size, Arrays.asList(result));
        return result;
    }

    @Required
    public void setArbiter(PromiseCollectionArbiter arbiter) {
        this.arbiter = arbiter;
    }
}
