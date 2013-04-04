package ru.taskurotta.example;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import ru.taskurotta.bootstrap.Bootstrap;
import ru.taskurotta.bootstrap.config.Config;

import java.io.IOException;

/**
 * User: stukushin
 * Date: 06.02.13
 * Time: 15:31
 */
public class Main {
    public static void main(String[] args) throws IOException, ArgumentParserException, ClassNotFoundException {
		Bootstrap bootstrap = new Bootstrap();
		Config config;
		if(args.length == 0) {
			config = bootstrap.parseArgs(new String[]{"-r", "ru/taskurotta/example/notification/wf-config.yml"});
    	} else {
    		config = bootstrap.parseArgs(args);
    	}
		bootstrap.start(config);
    }
}