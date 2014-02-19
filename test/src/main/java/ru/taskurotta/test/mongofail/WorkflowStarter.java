package ru.taskurotta.test.mongofail;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.bootstrap.Bootstrap;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.test.mongofail.decider.TimeLogDeciderClient;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Javadoc should be here
 * Date: 18.02.14 15:45
 */
public class WorkflowStarter {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowStarter.class);

    private ClientServiceManager clientServiceManager;

    private MongoTemplate mongoTemplate;

    private int count;

    private long failDelay;

    private long processStartDelay;

    private long checkDelay;

    private long actorDelay;

    private Client client = Client.create();

    private AtomicInteger started = new AtomicInteger(0);//TODO: should it really be atomic?

    private String endpoint;

    public static final String REST_SERVICE_PREFIX = "/rest/";

    public void start() {

        Thread starter = new Thread(new Runnable() {
            @Override
            public void run() {
                final DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
                final TimeLogDeciderClient decider = deciderClientProvider.getDeciderClient(TimeLogDeciderClient.class);

                for (int i = 0; i < count; i++) {
                    decider.execute();
                    started.incrementAndGet();
                    if (i>0 && i % 10 == 0) {
                        logger.info("Started [{}]/[{}] processes", i, count);
                    }

                    if (processStartDelay > 0) {
                        try {
                            Thread.sleep(processStartDelay);
                        } catch (InterruptedException e) {
                            logger.error("Starter thread interrupted!", e);
                        }
                    }
                }

                logger.info("Started [{}] processes", count);
            }
        });


        Thread dataKiller = new Thread(new Runnable() {
            @Override
            public void run() {
                if(failDelay > 0) {
                    try {
                        Thread.sleep(failDelay);
                    } catch (InterruptedException e) {
                        logger.error("DataKiller thread interrupted!", e);
                    }
                }

                mongoTemplate.getDb().dropDatabase();
                logger.info("Mongo DB drop triggered!");
            }
        });

        Thread checker = new Thread(new Runnable() {
            @Override
            public void run() {
                if (checkDelay > 0) {
                    try {
                        Thread.sleep(checkDelay);
                    } catch (InterruptedException e) {
                        logger.error("Checker thread interrupted!", e);
                    }
                }

                WebResource processesResource = client.resource(getContextUrl("/console/processes/finished/count"));
                WebResource.Builder rb = processesResource.getRequestBuilder();
                rb.type(MediaType.APPLICATION_JSON);
                rb.accept(MediaType.APPLICATION_JSON);
                Integer res = rb.get(Integer.class);

                logger.info("Checking processes finished: should be [{}], started[{}], actual [{}]", count, started.get(), res);

            }
        });
        checker.setName("Checker");
        starter.setName("Starter");
        dataKiller.setName("DataKiller");

        starter.start();
        checker.start();
        dataKiller.start();


        if (actorDelay > 0) {
            try {
                Thread.sleep(actorDelay);
            } catch (InterruptedException e) {
                logger.error("Actor main thread interrupted!", e);
            }
        }
    }

    protected String getContextUrl(String path) {
        return endpoint.replaceAll("/*$", "") + REST_SERVICE_PREFIX + path.replaceAll("^/*", "");
    }

    public static void main(String[] args) throws IOException, ArgumentParserException, ClassNotFoundException {
        new Bootstrap("ru/taskurotta/test/mongofail/conf.yml").start();
    }

    @Required
    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    @Required
    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Required
    public void setCount(int preCount) {
        this.count = preCount;
    }

    @Required
    public void setFailDelay(long failDelay) {
        this.failDelay = failDelay;
    }

    @Required
    public void setProcessStartDelay(long processStartDelay) {
        this.processStartDelay = processStartDelay;
    }

    @Required
    public void setCheckDelay(long checkDelay) {
        this.checkDelay = checkDelay;
    }

    @Required
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @Required
    public void setActorDelay(long actorDelay) {
        this.actorDelay = actorDelay;
    }
}
