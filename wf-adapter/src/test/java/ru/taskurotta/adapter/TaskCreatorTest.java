package ru.taskurotta.adapter;

import org.junit.Ignore;
import org.junit.Test;

/**
 * User: stukushin
 * Date: 22.05.13
 * Time: 13:15
 */

@Ignore
public class TaskCreatorTest {
    @Test
    public void testCreateTask() throws Exception {
        RestTaskCreator taskCreator = new RestTaskCreator("http://10.129.0.51");

        taskCreator.createTask("ru.taskurotta.example.calculate.decider.MathActionDecider#1.0", "performAction", null, null, -1, null);
    }
}
