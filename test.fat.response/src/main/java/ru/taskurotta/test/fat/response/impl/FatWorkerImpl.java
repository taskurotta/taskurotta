package ru.taskurotta.test.fat.response.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import ru.taskurotta.test.fat.response.FatWorker;
import ru.taskurotta.test.fat.response.Response;

import java.io.File;
import java.util.Random;

/**
 * Created on 28.05.2015.
 */
public class FatWorkerImpl implements FatWorker {

    //private static final Logger logger = LoggerFactory.getLogger(FatWorkerImpl.class);

    @Override
    public String createResponse(int size) throws Exception {
        String result = createStringOfLength(size);
        //logger.debug("Message generated with size[{}] is [{}]", size, result);
        if (size<0) {
            throw new IllegalArgumentException(result);
        }

        return result;
    }

    String createStringOfLength(int size) throws Exception {
        size = Math.abs(size);
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i<size; i++) {
            if (random.nextBoolean()) {
                sb.append(1);
            } else {
                sb.append(0);
            }
        }

        return sb.toString();
    }

}
