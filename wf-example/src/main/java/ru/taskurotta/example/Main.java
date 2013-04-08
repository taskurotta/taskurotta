package ru.taskurotta.example;

import java.io.IOException;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import ru.taskurotta.bootstrap.Bootstrap;
import ru.taskurotta.bootstrap.config.Config;

/**
 * User: stukushin
 * Date: 06.02.13
 * Time: 15:31
 */
public class Main {
    public static void main(String[] args) throws IOException, ArgumentParserException, ClassNotFoundException {
        Bootstrap bootstrap = new Bootstrap();
        Config config;
        if (args.length == 0) {
//            config = bootstrap.parseArgs(new String[]{"-r", "wf-config.yml"});
//    		new Main().run(new String[]{"-r", "wf-config-example2.yml"});

//    		Для работы теста сначала нужно запустить DW сервер (wf-server-dw/run.bat)
            config = bootstrap.parseArgs(new String[]{"-r", "wf-config-example2-jersey.yml"});
        } else {
            config = bootstrap.parseArgs(args);
        }
        bootstrap.start(config);
    }
}