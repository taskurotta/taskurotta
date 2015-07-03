package ru.taskurotta.test;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import ru.taskurotta.bootstrap.Bootstrap;

import java.io.IOException;

public class MainTimeout {
    public static void main(String[] args) throws IOException, ArgumentParserException, ClassNotFoundException {
        if (args.length == 0) {
            new Bootstrap("ru/taskurotta/test/fullfeature/conf-jersey-timeout.yml").start();
        } else {
            new Bootstrap(args).start();
        }
    }
}
