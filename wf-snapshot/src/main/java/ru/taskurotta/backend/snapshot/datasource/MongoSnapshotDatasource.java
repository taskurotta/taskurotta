package ru.taskurotta.backend.snapshot.datasource;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import ru.taskurotta.backend.snapshot.Snapshot;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * User: greg
 */
public class MongoSnapshotDatasource implements SnapshotDataSource {

    private MongoTemplate mongoTemplate;

    public MongoSnapshotDatasource(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(Snapshot snapshot) {
        mongoTemplate.save(snapshot);
    }

    @Override
    public Snapshot loadSnapshotById(UUID id) {
        return mongoTemplate.findById(id, Snapshot.class);
    }

    @Override
    public List<Snapshot> getSnapshotsForPeriod(Date startDate, Date endDate) {
        throw new java.lang.UnsupportedOperationException();
    }

    @Override
    public List<Snapshot> loadSnapshotsByProccessId(UUID id) {
        final Query query = new Query(where("processId").is(id));
        return mongoTemplate.find(query, Snapshot.class);
    }
}
