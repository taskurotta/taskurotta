package ru.taskurotta.example;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import ru.taskurotta.bootstrap.Bootstrap;

import java.io.IOException;

/**
 * User: stukushin
 * Date: 06.02.13
 * Time: 15:31
 */
public class Main {
    public static void main(String[] args) throws IOException, ArgumentParserException, ClassNotFoundException {
        if (args.length == 0) {
            new Bootstrap("ru/taskurotta/example/calculate/wf-config.yml").start();
        } else {
            new Bootstrap(args).start();
        }
    }
}