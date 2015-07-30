package ru.taskurotta.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.InterruptedTask;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 21.07.2015
 * Time: 15:07
 */

public abstract class AbstractInterruptedTasksService implements InterruptedTasksService {

    private static final Logger logger = LoggerFactory.getLogger(AbstractInterruptedTasksService.class);

    private ScriptEngine scriptEngine;
    private Invocable invocable;

    public AbstractInterruptedTasksService(String scriptLocation, long scriptReloadTimeout) {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine testScriptEngine = scriptEngineManager.getEngineByName("nashorn");

        new ScheduledThreadPoolExecutor(1).scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Path scriptPath = Paths.get(scriptLocation);
                try {
                    testScriptEngine.eval(new FileReader(scriptPath.toFile()));

                    scriptEngine = testScriptEngine;
                    invocable = (Invocable) scriptEngine;
                } catch (ScriptException e) {
                    logger.error("Catch exception when evaluate script by location [" + scriptPath + "]", e);
                } catch (FileNotFoundException e) {
                    logger.error("Script location [" + scriptLocation + "] not exists or not file", e);
                }
            }
        }, 0l, scriptReloadTimeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isKnown(InterruptedTask task) {
        try {
            return (boolean) invocable.invokeFunction("isKnown", task);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
