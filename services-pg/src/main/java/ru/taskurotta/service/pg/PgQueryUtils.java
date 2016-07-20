package ru.taskurotta.service.pg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.util.StringUtils;

public class PgQueryUtils {
    private static ObjectMapper mapper = new ObjectMapper();

    public static PGobject asJsonbObject(Object object) {
        try {
            PGobject result = null;
            String json = mapper.writeValueAsString(object);
            if (StringUtils.hasText(json)) {
                result = new PGobject();
                result.setType("jsonb");
                result.setValue(json);
            }
            return result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PGobject asJsonbString(String json) {
        try {
            PGobject result = null;
            if (StringUtils.hasText(json)) {
                result = new PGobject();
                result.setType("jsonb");
                result.setValue(json);
            }
            return result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T>T readValue(String json, Class<T> clazz) {
        T result = null;
        if (json != null) {
            try {
                result = mapper.readValue(json, clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

}
