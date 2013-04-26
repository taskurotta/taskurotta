package ru.taskurotta.server.recovery.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.config.model.ExpirationPolicy;
import ru.taskurotta.backend.config.model.ExpirationPolicyConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Abstract recovery process launched periodically.
 * Contains Map of actors configuration defined timeout policies
 */
public abstract class AbstractRecovery implements Runnable {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected List<CheckpointService> checkpointServices;

    protected int recoveryPeriod = Integer.MAX_VALUE;//Parameter to limit recovery for the given period in the past id needed
    protected TimeUnit recoveryPeriodUnit = TimeUnit.DAYS;

    private Map<String, Map<TimeoutType, ExpirationPolicy>> expirationPolicyMap;//Map of expiration policies initialized by initConfigs(ActorPreferences[] actorPrefs) method

    protected abstract void processRecoveryIteration();

    @Override
    public void run() {
        List<CheckpointService> cs = getCheckpointServices();
        if(cs!=null && !cs.isEmpty()) {
            logger.debug("Recovery process started, checkpointServices count[{}], expirationPolicies are [{}]", cs.size(), expirationPolicyMap!=null? expirationPolicyMap.keySet(): null);
            try {
                processRecoveryIteration();
            } catch(Throwable ex) {//Recovery should try to survive no matter what
               logger.error("Unexpected error at recovery process. Recover will continue as scheduled...", ex);
            }
        } else {
            logger.error("Cannot start recovery process: CheckpointService is not set");
        }
    }

    protected void initConfigs(ActorPreferences[] actorPrefs, ExpirationPolicyConfig[] expPolicies) {
        logger.debug("Initializing recovery config with actorPrefs[{}], expPolicies[{}]", actorPrefs, expPolicies);
        if (actorPrefs != null) {
            try {
                expirationPolicyMap = new HashMap<String, Map<TimeoutType, ExpirationPolicy>>();
                for(ActorPreferences actorConfig: actorPrefs) {
                    Properties actorTimeoutPolicies = actorConfig.getTimeoutPolicies();
                    if(actorTimeoutPolicies!=null && !actorTimeoutPolicies.isEmpty()) { //actor has expiration timeouts defined, try to initialize their policies
                        Map<TimeoutType, ExpirationPolicy> actorPolicyInstancesMap = new HashMap<TimeoutType, ExpirationPolicy>();

                        for(Object timeoutType: actorTimeoutPolicies.keySet()) {//iterating over actor's timeoutType-policy map
                            ExpirationPolicyConfig expPolicyConf = getPolicyByName(actorTimeoutPolicies.getProperty(timeoutType.toString()), expPolicies);

                            if (expPolicyConf != null) {
                                Class<?> expPolicyClass = Class.forName(expPolicyConf.getClassName());
                                Properties expPolicyProps = expPolicyConf.getProperties();

                                ExpirationPolicy instance = null;
                                if (expPolicyProps != null) {
                                    instance = (ExpirationPolicy) expPolicyClass.getConstructor(Properties.class).newInstance(expPolicyProps);
                                } else {
                                    instance = (ExpirationPolicy) expPolicyClass.newInstance();
                                }
                                actorPolicyInstancesMap.put(TimeoutType.forValue(timeoutType.toString()), instance);

                            } else {
                                throw new Exception("Not found ExpirationPolicy config for name["+actorTimeoutPolicies.get(timeoutType)+"]. Typo in confg?");
                            }
                        }

                        expirationPolicyMap.put(actorConfig.getId(), actorPolicyInstancesMap);
                    }
                }

            } catch (Exception e) {
                logger.error("AbstractSimpleScheduledRecovery#initConfigs invocation failed! actorPrefs[" + actorPrefs + "]", e);
                throw new RuntimeException(e);
            }
        }
    }

    private ExpirationPolicyConfig getPolicyByName(String name, ExpirationPolicyConfig[] expPolicies) {
        if(expPolicies!=null) {
            for(ExpirationPolicyConfig item: expPolicies) {
                if(name.equals(item.getName())) {
                    return item;
                }
            }
        }
        return null;
    }

    protected ExpirationPolicy getExpirationPolicy(String entityType, TimeoutType timeoutType) {
        ExpirationPolicy result = null;
        if(entityType!=null && expirationPolicyMap!=null) {
            Map<TimeoutType, ExpirationPolicy> valueMap = expirationPolicyMap.get(entityType);
            if(valueMap == null) {
                logger.trace("Not found expiration policy config for entityType[{}], timeoutType[{}]. Applying defaults[{}]", entityType, timeoutType, valueMap);
                valueMap = expirationPolicyMap.get("default");
            }

            if(valueMap != null) {
                result = valueMap.get(timeoutType);
            }
        }
        logger.trace("ExpirationPolicy getted for entityType[{}] is [{}]", entityType, result);
        return result;
    }

    public void setRecoveryPeriod(int recoveryPeriod) {
        this.recoveryPeriod = recoveryPeriod;
    }
    public void setRecoveryPeriodUnit(TimeUnit recoveryPeriodUnit) {
        this.recoveryPeriodUnit = recoveryPeriodUnit;
    }

    public void setCheckpointServices(List<CheckpointService> checkpointServices) {
        this.checkpointServices = checkpointServices;
    }

    public List<CheckpointService> getCheckpointServices() {
        return checkpointServices;
    }
}
