package ru.taskurotta.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.exception.server.ServerConnectionException;
import ru.taskurotta.exception.server.ServerException;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.TaskServerResource;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.utils.TransportUtils;
import ru.taskurotta.util.ActorDefinition;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Task server client implementation: new connection per rest request
 *
 * Created on 15.04.2015.
 */
public class SimpleHttpClient implements TaskServer {

    private static final Logger logger = LoggerFactory.getLogger(SimpleHttpClient.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    private String pollEndpoint;
    private String releaseEndpoint;
    private String startEndpoint;

    private int connTimeoutMs = 0;//0 = infinite
    private int readTimeoutMs = 0;//0 = infinite

    private String contentEncoding = "UTF-8";

    public SimpleHttpClient(String endpoint) throws Exception {
        this(endpoint, 0, 0, null);
    }

    public SimpleHttpClient(String endpoint, int connTimeoutMs, int readTimeoutMs, String contentEncoding) throws Exception {
        this.connTimeoutMs = connTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        if (contentEncoding != null) {
            this.contentEncoding = contentEncoding;
        }

        this.pollEndpoint = TransportUtils.getRestPath(endpoint, TaskServerResource.POLL);
        this.releaseEndpoint = TransportUtils.getRestPath(endpoint, TaskServerResource.RELEASE);
        this.startEndpoint = TransportUtils.getRestPath(endpoint, TaskServerResource.START);
    }

    @Override
    public void startProcess(TaskContainer task) {
        try {
            String reqJson = objectMapper.writeValueAsString(task);
            sendPost(startEndpoint, reqJson);
        } catch (Throwable e) {
            String mes = "Cannot start process for task ["+task+"]";
            logger.error(mes, e);
            throw new ServerConnectionException(mes, e);
        }
    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {
        TaskContainer result = null;

        try {
            String jsonReq = objectMapper.writeValueAsString(actorDefinition);
            String jsonResp = sendPost(pollEndpoint, jsonReq);
            if (jsonResp != null) {
                result = objectMapper.readValue(jsonResp, TaskContainer.class);
            }
        } catch (Throwable e) {
            logger.error("Cannot poll task for actorDefinition["+actorDefinition+"]", e);
        }

        return result;
    }

    @Override
    public void release(DecisionContainer taskResult) {
        try {
            String jsonReq = objectMapper.writeValueAsString(taskResult);
            sendPost(releaseEndpoint, jsonReq);
        } catch (Throwable e) {
            String mes = "Cannot release task result ["+taskResult+"]";
            logger.error(mes, e);
            throw new RuntimeException(mes, e);
        }
    }

    private String sendPost(String endpoint, String data) throws Exception {
        logger.trace("Try to send post, endpoint[{}], data[{}]", endpoint, data);
        String result = null;
        URL url = new URL(endpoint);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");

        con.setReadTimeout(readTimeoutMs);
        con.setConnectTimeout(connTimeoutMs);
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setDefaultUseCaches(false);
        if (data != null) {
            OutputStream ous = con.getOutputStream();
            ous.write(data.getBytes(contentEncoding));
            ous.flush();
            ous.close();
        }

        int responseCode = con.getResponseCode();
        String responseMessage = con.getResponseMessage();

        if (responseCode == 204) {
            result = null;

        } else if (responseCode == 200) {
            InputStream in = con.getInputStream();
            try {
                result = copyToString(in);
            } catch (Exception e) {
                throw new ServerException("Cannot post data ["+data+"] to endpoint["+endpoint+"], response code ["+responseCode+"], message [" + responseMessage + "]", e);
            } finally {
                in.close();
            }
        } else {
            throw new ServerException("Cannot post data ["+data+"] to endpoint["+endpoint+"], response code ["+responseCode+"], message [" + responseMessage + "]");
        }

        logger.debug("Post result is[{}], code[{}], endpoint[{}], data[{}]", result, responseCode, endpoint, data);
        return result;
    }

    public String copyToString(InputStream in) throws IOException {
        String result = null;

        if (in != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte[] data = new byte[1048576]; //1 mb
            long size = 0;
            int bytesRead = 0;
            while ((bytesRead = in.read(data,0,data.length)) != -1) {
                out.write(data, 0, bytesRead);
                size+=bytesRead;
            }
            if (size>0) {
                result = new String(out.toByteArray(), contentEncoding);
            }
        }

        return result;
    }

}
