package ru.taskurotta.service.storage.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.storage.EntityStore;
import ru.taskurotta.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created on 08.06.2015.
 */
public class JsonEntityStore<E> implements EntityStore<E> {
    private static final Logger logger = LoggerFactory.getLogger(JsonEntityStore.class);
    protected ObjectMapper objectMapper = new ObjectMapper();
    public static final String STORE_FILE_EXTENSION = ".json";
    public static final int JSON_FILE_MIN_INDEX = 1;

    protected String storeLocation;
    protected Class<? extends E> entityClass;
    protected File storeDir;

    public JsonEntityStore(Class<? extends E> entityClass, String storeLocation) {
        this.entityClass = entityClass;
        this.storeLocation = storeLocation;
    }

    private File getStore() {
        if (storeDir==null) {
            synchronized (this) {
                if (!StringUtils.isBlank(storeLocation)) {
                    storeDir = new File(storeLocation);
                    storeDir.mkdirs();
                }
                if (!storeDir.exists()) {
                    throw new IllegalStateException("Cannot find or create directory for entity store [" + storeLocation + "]");
                }
                logger.debug("Store dir initialized, location [{}]", storeDir.getPath());
            }
        }
        return storeDir;
    }

    protected int getAvailableFileNumber() {
        int result = JSON_FILE_MIN_INDEX;
        while (new File(getStore(), result + STORE_FILE_EXTENSION).exists()) {
            result++;
        }
        logger.debug("Available file number is [{}]", result);
        return result;
    }

    @Override
    public long add(E entity) {
        int key = getAvailableFileNumber();
        try {
            objectMapper.writeValue(new File(getStore(), key + STORE_FILE_EXTENSION), entity);
            logger.debug("Entity added with key [{}]", key);
        } catch (IOException e) {
            logger.error("Cannot add entity["+entity+"] to store", e);
            key = -1;
        }
        return key;
    }

    @Override
    public void remove (long id) {
        logger.debug("Try to remove entity with id [{}]", id);
        File entityFile = new File(getStore(), id + STORE_FILE_EXTENSION);
        if (entityFile.exists()) {
            entityFile.delete();
        }
        if (entityFile.exists()) {
            throw new IllegalStateException("Cannot delete file with id: " + id);
        }
        logger.debug("Entity with id [{}] successfully removed", id);
    }

    @Override
    public Collection<Long> getKeys() {
        Collection<Long> result = null;
        String[] stores = getStore().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(STORE_FILE_EXTENSION);
            }
        });
        if (stores != null && stores.length>0) {
            result = new ArrayList<>();
            for (String name : stores) {
                try {
                    result.add(Long.valueOf(name.substring(0, name.length() - STORE_FILE_EXTENSION.length())));
                } catch (Exception e) {
                    logger.error("Cannot extract id from file name " + name);
                }
            }
        }
        logger.debug("Keys are [{}]", result);
        return result;
    }

    @Override
    public E get(long id) {
        E result = null;
        File entityFile = new File(getStore(), id + STORE_FILE_EXTENSION);
        if (entityFile.exists()) {
            try {
                result = objectMapper.readValue(entityFile, entityClass);
            } catch (IOException e) {
                logger.error("Cannot read entity file with id["+id+"]", e);
            }
        }
        logger.debug("Entity got by id [{}] is [{}]", id, result);
        return result;
    }

    @Override
    public Collection<E> getAll() {
        Collection<E> result = null;
        File[] stores = getStore().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(STORE_FILE_EXTENSION);
            }
        });
        if (stores != null && stores.length>0) {
            result = new ArrayList<>();
            for (File file : stores) {
                try {
                    result.add(objectMapper.readValue(file, entityClass));
                } catch (Exception e) {
                    logger.error("Cannot parse entity file " + file);
                }
            }
        }
        logger.debug("Entities are [{}]", result);
        return result;
    }


    @Override
    public void update(E entity, long id) {
        if (entity != null && id>=0) {
            try {
                objectMapper.writeValue(new File(getStore(), id + STORE_FILE_EXTENSION), entity);
            } catch (IOException e) {
                logger.error("Cannot update entity["+entity+"] to store", e);
            }
        }
    }

}
