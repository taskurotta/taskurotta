package ru.taskurotta.test.fat.response.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.Promise;
import ru.taskurotta.test.fat.response.FatDecider;
import ru.taskurotta.test.fat.response.Response;

/**
 * Created on 28.05.2015.
 */
public class FatDeciderImpl implements FatDecider {

    private FatWorkerClient fatWorkerClient;

    private FatDeciderImpl self;

    private static final Logger logger = LoggerFactory.getLogger(FatDeciderImpl.class);

    @Override
    public void start(int size) {
        Promise<byte[]> resp = fatWorkerClient.createResponse(size);
        self.logResult(resp);
    }

    @Asynchronous
    public void logResult(Promise<byte[]> resp) {
        logger.info("Result length is [{}]", resp.get().length);
    }

    @Required
    public void setFatWorkerClient(FatWorkerClient fatWorkerClient) {
        this.fatWorkerClient = fatWorkerClient;
    }

    @Required
    public void setSelf(FatDeciderImpl self) {
        this.self = self;
    }
}
