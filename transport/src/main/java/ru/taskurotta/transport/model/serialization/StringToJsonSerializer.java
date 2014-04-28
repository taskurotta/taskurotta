package ru.taskurotta.transport.model.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created on 28.04.2014.
 */
public class StringToJsonSerializer  extends com.fasterxml.jackson.databind.JsonSerializer<String> {

    private static final Logger logger = LoggerFactory.getLogger(StringToJsonSerializer.class);

    @Override
    public void serialize(String s, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        jsonGenerator.writeRawValue(s);
    }

}
