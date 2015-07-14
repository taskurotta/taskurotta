package ru.taskurotta.util;

import org.junit.Assert;
import org.junit.Test;
import org.testng.reporters.Files;
import ru.taskurotta.service.console.model.InterruptedTask;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created on 11.06.2015.
 */
public class NotificationUtilsTest {

    @Test
    public void  testAsActorIdList() {
        Collection<InterruptedTask> empty = Collections.emptyList();
        Collection<InterruptedTask> nullColl = null;
        Collection<InterruptedTask> normalColl = new ArrayList<>();
        normalColl.add(createTask("ru.taskurotta.test.Actor1#1.0", "ru.taskurotta.test.Starter1#1.0"));
        normalColl.add(createTask("ru.taskurotta.test.Actor2#1.0", "ru.taskurotta.test.Starter2#1.0"));

        Collection<InterruptedTask> duplicatedActorsColl = new ArrayList<>();
        duplicatedActorsColl.add(createTask("ru.taskurotta.test.Actor#1.0", "ru.taskurotta.test.Actor#1.0"));
        duplicatedActorsColl.add(createTask("ru.taskurotta.test.Actor#1.0", "ru.taskurotta.test.Actor#1.0"));
        duplicatedActorsColl.add(createTask("ru.taskurotta.test.Actor#1.0", "ru.taskurotta.test.Actor#1.0"));
        duplicatedActorsColl.add(createTask("ru.taskurotta.test.Actor#1.0", "ru.taskurotta.test.Actor#1.0"));

        Set<String> res1 = NotificationUtils.asActorIdList(empty);
        Assert.assertNull(res1);
        Set<String> res2 = NotificationUtils.asActorIdList(nullColl);
        Assert.assertNull(res1);
        Set<String> res3 = NotificationUtils.asActorIdList(normalColl);
        assertEquals(4, res3.size());
        Set<String> res4 = NotificationUtils.asActorIdList(duplicatedActorsColl);
        assertEquals(1, res4.size());

    }

    @Test
    public void testGetTrackedValues() {
        Collection<String> empty = Collections.emptyList();

        Assert.assertNull(NotificationUtils.getTrackedValues(empty, empty));
        Assert.assertNull(NotificationUtils.getTrackedValues(empty, null));
        Assert.assertNull(NotificationUtils.getTrackedValues(null, empty));
        Assert.assertNull(NotificationUtils.getTrackedValues(null, null));

        Collection<String> colValuesl = createCollection();

        Collection<String> trackedVals1 = new ArrayList<>();
        trackedVals1.add("ru.taskurotta.test.Actor");
        trackedVals1.add("ru.taskurotta.another");
        trackedVals1.add("ru.taskurotta.someother");
        Set<String> res1 = NotificationUtils.getTrackedValues(trackedVals1, colValuesl);
        assertEquals(6, res1.size());
        Assert.assertTrue(res1.contains("ru.taskurotta.another.test.ZipWorker2#1.0#task4"));

        Collection<String> trackedVals2 = new ArrayList<>();
        trackedVals2.add("ru.taskurotta.test.Actor#1.0#task1");
        trackedVals2.add("ru.taskurotta.test.Actor#1.0#task2");
        Set<String> res2 = NotificationUtils.getTrackedValues(trackedVals2, colValuesl);
        assertEquals(2, res2.size());
        Assert.assertTrue(res2.contains("ru.taskurotta.test.Actor#1.0#task1"));
        Assert.assertTrue(res2.contains("ru.taskurotta.test.Actor#1.0#task2"));

        Set<String> res3 = NotificationUtils.getTrackedValues(trackedVals2, empty);
        Assert.assertNull(res3);
    }

