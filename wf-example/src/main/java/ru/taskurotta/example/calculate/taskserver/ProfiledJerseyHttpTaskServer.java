package ru.taskurotta.example.calculate.taskserver;

import java.util.Date;

import ru.taskurotta.client.jersey.JerseyHttpTaskServerProxy;
import ru.taskurotta.example.calculate.profiler.TaskLogger;
import ru.taskurotta.example.calculate.profiler.TaskLoggerImpl;
import ru.taskurotta.server.transport.DecisionContainer;
import ru.taskurotta.server.transport.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

public class ProfiledJerseyHttpTaskServer extends JerseyHttpTaskServerProxy {
	protected TaskLogger taskLogger;
	
	public  ProfiledJerseyHttpTaskServer(long loggingPeriod) {
		super();
		taskLogger = new TaskLoggerImpl(loggingPeriod);
	}
	
	@Override
	public TaskContainer pull(ActorDefinition actorDefinition) {
		Date startDate = new Date();
		TaskContainer result = super.pull(actorDefinition);
		if(result!=null) {
			taskLogger.markTaskStart(result.getTaskId().toString(), startDate);
		}
		return result;
	}

	@Override
	public void release(DecisionContainer taskResult) {
		super.release(taskResult);
		taskLogger.markTaskEnd(taskResult.getTaskId().toString(), new Date());
	}

}
