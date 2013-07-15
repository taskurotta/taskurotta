package ru.taskurotta.bugtest;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import ru.taskurotta.bootstrap.Bootstrap;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 15.07.13 17:08
 */
public class Main {

    public static void main(String[] args) throws IOException, ArgumentParserException, ClassNotFoundException {
        if (args.length == 0) {
            new Bootstrap("conf.yml").start();
        } else {
            new Bootstrap(args).start();
        }
    }

}
