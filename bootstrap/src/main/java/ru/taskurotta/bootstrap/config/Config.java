package ru.taskurotta.bootstrap.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * User: romario
 * Date: 2/12/13
 * Time: 1:22 PM
 */
public class Config {

    public Map<String, RuntimeConfig> runtimeConfigs = new HashMap<String, RuntimeConfig>();
    public Map<String, SpreaderConfig> spreaderConfigs = new HashMap<String, SpreaderConfig>();
    public Map<String, ProfilerConfig> profilerConfigs = new HashMap<String, ProfilerConfig>();
    public Map<String, RetryPolicyFactory> policyConfigs = new HashMap<String, RetryPolicyFactory>();
    public List<ActorConfig> actorConfigs = new LinkedList<ActorConfig>();

}
