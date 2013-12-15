package ru.taskurotta.service.config;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * General model object for configuration module
 * Date: 09.12.13 10:04
 */
public class CfgModule implements Serializable {

    /**
     * Name of the module (should be unique for each module)
     */
    protected String name;

    /**
     * User readable description (if any)
     */
    protected String description;

    /**
     * Type of configuration this module is for. ActorId, processId, task server background process name, etc
     */
    protected String cfgType;

    /**
     * Actual configuration properties for this module
     */
    protected Map<String, CfgProperty> props;

    /**
     * Dependent child modules names for this module
     */
    protected List<String> child;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, CfgProperty> getProps() {
        return props;
    }

    public void setProps(Map<String, CfgProperty> props) {
        this.props = props;
    }

    public String getCfgType() {
        return cfgType;
    }

    public void setCfgType(String cfgType) {
        this.cfgType = cfgType;
    }
}
