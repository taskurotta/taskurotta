package ru.taskurotta.test;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import ru.taskurotta.bootstrap.Bootstrap;

import java.io.IOException;

/**
 * Created by greg on 14/01/15.
 */
public class Main {
    public static void main(String[] args) throws ArgumentParserException, IOException, ClassNotFoundException {
        if (args.length == 0) {
            new Bootstrap("ru/taskurotta/test/time/conf.yml").start();
        } else {
            new Bootstrap(args).start();
        }
    }
}
