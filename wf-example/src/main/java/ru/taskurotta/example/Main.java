package ru.taskurotta.example;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import ru.taskurotta.bootstrap.Bootstrap;

import java.io.IOException;

/**
 * User: stukushin
 * Date: 06.02.13
 * Time: 15:31
 */
public class Main extends Bootstrap {
    public static void main(String[] args) throws IOException, ArgumentParserException, ClassNotFoundException {
    	
    	if(args.length == 0) {
    		new Main().run(new String[]{"-r", "wf-config.yml"});
//    		new Main().run(new String[]{"-r", "wf-config-example2.yml"});
    		
//    		Для работы теста сначала нужно запустить DW сервер (wf-server-dw/run.bat)
//    		new Main().run(new String[]{"-r", "wf-config-example2-jersey.yml"});
    	} else {
    		new Main().run(args);
    	}

    }
}