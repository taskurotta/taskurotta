package ru.taskurotta.example.calculate.worker;

import ru.taskurotta.annotation.Worker;

@Worker
public interface Summarizer {
	
	public Integer summarize(Integer a, Integer b);
}
