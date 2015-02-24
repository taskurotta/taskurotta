package ru.taskurotta.recipes.darg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.test.flow.BasicFlowArbiter;

import java.util.List;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 16.07.13 14:29
 */
public class DArgArbiter extends BasicFlowArbiter {

    private static DArgArbiter instance;

    public UUID uuid = UUID.randomUUID();

    private static final Logger logger = LoggerFactory.getLogger(DArgArbiter.class);

    public DArgArbiter(List<String> stages) {
        super(stages);
    }

    public void init() {
        logger.info("created arbiter instance[{}]", uuid);
    }


    public static DArgArbiter createArbiter(List<String> stages) {
        synchronized (DArgArbiter.class) {
            if(DArgArbiter.instance == null) {
                DArgArbiter instance = new DArgArbiter(stages);
                setInstance(instance);
            }
        }
        return DArgArbiter.getInstance();
    }

    public static DArgArbiter getInstance() {
        synchronized (DArgArbiter.class) {
            try {
                while (instance == null) {
                    DArgArbiter.class.wait();
                }
            } catch (InterruptedException e) {
                // go out
            }
        }
        return instance;
    }

    public static void setInstance(DArgArbiter instance) {
        synchronized (DArgArbiter.class) {
            DArgArbiter.instance = instance;
            DArgArbiter.class.notifyAll();
        }
    }

}
