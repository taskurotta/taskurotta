package ru.taskurotta.recipes.nowait.decider;

import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.annotation.NoWait;
import ru.taskurotta.core.Promise;

/**
 * Created by void 27.03.13 14:20
 */
@Decider
public interface NoWaitDecider {

	@Execute
	public void start();

}
