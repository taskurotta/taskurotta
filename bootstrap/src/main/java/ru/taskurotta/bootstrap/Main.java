package ru.taskurotta.bootstrap;

import net.sourceforge.argparse4j.inf.ArgumentParserException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * User: romario, stukushin
 * Date: 2/12/13
 * Time: 5:38 PM
 */
public class Main {

    public static void main(String[] args) throws IOException, ArgumentParserException, ClassNotFoundException {
        Bootstrap bootstrap = new Bootstrap(args);
        bootstrap.start();

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName name = new ObjectName("ru.taskurotta.bootstrap.jmx:type=BootstrapMBean");
            mBeanServer.registerMBean(bootstrap, name);
        } catch (MalformedObjectNameException | NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException e) {
            throw new RuntimeException("Catch exception while start new instance", e);
        }

    }
}
