package ru.taskurotta.example.calculate.worker.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.example.calculate.worker.Multiplier;

public class MultiplierImpl implements Multiplier {
	
	private static final Logger logger = LoggerFactory.getLogger(MultiplierImpl.class);
	
	private long sleep = -1l;
	
	@Override
	public Integer multiply(Integer a, Integer b) {
		
		if(sleep > 0) {
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				logger.error("Sleep interrupted", e);
			}			
		}
		
		Integer result =  a * b;
		
		logger.debug("Multiplication result is[{}]", result);
		return result;
	}

	public void setSleep(long sleep) {
		this.sleep = sleep;
	}
	
}
