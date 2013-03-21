package ru.taskurotta.recipes;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import ru.taskurotta.bootstrap.Bootstrap;

import java.io.IOException;

/**
 * User: stukushin
 * Date: 19.03.13
 * Time: 12:57
 */
public class Main extends Bootstrap {
    public static void main(String[] args) throws ArgumentParserException, IOException, ClassNotFoundException {
        new Main().run(args);
    }
}
