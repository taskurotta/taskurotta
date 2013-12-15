package ru.taskurotta.service.config;

import java.util.List;

/**
 * Date: 09.12.13 10:01
 */
public interface ConfigurationManager {

    /**
     * @return list of top level configuration modules or null
     */
    List<CfgModule> getTopLevelConfigs();

    /**
     * @param unique name of a module
     * @return confuguration module for this name or null
     */
    CfgModule getModuleByName(String name);

    /**
     * @param cfgType type of the configuration of interest
     * @return list of all configuration modules for the configurationType
     */
    List<CfgModule> getConfigsOfAType(String cfgType);


}
