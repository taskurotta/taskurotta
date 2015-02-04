package ru.taskurotta.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 */
public class PropertiesUtil {

    public static final String PROP_DUMP_PROPERTIES = "dumpProperties";

    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

    public static Properties mergeProperties(Properties target, Properties source, Properties traceSource, String
            sourceName, String prefix) {
        if (target == null || source == null) {
            return target;
        }

        for (Object key : source.keySet()) {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException("Key property is not instance of String: " + key
                        .getClass().getName() + " toString() = " + key.toString());
            }

            String sourceKey = (String) key;
            String targetKey = (String) key;

            if (prefix != null) {
                targetKey = sourceKey.substring(prefix.length());
            }

            Object targetValue = target.get(targetKey);
            Object sourceValue = source.get(sourceKey);

            if (targetValue != null && sourceValue != null && targetValue.equals(sourceValue)) {
                continue;
            }

            target.setProperty(targetKey, sourceValue.toString());

            if (traceSource != null) {
                traceSource.setProperty(targetKey, sourceName);
            }
        }

        return target;
    }

    public static Properties mergeProperties(Properties target, Properties source, Properties traceSource, String
            sourceName) {
        return mergeProperties(target, source, traceSource, sourceName, null);
    }

    public static void dumpProperties(Properties properties, Properties traceSource) {

        if (!Boolean.getBoolean(PROP_DUMP_PROPERTIES) ||
                !Boolean.valueOf(properties.getProperty(PROP_DUMP_PROPERTIES))) {
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos, true);

        out.println("\n========= Application properties ==========");

        if (properties == null) {
            out.println("no properties (null)...");
        } else {
            Set rowKeys = properties.keySet();
            if (rowKeys == null) {
                out.println("no properties...");
            } else {

                TreeSet<String> orderedSet = new TreeSet<String>(rowKeys);
                for (String key : orderedSet) {
                    out.printf("%1$s\t %2$s = \"%3$s\"\n", traceSource.get(key), key, properties.get(key));
                }
                out.println();
            }
        }

        logger.info(new String(baos.toByteArray()));

    }


}
