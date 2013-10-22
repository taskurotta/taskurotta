package ru.taskurotta.spring.configs;

import org.junit.Ignore;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 13:57
 */

@Ignore
public class TestActorImpl implements TestActor {

    private String testProperty;

    @Override
    public int sum(int a, int b) {
        return a + b;
    }

    public void setTestProperty(String testProperty) {
        this.testProperty = testProperty;
    }

    public String getTestProperty() {
        return testProperty;
    }
}
