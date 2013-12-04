package ru.taskurotta.recipes;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import ru.taskurotta.bootstrap.Bootstrap;

import java.io.IOException;

/**
 * User: greg
 */
public final class RecipesRunner {

    public static void run(String packageName) throws ArgumentParserException, IOException, ClassNotFoundException {
        new Bootstrap(packageName + getProfile()).start();
    }

    private static String getProfile() {
        return "conf.yml";
    }
}
