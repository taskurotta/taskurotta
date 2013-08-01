package ru.taskurotta.test.render.result;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.StringWriter;
import java.util.TreeMap;

/**
 * User: greg
 * Tool class for creating html test result page. Based on velocity template engine.
 */
public class ChartRender {

    private final static Logger log = LoggerFactory.getLogger(ChartRender.class);

    public static final String TEMPLATE = "page.vm";

    public static void saveDataToFile(String title, String xAxisTitle, String yAxisTitle, TreeMap<Double, Double> data) {
        try {
            String currentDir = System.getProperty("user.dir");
            VelocityEngine ve = new VelocityEngine();
            ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
            ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
            VelocityContext context = new VelocityContext();
            StringWriter writer = new StringWriter();
            Template template = ve.getTemplate(TEMPLATE);
            context.put("chartTitle", title);
            context.put("xTitle", xAxisTitle);
            context.put("yTitle", yAxisTitle);
            context.put("axisData", data);
            template.merge(context, writer);
            FileUtils.writeStringToFile(new File(currentDir + "/result.html"), writer.toString());
        } catch (Exception e) {
            log.error("Chart test result page creation failed", e);
        }
    }

    public static void main(String... args) {
        TreeMap<Double, Double> data = new TreeMap<>();
        data.put(100.0, 20.5);
        data.put(200.0, 25.0);
        data.put(300.0, 27.5);
        saveDataToFile("Test chart", "xt", "yt", data);
    }
}

