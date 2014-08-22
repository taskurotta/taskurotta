package ru.taskurotta.bootstrap.config.logging;

/**
 * Created on 22.08.2014.
 */
public class ConsoleAppenderCfg {

    private boolean enabled = true;
    private String logFormat = "%-4r [%t] %-5p %c - %m%n";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLogFormat() {
        return logFormat;
    }

    public void setLogFormat(String logFormat) {
        this.logFormat = logFormat;
    }

    @Override
    public String toString() {
        return "ConsoleCfg{" +
                "enabled=" + enabled +
                ", logFormat='" + logFormat + '\'' +
                '}';
    }

}
