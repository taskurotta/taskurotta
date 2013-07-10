package ru.taskurotta.server.standalone;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import ru.taskurotta.backend.hz.server.HazelcastTaskServer;

/**
 * User: stukushin
 * Date: 09.07.13
 * Time: 12:01
 */
public class Main {
    public static void main(String[] args) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("ru/taskurotta/server/standalone/serverContext.xml");
        HazelcastTaskServer taskServer = applicationContext.getBean("taskServer", HazelcastTaskServer.class);
        taskServer.init();
    }
}
