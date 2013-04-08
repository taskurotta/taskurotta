package ru.taskurotta.bootstrap.config;

/**
 * User: stukushin
 * Date: 04.02.13
 * Time: 17:52
 */
public class ActorConfig {

    private String actorInterface;
    private String runtimeConfig;
    private String spreaderConfig;
    private String profilerConfig;
    private String policyConfig;
    private int count = 1;

    public String getActorInterface() {
        return actorInterface;
    }

    public void setActorInterface(String actorInterface) {
        this.actorInterface = actorInterface;
    }

    public String getRuntimeConfig() {
        return runtimeConfig;
    }

    public void setRuntimeConfig(String runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public String getSpreaderConfig() {
        return spreaderConfig;
    }

    public void setSpreaderConfig(String spreaderConfig) {
        this.spreaderConfig = spreaderConfig;
    }

    public String getProfilerConfig() {
        return profilerConfig;
    }

    public void setProfilerConfig(String profilerConfig) {
        this.profilerConfig = profilerConfig;
    }

    public String getPolicyConfig() {
        return policyConfig;
    }

    public void setPolicyConfig(String policyConfig) {
        this.policyConfig = policyConfig;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
