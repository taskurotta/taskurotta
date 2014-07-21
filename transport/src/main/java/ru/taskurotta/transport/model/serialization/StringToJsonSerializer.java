package ru.taskurotta.transport.model.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Created on 28.04.2014.
 */
public class StringToJsonSerializer  extends com.fasterxml.jackson.databind.JsonSerializer<String> {

    @Override
    public void serialize(String s, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeRawValue(s);
    }

}
