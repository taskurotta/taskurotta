package ru.taskurotta.server.recovery.base;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.config.model.ExpirationPolicy;
import ru.taskurotta.backend.config.model.ExpirationPolicyConfig;

/**
 * Abstract recovery process launched periodically.
 * Contains Map of actors configuration defined timeout policies
 */
public abstract class AbstractRecovery implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRecovery.class);

    protected int recoveryPeriod = Integer.MAX_VALUE;//Parameter to limit recovery for the given period in the past id needed
    protected TimeUnit recoveryPeriodUnit = TimeUnit.DAYS;

    private Map<String, Map<TimeoutType, ExpirationPolicy>> expirationPolicyMap;//Map of expiration policies initialized by initConfigs(ActorPreferences[] actorPrefs) method

    protected abstract void processRecoveryIteration();

    @Override
    public void run() {
        logger.debug("Recovery process started, expirationPolicies are [{}]", expirationPolicyMap!=null? expirationPolicyMap.keySet(): null);
        processRecoveryIteration();
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
                valueMap = expirationPolicyMap.get("default");
            }

            if(valueMap != null) {
                result = valueMap.get(timeoutType);
            }
        }
        return result;
    }

    public void setRecoveryPeriod(int recoveryPeriod) {
        this.recoveryPeriod = recoveryPeriod;
    }
    public void setRecoveryPeriodUnit(TimeUnit recoveryPeriodUnit) {
        this.recoveryPeriodUnit = recoveryPeriodUnit;
    }

}
