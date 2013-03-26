package ru.taskurotta.example.calculate.taskserver;

import java.util.Date;

import ru.taskurotta.example.calculate.profiler.TaskLogger;
import ru.taskurotta.example.calculate.profiler.TaskLoggerImpl;
import ru.taskurotta.server.TaskDao;
import ru.taskurotta.server.TaskServerGeneral;
import ru.taskurotta.server.transport.DecisionContainer;
import ru.taskurotta.server.transport.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

public class ProfiledTaskServerGeneral extends TaskServerGeneral {
	
	private TaskLogger taskLogger;
	
    public ProfiledTaskServerGeneral(TaskDao taskDao, long loggingPeriod) {
    	super(taskDao);
    	taskLogger = new TaskLoggerImpl(loggingPeriod);
    }
    
    @Override
    public TaskContainer pull(ActorDefinition actorDefinition) {
    	Date date = new Date();
    	TaskContainer result =  super.pull(actorDefinition);
    	if(result!=null) {
    		taskLogger.markTaskStart(result.getTaskId().toString(), date);
    	}
    	return result;
    }

    @Override
    public void release(DecisionContainer taskResult) {
        super.release(taskResult);
        taskLogger.markTaskEnd(taskResult.getTaskId().toString(), new Date());
    }


}
