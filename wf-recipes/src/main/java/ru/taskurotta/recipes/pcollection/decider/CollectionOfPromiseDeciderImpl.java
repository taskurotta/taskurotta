package ru.taskurotta.recipes.pcollection.decider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.Wait;
import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.pcollection.PromiseCollectionArbiter;
import ru.taskurotta.recipes.pcollection.model.ModelObjectVO;
import ru.taskurotta.recipes.pcollection.worker.CollectionConsumerClient;
import ru.taskurotta.recipes.pcollection.worker.CollectionProducerClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: dimadin
 * Date: 22.07.13 11:36
 */
public class CollectionOfPromiseDeciderImpl implements CollectionOfPromiseDecider {

    private static final Logger logger = LoggerFactory.getLogger(CollectionOfPromiseDeciderImpl.class);

    private CollectionProducerClient producer;
    private CollectionConsumerClient consumer;
    private CollectionOfPromiseDeciderImpl selfAsync;
    private PromiseCollectionArbiter arbiter;

    @Override
    public void execute(int size) {
        arbiter.notify("execute");
        Promise<ModelObjectVO[]> array = producer.produceArray(Promise.asPromise(size));
        Promise<List<ModelObjectVO>> list = producer.produceList(Promise.asPromise(size));

        List<Promise> waitList = new ArrayList<Promise>();
        waitList.add(array);
        waitList.add(list);

        //Any method in the commented list below should work:
        //Promise <Boolean> waitComplete = selfAsync.waitForSeparate(array, list);
        Promise<Boolean> waitComplete = selfAsync.waitForList(waitList);

        Promise<Boolean> compareResult = selfAsync.isContainSameElements(array, list);
        selfAsync.logResult(compareResult, array, list);


        Promise<Void> pArray = consumer.consumeArray(array, waitComplete);
        Promise<Void> pCollection = consumer.consumeCollection(list, waitComplete);

    }

    @Asynchronous
    public void logResult(Promise<Boolean> compareResult, Promise<ModelObjectVO[]> array, Promise<List<ModelObjectVO>> list) {
        arbiter.notify("logResult");
        logger.info("Compare result is[{}], array[{}], list[{}]", compareResult.get(), Arrays.asList(array.get()), list.get());
    }

    @Asynchronous
    public Promise<Boolean> waitForSeparate(Promise<ModelObjectVO[]> array, Promise<List<ModelObjectVO>> list) {
        arbiter.notify("waitFor");
        StringBuilder sb = new StringBuilder();

        sb.append(array.get());
        sb.append(list.get());
        logger.debug("Wait separate args complete with result [{}]", sb);
        return Promise.asPromise(Boolean.TRUE);
    }

    @Asynchronous
    public Promise<Boolean> waitForList(@Wait List<Promise> waitFor) { //require all promises in list to be initialized
        arbiter.notify("waitFor");
        StringBuilder sb = new StringBuilder();

        for (Promise item : waitFor) {
            sb.append(item.get());
        }
        logger.debug("Wait list complete with result [{}]", sb);
        return Promise.asPromise(Boolean.TRUE);
    }

    @Asynchronous
    public Promise<Boolean> isContainSameElements(Promise<ModelObjectVO[]> pArray, Promise<List<ModelObjectVO>> pList) {
        arbiter.notify("isContainSameElements");
        ModelObjectVO[] oArray = pArray.get();
        List<ModelObjectVO> oList = pList.get();

        if (oArray == null || oList == null) {
            logger.error("Cannot compare null values!");
//            return Boolean.valueOf(Boolean.FALSE);
            return Promise.asPromise(Boolean.FALSE);
        }

        boolean result = (oArray.length == oList.size());

        if (result) {
            for (ModelObjectVO mo : oArray) {
                if (!oList.contains(mo)) {
                    result = false;
                    break;
                }
            }
        }

        logger.debug("isContainSameElements result is: {}", result);
//        return Boolean.valueOf(result);
        return Promise.asPromise(Boolean.valueOf(result));
    }

    @Required
    public void setSelfAsync(CollectionOfPromiseDeciderImpl selfAsync) {
        this.selfAsync = selfAsync;
    }

    @Required
    public void setProducer(CollectionProducerClient producer) {
        this.producer = producer;
    }

    @Required
    public void setConsumer(CollectionConsumerClient consumer) {
        this.consumer = consumer;
    }

    @Required
    public void setArbiter(PromiseCollectionArbiter arbiter) {
        this.arbiter = arbiter;
    }
}
