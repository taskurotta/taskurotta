package ru.taskurotta.recipes.pcollection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.test.flow.BasicFlowArbiter;

import java.util.List;
import java.util.UUID;

/**
 * User: dimadin
 * Date: 22.07.13 11:21
 */
public class PromiseCollectionArbiter extends BasicFlowArbiter {

    private static PromiseCollectionArbiter instance;

    public UUID uuid = UUID.randomUUID();

    private static final Logger logger = LoggerFactory.getLogger(PromiseCollectionArbiter.class);

    public PromiseCollectionArbiter(List<String> stages) {
        super(stages);
    }

    public void init() {
        logger.info("created arbiter instance[{}]", uuid);
    }


    public static PromiseCollectionArbiter createArbiter(List<String> stages) {
        synchronized (PromiseCollectionArbiter.class) {
            if(PromiseCollectionArbiter.instance == null) {
                PromiseCollectionArbiter instance = new PromiseCollectionArbiter(stages);
                setInstance(instance);
            }
        }
        return PromiseCollectionArbiter.getInstance();
    }

    public static PromiseCollectionArbiter getInstance() {
        synchronized (PromiseCollectionArbiter.class) {
            try {
                while (instance == null) {
                    PromiseCollectionArbiter.class.wait();
                }
            } catch (InterruptedException e) {
                // go out
            }
        }
        return instance;
    }

    public static void setInstance(PromiseCollectionArbiter instance) {
        synchronized (PromiseCollectionArbiter.class) {
            PromiseCollectionArbiter.instance = instance;
            PromiseCollectionArbiter.class.notifyAll();
        }
    }


}
