package ru.taskurotta.bootstrap.config.logging;

/**
 * Created on 22.08.2014.
 */
public class FileAppenderCfg {
    private boolean enabled = true;
    private String logFormat = "%-4r [%t] %-5p %c - %m%n";
    private String currentLogFilename = "log.log";
    private String archivedLogFilenamePattern = "log.%d{yyyy-MM-dd}.log";

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

    public String getCurrentLogFilename() {
        return currentLogFilename;
    }

    public void setCurrentLogFilename(String currentLogFilename) {
        this.currentLogFilename = currentLogFilename;
    }

    public String getArchivedLogFilenamePattern() {
        return archivedLogFilenamePattern;
    }

    public void setArchivedLogFilenamePattern(String archivedLogFilenamePattern) {
        this.archivedLogFilenamePattern = archivedLogFilenamePattern;
    }

    @Override
    public String toString() {
        return "FileCfg{" +
                "enabled=" + enabled +
                ", logFormat='" + logFormat + '\'' +
                ", currentLogFilename='" + currentLogFilename + '\'' +
                ", archivedLogFilenamePattern='" + archivedLogFilenamePattern + '\'' +
                '}';
    }

}
