package ru.taskurotta.mongo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * User: stukushin
 * Date: 27.12.12
 * Time: 14:03
 */
public class MongoStorage implements Storage {
    Logger log = LoggerFactory.getLogger(getClass());

    private MongoTemplate mongoTemplate;

    private final String journalCollectionName = "journal";

    public void init() {
        if (mongoTemplate.collectionExists(journalCollectionName)) {
            log.info("Collection [{}] exists", journalCollectionName);
        } else {
            mongoTemplate.createCollection(journalCollectionName);
            log.info("Create collection [{}]", journalCollectionName);
        }
    }

    @Override
    public void saveToJournal(Object task) throws InterruptedException {
        log.trace("Try to save task [{}] to journal", task);

        // todo stukushin : maybe do it in several threads?
        mongoTemplate.insert(task, journalCollectionName);
    }

    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
}
