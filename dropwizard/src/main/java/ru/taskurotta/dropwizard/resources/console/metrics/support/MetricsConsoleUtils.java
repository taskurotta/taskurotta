package ru.taskurotta.dropwizard.resources.console.metrics.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.OptionVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Static utility method set for metric POJO manipulation
 * User: dimadin
 * Date: 25.10.13 11:49
 */
public class MetricsConsoleUtils implements MetricsConstants {
    private static final Logger logger = LoggerFactory.getLogger(MetricsConsoleUtils.class);

    public static List<String> extractDatasets(String datasetStr) {
        logger.debug("Extracting dataset names from string[{}]", datasetStr);
        List<String> result = new ArrayList<>();
        if(datasetStr!=null && datasetStr.trim().length() > 0) {
            for(String dataset: datasetStr.split(",")) {
                if(dataset!=null) {
                    result.add(dataset.trim());
                }
            }
        }
        return result;
    }

    private static String constructLabel(String label, String dataType, String period) {
        if (OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            return "X: time, min; Y: count. " + label;
        } else if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            return "X: time, s; Y: count. " + label;
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            return "X: time, min; Y: mean, ms. " + label;
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            return "X: time, s; Y: mean, ms. " + label;
        } else {
            return label;
        }
    }

    public static List<OptionVO> setIsGeneralFlag(List<OptionVO> target, String dataSetValueToMark) {
        if(target!=null && !target.isEmpty()) {
            for(OptionVO option: target) {
                if(dataSetValueToMark.equals(option.getValue())) {
                    option.setGeneral(true);
                }
            }
        }
        return target;
    }

    public static List<OptionVO> getAsOptions(Collection<String> metricsNames) {
        List<OptionVO> result = new ArrayList<>();
        int i = 0;

        if (metricsNames != null && !metricsNames.isEmpty()) {
            for(String metricsName : metricsNames) {
                OptionVO item = new OptionVO();
                item.setName(metricsName);
                item.setValue(metricsName);
                result.add(item);
            }
        }

        return result;
    }

    /**
     * Converts Map of name-description values to a OptionVO list
     */
    public static List<OptionVO> getOptionsFromMap(Map<String, String> target) {
        List<OptionVO> result = new ArrayList<>();
        if (target != null && !target.isEmpty()) {
            for (String key: target.keySet()) {
                OptionVO option= new OptionVO();
                option.setName(target.get(key));
                option.setValue(key);
                result.add(option);
            }
        }
        return result;
    }

    public static String getXLabel(String dataType, String period) {
        if (OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            return "Timeline: minutes ago";
        } else if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            return "Timeline: seconds ago";
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            return "Timeline: minutes ago";
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            return "Timeline: seconds ago";
        } else if (OPT_DATATYPE_ITEMS.equals(dataType) && (OPT_PERIOD_HOUR.equals(period) || OPT_PERIOD_DAY.equals(period)) ) {
            return "Timeline: minutes ago";
        } else if (OPT_DATATYPE_ITEMS.equals(dataType) && OPT_PERIOD_5MINUTES.equals(period)) {
            return "Timeline: seconds ago";
        } else {
            return "";
        }
    }

    public static String getYLabel(String dataType, String period) {
        if (OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            return "Measured, times";
        } else if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            return "Measured, times";
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            return "Mean time, ms";
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            return "Mean time, ms";
        } else if (OPT_DATATYPE_ITEMS.equals(dataType)) {
            return "Size, items";
        } else {
            return "";
        }
    }


}
