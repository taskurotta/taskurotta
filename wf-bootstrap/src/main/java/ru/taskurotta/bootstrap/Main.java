package ru.taskurotta.bootstrap;

import java.io.IOException;

import net.sourceforge.argparse4j.inf.ArgumentParserException;

/**
 * User: romario
 * Date: 2/12/13
 * Time: 5:38 PM
 */
public class Main {

    public static void main(String[] args) throws IOException, ArgumentParserException, ClassNotFoundException {
        new Bootstrap(args).start();
    }
}
