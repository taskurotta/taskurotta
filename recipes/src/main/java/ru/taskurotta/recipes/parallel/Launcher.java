package ru.taskurotta.recipes.parallel;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import ru.taskurotta.bootstrap.Bootstrap;

import java.io.IOException;

/**
 * User: stukushin
 * Date: 04.04.2014
 * Time: 15:11
 */

public class Launcher {

    public static void main(String[] args) throws ArgumentParserException, IOException, ClassNotFoundException {
        if (args.length > 0) {
            new Bootstrap(args).start();
        } else {
            new Bootstrap("ru/taskurotta/recipes/parallel/wf-config.yml").start();
        }
    }

}
