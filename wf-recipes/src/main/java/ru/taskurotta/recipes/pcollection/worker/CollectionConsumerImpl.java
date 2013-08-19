package ru.taskurotta.recipes.pcollection.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.recipes.pcollection.PromiseCollectionArbiter;
import ru.taskurotta.recipes.pcollection.model.ModelObjectVO;

import java.util.Arrays;
import java.util.List;

/**
 * User: dimadin
 * Date: 22.07.13 11:36
 */
public class CollectionConsumerImpl implements CollectionConsumer {

    private static Logger logger = LoggerFactory.getLogger(CollectionConsumerImpl.class);
    private PromiseCollectionArbiter arbiter;

    @Override
    public void consumeCollection(List<ModelObjectVO> objects, Boolean doConsume) {
        arbiter.notify("consumeCollection");
        logger.debug("Processing list of objects type[{}], values are: [{}]", objects.getClass(), objects);
    }

    @Override
    public void consumeArray(ModelObjectVO[] objects, Boolean doConsume) {
        arbiter.notify("consumeArray");
        logger.debug("Processing array of objects, values are: [{}]", Arrays.asList(objects));
    }

    @Required
    public void setArbiter(PromiseCollectionArbiter arbiter) {
        this.arbiter = arbiter;
    }
}
