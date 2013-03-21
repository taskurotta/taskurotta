package ru.taskurotta.poc.mongo;

import com.mongodb.Mongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ru.taskurotta.poc.mongo.dao.TaskDao;
import ru.taskurotta.poc.mongo.test.CreateTask;
import ru.taskurotta.poc.mongo.test.DBTest;
import ru.taskurotta.poc.mongo.test.LockTask;
import ru.taskurotta.poc.mongo.test.LockTaskThree;
import ru.taskurotta.poc.mongo.test.LockTaskTwo;
import ru.taskurotta.poc.mongo.test.SimpleTask;
import ru.taskurotta.poc.mongo.test.SimpleUpdateTask;
import ru.taskurotta.poc.mongo.test.UpdateWithSpacesTask;

/**
 *
 */
@Configuration
@PropertySource("classpath:db.properties")
public class MongoConfiguration extends AbstractMongoConfiguration {

	@Autowired
	Environment env;

	private static final String dbName = "SWF";

	@Value("${mongo.host}")
	private String host;
	@Value("${mongo.db}")
	private String database;

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
		PropertySourcesPlaceholderConfigurer bean = new PropertySourcesPlaceholderConfigurer();
		bean.setIgnoreResourceNotFound(true);
		bean.setLocation(new FileSystemResource("db.properties"));
		bean.setLocation(new ClassPathResource("db.properties"));
		return bean;
	}

	@Override
	public String getDatabaseName() {
		return dbName;
	}

	@Override
	public @Bean Mongo mongo() throws Exception {
		return new Mongo(host);
	}

	public @Bean
	TaskDao taskDao() throws Exception {

		TaskDao taskDao = new TaskDao();
		taskDao.setDB(mongo(), database);
		return taskDao ;

	}

	public @Bean(destroyMethod = "finish")
	Launcher launcher() throws Exception {
		Launcher launcher = new Launcher();
		launcher.setExecutor(executor());
		launcher.setDao(taskDao());
		return launcher;
	}

	public @Bean
	ThreadPoolTaskExecutor executor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(50);
		executor.setMaxPoolSize(100);
		executor.setQueueCapacity(5);
		executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		return executor;
	}

	public @Bean
	SimpleUpdateTask simpleUpdate() throws Exception {
		return new SimpleUpdateTask();
	}
	public @Bean
	DBTest updateWithSpaces() throws Exception {
		return new UpdateWithSpacesTask();
	}
	public @Bean
	DBTest createTask() throws Exception {
		return new CreateTask();
	}
	public @Bean
	DBTest lockTaskOne() throws Exception {
		return new LockTask();
	}

	public @Bean
	DBTest lockTaskTwo() throws Exception {
		return new LockTaskTwo();
	}

	public @Bean
	DBTest lockTaskThree() throws Exception {
		return new LockTaskThree();
	}

    public @Bean
    DBTest createSimpleTask() throws Exception {
        return new SimpleTask();
    }
}