package ru.taskurotta.e2e;

/**
 */
public interface SpecSuite {

    /**
     * Prepare all test data
     */
    void init();

    /**
     * Remove all test data
     */
    void clean();
}
