package ru.taskurotta.util;

import java.util.Properties;

/**
 */
public class PropertiesUtil {

    public static Properties addProperties(Properties mergeTo, Properties mergeFrom, String prefix) {
        if (mergeTo == null) {
            return mergeFrom;
        }

        if (mergeFrom != null) {
            for (String stringKey : mergeFrom.stringPropertyNames()) {
                if (prefix != null) {//filter only prefixed properties
                    if (stringKey.startsWith(prefix)) {
                        mergeTo.setProperty(stringKey.substring(prefix.length()), mergeFrom.getProperty(stringKey));
                    }
                } else {
                    mergeTo.setProperty(stringKey, mergeFrom.getProperty(stringKey));
                }
            }
        }

        return mergeTo;
    }

    public static Properties addProperties(Properties mergeTo, Properties mergeFrom) {
        return addProperties(mergeTo, mergeFrom, null);
    }

}
