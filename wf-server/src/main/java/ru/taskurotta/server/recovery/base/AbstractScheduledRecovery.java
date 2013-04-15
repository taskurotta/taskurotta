package ru.taskurotta.server.recovery.base;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.server.config.expiration.ExpirationPolicy;

/**
 * Abstract recovery process launched periodically by String expression (non-cron by default impl),
 * ex. [every]"50 SECONDS" (template: "<Integer>value <TimeUnit>valueUnit")
 */
public abstract class AbstractScheduledRecovery implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AbstractScheduledRecovery.class);

    private String schedule;//shedule String, ex "100 SECONDS"

   // private TimeoutType timeoutType;//timeout type this recovery process interested in

    protected int recoveryPeriod = Integer.MAX_VALUE;//Parameter to limit recovery for the given period in the past id needed
    protected TimeUnit recoveryPeriodUnit = TimeUnit.DAYS;

    private Map<String, ExpirationPolicy> expirationPolicyMap;//Map of expiration policies initialized by initConfigs(ActorPreferences[] actorPrefs) method

    @Override
    public void run() {
        logger.debug("Recovery process daemon started. Schedule[{}], expirationPolicies for[{}]", schedule, expirationPolicyMap!=null? expirationPolicyMap.keySet(): null);
        while(repeat(schedule)) {
            processRecoveryIteration();
        }
    }

    protected abstract void processRecoveryIteration();

    protected void initConfigs(ActorPreferences[] actorPrefs) {
        logger.debug("Initializing recovery config with actorPrefs[{}]", actorPrefs);
        if (actorPrefs != null) {
            try {
                expirationPolicyMap = new HashMap<String, ExpirationPolicy>();
                for(ActorPreferences actorConfig: actorPrefs) {
                    ExpirationPolicy expPolicy = null;
                    ActorPreferences.ExpirationPolicyConfig expPolicyConf = actorConfig.getExpirationPolicy();

                    if (expPolicyConf != null) {
                        Class<?> expPolicyClass = Class.forName(expPolicyConf.getClassName());
                        Properties expPolicyProps = expPolicyConf.getProperties();

                        if (expPolicyProps != null) {
                            expPolicy = (ExpirationPolicy) expPolicyClass.getConstructor(Properties.class).newInstance(expPolicyProps);
                        } else {
                            expPolicy = (ExpirationPolicy) expPolicyClass.newInstance();
                        }

                        expirationPolicyMap.put(actorConfig.getId(), expPolicy);
                    }

                }

            } catch (Exception e) {
                logger.error("AbstractSimpleScheduledRecovery#initConfigs invocation failed! actorPrefs[" + actorPrefs + "]", e);
                throw new RuntimeException(e);
            }
        }
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    protected boolean repeat(String schedule) {
        if (schedule == null) {
            return false;
        }
        Integer number = Integer.valueOf(schedule.replaceAll("\\D", "").trim());
        TimeUnit unit = TimeUnit.valueOf(schedule.replaceAll("\\d", "").trim());
        try {
            Thread.sleep(unit.toMillis(number));
        } catch (InterruptedException e) {
            logger.error(getClass().getName() + " schedule interrupted", e);
        }
        return true;
    }

    protected ExpirationPolicy getExpirationPolicy(String entityType) {
        ExpirationPolicy result = null;
        if(entityType!=null && expirationPolicyMap!=null) {
            result = expirationPolicyMap.get(entityType);
            if(result == null) {
                result = expirationPolicyMap.get("default");
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
//    public void setTimeoutType(TimeoutType timeoutType) {
//        this.timeoutType = timeoutType;
//    }
//    public TimeoutType getTimeoutType() {
//        return timeoutType;
//    }

}
