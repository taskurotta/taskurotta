package ru.taskurotta.bootstrap.config;

import org.junit.Ignore;
import ru.taskurotta.bootstrap.config.SpreaderConfig;
import ru.taskurotta.client.TaskSpreader;

/**
 * User: romario
 * Date: 2/12/13
 * Time: 4:14 PM
 */
@Ignore
public class TestSpreaderConfig implements SpreaderConfig {

    private String context;

    @Override
    public void init() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TaskSpreader getTaskSpreader(Class clazz) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setContext(String context) {
        this.context = context;
    }
}
