package ru.taskurotta.dropwizard.server.application;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import ru.taskurotta.dropwizard.server.ServerPropertiesAware;
import ru.taskurotta.service.config.impl.MemoryConfigService;

import javax.ws.rs.Path;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Created on 22.01.2015.
 */
public class SpringApplication extends Application<TaskServerConfig> {

    private static final Logger logger = LoggerFactory.getLogger(SpringApplication.class);

    public static final String SYSTEM_PROP_PREFIX = "ts.";
    public static final String ASSETS_MODE_PROPERTY_NAME = "assetsMode";
    public static final String DEFAULT_PROPERTIES_FILE_NAME = "default.properties";

    @Override
    public void initialize(Bootstrap<TaskServerConfig> bootstrap) {
//        if (System.getProperties().get(ASSETS_MODE_PROPERTY_NAME) != null && System.getProperties().get(ASSETS_MODE_PROPERTY_NAME).toString().equalsIgnoreCase("dev")) {
//
//        } else {
//
//        }
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
    }

    @Override
    public void run(TaskServerConfig configuration, Environment environment) throws Exception {
        environment.jersey().setUrlPattern("/rest/*");
        registerEnvironmentBeans(configuration, environment);
    }


    protected void registerEnvironmentBeans(final TaskServerConfig configuration, Environment environment) throws Exception {
        String contextLocation = configuration.getContextLocation();
        AbstractApplicationContext appContext = new ClassPathXmlApplicationContext(contextLocation.split(","), false);
        final Properties props = getMergedProperties(configuration);
        logger.debug("TaskServer properties got are [{}]", props);

        appContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("customProperties", props));

        //Initializes YamlConfigService bean with actor preferences parsed from DW server YAML configuration
        if (configuration.getActorConfig() != null) {
            appContext.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {
                @Override
                public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                    beanFactory.addBeanPostProcessor(new BeanPostProcessor() {
                        @Override
                        public Object postProcessBeforeInitialization(Object bean, String beanName)
                                throws BeansException {
                            if (bean instanceof MemoryConfigService) {
                                MemoryConfigService cfgBean = (MemoryConfigService) bean;
                                cfgBean.setActorPreferencesCollection(configuration.getActorConfig().getAllActorPreferences());
                                cfgBean.setExpirationPoliciesCollection(configuration.getActorConfig().getAllExpirationPolicies());
                            }

                            if (bean instanceof ServerPropertiesAware) {
                                ServerPropertiesAware spa = (ServerPropertiesAware)bean;
                                spa.setProperties(props);
                            }

                            return bean;
                        }

                        @Override
                        public Object postProcessAfterInitialization(Object bean, String beanName)
                                throws BeansException {
                            return bean;
                        }
                    });
                }
            });
        }
        appContext.refresh();

        logger.debug("configuration.getResourceBeans() [{}]", configuration.getResourceBeans());

        //-----Register resources-----------------
        int resourcesCount = 0;
        if (configuration.getResourceBeans() == null
                || (configuration.getResourceBeans().length == 1 && "auto".equalsIgnoreCase(configuration.getResourceBeans()[0]))) {//find automatically
            Map<String, Object> resources = appContext.getBeansWithAnnotation(Path.class);
            if (resources != null && !resources.isEmpty()) {
                for (String resourceBeanName : resources.keySet()) {
                    Object resourceSingleton = appContext.getBean(resourceBeanName);
                    environment.jersey().register(resourceSingleton);
                    resourcesCount++;
                }

            }

        } else {//configured in external file
            for (String beanName : configuration.getResourceBeans()) {
                Object resourceSingleton = appContext.getBean(beanName);
                environment.jersey().register(resourceSingleton);
                resourcesCount++;
            }
        }
        logger.info("Registered [{}] resources from application context location [{}]", resourcesCount, contextLocation);
        //-----/Register resources-----------------

        //----- Register healthchecks ------------------
        int healthChecksCount = 0;
        if (configuration.getHealthCheckBeans() == null
                || (configuration.getHealthCheckBeans().length == 1 && "auto".equalsIgnoreCase(configuration.getHealthCheckBeans()[0]))) {
            Map<String, HealthCheck> healthChecks = appContext.getBeansOfType(HealthCheck.class);
            if (healthChecks != null && !healthChecks.isEmpty()) {
                for (String hcBeanName : healthChecks.keySet()) {
                    HealthCheck healthCheck = appContext.getBean(hcBeanName, HealthCheck.class);
                    environment.healthChecks().register(healthCheck.getClass().getSimpleName(), healthCheck);
                    healthChecksCount++;
                }
            }
        } else {
            for (String hcBeanName : configuration.getHealthCheckBeans()) {
                HealthCheck healthCheck = appContext.getBean(hcBeanName, HealthCheck.class);
                environment.healthChecks().register(healthCheck.getClass().getSimpleName(), healthCheck);
                healthChecksCount++;
            }
        }
        logger.info("Registered[{}] healthChecks from application context location [{}]", healthChecksCount, contextLocation);
        //----- /Register healthchecks ------------------
    }

    /**
     * @return properties merged from default->configuration file->system props
     */
    protected Properties getMergedProperties(TaskServerConfig configuration) throws IOException {
        Properties result = new Properties();

        //1. defaults from classpath file
        Resource res = new ClassPathResource(DEFAULT_PROPERTIES_FILE_NAME);
        if (res.exists()) {
            result.load(res.getInputStream());
        }

        //2. Override/extend them with properties from external configuration file
        result = extendProps(result, configuration.getProperties(), null);

        //3. Override/extend them with system properties
        result = extendProps(result, System.getProperties(), SYSTEM_PROP_PREFIX);

        return result;
    }

    private Properties extendProps(Properties mergeTo, Properties mergeFrom, String prefix) {
        if (mergeTo == null) {
            return mergeFrom;
        }
        if (mergeFrom != null) {
            for (Map.Entry<Object, Object> entry : mergeFrom.entrySet()) {
                if (prefix != null) {//filter only prefixed properties
                    String stringKey = entry.getKey().toString();
                    if (stringKey.startsWith(prefix)) {
                        mergeTo.put(stringKey.substring(prefix.length()), entry.getValue());
                    }
                } else {
                    mergeTo.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return mergeTo;
    }

    public static void main(String[] args) throws Exception {
        new SpringApplication().run(args);
    }

}