    @Test
    public void testExcludeOldValues() {
        Collection<String> empty = new ArrayList<>();
        NotificationUtils.excludeOldValues(empty, empty);
        Assert.assertTrue(empty.isEmpty());
        NotificationUtils.excludeOldValues(empty, null);
        Assert.assertTrue(empty.isEmpty());
        NotificationUtils.excludeOldValues(null, empty);
        Assert.assertTrue(empty.isEmpty());
        NotificationUtils.excludeOldValues(null, null);

        Collection<String> colValuesl = createCollection();
        Collection<String> colValues2 = createCollection();
        NotificationUtils.excludeOldValues(colValuesl, colValues2);
        Assert.assertTrue(colValuesl.isEmpty());
        Assert.assertTrue(!colValues2.isEmpty());

        Collection<String> colValues3 = createCollection();
        Collection<String> colValues4 = new ArrayList<>();
        colValues4.add("ru.taskurotta.test.ZipWorker#1.0");
        colValues4.add("ru.taskurotta.another.test.ZipWorker#1.0");

        assertEquals(7, colValues3.size());
        assertEquals(2, colValues4.size());
        NotificationUtils.excludeOldValues(colValues3, colValues4);
        assertEquals(5, colValues3.size());
        assertEquals(2, colValues4.size());

        Collection<String> colValues5 = new ArrayList<>();
        Collection<String> colValues6 = createCollection();
        Assert.assertTrue(colValues5.isEmpty());
        assertEquals(7, colValues6.size());
        NotificationUtils.excludeOldValues(colValues5, colValues6);
        Assert.assertTrue(colValues5.isEmpty());
        assertEquals(7, colValues6.size());

        Collection<String> colValues7 = new ArrayList<>();
        Collection<String> colValues8 = createCollection();
        colValues7.add("newVal1");
        colValues7.add("newVal2");
        assertEquals(2, colValues7.size());
        assertEquals(7, colValues8.size());
        NotificationUtils.excludeOldValues(colValues7, colValues8);
        assertEquals(2, colValues7.size());
        assertEquals(7, colValues8.size());
    }

    @Test
    public void testExcludeOldTasksValues() {
        Collection<InterruptedTask> empty = new ArrayList<>();
        NotificationUtils.excludeOldTasksValues(empty, empty);
        Assert.assertTrue(empty.isEmpty());
        NotificationUtils.excludeOldTasksValues(empty, null);
        Assert.assertTrue(empty.isEmpty());
        NotificationUtils.excludeOldTasksValues(null, empty);
        Assert.assertTrue(empty.isEmpty());
        NotificationUtils.excludeOldTasksValues(null, null);

        UUID uuid = UUID.randomUUID();
        long time = System.currentTimeMillis();

        Collection<InterruptedTask> colValuesl = createTaskCollection(uuid, time);
        Collection<InterruptedTask> colValues2 = createTaskCollection(uuid, time);
        NotificationUtils.excludeOldTasksValues(colValuesl, colValues2);
        Assert.assertTrue(colValuesl.isEmpty());
        Assert.assertTrue(!colValues2.isEmpty());

        Collection<InterruptedTask> colValues3 = createTaskCollection(uuid, time);
        Collection<InterruptedTask> colValues4 = new ArrayList<>();
        colValues4.add(createTask("ru.taskurotta.test.ZipWorker#1.0", "ru.taskurotta.test.ZipWorker#1.0", uuid, time));
        colValues4.add(createTask("ru.taskurotta.another.test.ZipWorker#1.0", "ru.taskurotta.another.test.ZipWorker#1.0", uuid, time));

        assertEquals(7, colValues3.size());
        assertEquals(2, colValues4.size());
        NotificationUtils.excludeOldTasksValues(colValues3, colValues4);
        assertEquals(5, colValues3.size());
        assertEquals(2, colValues4.size());

        Collection<InterruptedTask> colValues5 = new ArrayList<>();
        Collection<InterruptedTask> colValues6 = createTaskCollection(uuid, time);;
        Assert.assertTrue(colValues5.isEmpty());
        assertEquals(7, colValues6.size());
        NotificationUtils.excludeOldTasksValues(colValues5, colValues6);
        Assert.assertTrue(colValues5.isEmpty());
        assertEquals(7, colValues6.size());

        Collection<InterruptedTask> colValues7 = new ArrayList<>();
        Collection<InterruptedTask> colValues8 = createTaskCollection(uuid, time);;
        colValues7.add(createTask("newVal1", "newVal1"));
        colValues7.add(createTask("newVal1", "newVal1"));
        assertEquals(2, colValues7.size());
        assertEquals(7, colValues8.size());
        NotificationUtils.excludeOldTasksValues(colValues7, colValues8);
        assertEquals(2, colValues7.size());
        assertEquals(7, colValues8.size());
    }

    @Test
    public void testListToJson() {
        List<String> list1 = createCollection();
        String json1 = NotificationUtils.listToJson(list1, "[]");
        List<String> list2 = NotificationUtils.jsonToList(json1, null);
        for (int i = 0; i<list1.size(); i++) {
            assertEquals(list1.get(i), list2.get(i));
        }

        String json2 = NotificationUtils.listToJson(null, "[]");
        assertEquals(json2, "[]");

        String json3 = NotificationUtils.listToJson(new ArrayList<String>(), "[]");
        assertEquals(json3, "[]");

        List<String> list3 = NotificationUtils.jsonToList(null, new ArrayList<String>());
        Assert.assertTrue(list3.isEmpty());

        List<String> list4 = NotificationUtils.jsonToList("[]", new ArrayList<String>());
        Assert.assertTrue(list4.isEmpty());

    }

