package ru.taskurotta.dropwizard.server.core;

import com.bazaarvoice.dropwizard.assets.ConfiguredAssetsBundle;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.metrics.core.HealthCheck;
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

public class SpringTaskService extends Service<TaskServerConfig> {

    private static final Logger logger = LoggerFactory.getLogger(SpringTaskService.class);
    public static final String SYSTEM_PROP_PREFIX = "ts.";
    public static final String ASSETS_MODE_PROPERTY_NAME = "assetsMode";

    @Override
    public void initialize(Bootstrap<TaskServerConfig> bootstrap) {
        bootstrap.setName("task-queue-service");

        if (System.getProperties().get(ASSETS_MODE_PROPERTY_NAME) != null && System.getProperties().get(ASSETS_MODE_PROPERTY_NAME).toString().equalsIgnoreCase("dev")) {
            bootstrap.addBundle(new ConfiguredAssetsBundle("/assets", "/"));
        } else {
            bootstrap.addBundle(new AssetsBundle("/assets", "/"));
        }

    }

    @Override
    public void run(final TaskServerConfig configuration, Environment environment)
            throws Exception {

        String contextLocation = configuration.getContextLocation();
        AbstractApplicationContext appContext = new ClassPathXmlApplicationContext(contextLocation.split(","), false);
        final Properties props = getMergedProperties(configuration);
        logger.debug("TaskServer properties getted are [{}]", props);

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
                    environment.addResource(resourceSingleton);
                    resourcesCount++;
                }

            }

        } else {//configured in external file
            for (String beanName : configuration.getResourceBeans()) {
                Object resourceSingleton = appContext.getBean(beanName);
                environment.addResource(resourceSingleton);
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
                    environment.addHealthCheck(healthCheck);
                    healthChecksCount++;
                }
            }
        } else {
            for (String hcBeanName : configuration.getHealthCheckBeans()) {
                HealthCheck healthCheck = appContext.getBean(hcBeanName, HealthCheck.class);
                environment.addHealthCheck(healthCheck);
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
        Resource res = new ClassPathResource("default.properties");
        if (res.exists()) {
            result.load(res.getInputStream());
        }

        //2. Override/extend them with properties from external configuration file
        result = extendProps(result, configuration.getProperties(), null);

        //3. Override/extend them with system properties
        result = extendProps(result, System.getProperties(), SYSTEM_PROP_PREFIX);

        //4. Internal pool feature props (if present)
        if (configuration.getInternalPoolConfig() != null) {
            result = extendProps(result, configuration.getInternalPoolConfig().asProperties(), null);
        }

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
        new SpringTaskService().run(args);
    }

}
