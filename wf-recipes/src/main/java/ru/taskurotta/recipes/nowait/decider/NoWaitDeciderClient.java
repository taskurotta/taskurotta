package ru.taskurotta.recipes.nowait.decider;

import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.annotation.NoWait;
import ru.taskurotta.core.Promise;

/**
 * Created by void 27.03.13 17:04
 */
@DeciderClient(decider = NoWaitDecider.class)
public interface NoWaitDeciderClient {

	@Execute
	public void start();

}
