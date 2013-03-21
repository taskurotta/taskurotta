package ru.taskurotta.mongo;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import org.junit.Before;
import org.junit.Ignore;
import org.powermock.api.mockito.PowerMockito;

/**
 * User: stukushin
 * Date: 27.12.12
 * Time: 14:59
 */
@Ignore
public class MongoStorageTest {

    @Before
    public void init() {
        Mongo mongo = PowerMockito.mock(Mongo.class);
        DB db = PowerMockito.mock(DB.class);
        DBCollection dbCollection = PowerMockito.mock(DBCollection.class);
    }

	public void testUpdate() {

	}
}
