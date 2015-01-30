package ru.taskurotta.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Formatter;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class MemoryAllocationConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(MemoryAllocationConfigurator.class);

    public static final String AUTO_ENABLED = "taskurotta.memory.auto";
    // example value: c:60mb f:30% (p:21 g:25 gd:1 t:22 td:20 q:200)
    public static final String AUTO_PROPORTIONS = "taskurotta.memory.config";

    public static final String MAP_PROCESS_SIZE = "hz.map.process.memory.max-size";
    public static final String MAP_GRAPH_SIZE = "hz.map.graph.memory.max-size";
    public static final String MAP_GRAPH_DECISION = "hz.map.graph-decision.memory.max-size";
    public static final String MAP_TASK_SIZE = "hz.map.task.memory.max-size";
    public static final String MAP_TASK_DECISION_SIZE = "hz.map.decision.memory.max-size";
    public static final String QUEUES = "hz.queue.memory.max-size";

    public static void main(String[] args) {

        System.setProperty(AUTO_ENABLED, "true");
        System.setProperty(AUTO_PROPORTIONS, "c:60Mb f:30% (p:21 g:25 gd:1 t:22 td:20 q:200)");

        calculate(System.getProperties());
    }


    public static Properties calculate(Properties properties) {

        Properties resultProperties = new Properties();

        Boolean isEnabled = getBooleanProperty(properties, AUTO_ENABLED);
        if (isEnabled == null || !isEnabled) {
            return resultProperties;
        }

        int[] data = getProportionProperty(properties, AUTO_PROPORTIONS);

        long totalMemory = Runtime.getRuntime().totalMemory();
        long withoutKernelMemory = totalMemory - data[0] * 1024 * 1024;
        long freeMemory = (long) (1D * withoutKernelMemory / 100 * data[1]);
        long collectionsMemory = withoutKernelMemory - freeMemory;

        int sumOfProportions = 0;
        for (int i = 2; i < 8; i++) {
            sumOfProportions += data[i];
        }

        logger.info("Automatic memory calibration: ");
        long onePartSize = collectionsMemory / sumOfProportions;

        logger.info("Total memory: {}Mb", bytesToMb(totalMemory));
        logger.info("Kernel size: {}Mb", data[0]);
        logger.info("Free memory {}%: {}Mb", data[1], bytesToMb(freeMemory));
        logger.info("One of {} parts size: {}Mb", sumOfProportions, bytesToMb(onePartSize));
        logger.info("Maximum memory of all collections: {}Mb", bytesToMb(collectionsMemory));


        setAndPrint(resultProperties, MAP_PROCESS_SIZE, totalMemory, onePartSize, data[2]);
        setAndPrint(resultProperties, MAP_GRAPH_SIZE, totalMemory, onePartSize, data[3]);
        setAndPrint(resultProperties, MAP_GRAPH_DECISION, totalMemory, onePartSize, data[4]);
        setAndPrint(resultProperties, MAP_TASK_SIZE, totalMemory, onePartSize, data[5]);
        setAndPrint(resultProperties, MAP_TASK_DECISION_SIZE, totalMemory, onePartSize, data[6]);
        setAndPrint(resultProperties, QUEUES, totalMemory, onePartSize, data[7]);

        return resultProperties;
    }

    private static void setAndPrint(Properties properties, String propertyName, long collectionsMemory, long
            onePartSize, int
                                            parts) {
        long size = onePartSize * parts;
        int percentage = (int) (1D * size / collectionsMemory * 100);
        percentage = percentage == 0 ? 1 : percentage;

        long actualSize = (long) (1D * Runtime.getRuntime().totalMemory() / 100 * percentage);
        properties.setProperty(propertyName, "" + percentage);

        logger.info("Add property \"{}\" with value {}% = {} Mb ({} parts)", propertyName, percentage, bytesToMb
                        (actualSize), parts);
    }

    private static String bytesToMb(long bytes) {
        return new Formatter().format("%6.2f", ((double) bytes / 1024 / 1024)).toString();
    }

    public static int[] getProportionProperty(Properties properties, String name) {

        // get system value first!
        String value = System.getProperty(name);
        if (value == null) {
            value = properties.getProperty(name);
            if (value == null) {
                throw new IllegalArgumentException("Required property [" + name + "] not found");
            }
        }

        int[] data = new int[8];
        data[0] = getProportion(value, "k");
        data[1] = getProportion(value, "f");
        data[2] = getProportion(value, "p");
        data[3] = getProportion(value, "g");
        data[4] = getProportion(value, "gd");
        data[5] = getProportion(value, "t");
        data[6] = getProportion(value, "td");
        data[7] = getProportion(value, "q");

        return data;
    }

    private static int getProportion(String value, String prefix) {

        Pattern pattern = Pattern.compile("(?<=" + prefix + ":)\\d+");
        Matcher matcher = pattern.matcher(value);

        if (!matcher.find()) {
            new IllegalArgumentException("Can not find value of [" + prefix + "] on proportions property [" + value +
                    "]");
        }

        String digits = matcher.group();
        int result = Integer.parseInt(digits);

        if (result == 0) {
            new IllegalArgumentException("Property [" + value + "] has zero value of [" + prefix + "]");
        }

        return result;
    }

    public static Boolean getBooleanProperty(Properties properties, String name) {

        // get system value first!
        String value = System.getProperty(name);
        if (value == null) {
            value = properties.getProperty(name);
            if (value == null) {
                return null;
            }
        }

        try {
            return Boolean.parseBoolean(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Wrong format of boolean property [" + name + "] Its value is [" +
                    value + "]");
        }
    }

}
