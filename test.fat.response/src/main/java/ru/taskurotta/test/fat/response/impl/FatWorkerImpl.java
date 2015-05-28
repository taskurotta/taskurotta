package ru.taskurotta.test.fat.response.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.test.fat.response.FatWorker;
import ru.taskurotta.test.fat.response.Response;

import java.util.Random;

/**
 * Created on 28.05.2015.
 */
public class FatWorkerImpl implements FatWorker {

    //private static final Logger logger = LoggerFactory.getLogger(FatWorkerImpl.class);

    @Override
    public byte[] createResponse(int size) throws Exception {
        byte[] result = createArrayOfLength(size);
        //logger.debug("Message generated with size[{}] is [{}]", size, result);
        if (size<0) {
            throw new IllegalArgumentException(new String(result, "ISO-8859-1"));
        }

        return result;
    }

    byte[] createArrayOfLength(int size) throws Exception {
        size = Math.abs(size);
        byte[] result = new byte[size];
        Random random = new Random();
        for (int i = 0; i<size; i++) {
            if (random.nextBoolean()) {
                result[i] = 1;
            } else {
                result[i] = 0;
            }
        }

        return result;
    }

}
