package ru.taskurotta.backend.storage.model.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: moroz
 * Date: 11.04.13
 */
public class JsonSerializer<T> implements ModelSerializer<T> {

    private ObjectMapper mapper = new ObjectMapper();
    private final Class<T> genericType;
    private final static Logger log = LoggerFactory.getLogger(JsonSerializer.class);

    public JsonSerializer(Class<T> tClass) {
        this.genericType = tClass;
    }

    @Override
    public T deserialize(Object json) {
        try {
            return (T) mapper.readValue((String) json, genericType);
        } catch (IOException ex) {
            log.error("Serialization exception: " + ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public Object serialize(T obj) {

        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Serialization exception: " + e.getMessage(), e);
        }
        return null;
    }
}
