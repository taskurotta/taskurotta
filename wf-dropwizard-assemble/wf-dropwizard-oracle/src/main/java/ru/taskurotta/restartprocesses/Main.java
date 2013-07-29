package ru.taskurotta.restartprocesses;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import ru.taskurotta.dropwizard.server.core.TaskServerConfig;

import java.io.IOException;
import java.util.Properties;

/**
 * User: stukushin
 * Date: 25.07.13
 * Time: 15:34
 */
public class Main {

    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            throw new IllegalArgumentException("Not set config file");
        }

        AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext(new String[]{"classpath:context/restartProcessesContext.xml"}, false);

        Resource resource = applicationContext.getResource("classpath:" + args[0]);

        if (!resource.exists()) {
            throw new IllegalArgumentException("Not found config file");
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        TaskServerConfig config = mapper.readValue(resource.getInputStream(), TaskServerConfig.class);
        Properties properties = config.getProperties();
        properties.setProperty("retryTimes", "10");

        applicationContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("customProperties", properties));
        applicationContext.refresh();

        Restarter restarter = applicationContext.getBean("restarter", Restarter.class);
        restarter.init();
    }
}