    @Test(expected = RuntimeException.class)
    public void testListToJsonFail() {
        NotificationUtils.jsonToList("[[[[[", new ArrayList<String>());
    }

    @Test
    public void testFilterTrackedActors() throws IOException {
        Collection<InterruptedTask> interruptedTasks = createTaskCollection(UUID.randomUUID(), System.currentTimeMillis());
        String script = Files.readFile(this.getClass().getClassLoader().getResourceAsStream("script.js"));
        ScriptEngine scriptEngine = getScriptEngine();
        Collection<String> trackedActors = NotificationUtils.filterTrackedActors(interruptedTasks, script, scriptEngine);
        assertEquals(Collections.singletonList("ru.taskurotta.another.test.ZipWorker2#1.0#task4"), trackedActors);
    }

    private Collection<InterruptedTask> createTaskCollection(UUID uuid, long time) {
        Collection<InterruptedTask> res = new ArrayList<>();
        res.add(createTask("ru.taskurotta.test.Actor#1.0", "ru.taskurotta.test.Actor#1.0", uuid, time));
        res.add(createTask("ru.taskurotta.test.Actor#1.0#task1", "ru.taskurotta.test.Actor#1.0#task1", uuid, time));
        res.add(createTask("ru.taskurotta.test.Actor#1.0#task2", "ru.taskurotta.test.Actor#1.0#task2", uuid, time));
        res.add(createTask("ru.taskurotta.test.ZipWorker#1.0", "ru.taskurotta.test.ZipWorker#1.0", uuid, time));
        res.add(createTask("ru.taskurotta.another.test.ZipWorker#1.0", "ru.taskurotta.another.test.ZipWorker#1.0", uuid, time));
        res.add(createTask("ru.taskurotta.another.test.ZipWorker2#1.0", "ru.taskurotta.another.test.ZipWorker2#1.0", uuid, time));
        res.add(createTask("ru.taskurotta.another.test.ZipWorker2#1.0#task4", "ru.taskurotta.another.test.ZipWorker2#1.0#task4", uuid, time));
        res.add(createTask("ru.taskurotta.another.test.ZipWorker2#1.0#task4", "ru.taskurotta.another.test.ZipWorker2#1.0#task4", "testError"));
        return res;
    }

    private List<String> createCollection() {
        List<String> res = new ArrayList<>();
        res.add("ru.taskurotta.test.Actor#1.0");
        res.add("ru.taskurotta.test.Actor#1.0#task1");
        res.add("ru.taskurotta.test.Actor#1.0#task2");
        res.add("ru.taskurotta.test.ZipWorker#1.0");
        res.add("ru.taskurotta.another.test.ZipWorker#1.0");
        res.add("ru.taskurotta.another.test.ZipWorker2#1.0");
        res.add("ru.taskurotta.another.test.ZipWorker2#1.0#task4");
        return res;
    }

    private InterruptedTask createTask(String actorId, String starterId, UUID uuid, long time) {
        InterruptedTask result = new InterruptedTask();
        result.setActorId(actorId);
        result.setErrorClassName(getClass().getName());
        result.setErrorMessage("message");
        result.setProcessId(uuid);
        result.setStarterId(starterId);
        result.setTaskId(uuid);
        result.setTime(time);
        return result;
    }

    private InterruptedTask createTask(String actorId, String starterId) {
        InterruptedTask result = new InterruptedTask();
        result.setActorId(actorId);
        result.setErrorClassName(getClass().getName());
        result.setErrorMessage("message");
        result.setProcessId(UUID.randomUUID());
        result.setStarterId(starterId);
        result.setTaskId(UUID.randomUUID());
        result.setTime(System.currentTimeMillis());
        return result;
    }

    private InterruptedTask createTask(String actorId, String starterId, String errorMessage) {
        InterruptedTask result = new InterruptedTask();
        result.setActorId(actorId);
        result.setErrorClassName(getClass().getName());
        result.setErrorMessage(errorMessage);
        result.setProcessId(UUID.randomUUID());
        result.setStarterId(starterId);
        result.setTaskId(UUID.randomUUID());
        result.setTime(System.currentTimeMillis());
        return result;
    }

    private ScriptEngine getScriptEngine() {
        return new ScriptEngineManager().getEngineByName("nashorn");
    }

}
