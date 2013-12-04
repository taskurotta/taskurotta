package ru.taskurotta.recipes;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import ru.taskurotta.bootstrap.Bootstrap;

import java.io.IOException;

/**
 * User: greg
 */
public final class RecipesRunner {

    public static final String ENVIRONMENT_CONF = "recipesEnv";
    public static final String REAL_ENV = "REAL";

    public static void run(String packageName) throws ArgumentParserException, IOException, ClassNotFoundException {
        new Bootstrap(packageName + getProfile()).start();
    }

    private static String getProfile() {
        final String str = System.getenv(ENVIRONMENT_CONF);
        if (str != null && str.equals(REAL_ENV)) {
            return "conf-jersey.yml";
        }
        return "conf.yml";
    }
}
