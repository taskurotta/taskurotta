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
    private String defaultValue1;
    private String defaultValue2;

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

    public String getDefaultValue1() {
        return defaultValue1;
    }

    public void setDefaultValue1(String defaultValue1) {
        this.defaultValue1 = defaultValue1;
    }

    public String getDefaultValue2() {
        return defaultValue2;
    }

    public void setDefaultValue2(String defaultValue2) {
        this.defaultValue2 = defaultValue2;
    }
}
