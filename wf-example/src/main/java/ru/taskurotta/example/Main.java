package ru.taskurotta.example;

import java.io.IOException;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import ru.taskurotta.bootstrap.Bootstrap;

/**
 * User: stukushin
 * Date: 06.02.13
 * Time: 15:31
 */
public class Main {
    public static void main(String[] args) throws IOException, ArgumentParserException, ClassNotFoundException {
        if(args.length == 0) {
            new Bootstrap("ru/taskurotta/example/notification/wf-config.yml").start();
        } else {
            new Bootstrap(args).start();
        }
    }
}