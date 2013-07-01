package ru.taskurotta.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import ru.taskurotta.exception.server.InvalidServerRequestException;
import ru.taskurotta.exception.server.ServerConnectionException;
import ru.taskurotta.exception.server.ServerException;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskContainerWrapper;
import ru.taskurotta.transport.model.TaskType;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

/**
 * Class to submit new tasks to a task server (for test usage).
 *
 * Task submission configuration description should be in a YAML file somewhere on the FS or classpath
 *
 * Uses code from a Bootstrap and dropwizard modules
 * User: dimadin
 * Date: 15.05.13 18:36
 */
public class TaskSubmitter {

    public static final String START_RESOURCE = "/tasks/start";

    public static void main(String[] args) throws Exception {

        if(args.length == 1) {
            args = new String[]{"-r", args[0]};//Resource by default
        }

        SubmitterConfig config = parseStarterConfig(args);
        if(config!=null && config.getTask()!=null) {
            if(config.getEndpoint() == null || config.getEndpoint().trim().length() == 0) {
                throw new Exception("Field \"endpoint\" should be specified!");
            }
            if(config.getTask().getActorId() == null || config.getTask().getActorId().trim().length() == 0) {
                throw new Exception("Field \"task\"-> \"actorId\" should be specified!");
            }
            if(config.getTask().getMethod() == null || config.getTask().getMethod().trim().length() == 0) {
                throw new Exception("Field \"task\"-> \"method\" should be specified!");
            }
            int submitted = 0;
            Client client = Client.create();
            WebResource startResource = client.resource(getContextUrl(config.getEndpoint(), START_RESOURCE));
            while(submitted < config.getCount()) {
                try {
                    submitNewTask(extendTask(config.getTask()), startResource);
                    submitted++;
                    if(submitted % 100 == 0) {
                        System.out.println("Submitted "+submitted+" tasks out of " + config.getCount());
                    }
                } catch(Throwable ex) {
                    System.out.println("Cannot submit task("+submitted+"/"+config.getCount()+"): " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
            System.out.println("Successfully submitted "+config.getCount()+" tasks");
        }

    }

    //Replaces missing values with defaults
    public static TaskContainer extendTask(TaskContainer target) throws Exception{
        UUID taskId = target.getTaskId()!=null? target.getTaskId(): UUID.randomUUID();
        UUID processId = target.getProcessId() != null? target.getProcessId(): UUID.randomUUID();
        TaskType type = target.getType()!=null? target.getType(): TaskType.DECIDER_START;
        long startTime = target.getStartTime()!=0? target.getStartTime(): -1;
        int numberOfAttempts = target.getNumberOfAttempts()!=0? target.getNumberOfAttempts(): 5;

        return new TaskContainer(taskId, processId, target.getMethod(), target.getActorId(), type, startTime, numberOfAttempts, target.getArgs(), target.getOptions());
    }

    //copied from wf-bootstrap
    public static SubmitterConfig parseStarterConfig(String[] args) throws ArgumentParserException, IOException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser(TaskSubmitter.class.getName());
        parser.addArgument("-f", "--file")
                .required(false)
                .help("Specify config file to use");

        parser.addArgument("-r", "--resource")
                .required(false)
                .help("Specify resource file (in classpath) to use");

        Namespace namespace = parser.parseArgs(args);

        SubmitterConfig config = null;
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        String configFileName = namespace.getString("file");

        if (configFileName != null) {

            File configFile = new File(configFileName);
            if (configFile.exists()) {
                config = mapper.readValue(configFile, SubmitterConfig.class);
            } else {
                System.out.println("Configuration file doesn't exist: " + configFileName);
                parser.printHelp();
                return null;
            }

        }

        String resourceFileName = namespace.getString("resource");

        if (resourceFileName != null) {

            URL configPath = Thread.currentThread().getContextClassLoader().getResource(resourceFileName);

            if (configPath != null) {
                config = mapper.readValue(configPath, SubmitterConfig.class);
            } else {
                System.out.println("Resource file (in classpath) doesn't exist: " + resourceFileName);
                parser.printHelp();
                return null;
            }
        }

        if (config == null) {
            System.out.println("Config file doesn't specified");
            parser.printHelp();
            return null;
        }

        return config;

    }

    //copied from wf-dropwizard
    public static void submitNewTask(TaskContainer task, WebResource startResource) {
        try {
            WebResource.Builder rb = startResource.getRequestBuilder();
            rb.type(MediaType.APPLICATION_JSON);
            rb.accept(MediaType.APPLICATION_JSON);
            rb.post(new TaskContainerWrapper(task));
        } catch(UniformInterfaceException ex) {//server responded with error
            int status = ex.getResponse()!=null? ex.getResponse().getStatus(): -1;
            if(status>=400 && status<500) {
                throw new InvalidServerRequestException("Start process["+task.getProcessId()+"] with task["+task.getTaskId()+"] error: " + ex.getMessage(), ex);
            } else {
                throw new ServerException("Start process["+task.getProcessId()+"] with task["+task.getTaskId()+"] error: " + ex.getMessage(), ex);
            }
        } catch(ClientHandlerException ex) {//client processing error
            throw new ServerConnectionException("Start process["+task.getProcessId()+"] with task["+task.getTaskId()+"] error: " + ex.getMessage(), ex);
        } catch(Throwable ex) {//unexpected error
            throw new ServerException("Start process["+task.getProcessId()+"] with task["+task.getTaskId()+"] error: " + ex.getMessage(), ex);
        }

    }

    private static String getContextUrl(String endpoint, String path) {
        return endpoint.replaceAll("/*$", "") + "/" + path.replaceAll("^/*", "");
    }

}
