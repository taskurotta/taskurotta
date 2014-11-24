package ru.taskurotta.bootstrap.config.logging;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 22.08.2014.
 */
public class SimpleLogConfig {
    private String level = "WARN";
    private Map<String, String> loggers = new HashMap<String, String>();
    private ConsoleAppenderCfg console;
    private FileAppenderCfg file;

    public static SimpleLogConfig defaultConfiguration() {
        SimpleLogConfig result = new SimpleLogConfig();
        result.setConsole(new ConsoleAppenderCfg());
        result.setFile(new FileAppenderCfg());
        return result;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Map<String, String> getLoggers() {
        return loggers;
    }

    public void setLoggers(Map<String, String> loggers) {
        this.loggers = loggers;
    }


    public ConsoleAppenderCfg getConsole() {
        return console;
    }

    public void setConsole(ConsoleAppenderCfg console) {
        this.console = console;
    }

    public FileAppenderCfg getFile() {
        return file;
    }

    public void setFile(FileAppenderCfg file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "SimpleLoggingConfig{" +
                "level='" + level + '\'' +
                ", loggers=" + loggers +
                ", console=" + console +
                ", file=" + file +
                '}';
    }
}
