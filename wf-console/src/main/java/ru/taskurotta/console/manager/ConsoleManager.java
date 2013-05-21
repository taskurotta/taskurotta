package ru.taskurotta.console.manager;

import ru.taskurotta.console.model.QueueVO;

import java.util.List;

/**
 * manager interface, providing aggregated info gathered from concrete retrievers implementations
 * User: dimadin
 * Date: 17.05.13 16:03
 */
public interface ConsoleManager {

    public List<QueueVO> getQueuesState();

}
