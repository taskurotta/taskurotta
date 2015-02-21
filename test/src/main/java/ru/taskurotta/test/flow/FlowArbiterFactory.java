package ru.taskurotta.test.flow;

/**
 * Created by void 04.04.13 13:49
 */
public class FlowArbiterFactory {
	private static FlowArbiter instance;

	public FlowArbiter getInstance() {
		synchronized (FlowArbiterFactory.class) {
			try {
				while (instance == null) {
					FlowArbiterFactory.class.wait();
				}
			} catch (InterruptedException e) {
				// go out
			}
		}
		return instance;
	}

	public void setInstance(FlowArbiter instance) {
		synchronized (FlowArbiterFactory.class) {
			FlowArbiterFactory.instance = instance;
			FlowArbiterFactory.class.notifyAll();
		}
	}
}
