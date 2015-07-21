package ru.taskurotta.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.GroupCommand;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.console.model.TaskIdentifier;
import ru.taskurotta.service.console.model.TasksGroupVO;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 21.07.2015
 * Time: 15:07
 */

public class AbstractInterruptedTasksService implements InterruptedTasksService {

    private static final Logger logger = LoggerFactory.getLogger(AbstractInterruptedTasksService.class);

    protected ScriptEngine scriptEngine;

    public AbstractInterruptedTasksService(String scriptLocation, long scriptReloadTimeout) {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        this.scriptEngine = scriptEngineManager.getEngineByName("nashorn");

        Path scriptPath = Paths.get(scriptLocation);
        if (Files.notExists(scriptPath) || !Files.isRegularFile(scriptPath)) {
            String report = "Script location [" + scriptLocation + "] not exists or not file";
            logger.error(report);
            throw new RuntimeException(report);
        }

        new ScheduledThreadPoolExecutor(1).scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    scriptEngine.eval(new FileReader(scriptPath.toFile()));
                } catch (ScriptException e) {
                    logger.error("Catch exception when evaluate script by location [" + scriptPath + "]", e);
                } catch (FileNotFoundException e) {
                    logger.error("Script location [" + scriptLocation + "] not exists or not file", e);
                }
            }
        }, 0l, scriptReloadTimeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void save(InterruptedTask brokenProcess, String fullMessage, String stackTrace) {

    }

    @Override
    public Collection<InterruptedTask> find(SearchCommand searchCommand) {
        return null;
    }

    @Override
    public Collection<InterruptedTask> findAll() {
        return null;
    }

    @Override
    public void delete(UUID processId, UUID taskId) {

    }

    @Override
    public String getFullMessage(UUID processId, UUID taskId) {
        return null;
    }

    @Override
    public String getStackTrace(UUID processId, UUID taskId) {
        return null;
    }

    @Override
    public List<TasksGroupVO> getGroupList(GroupCommand command) {
        return null;
    }

    @Override
    public Collection<TaskIdentifier> getTaskIdentifiers(GroupCommand command) {
        return null;
    }

    @Override
    public Set<UUID> getProcessIds(GroupCommand command) {
        return null;
    }

    @Override
    public long deleteTasksForProcess(UUID processId) {
        return 0;
    }

    @Override
    public boolean isUnknown(InterruptedTask task) {
        try {
            Invocable invocable = (Invocable) scriptEngine;
            return (boolean) invocable.invokeFunction("detect", task);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
