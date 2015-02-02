package ru.taskurotta.mongodb.driver;

import java.io.IOException;

/**
 */
public interface BDataInput {

    long readLong(CString name) throws IOException;
}
