package ru.taskurotta.spring.configs;

import org.junit.Ignore;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 13:57
 */

@Ignore
public class TestActorImpl implements TestActor {

    private String replacedValue;
    private String defaultValue;

    @Override
    public int sum(int a, int b) {
        return a + b;
    }

    public String getReplacedValue() {
        return replacedValue;
    }

    public void setReplacedValue(String replacedValue) {
        this.replacedValue = replacedValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
