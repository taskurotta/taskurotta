package ru.taskurotta.spring.configs;

/**
 * User: stukushin
 * Date: 24.04.2015
 * Time: 14:33
 */

public class DefaultSpreaderConfigPathXmlApplicationContext extends SpreaderConfigPathXmlApplicationContext {

    public DefaultSpreaderConfigPathXmlApplicationContext() {
        this.context = "spring/ru.taskurotta.spreader.xml";
        this.defaultPropertiesLocation = "properties/ru.taskurotta.spreader.properties";
    }

}
